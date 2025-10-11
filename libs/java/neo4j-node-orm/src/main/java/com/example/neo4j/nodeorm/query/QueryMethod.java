package com.example.neo4j.nodeorm.query;

import lombok.Data;

import java.util.List;

@Data
public class QueryMethod {
    private String methodName;
    private String operation; // find, findAll, count, exists, delete, deleteAll
    private Class<?> entityClass;
    private Class<?> returnType;
    private List<QueryCriteria> criteria;
}
