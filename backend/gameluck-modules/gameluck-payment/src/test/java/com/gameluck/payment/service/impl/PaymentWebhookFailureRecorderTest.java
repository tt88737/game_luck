package com.gameluck.payment.service.impl;

import com.gameluck.payment.domain.PaymentWebhookEvent;
import com.gameluck.payment.mapper.PaymentWebhookEventMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PaymentWebhookFailureRecorderTest {
    @Test @Tag("local")
    void marksProductionConstructorForSpringInjection() throws Exception {
        assertTrue(PaymentWebhookFailureRecorder.class
            .getConstructor(PaymentWebhookEventMapper.class, PlatformTransactionManager.class, Clock.class)
            .isAnnotationPresent(Autowired.class));
    }

    @Test @Tag("local")
    void recordsOneAttemptInRequiresNewAndReturnsNoTerminal() {
        Fixture f = fixture();
        when(f.mapper.recordFailure(eq("000001"), eq(7L), anyString(), any(Date.class))).thenReturn(1);
        assertNull(f.recorder.record("000001", 7L));
        verify(f.tx).setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Test @Tag("local")
    void zeroRowReturnsOnlyConcurrentTerminalState() {
        Fixture f = fixture();
        when(f.mapper.recordFailure(anyString(), anyLong(), anyString(), any(Date.class))).thenReturn(0);
        when(f.mapper.selectByIdForUpdate("000001", 7L)).thenReturn(event("IGNORED"));
        assertEquals("IGNORED", f.recorder.record("000001", 7L).getStatus());
    }

    @Test @Tag("local")
    void zeroRowAlsoReturnsConcurrentProcessedState() {
        Fixture f = fixture();
        when(f.mapper.recordFailure(anyString(), anyLong(), anyString(), any(Date.class))).thenReturn(0);
        when(f.mapper.selectByIdForUpdate("000001", 7L)).thenReturn(event("PROCESSED"));
        assertEquals("PROCESSED", f.recorder.record("000001", 7L).getStatus());
    }

    @Test @Tag("local")
    void zeroRowNonTerminalOrMissingRaisesPersistenceFailure() {
        Fixture f = fixture();
        when(f.mapper.recordFailure(anyString(), anyLong(), anyString(), any(Date.class))).thenReturn(0);
        when(f.mapper.selectByIdForUpdate("000001", 7L)).thenReturn(event("FAILED"), (PaymentWebhookEvent) null);
        assertEquals("Unable to persist payment webhook failure state",
            assertThrows(IllegalStateException.class, () -> f.recorder.record("000001", 7L)).getMessage());
        assertEquals("Unable to persist payment webhook failure state",
            assertThrows(IllegalStateException.class, () -> f.recorder.record("000001", 7L)).getMessage());
    }

    private Fixture fixture() {
        PaymentWebhookEventMapper mapper = mock(PaymentWebhookEventMapper.class);
        TransactionTemplate tx = mock(TransactionTemplate.class);
        when(tx.execute(any())).thenAnswer(inv -> ((org.springframework.transaction.support.TransactionCallback<?>) inv.getArgument(0)).doInTransaction(mock(org.springframework.transaction.TransactionStatus.class)));
        PaymentWebhookFailureRecorder recorder = new PaymentWebhookFailureRecorder(mapper, tx,
            Clock.fixed(Instant.parse("2026-07-27T00:00:00Z"), ZoneOffset.UTC));
        return new Fixture(mapper, tx, recorder);
    }

    private PaymentWebhookEvent event(String status) {
        PaymentWebhookEvent event = new PaymentWebhookEvent(); event.setStatus(status); event.setProviderEventId("evt-1"); return event;
    }

    private record Fixture(PaymentWebhookEventMapper mapper, TransactionTemplate tx, PaymentWebhookFailureRecorder recorder) {}
}
