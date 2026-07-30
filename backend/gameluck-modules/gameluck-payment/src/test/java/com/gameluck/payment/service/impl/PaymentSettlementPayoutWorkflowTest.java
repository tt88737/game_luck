package com.gameluck.payment.service.impl;

import com.gameluck.common.core.exception.ServiceException;
import com.gameluck.common.tenant.helper.TenantHelper;
import com.gameluck.payment.domain.PaymentSettlementPayout;
import com.gameluck.payment.domain.PaymentSettlementPayoutActionLog;
import com.gameluck.payment.domain.bo.PaymentSettlementPayoutCommandBo;
import com.gameluck.payment.domain.bo.PaymentSettlementPayoutEditBo;
import com.gameluck.payment.mapper.PaymentSettlementBatchMapper;
import com.gameluck.payment.mapper.PaymentSettlementPayoutActionLogMapper;
import com.gameluck.payment.mapper.PaymentSettlementPayoutMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("local")
class PaymentSettlementPayoutWorkflowTest {

    @Test
    void editsDraftAndWritesExactlyOneActionWithoutChangingMaker() {
        Fixture f = fixture();
        PaymentSettlementPayout before = payout("DRAFT", 3);
        when(f.payouts.editDraftOrRejected(eq("000000"), eq(71L), eq(3),
            eq("Revised purpose"), eq("merchant-us"), any(Date.class))).thenReturn(1);
        when(f.payouts.selectByTenantAndId("000000", 71L)).thenReturn(before, after(before, "DRAFT", 4));

        try (MockedStatic<TenantHelper> tenant = tenant()) {
            f.service.edit(71L, editBo(3, " Revised purpose ", " merchant-us "));
        }

        verify(f.payouts).editDraftOrRejected(eq("000000"), eq(71L), eq(3),
            eq("Revised purpose"), eq("merchant-us"), any(Date.class));
        PaymentSettlementPayoutActionLog log = oneAction(f);
        assertAction(log, "EDIT", "DRAFT", "DRAFT", 3, 4);
        assertEquals(100L, before.getMakerId());
    }

    @Test
    void rejectedEditReturnsToDraftAndPreservesLatestReviewerMetadata() {
        Fixture f = fixture();
        PaymentSettlementPayout rejected = payout("REJECTED", 5);
        rejected.setReviewerId(200L); rejected.setReviewerName("reviewer");
        rejected.setReviewedTime(new Date(1234)); rejected.setDecisionReason("needs correction");
        PaymentSettlementPayout edited = after(rejected, "DRAFT", 6);
        edited.setDecisionReason(null);
        when(f.payouts.editDraftOrRejected(eq("000000"), eq(71L), eq(5), anyString(), anyString(), any(Date.class))).thenReturn(1);
        when(f.payouts.selectByTenantAndId("000000", 71L)).thenReturn(rejected, edited);

        try (MockedStatic<TenantHelper> tenant = tenant()) {
            f.service.edit(71L, editBo(5, "fixed", "merchant-us"));
        }

        assertEquals(200L, edited.getReviewerId());
        assertEquals(new Date(1234), edited.getReviewedTime());
        assertAction(oneAction(f), "EDIT", "REJECTED", "DRAFT", 5, 6);
    }

    @Test
    void submitsDraftAndResubmitsEditedRejectedInstruction() {
        Fixture draft = fixture();
        when(draft.payouts.selectByTenantAndId("000000", 71L))
            .thenReturn(payout("DRAFT", 2), payout("PENDING_APPROVAL", 3));
        when(draft.payouts.transition(eq("000000"), eq(71L), eq(2), eq("DRAFT"),
            eq("PENDING_APPROVAL"), eq(100L), eq("maker"), eq("workflow reason"), any(Date.class))).thenReturn(1);
        try (MockedStatic<TenantHelper> tenant = tenant()) {
            draft.service.submit(71L, command(2));
        }
        assertAction(oneAction(draft), "SUBMIT", "DRAFT", "PENDING_APPROVAL", 2, 3);

        Fixture resubmit = fixture();
        PaymentSettlementPayout edited = payout("DRAFT", 6);
        edited.setReviewerId(200L); edited.setReviewedTime(new Date(1234));
        when(resubmit.payouts.selectByTenantAndId("000000", 71L))
            .thenReturn(edited, after(edited, "PENDING_APPROVAL", 7));
        when(resubmit.payouts.transition(eq("000000"), eq(71L), eq(6), eq("DRAFT"),
            eq("PENDING_APPROVAL"), eq(100L), eq("maker"), eq("workflow reason"), any(Date.class))).thenReturn(1);
        try (MockedStatic<TenantHelper> tenant = tenant()) {
            resubmit.service.submit(71L, command(6));
        }
        assertAction(oneAction(resubmit), "SUBMIT", "DRAFT", "PENDING_APPROVAL", 6, 7);
    }

    @Test
    void cancelsOnlyDraftAndWritesOneAction() {
        Fixture f = fixture();
        when(f.payouts.selectByTenantAndId("000000", 71L))
            .thenReturn(payout("DRAFT", 1), payout("CANCELLED", 2));
        when(f.payouts.transition(eq("000000"), eq(71L), eq(1), eq("DRAFT"), eq("CANCELLED"),
            eq(100L), eq("maker"), eq("no longer needed"), any(Date.class))).thenReturn(1);
        PaymentSettlementPayoutCommandBo command = command(1); command.setReason(" no longer needed ");
        try (MockedStatic<TenantHelper> tenant = tenant()) {
            f.service.cancel(71L, command);
        }
        PaymentSettlementPayoutActionLog log = oneAction(f);
        assertAction(log, "CANCEL", "DRAFT", "CANCELLED", 1, 2);
        assertEquals("no longer needed", log.getReason());
    }

    @Test
    void distinguishesMissingInvalidStateAndStaleVersionAfterZeroRowUpdate() {
        Fixture missing = fixture();
        when(missing.payouts.selectByTenantAndId("000000", 71L)).thenReturn(null);
        try (MockedStatic<TenantHelper> tenant = tenant()) {
            assertError("payment.settlementPayout.notFound", () -> missing.service.submit(71L, command(2)));
        }
        verifyNoInteractions(missing.actions);

        Fixture invalid = fixture();
        when(invalid.payouts.selectByTenantAndId("000000", 71L)).thenReturn(payout("APPROVED", 2));
        try (MockedStatic<TenantHelper> tenant = tenant()) {
            assertError("payment.settlementPayout.state.invalid", () -> invalid.service.submit(71L, command(2)));
        }
        verify(invalid.payouts, never()).transition(anyString(), anyLong(), anyInt(), anyString(), anyString(), anyLong(), anyString(), any(), any());
        verifyNoInteractions(invalid.actions);

        Fixture stale = fixture();
        when(stale.payouts.selectByTenantAndId("000000", 71L))
            .thenReturn(payout("DRAFT", 2), payout("DRAFT", 3));
        when(stale.payouts.transition(anyString(), anyLong(), anyInt(), anyString(), anyString(), anyLong(), anyString(), any(), any())).thenReturn(0);
        try (MockedStatic<TenantHelper> tenant = tenant()) {
            assertError("payment.settlementPayout.version.conflict", () -> stale.service.submit(71L, command(2)));
        }
        verify(stale.payouts, times(2)).selectByTenantAndId("000000", 71L);
        verify(stale.payouts, times(1)).transition(anyString(), anyLong(), anyInt(), anyString(), anyString(), anyLong(), anyString(), any(), any());
        verifyNoInteractions(stale.actions);
    }

    @Test
    void zeroRowReloadCanDetectRemovalOrConcurrentStateChangeWithoutRetry() {
        Fixture removed = fixture();
        when(removed.payouts.selectByTenantAndId("000000", 71L))
            .thenReturn(payout("DRAFT", 2), (PaymentSettlementPayout) null);
        when(removed.payouts.transition(anyString(), anyLong(), anyInt(), anyString(), anyString(), anyLong(), anyString(), any(), any())).thenReturn(0);
        try (MockedStatic<TenantHelper> tenant = tenant()) {
            assertError("payment.settlementPayout.notFound", () -> removed.service.submit(71L, command(2)));
        }
        verify(removed.payouts, times(1)).transition(anyString(), anyLong(), anyInt(), anyString(), anyString(), anyLong(), anyString(), any(), any());

        Fixture changed = fixture();
        when(changed.payouts.selectByTenantAndId("000000", 71L))
            .thenReturn(payout("DRAFT", 2), payout("CANCELLED", 3));
        when(changed.payouts.transition(anyString(), anyLong(), anyInt(), anyString(), anyString(), anyLong(), anyString(), any(), any())).thenReturn(0);
        try (MockedStatic<TenantHelper> tenant = tenant()) {
            assertError("payment.settlementPayout.state.invalid", () -> changed.service.submit(71L, command(2)));
        }
        verify(changed.payouts, times(1)).transition(anyString(), anyLong(), anyInt(), anyString(), anyString(), anyLong(), anyString(), any(), any());
        verifyNoInteractions(removed.actions, changed.actions);
    }

    @Test
    void editFailureHasNoActionAndUsesSameFailureClassification() {
        Fixture f = fixture();
        when(f.payouts.editDraftOrRejected(anyString(), anyLong(), anyInt(), anyString(), anyString(), any(Date.class))).thenReturn(0);
        when(f.payouts.selectByTenantAndId("000000", 71L))
            .thenReturn(payout("DRAFT", 3), payout("DRAFT", 4));
        try (MockedStatic<TenantHelper> tenant = tenant()) {
            assertError("payment.settlementPayout.version.conflict", () -> f.service.edit(71L, editBo(3, "fixed", "ref")));
        }
        verify(f.payouts, times(1)).editDraftOrRejected(anyString(), anyLong(), anyInt(), anyString(), anyString(), any(Date.class));
        verifyNoInteractions(f.actions);
    }

    private static Fixture fixture() {
        PaymentSettlementBatchMapper batches = mock(PaymentSettlementBatchMapper.class);
        PaymentSettlementPayoutMapper payouts = mock(PaymentSettlementPayoutMapper.class);
        PaymentSettlementPayoutActionLogMapper actions = mock(PaymentSettlementPayoutActionLogMapper.class);
        PaymentReconciliationOperatorProvider operators = mock(PaymentReconciliationOperatorProvider.class);
        when(operators.current()).thenReturn(new PaymentReconciliationOperatorProvider.Operator(100L, " maker "));
        return new Fixture(payouts, actions, new PaymentSettlementPayoutServiceImpl(batches, payouts, actions, operators));
    }

    private static PaymentSettlementPayoutEditBo editBo(int version, String purpose, String reference) {
        PaymentSettlementPayoutEditBo bo = new PaymentSettlementPayoutEditBo();
        bo.setVersion(version); bo.setPayoutPurpose(purpose); bo.setPayeeReference(reference); return bo;
    }

    private static PaymentSettlementPayoutCommandBo command(int version) {
        PaymentSettlementPayoutCommandBo bo = new PaymentSettlementPayoutCommandBo();
        bo.setVersion(version); bo.setReason("workflow reason"); return bo;
    }

    private static PaymentSettlementPayout payout(String status, int version) {
        PaymentSettlementPayout p = new PaymentSettlementPayout();
        p.setId(71L); p.setTenantId("000000"); p.setStatus(status); p.setVersion(version);
        p.setMakerId(100L); p.setMakerName("maker"); p.setSettlementEvidenceJson("{\"count\":1}");
        p.setPayoutPurpose("purpose"); p.setPayeeReference("reference"); return p;
    }

    private static PaymentSettlementPayout after(PaymentSettlementPayout before, String status, int version) {
        PaymentSettlementPayout p = payout(status, version);
        p.setReviewerId(before.getReviewerId()); p.setReviewerName(before.getReviewerName());
        p.setReviewedTime(before.getReviewedTime()); p.setDecisionReason(before.getDecisionReason()); return p;
    }

    private static PaymentSettlementPayoutActionLog oneAction(Fixture f) {
        ArgumentCaptor<PaymentSettlementPayoutActionLog> captor = ArgumentCaptor.forClass(PaymentSettlementPayoutActionLog.class);
        verify(f.actions, times(1)).insert(captor.capture()); return captor.getValue();
    }

    private static void assertAction(PaymentSettlementPayoutActionLog log, String action, String before,
                                     String after, int expectedVersion, int resultVersion) {
        assertEquals(action, log.getActionType()); assertEquals(before, log.getBeforeStatus());
        assertEquals(after, log.getAfterStatus()); assertEquals(expectedVersion, log.getExpectedVersion());
        assertEquals(resultVersion, log.getResultVersion()); assertEquals(100L, log.getOperatorId());
        assertEquals("maker", log.getOperatorName()); assertEquals("{\"count\":1}", log.getEvidenceSnapshotJson());
    }

    private static MockedStatic<TenantHelper> tenant() {
        MockedStatic<TenantHelper> tenant = mockStatic(TenantHelper.class);
        tenant.when(TenantHelper::getTenantId).thenReturn("000000"); return tenant;
    }

    private static void assertError(String key, Runnable command) {
        ServiceException error = assertThrows(ServiceException.class, command::run); assertEquals(key, error.getMessage());
    }

    private record Fixture(PaymentSettlementPayoutMapper payouts, PaymentSettlementPayoutActionLogMapper actions,
                           PaymentSettlementPayoutServiceImpl service) { }
}
