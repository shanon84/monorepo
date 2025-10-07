package com.example.neo4j.nodeorm.save;

import com.example.neo4j.nodeorm.metadata.NodeMetadata;
import com.example.neo4j.nodeorm.metadata.NodeMetadataExtractor;
import com.example.neo4j.nodeorm.metadata.RelationshipMetadata;
import com.example.neo4j.nodeorm.validation.NodeValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class Neo4jSaveService {

    private final Neo4jSavePersistence savePersistence;
    private final NodeValidator nodeValidator;
    private final NodeMetadataExtractor metadataExtractor;

    public <S> List<S> saveAll(List<S> entities) {
        nodeValidator.validateNodes(entities);

        Class<?> entityClass = entities.get(0).getClass();
        NodeMetadata metadata = metadataExtractor.extractMetadata(entityClass);

        // Collect all nodes to save (including related nodes)
        Map<Object, Object> allNodesToSave = new LinkedHashMap<>();
        Map<Object, NodeMetadata> nodeMetadataMap = new HashMap<>();

        for (Object entity : entities) {
            collectNodesToSave(entity, metadata, allNodesToSave, nodeMetadataMap);
        }

        // Generate IDs for nodes with @GeneratedValue before saving
        Map<Object, Object> nodeIdMap = generateIdsForNodes(allNodesToSave, nodeMetadataMap);

        // Save all nodes in bulk
        saveNodesInBulk(allNodesToSave, nodeMetadataMap);

        // Create relationships
        createRelationships(entities, metadata, nodeIdMap);

        return entities;
    }

    private Map<Object, Object> generateIdsForNodes(
            Map<Object, Object> allNodesToSave,
            Map<Object, NodeMetadata> nodeMetadataMap
    ) {
        Map<Class<?>, Object> generatorCache = new HashMap<>();
        Map<Object, Object> nodeIdMap = new LinkedHashMap<>();

        for (Map.Entry<Object, Object> entry : allNodesToSave.entrySet()) {
            Object node = entry.getKey();
            NodeMetadata metadata = nodeMetadataMap.get(node);

            if (metadata == null) {
                continue;
            }

            Object nodeId = generateOrGetNodeId(node, metadata, generatorCache);
            if (nodeId != null) {
                nodeIdMap.put(node, nodeId);
            }
        }

        return nodeIdMap;
    }

    private Object generateOrGetNodeId(Object node, NodeMetadata metadata, Map<Class<?>, Object> generatorCache) {
        Field idField = metadata.getIdField().getField();
        Object currentId = getFieldValue(idField, node);

        if (currentId != null) {
            return currentId;
        }

        if (!metadata.getIdField().isGenerated()) {
            return null;
        }

        GeneratedValue generatedValue = idField.getAnnotation(GeneratedValue.class);

        // Try value() first (e.g. @GeneratedValue(UUIDGenerator.class)), then generatorClass()
        Class<?> generatorClass = generatedValue.value();

        // If value is not set, use generatorClass (deprecated but still supported)
        if (generatorClass == null || generatorClass.getName().contains("InternalIdGenerator")) {
            generatorClass = generatedValue.generatorClass();
        }

        return generateIdWithGenerator(node, metadata, idField, generatorClass, generatorCache);
    }

    private Object generateIdWithGenerator(
            Object node,
            NodeMetadata metadata,
            Field idField,
            Class<?> generatorClass,
            Map<Class<?>, Object> generatorCache
    ) {
        Object generator = generatorCache.computeIfAbsent(generatorClass, this::instantiateGenerator);

        Object generatedId = invokeGenerateId(generator, generatorClass, metadata.getNodeName(), node);

        if (generatedId == null) {
            throw new RuntimeException("ID generator " + generatorClass.getName() +
                    " returned null for entity " + node.getClass().getName());
        }

        Object convertedId = convertIdToFieldType(generatedId, idField.getType());
        setFieldValue(idField, node, convertedId);

        return convertedId;
    }

    private Object instantiateGenerator(Class<?> generatorClass) {
        try {
            return generatorClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate ID generator: " + generatorClass.getName(), e);
        }
    }

    private Object invokeGenerateId(Object generator, Class<?> generatorClass, String nodeName, Object node) {
        try {
            return generatorClass.getMethod("generateId", String.class, Object.class)
                    .invoke(generator, nodeName, node);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("ID generator " + generatorClass.getName() +
                    " must have a method: Object generateId(String primaryLabel, Object entity)", e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke generateId on " + generatorClass.getName(), e);
        }
    }

    private Object getFieldValue(Field field, Object object) {
        try {
            return field.get(object);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to access field: " + field.getName(), e);
        }
    }

    private void setFieldValue(Field field, Object object, Object value) {
        try {
            field.set(object, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to set field: " + field.getName(), e);
        }
    }

    private Object convertIdToFieldType(Object generatedId, Class<?> fieldType) {
        // If types match, return as-is
        if (fieldType.isInstance(generatedId)) {
            return generatedId;
        }

        // Convert UUID to String
        if (generatedId instanceof java.util.UUID && fieldType == String.class) {
            return generatedId.toString();
        }

        // Convert UUID to Long (hash code)
        if (generatedId instanceof java.util.UUID && (fieldType == Long.class || fieldType == long.class)) {
            return (long) generatedId.hashCode();
        }

        // Convert String UUID to Long (hash code)
        if (generatedId instanceof String && fieldType == Long.class) {
            return (long) generatedId.hashCode();
        }

        // Try to convert to Long
        if (fieldType == Long.class || fieldType == long.class) {
            if (generatedId instanceof Number) {
                return ((Number) generatedId).longValue();
            }
        }

        // Return as-is and let reflection handle the conversion/error
        return generatedId;
    }

    private void collectNodesToSave(
            Object entity,
            NodeMetadata metadata,
            Map<Object, Object> allNodesToSave,
            Map<Object, NodeMetadata> nodeMetadataMap
    ) {
        if (entity == null || allNodesToSave.containsKey(entity)) {
            return;
        }

        allNodesToSave.put(entity, entity);
        nodeMetadataMap.put(entity, metadata);

        collectRelatedNodes(entity, metadata, allNodesToSave, nodeMetadataMap);
    }

    private void collectRelatedNodes(
            Object entity,
            NodeMetadata metadata,
            Map<Object, Object> allNodesToSave,
            Map<Object, NodeMetadata> nodeMetadataMap
    ) {
        for (RelationshipMetadata relationship : metadata.getRelationships()) {
            Object relatedValue = getFieldValue(relationship.getField(), entity);

            if (relatedValue == null) {
                continue;
            }

            NodeMetadata relatedMetadata = metadataExtractor.extractMetadata(relationship.getTargetType());

            if (relationship.isCollection()) {
                collectRelatedEntitiesFromCollection((Collection<?>) relatedValue, relatedMetadata, allNodesToSave, nodeMetadataMap);
            } else {
                collectNodesToSave(relatedValue, relatedMetadata, allNodesToSave, nodeMetadataMap);
            }
        }
    }

    private void collectRelatedEntitiesFromCollection(
            Collection<?> relatedEntities,
            NodeMetadata relatedMetadata,
            Map<Object, Object> allNodesToSave,
            Map<Object, NodeMetadata> nodeMetadataMap
    ) {
        for (Object relatedEntity : relatedEntities) {
            collectNodesToSave(relatedEntity, relatedMetadata, allNodesToSave, nodeMetadataMap);
        }
    }

    private void saveNodesInBulk(
            Map<Object, Object> allNodesToSave,
            Map<Object, NodeMetadata> nodeMetadataMap
    ) {
        // Group nodes by type
        Map<Class<?>, List<Object>> nodesByType = allNodesToSave.values().stream()
                .collect(java.util.stream.Collectors.groupingBy(Object::getClass));

        // Save each type in bulk
        for (Map.Entry<Class<?>, List<Object>> entry : nodesByType.entrySet()) {
            Class<?> nodeClass = entry.getKey();
            List<Object> nodes = entry.getValue();
            NodeMetadata metadata = metadataExtractor.extractMetadata(nodeClass);

            savePersistence.saveNodesBulk(nodes, metadata);
        }
    }

    private <T> void createRelationships(List<T> entities, NodeMetadata metadata, Map<Object, Object> nodeIdMap) {
        for (T entity : entities) {
            createRelationshipsForEntity(entity, metadata, nodeIdMap);
        }
    }

    private void createRelationshipsForEntity(Object entity, NodeMetadata metadata, Map<Object, Object> nodeIdMap) {
        Object sourceId = nodeIdMap.get(entity);

        if (sourceId == null) {
            return;
        }

        for (RelationshipMetadata relationship : metadata.getRelationships()) {
            createRelationshipForField(entity, sourceId, relationship, metadata, nodeIdMap);
        }
    }

    private void createRelationshipForField(
            Object entity,
            Object sourceId,
            RelationshipMetadata relationship,
            NodeMetadata sourceMetadata,
            Map<Object, Object> nodeIdMap
    ) {
        Object relatedValue = getFieldValue(relationship.getField(), entity);

        if (relatedValue == null) {
            return;
        }

        NodeMetadata targetMetadata = metadataExtractor.extractMetadata(relationship.getTargetType());
        List<Object> targetIds = collectTargetIds(relatedValue, relationship, nodeIdMap);

        if (targetIds.isEmpty()) {
            return;
        }

        savePersistence.createRelationshipsBulk(sourceId, targetIds, relationship, sourceMetadata, targetMetadata);
    }

    private List<Object> collectTargetIds(Object relatedValue, RelationshipMetadata relationship, Map<Object, Object> nodeIdMap) {
        List<Object> targetIds = new java.util.ArrayList<>();

        if (relationship.isCollection()) {
            collectTargetIdsFromCollection((Collection<?>) relatedValue, nodeIdMap, targetIds);
        } else {
            addTargetIdIfExists(relatedValue, nodeIdMap, targetIds);
        }

        return targetIds;
    }

    private void collectTargetIdsFromCollection(Collection<?> relatedEntities, Map<Object, Object> nodeIdMap, List<Object> targetIds) {
        for (Object relatedEntity : relatedEntities) {
            addTargetIdIfExists(relatedEntity, nodeIdMap, targetIds);
        }
    }

    private void addTargetIdIfExists(Object relatedEntity, Map<Object, Object> nodeIdMap, List<Object> targetIds) {
        Object targetId = nodeIdMap.get(relatedEntity);

        if (targetId != null) {
            targetIds.add(targetId);
        }
    }
}
