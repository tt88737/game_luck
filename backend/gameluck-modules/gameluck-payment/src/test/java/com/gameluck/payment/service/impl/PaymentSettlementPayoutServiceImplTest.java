package com.gameluck.payment.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gameluck.common.core.exception.ServiceException;
import com.gameluck.common.mybatis.core.page.PageQuery;
import com.gameluck.common.tenant.helper.TenantHelper;
import com.gameluck.payment.domain.PaymentSettlementBatch;
import com.gameluck.payment.domain.PaymentSettlementPayout;
import com.gameluck.payment.domain.PaymentSettlementPayoutActionLog;
import com.gameluck.payment.domain.bo.PaymentSettlementPayoutCreateBo;
import com.gameluck.payment.domain.bo.PaymentSettlementPayoutQueryBo;
import com.gameluck.payment.domain.vo.PaymentSettlementPayoutDetailVo;
import com.gameluck.payment.mapper.PaymentSettlementBatchMapper;
import com.gameluck.payment.mapper.PaymentSettlementPayoutActionLogMapper;
import com.gameluck.payment.mapper.PaymentSettlementPayoutMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@Tag("local")
class PaymentSettlementPayoutServiceImplTest {

    @Test
    void createsDraftFromPositiveClosedBatchUsingOnlyServerOwnedSettlementFacts() {
        Fixture f = fixture();
        PaymentSettlementBatch batch = closed("12.340000");
        batch.setEvidenceSnapshotJson("{\"eventCount\":3}");
        when(f.batches.selectByTenantAndId("000000", 41L)).thenReturn(batch);
        when(f.payouts.selectByTenantAndBatchId("000000", 41L)).thenReturn(null);

        try (MockedStatic<TenantHelper> tenant = tenant("000000")) {
            PaymentSettlementPayoutDetailVo result = f.service.create(createBo("41"));

            assertEquals("12.340000", result.getPayoutAmount());
            assertEquals("DRAFT", result.getStatus());
            assertTrue(result.getPayoutNo().startsWith("PSP"));
            assertEquals("SIMULATED", result.getProviderCode());
            assertEquals("USD", result.getCurrencyCode());
            assertEquals("SET-41", result.getSettlementNo());
            assertEquals("{\"eventCount\":3}", result.getSettlementEvidenceJson());
        }

        ArgumentCaptor<PaymentSettlementPayout> payout = ArgumentCaptor.forClass(PaymentSettlementPayout.class);
        verify(f.payouts).insert(payout.capture());
        assertEquals(41L, payout.getValue().getSettlementBatchId());
        assertEquals(new BigDecimal("12.340000"), payout.getValue().getPayoutAmount());
        assertEquals("SIMULATED", payout.getValue().getProviderCode());
        assertEquals("USD", payout.getValue().getCurrencyCode());
        assertEquals(" treasury payout ".trim(), payout.getValue().getPayoutPurpose());
        assertEquals("merchant-alias", payout.getValue().getPayeeReference());
        assertEquals(100L, payout.getValue().getMakerId());

        ArgumentCaptor<PaymentSettlementPayoutActionLog> action = ArgumentCaptor.forClass(PaymentSettlementPayoutActionLog.class);
        verify(f.actions).insert(action.capture());
        assertEquals("CREATE", action.getValue().getActionType());
        assertEquals("DRAFT", action.getValue().getAfterStatus());
        assertEquals(0, action.getValue().getResultVersion());
        assertEquals(payout.getValue().getId(), action.getValue().getPayoutId());
    }

    @Test
    void rejectsZeroNegativeAndNonClosedBatchesWithoutWriting() {
        Fixture f = fixture();
        when(f.batches.selectByTenantAndId(eq("000000"), eq(41L)))
            .thenReturn(closed("0.000000"), closed("-0.000001"), batch("CALCULATED", "1.000000"));

        try (MockedStatic<TenantHelper> tenant = tenant("000000")) {
            assertError("payment.settlementPayout.amount.ineligible", () -> f.service.create(createBo("41")));
            assertError("payment.settlementPayout.amount.ineligible", () -> f.service.create(createBo("41")));
            assertError("payment.settlementPayout.status.ineligible", () -> f.service.create(createBo("41")));
        }
        verifyNoInteractions(f.actions);
        verify(f.payouts, never()).insert(any());
    }

    @Test
    void treatsMissingAndCrossTenantBatchesAsAbsent() {
        Fixture f = fixture();
        when(f.batches.selectByTenantAndId("000000", 41L)).thenReturn(null);
        try (MockedStatic<TenantHelper> tenant = tenant("000000")) {
            assertError("payment.settlementPayout.batch.notFound", () -> f.service.create(createBo("41")));
        }
        verify(f.batches).selectByTenantAndId("000000", 41L);
        verify(f.batches, never()).selectByTenantAndId("other", 41L);
        verifyNoInteractions(f.payouts, f.actions);
    }

    @Test
    void rejectsExistingInstructionAndTranslatesDuplicateKeyRace() {
        Fixture existing = fixture();
        when(existing.batches.selectByTenantAndId("000000", 41L)).thenReturn(closed("1.000000"));
        when(existing.payouts.selectByTenantAndBatchId("000000", 41L)).thenReturn(payout(7L));
        try (MockedStatic<TenantHelper> tenant = tenant("000000")) {
            assertError("payment.settlementPayout.duplicate", () -> existing.service.create(createBo("41")));
        }
        verify(existing.payouts, never()).insert(any());

        Fixture racing = fixture();
        when(racing.batches.selectByTenantAndId("000000", 41L)).thenReturn(closed("1.000000"));
        when(racing.payouts.selectByTenantAndBatchId("000000", 41L)).thenReturn(null);
        doThrow(new DuplicateKeyException("uk_tenant_batch")).when(racing.payouts).insert(any());
        try (MockedStatic<TenantHelper> tenant = tenant("000000")) {
            assertError("payment.settlementPayout.duplicate", () -> racing.service.create(createBo("41")));
        }
        verifyNoInteractions(racing.actions);
    }

    @Test
    void validatesAndTrimsOperatorFields() {
        Fixture f = fixture();
        PaymentSettlementPayoutCreateBo invalid = createBo("41");
        invalid.setPayoutPurpose(" \n ");
        when(f.batches.selectByTenantAndId("000000", 41L)).thenReturn(closed("1.000000"));
        try (MockedStatic<TenantHelper> tenant = tenant("000000")) {
            assertError("payment.settlementPayout.input.invalid", () -> f.service.create(invalid));
        }
        verifyNoInteractions(f.payouts, f.actions);
    }

    @Test
    void queriesTenantScopedFiltersAndSerializesIdsAndMoneyAsStrings() {
        Fixture f = fixture();
        PaymentSettlementPayout stored = payout(9007199254740993L);
        stored.setPayoutAmount(new BigDecimal("8.1"));
        Page<PaymentSettlementPayout> page = new Page<>(2, 10, 1);
        page.setRecords(List.of(stored));
        when(f.payouts.selectPageByTenant(any(), eq("000000"), eq("PSP1"), eq("SET1"), eq("DRAFT"),
            eq("SIMULATED"), eq("USD"), any(Date.class), any(Date.class))).thenReturn(page);
        PaymentSettlementPayoutQueryBo query = new PaymentSettlementPayoutQueryBo();
        query.setPayoutNo(" PSP1 "); query.setSettlementNo(" SET1 "); query.setStatus(" draft ");
        query.setProviderCode(" simulated "); query.setCurrencyCode(" usd ");
        query.setCreateStart(new Date(1)); query.setCreateEnd(new Date(2));

        try (MockedStatic<TenantHelper> tenant = tenant("000000")) {
            var result = f.service.queryPage(query, new PageQuery(10, 2));
            assertEquals("9007199254740993", result.getRows().get(0).getId());
            assertEquals("8.100000", result.getRows().get(0).getPayoutAmount());
        }
    }

    @Test
    void returnsTenantScopedDetailWithMapperOrderedActions() {
        Fixture f = fixture();
        PaymentSettlementPayout stored = payout(7L);
        PaymentSettlementPayoutActionLog first = action(11L, "CREATE", new Date(10));
        PaymentSettlementPayoutActionLog second = action(12L, "EDIT", new Date(20));
        when(f.payouts.selectByTenantAndId("000000", 7L)).thenReturn(stored);
        when(f.actions.selectByPayout("000000", 7L)).thenReturn(List.of(first, second));

        try (MockedStatic<TenantHelper> tenant = tenant("000000")) {
            PaymentSettlementPayoutDetailVo detail = f.service.queryDetail(7L);
            assertEquals(List.of("CREATE", "EDIT"), detail.getActionLogs().stream()
                .map(a -> a.getActionType()).toList());
            assertEquals("11", detail.getActionLogs().get(0).getId());
            assertEquals("7", detail.getActionLogs().get(0).getPayoutId());
        }
        verify(f.payouts).selectByTenantAndId("000000", 7L);
        verify(f.actions).selectByPayout("000000", 7L);
    }

    @Test
    void rejectsUnknownOrCrossTenantDetailAsAbsent() {
        Fixture f = fixture();
        when(f.payouts.selectByTenantAndId("000000", 7L)).thenReturn(null);
        try (MockedStatic<TenantHelper> tenant = tenant("000000")) {
            assertError("payment.settlementPayout.notFound", () -> f.service.queryDetail(7L));
        }
        verifyNoInteractions(f.actions);
    }

    private static Fixture fixture() {
        PaymentSettlementBatchMapper batches = mock(PaymentSettlementBatchMapper.class);
        PaymentSettlementPayoutMapper payouts = mock(PaymentSettlementPayoutMapper.class);
        PaymentSettlementPayoutActionLogMapper actions = mock(PaymentSettlementPayoutActionLogMapper.class);
        PaymentReconciliationOperatorProvider operators = mock(PaymentReconciliationOperatorProvider.class);
        when(operators.current()).thenReturn(new PaymentReconciliationOperatorProvider.Operator(100L, " maker "));
        return new Fixture(batches, payouts, actions, new PaymentSettlementPayoutServiceImpl(batches, payouts, actions, operators));
    }

    private static MockedStatic<TenantHelper> tenant(String tenantId) {
        MockedStatic<TenantHelper> tenant = mockStatic(TenantHelper.class);
        tenant.when(TenantHelper::getTenantId).thenReturn(tenantId);
        return tenant;
    }

    private static PaymentSettlementPayoutCreateBo createBo(String batchId) {
        PaymentSettlementPayoutCreateBo bo = new PaymentSettlementPayoutCreateBo();
        bo.setSettlementBatchId(batchId); bo.setPayoutPurpose(" treasury payout ");
        bo.setPayeeReference(" merchant-alias "); return bo;
    }

    private static PaymentSettlementBatch closed(String amount) { return batch("CLOSED", amount); }

    private static PaymentSettlementBatch batch(String status, String amount) {
        PaymentSettlementBatch batch = new PaymentSettlementBatch();
        batch.setId(41L); batch.setTenantId("000000"); batch.setSettlementNo("SET-41");
        batch.setProviderCode("SIMULATED"); batch.setCurrencyCode("USD"); batch.setStatus(status);
        batch.setNetSettlement(new BigDecimal(amount)); batch.setEvidenceSnapshotJson("{}");
        return batch;
    }

    private static PaymentSettlementPayout payout(long id) {
        PaymentSettlementPayout payout = new PaymentSettlementPayout();
        payout.setId(id); payout.setTenantId("000000"); payout.setPayoutNo("PSP" + id);
        payout.setSettlementBatchId(41L); payout.setSettlementNo("SET-41");
        payout.setProviderCode("SIMULATED"); payout.setCurrencyCode("USD");
        payout.setPayoutAmount(new BigDecimal("12.340000")); payout.setSettlementEvidenceJson("{}");
        payout.setPayoutPurpose("purpose"); payout.setPayeeReference("reference"); payout.setStatus("DRAFT");
        payout.setMakerId(100L); payout.setMakerName("maker"); payout.setVersion(0); payout.setCreateTime(new Date());
        return payout;
    }

    private static PaymentSettlementPayoutActionLog action(long id, String type, Date created) {
        PaymentSettlementPayoutActionLog action = new PaymentSettlementPayoutActionLog();
        action.setId(id); action.setTenantId("000000"); action.setPayoutId(7L); action.setActionType(type);
        action.setAfterStatus("DRAFT"); action.setOperatorId(100L); action.setOperatorName("maker");
        action.setResultVersion(0); action.setCreateTime(created); return action;
    }

    private static void assertError(String key, Runnable command) {
        ServiceException error = assertThrows(ServiceException.class, command::run);
        assertEquals(key, error.getMessage());
    }

    private record Fixture(PaymentSettlementBatchMapper batches, PaymentSettlementPayoutMapper payouts,
                           PaymentSettlementPayoutActionLogMapper actions, PaymentSettlementPayoutServiceImpl service) { }
}
