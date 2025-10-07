package com.example.neo4j.nodeorm;

import com.example.global.annotations.IT;
import com.example.neo4j.nodeorm.testdata.CompanyNode;
import com.example.neo4j.nodeorm.testdata.PersonNode;
import com.example.neo4j.nodeorm.testdata.ProjectNode;
import com.example.neo4j.nodeorm.testdata.SimpleNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.core.Neo4jClient;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;

@IT
class Neo4jNodeRepositorySaveIT {

    @Autowired
    private Neo4jClient neo4jClient;

    @Autowired
    private Neo4jNodeRepository repository;

    @AfterEach
    void cleanUp() {
        neo4jClient.query("MATCH (n) DETACH DELETE n").run();
    }

    @Test
    void shouldSaveSimpleNodes() {
        // Given
        SimpleNode node1 = new SimpleNode()
                .setName("Node 1");

        SimpleNode node2 = new SimpleNode()
                .setName("Node 2");

        List<SimpleNode> nodes = List.of(node1, node2);

        // When
        List<SimpleNode> savedNodes = StreamSupport.stream(repository.saveAll(nodes).spliterator(), false)
                .toList();

        // Then
        assertThat(savedNodes).hasSize(2);
        assertThat(savedNodes.get(0).getId()).isNotNull();
        assertThat(savedNodes.get(0).getName()).isEqualTo("Node 1");
        assertThat(savedNodes.get(1).getId()).isNotNull();
        assertThat(savedNodes.get(1).getName()).isEqualTo("Node 2");

        // Verify with findById
        SimpleNode foundNode1 = (SimpleNode) repository.findById(savedNodes.get(0).getId()).orElseThrow();
        assertThat(foundNode1.getName()).isEqualTo("Node 1");
    }

    @Test
    void shouldSavePersonNodesWithProperties() {
        // Given
        PersonNode person1 = new PersonNode()
                .setFirstName("John")
                .setLastName("Doe")
                .setBirthDate(LocalDate.of(1990, 5, 15))
                .setAge(34);

        PersonNode person2 = new PersonNode()
                .setFirstName("Jane")
                .setLastName("Smith")
                .setBirthDate(LocalDate.of(1985, 8, 22))
                .setAge(39);

        List<PersonNode> persons = List.of(person1, person2);

        // When
        List<PersonNode> savedPersons = StreamSupport.stream(repository.saveAll(persons).spliterator(), false)
                .toList();

        // Then
        assertThat(savedPersons).hasSize(2);
        assertThat(savedPersons.get(0).getId()).isNotNull();
        assertThat(savedPersons.get(0).getFirstName()).isEqualTo("John");
        assertThat(savedPersons.get(1).getLastName()).isEqualTo("Smith");
    }

    @Test
    void shouldSaveCompanyNodesWithRelationships() {
        // Given
        PersonNode employee1 = new PersonNode()
                .setFirstName("Alice")
                .setLastName("Johnson")
                .setAge(28);

        PersonNode employee2 = new PersonNode()
                .setFirstName("Bob")
                .setLastName("Williams")
                .setAge(32);

        CompanyNode company = new CompanyNode()
                .setName("Tech Corp")
                .setIndustry("Technology")
                .setEmployees(List.of(employee1, employee2));

        List<CompanyNode> companies = List.of(company);

        // When
        List<CompanyNode> savedCompanies = StreamSupport.stream(repository.saveAll(companies).spliterator(), false)
                .toList();

        // Then
        assertThat(savedCompanies).hasSize(1);
        assertThat(savedCompanies.get(0).getId()).isNotNull();
        assertThat(savedCompanies.get(0).getName()).isEqualTo("Tech Corp");
        assertThat(savedCompanies.get(0).getEmployees()).hasSize(2);
        assertThat(savedCompanies.get(0).getEmployees().get(0).getId()).isNotNull();
    }

    @Test
    void shouldSaveProjectNodesWithBidirectionalRelationships() {
        // Given
        PersonNode manager = new PersonNode()
                .setFirstName("Sarah")
                .setLastName("Connor")
                .setAge(45);

        PersonNode developer1 = new PersonNode()
                .setFirstName("Tom")
                .setLastName("Anderson")
                .setAge(30);

        PersonNode developer2 = new PersonNode()
                .setFirstName("Lisa")
                .setLastName("Brown")
                .setAge(27);

        ProjectNode project = new ProjectNode()
                .setProjectName("Neo4j ORM")
                .setStartDate(LocalDate.of(2024, 1, 1))
                .setEndDate(LocalDate.of(2024, 12, 31))
                .setProjectManager(manager)
                .setTeamMembers(List.of(developer1, developer2));

        List<ProjectNode> projects = List.of(project);

        // When
        List<ProjectNode> savedProjects = StreamSupport.stream(repository.saveAll(projects).spliterator(), false)
                .toList();

        // Then
        assertThat(savedProjects).hasSize(1);
        assertThat(savedProjects.get(0).getId()).isNotNull();
        assertThat(savedProjects.get(0).getProjectName()).isEqualTo("Neo4j ORM");
        assertThat(savedProjects.get(0).getProjectManager()).isNotNull();
        assertThat(savedProjects.get(0).getProjectManager().getId()).isNotNull();
        assertThat(savedProjects.get(0).getTeamMembers()).hasSize(2);
    }

    @Test
    void shouldGenerateIdForGeneratedValueAnnotation() {
        // Given
        PersonNode person = new PersonNode()
                .setFirstName("John")
                .setLastName("Doe")
                .setAge(30);

        // Verify ID is null before save
        assertThat(person.getId()).isNull();

        List<PersonNode> persons = List.of(person);

        // When
        List<PersonNode> savedPersons = StreamSupport.stream(repository.saveAll(persons).spliterator(), false)
                .toList();

        // Then
        assertThat(savedPersons.get(0).getId()).isNotNull();
        assertThat(person.getId()).isNotNull(); // Original object should also have ID
    }
}
