package com.gameluck.promotion.client.service;

import com.gameluck.common.core.client.ClientTokenService;
import com.gameluck.promotion.client.domain.bo.ClientPromotionClaimBo;
import com.gameluck.promotion.client.domain.vo.ClientPromotionVo;
import com.gameluck.promotion.domain.PromotionClaim;
import com.gameluck.promotion.domain.PromotionReward;
import com.gameluck.promotion.domain.bo.PromotionClaimBo;
import com.gameluck.promotion.domain.vo.PromotionClaimVo;
import com.gameluck.promotion.mapper.PromotionClaimMapper;
import com.gameluck.promotion.mapper.PromotionRewardMapper;
import com.gameluck.promotion.service.IPromotionRewardService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClientPromotionServiceTest {

    @Test
    @Tag("local")
    void listMarksClaimedRewardsForCurrentMember() {
        PromotionRewardMapper rewardMapper = mock(PromotionRewardMapper.class);
        PromotionClaimMapper claimMapper = mock(PromotionClaimMapper.class);
        IPromotionRewardService rewardService = mock(IPromotionRewardService.class);
        ClientTokenService tokenService = new ClientTokenService();
        ClientPromotionService service = new ClientPromotionService(rewardMapper, claimMapper, rewardService, tokenService);
        PromotionReward reward = reward();
        PromotionClaim claim = claim();
        when(rewardMapper.selectClientActiveRewards("000000")).thenReturn(List.of(reward));
        when(claimMapper.selectClientClaimsByMember("000000", 1001L)).thenReturn(List.of(claim));

        List<ClientPromotionVo> result = service.promotions("Bearer " + tokenService.issue(1001L));

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).getPromotionId());
        assertEquals("SUCCESS", result.get(0).getClaimStatus());
        assertFalse(result.get(0).getCanClaim());
    }

    @Test
    @Tag("local")
    void claimUsesCurrentMemberAndExistingRewardService() {
        PromotionRewardMapper rewardMapper = mock(PromotionRewardMapper.class);
        PromotionClaimMapper claimMapper = mock(PromotionClaimMapper.class);
        IPromotionRewardService rewardService = mock(IPromotionRewardService.class);
        ClientTokenService tokenService = new ClientTokenService();
        ClientPromotionService service = new ClientPromotionService(rewardMapper, claimMapper, rewardService, tokenService);
        PromotionClaimVo claimVo = new PromotionClaimVo();
        claimVo.setPromotionId(10L);
        claimVo.setPromotionNo("PR-DEMO-DAILY-SC");
        claimVo.setPromotionName("Daily SC reward");
        claimVo.setMemberId(1001L);
        claimVo.setCurrencyCode("SC");
        claimVo.setRewardAmount(new BigDecimal("8.000000"));
        claimVo.setStatus("SUCCESS");
        claimVo.setClaimNo("PC1001");
        claimVo.setWalletTransactionNo("WT_PROMO_1");
        when(rewardService.claim(any(PromotionClaimBo.class))).thenReturn(claimVo);
        ClientPromotionClaimBo bo = new ClientPromotionClaimBo();
        bo.setPromotionId(10L);

        ClientPromotionVo result = service.claim("Bearer " + tokenService.issue(1001L), bo);

        assertEquals(10L, result.getPromotionId());
        assertEquals("SUCCESS", result.getClaimStatus());
        assertEquals("WT_PROMO_1", result.getWalletTransactionNo());
        assertFalse(result.getCanClaim());
        verify(rewardService).claim(any(PromotionClaimBo.class));
    }

    private PromotionReward reward() {
        PromotionReward reward = new PromotionReward();
        reward.setId(10L);
        reward.setPromotionNo("PR-DEMO-DAILY-SC");
        reward.setPromotionName("Daily SC reward");
        reward.setCurrencyCode("SC");
        reward.setRewardAmount(new BigDecimal("8.000000"));
        reward.setStatus("ACTIVE");
        return reward;
    }

    private PromotionClaim claim() {
        PromotionClaim claim = new PromotionClaim();
        claim.setPromotionId(10L);
        claim.setClaimNo("PC1001");
        claim.setStatus("SUCCESS");
        claim.setWalletTransactionNo("WT_PROMO_1");
        return claim;
    }
}
