package com.tangluck.admin;

public record AdminCampaignDto(
        String campaignCode,
        String name,
        String campaignType,
        String status,
        String scStrategy,
        String rulesVersion,
        String legalApprovalId,
        String riskAction,
        String eligibleRegionsJson,
        String blockedRegionsJson,
        String rewardPolicyJson
) {
}
