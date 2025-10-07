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
class Neo4jNodeRepositoryFindIT {

    @Autowired
    private Neo4jClient neo4jClient;

    @Autowired
    private Neo4jNodeRepository repository;

    @AfterEach
    void cleanUp() {
        neo4jClient.query("MATCH (n) DETACH DELETE n").run();
    }

    @Test
    void shouldFindNodeById() {
        // Given
        SimpleNode node = new SimpleNode()
                .setName("Test Node");
        SimpleNode savedNode = (SimpleNode) repository.save(node);

        // When
        SimpleNode foundNode = (SimpleNode) repository.findById(savedNode.getId()).orElseThrow();

        // Then
        assertThat(foundNode).isNotNull();
        assertThat(foundNode.getId()).isEqualTo(savedNode.getId());
        assertThat(foundNode.getName()).isEqualTo("Test Node");
    }

    @Test
    void shouldReturnEmptyWhenNodeNotFound() {
        // Given
        SimpleNode node = new SimpleNode().setName("Test");
        repository.save(node);

        // When
        var result = repository.findById("non-existent-id");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldCheckIfNodeExists() {
        // Given
        SimpleNode node = new SimpleNode().setName("Test");
        SimpleNode savedNode = (SimpleNode) repository.save(node);

        // When / Then
        assertThat(repository.existsById(savedNode.getId())).isTrue();
        assertThat(repository.existsById("non-existent-id")).isFalse();
    }

    @Test
    void shouldFindAllNodes() {
        // Given
        SimpleNode node1 = new SimpleNode().setName("Node 1");
        SimpleNode node2 = new SimpleNode().setName("Node 2");
        SimpleNode node3 = new SimpleNode().setName("Node 3");
        repository.saveAll(List.of(node1, node2, node3));

        // When
        List<SimpleNode> allNodes = StreamSupport.stream(repository.findAll().spliterator(), false)
                .map(n -> (SimpleNode) n)
                .toList();

        // Then
        assertThat(allNodes).hasSize(3);
        assertThat(allNodes).extracting(SimpleNode::getName)
                .containsExactlyInAnyOrder("Node 1", "Node 2", "Node 3");
    }

    @Test
    void shouldFindAllNodesById() {
        // Given
        SimpleNode node1 = new SimpleNode().setName("Node 1");
        SimpleNode node2 = new SimpleNode().setName("Node 2");
        SimpleNode node3 = new SimpleNode().setName("Node 3");
        List<SimpleNode> savedNodes = StreamSupport.stream(repository.saveAll(List.of(node1, node2, node3)).spliterator(), false)
                .map(n -> (SimpleNode) n)
                .toList();

        List<String> idsToFind = List.of(savedNodes.get(0).getId(), savedNodes.get(2).getId());

        // When
        List<SimpleNode> foundNodes = StreamSupport.stream(repository.findAllById(idsToFind).spliterator(), false)
                .map(n -> (SimpleNode) n)
                .toList();

        // Then
        assertThat(foundNodes).hasSize(2);
        assertThat(foundNodes).extracting(SimpleNode::getName)
                .containsExactlyInAnyOrder("Node 1", "Node 3");
    }

    @Test
    void shouldCountNodes() {
        // Given
        SimpleNode node1 = new SimpleNode().setName("Node 1");
        SimpleNode node2 = new SimpleNode().setName("Node 2");
        repository.saveAll(List.of(node1, node2));

        // When
        long count = repository.count();

        // Then
        assertThat(count).isEqualTo(2);
    }
}
