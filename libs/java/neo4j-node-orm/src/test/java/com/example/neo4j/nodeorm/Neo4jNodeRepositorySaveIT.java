package com.example.neo4j.nodeorm;

import com.example.global.annotations.IT;
import com.example.neo4j.nodeorm.testdata.CompanyNode;
import com.example.neo4j.nodeorm.testdata.CompanyNodeRepository;
import com.example.neo4j.nodeorm.testdata.PersonNode;
import com.example.neo4j.nodeorm.testdata.PersonNodeRepository;
import com.example.neo4j.nodeorm.testdata.ProjectNode;
import com.example.neo4j.nodeorm.testdata.ProjectNodeRepository;
import com.example.neo4j.nodeorm.testdata.SimpleNode;
import com.example.neo4j.nodeorm.testdata.SimpleNodeRepository;
import com.example.neo4j.nodeorm.testdata.TestRepositoryConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.neo4j.core.Neo4jClient;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;

@IT
@Import(TestRepositoryConfiguration.class)
class Neo4jNodeRepositorySaveIT {

    @Autowired
    private Neo4jClient neo4jClient;

    @Autowired
    private SimpleNodeRepository simpleNodeRepository;

    @Autowired
    private PersonNodeRepository personNodeRepository;

    @Autowired
    private CompanyNodeRepository companyNodeRepository;

    @Autowired
    private ProjectNodeRepository projectNodeRepository;

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
        List<SimpleNode> savedNodes = StreamSupport.stream(simpleNodeRepository.saveAll(nodes).spliterator(), false)
                .toList();

        // Then
        assertThat(savedNodes).hasSize(2);
        assertThat(savedNodes.get(0).getId()).isNotNull();
        assertThat(savedNodes.get(0).getName()).isEqualTo("Node 1");
        assertThat(savedNodes.get(1).getId()).isNotNull();
        assertThat(savedNodes.get(1).getName()).isEqualTo("Node 2");

        // Verify with findById
        SimpleNode foundNode1 = simpleNodeRepository.findById(savedNodes.get(0).getId()).orElseThrow();
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
        List<PersonNode> savedPersons = StreamSupport.stream(personNodeRepository.saveAll(persons).spliterator(), false)
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
        List<CompanyNode> savedCompanies = StreamSupport.stream(companyNodeRepository.saveAll(companies).spliterator(), false)
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
        List<ProjectNode> savedProjects = StreamSupport.stream(projectNodeRepository.saveAll(projects).spliterator(), false)
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
        List<PersonNode> savedPersons = StreamSupport.stream(personNodeRepository.saveAll(persons).spliterator(), false)
                .toList();

        // Then
        assertThat(savedPersons.get(0).getId()).isNotNull();
        assertThat(person.getId()).isNotNull(); // Original object should also have ID
    }

    @Test
    void shouldUpdateNodeWhenIdIsSetAndNodeExists() {
        // Given - Create a node first
        SimpleNode node = new SimpleNode()
                .setName("Original Name");

        SimpleNode savedNode = simpleNodeRepository.save(node);
        String savedId = savedNode.getId();

        // When - Update the node by setting the ID and changing properties
        SimpleNode updateNode = new SimpleNode()
                .setId(savedId)
                .setName("Updated Name");

        SimpleNode updatedNode = simpleNodeRepository.save(updateNode);

        // Then - Verify the node was updated
        assertThat(updatedNode.getId()).isEqualTo(savedId);
        assertThat(updatedNode.getName()).isEqualTo("Updated Name");

        // Verify in DB
        SimpleNode foundNode = simpleNodeRepository.findById(savedId).orElseThrow();
        assertThat(foundNode.getName()).isEqualTo("Updated Name");
    }

    @Test
    void shouldCreateNodeWhenIdIsSetButNodeDoesNotExist() {
        // Given - A node with an ID that doesn't exist in DB
        SimpleNode node = new SimpleNode()
                .setId("non-existing-uuid")
                .setName("New Node with ID");

        // When
        SimpleNode savedNode = simpleNodeRepository.save(node);

        // Then - Node should be created with the specified ID
        assertThat(savedNode.getId()).isEqualTo("non-existing-uuid");
        assertThat(savedNode.getName()).isEqualTo("New Node with ID");

        // Verify in DB
        SimpleNode foundNode = simpleNodeRepository.findById("non-existing-uuid").orElseThrow();
        assertThat(foundNode.getName()).isEqualTo("New Node with ID");
    }

    @Test
    void shouldUpdatePersonNodeWithAuditFields() {
        // Given - Create a person first
        PersonNode person = new PersonNode()
                .setFirstName("John")
                .setLastName("Doe")
                .setAge(30);

        List<PersonNode> savedPersons = StreamSupport.stream(personNodeRepository.saveAll(List.of(person)).spliterator(), false).toList();
        PersonNode savedPerson = savedPersons.get(0);

        String savedId = savedPerson.getId();
        String originalCreatedBy = savedPerson.getCreatedBy();
        var originalCreatedDate = savedPerson.getCreatedDate();

        // When - Update the person
        PersonNode updatePerson = new PersonNode()
                .setId(savedId)
                .setFirstName("John")
                .setLastName("Smith") // Changed
                .setAge(31); // Changed

        List<PersonNode> updatedPersons = StreamSupport.stream(personNodeRepository.saveAll(List.of(updatePerson)).spliterator(), false).toList();
        PersonNode updatedPerson = updatedPersons.get(0);

        // Then - Verify updated fields
        assertThat(updatedPerson.getId()).isEqualTo(savedId);
        assertThat(updatedPerson.getLastName()).isEqualTo("Smith");
        assertThat(updatedPerson.getAge()).isEqualTo(31);


        Optional<PersonNode> loadedPersonNodeOpt = personNodeRepository.findById(savedId);
        assertThat(loadedPersonNodeOpt).isPresent();
        PersonNode loadedPersonNode = loadedPersonNodeOpt.get();

        assertThat(loadedPersonNode.getCreatedBy()).isEqualTo(originalCreatedBy); // Should remain unchanged
        assertThat(loadedPersonNode.getLastModifiedBy()).isEqualTo("system"); // Should be updated
        assertThat(loadedPersonNode.getLastModifiedDate()).isNotNull(); // Should be updated
    }

    @Test
    void shouldNotUpdateRelatedNodeWhenCascadeUpdatesIsFalse() {
        // Given - Create a project manager first
        PersonNode manager = new PersonNode()
                .setFirstName("Sarah")
                .setLastName("Connor")
                .setAge(45);

        List<PersonNode> savedManagers = StreamSupport.stream(personNodeRepository.saveAll(List.of(manager)).spliterator(), false).toList();
        PersonNode savedManager = savedManagers.get(0);
        String managerId = savedManager.getId();

        // Verify manager exists
        assertThat(managerId).isNotNull();
        assertThat(savedManager.getLastName()).isEqualTo("Connor");

        // When - Create a project with the existing manager but with modified properties
        PersonNode modifiedManager = new PersonNode()
                .setId(managerId)
                .setFirstName("Sarah")
                .setLastName("Modified") // Changed last name
                .setAge(50); // Changed age

        ProjectNode project = new ProjectNode()
                .setProjectName("Neo4j ORM")
                .setStartDate(LocalDate.of(2024, 1, 1))
                .setEndDate(LocalDate.of(2024, 12, 31))
                .setProjectManager(modifiedManager); // cascadeUpdates = false

        List<ProjectNode> savedProjects = StreamSupport.stream(projectNodeRepository.saveAll(List.of(project)).spliterator(), false).toList();

        // Then - Verify project was saved
        assertThat(savedProjects).hasSize(1);
        assertThat(savedProjects.get(0).getId()).isNotNull();
        assertThat(savedProjects.get(0).getProjectManager()).isNotNull();

        // Verify manager was NOT updated (cascadeUpdates = false)
        PersonNode managerInDb = personNodeRepository.findById(managerId).orElseThrow();
        assertThat(managerInDb.getLastName()).isEqualTo("Connor"); // Should remain unchanged
        assertThat(managerInDb.getAge()).isEqualTo(45); // Should remain unchanged
    }

    @Test
    void shouldUpdateRelatedNodeWhenCascadeUpdatesIsTrue() {
        // Given - Create a developer first
        PersonNode developer = new PersonNode()
                .setFirstName("Tom")
                .setLastName("Anderson")
                .setAge(30);

        List<PersonNode> savedDevelopers = StreamSupport.stream(personNodeRepository.saveAll(List.of(developer)).spliterator(), false).toList();
        PersonNode savedDeveloper = savedDevelopers.get(0);
        String developerId = savedDeveloper.getId();

        // Verify developer exists
        assertThat(developerId).isNotNull();
        assertThat(savedDeveloper.getLastName()).isEqualTo("Anderson");

        // When - Create a project with the existing developer but with modified properties
        PersonNode modifiedDeveloper = new PersonNode()
                .setId(developerId)
                .setFirstName("Tom")
                .setLastName("Modified") // Changed last name
                .setAge(35); // Changed age

        ProjectNode project = new ProjectNode()
                .setProjectName("Neo4j ORM")
                .setStartDate(LocalDate.of(2024, 1, 1))
                .setEndDate(LocalDate.of(2024, 12, 31))
                .setTeamMembers(List.of(modifiedDeveloper)); // cascadeUpdates = true (default)

        List<ProjectNode> savedProjects = StreamSupport.stream(projectNodeRepository.saveAll(List.of(project)).spliterator(), false).toList();

        // Then - Verify project was saved
        assertThat(savedProjects).hasSize(1);
        assertThat(savedProjects.get(0).getId()).isNotNull();
        assertThat(savedProjects.get(0).getTeamMembers()).hasSize(1);

        // Verify developer WAS updated (cascadeUpdates = true by default)
        PersonNode developerInDb = personNodeRepository.findById(developerId).orElseThrow();
        assertThat(developerInDb.getLastName()).isEqualTo("Modified"); // Should be updated
        assertThat(developerInDb.getAge()).isEqualTo(35); // Should be updated
    }
}
