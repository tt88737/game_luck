package com.gameluck.payment.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gameluck.payment.config.PaymentProviderConfiguration;
import com.gameluck.payment.config.PaymentProviderProperties;
import com.gameluck.payment.enums.PaymentProviderEventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("local")
class SimulatedPaymentProviderAdapterTest {

    private static final String SECRET = "test-payment-secret";
    private static final Instant NOW = Instant.parse("2026-07-27T04:00:00Z");
    private SimulatedPaymentProviderAdapter adapter;

    @BeforeEach
    void setUp() {
        PaymentProviderProperties properties = new PaymentProviderProperties();
        properties.getSimulated().setEnabled(true);
        properties.getSimulated().setSecret(SECRET);
        properties.getSimulated().setCheckoutBaseUrl("http://127.0.0.1:5174/simulated-checkout/");
        properties.getSimulated().setWebhookBaseUrl("http://127.0.0.1:8080/payment/webhooks");
        properties.getSimulated().setSessionTtlMinutes(15);
        properties.getSimulated().setSignatureToleranceSeconds(300);
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        adapter = new SimulatedPaymentProviderAdapter(properties, objectMapper, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void verifiesExactSignature() throws Exception {
        byte[] body = "{\"amount\":\"10.00\"}".getBytes(StandardCharsets.UTF_8);
        String timestamp = Long.toString(NOW.getEpochSecond());
        String signature = signature(timestamp, body);

        assertThat(adapter.verifyWebhook(timestamp, signature, body, NOW).verified()).isTrue();
    }

    @Test
    void rejectsTamperedBody() throws Exception {
        String timestamp = Long.toString(NOW.getEpochSecond());
        String signature = signature(timestamp, "original".getBytes(StandardCharsets.UTF_8));

        assertThat(adapter.verifyWebhook(timestamp, signature, "changed".getBytes(StandardCharsets.UTF_8), NOW).verified())
            .isFalse();
    }

    @Test
    void rejectsStaleAndFutureTimestampsOutsideTolerance() throws Exception {
        byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
        String stale = Long.toString(NOW.minusSeconds(301).getEpochSecond());
        String future = Long.toString(NOW.plusSeconds(301).getEpochSecond());

        assertThat(adapter.verifyWebhook(stale, signature(stale, body), body, NOW).verified()).isFalse();
        assertThat(adapter.verifyWebhook(future, signature(future, body), body, NOW).verified()).isFalse();
    }

    @Test
    void cryptographicReplayVerificationAcceptsOldExactBytesButRejectsTampering() throws Exception {
        byte[] body = "{\"event\":1}".getBytes(StandardCharsets.UTF_8);
        String oldTimestamp = Long.toString(NOW.minusSeconds(600).getEpochSecond());
        String signature = signature(oldTimestamp, body);

        assertThat(adapter.verifyWebhook(oldTimestamp, signature, body, NOW).verified()).isFalse();
        assertThat(adapter.verifyWebhookCryptographicSignature(oldTimestamp, signature, body).verified()).isTrue();
        assertThat(adapter.verifyWebhookCryptographicSignature(
            oldTimestamp, signature, "{\"event\":2}".getBytes(StandardCharsets.UTF_8)).verified()).isFalse();
    }

    @Test
    void rejectsMalformedTimestampBadHexAndEmptySignatureSafely() {
        byte[] body = "{}".getBytes(StandardCharsets.UTF_8);

        assertThat(adapter.verifyWebhook("not-a-time", "00", body, NOW).verified()).isFalse();
        assertThat(adapter.verifyWebhook(Long.toString(NOW.getEpochSecond()), "not-hex", body, NOW).verified()).isFalse();
        assertThat(adapter.verifyWebhook(Long.toString(NOW.getEpochSecond()), "", body, NOW).verified()).isFalse();
    }

    @Test
    void rejectsSignaturesThatAreNotExactly64AsciiHexCharactersBeforeComparison() {
        byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
        String timestamp = Long.toString(NOW.getEpochSecond());

        assertThat(adapter.verifyWebhook(timestamp, "a".repeat(66), body, NOW).failureReason())
            .contains("format");
        assertThat(adapter.verifyWebhook(timestamp, "a".repeat(63), body, NOW).failureReason())
            .contains("format");
        assertThat(adapter.verifyWebhook(timestamp, "é".repeat(64), body, NOW).failureReason())
            .contains("format");
        assertThat(adapter.verifyWebhook(timestamp, "g".repeat(64), body, NOW).failureReason())
            .contains("format");
    }

    @Test
    void rejectsOversizedBodyBeforeHmacCalculation() {
        PaymentProviderProperties properties = new PaymentProviderProperties();
        properties.getSimulated().setSignatureToleranceSeconds(300);
        SimulatedPaymentProviderAdapter adapterWithoutSecret = new SimulatedPaymentProviderAdapter(
            properties, new ObjectMapper(), Clock.fixed(NOW, ZoneOffset.UTC));
        byte[] oversizedBody = new byte[256 * 1024 + 1];

        PaymentWebhookVerificationResult result = adapterWithoutSecret.verifyWebhook(
            Long.toString(NOW.getEpochSecond()), "a".repeat(64), oversizedBody, NOW);

        assertThat(result.verified()).isFalse();
        assertThat(result.failureReason()).contains("too large");
    }

    @Test
    void parsesValidWebhookJson() {
        byte[] body = ("{\"tenantId\":42,\"providerEventId\":\"evt-1\","
            + "\"eventType\":\"PAYMENT_SUCCEEDED\",\"providerSessionNo\":\"ps-1\","
            + "\"purchaseOrderNo\":\"po-1\",\"payCurrencyCode\":\"USD\","
            + "\"payAmount\":\"12.50\",\"occurredTime\":\"2026-07-27T03:59:00Z\"}")
            .getBytes(StandardCharsets.UTF_8);

        PaymentWebhookEnvelope result = adapter.parseWebhook(body);

        assertThat(result.tenantId()).isEqualTo(42L);
        assertThat(result.providerEventId()).isEqualTo("evt-1");
        assertThat(result.eventType()).isEqualTo(PaymentProviderEventType.PAYMENT_SUCCEEDED);
        assertThat(result.providerSessionNo()).isEqualTo("ps-1");
        assertThat(result.purchaseOrderNo()).isEqualTo("po-1");
        assertThat(result.payCurrencyCode()).isEqualTo("USD");
        assertThat(result.payAmount()).isEqualByComparingTo("12.50");
        assertThat(result.occurredTime()).isEqualTo(Instant.parse("2026-07-27T03:59:00Z"));
    }

    @Test
    void rejectsUnsupportedEventAndMalformedJson() {
        assertThatThrownBy(() -> adapter.parseWebhook("{\"eventType\":\"UNKNOWN\"}".getBytes(StandardCharsets.UTF_8)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("webhook");
        assertThatThrownBy(() -> adapter.parseWebhook("{".getBytes(StandardCharsets.UTF_8)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("webhook");
    }

    @Test
    void rejectsWebhookBodyLargerThan256KiBBeforeJsonParsing() {
        byte[] body = new byte[256 * 1024 + 1];
        java.util.Arrays.fill(body, (byte) ' ');
        body[0] = '{';
        body[1] = '}';

        assertThatThrownBy(() -> adapter.parseWebhook(body))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("too large")
            .hasMessageNotContaining(new String(body, 0, 20, StandardCharsets.UTF_8));
    }

    @Test
    void productionAdapterUsesSingleInjectableClockConstructor() {
        assertThat(SimulatedPaymentProviderAdapter.class.getDeclaredConstructors()).hasSize(1);
        assertThat(SimulatedPaymentProviderAdapter.class.getDeclaredConstructors()[0].getParameterTypes())
            .contains(Clock.class);
        assertThat(new PaymentProviderConfiguration().paymentClock().getZone()).isEqualTo(ZoneOffset.UTC);
    }

    @Test
    void createsSessionWithCopiedFieldsUrlAndConfiguredTtl() {
        PaymentProviderSessionRequest request = new PaymentProviderSessionRequest(
            42L, "po-1", "USD", new BigDecimal("12.50"), "request-1");

        PaymentProviderSessionResult result = adapter.createSession(request);

        assertThat(result.providerSessionNo()).startsWith("SIM-");
        assertThat(result.checkoutUrl()).isEqualTo(
            "http://127.0.0.1:5174/simulated-checkout/" + result.providerSessionNo());
        assertThat(result.expireTime()).isEqualTo(NOW.plusSeconds(15 * 60));
        assertThat(result.purchaseOrderNo()).isEqualTo("po-1");
        assertThat(result.payCurrencyCode()).isEqualTo("USD");
        assertThat(result.payAmount()).isEqualByComparingTo("12.50");
    }

    @Test
    void sameRequestKeyCreatesStableProviderSessionAndDifferentKeyDoesNot() {
        PaymentProviderSessionRequest first = new PaymentProviderSessionRequest(
            42L, "po-1", "USD", new BigDecimal("12.50"), "request-1");
        PaymentProviderSessionRequest different = new PaymentProviderSessionRequest(
            42L, "po-1", "USD", new BigDecimal("12.50"), "request-2");
        assertThat(adapter.createSession(first).providerSessionNo())
            .isEqualTo(adapter.createSession(first).providerSessionNo())
            .isNotEqualTo(adapter.createSession(different).providerSessionNo());
        assertThat(adapter.createSession(first).checkoutUrl()).isEqualTo(adapter.createSession(first).checkoutUrl());
    }

    @Test
    void hostedSessionNumberIsNotAnUnkeyedUuidOfBrowserControlledInput() {
        PaymentProviderSessionRequest request = new PaymentProviderSessionRequest(
            42L, "po-1", "USD", new BigDecimal("12.50"), "request-1");
        String predictable = "SIM-" + java.util.UUID.nameUUIDFromBytes(
            "42:request-1".getBytes(StandardCharsets.UTF_8)).toString().replace("-", "");

        assertThat(adapter.createSession(request).providerSessionNo()).isNotEqualTo(predictable);
    }

    private static String signature(String timestamp, byte[] body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        mac.update(timestamp.getBytes(StandardCharsets.UTF_8));
        mac.update((byte) '.');
        return HexFormat.of().formatHex(mac.doFinal(body));
    }
}
