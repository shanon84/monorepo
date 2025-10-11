package com.example.neo4j.nodeorm.query;

import lombok.Data;

@Data
public class QueryCriteria {
    private String propertyName;
    private QueryOperator operator;
    private String parameterName;
}
