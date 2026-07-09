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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class ClientPromotionService {

    private static final String TENANT_ID = "000000";

    private final PromotionRewardMapper promotionRewardMapper;
    private final PromotionClaimMapper promotionClaimMapper;
    private final IPromotionRewardService promotionRewardService;
    private final ClientTokenService clientTokenService;

    public List<ClientPromotionVo> promotions(String authorization) {
        Long memberId = clientTokenService.requireMemberId(authorization);
        Map<Long, PromotionClaim> claims = promotionClaimMapper.selectClientClaimsByMember(TENANT_ID, memberId).stream()
            .collect(Collectors.toMap(PromotionClaim::getPromotionId, Function.identity(), (left, right) -> left));
        return promotionRewardMapper.selectClientActiveRewards(TENANT_ID).stream()
            .map(reward -> toClientPromotion(reward, claims.get(reward.getId())))
            .toList();
    }

    public ClientPromotionVo claim(String authorization, ClientPromotionClaimBo bo) {
        Long memberId = clientTokenService.requireMemberId(authorization);
        PromotionClaimBo claimBo = new PromotionClaimBo();
        claimBo.setPromotionId(bo.getPromotionId());
        claimBo.setMemberId(memberId);
        claimBo.setRemark("H5 client claim");
        PromotionClaimVo claim = promotionRewardService.claim(claimBo);
        return toClientPromotion(claim);
    }

    private ClientPromotionVo toClientPromotion(PromotionReward reward, PromotionClaim claim) {
        ClientPromotionVo vo = new ClientPromotionVo();
        vo.setPromotionId(reward.getId());
        vo.setPromotionNo(reward.getPromotionNo());
        vo.setPromotionName(reward.getPromotionName());
        vo.setCurrencyCode(reward.getCurrencyCode());
        vo.setRewardAmount(formatAmount(reward.getRewardAmount()));
        vo.setStatus(reward.getStatus());
        if (claim == null) {
            vo.setClaimStatus("UNCLAIMED");
            vo.setCanClaim(true);
            return vo;
        }
        vo.setClaimStatus(claim.getStatus());
        vo.setClaimNo(claim.getClaimNo());
        vo.setWalletTransactionNo(claim.getWalletTransactionNo());
        vo.setCanClaim(false);
        return vo;
    }

    private ClientPromotionVo toClientPromotion(PromotionClaimVo claim) {
        ClientPromotionVo vo = new ClientPromotionVo();
        vo.setPromotionId(claim.getPromotionId());
        vo.setPromotionNo(claim.getPromotionNo());
        vo.setPromotionName(claim.getPromotionName());
        vo.setCurrencyCode(claim.getCurrencyCode());
        vo.setRewardAmount(formatAmount(claim.getRewardAmount()));
        vo.setStatus("ACTIVE");
        vo.setClaimStatus(claim.getStatus());
        vo.setClaimNo(claim.getClaimNo());
        vo.setWalletTransactionNo(claim.getWalletTransactionNo());
        vo.setCanClaim(false);
        return vo;
    }

    private String formatAmount(BigDecimal amount) {
        return amount == null ? "0.00" : amount.setScale(2).toPlainString();
    }
}
