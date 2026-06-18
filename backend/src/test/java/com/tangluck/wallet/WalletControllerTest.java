package com.tangluck.wallet;

import com.tangluck.auth.AcceptedDocument;
import com.tangluck.auth.AuthService;
import com.tangluck.auth.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class WalletControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    @Autowired
    private WalletService walletService;

    @Test
    void summaryReturnsWalletAndScSources() throws Exception {
        var userId = register("wallet-controller-summary@example.com");
        walletService.credit(userId, "SC", new BigDecimal("0.50"), "register_bonus", "claim_api_1", "idem_api_sc_1");

        mockMvc.perform(get("/api/v1/wallet/summary").header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.wallet.scBalance").value(0.50))
                .andExpect(jsonPath("$.scSourceSummary[0].source").value("register_bonus"));
    }

    @Test
    void ledgerReturnsPagedRows() throws Exception {
        var userId = register("wallet-controller-ledger@example.com");
        walletService.credit(userId, "SC", new BigDecimal("0.50"), "register_bonus", "claim_api_2", "idem_api_sc_2");

        mockMvc.perform(get("/api/v1/wallet/ledger")
                        .header("X-User-Id", userId)
                        .param("currency", "SC")
                        .param("page", "1")
                        .param("page_size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].currency").value("SC"))
                .andExpect(jsonPath("$.total").value(1));
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
