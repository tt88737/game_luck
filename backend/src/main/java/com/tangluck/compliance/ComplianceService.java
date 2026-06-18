package com.tangluck.compliance;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ComplianceService {
    private final ComplianceDocumentRepository documentRepository;

    public ComplianceService(ComplianceDocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
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
}
