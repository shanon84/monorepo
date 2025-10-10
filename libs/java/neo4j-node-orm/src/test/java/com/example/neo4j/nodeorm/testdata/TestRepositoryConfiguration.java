package com.example.neo4j.nodeorm.testdata;

import com.example.neo4j.nodeorm.Neo4jNodeRepository;
import com.example.neo4j.nodeorm.Neo4jNodeRepositoryImpl;
import com.example.neo4j.nodeorm.delete.Neo4jDeleteService;
import com.example.neo4j.nodeorm.find.Neo4jFindService;
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

    @Bean
    public SimpleNodeRepository simpleNodeRepository() {
        return createTypedRepository(SimpleNodeRepository.class);
    }

    @Bean
    public PersonNodeRepository personNodeRepository() {
        return createTypedRepository(PersonNodeRepository.class);
    }

    @Bean
    public CompanyNodeRepository companyNodeRepository() {
        return createTypedRepository(CompanyNodeRepository.class);
    }

    @Bean
    public ProjectNodeRepository projectNodeRepository() {
        return createTypedRepository(ProjectNodeRepository.class);
    }

    @SuppressWarnings("unchecked")
    private <T, R extends Neo4jNodeRepository<T>> R createTypedRepository(Class<R> repositoryInterface) {
        Neo4jNodeRepositoryImpl<T> impl = new Neo4jNodeRepositoryImpl<>(saveService, findService, deleteService);

        return (R) Proxy.newProxyInstance(
                repositoryInterface.getClassLoader(),
                new Class<?>[]{repositoryInterface},
                (proxy, method, args) -> {
                    try {
                        return method.invoke(impl, args);
                    } catch (Exception e) {
                        throw e.getCause() != null ? e.getCause() : e;
                    }
                }
        );
    }
}
