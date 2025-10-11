package com.example.neo4j.nodeorm.query;

import com.example.neo4j.nodeorm.metadata.NodeMetadata;
import com.example.neo4j.nodeorm.metadata.PropertyMetadata;
import org.springframework.stereotype.Component;

@Component
public class CypherQueryGenerator {

    /**
     * Generate Cypher query from QueryMethod.
     */
    public String generate(QueryMethod queryMethod, NodeMetadata metadata) {
        String operation = queryMethod.getOperation();
        String nodeName = metadata.getNodeName();

        return switch (operation) {
            case "find", "findAll" -> generateFindQuery(queryMethod, metadata);
            case "count" -> generateCountQuery(queryMethod, metadata);
            case "exists" -> generateExistsQuery(queryMethod, metadata);
            case "delete", "deleteAll" -> generateDeleteQuery(queryMethod, metadata);
            default -> throw new IllegalArgumentException("Unsupported operation: " + operation);
        };
    }

    private String generateFindQuery(QueryMethod queryMethod, NodeMetadata metadata) {
        StringBuilder cypher = new StringBuilder();
        String nodeName = metadata.getNodeName();

        cypher.append("MATCH (n:").append(nodeName).append(")");

        // Add WHERE clause
        String whereClause = buildWhereClause(queryMethod, metadata);
        if (!whereClause.isEmpty()) {
            cypher.append(" WHERE ").append(whereClause);
        }

        cypher.append(" RETURN n");

        return cypher.toString();
    }

    private String generateCountQuery(QueryMethod queryMethod, NodeMetadata metadata) {
        StringBuilder cypher = new StringBuilder();
        String nodeName = metadata.getNodeName();

        cypher.append("MATCH (n:").append(nodeName).append(")");

        // Add WHERE clause
        String whereClause = buildWhereClause(queryMethod, metadata);
        if (!whereClause.isEmpty()) {
            cypher.append(" WHERE ").append(whereClause);
        }

        cypher.append(" RETURN count(n) AS count");

        return cypher.toString();
    }

    private String generateExistsQuery(QueryMethod queryMethod, NodeMetadata metadata) {
        StringBuilder cypher = new StringBuilder();
        String nodeName = metadata.getNodeName();

        cypher.append("MATCH (n:").append(nodeName).append(")");

        // Add WHERE clause
        String whereClause = buildWhereClause(queryMethod, metadata);
        if (!whereClause.isEmpty()) {
            cypher.append(" WHERE ").append(whereClause);
        }

        cypher.append(" RETURN count(n) > 0 AS exists");

        return cypher.toString();
    }

    private String generateDeleteQuery(QueryMethod queryMethod, NodeMetadata metadata) {
        StringBuilder cypher = new StringBuilder();
        String nodeName = metadata.getNodeName();

        cypher.append("MATCH (n:").append(nodeName).append(")");

        // Add WHERE clause
        String whereClause = buildWhereClause(queryMethod, metadata);
        if (!whereClause.isEmpty()) {
            cypher.append(" WHERE ").append(whereClause);
        }

        cypher.append(" DETACH DELETE n");

        return cypher.toString();
    }

    private String buildWhereClause(QueryMethod queryMethod, NodeMetadata metadata) {
        StringBuilder whereClause = new StringBuilder();

        for (int i = 0; i < queryMethod.getCriteria().size(); i++) {
            QueryCriteria criteria = queryMethod.getCriteria().get(i);

            if (i > 0) {
                whereClause.append(" AND ");
            }

            // Find property metadata to get the actual property name in Neo4j
            String propertyName = getPropertyNameInNeo4j(criteria.getPropertyName(), metadata);

            whereClause.append("n.").append(propertyName);

            // Add operator
            switch (criteria.getOperator()) {
                case EQUALS -> whereClause.append(" = $").append(criteria.getParameterName());
                case NOT_EQUALS -> whereClause.append(" <> $").append(criteria.getParameterName());
                case GREATER_THAN -> whereClause.append(" > $").append(criteria.getParameterName());
                case GREATER_THAN_EQUAL -> whereClause.append(" >= $").append(criteria.getParameterName());
                case LESS_THAN -> whereClause.append(" < $").append(criteria.getParameterName());
                case LESS_THAN_EQUAL -> whereClause.append(" <= $").append(criteria.getParameterName());
                case LIKE -> whereClause.append(" CONTAINS $").append(criteria.getParameterName());
                case IS_NULL -> whereClause.append(" IS NULL");
                case IS_NOT_NULL -> whereClause.append(" IS NOT NULL");
                default -> throw new IllegalArgumentException("Unsupported operator: " + criteria.getOperator());
            }
        }

        return whereClause.toString();
    }

    /**
     * Get the property name as stored in Neo4j.
     * Checks if property has @Property annotation with custom name.
     */
    private String getPropertyNameInNeo4j(String javaPropertyName, NodeMetadata metadata) {
        // Check in properties
        for (PropertyMetadata property : metadata.getProperties()) {
            if (property.getFieldName().equals(javaPropertyName)) {
                return property.getPropertyName();
            }
        }

        // If not found, assume field name equals property name
        return javaPropertyName;
    }
}
