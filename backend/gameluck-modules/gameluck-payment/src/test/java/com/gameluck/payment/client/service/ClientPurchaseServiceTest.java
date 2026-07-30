package com.gameluck.payment.client.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.gameluck.common.core.client.ClientTokenService;
import com.gameluck.common.core.exception.ServiceException;
import com.gameluck.member.compliance.MemberComplianceAction;
import com.gameluck.member.compliance.MemberComplianceContext;
import com.gameluck.member.compliance.MemberComplianceDecision;
import com.gameluck.member.compliance.MemberComplianceReason;
import com.gameluck.member.domain.MemberProfile;
import com.gameluck.member.mapper.MemberProfileMapper;
import com.gameluck.member.service.IMemberComplianceGateService;
import com.gameluck.payment.client.domain.bo.ClientPurchasePayBo;
import com.gameluck.payment.client.domain.vo.ClientPurchaseOfferVo;
import com.gameluck.payment.client.domain.vo.ClientPurchaseOrderVo;
import com.gameluck.payment.domain.PurchaseOffer;
import com.gameluck.payment.domain.PurchaseOfferGrantItem;
import com.gameluck.payment.domain.PurchaseOrder;
import com.gameluck.payment.domain.bo.PurchasePaymentCallbackBo;
import com.gameluck.payment.mapper.PurchaseOfferGrantItemMapper;
import com.gameluck.payment.mapper.PurchaseOfferMapper;
import com.gameluck.payment.mapper.PurchaseOrderGrantSnapshotMapper;
import com.gameluck.payment.mapper.PurchaseOrderMapper;
import com.gameluck.payment.service.IPurchasePaymentEventService;
import com.gameluck.payment.service.impl.PurchaseOfferServiceImpl;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;
import java.util.List;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("local")
class ClientPurchaseServiceTest {

    @Test
    void enabledOffersExposeBusinessFieldsWithoutFundPropertyCode() {
        PurchaseOfferMapper offerMapper = mock(PurchaseOfferMapper.class);
        PurchaseOfferGrantItemMapper itemMapper = mock(PurchaseOfferGrantItemMapper.class);
        when(offerMapper.selectList(any(Wrapper.class))).thenReturn(List.of(offer(100L)));
        when(itemMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
            grant(100L, "PURCHASE_GRANT", "GC", "10000", "NONE", "0"),
            grant(100L, "PURCHASE_BONUS", "SC", "1", "MULTIPLIER", "10")
        ));
        ClientPurchaseService service = service(offerMapper, itemMapper, mock(PurchaseOrderMapper.class), mock(IPurchasePaymentEventService.class));

        List<ClientPurchaseOfferVo> rows = service.offers();

        assertEquals(1, rows.size());
        assertEquals("Starter Pack", rows.get(0).getOfferName());
        assertEquals(2, rows.get(0).getGrantItems().size());
        assertEquals(new BigDecimal("10.000000"), rows.get(0).getPayAmount());
        assertEquals("SC requires 10x wagering.", rows.get(0).getWageringText());
    }

    @Test
    void payCreatesPendingOrderWithoutStartingPaymentOrCreditingWallet() {
        PurchaseOfferMapper offerMapper = mock(PurchaseOfferMapper.class);
        PurchaseOfferGrantItemMapper itemMapper = mock(PurchaseOfferGrantItemMapper.class);
        PurchaseOrderMapper orderMapper = mock(PurchaseOrderMapper.class);
        IPurchasePaymentEventService paymentEventService = mock(IPurchasePaymentEventService.class);
        MemberProfileMapper memberMapper = mock(MemberProfileMapper.class);
        IMemberComplianceGateService gate = allowingGate();
        when(offerMapper.selectById(100L)).thenReturn(offer(100L));
        when(itemMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
            grant(100L, "PURCHASE_GRANT", "GC", "10000", "NONE", "0"),
            grant(100L, "PURCHASE_BONUS", "SC", "1", "MULTIPLIER", "10")
        ));
        when(orderMapper.selectByIdempotencyKey("000000", "idem-1")).thenReturn(null);
        when(orderMapper.insert(any(PurchaseOrder.class))).thenReturn(1);
        when(memberMapper.selectById(1001L)).thenReturn(activeMember());
        ClientPurchaseService service = service(offerMapper, itemMapper, orderMapper, paymentEventService, memberMapper, gate);

        ClientPurchasePayBo bo = new ClientPurchasePayBo();
        bo.setOfferId(100L);
        bo.setIdempotencyKey("idem-1");
        ClientPurchaseOrderVo result = service.pay("Bearer " + new ClientTokenService().issue(1001L), bo);

        assertEquals("PENDING", result.getStatus());
        assertEquals(2, result.getGrantItems().size());
        assertEquals(new BigDecimal("10.0000"), result.getGrantItems().get(1).getWageringMultiplier());
        ArgumentCaptor<PurchaseOrder> orderCaptor = ArgumentCaptor.forClass(PurchaseOrder.class);
        verify(orderMapper).insert(orderCaptor.capture());
        PurchaseOrder inserted = orderCaptor.getValue();
        assertEquals(null, inserted.getProviderCode());
        assertEquals(null, inserted.getProviderOrderNo());
        assertEquals(null, inserted.getPaymentSessionNo());
        verify(paymentEventService, never()).applyEvent(any(PurchasePaymentCallbackBo.class));
        ArgumentCaptor<MemberComplianceContext> contextCaptor = ArgumentCaptor.forClass(MemberComplianceContext.class);
        verify(gate).evaluate(contextCaptor.capture());
        MemberComplianceContext context = contextCaptor.getValue();
        assertEquals(MemberComplianceAction.PURCHASE_PAY, context.getAction());
        assertEquals("000000", context.getTenantId());
        assertEquals(1001L, context.getMember().getId());
        assertEquals("USD", context.getCurrencyCode());
        assertEquals("h5", context.getChannel());
    }

    @Test
    void repeatedIdempotencyKeyReturnsExistingOrderWithoutCreditingAgain() {
        PurchaseOrder existing = new PurchaseOrder();
        existing.setId(1L);
        existing.setTenantId("000000");
        existing.setPurchaseOrderNo("PO202607160001");
        existing.setOfferId(100L);
        existing.setOfferNo("PO-STARTER");
        existing.setOfferNameSnapshot("Starter Pack");
        existing.setMemberId(1001L);
        existing.setPayCurrencyCode("USD");
        existing.setPayAmount(new BigDecimal("10.000000"));
        existing.setStatus("CREDITED");
        PurchaseOrderMapper orderMapper = mock(PurchaseOrderMapper.class);
        MemberProfileMapper memberMapper = mock(MemberProfileMapper.class);
        IMemberComplianceGateService gate = mock(IMemberComplianceGateService.class);
        when(orderMapper.selectByIdempotencyKey("000000", "idem-1")).thenReturn(existing);
        PurchaseOfferMapper offerMapper = mock(PurchaseOfferMapper.class);
        PurchaseOfferGrantItemMapper itemMapper = mock(PurchaseOfferGrantItemMapper.class);
        when(offerMapper.selectById(100L)).thenReturn(offer(100L));
        when(itemMapper.selectList(any(Wrapper.class))).thenReturn(List.of(grant(100L, "PURCHASE_GRANT", "GC", "10000", "NONE", "0")));
        ClientPurchaseService service = service(offerMapper, itemMapper,
            orderMapper, mock(IPurchasePaymentEventService.class), memberMapper, gate);

        ClientPurchasePayBo bo = new ClientPurchasePayBo();
        bo.setOfferId(100L);
        bo.setIdempotencyKey("idem-1");
        ClientPurchaseOrderVo result = service.pay("Bearer " + new ClientTokenService().issue(1001L), bo);

        assertEquals("CREDITED", result.getStatus());
        assertEquals("Starter Pack", result.getOfferName());
        assertEquals(1, result.getGrantItems().size());
        assertEquals(new BigDecimal("10.0000"), result.getGrantItems().get(0).getWageringMultiplier());
        verify(orderMapper, never()).insert(any(PurchaseOrder.class));
        verify(memberMapper, never()).selectById(any(Long.class));
        verify(gate, never()).evaluate(any(MemberComplianceContext.class));
    }

    @Test
    void duplicateOrderInsertReturnsConcurrentWinner() {
        PurchaseOfferMapper offers = mock(PurchaseOfferMapper.class);
        PurchaseOfferGrantItemMapper items = mock(PurchaseOfferGrantItemMapper.class);
        PurchaseOrderMapper orders = mock(PurchaseOrderMapper.class);
        when(offers.selectById(100L)).thenReturn(offer(100L));
        when(items.selectList(any(Wrapper.class))).thenReturn(List.of(grant(100L, "PURCHASE_GRANT", "GC", "10000", "NONE", "0")));
        when(orders.selectByIdempotencyKey("000000", "idem-race")).thenReturn(null);
        when(orders.selectByIdempotencyKeyForUpdate("000000", "idem-race")).thenReturn(orderWinner());
        when(orders.insert(any(PurchaseOrder.class))).thenThrow(new DuplicateKeyException("race"));
        ClientPurchaseOrderVo result = service(offers, items, orders, mock(IPurchasePaymentEventService.class))
            .pay("Bearer " + new ClientTokenService().issue(1001L), payBo(100L, "idem-race"));
        assertEquals("PO-WINNER", result.getOrderNo());
        assertEquals("Starter Pack", result.getOfferName());
        assertEquals(1, result.getGrantItems().size());
        verify(orders).selectByIdempotencyKeyForUpdate("000000", "idem-race");
    }

    @Test
    void missingMemberDecisionBlocksPurchaseBeforeSideEffects() {
        purchaseDeniedBeforeSideEffects(MemberComplianceReason.MEMBER_NOT_EXISTS,
            "client.purchase.member.not.exists", "idem-missing");
    }

    @Test
    void inactiveMemberDecisionBlocksPurchaseBeforeSideEffects() {
        purchaseDeniedBeforeSideEffects(MemberComplianceReason.MEMBER_INACTIVE,
            "client.purchase.member.inactive", "idem-inactive");
    }

    @Test
    void highRiskDecisionBlocksPurchaseBeforeSideEffects() {
        purchaseDeniedBeforeSideEffects(MemberComplianceReason.RISK_BLOCKED,
            "client.purchase.risk.blocked", "idem-risk");
    }

    @Test
    void disabledOfferCannotBePurchased() {
        PurchaseOffer disabled = offer(100L);
        disabled.setStatus("1");
        PurchaseOfferMapper offerMapper = mock(PurchaseOfferMapper.class);
        when(offerMapper.selectById(100L)).thenReturn(disabled);
        ClientPurchaseService service = service(offerMapper, mock(PurchaseOfferGrantItemMapper.class), mock(PurchaseOrderMapper.class), mock(IPurchasePaymentEventService.class));
        ClientPurchasePayBo bo = new ClientPurchasePayBo();
        bo.setOfferId(100L);
        bo.setIdempotencyKey("idem-1");

        ServiceException ex = assertThrows(ServiceException.class, () -> service.pay("Bearer " + new ClientTokenService().issue(1001L), bo));

        assertEquals("client.purchase.offer.not.available", ex.getMessage());
    }

    @Test
    void firstOnlyOfferRejectsMembersWithAnyCreditedPurchase() {
        PurchaseOffer firstOnly = offer(100L);
        firstOnly.setPurchaseLimitType("FIRST_ONLY");
        PurchaseOfferMapper offerMapper = mock(PurchaseOfferMapper.class);
        PurchaseOrderMapper orderMapper = mock(PurchaseOrderMapper.class);
        when(offerMapper.selectById(100L)).thenReturn(firstOnly);
        when(orderMapper.selectByIdempotencyKey("000000", "idem-first")).thenReturn(null);
        when(orderMapper.countCreditedByMember("000000", 1001L)).thenReturn(1L);
        ClientPurchaseService service = service(offerMapper, mock(PurchaseOfferGrantItemMapper.class), orderMapper, mock(IPurchasePaymentEventService.class));

        ServiceException ex = assertThrows(ServiceException.class,
            () -> service.pay("Bearer " + new ClientTokenService().issue(1001L), payBo(100L, "idem-first")));

        assertEquals("client.purchase.limit.first.only", ex.getMessage());
        verify(orderMapper, never()).insert(any(PurchaseOrder.class));
    }

    @Test
    void totalOnceOfferRejectsSecondCreditedPurchaseForSameMemberAndOffer() {
        PurchaseOffer totalOnce = offer(100L);
        totalOnce.setPurchaseLimitType("TOTAL_ONCE");
        PurchaseOfferMapper offerMapper = mock(PurchaseOfferMapper.class);
        PurchaseOrderMapper orderMapper = mock(PurchaseOrderMapper.class);
        when(offerMapper.selectById(100L)).thenReturn(totalOnce);
        when(orderMapper.selectByIdempotencyKey("000000", "idem-total")).thenReturn(null);
        when(orderMapper.countCreditedByMemberAndOffer("000000", 1001L, 100L)).thenReturn(1L);
        ClientPurchaseService service = service(offerMapper, mock(PurchaseOfferGrantItemMapper.class), orderMapper, mock(IPurchasePaymentEventService.class));

        ServiceException ex = assertThrows(ServiceException.class,
            () -> service.pay("Bearer " + new ClientTokenService().issue(1001L), payBo(100L, "idem-total")));

        assertEquals("client.purchase.limit.total.once", ex.getMessage());
        verify(orderMapper, never()).insert(any(PurchaseOrder.class));
    }

    @Test
    void dailyOnceOfferRejectsSecondCreditedPurchaseForSameMemberOfferAndDay() {
        PurchaseOffer dailyOnce = offer(100L);
        dailyOnce.setPurchaseLimitType("DAILY_ONCE");
        PurchaseOfferMapper offerMapper = mock(PurchaseOfferMapper.class);
        PurchaseOrderMapper orderMapper = mock(PurchaseOrderMapper.class);
        when(offerMapper.selectById(100L)).thenReturn(dailyOnce);
        when(orderMapper.selectByIdempotencyKey("000000", "idem-daily")).thenReturn(null);
        when(orderMapper.countCreditedByMemberOfferAndCreditedTimeRange(any(String.class), any(Long.class), any(Long.class), any(), any()))
            .thenReturn(1L);
        ClientPurchaseService service = service(offerMapper, mock(PurchaseOfferGrantItemMapper.class), orderMapper, mock(IPurchasePaymentEventService.class));

        ServiceException ex = assertThrows(ServiceException.class,
            () -> service.pay("Bearer " + new ClientTokenService().issue(1001L), payBo(100L, "idem-daily")));

        assertEquals("client.purchase.limit.daily.once", ex.getMessage());
        verify(orderMapper, never()).insert(any(PurchaseOrder.class));
    }

    @Test
    void unsupportedPeriodLimitDoesNotSilentlyBypassEnforcement() {
        PurchaseOffer periodLimit = offer(100L);
        periodLimit.setPurchaseLimitType("PERIOD_LIMIT");
        PurchaseOfferMapper offerMapper = mock(PurchaseOfferMapper.class);
        PurchaseOrderMapper orderMapper = mock(PurchaseOrderMapper.class);
        when(offerMapper.selectById(100L)).thenReturn(periodLimit);
        when(orderMapper.selectByIdempotencyKey("000000", "idem-period")).thenReturn(null);
        ClientPurchaseService service = service(offerMapper, mock(PurchaseOfferGrantItemMapper.class), orderMapper, mock(IPurchasePaymentEventService.class));

        ServiceException ex = assertThrows(ServiceException.class,
            () -> service.pay("Bearer " + new ClientTokenService().issue(1001L), payBo(100L, "idem-period")));

        assertEquals("client.purchase.limit.unsupported", ex.getMessage());
        verify(orderMapper, never()).insert(any(PurchaseOrder.class));
    }

    private ClientPurchaseService service(PurchaseOfferMapper offerMapper, PurchaseOfferGrantItemMapper itemMapper,
                                          PurchaseOrderMapper orderMapper, IPurchasePaymentEventService paymentEventService) {
        return service(offerMapper, itemMapper, orderMapper, paymentEventService,
            mock(MemberProfileMapper.class), allowingGate());
    }

    private ClientPurchaseService service(PurchaseOfferMapper offerMapper, PurchaseOfferGrantItemMapper itemMapper,
                                          PurchaseOrderMapper orderMapper, IPurchasePaymentEventService paymentEventService,
                                          MemberProfileMapper memberProfileMapper,
                                          IMemberComplianceGateService complianceGateService) {
        PurchaseOfferServiceImpl purchaseOfferService = new PurchaseOfferServiceImpl(
            offerMapper, itemMapper, snapshotMapper());
        return new ClientPurchaseService(new ClientTokenService(), offerMapper, itemMapper, orderMapper,
            purchaseOfferService, memberProfileMapper, complianceGateService);
    }

    private PurchaseOrderGrantSnapshotMapper snapshotMapper() {
        PurchaseOrderGrantSnapshotMapper mapper = mock(PurchaseOrderGrantSnapshotMapper.class);
        List<com.gameluck.payment.domain.PurchaseOrderGrantSnapshot> stored = new ArrayList<>();
        org.mockito.Mockito.doAnswer(invocation -> {
            stored.add(invocation.getArgument(0));
            return 1;
        }).when(mapper).insert(any(com.gameluck.payment.domain.PurchaseOrderGrantSnapshot.class));
        org.mockito.stubbing.Answer<List<com.gameluck.payment.domain.PurchaseOrderGrantSnapshot>> snapshots = invocation -> {
            if (!stored.isEmpty()) return List.copyOf(stored);
            com.gameluck.payment.domain.PurchaseOrderGrantSnapshot snapshot = new com.gameluck.payment.domain.PurchaseOrderGrantSnapshot();
            snapshot.setGrantType("PURCHASE_BONUS"); snapshot.setCurrencyCode("SC");
            snapshot.setGrantAmount(new BigDecimal("1.000000")); snapshot.setWageringMode("MULTIPLIER");
            snapshot.setWageringMultiplier(new BigDecimal("10.0000"));
            snapshot.setRequiredTurnover(new BigDecimal("10.000000")); snapshot.setGameScopeType("ALL");
            return List.of(snapshot);
        };
        when(mapper.selectByPurchaseOrderNo(any(String.class), any(String.class))).thenAnswer(snapshots);
        when(mapper.selectByPurchaseOrderNoForUpdate(any(String.class), any(String.class))).thenAnswer(snapshots);
        return mapper;
    }

    private void purchaseDeniedBeforeSideEffects(MemberComplianceReason reason, String messageKey, String idempotencyKey) {
        PurchaseOfferMapper offerMapper = mock(PurchaseOfferMapper.class);
        PurchaseOfferGrantItemMapper itemMapper = mock(PurchaseOfferGrantItemMapper.class);
        PurchaseOrderMapper orderMapper = mock(PurchaseOrderMapper.class);
        IPurchasePaymentEventService eventService = mock(IPurchasePaymentEventService.class);
        MemberProfileMapper memberMapper = mock(MemberProfileMapper.class);
        IMemberComplianceGateService gate = mock(IMemberComplianceGateService.class);
        when(orderMapper.selectByIdempotencyKey("000000", idempotencyKey)).thenReturn(null);
        when(offerMapper.selectById(100L)).thenReturn(offer(100L));
        when(memberMapper.selectById(1001L)).thenReturn(activeMember());
        when(gate.evaluate(any(MemberComplianceContext.class))).thenReturn(MemberComplianceDecision.builder()
            .allowed(false)
            .action(MemberComplianceAction.PURCHASE_PAY.name())
            .reasonCode(reason.name())
            .messageKey(messageKey)
            .build());
        ClientPurchaseService service = service(
            offerMapper, itemMapper, orderMapper, eventService, memberMapper, gate);

        ServiceException ex = assertThrows(ServiceException.class,
            () -> service.pay("Bearer " + new ClientTokenService().issue(1001L), payBo(100L, idempotencyKey)));

        assertEquals(messageKey, ex.getMessage());
        verify(orderMapper, never()).insert(any(PurchaseOrder.class));
        verify(eventService, never()).applyEvent(any(PurchasePaymentCallbackBo.class));
        verify(itemMapper, never()).selectList(any(Wrapper.class));
    }

    private IMemberComplianceGateService allowingGate() {
        IMemberComplianceGateService gate = mock(IMemberComplianceGateService.class);
        when(gate.evaluate(any(MemberComplianceContext.class))).thenAnswer(invocation ->
            MemberComplianceDecision.allow(invocation.getArgument(0), "USD", "US", "CA", "h5"));
        return gate;
    }

    private MemberProfile activeMember() {
        MemberProfile member = new MemberProfile();
        member.setId(1001L);
        member.setTenantId("000000");
        member.setStatus("ACTIVE");
        member.setRiskLevel("NORMAL");
        return member;
    }

    private PurchaseOffer offer(Long id) {
        PurchaseOffer offer = new PurchaseOffer();
        offer.setId(id);
        offer.setTenantId("000000");
        offer.setOfferNo("PO-STARTER");
        offer.setOfferName("Starter Pack");
        offer.setOfferType("STANDARD");
        offer.setPayCurrencyCode("USD");
        offer.setPayAmount(new BigDecimal("10.000000"));
        offer.setPurchaseLimitType("NONE");
        offer.setStatus("0");
        return offer;
    }

    private PurchaseOfferGrantItem grant(Long offerId, String grantType, String currencyCode, String amount, String wageringMode, String multiplier) {
        PurchaseOfferGrantItem item = new PurchaseOfferGrantItem();
        item.setOfferId(offerId);
        item.setGrantType(grantType);
        item.setCurrencyCode(currencyCode);
        item.setGrantAmount(new BigDecimal(amount + ".000000"));
        item.setFundPropertyCode(grantType + "_" + currencyCode);
        item.setWageringMode(wageringMode);
        item.setWageringRequiredAmount(BigDecimal.ZERO.setScale(6));
        item.setWageringMultiplier(new BigDecimal(multiplier + ".0000"));
        item.setGameScopeType("ALL");
        return item;
    }

    private ClientPurchasePayBo payBo(Long offerId, String idempotencyKey) {
        ClientPurchasePayBo bo = new ClientPurchasePayBo();
        bo.setOfferId(offerId);
        bo.setIdempotencyKey(idempotencyKey);
        return bo;
    }

    private PurchaseOrder orderWinner() {
        PurchaseOrder order = new PurchaseOrder();
        order.setId(9L); order.setTenantId("000000"); order.setPurchaseOrderNo("PO-WINNER");
        order.setOfferId(100L); order.setOfferNo("PO-STARTER"); order.setMemberId(1001L);
        order.setOfferNameSnapshot("Starter Pack");
        order.setPayCurrencyCode("USD"); order.setPayAmount(new BigDecimal("10.000000")); order.setStatus("PENDING");
        return order;
    }

}
