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

        // Add ORDER BY clause
        String orderByClause = buildOrderByClause(queryMethod, metadata);
        if (!orderByClause.isEmpty()) {
            cypher.append(" ORDER BY ").append(orderByClause);
        }

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

            // Add logical operator (AND/OR) if not the first criterion
            if (i > 0) {
                String logicalOp = criteria.getLogicalOperator();
                whereClause.append(" ").append(logicalOp != null ? logicalOp : "AND").append(" ");
            }

            // Build criterion clause
            String criterionClause = buildCriterionClause(criteria, metadata);
            whereClause.append(criterionClause);
        }

        return whereClause.toString();
    }

    private String buildCriterionClause(QueryCriteria criteria, NodeMetadata metadata) {
        // Find property metadata to get the actual property name in Neo4j
        String propertyName = getPropertyNameInNeo4j(criteria.getPropertyName(), metadata);
        StringBuilder clause = new StringBuilder();

        // Add operator
        switch (criteria.getOperator()) {
            case EQUALS -> {
                clause.append("n.").append(propertyName).append(" = $").append(criteria.getParameterName());
            }
            case NOT_EQUALS -> {
                clause.append("n.").append(propertyName).append(" <> $").append(criteria.getParameterName());
            }
            case GREATER_THAN -> {
                clause.append("n.").append(propertyName).append(" > $").append(criteria.getParameterName());
            }
            case GREATER_THAN_EQUAL -> {
                clause.append("n.").append(propertyName).append(" >= $").append(criteria.getParameterName());
            }
            case LESS_THAN -> {
                clause.append("n.").append(propertyName).append(" < $").append(criteria.getParameterName());
            }
            case LESS_THAN_EQUAL -> {
                clause.append("n.").append(propertyName).append(" <= $").append(criteria.getParameterName());
            }
            case BETWEEN -> {
                clause.append("n.").append(propertyName)
                        .append(" >= $").append(criteria.getParameterName())
                        .append(" AND n.").append(propertyName)
                        .append(" <= $").append(criteria.getParameterName2());
            }
            case LIKE -> {
                // For LIKE with wildcards
                clause.append("n.").append(propertyName).append(" =~ $").append(criteria.getParameterName());
            }
            case NOT_LIKE -> {
                clause.append("NOT n.").append(propertyName).append(" =~ $").append(criteria.getParameterName());
            }
            case STARTING_WITH -> {
                clause.append("n.").append(propertyName).append(" STARTS WITH $").append(criteria.getParameterName());
            }
            case ENDING_WITH -> {
                clause.append("n.").append(propertyName).append(" ENDS WITH $").append(criteria.getParameterName());
            }
            case CONTAINING -> {
                clause.append("n.").append(propertyName).append(" CONTAINS $").append(criteria.getParameterName());
            }
            case NOT_CONTAINING -> {
                clause.append("NOT n.").append(propertyName).append(" CONTAINS $").append(criteria.getParameterName());
            }
            case IN -> {
                clause.append("n.").append(propertyName).append(" IN $").append(criteria.getParameterName());
            }
            case NOT_IN -> {
                clause.append("NOT n.").append(propertyName).append(" IN $").append(criteria.getParameterName());
            }
            case IS_NULL -> {
                clause.append("n.").append(propertyName).append(" IS NULL");
            }
            case IS_NOT_NULL -> {
                clause.append("n.").append(propertyName).append(" IS NOT NULL");
            }
            case IS_TRUE -> {
                clause.append("n.").append(propertyName).append(" = true");
            }
            case IS_FALSE -> {
                clause.append("n.").append(propertyName).append(" = false");
            }
            case BEFORE -> {
                // For temporal comparisons (dates)
                clause.append("n.").append(propertyName).append(" < $").append(criteria.getParameterName());
            }
            case AFTER -> {
                // For temporal comparisons (dates)
                clause.append("n.").append(propertyName).append(" > $").append(criteria.getParameterName());
            }
            default -> throw new IllegalArgumentException("Unsupported operator: " + criteria.getOperator());
        }

        return clause.toString();
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

    /**
     * Build ORDER BY clause from sort orders.
     * Example: "n.age DESC, n.firstName ASC"
     */
    private String buildOrderByClause(QueryMethod queryMethod, NodeMetadata metadata) {
        if (queryMethod.getSortOrders() == null || queryMethod.getSortOrders().isEmpty()) {
            return "";
        }

        StringBuilder orderBy = new StringBuilder();
        boolean first = true;

        for (SortOrder sortOrder : queryMethod.getSortOrders()) {
            if (!first) {
                orderBy.append(", ");
            }
            first = false;

            String propertyName = getPropertyNameInNeo4j(sortOrder.getPropertyName(), metadata);
            orderBy.append("n.").append(propertyName);

            if (sortOrder.getDirection() == SortOrder.Direction.DESC) {
                orderBy.append(" DESC");
            } else {
                orderBy.append(" ASC");
            }
        }

        return orderBy.toString();
    }
}
