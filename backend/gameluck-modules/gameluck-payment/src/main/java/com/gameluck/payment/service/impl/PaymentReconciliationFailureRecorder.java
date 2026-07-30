package com.gameluck.payment.service.impl;

import com.gameluck.payment.mapper.PaymentReconciliationBatchMapper;
import com.gameluck.payment.domain.PaymentReconciliationActionLog;
import com.gameluck.payment.mapper.PaymentReconciliationActionLogMapper;
import cn.hutool.core.util.IdUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.Date;

@Component
public class PaymentReconciliationFailureRecorder {
    private final PaymentReconciliationBatchMapper batchMapper;
    private final PaymentReconciliationActionLogMapper actionLogMapper;

    public PaymentReconciliationFailureRecorder(PaymentReconciliationBatchMapper batchMapper) {
        this(batchMapper, null);
    }

    @Autowired
    public PaymentReconciliationFailureRecorder(PaymentReconciliationBatchMapper batchMapper,
                                                PaymentReconciliationActionLogMapper actionLogMapper) {
        this.batchMapper = batchMapper;
        this.actionLogMapper = actionLogMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String tenantId, Long batchId, String reason) {
        batchMapper.markFailed(tenantId, batchId, reason, new Date());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(String tenantId, Long batchId, String stableReason) {
        recordFailure(tenantId, batchId, stableReason, 0L, "SYSTEM");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(String tenantId, Long batchId, String stableReason,
                              Long operatorId, String operatorName) {
        String reason = "Reconciliation execution failed";
        Date now = new Date();
        if (batchMapper.markExecutionFailed(tenantId, batchId, reason, now) != 1 || actionLogMapper == null) return;
        PaymentReconciliationActionLog log = new PaymentReconciliationActionLog();
        log.setId(IdUtil.getSnowflakeNextId()); log.setTenantId(tenantId); log.setBatchId(batchId);
        log.setActionType("EXECUTION_FAILED"); log.setBeforeStatus("RECONCILING"); log.setAfterStatus("FAILED");
        log.setOperatorId(operatorId == null ? 0L : operatorId);
        log.setOperatorName(operatorName == null || operatorName.isBlank() ? "SYSTEM" : operatorName);
        log.setRemark(reason); log.setCreateTime(now);
        actionLogMapper.insert(log);
    }
}
