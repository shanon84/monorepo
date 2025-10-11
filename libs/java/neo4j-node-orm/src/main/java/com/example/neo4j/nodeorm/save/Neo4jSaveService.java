package com.example.neo4j.nodeorm.save;

import com.example.neo4j.nodeorm.metadata.NodeMetadata;
import com.example.neo4j.nodeorm.metadata.NodeMetadataExtractor;
import com.example.neo4j.nodeorm.metadata.RelationshipMetadata;
import com.example.neo4j.nodeorm.reflection.ReflectionService;
import com.example.neo4j.nodeorm.validation.NodeValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
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

        for (Object entity : entities) {
            collectNodesToSave(entity, allNodesToSave);
        }

        // Process each node: validate, generate ID, set audit fields
        for (Object node : allNodesToSave) {
            // 1. Validate
            nodeValidator.validateNodeAnnotation(node.getClass());

            // 2. Get metadata
            NodeMetadata metadata = metadataExtractor.extractMetadata(node.getClass());

            // 3. Determine if create or update (BEFORE generating ID)
            boolean isCreate = isNewNode(node, metadata);

            // 4. Generate or get ID (sets ID on the node object)
            getOrGenerateId(node, metadata);

            // 5. Set audit fields directly on entity
            auditFieldPopulatorService.populateAuditFieldsOnNode(node, metadata, isCreate);
        }

        // 6. Save all nodes in bulk
        saveNodesInBulk(allNodesToSave);

        // 7. Create relationships
        for (Object entity : entities) {
            NodeMetadata metadata = metadataExtractor.extractMetadata(entity.getClass());
            createRelationshipsForEntity(entity, metadata);
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
            Set<Object> allNodesToSave
    ) {
        if (entity == null || allNodesToSave.contains(entity)) {
            return;
        }

        allNodesToSave.add(entity);
        NodeMetadata metadata = metadataExtractor.extractMetadata(entity.getClass());

        collectRelatedNodes(entity, metadata, allNodesToSave);
    }

    private void collectRelatedNodes(
            Object entity,
            NodeMetadata metadata,
            Set<Object> allNodesToSave
    ) {
        for (RelationshipMetadata relationship : metadata.getRelationships()) {
            Object relatedValue = reflectionService.getFieldValue(relationship.getField(), entity);

            if (relatedValue == null) {
                continue;
            }

            if (relationship.isCollection()) {
                collectRelatedEntitiesFromCollection((Collection<?>) relatedValue, relationship, allNodesToSave);
            } else {
                collectRelatedNode(relatedValue, relationship, allNodesToSave);
            }
        }
    }

    private void collectRelatedEntitiesFromCollection(
            Collection<?> relatedEntities,
            RelationshipMetadata relationship,
            Set<Object> allNodesToSave
    ) {
        for (Object relatedEntity : relatedEntities) {
            collectRelatedNode(relatedEntity, relationship, allNodesToSave);
        }
    }

    private void collectRelatedNode(
            Object relatedNode,
            RelationshipMetadata relationship,
            Set<Object> allNodesToSave
    ) {
        // If cascadeUpdates is false, only collect nodes that don't have an ID yet (new nodes)
        if (!relationship.isCascadeUpdates()) {
            NodeMetadata relatedMetadata = metadataExtractor.extractMetadata(relatedNode.getClass());
            Object relatedId = reflectionService.getFieldValue(relatedMetadata.getIdField().getField(), relatedNode);

            // Skip nodes with existing IDs when cascadeUpdates = false
            if (relatedId != null) {
                return;
            }
        }

        // Collect the node for saving
        collectNodesToSave(relatedNode, allNodesToSave);
    }

    private void saveNodesInBulk(Set<Object> allNodesToSave) {
        // Group nodes by type
        Map<Class<?>, List<Object>> nodesByType = allNodesToSave.stream()
                .collect(java.util.stream.Collectors.groupingBy(Object::getClass));

        // Save each type in bulk
        for (Map.Entry<Class<?>, List<Object>> entry : nodesByType.entrySet()) {
            Class<?> nodeClass = entry.getKey();
            List<Object> nodes = entry.getValue();
            NodeMetadata metadata = metadataExtractor.extractMetadata(nodeClass);

            // Separate nodes into CREATE and UPDATE lists
            List<Object> nodesToCreate = new java.util.ArrayList<>();
            List<Object> nodesToUpdate = new java.util.ArrayList<>();

            for (Object node : nodes) {
                // Get ID directly from node object
                Object nodeId = reflectionService.getFieldValue(metadata.getIdField().getField(), node);

                if (nodeId != null) {
                    // Real ID exists - check if node exists in DB
                    boolean exists = savePersistence.nodeExistsById(nodeId, metadata);
                    if (exists) {
                        nodesToUpdate.add(node);
                    } else {
                        nodesToCreate.add(node);
                    }
                } else {
                    // No ID - definitely a new node
                    nodesToCreate.add(node);
                }
            }

            // Execute bulk CREATE
            if (!nodesToCreate.isEmpty()) {
                savePersistence.createNodesBulk(nodesToCreate, metadata);
            }

            // Execute bulk UPDATE
            if (!nodesToUpdate.isEmpty()) {
                savePersistence.updateNodesBulk(nodesToUpdate, metadata);
            }
        }
    }

    private void createRelationshipsForEntity(Object entity, NodeMetadata metadata) {
        // Get ID directly from entity
        Object sourceId = reflectionService.getFieldValue(metadata.getIdField().getField(), entity);

        if (sourceId == null) {
            return;
        }

        for (RelationshipMetadata relationship : metadata.getRelationships()) {
            createRelationshipForField(entity, sourceId, relationship, metadata);
        }
    }

    private void createRelationshipForField(
            Object entity,
            Object sourceId,
            RelationshipMetadata relationship,
            NodeMetadata sourceMetadata
    ) {
        Object relatedValue = reflectionService.getFieldValue(relationship.getField(), entity);

        if (relatedValue == null) {
            return;
        }

        NodeMetadata targetMetadata = metadataExtractor.extractMetadata(relationship.getTargetType());
        List<Object> targetIds = collectTargetIds(relatedValue, relationship, targetMetadata);

        if (targetIds.isEmpty()) {
            return;
        }

        savePersistence.createRelationshipsBulk(sourceId, targetIds, relationship, sourceMetadata, targetMetadata);
    }

    private List<Object> collectTargetIds(Object relatedValue, RelationshipMetadata relationship, NodeMetadata targetMetadata) {
        List<Object> targetIds = new java.util.ArrayList<>();

        if (relationship.isCollection()) {
            collectTargetIdsFromCollection((Collection<?>) relatedValue, targetMetadata, targetIds);
        } else {
            addTargetIdIfExists(relatedValue, targetMetadata, targetIds);
        }

        return targetIds;
    }

    private void collectTargetIdsFromCollection(Collection<?> relatedEntities, NodeMetadata targetMetadata, List<Object> targetIds) {
        for (Object relatedEntity : relatedEntities) {
            addTargetIdIfExists(relatedEntity, targetMetadata, targetIds);
        }
    }

    private void addTargetIdIfExists(Object relatedEntity, NodeMetadata targetMetadata, List<Object> targetIds) {
        // Get ID directly from related entity
        Object targetId = reflectionService.getFieldValue(targetMetadata.getIdField().getField(), relatedEntity);

        if (targetId != null) {
            targetIds.add(targetId);
        }
    }
}
