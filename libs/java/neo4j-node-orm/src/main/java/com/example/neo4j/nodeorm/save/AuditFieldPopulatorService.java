package com.example.neo4j.nodeorm.save;

import com.example.neo4j.nodeorm.audit.AuditProvider;
import com.example.neo4j.nodeorm.metadata.AuditFieldMetadata;
import com.example.neo4j.nodeorm.metadata.NodeMetadata;
import com.example.neo4j.nodeorm.reflection.ReflectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuditFieldPopulatorService {

    private final AuditProvider auditProvider;
    private final ReflectionService reflectionService;

    public void populateAuditFieldsOnNode(Object node, NodeMetadata metadata, boolean isCreate) {
        if (!metadata.getAuditFields().hasAnyAuditFields()) {
            return;
        }

        AuditFieldMetadata auditFields = metadata.getAuditFields();
        String currentUser = auditProvider.getCurrentUser();
        LocalDateTime currentTimestamp = auditProvider.getCurrentTimestamp();

        if (isCreate) {
            // Set createdBy and createdDate for new nodes
            if (auditFields.hasCreatedBy()) {
                reflectionService.setFieldValue(auditFields.getCreatedByField(), node, currentUser);
            }
            if (auditFields.hasCreatedDate()) {
                reflectionService.setFieldValue(auditFields.getCreatedDateField(), node, currentTimestamp);
            }
        }

        // Always set lastModifiedBy and lastModifiedDate
        if (auditFields.hasLastModifiedBy()) {
            reflectionService.setFieldValue(auditFields.getLastModifiedByField(), node, currentUser);
        }
        if (auditFields.hasLastModifiedDate()) {
            reflectionService.setFieldValue(auditFields.getLastModifiedDateField(), node, currentTimestamp);
        }
    }
}
