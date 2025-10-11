package com.example.neo4j.nodeorm;

import com.example.global.annotations.IT;
import com.example.neo4j.nodeorm.audit.AuditProvider;
import com.example.neo4j.nodeorm.testdata.PersonNode;
import com.example.neo4j.nodeorm.testdata.PersonNodeRepository;
import com.example.neo4j.nodeorm.testdata.TestRepositoryConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.neo4j.core.Neo4jClient;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@IT
@Import(TestRepositoryConfiguration.class)
class Neo4jNodeRepositoryAuditIT {

    @Autowired
    private Neo4jClient neo4jClient;

    @Autowired
    private PersonNodeRepository personNodeRepository;

    @Autowired
    private AuditProvider auditProvider;

    @AfterEach
    void cleanUp() {
        neo4jClient.query("MATCH (n) DETACH DELETE n").run();
    }

    @Test
    void shouldPopulateAuditFieldsWhenSavingNode() {
        // Given
        String expectedUser = auditProvider.getCurrentUser();
        assertThat(expectedUser).isNotNull();
        LocalDateTime beforeSave = auditProvider.getCurrentTimestamp();
        assertThat(beforeSave).isNotNull();

        PersonNode person = new PersonNode()
                .setFirstName("John")
                .setLastName("Doe")
                .setAge(30);

        // Verify audit fields are null before save
        assertThat(person.getCreatedBy()).isNull();
        assertThat(person.getCreatedDate()).isNull();
        assertThat(person.getLastModifiedBy()).isNull();
        assertThat(person.getLastModifiedDate()).isNull();

        List<PersonNode> persons = List.of(person);

        // When
        List<PersonNode> savedPersons = StreamSupport.stream(personNodeRepository.saveAll(persons).spliterator(), false)
                .toList();

        LocalDateTime afterSave = LocalDateTime.now();

        // Then
        PersonNode savedPerson = savedPersons.get(0);

        // Verify createdBy
        assertThat(savedPerson.getCreatedBy()).isNotNull();
        assertThat(savedPerson.getCreatedBy()).isEqualTo(expectedUser);

        // Verify createdDate
        assertThat(savedPerson.getCreatedDate()).isNotNull();
        assertThat(savedPerson.getCreatedDate()).isAfterOrEqualTo(beforeSave);
        assertThat(savedPerson.getCreatedDate()).isBeforeOrEqualTo(afterSave);

        // Verify lastModifiedBy
        assertThat(savedPerson.getLastModifiedBy()).isNotNull();
        assertThat(savedPerson.getLastModifiedBy()).isEqualTo(expectedUser);

        // Verify lastModifiedDate
        assertThat(savedPerson.getLastModifiedDate()).isNotNull();
        assertThat(savedPerson.getLastModifiedDate()).isAfterOrEqualTo(beforeSave);
        assertThat(savedPerson.getLastModifiedDate()).isBeforeOrEqualTo(afterSave);

        // Verify original object also has audit fields populated
        assertThat(person.getCreatedBy()).isEqualTo(expectedUser);
        assertThat(person.getCreatedDate()).isNotNull();
        assertThat(person.getLastModifiedBy()).isEqualTo(expectedUser);
        assertThat(person.getLastModifiedDate()).isNotNull();
    }

    @Test
    void shouldPersistAuditFieldsToDatabase() {
        // Given
        PersonNode person = new PersonNode()
                .setFirstName("Jane")
                .setLastName("Smith")
                .setAge(25);

        List<PersonNode> persons = List.of(person);

        // When
        List<PersonNode> savedPersons = StreamSupport.stream(personNodeRepository.saveAll(persons).spliterator(), false)
                .toList();

        String personId = savedPersons.get(0).getId();

        // Retrieve from database using direct Cypher query
        var foundPerson = neo4jClient.query(
                        "MATCH (p:Person) WHERE p.id = $id " +
                                "RETURN p.id AS id, p.firstName AS firstName, p.lastName AS lastName, p.age AS age, " +
                                "p.createdBy AS createdBy, p.createdDate AS createdDate, " +
                                "p.lastModifiedBy AS lastModifiedBy, p.lastModifiedDate AS lastModifiedDate"
                )
                .bind(personId).to("id")
                .fetchAs(PersonNode.class)
                .mappedBy((typeSystem, record) -> {
                    PersonNode node = new PersonNode();
                    node.setId(record.get("id").asString());
                    node.setFirstName(record.get("firstName").asString());
                    node.setLastName(record.get("lastName").asString());
                    node.setAge(record.get("age").asInt());
                    node.setCreatedBy(record.get("createdBy").asString(null));
                    node.setCreatedDate(record.get("createdDate").asLocalDateTime(null));
                    node.setLastModifiedBy(record.get("lastModifiedBy").asString(null));
                    node.setLastModifiedDate(record.get("lastModifiedDate").asLocalDateTime(null));
                    return node;
                })
                .one()
                .orElseThrow();

        // Then
        assertThat(foundPerson.getCreatedBy()).isNotNull();
        assertThat(foundPerson.getCreatedDate()).isNotNull();
        assertThat(foundPerson.getLastModifiedBy()).isNotNull();
        assertThat(foundPerson.getLastModifiedDate()).isNotNull();

        // Verify values match
        assertThat(foundPerson.getCreatedBy()).isEqualTo(savedPersons.get(0).getCreatedBy());
        assertThat(foundPerson.getCreatedDate()).isEqualTo(savedPersons.get(0).getCreatedDate());
        assertThat(foundPerson.getLastModifiedBy()).isEqualTo(savedPersons.get(0).getLastModifiedBy());
        assertThat(foundPerson.getLastModifiedDate()).isEqualTo(savedPersons.get(0).getLastModifiedDate());
    }

    @Test
    void shouldPopulateAuditFieldsForMultipleNodes() {
        // Given
        PersonNode person1 = new PersonNode()
                .setFirstName("Alice")
                .setLastName("Johnson")
                .setAge(28);

        PersonNode person2 = new PersonNode()
                .setFirstName("Bob")
                .setLastName("Williams")
                .setAge(32);

        PersonNode person3 = new PersonNode()
                .setFirstName("Charlie")
                .setLastName("Brown")
                .setAge(35);

        List<PersonNode> persons = List.of(person1, person2, person3);

        // When
        List<PersonNode> savedPersons = StreamSupport.stream(personNodeRepository.saveAll(persons).spliterator(), false)
                .toList();

        // Then
        for (PersonNode savedPerson : savedPersons) {
            assertThat(savedPerson.getCreatedBy()).isNotNull();
            assertThat(savedPerson.getCreatedDate()).isNotNull();
            assertThat(savedPerson.getLastModifiedBy()).isNotNull();
            assertThat(savedPerson.getLastModifiedDate()).isNotNull();
        }

        // Verify all timestamps are close to each other (batch operation)
        LocalDateTime firstTimestamp = savedPersons.get(0).getCreatedDate();
        for (PersonNode savedPerson : savedPersons) {
            assertThat(savedPerson.getCreatedDate()).isCloseTo(firstTimestamp, within(1, ChronoUnit.SECONDS));
        }
    }

    @Test
    void shouldUseDefaultAuditProviderValues() {
        // Given
        PersonNode person = new PersonNode()
                .setFirstName("Test")
                .setLastName("User")
                .setAge(40);

        List<PersonNode> persons = List.of(person);

        // When
        List<PersonNode> savedPersons = StreamSupport.stream(personNodeRepository.saveAll(persons).spliterator(), false)
                .toList();

        // Then
        PersonNode savedPerson = savedPersons.get(0);

        // DefaultAuditProvider returns "system" as current user
        assertThat(savedPerson.getCreatedBy()).isEqualTo("system");
        assertThat(savedPerson.getLastModifiedBy()).isEqualTo("system");

        // Timestamps should be set to current time
        assertThat(savedPerson.getCreatedDate()).isCloseTo(LocalDateTime.now(), within(5, ChronoUnit.SECONDS));
        assertThat(savedPerson.getLastModifiedDate()).isCloseTo(LocalDateTime.now(), within(5, ChronoUnit.SECONDS));
    }

    @Test
    void shouldVerifyAuditFieldsInNeo4jDatabase() {
        // Given
        PersonNode person = new PersonNode()
                .setFirstName("Neo4j")
                .setLastName("Test")
                .setAge(99);

        List<PersonNode> persons = List.of(person);

        // When
        List<PersonNode> savedPersons = StreamSupport.stream(personNodeRepository.saveAll(persons).spliterator(), false)
                .toList();

        String personId = savedPersons.get(0).getId();

        // Query Neo4j directly to verify audit fields are stored
        var result = neo4jClient.query(
                        "MATCH (p:Person) WHERE p.id = $id " +
                                "RETURN p.createdBy AS createdBy, p.createdDate AS createdDate, " +
                                "p.lastModifiedBy AS lastModifiedBy, p.lastModifiedDate AS lastModifiedDate"
                )
                .bind(personId).to("id")
                .fetchAs(AuditFields.class)
                .mappedBy((typeSystem, record) -> {
                    AuditFields fields = new AuditFields();
                    fields.createdBy = record.get("createdBy").asString(null);
                    fields.createdDate = record.get("createdDate").asLocalDateTime(null);
                    fields.lastModifiedBy = record.get("lastModifiedBy").asString(null);
                    fields.lastModifiedDate = record.get("lastModifiedDate").asLocalDateTime(null);
                    return fields;
                })
                .one()
                .orElseThrow();

        // Then
        assertThat(result.createdBy).isEqualTo("system");
        assertThat(result.createdDate).isNotNull();
        assertThat(result.lastModifiedBy).isEqualTo("system");
        assertThat(result.lastModifiedDate).isNotNull();

        // Verify timestamps match
        assertThat(result.createdDate).isEqualTo(savedPersons.get(0).getCreatedDate());
        assertThat(result.lastModifiedDate).isEqualTo(savedPersons.get(0).getLastModifiedDate());
    }

    private static class AuditFields {
        String createdBy;
        LocalDateTime createdDate;
        String lastModifiedBy;
        LocalDateTime lastModifiedDate;
    }
}