package com.tangluck.mvp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class MvpClosedLoopTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void userRewardWalletAndAdminAuditCloseTheMvpLoop() throws Exception {
        String email = "mvp-loop@example.com";
        String registerJson = """
                {
                  "email": "%s",
                  "password": "StrongPass123!",
                  "birthDate": "1996-04-12",
                  "countryCode": "US",
                  "stateCode": "CA",
                  "acceptedDocuments": [
                    {"documentType": "terms", "version": "terms-v1"},
                    {"documentType": "sweepstakes_rules", "version": "rules-v1"},
                    {"documentType": "privacy", "version": "privacy-v1"}
                  ],
                  "utmSource": "mvp",
                  "deviceId": "mvp_loop_device"
                }
                """.formatted(email);

        String registerResponse = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value(email))
                .andExpect(jsonPath("$.wallet.gcBalance").value(0))
                .andExpect(jsonPath("$.wallet.scBalance").value(0))
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long userId = objectMapper.readTree(registerResponse).path("user").path("userId").asLong();

        mockMvc.perform(post("/api/v1/campaigns/register_bonus_v1/claim")
                        .header("X-User-Id", userId)
                        .header("Idempotency-Key", "mvp-welcome-" + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("granted"))
                .andExpect(jsonPath("$.rewards[?(@.currency == 'GC' && @.amount == 10000)]").exists())
                .andExpect(jsonPath("$.rewards[?(@.currency == 'SC' && @.amount == 0.50)]").exists());

        mockMvc.perform(post("/api/v1/coupon/claim")
                        .header("X-User-Id", userId)
                        .header("Idempotency-Key", "mvp-coupon-" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"WELCOME500\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("granted"))
                .andExpect(jsonPath("$.rewards[0].currency").value("GC"))
                .andExpect(jsonPath("$.rewards[0].amount").value(500));

        mockMvc.perform(get("/api/v1/wallet/summary").header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.wallet.gcBalance").value(10500.0000))
                .andExpect(jsonPath("$.wallet.scBalance").value(0.5000))
                .andExpect(jsonPath("$.scSourceSummary[?(@.source == 'register')]").exists());

        mockMvc.perform(get("/api/v1/wallet/ledger")
                        .header("X-User-Id", userId)
                        .param("currency", "GC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.items[?(@.businessType == 'coupon')]").exists())
                .andExpect(jsonPath("$.items[?(@.businessType == 'register')]").exists());

        String campaignCode = "mvp_loop_campaign";
        String campaignJson = """
                {
                  "campaignCode": "%s",
                  "name": "MVP Loop Campaign",
                  "campaignType": "daily_login",
                  "eligibleRegions": ["CA"],
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
                """.formatted(campaignCode);

        mockMvc.perform(post("/api/v1/admin/campaigns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(campaignJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("draft"));

        mockMvc.perform(post("/api/v1/admin/campaigns/" + campaignCode + "/publish"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("active"));

        mockMvc.perform(post("/api/v1/admin/campaigns/" + campaignCode + "/pause"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("paused"));

        String auditJson = mockMvc.perform(get("/api/v1/admin/audit-logs")
                        .param("target_type", "promotion_campaign")
                        .param("target_id", campaignCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.action == 'campaign_publish')]").exists())
                .andExpect(jsonPath("$[?(@.action == 'campaign_pause')]").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode auditLogs = objectMapper.readTree(auditJson);
        assert auditLogs.size() >= 2;
    }
}
