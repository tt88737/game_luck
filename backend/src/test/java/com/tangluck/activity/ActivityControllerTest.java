package com.tangluck.activity;

import com.tangluck.auth.AcceptedDocument;
import com.tangluck.auth.AuthService;
import com.tangluck.auth.RegisterRequest;
import com.tangluck.slots.SlotDtos;
import com.tangluck.slots.SlotService;
import com.tangluck.wallet.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
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
class ActivityControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    @Autowired
    private WalletService walletService;

    @Autowired
    private SlotService slotService;

    @BeforeEach
    void resetSeedGame() {
        slotService.updateGame(
                "lucky_slots",
                new SlotDtos.UpdateSlotGameRequest("Lucky Slots", "active", new BigDecimal("1.0000"), new BigDecimal("100.0000"), 10, "LEGAL-SLOTS-001"),
                new com.tangluck.admin.AdminOperatorContext(1L, "test", java.util.Set.of("*"), "127.0.0.1")
        );
    }

    @Test
    void spinUpdatesSpinCountBetAmountAndWinAmountProgress() throws Exception {
        var userId = register("activity-progress@example.com");
        walletService.credit(userId, "GC", new BigDecimal("100.0000"), "test_seed", "activity-progress-seed", "activity-progress-seed");

        spin(userId, "activity-spin-1", "10.0000");
        spin(userId, "activity-spin-2", "25.0000");

        mockMvc.perform(get("/api/v1/player/activity-summary").header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tasks[?(@.taskCode == 'daily_spin_10')].progress").value(2.0000))
                .andExpect(jsonPath("$.tasks[?(@.taskCode == 'bet_5000_gc')].progress").value(35.0000))
                .andExpect(jsonPath("$.tasks[?(@.taskCode == 'win_1000_gc')].progress").value(20.0000));
    }

    @Test
    void claimCompletedTaskCreditsGcOnce() throws Exception {
        var userId = register("activity-claim@example.com");
        walletService.credit(userId, "GC", new BigDecimal("200.0000"), "test_seed", "activity-claim-seed", "activity-claim-seed");

        for (int i = 0; i < 10; i++) {
            spin(userId, "activity-claim-spin-" + i, "1.0000");
        }

        mockMvc.perform(post("/api/v1/player/tasks/daily_spin_10/claim").header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskCode").value("daily_spin_10"))
                .andExpect(jsonPath("$.status").value("completed"))
                .andExpect(jsonPath("$.rewardAmount").value(1000.0000));

        mockMvc.perform(post("/api/v1/player/tasks/daily_spin_10/claim").header("X-User-Id", userId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CLAIM_DUPLICATED"));

        mockMvc.perform(get("/api/v1/wallet/ledger")
                        .header("X-User-Id", userId)
                        .param("currency", "GC")
                        .param("businessType", "activity_task"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void adminActivityDashboardShowsCompletionAndGrantTotals() throws Exception {
        var userId = register("activity-admin@example.com");
        walletService.credit(userId, "GC", new BigDecimal("200.0000"), "test_seed", "activity-admin-seed", "activity-admin-seed");
        for (int i = 0; i < 10; i++) {
            spin(userId, "activity-admin-spin-" + i, "1.0000");
        }
        mockMvc.perform(post("/api/v1/player/tasks/daily_spin_10/claim").header("X-User-Id", userId))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/admin/activity-dashboard")
                        .header("X-Admin-Permissions", "activity.read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalParticipants").value(1))
                .andExpect(jsonPath("$.completedTasks").value(1))
                .andExpect(jsonPath("$.gcGranted").value(1000.0000))
                .andExpect(jsonPath("$.tasks[?(@.taskCode == 'daily_spin_10')].completedCount").value(1));
    }

    private void spin(Long userId, String idempotencyKey, String betAmount) throws Exception {
        mockMvc.perform(post("/api/v1/slots/lucky_slots/spin")
                        .header("X-User-Id", userId)
                        .header("X-Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currency\":\"GC\",\"betAmount\":\"%s\"}".formatted(betAmount)))
                .andExpect(status().isOk());
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
                "activity",
                "dev_" + email
        )).user().userId();
    }
}
