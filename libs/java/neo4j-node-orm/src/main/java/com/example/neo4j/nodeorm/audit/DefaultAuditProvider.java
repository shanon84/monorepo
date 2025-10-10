package com.example.neo4j.nodeorm.audit;

import java.time.LocalDateTime;

public class DefaultAuditProvider implements AuditProvider {

    @Override
    public String getCurrentUser() {
        return "system";
    }

    @Override
    public LocalDateTime getCurrentTimestamp() {
        return LocalDateTime.now();
    }
}
