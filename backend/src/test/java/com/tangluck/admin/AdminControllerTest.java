package com.tangluck.admin;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class AdminControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void createsPublishesPausesAndAuditsCampaign() throws Exception {
        mockMvc.perform(post("/api/v1/admin/campaigns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "campaignCode": "admin_api_campaign",
                                  "name": "Admin API Campaign",
                                  "campaignType": "daily_login",
                                  "eligibleRegions": ["CA", "TX"],
                                  "blockedRegions": ["WA"],
                                  "rewardPolicy": [
                                    {"currency": "GC", "amount": "1000"},
                                    {"currency": "SC", "amount": "0.05"}
                                  ],
                                  "scStrategy": "default_small_sc",
                                  "rulesVersion": "rules-v1",
                                  "legalApprovalId": "LEGAL-2026-0617-CA",
                                  "riskAction": "gc_only"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("draft"));

        mockMvc.perform(post("/api/v1/admin/campaigns/admin_api_campaign/publish"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("active"));

        mockMvc.perform(post("/api/v1/admin/campaigns/admin_api_campaign/pause"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("paused"));

        mockMvc.perform(get("/api/v1/admin/audit-logs")
                        .param("target_type", "promotion_campaign")
                        .param("target_id", "admin_api_campaign"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.action == 'campaign_publish')]").exists())
                .andExpect(jsonPath("$[?(@.action == 'campaign_pause')]").exists());
    }

    @Test
    void adminWriteActionsUseOperatorHeadersForAudit() throws Exception {
        mockMvc.perform(post("/api/v1/admin/campaigns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "campaignCode": "operator_header_campaign",
                                  "name": "Operator Header Campaign",
                                  "campaignType": "daily_login",
                                  "eligibleRegions": ["CA"],
                                  "blockedRegions": [],
                                  "rewardPolicy": [{"currency": "GC", "amount": "1000"}],
                                  "scStrategy": "gc_only",
                                  "rulesVersion": "rules-v1",
                                  "legalApprovalId": "",
                                  "riskAction": "pass"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/campaigns/operator_header_campaign/publish")
                        .header("X-Admin-Operator-Id", "42")
                        .header("X-Admin-Operator-Role", "legal_admin")
                        .header("X-Admin-Permissions", "campaign.publish")
                        .header("X-Forwarded-For", "203.0.113.9"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/admin/audit-logs")
                        .param("target_type", "promotion_campaign")
                        .param("target_id", "operator_header_campaign"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.action == 'campaign_publish' && @.operatorId == 42 && @.operatorRole == 'legal_admin' && @.ip == '203.0.113.9')]").exists());
    }

    @Test
    void adminWriteActionsRequirePermission() throws Exception {
        mockMvc.perform(post("/api/v1/admin/campaigns/seed_register_bonus/publish")
                        .header("X-Admin-Permissions", "campaign.read"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ADMIN_PERMISSION_DENIED"));
    }

    @Test
    void kycApprovalUsesOperatorHeadersForAuditAndPermission() throws Exception {
        mockMvc.perform(post("/api/v1/kyc/applications")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "legalName": "Audit User",
                                  "birthDate": "1990-01-01",
                                  "addressLine": "100 Main Street",
                                  "stateCode": "CA"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/kyc/1/approve")
                        .header("X-Admin-Permissions", "kyc.read"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ADMIN_PERMISSION_DENIED"));

        mockMvc.perform(post("/api/v1/admin/kyc/1/approve")
                        .header("X-Admin-Operator-Id", "77")
                        .header("X-Admin-Operator-Role", "kyc_admin")
                        .header("X-Admin-Permissions", "kyc.review")
                        .header("X-Forwarded-For", "198.51.100.8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("approved"));

        mockMvc.perform(get("/api/v1/admin/audit-logs")
                        .param("target_type", "kyc_application")
                        .param("target_id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.action == 'kyc_approve' && @.operatorId == 77 && @.operatorRole == 'kyc_admin' && @.ip == '198.51.100.8')]").exists());
    }

    @Test
    void dashboardSummaryReturnsMetrics() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registrations").exists())
                .andExpect(jsonPath("$.claims").exists())
                .andExpect(jsonPath("$.scGranted").exists())
                .andExpect(jsonPath("$.riskEvents").exists());
    }
}
