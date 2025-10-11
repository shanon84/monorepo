package com.example.neo4j.nodeorm.testdata;

import com.example.neo4j.nodeorm.Neo4jNodeRepository;
import org.springframework.data.neo4j.repository.query.Query;

import java.time.LocalDate;
import java.util.List;

public interface PersonNodeRepository extends Neo4jNodeRepository<PersonNode, String> {

    // Basic EQUALS (already tested)
    List<PersonNode> findAllByLastName(String name);

    // @Query Annotation
    @Query("""
            MATCH (n:Person) WHERE
            n.age > $age
            RETURN n
            """)
    List<PersonNode> findAllByAgeGreateThan(Integer age);

    // Comparison Operators
    List<PersonNode> findByAgeGreaterThan(Integer age);

    List<PersonNode> findByAgeGreaterThanEqual(Integer age);

    List<PersonNode> findByAgeLessThan(Integer age);

    List<PersonNode> findByAgeLessThanEqual(Integer age);

    List<PersonNode> findByAgeBetween(Integer minAge, Integer maxAge);

    List<PersonNode> findByBirthDateBefore(LocalDate date);

    List<PersonNode> findByBirthDateAfter(LocalDate date);

    // String Operators
    List<PersonNode> findByFirstNameLike(String pattern);

    List<PersonNode> findByFirstNameContaining(String substring);

    List<PersonNode> findByFirstNameStartingWith(String prefix);

    List<PersonNode> findByFirstNameEndingWith(String suffix);

    List<PersonNode> findByFirstNameNotContaining(String substring);

    // Null Checks
    List<PersonNode> findByBirthDateIsNull();

    List<PersonNode> findByBirthDateIsNotNull();

    // Boolean
    List<PersonNode> findByActiveTrue();

    List<PersonNode> findByActiveFalse();

    // Collection Operators
    List<PersonNode> findByIdIn(List<String> ids);

    List<PersonNode> findByIdNotIn(List<String> ids);

    // Logical Operators - AND (already works)
    List<PersonNode> findByFirstNameAndLastName(String firstName, String lastName);

    // Logical Operators - OR
    List<PersonNode> findByFirstNameOrLastName(String firstName, String lastName);

    // Complex combinations
    List<PersonNode> findByAgeGreaterThanAndLastName(Integer age, String lastName);

    List<PersonNode> findByAgeGreaterThanOrLastName(Integer age, String lastName);

    // NOT operator
    List<PersonNode> findByAgeNot(Integer age);

    // Count operations
    Long countByAgeGreaterThan(Integer age);

    Long countByFirstNameOrLastName(String firstName, String lastName);

    // Exists operations
    Boolean existsByAgeGreaterThan(Integer age);

    Boolean existsByFirstNameAndLastName(String firstName, String lastName);

    // Delete operations
    void deleteByAgeGreaterThan(Integer age);

    void deleteByFirstNameOrLastName(String firstName, String lastName);
}
