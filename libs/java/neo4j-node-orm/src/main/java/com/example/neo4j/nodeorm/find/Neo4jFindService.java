package com.example.neo4j.nodeorm.find;

import com.example.neo4j.nodeorm.metadata.NodeMetadata;
import com.example.neo4j.nodeorm.metadata.NodeMetadataExtractor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class Neo4jFindService {

    private final Neo4jFindPersistence findPersistence;
    private final NodeMetadataExtractor metadataExtractor;

    public <T, ID> Optional<T> findById(ID id, Class<T> entityClass) {
        NodeMetadata metadata = metadataExtractor.extractMetadata(entityClass);
        return findPersistence.findNodeById(String.valueOf(id), metadata, entityClass);
    }

    public <T, ID> boolean existsById(ID id, Class<T> entityClass) {
        NodeMetadata metadata = metadataExtractor.extractMetadata(entityClass);
        return findPersistence.existsNodeById(String.valueOf(id), metadata);
    }

    public <T> List<T> findAll(Class<T> entityClass) {
        NodeMetadata metadata = metadataExtractor.extractMetadata(entityClass);
        return findPersistence.findAllNodes(metadata, entityClass);
    }

    public <T, ID> List<T> findAllById(Iterable<ID> ids, Class<T> entityClass) {
        NodeMetadata metadata = metadataExtractor.extractMetadata(entityClass);
        List<String> stringIds = new ArrayList<>();
        ids.forEach(id -> stringIds.add(String.valueOf(id)));
        return findPersistence.findAllNodesById(stringIds, metadata, entityClass);
    }

    public <T> long count(Class<T> entityClass) {
        NodeMetadata metadata = metadataExtractor.extractMetadata(entityClass);
        return findPersistence.countNodes(metadata);
    }
}
