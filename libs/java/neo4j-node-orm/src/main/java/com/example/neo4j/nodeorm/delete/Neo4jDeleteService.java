package com.example.neo4j.nodeorm.delete;

import com.example.neo4j.nodeorm.metadata.NodeMetadata;
import com.example.neo4j.nodeorm.metadata.NodeMetadataExtractor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class Neo4jDeleteService {

    private final Neo4jDeletePersistence deletePersistence;
    private final NodeMetadataExtractor metadataExtractor;

    public <T> void deleteById(String id, Class<T> entityClass) {
        NodeMetadata metadata = metadataExtractor.extractMetadata(entityClass);
        deletePersistence.deleteNodeById(id, metadata);
    }

    public <T> void delete(T entity) {
        NodeMetadata metadata = metadataExtractor.extractMetadata(entity.getClass());
        Field idField = metadata.getIdField().getField();

        try {
            String id = (String) idField.get(entity);
            if (id == null) {
                throw new IllegalArgumentException("Cannot delete entity without ID");
            }
            deletePersistence.deleteNodeById(id, metadata);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to access ID field", e);
        }
    }

    public <T> void deleteAllById(Iterable<String> ids, Class<T> entityClass) {
        NodeMetadata metadata = metadataExtractor.extractMetadata(entityClass);
        List<String> idList = new ArrayList<>();
        ids.forEach(idList::add);
        deletePersistence.deleteNodesById(idList, metadata);
    }

    public <T> void deleteAll(Iterable<T> entities) {
        if (!entities.iterator().hasNext()) {
            return;
        }

        T firstEntity = entities.iterator().next();
        NodeMetadata metadata = metadataExtractor.extractMetadata(firstEntity.getClass());
        Field idField = metadata.getIdField().getField();

        List<String> ids = new ArrayList<>();
        try {
            for (T entity : entities) {
                String id = (String) idField.get(entity);
                if (id != null) {
                    ids.add(id);
                }
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to access ID field", e);
        }

        deletePersistence.deleteNodesById(ids, metadata);
    }

    public <T> void deleteAll(Class<T> entityClass) {
        NodeMetadata metadata = metadataExtractor.extractMetadata(entityClass);
        deletePersistence.deleteAllNodes(metadata);
    }
}
