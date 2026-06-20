package com.tangluck.compliance;

import com.tangluck.admin.AdminAuditService;
import com.tangluck.admin.AdminOperatorContext;
import com.tangluck.common.api.BusinessException;
import com.tangluck.common.api.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Map;

import static com.tangluck.compliance.AdminComplianceDtos.CreateLegalDocumentRequest;
import static com.tangluck.compliance.AdminComplianceDtos.LegalDocumentDto;
import static com.tangluck.compliance.AdminComplianceDtos.RegionDto;
import static com.tangluck.compliance.AdminComplianceDtos.UpdateRegionRequest;

@Service
public class AdminComplianceService {
    private final ComplianceRegionRepository regionRepository;
    private final ComplianceDocumentRepository documentRepository;
    private final AdminAuditService auditService;
    private final Clock clock;

    public AdminComplianceService(
            ComplianceRegionRepository regionRepository,
            ComplianceDocumentRepository documentRepository,
            AdminAuditService auditService
    ) {
        this.regionRepository = regionRepository;
        this.documentRepository = documentRepository;
        this.auditService = auditService;
        this.clock = Clock.systemUTC();
    }

    @Transactional(readOnly = true)
    public List<RegionDto> regions() {
        return regionRepository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional
    public RegionDto updateRegion(String countryCode, String stateCode, UpdateRegionRequest request, AdminOperatorContext operator) {
        var region = regionRepository.findByCountryCodeAndStateCode(countryCode, stateCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.REGION_BLOCKED, "This region is not configured.", Map.of("state_code", stateCode)));
        var before = regionJson(region);
        region.update(
                request.registrationAllowed(),
                request.gameAllowed(),
                request.purchaseAllowed(),
                request.scGrantAllowed(),
                request.redemptionAllowed(),
                request.amoeAllowed(),
                request.requiresLegalReview(),
                request.status(),
                request.legalApprovalId()
        );
        var saved = regionRepository.save(region);
        auditService.write(operator, "region_update", "compliance_region", countryCode + "-" + stateCode, before, regionJson(saved), null);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<LegalDocumentDto> legalDocuments() {
        return documentRepository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional
    public LegalDocumentDto createLegalDocument(CreateLegalDocumentRequest request, AdminOperatorContext operator) {
        var document = documentRepository.save(new ComplianceDocument(
                request.documentType(),
                request.version(),
                request.title(),
                request.contentUrl(),
                "draft",
                request.legalApprovalId(),
                clock.instant()
        ));
        auditService.write(operator, "legal_document_create", "compliance_document", targetId(document), null, documentJson(document), null);
        return toDto(document);
    }

    @Transactional
    public LegalDocumentDto publishLegalDocument(String documentType, String version, AdminOperatorContext operator) {
        var document = documentRepository.findByDocumentTypeAndVersion(documentType, version)
                .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_FAILED, "Legal document does not exist.", Map.of("document_type", documentType, "version", version)));
        documentRepository.findByDocumentTypeAndStatus(documentType, "active").forEach(ComplianceDocument::archive);
        var before = documentJson(document);
        document.publish();
        var saved = documentRepository.save(document);
        auditService.write(operator, "legal_document_publish", "compliance_document", targetId(saved), before, documentJson(saved), null);
        return toDto(saved);
    }

    private RegionDto toDto(ComplianceRegion region) {
        return new RegionDto(
                region.getCountryCode(),
                region.getStateCode(),
                region.isRegistrationAllowed(),
                region.isGameAllowed(),
                region.isPurchaseAllowed(),
                region.isScGrantAllowed(),
                region.isRedemptionAllowed(),
                region.isAmoeAllowed(),
                region.isRequiresLegalReview(),
                region.getStatus(),
                region.getLegalApprovalId()
        );
    }

    private LegalDocumentDto toDto(ComplianceDocument document) {
        return new LegalDocumentDto(
                document.getDocumentType(),
                document.getVersion(),
                document.getTitle(),
                document.getContentUrl(),
                document.getStatus(),
                document.getLegalApprovalId()
        );
    }

    private String targetId(ComplianceDocument document) {
        return document.getDocumentType() + "/" + document.getVersion();
    }

    private String regionJson(ComplianceRegion region) {
        return "{\"status\":\"" + region.getStatus() + "\",\"purchaseAllowed\":" + region.isPurchaseAllowed() + "}";
    }

    private String documentJson(ComplianceDocument document) {
        return "{\"status\":\"" + document.getStatus() + "\",\"version\":\"" + document.getVersion() + "\"}";
    }
}
