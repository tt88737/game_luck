package com.gameluck.payment.service.impl;

import cn.hutool.core.util.IdUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gameluck.common.core.exception.ServiceException;
import com.gameluck.common.tenant.helper.TenantHelper;
import com.gameluck.payment.domain.PaymentSettlementActionLog;
import com.gameluck.payment.domain.PaymentSettlementBatch;
import com.gameluck.payment.domain.PaymentSettlementItem;
import com.gameluck.payment.mapper.PaymentSettlementActionLogMapper;
import com.gameluck.payment.mapper.PaymentSettlementBatchMapper;
import com.gameluck.payment.mapper.PaymentSettlementItemMapper;
import com.gameluck.payment.mapper.PaymentWebhookEventMapper;
import com.gameluck.payment.service.settlement.PaymentSettlementCalculator;
import com.gameluck.payment.service.settlement.SettlementItemDraft;
import com.gameluck.payment.service.settlement.SettlementSourceEvent;
import com.gameluck.payment.service.settlement.SettlementTotals;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PaymentSettlementCalculationService {
    private static final int CHUNK_SIZE = 500;
    private static final BigDecimal ZERO = new BigDecimal("0.000000");
    private final PaymentSettlementBatchMapper batchMapper;
    private final PaymentSettlementItemMapper itemMapper;
    private final PaymentSettlementActionLogMapper actionLogMapper;
    private final PaymentWebhookEventMapper webhookEventMapper;
    private final PaymentSettlementCalculator calculator;
    private final PaymentReconciliationOperatorProvider operatorProvider;
    private final PaymentSettlementFailureRecorder failureRecorder;
    private final ObjectProvider<PaymentSettlementCalculationService> proxyProvider;
    private final ObjectMapper objectMapper;

    @Autowired
    public PaymentSettlementCalculationService(PaymentSettlementBatchMapper batchMapper,
        PaymentSettlementItemMapper itemMapper, PaymentSettlementActionLogMapper actionLogMapper,
        PaymentWebhookEventMapper webhookEventMapper, PaymentSettlementCalculator calculator,
        PaymentReconciliationOperatorProvider operatorProvider, PaymentSettlementFailureRecorder failureRecorder,
        ObjectProvider<PaymentSettlementCalculationService> proxyProvider, ObjectMapper objectMapper) {
        this.batchMapper = batchMapper; this.itemMapper = itemMapper; this.actionLogMapper = actionLogMapper;
        this.webhookEventMapper = webhookEventMapper; this.calculator = calculator;
        this.operatorProvider = operatorProvider; this.failureRecorder = failureRecorder;
        this.proxyProvider = proxyProvider; this.objectMapper = objectMapper;
    }

    public PaymentSettlementBatch calculate(Long batchId) {
        PaymentSettlementCalculationService proxy = proxyProvider == null ? this : proxyProvider.getObject();
        CalculationLease lease = proxy.acquire(batchId);
        try {
            return proxy.reconcile(lease);
        } catch (RuntimeException exception) {
            failureRecorder.recordFailure(lease.tenantId(), lease.batchId(), "SETTLEMENT_CALCULATION_FAILED",
                lease.operatorId(), lease.operatorName());
            throw new ServiceException("payment.settlement.calculate.failed");
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CalculationLease acquire(Long batchId) {
        return acquire(TenantHelper.getTenantId(), batchId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CalculationLease acquire(String tenantId, Long batchId) {
        PaymentSettlementBatch batch = batchMapper.selectByTenantAndId(tenantId, batchId);
        if (batch == null || !"CREATED".equals(batch.getStatus())) throw stateConflict();
        PaymentReconciliationOperatorProvider.Operator operator = operatorProvider.current();
        Long operatorId = operator == null || operator.id() == null ? 0L : operator.id();
        String operatorName = operator == null || operator.name() == null || operator.name().isBlank()
            ? "SYSTEM" : operator.name().trim();
        int version = batch.getVersion() == null ? 0 : batch.getVersion();
        if (batchMapper.transitionStatus(tenantId, batchId, "CREATED", "CALCULATING", new Date()) != 1) {
            throw stateConflict();
        }
        return new CalculationLease(tenantId, batchId, version + 1, operatorId, operatorName,
            batch.getProviderCode(), batch.getCurrencyCode(), batch.getPeriodStart().toInstant(),
            batch.getPeriodEnd().toInstant(), batch.getPaymentFeeRate(), batch.getPaymentFixedFee(),
            batch.getChargebackFixedFee());
    }

    @Transactional
    public PaymentSettlementBatch reconcile(CalculationLease lease) {
        Instant cursorTime = null;
        Long cursorId = null;
        List<SettlementTotals> pageTotals = new ArrayList<>();
        int sourceCount = 0;
        while (true) {
            List<SettlementSourceEvent> page = webhookEventMapper.selectSettlementSourceEvents(
                lease.tenantId(), lease.providerCode(), lease.periodStart(), lease.periodEnd(),
                cursorTime, cursorId, CHUNK_SIZE);
            if (page.isEmpty()) break;
            PaymentSettlementCalculator.Result result = calculator.calculate(page,
                new PaymentSettlementCalculator.Scope(lease.providerCode(), lease.currencyCode(),
                    lease.periodStart(), lease.periodEnd()),
                new PaymentSettlementCalculator.FeeRule(lease.paymentFeeRate(), lease.paymentFixedFee(),
                    lease.chargebackFixedFee()));
            if (!result.items().isEmpty()) {
                List<PaymentSettlementItem> entities = result.items().stream()
                    .map(item -> item(lease, item)).toList();
                if (itemMapper.insertBatch(entities) != entities.size()) throw stateConflict();
                pageTotals.add(result.totals());
                sourceCount += result.items().size();
            }
            SettlementSourceEvent last = page.get(page.size() - 1);
            cursorTime = last.receivedTime(); cursorId = last.webhookEventId();
            if (page.size() < CHUNK_SIZE) break;
        }
        if (sourceCount == 0) throw new ServiceException("payment.settlement.calculate.noEvents");
        SettlementTotals totals = total(pageTotals);
        Date now = new Date();
        String evidence = evidence(totals);
        if (batchMapper.completeCalculation(lease.tenantId(), lease.batchId(), lease.version(),
            totals.eventCount(), totals.paymentCount(), totals.refundCount(), totals.chargebackCount(),
            totals.grossPayment(), totals.refundAmount(), totals.chargebackAmount(), totals.totalFee(),
            totals.netSettlement(), evidence, lease.operatorId(), lease.operatorName(), now) != 1) {
            throw stateConflict();
        }
        actionLogMapper.insert(successLog(lease, evidence, now));
        return calculatedBatch(lease, totals, evidence, now);
    }

    private PaymentSettlementItem item(CalculationLease lease, SettlementItemDraft draft) {
        PaymentSettlementItem item = new PaymentSettlementItem();
        item.setId(IdUtil.getSnowflakeNextId()); item.setTenantId(lease.tenantId()); item.setBatchId(lease.batchId());
        item.setWebhookEventId(draft.webhookEventId()); item.setProviderEventId(draft.providerEventId());
        item.setPaymentSessionId(draft.paymentSessionId()); item.setSessionNo(draft.sessionNo());
        item.setProviderSessionNo(draft.providerSessionNo()); item.setPurchaseOrderId(draft.purchaseOrderId());
        item.setPurchaseOrderNo(draft.purchaseOrderNo()); item.setEventType(draft.eventType());
        item.setReceivedTime(Date.from(draft.receivedTime())); item.setCurrencyCode(draft.currencyCode());
        item.setSourceAmount(draft.sourceAmount()); item.setGrossPayment(draft.grossPayment());
        item.setRefundAmount(draft.refundAmount()); item.setChargebackAmount(draft.chargebackAmount());
        item.setFeeAmount(draft.feeAmount()); item.setNetContribution(draft.netContribution());
        item.setSourceSnapshotJson(draft.sourceSnapshotJson()); item.setCreateTime(new Date()); return item;
    }

    private SettlementTotals total(List<SettlementTotals> pages) {
        int events = 0, payments = 0, refunds = 0, chargebacks = 0;
        BigDecimal gross = ZERO, refund = ZERO, chargeback = ZERO, fee = ZERO, net = ZERO;
        for (SettlementTotals value : pages) {
            events += value.eventCount(); payments += value.paymentCount(); refunds += value.refundCount();
            chargebacks += value.chargebackCount(); gross = gross.add(value.grossPayment());
            refund = refund.add(value.refundAmount()); chargeback = chargeback.add(value.chargebackAmount());
            fee = fee.add(value.totalFee()); net = net.add(value.netSettlement());
        }
        return new SettlementTotals(events, payments, refunds, chargebacks, gross, refund, chargeback, fee, net);
    }

    private String evidence(SettlementTotals totals) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("sourceEventCount", totals.eventCount());
        value.put("calculationVersion", 1);
        try { return objectMapper.writeValueAsString(value); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("Settlement evidence serialization failed"); }
    }

    private PaymentSettlementActionLog successLog(CalculationLease lease, String evidence, Date now) {
        PaymentSettlementActionLog log = new PaymentSettlementActionLog();
        log.setId(IdUtil.getSnowflakeNextId()); log.setTenantId(lease.tenantId()); log.setBatchId(lease.batchId());
        log.setActionType("CALCULATE"); log.setBeforeStatus("CALCULATING"); log.setAfterStatus("CALCULATED");
        log.setOperatorId(lease.operatorId()); log.setOperatorName(lease.operatorName());
        log.setEvidenceSnapshotJson(evidence); log.setCreateTime(now); return log;
    }

    private PaymentSettlementBatch calculatedBatch(CalculationLease lease, SettlementTotals totals,
                                                     String evidence, Date now) {
        PaymentSettlementBatch batch = new PaymentSettlementBatch(); batch.setId(lease.batchId());
        batch.setTenantId(lease.tenantId()); batch.setProviderCode(lease.providerCode());
        batch.setCurrencyCode(lease.currencyCode()); batch.setStatus("CALCULATED");
        batch.setEventCount(totals.eventCount()); batch.setPaymentCount(totals.paymentCount());
        batch.setRefundCount(totals.refundCount()); batch.setChargebackCount(totals.chargebackCount());
        batch.setGrossPayment(totals.grossPayment()); batch.setRefundAmount(totals.refundAmount());
        batch.setChargebackAmount(totals.chargebackAmount()); batch.setTotalFee(totals.totalFee());
        batch.setNetSettlement(totals.netSettlement()); batch.setEvidenceSnapshotJson(evidence);
        batch.setCalculatorId(lease.operatorId()); batch.setCalculatorName(lease.operatorName());
        batch.setCalculatedTime(now); batch.setVersion(lease.version() + 1); batch.setUpdateTime(now); return batch;
    }

    private ServiceException stateConflict() {
        return new ServiceException("payment.settlement.calculate.stateConflict");
    }

    public record CalculationLease(String tenantId, Long batchId, int version, Long operatorId,
        String operatorName, String providerCode, String currencyCode, Instant periodStart, Instant periodEnd,
        BigDecimal paymentFeeRate, BigDecimal paymentFixedFee, BigDecimal chargebackFixedFee) { }
}
