package com.example.neo4j.nodeorm.testdata;

import com.example.neo4j.nodeorm.Neo4jNodeRepository;
import org.springframework.data.neo4j.repository.query.Query;

import java.util.List;

public interface PersonNodeRepository extends Neo4jNodeRepository<PersonNode, String> {

    List<PersonNode> findAllByLastName(String name);

    @Query("""
            MATCH (n:Person) WHERE
            n.age > $age
            RETURN n
            """)
    List<PersonNode> findAllByAgeGreateThan(Integer age);

}
