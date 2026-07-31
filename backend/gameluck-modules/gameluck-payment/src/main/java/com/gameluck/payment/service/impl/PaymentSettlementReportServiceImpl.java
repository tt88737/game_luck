package com.gameluck.payment.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gameluck.common.core.exception.ServiceException;
import com.gameluck.common.mybatis.core.page.PageQuery;
import com.gameluck.common.tenant.helper.TenantHelper;
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

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PaymentSettlementReportServiceImpl implements IPaymentSettlementReportService {
    private static final long EXPORT_LIMIT = 2000;
    private final PaymentSettlementReportMapper mapper;
    private final PaymentProviderRegistry providerRegistry;

    @Override
    public PaymentSettlementReportPageVo queryPage(PaymentSettlementReportQueryBo bo, PageQuery pageQuery) {
        NormalizedQuery query = normalize(bo);
        String tenantId = TenantHelper.getTenantId();
        Page<PaymentSettlementReportRowVo> page = mapper.selectGroupedRows(pageQuery.build(), tenantId,
            query.periodStart(), query.periodEndExclusive(), query.providerCode(), query.currencyCode());
        PaymentSettlementReportPageVo result = new PaymentSettlementReportPageVo();
        result.setRows(page.getRecords());
        result.setTotal(page.getTotal());
        result.setCurrencyTotals(mapper.selectCurrencyTotals(tenantId, query.periodStart(),
            query.periodEndExclusive(), query.providerCode(), query.currencyCode()));
        result.setGeneratedAt(Instant.now().toString());
        return result;
    }

    @Override
    public List<PaymentSettlementBatchVo> queryBatches(LocalDate date, String providerCode, String currencyCode) {
        PaymentSettlementReportQueryBo bo = new PaymentSettlementReportQueryBo();
        bo.setStartDate(date);
        bo.setEndDate(date);
        bo.setProviderCode(providerCode);
        bo.setCurrencyCode(currencyCode);
        NormalizedQuery query = normalize(bo);
        List<PaymentSettlementBatchVo> batches = mapper.selectGroupBatches(TenantHelper.getTenantId(),
            query.periodStart(), query.periodEndExclusive(), query.providerCode(), query.currencyCode());
        if (batches.isEmpty()) throw new ServiceException("payment.settlementReport.group.notFound");
        return batches;
    }

    @Override
    public byte[] export(PaymentSettlementReportQueryBo bo) {
        NormalizedQuery query = normalize(bo);
        String tenantId = TenantHelper.getTenantId();
        long count = mapper.countGroupedRows(tenantId, query.periodStart(), query.periodEndExclusive(),
            query.providerCode(), query.currencyCode());
        if (count > EXPORT_LIMIT) throw new ServiceException("payment.settlementReport.export.tooLarge");
        return new SettlementReportCsvWriter().write(mapper.selectExportRows(tenantId, query.periodStart(),
            query.periodEndExclusive(), query.providerCode(), query.currencyCode()));
    }

    private NormalizedQuery normalize(PaymentSettlementReportQueryBo bo) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate start = bo == null || bo.getStartDate() == null ? today.minusDays(6) : bo.getStartDate();
        LocalDate end = bo == null || bo.getEndDate() == null ? today : bo.getEndDate();
        if (start.isAfter(end) || ChronoUnit.DAYS.between(start, end) >= 31) {
            throw new ServiceException("payment.settlementReport.date.invalid");
        }
        if (end.isAfter(today)) throw new ServiceException("payment.settlementReport.date.future");
        String providerCode = normalizeProvider(bo == null ? null : bo.getProviderCode());
        String currencyCode = normalizeCurrency(bo == null ? null : bo.getCurrencyCode());
        return new NormalizedQuery(atUtc(start), atUtc(end.plusDays(1)), providerCode, currencyCode);
    }

    private String normalizeProvider(String value) {
        if (value == null || value.isBlank()) return null;
        String code = value.trim().toUpperCase(Locale.ROOT);
        try {
            return providerRegistry.resolve(code).providerCode().trim().toUpperCase(Locale.ROOT);
        } catch (RuntimeException exception) {
            throw new ServiceException("payment.settlementReport.provider.invalid");
        }
    }

    private String normalizeCurrency(String value) {
        if (value == null || value.isBlank()) return null;
        String code = value.trim().toUpperCase(Locale.ROOT);
        if (!code.matches("[A-Z]{3}")) throw new ServiceException("payment.settlementReport.currency.invalid");
        return code;
    }

    private Date atUtc(LocalDate date) {
        return Date.from(date.atStartOfDay().toInstant(ZoneOffset.UTC));
    }

    private record NormalizedQuery(Date periodStart, Date periodEndExclusive,
                                   String providerCode, String currencyCode) {
    }
}
