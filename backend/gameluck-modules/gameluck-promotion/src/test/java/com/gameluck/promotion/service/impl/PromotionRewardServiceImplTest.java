package com.gameluck.promotion.service.impl;

import com.gameluck.common.core.exception.ServiceException;
import com.gameluck.promotion.client.domain.vo.ClientDailyLoginRewardVo;
import com.gameluck.promotion.domain.PromotionClaim;
import com.gameluck.promotion.domain.PromotionReward;
import com.gameluck.promotion.domain.bo.PromotionClaimBo;
import com.gameluck.promotion.domain.bo.PromotionRewardBo;
import com.gameluck.promotion.domain.bo.PromotionRewardItemBo;
import com.gameluck.promotion.domain.vo.PromotionClaimVo;
import com.gameluck.promotion.enums.PromotionRewardStatus;
import com.gameluck.promotion.mapper.PromotionClaimMapper;
import com.gameluck.promotion.mapper.PromotionRewardMapper;
import com.gameluck.wallet.domain.WalletTransaction;
import com.gameluck.wallet.domain.bo.WalletCreditBo;
import com.gameluck.wallet.enums.WalletTransactionStatus;
import com.gameluck.wallet.service.IWalletCoreService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PromotionRewardServiceImplTest {

    @Test
    @Tag("local")
    void claimActivePromotionCreditsWalletAndCreatesClaim() {
        PromotionRewardMapper rewardMapper = mock(PromotionRewardMapper.class);
        PromotionClaimMapper claimMapper = mock(PromotionClaimMapper.class);
        IWalletCoreService walletCoreService = mock(IWalletCoreService.class);
        PromotionRewardServiceImpl service = new PromotionRewardServiceImpl(rewardMapper, claimMapper, walletCoreService);

        PromotionReward reward = activeReward();
        when(rewardMapper.selectByIdForUpdate(10L)).thenReturn(reward);
        when(claimMapper.selectByPromotionAndMember("000000", 10L, 1001L)).thenReturn(null);
        when(claimMapper.insert(any(PromotionClaim.class))).thenReturn(1);
        when(claimMapper.updateById(any(PromotionClaim.class))).thenReturn(1);
        WalletTransaction transaction = new WalletTransaction();
        transaction.setTransactionNo("WT_PROMOTION_1");
        transaction.setStatus(WalletTransactionStatus.SUCCESS.name());
        when(walletCoreService.credit(any())).thenReturn(transaction);

        PromotionClaimVo claim = service.claim(claimBo());

        assertEquals(10L, claim.getPromotionId());
        assertEquals(1001L, claim.getMemberId());
        assertEquals("SUCCESS", claim.getStatus());
        assertEquals("WT_PROMOTION_1", claim.getWalletTransactionNo());
        ArgumentCaptor<PromotionClaim> claimCaptor = ArgumentCaptor.forClass(PromotionClaim.class);
        verify(claimMapper).insert(claimCaptor.capture());
        assertEquals(LocalDate.of(1000, 1, 1), claimCaptor.getValue().getClaimDate());
        ArgumentCaptor<WalletCreditBo> creditCaptor = ArgumentCaptor.forClass(WalletCreditBo.class);
        verify(walletCoreService).credit(creditCaptor.capture());
        assertEquals("SC", creditCaptor.getValue().getCurrencyCode());
        assertNull(creditCaptor.getValue().getReleaseMode());
        assertNull(creditCaptor.getValue().getRequiredTurnover());
    }

    @Test
    @Tag("local")
    void repeatedClaimReturnsExistingClaimWithoutWalletCredit() {
        PromotionRewardMapper rewardMapper = mock(PromotionRewardMapper.class);
        PromotionClaimMapper claimMapper = mock(PromotionClaimMapper.class);
        IWalletCoreService walletCoreService = mock(IWalletCoreService.class);
        PromotionRewardServiceImpl service = new PromotionRewardServiceImpl(rewardMapper, claimMapper, walletCoreService);

        PromotionReward reward = activeReward();
        PromotionClaim existing = new PromotionClaim();
        existing.setId(99L);
        existing.setTenantId("000000");
        existing.setPromotionId(10L);
        existing.setPromotionNo("PR202607030001");
        existing.setMemberId(1001L);
        existing.setCurrencyCode("SC");
        existing.setRewardAmount(new BigDecimal("3.000000"));
        existing.setStatus("SUCCESS");
        existing.setWalletTransactionNo("WT_EXISTING");
        when(rewardMapper.selectByIdForUpdate(10L)).thenReturn(reward);
        when(claimMapper.selectByPromotionAndMember("000000", 10L, 1001L)).thenReturn(existing);

        PromotionClaimVo claim = service.claim(claimBo());

        assertEquals(99L, claim.getId());
        assertEquals("WT_EXISTING", claim.getWalletTransactionNo());
        verifyNoInteractions(walletCoreService);
        verify(claimMapper, never()).insert(any(PromotionClaim.class));
    }

    @Test
    @Tag("local")
    void inactivePromotionCannotBeClaimed() {
        PromotionRewardMapper rewardMapper = mock(PromotionRewardMapper.class);
        PromotionClaimMapper claimMapper = mock(PromotionClaimMapper.class);
        IWalletCoreService walletCoreService = mock(IWalletCoreService.class);
        PromotionRewardServiceImpl service = new PromotionRewardServiceImpl(rewardMapper, claimMapper, walletCoreService);

        PromotionReward reward = activeReward();
        reward.setStatus(PromotionRewardStatus.INACTIVE.name());
        when(rewardMapper.selectByIdForUpdate(10L)).thenReturn(reward);

        ServiceException exception = assertThrows(ServiceException.class, () -> service.claim(claimBo()));

        assertEquals("promotion.reward.not.active", exception.getMessage());
        verifyNoInteractions(walletCoreService);
    }

    @Test
    @Tag("local")
    void dailyLoginRewardCreditsConfiguredGcAndScOncePerDay() {
        PromotionRewardMapper rewardMapper = mock(PromotionRewardMapper.class);
        PromotionClaimMapper claimMapper = mock(PromotionClaimMapper.class);
        IWalletCoreService walletCoreService = mock(IWalletCoreService.class);
        PromotionRewardServiceImpl service = new PromotionRewardServiceImpl(rewardMapper, claimMapper, walletCoreService);

        PromotionReward reward = activeReward();
        reward.setPromotionType("DAILY_LOGIN");
        reward.setClaimCycle("DAILY");
        reward.setDailyClaimLimit(1);
        reward.setRewardItems("[{\"currencyCode\":\"GC\",\"rewardAmount\":\"100.000000\"},{\"currencyCode\":\"SC\",\"rewardAmount\":\"1.000000\"}]");
        when(rewardMapper.selectActiveDailyLoginReward("000000")).thenReturn(reward);
        when(claimMapper.selectDailyClaim("000000", 10L, 1001L, LocalDate.now())).thenReturn(null);
        when(claimMapper.insert(any(PromotionClaim.class))).thenReturn(1);
        when(claimMapper.updateById(any(PromotionClaim.class))).thenReturn(1);

        WalletTransaction gc = new WalletTransaction();
        gc.setTransactionNo("WT_GC_DAILY");
        gc.setStatus(WalletTransactionStatus.SUCCESS.name());
        WalletTransaction sc = new WalletTransaction();
        sc.setTransactionNo("WT_SC_DAILY");
        sc.setStatus(WalletTransactionStatus.SUCCESS.name());
        when(walletCoreService.credit(any())).thenReturn(gc, sc);

        PromotionClaimVo claim = service.claimDailyLoginReward(1001L);

        assertEquals("SUCCESS", claim.getStatus());
        assertEquals("DAILY_LOGIN", claim.getPromotionType());
        assertEquals("WT_GC_DAILY,WT_SC_DAILY", claim.getWalletTransactionNo());
        ArgumentCaptor<WalletCreditBo> creditCaptor = ArgumentCaptor.forClass(WalletCreditBo.class);
        verify(walletCoreService, org.mockito.Mockito.times(2)).credit(creditCaptor.capture());
        assertEquals("GC", creditCaptor.getAllValues().get(0).getCurrencyCode());
        assertEquals(new BigDecimal("100.000000"), creditCaptor.getAllValues().get(0).getAmount());
        assertEquals("SC", creditCaptor.getAllValues().get(1).getCurrencyCode());
        assertEquals(new BigDecimal("1.000000"), creditCaptor.getAllValues().get(1).getAmount());
        assertEquals("DAILY_REWARD", creditCaptor.getAllValues().get(0).getSourceType());
        assertEquals("DAILY_REWARD", creditCaptor.getAllValues().get(1).getSourceType());
    }

    @Test
    @Tag("local")
    void dailyLoginRewardDuplicateReturnsExistingClaimWithoutCredit() {
        PromotionRewardMapper rewardMapper = mock(PromotionRewardMapper.class);
        PromotionClaimMapper claimMapper = mock(PromotionClaimMapper.class);
        IWalletCoreService walletCoreService = mock(IWalletCoreService.class);
        PromotionRewardServiceImpl service = new PromotionRewardServiceImpl(rewardMapper, claimMapper, walletCoreService);

        PromotionReward reward = activeReward();
        reward.setPromotionType("DAILY_LOGIN");
        reward.setClaimCycle("DAILY");
        when(rewardMapper.selectActiveDailyLoginReward("000000")).thenReturn(reward);
        PromotionClaim existing = new PromotionClaim();
        existing.setId(99L);
        existing.setPromotionId(10L);
        existing.setMemberId(1001L);
        existing.setStatus("SUCCESS");
        existing.setWalletTransactionNo("WT_EXISTING");
        existing.setClaimDate(LocalDate.now());
        when(claimMapper.selectDailyClaim("000000", 10L, 1001L, LocalDate.now())).thenReturn(existing);

        PromotionClaimVo claim = service.claimDailyLoginReward(1001L);

        assertEquals(99L, claim.getId());
        assertEquals("WT_EXISTING", claim.getWalletTransactionNo());
        verifyNoInteractions(walletCoreService);
    }

    @Test
    @Tag("local")
    void dailyLoginRewardDuplicateInsertReturnsExistingClaimWithoutCredit() {
        PromotionRewardMapper rewardMapper = mock(PromotionRewardMapper.class);
        PromotionClaimMapper claimMapper = mock(PromotionClaimMapper.class);
        IWalletCoreService walletCoreService = mock(IWalletCoreService.class);
        PromotionRewardServiceImpl service = new PromotionRewardServiceImpl(rewardMapper, claimMapper, walletCoreService);

        PromotionReward reward = activeReward();
        reward.setPromotionType("DAILY_LOGIN");
        reward.setRewardItems("[{\"currencyCode\":\"GC\",\"rewardAmount\":\"100.000000\"}]");
        PromotionClaim existing = new PromotionClaim();
        existing.setId(99L);
        existing.setPromotionId(10L);
        existing.setMemberId(1001L);
        existing.setStatus("SUCCESS");
        existing.setWalletTransactionNo("WT_EXISTING");
        existing.setClaimDate(LocalDate.now());
        when(rewardMapper.selectActiveDailyLoginReward("000000")).thenReturn(reward);
        when(claimMapper.selectDailyClaim("000000", 10L, 1001L, LocalDate.now())).thenReturn(null, existing);
        when(claimMapper.insert(any(PromotionClaim.class))).thenThrow(new DuplicateKeyException("duplicate daily claim"));

        PromotionClaimVo claim = service.claimDailyLoginReward(1001L);

        assertEquals(99L, claim.getId());
        assertEquals("WT_EXISTING", claim.getWalletTransactionNo());
        verifyNoInteractions(walletCoreService);
        verify(claimMapper, never()).updateById(any(PromotionClaim.class));
    }

    @Test
    @Tag("local")
    void dailyLoginRewardThrowsAndDoesNotUpdateClaimWhenSecondCurrencyCreditFails() {
        PromotionRewardMapper rewardMapper = mock(PromotionRewardMapper.class);
        PromotionClaimMapper claimMapper = mock(PromotionClaimMapper.class);
        IWalletCoreService walletCoreService = mock(IWalletCoreService.class);
        PromotionRewardServiceImpl service = new PromotionRewardServiceImpl(rewardMapper, claimMapper, walletCoreService);

        PromotionReward reward = activeReward();
        reward.setPromotionType("DAILY_LOGIN");
        reward.setRewardItems("[{\"currencyCode\":\"GC\",\"rewardAmount\":\"100.000000\"},{\"currencyCode\":\"SC\",\"rewardAmount\":\"1.000000\"}]");
        when(rewardMapper.selectActiveDailyLoginReward("000000")).thenReturn(reward);
        when(claimMapper.selectDailyClaim("000000", 10L, 1001L, LocalDate.now())).thenReturn(null);
        when(claimMapper.insert(any(PromotionClaim.class))).thenReturn(1);
        WalletTransaction gc = new WalletTransaction();
        gc.setTransactionNo("WT_GC_DAILY");
        gc.setStatus(WalletTransactionStatus.SUCCESS.name());
        WalletTransaction sc = new WalletTransaction();
        sc.setTransactionNo("WT_SC_DAILY");
        sc.setStatus(WalletTransactionStatus.FAILED.name());
        sc.setFailReason("wallet unavailable");
        when(walletCoreService.credit(any())).thenReturn(gc, sc);

        ServiceException exception = assertThrows(ServiceException.class, () -> service.claimDailyLoginReward(1001L));

        assertEquals("wallet unavailable", exception.getMessage());
        verify(walletCoreService, org.mockito.Mockito.times(2)).credit(any());
        verify(claimMapper, never()).updateById(any(PromotionClaim.class));
    }

    @Test
    @Tag("local")
    void dailyLoginRewardRejectsJsonNullRewardItems() {
        PromotionRewardMapper rewardMapper = mock(PromotionRewardMapper.class);
        PromotionClaimMapper claimMapper = mock(PromotionClaimMapper.class);
        IWalletCoreService walletCoreService = mock(IWalletCoreService.class);
        PromotionRewardServiceImpl service = new PromotionRewardServiceImpl(rewardMapper, claimMapper, walletCoreService);

        PromotionReward reward = activeReward();
        reward.setPromotionType("DAILY_LOGIN");
        reward.setRewardItems("null");
        when(rewardMapper.selectActiveDailyLoginReward("000000")).thenReturn(reward);
        when(claimMapper.selectDailyClaim("000000", 10L, 1001L, LocalDate.now())).thenReturn(null);

        ServiceException exception = assertThrows(ServiceException.class, () -> service.claimDailyLoginReward(1001L));

        assertEquals("promotion.reward.items.invalid", exception.getMessage());
        verifyNoInteractions(walletCoreService);
    }

    @Test
    @Tag("local")
    void insertRewardRejectsNullRewardItem() {
        PromotionRewardMapper rewardMapper = mock(PromotionRewardMapper.class);
        PromotionClaimMapper claimMapper = mock(PromotionClaimMapper.class);
        IWalletCoreService walletCoreService = mock(IWalletCoreService.class);
        PromotionRewardServiceImpl service = new PromotionRewardServiceImpl(rewardMapper, claimMapper, walletCoreService);
        PromotionRewardBo bo = new PromotionRewardBo();
        bo.setPromotionName("Daily simulated reward");
        bo.setRewardAmount(new BigDecimal("1.000000"));
        bo.setStatus(PromotionRewardStatus.ACTIVE.name());
        bo.setRewardItems(Collections.singletonList((PromotionRewardItemBo) null));

        ServiceException exception = assertThrows(ServiceException.class, () -> service.insertByBo(bo));

        assertEquals("promotion.reward.items.invalid", exception.getMessage());
        verify(rewardMapper, never()).insert(any(PromotionReward.class));
    }

    @Test
    @Tag("local")
    void dailyLoginRewardStateReturnsNotConfiguredWhenNoActiveReward() {
        PromotionRewardMapper rewardMapper = mock(PromotionRewardMapper.class);
        PromotionClaimMapper claimMapper = mock(PromotionClaimMapper.class);
        IWalletCoreService walletCoreService = mock(IWalletCoreService.class);
        PromotionRewardServiceImpl service = new PromotionRewardServiceImpl(rewardMapper, claimMapper, walletCoreService);

        when(rewardMapper.selectActiveDailyLoginReward("000000")).thenReturn(null);

        ClientDailyLoginRewardVo state = service.dailyLoginReward(1001L);

        assertEquals(LocalDate.now(), state.getClaimDate());
        assertEquals("NOT_CONFIGURED", state.getClaimStatus());
        assertFalse(state.getCanClaim());
        assertTrue(state.getRewardItems().isEmpty());
        verifyNoInteractions(claimMapper);
        verifyNoInteractions(walletCoreService);
    }

    @Test
    @Tag("local")
    void dailyLoginRewardStateReturnsUnclaimedWhenConfiguredAndNoClaimToday() {
        PromotionRewardMapper rewardMapper = mock(PromotionRewardMapper.class);
        PromotionClaimMapper claimMapper = mock(PromotionClaimMapper.class);
        IWalletCoreService walletCoreService = mock(IWalletCoreService.class);
        PromotionRewardServiceImpl service = new PromotionRewardServiceImpl(rewardMapper, claimMapper, walletCoreService);

        PromotionReward reward = activeReward();
        reward.setPromotionType("DAILY_LOGIN");
        reward.setRewardItems("[{\"currencyCode\":\"GC\",\"rewardAmount\":\"100.000000\"},{\"currencyCode\":\"SC\",\"rewardAmount\":\"1.000000\"}]");
        when(rewardMapper.selectActiveDailyLoginReward("000000")).thenReturn(reward);
        when(claimMapper.selectDailyClaim("000000", 10L, 1001L, LocalDate.now())).thenReturn(null);

        ClientDailyLoginRewardVo state = service.dailyLoginReward(1001L);

        assertEquals(10L, state.getPromotionId());
        assertEquals("Daily simulated reward", state.getPromotionName());
        assertEquals("DAILY_LOGIN", state.getPromotionType());
        assertEquals(LocalDate.now(), state.getClaimDate());
        assertEquals("UNCLAIMED", state.getClaimStatus());
        assertTrue(state.getCanClaim());
        assertEquals(2, state.getRewardItems().size());
        assertEquals("GC", state.getRewardItems().get(0).getCurrencyCode());
        assertEquals(new BigDecimal("100.000000"), state.getRewardItems().get(0).getRewardAmount());
        assertNull(state.getClaimNo());
        assertNull(state.getWalletTransactionNo());
        verifyNoInteractions(walletCoreService);
    }

    @Test
    @Tag("local")
    void dailyLoginRewardStateReturnsClaimStatusWhenClaimedToday() {
        PromotionRewardMapper rewardMapper = mock(PromotionRewardMapper.class);
        PromotionClaimMapper claimMapper = mock(PromotionClaimMapper.class);
        IWalletCoreService walletCoreService = mock(IWalletCoreService.class);
        PromotionRewardServiceImpl service = new PromotionRewardServiceImpl(rewardMapper, claimMapper, walletCoreService);

        PromotionReward reward = activeReward();
        reward.setPromotionType("DAILY_LOGIN");
        reward.setRewardItems("[{\"currencyCode\":\"GC\",\"rewardAmount\":\"100.000000\"}]");
        PromotionClaim existing = new PromotionClaim();
        existing.setPromotionId(10L);
        existing.setPromotionName("Daily simulated reward");
        existing.setPromotionType("DAILY_LOGIN");
        existing.setMemberId(1001L);
        existing.setClaimNo("PC_EXISTING");
        existing.setClaimDate(LocalDate.now());
        existing.setStatus("SUCCESS");
        existing.setWalletTransactionNo("WT_EXISTING");
        when(rewardMapper.selectActiveDailyLoginReward("000000")).thenReturn(reward);
        when(claimMapper.selectDailyClaim("000000", 10L, 1001L, LocalDate.now())).thenReturn(existing);

        ClientDailyLoginRewardVo state = service.dailyLoginReward(1001L);

        assertEquals("SUCCESS", state.getClaimStatus());
        assertFalse(state.getCanClaim());
        assertEquals("PC_EXISTING", state.getClaimNo());
        assertEquals("WT_EXISTING", state.getWalletTransactionNo());
        assertEquals(1, state.getRewardItems().size());
        verifyNoInteractions(walletCoreService);
    }

    private PromotionReward activeReward() {
        PromotionReward reward = new PromotionReward();
        reward.setId(10L);
        reward.setTenantId("000000");
        reward.setPromotionNo("PR202607030001");
        reward.setPromotionName("Daily simulated reward");
        reward.setCurrencyCode("SC");
        reward.setRewardAmount(new BigDecimal("3.000000"));
        reward.setStatus(PromotionRewardStatus.ACTIVE.name());
        reward.setStartTime(new Date(System.currentTimeMillis() - 1000));
        reward.setEndTime(new Date(System.currentTimeMillis() + 100000));
        return reward;
    }

    private PromotionClaimBo claimBo() {
        PromotionClaimBo bo = new PromotionClaimBo();
        bo.setPromotionId(10L);
        bo.setMemberId(1001L);
        bo.setRemark("manual claim");
        return bo;
    }
}
