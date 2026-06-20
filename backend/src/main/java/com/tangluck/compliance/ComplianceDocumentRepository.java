package com.tangluck.compliance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ComplianceDocumentRepository extends JpaRepository<ComplianceDocument, Long> {
    List<ComplianceDocument> findByStatusOrderByDocumentTypeAsc(String status);

    List<ComplianceDocument> findByDocumentTypeAndStatus(String documentType, String status);

    Optional<ComplianceDocument> findByDocumentTypeAndVersion(String documentType, String version);
}
