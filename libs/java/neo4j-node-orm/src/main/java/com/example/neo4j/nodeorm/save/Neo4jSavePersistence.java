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

    public Map<Object, Object> saveNodesBulk(
            List<Object> nodes,
            NodeMetadata metadata
    ) {
        String nodeName = metadata.getNodeName();
        String idPropertyName = metadata.getIdField().getFieldName();

        // Build UNWIND query for bulk CREATE (simplified - audit fields are now in properties)
        StringBuilder cypher = new StringBuilder("UNWIND $nodes AS node\n");
        cypher.append("CREATE (n:").append(nodeName).append(")\n");
        cypher.append("SET n = node.properties\n");
        cypher.append("RETURN node.tempId AS tempId, id(n) AS generatedId");

        // Prepare node data
        List<Map<String, Object>> nodeData = new ArrayList<>();
        for (Object node : nodes) {
            Map<String, Object> properties = extractNodeProperties(node, metadata);

            Map<String, Object> nodeEntry = new HashMap<>();
            nodeEntry.put("tempId", System.identityHashCode(node));
            nodeEntry.put("properties", properties);

            nodeData.add(nodeEntry);
        }

        // Execute bulk create and collect results
        List<Map<String, Object>> results = neo4jClient.query(cypher.toString())
                .bind(nodeData).to("nodes")
                .fetch()
                .all()
                .stream()
                .toList();

        // Build map: node -> generatedId
        Map<Object, Object> idMap = new HashMap<>();
        for (int i = 0; i < nodes.size(); i++) {
            Map<String, Object> result = results.get(i);
            Long generatedId = (Long) result.get("generatedId");

            // Map node to generatedId
            Object node = nodes.get(i);
            idMap.put(node, generatedId);
        }

        return idMap;
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
