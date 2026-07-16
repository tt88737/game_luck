package com.gameluck.payment.service.impl;

import com.gameluck.payment.domain.PurchaseOffer;
import com.gameluck.payment.domain.PurchaseOfferGrantItem;
import com.gameluck.payment.domain.bo.PurchaseOfferBo;
import com.gameluck.payment.mapper.PurchaseOfferGrantItemMapper;
import com.gameluck.payment.mapper.PurchaseOfferMapper;
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
        when(offerMapper.insert(any(PurchaseOffer.class))).thenReturn(1);
        when(grantItemMapper.insert(any(PurchaseOfferGrantItem.class))).thenReturn(1);
        PurchaseOfferServiceImpl service = new PurchaseOfferServiceImpl(offerMapper, grantItemMapper);

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
}
