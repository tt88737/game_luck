package com.gameluck.payment.service.reconciliation;

import java.math.BigDecimal;

public record ReconciliationPlatformSnapshot(
    int identityCandidateCount,
    String purchaseOrderNo,
    String currency,
    BigDecimal amount,
    boolean webhookPresent,
    String webhookEventType,
    String paymentEventType,
    String sessionStatus,
    String orderStatus,
    String paymentEventStatus,
    String reversalStatus,
    String reversalType,
    String reversalDispositionStatus,
    boolean duplicatePriorStatementEvidence,
    boolean supported,
    Long paymentSessionId,
    Long purchaseOrderId,
    Long webhookEventId,
    Long reversalId
) {
    public ReconciliationPlatformSnapshot(int identityCandidateCount, String purchaseOrderNo, String currency,
        BigDecimal amount, boolean webhookPresent, String webhookEventType, String paymentEventType,
        String sessionStatus, String orderStatus, String paymentEventStatus, String reversalStatus,
        String reversalType, String reversalDispositionStatus, boolean duplicatePriorStatementEvidence,
        boolean supported) {
        this(identityCandidateCount, purchaseOrderNo, currency, amount, webhookPresent, webhookEventType,
            paymentEventType, sessionStatus, orderStatus, paymentEventStatus, reversalStatus, reversalType,
            reversalDispositionStatus, duplicatePriorStatementEvidence, supported, null, null, null, null);
    }
    public ReconciliationPlatformSnapshot withIdentityCandidateCount(int value) {
        return copy(value, purchaseOrderNo, currency, amount, webhookPresent, webhookEventType, paymentEventType,
            sessionStatus, orderStatus, paymentEventStatus, reversalStatus, reversalType, reversalDispositionStatus,
            duplicatePriorStatementEvidence, supported);
    }

    public ReconciliationPlatformSnapshot withPurchaseOrderNo(String value) {
        return copy(identityCandidateCount, value, currency, amount, webhookPresent, webhookEventType, paymentEventType,
            sessionStatus, orderStatus, paymentEventStatus, reversalStatus, reversalType, reversalDispositionStatus,
            duplicatePriorStatementEvidence, supported);
    }

    public ReconciliationPlatformSnapshot withCurrency(String value) {
        return copy(identityCandidateCount, purchaseOrderNo, value, amount, webhookPresent, webhookEventType, paymentEventType,
            sessionStatus, orderStatus, paymentEventStatus, reversalStatus, reversalType, reversalDispositionStatus,
            duplicatePriorStatementEvidence, supported);
    }

    public ReconciliationPlatformSnapshot withAmount(BigDecimal value) {
        return copy(identityCandidateCount, purchaseOrderNo, currency, value, webhookPresent, webhookEventType, paymentEventType,
            sessionStatus, orderStatus, paymentEventStatus, reversalStatus, reversalType, reversalDispositionStatus,
            duplicatePriorStatementEvidence, supported);
    }

    public ReconciliationPlatformSnapshot withWebhookPresent(boolean value) {
        return copy(identityCandidateCount, purchaseOrderNo, currency, amount, value, webhookEventType, paymentEventType,
            sessionStatus, orderStatus, paymentEventStatus, reversalStatus, reversalType, reversalDispositionStatus,
            duplicatePriorStatementEvidence, supported);
    }

    public ReconciliationPlatformSnapshot withWebhookEventType(String value) {
        return copy(identityCandidateCount, purchaseOrderNo, currency, amount, webhookPresent, value, paymentEventType,
            sessionStatus, orderStatus, paymentEventStatus, reversalStatus, reversalType, reversalDispositionStatus,
            duplicatePriorStatementEvidence, supported);
    }

    public ReconciliationPlatformSnapshot withPaymentEventType(String value) {
        return copy(identityCandidateCount, purchaseOrderNo, currency, amount, webhookPresent, webhookEventType, value,
            sessionStatus, orderStatus, paymentEventStatus, reversalStatus, reversalType, reversalDispositionStatus,
            duplicatePriorStatementEvidence, supported);
    }

    public ReconciliationPlatformSnapshot withReversalType(String value) {
        return copy(identityCandidateCount, purchaseOrderNo, currency, amount, webhookPresent, webhookEventType,
            paymentEventType, sessionStatus, orderStatus, paymentEventStatus, reversalStatus, value,
            reversalDispositionStatus, duplicatePriorStatementEvidence, supported);
    }

    public ReconciliationPlatformSnapshot withOrderStatus(String value) {
        return copy(identityCandidateCount, purchaseOrderNo, currency, amount, webhookPresent, webhookEventType, paymentEventType,
            sessionStatus, value, paymentEventStatus, reversalStatus, reversalType, reversalDispositionStatus,
            duplicatePriorStatementEvidence, supported);
    }

    public ReconciliationPlatformSnapshot withDuplicatePriorStatementEvidence(boolean value) {
        return copy(identityCandidateCount, purchaseOrderNo, currency, amount, webhookPresent, webhookEventType, paymentEventType,
            sessionStatus, orderStatus, paymentEventStatus, reversalStatus, reversalType, reversalDispositionStatus, value, supported);
    }

    public ReconciliationPlatformSnapshot withSupported(boolean value) {
        return copy(identityCandidateCount, purchaseOrderNo, currency, amount, webhookPresent, webhookEventType, paymentEventType,
            sessionStatus, orderStatus, paymentEventStatus, reversalStatus, reversalType, reversalDispositionStatus,
            duplicatePriorStatementEvidence, value);
    }

    private ReconciliationPlatformSnapshot copy(int candidates, String orderNo, String currencyCode,
                                                BigDecimal payAmount, boolean eventPresent, String eventType,
                                                String platformPaymentEventType, String session, String order,
                                                String paymentEvent, String reversal, String platformReversalType,
                                                String disposition, boolean duplicate, boolean isSupported) {
        return new ReconciliationPlatformSnapshot(candidates, orderNo, currencyCode, payAmount, eventPresent,
            eventType, platformPaymentEventType, session, order, paymentEvent, reversal, platformReversalType,
            disposition, duplicate, isSupported, paymentSessionId, purchaseOrderId, webhookEventId, reversalId);
    }
}
