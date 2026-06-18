package com.tangluck.compliance;

public record ComplianceDocumentDto(
        String documentType,
        String version,
        String title,
        String contentUrl
) {
}
