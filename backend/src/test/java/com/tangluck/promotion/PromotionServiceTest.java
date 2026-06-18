package com.tangluck.promotion;

import com.tangluck.auth.AcceptedDocument;
import com.tangluck.auth.AuthService;
import com.tangluck.auth.RegisterRequest;
import com.tangluck.common.api.BusinessException;
import com.tangluck.common.api.ErrorCode;
import com.tangluck.wallet.WalletAccountRepository;
import com.tangluck.wallet.WalletLedgerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class PromotionServiceTest {
    @Autowired
    private AuthService authService;

    @Autowired
    private PromotionService promotionService;

    @Autowired
    private WalletAccountRepository walletAccountRepository;

    @Autowired
    private WalletLedgerRepository ledgerRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void normalUserClaimsRegisterBonus() {
        var userId = register("promo-normal@example.com", "CA");

        var response = promotionService.claimCampaign(userId, "register_bonus_v1", "idem_claim_register");

        assertThat(response.status()).isEqualTo("granted");
        assertThat(response.rewards()).hasSize(2);
        assertThat(walletAccountRepository.findByUserIdAndCurrency(userId, "GC").orElseThrow().getBalance())
                .isEqualByComparingTo("10000");
        assertThat(walletAccountRepository.findByUserIdAndCurrency(userId, "SC").orElseThrow().getBalance())
                .isEqualByComparingTo("0.50");
        assertThat(ledgerRepository.findByUserId(userId)).hasSize(2);
    }

    @Test
    void duplicateClaimIsRejected() {
        var userId = register("promo-dupe@example.com", "CA");
        promotionService.claimCampaign(userId, "register_bonus_v1", "idem_claim_dupe_1");

        assertThatThrownBy(() -> promotionService.claimCampaign(userId, "register_bonus_v1", "idem_claim_dupe_2"))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo(ErrorCode.CLAIM_DUPLICATED));
    }

    @Test
    void riskUserReceivesGcOnly() {
        var userId = register("promo-risk@example.com", "CA");
        jdbcTemplate.update("update users set risk_level='manual_review' where id=?", userId);

        promotionService.claimCampaign(userId, "daily_login_v1", "idem_claim_risk");

        assertThat(walletAccountRepository.findByUserIdAndCurrency(userId, "GC").orElseThrow().getBalance())
                .isEqualByComparingTo("1000");
        assertThat(walletAccountRepository.findByUserIdAndCurrency(userId, "SC").orElseThrow().getBalance())
                .isEqualByComparingTo("0");
    }

    @Test
    void blockedRegionCannotClaimScCampaign() {
        var userId = register("promo-wa@example.com", "CA");
        jdbcTemplate.update("update users set state_code='WA' where id=?", userId);

        assertThatThrownBy(() -> promotionService.claimCampaign(userId, "daily_login_v1", "idem_claim_wa"))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo(ErrorCode.REGION_BLOCKED));
    }

    @Test
    void rulesTaskGrantsGcOnly() {
        var userId = register("promo-task@example.com", "CA");

        var response = promotionService.claimCampaign(userId, "view_rules_task_v1", "idem_claim_task");

        assertThat(response.rewards()).singleElement().satisfies(reward -> {
            assertThat(reward.currency()).isEqualTo("GC");
            assertThat(reward.amount()).isEqualByComparingTo("500");
        });
    }

    private Long register(String email, String stateCode) {
        return authService.register(new RegisterRequest(
                email,
                "StrongPass123!",
                LocalDate.of(1996, 4, 12),
                "US",
                stateCode,
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
