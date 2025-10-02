package com.example.neo4j.nodeorm.testdata;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.ArrayList;
import java.util.List;

@Node("Company")
@Data
public class CompanyNode {

  @Id
  @GeneratedValue
  private Long id;

  private String name;

  private String industry;

  @Relationship(type = "EMPLOYS", direction = Relationship.Direction.OUTGOING)
  private List<PersonNode> employees = new ArrayList<>();
}
