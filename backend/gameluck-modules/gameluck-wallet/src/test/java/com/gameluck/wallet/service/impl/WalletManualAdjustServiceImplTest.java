package com.gameluck.wallet.service.impl;

import com.gameluck.common.core.exception.ServiceException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gameluck.wallet.domain.WalletTransaction;
import com.gameluck.wallet.domain.bo.WalletCreditBo;
import com.gameluck.wallet.domain.bo.WalletManualAdjustBo;
import com.gameluck.wallet.enums.WalletReleaseMode;
import com.gameluck.wallet.service.IWalletCoreService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WalletManualAdjustServiceImplTest {

    @Test
    @Tag("local")
    void creditBoIgnoresManualAdjustOverrideFromJson() throws Exception {
        String json = """
            {
              "idempotencyKey": "manual-adjust:ADJ-20260712-0001",
              "memberId": 1001,
              "currencyCode": "SC",
              "sourceType": "MANUAL_ADJUST",
              "businessNo": "ADJ-20260712-0001",
              "amount": 10,
              "releaseMode": "IMMEDIATE",
              "manualAdjustOverride": true
            }
            """;

        WalletCreditBo creditBo = new ObjectMapper().readValue(json, WalletCreditBo.class);

        assertFalse(Boolean.TRUE.equals(creditBo.getManualAdjustOverride()));
    }

    @Test
    @Tag("local")
    void afterTurnoverRequiresPositiveRequiredTurnover() {
        IWalletCoreService walletCoreService = mock(IWalletCoreService.class);
        WalletManualAdjustServiceImpl service = manualAdjustService(walletCoreService);
        WalletManualAdjustBo bo = manualAdjustBo("AFTER_TURNOVER", BigDecimal.ZERO);

        assertThrows(ServiceException.class, () -> service.adjust(bo));

        verify(walletCoreService, never()).credit(any());
    }

    @Test
    @Tag("local")
    void afterTurnoverRejectsTurnoverRoundedToZero() {
        IWalletCoreService walletCoreService = mock(IWalletCoreService.class);
        WalletManualAdjustServiceImpl service = manualAdjustService(walletCoreService);
        WalletManualAdjustBo bo = manualAdjustBo("AFTER_TURNOVER", new BigDecimal("0.0000004"));

        assertThrows(ServiceException.class, () -> service.adjust(bo));

        verify(walletCoreService, never()).credit(any());
    }

    @Test
    @Tag("local")
    void immediateBuildsCreditBoWithImmediateReleaseAndZeroTurnover() {
        IWalletCoreService walletCoreService = mock(IWalletCoreService.class);
        WalletTransaction expected = new WalletTransaction();
        when(walletCoreService.credit(any())).thenReturn(expected);
        WalletManualAdjustServiceImpl service = manualAdjustService(walletCoreService);

        WalletTransaction actual = service.adjust(manualAdjustBo("IMMEDIATE", new BigDecimal("99")));

        assertSame(expected, actual);
        WalletCreditBo creditBo = capturedCreditBo(walletCoreService);
        assertCommonCreditBo(creditBo);
        assertEquals(WalletReleaseMode.IMMEDIATE.name(), creditBo.getReleaseMode());
        assertEquals(0, BigDecimal.ZERO.compareTo(creditBo.getRequiredTurnover()));
    }

    @Test
    @Tag("local")
    void blankStrategyBuildsCreditBoWithoutReleaseModeOrTurnover() {
        IWalletCoreService walletCoreService = mock(IWalletCoreService.class);
        when(walletCoreService.credit(any())).thenReturn(new WalletTransaction());
        WalletManualAdjustServiceImpl service = manualAdjustService(walletCoreService);

        service.adjust(manualAdjustBo(" ", new BigDecimal("25")));

        WalletCreditBo creditBo = capturedCreditBo(walletCoreService);
        assertCommonCreditBo(creditBo);
        assertNull(creditBo.getReleaseMode());
        assertNull(creditBo.getRequiredTurnover());
    }

    @Test
    @Tag("local")
    void manualReviewBuildsCreditBoWithManualReviewReleaseAndZeroTurnover() {
        IWalletCoreService walletCoreService = mock(IWalletCoreService.class);
        WalletTransaction expected = new WalletTransaction();
        when(walletCoreService.credit(any())).thenReturn(expected);
        WalletManualAdjustServiceImpl service = manualAdjustService(walletCoreService);

        WalletTransaction actual = service.adjust(manualAdjustBo("MANUAL_REVIEW", new BigDecimal("99")));

        assertSame(expected, actual);
        WalletCreditBo creditBo = capturedCreditBo(walletCoreService);
        assertCommonCreditBo(creditBo);
        assertEquals(WalletReleaseMode.MANUAL_REVIEW.name(), creditBo.getReleaseMode());
        assertEquals(0, BigDecimal.ZERO.compareTo(creditBo.getRequiredTurnover()));
    }

    @Test
    @Tag("local")
    void afterTurnoverBuildsCreditBoWithProvidedTurnover() {
        IWalletCoreService walletCoreService = mock(IWalletCoreService.class);
        when(walletCoreService.credit(any())).thenReturn(new WalletTransaction());
        WalletManualAdjustServiceImpl service = manualAdjustService(walletCoreService);

        service.adjust(manualAdjustBo("AFTER_TURNOVER", new BigDecimal("25")));

        WalletCreditBo creditBo = capturedCreditBo(walletCoreService);
        assertCommonCreditBo(creditBo);
        assertEquals(WalletReleaseMode.AFTER_TURNOVER.name(), creditBo.getReleaseMode());
        assertEquals(0, new BigDecimal("25").compareTo(creditBo.getRequiredTurnover()));
    }

    @Test
    @Tag("local")
    void repeatedAdjustmentNoBuildsStableBusinessNoAndIdempotencyKey() {
        IWalletCoreService walletCoreService = mock(IWalletCoreService.class);
        when(walletCoreService.credit(any())).thenReturn(new WalletTransaction());
        WalletManualAdjustServiceImpl service = manualAdjustService(walletCoreService);

        service.adjust(manualAdjustBo("IMMEDIATE", BigDecimal.ZERO));
        service.adjust(manualAdjustBo("IMMEDIATE", BigDecimal.ZERO));

        ArgumentCaptor<WalletCreditBo> captor = ArgumentCaptor.forClass(WalletCreditBo.class);
        verify(walletCoreService, org.mockito.Mockito.times(2)).credit(captor.capture());
        for (WalletCreditBo creditBo : captor.getAllValues()) {
            assertEquals("ADJ-20260712-0001", creditBo.getBusinessNo());
            assertEquals("manual-adjust:ADJ-20260712-0001", creditBo.getIdempotencyKey());
        }
    }

    private static WalletManualAdjustBo manualAdjustBo(String strategy, BigDecimal requiredTurnover) {
        WalletManualAdjustBo bo = new WalletManualAdjustBo();
        bo.setAdjustmentNo("ADJ-20260712-0001");
        bo.setMemberId(1001L);
        bo.setCurrencyCode("SC");
        bo.setAmount(new BigDecimal("10"));
        bo.setStrategy(strategy);
        bo.setRequiredTurnover(requiredTurnover);
        bo.setReason("ops adjustment");
        return bo;
    }

    private static WalletManualAdjustServiceImpl manualAdjustService(IWalletCoreService walletCoreService) {
        return new WalletManualAdjustServiceImpl(walletCoreService) {
            @Override
            protected Long currentOperatorId() {
                return 9001L;
            }
        };
    }

    private static WalletCreditBo capturedCreditBo(IWalletCoreService walletCoreService) {
        ArgumentCaptor<WalletCreditBo> captor = ArgumentCaptor.forClass(WalletCreditBo.class);
        verify(walletCoreService).credit(captor.capture());
        return captor.getValue();
    }

    private static void assertCommonCreditBo(WalletCreditBo creditBo) {
        assertEquals(1001L, creditBo.getMemberId());
        assertEquals("SC", creditBo.getCurrencyCode());
        assertEquals(0, new BigDecimal("10").compareTo(creditBo.getAmount()));
        assertEquals("MANUAL_ADJUST", creditBo.getSourceType());
        assertEquals("ADJ-20260712-0001", creditBo.getBusinessNo());
        assertEquals("manual-adjust:ADJ-20260712-0001", creditBo.getIdempotencyKey());
        assertEquals(Boolean.TRUE, creditBo.getManualAdjustOverride());
        assertEquals(9001L, creditBo.getOperatorId());
        assertEquals("ops adjustment", creditBo.getRemark());
    }
}
