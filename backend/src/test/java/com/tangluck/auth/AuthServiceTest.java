package com.tangluck.auth;

import com.tangluck.common.api.BusinessException;
import com.tangluck.common.api.ErrorCode;
import com.tangluck.wallet.WalletAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserConsentLogRepository consentLogRepository;

    @Autowired
    private WalletAccountRepository walletAccountRepository;

    @Test
    void registersAdultCaUserAndCreatesTwoWallets() {
        var result = authService.register(validRequest("ava@example.com", "CA"));

        assertThat(result.user().email()).isEqualTo("ava@example.com");
        assertThat(result.user().status()).isEqualTo("active");
        assertThat(result.wallet().gcBalance()).isEqualByComparingTo("0");
        assertThat(result.wallet().scBalance()).isEqualByComparingTo("0.00");
        assertThat(result.token()).isNotBlank();

        var user = userRepository.findByEmail("ava@example.com").orElseThrow();
        assertThat(consentLogRepository.findByUserId(user.getId())).hasSize(3);
        assertThat(walletAccountRepository.findByUserId(user.getId()))
                .extracting("currency")
                .containsExactlyInAnyOrder("GC", "SC");
    }

    @Test
    void rejectsDuplicateEmail() {
        authService.register(validRequest("dupe@example.com", "CA"));

        assertThatThrownBy(() -> authService.register(validRequest("dupe@example.com", "CA")))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo(ErrorCode.EMAIL_EXISTS));
    }

    @Test
    void rejectsUnderAgeUser() {
        var request = validRequest("minor@example.com", "CA")
                .withBirthDate(LocalDate.now().minusYears(17));

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo(ErrorCode.AGE_NOT_ALLOWED));
    }

    @Test
    void rejectsMissingRequiredConsent() {
        var request = validRequest("consent@example.com", "CA")
                .withAcceptedDocuments(List.of(
                        new AcceptedDocument("terms", "terms-v1"),
                        new AcceptedDocument("privacy", "privacy-v1")
                ));

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo(ErrorCode.CONSENT_REQUIRED));
    }

    @Test
    void rejectsBlockedRegion() {
        assertThatThrownBy(() -> authService.register(validRequest("blocked@example.com", "WA")))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo(ErrorCode.REGION_BLOCKED));
    }

    @Test
    void loginReturnsRegisteredUserAndWallets() {
        authService.register(validRequest("service-login@example.com", "CA"));

        var result = authService.login(new LoginRequest("service-login@example.com", "StrongPass123!"));

        assertThat(result.user().email()).isEqualTo("service-login@example.com");
        assertThat(result.wallet().gcBalance()).isEqualByComparingTo("0");
        assertThat(result.wallet().scBalance()).isEqualByComparingTo("0");
        assertThat(result.token()).isNotBlank();
    }

    @Test
    void loginRejectsWrongPassword() {
        authService.register(validRequest("wrong-password@example.com", "CA"));

        assertThatThrownBy(() -> authService.login(new LoginRequest("wrong-password@example.com", "bad-password")))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo(ErrorCode.AUTH_INVALID_CREDENTIALS));
    }

    private RegisterRequest validRequest(String email, String stateCode) {
        return new RegisterRequest(
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
                "dev_abc123"
        );
    }
}
