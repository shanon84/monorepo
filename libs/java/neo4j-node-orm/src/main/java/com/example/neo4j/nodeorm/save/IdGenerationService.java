package com.example.neo4j.nodeorm.save;

import com.example.neo4j.nodeorm.metadata.NodeMetadata;
import com.example.neo4j.nodeorm.reflection.ReflectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class IdGenerationService {

    private final ReflectionService reflectionService;
    private final Map<Class<?>, Object> generatorCache = new HashMap<>();

    public Object generateIdValue(Object node, NodeMetadata metadata) {
        Field idField = metadata.getIdField().getField();
        GeneratedValue generatedValue = idField.getAnnotation(GeneratedValue.class);

        // Try value() first (e.g. @GeneratedValue(UUIDGenerator.class)), then generatorClass()
        Class<?> generatorClass = generatedValue.value();

        // If value is not set, use generatorClass (deprecated but still supported)
        if (generatorClass == null || generatorClass.getName().contains("InternalIdGenerator")) {
            generatorClass = generatedValue.generatorClass();
        }

        Object generator = generatorCache.computeIfAbsent(generatorClass, reflectionService::instantiateClass);
        Object generatedId = reflectionService.invokeMethod(
                generator,
                "generateId",
                new Class<?>[]{String.class, Object.class},
                metadata.getNodeName(),
                node
        );

        if (generatedId == null) {
            throw new RuntimeException("ID generator " + generatorClass.getName() +
                    " returned null for entity " + node.getClass().getName());
        }

        return reflectionService.convertIdToFieldType(generatedId, idField.getType());
    }
}
