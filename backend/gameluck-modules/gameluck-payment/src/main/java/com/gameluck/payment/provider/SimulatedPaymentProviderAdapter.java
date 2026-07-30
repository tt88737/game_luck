package com.gameluck.payment.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gameluck.payment.config.PaymentProviderProperties;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;

@Component
public class SimulatedPaymentProviderAdapter implements PaymentProviderAdapter {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int SIGNATURE_HEX_LENGTH = 64;
    private static final int MAX_WEBHOOK_BODY_BYTES = 256 * 1024;

    private final PaymentProviderProperties.Simulated properties;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public SimulatedPaymentProviderAdapter(
        PaymentProviderProperties properties, ObjectMapper objectMapper, Clock clock) {
        this.properties = properties.getSimulated();
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    public String providerCode() {
        return "SIMULATED";
    }

    @Override
    public PaymentProviderSessionResult createSession(PaymentProviderSessionRequest request) {
        String stableInput = request.tenantId() + ":" + request.requestKey();
        String providerSessionNo = "SIM-" + HexFormat.of()
            .formatHex(hmac("session", stableInput.getBytes(StandardCharsets.UTF_8)), 0, 16);
        String checkoutUrl = stripTrailingSlashes(properties.getCheckoutBaseUrl()) + "/" + providerSessionNo;
        Instant expireTime = clock.instant().plus(properties.getSessionTtlMinutes(), ChronoUnit.MINUTES);
        return new PaymentProviderSessionResult(
            providerSessionNo,
            checkoutUrl,
            expireTime,
            request.purchaseOrderNo(),
            request.payCurrencyCode(),
            request.payAmount()
        );
    }

    @Override
    public PaymentWebhookVerificationResult verifyWebhook(
        String timestamp, String signature, byte[] rawBody, Instant now) {
        if (now == null) {
            return failure(PaymentWebhookVerificationFailureKind.MISSING_INPUT, "Missing webhook signature input");
        }
        PaymentWebhookVerificationResult cryptographic = verifyWebhookCryptographicSignature(timestamp, signature, rawBody);
        if (!cryptographic.verified()) {
            return cryptographic;
        }
        long epochSeconds = Long.parseLong(timestamp);
        long difference;
        try {
            difference = Math.abs(Math.subtractExact(now.getEpochSecond(), epochSeconds));
        } catch (ArithmeticException exception) {
            return failure(PaymentWebhookVerificationFailureKind.STALE_TIMESTAMP,
                "Webhook timestamp outside tolerance");
        }
        if (difference > properties.getSignatureToleranceSeconds()) {
            return failure(PaymentWebhookVerificationFailureKind.STALE_TIMESTAMP,
                "Webhook timestamp outside tolerance");
        }
        return PaymentWebhookVerificationResult.success();
    }

    @Override
    public PaymentWebhookVerificationResult verifyWebhookCryptographicSignature(
        String timestamp, String signature, byte[] rawBody) {
        if (timestamp == null || signature == null || rawBody == null) {
            return failure(PaymentWebhookVerificationFailureKind.MISSING_INPUT, "Missing webhook signature input");
        }
        if (rawBody.length > MAX_WEBHOOK_BODY_BYTES) {
            return failure(PaymentWebhookVerificationFailureKind.PAYLOAD_TOO_LARGE,
                "Webhook payload is too large");
        }
        if (!isSha256Hex(signature)) {
            return failure(PaymentWebhookVerificationFailureKind.INVALID_SIGNATURE_FORMAT,
                "Invalid webhook signature format");
        }

        try {
            Long.parseLong(timestamp);
        } catch (NumberFormatException exception) {
            return failure(PaymentWebhookVerificationFailureKind.INVALID_TIMESTAMP, "Invalid webhook timestamp");
        }

        final byte[] supplied;
        try {
            supplied = HexFormat.of().parseHex(signature);
        } catch (IllegalArgumentException exception) {
            return failure(PaymentWebhookVerificationFailureKind.INVALID_SIGNATURE_FORMAT,
                "Invalid webhook signature format");
        }

        byte[] expected = hmac(timestamp, rawBody);
        if (!MessageDigest.isEqual(expected, supplied)) {
            return failure(PaymentWebhookVerificationFailureKind.SIGNATURE_MISMATCH,
                "Webhook signature mismatch");
        }
        return PaymentWebhookVerificationResult.success();
    }

    @Override
    public PaymentWebhookEnvelope parseWebhook(byte[] rawBody) {
        if (rawBody == null || rawBody.length > MAX_WEBHOOK_BODY_BYTES) {
            throw new IllegalArgumentException("Simulated payment webhook payload is too large");
        }
        // Structural depth is governed by the application's shared Jackson stream constraints.
        try {
            return objectMapper.readValue(rawBody, PaymentWebhookEnvelope.class);
        } catch (IOException | IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid simulated payment webhook", exception);
        }
    }

    public String signWebhook(String timestamp, byte[] rawBody) {
        if (timestamp == null || timestamp.isBlank() || rawBody == null || rawBody.length > MAX_WEBHOOK_BODY_BYTES) {
            throw new IllegalArgumentException("Invalid simulated webhook signature input");
        }
        return HexFormat.of().formatHex(hmac(timestamp, rawBody));
    }

    private static boolean isSha256Hex(String signature) {
        if (signature.length() != SIGNATURE_HEX_LENGTH) {
            return false;
        }
        for (int index = 0; index < signature.length(); index++) {
            char character = signature.charAt(index);
            if (!((character >= '0' && character <= '9')
                || (character >= 'a' && character <= 'f')
                || (character >= 'A' && character <= 'F'))) {
                return false;
            }
        }
        return true;
    }

    private static PaymentWebhookVerificationResult failure(
        PaymentWebhookVerificationFailureKind kind, String reason) {
        return PaymentWebhookVerificationResult.failure(kind, reason);
    }

    private byte[] hmac(String timestamp, byte[] rawBody) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(properties.getSecret().getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            mac.update(timestamp.getBytes(StandardCharsets.UTF_8));
            mac.update((byte) '.');
            return mac.doFinal(rawBody);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Unable to verify payment webhook", exception);
        }
    }

    private static String stripTrailingSlashes(String value) {
        int end = value.length();
        while (end > 0 && value.charAt(end - 1) == '/') {
            end--;
        }
        return value.substring(0, end);
    }
}
