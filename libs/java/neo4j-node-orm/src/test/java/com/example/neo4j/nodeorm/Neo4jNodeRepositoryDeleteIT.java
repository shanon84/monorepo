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
class Neo4jNodeRepositoryDeleteIT {

    @Autowired
    private Neo4jClient neo4jClient;

    @Autowired
    private SimpleNodeRepository simpleNodeRepository;

    @AfterEach
    void cleanUp() {
        neo4jClient.query("MATCH (n) DETACH DELETE n").run();
    }

    @Test
    void shouldDeleteNodeById() {
        // Given
        SimpleNode node = new SimpleNode().setName("To Delete");
        SimpleNode savedNode = simpleNodeRepository.save(node);
        String nodeId = savedNode.getId();

        // When
        simpleNodeRepository.deleteById(nodeId);

        // Then
        assertThat(simpleNodeRepository.existsById(nodeId)).isFalse();
    }

    @Test
    void shouldDeleteNode() {
        // Given
        SimpleNode node = new SimpleNode().setName("To Delete");
        SimpleNode savedNode = simpleNodeRepository.save(node);

        // When
        simpleNodeRepository.delete(savedNode);

        // Then
        assertThat(simpleNodeRepository.existsById(savedNode.getId())).isFalse();
    }

    @Test
    void shouldDeleteAllById() {
        // Given
        SimpleNode node1 = new SimpleNode().setName("Node 1");
        SimpleNode node2 = new SimpleNode().setName("Node 2");
        SimpleNode node3 = new SimpleNode().setName("Node 3");
        List<SimpleNode> savedNodes = StreamSupport.stream(simpleNodeRepository.saveAll(List.of(node1, node2, node3)).spliterator(), false)
                .toList();

        List<String> idsToDelete = List.of(savedNodes.get(0).getId(), savedNodes.get(1).getId());

        // When
        simpleNodeRepository.deleteAllById(idsToDelete);

        // Then
        assertThat(simpleNodeRepository.count()).isEqualTo(1);
        assertThat(simpleNodeRepository.existsById(savedNodes.get(2).getId())).isTrue();
    }

    @Test
    void shouldDeleteAllByEntity() {
        // Given
        SimpleNode node1 = new SimpleNode().setName("Node 1");
        SimpleNode node2 = new SimpleNode().setName("Node 2");
        List<SimpleNode> savedNodes = StreamSupport.stream(simpleNodeRepository.saveAll(List.of(node1, node2)).spliterator(), false)
                .toList();

        // When
        simpleNodeRepository.deleteAll(savedNodes);

        // Then
        assertThat(simpleNodeRepository.count()).isEqualTo(0);
    }

    @Test
    void shouldDeleteAllNodes() {
        // Given
        SimpleNode node1 = new SimpleNode().setName("Node 1");
        SimpleNode node2 = new SimpleNode().setName("Node 2");
        simpleNodeRepository.saveAll(List.of(node1, node2));

        // When
        simpleNodeRepository.deleteAll();

        // Then
        assertThat(simpleNodeRepository.count()).isEqualTo(0);
    }
}
