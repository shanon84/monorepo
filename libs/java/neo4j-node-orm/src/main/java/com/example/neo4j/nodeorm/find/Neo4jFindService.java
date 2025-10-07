package com.example.neo4j.nodeorm.find;

import com.example.neo4j.nodeorm.metadata.NodeMetadata;
import com.example.neo4j.nodeorm.metadata.NodeMetadataExtractor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class Neo4jFindService {

    private final Neo4jFindPersistence findPersistence;
    private final NodeMetadataExtractor metadataExtractor;

    public <T> Optional<T> findById(String id, Class<T> entityClass) {
        NodeMetadata metadata = metadataExtractor.extractMetadata(entityClass);
        return findPersistence.findNodeById(id, metadata, entityClass);
    }

    public <T> boolean existsById(String id, Class<T> entityClass) {
        NodeMetadata metadata = metadataExtractor.extractMetadata(entityClass);
        return findPersistence.existsNodeById(id, metadata);
    }

    public <T> List<T> findAll(Class<T> entityClass) {
        NodeMetadata metadata = metadataExtractor.extractMetadata(entityClass);
        return findPersistence.findAllNodes(metadata, entityClass);
    }

    public <T> List<T> findAllById(Iterable<String> ids, Class<T> entityClass) {
        NodeMetadata metadata = metadataExtractor.extractMetadata(entityClass);
        return findPersistence.findAllNodesById(ids, metadata, entityClass);
    }

    public <T> long count(Class<T> entityClass) {
        NodeMetadata metadata = metadataExtractor.extractMetadata(entityClass);
        return findPersistence.countNodes(metadata);
    }
}
