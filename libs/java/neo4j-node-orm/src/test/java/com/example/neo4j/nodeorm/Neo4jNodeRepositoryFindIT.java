package com.example.neo4j.nodeorm;

import com.example.global.annotations.IT;
import com.example.neo4j.nodeorm.testdata.SimpleNode;
import com.example.neo4j.nodeorm.testdata.SimpleNodeRepository;
import com.example.neo4j.nodeorm.testdata.TestRepositoryConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.neo4j.core.Neo4jClient;

import java.util.List;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;

@IT
@Import(TestRepositoryConfiguration.class)
class Neo4jNodeRepositoryFindIT {

    @Autowired
    private Neo4jClient neo4jClient;

    @Autowired
    private SimpleNodeRepository simpleNodeRepository;

    @AfterEach
    void cleanUp() {
        neo4jClient.query("MATCH (n) DETACH DELETE n").run();
    }

    @Test
    void shouldFindNodeById() {
        // Given
        SimpleNode node = new SimpleNode()
                .setName("Test Node");
        SimpleNode savedNode = simpleNodeRepository.save(node);

        // When
        SimpleNode foundNode = simpleNodeRepository.findById(savedNode.getId()).orElseThrow();

        // Then
        assertThat(foundNode).isNotNull();
        assertThat(foundNode.getId()).isEqualTo(savedNode.getId());
        assertThat(foundNode.getName()).isEqualTo("Test Node");
    }

    @Test
    void shouldReturnEmptyWhenNodeNotFound() {
        // Given
        SimpleNode node = new SimpleNode().setName("Test");
        simpleNodeRepository.save(node);

        // When
        var result = simpleNodeRepository.findById("non-existent-id");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldCheckIfNodeExists() {
        // Given
        SimpleNode node = new SimpleNode().setName("Test");
        SimpleNode savedNode = simpleNodeRepository.save(node);

        // When / Then
        assertThat(simpleNodeRepository.existsById(savedNode.getId())).isTrue();
        assertThat(simpleNodeRepository.existsById("non-existent-id")).isFalse();
    }

    @Test
    void shouldFindAllNodes() {
        // Given
        SimpleNode node1 = new SimpleNode().setName("Node 1");
        SimpleNode node2 = new SimpleNode().setName("Node 2");
        SimpleNode node3 = new SimpleNode().setName("Node 3");
        simpleNodeRepository.saveAll(List.of(node1, node2, node3));

        // When
        List<SimpleNode> allNodes = StreamSupport.stream(simpleNodeRepository.findAll().spliterator(), false)
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
        List<SimpleNode> savedNodes = StreamSupport.stream(simpleNodeRepository.saveAll(List.of(node1, node2, node3)).spliterator(), false)
                .toList();

        List<String> idsToFind = List.of(savedNodes.get(0).getId(), savedNodes.get(2).getId());

        // When
        List<SimpleNode> foundNodes = StreamSupport.stream(simpleNodeRepository.findAllById(idsToFind).spliterator(), false)
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
        simpleNodeRepository.saveAll(List.of(node1, node2));

        // When
        long count = simpleNodeRepository.count();

        // Then
        assertThat(count).isEqualTo(2);
    }
}
