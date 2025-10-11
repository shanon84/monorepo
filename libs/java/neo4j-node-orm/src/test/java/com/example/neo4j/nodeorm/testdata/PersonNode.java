package com.example.neo4j.nodeorm.testdata;

import lombok.Data;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Node("Person")
@Data
public class PersonNode {

    @Id
    @GeneratedValue(GeneratedValue.UUIDGenerator.class)
    private String id;

    @Property("firstName")
    private String firstName;

    @Property("lastName")
    private String lastName;

    private LocalDate birthDate;

    private Integer age;

    @Version
    private Integer version;

    @CreatedBy
    private String createdBy;

    @CreatedDate
    private LocalDateTime createdDate;

    @LastModifiedBy
    private String lastModifiedBy;

    @LastModifiedDate
    private LocalDateTime lastModifiedDate;
}
