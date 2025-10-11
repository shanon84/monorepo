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
class Neo4jNodeRepositoryPaginationIT {

    @Autowired
    private Neo4jClient neo4jClient;

    @Autowired
    private PersonNodeRepository personNodeRepository;

    @BeforeEach
    void setUp() {
        // Create test data with different ages and names
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

        PersonNode david = new PersonNode()
                .setFirstName("David")
                .setLastName("Taylor")
                .setAge(33)
                .setBirthDate(LocalDate.of(1991, 9, 25))
                .setActive(true);

        personNodeRepository.saveAll(List.of(john, jane, bob, alice, charlie, david));
    }

    @AfterEach
    void cleanUp() {
        neo4jClient.query("MATCH (n) DETACH DELETE n").run();
    }

    // ========== Top Keyword ==========

    @Test
    void shouldFindTop3ByOrderByAgeDesc() {
        List<PersonNode> result = personNodeRepository.findTop3ByOrderByAgeDesc();

        assertThat(result).hasSize(3);
        assertThat(result).extracting(PersonNode::getAge)
                .containsExactly(40, 35, 33);
        assertThat(result).extracting(PersonNode::getFirstName)
                .containsExactly("Bob", "Alice", "David");
    }

    @Test
    void shouldFindTop5ByActiveTrueOrderByAgeAsc() {
        List<PersonNode> result = personNodeRepository.findTop5ByActiveTrueOrderByAgeAsc();

        // Only 5 active persons
        assertThat(result).hasSize(5);
        assertThat(result).extracting(PersonNode::getAge)
                .containsExactly(25, 28, 30, 33, 35);
        assertThat(result).allMatch(PersonNode::getActive);
    }

    @Test
    void shouldFindTop2ByLastNameOrderByFirstNameAsc() {
        // Add more Does
        personNodeRepository.saveAll(List.of(
                new PersonNode().setFirstName("Amy").setLastName("Doe").setAge(27).setActive(true),
                new PersonNode().setFirstName("Zoe").setLastName("Doe").setAge(29).setActive(true)
        ));

        List<PersonNode> result = personNodeRepository.findTop2ByLastNameOrderByFirstNameAsc("Doe");

        assertThat(result).hasSize(2);
        assertThat(result).extracting(PersonNode::getFirstName)
                .containsExactly("Amy", "John"); // Amy and John (alphabetically first 2)
        assertThat(result).allMatch(p -> p.getLastName().equals("Doe"));
    }

    // ========== First Keyword ==========

    @Test
    void shouldFindFirst1ByOrderByAgeAsc() {
        List<PersonNode> result = personNodeRepository.findFirst1ByOrderByAgeAsc();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFirstName()).isEqualTo("Jane");
        assertThat(result.get(0).getAge()).isEqualTo(25);
    }

    @Test
    void shouldFindFirstByOrderByLastNameAsc() {
        // First without number defaults to 1
        List<PersonNode> result = personNodeRepository.findFirstByOrderByLastNameAsc();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getLastName()).isEqualTo("Brown");
        assertThat(result.get(0).getFirstName()).isEqualTo("Charlie");
    }

    @Test
    void shouldFindFirst10ByAgeGreaterThanOrderByAgeDesc() {
        // Only 3 persons with age > 30 (Bob=40, Alice=35, David=33)
        List<PersonNode> result = personNodeRepository.findFirst10ByAgeGreaterThanOrderByAgeDesc(30);

        assertThat(result).hasSize(3); // Limit is 10, but only 3 match
        assertThat(result).extracting(PersonNode::getAge)
                .containsExactly(40, 35, 33);
        assertThat(result).allMatch(p -> p.getAge() > 30);
    }
}
