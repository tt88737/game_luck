package com.gameluck.payment.service.impl;

import com.gameluck.common.core.exception.ServiceException;
import com.gameluck.payment.domain.PaymentReconciliationIssue;
import com.gameluck.payment.domain.bo.PaymentReconciliationResolutionBo;
import com.gameluck.payment.mapper.PaymentReconciliationActionLogMapper;
import com.gameluck.payment.mapper.PaymentReconciliationIssueMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("local")
class PaymentReconciliationResolutionServiceTest {
    private PaymentReconciliationIssueMapper issueMapper;
    private PaymentReconciliationActionLogMapper logMapper;
    private PaymentReconciliationResolutionService service;

    @BeforeEach
    void setUp() {
        issueMapper = mock(PaymentReconciliationIssueMapper.class);
        logMapper = mock(PaymentReconciliationActionLogMapper.class);
        service = new PaymentReconciliationResolutionService(issueMapper, logMapper,
            () -> "tenant-a", () -> new PaymentReconciliationOperatorProvider.Operator(7L, "operator"));
    }

    @Test
    void rejectsBlankRemarkAndUnknownResolutionType() {
        assertThatThrownBy(() -> service.resolve(11L, bo("OTHER", "  "))).isInstanceOf(ServiceException.class);
        assertThatThrownBy(() -> service.resolve(11L, bo("FIX_DATA", "reviewed"))).isInstanceOf(ServiceException.class);
        assertThatThrownBy(() -> service.ignore(11L, bo(null, "reviewed"))).isInstanceOf(ServiceException.class);
        verifyNoInteractions(issueMapper, logMapper);
    }

    @Test
    void ignorePersistsClassificationRemarkAndExpectedVersion() {
        PaymentReconciliationIssue open = issue("OPEN");
        when(issueMapper.selectByTenantAndId("tenant-a", 11L)).thenReturn(open);
        when(issueMapper.resolveOpenIssue(eq("tenant-a"), eq(11L), eq(3), eq("IGNORED"),
            eq("EXPECTED_DIFFERENCE"), eq("expected provider delay"), eq(7L), any())).thenReturn(1);

        PaymentReconciliationIssue ignored = service.ignore(11L, bo("EXPECTED_DIFFERENCE", "expected provider delay"));

        assertThat(ignored.getStatus()).isEqualTo("IGNORED");
        assertThat(ignored.getResolutionType()).isEqualTo("EXPECTED_DIFFERENCE");
        verify(logMapper).insert(argThat(log -> "IGNORE".equals(log.getActionType())
            && "expected provider delay".equals(log.getRemark())));
    }

    @Test
    void rejectsCrossTenantOrTerminalIssueWithoutActionLog() {
        when(issueMapper.selectByTenantAndId("tenant-a", 11L)).thenReturn(null);
        assertThatThrownBy(() -> service.resolve(11L, bo("OTHER", "reviewed"))).isInstanceOf(ServiceException.class);
        PaymentReconciliationIssue terminal = issue("RESOLVED");
        when(issueMapper.selectByTenantAndId("tenant-a", 11L)).thenReturn(terminal);
        assertThatThrownBy(() -> service.ignore(11L, bo(null, "reviewed"))).isInstanceOf(ServiceException.class);
        verify(logMapper, never()).insert(any());
    }

    @Test
    void concurrentResolutionAllowsOneWinnerAndOneBusinessActionLog() {
        PaymentReconciliationIssue open = issue("OPEN");
        when(issueMapper.selectByTenantAndId("tenant-a", 11L)).thenReturn(open);
        when(issueMapper.resolveOpenIssue(eq("tenant-a"), eq(11L), eq(3), anyString(), any(), anyString(), eq(7L), any()))
            .thenReturn(1, 0);
        service.resolve(11L, bo("PROVIDER_CONFIRMED", "  checked  "));
        assertThatThrownBy(() -> service.resolve(11L, bo("PROVIDER_CONFIRMED", "again")))
            .isInstanceOf(ServiceException.class);
        ArgumentCaptor<com.gameluck.payment.domain.PaymentReconciliationActionLog> captor =
            ArgumentCaptor.forClass(com.gameluck.payment.domain.PaymentReconciliationActionLog.class);
        verify(logMapper, times(1)).insert(captor.capture());
        assertThat(captor.getValue().getRemark()).isEqualTo("checked");
        assertThat(captor.getValue().getActionType()).isEqualTo("RESOLVE");
    }

    @Test
    void staleExpectedVersionFailsWithoutBusinessActionLog() {
        PaymentReconciliationIssue current = issue("OPEN");
        current.setVersion(2);
        when(issueMapper.selectByTenantAndId("tenant-a", 11L)).thenReturn(current);
        when(issueMapper.resolveOpenIssue(eq("tenant-a"), eq(11L), eq(1), anyString(), any(), anyString(), eq(7L), any()))
            .thenReturn(0);

        assertThatThrownBy(() -> service.resolve(11L, bo("OTHER", "reviewed", 1)))
            .isInstanceOfSatisfying(ServiceException.class,
                error -> assertThat(error.getCode()).isEqualTo(PaymentReconciliationResolutionService.STATE_CONFLICT_CODE));
        verify(issueMapper).resolveOpenIssue(eq("tenant-a"), eq(11L), eq(1), anyString(), eq("OTHER"),
            eq("reviewed"), eq(7L), any());
        verify(logMapper, never()).insert(any());
    }

    private static PaymentReconciliationIssue issue(String status) {
        PaymentReconciliationIssue issue = new PaymentReconciliationIssue();
        issue.setId(11L); issue.setBatchId(22L); issue.setStatus(status); issue.setVersion(3);
        return issue;
    }

    private static PaymentReconciliationResolutionBo bo(String type, String remark) {
        return bo(type, remark, 3);
    }

    private static PaymentReconciliationResolutionBo bo(String type, String remark, Integer expectedVersion) {
        PaymentReconciliationResolutionBo bo = new PaymentReconciliationResolutionBo();
        bo.setResolutionType(type); bo.setRemark(remark); bo.setExpectedVersion(expectedVersion); return bo;
    }
}
