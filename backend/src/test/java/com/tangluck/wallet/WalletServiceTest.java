package com.tangluck.wallet;

import com.tangluck.auth.AuthService;
import com.tangluck.auth.AcceptedDocument;
import com.tangluck.auth.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class WalletServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private WalletService walletService;

    @Autowired
    private WalletLedgerRepository ledgerRepository;

    @Autowired
    private WalletAccountRepository accountRepository;

    @Test
    void creditsGcAndWritesLedger() {
        var userId = register("wallet-gc@example.com");

        var ledger = walletService.credit(userId, "GC", new BigDecimal("10000"), "register_bonus", "claim_1", "idem_gc_1");

        assertThat(ledger.amount()).isEqualByComparingTo("10000");
        assertThat(ledger.currency()).isEqualTo("GC");
        assertThat(accountRepository.findByUserIdAndCurrency(userId, "GC").orElseThrow().getBalance())
                .isEqualByComparingTo("10000");
        assertThat(ledgerRepository.findByUserId(userId)).hasSize(1);
    }

    @Test
    void creditsScWithDecimalPrecision() {
        var userId = register("wallet-sc@example.com");

        walletService.credit(userId, "SC", new BigDecimal("0.50"), "daily_login", "claim_2", "idem_sc_1");

        assertThat(accountRepository.findByUserIdAndCurrency(userId, "SC").orElseThrow().getBalance())
                .isEqualByComparingTo("0.50");
    }

    @Test
    void reusesIdempotencyKeyWithoutSecondLedger() {
        var userId = register("wallet-idem@example.com");

        var first = walletService.credit(userId, "GC", new BigDecimal("500"), "coupon", "coupon_1", "idem_coupon_1");
        var second = walletService.credit(userId, "GC", new BigDecimal("500"), "coupon", "coupon_1", "idem_coupon_1");

        assertThat(second.ledgerId()).isEqualTo(first.ledgerId());
        assertThat(ledgerRepository.findByUserId(userId)).hasSize(1);
        assertThat(accountRepository.findByUserIdAndCurrency(userId, "GC").orElseThrow().getBalance())
                .isEqualByComparingTo("500");
    }

    @Test
    void summarizesWalletAndScSources() {
        var userId = register("wallet-summary@example.com");
        walletService.credit(userId, "GC", new BigDecimal("10000"), "register_bonus", "claim_3", "idem_sum_gc");
        walletService.credit(userId, "SC", new BigDecimal("0.50"), "register_bonus", "claim_3", "idem_sum_sc");

        var summary = walletService.summary(userId);

        assertThat(summary.wallet().gcBalance()).isEqualByComparingTo("10000");
        assertThat(summary.wallet().scBalance()).isEqualByComparingTo("0.50");
        assertThat(summary.scSourceSummary())
                .anySatisfy(source -> {
                    assertThat(source.source()).isEqualTo("register_bonus");
                    assertThat(source.amount()).isEqualByComparingTo("0.50");
                });
    }

    @Test
    void filtersLedgerByCurrency() {
        var userId = register("wallet-ledger@example.com");
        walletService.credit(userId, "GC", new BigDecimal("10000"), "register_bonus", "claim_4", "idem_filter_gc");
        walletService.credit(userId, "SC", new BigDecimal("0.50"), "register_bonus", "claim_4", "idem_filter_sc");

        var page = walletService.ledger(userId, "SC", null, 1, 20);

        assertThat(page.items()).hasSize(1);
        assertThat(page.items().get(0).currency()).isEqualTo("SC");
        assertThat(page.total()).isEqualTo(1);
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
