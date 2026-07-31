package com.gameluck.payment.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gameluck.common.core.exception.ServiceException;
import com.gameluck.common.mybatis.core.page.PageQuery;
import com.gameluck.common.tenant.helper.TenantHelper;
import com.gameluck.payment.domain.bo.PaymentSettlementReportQueryBo;
import com.gameluck.payment.domain.vo.PaymentSettlementBatchVo;
import com.gameluck.payment.domain.vo.PaymentSettlementReportCurrencyTotalVo;
import com.gameluck.payment.domain.vo.PaymentSettlementReportRowVo;
import com.gameluck.payment.mapper.PaymentSettlementReportMapper;
import com.gameluck.payment.provider.PaymentProviderAdapter;
import com.gameluck.payment.provider.PaymentProviderRegistry;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("local")
class PaymentSettlementReportServiceImplTest {

    private final PaymentSettlementReportMapper mapper = mock(PaymentSettlementReportMapper.class);
    private final PaymentProviderRegistry registry = mock(PaymentProviderRegistry.class);
    private final PaymentSettlementReportServiceImpl service = new PaymentSettlementReportServiceImpl(mapper, registry);

    @Test
    void queriesTenantScopedUtcGroupsAndFullFilterCurrencyTotals() {
        PaymentProviderAdapter adapter = mock(PaymentProviderAdapter.class);
        when(adapter.providerCode()).thenReturn("SIMULATED");
        when(registry.resolve("SIMULATED")).thenReturn(adapter);
        PaymentSettlementReportQueryBo query = query(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31));
        query.setProviderCode(" simulated ");
        query.setCurrencyCode(" usd ");
        PaymentSettlementReportRowVo row = new PaymentSettlementReportRowVo();
        row.setSettlementDate("2026-07-31");
        Page<PaymentSettlementReportRowVo> page = new Page<>(2, 20, 1);
        page.setRecords(List.of(row));
        PaymentSettlementReportCurrencyTotalVo total = new PaymentSettlementReportCurrencyTotalVo();
        total.setCurrencyCode("USD");
        when(mapper.selectGroupedRows(any(), eq("tenant-a"), any(), any(), eq("SIMULATED"), eq("USD")))
            .thenReturn(page);
        when(mapper.selectCurrencyTotals(eq("tenant-a"), any(), any(), eq("SIMULATED"), eq("USD")))
            .thenReturn(List.of(total));

        try (MockedStatic<TenantHelper> tenant = mockStatic(TenantHelper.class)) {
            tenant.when(TenantHelper::getTenantId).thenReturn("tenant-a");
            var result = service.queryPage(query, new PageQuery(20, 2));

            assertThat(result.getRows()).containsExactly(row);
            assertThat(result.getTotal()).isEqualTo(1L);
            assertThat(result.getCurrencyTotals()).containsExactly(total);
            assertThat(result.getGeneratedAt()).endsWith("Z");
        }

        Date start = Date.from(LocalDate.of(2026, 7, 1).atStartOfDay().toInstant(ZoneOffset.UTC));
        Date endExclusive = Date.from(LocalDate.of(2026, 8, 1).atStartOfDay().toInstant(ZoneOffset.UTC));
        verify(mapper).selectGroupedRows(any(), eq("tenant-a"), eq(start), eq(endExclusive),
            eq("SIMULATED"), eq("USD"));
        verify(mapper).selectCurrencyTotals("tenant-a", start, endExclusive, "SIMULATED", "USD");
    }

    @Test
    void rejectsReversedOverlongFutureAndInvalidDimensionFilters() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        assertFailure(query(today, today.minusDays(1)), "payment.settlementReport.date.invalid");
        assertFailure(query(today.minusDays(31), today), "payment.settlementReport.date.invalid");
        assertFailure(query(today, today.plusDays(1)), "payment.settlementReport.date.future");

        PaymentSettlementReportQueryBo currency = query(today, today);
        currency.setCurrencyCode("US");
        assertFailure(currency, "payment.settlementReport.currency.invalid");

        PaymentSettlementReportQueryBo provider = query(today, today);
        provider.setProviderCode("missing");
        when(registry.resolve("MISSING")).thenThrow(new IllegalArgumentException("unknown"));
        assertFailure(provider, "payment.settlementReport.provider.invalid");
    }

    @Test
    void returnsExactGroupBatchesAndTreatsAbsentGroupsAsNotFound() {
        PaymentProviderAdapter adapter = mock(PaymentProviderAdapter.class);
        when(adapter.providerCode()).thenReturn("SIMULATED");
        when(registry.resolve("SIMULATED")).thenReturn(adapter);
        PaymentSettlementBatchVo batch = new PaymentSettlementBatchVo();
        batch.setId("2082");
        Date start = Date.from(LocalDate.of(2026, 7, 30).atStartOfDay().toInstant(ZoneOffset.UTC));
        Date end = Date.from(LocalDate.of(2026, 7, 31).atStartOfDay().toInstant(ZoneOffset.UTC));
        when(mapper.selectGroupBatches("tenant-a", start, end, "SIMULATED", "USD"))
            .thenReturn(List.of(batch), List.of());

        try (MockedStatic<TenantHelper> tenant = mockStatic(TenantHelper.class)) {
            tenant.when(TenantHelper::getTenantId).thenReturn("tenant-a");
            assertThat(service.queryBatches(LocalDate.of(2026, 7, 30), " simulated ", " usd "))
                .containsExactly(batch);
            assertThatThrownBy(() -> service.queryBatches(LocalDate.of(2026, 7, 30), "SIMULATED", "USD"))
                .isInstanceOf(ServiceException.class)
                .hasMessage("payment.settlementReport.group.notFound");
        }
    }

    @Test
    void rejectsOversizedExportBeforeReadingRowsAndExportsTheExactLimit() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        PaymentSettlementReportQueryBo query = query(today, today);
        when(mapper.countGroupedRows(eq("tenant-a"), any(), any(), eq(null), eq(null)))
            .thenReturn(2001L, 2000L);
        when(mapper.selectExportRows(eq("tenant-a"), any(), any(), eq(null), eq(null)))
            .thenReturn(List.of());

        try (MockedStatic<TenantHelper> tenant = mockStatic(TenantHelper.class)) {
            tenant.when(TenantHelper::getTenantId).thenReturn("tenant-a");
            assertThatThrownBy(() -> service.export(query)).isInstanceOf(ServiceException.class)
                .hasMessage("payment.settlementReport.export.tooLarge");
            assertThat(service.export(query)).startsWith((byte) 0xEF, (byte) 0xBB, (byte) 0xBF);
        }

        verify(mapper).selectExportRows(eq("tenant-a"), any(), any(), eq(null), eq(null));
    }

    private void assertFailure(PaymentSettlementReportQueryBo query, String message) {
        assertThatThrownBy(() -> service.queryPage(query, new PageQuery(20, 1)))
            .isInstanceOf(ServiceException.class).hasMessage(message);
    }

    private static PaymentSettlementReportQueryBo query(LocalDate start, LocalDate end) {
        PaymentSettlementReportQueryBo query = new PaymentSettlementReportQueryBo();
        query.setStartDate(start);
        query.setEndDate(end);
        return query;
    }
}
