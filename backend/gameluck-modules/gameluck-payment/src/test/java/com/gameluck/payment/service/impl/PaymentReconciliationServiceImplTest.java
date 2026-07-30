package com.gameluck.payment.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gameluck.common.core.exception.ServiceException;
import com.gameluck.common.mybatis.core.page.PageQuery;
import com.gameluck.common.tenant.helper.TenantHelper;
import com.gameluck.payment.config.PaymentProviderProperties;
import com.gameluck.payment.domain.PaymentReconciliationBatch;
import com.gameluck.payment.domain.PaymentReconciliationLine;
import com.gameluck.payment.domain.PaymentReconciliationIssue;
import com.gameluck.payment.domain.bo.PaymentReconciliationBatchBo;
import com.gameluck.payment.mapper.PaymentReconciliationBatchMapper;
import com.gameluck.payment.mapper.PaymentReconciliationLineMapper;
import com.gameluck.payment.mapper.PaymentReconciliationIssueMapper;
import com.gameluck.payment.mapper.PaymentReconciliationActionLogMapper;
import com.gameluck.payment.provider.PaymentProviderAdapter;
import com.gameluck.payment.provider.PaymentProviderRegistry;
import com.gameluck.payment.service.reconciliation.PaymentReconciliationCsvParser;
import com.gameluck.payment.service.reconciliation.ReconciliationParseResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.InOrder;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;
import com.gameluck.payment.service.reconciliation.ReconciliationParsedLine;
import java.time.Instant;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Files;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PaymentReconciliationServiceImplTest {
    private static final String HEADER = "provider_record_id,event_type,provider_session_no,purchase_order_no,pay_currency_code,pay_amount,occurred_time\n";
    private static final String VALID = "r1,PAYMENT_SUCCEEDED,s1,o1,usd,10.00,2026-07-28T01:02:03+08:00\n";

    @Test @Tag("local")
    void uploadUsesCurrentTenantNormalizesProviderSanitizesFilenameAndFinalizesExactCounts() {
        Fixture f = fixture();
        try (MockedStatic<TenantHelper> tenant = mockStatic(TenantHelper.class)) {
            tenant.when(TenantHelper::getTenantId).thenReturn("tenant-a");
            var detail = f.service.upload(" simulated ", LocalDate.of(2026, 7, 28), "C:\\secret\\statement.csv", 1,
                stream(HEADER + VALID));
            assertEquals("tenant-a", detail.getTenantId());
            assertEquals("SIMULATED", detail.getProviderCode());
            assertEquals("statement.csv", detail.getOriginalFileName());
            assertEquals("88", detail.getCreatorId()); assertEquals("operator", detail.getCreatorName());
            assertEquals("VALIDATED", detail.getStatus());
            assertEquals(1, detail.getTotalCount()); assertEquals(1, detail.getValidCount()); assertEquals(0, detail.getInvalidCount());
        }
        ArgumentCaptor<PaymentReconciliationBatch> batch = ArgumentCaptor.forClass(PaymentReconciliationBatch.class);
        verify(f.batches).insert(batch.capture());
        assertEquals("tenant-a", batch.getValue().getTenantId());
        assertEquals(88L, batch.getValue().getCreatorId()); assertEquals("operator", batch.getValue().getCreatorName());
        verify(f.batches).finalizeValidation(eq("tenant-a"), anyLong(), eq(1), eq(1), eq(0), any());
        verify(f.lines).insertBatch(argThat(rows -> rows.size() == 1 && "tenant-a".equals(rows.get(0).getTenantId())
            && "USD".equals(rows.get(0).getCurrencyCode())));
        verify(f.lines, never()).insert(any());
    }

    @Test @Tag("local")
    void invalidLineIsRetainedAndBatchStillValidated() {
        Fixture f = fixture();
        try (MockedStatic<TenantHelper> tenant = mockStatic(TenantHelper.class)) {
            tenant.when(TenantHelper::getTenantId).thenReturn("tenant-a");
            var result = f.service.upload("SIMULATED", LocalDate.now(), "bad.csv", 1,
                stream(HEADER + "r1,NOPE,s1,o1,USD,x,not-a-time\n"));
            assertEquals("VALIDATED", result.getStatus());
            assertEquals(1, result.getTotalCount()); assertEquals(0, result.getValidCount()); assertEquals(1, result.getInvalidCount());
        }
        verify(f.lines).insertBatch(argThat(rows -> "INVALID".equals(rows.get(0).getStatus())
            && rows.get(0).getParseError() != null && rows.get(0).getRawFieldsJson().startsWith("[")));
    }

    @Test @Tag("local")
    void rejectsSameTenantProviderDigestBeforeInsertingLinesButAllowsOtherTenant() {
        Fixture duplicate = fixture();
        when(duplicate.batches.selectByDigest(eq("tenant-a"), eq("SIMULATED"), anyString())).thenReturn(new PaymentReconciliationBatch());
        try (MockedStatic<TenantHelper> tenant = mockStatic(TenantHelper.class)) {
            tenant.when(TenantHelper::getTenantId).thenReturn("tenant-a");
            ServiceException error = assertThrows(ServiceException.class, () -> duplicate.service.upload("SIMULATED", LocalDate.now(), "a.csv", 1, stream(HEADER + VALID)));
            assertEquals("payment.reconciliation.upload.duplicate", error.getMessage());
        }
        verifyNoInteractions(duplicate.lines);

        Fixture allowed = fixture();
        try (MockedStatic<TenantHelper> tenant = mockStatic(TenantHelper.class)) {
            tenant.when(TenantHelper::getTenantId).thenReturn("tenant-b");
            allowed.service.upload("SIMULATED", LocalDate.now(), "a.csv", 1, stream(HEADER + VALID));
        }
        verify(allowed.batches).selectByDigest(eq("tenant-b"), eq("SIMULATED"), anyString());
        verify(allowed.lines).insertBatch(any());
    }

    @Test @Tag("local")
    void queriesAreTenantScopedAndLinesArePaginatedWithStatus() {
        Fixture f = fixture();
        PaymentReconciliationBatch stored = new PaymentReconciliationBatch(); stored.setId(7L); stored.setTenantId("tenant-a"); stored.setStatus("VALIDATED");
        when(f.batches.selectByTenantAndId("tenant-a", 7L)).thenReturn(stored);
        when(f.batches.selectPageByTenant(any(), eq("tenant-a"), any())).thenReturn(new Page<>());
        when(f.lines.selectPageByBatch(any(), eq("tenant-a"), eq(7L), eq("INVALID"))).thenReturn(new Page<>());
        try (MockedStatic<TenantHelper> tenant = mockStatic(TenantHelper.class)) {
            tenant.when(TenantHelper::getTenantId).thenReturn("tenant-a");
            assertEquals("7", f.service.queryDetail(7L).getId());
            f.service.queryPage(new PaymentReconciliationBatchBo(), new PageQuery(10, 1));
            f.service.queryLines(7L, "INVALID", new PageQuery(10, 2));
            assertThrows(ServiceException.class, () -> f.service.queryDetail(8L));
            assertThrows(ServiceException.class, () -> f.service.queryLines(8L, null, new PageQuery(10, 1)));
        }
        verify(f.lines).selectPageByBatch(argThat(p -> p.getCurrent() == 2 && p.getSize() == 10), eq("tenant-a"), eq(7L), eq("INVALID"));
    }

    @Test @Tag("local")
    void issueDetailProjectsCurrentVersionForSubsequentResolution() {
        Fixture f = fixture();
        PaymentReconciliationIssueMapper issues = mock(PaymentReconciliationIssueMapper.class);
        PaymentReconciliationActionLogMapper logs = mock(PaymentReconciliationActionLogMapper.class);
        f.service.setIssueSupport(issues, logs, mock(PaymentReconciliationResolutionService.class));
        PaymentReconciliationIssue issue = new PaymentReconciliationIssue();
        issue.setId(11L); issue.setBatchId(7L); issue.setLineId(12L); issue.setStatus("OPEN"); issue.setVersion(4);
        PaymentReconciliationLine source = new PaymentReconciliationLine();
        source.setId(12L); source.setBatchId(7L); source.setSourceRowNumber(9L); source.setStatus("ISSUE"); source.setRawFieldsJson("[\"provider-record\"]");
        when(issues.selectByTenantAndId("tenant-a", 11L)).thenReturn(issue);
        when(f.lines.selectByTenantAndId("tenant-a", 12L)).thenReturn(source);
        when(logs.selectByIssue("tenant-a", 11L)).thenReturn(List.of());
        try (MockedStatic<TenantHelper> tenant = mockStatic(TenantHelper.class)) {
            tenant.when(TenantHelper::getTenantId).thenReturn("tenant-a");
            var detail = f.service.queryIssueDetail(11L);
            assertEquals(4, detail.getVersion());
            assertEquals(9L, detail.getSourceRowNumber());
            assertEquals("[\"provider-record\"]", detail.getCanonicalOriginalFields());
            assertEquals("12", detail.getSourceLine().getId());
            assertFalse(detail.isPlatformOnly());
        }
    }

    @Test @Tag("local")
    void platformMissingIssueHasNoSourceLineAndIsExplicitlyPlatformOnly() {
        Fixture f = fixture();
        PaymentReconciliationIssueMapper issues = mock(PaymentReconciliationIssueMapper.class);
        PaymentReconciliationActionLogMapper logs = mock(PaymentReconciliationActionLogMapper.class);
        f.service.setIssueSupport(issues, logs, mock(PaymentReconciliationResolutionService.class));
        PaymentReconciliationIssue issue = new PaymentReconciliationIssue();
        issue.setId(13L); issue.setBatchId(7L); issue.setIssueType("PROVIDER_RECORD_MISSING"); issue.setStatus("OPEN"); issue.setVersion(0);
        when(issues.selectByTenantAndId("tenant-a", 13L)).thenReturn(issue);
        when(logs.selectByIssue("tenant-a", 13L)).thenReturn(List.of());
        try (MockedStatic<TenantHelper> tenant = mockStatic(TenantHelper.class)) {
            tenant.when(TenantHelper::getTenantId).thenReturn("tenant-a");
            var detail = f.service.queryIssueDetail(13L);
            assertTrue(detail.isPlatformOnly());
            assertNull(detail.getSourceRowNumber());
            assertNull(detail.getSourceLine());
            assertNull(detail.getCanonicalOriginalFields());
        }
        verify(f.lines, never()).selectByTenantAndId(anyString(), anyLong());
    }

    @Test @Tag("local")
    void infrastructureFailureUsesSeparateRecorderAndDoesNotFinalize() {
        PaymentReconciliationBatchMapper batches = mock(PaymentReconciliationBatchMapper.class);
        PaymentReconciliationLineMapper lines = mock(PaymentReconciliationLineMapper.class);
        PaymentProviderAdapter adapter = mock(PaymentProviderAdapter.class); when(adapter.providerCode()).thenReturn("SIMULATED");
        PaymentProviderProperties props = new PaymentProviderProperties(); props.getSimulated().setEnabled(true);
        PaymentReconciliationBatchCreator creator = mock(PaymentReconciliationBatchCreator.class);
        PaymentReconciliationValidationWorker worker = mock(PaymentReconciliationValidationWorker.class);
        PaymentReconciliationFailureRecorder recorder = mock(PaymentReconciliationFailureRecorder.class);
        doAnswer(inv -> { PaymentReconciliationBatch b = inv.getArgument(0); b.setId(9L); return b; }).when(creator).create(any());
        doThrow(new IllegalStateException("SQL failed at C:\\private\\db.sql\nstack line"))
            .when(worker).validate(eq("tenant-a"), any(), any(), anyLong(), any());
        PaymentReconciliationServiceImpl service = new PaymentReconciliationServiceImpl(batches, lines,
            new PaymentProviderRegistry(List.of(adapter), props), creator, worker, recorder, spooler(), operator());
        try (MockedStatic<TenantHelper> tenant = mockStatic(TenantHelper.class)) {
            tenant.when(TenantHelper::getTenantId).thenReturn("tenant-a");
            assertThrows(ServiceException.class, () -> service.upload("SIMULATED", LocalDate.now(), "a.csv", 1, stream(HEADER + VALID)));
        }
        verify(recorder).record(eq("tenant-a"), eq(9L), argThat((String reason) -> !reason.contains("C:\\")
            && !reason.toLowerCase().contains("sql") && !reason.contains("stack")));
        verify(batches, never()).finalizeValidation(anyString(), anyLong(), anyInt(), anyInt(), anyInt(), any());
        verifyNoInteractions(lines);
    }

    @Test @Tag("local")
    void lifecycleBeansDeclareRealProxyTransactionBoundaries() throws Exception {
        assertEquals(Propagation.REQUIRES_NEW, PaymentReconciliationBatchCreator.class
            .getMethod("create", PaymentReconciliationBatch.class).getAnnotation(Transactional.class).propagation());
        assertEquals(Propagation.REQUIRES_NEW, PaymentReconciliationFailureRecorder.class
            .getMethod("record", String.class, Long.class, String.class).getAnnotation(Transactional.class).propagation());
        assertNotNull(PaymentReconciliationValidationWorker.class
            .getMethod("validate", String.class, PaymentReconciliationBatch.class, Path.class, long.class)
            .getAnnotation(Transactional.class));
    }

    @Test @Tag("local")
    void validationBusinessErrorDoesNotMarkBatchFailed() {
        PaymentReconciliationBatchMapper batches = mock(PaymentReconciliationBatchMapper.class);
        PaymentProviderAdapter adapter = mock(PaymentProviderAdapter.class); when(adapter.providerCode()).thenReturn("SIMULATED");
        PaymentProviderProperties props = new PaymentProviderProperties(); props.getSimulated().setEnabled(true);
        PaymentReconciliationBatchCreator creator = mock(PaymentReconciliationBatchCreator.class);
        doAnswer(inv -> { PaymentReconciliationBatch b = inv.getArgument(0); b.setId(9L); return b; }).when(creator).create(any());
        PaymentReconciliationValidationWorker worker = mock(PaymentReconciliationValidationWorker.class);
        doThrow(new ServiceException(com.gameluck.common.core.utils.MessageUtils.message("payment.reconciliation.upload.file.invalidHeader"))).when(worker).validate(anyString(), any(), any(), anyLong(), any());
        PaymentReconciliationFailureRecorder recorder = mock(PaymentReconciliationFailureRecorder.class);
        PaymentReconciliationServiceImpl service = new PaymentReconciliationServiceImpl(batches,
            mock(PaymentReconciliationLineMapper.class), new PaymentProviderRegistry(List.of(adapter), props),
            creator, worker, recorder, spooler(), operator());
        try (MockedStatic<TenantHelper> tenant = mockStatic(TenantHelper.class)) {
            tenant.when(TenantHelper::getTenantId).thenReturn("tenant-a");
            ServiceException error = assertThrows(ServiceException.class,
                () -> service.upload("SIMULATED", LocalDate.now(), "a.csv", 1, stream(HEADER + VALID)));
            assertEquals(com.gameluck.common.core.utils.MessageUtils.message("payment.reconciliation.upload.file.invalidHeader"), error.getMessage());
        }
        verifyNoInteractions(recorder);
    }

    @Test @Tag("local")
    void workerUsesFixedChunksAndNeverSingleRowInsert() {
        PaymentReconciliationLineMapper lines = mock(PaymentReconciliationLineMapper.class);
        PaymentReconciliationBatchMapper batches = mock(PaymentReconciliationBatchMapper.class);
        when(batches.finalizeValidation(anyString(), anyLong(), anyInt(), anyInt(), anyInt(), any())).thenReturn(1);
        List<ReconciliationParsedLine> parsedLines = IntStream.range(0, 501).mapToObj(i ->
            new ReconciliationParsedLine(i + 2L, "r" + i, "PAYMENT_SUCCEEDED", "s", "o", "USD",
                new BigDecimal("1.000000"), Instant.EPOCH, "[\"r\"]", ReconciliationParsedLine.Status.VALID, null)).toList();
        PaymentReconciliationBatch batch = new PaymentReconciliationBatch(); batch.setId(7L);
        new PaymentReconciliationValidationWorker(lines, batches, parser()).persist("tenant-a", batch,
            new ReconciliationParseResult("digest", 501, 501, 0, parsedLines, null));
        ArgumentCaptor<List<PaymentReconciliationLine>> chunks = ArgumentCaptor.forClass(List.class);
        verify(lines, times(2)).insertBatch(chunks.capture());
        assertEquals(List.of(500, 1), chunks.getAllValues().stream().map(List::size).toList());
        verify(lines, never()).insert(any());
    }

    @Test @Tag("local")
    void concurrentUniqueConflictReturnsStableDuplicateWithoutWorkerOrLines() {
        Fixture f = fixture();
        PaymentReconciliationBatchCreator creator = mock(PaymentReconciliationBatchCreator.class);
        doThrow(new DataIntegrityViolationException("insert failed",
            new java.sql.SQLException("uk_gl_payment_reconciliation_batch_01", "23000"))).when(creator).create(any());
        PaymentReconciliationValidationWorker worker = mock(PaymentReconciliationValidationWorker.class);
        PaymentReconciliationServiceImpl service = new PaymentReconciliationServiceImpl(f.batches, f.lines,
            f.registry, creator, worker, mock(PaymentReconciliationFailureRecorder.class), spooler(), operator());
        try (MockedStatic<TenantHelper> tenant = mockStatic(TenantHelper.class)) {
            tenant.when(TenantHelper::getTenantId).thenReturn("tenant-a");
            ServiceException error = assertThrows(ServiceException.class,
                () -> service.upload("SIMULATED", LocalDate.now(), "a.csv", 1, stream(HEADER + VALID)));
            assertEquals("payment.reconciliation.upload.duplicate", error.getMessage());
        }
        verifyNoInteractions(worker);
        verifyNoInteractions(f.lines);
    }

    @Test @Tag("local")
    void filenameUsesUnicodeCodePointLimitBeforeSpoolingOrCreatingBatch() {
        Fixture accepted = fixture();
        String supplementary = new String(Character.toChars(0x1F600));
        String name255 = supplementary.repeat(251) + ".csv";
        try (MockedStatic<TenantHelper> tenant = mockStatic(TenantHelper.class)) {
            tenant.when(TenantHelper::getTenantId).thenReturn("tenant-a");
            accepted.service.upload("SIMULATED", LocalDate.now(), name255, 1, stream(HEADER + VALID));
        }
        verify(accepted.batches).insert(argThat(batch -> name255.equals(batch.getOriginalFileName())));

        Fixture rejected = fixture();
        String name256 = supplementary.repeat(252) + ".csv";
        try (MockedStatic<TenantHelper> tenant = mockStatic(TenantHelper.class)) {
            tenant.when(TenantHelper::getTenantId).thenReturn("tenant-a");
            ServiceException error = assertThrows(ServiceException.class,
                () -> rejected.service.upload("SIMULATED", LocalDate.now(), name256, 1, stream(HEADER + VALID)));
            assertEquals("payment.reconciliation.upload.filename.tooLong", error.getMessage());
        }
        verifyNoInteractions(rejected.batches, rejected.lines);
    }

    @Test @Tag("local")
    void nonDuplicateIntegrityFailureIsNotReportedAsDuplicate() {
        Fixture f = fixture();
        PaymentReconciliationBatchCreator creator = mock(PaymentReconciliationBatchCreator.class);
        doThrow(new DataIntegrityViolationException("creator_id cannot be null",
            new java.sql.SQLException("not null violation", "23000"))).when(creator).create(any());
        PaymentReconciliationServiceImpl service = new PaymentReconciliationServiceImpl(f.batches, f.lines,
            f.registry, creator, mock(PaymentReconciliationValidationWorker.class),
            mock(PaymentReconciliationFailureRecorder.class), spooler(), operator());
        try (MockedStatic<TenantHelper> tenant = mockStatic(TenantHelper.class)) {
            tenant.when(TenantHelper::getTenantId).thenReturn("tenant-a");
            ServiceException error = assertThrows(ServiceException.class,
                () -> service.upload("SIMULATED", LocalDate.now(), "a.csv", 1, stream(HEADER + VALID)));
            assertNotEquals("payment.reconciliation.upload.duplicate", error.getMessage());
        }
    }

    @Test @Tag("local")
    void uploadActionPersistenceFailureKeepsUploadedBatchWithoutFailureRecorder() {
        Fixture f = fixture();
        PaymentReconciliationValidationWorker worker = mock(PaymentReconciliationValidationWorker.class);
        PaymentReconciliationFailureRecorder recorder = mock(PaymentReconciliationFailureRecorder.class);
        when(worker.validate(anyString(), any(), any(), anyLong(), any())).thenThrow(
            new PaymentReconciliationValidationWorker.UploadActionPersistenceException(
                new DataIntegrityViolationException("action log rejected")));
        PaymentReconciliationServiceImpl service = new PaymentReconciliationServiceImpl(f.batches, f.lines,
            f.registry, new PaymentReconciliationBatchCreator(f.batches), worker, recorder, spooler(), operator());
        try (MockedStatic<TenantHelper> tenant = mockStatic(TenantHelper.class)) {
            tenant.when(TenantHelper::getTenantId).thenReturn("tenant-a");
            ServiceException error = assertThrows(ServiceException.class,
                () -> service.upload("SIMULATED", LocalDate.now(), "a.csv", 1, stream(HEADER + VALID)));
            assertEquals("payment.reconciliation.upload.failed", error.getMessage());
        }
        verifyNoInteractions(recorder);
    }

    @Test @Tag("local")
    void parserFileErrorCreatesNoMisleadingUploadedBatch() {
        PaymentReconciliationBatchMapper batches = mock(PaymentReconciliationBatchMapper.class);
        PaymentProviderAdapter adapter = mock(PaymentProviderAdapter.class); when(adapter.providerCode()).thenReturn("SIMULATED");
        PaymentProviderProperties props = new PaymentProviderProperties(); props.getSimulated().setEnabled(true);
        PaymentReconciliationBatchCreator creator = mock(PaymentReconciliationBatchCreator.class);
        PaymentReconciliationValidationWorker worker = mock(PaymentReconciliationValidationWorker.class);
        PaymentReconciliationFailureRecorder recorder = mock(PaymentReconciliationFailureRecorder.class);
        PaymentReconciliationServiceImpl service = new PaymentReconciliationServiceImpl(batches,
            mock(PaymentReconciliationLineMapper.class), new PaymentProviderRegistry(List.of(adapter), props),
            creator, worker, recorder, spooler(), operator());
        try (MockedStatic<TenantHelper> tenant = mockStatic(TenantHelper.class)) {
            tenant.when(TenantHelper::getTenantId).thenReturn("tenant-a");
            ServiceException error = assertThrows(ServiceException.class, () -> service.upload("SIMULATED",
                LocalDate.now(), "bad.csv", PaymentReconciliationCsvParser.MAX_BYTES + 1, stream("ignored")));
            assertEquals(com.gameluck.common.core.utils.MessageUtils.message("payment.reconciliation.upload.file.tooLarge"), error.getMessage());
        }
        verifyNoInteractions(creator, worker, recorder, batches);
    }

    @Test @Tag("local")
    void uploadedIsCreatedBeforeParserWorkerAndInfrastructureFailureCleansSpool() throws Exception {
        Path testDirectory = Files.createTempDirectory("reconciliation-order-");
        Path path = Files.createFile(testDirectory.resolve("test.spool"));
        Files.writeString(path, HEADER + VALID, StandardCharsets.UTF_8);
        PaymentReconciliationUploadSpooler spooler = mock(PaymentReconciliationUploadSpooler.class);
        when(spooler.spool(any(), anyLong())).thenReturn(new PaymentReconciliationUploadSpooler.Spool(
            path.getParent(), path, "digest", Files.size(path), "test-id", new PaymentReconciliationSpoolCleanup()));
        PaymentReconciliationBatchMapper batches = mock(PaymentReconciliationBatchMapper.class);
        PaymentReconciliationLineMapper lines = mock(PaymentReconciliationLineMapper.class);
        PaymentProviderAdapter adapter = mock(PaymentProviderAdapter.class); when(adapter.providerCode()).thenReturn("SIMULATED");
        PaymentProviderProperties props = new PaymentProviderProperties(); props.getSimulated().setEnabled(true);
        PaymentReconciliationBatchCreator creator = mock(PaymentReconciliationBatchCreator.class);
        doAnswer(inv -> { PaymentReconciliationBatch b = inv.getArgument(0); b.setId(9L); return b; }).when(creator).create(any());
        PaymentReconciliationValidationWorker worker = mock(PaymentReconciliationValidationWorker.class);
        doThrow(new IllegalStateException("parser infrastructure")).when(worker).validate(anyString(), any(), eq(path), anyLong(), any());
        PaymentReconciliationFailureRecorder recorder = mock(PaymentReconciliationFailureRecorder.class);
        PaymentReconciliationServiceImpl service = new PaymentReconciliationServiceImpl(batches, lines,
            new PaymentProviderRegistry(List.of(adapter), props), creator, worker, recorder, spooler, operator());
        try (MockedStatic<TenantHelper> tenant = mockStatic(TenantHelper.class)) {
            tenant.when(TenantHelper::getTenantId).thenReturn("tenant-a");
            assertThrows(ServiceException.class, () -> service.upload("SIMULATED", LocalDate.now(), "a.csv", 1, stream(HEADER + VALID)));
        }
        InOrder order = inOrder(creator, worker, recorder);
        order.verify(creator).create(any());
        order.verify(worker).validate(eq("tenant-a"), any(), eq(path), anyLong(), any());
        order.verify(recorder).record(eq("tenant-a"), eq(9L), eq("RECONCILIATION_FILE_PROCESSING_FAILED"));
        assertFalse(Files.exists(path));
    }

    @Test @Tag("local")
    void fileRejectionKeepsUploadedAndNeverCallsFailureRecorderOrWritesLines() {
        PaymentReconciliationBatchMapper batches = mock(PaymentReconciliationBatchMapper.class);
        PaymentReconciliationLineMapper lines = mock(PaymentReconciliationLineMapper.class);
        PaymentProviderAdapter adapter = mock(PaymentProviderAdapter.class); when(adapter.providerCode()).thenReturn("SIMULATED");
        PaymentProviderProperties props = new PaymentProviderProperties(); props.getSimulated().setEnabled(true);
        PaymentReconciliationBatchCreator creator = mock(PaymentReconciliationBatchCreator.class);
        doAnswer(inv -> { PaymentReconciliationBatch b = inv.getArgument(0); b.setId(11L); return b; }).when(creator).create(any());
        PaymentReconciliationFailureRecorder recorder = mock(PaymentReconciliationFailureRecorder.class);
        PaymentReconciliationValidationWorker worker = new PaymentReconciliationValidationWorker(lines, batches, parser());
        PaymentReconciliationServiceImpl service = new PaymentReconciliationServiceImpl(batches, lines,
            new PaymentProviderRegistry(List.of(adapter), props), creator, worker, recorder, spooler(), operator());
        try (MockedStatic<TenantHelper> tenant = mockStatic(TenantHelper.class)) {
            tenant.when(TenantHelper::getTenantId).thenReturn("tenant-a");
            ServiceException error = assertThrows(ServiceException.class,
                () -> service.upload("SIMULATED", LocalDate.now(), "bad.csv", 3, stream("bad")));
            assertEquals(com.gameluck.common.core.utils.MessageUtils.message("payment.reconciliation.upload.file.invalidHeader"), error.getMessage());
        }
        verify(creator).create(argThat(batch -> "UPLOADED".equals(batch.getStatus())));
        verifyNoInteractions(recorder, lines);
        verify(batches, never()).finalizeValidation(anyString(), anyLong(), anyInt(), anyInt(), anyInt(), any());
    }

    @Test @Tag("local")
    void spoolUsesPrivatePermissionsAndCleanupSeam() throws Exception {
        PaymentReconciliationSpoolCleanup cleanup = mock(PaymentReconciliationSpoolCleanup.class);
        PaymentReconciliationUploadSpooler.Spool spool = new PaymentReconciliationUploadSpooler(cleanup)
            .spool(stream(HEADER + VALID), 1);
        var posix = Files.getFileAttributeView(spool.path(), java.nio.file.attribute.PosixFileAttributeView.class);
        if (posix != null) {
            assertEquals(java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"), posix.readAttributes().permissions());
            assertEquals(java.nio.file.attribute.PosixFilePermissions.fromString("rwx------"),
                Files.getPosixFilePermissions(spool.directory()));
        } else {
            var acl = Files.getFileAttributeView(spool.path(), java.nio.file.attribute.AclFileAttributeView.class);
            assertNotNull(acl); assertEquals(1, acl.getAcl().size()); assertEquals(Files.getOwner(spool.path()), acl.getAcl().get(0).principal());
        }
        spool.close();
        verify(cleanup).cleanup(spool.path(), spool.directory(), spool.fileId());
        Files.deleteIfExists(spool.path()); Files.deleteIfExists(spool.directory());
    }

    @Test @Tag("local")
    void digestMismatchIsCheckedBeforeAnyLineInsert() throws Exception {
        PaymentReconciliationLineMapper lines = mock(PaymentReconciliationLineMapper.class);
        PaymentReconciliationBatchMapper batches = mock(PaymentReconciliationBatchMapper.class);
        PaymentReconciliationCsvParser parser = mock(PaymentReconciliationCsvParser.class);
        when(parser.parse(any(), anyLong())).thenReturn(new ReconciliationParseResult("00".repeat(32), 0, 0, 0, List.of(), null));
        Path path = Files.createTempFile("digest-mismatch-", ".spool");
        PaymentReconciliationBatch batch = new PaymentReconciliationBatch(); batch.setId(7L); batch.setFileDigest("11".repeat(32));
        try {
            assertThrows(SecurityException.class,
                () -> new PaymentReconciliationValidationWorker(lines, batches, parser).validate("tenant-a", batch, path, 0));
        } finally { Files.deleteIfExists(path); }
        verifyNoInteractions(lines);
        verify(batches, never()).finalizeValidation(anyString(), anyLong(), anyInt(), anyInt(), anyInt(), any());
    }

    @Test @Tag("local")
    void missingOperatorIdentityFailsBeforeBatchCreationAndCleansSpool() throws Exception {
        for (PaymentReconciliationOperatorProvider.Operator invalid : List.of(
            new PaymentReconciliationOperatorProvider.Operator(null, "name"),
            new PaymentReconciliationOperatorProvider.Operator(9L, "  "))) {
            Path directory = Files.createTempDirectory("operator-invalid-"); Path path = Files.createFile(directory.resolve("test.spool"));
            PaymentReconciliationSpoolCleanup cleanup = new PaymentReconciliationSpoolCleanup();
            PaymentReconciliationUploadSpooler spooler = mock(PaymentReconciliationUploadSpooler.class);
            when(spooler.spool(any(), anyLong())).thenReturn(new PaymentReconciliationUploadSpooler.Spool(
                directory, path, "00".repeat(32), 0, "id", cleanup));
            PaymentReconciliationBatchCreator creator = mock(PaymentReconciliationBatchCreator.class);
            PaymentReconciliationOperatorProvider operator = mock(PaymentReconciliationOperatorProvider.class);
            when(operator.current()).thenReturn(invalid);
            PaymentProviderAdapter adapter = mock(PaymentProviderAdapter.class); when(adapter.providerCode()).thenReturn("SIMULATED");
            PaymentProviderProperties props = new PaymentProviderProperties(); props.getSimulated().setEnabled(true);
            PaymentReconciliationServiceImpl service = new PaymentReconciliationServiceImpl(mock(PaymentReconciliationBatchMapper.class),
                mock(PaymentReconciliationLineMapper.class), new PaymentProviderRegistry(List.of(adapter), props), creator,
                mock(PaymentReconciliationValidationWorker.class), mock(PaymentReconciliationFailureRecorder.class), spooler, operator);
            try (MockedStatic<TenantHelper> tenant = mockStatic(TenantHelper.class)) {
                tenant.when(TenantHelper::getTenantId).thenReturn("tenant-a");
                assertThrows(ServiceException.class, () -> service.upload("SIMULATED", LocalDate.now(), "a.csv", 0, stream("")));
            }
            verifyNoInteractions(creator); assertFalse(Files.exists(path)); assertFalse(Files.exists(directory));
        }
    }

    @Test @Tag("local")
    void cleanupFailureRegistersDirectoryThenFileForReverseJvmDeletion() throws Exception {
        PaymentReconciliationDeleteOnExitRegistrar registrar = mock(PaymentReconciliationDeleteOnExitRegistrar.class);
        PaymentReconciliationSpoolCleanup cleanup = new PaymentReconciliationSpoolCleanup(registrar);
        Path directory = Files.createTempDirectory("cleanup-order-"); Path file = Files.createFile(directory.resolve("spool"));
        Path blocker = Files.createFile(directory.resolve("blocker"));
        cleanup.cleanup(file, directory, "safe-id");
        InOrder order = inOrder(registrar); order.verify(registrar).register(directory); order.verify(registrar).register(file);
        Files.deleteIfExists(blocker); Files.deleteIfExists(directory);
    }

    private Fixture fixture() {
        PaymentReconciliationBatchMapper batches = mock(PaymentReconciliationBatchMapper.class);
        PaymentReconciliationLineMapper lines = mock(PaymentReconciliationLineMapper.class);
        doAnswer(inv -> { ((PaymentReconciliationBatch) inv.getArgument(0)).setId(7L); return 1; }).when(batches).insert(any());
        when(batches.finalizeValidation(anyString(), anyLong(), anyInt(), anyInt(), anyInt(), any())).thenReturn(1);
        PaymentProviderAdapter adapter = mock(PaymentProviderAdapter.class); when(adapter.providerCode()).thenReturn("SIMULATED");
        PaymentProviderProperties props = new PaymentProviderProperties(); props.getSimulated().setEnabled(true);
        PaymentProviderRegistry registry = new PaymentProviderRegistry(List.of(adapter), props);
        PaymentReconciliationBatchCreator creator = new PaymentReconciliationBatchCreator(batches);
        PaymentReconciliationValidationWorker worker = new PaymentReconciliationValidationWorker(lines, batches, parser());
        return new Fixture(batches, lines, registry, new PaymentReconciliationServiceImpl(batches, lines, registry, creator, worker,
            mock(PaymentReconciliationFailureRecorder.class), spooler(), operator()));
    }

    private ByteArrayInputStream stream(String csv) { return new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)); }
    private PaymentReconciliationCsvParser parser() { return new PaymentReconciliationCsvParser(new ObjectMapper()); }
    private PaymentReconciliationUploadSpooler spooler() {
        return new PaymentReconciliationUploadSpooler(new PaymentReconciliationSpoolCleanup());
    }
    private PaymentReconciliationOperatorProvider operator() {
        PaymentReconciliationOperatorProvider provider = mock(PaymentReconciliationOperatorProvider.class);
        when(provider.current()).thenReturn(new PaymentReconciliationOperatorProvider.Operator(88L, "operator"));
        return provider;
    }
    private record Fixture(PaymentReconciliationBatchMapper batches, PaymentReconciliationLineMapper lines,
                           PaymentProviderRegistry registry,
                           PaymentReconciliationServiceImpl service) { }
}
