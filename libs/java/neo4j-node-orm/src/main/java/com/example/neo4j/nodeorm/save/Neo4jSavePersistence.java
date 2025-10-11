package com.example.neo4j.nodeorm.save;

import com.example.global.annotations.Persistence;
import com.example.neo4j.nodeorm.metadata.NodeMetadata;
import com.example.neo4j.nodeorm.metadata.PropertyMetadata;
import com.example.neo4j.nodeorm.metadata.RelationshipMetadata;
import lombok.RequiredArgsConstructor;
import org.springframework.data.neo4j.core.Neo4jClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Persistence
@RequiredArgsConstructor
public class Neo4jSavePersistence {

    private final Neo4jClient neo4jClient;

    public boolean nodeExistsById(Object id, NodeMetadata metadata) {
        String nodeName = metadata.getNodeName();
        String idPropertyName = metadata.getIdField().getFieldName();

        String cypher = "MATCH (n:" + nodeName + ") WHERE n." + idPropertyName + " = $id RETURN count(n) > 0 AS exists";

        Map<String, Object> result = neo4jClient.query(cypher)
                .bind(id).to("id")
                .fetch()
                .one()
                .orElse(Map.of("exists", false));

        return (Boolean) result.get("exists");
    }

    public void updateNodesBulk(
            List<Object> nodes,
            NodeMetadata metadata
    ) {
        String nodeName = metadata.getNodeName();
        String idPropertyName = metadata.getIdField().getFieldName();

        // Build UNWIND query for bulk UPDATE (only modified fields)
        StringBuilder cypher = new StringBuilder("UNWIND $nodes AS node\n");
        cypher.append("MATCH (n:").append(nodeName).append(") WHERE n.").append(idPropertyName).append(" = node.id\n");
        cypher.append("SET n += node.properties");

        // Prepare node data
        List<Map<String, Object>> nodeData = new ArrayList<>();
        for (Object node : nodes) {
            Map<String, Object> properties = extractNodePropertiesWithoutId(node, metadata);

            Map<String, Object> nodeEntry = new HashMap<>();
            try {
                nodeEntry.put("id", metadata.getIdField().getField().get(node));
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Failed to access ID field on node", e);
            }
            nodeEntry.put("properties", properties);

            nodeData.add(nodeEntry);
        }

        // Execute bulk update
        neo4jClient.query(cypher.toString())
                .bind(nodeData).to("nodes")
                .run();
    }

    public void createNodesBulk(
            List<Object> nodes,
            NodeMetadata metadata
    ) {
        String nodeName = metadata.getNodeName();

        // Build UNWIND query for bulk CREATE
        StringBuilder cypher = new StringBuilder("UNWIND $nodes AS node\n");
        cypher.append("CREATE (n:").append(nodeName).append(")\n");
        cypher.append("SET n = node.properties");

        // Prepare node data
        List<Map<String, Object>> nodeData = new ArrayList<>();
        for (Object node : nodes) {
            Map<String, Object> properties = extractNodeProperties(node, metadata);

            Map<String, Object> nodeEntry = new HashMap<>();
            nodeEntry.put("properties", properties);

            nodeData.add(nodeEntry);
        }

        // Execute bulk create
        neo4jClient.query(cypher.toString())
                .bind(nodeData).to("nodes")
                .run();
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

            // Add all other properties (including audit fields now)
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

    private Map<String, Object> extractNodePropertiesWithoutId(Object node, NodeMetadata metadata) {
        Map<String, Object> properties = new HashMap<>();

        try {
            // Add all properties EXCEPT ID field (including audit fields)
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
