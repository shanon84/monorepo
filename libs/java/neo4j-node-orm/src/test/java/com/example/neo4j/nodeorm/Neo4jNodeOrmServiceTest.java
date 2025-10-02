package com.example.neo4j.nodeorm;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class Neo4jNodeOrmServiceTest {
  Neo4jNodeOrmService service = new Neo4jNodeOrmService();

  @Test
  void shouldReturnVersion() {
    assertThat(service.getVersion()).isEqualTo("1.0.0");
  }
}
