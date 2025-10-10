package com.example.neo4j.nodeorm.metadata;

import lombok.Data;

import java.lang.reflect.Field;

@Data
public class AuditFieldMetadata {

    private Field createdByField;
    private Field createdDateField;
    private Field lastModifiedByField;
    private Field lastModifiedDateField;

    public boolean hasCreatedBy() {
        return createdByField != null;
    }

    public boolean hasCreatedDate() {
        return createdDateField != null;
    }

    public boolean hasLastModifiedBy() {
        return lastModifiedByField != null;
    }

    public boolean hasLastModifiedDate() {
        return lastModifiedDateField != null;
    }

    public boolean hasAnyAuditFields() {
        return hasCreatedBy() || hasCreatedDate() || hasLastModifiedBy() || hasLastModifiedDate();
    }
}
