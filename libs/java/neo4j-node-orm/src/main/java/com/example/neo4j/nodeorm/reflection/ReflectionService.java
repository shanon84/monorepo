package com.example.neo4j.nodeorm.reflection;

import org.springframework.stereotype.Service;

import java.lang.reflect.Field;

@Service
public class ReflectionService {

    public Object getFieldValue(Field field, Object object) {
        try {
            return field.get(object);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to access field: " + field.getName(), e);
        }
    }

    public void setFieldValue(Field field, Object object, Object value) {
        try {
            field.set(object, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to set field: " + field.getName(), e);
        }
    }

    public Object convertIdToFieldType(Object generatedId, Class<?> fieldType) {
        if (generatedId == null) {
            return null;
        }

        // If types match, return as-is
        if (fieldType.isInstance(generatedId)) {
            return generatedId;
        }

        // Convert UUID to String
        if (generatedId instanceof java.util.UUID && fieldType == String.class) {
            return generatedId.toString();
        }

        // Convert UUID to Long (hash code)
        if (generatedId instanceof java.util.UUID && (fieldType == Long.class || fieldType == long.class)) {
            return (long) generatedId.hashCode();
        }

        // Convert String UUID to Long (hash code)
        if (generatedId instanceof String && fieldType == Long.class) {
            return (long) generatedId.hashCode();
        }

        // Try to convert to Long
        if (fieldType == Long.class || fieldType == long.class) {
            if (generatedId instanceof Number number) {
                return number.longValue();
            }
        }

        // Return as-is and let reflection handle the conversion/error
        return generatedId;
    }

    public Object instantiateClass(Class<?> clazz) {
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate class: " + clazz.getName(), e);
        }
    }

    public Object invokeMethod(Object target, String methodName, Class<?>[] parameterTypes, Object... args) {
        try {
            return target.getClass().getMethod(methodName, parameterTypes).invoke(target, args);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke method " + methodName + " on " + target.getClass().getName(), e);
        }
    }
}
