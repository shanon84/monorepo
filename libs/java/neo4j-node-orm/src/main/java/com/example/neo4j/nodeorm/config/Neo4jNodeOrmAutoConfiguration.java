package com.example.neo4j.nodeorm.config;

import com.example.neo4j.nodeorm.audit.AuditProvider;
import com.example.neo4j.nodeorm.audit.DefaultAuditProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;

@Configuration
@EnableNeo4jRepositories
@ComponentScan(basePackages = "com.example.neo4j.nodeorm")
public class Neo4jNodeOrmAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AuditProvider auditProvider() {
        return new DefaultAuditProvider();
    }
}
