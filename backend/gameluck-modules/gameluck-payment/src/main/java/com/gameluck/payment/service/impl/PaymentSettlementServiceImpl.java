package com.gameluck.payment.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gameluck.common.core.exception.ServiceException;
import com.gameluck.common.mybatis.core.page.PageQuery;
import com.gameluck.common.mybatis.core.page.TableDataInfo;
import com.gameluck.common.tenant.helper.TenantHelper;
import com.gameluck.payment.domain.PaymentSettlementActionLog;
import com.gameluck.payment.domain.PaymentSettlementBatch;
import com.gameluck.payment.domain.PaymentSettlementItem;
import com.gameluck.payment.domain.bo.PaymentSettlementCreateBo;
import com.gameluck.payment.domain.bo.PaymentSettlementQueryBo;
import com.gameluck.payment.domain.bo.PaymentSettlementCloseBo;
import com.gameluck.payment.domain.vo.PaymentSettlementActionLogVo;
import com.gameluck.payment.domain.vo.PaymentSettlementBatchVo;
import com.gameluck.payment.domain.vo.PaymentSettlementDetailVo;
import com.gameluck.payment.domain.vo.PaymentSettlementItemVo;
import com.gameluck.payment.mapper.PaymentSettlementActionLogMapper;
import com.gameluck.payment.mapper.PaymentSettlementBatchMapper;
import com.gameluck.payment.mapper.PaymentSettlementItemMapper;
import com.gameluck.payment.provider.PaymentProviderRegistry;
import com.gameluck.payment.service.IPaymentSettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.Date;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PaymentSettlementServiceImpl implements IPaymentSettlementService {
    private static final BigDecimal ZERO_MONEY = new BigDecimal("0.000000");
    private final PaymentSettlementBatchMapper batchMapper;
    private final PaymentSettlementItemMapper itemMapper;
    private final PaymentSettlementActionLogMapper actionLogMapper;
    private final PaymentProviderRegistry providerRegistry;
    private final PaymentReconciliationOperatorProvider operatorProvider;
    private PaymentSettlementCalculationService calculationService;
    private PaymentSettlementCloseService closeService;

    @org.springframework.beans.factory.annotation.Autowired
    void setCalculationService(PaymentSettlementCalculationService calculationService) {
        this.calculationService = calculationService;
    }

    @org.springframework.beans.factory.annotation.Autowired
    void setCloseService(PaymentSettlementCloseService closeService) {
        this.closeService = closeService;
    }

    @Override
    public PaymentSettlementDetailVo close(Long batchId, PaymentSettlementCloseBo bo) {
        if (closeService == null) throw new ServiceException("payment.settlement.close.unavailable");
        closeService.close(TenantHelper.getTenantId(), batchId, bo);
        return queryDetail(batchId);
    }

    @Override
    public PaymentSettlementDetailVo calculate(Long batchId) {
        if (calculationService == null) throw new ServiceException("payment.settlement.calculate.unavailable");
        calculationService.calculate(batchId);
        return queryDetail(batchId);
    }

    @Override
    @Transactional
    public PaymentSettlementDetailVo create(PaymentSettlementCreateBo bo) {
        validateCreate(bo);
        String tenantId = TenantHelper.getTenantId();
        String providerCode;
        try {
            providerCode = providerRegistry.resolve(bo.getProviderCode()).providerCode().trim().toUpperCase(Locale.ROOT);
        } catch (RuntimeException e) {
            throw new ServiceException("payment.settlement.provider.invalid");
        }
        String currencyCode = bo.getCurrencyCode().trim().toUpperCase(Locale.ROOT);
        if (batchMapper.countOverlapping(tenantId, providerCode, currencyCode,
            bo.getPeriodStart(), bo.getPeriodEnd(), null) > 0) {
            throw new ServiceException("payment.settlement.overlap");
        }
        PaymentReconciliationOperatorProvider.Operator operator = operatorProvider.current();
        if (operator.id() == null || operator.name() == null || operator.name().isBlank()) {
            throw new ServiceException("payment.settlement.operator.required");
        }
        long id = IdUtil.getSnowflakeNextId();
        Date now = new Date();
        PaymentSettlementBatch batch = new PaymentSettlementBatch();
        batch.setId(id); batch.setTenantId(tenantId); batch.setSettlementNo("PST" + id);
        batch.setProviderCode(providerCode); batch.setCurrencyCode(currencyCode);
        batch.setPeriodStart(bo.getPeriodStart()); batch.setPeriodEnd(bo.getPeriodEnd());
        batch.setStatus("CREATED"); batch.setPaymentFeeRate(rate(bo.getPaymentFeeRate()));
        batch.setPaymentFixedFee(money(bo.getPaymentFixedFee()));
        batch.setChargebackFixedFee(money(bo.getChargebackFixedFee()));
        batch.setEventCount(0); batch.setPaymentCount(0); batch.setRefundCount(0); batch.setChargebackCount(0);
        batch.setGrossPayment(ZERO_MONEY); batch.setRefundAmount(ZERO_MONEY);
        batch.setChargebackAmount(ZERO_MONEY); batch.setTotalFee(ZERO_MONEY); batch.setNetSettlement(ZERO_MONEY);
        batch.setReconciliationCoverageCount(0); batch.setOpenIssueCount(0);
        batch.setCreatorId(operator.id()); batch.setCreatorName(operator.name().trim());
        batch.setVersion(0); batch.setCreateTime(now);
        batchMapper.insert(batch);
        PaymentSettlementActionLog log = new PaymentSettlementActionLog();
        log.setId(IdUtil.getSnowflakeNextId()); log.setTenantId(tenantId); log.setBatchId(id);
        log.setActionType("CREATE"); log.setAfterStatus("CREATED"); log.setOperatorId(operator.id());
        log.setOperatorName(operator.name().trim()); log.setCreateTime(now);
        actionLogMapper.insert(log);
        return detail(batch, java.util.List.of(actionLogVo(log)));
    }

    @Override
    public TableDataInfo<PaymentSettlementBatchVo> queryPage(PaymentSettlementQueryBo bo, PageQuery pageQuery) {
        PaymentSettlementQueryBo query = bo == null ? new PaymentSettlementQueryBo() : bo;
        Page<PaymentSettlementBatch> page = batchMapper.selectPageByTenant(pageQuery.build(), TenantHelper.getTenantId(),
            normalize(query.getProviderCode()), normalize(query.getCurrencyCode()), normalize(query.getStatus()));
        Page<PaymentSettlementBatchVo> projected = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        projected.setRecords(page.getRecords().stream().map(this::batchVo).toList());
        return TableDataInfo.build(projected);
    }

    @Override
    public PaymentSettlementDetailVo queryDetail(Long batchId) {
        String tenantId = TenantHelper.getTenantId();
        PaymentSettlementBatch batch = requiredBatch(tenantId, batchId);
        return detail(batch, actionLogMapper.selectByBatch(tenantId, batchId).stream().map(this::actionLogVo).toList());
    }

    @Override
    public TableDataInfo<PaymentSettlementItemVo> queryItems(Long batchId, String eventType, PageQuery pageQuery) {
        String tenantId = TenantHelper.getTenantId();
        requiredBatch(tenantId, batchId);
        Page<PaymentSettlementItem> page = itemMapper.selectPageByBatch(pageQuery.build(), tenantId, batchId,
            normalize(eventType));
        Page<PaymentSettlementItemVo> projected = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        projected.setRecords(page.getRecords().stream().map(this::itemVo).toList());
        return TableDataInfo.build(projected);
    }

    private void validateCreate(PaymentSettlementCreateBo bo) {
        if (bo == null || bo.getProviderCode() == null || bo.getProviderCode().isBlank()
            || bo.getCurrencyCode() == null || !bo.getCurrencyCode().trim().matches("[A-Za-z]{3}")
            || bo.getPeriodStart() == null || bo.getPeriodEnd() == null
            || bo.getPaymentFeeRate() == null || bo.getPaymentFixedFee() == null
            || bo.getChargebackFixedFee() == null) throw new ServiceException("payment.settlement.input.invalid");
        if (!bo.getPeriodStart().before(bo.getPeriodEnd()) || bo.getPeriodEnd().after(new Date())
            || Duration.between(bo.getPeriodStart().toInstant(), bo.getPeriodEnd().toInstant()).compareTo(Duration.ofDays(31)) > 0)
            throw new ServiceException("payment.settlement.window.invalid");
        if (bo.getPaymentFeeRate().signum() < 0 || bo.getPaymentFeeRate().compareTo(BigDecimal.ONE) > 0
            || bo.getPaymentFeeRate().scale() > 8 || bo.getPaymentFixedFee().signum() < 0
            || bo.getChargebackFixedFee().signum() < 0 || bo.getPaymentFixedFee().scale() > 6
            || bo.getChargebackFixedFee().scale() > 6)
            throw new ServiceException("payment.settlement.fee.invalid");
    }

    private PaymentSettlementBatch requiredBatch(String tenantId, Long id) {
        PaymentSettlementBatch batch = id == null ? null : batchMapper.selectByTenantAndId(tenantId, id);
        if (batch == null) throw new ServiceException("payment.settlement.batch.notFound");
        return batch;
    }

    private PaymentSettlementBatchVo batchVo(PaymentSettlementBatch b) {
        PaymentSettlementBatchVo v = new PaymentSettlementBatchVo(); copyBatch(b, v); return v;
    }

    private PaymentSettlementDetailVo detail(PaymentSettlementBatch b,
                                               java.util.List<PaymentSettlementActionLogVo> logs) {
        PaymentSettlementDetailVo v = new PaymentSettlementDetailVo(); copyBatch(b, v);
        v.setEvidenceSnapshotJson(b.getEvidenceSnapshotJson()); v.setActionLogs(logs); return v;
    }

    private void copyBatch(PaymentSettlementBatch b, PaymentSettlementBatchVo v) {
        v.setId(id(b.getId())); v.setSettlementNo(b.getSettlementNo()); v.setProviderCode(b.getProviderCode());
        v.setCurrencyCode(b.getCurrencyCode()); v.setPeriodStart(b.getPeriodStart()); v.setPeriodEnd(b.getPeriodEnd());
        v.setStatus(b.getStatus()); v.setPaymentFeeRate(decimal(b.getPaymentFeeRate(), 8));
        v.setPaymentFixedFee(decimal(b.getPaymentFixedFee(), 6)); v.setChargebackFixedFee(decimal(b.getChargebackFixedFee(), 6));
        v.setEventCount(b.getEventCount()); v.setPaymentCount(b.getPaymentCount()); v.setRefundCount(b.getRefundCount());
        v.setChargebackCount(b.getChargebackCount()); v.setGrossPayment(decimal(b.getGrossPayment(), 6));
        v.setRefundAmount(decimal(b.getRefundAmount(), 6)); v.setChargebackAmount(decimal(b.getChargebackAmount(), 6));
        v.setTotalFee(decimal(b.getTotalFee(), 6)); v.setNetSettlement(decimal(b.getNetSettlement(), 6));
        v.setReconciliationCoverageCount(b.getReconciliationCoverageCount()); v.setOpenIssueCount(b.getOpenIssueCount());
        v.setFailureReason(b.getFailureReason()); v.setCreatorId(id(b.getCreatorId())); v.setCreatorName(b.getCreatorName());
        v.setCalculatorId(id(b.getCalculatorId())); v.setCalculatorName(b.getCalculatorName());
        v.setCloserId(id(b.getCloserId())); v.setCloserName(b.getCloserName()); v.setCloseRemark(b.getCloseRemark());
        v.setCalculatedTime(b.getCalculatedTime()); v.setClosedTime(b.getClosedTime()); v.setVersion(b.getVersion());
        v.setCreateTime(b.getCreateTime()); v.setUpdateTime(b.getUpdateTime());
    }

    private PaymentSettlementItemVo itemVo(PaymentSettlementItem i) {
        PaymentSettlementItemVo v = new PaymentSettlementItemVo();
        v.setId(id(i.getId())); v.setBatchId(id(i.getBatchId())); v.setWebhookEventId(id(i.getWebhookEventId()));
        v.setProviderEventId(i.getProviderEventId()); v.setPaymentSessionId(id(i.getPaymentSessionId()));
        v.setSessionNo(i.getSessionNo()); v.setProviderSessionNo(i.getProviderSessionNo());
        v.setPurchaseOrderId(id(i.getPurchaseOrderId())); v.setPurchaseOrderNo(i.getPurchaseOrderNo());
        v.setEventType(i.getEventType()); v.setReceivedTime(i.getReceivedTime()); v.setCurrencyCode(i.getCurrencyCode());
        v.setSourceAmount(decimal(i.getSourceAmount(), 6)); v.setGrossPayment(decimal(i.getGrossPayment(), 6));
        v.setRefundAmount(decimal(i.getRefundAmount(), 6)); v.setChargebackAmount(decimal(i.getChargebackAmount(), 6));
        v.setFeeAmount(decimal(i.getFeeAmount(), 6)); v.setNetContribution(decimal(i.getNetContribution(), 6));
        v.setSourceSnapshotJson(i.getSourceSnapshotJson()); v.setCreateTime(i.getCreateTime()); return v;
    }

    private PaymentSettlementActionLogVo actionLogVo(PaymentSettlementActionLog l) {
        PaymentSettlementActionLogVo v = new PaymentSettlementActionLogVo();
        v.setId(id(l.getId())); v.setBatchId(id(l.getBatchId())); v.setActionType(l.getActionType());
        v.setBeforeStatus(l.getBeforeStatus()); v.setAfterStatus(l.getAfterStatus()); v.setOperatorId(id(l.getOperatorId()));
        v.setOperatorName(l.getOperatorName()); v.setRemark(l.getRemark());
        v.setEvidenceSnapshotJson(l.getEvidenceSnapshotJson()); v.setCreateTime(l.getCreateTime()); return v;
    }

    private BigDecimal money(BigDecimal value) { return value.setScale(6, RoundingMode.UNNECESSARY); }
    private BigDecimal rate(BigDecimal value) { return value.setScale(8, RoundingMode.UNNECESSARY); }
    private String decimal(BigDecimal value, int scale) { return value == null ? null : value.setScale(scale).toPlainString(); }
    private String id(Long value) { return value == null ? null : value.toString(); }
    private String normalize(String value) { return value == null || value.isBlank() ? null : value.trim().toUpperCase(Locale.ROOT); }
}
