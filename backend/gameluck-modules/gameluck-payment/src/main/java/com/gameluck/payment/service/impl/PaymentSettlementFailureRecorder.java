package com.gameluck.payment.service.impl;

import cn.hutool.core.util.IdUtil;
import com.gameluck.payment.domain.PaymentSettlementActionLog;
import com.gameluck.payment.mapper.PaymentSettlementActionLogMapper;
import com.gameluck.payment.mapper.PaymentSettlementBatchMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Component
@RequiredArgsConstructor
public class PaymentSettlementFailureRecorder {
    private final PaymentSettlementBatchMapper batchMapper;
    private final PaymentSettlementActionLogMapper actionLogMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(String tenantId, Long batchId, String stableReason,
                              Long operatorId, String operatorName) {
        Date now = new Date();
        if (batchMapper.markFailed(tenantId, batchId, stableReason, now) != 1) return;
        PaymentSettlementActionLog log = new PaymentSettlementActionLog();
        log.setId(IdUtil.getSnowflakeNextId()); log.setTenantId(tenantId); log.setBatchId(batchId);
        log.setActionType("CALCULATION_FAILED"); log.setBeforeStatus("CALCULATING");
        log.setAfterStatus("FAILED"); log.setOperatorId(operatorId == null ? 0L : operatorId);
        log.setOperatorName(operatorName == null || operatorName.isBlank() ? "SYSTEM" : operatorName);
        log.setRemark(stableReason); log.setCreateTime(now);
        actionLogMapper.insert(log);
    }
}
