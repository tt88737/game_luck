package com.gameluck.payment.service.impl;

import com.gameluck.common.core.exception.ServiceException;
import com.gameluck.payment.domain.PaymentSettlementPayout;
import com.gameluck.payment.domain.PaymentSettlementPayoutActionLog;
import com.gameluck.payment.domain.bo.PaymentSettlementPayoutCommandBo;
import com.gameluck.payment.mapper.PaymentSettlementPayoutActionLogMapper;
import com.gameluck.payment.mapper.PaymentSettlementPayoutMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("local")
class PaymentSettlementPayoutApprovalServiceTest {

    @Test
    void deniesMakerSelfApprovalWithoutWriting() {
        Fixture f = fixture(100L);
        PaymentSettlementPayout payout = pending(100L, 2);
        assertError("payment.settlementPayout.selfApproval", () -> f.service.approve("000000", payout, command(2, "approved")));
        verifyNoInteractions(f.payouts, f.actions);
    }

    @Test
    void secondUserApprovesAndPersistsReviewerAndSanitizedEvidence() {
        Fixture f = fixture(200L);
        PaymentSettlementPayout before = pending(100L, 2);
        before.setSettlementEvidenceJson("{\"count\":3,\"bank_account\":\"x\",\"nested\":{\"token\":\"y\"}}");
        PaymentSettlementPayout after = pending(100L, 3);
        after.setStatus("APPROVED"); after.setReviewerId(200L); after.setReviewerName("reviewer");
        when(f.payouts.transition(eq("000000"), eq(71L), eq(2), eq("PENDING_APPROVAL"), eq("APPROVED"),
            eq(200L), eq("reviewer"), eq("approved"), any(Date.class))).thenReturn(1);
        when(f.payouts.selectByTenantAndId("000000", 71L)).thenReturn(after);

        f.service.approve("000000", before, command(2, " approved "));

        ArgumentCaptor<PaymentSettlementPayoutActionLog> action = ArgumentCaptor.forClass(PaymentSettlementPayoutActionLog.class);
        verify(f.actions).insert(action.capture());
        assertEquals("APPROVE", action.getValue().getActionType());
        assertEquals("PENDING_APPROVAL", action.getValue().getBeforeStatus());
        assertEquals("APPROVED", action.getValue().getAfterStatus());
        assertEquals("{\"count\":3,\"nested\":{}}", action.getValue().getEvidenceSnapshotJson());
        assertFalse(action.getValue().getEvidenceSnapshotJson().toLowerCase().matches(".*(bank|account|credential|secret|token|signature).*"));
    }

    @Test
    void secondUserRejectsWithRequiredReason() {
        Fixture f = fixture(200L);
        PaymentSettlementPayout before = pending(100L, 2);
        PaymentSettlementPayout after = pending(100L, 3); after.setStatus("REJECTED");
        when(f.payouts.transition(eq("000000"), eq(71L), eq(2), eq("PENDING_APPROVAL"), eq("REJECTED"),
            eq(200L), eq("reviewer"), eq("needs correction"), any(Date.class))).thenReturn(1);
        when(f.payouts.selectByTenantAndId("000000", 71L)).thenReturn(after);
        f.service.reject("000000", before, command(2, " needs correction "));
        ArgumentCaptor<PaymentSettlementPayoutActionLog> action = ArgumentCaptor.forClass(PaymentSettlementPayoutActionLog.class);
        verify(f.actions).insert(action.capture());
        assertEquals("REJECT", action.getValue().getActionType());
        assertEquals("needs correction", action.getValue().getReason());
    }

    @Test
    void rejectsTerminalOrRejectedReplayAndMissingReason() {
        Fixture f = fixture(200L);
        for (String status : new String[]{"APPROVED", "CANCELLED", "REJECTED"}) {
            PaymentSettlementPayout payout = pending(100L, 2); payout.setStatus(status);
            assertError("payment.settlementPayout.state.invalid", () -> f.service.approve("000000", payout, command(2, "ok")));
        }
        assertError("payment.settlementPayout.input.invalid", () -> f.service.reject("000000", pending(100L, 2), command(2, " ")));
        verifyNoInteractions(f.payouts, f.actions);
    }

    @Test
    void classifiesZeroRowWithoutRetryAndWritesNoAction() {
        Fixture f = fixture(200L);
        PaymentSettlementPayout before = pending(100L, 2);
        when(f.payouts.transition(anyString(), anyLong(), anyInt(), anyString(), anyString(), anyLong(), anyString(), anyString(), any(Date.class))).thenReturn(0);
        when(f.payouts.selectByTenantAndId("000000", 71L)).thenReturn(pending(100L, 3));
        assertError("payment.settlementPayout.version.conflict", () -> f.service.approve("000000", before, command(2, "ok")));
        verify(f.payouts, times(1)).transition(anyString(), anyLong(), anyInt(), anyString(), anyString(), anyLong(), anyString(), anyString(), any(Date.class));
        verifyNoInteractions(f.actions);
    }

    private static Fixture fixture(long operatorId) {
        PaymentSettlementPayoutMapper payouts = mock(PaymentSettlementPayoutMapper.class);
        PaymentSettlementPayoutActionLogMapper actions = mock(PaymentSettlementPayoutActionLogMapper.class);
        PaymentReconciliationOperatorProvider operators = mock(PaymentReconciliationOperatorProvider.class);
        when(operators.current()).thenReturn(new PaymentReconciliationOperatorProvider.Operator(operatorId, " reviewer "));
        return new Fixture(payouts, actions, new PaymentSettlementPayoutApprovalService(payouts, actions, operators));
    }

    private static PaymentSettlementPayout pending(long makerId, int version) {
        PaymentSettlementPayout p = new PaymentSettlementPayout(); p.setId(71L); p.setTenantId("000000");
        p.setStatus("PENDING_APPROVAL"); p.setVersion(version); p.setMakerId(makerId); p.setSettlementEvidenceJson("{}"); return p;
    }

    private static PaymentSettlementPayoutCommandBo command(int version, String reason) {
        PaymentSettlementPayoutCommandBo bo = new PaymentSettlementPayoutCommandBo(); bo.setVersion(version); bo.setReason(reason); return bo;
    }

    private static void assertError(String key, Runnable command) {
        ServiceException error = assertThrows(ServiceException.class, command::run); assertEquals(key, error.getMessage());
    }

    private record Fixture(PaymentSettlementPayoutMapper payouts, PaymentSettlementPayoutActionLogMapper actions,
                           PaymentSettlementPayoutApprovalService service) { }
}
