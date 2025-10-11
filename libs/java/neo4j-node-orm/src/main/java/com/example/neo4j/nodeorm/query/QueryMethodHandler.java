package com.example.neo4j.nodeorm.query;

import com.example.neo4j.nodeorm.metadata.NodeMetadata;
import com.example.neo4j.nodeorm.metadata.NodeMetadataExtractor;
import lombok.RequiredArgsConstructor;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class QueryMethodHandler {

    private final Neo4jClient neo4jClient;
    private final NodeMetadataExtractor metadataExtractor;
    private final QueryMethodParser queryMethodParser;
    private final CypherQueryGenerator cypherQueryGenerator;
    private final Neo4jNodeMapper nodeMapper;

    /**
     * Check if a method can be handled by this handler.
     * Returns true if method has @Query annotation or follows query derivation pattern.
     * <p>
     * IMPORTANT: This should NOT handle standard CrudRepository methods like:
     * - save, saveAll
     * - findById, findAll, findAllById
     * - existsById
     * - count
     * - deleteById, delete, deleteAll, deleteAllById
     */
    public boolean canHandle(Method method) {
        // Check for @Query annotation
        if (method.isAnnotationPresent(Query.class)) {
            return true;
        }

        // Check if method name follows query derivation pattern
        // BUT exclude standard CrudRepository methods
        String methodName = method.getName();

        // Exclude standard CrudRepository methods
        if (isStandardCrudMethod(methodName)) {
            return false;
        }

        // Handle custom query derivation methods
        return methodName.startsWith("findBy")
                || methodName.startsWith("findAllBy")
                || methodName.startsWith("findTop")
                || methodName.startsWith("findFirst")
                || methodName.startsWith("countBy")
                || methodName.startsWith("existsBy")
                || methodName.startsWith("deleteBy")
                || methodName.startsWith("deleteAllBy");
    }

    private boolean isStandardCrudMethod(String methodName) {
        return methodName.equals("save")
                || methodName.equals("saveAll")
                || methodName.equals("findById")
                || methodName.equals("findAll")
                || methodName.equals("findAllById")
                || methodName.equals("existsById")
                || methodName.equals("count")
                || methodName.equals("deleteById")
                || methodName.equals("delete")
                || methodName.equals("deleteAll")
                || methodName.equals("deleteAllById");
    }

    /**
     * Execute the query method and return the result.
     */
    @SuppressWarnings("unchecked")
    public <T> Object executeQuery(Method method, Object[] args, Class<T> entityClass) {
        String cypher;
        Map<String, Object> parameters = new HashMap<>();

        // Check if @Query annotation is present
        if (method.isAnnotationPresent(Query.class)) {
            Query queryAnnotation = method.getAnnotation(Query.class);
            cypher = queryAnnotation.value();

            // Bind parameters by position or name
            parameters = bindParameters(method, args);
        } else {
            // Derive query from method name
            NodeMetadata metadata = metadataExtractor.extractMetadata(entityClass);
            QueryMethod queryMethod = queryMethodParser.parse(method, metadata);
            cypher = cypherQueryGenerator.generate(queryMethod, metadata);
            parameters = bindParameters(method, args);
        }

        // Execute query based on return type
        return executeQueryAndMapResult(cypher, parameters, method, entityClass);
    }

    private Map<String, Object> bindParameters(Method method, Object[] args) {
        Map<String, Object> parameters = new HashMap<>();

        if (args == null || args.length == 0) {
            return parameters;
        }

        // Bind parameters by parameter name (requires -parameters compiler flag)
        // For now, use simple positional binding
        java.lang.reflect.Parameter[] methodParams = method.getParameters();
        for (int i = 0; i < args.length; i++) {
            String paramName = methodParams[i].getName();

            // Try to get @Param annotation
            org.springframework.data.repository.query.Param paramAnnotation =
                    methodParams[i].getAnnotation(org.springframework.data.repository.query.Param.class);

            if (paramAnnotation != null) {
                paramName = paramAnnotation.value();
            }

            parameters.put(paramName, args[i]);
        }

        return parameters;
    }

    @SuppressWarnings("unchecked")
    private <T> Object executeQueryAndMapResult(String cypher, Map<String, Object> parameters, Method method, Class<T> entityClass) {
        Class<?> returnType = method.getReturnType();

        // Build query
        Neo4jClient.UnboundRunnableSpec runnableSpec = neo4jClient.query(cypher);

        // Bind parameters
        Neo4jClient.RunnableSpec executableSpec = runnableSpec;
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            executableSpec = ((Neo4jClient.RunnableSpec) executableSpec).bind(entry.getValue()).to(entry.getKey());
        }

        // Determine return type and fetch accordingly
        if (returnType.equals(List.class) || Collection.class.isAssignableFrom(returnType)) {
            // Return List<Entity>
            return executableSpec.fetchAs(entityClass)
                    .mappedBy((typeSystem, record) ->
                            nodeMapper.mapNodePropertiesToEntity(record.get("n").asMap(), entityClass))
                    .all()
                    .stream()
                    .toList();
        } else if (returnType.equals(Long.class) || returnType.equals(long.class)) {
            // Return count
            return executableSpec.fetchAs(Long.class)
                    .mappedBy((typeSystem, record) ->
                            record.get("count").asLong())
                    .one()
                    .orElse(0L);
        } else if (returnType.equals(Boolean.class) || returnType.equals(boolean.class)) {
            // Return boolean (exists check)
            return executableSpec.fetchAs(Boolean.class)
                    .mappedBy((typeSystem, record) ->
                            record.get("exists").asBoolean())
                    .one()
                    .orElse(false);
        } else {
            // Return single entity
            return executableSpec.fetchAs(entityClass)
                    .mappedBy((typeSystem, record) ->
                            nodeMapper.mapNodePropertiesToEntity(record.get("n").asMap(), entityClass))
                    .one()
                    .orElse(null);
        }
    }
}
