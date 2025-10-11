package com.example.neo4j.nodeorm.query;

import lombok.Data;

@Data
public class QueryCriteria {
    private String propertyName;
    private QueryOperator operator;
    private String parameterName;
    private String parameterName2; // For operators like BETWEEN
    private String logicalOperator; // "AND" or "OR" - for chaining with previous criterion
}
