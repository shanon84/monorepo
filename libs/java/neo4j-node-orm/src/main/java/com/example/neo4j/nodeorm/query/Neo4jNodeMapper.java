package com.example.neo4j.nodeorm.query;

import com.example.neo4j.nodeorm.metadata.NodeMetadata;
import com.example.neo4j.nodeorm.metadata.NodeMetadataExtractor;
import com.example.neo4j.nodeorm.metadata.PropertyMetadata;
import com.example.neo4j.nodeorm.reflection.ReflectionService;
import lombok.RequiredArgsConstructor;
import org.neo4j.driver.types.Node;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class Neo4jNodeMapper {

    private final NodeMetadataExtractor metadataExtractor;
    private final ReflectionService reflectionService;

    /**
     * Map a Neo4j Node to an entity object.
     */
    public <T> T mapNodeToEntity(Node node, Class<T> entityClass) {
        return mapNodePropertiesToEntity(node.asMap(), entityClass);
    }

    /**
     * Map node properties (from asMap()) to an entity object.
     */
    public <T> T mapNodePropertiesToEntity(java.util.Map<String, Object> nodeProperties, Class<T> entityClass) {
        NodeMetadata metadata = metadataExtractor.extractMetadata(entityClass);

        // Create new instance
        T entity = (T) reflectionService.instantiateClass(entityClass);

        // Map ID field
        Object idValue = nodeProperties.get(metadata.getIdField().getFieldName());
        if (idValue != null) {
            reflectionService.setFieldValue(metadata.getIdField().getField(), entity, idValue);
        }

        // Map properties
        for (PropertyMetadata property : metadata.getProperties()) {
            Object value = nodeProperties.get(property.getPropertyName());
            if (value != null) {
                reflectionService.setFieldValue(property.getField(), entity, value);
            }
        }

        return entity;
    }
}
