package com.gameluck.payment.service.impl;

import com.gameluck.payment.domain.*;
import com.gameluck.payment.mapper.*;
import com.gameluck.payment.service.reconciliation.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.function.Function;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PaymentReconciliationPlatformDataSource implements PaymentReconciliationExecutionService.PlatformDataSource {
    private static final int PAGE_SIZE = 500;
    private final PaymentSessionMapper sessionMapper;
    private final PurchaseOrderMapper orderMapper;
    private final PaymentWebhookEventMapper webhookMapper;
    private final PurchasePaymentEventMapper paymentEventMapper;
    private final PurchaseReversalMapper reversalMapper;
    private final PaymentReconciliationLineMapper reconciliationLineMapper;

    @Override
    public ReconciliationPlatformSnapshot snapshot(String tenantId, String providerCode, PaymentReconciliationLine line) {
        List<PaymentSession> candidates = sessionMapper.selectReconciliationCandidates(
            tenantId, providerCode, line.getProviderSessionNo());
        PaymentSession session = candidates.size() == 1 ? candidates.get(0) : null;
        PurchaseOrder order = session == null ? null : orderMapper.selectByOrderNo(tenantId, session.getPurchaseOrderNo());
        PaymentWebhookEvent webhook = webhookMapper.selectByProviderEventId(tenantId, providerCode, line.getProviderRecordId());
        PurchasePaymentEvent paymentEvent = latest(paymentEventMapper.selectByPurchaseOrderNo(
            tenantId, session == null ? line.getPurchaseOrderNo() : session.getPurchaseOrderNo()));
        PurchaseReversal reversal = reversalMapper.selectByPurchaseOrderNo(
            tenantId, session == null ? line.getPurchaseOrderNo() : session.getPurchaseOrderNo());
        boolean duplicate = !reconciliationLineMapper.selectProviderIdsFromOtherCompletedBatches(
            tenantId, line.getBatchId(), List.of(line.getProviderRecordId())).isEmpty();
        return compose(candidates.size(), session, order, webhook, paymentEvent, reversal, duplicate);
    }

    @Override
    public Map<Long, ReconciliationPlatformSnapshot> prefetch(String tenantId, String providerCode,
        List<PaymentReconciliationLine> lines) {
        Set<String> sessionNos = values(lines, PaymentReconciliationLine::getProviderSessionNo);
        Set<String> providerIds = values(lines, PaymentReconciliationLine::getProviderRecordId);
        Map<String, List<PaymentSession>> sessions = sessionNos.isEmpty() ? Map.of()
            : sessionMapper.selectReconciliationCandidatesBatch(tenantId, providerCode, List.copyOf(sessionNos)).stream()
                .collect(Collectors.groupingBy(PaymentSession::getProviderSessionNo, LinkedHashMap::new, Collectors.toList()));
        Set<String> orderNos = values(lines, PaymentReconciliationLine::getPurchaseOrderNo);
        sessions.values().stream().flatMap(List::stream).map(PaymentSession::getPurchaseOrderNo)
            .filter(v -> v != null && !v.isBlank()).forEach(orderNos::add);
        Map<String, PurchaseOrder> orders = orderNos.isEmpty() ? Map.of() : orderMapper
            .selectReconciliationByOrderNos(tenantId, List.copyOf(orderNos)).stream()
            .collect(Collectors.toMap(PurchaseOrder::getPurchaseOrderNo, Function.identity(), (a,b) -> a));
        Map<String, PaymentWebhookEvent> webhooks = providerIds.isEmpty() ? Map.of() : webhookMapper
            .selectReconciliationByProviderEventIds(tenantId, providerCode, List.copyOf(providerIds)).stream()
            .collect(Collectors.toMap(PaymentWebhookEvent::getProviderEventId, Function.identity(), (a,b) -> a));
        Map<String, PurchasePaymentEvent> events = orderNos.isEmpty() ? Map.of() : paymentEventMapper
            .selectReconciliationLatestCandidates(tenantId, List.copyOf(orderNos)).stream()
            .collect(Collectors.toMap(PurchasePaymentEvent::getPurchaseOrderNo, Function.identity(), (a,b) -> b));
        Map<String, PurchaseReversal> reversals = orderNos.isEmpty() ? Map.of() : reversalMapper
            .selectReconciliationLatestCandidates(tenantId, List.copyOf(orderNos)).stream()
            .collect(Collectors.toMap(PurchaseReversal::getPurchaseOrderNo, Function.identity(), (a,b) -> a));
        Long batchId = lines.get(0).getBatchId();
        Set<String> duplicateProviderIds = providerIds.isEmpty() ? Set.of() : Set.copyOf(reconciliationLineMapper
            .selectProviderIdsFromOtherCompletedBatches(tenantId, batchId, List.copyOf(providerIds)));
        Map<Long, ReconciliationPlatformSnapshot> result = new LinkedHashMap<>();
        for (PaymentReconciliationLine line : lines) {
            List<PaymentSession> candidates = sessions.getOrDefault(line.getProviderSessionNo(), List.of());
            PaymentSession session = candidates.size() == 1 ? candidates.get(0) : null;
            String orderNo = session == null ? line.getPurchaseOrderNo() : session.getPurchaseOrderNo();
            result.put(line.getId(), compose(candidates.size(), session, orders.get(orderNo),
                webhooks.get(line.getProviderRecordId()), events.get(orderNo), reversals.get(orderNo),
                duplicateProviderIds.contains(line.getProviderRecordId())));
        }
        return Map.copyOf(result);
    }

    private ReconciliationPlatformSnapshot compose(int candidateCount, PaymentSession session, PurchaseOrder order,
        PaymentWebhookEvent webhook, PurchasePaymentEvent paymentEvent, PurchaseReversal reversal,
        boolean duplicatePriorStatementEvidence) {
        return new ReconciliationPlatformSnapshot(candidateCount, session == null ? null : session.getPurchaseOrderNo(),
            session == null ? null : session.getPayCurrencyCode(), session == null ? null : session.getPayAmount(),
            webhook != null, webhook == null ? null : webhook.getEventType(),
            paymentEvent == null ? null : paymentEvent.getEventType(), session == null ? null : session.getStatus(),
            order == null ? null : order.getStatus(), paymentEvent == null ? null : paymentEvent.getEventStatus(),
            reversal == null ? null : reversal.getStatus(), reversal == null ? null : reversal.getReversalType(),
            reversal == null ? null : reversal.getDispositionStatus(), duplicatePriorStatementEvidence, true,
            session == null ? null : session.getId(), order == null ? null : order.getId(),
            webhook == null ? null : webhook.getId(), reversal == null ? null : reversal.getId());
    }

    private <T> Set<String> values(List<T> source, Function<T, String> getter) {
        return source.stream().map(getter).filter(v -> v != null && !v.isBlank())
            .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
    }

    @Override
    public void forEachMissingProviderEventPage(String tenantId, Long batchId,
        String providerCode, Instant windowStart, Instant windowNext,
        Consumer<List<ReconciliationPlatformEventProjection>> consumer) {
        ReconciliationPlatformEventPager.forEachPage((cursorTime, cursorId, limit) ->
            webhookMapper.selectReconciliationStatementEvents(tenantId, batchId, providerCode, windowStart, windowNext,
                cursorTime, cursorId, limit), PAGE_SIZE, consumer);
    }

    private PurchasePaymentEvent latest(List<PurchasePaymentEvent> events) {
        return events == null || events.isEmpty() ? null : events.get(events.size() - 1);
    }
}
