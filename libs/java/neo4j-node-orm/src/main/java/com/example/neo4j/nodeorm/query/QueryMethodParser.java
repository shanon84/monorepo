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
            "(find|findAll|findTop|findFirst|count|exists|delete|deleteAll)(\\d+)?By(.+)"
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

        String operation = matcher.group(1); // find, findAll, findTop, findFirst, count, etc.
        String limitStr = matcher.group(2);  // Optional number for Top/First (e.g., "10", "5")
        String criteriaAndSort = matcher.group(3);  // FirstName, LastNameAndAge, etc.

        QueryMethod queryMethod = new QueryMethod();
        queryMethod.setMethodName(methodName);

        // Normalize operation (findTop -> find, findFirst -> find)
        String normalizedOperation = operation;
        if (operation.equals("findTop") || operation.equals("findFirst")) {
            normalizedOperation = "find";
            // Parse limit
            if (limitStr != null && !limitStr.isEmpty()) {
                queryMethod.setLimit(Integer.parseInt(limitStr));
            } else {
                // findTop/findFirst without number defaults to 1
                queryMethod.setLimit(1);
            }
        }

        queryMethod.setOperation(normalizedOperation);
        queryMethod.setEntityClass(metadata.getNodeClass());
        queryMethod.setReturnType(method.getReturnType());

        // Split criteria and sorting (e.g., "LastNameOrderByAgeDesc" -> criteria="LastName", sort="AgeDesc")
        String criteria;
        String sortPart = null;
        int orderByIndex = criteriaAndSort.indexOf("OrderBy");
        if (orderByIndex >= 0) {
            criteria = criteriaAndSort.substring(0, orderByIndex);
            sortPart = criteriaAndSort.substring(orderByIndex + 7); // Skip "OrderBy"
        } else {
            criteria = criteriaAndSort;
        }

        // Parse criteria (e.g., "FirstNameAndAge" -> ["FirstName", "Age"])
        if (!criteria.isEmpty()) {
            List<QueryCriteria> criteriaList = parseCriteria(criteria, method);
            queryMethod.setCriteria(criteriaList);
        } else {
            // No criteria - empty list instead of null
            queryMethod.setCriteria(new ArrayList<>());
        }

        // Parse sorting (e.g., "AgeDescFirstNameAsc" -> [SortOrder("age", DESC), SortOrder("firstName", ASC)])
        if (sortPart != null && !sortPart.isEmpty()) {
            List<SortOrder> sortOrders = parseSortOrders(sortPart);
            queryMethod.setSortOrders(sortOrders);
        }

        return queryMethod;
    }

    private List<QueryCriteria> parseCriteria(String criteriaString, Method method) {
        List<QueryCriteria> criteriaList = new ArrayList<>();

        // Split by "And" and "Or"
        List<String> parts = splitByLogicalOperators(criteriaString);
        List<String> operators = extractLogicalOperators(criteriaString);

        java.lang.reflect.Parameter[] parameters = method.getParameters();
        int paramIndex = 0;

        for (int i = 0; i < parts.size(); i++) {
            String part = parts.get(i);
            QueryCriteria criteria = new QueryCriteria();

            // Extract operator and property name from part
            OperatorParseResult result = extractOperatorAndProperty(part);
            criteria.setPropertyName(result.propertyName);
            criteria.setOperator(result.operator);

            // Set logical operator (AND/OR) for chaining
            if (i > 0 && i - 1 < operators.size()) {
                criteria.setLogicalOperator(operators.get(i - 1));
            }

            // Get parameter name(s) - some operators need multiple params (e.g., Between)
            if (result.operator == QueryOperator.BETWEEN) {
                if (paramIndex < parameters.length) {
                    criteria.setParameterName(parameters[paramIndex].getName());
                    paramIndex++;
                }
                if (paramIndex < parameters.length) {
                    criteria.setParameterName2(parameters[paramIndex].getName());
                    paramIndex++;
                }
            } else if (result.operator == QueryOperator.IS_NULL || result.operator == QueryOperator.IS_NOT_NULL
                    || result.operator == QueryOperator.IS_TRUE || result.operator == QueryOperator.IS_FALSE) {
                // No parameter needed
            } else {
                if (paramIndex < parameters.length) {
                    criteria.setParameterName(parameters[paramIndex].getName());
                    paramIndex++;
                }
            }

            criteriaList.add(criteria);
        }

        return criteriaList;
    }

    private List<String> splitByLogicalOperators(String criteriaString) {
        // Split by "And" and "Or" but preserve order
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        int i = 0;
        while (i < criteriaString.length()) {
            if (criteriaString.startsWith("And", i)) {
                parts.add(current.toString());
                current = new StringBuilder();
                i += 3;
            } else if (criteriaString.startsWith("Or", i)) {
                parts.add(current.toString());
                current = new StringBuilder();
                i += 2;
            } else {
                current.append(criteriaString.charAt(i));
                i++;
            }
        }
        if (current.length() > 0) {
            parts.add(current.toString());
        }

        return parts;
    }

    private List<String> extractLogicalOperators(String criteriaString) {
        List<String> operators = new ArrayList<>();
        int i = 0;
        while (i < criteriaString.length()) {
            if (criteriaString.startsWith("And", i)) {
                operators.add("AND");
                i += 3;
            } else if (criteriaString.startsWith("Or", i)) {
                operators.add("OR");
                i += 2;
            } else {
                i++;
            }
        }
        return operators;
    }

    private OperatorParseResult extractOperatorAndProperty(String part) {
        OperatorParseResult result = new OperatorParseResult();

        // Check for operators (order matters - check longer keywords first!)
        if (part.endsWith("IsNotNull")) {
            result.propertyName = toCamelCase(part.substring(0, part.length() - 9));
            result.operator = QueryOperator.IS_NOT_NULL;
        } else if (part.endsWith("IsNull")) {
            result.propertyName = toCamelCase(part.substring(0, part.length() - 6));
            result.operator = QueryOperator.IS_NULL;
        } else if (part.endsWith("NotContaining")) {
            result.propertyName = toCamelCase(part.substring(0, part.length() - 13));
            result.operator = QueryOperator.NOT_CONTAINING;
        } else if (part.endsWith("Containing")) {
            result.propertyName = toCamelCase(part.substring(0, part.length() - 10));
            result.operator = QueryOperator.CONTAINING;
        } else if (part.endsWith("StartingWith")) {
            result.propertyName = toCamelCase(part.substring(0, part.length() - 12));
            result.operator = QueryOperator.STARTING_WITH;
        } else if (part.endsWith("EndingWith")) {
            result.propertyName = toCamelCase(part.substring(0, part.length() - 10));
            result.operator = QueryOperator.ENDING_WITH;
        } else if (part.endsWith("GreaterThanEqual")) {
            result.propertyName = toCamelCase(part.substring(0, part.length() - 16));
            result.operator = QueryOperator.GREATER_THAN_EQUAL;
        } else if (part.endsWith("GreaterThan")) {
            result.propertyName = toCamelCase(part.substring(0, part.length() - 11));
            result.operator = QueryOperator.GREATER_THAN;
        } else if (part.endsWith("LessThanEqual")) {
            result.propertyName = toCamelCase(part.substring(0, part.length() - 13));
            result.operator = QueryOperator.LESS_THAN_EQUAL;
        } else if (part.endsWith("LessThan")) {
            result.propertyName = toCamelCase(part.substring(0, part.length() - 8));
            result.operator = QueryOperator.LESS_THAN;
        } else if (part.endsWith("Between")) {
            result.propertyName = toCamelCase(part.substring(0, part.length() - 7));
            result.operator = QueryOperator.BETWEEN;
        } else if (part.endsWith("NotLike")) {
            result.propertyName = toCamelCase(part.substring(0, part.length() - 7));
            result.operator = QueryOperator.NOT_LIKE;
        } else if (part.endsWith("Like")) {
            result.propertyName = toCamelCase(part.substring(0, part.length() - 4));
            result.operator = QueryOperator.LIKE;
        } else if (part.endsWith("NotIn")) {
            result.propertyName = toCamelCase(part.substring(0, part.length() - 5));
            result.operator = QueryOperator.NOT_IN;
        } else if (part.endsWith("In")) {
            result.propertyName = toCamelCase(part.substring(0, part.length() - 2));
            result.operator = QueryOperator.IN;
        } else if (part.endsWith("Not")) {
            result.propertyName = toCamelCase(part.substring(0, part.length() - 3));
            result.operator = QueryOperator.NOT_EQUALS;
        } else if (part.endsWith("IsTrue") || part.endsWith("True")) {
            int suffixLength = part.endsWith("IsTrue") ? 6 : 4;
            result.propertyName = toCamelCase(part.substring(0, part.length() - suffixLength));
            result.operator = QueryOperator.IS_TRUE;
        } else if (part.endsWith("IsFalse") || part.endsWith("False")) {
            int suffixLength = part.endsWith("IsFalse") ? 7 : 5;
            result.propertyName = toCamelCase(part.substring(0, part.length() - suffixLength));
            result.operator = QueryOperator.IS_FALSE;
        } else if (part.endsWith("Before")) {
            result.propertyName = toCamelCase(part.substring(0, part.length() - 6));
            result.operator = QueryOperator.BEFORE;
        } else if (part.endsWith("After")) {
            result.propertyName = toCamelCase(part.substring(0, part.length() - 5));
            result.operator = QueryOperator.AFTER;
        } else {
            // Default: EQUALS
            result.propertyName = toCamelCase(part);
            result.operator = QueryOperator.EQUALS;
        }

        return result;
    }

    private static class OperatorParseResult {
        String propertyName;
        QueryOperator operator;
    }

    private List<SortOrder> parseSortOrders(String sortPart) {
        List<SortOrder> sortOrders = new ArrayList<>();

        // Parse sort orders like "AgeDescFirstNameAsc" or "LastNameAsc"
        // We need to split by Asc/Desc keywords
        int i = 0;
        StringBuilder currentProperty = new StringBuilder();

        while (i < sortPart.length()) {
            if (sortPart.startsWith("Desc", i)) {
                // Found DESC - add sort order
                String propertyName = toCamelCase(currentProperty.toString());
                sortOrders.add(new SortOrder(propertyName, SortOrder.Direction.DESC));
                currentProperty = new StringBuilder();
                i += 4; // Skip "Desc"
            } else if (sortPart.startsWith("Asc", i)) {
                // Found ASC - add sort order
                String propertyName = toCamelCase(currentProperty.toString());
                sortOrders.add(new SortOrder(propertyName, SortOrder.Direction.ASC));
                currentProperty = new StringBuilder();
                i += 3; // Skip "Asc"
            } else {
                currentProperty.append(sortPart.charAt(i));
                i++;
            }
        }

        // If there's remaining property without Asc/Desc, default to ASC
        if (currentProperty.length() > 0) {
            String propertyName = toCamelCase(currentProperty.toString());
            sortOrders.add(new SortOrder(propertyName, SortOrder.Direction.ASC));
        }

        return sortOrders;
    }

    private String toCamelCase(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        return Character.toLowerCase(input.charAt(0)) + input.substring(1);
    }
}
