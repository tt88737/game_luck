package com.tangluck.admin;

import java.util.List;

public record AdminCampaignRequest(
        String campaignCode,
        String name,
        String campaignType,
        List<String> eligibleRegions,
        List<String> blockedRegions,
        List<AdminRewardRequest> rewardPolicy,
        String scStrategy,
        String rulesVersion,
        String legalApprovalId,
        String riskAction
) {
    public AdminCampaignRequest withLegalApprovalId(String newLegalApprovalId) {
        return new AdminCampaignRequest(campaignCode, name, campaignType, eligibleRegions, blockedRegions, rewardPolicy, scStrategy, rulesVersion, newLegalApprovalId, riskAction);
    }
}
