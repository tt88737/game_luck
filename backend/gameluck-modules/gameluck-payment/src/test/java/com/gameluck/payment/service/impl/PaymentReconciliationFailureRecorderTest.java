package com.gameluck.payment.service.impl;

import com.gameluck.payment.mapper.PaymentReconciliationActionLogMapper;
import com.gameluck.payment.mapper.PaymentReconciliationBatchMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("local")
class PaymentReconciliationFailureRecorderTest {

    @Test
    void executionFailureTransitionsAndAppendsExactlyOneLog() {
        PaymentReconciliationBatchMapper batches = mock(PaymentReconciliationBatchMapper.class);
        PaymentReconciliationActionLogMapper logs = mock(PaymentReconciliationActionLogMapper.class);
        when(batches.markExecutionFailed(eq("tenant-a"), eq(7L), eq("Reconciliation execution failed"), any()))
            .thenReturn(1);

        new PaymentReconciliationFailureRecorder(batches, logs)
            .recordFailure("tenant-a", 7L, "Reconciliation execution failed");

        verify(logs, times(1)).insert(argThat(log -> "EXECUTION_FAILED".equals(log.getActionType())
            && "RECONCILING".equals(log.getBeforeStatus()) && "FAILED".equals(log.getAfterStatus())
            && "tenant-a".equals(log.getTenantId()) && Long.valueOf(7L).equals(log.getBatchId())
            && Long.valueOf(0L).equals(log.getOperatorId()) && "SYSTEM".equals(log.getOperatorName())));
    }

    @Test
    void concurrencyLoserCreatesNoFailureLog() {
        PaymentReconciliationBatchMapper batches = mock(PaymentReconciliationBatchMapper.class);
        PaymentReconciliationActionLogMapper logs = mock(PaymentReconciliationActionLogMapper.class);
        new PaymentReconciliationFailureRecorder(batches, logs)
            .recordFailure("tenant-a", 7L, "Reconciliation execution failed");
        verifyNoInteractions(logs);
    }
}
