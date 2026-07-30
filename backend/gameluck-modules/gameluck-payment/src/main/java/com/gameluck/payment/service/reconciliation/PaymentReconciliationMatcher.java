package com.gameluck.payment.service.reconciliation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gameluck.payment.enums.PaymentProviderEventType;
import com.gameluck.payment.enums.PaymentReconciliationIssueType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public final class PaymentReconciliationMatcher {

    private final ObjectMapper objectMapper;

    public PaymentReconciliationMatcher(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    public ReconciliationMatchResult match(ReconciliationParsedLine line,
                                           ReconciliationPlatformSnapshot platform) {
        Objects.requireNonNull(line, "line");
        Objects.requireNonNull(platform, "platform");
        List<ReconciliationDifference> differences = new ArrayList<>();

        if (platform.identityCandidateCount() != 1) {
            add(differences, PaymentReconciliationIssueType.PLATFORM_RECORD_MISSING, "identityCandidateCount",
                1, platform.identityCandidateCount());
        }
        if (platform.duplicatePriorStatementEvidence()) {
            add(differences, PaymentReconciliationIssueType.DUPLICATE_PROVIDER_RECORD, "providerRecordId",
                "unique prior evidence", line.providerRecordId());
        }
        if (platform.identityCandidateCount() != 1) {
            return result(differences);
        }
        if (!Objects.equals(line.purchaseOrderNo(), platform.purchaseOrderNo())) {
            add(differences, PaymentReconciliationIssueType.ORDER_IDENTITY_MISMATCH, "purchaseOrderNo",
                line.purchaseOrderNo(), platform.purchaseOrderNo());
        }
        if (!normalizedCurrency(line.currency()).equals(normalizedCurrency(platform.currency()))) {
            add(differences, PaymentReconciliationIssueType.CURRENCY_MISMATCH, "currency",
                normalizedCurrency(line.currency()), normalizedCurrency(platform.currency()));
        }
        if (line.amount() == null || platform.amount() == null || line.amount().compareTo(platform.amount()) != 0) {
            add(differences, PaymentReconciliationIssueType.AMOUNT_MISMATCH, "amount",
                line.amount(), platform.amount());
        }
        boolean incompatibleWebhookType = false;
        if (!platform.webhookPresent()) {
            add(differences, PaymentReconciliationIssueType.EVENT_MISSING, "webhookEventType",
                line.eventType(), null);
        } else if (!Objects.equals(line.eventType(), platform.webhookEventType())) {
            incompatibleWebhookType = true;
        }
        ExpectedState expectedState = expectedState(line.eventType(), platform.reversalDispositionStatus());
        if (expectedState != null) {
            if (!Objects.equals(expectedState.paymentEventType(), platform.paymentEventType())) {
                add(differences, PaymentReconciliationIssueType.STATUS_MISMATCH, "paymentEventType",
                    expectedState.paymentEventType(), platform.paymentEventType());
            }
            if (!Objects.equals(expectedState.reversalType(), platform.reversalType())) {
                add(differences, PaymentReconciliationIssueType.STATUS_MISMATCH, "reversalType",
                    expectedState.reversalType(), platform.reversalType());
            }
            if (!expectedState.matches(platform)) {
                add(differences, PaymentReconciliationIssueType.STATUS_MISMATCH, "platformState",
                    expectedState.asMap(), actualState(platform));
            }
        }
        if (incompatibleWebhookType) {
            add(differences, PaymentReconciliationIssueType.UNSUPPORTED_RECORD, "webhookEventType",
                line.eventType(), platform.webhookEventType());
        }
        if (isReversalEvent(line.eventType()) && !isSupportedDisposition(platform.reversalDispositionStatus())) {
            add(differences, PaymentReconciliationIssueType.UNSUPPORTED_RECORD, "reversalDispositionStatus",
                "null|PENDING_REVIEW|RECOVERY_COMPLETED|LOSS_ACCEPTED",
                platform.reversalDispositionStatus());
        }
        if (!platform.supported() || expectedState == null) {
            add(differences, PaymentReconciliationIssueType.UNSUPPORTED_RECORD, "eventType",
                "Phase 44 supported event", line.eventType());
        }

        return result(differences);
    }

    private ReconciliationMatchResult result(List<ReconciliationDifference> differences) {
        List<ReconciliationDifference> immutable = List.copyOf(differences);
        Optional<PaymentReconciliationIssueType> primary = immutable.stream()
            .map(ReconciliationDifference::issueType).findFirst();
        return new ReconciliationMatchResult(immutable.isEmpty(), primary, immutable, serialize(immutable));
    }

    private static String normalizedCurrency(String currency) {
        return currency == null ? "" : currency.toUpperCase(Locale.ROOT);
    }

    private static void add(List<ReconciliationDifference> differences, PaymentReconciliationIssueType type,
                            String field, Object expected, Object actual) {
        differences.add(new ReconciliationDifference(type, field, expected, actual));
    }

    private String serialize(List<ReconciliationDifference> differences) {
        try {
            return objectMapper.writeValueAsString(differences);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize reconciliation diagnostic snapshot", exception);
        }
    }

    private static ExpectedState expectedState(String eventType, String disposition) {
        PaymentProviderEventType event;
        try {
            event = PaymentProviderEventType.valueOf(eventType);
        } catch (IllegalArgumentException | NullPointerException exception) {
            return null;
        }
        return switch (event) {
            case PAYMENT_SUCCEEDED -> new ExpectedState("PAY_SUCCESS", null, "SUCCEEDED", "CREDITED", "PROCESSED", null);
            case PAYMENT_FAILED -> new ExpectedState("PAY_FAILED", null, "FAILED", "FAILED", "PROCESSED", null);
            case PAYMENT_CANCELLED -> new ExpectedState("CANCELLED", null, "CANCELLED", "CANCELLED", "PROCESSED", null);
            case REFUND_SUCCEEDED -> reversalState("REFUNDED", "REFUND", "REFUNDED", "REFUND_REVIEW", disposition);
            case CHARGEBACK_CREATED -> reversalState("CHARGEBACK", "CHARGEBACK", "CHARGEBACK", "CHARGEBACK_REVIEW", disposition);
        };
    }

    private static ExpectedState reversalState(String paymentEventType, String reversalType,
                                               String terminalOrder, String reviewOrder, String disposition) {
        if ("PENDING_REVIEW".equals(disposition)) {
            return new ExpectedState(paymentEventType, reversalType, "SUCCEEDED", reviewOrder, "PROCESSED", "REVIEW_REQUIRED");
        }
        if ("RECOVERY_COMPLETED".equals(disposition)) {
            return new ExpectedState(paymentEventType, reversalType, "SUCCEEDED", terminalOrder, "PROCESSED", "COMPLETED");
        }
        if ("LOSS_ACCEPTED".equals(disposition)) {
            return new ExpectedState(paymentEventType, reversalType, "SUCCEEDED", terminalOrder, "PROCESSED", "REVIEW_REQUIRED");
        }
        return new ExpectedState(paymentEventType, reversalType, "SUCCEEDED", terminalOrder, "PROCESSED", "COMPLETED");
    }

    private static boolean isReversalEvent(String eventType) {
        return "REFUND_SUCCEEDED".equals(eventType) || "CHARGEBACK_CREATED".equals(eventType);
    }

    private static boolean isSupportedDisposition(String disposition) {
        return disposition == null || "PENDING_REVIEW".equals(disposition)
            || "RECOVERY_COMPLETED".equals(disposition) || "LOSS_ACCEPTED".equals(disposition);
    }

    private static Map<String, Object> actualState(ReconciliationPlatformSnapshot platform) {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("sessionStatus", platform.sessionStatus());
        state.put("orderStatus", platform.orderStatus());
        state.put("paymentEventStatus", platform.paymentEventStatus());
        state.put("paymentEventType", platform.paymentEventType());
        state.put("reversalStatus", platform.reversalStatus());
        state.put("reversalType", platform.reversalType());
        state.put("reversalDispositionStatus", platform.reversalDispositionStatus());
        return state;
    }

    private record ExpectedState(String paymentEventType, String reversalType, String sessionStatus,
                                 String orderStatus, String paymentEventStatus, String reversalStatus) {
        private boolean matches(ReconciliationPlatformSnapshot platform) {
            return Objects.equals(sessionStatus, platform.sessionStatus())
                && Objects.equals(orderStatus, platform.orderStatus())
                && Objects.equals(paymentEventStatus, platform.paymentEventStatus())
                && Objects.equals(reversalStatus, platform.reversalStatus());
        }

        private Map<String, Object> asMap() {
            Map<String, Object> state = new LinkedHashMap<>();
            state.put("sessionStatus", sessionStatus);
            state.put("orderStatus", orderStatus);
            state.put("paymentEventStatus", paymentEventStatus);
            state.put("paymentEventType", paymentEventType);
            state.put("reversalStatus", reversalStatus);
            state.put("reversalType", reversalType);
            return state;
        }
    }
}
