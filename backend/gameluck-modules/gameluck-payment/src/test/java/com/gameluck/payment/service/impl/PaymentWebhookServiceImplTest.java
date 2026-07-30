package com.gameluck.payment.service.impl;

import com.gameluck.common.tenant.helper.TenantHelper;
import com.gameluck.payment.domain.PaymentSession;
import com.gameluck.payment.domain.PaymentWebhookEvent;
import com.gameluck.payment.domain.PurchaseOrder;
import com.gameluck.payment.domain.bo.PurchasePaymentCallbackBo;
import com.gameluck.payment.domain.vo.PaymentWebhookAckVo;
import com.gameluck.payment.enums.PaymentProviderEventType;
import com.gameluck.payment.mapper.PaymentSessionMapper;
import com.gameluck.payment.mapper.PaymentWebhookEventMapper;
import com.gameluck.payment.mapper.PurchaseOrderMapper;
import com.gameluck.payment.provider.PaymentProviderAdapter;
import com.gameluck.payment.provider.PaymentProviderRegistry;
import com.gameluck.payment.provider.PaymentWebhookEnvelope;
import com.gameluck.payment.provider.PaymentWebhookVerificationResult;
import com.gameluck.payment.provider.PaymentWebhookVerificationFailureKind;
import com.gameluck.payment.service.IPurchasePaymentEventService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@Tag("local")
class PaymentWebhookServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-07-27T00:00:00Z");

    @Test
    void validWebhookIsVerifiedBeforePersistenceAndReturnsProcessedAck() {
        Fixture f = fixture(PaymentProviderEventType.PAYMENT_SUCCEEDED);
        when(f.processor.processBusiness(anyLong())).thenReturn(
            new PaymentWebhookBusinessProcessor.WebhookProcessingOutcome("evt-1", "PROCESSED"));

        PaymentWebhookAckVo ack = receive(f);

        assertEquals("evt-1", ack.providerEventId());
        assertEquals("PROCESSED", ack.status());
        verify(f.adapter).verifyWebhook("100", "sig", f.raw, NOW);
        verify(f.adapter).parseWebhook(f.raw);
        verify(f.events).insert(any(PaymentWebhookEvent.class));
        verify(f.processor).processBusiness(anyLong());
    }

    @Test
    void invalidSignatureReturns401AndCreatesNoEvent() {
        Fixture f = fixture(PaymentProviderEventType.PAYMENT_SUCCEEDED);
        when(f.adapter.verifyWebhook(anyString(), anyString(), any(), any()))
            .thenReturn(PaymentWebhookVerificationResult.failure("signature mismatch"));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> receive(f));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        verify(f.adapter, never()).parseWebhook(any());
        verifyNoInteractions(f.events, f.processor);
    }

    @Test
    void nonStalePolicyFailureNeverEntersCryptographicReplayFallback() {
        Fixture f = fixture(PaymentProviderEventType.PAYMENT_SUCCEEDED);
        when(f.adapter.verifyWebhook("100", "sig", f.raw, NOW)).thenReturn(
            PaymentWebhookVerificationResult.failure(
                PaymentWebhookVerificationFailureKind.POLICY_REJECTED, "provider policy"));
        when(f.adapter.verifyWebhookCryptographicSignature("100", "sig", f.raw))
            .thenReturn(PaymentWebhookVerificationResult.success());

        assertEquals(HttpStatus.UNAUTHORIZED,
            assertThrows(ResponseStatusException.class, () -> receive(f)).getStatusCode());

        verify(f.adapter, never()).verifyWebhookCryptographicSignature(anyString(), anyString(), any());
        verify(f.adapter, never()).parseWebhook(any());
        verifyNoInteractions(f.events, f.processor);
    }

    @Test
    void staleExactPersistedEventUsesCryptographicReplayVerification() {
        Fixture f = fixture(PaymentProviderEventType.PAYMENT_SUCCEEDED);
        when(f.adapter.verifyWebhook("old", "sig", f.raw, NOW))
            .thenReturn(PaymentWebhookVerificationResult.failure(
                PaymentWebhookVerificationFailureKind.STALE_TIMESTAMP, "timestamp outside tolerance"));
        when(f.adapter.verifyWebhookCryptographicSignature("old", "sig", f.raw))
            .thenReturn(PaymentWebhookVerificationResult.success());
        when(f.events.selectByProviderEventId("000000", "SIMULATED", "evt-1"))
            .thenReturn(event("PROCESSED"));

        PaymentWebhookAckVo ack = receive(f, "old", "sig");

        assertEquals("PROCESSED", ack.status());
        verify(f.events, never()).insert(any(PaymentWebhookEvent.class));
        verifyNoInteractions(f.processor);
    }

    @Test
    void staleNewOrTamperedEventReturns401WithoutPersistence() {
        Fixture fresh = fixture(PaymentProviderEventType.PAYMENT_SUCCEEDED);
        when(fresh.adapter.verifyWebhook("old", "sig", fresh.raw, NOW))
            .thenReturn(PaymentWebhookVerificationResult.failure(
                PaymentWebhookVerificationFailureKind.STALE_TIMESTAMP, "stale"));
        when(fresh.adapter.verifyWebhookCryptographicSignature("old", "sig", fresh.raw))
            .thenReturn(PaymentWebhookVerificationResult.success());
        assertEquals(HttpStatus.UNAUTHORIZED,
            assertThrows(ResponseStatusException.class, () -> receive(fresh, "old", "sig")).getStatusCode());
        verify(fresh.events, never()).insert(any(PaymentWebhookEvent.class));

        Fixture tampered = fixture(PaymentProviderEventType.PAYMENT_SUCCEEDED);
        when(tampered.adapter.verifyWebhook("old", "sig", tampered.raw, NOW))
            .thenReturn(PaymentWebhookVerificationResult.failure(
                PaymentWebhookVerificationFailureKind.STALE_TIMESTAMP, "stale"));
        when(tampered.adapter.verifyWebhookCryptographicSignature("old", "sig", tampered.raw))
            .thenReturn(PaymentWebhookVerificationResult.failure("signature mismatch"));
        assertEquals(HttpStatus.UNAUTHORIZED,
            assertThrows(ResponseStatusException.class, () -> receive(tampered, "old", "sig")).getStatusCode());
        verify(tampered.adapter, never()).parseWebhook(any());
        verifyNoInteractions(tampered.events, tampered.processor);

        Fixture conflictingSignature = fixture(PaymentProviderEventType.PAYMENT_SUCCEEDED);
        when(conflictingSignature.adapter.verifyWebhook("old", "other", conflictingSignature.raw, NOW))
            .thenReturn(PaymentWebhookVerificationResult.failure(
                PaymentWebhookVerificationFailureKind.STALE_TIMESTAMP, "stale"));
        when(conflictingSignature.adapter.verifyWebhookCryptographicSignature(
            "old", "other", conflictingSignature.raw)).thenReturn(PaymentWebhookVerificationResult.success());
        when(conflictingSignature.events.selectByProviderEventId("000000", "SIMULATED", "evt-1"))
            .thenReturn(event("PROCESSED"));
        assertEquals(HttpStatus.UNAUTHORIZED,
            assertThrows(ResponseStatusException.class,
                () -> receive(conflictingSignature, "old", "other")).getStatusCode());
        verify(conflictingSignature.events, never()).insert(any(PaymentWebhookEvent.class));
    }

    @Test
    void sameRawEventWithNewTimestampAndValidSignatureReplaysStableAck() {
        Fixture f = fixture(PaymentProviderEventType.PAYMENT_SUCCEEDED);
        when(f.events.insert(any(PaymentWebhookEvent.class))).thenThrow(new DuplicateKeyException("race"));
        PaymentWebhookEvent winner = event("PROCESSED");
        when(f.events.selectByProviderEventIdForUpdate("000000", "SIMULATED", "evt-1")).thenReturn(winner);
        String newSignature = "b".repeat(64);
        when(f.adapter.verifyWebhook("101", newSignature, f.raw, NOW))
            .thenReturn(PaymentWebhookVerificationResult.success());

        PaymentWebhookAckVo ack = receive(f, "101", newSignature);

        assertEquals("PROCESSED", ack.status());
        verify(f.events).selectByProviderEventIdForUpdate("000000", "SIMULATED", "evt-1");
        verifyNoInteractions(f.processor);
    }

    @Test
    void exactFailedEventReentersLockedBusinessProcessorAndCanRecover() {
        Fixture f = fixture(PaymentProviderEventType.PAYMENT_SUCCEEDED);
        when(f.events.insert(any(PaymentWebhookEvent.class))).thenThrow(new DuplicateKeyException("replay"));
        when(f.events.selectByProviderEventIdForUpdate("000000", "SIMULATED", "evt-1"))
            .thenReturn(event("FAILED"));
        when(f.processor.processBusiness(7L)).thenReturn(
            new PaymentWebhookBusinessProcessor.WebhookProcessingOutcome("evt-1", "PROCESSED"));

        PaymentWebhookAckVo ack = receive(f);

        assertEquals("PROCESSED", ack.status());
        verify(f.processor).processBusiness(7L);
    }

    @Test
    void sameProviderEventIdWithDifferentRawPayloadIsRejected() {
        Fixture f = fixture(PaymentProviderEventType.PAYMENT_SUCCEEDED);
        when(f.events.insert(any(PaymentWebhookEvent.class))).thenThrow(new DuplicateKeyException("race"));
        PaymentWebhookEvent winner = event("PROCESSED");
        winner.setRawBody("{\"event\":2}");
        when(f.events.selectByProviderEventIdForUpdate("000000", "SIMULATED", "evt-1")).thenReturn(winner);

        assertThrows(RuntimeException.class, () -> receive(f));

        verifyNoInteractions(f.processor);
    }

    @Test
    void businessFailureRollsBackStageBThenPersistsOneFailedEventInRequiresNew() {
        Fixture f = fixture(PaymentProviderEventType.PAYMENT_SUCCEEDED);
        when(f.processor.processBusiness(anyLong())).thenThrow(new IllegalStateException("wallet secret=bad raw={x}"));

        assertThrows(IllegalStateException.class, () -> receive(f));

        verify(f.events, times(1)).recordFailure(eq("000000"), anyLong(), eq("Business processing failed"), any(Date.class));
        assertEquals(2, f.transactionManager.beginCount);
        assertEquals(TransactionDefinition.PROPAGATION_REQUIRES_NEW, f.transactionManager.lastPropagation);
    }

    @Test
    void failureUpdateLosingToConcurrentTerminalStateReturnsStableAckWithoutOverwrite() {
        Fixture f = fixture(PaymentProviderEventType.PAYMENT_SUCCEEDED);
        when(f.processor.processBusiness(anyLong())).thenThrow(new IllegalStateException("business failed"));
        when(f.events.recordFailure(eq("000000"), anyLong(), eq("Business processing failed"), any(Date.class)))
            .thenReturn(0);
        when(f.events.selectByIdForUpdate(eq("000000"), anyLong())).thenReturn(event("PROCESSED"));

        PaymentWebhookAckVo ack = receive(f);

        assertEquals("evt-1", ack.providerEventId());
        assertEquals("PROCESSED", ack.status());
        verify(f.events).selectByIdForUpdate(eq("000000"), anyLong());
    }

    @Test
    void failureUpdateWithNonTerminalCurrentStateRaisesPersistenceError() {
        Fixture f = fixture(PaymentProviderEventType.PAYMENT_SUCCEEDED);
        when(f.processor.processBusiness(anyLong())).thenThrow(new IllegalStateException("business failed"));
        when(f.events.recordFailure(eq("000000"), anyLong(), eq("Business processing failed"), any(Date.class)))
            .thenReturn(0);
        when(f.events.selectByIdForUpdate(eq("000000"), anyLong())).thenReturn(event("FAILED"));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> receive(f));

        assertEquals("Unable to persist payment webhook failure state", exception.getMessage());
        verify(f.events).selectByIdForUpdate(eq("000000"), anyLong());
    }

    @Test
    void processorMapsEveryProviderEventAndStrictlyChecksSnapshot() {
        Object[][] mappings = {
            {PaymentProviderEventType.PAYMENT_SUCCEEDED, "PAY_SUCCESS", "SUCCEEDED"},
            {PaymentProviderEventType.PAYMENT_FAILED, "PAY_FAILED", "FAILED"},
            {PaymentProviderEventType.PAYMENT_CANCELLED, "CANCELLED", "CANCELLED"},
            {PaymentProviderEventType.REFUND_SUCCEEDED, "REFUNDED", "SUCCEEDED"},
            {PaymentProviderEventType.CHARGEBACK_CREATED, "CHARGEBACK", "SUCCEEDED"}
        };
        for (Object[] mapping : mappings) {
            ProcessorFixture f = processorFixture((PaymentProviderEventType) mapping[0]);
            process(f.processor, 7L);
            ArgumentCaptor<PurchasePaymentCallbackBo> callback = ArgumentCaptor.forClass(PurchasePaymentCallbackBo.class);
            verify(f.paymentEvents).applyEvent(callback.capture());
            assertEquals(mapping[1], callback.getValue().getEventType().name());
            assertEquals("SIMULATED:evt-1", callback.getValue().getEventKey());
            assertEquals("SIM-1", callback.getValue().getProviderOrderNo());
            verify(f.sessions).updateStatusGuarded(eq("000000"), eq(9L), eq((String) mapping[2]), any(), any(Date.class));
        }

        ProcessorFixture mismatch = processorFixture(PaymentProviderEventType.PAYMENT_SUCCEEDED);
        mismatch.session.setPayAmount(new BigDecimal("99.00"));
        assertThrows(RuntimeException.class, () -> process(mismatch.processor, 7L));
        verifyNoInteractions(mismatch.paymentEvents);
    }

    @Test
    void contradictoryLateTerminalEventIsIgnoredWithoutBusinessMutation() {
        ProcessorFixture f = processorFixture(PaymentProviderEventType.PAYMENT_FAILED);
        f.session.setStatus("SUCCEEDED");

        PaymentWebhookBusinessProcessor.WebhookProcessingOutcome outcome = process(f.processor, 7L);

        assertEquals("IGNORED", outcome.status());
        verifyNoInteractions(f.paymentEvents);
        verify(f.events).completeProcessing(eq("000000"), eq(7L), eq("RECEIVED"), eq("IGNORED"), any(Date.class));
        verify(f.sessions, never()).updateStatusGuarded(anyString(), anyLong(), anyString(), any(), any());
    }

    @Test
    void currencyMismatchFailsBeforePurchaseBusinessService() {
        ProcessorFixture f = processorFixture(PaymentProviderEventType.PAYMENT_SUCCEEDED);
        f.session.setPayCurrencyCode("EUR");

        assertThrows(RuntimeException.class, () -> process(f.processor, 7L));

        verifyNoInteractions(f.paymentEvents);
        verify(f.sessions, never()).updateStatusGuarded(anyString(), anyLong(), anyString(), anyString(), any());
    }

    @Test
    void successAtExactExpiryIsIgnoredAndAtomicallyExpiresSession() {
        ProcessorFixture f = processorFixture(PaymentProviderEventType.PAYMENT_SUCCEEDED);
        f.session.setExpireTime(Date.from(NOW));

        PaymentWebhookBusinessProcessor.WebhookProcessingOutcome outcome = process(f.processor, 7L);

        assertEquals("IGNORED", outcome.status());
        verifyNoInteractions(f.paymentEvents);
        verify(f.sessions).updateStatusGuarded(eq("000000"), eq(9L), eq("EXPIRED"), eq("PENDING"), any(Date.class));
        verify(f.events).completeProcessing(eq("000000"), eq(7L), eq("RECEIVED"), eq("IGNORED"), any(Date.class));
    }

    @Test
    void transactionAnnotationsProveBusinessRunsThroughSeparateTransactionalBean() throws Exception {
        Transactional annotation = PaymentWebhookBusinessProcessor.class
            .getDeclaredMethod("processBusiness", Long.class).getAnnotation(Transactional.class);
        assertNotNull(annotation);
        assertEquals(Propagation.REQUIRED, annotation.propagation());
        assertEquals(PaymentWebhookBusinessProcessor.class,
            PaymentWebhookServiceImpl.class.getDeclaredField("businessProcessor").getType());
    }

    @Test
    void realSpringProxyRollsBackBusinessAndCommitsFailureInIndependentTransaction() {
        DurableInMemoryTransactionManager transactions = new DurableInMemoryTransactionManager();
        PaymentWebhookEventMapper events = mock(PaymentWebhookEventMapper.class);
        PaymentSessionMapper sessions = mock(PaymentSessionMapper.class);
        PurchaseOrderMapper orders = mock(PurchaseOrderMapper.class);
        IPurchasePaymentEventService paymentEvents = mock(IPurchasePaymentEventService.class);
        PaymentProviderRegistry registry = mock(PaymentProviderRegistry.class);
        PaymentProviderAdapter adapter = mock(PaymentProviderAdapter.class);
        AtomicReference<PaymentWebhookEvent> storedEvent = new AtomicReference<>();
        AtomicBoolean downstreamCommitted = new AtomicBoolean();
        AtomicBoolean failureCommitted = new AtomicBoolean();
        AtomicInteger remainingFailures = new AtomicInteger(2);
        MutableClock clock = new MutableClock(NOW, ZoneOffset.UTC);
        Instant firstFailureTime = NOW.plusSeconds(1);
        Instant secondFailureTime = NOW.plusSeconds(2);
        Instant successTime = NOW.plusSeconds(3);
        Instant terminalReplayTime = NOW.plusSeconds(4);
        byte[] raw = "{\"event\":1}".getBytes(StandardCharsets.UTF_8);
        PaymentSession session = session();
        PurchaseOrder order = order();

        when(registry.resolve("SIMULATED")).thenReturn(adapter);
        when(adapter.verifyWebhook(eq("100"), eq("sig"), eq(raw), any(Instant.class)))
            .thenReturn(PaymentWebhookVerificationResult.success());
        when(adapter.parseWebhook(raw)).thenReturn(envelope(PaymentProviderEventType.PAYMENT_SUCCEEDED));
        when(sessions.selectByProviderSessionNo("000000", "SIMULATED", "SIM-1")).thenReturn(session);
        when(events.insert(any(PaymentWebhookEvent.class))).thenAnswer(invocation -> {
            if (storedEvent.get() != null) {
                throw new DuplicateKeyException("exact replay");
            }
            PaymentWebhookEvent event = invocation.getArgument(0);
            storedEvent.set(event);
            return 1;
        });
        when(events.selectByProviderEventIdForUpdate("000000", "SIMULATED", "evt-1"))
            .thenAnswer(invocation -> storedEvent.get());
        when(events.selectByIdForUpdate(eq("000000"), anyLong())).thenAnswer(invocation -> storedEvent.get());
        when(sessions.selectByProviderSessionNoForUpdate("000000", "SIMULATED", "SIM-1")).thenReturn(session);
        when(orders.selectByOrderNoForUpdate("000000", "PO-1")).thenReturn(order);
        when(paymentEvents.applyEvent(any())).thenAnswer(invocation -> {
            if (remainingFailures.getAndDecrement() <= 0) {
                return order;
            }
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { downstreamCommitted.set(true); }
            });
            throw new IllegalStateException("forced wallet failure secret=must-not-leak");
        });
        when(events.recordFailure(eq("000000"), anyLong(), eq("Business processing failed"), any(Date.class)))
            .thenAnswer(invocation -> {
                Date failureTime = invocation.getArgument(3);
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override public void afterCommit() {
                        storedEvent.get().setStatus("FAILED");
                        storedEvent.get().setFailureReason("Business processing failed");
                        storedEvent.get().setProcessingCount(storedEvent.get().getProcessingCount() + 1);
                        storedEvent.get().setLastProcessingTime(failureTime);
                        failureCommitted.set(true);
                    }
                });
                return 1;
            });
        when(sessions.updateStatusGuarded(anyString(), anyLong(), anyString(), anyString(), any(Date.class)))
            .thenReturn(1);
        when(events.completeProcessing(eq("000000"), anyLong(), eq("FAILED"), eq("PROCESSED"), any(Date.class)))
            .thenAnswer(invocation -> {
                Date completionTime = invocation.getArgument(4);
                storedEvent.get().setStatus("PROCESSED");
                storedEvent.get().setFailureReason(null);
                storedEvent.get().setProcessingCount(storedEvent.get().getProcessingCount() + 1);
                storedEvent.get().setLastProcessingTime(completionTime);
                return 1;
            });

        PaymentWebhookBusinessProcessor target = new PaymentWebhookBusinessProcessor(
            events, sessions, orders, paymentEvents, registry, clock);
        ProxyFactory proxyFactory = new ProxyFactory(target);
        proxyFactory.addAdvice(new TransactionInterceptor(
            transactions, new AnnotationTransactionAttributeSource()));
        PaymentWebhookBusinessProcessor proxiedProcessor =
            (PaymentWebhookBusinessProcessor) proxyFactory.getProxy();
        PaymentWebhookServiceImpl service = new PaymentWebhookServiceImpl(
            registry, events, sessions, proxiedProcessor, transactions, clock);

        try (MockedStatic<TenantHelper> tenant = org.mockito.Mockito.mockStatic(TenantHelper.class)) {
            tenant.when(TenantHelper::getTenantId).thenReturn("000000");
            tenant.when(() -> TenantHelper.dynamic(eq("000000"), any(java.util.function.Supplier.class)))
                .thenAnswer(invocation -> ((java.util.function.Supplier<?>) invocation.getArgument(1)).get());
            tenant.when(() -> TenantHelper.dynamic(eq("000000"), any(Runnable.class)))
                .thenAnswer(invocation -> {
                    ((Runnable) invocation.getArgument(1)).run();
                    return null;
                });
            clock.set(firstFailureTime);
            assertThrows(IllegalStateException.class,
                () -> service.receive("SIMULATED", "100", "sig", raw));
            assertEquals(1, storedEvent.get().getProcessingCount());
            assertEquals("FAILED", storedEvent.get().getStatus());
            assertEquals("Business processing failed", storedEvent.get().getFailureReason());
            assertEquals(Date.from(firstFailureTime), storedEvent.get().getLastProcessingTime());
            clock.set(secondFailureTime);
            assertThrows(IllegalStateException.class,
                () -> service.receive("SIMULATED", "100", "sig", raw));
            assertEquals(2, storedEvent.get().getProcessingCount());
            assertEquals("FAILED", storedEvent.get().getStatus());
            assertEquals("Business processing failed", storedEvent.get().getFailureReason());
            assertEquals(Date.from(secondFailureTime), storedEvent.get().getLastProcessingTime());
            clock.set(successTime);
            PaymentWebhookAckVo recovered = service.receive("SIMULATED", "100", "sig", raw);
            assertEquals("PROCESSED", recovered.status());
            assertEquals(3, storedEvent.get().getProcessingCount());
            assertEquals(null, storedEvent.get().getFailureReason());
            assertEquals(Date.from(successTime), storedEvent.get().getLastProcessingTime());
            clock.set(terminalReplayTime);
            PaymentWebhookAckVo replayed = service.receive("SIMULATED", "100", "sig", raw);
            assertEquals("PROCESSED", replayed.status());
            assertEquals(3, storedEvent.get().getProcessingCount());
            assertEquals(Date.from(successTime), storedEvent.get().getLastProcessingTime());
        }

        assertEquals(9, transactions.beginCount);
        assertEquals(7, transactions.commitCount);
        assertEquals(2, transactions.rollbackCount);
        assertEquals("PROCESSED", storedEvent.get().getStatus());
        assertEquals("{\"event\":1}", storedEvent.get().getRawBody());
        assertEquals(3, storedEvent.get().getProcessingCount());
        assertEquals(null, storedEvent.get().getFailureReason());
        assertEquals(Date.from(successTime), storedEvent.get().getLastProcessingTime());
        assertEquals(false, downstreamCommitted.get());
        assertEquals(true, failureCommitted.get());
        verify(events, times(4)).insert(any(PaymentWebhookEvent.class));
        verify(events, times(2)).recordFailure(eq("000000"), anyLong(),
            eq("Business processing failed"), any(Date.class));
        verify(paymentEvents, times(3)).applyEvent(any());
    }

    private PaymentWebhookAckVo receive(Fixture f) {
        return receive(f, "100", "sig");
    }

    private PaymentWebhookAckVo receive(Fixture f, String timestamp, String signature) {
        try (MockedStatic<TenantHelper> tenant = org.mockito.Mockito.mockStatic(TenantHelper.class)) {
            tenant.when(() -> TenantHelper.dynamic(eq("000000"), any(java.util.function.Supplier.class)))
                .thenAnswer(invocation -> ((java.util.function.Supplier<?>) invocation.getArgument(1)).get());
            tenant.when(() -> TenantHelper.dynamic(eq("000000"), any(Runnable.class)))
                .thenAnswer(invocation -> {
                    ((Runnable) invocation.getArgument(1)).run();
                    return null;
                });
            return f.service.receive("SIMULATED", timestamp, signature, f.raw);
        }
    }

    private <T> T process(PaymentWebhookBusinessProcessor processor, Long eventId) {
        try (MockedStatic<TenantHelper> tenant = org.mockito.Mockito.mockStatic(TenantHelper.class)) {
            tenant.when(TenantHelper::getTenantId).thenReturn("000000");
            @SuppressWarnings("unchecked")
            T result = (T) processor.processBusiness(eventId);
            return result;
        }
    }

    private Fixture fixture(PaymentProviderEventType type) {
        PaymentWebhookEventMapper events = mock(PaymentWebhookEventMapper.class);
        PaymentSessionMapper sessions = mock(PaymentSessionMapper.class);
        PaymentProviderRegistry registry = mock(PaymentProviderRegistry.class);
        PaymentProviderAdapter adapter = mock(PaymentProviderAdapter.class);
        PaymentWebhookBusinessProcessor processor = mock(PaymentWebhookBusinessProcessor.class);
        RecordingTransactionManager transactionManager = new RecordingTransactionManager();
        byte[] raw = "{\"event\":1}".getBytes(StandardCharsets.UTF_8);
        when(registry.resolve("SIMULATED")).thenReturn(adapter);
        when(adapter.verifyWebhook("100", "sig", raw, NOW)).thenReturn(PaymentWebhookVerificationResult.success());
        when(adapter.parseWebhook(raw)).thenReturn(envelope(type));
        when(sessions.selectByProviderSessionNo("000000", "SIMULATED", "SIM-1")).thenReturn(session());
        when(events.insert(any(PaymentWebhookEvent.class))).thenAnswer(invocation -> 1);
        when(events.recordFailure(anyString(), anyLong(), anyString(), any(Date.class))).thenReturn(1);
        PaymentWebhookServiceImpl service = new PaymentWebhookServiceImpl(
            registry, events, sessions, processor, transactionManager, Clock.fixed(NOW, ZoneOffset.UTC));
        return new Fixture(events, adapter, processor, transactionManager, raw, service);
    }

    private ProcessorFixture processorFixture(PaymentProviderEventType type) {
        PaymentWebhookEventMapper events = mock(PaymentWebhookEventMapper.class);
        PaymentSessionMapper sessions = mock(PaymentSessionMapper.class);
        PurchaseOrderMapper orders = mock(PurchaseOrderMapper.class);
        IPurchasePaymentEventService paymentEvents = mock(IPurchasePaymentEventService.class);
        PaymentProviderRegistry registry = mock(PaymentProviderRegistry.class);
        PaymentProviderAdapter adapter = mock(PaymentProviderAdapter.class);
        PaymentWebhookEvent event = event("RECEIVED");
        PaymentSession session = session();
        event.setEventType(type.name());
        if (type == PaymentProviderEventType.REFUND_SUCCEEDED
            || type == PaymentProviderEventType.CHARGEBACK_CREATED) {
            session.setStatus("SUCCEEDED");
        }
        PurchaseOrder order = order();
        when(events.selectByIdForUpdate("000000", 7L)).thenReturn(event);
        when(sessions.selectByProviderSessionNoForUpdate("000000", "SIMULATED", "SIM-1")).thenReturn(session);
        when(orders.selectByOrderNoForUpdate("000000", "PO-1")).thenReturn(order);
        when(registry.resolve("SIMULATED")).thenReturn(adapter);
        when(adapter.parseWebhook(any())).thenReturn(envelope(type));
        when(paymentEvents.applyEvent(any())).thenReturn(order);
        when(sessions.updateStatusGuarded(anyString(), anyLong(), anyString(), any(), any())).thenReturn(1);
        when(events.completeProcessing(anyString(), anyLong(), anyString(), anyString(), any())).thenReturn(1);
        PaymentWebhookBusinessProcessor processor = new PaymentWebhookBusinessProcessor(
            events, sessions, orders, paymentEvents, registry, Clock.fixed(NOW, ZoneOffset.UTC));
        try (MockedStatic<TenantHelper> tenant = org.mockito.Mockito.mockStatic(TenantHelper.class)) {
            tenant.when(TenantHelper::getTenantId).thenReturn("000000");
            // Prime no state: the actual invocation happens after this scope in most tests.
        }
        return new ProcessorFixture(processor, events, sessions, paymentEvents, session);
    }

    private PaymentWebhookEnvelope envelope(PaymentProviderEventType type) {
        return new PaymentWebhookEnvelope(0L, "evt-1", type, "SIM-1", "PO-1", "USD",
            new BigDecimal("12.340000"), NOW.minusSeconds(1));
    }

    private PaymentWebhookEvent event(String status) {
        PaymentWebhookEvent event = new PaymentWebhookEvent();
        event.setId(7L); event.setTenantId("000000"); event.setProviderCode("SIMULATED");
        event.setProviderEventId("evt-1"); event.setEventType("PAYMENT_SUCCEEDED");
        event.setProviderSessionNo("SIM-1"); event.setSessionNo("PS-1");
        event.setPurchaseOrderNo("PO-1"); event.setRawBody("{\"event\":1}");
        event.setSignatureDigest("a543997d84f12798350c09bdef2cdb171bf41ed3e4a5f808af2feb0c56263009");
        event.setStatus(status); event.setProcessingCount(0);
        return event;
    }

    private PaymentSession session() {
        PaymentSession session = new PaymentSession();
        session.setId(9L); session.setTenantId("000000"); session.setSessionNo("PS-1");
        session.setPurchaseOrderNo("PO-1"); session.setProviderCode("SIMULATED");
        session.setProviderSessionNo("SIM-1"); session.setPayCurrencyCode("USD");
        session.setPayAmount(new BigDecimal("12.340000")); session.setStatus("PENDING");
        session.setExpireTime(Date.from(NOW.plusSeconds(60)));
        return session;
    }

    private PurchaseOrder order() {
        PurchaseOrder order = new PurchaseOrder();
        order.setId(11L); order.setTenantId("000000"); order.setPurchaseOrderNo("PO-1");
        order.setPayCurrencyCode("USD"); order.setPayAmount(new BigDecimal("12.340000"));
        order.setStatus("PENDING"); order.setProviderCode("SIMULATED");
        order.setProviderOrderNo("SIM-1"); order.setPaymentSessionNo("PS-1");
        return order;
    }

    private record Fixture(PaymentWebhookEventMapper events, PaymentProviderAdapter adapter,
                           PaymentWebhookBusinessProcessor processor, RecordingTransactionManager transactionManager,
                           byte[] raw, PaymentWebhookServiceImpl service) { }

    private record ProcessorFixture(PaymentWebhookBusinessProcessor processor, PaymentWebhookEventMapper events,
                                    PaymentSessionMapper sessions, IPurchasePaymentEventService paymentEvents,
                                    PaymentSession session) { }

    private static class RecordingTransactionManager implements PlatformTransactionManager {
        int beginCount;
        int lastPropagation;
        @Override public TransactionStatus getTransaction(TransactionDefinition definition) {
            beginCount++;
            lastPropagation = definition.getPropagationBehavior();
            return new SimpleTransactionStatus();
        }
        @Override public void commit(TransactionStatus status) { }
        @Override public void rollback(TransactionStatus status) { }
    }

    private static class DurableInMemoryTransactionManager extends AbstractPlatformTransactionManager {
        int beginCount;
        int commitCount;
        int rollbackCount;

        @Override protected Object doGetTransaction() { return new Object(); }
        @Override protected void doBegin(Object transaction, TransactionDefinition definition) { beginCount++; }
        @Override protected void doCommit(DefaultTransactionStatus status) { commitCount++; }
        @Override protected void doRollback(DefaultTransactionStatus status) { rollbackCount++; }
    }

    private static class MutableClock extends Clock {
        private final AtomicReference<Instant> instant;
        private final ZoneId zone;

        private MutableClock(Instant instant, ZoneId zone) {
            this.instant = new AtomicReference<>(instant);
            this.zone = zone;
        }

        private void set(Instant value) {
            instant.set(value);
        }

        @Override public ZoneId getZone() { return zone; }
        @Override public Clock withZone(ZoneId value) { return new MutableClock(instant(), value); }
        @Override public Instant instant() { return instant.get(); }
    }
}
