package com.tangluck.promotion;

import com.tangluck.common.api.BusinessException;
import com.tangluck.common.api.ErrorCode;
import com.tangluck.wallet.WalletService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.ArrayList;

@Service
public class CouponService {
    private final CouponCodeRepository couponCodeRepository;
    private final PromotionClaimRepository claimRepository;
    private final PromotionRewardGrantRepository grantRepository;
    private final WalletService walletService;
    private final RewardPolicyParser rewardPolicyParser;
    private final Clock clock;

    public CouponService(CouponCodeRepository couponCodeRepository, PromotionClaimRepository claimRepository, PromotionRewardGrantRepository grantRepository, WalletService walletService, RewardPolicyParser rewardPolicyParser) {
        this.couponCodeRepository = couponCodeRepository;
        this.claimRepository = claimRepository;
        this.grantRepository = grantRepository;
        this.walletService = walletService;
        this.rewardPolicyParser = rewardPolicyParser;
        this.clock = Clock.systemUTC();
    }

    @Transactional
    public ClaimResponse claim(Long userId, String code, String idempotencyKey) {
        var coupon = couponCodeRepository.findByCode(code)
                .orElseThrow(() -> new BusinessException(ErrorCode.CAMPAIGN_NOT_ACTIVE, "Coupon is not active."));
        if (!"active".equals(coupon.getStatus())) {
            throw new BusinessException(ErrorCode.CAMPAIGN_NOT_ACTIVE, "Coupon is not active.");
        }
        if (claimRepository.existsByUserIdAndCampaignIdAndPeriodKey(userId, -coupon.getId(), "once")) {
            throw new BusinessException(ErrorCode.CLAIM_DUPLICATED, "Coupon already claimed.");
        }

        var claim = claimRepository.save(new PromotionClaim(userId, -coupon.getId(), "once", "granted", "pass", idempotencyKey, clock.instant()));
        var rewards = rewardPolicyParser.parse(coupon.getRewardPolicyJson());
        var ledgerIds = new ArrayList<Long>();
        for (RewardDto reward : rewards) {
            var ledger = walletService.credit(userId, reward.currency(), reward.amount(), "coupon", claim.getId().toString(), idempotencyKey + ":" + reward.currency());
            grantRepository.save(new PromotionRewardGrant(claim.getId(), userId, reward.currency(), reward.amount(), ledger.ledgerId(), "posted", null, clock.instant()));
            ledgerIds.add(ledger.ledgerId());
        }
        return new ClaimResponse(claim.getId(), code, "granted", "pass", rewards, ledgerIds);
    }
}
