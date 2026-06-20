package com.tangluck.compliance;

public final class AdminComplianceDtos {
    private AdminComplianceDtos() {
    }

    public record RegionDto(
            String countryCode,
            String stateCode,
            boolean registrationAllowed,
            boolean gameAllowed,
            boolean purchaseAllowed,
            boolean scGrantAllowed,
            boolean redemptionAllowed,
            boolean amoeAllowed,
            boolean requiresLegalReview,
            String status,
            String legalApprovalId
    ) {
    }

    public record UpdateRegionRequest(
            boolean registrationAllowed,
            boolean gameAllowed,
            boolean purchaseAllowed,
            boolean scGrantAllowed,
            boolean redemptionAllowed,
            boolean amoeAllowed,
            boolean requiresLegalReview,
            String status,
            String legalApprovalId
    ) {
    }

    public record LegalDocumentDto(
            String documentType,
            String version,
            String title,
            String contentUrl,
            String status,
            String legalApprovalId
    ) {
    }

    public record CreateLegalDocumentRequest(
            String documentType,
            String version,
            String title,
            String contentUrl,
            String legalApprovalId
    ) {
    }
}
