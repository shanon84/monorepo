package com.example.neo4j.nodeorm.query;

public enum QueryOperator {
    EQUALS,
    NOT_EQUALS,
    GREATER_THAN,
    GREATER_THAN_EQUAL,
    LESS_THAN,
    LESS_THAN_EQUAL,
    BETWEEN,
    LIKE,
    NOT_LIKE,
    STARTING_WITH,
    ENDING_WITH,
    CONTAINING,
    NOT_CONTAINING,
    IN,
    NOT_IN,
    IS_NULL,
    IS_NOT_NULL,
    IS_TRUE,
    IS_FALSE,
    BEFORE,  // For date/time comparisons
    AFTER    // For date/time comparisons
}
