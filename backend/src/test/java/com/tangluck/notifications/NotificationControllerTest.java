package com.tangluck.notifications;

import com.tangluck.auth.AcceptedDocument;
import com.tangluck.auth.AuthService;
import com.tangluck.auth.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
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
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class NotificationControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    @Test
    void playerCanClaimInboxRewardOnce() throws Exception {
        var userId = register("inbox-claim@example.com");
        issueManualGrant(userId, "500.0000", "retention_bonus");

        var id = firstInboxId(userId);
        mockMvc.perform(post("/api/v1/player/notifications/" + id + "/claim").header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("claimed"))
                .andExpect(jsonPath("$.rewardAmount").value(500.0000))
                .andExpect(jsonPath("$.ledgerId").isNumber());

        mockMvc.perform(post("/api/v1/player/notifications/" + id + "/claim").header("X-User-Id", userId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CLAIM_DUPLICATED"));
    }

    @Test
    void expiredInboxRewardCannotBeClaimed() throws Exception {
        var userId = register("inbox-expired@example.com");
        issueManualGrant(userId, "300.0000", "expired_bonus");
        var id = firstInboxId(userId);

        mockMvc.perform(post("/api/v1/admin/notifications/" + id + "/expire")
                        .header("X-Admin-Permissions", "notification.write"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("expired"));

        mockMvc.perform(post("/api/v1/player/notifications/" + id + "/claim").header("X-User-Id", userId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void adminCanIssueManualGrantAndAuditIsWritten() throws Exception {
        var userId = register("inbox-admin@example.com");

        mockMvc.perform(post("/api/v1/admin/notifications/manual-grant")
                        .header("X-Admin-Operator-Id", "91")
                        .header("X-Admin-Operator-Role", "crm_admin")
                        .header("X-Admin-Permissions", "notification.write")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": %d,
                                  "title": "Retention bonus",
                                  "message": "Claim your GC reward.",
                                  "rewardCurrency": "GC",
                                  "rewardAmount": "750.0000",
                                  "sourceType": "manual_grant",
                                  "sourceId": "crm-ticket-1001"
                                }
                                """.formatted(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Retention bonus"))
                .andExpect(jsonPath("$.status").value("claimable"));

        mockMvc.perform(get("/api/v1/admin/notifications")
                        .header("X-Admin-Permissions", "notification.read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.userId == %d && @.rewardAmount == 750.0000)]".formatted(userId)).exists());

        mockMvc.perform(get("/api/v1/admin/audit-logs")
                        .param("target_type", "reward_inbox")
                        .param("target_id", "crm-ticket-1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.action == 'reward_inbox_manual_grant' && @.operatorId == 91)]").exists());
    }

    private void issueManualGrant(Long userId, String amount, String sourceId) throws Exception {
        mockMvc.perform(post("/api/v1/admin/notifications/manual-grant")
                        .header("X-Admin-Permissions", "notification.write")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": %d,
                                  "title": "Inbox reward",
                                  "message": "Claim your reward.",
                                  "rewardCurrency": "GC",
                                  "rewardAmount": "%s",
                                  "sourceType": "manual_grant",
                                  "sourceId": "%s"
                                }
                                """.formatted(userId, amount, sourceId)))
                .andExpect(status().isOk());
    }

    private Long firstInboxId(Long userId) throws Exception {
        var response = mockMvc.perform(get("/api/v1/player/notifications").header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("claimable"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return com.fasterxml.jackson.databind.json.JsonMapper.builder().build().readTree(response).get(0).path("id").asLong();
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
                "notifications",
                "dev_" + email
        )).user().userId();
    }
}
