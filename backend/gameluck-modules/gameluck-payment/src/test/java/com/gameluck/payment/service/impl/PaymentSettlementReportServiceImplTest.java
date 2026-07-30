package com.gameluck.payment.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gameluck.common.core.exception.ServiceException;
import com.gameluck.common.mybatis.core.page.PageQuery;
import com.gameluck.common.tenant.helper.TenantHelper;
import com.gameluck.payment.config.PaymentProviderProperties;
import com.gameluck.payment.domain.PaymentSettlementBatch;
import com.gameluck.payment.domain.bo.PaymentSettlementReportQueryBo;
import com.gameluck.payment.domain.vo.PaymentSettlementReportCurrencyTotalVo;
import com.gameluck.payment.domain.vo.PaymentSettlementReportRowVo;
import com.gameluck.payment.mapper.PaymentSettlementReportMapper;
import com.gameluck.payment.provider.PaymentProviderAdapter;
import com.gameluck.payment.provider.PaymentProviderRegistry;
import com.gameluck.payment.service.report.SettlementReportCsvWriter;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("local")
class PaymentSettlementReportServiceImplTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-30T12:00:00Z"), ZoneOffset.UTC);

    @Test
    void queriesGroupedPageAndFullFilterCurrencyTotalsWithinUtcBounds() {
        Fixture f = fixture();
        PaymentSettlementReportRowVo row = row("2026-07-29", "SIMULATED", "USD", "-6.770000");
        Page<PaymentSettlementReportRowVo> page = new Page<>(2, 10, 24);
        page.setRecords(List.of(row));
        when(f.mapper.selectGroupedRows(any(), eq("tenant-a"), any(), any(), eq("SIMULATED"), eq("USD")))
            .thenReturn(page);
        PaymentSettlementReportCurrencyTotalVo total = new PaymentSettlementReportCurrencyTotalVo();
        total.setCurrencyCode("USD"); total.setNetSettlement("-6.770000");
        when(f.mapper.selectCurrencyTotals(eq("tenant-a"), any(), any(), eq("SIMULATED"), eq("USD")))
            .thenReturn(List.of(total));

        PaymentSettlementReportQueryBo query = query("2026-07-01", "2026-07-30", " simulated ", " usd ");
        try (MockedStatic<TenantHelper> tenant = mockStatic(TenantHelper.class)) {
            tenant.when(TenantHelper::getTenantId).thenReturn("tenant-a");
            var result = f.service.queryPage(query, new PageQuery(10, 2));
            assertThat(result.getRows()).containsExactly(row);
            assertThat(result.getTotal()).isEqualTo(24);
            assertThat(result.getCurrencyTotals()).containsExactly(total);
            assertThat(result.getGeneratedAt()).isEqualTo(Date.from(CLOCK.instant()));
        }

        ArgumentCaptor<Date> starts = ArgumentCaptor.forClass(Date.class);
        ArgumentCaptor<Date> ends = ArgumentCaptor.forClass(Date.class);
        verify(f.mapper).selectGroupedRows(any(), eq("tenant-a"), starts.capture(), ends.capture(), eq("SIMULATED"), eq("USD"));
        assertThat(starts.getValue().toInstant()).isEqualTo(Instant.parse("2026-07-01T00:00:00Z"));
        assertThat(ends.getValue().toInstant()).isEqualTo(Instant.parse("2026-07-31T00:00:00Z"));
    }

    @Test
    void rejectsInvalidFutureAndUnsupportedDimensionsBeforeQuery() {
        Fixture f = fixture();
        List<PaymentSettlementReportQueryBo> invalid = List.of(
            query("2026-07-30", "2026-07-29", null, null),
            query("2026-06-29", "2026-07-30", null, null),
            query("2026-07-01", "2026-07-31", null, null),
            query("2026-07-01", "2026-07-30", null, "US"),
            query("2026-07-01", "2026-07-30", "unknown", null));
        try (MockedStatic<TenantHelper> tenant = mockStatic(TenantHelper.class)) {
            tenant.when(TenantHelper::getTenantId).thenReturn("tenant-a");
            invalid.forEach(value -> assertThrows(ServiceException.class,
                () -> f.service.queryPage(value, new PageQuery(10, 1))));
        }
        verifyNoInteractions(f.mapper);
    }

    @Test
    void returnsEmptyReportAndQueriesExactVisibleBatchGroup() {
        Fixture f = fixture();
        Page<PaymentSettlementReportRowVo> empty = new Page<>(1, 10, 0);
        empty.setRecords(List.of());
        when(f.mapper.selectGroupedRows(any(), anyString(), any(), any(), isNull(), isNull())).thenReturn(empty);
        when(f.mapper.selectCurrencyTotals(anyString(), any(), any(), isNull(), isNull())).thenReturn(List.of());
        PaymentSettlementBatch batch = new PaymentSettlementBatch();
        batch.setId(7L); batch.setSettlementNo("PST7"); batch.setStatus("CLOSED");
        batch.setProviderCode("SIMULATED"); batch.setCurrencyCode("USD");
        batch.setGrossPayment(new BigDecimal("30.000000")); batch.setNetSettlement(new BigDecimal("-6.770000"));
        when(f.mapper.selectBatchesByGroup(eq("tenant-a"), eq(LocalDate.parse("2026-07-29")),
            eq("SIMULATED"), eq("USD"))).thenReturn(List.of(batch));
        try (MockedStatic<TenantHelper> tenant = mockStatic(TenantHelper.class)) {
            tenant.when(TenantHelper::getTenantId).thenReturn("tenant-a");
            var result = f.service.queryPage(query("2026-07-24", "2026-07-30", null, null), new PageQuery(10, 1));
            assertThat(result.getRows()).isEmpty();
            assertThat(result.getCurrencyTotals()).isEmpty();
            var batches = f.service.queryBatches(LocalDate.parse("2026-07-29"), " simulated ", " usd ");
            assertThat(batches).singleElement().satisfies(value -> {
                assertThat(value.getId()).isEqualTo("7");
                assertThat(value.getGrossPayment()).isEqualTo("30.000000");
                assertThat(value.getNetSettlement()).isEqualTo("-6.770000");
            });
        }
    }

    @Test
    void absentOrInvalidDrillDownGroupLooksNotFound() {
        Fixture f = fixture();
        when(f.mapper.selectBatchesByGroup(anyString(), any(), anyString(), anyString())).thenReturn(List.of());
        try (MockedStatic<TenantHelper> tenant = mockStatic(TenantHelper.class)) {
            tenant.when(TenantHelper::getTenantId).thenReturn("tenant-a");
            ServiceException absent = assertThrows(ServiceException.class,
                () -> f.service.queryBatches(LocalDate.parse("2026-07-29"), "SIMULATED", "USD"));
            assertThat(absent.getMessage()).isEqualTo("payment.settlementReport.group.notFound");
            assertThrows(ServiceException.class,
                () -> f.service.queryBatches(LocalDate.parse("2026-07-31"), "SIMULATED", "USD"));
        }
    }

    @Test
    void rejectsOversizedExportBeforeLoadingRowsAndExportsFullBoundedFilterOnce() {
        Fixture f = fixture();
        PaymentSettlementReportQueryBo query = query("2026-07-24", "2026-07-30", "SIMULATED", "USD");
        when(f.mapper.countGroupedRows(anyString(), any(), any(), anyString(), anyString())).thenReturn(2001L);
        try (MockedStatic<TenantHelper> tenant = mockStatic(TenantHelper.class)) {
            tenant.when(TenantHelper::getTenantId).thenReturn("tenant-a");
            ServiceException error = assertThrows(ServiceException.class, () -> f.service.export(query));
            assertThat(error.getMessage()).isEqualTo("payment.settlementReport.export.tooLarge");
            verify(f.mapper, never()).selectExportRows(anyString(), any(), any(), any(), any());

            reset(f.mapper);
            when(f.mapper.countGroupedRows(anyString(), any(), any(), anyString(), anyString())).thenReturn(2000L);
            when(f.mapper.selectExportRows(anyString(), any(), any(), anyString(), anyString()))
                .thenReturn(List.of(row("2026-07-29", "SIMULATED", "USD", "-6.770000")));
            byte[] csv = f.service.export(query);
            assertThat(csv).startsWith((byte) 0xEF, (byte) 0xBB, (byte) 0xBF);
            verify(f.mapper, times(1)).selectExportRows(eq("tenant-a"), any(), any(), eq("SIMULATED"), eq("USD"));
        }
    }

    private static Fixture fixture() {
        PaymentSettlementReportMapper mapper = mock(PaymentSettlementReportMapper.class);
        PaymentProviderAdapter adapter = mock(PaymentProviderAdapter.class);
        when(adapter.providerCode()).thenReturn("SIMULATED");
        PaymentProviderProperties properties = new PaymentProviderProperties();
        properties.getSimulated().setEnabled(true);
        PaymentProviderRegistry registry = new PaymentProviderRegistry(List.of(adapter), properties);
        return new Fixture(mapper, new PaymentSettlementReportServiceImpl(mapper, registry, CLOCK,
            new SettlementReportCsvWriter()));
    }

    private static PaymentSettlementReportQueryBo query(String start, String end, String provider, String currency) {
        PaymentSettlementReportQueryBo value = new PaymentSettlementReportQueryBo();
        value.setStartDate(LocalDate.parse(start)); value.setEndDate(LocalDate.parse(end));
        value.setProviderCode(provider); value.setCurrencyCode(currency); return value;
    }

    private static PaymentSettlementReportRowVo row(String date, String provider, String currency, String net) {
        PaymentSettlementReportRowVo value = new PaymentSettlementReportRowVo();
        value.setReportDate(LocalDate.parse(date)); value.setProviderCode(provider); value.setCurrencyCode(currency);
        value.setNetSettlement(net); value.setNegativeNet(net.startsWith("-")); return value;
    }

    private record Fixture(PaymentSettlementReportMapper mapper, PaymentSettlementReportServiceImpl service) { }
}
