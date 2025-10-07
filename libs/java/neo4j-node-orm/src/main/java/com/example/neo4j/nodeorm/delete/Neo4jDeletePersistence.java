package com.example.neo4j.nodeorm.delete;

import com.example.global.annotations.Persistence;
import com.example.neo4j.nodeorm.metadata.NodeMetadata;
import lombok.RequiredArgsConstructor;
import org.springframework.data.neo4j.core.Neo4jClient;

import java.util.List;

@Persistence
@RequiredArgsConstructor
public class Neo4jDeletePersistence {

    private final Neo4jClient neo4jClient;

    public void deleteNodeById(String id, NodeMetadata metadata) {
        String nodeName = metadata.getNodeName();
        String idField = metadata.getIdField().getFieldName();

        String cypher = "MATCH (n:" + nodeName + ") WHERE n." + idField + " = $id DETACH DELETE n";

        neo4jClient.query(cypher)
                .bind(id).to("id")
                .run();
    }

    public void deleteNodesById(List<String> ids, NodeMetadata metadata) {
        if (ids.isEmpty()) {
            return;
        }

        String nodeName = metadata.getNodeName();
        String idField = metadata.getIdField().getFieldName();

        String cypher = "MATCH (n:" + nodeName + ") WHERE n." + idField + " IN $ids DETACH DELETE n";

        neo4jClient.query(cypher)
                .bind(ids).to("ids")
                .run();
    }

    public void deleteAllNodes(NodeMetadata metadata) {
        String nodeName = metadata.getNodeName();

        String cypher = "MATCH (n:" + nodeName + ") DETACH DELETE n";

        neo4jClient.query(cypher)
                .run();
    }
}
