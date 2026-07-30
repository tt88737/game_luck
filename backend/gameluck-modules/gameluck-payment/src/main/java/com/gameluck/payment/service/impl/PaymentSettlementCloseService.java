package com.gameluck.payment.service.impl;

import cn.hutool.core.util.IdUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gameluck.common.core.exception.ServiceException;
import com.gameluck.payment.domain.PaymentSettlementActionLog;
import com.gameluck.payment.domain.PaymentSettlementBatch;
import com.gameluck.payment.domain.bo.PaymentSettlementCloseBo;
import com.gameluck.payment.mapper.PaymentSettlementActionLogMapper;
import com.gameluck.payment.mapper.PaymentSettlementBatchMapper;
import com.gameluck.payment.service.settlement.PaymentSettlementReconciliationGate;
import com.gameluck.payment.service.settlement.SettlementReconciliationEvidence;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
public class PaymentSettlementCloseService {
    private final PaymentSettlementBatchMapper batchMapper;
    private final PaymentSettlementActionLogMapper logMapper;
    private final PaymentSettlementReconciliationGate gate;
    private final PaymentReconciliationOperatorProvider operatorProvider;
    private final PaymentSettlementCloseRejectionRecorder rejectionRecorder;
    private final ObjectMapper objectMapper;

    public PaymentSettlementCloseService(PaymentSettlementBatchMapper batchMapper,
        PaymentSettlementActionLogMapper logMapper, PaymentSettlementReconciliationGate gate,
        PaymentReconciliationOperatorProvider operatorProvider,
        PaymentSettlementCloseRejectionRecorder rejectionRecorder, ObjectMapper objectMapper) {
        this.batchMapper = batchMapper; this.logMapper = logMapper; this.gate = gate;
        this.operatorProvider = operatorProvider; this.rejectionRecorder = rejectionRecorder;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public PaymentSettlementBatch close(String tenantId, Long batchId, PaymentSettlementCloseBo bo) {
        PaymentSettlementBatch batch = batchMapper.selectByTenantAndId(tenantId, batchId);
        if (batch == null || !"CALCULATED".equals(batch.getStatus()) || bo == null
            || bo.getVersion() == null || bo.getRemark() == null || bo.getRemark().isBlank()) {
            throw new ServiceException("payment.settlement.close.stateConflict");
        }
        SettlementReconciliationEvidence evidence = gate.evaluate(tenantId, batch.getProviderCode(),
            batch.getCurrencyCode(), batch.getPeriodStart().toInstant(), batch.getPeriodEnd().toInstant());
        String evidenceJson = json(evidence);
        PaymentReconciliationOperatorProvider.Operator actor = operatorProvider.current();
        long operatorId = actor == null || actor.id() == null ? 0L : actor.id();
        String operatorName = actor == null || actor.name() == null || actor.name().isBlank()
            ? "SYSTEM" : actor.name().trim();
        Date now = new Date();
        if (!evidence.missingDates().isEmpty() || evidence.openIssueCount() > 0) {
            rejectionRecorder.record(tenantId, batchId, operatorId, operatorName, evidenceJson, now);
            throw new ServiceException(!evidence.missingDates().isEmpty()
                ? "payment.settlement.close.missingReconciliation" : "payment.settlement.close.openIssues");
        }
        String remark = bo.getRemark().trim();
        if (batchMapper.closeCalculated(tenantId, batchId, bo.getVersion(), evidence.coveredDates().size(),
            evidence.openIssueCount(), evidenceJson, operatorId, operatorName, remark, now) != 1) {
            throw new ServiceException("payment.settlement.close.stateConflict");
        }
        logMapper.insert(log(batchId, tenantId, "CLOSE", "CALCULATED", "CLOSED", operatorId,
            operatorName, evidenceJson, now));
        batch.setStatus("CLOSED"); batch.setReconciliationCoverageCount(evidence.coveredDates().size());
        batch.setOpenIssueCount(evidence.openIssueCount()); batch.setEvidenceSnapshotJson(evidenceJson);
        batch.setCloserId(operatorId); batch.setCloserName(operatorName); batch.setCloseRemark(remark);
        batch.setClosedTime(now); batch.setVersion(bo.getVersion() + 1); batch.setUpdateTime(now); return batch;
    }

    private String json(SettlementReconciliationEvidence evidence) {
        try { return objectMapper.writeValueAsString(evidence); }
        catch (JsonProcessingException exception) { throw new ServiceException("payment.settlement.close.evidenceFailed"); }
    }

    private PaymentSettlementActionLog log(Long batchId, String tenantId, String type, String before, String after,
        long operatorId, String operatorName, String evidence, Date now) {
        PaymentSettlementActionLog log = new PaymentSettlementActionLog(); log.setId(IdUtil.getSnowflakeNextId());
        log.setTenantId(tenantId); log.setBatchId(batchId); log.setActionType(type); log.setBeforeStatus(before);
        log.setAfterStatus(after); log.setOperatorId(operatorId); log.setOperatorName(operatorName);
        log.setEvidenceSnapshotJson(evidence); log.setCreateTime(now); return log;
    }
}
