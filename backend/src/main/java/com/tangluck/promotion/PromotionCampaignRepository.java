package com.tangluck.promotion;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PromotionCampaignRepository extends JpaRepository<PromotionCampaign, Long> {
    Optional<PromotionCampaign> findByCampaignCode(String campaignCode);
}
