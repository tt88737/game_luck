package com.tangluck.compliance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "compliance_documents")
public class ComplianceDocument {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_type", nullable = false)
    private String documentType;

    @Column(nullable = false)
    private String version;

    @Column(nullable = false)
    private String title;

    @Column(name = "content_url", nullable = false)
    private String contentUrl;

    @Column(name = "effective_at", nullable = false)
    private Instant effectiveAt;

    @Column(nullable = false)
    private String status;

    @Column(name = "legal_approval_id")
    private String legalApprovalId;

    protected ComplianceDocument() {
    }

    public ComplianceDocument(String documentType, String version, String title, String contentUrl, String status, String legalApprovalId, Instant now) {
        this.documentType = documentType;
        this.version = version;
        this.title = title;
        this.contentUrl = contentUrl;
        this.effectiveAt = now;
        this.status = status;
        this.legalApprovalId = legalApprovalId;
    }

    public String getDocumentType() {
        return documentType;
    }

    public String getVersion() {
        return version;
    }

    public String getTitle() {
        return title;
    }

    public String getContentUrl() {
        return contentUrl;
    }

    public String getStatus() {
        return status;
    }

    public String getLegalApprovalId() {
        return legalApprovalId;
    }

    public void publish() {
        this.status = "active";
    }

    public void archive() {
        this.status = "archived";
    }
}
