package com.tangluck.promotion;

import com.tangluck.auth.AcceptedDocument;
import com.tangluck.auth.AuthService;
import com.tangluck.auth.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class PromotionControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    @Test
    void listsCampaignsAndClaimsDailyLogin() throws Exception {
        var userId = register("promo-controller@example.com");

        mockMvc.perform(get("/api/v1/campaigns"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.campaignCode == 'daily_login_v1')]").exists());

        mockMvc.perform(post("/api/v1/campaigns/daily_login_v1/claim")
                        .header("X-User-Id", userId)
                        .header("Idempotency-Key", "idem_controller_daily"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("granted"))
                .andExpect(jsonPath("$.rewards[0].currency").value("GC"));
    }

    @Test
    void dailyTaskCanBeClaimed() throws Exception {
        var userId = register("task-controller@example.com");

        mockMvc.perform(get("/api/v1/tasks/daily"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.taskCode == 'view_rules_task_v1')]").exists());

        mockMvc.perform(post("/api/v1/tasks/view_rules_task_v1/progress")
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"progress\": 1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("completed"));

        mockMvc.perform(post("/api/v1/tasks/view_rules_task_v1/claim")
                        .header("X-User-Id", userId)
                        .header("Idempotency-Key", "idem_controller_task"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rewards[0].currency").value("GC"));
    }

    @Test
    void couponCanBeClaimed() throws Exception {
        var userId = register("coupon-controller@example.com");

        mockMvc.perform(post("/api/v1/coupon/claim")
                        .header("X-User-Id", userId)
                        .header("Idempotency-Key", "idem_controller_coupon")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"WELCOME500\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("granted"))
                .andExpect(jsonPath("$.rewards[0].amount").value(500));
    }

    private Long register(String email) {
        return authService.register(new RegisterRequest(
                email,
                "StrongPass123!",
                LocalDate.of(1996, 4, 12),
                "US",
                "CA",
                List.of(
                        new AcceptedDocument("terms", "terms-v1"),
                        new AcceptedDocument("sweepstakes_rules", "rules-v1"),
                        new AcceptedDocument("privacy", "privacy-v1")
                ),
                "demo",
                "dev_" + email
        )).user().userId();
    }
}
