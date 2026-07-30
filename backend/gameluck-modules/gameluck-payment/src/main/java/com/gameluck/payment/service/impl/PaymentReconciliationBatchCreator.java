package com.gameluck.payment.service.impl;

import com.gameluck.payment.domain.PaymentReconciliationBatch;
import com.gameluck.payment.mapper.PaymentReconciliationBatchMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class PaymentReconciliationBatchCreator {
    private final PaymentReconciliationBatchMapper batchMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PaymentReconciliationBatch create(PaymentReconciliationBatch batch) {
        batchMapper.insert(batch);
        return batch;
    }
}
