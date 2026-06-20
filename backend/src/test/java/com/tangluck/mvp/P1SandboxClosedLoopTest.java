package com.tangluck.mvp;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class P1SandboxClosedLoopTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void sandboxPurchaseKycAndRedemptionCreateTheP1OperatingLoop() throws Exception {
        Long userId = registerUser("p1-loop@example.com");

        mockMvc.perform(get("/api/v1/purchase/packages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].packageCode").value("gc_499"))
                .andExpect(jsonPath("$[0].name").value("GC 5,000 Pack"))
                .andExpect(jsonPath("$[0].gcAmount").value(5000))
                .andExpect(jsonPath("$[0].sandboxOnly").value(false));

        String orderJson = mockMvc.perform(post("/api/v1/purchase/orders")
                        .header("X-User-Id", userId)
                        .header("Idempotency-Key", "p1-order-" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"packageCode\":\"gc_499\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("paid"))
                .andExpect(jsonPath("$.provider").value("manual"))
                .andExpect(jsonPath("$.currencyGranted").value("GC"))
                .andExpect(jsonPath("$.amountGranted").value(5000))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String orderId = objectMapper.readTree(orderJson).path("orderId").asText();

        mockMvc.perform(get("/api/v1/wallet/ledger")
                        .header("X-User-Id", userId)
                        .param("currency", "GC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.businessType == 'purchase' && @.businessId == '%s')]".formatted(orderId)).exists());

        mockMvc.perform(post("/api/v1/campaigns/register_bonus_v1/claim")
                        .header("X-User-Id", userId)
                        .header("Idempotency-Key", "p1-sc-" + userId))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/redemptions")
                        .header("X-User-Id", userId)
                        .header("Idempotency-Key", "p1-redemption-blocked-" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scAmount\":\"0.50\",\"method\":\"sandbox_gift_card\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("KYC_REQUIRED"));

        mockMvc.perform(get("/api/v1/kyc/status").header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("not_started"));

        mockMvc.perform(post("/api/v1/kyc/applications")
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"legalName\":\"P1 Demo User\",\"birthDate\":\"1994-03-02\",\"addressLine\":\"100 Demo Street\",\"stateCode\":\"CA\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("reviewing"));

        mockMvc.perform(post("/api/v1/admin/kyc/" + userId + "/approve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("approved"));

        String redemptionJson = mockMvc.perform(post("/api/v1/redemptions")
                        .header("X-User-Id", userId)
                        .header("Idempotency-Key", "p1-redemption-" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scAmount\":\"0.50\",\"method\":\"sandbox_gift_card\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("reviewing"))
                .andExpect(jsonPath("$.sandboxOnly").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String redemptionId = objectMapper.readTree(redemptionJson).path("redemptionId").asText();

        mockMvc.perform(get("/api/v1/admin/p1/operations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.purchaseOrders[?(@.orderId == '%s')]".formatted(orderId)).exists())
                .andExpect(jsonPath("$.kycApplications[?(@.userId == %d && @.status == 'approved')]".formatted(userId)).exists())
                .andExpect(jsonPath("$.redemptionRequests[?(@.redemptionId == '%s' && @.status == 'reviewing')]".formatted(redemptionId)).exists());
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
                  "utmSource": "p1",
                  "deviceId": "p1_loop_device"
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
