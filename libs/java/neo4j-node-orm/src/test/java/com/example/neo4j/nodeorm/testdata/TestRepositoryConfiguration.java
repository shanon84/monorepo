package com.example.neo4j.nodeorm.testdata;

import com.example.neo4j.nodeorm.Neo4jNodeRepository;
import com.example.neo4j.nodeorm.Neo4jNodeRepositoryImpl;
import com.example.neo4j.nodeorm.delete.Neo4jDeleteService;
import com.example.neo4j.nodeorm.find.Neo4jFindService;
import com.example.neo4j.nodeorm.query.QueryMethodHandler;
import com.example.neo4j.nodeorm.save.Neo4jSaveService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.lang.reflect.Proxy;

@TestConfiguration
@RequiredArgsConstructor
public class TestRepositoryConfiguration {

    private final Neo4jSaveService saveService;
    private final Neo4jFindService findService;
    private final Neo4jDeleteService deleteService;
    private final QueryMethodHandler queryMethodHandler;

    @Bean
    public SimpleNodeRepository simpleNodeRepository() {
        return createTypedRepository(SimpleNodeRepository.class, SimpleNode.class);
    }

    @Bean
    public PersonNodeRepository personNodeRepository() {
        return createTypedRepository(PersonNodeRepository.class, PersonNode.class);
    }

    @Bean
    public CompanyNodeRepository companyNodeRepository() {
        return createTypedRepository(CompanyNodeRepository.class, CompanyNode.class);
    }

    @Bean
    public ProjectNodeRepository projectNodeRepository() {
        return createTypedRepository(ProjectNodeRepository.class, ProjectNode.class);
    }

    @SuppressWarnings("unchecked")
    private <T, ID, R extends Neo4jNodeRepository<T, ID>> R createTypedRepository(
            Class<R> repositoryInterface,
            Class<T> entityClass
    ) {
        Neo4jNodeRepositoryImpl<T, ID> impl = new Neo4jNodeRepositoryImpl<>(saveService, findService, deleteService);

        return (R) Proxy.newProxyInstance(
                repositoryInterface.getClassLoader(),
                new Class<?>[]{repositoryInterface},
                (proxy, method, args) -> {
                    try {
                        // Check if method can be handled by QueryMethodHandler
                        if (queryMethodHandler.canHandle(method)) {
                            return queryMethodHandler.executeQuery(method, args, entityClass);
                        }

                        // Otherwise, delegate to standard implementation
                        return method.invoke(impl, args);
                    } catch (Exception e) {
                        throw e.getCause() != null ? e.getCause() : e;
                    }
                }
        );
    }
}
