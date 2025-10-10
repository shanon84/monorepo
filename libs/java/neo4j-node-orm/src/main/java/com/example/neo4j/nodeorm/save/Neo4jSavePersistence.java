package com.example.neo4j.nodeorm.save;

import com.example.global.annotations.Persistence;
import com.example.neo4j.nodeorm.metadata.AuditFieldMetadata;
import com.example.neo4j.nodeorm.metadata.NodeMetadata;
import com.example.neo4j.nodeorm.metadata.PropertyMetadata;
import com.example.neo4j.nodeorm.metadata.RelationshipMetadata;
import lombok.RequiredArgsConstructor;
import org.springframework.data.neo4j.core.Neo4jClient;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Persistence
@RequiredArgsConstructor
public class Neo4jSavePersistence {

    private final Neo4jClient neo4jClient;

    public Map<Object, Boolean> saveNodesBulk(
            List<Object> nodes,
            NodeMetadata metadata,
            Map<Object, Map<String, Object>> auditValuesMap
    ) {
        String nodeName = metadata.getNodeName();
        String idPropertyName = metadata.getIdField().getFieldName();

        // Build UNWIND query for bulk MERGE
        StringBuilder cypher = new StringBuilder("UNWIND $nodes AS node\n");
        cypher.append("MERGE (n:").append(nodeName).append(" {")
                .append(idPropertyName).append(": node.properties.").append(idPropertyName).append("})\n");

        // Set properties on CREATE
        cypher.append("ON CREATE SET n = node.properties, n.___wasCreated = true");
        if (metadata.getAuditFields().hasAnyAuditFields()) {
            cypher.append(", n += node.auditCreate");
        }

        // Set properties on MATCH (update)
        cypher.append("\nON MATCH SET n += node.properties, n.___wasCreated = false");
        if (metadata.getAuditFields().hasLastModifiedBy() || metadata.getAuditFields().hasLastModifiedDate()) {
            cypher.append(", n += node.auditUpdate");
        }

        cypher.append("\nWITH n, node, n.___wasCreated AS wasCreated")
                .append("\nREMOVE n.___wasCreated")
                .append("\nRETURN node.tempId AS tempId, id(n) AS generatedId, wasCreated");

        // Prepare node data
        List<Map<String, Object>> nodeData = new ArrayList<>();
        for (Object node : nodes) {
            Map<String, Object> properties = extractNodeProperties(node, metadata);
            Map<String, Object> auditCreate = auditValuesMap.getOrDefault(node, new HashMap<>());
            Map<String, Object> auditUpdate = extractAuditUpdateValues(auditValuesMap.get(node));

            Map<String, Object> nodeEntry = new HashMap<>();
            nodeEntry.put("tempId", System.identityHashCode(node));
            nodeEntry.put("properties", properties);
            nodeEntry.put("auditCreate", auditCreate);
            nodeEntry.put("auditUpdate", auditUpdate);

            nodeData.add(nodeEntry);
        }

        // Execute bulk merge and collect results
        List<Map<String, Object>> results = neo4jClient.query(cypher.toString())
                .bind(nodeData).to("nodes")
                .fetch()
                .all()
                .stream()
                .toList();

        // Build map: node -> wasCreated
        Map<Object, Boolean> creationStatusMap = new HashMap<>();
        for (int i = 0; i < nodes.size(); i++) {
            Object node = nodes.get(i);
            Map<String, Object> result = results.get(i);
            Boolean wasCreated = (Boolean) result.get("wasCreated");
            creationStatusMap.put(node, wasCreated);
        }

        return creationStatusMap;
    }

    private Map<String, Object> extractAuditUpdateValues(Map<String, Object> auditValues) {
        if (auditValues == null) {
            return new HashMap<>();
        }

        Map<String, Object> updateValues = new HashMap<>();
        auditValues.forEach((key, value) -> {
            if (key.startsWith("lastModified")) {
                updateValues.put(key, value);
            }
        });

        return updateValues;
    }

    private Map<String, Object> extractNodeProperties(Object node, NodeMetadata metadata) {
        Map<String, Object> properties = new HashMap<>();
        String idPropertyName = metadata.getIdField().getFieldName();

        try {
            // Add ID field to properties if present
            Object idValue = metadata.getIdField().getField().get(node);
            if (idValue != null) {
                properties.put(idPropertyName, idValue);
            }

            // Add all other properties (excluding audit fields)
            for (PropertyMetadata property : metadata.getProperties()) {
                Object value = property.getField().get(node);
                if (value != null) {
                    properties.put(property.getPropertyName(), value);
                }
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to access field on node", e);
        }

        return properties;
    }

    public Map<String, Object> extractAuditProperties(Object node, AuditFieldMetadata auditFields) {
        Map<String, Object> auditProperties = new HashMap<>();

        try {
            if (auditFields.hasCreatedBy()) {
                Object value = auditFields.getCreatedByField().get(node);
                if (value != null) {
                    auditProperties.put(auditFields.getCreatedByField().getName(), value);
                }
            }

            if (auditFields.hasCreatedDate()) {
                Object value = auditFields.getCreatedDateField().get(node);
                if (value != null) {
                    auditProperties.put(auditFields.getCreatedDateField().getName(), value);
                }
            }

            if (auditFields.hasLastModifiedBy()) {
                Object value = auditFields.getLastModifiedByField().get(node);
                if (value != null) {
                    auditProperties.put(auditFields.getLastModifiedByField().getName(), value);
                }
            }

            if (auditFields.hasLastModifiedDate()) {
                Object value = auditFields.getLastModifiedDateField().get(node);
                if (value != null) {
                    auditProperties.put(auditFields.getLastModifiedDateField().getName(), value);
                }
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to access audit field on node", e);
        }

        return auditProperties;
    }

    public void createRelationshipsBulk(
            Object sourceId,
            List<Object> targetIds,
            RelationshipMetadata relationship,
            NodeMetadata sourceMetadata,
            NodeMetadata targetMetadata
    ) {
        String relationshipType = relationship.getRelationshipType();
        String sourceIdField = sourceMetadata.getIdField().getFieldName();
        String targetIdField = targetMetadata.getIdField().getFieldName();
        String sourceLabel = sourceMetadata.getNodeName();
        String targetLabel = targetMetadata.getNodeName();

        String cypher;
        if (relationship.getDirection() == org.springframework.data.neo4j.core.schema.Relationship.Direction.OUTGOING) {
            cypher = "MATCH (source:" + sourceLabel + ") WHERE source." + sourceIdField + " = $sourceId " +
                    "UNWIND $targetIds AS targetId " +
                    "MATCH (target:" + targetLabel + ") WHERE target." + targetIdField + " = targetId " +
                    "CREATE (source)-[:" + relationshipType + "]->(target)";
        } else {
            cypher = "MATCH (target:" + targetLabel + ") WHERE target." + targetIdField + " = $sourceId " +
                    "UNWIND $targetIds AS sourceId " +
                    "MATCH (source:" + sourceLabel + ") WHERE source." + sourceIdField + " = sourceId " +
                    "CREATE (source)-[:" + relationshipType + "]->(target)";
        }

        neo4jClient.query(cypher)
                .bind(sourceId).to("sourceId")
                .bind(targetIds).to("targetIds")
                .run();
    }
}
