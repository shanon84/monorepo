package com.example.neo4j.nodeorm.testdata;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;

import java.time.LocalDate;

@Node("Person")
@Data
public class PersonNode {

  @Id
  @GeneratedValue
  private Long id;

  @Property("firstName")
  private String firstName;

  @Property("lastName")
  private String lastName;

  private LocalDate birthDate;

  private Integer age;
}
