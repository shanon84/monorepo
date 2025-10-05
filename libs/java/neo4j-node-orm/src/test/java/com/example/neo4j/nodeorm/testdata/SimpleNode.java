package com.example.neo4j.nodeorm.testdata;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

@Node
@Data
public class SimpleNode {

    @Id
    @GeneratedValue(GeneratedValue.UUIDGenerator.class)
    private Long id;

    private String name;
}
