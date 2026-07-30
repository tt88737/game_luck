package com.gameluck.payment.service.impl;

import cn.hutool.core.util.IdUtil;
import com.gameluck.payment.domain.PaymentSettlementActionLog;
import com.gameluck.payment.mapper.PaymentSettlementActionLogMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
public class PaymentSettlementCloseRejectionRecorder {
    private final PaymentSettlementActionLogMapper logMapper;

    public PaymentSettlementCloseRejectionRecorder(PaymentSettlementActionLogMapper logMapper) {
        this.logMapper = logMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String tenantId, Long batchId, long operatorId, String operatorName,
        String evidenceJson, Date now) {
        PaymentSettlementActionLog log = new PaymentSettlementActionLog();
        log.setId(IdUtil.getSnowflakeNextId()); log.setTenantId(tenantId); log.setBatchId(batchId);
        log.setActionType("CLOSE_REJECTED"); log.setBeforeStatus("CALCULATED");
        log.setAfterStatus("CALCULATED"); log.setOperatorId(operatorId); log.setOperatorName(operatorName);
        log.setEvidenceSnapshotJson(evidenceJson); log.setCreateTime(now); logMapper.insert(log);
    }
}
