package com.tangluck.compliance;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AdminComplianceControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void adminUpdatesRegionAndWritesAudit() throws Exception {
        mockMvc.perform(get("/api/v1/admin/regions")
                        .header("X-Admin-Permissions", "compliance.read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.stateCode == 'CA')]").exists());

        mockMvc.perform(patch("/api/v1/admin/regions/US/CA")
                        .header("X-Admin-Operator-Id", "21")
                        .header("X-Admin-Operator-Role", "compliance_admin")
                        .header("X-Admin-Permissions", "compliance.write")
                        .header("X-Forwarded-For", "203.0.113.21")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "registrationAllowed": true,
                                  "gameAllowed": true,
                                  "purchaseAllowed": false,
                                  "scGrantAllowed": true,
                                  "redemptionAllowed": true,
                                  "amoeAllowed": true,
                                  "requiresLegalReview": false,
                                  "status": "active",
                                  "legalApprovalId": "LEGAL-CA-UPDATED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stateCode").value("CA"))
                .andExpect(jsonPath("$.purchaseAllowed").value(false))
                .andExpect(jsonPath("$.legalApprovalId").value("LEGAL-CA-UPDATED"));

        mockMvc.perform(get("/api/v1/admin/audit-logs")
                        .param("target_type", "compliance_region")
                        .param("target_id", "US-CA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.action == 'region_update' && @.operatorId == 21 && @.operatorRole == 'compliance_admin' && @.ip == '203.0.113.21')]").exists());
    }

    @Test
    void regionPurchaseSwitchBlocksPurchaseOrder() throws Exception {
        Long userId = registerUser("region-purchase-block@example.com", "CA");

        mockMvc.perform(patch("/api/v1/admin/regions/US/CA")
                        .header("X-Admin-Permissions", "compliance.write")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "registrationAllowed": true,
                                  "gameAllowed": true,
                                  "purchaseAllowed": false,
                                  "scGrantAllowed": true,
                                  "redemptionAllowed": true,
                                  "amoeAllowed": true,
                                  "requiresLegalReview": false,
                                  "status": "active",
                                  "legalApprovalId": "LEGAL-CA-PURCHASE-OFF"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/purchase/orders")
                        .header("X-User-Id", userId)
                        .header("Idempotency-Key", "region-blocked-order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"packageCode\":\"gc_499\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("REGION_BLOCKED"))
                .andExpect(jsonPath("$.details.feature").value("purchase"));
    }

    @Test
    void adminPublishesLegalDocumentVersionAndPublicDocumentsUseActiveVersion() throws Exception {
        mockMvc.perform(post("/api/v1/admin/legal-documents")
                        .header("X-Admin-Operator-Id", "22")
                        .header("X-Admin-Operator-Role", "legal_admin")
                        .header("X-Admin-Permissions", "legal.write")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "documentType": "privacy",
                                  "version": "privacy-v2",
                                  "title": "Privacy Policy v2",
                                  "contentUrl": "/legal/privacy-v2",
                                  "legalApprovalId": "LEGAL-PRIVACY-V2"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("draft"));

        mockMvc.perform(post("/api/v1/admin/legal-documents/privacy/privacy-v2/publish")
                        .header("X-Admin-Operator-Id", "22")
                        .header("X-Admin-Operator-Role", "legal_admin")
                        .header("X-Admin-Permissions", "legal.publish"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("active"));

        mockMvc.perform(get("/api/v1/compliance/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.documentType == 'privacy' && @.version == 'privacy-v2' && @.title == 'Privacy Policy v2')]").exists());

        mockMvc.perform(get("/api/v1/admin/audit-logs")
                        .param("target_type", "compliance_document")
                        .param("target_id", "privacy/privacy-v2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.action == 'legal_document_publish' && @.operatorId == 22)]").exists());
    }

    private Long registerUser(String email, String stateCode) throws Exception {
        String registerJson = """
                {
                  "email": "%s",
                  "password": "StrongPass123!",
                  "birthDate": "1994-03-02",
                  "countryCode": "US",
                  "stateCode": "%s",
                  "acceptedDocuments": [
                    {"documentType": "terms", "version": "terms-v1"},
                    {"documentType": "sweepstakes_rules", "version": "rules-v1"},
                    {"documentType": "privacy", "version": "privacy-v1"}
                  ],
                  "utmSource": "p1",
                  "deviceId": "region_device"
                }
                """.formatted(email, stateCode);

        String registerResponse = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(registerResponse).path("user").path("userId").asLong();
    }
}
