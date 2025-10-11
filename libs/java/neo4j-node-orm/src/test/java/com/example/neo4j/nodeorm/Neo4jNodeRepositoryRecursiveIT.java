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
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;

@IT
@Import(TestRepositoryConfiguration.class)
class Neo4jNodeRepositoryRecursiveIT {

    @Autowired
    private Neo4jClient neo4jClient;

    @Autowired
    private PersonNodeRepository personNodeRepository;

    @AfterEach
    void cleanUp() {
        neo4jClient.query("MATCH (n) DETACH DELETE n").run();
    }

    @Test
    void shouldSavePersonWithManagerRecursively() {
        // Given - Create organizational hierarchy
        // CEO
        PersonNode ceo = new PersonNode()
                .setFirstName("Alice")
                .setLastName("CEO");

        // Manager reporting to CEO
        PersonNode manager = new PersonNode()
                .setFirstName("Bob")
                .setLastName("Manager")
                .setManager(ceo);

        // Employee reporting to Manager
        PersonNode employee = new PersonNode()
                .setFirstName("Charlie")
                .setLastName("Employee")
                .setManager(manager);

        // When - Save only the employee (should recursively save manager and CEO)
        PersonNode savedEmployee = personNodeRepository.save(employee);

        // Then - All persons should be saved
        assertThat(savedEmployee.getId()).isNotNull();
        assertThat(savedEmployee.getManager()).isNotNull();
        assertThat(savedEmployee.getManager().getId()).isNotNull();
        assertThat(savedEmployee.getManager().getManager()).isNotNull();
        assertThat(savedEmployee.getManager().getManager().getId()).isNotNull();

        // Verify all 3 persons are in database
        List<PersonNode> allPersons = personNodeRepository.findAll();
        assertThat(allPersons).hasSize(3);

        // Verify REPORTS_TO relationships exist
        Long relationshipCount = neo4jClient.query(
                        "MATCH ()-[r:REPORTS_TO]->() RETURN count(r) AS count"
                ).fetchAs(Long.class)
                .mappedBy((typeSystem, record) -> record.get("count").asLong())
                .one()
                .orElse(0L);

        assertThat(relationshipCount).isEqualTo(2); // Employee->Manager and Manager->CEO
    }

    @Test
    void shouldSaveMultipleEmployeesWithSameManager() {
        // Given - Multiple employees reporting to same manager
        PersonNode manager = new PersonNode()
                .setFirstName("Alice")
                .setLastName("Manager");

        PersonNode employee1 = new PersonNode()
                .setFirstName("Bob")
                .setLastName("Employee1")
                .setManager(manager);

        PersonNode employee2 = new PersonNode()
                .setFirstName("Charlie")
                .setLastName("Employee2")
                .setManager(manager);

        PersonNode employee3 = new PersonNode()
                .setFirstName("Dave")
                .setLastName("Employee3")
                .setManager(manager);

        // When - Save all employees
        List<PersonNode> savedEmployees = StreamSupport.stream(
                personNodeRepository.saveAll(List.of(employee1, employee2, employee3)).spliterator(),
                false
        ).toList();

        // Then - All employees should be saved with IDs
        assertThat(savedEmployees).hasSize(3);
        assertThat(savedEmployees).allMatch(p -> p.getId() != null);
        assertThat(savedEmployees).allMatch(p -> p.getManager() != null);
        assertThat(savedEmployees).allMatch(p -> p.getManager().getId() != null);

        // Verify manager is saved only once (not duplicated)
        List<PersonNode> allPersons = personNodeRepository.findAll();
        assertThat(allPersons).hasSize(4); // 3 employees + 1 manager

        // Verify manager ID is the same for all employees
        String managerId = savedEmployees.get(0).getManager().getId();
        assertThat(savedEmployees.get(1).getManager().getId()).isEqualTo(managerId);
        assertThat(savedEmployees.get(2).getManager().getId()).isEqualTo(managerId);

        // Verify 3 REPORTS_TO relationships exist
        Long relationshipCount = neo4jClient.query(
                        "MATCH ()-[r:REPORTS_TO]->() RETURN count(r) AS count"
                ).fetchAs(Long.class)
                .mappedBy((typeSystem, record) -> record.get("count").asLong())
                .one()
                .orElse(0L);

        assertThat(relationshipCount).isEqualTo(3);
    }

    @Test
    void shouldHandleDeepOrganizationalHierarchy() {
        // Given - Deep hierarchy: CEO -> VP -> Director -> Manager -> TeamLead -> Employee
        PersonNode ceo = new PersonNode()
                .setFirstName("Level1")
                .setLastName("CEO");

        PersonNode vp = new PersonNode()
                .setFirstName("Level2")
                .setLastName("VP")
                .setManager(ceo);

        PersonNode director = new PersonNode()
                .setFirstName("Level3")
                .setLastName("Director")
                .setManager(vp);

        PersonNode manager = new PersonNode()
                .setFirstName("Level4")
                .setLastName("Manager")
                .setManager(director);

        PersonNode teamLead = new PersonNode()
                .setFirstName("Level5")
                .setLastName("TeamLead")
                .setManager(manager);

        PersonNode employee = new PersonNode()
                .setFirstName("Level6")
                .setLastName("Employee")
                .setManager(teamLead);

        // When - Save only the bottom-level employee
        PersonNode savedEmployee = personNodeRepository.save(employee);

        // Then - All 6 levels should be saved
        assertThat(savedEmployee.getId()).isNotNull();

        List<PersonNode> allPersons = personNodeRepository.findAll();
        assertThat(allPersons).hasSize(6);

        // Verify 5 REPORTS_TO relationships exist (6 people, 5 connections)
        Long relationshipCount = neo4jClient.query(
                        "MATCH ()-[r:REPORTS_TO]->() RETURN count(r) AS count"
                ).fetchAs(Long.class)
                .mappedBy((typeSystem, record) -> record.get("count").asLong())
                .one()
                .orElse(0L);

        assertThat(relationshipCount).isEqualTo(5);

        // Verify we can traverse the entire chain
        PersonNode current = savedEmployee;
        int levels = 0;
        while (current.getManager() != null) {
            current = current.getManager();
            levels++;
        }
        assertThat(levels).isEqualTo(5); // 5 manager levels above employee
    }

    @Test
    void shouldSavePersonWithoutManager() {
        // Given - Person without manager (CEO/top level)
        PersonNode ceo = new PersonNode()
                .setFirstName("Alice")
                .setLastName("CEO");

        // When
        PersonNode savedCeo = personNodeRepository.save(ceo);

        // Then
        assertThat(savedCeo.getId()).isNotNull();
        assertThat(savedCeo.getManager()).isNull();

        // Verify no REPORTS_TO relationships exist
        Long relationshipCount = neo4jClient.query(
                        "MATCH ()-[r:REPORTS_TO]->() RETURN count(r) AS count"
                ).fetchAs(Long.class)
                .mappedBy((typeSystem, record) -> record.get("count").asLong())
                .one()
                .orElse(0L);

        assertThat(relationshipCount).isEqualTo(0);
    }

    @Test
    void shouldUpdateExistingPersonAndAddManager() {
        // Given - Save person without manager first
        PersonNode employee = new PersonNode()
                .setFirstName("Bob")
                .setLastName("Employee");

        PersonNode savedEmployee = personNodeRepository.save(employee);
        String employeeId = savedEmployee.getId();

        // When - Add manager to existing person and save again
        PersonNode manager = new PersonNode()
                .setFirstName("Alice")
                .setLastName("Manager");

        savedEmployee.setManager(manager);
        PersonNode updatedEmployee = personNodeRepository.save(savedEmployee);

        // Then - Employee should have manager now
        assertThat(updatedEmployee.getId()).isEqualTo(employeeId);
        assertThat(updatedEmployee.getManager()).isNotNull();
        assertThat(updatedEmployee.getManager().getId()).isNotNull();

        // Verify 2 persons exist
        List<PersonNode> allPersons = personNodeRepository.findAll();
        assertThat(allPersons).hasSize(2);

        // Verify 1 REPORTS_TO relationship exists
        Long relationshipCount = neo4jClient.query(
                        "MATCH ()-[r:REPORTS_TO]->() RETURN count(r) AS count"
                ).fetchAs(Long.class)
                .mappedBy((typeSystem, record) -> record.get("count").asLong())
                .one()
                .orElse(0L);

        assertThat(relationshipCount).isEqualTo(1);
    }
}
