package com.gameluck.payment.provider;

public record PaymentWebhookVerificationResult(
    boolean verified,
    PaymentWebhookVerificationFailureKind failureKind,
    String failureReason
) {

    public static PaymentWebhookVerificationResult success() {
        return new PaymentWebhookVerificationResult(true, null, null);
    }

    public static PaymentWebhookVerificationResult failure(String reason) {
        return failure(PaymentWebhookVerificationFailureKind.POLICY_REJECTED, reason);
    }

    public static PaymentWebhookVerificationResult failure(
        PaymentWebhookVerificationFailureKind kind, String reason) {
        if (kind == null) {
            throw new IllegalArgumentException("Payment webhook verification failure kind is required");
        }
        return new PaymentWebhookVerificationResult(false, kind, reason);
    }
}
