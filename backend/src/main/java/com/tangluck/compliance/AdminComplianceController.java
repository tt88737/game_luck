package com.tangluck.compliance;

import com.tangluck.admin.AdminOperatorContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.tangluck.compliance.AdminComplianceDtos.CreateLegalDocumentRequest;
import static com.tangluck.compliance.AdminComplianceDtos.LegalDocumentDto;
import static com.tangluck.compliance.AdminComplianceDtos.RegionDto;
import static com.tangluck.compliance.AdminComplianceDtos.UpdateRegionRequest;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminComplianceController {
    private final AdminComplianceService complianceService;

    public AdminComplianceController(AdminComplianceService complianceService) {
        this.complianceService = complianceService;
    }

    @GetMapping("/regions")
    public List<RegionDto> regions(HttpServletRequest servletRequest) {
        AdminOperatorContext.from(servletRequest).require("compliance.read");
        return complianceService.regions();
    }

    @PatchMapping("/regions/{countryCode}/{stateCode}")
    public RegionDto updateRegion(
            @PathVariable String countryCode,
            @PathVariable String stateCode,
            @RequestBody UpdateRegionRequest request,
            HttpServletRequest servletRequest
    ) {
        var operator = AdminOperatorContext.from(servletRequest);
        operator.require("compliance.write");
        return complianceService.updateRegion(countryCode, stateCode, request, operator);
    }

    @GetMapping("/legal-documents")
    public List<LegalDocumentDto> legalDocuments(HttpServletRequest servletRequest) {
        AdminOperatorContext.from(servletRequest).require("legal.read");
        return complianceService.legalDocuments();
    }

    @PostMapping("/legal-documents")
    public LegalDocumentDto createLegalDocument(@RequestBody CreateLegalDocumentRequest request, HttpServletRequest servletRequest) {
        var operator = AdminOperatorContext.from(servletRequest);
        operator.require("legal.write");
        return complianceService.createLegalDocument(request, operator);
    }

    @PostMapping("/legal-documents/{documentType}/{version}/publish")
    public LegalDocumentDto publishLegalDocument(
            @PathVariable String documentType,
            @PathVariable String version,
            HttpServletRequest servletRequest
    ) {
        var operator = AdminOperatorContext.from(servletRequest);
        operator.require("legal.publish");
        return complianceService.publishLegalDocument(documentType, version, operator);
    }
}
