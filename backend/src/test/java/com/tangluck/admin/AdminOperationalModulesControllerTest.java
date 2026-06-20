package com.tangluck.admin;

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
class AdminOperationalModulesControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void adminUsersAndWalletLedgerExposeRealOperationalRecords() throws Exception {
        Long userId = registerUser("admin-user-module@example.com");

        mockMvc.perform(post("/api/v1/campaigns/daily_login_v1/claim")
                        .header("X-User-Id", userId)
                        .header("Idempotency-Key", "admin-user-module-claim"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/admin/users")
                        .header("X-Admin-Permissions", "user.read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.userId == %s && @.email == 'admin-user-module@example.com' && @.status == 'active')]".formatted(userId)).exists());

        mockMvc.perform(get("/api/v1/admin/wallet-ledger")
                        .header("X-Admin-Permissions", "wallet.read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.userId == %s && @.businessType == 'daily_login' && @.status == 'posted')]".formatted(userId)).exists());
    }

    @Test
    void adminKycAndRedemptionModulesExposeReviewQueues() throws Exception {
        Long userId = registerUser("admin-review-module@example.com");

        mockMvc.perform(post("/api/v1/kyc/applications")
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "legalName": "Review User",
                                  "birthDate": "1990-01-01",
                                  "addressLine": "100 Main Street",
                                  "stateCode": "CA"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("reviewing"));

        mockMvc.perform(get("/api/v1/admin/kyc-applications")
                        .header("X-Admin-Permissions", "kyc.read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.userId == %s && @.legalName == 'Review User' && @.status == 'reviewing')]".formatted(userId)).exists());

        mockMvc.perform(post("/api/v1/admin/kyc/" + userId + "/approve")
                        .header("X-Admin-Permissions", "kyc.review"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("approved"));

        mockMvc.perform(post("/api/v1/campaigns/daily_login_v1/claim")
                        .header("X-User-Id", userId)
                        .header("Idempotency-Key", "admin-review-module-claim"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/redemptions")
                        .header("X-User-Id", userId)
                        .header("Idempotency-Key", "admin-review-module-redemption")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scAmount\":\"0.0100\",\"method\":\"paypal\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("reviewing"));

        mockMvc.perform(get("/api/v1/admin/redemptions")
                        .header("X-Admin-Permissions", "redemption.read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.userId == %s && @.method == 'paypal' && @.status == 'reviewing')]".formatted(userId)).exists());
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
                  "utmSource": "admin_ops",
                  "deviceId": "admin_ops_device"
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
