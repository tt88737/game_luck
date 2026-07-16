package com.gameluck.payment.client.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.gameluck.common.core.client.ClientTokenService;
import com.gameluck.common.core.exception.ServiceException;
import com.gameluck.payment.client.domain.bo.ClientPurchasePayBo;
import com.gameluck.payment.client.domain.vo.ClientPurchaseOfferVo;
import com.gameluck.payment.client.domain.vo.ClientPurchaseOrderVo;
import com.gameluck.payment.domain.PurchaseOffer;
import com.gameluck.payment.domain.PurchaseOfferGrantItem;
import com.gameluck.payment.domain.PurchaseOrder;
import com.gameluck.payment.mapper.PurchaseOfferGrantItemMapper;
import com.gameluck.payment.mapper.PurchaseOfferMapper;
import com.gameluck.payment.mapper.PurchaseOrderGrantSnapshotMapper;
import com.gameluck.payment.mapper.PurchaseOrderMapper;
import com.gameluck.payment.service.impl.PurchaseOfferServiceImpl;
import com.gameluck.wallet.domain.WalletTransaction;
import com.gameluck.wallet.domain.bo.WalletCreditBo;
import com.gameluck.wallet.enums.WalletTransactionStatus;
import com.gameluck.wallet.service.IWalletCoreService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
        ClientPurchaseService service = service(offerMapper, itemMapper, mock(PurchaseOrderMapper.class), mock(IWalletCoreService.class));

        List<ClientPurchaseOfferVo> rows = service.offers();

        assertEquals(1, rows.size());
        assertEquals("Starter Pack", rows.get(0).getOfferName());
        assertEquals(2, rows.get(0).getGrantItems().size());
        assertEquals(new BigDecimal("10.000000"), rows.get(0).getPayAmount());
        assertEquals("SC requires 10x wagering.", rows.get(0).getWageringText());
    }

    @Test
    void simulatedPayCreatesOrderSnapshotsAndCreditsWallet() {
        PurchaseOfferMapper offerMapper = mock(PurchaseOfferMapper.class);
        PurchaseOfferGrantItemMapper itemMapper = mock(PurchaseOfferGrantItemMapper.class);
        PurchaseOrderMapper orderMapper = mock(PurchaseOrderMapper.class);
        IWalletCoreService walletCoreService = mock(IWalletCoreService.class);
        when(offerMapper.selectById(100L)).thenReturn(offer(100L));
        when(itemMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
            grant(100L, "PURCHASE_GRANT", "GC", "10000", "NONE", "0"),
            grant(100L, "PURCHASE_BONUS", "SC", "1", "MULTIPLIER", "10")
        ));
        when(orderMapper.selectByIdempotencyKey("000000", "idem-1")).thenReturn(null);
        when(orderMapper.insert(any(PurchaseOrder.class))).thenReturn(1);
        when(orderMapper.updateById(any(PurchaseOrder.class))).thenReturn(1);
        when(walletCoreService.credit(any())).thenReturn(successTx("WT1"), successTx("WT2"));
        ClientPurchaseService service = service(offerMapper, itemMapper, orderMapper, walletCoreService);

        ClientPurchasePayBo bo = new ClientPurchasePayBo();
        bo.setOfferId(100L);
        bo.setIdempotencyKey("idem-1");
        ClientPurchaseOrderVo result = service.pay("Bearer " + new ClientTokenService().issue(1001L), bo);

        assertEquals("CREDITED", result.getStatus());
        assertEquals(2, result.getGrantItems().size());
        ArgumentCaptor<WalletCreditBo> captor = ArgumentCaptor.forClass(WalletCreditBo.class);
        verify(walletCoreService, times(2)).credit(captor.capture());
        assertEquals("PURCHASE", captor.getAllValues().get(0).getSourceType());
        assertEquals(new BigDecimal("0.000000"), captor.getAllValues().get(0).getTurnoverRequiredAmount());
        assertEquals(new BigDecimal("10.000000"), captor.getAllValues().get(1).getTurnoverRequiredAmount());
    }

    @Test
    void repeatedIdempotencyKeyReturnsExistingOrderWithoutCreditingAgain() {
        PurchaseOrder existing = new PurchaseOrder();
        existing.setId(1L);
        existing.setPurchaseOrderNo("PO202607160001");
        existing.setOfferId(100L);
        existing.setOfferNo("PO-STARTER");
        existing.setMemberId(1001L);
        existing.setPayCurrencyCode("USD");
        existing.setPayAmount(new BigDecimal("10.000000"));
        existing.setStatus("CREDITED");
        PurchaseOrderMapper orderMapper = mock(PurchaseOrderMapper.class);
        when(orderMapper.selectByIdempotencyKey("000000", "idem-1")).thenReturn(existing);
        ClientPurchaseService service = service(mock(PurchaseOfferMapper.class), mock(PurchaseOfferGrantItemMapper.class), orderMapper, mock(IWalletCoreService.class));

        ClientPurchasePayBo bo = new ClientPurchasePayBo();
        bo.setOfferId(100L);
        bo.setIdempotencyKey("idem-1");
        ClientPurchaseOrderVo result = service.pay("Bearer " + new ClientTokenService().issue(1001L), bo);

        assertEquals("CREDITED", result.getStatus());
        verify(orderMapper, never()).insert(any(PurchaseOrder.class));
    }

    @Test
    void disabledOfferCannotBePurchased() {
        PurchaseOffer disabled = offer(100L);
        disabled.setStatus("1");
        PurchaseOfferMapper offerMapper = mock(PurchaseOfferMapper.class);
        when(offerMapper.selectById(100L)).thenReturn(disabled);
        ClientPurchaseService service = service(offerMapper, mock(PurchaseOfferGrantItemMapper.class), mock(PurchaseOrderMapper.class), mock(IWalletCoreService.class));
        ClientPurchasePayBo bo = new ClientPurchasePayBo();
        bo.setOfferId(100L);
        bo.setIdempotencyKey("idem-1");

        ServiceException ex = assertThrows(ServiceException.class, () -> service.pay("Bearer " + new ClientTokenService().issue(1001L), bo));

        assertEquals("client.purchase.offer.not.available", ex.getMessage());
    }

    private ClientPurchaseService service(PurchaseOfferMapper offerMapper, PurchaseOfferGrantItemMapper itemMapper,
                                          PurchaseOrderMapper orderMapper, IWalletCoreService walletCoreService) {
        PurchaseOfferServiceImpl purchaseOfferService = new PurchaseOfferServiceImpl(
            offerMapper, itemMapper, mock(PurchaseOrderGrantSnapshotMapper.class));
        return new ClientPurchaseService(new ClientTokenService(), offerMapper, itemMapper, orderMapper, purchaseOfferService, walletCoreService);
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

    private WalletTransaction successTx(String transactionNo) {
        WalletTransaction tx = new WalletTransaction();
        tx.setTransactionNo(transactionNo);
        tx.setStatus(WalletTransactionStatus.SUCCESS.name());
        return tx;
    }
}
