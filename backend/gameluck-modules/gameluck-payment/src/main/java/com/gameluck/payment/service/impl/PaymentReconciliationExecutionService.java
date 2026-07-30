package com.gameluck.payment.service.impl;

import cn.hutool.core.util.IdUtil;
import com.gameluck.common.core.exception.ServiceException;
import com.gameluck.common.core.utils.MessageUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gameluck.common.tenant.helper.TenantHelper;
import com.gameluck.payment.domain.PaymentReconciliationActionLog;
import com.gameluck.payment.domain.PaymentReconciliationBatch;
import com.gameluck.payment.domain.PaymentReconciliationIssue;
import com.gameluck.payment.domain.PaymentReconciliationLine;
import com.gameluck.payment.domain.vo.PaymentReconciliationBatchDetailVo;
import com.gameluck.payment.mapper.PaymentReconciliationActionLogMapper;
import com.gameluck.payment.mapper.PaymentReconciliationBatchMapper;
import com.gameluck.payment.mapper.PaymentReconciliationIssueMapper;
import com.gameluck.payment.mapper.PaymentReconciliationLineMapper;
import com.gameluck.payment.service.reconciliation.PaymentReconciliationMatcher;
import com.gameluck.payment.service.reconciliation.ReconciliationMatchResult;
import com.gameluck.payment.service.reconciliation.ReconciliationParsedLine;
import com.gameluck.payment.service.reconciliation.ReconciliationPlatformEventProjection;
import com.gameluck.payment.service.reconciliation.ReconciliationPlatformSnapshot;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.function.Consumer;

@Service
public class PaymentReconciliationExecutionService {
    private static final int LINE_CHUNK_SIZE = 500;
    private static final String FAILURE_REASON = "Reconciliation execution failed";
    private final PaymentReconciliationBatchMapper batchMapper;
    private final PaymentReconciliationLineMapper lineMapper;
    private final PaymentReconciliationIssueMapper issueMapper;
    private final PaymentReconciliationActionLogMapper actionLogMapper;
    private final PaymentReconciliationMatcher matcher;
    private final PlatformDataSource platformDataSource;
    private final PaymentReconciliationOperatorProvider operatorProvider;
    private final PaymentReconciliationFailureRecorder failureRecorder;
    private final ObjectProvider<PaymentReconciliationExecutionService> proxyProvider;
    private final ObjectMapper objectMapper;

    public PaymentReconciliationExecutionService(PaymentReconciliationBatchMapper batchMapper,
        PaymentReconciliationLineMapper lineMapper, PaymentReconciliationIssueMapper issueMapper,
        PaymentReconciliationActionLogMapper actionLogMapper, PaymentReconciliationMatcher matcher,
        PlatformDataSource platformDataSource, PaymentReconciliationOperatorProvider operatorProvider,
        PaymentReconciliationFailureRecorder failureRecorder) {
        this(batchMapper, lineMapper, issueMapper, actionLogMapper, matcher, platformDataSource,
            operatorProvider, failureRecorder, null, new ObjectMapper());
    }

    @Autowired
    public PaymentReconciliationExecutionService(PaymentReconciliationBatchMapper batchMapper,
        PaymentReconciliationLineMapper lineMapper, PaymentReconciliationIssueMapper issueMapper,
        PaymentReconciliationActionLogMapper actionLogMapper, PaymentReconciliationMatcher matcher,
        PlatformDataSource platformDataSource, PaymentReconciliationOperatorProvider operatorProvider,
        PaymentReconciliationFailureRecorder failureRecorder,
        ObjectProvider<PaymentReconciliationExecutionService> proxyProvider,
        ObjectMapper objectMapper) {
        this.batchMapper = batchMapper; this.lineMapper = lineMapper; this.issueMapper = issueMapper;
        this.actionLogMapper = actionLogMapper; this.matcher = matcher; this.platformDataSource = platformDataSource;
        this.operatorProvider = operatorProvider; this.failureRecorder = failureRecorder; this.proxyProvider = proxyProvider;
        this.objectMapper = objectMapper;
    }

    public PaymentReconciliationBatchDetailVo execute(Long batchId) {
        PaymentReconciliationExecutionService proxy = proxyProvider == null ? this : proxyProvider.getObject();
        ExecutionLease lease = proxy.acquire(batchId);
        try {
            return proxy.reconcile(lease);
        } catch (RuntimeException exception) {
            failureRecorder.recordFailure(lease.tenantId(), lease.batchId(), FAILURE_REASON,
                lease.operatorId(), lease.operatorName());
            throw new ServiceException(MessageUtils.message("payment.reconciliation.execute.failed"));
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ExecutionLease acquire(Long batchId) {
        return acquire(TenantHelper.getTenantId(), batchId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ExecutionLease acquire(String tenantId, Long batchId) {
        PaymentReconciliationBatch batch = batchMapper.selectByTenantAndId(tenantId, batchId);
        if (batch == null || !"VALIDATED".equals(batch.getStatus())) {
            throw stateConflict();
        }
        if (batch.getInvalidCount() != null && batch.getInvalidCount() > 0) {
            throw new ServiceException(MessageUtils.message("payment.reconciliation.execute.invalidLines"));
        }
        PaymentReconciliationOperatorProvider.Operator operator = operatorProvider.current();
        Long operatorId = operator == null || operator.id() == null ? 0L : operator.id();
        String operatorName = operator == null || operator.name() == null || operator.name().isBlank()
            ? "SYSTEM" : operator.name();
        int version = batch.getVersion() == null ? 0 : batch.getVersion();
        if (batchMapper.acquireExecution(tenantId, batchId, version, new Date()) != 1) {
            throw stateConflict();
        }
        return new ExecutionLease(tenantId, batchId, version + 1, operatorId, operatorName);
    }

    @Transactional
    public PaymentReconciliationBatchDetailVo reconcile(ExecutionLease lease) {
        PaymentReconciliationBatch batch = batchMapper.selectByTenantAndId(lease.tenantId(), lease.batchId());
        if (batch == null || !"RECONCILING".equals(batch.getStatus())
            || !Integer.valueOf(lease.version()).equals(batch.getVersion())) {
            throw stateConflict();
        }
        int matched = 0;
        int issues = 0;
        long cursorId = 0L;
        while (true) {
            List<PaymentReconciliationLine> lines = lineMapper.selectValidChunk(
                lease.tenantId(), lease.batchId(), cursorId, LINE_CHUNK_SIZE);
            if (lines.isEmpty()) break;
            Map<Long, ReconciliationPlatformSnapshot> snapshots = platformDataSource.prefetch(
                lease.tenantId(), batch.getProviderCode(), lines);
            List<Long> matchedIds = new ArrayList<>();
            List<Long> issueIds = new ArrayList<>();
            List<PaymentReconciliationIssue> chunkIssues = new ArrayList<>();
            for (PaymentReconciliationLine line : lines) {
                ReconciliationPlatformSnapshot snapshot = snapshots.get(line.getId());
                if (snapshot == null) throw new IllegalStateException("Missing prefetched reconciliation snapshot");
                ReconciliationMatchResult result = matcher.match(parsed(line), snapshot);
                if (result.matched()) { matchedIds.add(line.getId()); matched++; }
                else { issueIds.add(line.getId()); chunkIssues.add(issue(lease, line, result, snapshot)); issues++; }
            }
            conclude(lease, matchedIds, "MATCHED");
            conclude(lease, issueIds, "ISSUE");
            if (!chunkIssues.isEmpty() && issueMapper.insertBatch(chunkIssues) != chunkIssues.size()) throw stateConflict();
            cursorId = lines.get(lines.size() - 1).getId();
        }
        Instant start = new java.sql.Date(batch.getStatementDate().getTime()).toLocalDate()
            .atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant next = start.plusSeconds(86_400);
        int[] missingCount = {0};
        platformDataSource.forEachMissingProviderEventPage(lease.tenantId(), lease.batchId(),
            batch.getProviderCode(), start, next, page -> {
                List<PaymentReconciliationIssue> pageIssues = page.stream()
                    .map(event -> missingIssue(lease, event)).toList();
                if (issueMapper.insertBatch(pageIssues) != pageIssues.size()) throw stateConflict();
                missingCount[0] += pageIssues.size();
            });
        issues += missingCount[0];
        Date now = new Date();
        if (batchMapper.completeExecution(lease.tenantId(), lease.batchId(), lease.version(), matched, issues, now) != 1) {
            throw stateConflict();
        }
        actionLogMapper.insert(executionLog(lease, now));
        batch.setStatus("COMPLETED"); batch.setMatchedCount(matched); batch.setDiscrepancyCount(issues); batch.setUpdateTime(now);
        return detail(batch);
    }

    private void conclude(ExecutionLease lease, List<Long> ids, String status) {
        if (!ids.isEmpty() && lineMapper.concludeValidLines(lease.tenantId(), lease.batchId(), ids, status) != ids.size()) {
            throw stateConflict();
        }
    }

    private ReconciliationParsedLine parsed(PaymentReconciliationLine l) {
        return new ReconciliationParsedLine(l.getSourceRowNumber(), l.getProviderRecordId(), l.getEventType(),
            l.getProviderSessionNo(), l.getPurchaseOrderNo(), l.getCurrencyCode(), l.getAmount(),
            l.getOccurredTime().toInstant(), l.getRawFieldsJson(), ReconciliationParsedLine.Status.VALID, null);
    }

    private PaymentReconciliationIssue issue(ExecutionLease lease, PaymentReconciliationLine line,
        ReconciliationMatchResult result, ReconciliationPlatformSnapshot snapshot) {
        PaymentReconciliationIssue issue = baseIssue(lease);
        issue.setLineId(line.getId()); issue.setIssueType(result.primaryIssueType().orElseThrow().name());
        issue.setSessionNo(line.getProviderSessionNo()); issue.setPurchaseOrderNo(line.getPurchaseOrderNo());
        issue.setProviderEventType(line.getEventType()); issue.setPlatformEventType(snapshot.webhookEventType());
        issue.setProviderCurrencyCode(line.getCurrencyCode()); issue.setPlatformCurrencyCode(snapshot.currency());
        issue.setProviderAmount(line.getAmount()); issue.setPlatformAmount(snapshot.amount());
        issue.setPaymentSessionId(snapshot.paymentSessionId()); issue.setPurchaseOrderId(snapshot.purchaseOrderId());
        issue.setWebhookEventId(snapshot.webhookEventId()); issue.setReversalId(snapshot.reversalId());
        issue.setDiagnosticSnapshotJson(result.diagnosticSnapshotJson());
        return issue;
    }

    private PaymentReconciliationIssue missingIssue(ExecutionLease lease, ReconciliationPlatformEventProjection event) {
        PaymentReconciliationIssue issue = baseIssue(lease); issue.setIssueType("PROVIDER_RECORD_MISSING");
        issue.setWebhookEventId(event.id()); issue.setPlatformEventType(event.eventType());
        issue.setPaymentSessionId(event.paymentSessionId()); issue.setPurchaseOrderId(event.purchaseOrderId());
        issue.setReversalId(event.reversalId()); issue.setSessionNo(event.providerSessionNo());
        issue.setPurchaseOrderNo(event.purchaseOrderNo()); issue.setPlatformCurrencyCode(event.currency());
        issue.setPlatformAmount(event.amount()); issue.setPlatformStatus(event.status());
        issue.setDiagnosticSnapshotJson(serialize(new ProviderMissingDiagnostic(true, event.id(),
            event.providerEventId(), event.eventType(), event.providerSessionNo(), event.purchaseOrderNo(),
            event.currency(), event.amount(), event.occurredTime(), event.receivedTime(), event.status(),
            event.paymentSessionId(), event.purchaseOrderId(), event.reversalId())));
        return issue;
    }

    private PaymentReconciliationIssue baseIssue(ExecutionLease lease) {
        PaymentReconciliationIssue issue = new PaymentReconciliationIssue(); issue.setId(IdUtil.getSnowflakeNextId());
        issue.setTenantId(lease.tenantId()); issue.setBatchId(lease.batchId()); issue.setStatus("OPEN");
        issue.setVersion(0); issue.setCreateTime(new Date()); return issue;
    }

    private PaymentReconciliationActionLog executionLog(ExecutionLease lease, Date now) {
        PaymentReconciliationActionLog log = new PaymentReconciliationActionLog(); log.setId(IdUtil.getSnowflakeNextId());
        log.setTenantId(lease.tenantId()); log.setBatchId(lease.batchId()); log.setActionType("EXECUTE");
        log.setBeforeStatus("VALIDATED"); log.setAfterStatus("COMPLETED"); log.setOperatorId(lease.operatorId());
        log.setOperatorName(lease.operatorName()); log.setCreateTime(now); return log;
    }

    private PaymentReconciliationBatchDetailVo detail(PaymentReconciliationBatch b) {
        PaymentReconciliationBatchDetailVo v = new PaymentReconciliationBatchDetailVo(); v.setId(b.getId() == null ? null : b.getId().toString());
        v.setTenantId(b.getTenantId()); v.setProviderCode(b.getProviderCode()); v.setStatementDate(b.getStatementDate());
        v.setOriginalFileName(b.getOriginalFileName()); v.setFileDigest(b.getFileDigest()); v.setTotalCount(b.getTotalCount());
        v.setValidCount(b.getValidCount()); v.setInvalidCount(b.getInvalidCount()); v.setMatchedCount(b.getMatchedCount());
        v.setDiscrepancyCount(b.getDiscrepancyCount()); v.setStatus(b.getStatus()); v.setCreateTime(b.getCreateTime());
        v.setUpdateTime(b.getUpdateTime()); return v;
    }

    private String serialize(Object value) {
        try { return objectMapper.writeValueAsString(value); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("Unable to serialize reconciliation diagnostic", exception); }
    }

    private ServiceException stateConflict() {
        return new ServiceException(MessageUtils.message("payment.reconciliation.execute.stateConflict"));
    }

    private record ProviderMissingDiagnostic(boolean providerAbsent, Long webhookEventId,
        String providerEventId, String eventType, String providerSessionNo, String purchaseOrderNo,
        String currency, java.math.BigDecimal amount, Instant occurredTime, Instant receivedTime,
        String status, Long paymentSessionId, Long purchaseOrderId, Long reversalId) { }

    public record ExecutionLease(String tenantId, Long batchId, int version, Long operatorId, String operatorName) {
        public ExecutionLease(String tenantId, Long batchId, int version) { this(tenantId, batchId, version, 0L, "SYSTEM"); }
    }

    public interface PlatformDataSource {
        ReconciliationPlatformSnapshot snapshot(String tenantId, String providerCode, PaymentReconciliationLine line);
        default Map<Long, ReconciliationPlatformSnapshot> prefetch(String tenantId, String providerCode,
            List<PaymentReconciliationLine> lines) {
            Map<Long, ReconciliationPlatformSnapshot> result = new LinkedHashMap<>();
            for (PaymentReconciliationLine line : lines) result.put(line.getId(), snapshot(tenantId, providerCode, line));
            return Map.copyOf(result);
        }
        default void forEachMissingProviderEventPage(String tenantId, Long batchId, String providerCode,
            Instant windowStart, Instant windowNext,
            Consumer<List<ReconciliationPlatformEventProjection>> consumer) { }
    }
}
