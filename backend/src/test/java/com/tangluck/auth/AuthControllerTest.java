package com.tangluck.auth;

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
    void complianceDocumentsReturnsActiveDocuments() throws Exception {
        mockMvc.perform(get("/api/v1/compliance/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.documentType == 'terms')]").exists())
                .andExpect(jsonPath("$[?(@.documentType == 'sweepstakes_rules')]").exists())
                .andExpect(jsonPath("$[?(@.documentType == 'privacy')]").exists())
                .andExpect(jsonPath("$[?(@.documentType == 'amoe')]").exists());
    }
}
