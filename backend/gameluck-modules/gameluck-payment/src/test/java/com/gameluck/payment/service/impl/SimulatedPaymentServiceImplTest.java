package com.gameluck.payment.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gameluck.common.core.exception.ServiceException;
import com.gameluck.payment.config.PaymentProviderProperties;
import com.gameluck.payment.domain.PaymentSession;
import com.gameluck.payment.domain.PaymentWebhookEvent;
import com.gameluck.payment.domain.SimulatedPaymentDispatch;
import com.gameluck.payment.domain.bo.SimulatedPaymentActionBo;
import com.gameluck.payment.domain.vo.PaymentWebhookAckVo;
import com.gameluck.payment.enums.PaymentProviderEventType;
import com.gameluck.payment.mapper.PaymentSessionMapper;
import com.gameluck.payment.mapper.PaymentWebhookEventMapper;
import com.gameluck.payment.mapper.SimulatedPaymentDispatchMapper;
import com.gameluck.payment.provider.SimulatedPaymentProviderAdapter;
import com.gameluck.payment.provider.PaymentProviderRegistry;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.http.MediaType;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.security.MessageDigest;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

@Tag("local")
class SimulatedPaymentServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-07-27T08:00:00Z");
    private final List<CapturedRequest> requests = new CopyOnWriteArrayList<>();
    private HttpServer server;
    private PaymentSessionMapper sessionMapper;
    private PaymentWebhookEventMapper eventMapper;
    private SimulatedPaymentDispatchMapper dispatchMapper;
    private SimulatedPaymentProviderAdapter adapter;
    private SimulatedPaymentServiceImpl service;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/payment/webhooks/SIMULATED", exchange -> {
            byte[] body = exchange.getRequestBody().readAllBytes();
            requests.add(new CapturedRequest(body,
                exchange.getRequestHeaders().getFirst("X-Payment-Timestamp"),
                exchange.getRequestHeaders().getFirst("X-Payment-Signature"),
                exchange.getRequestHeaders().getFirst("Content-Type")));
            String eventId = new ObjectMapper().readTree(body).get("providerEventId").asText();
            byte[] response = ("{\"providerEventId\":\"" + eventId + "\",\"status\":\"PROCESSED\"}")
                .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        objectMapper = new ObjectMapper().findAndRegisterModules();
        PaymentProviderProperties properties = new PaymentProviderProperties();
        properties.getSimulated().setEnabled(true);
        properties.getSimulated().setSecret("task5-test-secret");
        properties.getSimulated().setCheckoutBaseUrl("http://127.0.0.1/checkout");
        properties.getSimulated().setWebhookBaseUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/payment/webhooks");
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        adapter = new SimulatedPaymentProviderAdapter(properties, objectMapper, clock);
        PaymentProviderRegistry registry = new PaymentProviderRegistry(List.of(adapter), properties);
        sessionMapper = mock(PaymentSessionMapper.class);
        eventMapper = mock(PaymentWebhookEventMapper.class);
        dispatchMapper = mock(SimulatedPaymentDispatchMapper.class);
        when(dispatchMapper.insert(any(SimulatedPaymentDispatch.class))).thenReturn(1);
        service = new SimulatedPaymentServiceImpl(sessionMapper, eventMapper, dispatchMapper, registry, properties,
            objectMapper, RestClient.builder(), clock);
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void pendingCheckoutOffersAndAcceptsOnlyPaymentResults() {
        PaymentSession session = session("PENDING", NOW.plusSeconds(60));
        uniqueSession(session);

        assertThat(service.getCheckout("SIM-1").allowedActions()).containsExactlyInAnyOrder(
            PaymentProviderEventType.PAYMENT_SUCCEEDED,
            PaymentProviderEventType.PAYMENT_FAILED,
            PaymentProviderEventType.PAYMENT_CANCELLED);

        for (PaymentProviderEventType action : List.of(
            PaymentProviderEventType.PAYMENT_SUCCEEDED,
            PaymentProviderEventType.PAYMENT_FAILED,
            PaymentProviderEventType.PAYMENT_CANCELLED)) {
            PaymentWebhookAckVo ack = service.executeAction("SIM-1", new SimulatedPaymentActionBo(action));
            assertThat(ack.status()).isEqualTo("PROCESSED");
        }
        assertThat(requests).hasSize(3).allSatisfy(this::assertSignedRequest);
    }

    @Test
    void succeededCheckoutOffersRefundAndChargeback() {
        uniqueSession(session("SUCCEEDED", NOW.plusSeconds(60)));
        assertThat(service.getCheckout("SIM-1").allowedActions()).containsExactlyInAnyOrder(
            PaymentProviderEventType.REFUND_SUCCEEDED, PaymentProviderEventType.CHARGEBACK_CREATED);
        service.executeAction("SIM-1", new SimulatedPaymentActionBo(PaymentProviderEventType.REFUND_SUCCEEDED));
        service.executeAction("SIM-1", new SimulatedPaymentActionBo(PaymentProviderEventType.CHARGEBACK_CREATED));
        assertThat(requests).hasSize(2);
    }

    @Test
    void processedReversalEventMakesSucceededCheckoutTerminal() {
        uniqueSession(session("SUCCEEDED", NOW.plusSeconds(60)));
        PaymentWebhookEvent latest = new PaymentWebhookEvent();
        latest.setTenantId("000001");
        latest.setProviderCode("SIMULATED");
        latest.setProviderSessionNo("SIM-1");
        latest.setProviderEventId("evt-refund");
        latest.setEventType("REFUND_SUCCEEDED");
        latest.setStatus("PROCESSED");
        when(dispatchMapper.countProcessedReversal("000001", "SIM-1")).thenReturn(1);

        assertThat(service.getCheckout("SIM-1").allowedActions()).isEmpty();
        assertThatThrownBy(() -> service.executeAction("SIM-1",
            new SimulatedPaymentActionBo(PaymentProviderEventType.CHARGEBACK_CREATED)))
            .isInstanceOf(ServiceException.class);
    }

    @Test
    void externalFailedIgnoredOrOutOfOrderEventsCannotBecomeHostedReplayOrCloseActions() {
        uniqueSession(session("SUCCEEDED", NOW.plusSeconds(60)));
        PaymentWebhookEvent external = new PaymentWebhookEvent();
        external.setProviderEventId("external-late");
        external.setEventType("CHARGEBACK_CREATED");
        external.setStatus("FAILED");
        when(eventMapper.selectLatestByProviderSessionNo("000001", "SIMULATED", "SIM-1"))
            .thenReturn(external);
        when(dispatchMapper.countProcessedReversal("000001", "SIM-1")).thenReturn(0);

        assertThat(service.getCheckout("SIM-1").allowedActions()).containsExactlyInAnyOrder(
            PaymentProviderEventType.REFUND_SUCCEEDED, PaymentProviderEventType.CHARGEBACK_CREATED);
        assertThat(service.getCheckout("SIM-1").latestProviderEventId()).isNull();
        assertThatThrownBy(() -> service.replay("SIM-1")).isInstanceOf(ServiceException.class);
    }

    @Test
    void disabledProviderRejectsAllHostedEntrancesBeforePersistenceOrHttp() {
        PaymentProviderProperties disabled = new PaymentProviderProperties();
        disabled.getSimulated().setEnabled(false);
        disabled.getSimulated().setCheckoutBaseUrl("http://127.0.0.1/checkout");
        disabled.getSimulated().setWebhookBaseUrl(
            "http://127.0.0.1:" + server.getAddress().getPort() + "/payment/webhooks");
        PaymentProviderRegistry disabledRegistry = new PaymentProviderRegistry(List.of(adapter), disabled);
        SimulatedPaymentServiceImpl disabledService = new SimulatedPaymentServiceImpl(
            sessionMapper, eventMapper, dispatchMapper, disabledRegistry, disabled,
            objectMapper, RestClient.builder(), Clock.fixed(NOW, ZoneOffset.UTC));

        assertThatThrownBy(() -> disabledService.getCheckout("SIM-1")).isInstanceOf(ServiceException.class);
        assertThatThrownBy(() -> disabledService.executeAction("SIM-1",
            new SimulatedPaymentActionBo(PaymentProviderEventType.PAYMENT_SUCCEEDED)))
            .isInstanceOf(ServiceException.class);
        assertThatThrownBy(() -> disabledService.replay("SIM-1")).isInstanceOf(ServiceException.class);
        verifyNoInteractions(sessionMapper, eventMapper, dispatchMapper);
        assertThat(requests).isEmpty();
    }

    @Test
    void corruptReplayMarkerOrEnvelopeReturnsControlledInvalidError() {
        uniqueSession(session("SUCCEEDED", NOW.plusSeconds(60)));
        SimulatedPaymentDispatch marker = new SimulatedPaymentDispatch();
        marker.setProviderEventId("evt-bad");
        marker.setAction(null);
        PaymentWebhookEvent event = new PaymentWebhookEvent();
        event.setProviderEventId("evt-bad");
        event.setRawBody("{}");
        when(dispatchMapper.selectLatestReplayable("000001", "SIM-1")).thenReturn(marker);
        when(eventMapper.selectByProviderEventId("000001", "SIMULATED", "evt-bad")).thenReturn(event);

        assertThatThrownBy(() -> service.replay("SIM-1"))
            .isInstanceOf(ServiceException.class);
        assertThat(requests).isEmpty();
    }

    @Test
    void rejectsTerminalInvalidExpiredMissingAndAmbiguousSessions() {
        uniqueSession(session("FAILED", NOW.plusSeconds(60)));
        assertThatThrownBy(() -> service.executeAction("SIM-1",
            new SimulatedPaymentActionBo(PaymentProviderEventType.PAYMENT_SUCCEEDED)))
            .isInstanceOf(ServiceException.class);

        uniqueSession(session("PENDING", NOW));
        assertThat(service.getCheckout("SIM-1").status()).isEqualTo("EXPIRED");
        assertThatThrownBy(() -> service.executeAction("SIM-1",
            new SimulatedPaymentActionBo(PaymentProviderEventType.PAYMENT_SUCCEEDED)))
            .isInstanceOf(ServiceException.class);

        when(sessionMapper.selectPublicByProviderSessionNo("SIMULATED", "missing")).thenReturn(List.of());
        assertThatThrownBy(() -> service.getCheckout("missing")).isInstanceOf(ServiceException.class);
        when(sessionMapper.selectPublicByProviderSessionNo("SIMULATED", "duplicate"))
            .thenReturn(List.of(session("PENDING", NOW.plusSeconds(60)), session("PENDING", NOW.plusSeconds(60))));
        assertThatThrownBy(() -> service.getCheckout("duplicate")).isInstanceOf(ServiceException.class);
    }

    @Test
    void eachNewActionUsesUniqueEventId() throws Exception {
        uniqueSession(session("PENDING", NOW.plusSeconds(60)));
        service.executeAction("SIM-1", new SimulatedPaymentActionBo(PaymentProviderEventType.PAYMENT_FAILED));
        service.executeAction("SIM-1", new SimulatedPaymentActionBo(PaymentProviderEventType.PAYMENT_FAILED));
        assertThat(objectMapper.readTree(requests.get(0).body()).get("providerEventId").asText())
            .isNotEqualTo(objectMapper.readTree(requests.get(1).body()).get("providerEventId").asText());
    }

    @Test
    void replayUsesExactPersistedEventIdBodyAndSignature() throws Exception {
        uniqueSession(session("SUCCEEDED", NOW.plusSeconds(60)));
        byte[] raw = ("{\"tenantId\":1,\"providerEventId\":\"evt-stable\",\"eventType\":\"PAYMENT_SUCCEEDED\","
            + "\"providerSessionNo\":\"SIM-1\",\"purchaseOrderNo\":\"PO-1\",\"payCurrencyCode\":\"USD\","
            + "\"payAmount\":10.00,\"occurredTime\":\"2026-07-27T08:00:00Z\"}").getBytes(StandardCharsets.UTF_8);
        PaymentWebhookEvent event = new PaymentWebhookEvent();
        event.setTenantId("000001");
        event.setProviderCode("SIMULATED");
        event.setProviderEventId("evt-stable");
        event.setProviderSessionNo("SIM-1");
        event.setRawBody(new String(raw, StandardCharsets.UTF_8));
        SimulatedPaymentDispatch marker = new SimulatedPaymentDispatch();
        marker.setTenantId("000001");
        marker.setProviderSessionNo("SIM-1");
        marker.setProviderEventId("evt-stable");
        marker.setAction("PAYMENT_SUCCEEDED");
        when(dispatchMapper.selectLatestReplayable("000001", "SIM-1")).thenReturn(marker);
        when(eventMapper.selectByProviderEventId("000001", "SIMULATED", "evt-stable")).thenReturn(event);

        service.replay("SIM-1");

        CapturedRequest original = requests.get(0);
        event.setId(77L);
        event.setEventType("PAYMENT_SUCCEEDED");
        event.setPurchaseOrderNo("PO-1");
        event.setSignatureDigest(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
            .digest(original.signature().getBytes(StandardCharsets.UTF_8))));
        event.setStatus("PROCESSED");
        when(sessionMapper.selectByProviderSessionNo("000001", "SIMULATED", "SIM-1"))
            .thenReturn(session("SUCCEEDED", NOW.plusSeconds(60)));

        PaymentProviderProperties restartedProperties = new PaymentProviderProperties();
        restartedProperties.getSimulated().setEnabled(true);
        restartedProperties.getSimulated().setSecret("task5-test-secret");
        restartedProperties.getSimulated().setCheckoutBaseUrl("http://127.0.0.1/checkout");
        restartedProperties.getSimulated().setWebhookBaseUrl(
            "http://127.0.0.1:" + server.getAddress().getPort() + "/payment/webhooks");
        Clock advancedClock = Clock.fixed(NOW.plusSeconds(600), ZoneOffset.UTC);
        SimulatedPaymentProviderAdapter restartedAdapter =
            new SimulatedPaymentProviderAdapter(restartedProperties, objectMapper, advancedClock);
        PaymentProviderRegistry actualRegistry =
            new PaymentProviderRegistry(List.of(restartedAdapter), restartedProperties);
        PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
        when(transactions.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        PaymentWebhookServiceImpl actualWebhookService = new PaymentWebhookServiceImpl(
            actualRegistry, eventMapper, sessionMapper, mock(PaymentWebhookBusinessProcessor.class),
            transactions, advancedClock);

        server.removeContext("/payment/webhooks/SIMULATED");
        server.createContext("/payment/webhooks/SIMULATED", exchange -> {
            byte[] requestBody = exchange.getRequestBody().readAllBytes();
            String timestamp = exchange.getRequestHeaders().getFirst("X-Payment-Timestamp");
            String signature = exchange.getRequestHeaders().getFirst("X-Payment-Signature");
            requests.add(new CapturedRequest(requestBody, timestamp, signature,
                exchange.getRequestHeaders().getFirst("Content-Type")));
            try {
                PaymentWebhookAckVo ack = actualWebhookService.receive("SIMULATED", timestamp, signature, requestBody);
                byte[] response = objectMapper.writeValueAsBytes(ack);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.length);
                exchange.getResponseBody().write(response);
            } catch (ResponseStatusException exception) {
                exchange.sendResponseHeaders(exception.getStatusCode().value(), -1);
            } finally {
                exchange.close();
            }
        });
        SimulatedPaymentServiceImpl restarted = new SimulatedPaymentServiceImpl(
            sessionMapper, eventMapper, dispatchMapper, actualRegistry, restartedProperties,
            objectMapper, RestClient.builder(), advancedClock);
        restarted.replay("SIM-1");

        assertThat(requests).hasSize(2);
        assertThat(requests.get(0)).usingRecursiveComparison().isEqualTo(requests.get(1));
        assertThat(requests.get(0).body()).isEqualTo(raw);
        assertThat(restartedAdapter.verifyWebhookCryptographicSignature(
            requests.get(1).timestamp(), requests.get(1).signature(), requests.get(1).body()).verified()).isTrue();
        assertThat(restartedAdapter.verifyWebhook(
            requests.get(1).timestamp(), requests.get(1).signature(), requests.get(1).body(),
            NOW.plusSeconds(600)).verified()).isFalse();

        RestClient ingress = RestClient.builder().baseUrl(
            "http://127.0.0.1:" + server.getAddress().getPort() + "/payment/webhooks/SIMULATED").build();
        byte[] newStale = new String(raw, StandardCharsets.UTF_8)
            .replace("evt-stable", "evt-new-stale").getBytes(StandardCharsets.UTF_8);
        String oldTimestamp = Long.toString(NOW.getEpochSecond());
        assertThatThrownBy(() -> ingress.post().contentType(MediaType.APPLICATION_JSON)
            .header("X-Payment-Timestamp", oldTimestamp)
            .header("X-Payment-Signature", restartedAdapter.signWebhook(oldTimestamp, newStale))
            .body(newStale).retrieve().body(PaymentWebhookAckVo.class))
            .isInstanceOf(HttpClientErrorException.Unauthorized.class);

        byte[] tampered = new String(raw, StandardCharsets.UTF_8)
            .replace("10.00", "11.00").getBytes(StandardCharsets.UTF_8);
        assertThatThrownBy(() -> ingress.post().contentType(MediaType.APPLICATION_JSON)
            .header("X-Payment-Timestamp", oldTimestamp)
            .header("X-Payment-Signature", original.signature())
            .body(tampered).retrieve().body(PaymentWebhookAckVo.class))
            .isInstanceOf(HttpClientErrorException.Unauthorized.class);
        assertThat(requests).hasSize(4);
    }

    private void uniqueSession(PaymentSession session) {
        when(sessionMapper.selectPublicByProviderSessionNo("SIMULATED", "SIM-1")).thenReturn(List.of(session));
    }

    private PaymentSession session(String status, Instant expiry) {
        PaymentSession session = new PaymentSession();
        session.setTenantId("000001");
        session.setSessionNo("PS-1");
        session.setPurchaseOrderNo("PO-1");
        session.setProviderCode("SIMULATED");
        session.setProviderSessionNo("SIM-1");
        session.setPayCurrencyCode("USD");
        session.setPayAmount(new BigDecimal("10.00"));
        session.setCheckoutUrl("http://127.0.0.1/checkout/SIM-1");
        session.setStatus(status);
        session.setExpireTime(Date.from(expiry));
        return session;
    }

    private void assertSignedRequest(CapturedRequest request) {
        assertThat(request.contentType()).startsWith("application/json");
        assertThat(adapter.verifyWebhook(request.timestamp(), request.signature(), request.body(), NOW).verified()).isTrue();
    }

    private record CapturedRequest(byte[] body, String timestamp, String signature, String contentType) {
    }
}
