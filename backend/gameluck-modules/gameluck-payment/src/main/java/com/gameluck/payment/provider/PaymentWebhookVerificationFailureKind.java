package com.gameluck.payment.provider;

public enum PaymentWebhookVerificationFailureKind {
    MISSING_INPUT,
    PAYLOAD_TOO_LARGE,
    INVALID_SIGNATURE_FORMAT,
    INVALID_TIMESTAMP,
    SIGNATURE_MISMATCH,
    STALE_TIMESTAMP,
    POLICY_REJECTED
}
