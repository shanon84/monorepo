package com.example.neo4j.nodeorm;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan(basePackages = "com.example.neo4j.nodeorm")
public class TestApplication {
}
