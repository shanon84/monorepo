package com.example.neo4j.nodeorm.metadata;

import lombok.Data;

import java.lang.reflect.Field;

@Data
public class FieldMetadata {

    private Field field;
    private String fieldName;
    private Class<?> fieldType;
    private boolean generated;
}
