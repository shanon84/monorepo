package com.example.neo4j.nodeorm.find;

import com.example.global.annotations.Persistence;
import com.example.neo4j.nodeorm.metadata.NodeMetadata;
import com.example.neo4j.nodeorm.metadata.PropertyMetadata;
import lombok.RequiredArgsConstructor;
import org.springframework.data.neo4j.core.Neo4jClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Persistence
@RequiredArgsConstructor
public class Neo4jFindPersistence {

    private final Neo4jClient neo4jClient;

    public <T> java.util.Optional<T> findNodeById(String id, NodeMetadata metadata, Class<T> entityClass) {
        String nodeName = metadata.getNodeName();
        String idField = metadata.getIdField().getFieldName();

        String cypher = "MATCH (n:" + nodeName + ") WHERE n." + idField + " = $id RETURN n";

        return neo4jClient.query(cypher)
                .bind(id).to("id")
                .fetchAs(entityClass)
                .mappedBy((typeSystem, record) -> mapRecordToEntity(record.get("n").asMap(), metadata, entityClass))
                .one();
    }

    public boolean existsNodeById(String id, NodeMetadata metadata) {
        String nodeName = metadata.getNodeName();
        String idField = metadata.getIdField().getFieldName();

        String cypher = "MATCH (n:" + nodeName + ") WHERE n." + idField + " = $id RETURN count(n) > 0 AS exists";

        Boolean exists = neo4jClient.query(cypher)
                .bind(id).to("id")
                .fetchAs(Boolean.class)
                .mappedBy((typeSystem, record) -> record.get("exists").asBoolean())
                .one()
                .orElse(false);

        return exists;
    }

    public <T> List<T> findAllNodes(NodeMetadata metadata, Class<T> entityClass) {
        String nodeName = metadata.getNodeName();

        String cypher = "MATCH (n:" + nodeName + ") RETURN n";

        return neo4jClient.query(cypher)
                .fetchAs(entityClass)
                .mappedBy((typeSystem, record) -> mapRecordToEntity(record.get("n").asMap(), metadata, entityClass))
                .all()
                .stream()
                .toList();
    }

    public <T> List<T> findAllNodesById(Iterable<String> ids, NodeMetadata metadata, Class<T> entityClass) {
        String nodeName = metadata.getNodeName();
        String idField = metadata.getIdField().getFieldName();

        List<String> idList = new ArrayList<>();
        ids.forEach(idList::add);

        if (idList.isEmpty()) {
            return List.of();
        }

        String cypher = "MATCH (n:" + nodeName + ") WHERE n." + idField + " IN $ids RETURN n";

        return neo4jClient.query(cypher)
                .bind(idList).to("ids")
                .fetchAs(entityClass)
                .mappedBy((typeSystem, record) -> mapRecordToEntity(record.get("n").asMap(), metadata, entityClass))
                .all()
                .stream()
                .toList();
    }

    public long countNodes(NodeMetadata metadata) {
        String nodeName = metadata.getNodeName();

        String cypher = "MATCH (n:" + nodeName + ") RETURN count(n) AS count";

        return neo4jClient.query(cypher)
                .fetchAs(Long.class)
                .mappedBy((typeSystem, record) -> record.get("count").asLong())
                .one()
                .orElse(0L);
    }

    private <T> T mapRecordToEntity(Map<String, Object> nodeProperties, NodeMetadata metadata, Class<T> entityClass) {
        try {
            T entity = entityClass.getDeclaredConstructor().newInstance();

            // Map ID field
            Object idValue = nodeProperties.get(metadata.getIdField().getFieldName());
            if (idValue != null) {
                metadata.getIdField().getField().set(entity, idValue.toString());
            }

            // Map properties
            for (PropertyMetadata property : metadata.getProperties()) {
                Object value = nodeProperties.get(property.getPropertyName());
                if (value != null) {
                    property.getField().set(entity, value);
                }
            }

            return entity;
        } catch (Exception e) {
            throw new RuntimeException("Failed to map node to entity: " + entityClass.getName(), e);
        }
    }
}
