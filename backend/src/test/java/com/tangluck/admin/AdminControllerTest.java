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
    void dashboardSummaryReturnsMetrics() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registrations").exists())
                .andExpect(jsonPath("$.claims").exists())
                .andExpect(jsonPath("$.scGranted").exists())
                .andExpect(jsonPath("$.riskEvents").exists());
    }
}
