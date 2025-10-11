package com.example.neo4j.nodeorm.query;

import com.example.neo4j.nodeorm.metadata.NodeMetadata;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class QueryMethodParser {

    private static final Pattern METHOD_PATTERN = Pattern.compile(
            "(find|findAll|count|exists|delete|deleteAll)By(.+)"
    );

    /**
     * Parse a query method and extract query information.
     */
    public QueryMethod parse(Method method, NodeMetadata metadata) {
        String methodName = method.getName();
        Matcher matcher = METHOD_PATTERN.matcher(methodName);

        if (!matcher.matches()) {
            throw new IllegalArgumentException("Cannot parse method name: " + methodName);
        }

        String operation = matcher.group(1); // find, findAll, count, etc.
        String criteria = matcher.group(2);  // FirstName, LastNameAndAge, etc.

        QueryMethod queryMethod = new QueryMethod();
        queryMethod.setMethodName(methodName);
        queryMethod.setOperation(operation);
        queryMethod.setEntityClass(metadata.getNodeClass());
        queryMethod.setReturnType(method.getReturnType());

        // Parse criteria (e.g., "FirstNameAndAge" -> ["FirstName", "Age"])
        List<QueryCriteria> criteriaList = parseCriteria(criteria, method);
        queryMethod.setCriteria(criteriaList);

        return queryMethod;
    }

    private List<QueryCriteria> parseCriteria(String criteriaString, Method method) {
        List<QueryCriteria> criteriaList = new ArrayList<>();

        // Split by "And" and "Or" (for now only support And)
        String[] parts = criteriaString.split("And");

        java.lang.reflect.Parameter[] parameters = method.getParameters();
        int paramIndex = 0;

        for (String part : parts) {
            QueryCriteria criteria = new QueryCriteria();

            // Convert camelCase to property name (e.g., "LastName" -> "lastName")
            String propertyName = toCamelCase(part);
            criteria.setPropertyName(propertyName);
            criteria.setOperator(QueryOperator.EQUALS); // Default operator

            // Get parameter name
            if (paramIndex < parameters.length) {
                criteria.setParameterName(parameters[paramIndex].getName());
                paramIndex++;
            }

            criteriaList.add(criteria);
        }

        return criteriaList;
    }

    private String toCamelCase(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        return Character.toLowerCase(input.charAt(0)) + input.substring(1);
    }
}
