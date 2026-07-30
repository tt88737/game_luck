package com.gameluck.payment.service.impl;

import com.gameluck.common.core.exception.ServiceException;
import com.gameluck.common.tenant.helper.TenantHelper;
import com.gameluck.payment.domain.PaymentSession;
import com.gameluck.payment.domain.PaymentWebhookEvent;
import com.gameluck.payment.domain.PurchaseOrder;
import com.gameluck.payment.domain.bo.PurchasePaymentCallbackBo;
import com.gameluck.payment.enums.PaymentProviderEventType;
import com.gameluck.payment.enums.PaymentSessionStatus;
import com.gameluck.payment.enums.PaymentWebhookEventStatus;
import com.gameluck.payment.enums.PurchasePaymentEventType;
import com.gameluck.payment.mapper.PaymentSessionMapper;
import com.gameluck.payment.mapper.PaymentWebhookEventMapper;
import com.gameluck.payment.mapper.PurchaseOrderMapper;
import com.gameluck.payment.provider.PaymentProviderAdapter;
import com.gameluck.payment.provider.PaymentProviderRegistry;
import com.gameluck.payment.provider.PaymentWebhookEnvelope;
import com.gameluck.payment.service.IPurchasePaymentEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Date;
import java.util.Set;

@RequiredArgsConstructor
@Service
public class PaymentWebhookBusinessProcessor {

    private static final Set<String> COMPLETED_EVENT_STATUSES = Set.of("PROCESSED", "IGNORED");

    private final PaymentWebhookEventMapper eventMapper;
    private final PaymentSessionMapper sessionMapper;
    private final PurchaseOrderMapper orderMapper;
    private final IPurchasePaymentEventService purchasePaymentEventService;
    private final PaymentProviderRegistry providerRegistry;
    private final Clock clock;

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public WebhookProcessingOutcome processBusiness(Long eventId) {
        String tenantId = TenantHelper.getTenantId();
        PaymentWebhookEvent event = eventMapper.selectByIdForUpdate(tenantId, eventId);
        if (event == null) {
            throw new ServiceException("Payment webhook event does not exist");
        }
        if (COMPLETED_EVENT_STATUSES.contains(event.getStatus())) {
            return new WebhookProcessingOutcome(event.getProviderEventId(), event.getStatus());
        }
        if (!PaymentWebhookEventStatus.RECEIVED.name().equals(event.getStatus())
            && !PaymentWebhookEventStatus.FAILED.name().equals(event.getStatus())) {
            throw new ServiceException("Payment webhook event status is invalid");
        }

        PaymentProviderAdapter adapter = providerRegistry.resolve(event.getProviderCode());
        PaymentWebhookEnvelope envelope = adapter.parseWebhook(event.getRawBody().getBytes(StandardCharsets.UTF_8));
        validateStoredEnvelope(tenantId, event, envelope);

        PaymentSession session = sessionMapper.selectByProviderSessionNoForUpdate(
            tenantId, event.getProviderCode(), envelope.providerSessionNo());
        if (session == null || (event.getSessionNo() != null && !event.getSessionNo().equals(session.getSessionNo()))) {
            throw new ServiceException("Payment session does not exist");
        }
        PurchaseOrder order = orderMapper.selectByOrderNoForUpdate(tenantId, envelope.purchaseOrderNo());
        if (order == null) {
            throw new ServiceException("Purchase order does not exist");
        }
        validateSnapshots(event, envelope, session, order);

        Date now = Date.from(clock.instant());
        String currentEventStatus = event.getStatus();
        if (envelope.eventType() == PaymentProviderEventType.PAYMENT_SUCCEEDED
            && isActive(session.getStatus()) && isExpired(session, now)) {
            int sessionUpdated = sessionMapper.updateStatusGuarded(
                tenantId, session.getId(), PaymentSessionStatus.EXPIRED.name(), session.getStatus(), now);
            if (sessionUpdated != 1) {
                throw new ServiceException("Payment session state changed concurrently");
            }
            completeEvent(tenantId, eventId, currentEventStatus, PaymentWebhookEventStatus.IGNORED.name(), now);
            return new WebhookProcessingOutcome(event.getProviderEventId(), PaymentWebhookEventStatus.IGNORED.name());
        }
        if (isContradictoryTerminal(envelope.eventType(), session.getStatus())) {
            completeEvent(tenantId, eventId, currentEventStatus, PaymentWebhookEventStatus.IGNORED.name(), now);
            return new WebhookProcessingOutcome(event.getProviderEventId(), PaymentWebhookEventStatus.IGNORED.name());
        }

        purchasePaymentEventService.applyEvent(PurchasePaymentCallbackBo.builder()
            .tenantId(tenantId)
            .eventKey(event.getProviderCode() + ":" + event.getProviderEventId())
            .purchaseOrderNo(order.getPurchaseOrderNo())
            .providerCode(event.getProviderCode())
            .providerOrderNo(session.getProviderSessionNo())
            .eventType(mapEventType(envelope.eventType()))
            .requestBody(event.getRawBody())
            .failReason(providerFailureReason(envelope.eventType()))
            .build());

        String nextSessionStatus = mapSessionStatus(envelope.eventType());
        int sessionUpdated = sessionMapper.updateStatusGuarded(
            tenantId, session.getId(), nextSessionStatus, session.getStatus(), now);
        if (sessionUpdated != 1) {
            throw new ServiceException("Payment session state changed concurrently");
        }
        completeEvent(tenantId, eventId, currentEventStatus, PaymentWebhookEventStatus.PROCESSED.name(), now);
        return new WebhookProcessingOutcome(event.getProviderEventId(), PaymentWebhookEventStatus.PROCESSED.name());
    }

    private void completeEvent(String tenantId, Long eventId, String expected, String status, Date now) {
        if (eventMapper.completeProcessing(tenantId, eventId, expected, status, now) != 1) {
            throw new ServiceException("Payment webhook event state changed concurrently");
        }
    }

    private void validateStoredEnvelope(String tenantId, PaymentWebhookEvent event, PaymentWebhookEnvelope envelope) {
        if (envelope == null || envelope.tenantId() == null || envelope.eventType() == null
            || !tenantId.equals(canonicalTenantId(envelope.tenantId()))
            || !event.getProviderEventId().equals(envelope.providerEventId())
            || !event.getEventType().equals(envelope.eventType().name())
            || !event.getProviderSessionNo().equals(envelope.providerSessionNo())
            || !event.getPurchaseOrderNo().equals(envelope.purchaseOrderNo())) {
            throw new ServiceException("Payment webhook envelope does not match stored event");
        }
    }

    private void validateSnapshots(PaymentWebhookEvent event, PaymentWebhookEnvelope envelope,
                                   PaymentSession session, PurchaseOrder order) {
        if (!event.getTenantId().equals(session.getTenantId()) || !event.getTenantId().equals(order.getTenantId())
            || !session.getPurchaseOrderNo().equals(order.getPurchaseOrderNo())
            || !session.getSessionNo().equals(order.getPaymentSessionNo())
            || !event.getProviderCode().equals(session.getProviderCode())
            || !event.getProviderCode().equals(order.getProviderCode())
            || !session.getProviderSessionNo().equals(order.getProviderOrderNo())
            || !equalMoney(envelope.payAmount(), session.getPayAmount())
            || !equalMoney(envelope.payAmount(), order.getPayAmount())
            || !envelope.payCurrencyCode().equals(session.getPayCurrencyCode())
            || !envelope.payCurrencyCode().equals(order.getPayCurrencyCode())) {
            throw new ServiceException("Payment webhook amount, currency, or identity mismatch");
        }
    }

    private boolean isContradictoryTerminal(PaymentProviderEventType type, String sessionStatus) {
        if (isActive(sessionStatus)) {
            return type == PaymentProviderEventType.REFUND_SUCCEEDED
                || type == PaymentProviderEventType.CHARGEBACK_CREATED;
        }
        if (PaymentSessionStatus.SUCCEEDED.name().equals(sessionStatus)) {
            return type == PaymentProviderEventType.PAYMENT_FAILED
                || type == PaymentProviderEventType.PAYMENT_CANCELLED;
        }
        return true;
    }

    private boolean isActive(String sessionStatus) {
        return PaymentSessionStatus.PENDING.name().equals(sessionStatus)
            || PaymentSessionStatus.CREATED.name().equals(sessionStatus);
    }

    private boolean isExpired(PaymentSession session, Date now) {
        return session.getExpireTime() != null && !session.getExpireTime().after(now);
    }

    private PurchasePaymentEventType mapEventType(PaymentProviderEventType type) {
        return switch (type) {
            case PAYMENT_SUCCEEDED -> PurchasePaymentEventType.PAY_SUCCESS;
            case PAYMENT_FAILED -> PurchasePaymentEventType.PAY_FAILED;
            case PAYMENT_CANCELLED -> PurchasePaymentEventType.CANCELLED;
            case REFUND_SUCCEEDED -> PurchasePaymentEventType.REFUNDED;
            case CHARGEBACK_CREATED -> PurchasePaymentEventType.CHARGEBACK;
        };
    }

    private String mapSessionStatus(PaymentProviderEventType type) {
        return switch (type) {
            case PAYMENT_SUCCEEDED -> PaymentSessionStatus.SUCCEEDED.name();
            case PAYMENT_FAILED -> PaymentSessionStatus.FAILED.name();
            case PAYMENT_CANCELLED -> PaymentSessionStatus.CANCELLED.name();
            case REFUND_SUCCEEDED, CHARGEBACK_CREATED -> PaymentSessionStatus.SUCCEEDED.name();
        };
    }

    private String providerFailureReason(PaymentProviderEventType type) {
        return type == PaymentProviderEventType.PAYMENT_FAILED ? "Provider reported payment failure" : null;
    }

    private boolean equalMoney(BigDecimal left, BigDecimal right) {
        return left != null && right != null && left.compareTo(right) == 0;
    }

    static String canonicalTenantId(Long tenantId) {
        if (tenantId == null || tenantId < 0 || tenantId > 999999L) {
            throw new ServiceException("Payment webhook tenant is invalid");
        }
        return String.format("%06d", tenantId);
    }

    public record WebhookProcessingOutcome(String providerEventId, String status) {
    }
}
