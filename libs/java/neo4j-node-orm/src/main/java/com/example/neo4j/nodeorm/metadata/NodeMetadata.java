package com.example.neo4j.nodeorm.metadata;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class NodeMetadata {

    private Class<?> nodeClass;
    private String nodeName;
    private FieldMetadata idField;
    private FieldMetadata versionField;
    private List<PropertyMetadata> properties = new ArrayList<>();
    private List<RelationshipMetadata> relationships = new ArrayList<>();
    private AuditFieldMetadata auditFields = new AuditFieldMetadata();
}
