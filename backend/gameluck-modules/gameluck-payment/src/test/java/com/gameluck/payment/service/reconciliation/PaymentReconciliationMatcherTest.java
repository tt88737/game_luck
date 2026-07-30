package com.gameluck.payment.service.reconciliation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gameluck.payment.enums.PaymentReconciliationIssueType;
import com.gameluck.payment.mapper.PaymentWebhookEventMapper;
import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("local")
class PaymentReconciliationMatcherTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PaymentReconciliationMatcher matcher = new PaymentReconciliationMatcher(objectMapper);

    @ParameterizedTest
    @MethodSource("matchingStates")
    void matchesAllFiveProviderEventExpectedStates(String event, String session, String order,
                                                     String paymentEvent, String reversal,
                                                     String disposition) {
        ReconciliationMatchResult result = matcher.match(line(event),
            snapshot(1, "order-1", "usd", "12.340000", true, event,
                session, order, paymentEvent, reversal, disposition, false, true));

        assertThat(result.matched()).isTrue();
        assertThat(result.primaryIssueType()).isEmpty();
        assertThat(result.differences()).isEmpty();
        assertThat(result.diagnosticSnapshotJson()).isEqualTo("[]");
    }

    static Stream<Arguments> matchingStates() {
        return Stream.of(
            Arguments.of("PAYMENT_SUCCEEDED", "SUCCEEDED", "CREDITED", "PROCESSED", null, null),
            Arguments.of("PAYMENT_FAILED", "FAILED", "FAILED", "PROCESSED", null, null),
            Arguments.of("PAYMENT_CANCELLED", "CANCELLED", "CANCELLED", "PROCESSED", null, null),
            Arguments.of("REFUND_SUCCEEDED", "SUCCEEDED", "REFUNDED", "PROCESSED", "COMPLETED", null),
            Arguments.of("CHARGEBACK_CREATED", "SUCCEEDED", "CHARGEBACK", "PROCESSED", "COMPLETED", null),
            Arguments.of("REFUND_SUCCEEDED", "SUCCEEDED", "REFUND_REVIEW", "PROCESSED", "REVIEW_REQUIRED", "PENDING_REVIEW"),
            Arguments.of("REFUND_SUCCEEDED", "SUCCEEDED", "REFUNDED", "PROCESSED", "COMPLETED", "RECOVERY_COMPLETED"),
            Arguments.of("CHARGEBACK_CREATED", "SUCCEEDED", "CHARGEBACK", "PROCESSED", "REVIEW_REQUIRED", "LOSS_ACCEPTED")
        );
    }

    @ParameterizedTest
    @MethodSource("singleIssues")
    void createsEveryPrimaryIssueType(PaymentReconciliationIssueType expected,
                                      ReconciliationPlatformSnapshot platform) {
        ReconciliationMatchResult result = matcher.match(line("PAYMENT_SUCCEEDED"), platform);

        assertThat(result.matched()).isFalse();
        assertThat(result.primaryIssueType()).contains(expected);
        assertThat(result.differences()).hasSize(1);
        assertThat(result.differences().get(0).issueType()).isEqualTo(expected);
    }

    static Stream<Arguments> singleIssues() {
        ReconciliationPlatformSnapshot ok = success();
        return Stream.of(
            Arguments.of(PaymentReconciliationIssueType.PLATFORM_RECORD_MISSING, ok.withIdentityCandidateCount(0)),
            Arguments.of(PaymentReconciliationIssueType.PLATFORM_RECORD_MISSING, ok.withIdentityCandidateCount(2)),
            Arguments.of(PaymentReconciliationIssueType.ORDER_IDENTITY_MISMATCH, ok.withPurchaseOrderNo("other")),
            Arguments.of(PaymentReconciliationIssueType.AMOUNT_MISMATCH, ok.withAmount(new BigDecimal("12.35"))),
            Arguments.of(PaymentReconciliationIssueType.CURRENCY_MISMATCH, ok.withCurrency("EUR")),
            Arguments.of(PaymentReconciliationIssueType.EVENT_MISSING, ok.withWebhookPresent(false)),
            Arguments.of(PaymentReconciliationIssueType.STATUS_MISMATCH, ok.withOrderStatus("PENDING")),
            Arguments.of(PaymentReconciliationIssueType.DUPLICATE_PROVIDER_RECORD, ok.withDuplicatePriorStatementEvidence(true)),
            Arguments.of(PaymentReconciliationIssueType.UNSUPPORTED_RECORD, ok.withSupported(false))
        );
    }

    @Test
    void ordersPrimaryPriorityAndSerializesEveryDetectedDifference() throws Exception {
        ReconciliationPlatformSnapshot platform = snapshot(1, "wrong-order", "EUR", "99", false,
            "PAYMENT_FAILED", "PENDING", "PENDING", "FAILED", null, null, false, false);

        ReconciliationMatchResult result = matcher.match(line("PAYMENT_SUCCEEDED"), platform);

        assertThat(result.primaryIssueType()).contains(PaymentReconciliationIssueType.ORDER_IDENTITY_MISMATCH);
        assertThat(result.differences()).extracting(ReconciliationDifference::issueType).containsExactly(
            PaymentReconciliationIssueType.ORDER_IDENTITY_MISMATCH,
            PaymentReconciliationIssueType.CURRENCY_MISMATCH,
            PaymentReconciliationIssueType.AMOUNT_MISMATCH,
            PaymentReconciliationIssueType.EVENT_MISSING,
            PaymentReconciliationIssueType.STATUS_MISMATCH,
            PaymentReconciliationIssueType.STATUS_MISMATCH,
            PaymentReconciliationIssueType.UNSUPPORTED_RECORD);
        JsonNode json = objectMapper.readTree(result.diagnosticSnapshotJson());
        assertThat(json).hasSize(7);
        assertThat(json.get(0).get("expected").asText()).isEqualTo("order-1");
        assertThat(json.toString()).contains("PENDING").contains("FAILED");
    }

    @Test
    void currencyPrecedesAmountAndAmountPrecedesEventAndStatus() {
        ReconciliationMatchResult currency = matcher.match(line("PAYMENT_SUCCEEDED"),
            success().withCurrency("EUR").withAmount(new BigDecimal("99"))
                .withWebhookEventType("PAYMENT_FAILED").withOrderStatus("PENDING"));
        assertThat(currency.primaryIssueType()).contains(PaymentReconciliationIssueType.CURRENCY_MISMATCH);

        ReconciliationMatchResult amount = matcher.match(line("PAYMENT_SUCCEEDED"),
            success().withAmount(new BigDecimal("99"))
                .withWebhookEventType("PAYMENT_FAILED").withOrderStatus("PENDING"));
        assertThat(amount.primaryIssueType()).contains(PaymentReconciliationIssueType.AMOUNT_MISMATCH);
    }

    @Test
    void rejectsUnknownReviewTerminalDisposition() {
        ReconciliationPlatformSnapshot inconsistent = snapshot(1, "order-1", "USD", "12.34", true,
            "REFUND_SUCCEEDED", "SUCCEEDED", "REFUNDED", "PROCESSED", "REVIEW_REQUIRED",
            "NOT_A_TERMINAL_DISPOSITION", false, true);

        ReconciliationMatchResult result = matcher.match(line("REFUND_SUCCEEDED"), inconsistent);

        assertThat(result.primaryIssueType()).contains(PaymentReconciliationIssueType.STATUS_MISMATCH);
    }

    @Test
    void recoveryCompletedAndLossAcceptedRequireDifferentReversalStatuses() {
        ReconciliationPlatformSnapshot recoveryWithReviewStatus = snapshot(1, "order-1", "USD", "12.34", true,
            "REFUND_SUCCEEDED", "SUCCEEDED", "REFUNDED", "PROCESSED", "REVIEW_REQUIRED",
            "RECOVERY_COMPLETED", false, true);
        ReconciliationPlatformSnapshot lossWithCompletedStatus = snapshot(1, "order-1", "USD", "12.34", true,
            "CHARGEBACK_CREATED", "SUCCEEDED", "CHARGEBACK", "PROCESSED", "COMPLETED",
            "LOSS_ACCEPTED", false, true);

        assertThat(matcher.match(line("REFUND_SUCCEEDED"), recoveryWithReviewStatus).primaryIssueType())
            .contains(PaymentReconciliationIssueType.STATUS_MISMATCH);
        assertThat(matcher.match(line("CHARGEBACK_CREATED"), lossWithCompletedStatus).primaryIssueType())
            .contains(PaymentReconciliationIssueType.STATUS_MISMATCH);
    }

    @Test
    void unknownDispositionIsUnsupportedEvenWhenOtherStateLooksCompleted() {
        ReconciliationPlatformSnapshot unknown = snapshot(1, "order-1", "USD", "12.34", true,
            "REFUND_SUCCEEDED", "SUCCEEDED", "REFUNDED", "PROCESSED", "COMPLETED",
            "UNKNOWN_DISPOSITION", false, true);

        ReconciliationMatchResult result = matcher.match(line("REFUND_SUCCEEDED"), unknown);

        assertThat(result.primaryIssueType()).contains(PaymentReconciliationIssueType.UNSUPPORTED_RECORD);
        assertThat(result.differences()).anySatisfy(difference -> {
            assertThat(difference.field()).isEqualTo("reversalDispositionStatus");
            assertThat(difference.actual()).isEqualTo("UNKNOWN_DISPOSITION");
        });
    }

    @Test
    void rejectsWrongPlatformPaymentEventTypeEvenWhenStatusesMatch() {
        ReconciliationMatchResult result = matcher.match(line("PAYMENT_SUCCEEDED"),
            success().withPaymentEventType("PAY_FAILED"));

        assertThat(result.primaryIssueType()).contains(PaymentReconciliationIssueType.STATUS_MISMATCH);
        assertThat(result.differences()).anySatisfy(difference -> {
            assertThat(difference.field()).isEqualTo("paymentEventType");
            assertThat(difference.expected()).isEqualTo("PAY_SUCCESS");
            assertThat(difference.actual()).isEqualTo("PAY_FAILED");
        });
    }

    @Test
    void rejectsWrongReversalTypeEvenWhenStatusesMatch() {
        ReconciliationPlatformSnapshot refund = snapshot(1, "order-1", "USD", "12.34", true,
            "REFUND_SUCCEEDED", "SUCCEEDED", "REFUNDED", "PROCESSED", "COMPLETED", null, false, true)
            .withReversalType("CHARGEBACK");

        ReconciliationMatchResult result = matcher.match(line("REFUND_SUCCEEDED"), refund);

        assertThat(result.primaryIssueType()).contains(PaymentReconciliationIssueType.STATUS_MISMATCH);
        assertThat(result.differences()).anySatisfy(difference -> {
            assertThat(difference.field()).isEqualTo("reversalType");
            assertThat(difference.expected()).isEqualTo("REFUND");
            assertThat(difference.actual()).isEqualTo("CHARGEBACK");
        });
    }

    @Test
    void distinguishesAbsentWebhookFromPresentIncompatibleWebhookType() {
        ReconciliationMatchResult absent = matcher.match(line("PAYMENT_SUCCEEDED"),
            success().withWebhookPresent(false));
        ReconciliationMatchResult incompatible = matcher.match(line("PAYMENT_SUCCEEDED"),
            success().withWebhookEventType("PAYMENT_FAILED"));

        assertThat(absent.differences()).extracting(ReconciliationDifference::issueType)
            .contains(PaymentReconciliationIssueType.EVENT_MISSING);
        assertThat(incompatible.primaryIssueType()).contains(PaymentReconciliationIssueType.UNSUPPORTED_RECORD);
        assertThat(incompatible.differences()).noneMatch(difference ->
            difference.issueType() == PaymentReconciliationIssueType.EVENT_MISSING);
        assertThat(incompatible.differences()).anySatisfy(difference -> {
            assertThat(difference.field()).isEqualTo("webhookEventType");
            assertThat(difference.expected()).isEqualTo("PAYMENT_SUCCEEDED");
            assertThat(difference.actual()).isEqualTo("PAYMENT_FAILED");
        });
    }

    @Test
    void statusMismatchPrecedesIncompatibleWebhookUnsupportedEvidence() {
        ReconciliationMatchResult result = matcher.match(line("PAYMENT_SUCCEEDED"),
            success().withWebhookEventType("PAYMENT_FAILED").withOrderStatus("PENDING"));

        assertThat(result.primaryIssueType()).contains(PaymentReconciliationIssueType.STATUS_MISMATCH);
        assertThat(result.differences()).extracting(ReconciliationDifference::issueType)
            .containsExactly(PaymentReconciliationIssueType.STATUS_MISMATCH,
                PaymentReconciliationIssueType.UNSUPPORTED_RECORD);
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(ints = {0, 2})
    void unresolvedOrAmbiguousIdentityDoesNotProduceCandidateFieldDifferences(int candidates) {
        ReconciliationPlatformSnapshot unresolved = snapshot(candidates, null, "EUR", "99", false,
            "PAYMENT_FAILED", null, null, null, null, "UNKNOWN_DISPOSITION", false, false);

        ReconciliationMatchResult result = matcher.match(line("PAYMENT_SUCCEEDED"), unresolved);

        assertThat(result.differences()).extracting(ReconciliationDifference::issueType)
            .containsExactly(PaymentReconciliationIssueType.PLATFORM_RECORD_MISSING);
    }

    @Test
    void platformMissingPrecedesIndependentPriorStatementDuplicateAndStopsCandidateComparison() {
        ReconciliationPlatformSnapshot unresolvedDuplicate = snapshot(0, null, "EUR", "99", false,
            "PAYMENT_FAILED", null, null, null, null, "UNKNOWN_DISPOSITION", true, false);

        ReconciliationMatchResult result = matcher.match(line("PAYMENT_SUCCEEDED"), unresolvedDuplicate);

        assertThat(result.differences()).extracting(ReconciliationDifference::issueType).containsExactly(
            PaymentReconciliationIssueType.PLATFORM_RECORD_MISSING,
            PaymentReconciliationIssueType.DUPLICATE_PROVIDER_RECORD);
    }

    @Test
    @SuppressWarnings("unchecked")
    void diagnosticDifferencesAreDeeplyImmutableAndSerializationRemainsStable() throws Exception {
        ReconciliationMatchResult result = matcher.match(line("PAYMENT_SUCCEEDED"),
            success().withOrderStatus("PENDING"));
        ReconciliationDifference status = result.differences().get(0);
        Map<String, Object> actual = (Map<String, Object>) status.actual();
        String before = result.diagnosticSnapshotJson();

        assertThatThrownBy(() -> actual.put("orderStatus", "CREDITED"))
            .isInstanceOf(UnsupportedOperationException.class);

        Map<String, Object> nested = new LinkedHashMap<>();
        nested.put("states", new java.util.ArrayList<>(List.of("PENDING")));
        ReconciliationDifference difference = new ReconciliationDifference(
            PaymentReconciliationIssueType.STATUS_MISMATCH, "nested", nested, null);
        Map<String, Object> immutableExpected = (Map<String, Object>) difference.expected();
        assertThatThrownBy(() -> ((List<String>) immutableExpected.get("states")).add("COMPLETED"))
            .isInstanceOf(UnsupportedOperationException.class);

        assertThat(objectMapper.writeValueAsString(result.differences())).isEqualTo(before);
    }

    @ParameterizedTest
    @MethodSource("duplicateWithLowerPriorityDifference")
    void duplicatePriorStatementEvidenceIsIdentityFirstAndKeepsLowerDifference(
        PaymentReconciliationIssueType lowerType, ReconciliationPlatformSnapshot platform) {
        ReconciliationMatchResult result = matcher.match(line("PAYMENT_SUCCEEDED"), platform);

        assertThat(result.primaryIssueType()).contains(PaymentReconciliationIssueType.DUPLICATE_PROVIDER_RECORD);
        assertThat(result.differences().get(0).issueType())
            .isEqualTo(PaymentReconciliationIssueType.DUPLICATE_PROVIDER_RECORD);
        assertThat(result.differences()).extracting(ReconciliationDifference::issueType).contains(lowerType);
    }

    static Stream<Arguments> duplicateWithLowerPriorityDifference() {
        return Stream.of(
            Arguments.of(PaymentReconciliationIssueType.ORDER_IDENTITY_MISMATCH,
                success().withDuplicatePriorStatementEvidence(true).withPurchaseOrderNo("other")),
            Arguments.of(PaymentReconciliationIssueType.CURRENCY_MISMATCH,
                success().withDuplicatePriorStatementEvidence(true).withCurrency("EUR")),
            Arguments.of(PaymentReconciliationIssueType.AMOUNT_MISMATCH,
                success().withDuplicatePriorStatementEvidence(true).withAmount(new BigDecimal("99"))),
            Arguments.of(PaymentReconciliationIssueType.EVENT_MISSING,
                success().withDuplicatePriorStatementEvidence(true).withWebhookPresent(false)),
            Arguments.of(PaymentReconciliationIssueType.STATUS_MISMATCH,
                success().withDuplicatePriorStatementEvidence(true).withOrderStatus("PENDING")),
            Arguments.of(PaymentReconciliationIssueType.UNSUPPORTED_RECORD,
                success().withDuplicatePriorStatementEvidence(true).withSupported(false))
        );
    }

    @Test
    void platformMissingDiscoveryIsTenantProviderUtcDayScopedAndExcludesKnownCsvIds() throws Exception {
        Select select = PaymentWebhookEventMapper.class.getMethod("selectReconciliationStatementEvents",
            String.class, Long.class, String.class, Instant.class, Instant.class,
            Instant.class, Long.class, int.class).getAnnotation(Select.class);

        String sql = String.join(" ", select.value()).toLowerCase();
        assertThat(sql).contains("select w.id,w.provider_event_id,w.event_type,w.provider_session_no,w.purchase_order_no")
            .contains("s.pay_currency_code as currency,s.pay_amount as amount")
            .contains("s.id as payment_session_id,s.purchase_order_id,r.id as reversal_id")
            .contains("row_number() over(partition by r0.tenant_id,r0.purchase_order_no")
            .contains("order by r0.create_time desc,r0.id desc")
            .contains("where r0.tenant_id=#{tenantid}")
            .doesNotContain("max(r2.id)")
            .doesNotContain("select *").doesNotContain("raw_body").doesNotContain("signature_digest")
            .contains("w.tenant_id = #{tenantid}")
            .contains("w.provider_code = #{providercode}")
            .contains("w.received_time &gt;= #{windowstart}")
            .contains("w.received_time &lt; #{windownext}")
            .contains("not exists (select 1 from gl_payment_reconciliation_line rl")
            .contains("rl.tenant_id=#{tenantid}")
            .contains("rl.batch_id=#{batchid}")
            .contains("rl.provider_record_id=w.provider_event_id")
            .doesNotContain(" not in ").doesNotContain("knownproviderrecordids")
            .contains("w.received_time &gt; #{cursorreceivedtime}")
            .contains("w.received_time = #{cursorreceivedtime} and w.id &gt; #{cursorid}")
            .contains("order by w.received_time,w.id limit #{limit}");
        assertThat(PaymentWebhookEventMapper.class.getMethod("selectReconciliationStatementEvents",
            String.class, Long.class, String.class, Instant.class, Instant.class,
            Instant.class, Long.class, int.class).getGenericReturnType().getTypeName())
            .contains("ReconciliationPlatformEventProjection");
    }

    @Test
    void platformEventPagerUsesExclusiveCompositeCursorUntilEmptyWithoutDuplicates() {
        Instant time = Instant.parse("2026-07-28T00:00:00Z");
        List<ReconciliationPlatformEventProjection> source = List.of(
            new ReconciliationPlatformEventProjection(1L, "p1", "PAYMENT_SUCCEEDED", time),
            new ReconciliationPlatformEventProjection(2L, "p2", "PAYMENT_SUCCEEDED", time),
            new ReconciliationPlatformEventProjection(3L, "p3", "PAYMENT_FAILED", time.plusSeconds(1)));
        int[] calls = {0};

        List<ReconciliationPlatformEventProjection> collected = new java.util.ArrayList<>();
        ReconciliationPlatformEventPager.forEachPage(
            (cursorTime, cursorId, limit) -> {
                calls[0]++;
                return source.stream().filter(event -> cursorTime == null
                        || event.receivedTime().isAfter(cursorTime)
                        || event.receivedTime().equals(cursorTime) && event.id() > cursorId)
                    .limit(limit).toList();
            }, 2, collected::addAll);

        assertThat(collected).extracting(ReconciliationPlatformEventProjection::id).containsExactly(1L, 2L, 3L);
        assertThat(calls[0]).isEqualTo(3);
        assertThatThrownBy(() -> ReconciliationPlatformEventPager.forEachPage((cursorTime, cursorId, limit) ->
            cursorTime == null ? source.subList(0, 2) : source.subList(1, 3), 2, page -> { }))
            .isInstanceOf(IllegalStateException.class).hasMessageContaining("strictly ordered");
    }

    @Test
    void webhookDiscoveryHasDedicatedTenantProviderTimeIdIndex() throws Exception {
        Path current = Path.of("").toAbsolutePath();
        Path walletSql = null;
        while (current != null && walletSql == null) {
            Path candidate = current.resolve("script/sql/gameluck_wallet.sql");
            if (Files.isRegularFile(candidate)) walletSql = candidate;
            current = current.getParent();
        }
        assertThat(walletSql).isNotNull();
        String sql = Files.readString(walletSql).toLowerCase();
        assertThat(sql).contains("(tenant_id, provider_code, received_time, id)");
        assertThat(sql).contains("(tenant_id, purchase_order_no, create_time, id)");
    }

    private static ReconciliationParsedLine line(String event) {
        return new ReconciliationParsedLine(2, "record-1", event, "provider-session-1", "order-1",
            "USD", new BigDecimal("12.34"), Instant.parse("2026-07-28T00:00:00Z"), "[]",
            ReconciliationParsedLine.Status.VALID, null);
    }

    private static ReconciliationPlatformSnapshot success() {
        return snapshot(1, "order-1", "USD", "12.340000", true, "PAYMENT_SUCCEEDED",
            "SUCCEEDED", "CREDITED", "PROCESSED", null, null, false, true);
    }

    private static ReconciliationPlatformSnapshot snapshot(int candidates, String order, String currency,
                                                            String amount, boolean webhook, String event,
                                                            String sessionStatus, String orderStatus,
                                                            String paymentEventStatus, String reversalStatus,
                                                            String disposition, boolean duplicate, boolean supported) {
        String paymentEventType = switch (event) {
            case "PAYMENT_SUCCEEDED" -> "PAY_SUCCESS";
            case "PAYMENT_FAILED" -> "PAY_FAILED";
            case "PAYMENT_CANCELLED" -> "CANCELLED";
            case "REFUND_SUCCEEDED" -> "REFUNDED";
            case "CHARGEBACK_CREATED" -> "CHARGEBACK";
            default -> null;
        };
        String reversalType = "REFUND_SUCCEEDED".equals(event) ? "REFUND"
            : "CHARGEBACK_CREATED".equals(event) ? "CHARGEBACK" : null;
        return new ReconciliationPlatformSnapshot(candidates, order, currency, new BigDecimal(amount), webhook,
            event, paymentEventType, sessionStatus, orderStatus, paymentEventStatus, reversalStatus, reversalType,
            disposition, duplicate, supported);
    }
}
