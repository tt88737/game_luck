package com.gameluck.payment.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gameluck.common.core.exception.ServiceException;
import com.gameluck.common.core.utils.MessageUtils;
import com.gameluck.common.tenant.helper.TenantHelper;
import com.gameluck.payment.config.PaymentProviderProperties;
import com.gameluck.payment.domain.PaymentSession;
import com.gameluck.payment.domain.PaymentWebhookEvent;
import com.gameluck.payment.domain.SimulatedPaymentDispatch;
import com.gameluck.payment.domain.bo.SimulatedPaymentActionBo;
import com.gameluck.payment.domain.vo.PaymentWebhookAckVo;
import com.gameluck.payment.domain.vo.SimulatedCheckoutVo;
import com.gameluck.payment.enums.PaymentProviderEventType;
import com.gameluck.payment.enums.PaymentSessionStatus;
import com.gameluck.payment.mapper.PaymentSessionMapper;
import com.gameluck.payment.mapper.PaymentWebhookEventMapper;
import com.gameluck.payment.mapper.SimulatedPaymentDispatchMapper;
import com.gameluck.payment.provider.PaymentWebhookEnvelope;
import com.gameluck.payment.provider.SimulatedPaymentProviderAdapter;
import com.gameluck.payment.provider.PaymentProviderAdapter;
import com.gameluck.payment.provider.PaymentProviderRegistry;
import com.gameluck.payment.service.ISimulatedPaymentService;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Clock;
import java.time.Instant;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class SimulatedPaymentServiceImpl implements ISimulatedPaymentService {

    private static final String PROVIDER_CODE = "SIMULATED";
    private static final int MAX_PROVIDER_SESSION_NO_LENGTH = 128;
    private static final int MAX_WEBHOOK_RESPONSE_BYTES = 64 * 1024;
    private static final Set<PaymentProviderEventType> PENDING_ACTIONS = Set.of(
        PaymentProviderEventType.PAYMENT_SUCCEEDED,
        PaymentProviderEventType.PAYMENT_FAILED,
        PaymentProviderEventType.PAYMENT_CANCELLED);
    private static final Set<PaymentProviderEventType> SUCCEEDED_ACTIONS = Set.of(
        PaymentProviderEventType.REFUND_SUCCEEDED,
        PaymentProviderEventType.CHARGEBACK_CREATED);

    private final PaymentSessionMapper sessionMapper;
    private final PaymentWebhookEventMapper eventMapper;
    private final SimulatedPaymentDispatchMapper dispatchMapper;
    private final PaymentProviderRegistry providerRegistry;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final Clock clock;
    private final Object[] actionLocks = new Object[64];

    public SimulatedPaymentServiceImpl(PaymentSessionMapper sessionMapper,
                                       PaymentWebhookEventMapper eventMapper,
                                       SimulatedPaymentDispatchMapper dispatchMapper,
                                       PaymentProviderRegistry providerRegistry,
                                       PaymentProviderProperties properties,
                                       ObjectMapper objectMapper,
                                       RestClient.Builder restClientBuilder,
                                       Clock clock) {
        this.sessionMapper = sessionMapper;
        this.eventMapper = eventMapper;
        this.dispatchMapper = dispatchMapper;
        this.providerRegistry = providerRegistry;
        this.objectMapper = objectMapper;
        this.clock = clock;
        java.util.Arrays.setAll(actionLocks, index -> new Object());
        String webhookUrl = stripTrailingSlashes(properties.getSimulated().getWebhookBaseUrl()) + "/" + PROVIDER_CODE;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(10));
        this.restClient = restClientBuilder.clone().requestFactory(requestFactory).baseUrl(webhookUrl).build();
    }

    @Override
    public SimulatedCheckoutVo getCheckout(String providerSessionNo) {
        requireSimulatedProvider();
        PaymentSession session = resolveSession(providerSessionNo);
        SimulatedPaymentDispatch latestDispatch = TenantHelper.dynamic(session.getTenantId(),
            () -> dispatchMapper.selectLatestDelivered(session.getTenantId(), session.getProviderSessionNo()));
        PaymentWebhookEvent latest = latestDispatch == null ? null : TenantHelper.dynamic(session.getTenantId(),
            () -> eventMapper.selectByProviderEventId(
                session.getTenantId(), PROVIDER_CODE, latestDispatch.getProviderEventId()));
        boolean reversalProcessed = TenantHelper.dynamic(session.getTenantId(),
            () -> dispatchMapper.countProcessedReversal(session.getTenantId(), session.getProviderSessionNo())) > 0;
        String displayStatus = displayStatus(session);
        return new SimulatedCheckoutVo(
            session.getSessionNo(), session.getPurchaseOrderNo(), session.getProviderCode(),
            session.getProviderSessionNo(), session.getPayCurrencyCode(), session.getPayAmount(),
            displayStatus, session.getExpireTime(), session.getCheckoutUrl(),
            allowedActions(displayStatus, reversalProcessed), latest == null ? null : latest.getProviderEventId(),
            latest == null ? null : latest.getStatus());
    }

    @Override
    public PaymentWebhookAckVo executeAction(String providerSessionNo, SimulatedPaymentActionBo request) {
        requireSimulatedProvider();
        validateProviderSessionNo(providerSessionNo);
        Object actionLock = actionLocks[(providerSessionNo.hashCode() & Integer.MAX_VALUE) % actionLocks.length];
        synchronized (actionLock) {
            return executeActionSerialized(providerSessionNo, request);
        }
    }

    private PaymentWebhookAckVo executeActionSerialized(String providerSessionNo, SimulatedPaymentActionBo request) {
        PaymentSession session = resolveSession(providerSessionNo);
        PaymentProviderEventType action = request == null ? null : request.action();
        boolean reversalProcessed = TenantHelper.dynamic(session.getTenantId(),
            () -> dispatchMapper.countProcessedReversal(session.getTenantId(), session.getProviderSessionNo())) > 0;
        if (action == null || !allowedActions(displayStatus(session), reversalProcessed).contains(action)) {
            throw error("payment.simulated.action.invalid");
        }
        Instant occurredTime = clock.instant();
        PaymentWebhookEnvelope envelope = new PaymentWebhookEnvelope(
            parseTenantId(session.getTenantId()),
            "sim_evt_" + UUID.randomUUID().toString().replace("-", ""),
            action,
            session.getProviderSessionNo(),
            session.getPurchaseOrderNo(),
            session.getPayCurrencyCode(),
            session.getPayAmount(),
            occurredTime);
        SimulatedPaymentDispatch marker = new SimulatedPaymentDispatch();
        marker.setId(cn.hutool.core.util.IdUtil.getSnowflakeNextId());
        marker.setTenantId(session.getTenantId());
        marker.setProviderSessionNo(session.getProviderSessionNo());
        marker.setProviderEventId(envelope.providerEventId());
        marker.setAction(action.name());
        marker.setOccurredTime(Date.from(occurredTime));
        marker.setCreateTime(Date.from(occurredTime));
        int inserted = TenantHelper.dynamic(session.getTenantId(), () -> dispatchMapper.insert(marker));
        if (inserted != 1) {
            throw error("payment.simulated.dispatch.failed");
        }
        return dispatch(serialize(envelope), Long.toString(occurredTime.getEpochSecond()));
    }

    @Override
    public PaymentWebhookAckVo replay(String providerSessionNo) {
        requireSimulatedProvider();
        PaymentSession session = resolveSession(providerSessionNo);
        SimulatedPaymentDispatch marker = TenantHelper.dynamic(session.getTenantId(),
            () -> dispatchMapper.selectLatestReplayable(session.getTenantId(), session.getProviderSessionNo()));
        PaymentWebhookEvent event = marker == null ? null : TenantHelper.dynamic(session.getTenantId(),
            () -> eventMapper.selectByProviderEventId(
                session.getTenantId(), PROVIDER_CODE, marker.getProviderEventId()));
        if (event == null || event.getRawBody() == null || event.getRawBody().isEmpty()) {
            throw error("payment.simulated.replay.not.exists");
        }
        byte[] rawBody = event.getRawBody().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        try {
            PaymentWebhookEnvelope envelope = requireSimulatedProvider().parseWebhook(rawBody);
            if (marker.getAction() == null || marker.getAction().isBlank()
                || envelope == null || envelope.eventType() == null || envelope.occurredTime() == null
                || !event.getProviderEventId().equals(envelope.providerEventId())
                || !marker.getProviderEventId().equals(envelope.providerEventId())
                || !marker.getAction().equals(envelope.eventType().name())
                || !session.getTenantId().equals(PaymentWebhookBusinessProcessor.canonicalTenantId(envelope.tenantId()))
                || !session.getProviderSessionNo().equals(envelope.providerSessionNo())) {
                throw error("payment.simulated.replay.invalid");
            }
            return dispatch(rawBody, Long.toString(envelope.occurredTime().getEpochSecond()));
        } catch (RuntimeException exception) {
            if (exception instanceof ServiceException
                && MessageUtils.message("payment.simulated.replay.invalid").equals(exception.getMessage())) {
                throw exception;
            }
            throw error("payment.simulated.replay.invalid");
        }
    }

    private PaymentWebhookAckVo dispatch(byte[] rawBody, String timestamp) {
        SimulatedPaymentProviderAdapter adapter = requireSimulatedProvider();
        String signature = adapter.signWebhook(timestamp, rawBody);
        String expectedEventId = adapter.parseWebhook(rawBody).providerEventId();
        try {
            PaymentWebhookAckVo ack = restClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Payment-Timestamp", timestamp)
                .header("X-Payment-Signature", signature)
                .body(rawBody)
                .exchange((request, response) -> {
                    if (!response.getStatusCode().is2xxSuccessful()) {
                        throw error("payment.simulated.dispatch.failed");
                    }
                    byte[] responseBody = response.getBody().readNBytes(MAX_WEBHOOK_RESPONSE_BYTES + 1);
                    if (responseBody.length > MAX_WEBHOOK_RESPONSE_BYTES) {
                        throw error("payment.simulated.dispatch.failed");
                    }
                    try {
                        return objectMapper.readValue(responseBody, PaymentWebhookAckVo.class);
                    } catch (JsonProcessingException exception) {
                        throw error("payment.simulated.dispatch.failed");
                    }
                });
            if (ack == null || !expectedEventId.equals(ack.providerEventId()) || ack.status() == null) {
                throw error("payment.simulated.dispatch.failed");
            }
            return ack;
        } catch (RestClientException exception) {
            throw error("payment.simulated.dispatch.failed");
        }
    }

    private PaymentSession resolveSession(String providerSessionNo) {
        validateProviderSessionNo(providerSessionNo);
        List<PaymentSession> matches = sessionMapper.selectPublicByProviderSessionNo(
            PROVIDER_CODE, providerSessionNo);
        if (matches == null || matches.size() != 1) {
            throw error("payment.session.not.exists");
        }
        PaymentSession session = matches.get(0);
        if (session.getTenantId() == null || !PROVIDER_CODE.equals(session.getProviderCode())
            || !providerSessionNo.equals(session.getProviderSessionNo())) {
            throw error("payment.session.not.exists");
        }
        return session;
    }

    private void validateProviderSessionNo(String providerSessionNo) {
        if (providerSessionNo == null || providerSessionNo.isBlank()
            || providerSessionNo.length() > MAX_PROVIDER_SESSION_NO_LENGTH) {
            throw error("payment.session.not.exists");
        }
    }

    private List<PaymentProviderEventType> allowedActions(String status, boolean reversalProcessed) {
        if (PaymentSessionStatus.PENDING.name().equals(status)
            || PaymentSessionStatus.CREATED.name().equals(status)) {
            return List.copyOf(PENDING_ACTIONS);
        }
        if (PaymentSessionStatus.SUCCEEDED.name().equals(status)) {
            if (reversalProcessed) {
                return List.of();
            }
            return List.copyOf(SUCCEEDED_ACTIONS);
        }
        return List.of();
    }

    private String displayStatus(PaymentSession session) {
        if ((PaymentSessionStatus.PENDING.name().equals(session.getStatus())
            || PaymentSessionStatus.CREATED.name().equals(session.getStatus()))
            && session.getExpireTime() != null && !session.getExpireTime().after(Date.from(clock.instant()))) {
            return PaymentSessionStatus.EXPIRED.name();
        }
        return session.getStatus();
    }

    private byte[] serialize(PaymentWebhookEnvelope envelope) {
        try {
            return objectMapper.writeValueAsBytes(envelope);
        } catch (JsonProcessingException exception) {
            throw error("payment.simulated.dispatch.failed");
        }
    }

    private Long parseTenantId(String tenantId) {
        try {
            return Long.valueOf(tenantId);
        } catch (NumberFormatException exception) {
            throw error("payment.simulated.session.invalid");
        }
    }

    private ServiceException error(String key) {
        return new ServiceException(MessageUtils.message(key));
    }

    private SimulatedPaymentProviderAdapter requireSimulatedProvider() {
        PaymentProviderAdapter resolved;
        try {
            resolved = providerRegistry.resolve(PROVIDER_CODE);
        } catch (RuntimeException exception) {
            throw error("payment.session.not.exists");
        }
        if (!(resolved instanceof SimulatedPaymentProviderAdapter simulated)) {
            throw error("payment.session.not.exists");
        }
        return simulated;
    }

    private static String stripTrailingSlashes(String value) {
        int end = value.length();
        while (end > 0 && value.charAt(end - 1) == '/') {
            end--;
        }
        return value.substring(0, end);
    }
}
