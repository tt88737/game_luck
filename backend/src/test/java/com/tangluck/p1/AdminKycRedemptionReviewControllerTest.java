package com.tangluck.p1;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AdminKycRedemptionReviewControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void adminCanRejectKycAndUserCanResubmitForReview() throws Exception {
        Long userId = registerUser("kyc-reject-loop@example.com");
        submitKyc(userId, "KYC Reject User");

        mockMvc.perform(post("/api/v1/admin/kyc/" + userId + "/reject")
                        .header("X-Admin-Operator-Id", "81")
                        .header("X-Admin-Operator-Role", "kyc_admin")
                        .header("X-Admin-Permissions", "kyc.review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Document image is unreadable.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("rejected"))
                .andExpect(jsonPath("$.reviewReason").value("Document image is unreadable."));

        mockMvc.perform(get("/api/v1/kyc/status").header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("rejected"))
                .andExpect(jsonPath("$.reviewReason").value("Document image is unreadable."));

        submitKyc(userId, "KYC Resubmitted User")
                .andExpect(jsonPath("$.status").value("reviewing"))
                .andExpect(jsonPath("$.reviewReason").doesNotExist());

        mockMvc.perform(get("/api/v1/admin/audit-logs")
                        .param("target_type", "kyc_application")
                        .param("target_id", String.valueOf(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.action == 'kyc_reject' && @.operatorId == 81)]").exists());
    }

    @Test
    void adminCanRejectOrPayoutRedemptionWithWalletLedgerAndAudit() throws Exception {
        Long rejectUserId = registerUser("redemption-reject-loop@example.com");
        approveKycWithScBalance(rejectUserId);
        String rejectedRedemptionId = createRedemption(rejectUserId, "redemption-reject", "0.0100");

        mockMvc.perform(get("/api/v1/wallet/summary").header("X-User-Id", rejectUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.wallet.scFrozen").value(0.0100));

        mockMvc.perform(post("/api/v1/admin/redemptions/" + rejectedRedemptionId + "/reject")
                        .header("X-Admin-Operator-Id", "82")
                        .header("X-Admin-Operator-Role", "redemption_admin")
                        .header("X-Admin-Permissions", "redemption.review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Payment account mismatch.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("rejected"))
                .andExpect(jsonPath("$.reviewReason").value("Payment account mismatch."));

        mockMvc.perform(get("/api/v1/wallet/summary").header("X-User-Id", rejectUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.wallet.scFrozen").value(0.0000))
                .andExpect(jsonPath("$.wallet.scRedeemable").value(0.0500));

        Long payoutUserId = registerUser("redemption-payout-loop@example.com");
        approveKycWithScBalance(payoutUserId);
        String paidRedemptionId = createRedemption(payoutUserId, "redemption-payout", "0.0100");

        mockMvc.perform(post("/api/v1/admin/redemptions/" + paidRedemptionId + "/approve")
                        .header("X-Admin-Permissions", "redemption.review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Approved for payout.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("payout_pending"));

        mockMvc.perform(post("/api/v1/admin/redemptions/" + paidRedemptionId + "/mark-paid")
                        .header("X-Admin-Operator-Id", "83")
                        .header("X-Admin-Operator-Role", "redemption_admin")
                        .header("X-Admin-Permissions", "redemption.payout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"providerReference\":\"paypal-payout-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("paid"))
                .andExpect(jsonPath("$.providerReference").value("paypal-payout-1"));

        mockMvc.perform(post("/api/v1/admin/redemptions/" + paidRedemptionId + "/mark-paid")
                        .header("X-Admin-Permissions", "redemption.payout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"providerReference\":\"paypal-payout-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("paid"));

        mockMvc.perform(get("/api/v1/wallet/summary").header("X-User-Id", payoutUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.wallet.scBalance").value(0.0400))
                .andExpect(jsonPath("$.wallet.scFrozen").value(0.0000))
                .andExpect(jsonPath("$.wallet.scRedeemable").value(0.0400));

        mockMvc.perform(get("/api/v1/wallet/ledger")
                        .header("X-User-Id", payoutUserId)
                        .param("currency", "SC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.businessType == 'redemption_payout' && @.businessId == '%s')]".formatted(paidRedemptionId)).exists())
                .andExpect(jsonPath("$.total").value(3));

        mockMvc.perform(get("/api/v1/admin/audit-logs")
                        .param("target_type", "redemption_request")
                        .param("target_id", paidRedemptionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.action == 'redemption_mark_paid' && @.operatorId == 83)]").exists());
    }

    private org.springframework.test.web.servlet.ResultActions submitKyc(Long userId, String legalName) throws Exception {
        return mockMvc.perform(post("/api/v1/kyc/applications")
                .header("X-User-Id", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "legalName": "%s",
                          "birthDate": "1990-01-01",
                          "addressLine": "100 Main Street",
                          "stateCode": "CA"
                        }
                        """.formatted(legalName)))
                .andExpect(status().isOk());
    }

    private void approveKycWithScBalance(Long userId) throws Exception {
        submitKyc(userId, "Redemption User");
        mockMvc.perform(post("/api/v1/admin/kyc/" + userId + "/approve")
                        .header("X-Admin-Permissions", "kyc.review"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/campaigns/daily_login_v1/claim")
                        .header("X-User-Id", userId)
                        .header("Idempotency-Key", "sc-balance-" + userId))
                .andExpect(status().isOk());
    }

    private String createRedemption(Long userId, String idempotencyKey, String amount) throws Exception {
        String response = mockMvc.perform(post("/api/v1/redemptions")
                        .header("X-User-Id", userId)
                        .header("Idempotency-Key", idempotencyKey + "-" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scAmount\":\"%s\",\"method\":\"paypal\"}".formatted(amount)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("reviewing"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).path("redemptionId").asText();
    }

    private Long registerUser(String email) throws Exception {
        String registerJson = """
                {
                  "email": "%s",
                  "password": "StrongPass123!",
                  "birthDate": "1994-03-02",
                  "countryCode": "US",
                  "stateCode": "CA",
                  "acceptedDocuments": [
                    {"documentType": "terms", "version": "terms-v1"},
                    {"documentType": "sweepstakes_rules", "version": "rules-v1"},
                    {"documentType": "privacy", "version": "privacy-v1"}
                  ],
                  "utmSource": "review_loop",
                  "deviceId": "review_loop_device"
                }
                """.formatted(email);

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
