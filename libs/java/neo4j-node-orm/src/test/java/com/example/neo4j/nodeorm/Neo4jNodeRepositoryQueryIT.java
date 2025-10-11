package com.example.neo4j.nodeorm;

import com.example.global.annotations.IT;
import com.example.neo4j.nodeorm.testdata.PersonNode;
import com.example.neo4j.nodeorm.testdata.PersonNodeRepository;
import com.example.neo4j.nodeorm.testdata.TestRepositoryConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.neo4j.core.Neo4jClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@IT
@Import(TestRepositoryConfiguration.class)
class Neo4jNodeRepositoryQueryIT {

    @Autowired
    private Neo4jClient neo4jClient;

    @Autowired
    private PersonNodeRepository personNodeRepository;

    @AfterEach
    void cleanUp() {
        neo4jClient.query("MATCH (n) DETACH DELETE n").run();
    }

    @Test
    void shouldSupportQueryDerivationFindAllBy() {
        // Given - Create some test persons
        PersonNode person1 = new PersonNode()
                .setFirstName("John")
                .setLastName("Doe")
                .setAge(30);

        PersonNode person2 = new PersonNode()
                .setFirstName("Jane")
                .setLastName("Doe")
                .setAge(25);

        PersonNode person3 = new PersonNode()
                .setFirstName("Bob")
                .setLastName("Smith")
                .setAge(40);

        personNodeRepository.saveAll(List.of(person1, person2, person3));

        // When - Find all persons with lastName "Doe"
        List<PersonNode> doesPersons = personNodeRepository.findAllByLastName("Doe");

        // Then
        assertThat(doesPersons).hasSize(2);
        assertThat(doesPersons).allMatch(p -> p.getLastName().equals("Doe"));
        assertThat(doesPersons).extracting(PersonNode::getFirstName)
                .containsExactlyInAnyOrder("John", "Jane");
    }

    @Test
    void shouldSupportQueryAnnotation() {
        // Given - Create some test persons
        PersonNode person1 = new PersonNode()
                .setFirstName("John")
                .setLastName("Doe")
                .setAge(30);

        PersonNode person2 = new PersonNode()
                .setFirstName("Jane")
                .setLastName("Smith")
                .setAge(25);

        PersonNode person3 = new PersonNode()
                .setFirstName("Bob")
                .setLastName("Johnson")
                .setAge(40);

        personNodeRepository.saveAll(List.of(person1, person2, person3));

        // When - Find all persons with age > 28
        List<PersonNode> olderPersons = personNodeRepository.findAllByAgeGreateThan(28);

        // Then
        assertThat(olderPersons).hasSize(2);
        assertThat(olderPersons).allMatch(p -> p.getAge() > 28);
        assertThat(olderPersons).extracting(PersonNode::getFirstName)
                .containsExactlyInAnyOrder("John", "Bob");
    }

    @Test
    void shouldSupportQueryDerivationWithEmptyResult() {
        // Given - Create some test persons
        PersonNode person1 = new PersonNode()
                .setFirstName("John")
                .setLastName("Doe")
                .setAge(30);

        personNodeRepository.save(person1);

        // When - Find all persons with lastName "Smith" (doesn't exist)
        List<PersonNode> smithPersons = personNodeRepository.findAllByLastName("Smith");

        // Then
        assertThat(smithPersons).isEmpty();
    }

    @Test
    void shouldSupportQueryAnnotationWithEmptyResult() {
        // Given - Create some test persons
        PersonNode person1 = new PersonNode()
                .setFirstName("John")
                .setLastName("Doe")
                .setAge(20);

        personNodeRepository.save(person1);

        // When - Find all persons with age > 50 (none exist)
        List<PersonNode> olderPersons = personNodeRepository.findAllByAgeGreateThan(50);

        // Then
        assertThat(olderPersons).isEmpty();
    }
}
