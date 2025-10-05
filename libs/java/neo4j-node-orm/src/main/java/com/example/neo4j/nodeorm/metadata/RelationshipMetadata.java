package com.example.neo4j.nodeorm.metadata;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.lang.reflect.Field;

@Data
public class RelationshipMetadata {

    private Field field;
    private String fieldName;
    private String relationshipType;
    private Relationship.Direction direction;
    private Class<?> targetType;
    private boolean collection;
}
