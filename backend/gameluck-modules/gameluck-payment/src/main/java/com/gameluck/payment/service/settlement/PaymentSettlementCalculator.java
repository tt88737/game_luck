package com.gameluck.payment.service.settlement;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gameluck.common.core.exception.ServiceException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class PaymentSettlementCalculator {
    private static final int MONEY_SCALE = 6;
    private static final BigDecimal ZERO = new BigDecimal("0.000000");
    private static final Set<String> FINANCIAL_TYPES = Set.of(
        "PAYMENT_SUCCEEDED", "REFUND_SUCCEEDED", "CHARGEBACK_CREATED");

    private final ObjectMapper objectMapper;

    public PaymentSettlementCalculator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Result calculate(List<SettlementSourceEvent> sourceEvents, Scope scope, FeeRule feeRule) {
        List<SettlementItemDraft> items = sourceEvents.stream()
            .filter(event -> eligible(event, scope))
            .sorted(Comparator.comparing(SettlementSourceEvent::receivedTime)
                .thenComparing(SettlementSourceEvent::webhookEventId))
            .map(event -> calculateItem(event, scope, feeRule))
            .toList();
        return new Result(items, total(items));
    }

    private boolean eligible(SettlementSourceEvent event, Scope scope) {
        return event != null && "PROCESSED".equals(event.webhookStatus())
            && FINANCIAL_TYPES.contains(event.eventType())
            && scope.providerCode().equals(event.providerCode())
            && scope.currencyCode().equals(event.sessionCurrencyCode())
            && event.receivedTime() != null && !event.receivedTime().isBefore(scope.periodStart())
            && event.receivedTime().isBefore(scope.periodEnd());
    }

    private SettlementItemDraft calculateItem(SettlementSourceEvent event, Scope scope, FeeRule feeRule) {
        validateIdentity(event, scope);
        BigDecimal amount = money(event.sessionAmount());
        BigDecimal gross = ZERO;
        BigDecimal refund = ZERO;
        BigDecimal chargeback = ZERO;
        BigDecimal fee = ZERO;
        switch (event.eventType()) {
            case "PAYMENT_SUCCEEDED" -> {
                gross = amount;
                fee = amount.multiply(feeRule.paymentFeeRate()).setScale(MONEY_SCALE, RoundingMode.HALF_UP)
                    .add(money(feeRule.paymentFixedFee()));
            }
            case "REFUND_SUCCEEDED" -> refund = amount;
            case "CHARGEBACK_CREATED" -> {
                chargeback = amount;
                fee = money(feeRule.chargebackFixedFee());
            }
            default -> throw new ServiceException("payment.settlement.event.unsupported");
        }
        BigDecimal net = gross.subtract(refund).subtract(chargeback).subtract(fee).setScale(MONEY_SCALE);
        return new SettlementItemDraft(event.webhookEventId(), event.providerEventId(), event.sessionId(),
            event.sessionNo(), event.providerSessionNo(), event.orderId(), event.orderPurchaseOrderNo(),
            event.eventType(), event.receivedTime(), scope.currencyCode(), amount, gross, refund,
            chargeback, fee, net, snapshot(event, amount));
    }

    private void validateIdentity(SettlementSourceEvent event, Scope scope) {
        if (event.webhookEventId() == null || event.providerEventId() == null || event.providerEventId().isBlank()
            || event.sessionId() == null || event.orderId() == null || event.providerSessionNo() == null
            || event.sessionNo() == null || event.sessionPurchaseOrderNo() == null
            || !event.sessionPurchaseOrderNo().equals(event.orderPurchaseOrderNo())
            || !scope.providerCode().equals(event.orderProviderCode())
            || !scope.currencyCode().equals(event.orderCurrencyCode())
            || event.sessionAmount() == null || event.orderAmount() == null
            || event.sessionAmount().compareTo(event.orderAmount()) != 0) {
            throw new ServiceException("payment.settlement.source.mismatch");
        }
    }

    private SettlementTotals total(List<SettlementItemDraft> items) {
        int payments = 0, refunds = 0, chargebacks = 0;
        BigDecimal gross = ZERO, refund = ZERO, chargeback = ZERO, fee = ZERO, net = ZERO;
        for (SettlementItemDraft item : items) {
            if ("PAYMENT_SUCCEEDED".equals(item.eventType())) payments++;
            else if ("REFUND_SUCCEEDED".equals(item.eventType())) refunds++;
            else chargebacks++;
            gross = gross.add(item.grossPayment()); refund = refund.add(item.refundAmount());
            chargeback = chargeback.add(item.chargebackAmount()); fee = fee.add(item.feeAmount());
            net = net.add(item.netContribution());
        }
        return new SettlementTotals(items.size(), payments, refunds, chargebacks, money(gross), money(refund),
            money(chargeback), money(fee), money(net));
    }

    private String snapshot(SettlementSourceEvent event, BigDecimal amount) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("webhookEventId", event.webhookEventId().toString());
        snapshot.put("providerEventId", event.providerEventId());
        snapshot.put("eventType", event.eventType());
        snapshot.put("providerCode", event.providerCode());
        snapshot.put("providerSessionNo", event.providerSessionNo());
        snapshot.put("sessionId", event.sessionId().toString());
        snapshot.put("sessionNo", event.sessionNo());
        snapshot.put("purchaseOrderId", event.orderId().toString());
        snapshot.put("purchaseOrderNo", event.orderPurchaseOrderNo());
        snapshot.put("currencyCode", event.sessionCurrencyCode());
        snapshot.put("sourceAmount", amount.toPlainString());
        snapshot.put("receivedTime", event.receivedTime().toString());
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException e) {
            throw new ServiceException("payment.settlement.snapshot.failed");
        }
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(MONEY_SCALE, RoundingMode.UNNECESSARY);
    }

    public record Scope(String providerCode, String currencyCode, Instant periodStart, Instant periodEnd) { }
    public record FeeRule(BigDecimal paymentFeeRate, BigDecimal paymentFixedFee,
                          BigDecimal chargebackFixedFee) { }
    public record Result(List<SettlementItemDraft> items, SettlementTotals totals) {
        public Result {
            items = List.copyOf(items);
        }
    }
}
