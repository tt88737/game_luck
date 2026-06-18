package com.tangluck.promotion;

import com.tangluck.auth.UserRepository;
import com.tangluck.common.api.BusinessException;
import com.tangluck.common.api.ErrorCode;
import com.tangluck.compliance.ComplianceRegionRepository;
import com.tangluck.wallet.WalletService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class PromotionService {
    private final PromotionCampaignRepository campaignRepository;
    private final PromotionClaimRepository claimRepository;
    private final PromotionRewardGrantRepository grantRepository;
    private final UserRepository userRepository;
    private final ComplianceRegionRepository regionRepository;
    private final WalletService walletService;
    private final RewardPolicyParser rewardPolicyParser;
    private final Clock clock;

    public PromotionService(PromotionCampaignRepository campaignRepository, PromotionClaimRepository claimRepository, PromotionRewardGrantRepository grantRepository, UserRepository userRepository, ComplianceRegionRepository regionRepository, WalletService walletService, RewardPolicyParser rewardPolicyParser) {
        this.campaignRepository = campaignRepository;
        this.claimRepository = claimRepository;
        this.grantRepository = grantRepository;
        this.userRepository = userRepository;
        this.regionRepository = regionRepository;
        this.walletService = walletService;
        this.rewardPolicyParser = rewardPolicyParser;
        this.clock = Clock.systemUTC();
    }

    @Transactional
    public ClaimResponse claimCampaign(Long userId, String campaignCode, String idempotencyKey) {
        var user = userRepository.findById(userId).orElseThrow();
        var campaign = campaignRepository.findByCampaignCode(campaignCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.CAMPAIGN_NOT_ACTIVE, "Campaign does not exist."));
        if (!"active".equals(campaign.getStatus())) {
            throw new BusinessException(ErrorCode.CAMPAIGN_NOT_ACTIVE, "Campaign is not active.");
        }
        if (!rewardPolicyParser.parseRegions(campaign.getEligibleRegionsJson()).contains(user.getStateCode())
                || rewardPolicyParser.parseRegions(campaign.getBlockedRegionsJson()).contains(user.getStateCode())) {
            throw new BusinessException(ErrorCode.REGION_BLOCKED, "This feature is not available in your region.", Map.of("state_code", user.getStateCode()));
        }
        var region = regionRepository.findByCountryCodeAndStateCode(user.getCountryCode(), user.getStateCode()).orElseThrow();
        var rewards = rewardPolicyParser.parse(campaign.getRewardPolicyJson());
        if (rewards.stream().anyMatch(reward -> "SC".equals(reward.currency())) && !region.isScGrantAllowed()) {
            throw new BusinessException(ErrorCode.REGION_BLOCKED, "This feature is not available in your region.", Map.of("state_code", user.getStateCode(), "feature", "sc_grant"));
        }

        var periodKey = periodKey(campaign);
        if (claimRepository.existsByUserIdAndCampaignIdAndPeriodKey(userId, campaign.getId(), periodKey)) {
            throw new BusinessException(ErrorCode.CLAIM_DUPLICATED, "Campaign already claimed.");
        }

        var riskAction = "manual_review".equals(user.getRiskLevel()) || "blocked".equals(user.getRiskLevel())
                ? campaign.getRiskAction()
                : "pass";
        var claim = claimRepository.save(new PromotionClaim(userId, campaign.getId(), periodKey, "granted", riskAction, idempotencyKey, clock.instant()));
        List<RewardDto> grantedRewards = new ArrayList<>();
        List<Long> ledgerIds = new ArrayList<>();
        for (RewardDto reward : rewards) {
            if ("SC".equals(reward.currency()) && "gc_only".equals(riskAction)) {
                grantRepository.save(new PromotionRewardGrant(claim.getId(), userId, reward.currency(), reward.amount(), null, "rejected", "risk_action=gc_only", clock.instant()));
                continue;
            }
            var ledger = walletService.credit(userId, reward.currency(), reward.amount(), campaign.getCampaignType(), claim.getId().toString(), idempotencyKey + ":" + reward.currency());
            grantRepository.save(new PromotionRewardGrant(claim.getId(), userId, reward.currency(), reward.amount(), ledger.ledgerId(), "posted", null, clock.instant()));
            grantedRewards.add(reward);
            ledgerIds.add(ledger.ledgerId());
        }

        return new ClaimResponse(claim.getId(), campaignCode, "granted", riskAction, grantedRewards, ledgerIds);
    }

    private String periodKey(PromotionCampaign campaign) {
        if ("daily".equals(campaign.getCampaignType()) || "daily_login".equals(campaign.getCampaignType()) || "daily_task".equals(campaign.getCampaignType())) {
            return LocalDate.now(clock).toString();
        }
        return "once";
    }
}
