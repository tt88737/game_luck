package com.gameluck.wallet.service.impl;

import com.gameluck.common.core.exception.ServiceException;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WalletManualAdjustServiceImplTest {

    @Test
    @Tag("local")
    void afterTurnoverRequiresPositiveRequiredTurnover() {
        IWalletCoreService walletCoreService = mock(IWalletCoreService.class);
        WalletManualAdjustServiceImpl service = new WalletManualAdjustServiceImpl(walletCoreService);
        WalletManualAdjustBo bo = manualAdjustBo("AFTER_TURNOVER", BigDecimal.ZERO);

        assertThrows(ServiceException.class, () -> service.adjust(bo));

        verify(walletCoreService, never()).credit(any());
    }

    @Test
    @Tag("local")
    void immediateBuildsCreditBoWithImmediateReleaseAndZeroTurnover() {
        IWalletCoreService walletCoreService = mock(IWalletCoreService.class);
        WalletTransaction expected = new WalletTransaction();
        when(walletCoreService.credit(any())).thenReturn(expected);
        WalletManualAdjustServiceImpl service = new WalletManualAdjustServiceImpl(walletCoreService);

        WalletTransaction actual = service.adjust(manualAdjustBo("IMMEDIATE", new BigDecimal("99")));

        assertSame(expected, actual);
        WalletCreditBo creditBo = capturedCreditBo(walletCoreService);
        assertCommonCreditBo(creditBo);
        assertEquals(WalletReleaseMode.IMMEDIATE.name(), creditBo.getReleaseMode());
        assertEquals(0, BigDecimal.ZERO.compareTo(creditBo.getRequiredTurnover()));
    }

    @Test
    @Tag("local")
    void manualReviewBuildsCreditBoWithManualReviewReleaseAndZeroTurnover() {
        IWalletCoreService walletCoreService = mock(IWalletCoreService.class);
        WalletTransaction expected = new WalletTransaction();
        when(walletCoreService.credit(any())).thenReturn(expected);
        WalletManualAdjustServiceImpl service = new WalletManualAdjustServiceImpl(walletCoreService);

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
        WalletManualAdjustServiceImpl service = new WalletManualAdjustServiceImpl(walletCoreService);

        service.adjust(manualAdjustBo("AFTER_TURNOVER", new BigDecimal("25")));

        WalletCreditBo creditBo = capturedCreditBo(walletCoreService);
        assertCommonCreditBo(creditBo);
        assertEquals(WalletReleaseMode.AFTER_TURNOVER.name(), creditBo.getReleaseMode());
        assertEquals(0, new BigDecimal("25").compareTo(creditBo.getRequiredTurnover()));
    }

    private static WalletManualAdjustBo manualAdjustBo(String strategy, BigDecimal requiredTurnover) {
        WalletManualAdjustBo bo = new WalletManualAdjustBo();
        bo.setMemberId(1001L);
        bo.setCurrencyCode("SC");
        bo.setAmount(new BigDecimal("10"));
        bo.setStrategy(strategy);
        bo.setRequiredTurnover(requiredTurnover);
        bo.setOperatorId(9001L);
        bo.setReason("ops adjustment");
        return bo;
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
        assertNotNull(creditBo.getBusinessNo());
        assertTrue(creditBo.getBusinessNo().startsWith("MA"));
        assertEquals("manual-adjust:" + creditBo.getBusinessNo(), creditBo.getIdempotencyKey());
        assertEquals(9001L, creditBo.getOperatorId());
        assertEquals("ops adjustment", creditBo.getRemark());
    }
}
