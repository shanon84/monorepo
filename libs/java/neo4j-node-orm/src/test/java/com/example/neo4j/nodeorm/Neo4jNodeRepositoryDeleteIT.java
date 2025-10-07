package com.example.neo4j.nodeorm;

import com.example.global.annotations.IT;
import com.example.neo4j.nodeorm.testdata.SimpleNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.core.Neo4jClient;

import java.util.List;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;

@IT
class Neo4jNodeRepositoryDeleteIT {

    @Autowired
    private Neo4jClient neo4jClient;

    @Autowired
    private Neo4jNodeRepository repository;

    @AfterEach
    void cleanUp() {
        neo4jClient.query("MATCH (n) DETACH DELETE n").run();
    }

    @Test
    void shouldDeleteNodeById() {
        // Given
        SimpleNode node = new SimpleNode().setName("To Delete");
        SimpleNode savedNode = (SimpleNode) repository.save(node);
        String nodeId = savedNode.getId();

        // When
        repository.deleteById(nodeId);

        // Then
        assertThat(repository.existsById(nodeId)).isFalse();
    }

    @Test
    void shouldDeleteNode() {
        // Given
        SimpleNode node = new SimpleNode().setName("To Delete");
        SimpleNode savedNode = (SimpleNode) repository.save(node);

        // When
        repository.delete(savedNode);

        // Then
        assertThat(repository.existsById(savedNode.getId())).isFalse();
    }

    @Test
    void shouldDeleteAllById() {
        // Given
        SimpleNode node1 = new SimpleNode().setName("Node 1");
        SimpleNode node2 = new SimpleNode().setName("Node 2");
        SimpleNode node3 = new SimpleNode().setName("Node 3");
        List<SimpleNode> savedNodes = StreamSupport.stream(repository.saveAll(List.of(node1, node2, node3)).spliterator(), false)
                .map(n -> (SimpleNode) n)
                .toList();

        List<String> idsToDelete = List.of(savedNodes.get(0).getId(), savedNodes.get(1).getId());

        // When
        repository.deleteAllById(idsToDelete);

        // Then
        assertThat(repository.count()).isEqualTo(1);
        assertThat(repository.existsById(savedNodes.get(2).getId())).isTrue();
    }

    @Test
    void shouldDeleteAllByEntity() {
        // Given
        SimpleNode node1 = new SimpleNode().setName("Node 1");
        SimpleNode node2 = new SimpleNode().setName("Node 2");
        List<SimpleNode> savedNodes = StreamSupport.stream(repository.saveAll(List.of(node1, node2)).spliterator(), false)
                .map(n -> (SimpleNode) n)
                .toList();

        // When
        repository.deleteAll(savedNodes);

        // Then
        assertThat(repository.count()).isEqualTo(0);
    }

    @Test
    void shouldDeleteAllNodes() {
        // Given
        SimpleNode node1 = new SimpleNode().setName("Node 1");
        SimpleNode node2 = new SimpleNode().setName("Node 2");
        repository.saveAll(List.of(node1, node2));

        // When
        repository.deleteAll();

        // Then
        assertThat(repository.count()).isEqualTo(0);
    }
}
