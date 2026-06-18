package com.tangluck.promotion;

import java.util.List;

public record ClaimResponse(
        Long claimId,
        String campaignCode,
        String status,
        String riskAction,
        List<RewardDto> rewards,
        List<Long> ledgerIds
) {
}
