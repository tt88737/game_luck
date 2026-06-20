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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AdminPurchaseOperationsControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void adminCanDisablePackageAndStoreHidesIt() throws Exception {
        mockMvc.perform(get("/api/v1/admin/product-packages")
                        .header("X-Admin-Permissions", "package.read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.packageCode == 'gc_999')]").exists());

        mockMvc.perform(patch("/api/v1/admin/product-packages/gc_999")
                        .header("X-Admin-Operator-Id", "51")
                        .header("X-Admin-Operator-Role", "commerce_admin")
                        .header("X-Admin-Permissions", "package.write")
                        .header("X-Forwarded-For", "203.0.113.51")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "GC 12,000 Pack",
                                  "priceAmount": "9.9900",
                                  "priceCurrency": "USD",
                                  "gcAmount": "12000.0000",
                                  "status": "paused",
                                  "provider": "manual",
                                  "sortOrder": 10,
                                  "legalApprovalId": "LEGAL-PACKAGE-499"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("paused"));

        mockMvc.perform(get("/api/v1/purchase/packages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.packageCode == 'gc_999')]").doesNotExist());

        mockMvc.perform(get("/api/v1/admin/audit-logs")
                        .param("target_type", "product_package")
                        .param("target_id", "gc_999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.action == 'product_package_update' && @.operatorId == 51)]").exists());
    }

    @Test
    void orderIsPaymentPendingUntilAdminMarksPaidAndLedgerIsPostedOnce() throws Exception {
        enableCaPurchase();
        Long userId = registerUser("purchase-admin-loop@example.com");

        String orderJson = mockMvc.perform(post("/api/v1/purchase/orders")
                        .header("X-User-Id", userId)
                        .header("Idempotency-Key", "admin-order-" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"packageCode\":\"gc_499\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("payment_pending"))
                .andExpect(jsonPath("$.provider").value("manual"))
                .andExpect(jsonPath("$.currencyGranted").value("GC"))
                .andExpect(jsonPath("$.amountGranted").value(0))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String orderId = objectMapper.readTree(orderJson).path("orderId").asText();

        mockMvc.perform(get("/api/v1/wallet/ledger")
                        .header("X-User-Id", userId)
                        .param("currency", "GC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.businessType == 'purchase' && @.businessId == '%s')]".formatted(orderId)).doesNotExist());

        mockMvc.perform(post("/api/v1/admin/purchase-orders/" + orderId + "/mark-paid")
                        .header("X-Admin-Operator-Id", "52")
                        .header("X-Admin-Operator-Role", "commerce_admin")
                        .header("X-Admin-Permissions", "order.settle")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"providerReference\":\"manual-settlement-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("paid"))
                .andExpect(jsonPath("$.amountGranted").value(5000));

        mockMvc.perform(post("/api/v1/admin/purchase-orders/" + orderId + "/mark-paid")
                        .header("X-Admin-Permissions", "order.settle")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"providerReference\":\"manual-settlement-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("paid"));

        mockMvc.perform(get("/api/v1/wallet/ledger")
                        .header("X-User-Id", userId)
                        .param("currency", "GC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.businessType == 'purchase' && @.businessId == '%s')]".formatted(orderId)).exists())
                .andExpect(jsonPath("$.total").value(1));

        mockMvc.perform(get("/api/v1/admin/audit-logs")
                        .param("target_type", "purchase_order")
                        .param("target_id", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.action == 'purchase_order_mark_paid' && @.operatorId == 52)]").exists());
    }

    private void enableCaPurchase() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/regions/US/CA")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "registrationAllowed": true,
                                  "gameAllowed": true,
                                  "purchaseAllowed": true,
                                  "scGrantAllowed": true,
                                  "redemptionAllowed": true,
                                  "amoeAllowed": true,
                                  "requiresLegalReview": false,
                                  "status": "active",
                                  "legalApprovalId": "LEGAL-CA-PURCHASE-ON"
                                }
                                """))
                .andExpect(status().isOk());
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
                  "utmSource": "purchase_admin",
                  "deviceId": "purchase_admin_device"
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
