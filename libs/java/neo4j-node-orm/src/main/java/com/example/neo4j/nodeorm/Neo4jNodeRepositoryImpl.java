package com.example.neo4j.nodeorm;

import com.example.neo4j.nodeorm.delete.Neo4jDeleteService;
import com.example.neo4j.nodeorm.find.Neo4jFindService;
import com.example.neo4j.nodeorm.save.Neo4jSaveService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
public class Neo4jNodeRepositoryImpl<T> implements Neo4jNodeRepository<T> {

    private final Neo4jSaveService saveService;
    private final Neo4jFindService findService;
    private final Neo4jDeleteService deleteService;

    private Class<T> entityClass;

    @Override
    public <S extends T> S save(S entity) {
        if (entityClass == null) {
            entityClass = (Class<T>) entity.getClass();
        }
        List<S> entities = List.of(entity);
        return saveService.saveAll(entities).get(0);
    }

    @Override
    public <S extends T> Iterable<S> saveAll(Iterable<S> entities) {
        List<S> entityList = StreamSupport.stream(entities.spliterator(), false)
                .collect(Collectors.toList());
        if (entityClass == null && !entityList.isEmpty()) {
            entityClass = (Class<T>) entityList.get(0).getClass();
        }
        return saveService.saveAll(entityList);
    }

    @Override
    public Optional<T> findById(String id) {
        ensureEntityClassSet();
        return findService.findById(id, entityClass);
    }

    @Override
    public boolean existsById(String id) {
        ensureEntityClassSet();
        return findService.existsById(id, entityClass);
    }

    @Override
    public Iterable<T> findAll() {
        ensureEntityClassSet();
        return findService.findAll(entityClass);
    }

    @Override
    public Iterable<T> findAllById(Iterable<String> ids) {
        ensureEntityClassSet();
        return findService.findAllById(ids, entityClass);
    }

    @Override
    public long count() {
        ensureEntityClassSet();
        return findService.count(entityClass);
    }

    @Override
    public void deleteById(String id) {
        ensureEntityClassSet();
        deleteService.deleteById(id, entityClass);
    }

    @Override
    public void delete(T entity) {
        deleteService.delete(entity);
    }

    @Override
    public void deleteAllById(Iterable<? extends String> ids) {
        ensureEntityClassSet();
        deleteService.deleteAllById((Iterable<String>) ids, entityClass);
    }

    @Override
    public void deleteAll(Iterable<? extends T> entities) {
        deleteService.deleteAll((Iterable<T>) entities);
    }

    @Override
    public void deleteAll() {
        ensureEntityClassSet();
        deleteService.deleteAll(entityClass);
    }

    private void ensureEntityClassSet() {
        if (entityClass == null) {
            throw new IllegalStateException("Entity class not set. Call save() or saveAll() first to initialize the repository.");
        }
    }
}
