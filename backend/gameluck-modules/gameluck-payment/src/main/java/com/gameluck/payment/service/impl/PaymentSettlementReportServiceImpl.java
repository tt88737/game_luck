package com.gameluck.payment.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gameluck.common.core.exception.ServiceException;
import com.gameluck.common.mybatis.core.page.PageQuery;
import com.gameluck.common.tenant.helper.TenantHelper;
import com.gameluck.payment.domain.PaymentSettlementBatch;
import com.gameluck.payment.domain.bo.PaymentSettlementReportQueryBo;
import com.gameluck.payment.domain.vo.PaymentSettlementBatchVo;
import com.gameluck.payment.domain.vo.PaymentSettlementReportPageVo;
import com.gameluck.payment.domain.vo.PaymentSettlementReportRowVo;
import com.gameluck.payment.mapper.PaymentSettlementReportMapper;
import com.gameluck.payment.provider.PaymentProviderRegistry;
import com.gameluck.payment.service.IPaymentSettlementReportService;
import com.gameluck.payment.service.report.SettlementReportCsvWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PaymentSettlementReportServiceImpl implements IPaymentSettlementReportService {
    private final PaymentSettlementReportMapper reportMapper;
    private final PaymentProviderRegistry providerRegistry;
    private final Clock clock;
    private final SettlementReportCsvWriter csvWriter;

    @Override
    public PaymentSettlementReportPageVo queryPage(PaymentSettlementReportQueryBo query, PageQuery pageQuery) {
        Bounds bounds = validate(query);
        String tenantId = TenantHelper.getTenantId();
        Page<PaymentSettlementReportRowVo> page = reportMapper.selectGroupedRows(pageQuery.build(), tenantId,
            bounds.start(), bounds.endExclusive(), bounds.providerCode(), bounds.currencyCode());
        PaymentSettlementReportPageVo result = new PaymentSettlementReportPageVo();
        result.setRows(page.getRecords());
        result.setTotal(page.getTotal());
        result.setCurrencyTotals(reportMapper.selectCurrencyTotals(tenantId, bounds.start(), bounds.endExclusive(),
            bounds.providerCode(), bounds.currencyCode()));
        result.setGeneratedAt(Date.from(clock.instant()));
        return result;
    }

    @Override
    public List<PaymentSettlementBatchVo> queryBatches(LocalDate reportDate, String providerCode, String currencyCode) {
        if (reportDate == null || reportDate.isAfter(LocalDate.now(clock))) {
            throw new ServiceException("payment.settlementReport.group.notFound");
        }
        String provider = provider(providerCode);
        String currency = currency(currencyCode);
        List<PaymentSettlementBatch> batches = reportMapper.selectBatchesByGroup(
            TenantHelper.getTenantId(), reportDate, provider, currency);
        if (batches.isEmpty()) throw new ServiceException("payment.settlementReport.group.notFound");
        return batches.stream().map(this::batchVo).toList();
    }

    @Override
    public byte[] export(PaymentSettlementReportQueryBo query) {
        Bounds bounds = validate(query);
        String tenantId = TenantHelper.getTenantId();
        long rowCount = reportMapper.countGroupedRows(tenantId, bounds.start(), bounds.endExclusive(),
            bounds.providerCode(), bounds.currencyCode());
        if (rowCount > 2000) throw new ServiceException("payment.settlementReport.export.tooLarge");
        return csvWriter.write(reportMapper.selectExportRows(tenantId, bounds.start(), bounds.endExclusive(),
            bounds.providerCode(), bounds.currencyCode()));
    }

    private Bounds validate(PaymentSettlementReportQueryBo query) {
        if (query == null || query.getStartDate() == null || query.getEndDate() == null
            || query.getStartDate().isAfter(query.getEndDate())
            || ChronoUnit.DAYS.between(query.getStartDate(), query.getEndDate()) >= 31) {
            throw new ServiceException("payment.settlementReport.date.invalid");
        }
        if (query.getEndDate().isAfter(LocalDate.now(clock))) {
            throw new ServiceException("payment.settlementReport.date.future");
        }
        Date start = Date.from(query.getStartDate().atStartOfDay(ZoneOffset.UTC).toInstant());
        Date end = Date.from(query.getEndDate().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant());
        return new Bounds(start, end, optionalProvider(query.getProviderCode()), optionalCurrency(query.getCurrencyCode()));
    }

    private String optionalProvider(String value) { return blank(value) ? null : provider(value); }
    private String optionalCurrency(String value) { return blank(value) ? null : currency(value); }

    private String provider(String value) {
        try {
            return providerRegistry.resolve(value).providerCode().trim().toUpperCase(Locale.ROOT);
        } catch (RuntimeException exception) {
            throw new ServiceException("payment.settlementReport.provider.invalid");
        }
    }

    private String currency(String value) {
        if (value == null || !value.trim().matches("[A-Za-z]{3}")) {
            throw new ServiceException("payment.settlementReport.currency.invalid");
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private PaymentSettlementBatchVo batchVo(PaymentSettlementBatch b) {
        PaymentSettlementBatchVo v = new PaymentSettlementBatchVo();
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
        return v;
    }

    private static boolean blank(String value) { return value == null || value.isBlank(); }
    private static String id(Long value) { return value == null ? null : value.toString(); }
    private static String decimal(BigDecimal value, int scale) { return value == null ? null : value.setScale(scale).toPlainString(); }

    private record Bounds(Date start, Date endExclusive, String providerCode, String currencyCode) { }
}
