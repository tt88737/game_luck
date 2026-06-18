package com.tangluck.promotion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "promotion_campaigns")
public class PromotionCampaign {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "campaign_code", nullable = false)
    private String campaignCode;

    @Column(nullable = false)
    private String name;

    @Column(name = "campaign_type", nullable = false)
    private String campaignType;

    @Column(nullable = false)
    private String status;

    @Column(name = "eligible_regions_json", nullable = false)
    private String eligibleRegionsJson;

    @Column(name = "blocked_regions_json")
    private String blockedRegionsJson;

    @Column(name = "reward_policy_json", nullable = false)
    private String rewardPolicyJson;

    @Column(name = "sc_strategy", nullable = false)
    private String scStrategy;

    @Column(name = "period_type", nullable = false)
    private String periodType;

    @Column(name = "legal_approval_id")
    private String legalApprovalId;

    @Column(name = "risk_action", nullable = false)
    private String riskAction;

    protected PromotionCampaign() {
    }

    public Long getId() {
        return id;
    }

    public String getCampaignCode() {
        return campaignCode;
    }

    public String getCampaignType() {
        return campaignType;
    }

    public String getStatus() {
        return status;
    }

    public String getEligibleRegionsJson() {
        return eligibleRegionsJson;
    }

    public String getBlockedRegionsJson() {
        return blockedRegionsJson;
    }

    public String getRewardPolicyJson() {
        return rewardPolicyJson;
    }

    public String getRiskAction() {
        return riskAction;
    }
}
