package com.example.neo4j.nodeorm;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class Neo4jNodeOrmService {

  public String getVersion() {
    return "1.0.0";
  }
}
