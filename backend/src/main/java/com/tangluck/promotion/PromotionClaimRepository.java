package com.tangluck.promotion;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PromotionClaimRepository extends JpaRepository<PromotionClaim, Long> {
    boolean existsByUserIdAndCampaignIdAndPeriodKey(Long userId, Long campaignId, String periodKey);
}
