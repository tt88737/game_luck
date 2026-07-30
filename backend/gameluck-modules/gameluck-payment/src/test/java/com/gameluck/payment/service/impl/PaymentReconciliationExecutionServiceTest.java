package com.gameluck.payment.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gameluck.common.core.exception.ServiceException;
import com.gameluck.payment.domain.PaymentReconciliationBatch;
import com.gameluck.payment.mapper.PaymentReconciliationActionLogMapper;
import com.gameluck.payment.mapper.PaymentReconciliationBatchMapper;
import com.gameluck.payment.mapper.PaymentReconciliationIssueMapper;
import com.gameluck.payment.mapper.PaymentReconciliationLineMapper;
import com.gameluck.payment.service.reconciliation.PaymentReconciliationMatcher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("local")
class PaymentReconciliationExecutionServiceTest {

    @Test
    void acquisitionRejectsInvalidValidatedBatchWithoutTransition() {
        PaymentReconciliationBatchMapper batches = mock(PaymentReconciliationBatchMapper.class);
        PaymentReconciliationBatch batch = batch("VALIDATED", 1);
        when(batches.selectByTenantAndId("tenant-a", 7L)).thenReturn(batch);
        PaymentReconciliationExecutionService service = service(batches, mock(PaymentReconciliationFailureRecorder.class));

        ServiceException error = assertThrows(ServiceException.class,
            () -> service.acquire("tenant-a", 7L));

        assertEquals(com.gameluck.common.core.utils.MessageUtils.message("payment.reconciliation.execute.invalidLines"), error.getMessage());
        verify(batches, never()).acquireExecution(anyString(), anyLong(), anyInt(), any());
    }

    @Test
    void onlyOneGuardedRequestAcquiresExecution() {
        PaymentReconciliationBatchMapper batches = mock(PaymentReconciliationBatchMapper.class);
        PaymentReconciliationBatch batch = batch("VALIDATED", 0);
        when(batches.selectByTenantAndId("tenant-a", 7L)).thenReturn(batch);
        when(batches.acquireExecution(eq("tenant-a"), eq(7L), eq(3), any())).thenReturn(1, 0);
        PaymentReconciliationExecutionService service = service(batches, mock(PaymentReconciliationFailureRecorder.class));

        service.acquire("tenant-a", 7L);
        ServiceException loser = assertThrows(ServiceException.class, () -> service.acquire("tenant-a", 7L));

        assertEquals(com.gameluck.common.core.utils.MessageUtils.message("payment.reconciliation.execute.stateConflict"), loser.getMessage());
    }

    @Test
    void acquisitionCapturesOperatorForLaterAtomicAndFailureLogs() {
        PaymentReconciliationBatchMapper batches = mock(PaymentReconciliationBatchMapper.class);
        when(batches.selectByTenantAndId("tenant-a", 7L)).thenReturn(batch("VALIDATED", 0));
        when(batches.acquireExecution(eq("tenant-a"), eq(7L), eq(3), any())).thenReturn(1);
        PaymentReconciliationOperatorProvider operators = mock(PaymentReconciliationOperatorProvider.class);
        when(operators.current()).thenReturn(new PaymentReconciliationOperatorProvider.Operator(91L, "Alice"));
        PaymentReconciliationExecutionService service = new PaymentReconciliationExecutionService(batches,
            mock(PaymentReconciliationLineMapper.class), mock(PaymentReconciliationIssueMapper.class),
            mock(PaymentReconciliationActionLogMapper.class), new PaymentReconciliationMatcher(new ObjectMapper()),
            mock(PaymentReconciliationExecutionService.PlatformDataSource.class), operators,
            mock(PaymentReconciliationFailureRecorder.class));
        var lease = service.acquire("tenant-a", 7L);
        assertEquals(91L, lease.operatorId());
        assertEquals("Alice", lease.operatorName());
    }

    @Test
    void nonValidatedAndTerminalBatchesRejectWithoutCreatingDuplicateWork() {
        for (String status : new String[]{"UPLOADED", "RECONCILING", "COMPLETED", "FAILED"}) {
            PaymentReconciliationBatchMapper batches = mock(PaymentReconciliationBatchMapper.class);
            when(batches.selectByTenantAndId("tenant-a", 7L)).thenReturn(batch(status, 0));
            PaymentReconciliationLineMapper lines = mock(PaymentReconciliationLineMapper.class);
            PaymentReconciliationIssueMapper issues = mock(PaymentReconciliationIssueMapper.class);
            PaymentReconciliationExecutionService service = new PaymentReconciliationExecutionService(batches, lines,
                issues, mock(PaymentReconciliationActionLogMapper.class), new PaymentReconciliationMatcher(new ObjectMapper()),
                mock(PaymentReconciliationExecutionService.PlatformDataSource.class),
                mock(PaymentReconciliationOperatorProvider.class), mock(PaymentReconciliationFailureRecorder.class));

            assertEquals(com.gameluck.common.core.utils.MessageUtils.message("payment.reconciliation.execute.stateConflict"),
                assertThrows(ServiceException.class, () -> service.acquire("tenant-a", 7L)).getMessage());
            verifyNoInteractions(lines, issues);
            verify(batches, never()).acquireExecution(anyString(), anyLong(), anyInt(), any());
        }
    }

    @Test
    void stateConflictKeyExistsInAllSupportedMessageBundles() throws Exception {
        Path current = Path.of("").toAbsolutePath();
        Path i18n = null;
        while (current != null && i18n == null) {
            Path candidate = current.resolve("gameluck-admin/src/main/resources/i18n");
            if (Files.isDirectory(candidate)) i18n = candidate;
            current = current.getParent();
        }
        assertEquals(true, i18n != null);
        for (String file : new String[]{"messages.properties", "messages_zh_CN.properties", "messages_en_US.properties"}) {
            String messages = Files.readString(i18n.resolve(file));
            assertEquals(true, messages.contains("payment.reconciliation.execute.stateConflict="));
            assertEquals(true, messages.contains("payment.reconciliation.execute.invalidLines="));
            assertEquals(true, messages.contains("payment.reconciliation.execute.failed="));
        }
    }

    @Test
    void executionExceptionRecordsStableFailureReasonAndHidesInternalMessage() {
        PaymentReconciliationBatchMapper batches = mock(PaymentReconciliationBatchMapper.class);
        PaymentReconciliationFailureRecorder recorder = mock(PaymentReconciliationFailureRecorder.class);
        PaymentReconciliationExecutionService service = spy(service(batches, recorder));
        PaymentReconciliationExecutionService.ExecutionLease lease =
            new PaymentReconciliationExecutionService.ExecutionLease("tenant-a", 7L, 4);
        doReturn(lease).when(service).acquire(7L);
        doThrow(new IllegalStateException("CSV /server/path secret SQL" )).when(service).reconcile(lease);

        assertThrows(ServiceException.class, () -> service.execute(7L));

        verify(recorder).recordFailure("tenant-a", 7L, "Reconciliation execution failed", 0L, "SYSTEM");
    }

    private PaymentReconciliationExecutionService service(PaymentReconciliationBatchMapper batches,
                                                            PaymentReconciliationFailureRecorder recorder) {
        return new PaymentReconciliationExecutionService(batches, mock(PaymentReconciliationLineMapper.class),
            mock(PaymentReconciliationIssueMapper.class), mock(PaymentReconciliationActionLogMapper.class),
            new PaymentReconciliationMatcher(new ObjectMapper()), mock(PaymentReconciliationExecutionService.PlatformDataSource.class),
            mock(PaymentReconciliationOperatorProvider.class), recorder);
    }

    private PaymentReconciliationBatch batch(String status, int invalidCount) {
        PaymentReconciliationBatch batch = new PaymentReconciliationBatch();
        batch.setId(7L); batch.setTenantId("tenant-a"); batch.setStatus(status);
        batch.setInvalidCount(invalidCount); batch.setVersion(3);
        return batch;
    }
}
