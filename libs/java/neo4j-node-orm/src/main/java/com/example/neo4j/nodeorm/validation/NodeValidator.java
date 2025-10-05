package com.example.neo4j.nodeorm.validation;

import com.example.global.annotations.Validator;
import org.springframework.data.neo4j.core.schema.Node;

import java.util.List;

@Validator
public class NodeValidator {

    public <T> void validateNodes(List<T> entities) {
        if (entities == null || entities.isEmpty()) {
            throw new IllegalArgumentException("Entity list cannot be null or empty");
        }

        Class<?> entityClass = entities.get(0).getClass();
        validateNodeAnnotation(entityClass);

        // Check all entities are of same type
        for (T entity : entities) {
            if (!entity.getClass().equals(entityClass)) {
                throw new IllegalArgumentException(
                        "All entities must be of the same type. Expected: " + entityClass.getName() +
                                ", but found: " + entity.getClass().getName()
                );
            }
        }
    }

    public void validateNodeAnnotation(Class<?> entityClass) {
        if (!entityClass.isAnnotationPresent(Node.class)) {
            throw new IllegalArgumentException(
                    "Class " + entityClass.getName() + " must be annotated with @Node"
            );
        }
    }
}
