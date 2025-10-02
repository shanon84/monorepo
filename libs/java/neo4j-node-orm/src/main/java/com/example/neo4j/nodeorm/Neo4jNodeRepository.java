package com.example.neo4j.nodeorm;

import java.util.List;

public interface Neo4jNodeRepository<T> {

  List<T> saveAll(List<T> entities);
}
