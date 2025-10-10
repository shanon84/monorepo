package com.example.neo4j.nodeorm.audit;

import java.time.LocalDateTime;

public interface AuditProvider {

    /**
     * Returns the current user identifier for auditing purposes.
     * @return the current user (e.g., username, user ID, email)
     */
    String getCurrentUser();

    /**
     * Returns the current timestamp for auditing purposes.
     * @return the current timestamp
     */
    LocalDateTime getCurrentTimestamp();
}
