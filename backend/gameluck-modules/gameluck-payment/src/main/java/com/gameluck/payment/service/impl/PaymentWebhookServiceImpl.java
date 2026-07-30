package com.gameluck.payment.service.impl;

import cn.hutool.core.util.IdUtil;
import com.gameluck.common.core.exception.ServiceException;
import com.gameluck.common.tenant.helper.TenantHelper;
import com.gameluck.payment.domain.PaymentWebhookEvent;
import com.gameluck.payment.domain.PaymentSession;
import com.gameluck.payment.domain.vo.PaymentWebhookAckVo;
import com.gameluck.payment.enums.PaymentWebhookEventStatus;
import com.gameluck.payment.mapper.PaymentWebhookEventMapper;
import com.gameluck.payment.mapper.PaymentSessionMapper;
import com.gameluck.payment.provider.PaymentProviderAdapter;
import com.gameluck.payment.provider.PaymentProviderRegistry;
import com.gameluck.payment.provider.PaymentWebhookEnvelope;
import com.gameluck.payment.provider.PaymentWebhookVerificationResult;
import com.gameluck.payment.provider.PaymentWebhookVerificationFailureKind;
import com.gameluck.payment.service.IPaymentWebhookService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.Date;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;

@Service
public class PaymentWebhookServiceImpl implements IPaymentWebhookService {

    private static final int MAX_PROVIDER_CODE_LENGTH = 32;
    private static final int MAX_HEADER_LENGTH = 512;
    private static final int MAX_IDENTIFIER_LENGTH = 128;
    private static final int MAX_CURRENCY_LENGTH = 16;
    private static final int MAX_WEBHOOK_BODY_BYTES = 256 * 1024;
    private static final Set<String> TERMINAL_EVENT_STATUSES = Set.of("PROCESSED", "IGNORED");

    private final PaymentProviderRegistry providerRegistry;
    private final PaymentWebhookEventMapper eventMapper;
    private final PaymentSessionMapper sessionMapper;
    private final PaymentWebhookBusinessProcessor businessProcessor;
    private final TransactionTemplate receiveTransaction;
    private final PaymentWebhookFailureRecorder failureRecorder;
    private final Clock clock;

    public PaymentWebhookServiceImpl(PaymentProviderRegistry providerRegistry,
                                     PaymentWebhookEventMapper eventMapper,
                                     PaymentSessionMapper sessionMapper,
                                     PaymentWebhookBusinessProcessor businessProcessor,
                                     PlatformTransactionManager transactionManager,
                                     Clock clock) {
        this.providerRegistry = providerRegistry;
        this.eventMapper = eventMapper;
        this.sessionMapper = sessionMapper;
        this.businessProcessor = businessProcessor;
        this.clock = clock;
        this.receiveTransaction = new TransactionTemplate(transactionManager);
        this.receiveTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.failureRecorder = new PaymentWebhookFailureRecorder(eventMapper, transactionManager, clock);
    }

    @Override
    public PaymentWebhookAckVo receive(String providerCode, String timestamp, String signature, byte[] rawBody) {
        String normalizedProvider = normalizeProvider(providerCode);
        validateTransport(timestamp, signature, rawBody);
        PaymentProviderAdapter adapter = providerRegistry.resolve(normalizedProvider);
        PaymentWebhookVerificationResult verification = adapter.verifyWebhook(timestamp, signature, rawBody, clock.instant());
        if (verification == null || !verification.verified()) {
            if (verification == null
                || verification.failureKind() != PaymentWebhookVerificationFailureKind.STALE_TIMESTAMP) {
                throw unauthorized();
            }
            return receiveStaleReplay(adapter, normalizedProvider, timestamp, signature, rawBody);
        }

        PaymentWebhookEnvelope envelope;
        try {
            envelope = adapter.parseWebhook(rawBody);
            validateEnvelope(envelope);
        } catch (RuntimeException exception) {
            throw new ServiceException("Invalid payment webhook payload");
        }
        String tenantId = PaymentWebhookBusinessProcessor.canonicalTenantId(envelope.tenantId());
        PaymentWebhookEvent event = TenantHelper.dynamic(tenantId,
            () -> receiveTransaction.execute(status -> receiveVerified(
                tenantId, normalizedProvider, signature, rawBody, envelope)));
        if (event == null) {
            throw new IllegalStateException("Unable to persist payment webhook event");
        }
        if (TERMINAL_EVENT_STATUSES.contains(event.getStatus())) {
            return new PaymentWebhookAckVo(event.getProviderEventId(), event.getStatus());
        }

        try {
            PaymentWebhookBusinessProcessor.WebhookProcessingOutcome outcome = TenantHelper.dynamic(
                tenantId, () -> businessProcessor.processBusiness(event.getId()));
            return new PaymentWebhookAckVo(outcome.providerEventId(), outcome.status());
        } catch (RuntimeException exception) {
            PaymentWebhookEvent concurrentTerminal = TenantHelper.dynamic(tenantId,
                () -> failureRecorder.record(tenantId, event.getId()));
            if (concurrentTerminal != null) {
                return new PaymentWebhookAckVo(
                    concurrentTerminal.getProviderEventId(), concurrentTerminal.getStatus());
            }
            throw exception;
        }
    }

    private PaymentWebhookAckVo receiveStaleReplay(PaymentProviderAdapter adapter, String providerCode,
                                                    String timestamp, String signature, byte[] rawBody) {
        PaymentWebhookVerificationResult cryptographic =
            adapter.verifyWebhookCryptographicSignature(timestamp, signature, rawBody);
        if (cryptographic == null || !cryptographic.verified()) {
            throw unauthorized();
        }
        final PaymentWebhookEnvelope envelope;
        try {
            envelope = adapter.parseWebhook(rawBody);
            validateEnvelope(envelope);
        } catch (RuntimeException exception) {
            throw unauthorized();
        }
        String tenantId;
        try {
            tenantId = PaymentWebhookBusinessProcessor.canonicalTenantId(envelope.tenantId());
        } catch (RuntimeException exception) {
            throw unauthorized();
        }
        PaymentWebhookEvent stored = TenantHelper.dynamic(tenantId,
            () -> eventMapper.selectByProviderEventId(tenantId, providerCode, envelope.providerEventId()));
        PaymentWebhookEvent candidate = buildEvent(tenantId, providerCode, signature, rawBody, envelope);
        if (stored == null || !sameImmutableEvent(stored, candidate)
            || !equals(stored.getSignatureDigest(), candidate.getSignatureDigest())) {
            throw unauthorized();
        }
        if (TERMINAL_EVENT_STATUSES.contains(stored.getStatus())) {
            return new PaymentWebhookAckVo(stored.getProviderEventId(), stored.getStatus());
        }
        try {
            PaymentWebhookBusinessProcessor.WebhookProcessingOutcome outcome = TenantHelper.dynamic(
                tenantId, () -> businessProcessor.processBusiness(stored.getId()));
            return new PaymentWebhookAckVo(outcome.providerEventId(), outcome.status());
        } catch (RuntimeException exception) {
            PaymentWebhookEvent concurrentTerminal = TenantHelper.dynamic(tenantId,
                () -> failureRecorder.record(tenantId, stored.getId()));
            if (concurrentTerminal != null) {
                return new PaymentWebhookAckVo(
                    concurrentTerminal.getProviderEventId(), concurrentTerminal.getStatus());
            }
            throw exception;
        }
    }

    private ResponseStatusException unauthorized() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid payment webhook");
    }

    PaymentWebhookEvent receiveVerified(String tenantId, String providerCode, String signature,
                                        byte[] rawBody, PaymentWebhookEnvelope envelope) {
        PaymentWebhookEvent event = buildEvent(tenantId, providerCode, signature, rawBody, envelope);
        try {
            eventMapper.insert(event);
            return event;
        } catch (DuplicateKeyException exception) {
            PaymentWebhookEvent winner = eventMapper.selectByProviderEventIdForUpdate(
                tenantId, providerCode, envelope.providerEventId());
            if (winner == null || !sameImmutableEvent(winner, event)) {
                throw new ServiceException("Payment webhook event idempotency conflict");
            }
            return winner;
        }
    }

    private PaymentWebhookEvent buildEvent(String tenantId, String providerCode, String signature,
                                           byte[] rawBody, PaymentWebhookEnvelope envelope) {
        Date now = Date.from(clock.instant());
        PaymentWebhookEvent event = new PaymentWebhookEvent();
        event.setId(IdUtil.getSnowflakeNextId());
        event.setTenantId(tenantId);
        event.setProviderCode(providerCode);
        event.setProviderEventId(envelope.providerEventId());
        event.setEventType(envelope.eventType().name());
        event.setProviderSessionNo(envelope.providerSessionNo());
        PaymentSession session = sessionMapper.selectByProviderSessionNo(
            tenantId, providerCode, envelope.providerSessionNo());
        event.setSessionNo(session == null ? null : session.getSessionNo());
        event.setPurchaseOrderNo(envelope.purchaseOrderNo());
        event.setRawBody(new String(rawBody, StandardCharsets.UTF_8));
        event.setSignatureDigest(sha256(signature));
        event.setReceivedTime(now);
        event.setStatus(PaymentWebhookEventStatus.RECEIVED.name());
        event.setProcessingCount(0);
        event.setCreateTime(now);
        event.setUpdateTime(now);
        return event;
    }

    private boolean sameImmutableEvent(PaymentWebhookEvent left, PaymentWebhookEvent right) {
        return equals(left.getEventType(), right.getEventType())
            && equals(left.getProviderSessionNo(), right.getProviderSessionNo())
            && equals(left.getPurchaseOrderNo(), right.getPurchaseOrderNo())
            && equals(left.getRawBody(), right.getRawBody());
    }

    private void validateTransport(String timestamp, String signature, byte[] rawBody) {
        if (isBlank(timestamp) || timestamp.length() > MAX_HEADER_LENGTH
            || isBlank(signature) || signature.length() > MAX_HEADER_LENGTH
            || rawBody == null || rawBody.length == 0 || rawBody.length > MAX_WEBHOOK_BODY_BYTES) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid payment webhook");
        }
    }

    private void validateEnvelope(PaymentWebhookEnvelope envelope) {
        if (envelope == null || envelope.tenantId() == null || envelope.eventType() == null
            || !validIdentifier(envelope.providerEventId()) || !validIdentifier(envelope.providerSessionNo())
            || !validIdentifier(envelope.purchaseOrderNo()) || isBlank(envelope.payCurrencyCode())
            || envelope.payCurrencyCode().length() > MAX_CURRENCY_LENGTH || envelope.payAmount() == null
            || envelope.payAmount().signum() <= 0 || envelope.occurredTime() == null) {
            throw new ServiceException("Invalid payment webhook payload");
        }
    }

    private String normalizeProvider(String providerCode) {
        if (isBlank(providerCode) || providerCode.trim().length() > MAX_PROVIDER_CODE_LENGTH) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid payment webhook");
        }
        return providerCode.trim().toUpperCase(Locale.ROOT);
    }

    private boolean validIdentifier(String value) {
        return !isBlank(value) && value.length() <= MAX_IDENTIFIER_LENGTH;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private boolean equals(Object left, Object right) {
        return left == null ? right == null : left.equals(right);
    }
}
