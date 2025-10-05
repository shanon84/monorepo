package com.example.neo4j.nodeorm.testdata;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Node("Project")
@Data
public class ProjectNode {

    @Id
    @GeneratedValue(GeneratedValue.UUIDGenerator.class)
    private Long id;

    private String projectName;

    private LocalDate startDate;

    private LocalDate endDate;

    @Relationship(type = "WORKS_ON", direction = Relationship.Direction.INCOMING)
    private List<PersonNode> teamMembers = new ArrayList<>();

    @Relationship(type = "MANAGED_BY", direction = Relationship.Direction.OUTGOING)
    private PersonNode projectManager;
}
