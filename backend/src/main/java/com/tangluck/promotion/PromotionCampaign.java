package com.tangluck.promotion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

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

    @Column(name = "user_period_cap", nullable = false)
    private Integer userPeriodCap;

    @Column(name = "daily_budget_cap_json", nullable = false)
    private String dailyBudgetCapJson;

    @Column(name = "rules_version", nullable = false)
    private String rulesVersion;

    @Column(name = "legal_approval_id")
    private String legalApprovalId;

    @Column(name = "risk_action", nullable = false)
    private String riskAction;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "updated_by", nullable = false)
    private Long updatedBy;

    protected PromotionCampaign() {
    }

    public PromotionCampaign(String campaignCode, String name, String campaignType, String status, String eligibleRegionsJson, String blockedRegionsJson, String rewardPolicyJson, String scStrategy, String periodType, String rulesVersion, String legalApprovalId, String riskAction) {
        this.campaignCode = campaignCode;
        this.name = name;
        this.campaignType = campaignType;
        this.status = status;
        this.eligibleRegionsJson = eligibleRegionsJson;
        this.blockedRegionsJson = blockedRegionsJson;
        this.rewardPolicyJson = rewardPolicyJson;
        this.scStrategy = scStrategy;
        this.periodType = periodType;
        this.userPeriodCap = 1;
        this.rulesVersion = rulesVersion;
        this.legalApprovalId = legalApprovalId;
        this.riskAction = riskAction;
        this.dailyBudgetCapJson = "{\"GC\":1000000,\"SC\":\"100.00\"}";
        this.startsAt = Instant.now();
        this.createdBy = 1L;
        this.updatedBy = 1L;
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

    public String getName() {
        return name;
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

    public String getScStrategy() {
        return scStrategy;
    }

    public String getLegalApprovalId() {
        return legalApprovalId;
    }

    public String getRulesVersion() {
        return rulesVersion;
    }

    public void publish() {
        this.status = "active";
    }

    public void pause() {
        this.status = "paused";
    }
}
