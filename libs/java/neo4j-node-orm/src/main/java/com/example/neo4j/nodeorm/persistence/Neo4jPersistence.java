package com.example.neo4j.nodeorm.persistence;

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
public class Neo4jPersistence {

    private final Neo4jClient neo4jClient;

    public Map<Object, Object> saveNodesBulk(List<Object> nodes, NodeMetadata metadata) {
        String nodeName = metadata.getNodeName();
        String idPropertyName = metadata.getIdField().getFieldName();

        // Build UNWIND query for bulk insert
        StringBuilder cypher = new StringBuilder("UNWIND $nodes AS node\n");
        cypher.append("CREATE (n:").append(nodeName).append(")\n");
        cypher.append("SET n = node.properties");

        // Prepare node data
        List<Map<String, Object>> nodeData = new ArrayList<>();
        for (Object node : nodes) {
            Map<String, Object> properties = new HashMap<>();

            // Add ID field to properties if present
            try {
                Object idValue = metadata.getIdField().getField().get(node);
                if (idValue != null) {
                    properties.put(idPropertyName, idValue);
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Failed to access ID field", e);
            }

            for (PropertyMetadata property : metadata.getProperties()) {
                try {
                    Object value = property.getField().get(node);
                    if (value != null) {
                        properties.put(property.getPropertyName(), value);
                    }
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("Failed to access property: " + property.getFieldName(), e);
                }
            }

            nodeData.add(Map.of("properties", properties));
        }

        // Execute bulk insert
        neo4jClient.query(cypher.toString())
                .bind(nodeData).to("nodes")
                .run();

        return Map.of(); // No IDs to return, already generated before save
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