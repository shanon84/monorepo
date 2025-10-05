package com.example.neo4j.nodeorm.metadata;

import lombok.Data;

import java.lang.reflect.Field;

@Data
public class PropertyMetadata {

    private Field field;
    private String fieldName;
    private String propertyName;
    private Class<?> fieldType;
}
