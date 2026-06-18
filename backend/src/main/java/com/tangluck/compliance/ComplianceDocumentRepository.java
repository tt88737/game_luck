package com.tangluck.compliance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComplianceDocumentRepository extends JpaRepository<ComplianceDocument, Long> {
    List<ComplianceDocument> findByStatusOrderByDocumentTypeAsc(String status);
}
