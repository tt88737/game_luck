package com.gameluck.payment.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gameluck.common.core.exception.ServiceException;
import com.gameluck.common.mybatis.core.page.PageQuery;
import com.gameluck.common.tenant.helper.TenantHelper;
import com.gameluck.payment.config.PaymentProviderProperties;
import com.gameluck.payment.domain.PaymentSettlementActionLog;
import com.gameluck.payment.domain.PaymentSettlementBatch;
import com.gameluck.payment.domain.PaymentSettlementItem;
import com.gameluck.payment.domain.bo.PaymentSettlementCreateBo;
import com.gameluck.payment.domain.bo.PaymentSettlementQueryBo;
import com.gameluck.payment.mapper.PaymentSettlementActionLogMapper;
import com.gameluck.payment.mapper.PaymentSettlementBatchMapper;
import com.gameluck.payment.mapper.PaymentSettlementItemMapper;
import com.gameluck.payment.provider.PaymentProviderAdapter;
import com.gameluck.payment.provider.PaymentProviderRegistry;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("local")
class PaymentSettlementServiceImplTest {

    @Test
    void createsTenantScopedImmutableFeeSnapshotAndAuditLog() {
        Fixture f = fixture();
        PaymentSettlementCreateBo bo = validCreate();
        try (MockedStatic<TenantHelper> tenant = mockStatic(TenantHelper.class)) {
            tenant.when(TenantHelper::getTenantId).thenReturn("tenant-a");
            var result = f.service.create(bo);
            assertEquals("CREATED", result.getStatus());
            assertEquals("SIMULATED", result.getProviderCode());
            assertEquals("USD", result.getCurrencyCode());
            assertEquals("0.02900000", result.getPaymentFeeRate());
            assertEquals("0.300000", result.getPaymentFixedFee());
            assertEquals("15.000000", result.getChargebackFixedFee());
            assertNotNull(result.getId());
            assertTrue(result.getSettlementNo().startsWith("PST"));
        }
        ArgumentCaptor<PaymentSettlementBatch> batch = ArgumentCaptor.forClass(PaymentSettlementBatch.class);
        verify(f.batches).insert(batch.capture());
        assertEquals("tenant-a", batch.getValue().getTenantId());
        assertEquals(88L, batch.getValue().getCreatorId());
        assertEquals(0, batch.getValue().getVersion());
        verify(f.logs).insert(argThat(log -> "tenant-a".equals(log.getTenantId())
            && "CREATE".equals(log.getActionType()) && "CREATED".equals(log.getAfterStatus())
            && log.getBatchId().equals(batch.getValue().getId())));
    }

    @Test
    void rejectsInvalidWindowFutureEndCurrencyAndFeesBeforeInsert() {
        Fixture f = fixture();
        List<PaymentSettlementCreateBo> invalid = List.of(
            create("US", "0.029", "0", "0", Instant.now().minus(2, ChronoUnit.DAYS), Instant.now().minus(1, ChronoUnit.DAYS)),
            create("USD", "1.00000001", "0", "0", Instant.now().minus(2, ChronoUnit.DAYS), Instant.now().minus(1, ChronoUnit.DAYS)),
            create("USD", "0.029", "-0.01", "0", Instant.now().minus(2, ChronoUnit.DAYS), Instant.now().minus(1, ChronoUnit.DAYS)),
            create("USD", "0.029", "0", "0", Instant.now().minus(1, ChronoUnit.DAYS), Instant.now().minus(2, ChronoUnit.DAYS)),
            create("USD", "0.029", "0", "0", Instant.now().minus(33, ChronoUnit.DAYS), Instant.now().minus(1, ChronoUnit.DAYS)),
            create("USD", "0.029", "0", "0", Instant.now().minus(1, ChronoUnit.DAYS), Instant.now().plus(1, ChronoUnit.DAYS)));
        try (MockedStatic<TenantHelper> tenant = mockStatic(TenantHelper.class)) {
            tenant.when(TenantHelper::getTenantId).thenReturn("tenant-a");
            invalid.forEach(bo -> assertThrows(ServiceException.class, () -> f.service.create(bo)));
        }
        verifyNoInteractions(f.batches, f.logs, f.items);
    }

    @Test
    void rejectsOverlappingActiveBatch() {
        Fixture f = fixture();
        when(f.batches.countOverlapping(eq("tenant-a"), eq("SIMULATED"), eq("USD"), any(), any(), isNull()))
            .thenReturn(1);
        try (MockedStatic<TenantHelper> tenant = mockStatic(TenantHelper.class)) {
            tenant.when(TenantHelper::getTenantId).thenReturn("tenant-a");
            ServiceException error = assertThrows(ServiceException.class, () -> f.service.create(validCreate()));
            assertEquals("payment.settlement.overlap", error.getMessage());
        }
        verify(f.batches, never()).insert(any());
        verifyNoInteractions(f.logs);
    }

    @Test
    void queriesBatchItemsAndActionsWithinCurrentTenant() {
        Fixture f = fixture();
        PaymentSettlementBatch batch = storedBatch();
        when(f.batches.selectByTenantAndId("tenant-a", 7L)).thenReturn(batch);
        Page<PaymentSettlementBatch> batchPage = new Page<>(1, 10, 1); batchPage.setRecords(List.of(batch));
        when(f.batches.selectPageByTenant(any(), eq("tenant-a"), eq("SIMULATED"), eq("USD"), eq("CREATED")))
            .thenReturn(batchPage);
        PaymentSettlementItem item = new PaymentSettlementItem(); item.setId(9L); item.setBatchId(7L);
        item.setSourceAmount(new BigDecimal("10.000000")); item.setNetContribution(new BigDecimal("9.410000"));
        Page<PaymentSettlementItem> itemPage = new Page<>(2, 10, 1); itemPage.setRecords(List.of(item));
        when(f.items.selectPageByBatch(any(), eq("tenant-a"), eq(7L), eq("PAYMENT_SUCCEEDED"))).thenReturn(itemPage);
        PaymentSettlementActionLog log = new PaymentSettlementActionLog(); log.setId(10L); log.setBatchId(7L); log.setActionType("CREATE");
        when(f.logs.selectByBatch("tenant-a", 7L)).thenReturn(List.of(log));
        PaymentSettlementQueryBo query = new PaymentSettlementQueryBo();
        query.setProviderCode("SIMULATED"); query.setCurrencyCode("USD"); query.setStatus("CREATED");
        try (MockedStatic<TenantHelper> tenant = mockStatic(TenantHelper.class)) {
            tenant.when(TenantHelper::getTenantId).thenReturn("tenant-a");
            assertEquals("7", f.service.queryDetail(7L).getId());
            assertEquals("7", f.service.queryPage(query, new PageQuery(10, 1)).getRows().get(0).getId());
            assertEquals("9", f.service.queryItems(7L, "PAYMENT_SUCCEEDED", new PageQuery(10, 2)).getRows().get(0).getId());
            assertEquals("10", f.service.queryDetail(7L).getActionLogs().get(0).getId());
        }
    }

    @Test
    void inaccessibleCrossTenantBatchLooksAbsent() {
        Fixture f = fixture();
        try (MockedStatic<TenantHelper> tenant = mockStatic(TenantHelper.class)) {
            tenant.when(TenantHelper::getTenantId).thenReturn("tenant-a");
            assertThrows(ServiceException.class, () -> f.service.queryDetail(8L));
            assertThrows(ServiceException.class,
                () -> f.service.queryItems(8L, null, new PageQuery(10, 1)));
        }
        verify(f.items, never()).selectPageByBatch(any(), anyString(), anyLong(), any());
    }

    private static PaymentSettlementCreateBo validCreate() {
        return create(" usd ", "0.029", "0.3", "15",
            Instant.now().minus(2, ChronoUnit.DAYS), Instant.now().minus(1, ChronoUnit.DAYS));
    }

    private static PaymentSettlementCreateBo create(String currency, String rate, String fixed,
                                                     String chargeback, Instant start, Instant end) {
        PaymentSettlementCreateBo bo = new PaymentSettlementCreateBo();
        bo.setProviderCode(" simulated "); bo.setCurrencyCode(currency);
        bo.setPeriodStart(Date.from(start)); bo.setPeriodEnd(Date.from(end));
        bo.setPaymentFeeRate(new BigDecimal(rate)); bo.setPaymentFixedFee(new BigDecimal(fixed));
        bo.setChargebackFixedFee(new BigDecimal(chargeback));
        return bo;
    }

    private static PaymentSettlementBatch storedBatch() {
        PaymentSettlementBatch b = new PaymentSettlementBatch();
        b.setId(7L); b.setTenantId("tenant-a"); b.setSettlementNo("PST7");
        b.setProviderCode("SIMULATED"); b.setCurrencyCode("USD"); b.setStatus("CREATED");
        b.setPaymentFeeRate(new BigDecimal("0.02900000"));
        b.setPaymentFixedFee(new BigDecimal("0.300000"));
        b.setChargebackFixedFee(new BigDecimal("15.000000")); b.setVersion(0);
        return b;
    }

    private static Fixture fixture() {
        PaymentSettlementBatchMapper batches = mock(PaymentSettlementBatchMapper.class);
        PaymentSettlementItemMapper items = mock(PaymentSettlementItemMapper.class);
        PaymentSettlementActionLogMapper logs = mock(PaymentSettlementActionLogMapper.class);
        PaymentProviderAdapter adapter = mock(PaymentProviderAdapter.class);
        when(adapter.providerCode()).thenReturn("SIMULATED");
        PaymentProviderProperties props = new PaymentProviderProperties(); props.getSimulated().setEnabled(true);
        PaymentProviderRegistry registry = new PaymentProviderRegistry(List.of(adapter), props);
        PaymentReconciliationOperatorProvider operator = mock(PaymentReconciliationOperatorProvider.class);
        when(operator.current()).thenReturn(new PaymentReconciliationOperatorProvider.Operator(88L, "operator"));
        return new Fixture(batches, items, logs,
            new PaymentSettlementServiceImpl(batches, items, logs, registry, operator));
    }

    private record Fixture(PaymentSettlementBatchMapper batches, PaymentSettlementItemMapper items,
                           PaymentSettlementActionLogMapper logs, PaymentSettlementServiceImpl service) { }
}
