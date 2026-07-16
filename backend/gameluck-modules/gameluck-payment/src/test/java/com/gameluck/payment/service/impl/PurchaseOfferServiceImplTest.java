package com.gameluck.payment.service.impl;

import com.gameluck.payment.domain.PurchaseOffer;
import com.gameluck.payment.domain.PurchaseOfferGrantItem;
import com.gameluck.payment.domain.PurchaseOrder;
import com.gameluck.payment.domain.PurchaseOrderGrantSnapshot;
import com.gameluck.payment.domain.bo.PurchaseOfferBo;
import com.gameluck.payment.mapper.PurchaseOfferGrantItemMapper;
import com.gameluck.payment.mapper.PurchaseOfferMapper;
import com.gameluck.payment.mapper.PurchaseOrderGrantSnapshotMapper;
import com.gameluck.wallet.domain.bo.WalletCreditBo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PurchaseOfferServiceImplTest {

    @Test
    @Tag("local")
    void insertStandardGcScOfferStoresGrantItemsWithSystemFundProperties() {
        PurchaseOfferMapper offerMapper = mock(PurchaseOfferMapper.class);
        PurchaseOfferGrantItemMapper grantItemMapper = mock(PurchaseOfferGrantItemMapper.class);
        PurchaseOrderGrantSnapshotMapper snapshotMapper = mock(PurchaseOrderGrantSnapshotMapper.class);
        when(offerMapper.insert(any(PurchaseOffer.class))).thenReturn(1);
        when(grantItemMapper.insert(any(PurchaseOfferGrantItem.class))).thenReturn(1);
        PurchaseOfferServiceImpl service = new PurchaseOfferServiceImpl(offerMapper, grantItemMapper, snapshotMapper);

        PurchaseOfferBo bo = new PurchaseOfferBo();
        bo.setOfferNo("PO-STARTER-10");
        bo.setOfferName("Starter Pack");
        bo.setOfferType("STANDARD");
        bo.setPayCurrencyCode("USD");
        bo.setPayAmount(new BigDecimal("10.000000"));
        bo.setStatus("1");
        bo.setGrantItems(List.of(
            PurchaseOfferBo.grantItem("PURCHASE_GRANT", "GC", new BigDecimal("10000.000000"), "NONE", BigDecimal.ZERO),
            PurchaseOfferBo.grantItem("PURCHASE_BONUS", "SC", new BigDecimal("1.000000"), "MULTIPLIER", new BigDecimal("10"))
        ));

        int rows = service.insertByBo(bo);

        assertEquals(1, rows);
        ArgumentCaptor<PurchaseOfferGrantItem> captor = ArgumentCaptor.forClass(PurchaseOfferGrantItem.class);
        verify(grantItemMapper, times(2)).insert(captor.capture());
        List<PurchaseOfferGrantItem> items = captor.getAllValues();
        assertEquals("PURCHASE_GRANT_GC", items.get(0).getFundPropertyCode());
        assertEquals("NONE", items.get(0).getWageringMode());
        assertEquals("PURCHASE_BONUS_SC", items.get(1).getFundPropertyCode());
        assertEquals("MULTIPLIER", items.get(1).getWageringMode());
        assertEquals(new BigDecimal("10"), items.get(1).getWageringMultiplier());
    }

    @Test
    @Tag("local")
    void paidPurchaseCreatesGrantSnapshotsAndWalletCredits() {
        PurchaseOrderGrantSnapshotMapper snapshotMapper = mock(PurchaseOrderGrantSnapshotMapper.class);
        when(snapshotMapper.insert(any(PurchaseOrderGrantSnapshot.class))).thenReturn(1);
        PurchaseOfferServiceImpl service = new PurchaseOfferServiceImpl(mock(PurchaseOfferMapper.class), mock(PurchaseOfferGrantItemMapper.class), snapshotMapper);
        PurchaseOrder order = new PurchaseOrder();
        order.setId(200L);
        order.setTenantId("000000");
        order.setPurchaseOrderNo("PO202607160001");
        order.setMemberId(1001L);

        PurchaseOfferGrantItem gc = grantItem("PURCHASE_GRANT", "GC", "10000.000000", "PURCHASE_GRANT_GC", "NONE", "0");
        PurchaseOfferGrantItem sc = grantItem("PURCHASE_BONUS", "SC", "1.000000", "PURCHASE_BONUS_SC", "MULTIPLIER", "10");

        List<WalletCreditBo> credits = service.snapshotPaidOrderGrants(order, List.of(gc, sc));

        ArgumentCaptor<PurchaseOrderGrantSnapshot> captor = ArgumentCaptor.forClass(PurchaseOrderGrantSnapshot.class);
        verify(snapshotMapper, times(2)).insert(captor.capture());
        List<PurchaseOrderGrantSnapshot> snapshots = captor.getAllValues();
        assertEquals(new BigDecimal("0.000000"), snapshots.get(0).getRequiredTurnover());
        assertEquals(new BigDecimal("10.000000"), snapshots.get(1).getRequiredTurnover());
        assertEquals("PURCHASE", credits.get(0).getSourceType());
        assertEquals("PO202607160001", credits.get(1).getBusinessNo());
        assertEquals("PURCHASE_BONUS_SC", credits.get(1).getFundPropertyCode());
        assertEquals(new BigDecimal("10.000000"), credits.get(1).getTurnoverRequiredAmount());
        assertEquals(new BigDecimal("10"), credits.get(1).getTurnoverMultiplier());
    }

    private PurchaseOfferGrantItem grantItem(String grantType, String currencyCode, String amount, String propertyCode, String wageringMode, String multiplier) {
        PurchaseOfferGrantItem item = new PurchaseOfferGrantItem();
        item.setGrantType(grantType);
        item.setCurrencyCode(currencyCode);
        item.setGrantAmount(new BigDecimal(amount));
        item.setFundPropertyCode(propertyCode);
        item.setWageringMode(wageringMode);
        item.setWageringMultiplier(new BigDecimal(multiplier));
        item.setWageringRequiredAmount(BigDecimal.ZERO);
        item.setGameScopeType("ALL");
        item.setWageringExpireDays(0);
        return item;
    }
}
