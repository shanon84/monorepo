package com.example.neo4j.nodeorm.query;

import lombok.Data;

@Data
public class SortOrder {
    private String propertyName;
    private Direction direction;

    public enum Direction {
        ASC,
        DESC
    }

    public SortOrder(String propertyName, Direction direction) {
        this.propertyName = propertyName;
        this.direction = direction;
    }
}
