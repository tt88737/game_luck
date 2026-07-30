package com.gameluck.payment.provider;

import java.time.Instant;

public interface PaymentProviderAdapter {

    // Remote adapters require idempotent create and must not be registered until two-phase orchestration exists.

    String providerCode();

    PaymentProviderSessionResult createSession(PaymentProviderSessionRequest request);

    PaymentWebhookVerificationResult verifyWebhook(String timestamp, String signature, byte[] rawBody, Instant now);

    default PaymentWebhookVerificationResult verifyWebhookCryptographicSignature(
        String timestamp, String signature, byte[] rawBody) {
        return PaymentWebhookVerificationResult.failure(
            PaymentWebhookVerificationFailureKind.POLICY_REJECTED,
            "Stale replay verification is not supported");
    }

    PaymentWebhookEnvelope parseWebhook(byte[] rawBody);
}
