package com.tangluck.promotion;

import com.tangluck.auth.AcceptedDocument;
import com.tangluck.auth.AuthService;
import com.tangluck.auth.RegisterRequest;
import com.tangluck.wallet.WalletAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class CouponServiceTest {
    @Autowired
    private AuthService authService;

    @Autowired
    private CouponService couponService;

    @Autowired
    private WalletAccountRepository walletAccountRepository;

    @Test
    void welcomeCouponGrantsGc() {
        var userId = register("coupon@example.com");

        var response = couponService.claim(userId, "WELCOME500", "idem_coupon_api");

        assertThat(response.status()).isEqualTo("granted");
        assertThat(response.rewards()).singleElement().satisfies(reward -> {
            assertThat(reward.currency()).isEqualTo("GC");
            assertThat(reward.amount()).isEqualByComparingTo("500");
        });
        assertThat(walletAccountRepository.findByUserIdAndCurrency(userId, "GC").orElseThrow().getBalance())
                .isEqualByComparingTo("500");
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
