package com.tangluck.slots;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tangluck.auth.AcceptedDocument;
import com.tangluck.auth.AuthService;
import com.tangluck.auth.RegisterRequest;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class SlotControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    @Autowired
    private WalletService walletService;

    @Autowired
    private ObjectMapper objectMapper;

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
    void activeGamesAreVisibleToPlayers() throws Exception {
        mockMvc.perform(get("/api/v1/slots/games"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.gameCode == 'lucky_slots' && @.status == 'active')]").exists())
                .andExpect(jsonPath("$[?(@.gameCode == 'lucky_slots')].reelCount").value(5));
    }

    @Test
    void spinDebitsGcCreditsPayoutAndRecordsRound() throws Exception {
        var userId = register("slots-spin@example.com");
        walletService.credit(userId, "GC", new BigDecimal("100.0000"), "test_seed", "slots-spin-seed", "slots-spin-seed");

        var response = mockMvc.perform(post("/api/v1/slots/lucky_slots/spin")
                        .header("X-User-Id", userId)
                        .header("X-Idempotency-Key", "slot-spin-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currency\":\"GC\",\"betAmount\":\"10.0000\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gameCode").value("lucky_slots"))
                .andExpect(jsonPath("$.betAmount").value(10.0000))
                .andExpect(jsonPath("$.payoutAmount").value(20.0000))
                .andExpect(jsonPath("$.multiplier").value(2.0000))
                .andExpect(jsonPath("$.status").value("settled"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        var roundId = objectMapper.readTree(response).path("roundId").asText();

        mockMvc.perform(get("/api/v1/slots/rounds").header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].roundId").value(roundId))
                .andExpect(jsonPath("$.items[0].debitLedgerId").isNumber())
                .andExpect(jsonPath("$.items[0].creditLedgerId").isNumber());

        mockMvc.perform(get("/api/v1/wallet/ledger")
                        .header("X-User-Id", userId)
                        .param("currency", "GC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.businessType == 'slot_bet' && @.businessId == '%s')]".formatted(roundId)).exists())
                .andExpect(jsonPath("$.items[?(@.businessType == 'slot_payout' && @.businessId == '%s')]".formatted(roundId)).exists());
    }

    @Test
    void insufficientGcDoesNotCreateRound() throws Exception {
        var userId = register("slots-insufficient@example.com");

        mockMvc.perform(post("/api/v1/slots/lucky_slots/spin")
                        .header("X-User-Id", userId)
                        .header("X-Idempotency-Key", "slot-spin-insufficient")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currency\":\"GC\",\"betAmount\":\"10.0000\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        mockMvc.perform(get("/api/v1/slots/rounds").header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void adminCanPauseGameAndAuditIsWritten() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/games/lucky_slots")
                        .header("X-Admin-Operator-Id", "77")
                        .header("X-Admin-Operator-Role", "game_admin")
                        .header("X-Admin-Permissions", "game.write")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Lucky Slots",
                                  "status": "paused",
                                  "minBet": "1.0000",
                                  "maxBet": "100.0000",
                                  "sortOrder": 10,
                                  "legalApprovalId": "LEGAL-SLOTS-001"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("paused"));

        mockMvc.perform(get("/api/v1/slots/games"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.gameCode == 'lucky_slots')]").doesNotExist());

        mockMvc.perform(get("/api/v1/admin/audit-logs")
                        .param("target_type", "slot_game")
                        .param("target_id", "lucky_slots"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.action == 'slot_game_update' && @.operatorId == 77)]").exists());
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
                "slots",
                "dev_" + email
        )).user().userId();
    }
}
