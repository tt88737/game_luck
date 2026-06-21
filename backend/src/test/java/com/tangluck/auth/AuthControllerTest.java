package com.tangluck.auth;

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
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registerReturnsUserWalletAndToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "controller@example.com",
                                  "password": "StrongPass123!",
                                  "birthDate": "1996-04-12",
                                  "countryCode": "US",
                                  "stateCode": "CA",
                                  "acceptedDocuments": [
                                    {"documentType": "terms", "version": "terms-v1"},
                                    {"documentType": "sweepstakes_rules", "version": "rules-v1"},
                                    {"documentType": "privacy", "version": "privacy-v1"}
                                  ],
                                  "utmSource": "demo",
                                  "deviceId": "dev_abc123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value("controller@example.com"))
                .andExpect(jsonPath("$.wallet.gcBalance").value(0))
                .andExpect(jsonPath("$.wallet.scBalance").value(0))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void loginReturnsUserWalletAndTokenForValidCredentials() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "login-user@example.com",
                                  "password": "StrongPass123!",
                                  "birthDate": "1996-04-12",
                                  "countryCode": "US",
                                  "stateCode": "CA",
                                  "acceptedDocuments": [
                                    {"documentType": "terms", "version": "terms-v1"},
                                    {"documentType": "sweepstakes_rules", "version": "rules-v1"},
                                    {"documentType": "privacy", "version": "privacy-v1"}
                                  ],
                                  "utmSource": "web",
                                  "deviceId": "dev_login"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "login-user@example.com",
                                  "password": "StrongPass123!"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value("login-user@example.com"))
                .andExpect(jsonPath("$.wallet.gcBalance").value(0))
                .andExpect(jsonPath("$.wallet.scBalance").value(0))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void loginRejectsInvalidCredentials() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "missing-user@example.com",
                                  "password": "WrongPass123!"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_INVALID_CREDENTIALS"));
    }

    @Test
    void guestAccountGetsWalletAndCanBeHydrated() throws Exception {
        var guestResponse = mockMvc.perform(post("/api/v1/auth/guest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "deviceId": "guest_device_1",
                                  "countryCode": "US",
                                  "stateCode": "CA",
                                  "utmSource": "guest_lobby"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountType").value("guest"))
                .andExpect(jsonPath("$.user.status").value("guest"))
                .andExpect(jsonPath("$.user.email").value(org.hamcrest.Matchers.startsWith("guest_")))
                .andExpect(jsonPath("$.wallet.gcBalance").value(0))
                .andExpect(jsonPath("$.wallet.scBalance").value(0))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var token = objectMapper.readTree(guestResponse).path("token").asText();

        mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountType").value("guest"))
                .andExpect(jsonPath("$.user.status").value("guest"))
                .andExpect(jsonPath("$.token").value(token));
    }

    @Test
    void bindEmailUpgradesGuestWithoutChangingUserId() throws Exception {
        var guestResponse = mockMvc.perform(post("/api/v1/auth/guest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "deviceId": "guest_device_bind",
                                  "countryCode": "US",
                                  "stateCode": "CA",
                                  "utmSource": "guest_lobby"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        var userId = objectMapper.readTree(guestResponse).path("user").path("userId").asLong();

        mockMvc.perform(post("/api/v1/auth/bind-email")
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "bound-guest@example.com",
                                  "password": "StrongPass123!",
                                  "birthDate": "1996-04-12",
                                  "countryCode": "US",
                                  "stateCode": "CA",
                                  "acceptedDocuments": [
                                    {"documentType": "terms", "version": "terms-v1"},
                                    {"documentType": "sweepstakes_rules", "version": "rules-v1"},
                                    {"documentType": "privacy", "version": "privacy-v1"}
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountType").value("formal"))
                .andExpect(jsonPath("$.user.userId").value(userId))
                .andExpect(jsonPath("$.user.email").value("bound-guest@example.com"))
                .andExpect(jsonPath("$.user.status").value("active"))
                .andExpect(jsonPath("$.wallet.gcBalance").value(0));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "bound-guest@example.com",
                                  "password": "StrongPass123!"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountType").value("formal"))
                .andExpect(jsonPath("$.user.userId").value(userId));
    }

    @Test
    void bindEmailRejectsDuplicateFormalEmail() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "duplicate-bind@example.com",
                                  "password": "StrongPass123!",
                                  "birthDate": "1996-04-12",
                                  "countryCode": "US",
                                  "stateCode": "CA",
                                  "acceptedDocuments": [
                                    {"documentType": "terms", "version": "terms-v1"},
                                    {"documentType": "sweepstakes_rules", "version": "rules-v1"},
                                    {"documentType": "privacy", "version": "privacy-v1"}
                                  ],
                                  "utmSource": "web",
                                  "deviceId": "duplicate_formal"
                                }
                                """))
                .andExpect(status().isOk());

        var guestResponse = mockMvc.perform(post("/api/v1/auth/guest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "deviceId": "guest_duplicate_bind",
                                  "countryCode": "US",
                                  "stateCode": "CA",
                                  "utmSource": "guest_lobby"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        var userId = objectMapper.readTree(guestResponse).path("user").path("userId").asLong();

        mockMvc.perform(post("/api/v1/auth/bind-email")
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "duplicate-bind@example.com",
                                  "password": "StrongPass123!",
                                  "birthDate": "1996-04-12",
                                  "countryCode": "US",
                                  "stateCode": "CA",
                                  "acceptedDocuments": [
                                    {"documentType": "terms", "version": "terms-v1"},
                                    {"documentType": "sweepstakes_rules", "version": "rules-v1"},
                                    {"documentType": "privacy", "version": "privacy-v1"}
                                  ]
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_EXISTS"));
    }

    @Test
    void meReturnsCurrentUserForBearerSessionToken() throws Exception {
        var registerResponse = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "me-user@example.com",
                                  "password": "StrongPass123!",
                                  "birthDate": "1996-04-12",
                                  "countryCode": "US",
                                  "stateCode": "CA",
                                  "acceptedDocuments": [
                                    {"documentType": "terms", "version": "terms-v1"},
                                    {"documentType": "sweepstakes_rules", "version": "rules-v1"},
                                    {"documentType": "privacy", "version": "privacy-v1"}
                                  ],
                                  "utmSource": "web",
                                  "deviceId": "dev_me"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var token = objectMapper.readTree(registerResponse).path("token").asText();

        mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value("me-user@example.com"))
                .andExpect(jsonPath("$.wallet.gcBalance").value(0))
                .andExpect(jsonPath("$.token").value(token));
    }

    @Test
    void meRejectsMissingSessionToken() throws Exception {
        mockMvc.perform(get("/api/v1/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_INVALID_CREDENTIALS"));
    }

    @Test
    void complianceDocumentsReturnsActiveDocuments() throws Exception {
        mockMvc.perform(get("/api/v1/compliance/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.documentType == 'terms')]").exists())
                .andExpect(jsonPath("$[?(@.documentType == 'sweepstakes_rules')]").exists())
                .andExpect(jsonPath("$[?(@.documentType == 'privacy')]").exists())
                .andExpect(jsonPath("$[?(@.documentType == 'amoe')]").exists());
    }
}
