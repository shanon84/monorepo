package com.example.neo4j.nodeorm.save;

import com.example.neo4j.nodeorm.metadata.NodeMetadata;
import com.example.neo4j.nodeorm.metadata.NodeMetadataExtractor;
import com.example.neo4j.nodeorm.metadata.RelationshipMetadata;
import com.example.neo4j.nodeorm.reflection.ReflectionService;
import com.example.neo4j.nodeorm.validation.NodeValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class Neo4jSaveService {

    private final Neo4jSavePersistence savePersistence;
    private final NodeValidator nodeValidator;
    private final NodeMetadataExtractor metadataExtractor;
    private final IdGenerationService idGenerationService;
    private final AuditFieldPopulatorService auditFieldPopulatorService;
    private final ReflectionService reflectionService;

    public <S> List<S> saveAll(List<S> entities) {
        // Collect all nodes to save (including related nodes)
        Set<Object> allNodesToSave = new LinkedHashSet<>();
        Map<Object, NodeMetadata> nodeMetadataMap = new HashMap<>();

        for (Object entity : entities) {
            collectNodesToSave(entity, allNodesToSave, nodeMetadataMap);
        }

        // Process each node: validate, generate ID, set audit fields
        // Use ID (or temp ID) as key for all subsequent operations
        Map<Object, Object> nodeIdMap = new LinkedHashMap<>();

        for (Object node : allNodesToSave) {
            // 1. Validate
            nodeValidator.validateNodeAnnotation(node.getClass());

            // 2. Get metadata
            NodeMetadata metadata = metadataExtractor.extractMetadata(node.getClass());

            // 3. Determine if create or update (BEFORE generating ID)
            boolean isCreate = isNewNode(node, metadata);

            // 4. Generate or get ID
            Object nodeId = getOrGenerateId(node, metadata);
            nodeIdMap.put(node, nodeId);

            // 5. Set audit fields directly on entity
            auditFieldPopulatorService.populateAuditFieldsOnNode(node, metadata, isCreate);
        }

        // 6. Extract properties from entities (last step - after audit fields are set)
        // 7. Save all nodes in bulk
        saveNodesInBulk(allNodesToSave, nodeIdMap);

        // 8. Create relationships
        for (Object entity : entities) {
            NodeMetadata metadata = metadataExtractor.extractMetadata(entity.getClass());
            createRelationshipsForEntity(entity, metadata, nodeIdMap);
        }

        return entities;
    }

    private Object getOrGenerateId(Object node, NodeMetadata metadata) {
        Object currentId = reflectionService.getFieldValue(metadata.getIdField().getField(), node);

        // If ID already exists, return it
        if (currentId != null) {
            return currentId;
        }

        // Generate ID if field has @GeneratedValue
        if (metadata.getIdField().isGenerated()) {
            Object generatedId = idGenerationService.generateIdValue(node, metadata);
            reflectionService.setFieldValue(metadata.getIdField().getField(), node, generatedId);
            return generatedId;
        }

        // No ID and not generated - will be set by Neo4j
        // Use temp ID for now (system identity hash code)
        return System.identityHashCode(node);
    }

    private boolean isNewNode(Object node, NodeMetadata metadata) {
        Object currentId = reflectionService.getFieldValue(metadata.getIdField().getField(), node);
        return currentId == null || (currentId instanceof Number && ((Number) currentId).longValue() == 0);
    }

    private void collectNodesToSave(
            Object entity,
            Set<Object> allNodesToSave,
            Map<Object, NodeMetadata> nodeMetadataMap
    ) {
        if (entity == null || allNodesToSave.contains(entity)) {
            return;
        }

        allNodesToSave.add(entity);
        NodeMetadata metadata = metadataExtractor.extractMetadata(entity.getClass());
        nodeMetadataMap.put(entity, metadata);

        collectRelatedNodes(entity, metadata, allNodesToSave, nodeMetadataMap);
    }

    private void collectRelatedNodes(
            Object entity,
            NodeMetadata metadata,
            Set<Object> allNodesToSave,
            Map<Object, NodeMetadata> nodeMetadataMap
    ) {
        for (RelationshipMetadata relationship : metadata.getRelationships()) {
            Object relatedValue = reflectionService.getFieldValue(relationship.getField(), entity);

            if (relatedValue == null) {
                continue;
            }

            if (relationship.isCollection()) {
                collectRelatedEntitiesFromCollection((Collection<?>) relatedValue, allNodesToSave, nodeMetadataMap);
            } else {
                collectNodesToSave(relatedValue, allNodesToSave, nodeMetadataMap);
            }
        }
    }

    private void collectRelatedEntitiesFromCollection(
            Collection<?> relatedEntities,
            Set<Object> allNodesToSave,
            Map<Object, NodeMetadata> nodeMetadataMap
    ) {
        for (Object relatedEntity : relatedEntities) {
            collectNodesToSave(relatedEntity, allNodesToSave, nodeMetadataMap);
        }
    }

    private void saveNodesInBulk(
            Set<Object> allNodesToSave,
            Map<Object, Object> nodeIdMap
    ) {
        // Group nodes by type
        Map<Class<?>, List<Object>> nodesByType = allNodesToSave.stream()
                .collect(java.util.stream.Collectors.groupingBy(Object::getClass));

        // Save each type in bulk
        for (Map.Entry<Class<?>, List<Object>> entry : nodesByType.entrySet()) {
            Class<?> nodeClass = entry.getKey();
            List<Object> nodes = entry.getValue();
            NodeMetadata metadata = metadataExtractor.extractMetadata(nodeClass);

            Map<Object, Object> generatedIds = savePersistence.saveNodesBulk(nodes, metadata);

            // Update the nodeIdMap with generated IDs from Neo4j
            nodeIdMap.putAll(generatedIds);
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
        Object relatedValue = reflectionService.getFieldValue(relationship.getField(), entity);

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
