package com.example.neo4j.nodeorm;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;

@NoRepositoryBean
public interface Neo4jNodeRepository<T, ID> extends CrudRepository<T, ID> {
    @Override
    <S extends T> List<S> saveAll(Iterable<S> entities);

    @Override
    List<T> findAll();


    @Override
    List<T> findAllById(Iterable<ID> iterable);
}
