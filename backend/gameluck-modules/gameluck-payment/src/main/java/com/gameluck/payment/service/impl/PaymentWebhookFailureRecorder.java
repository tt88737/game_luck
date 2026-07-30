package com.gameluck.payment.service.impl;

import com.gameluck.payment.domain.PaymentWebhookEvent;
import com.gameluck.payment.mapper.PaymentWebhookEventMapper;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.util.Date;
import java.util.Set;

@Service
public class PaymentWebhookFailureRecorder {
    private static final Set<String> TERMINAL_STATUSES = Set.of("PROCESSED", "IGNORED");
    private final PaymentWebhookEventMapper eventMapper;
    private final TransactionTemplate transaction;
    private final Clock clock;

    @Autowired
    public PaymentWebhookFailureRecorder(PaymentWebhookEventMapper eventMapper,
                                         PlatformTransactionManager transactionManager, Clock clock) {
        this(eventMapper, new TransactionTemplate(transactionManager), clock);
    }

    PaymentWebhookFailureRecorder(PaymentWebhookEventMapper eventMapper, TransactionTemplate transaction, Clock clock) {
        this.eventMapper = eventMapper;
        this.transaction = transaction;
        this.clock = clock;
        this.transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public PaymentWebhookEvent record(String tenantId, Long eventId) {
        return transaction.execute(status -> {
            if (eventMapper.recordFailure(tenantId, eventId, "Business processing failed",
                Date.from(clock.instant())) == 1) return null;
            PaymentWebhookEvent current = eventMapper.selectByIdForUpdate(tenantId, eventId);
            if (current != null && TERMINAL_STATUSES.contains(current.getStatus())) return current;
            throw new IllegalStateException("Unable to persist payment webhook failure state");
        });
    }
}
