package com.gameluck.payment.service.settlement;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gameluck.common.core.exception.ServiceException;
import com.gameluck.payment.mapper.PaymentWebhookEventMapper;
import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@Tag("local")
class PaymentSettlementCalculatorTest {
    private final PaymentSettlementCalculator calculator = new PaymentSettlementCalculator(new ObjectMapper());
    private final PaymentSettlementCalculator.Scope scope = new PaymentSettlementCalculator.Scope(
        "SIMULATED", "USD", Instant.parse("2026-07-01T00:00:00Z"), Instant.parse("2026-07-02T00:00:00Z"));
    private final PaymentSettlementCalculator.FeeRule fees = new PaymentSettlementCalculator.FeeRule(
        new BigDecimal("0.02900000"), new BigDecimal("0.300000"), new BigDecimal("15.000000"));

    @Test
    void calculatesExactPaymentRefundChargebackFeesAndNegativeNet() {
        PaymentSettlementCalculator.Result result = calculator.calculate(List.of(
            event(1, "PAYMENT_SUCCEEDED", "100.000000", "2026-07-01T00:00:00Z"),
            event(2, "REFUND_SUCCEEDED", "40.000000", "2026-07-01T10:00:00Z"),
            event(3, "CHARGEBACK_CREATED", "60.000000", "2026-07-01T20:00:00Z")), scope, fees);
        SettlementTotals totals = result.totals();
        assertEquals(3, totals.eventCount());
        assertEquals(1, totals.paymentCount()); assertEquals(1, totals.refundCount());
        assertEquals(1, totals.chargebackCount());
        assertEquals(new BigDecimal("100.000000"), totals.grossPayment());
        assertEquals(new BigDecimal("40.000000"), totals.refundAmount());
        assertEquals(new BigDecimal("60.000000"), totals.chargebackAmount());
        assertEquals(new BigDecimal("18.200000"), totals.totalFee());
        assertEquals(new BigDecimal("-18.200000"), totals.netSettlement());
        assertEquals(List.of(new BigDecimal("96.800000"), new BigDecimal("-40.000000"),
            new BigDecimal("-75.000000")), result.items().stream().map(SettlementItemDraft::netContribution).toList());
    }

    @Test
    void roundsPercentageAtSixDecimalsAndSupportsZeroFees() {
        var rounded = calculator.calculate(List.of(event(1, "PAYMENT_SUCCEEDED", "0.333333", "2026-07-01T00:00:00Z")),
            scope, new PaymentSettlementCalculator.FeeRule(new BigDecimal("0.02900000"), BigDecimal.ZERO, BigDecimal.ZERO));
        assertEquals(new BigDecimal("0.009667"), rounded.totals().totalFee());
        assertEquals(new BigDecimal("0.323666"), rounded.totals().netSettlement());
        var free = calculator.calculate(List.of(event(2, "PAYMENT_SUCCEEDED", "10", "2026-07-01T01:00:00Z")),
            scope, new PaymentSettlementCalculator.FeeRule(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
        assertEquals(new BigDecimal("10.000000"), free.totals().netSettlement());
    }

    @Test
    void excludesIneligibleStatusesTypesCurrenciesAndHalfOpenBoundariesThenSortsStably() {
        SettlementSourceEvent end = event(9, "PAYMENT_SUCCEEDED", "9", "2026-07-02T00:00:00Z");
        SettlementSourceEvent failed = withStatus(event(8, "PAYMENT_SUCCEEDED", "8", "2026-07-01T08:00:00Z"), "FAILED");
        SettlementSourceEvent cancel = event(7, "PAYMENT_CANCELLED", "7", "2026-07-01T07:00:00Z");
        SettlementSourceEvent eur = withCurrency(event(6, "PAYMENT_SUCCEEDED", "6", "2026-07-01T06:00:00Z"), "EUR");
        PaymentSettlementCalculator.Result result = calculator.calculate(List.of(end, failed, cancel, eur,
            event(5, "PAYMENT_SUCCEEDED", "5", "2026-07-01T05:00:00Z"),
            event(4, "PAYMENT_SUCCEEDED", "4", "2026-07-01T00:00:00Z")), scope, fees);
        assertEquals(List.of(4L, 5L), result.items().stream().map(SettlementItemDraft::webhookEventId).toList());
        assertEquals(2, result.totals().eventCount());
    }

    @Test
    void rejectsIncludedIdentityOrMoneyMismatchAndRedactsSourceJson() {
        SettlementSourceEvent mismatch = event(1, "PAYMENT_SUCCEEDED", "10", "2026-07-01T01:00:00Z");
        mismatch = new SettlementSourceEvent(mismatch.webhookEventId(), mismatch.providerCode(), mismatch.providerEventId(),
            mismatch.eventType(), mismatch.webhookStatus(), mismatch.providerSessionNo(), mismatch.sessionId(),
            mismatch.sessionNo(), mismatch.sessionPurchaseOrderNo(), mismatch.sessionCurrencyCode(), mismatch.sessionAmount(),
            mismatch.orderId(), "OTHER", mismatch.orderProviderCode(), mismatch.orderCurrencyCode(), mismatch.orderAmount(),
            mismatch.receivedTime());
        SettlementSourceEvent finalMismatch = mismatch;
        assertThrows(ServiceException.class, () -> calculator.calculate(List.of(finalMismatch), scope, fees));

        SettlementSourceEvent currencyMismatch = event(3, "PAYMENT_SUCCEEDED", "10", "2026-07-01T03:00:00Z");
        currencyMismatch = new SettlementSourceEvent(currencyMismatch.webhookEventId(), currencyMismatch.providerCode(),
            currencyMismatch.providerEventId(), currencyMismatch.eventType(), currencyMismatch.webhookStatus(),
            currencyMismatch.providerSessionNo(), currencyMismatch.sessionId(), currencyMismatch.sessionNo(),
            currencyMismatch.sessionPurchaseOrderNo(), "USD", currencyMismatch.sessionAmount(), currencyMismatch.orderId(),
            currencyMismatch.orderPurchaseOrderNo(), currencyMismatch.orderProviderCode(), "EUR",
            currencyMismatch.orderAmount(), currencyMismatch.receivedTime());
        SettlementSourceEvent finalCurrencyMismatch = currencyMismatch;
        assertThrows(ServiceException.class, () -> calculator.calculate(List.of(finalCurrencyMismatch), scope, fees));

        PaymentSettlementCalculator.Result valid = calculator.calculate(List.of(
            event(2, "PAYMENT_SUCCEEDED", "10", "2026-07-01T02:00:00Z")), scope, fees);
        String snapshot = valid.items().get(0).sourceSnapshotJson();
        assertThat(snapshot).contains("providerEventId", "eventType", "sourceAmount")
            .doesNotContainIgnoringCase("rawBody", "signature", "secret", "credential");
    }

    @Test
    void webhookMapperUsesTenantScopedBoundedJoinedSourceQuery() throws Exception {
        Method method = PaymentWebhookEventMapper.class.getMethod("selectSettlementSourceEvents",
            String.class, String.class, Instant.class, Instant.class, Instant.class, Long.class, int.class);
        String sql = String.join(" ", method.getAnnotation(Select.class).value()).replaceAll("\\s+", " ").toLowerCase();
        assertThat(sql).contains("w.tenant_id=#{tenantid}", "w.provider_code=#{providercode}",
            "w.status='processed'", "w.received_time &gt;= #{periodstart}", "w.received_time &lt; #{periodend}",
            "left join gl_payment_session", "left join gl_purchase_order", "order by w.received_time,w.id",
            "limit #{limit}").doesNotContain("raw_body", "signature_digest");
    }

    private static SettlementSourceEvent event(long id, String type, String amount, String received) {
        BigDecimal money = new BigDecimal(amount);
        return new SettlementSourceEvent(id, "SIMULATED", "evt-" + id, type, "PROCESSED", "ps-" + id,
            id + 100, "s-" + id, "o-" + id, "USD", money, id + 200, "o-" + id,
            "SIMULATED", "USD", money, Instant.parse(received));
    }

    private static SettlementSourceEvent withStatus(SettlementSourceEvent e, String status) {
        return new SettlementSourceEvent(e.webhookEventId(), e.providerCode(), e.providerEventId(), e.eventType(), status,
            e.providerSessionNo(), e.sessionId(), e.sessionNo(), e.sessionPurchaseOrderNo(), e.sessionCurrencyCode(),
            e.sessionAmount(), e.orderId(), e.orderPurchaseOrderNo(), e.orderProviderCode(), e.orderCurrencyCode(),
            e.orderAmount(), e.receivedTime());
    }

    private static SettlementSourceEvent withCurrency(SettlementSourceEvent e, String currency) {
        return new SettlementSourceEvent(e.webhookEventId(), e.providerCode(), e.providerEventId(), e.eventType(),
            e.webhookStatus(), e.providerSessionNo(), e.sessionId(), e.sessionNo(), e.sessionPurchaseOrderNo(), currency,
            e.sessionAmount(), e.orderId(), e.orderPurchaseOrderNo(), e.orderProviderCode(), currency,
            e.orderAmount(), e.receivedTime());
    }
}
