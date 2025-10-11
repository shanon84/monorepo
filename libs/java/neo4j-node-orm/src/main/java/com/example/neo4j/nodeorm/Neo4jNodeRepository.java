package com.example.neo4j.nodeorm;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface Neo4jNodeRepository<T, ID> extends CrudRepository<T, ID> {

}
