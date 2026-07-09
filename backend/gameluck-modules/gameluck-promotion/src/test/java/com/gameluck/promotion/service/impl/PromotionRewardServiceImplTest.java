package com.gameluck.promotion.service.impl;

import com.gameluck.common.core.exception.ServiceException;
import com.gameluck.promotion.domain.PromotionClaim;
import com.gameluck.promotion.domain.PromotionReward;
import com.gameluck.promotion.domain.bo.PromotionClaimBo;
import com.gameluck.promotion.domain.vo.PromotionClaimVo;
import com.gameluck.promotion.enums.PromotionRewardStatus;
import com.gameluck.promotion.mapper.PromotionClaimMapper;
import com.gameluck.promotion.mapper.PromotionRewardMapper;
import com.gameluck.wallet.domain.WalletTransaction;
import com.gameluck.wallet.enums.WalletTransactionStatus;
import com.gameluck.wallet.service.IWalletCoreService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
        verify(walletCoreService).credit(any());
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
