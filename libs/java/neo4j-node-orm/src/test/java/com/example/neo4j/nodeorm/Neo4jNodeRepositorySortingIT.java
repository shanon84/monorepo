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
class Neo4jNodeRepositorySortingIT {

    @Autowired
    private Neo4jClient neo4jClient;

    @Autowired
    private PersonNodeRepository personNodeRepository;

    @BeforeEach
    void setUp() {
        // Create test data with different ages and names for sorting
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

        PersonNode charlie = new PersonNode()
                .setFirstName("Charlie")
                .setLastName("Brown")
                .setAge(28)
                .setBirthDate(LocalDate.of(1996, 1, 10))
                .setActive(true);

        personNodeRepository.saveAll(List.of(john, jane, bob, alice, charlie));
    }

    @AfterEach
    void cleanUp() {
        neo4jClient.query("MATCH (n) DETACH DELETE n").run();
    }

    // ========== Simple Sorting ==========

    @Test
    void shouldFindAllOrderByAgeAsc() {
        List<PersonNode> result = personNodeRepository.findAllByOrderByAgeAsc();

        assertThat(result).hasSize(5);
        assertThat(result).extracting(PersonNode::getAge)
                .containsExactly(25, 28, 30, 35, 40);
        assertThat(result).extracting(PersonNode::getFirstName)
                .containsExactly("Jane", "Charlie", "John", "Alice", "Bob");
    }

    @Test
    void shouldFindAllOrderByAgeDesc() {
        List<PersonNode> result = personNodeRepository.findAllByOrderByAgeDesc();

        assertThat(result).hasSize(5);
        assertThat(result).extracting(PersonNode::getAge)
                .containsExactly(40, 35, 30, 28, 25);
        assertThat(result).extracting(PersonNode::getFirstName)
                .containsExactly("Bob", "Alice", "John", "Charlie", "Jane");
    }

    // ========== Multiple Field Sorting ==========

    @Test
    void shouldFindAllOrderByLastNameAscFirstNameAsc() {
        List<PersonNode> result = personNodeRepository.findAllByOrderByLastNameAscFirstNameAsc();

        assertThat(result).hasSize(5);
        assertThat(result).extracting(PersonNode::getLastName)
                .containsExactly("Brown", "Doe", "Johnson", "Smith", "Williams");
        assertThat(result).extracting(PersonNode::getFirstName)
                .containsExactly("Charlie", "John", "Bob", "Jane", "Alice");
    }

    @Test
    void shouldFindAllOrderByAgeDescFirstNameAsc() {
        // Test multiple people with same age
        PersonNode anotherJohn = new PersonNode()
                .setFirstName("Adam")
                .setLastName("Doe")
                .setAge(30)
                .setActive(true);
        personNodeRepository.save(anotherJohn);

        List<PersonNode> result = personNodeRepository.findAllByOrderByAgeDescFirstNameAsc();

        assertThat(result).hasSize(6);
        assertThat(result).extracting(PersonNode::getAge)
                .containsExactly(40, 35, 30, 30, 28, 25);

        // Check that persons with age 30 are sorted by firstName ASC
        assertThat(result.get(2).getAge()).isEqualTo(30);
        assertThat(result.get(3).getAge()).isEqualTo(30);
        assertThat(result.get(2).getFirstName()).isEqualTo("Adam");
        assertThat(result.get(3).getFirstName()).isEqualTo("John");
    }

    // ========== Sorting with Criteria ==========

    @Test
    void shouldFindByActiveTrueOrderByAgeDesc() {
        List<PersonNode> result = personNodeRepository.findByActiveTrueOrderByAgeDesc();

        assertThat(result).hasSize(4); // Bob is inactive
        assertThat(result).extracting(PersonNode::getAge)
                .containsExactly(35, 30, 28, 25);
        assertThat(result).extracting(PersonNode::getFirstName)
                .containsExactly("Alice", "John", "Charlie", "Jane");
        assertThat(result).allMatch(PersonNode::getActive);
    }

    @Test
    void shouldFindByLastNameOrderByFirstNameAsc() {
        // Add another Doe
        PersonNode anotherDoe = new PersonNode()
                .setFirstName("Amy")
                .setLastName("Doe")
                .setAge(27)
                .setActive(true);
        personNodeRepository.save(anotherDoe);

        List<PersonNode> result = personNodeRepository.findByLastNameOrderByFirstNameAsc("Doe");

        assertThat(result).hasSize(2);
        assertThat(result).extracting(PersonNode::getFirstName)
                .containsExactly("Amy", "John");
        assertThat(result).allMatch(p -> p.getLastName().equals("Doe"));
    }

    @Test
    void shouldFindByAgeGreaterThanOrderByAgeAscFirstNameDesc() {
        List<PersonNode> result = personNodeRepository.findByAgeGreaterThanOrderByAgeAscFirstNameDesc(25);

        assertThat(result).hasSize(4);
        assertThat(result).extracting(PersonNode::getAge)
                .containsExactly(28, 30, 35, 40);
        assertThat(result).extracting(PersonNode::getFirstName)
                .containsExactly("Charlie", "John", "Alice", "Bob");
    }
}
