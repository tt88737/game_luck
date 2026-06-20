package com.tangluck.compliance;

import com.tangluck.common.api.BusinessException;
import com.tangluck.common.api.ErrorCode;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ComplianceService {
    private final ComplianceDocumentRepository documentRepository;
    private final ComplianceRegionRepository regionRepository;

    public ComplianceService(ComplianceDocumentRepository documentRepository, ComplianceRegionRepository regionRepository) {
        this.documentRepository = documentRepository;
        this.regionRepository = regionRepository;
    }

    public List<ComplianceDocumentDto> activeDocuments() {
        return documentRepository.findByStatusOrderByDocumentTypeAsc("active").stream()
                .map(document -> new ComplianceDocumentDto(
                        document.getDocumentType(),
                        document.getVersion(),
                        document.getTitle(),
                        document.getContentUrl()
                ))
                .toList();
    }

    public ComplianceRegion requireFeatureAllowed(String countryCode, String stateCode, String feature) {
        var region = regionRepository.findByCountryCodeAndStateCode(countryCode, stateCode)
                .orElseThrow(() -> blocked(stateCode, feature));
        var allowed = switch (feature) {
            case "registration" -> region.isRegistrationAllowed();
            case "game" -> region.isGameAllowed();
            case "purchase" -> region.isPurchaseAllowed();
            case "sc_grant" -> region.isScGrantAllowed();
            case "redemption" -> region.isRedemptionAllowed();
            case "amoe" -> region.isAmoeAllowed();
            default -> false;
        };
        if (!allowed || !"active".equals(region.getStatus())) {
            throw blocked(stateCode, feature);
        }
        return region;
    }

    private BusinessException blocked(String stateCode, String feature) {
        return new BusinessException(
                ErrorCode.REGION_BLOCKED,
                "This feature is not available in your region.",
                Map.of("state_code", stateCode, "feature", feature)
        );
    }
}
