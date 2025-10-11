package com.example.neo4j.nodeorm;

import com.example.global.annotations.IT;
import com.example.neo4j.nodeorm.testdata.PersonNode;
import com.example.neo4j.nodeorm.testdata.PersonNodeRepository;
import com.example.neo4j.nodeorm.testdata.TestRepositoryConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.neo4j.core.Neo4jClient;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@IT
@Import(TestRepositoryConfiguration.class)
class Neo4jNodeRepositoryExtendedQueryIT {

    @Autowired
    private Neo4jClient neo4jClient;

    @Autowired
    private PersonNodeRepository personNodeRepository;

    @BeforeEach
    void setUp() {
        // Create test data
        PersonNode john = new PersonNode()
                .setFirstName("John")
                .setLastName("Doe")
                .setAge(30)
                .setBirthDate(LocalDate.of(1994, 5, 15))
                .setActive(true);

        PersonNode jane = new PersonNode()
                .setFirstName("Jane")
                .setLastName("Smith")
                .setAge(25)
                .setBirthDate(LocalDate.of(1999, 3, 20))
                .setActive(true);

        PersonNode bob = new PersonNode()
                .setFirstName("Bob")
                .setLastName("Johnson")
                .setAge(40)
                .setBirthDate(LocalDate.of(1984, 11, 8))
                .setActive(false);

        PersonNode alice = new PersonNode()
                .setFirstName("Alice")
                .setLastName("Williams")
                .setAge(35)
                .setBirthDate(LocalDate.of(1989, 7, 2))
                .setActive(true);

        personNodeRepository.saveAll(List.of(john, jane, bob, alice));
    }

    @AfterEach
    void cleanUp() {
        neo4jClient.query("MATCH (n) DETACH DELETE n").run();
    }

    // ========== Comparison Operators ==========

    @Test
    void shouldFindByGreaterThan() {
        List<PersonNode> result = personNodeRepository.findByAgeGreaterThan(30);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(PersonNode::getFirstName)
                .containsExactlyInAnyOrder("Bob", "Alice");
    }

    @Test
    void shouldFindByGreaterThanEqual() {
        List<PersonNode> result = personNodeRepository.findByAgeGreaterThanEqual(30);

        assertThat(result).hasSize(3);
        assertThat(result).extracting(PersonNode::getFirstName)
                .containsExactlyInAnyOrder("John", "Bob", "Alice");
    }

    @Test
    void shouldFindByLessThan() {
        List<PersonNode> result = personNodeRepository.findByAgeLessThan(30);

        assertThat(result).hasSize(1);
        assertThat(result).extracting(PersonNode::getFirstName)
                .containsExactly("Jane");
    }

    @Test
    void shouldFindByLessThanEqual() {
        List<PersonNode> result = personNodeRepository.findByAgeLessThanEqual(30);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(PersonNode::getFirstName)
                .containsExactlyInAnyOrder("John", "Jane");
    }

    @Test
    void shouldFindByBetween() {
        List<PersonNode> result = personNodeRepository.findByAgeBetween(25, 35);

        assertThat(result).hasSize(3);
        assertThat(result).extracting(PersonNode::getFirstName)
                .containsExactlyInAnyOrder("John", "Jane", "Alice");
    }

    @Test
    void shouldFindByBefore() {
        LocalDate cutoffDate = LocalDate.of(1990, 1, 1);
        List<PersonNode> result = personNodeRepository.findByBirthDateBefore(cutoffDate);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(PersonNode::getFirstName)
                .containsExactlyInAnyOrder("Bob", "Alice");
    }

    @Test
    void shouldFindByAfter() {
        LocalDate cutoffDate = LocalDate.of(1995, 1, 1);
        List<PersonNode> result = personNodeRepository.findByBirthDateAfter(cutoffDate);

        assertThat(result).hasSize(1);
        assertThat(result).extracting(PersonNode::getFirstName)
                .containsExactly("Jane");
    }

    // ========== String Operators ==========

    @Test
    void shouldFindByLike() {
        // Regex pattern for names containing 'o'
        List<PersonNode> result = personNodeRepository.findByFirstNameLike(".*o.*");

        assertThat(result).hasSize(2);
        assertThat(result).extracting(PersonNode::getFirstName)
                .containsExactlyInAnyOrder("John", "Bob");
    }

    @Test
    void shouldFindByContaining() {
        List<PersonNode> result = personNodeRepository.findByFirstNameContaining("li");

        assertThat(result).hasSize(1);
        assertThat(result).extracting(PersonNode::getFirstName)
                .containsExactly("Alice");
    }

    @Test
    void shouldFindByStartingWith() {
        List<PersonNode> result = personNodeRepository.findByFirstNameStartingWith("J");

        assertThat(result).hasSize(2);
        assertThat(result).extracting(PersonNode::getFirstName)
                .containsExactlyInAnyOrder("John", "Jane");
    }

    @Test
    void shouldFindByEndingWith() {
        List<PersonNode> result = personNodeRepository.findByFirstNameEndingWith("e");

        assertThat(result).hasSize(2);
        assertThat(result).extracting(PersonNode::getFirstName)
                .containsExactlyInAnyOrder("Jane", "Alice");
    }

    @Test
    void shouldFindByNotContaining() {
        List<PersonNode> result = personNodeRepository.findByFirstNameNotContaining("o");

        assertThat(result).hasSize(2);
        assertThat(result).extracting(PersonNode::getFirstName)
                .containsExactlyInAnyOrder("Jane", "Alice");
    }

    // ========== Null Checks ==========

    @Test
    void shouldFindByIsNull() {
        // Create persons with and without birthDate
        PersonNode withoutBirthDate = new PersonNode()
                .setFirstName("NoDate")
                .setLastName("Person")
                .setAge(25)
                .setActive(true);
        personNodeRepository.save(withoutBirthDate);

        List<PersonNode> result = personNodeRepository.findByBirthDateIsNull();

        assertThat(result).hasSize(1);
        assertThat(result).extracting(PersonNode::getFirstName)
                .containsExactly("NoDate");
    }

    @Test
    void shouldFindByIsNotNull() {
        // All persons from setUp() have birthDate
        List<PersonNode> result = personNodeRepository.findByBirthDateIsNotNull();

        assertThat(result).hasSize(4);
        assertThat(result).extracting(PersonNode::getFirstName)
                .containsExactlyInAnyOrder("John", "Jane", "Bob", "Alice");
    }

    // ========== Boolean Operators ==========

    @Test
    void shouldFindByTrue() {
        List<PersonNode> result = personNodeRepository.findByActiveTrue();

        assertThat(result).hasSize(3);
        assertThat(result).extracting(PersonNode::getFirstName)
                .containsExactlyInAnyOrder("John", "Jane", "Alice");
    }

    @Test
    void shouldFindByFalse() {
        List<PersonNode> result = personNodeRepository.findByActiveFalse();

        assertThat(result).hasSize(1);
        assertThat(result).extracting(PersonNode::getFirstName)
                .containsExactly("Bob");
    }

    // ========== Collection Operators ==========

    @Test
    void shouldFindByIn() {
        List<PersonNode> allPersons = personNodeRepository.findAll();
        List<String> selectedIds = List.of(
                allPersons.get(0).getId(),
                allPersons.get(1).getId()
        );

        List<PersonNode> result = personNodeRepository.findByIdIn(selectedIds);

        assertThat(result).hasSize(2);
    }

    @Test
    void shouldFindByNotIn() {
        List<PersonNode> allPersons = personNodeRepository.findAll();
        List<String> excludedIds = List.of(
                allPersons.get(0).getId(),
                allPersons.get(1).getId()
        );

        List<PersonNode> result = personNodeRepository.findByIdNotIn(excludedIds);

        assertThat(result).hasSize(2);
    }

    // ========== Logical Operators ==========

    @Test
    void shouldFindByOr() {
        List<PersonNode> result = personNodeRepository.findByFirstNameOrLastName("John", "Smith");

        assertThat(result).hasSize(2);
        assertThat(result).extracting(PersonNode::getFirstName)
                .containsExactlyInAnyOrder("John", "Jane");
    }

    @Test
    void shouldFindByComplexAndCondition() {
        List<PersonNode> result = personNodeRepository.findByAgeGreaterThanAndLastName(25, "Doe");

        assertThat(result).hasSize(1);
        assertThat(result).extracting(PersonNode::getFirstName)
                .containsExactly("John");
    }

    @Test
    void shouldFindByComplexOrCondition() {
        List<PersonNode> result = personNodeRepository.findByAgeGreaterThanOrLastName(35, "Doe");

        assertThat(result).hasSize(2);
        assertThat(result).extracting(PersonNode::getFirstName)
                .containsExactlyInAnyOrder("John", "Bob");
    }

    @Test
    void shouldFindByNot() {
        List<PersonNode> result = personNodeRepository.findByAgeNot(30);

        assertThat(result).hasSize(3);
        assertThat(result).extracting(PersonNode::getFirstName)
                .containsExactlyInAnyOrder("Jane", "Bob", "Alice");
    }

    // ========== Count Operations ==========

    @Test
    void shouldCountByGreaterThan() {
        Long count = personNodeRepository.countByAgeGreaterThan(30);

        assertThat(count).isEqualTo(2);
    }

    @Test
    void shouldCountByOr() {
        Long count = personNodeRepository.countByFirstNameOrLastName("John", "Smith");

        assertThat(count).isEqualTo(2);
    }

    // ========== Exists Operations ==========

    @Test
    void shouldExistsByGreaterThan() {
        Boolean exists = personNodeRepository.existsByAgeGreaterThan(30);

        assertThat(exists).isTrue();
    }

    @Test
    void shouldNotExistsByGreaterThan() {
        Boolean exists = personNodeRepository.existsByAgeGreaterThan(100);

        assertThat(exists).isFalse();
    }

    @Test
    void shouldExistsByAnd() {
        Boolean exists = personNodeRepository.existsByFirstNameAndLastName("John", "Doe");

        assertThat(exists).isTrue();
    }

    // ========== Delete Operations ==========

    @Test
    void shouldDeleteByGreaterThan() {
        personNodeRepository.deleteByAgeGreaterThan(35);

        List<PersonNode> remaining = personNodeRepository.findAll();
        assertThat(remaining).hasSize(3);
        assertThat(remaining).noneMatch(p -> p.getAge() > 35);
    }

    @Test
    void shouldDeleteByOr() {
        personNodeRepository.deleteByFirstNameOrLastName("John", "Smith");

        List<PersonNode> remaining = personNodeRepository.findAll();
        assertThat(remaining).hasSize(2);
        assertThat(remaining).extracting(PersonNode::getFirstName)
                .containsExactlyInAnyOrder("Bob", "Alice");
    }
}
