package com.gameluck.payment.provider;

import com.gameluck.payment.config.PaymentProviderProperties;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("local")
class PaymentProviderRegistryTest {

    @Test
    void resolvesEnabledSimulatedProviderWithNormalizedCode() {
        PaymentProviderProperties properties = properties(true);
        PaymentProviderAdapter adapter = adapter("SIMULATED");
        PaymentProviderRegistry registry = new PaymentProviderRegistry(List.of(adapter), properties);

        assertThat(registry.resolve(" simulated ")).isSameAs(adapter);
    }

    @Test
    void rejectsUnknownProvider() {
        PaymentProviderRegistry registry = new PaymentProviderRegistry(List.of(adapter("SIMULATED")), properties(true));

        assertThatThrownBy(() -> registry.resolve("missing"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unknown payment provider");
    }

    @Test
    void rejectsDisabledProvider() {
        PaymentProviderRegistry registry = new PaymentProviderRegistry(List.of(adapter("SIMULATED")), properties(false));

        assertThatThrownBy(() -> registry.resolve("SIMULATED"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("disabled");
    }

    @Test
    void rejectsDuplicateNormalizedProviderCodesAtConstruction() {
        assertThatThrownBy(() -> new PaymentProviderRegistry(
            List.of(adapter("SIMULATED"), adapter(" simulated ")), properties(true)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Duplicate payment provider");
    }

    @Test
    void enabledSimulatedProviderRequiresSafeCompleteConfiguration() {
        PaymentProviderProperties properties = properties(true);
        properties.getSimulated().setSecret(" ");
        properties.getSimulated().setCheckoutBaseUrl("relative/checkout");
        properties.getSimulated().setWebhookBaseUrl("ftp://example.test/webhook");
        properties.getSimulated().setSessionTtlMinutes(0);
        properties.getSimulated().setSignatureToleranceSeconds(-1);

        try (var factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();
            assertThat(validator.validate(properties)).isNotEmpty();
        }
    }

    @Test
    void disabledSimulatedProviderAllowsUnsetEndpointsAndSecretWithValidNumericDefaults() {
        PaymentProviderProperties properties = properties(false);

        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertThat(factory.getValidator().validate(properties)).isEmpty();
        }
    }

    @Test
    void enabledSimulatedProviderRejectsEndpointQueryFragmentAndUserInfo() {
        PaymentProviderProperties properties = properties(true);
        properties.getSimulated().setSecret("configured-secret");
        properties.getSimulated().setCheckoutBaseUrl("https://checkout.example.test/pay?theme=x");
        properties.getSimulated().setWebhookBaseUrl("https://user@example.test/webhook#fragment");

        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertThat(factory.getValidator().validate(properties)).isNotEmpty();
        }
    }

    private static PaymentProviderProperties properties(boolean enabled) {
        PaymentProviderProperties properties = new PaymentProviderProperties();
        properties.getSimulated().setEnabled(enabled);
        return properties;
    }

    private static PaymentProviderAdapter adapter(String code) {
        return new PaymentProviderAdapter() {
            @Override
            public String providerCode() {
                return code;
            }

            @Override
            public PaymentProviderSessionResult createSession(PaymentProviderSessionRequest request) {
                throw new UnsupportedOperationException();
            }

            @Override
            public PaymentWebhookVerificationResult verifyWebhook(
                String timestamp, String signature, byte[] rawBody, Instant now) {
                throw new UnsupportedOperationException();
            }

            @Override
            public PaymentWebhookEnvelope parseWebhook(byte[] rawBody) {
                throw new UnsupportedOperationException();
            }
        };
    }
}
