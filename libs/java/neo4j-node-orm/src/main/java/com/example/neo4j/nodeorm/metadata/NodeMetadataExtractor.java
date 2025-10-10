package com.example.neo4j.nodeorm.metadata;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class NodeMetadataExtractor {

    private final Map<Class<?>, NodeMetadata> metadataCache = new ConcurrentHashMap<>();

    public NodeMetadata extractMetadata(Class<?> nodeClass) {
        return metadataCache.computeIfAbsent(nodeClass, this::doExtractMetadata);
    }

    private NodeMetadata doExtractMetadata(Class<?> nodeClass) {
        NodeMetadata metadata = new NodeMetadata();
        metadata.setNodeClass(nodeClass);
        metadata.setNodeName(extractNodeName(nodeClass));

        Field[] fields = nodeClass.getDeclaredFields();

        for (Field field : fields) {
            field.setAccessible(true);

            if (field.isAnnotationPresent(Id.class)) {
                metadata.setIdField(extractIdField(field));
            } else if (field.isAnnotationPresent(Relationship.class)) {
                metadata.getRelationships().add(extractRelationship(field));
            } else if (isAuditField(field)) {
                extractAuditField(field, metadata.getAuditFields());
            } else {
                metadata.getProperties().add(extractProperty(field));
            }
        }

        if (metadata.getIdField() == null) {
            throw new IllegalStateException(
                    "Node class " + nodeClass.getName() + " must have a field annotated with @Id"
            );
        }

        return metadata;
    }

    private boolean isAuditField(Field field) {
        return field.isAnnotationPresent(CreatedBy.class)
                || field.isAnnotationPresent(CreatedDate.class)
                || field.isAnnotationPresent(LastModifiedBy.class)
                || field.isAnnotationPresent(LastModifiedDate.class);
    }

    private void extractAuditField(Field field, AuditFieldMetadata auditFields) {
        if (field.isAnnotationPresent(CreatedBy.class)) {
            auditFields.setCreatedByField(field);
        } else if (field.isAnnotationPresent(CreatedDate.class)) {
            auditFields.setCreatedDateField(field);
        } else if (field.isAnnotationPresent(LastModifiedBy.class)) {
            auditFields.setLastModifiedByField(field);
        } else if (field.isAnnotationPresent(LastModifiedDate.class)) {
            auditFields.setLastModifiedDateField(field);
        }
    }

    private String extractNodeName(Class<?> nodeClass) {
        Node nodeAnnotation = nodeClass.getAnnotation(Node.class);
        String[] labels = nodeAnnotation.labels();

        if (labels.length > 0 && !labels[0].isEmpty()) {
            return labels[0];
        }

        String[] value = nodeAnnotation.value();
        if (value.length > 0 && !value[0].isEmpty()) {
            return value[0];
        }

        return nodeClass.getSimpleName();
    }

    private FieldMetadata extractIdField(Field field) {
        FieldMetadata metadata = new FieldMetadata();
        metadata.setField(field);
        metadata.setFieldName(field.getName());
        metadata.setFieldType(field.getType());
        metadata.setGenerated(field.isAnnotationPresent(GeneratedValue.class));
        return metadata;
    }

    private PropertyMetadata extractProperty(Field field) {
        PropertyMetadata metadata = new PropertyMetadata();
        metadata.setField(field);
        metadata.setFieldName(field.getName());
        metadata.setFieldType(field.getType());

        if (field.isAnnotationPresent(Property.class)) {
            Property propertyAnnotation = field.getAnnotation(Property.class);
            String propertyName = propertyAnnotation.name();
            if (propertyName.isEmpty()) {
                propertyName = propertyAnnotation.value();
            }
            metadata.setPropertyName(propertyName.isEmpty() ? field.getName() : propertyName);
        } else {
            metadata.setPropertyName(field.getName());
        }

        return metadata;
    }

    private RelationshipMetadata extractRelationship(Field field) {
        RelationshipMetadata metadata = new RelationshipMetadata();
        metadata.setField(field);
        metadata.setFieldName(field.getName());

        Relationship relationshipAnnotation = field.getAnnotation(Relationship.class);
        metadata.setRelationshipType(relationshipAnnotation.type());
        metadata.setDirection(relationshipAnnotation.direction());

        Class<?> fieldType = field.getType();
        boolean isCollection = Collection.class.isAssignableFrom(fieldType);
        metadata.setCollection(isCollection);

        if (isCollection) {
            ParameterizedType parameterizedType = (ParameterizedType) field.getGenericType();
            Class<?> targetType = (Class<?>) parameterizedType.getActualTypeArguments()[0];
            metadata.setTargetType(targetType);
        } else {
            metadata.setTargetType(fieldType);
        }

        return metadata;
    }
}
