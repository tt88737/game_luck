package com.gameluck.redemption.service.impl;

import com.gameluck.member.compliance.MemberRegionEligibilityChecker;
import com.gameluck.redemption.service.IRedemptionEligibilityPolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class RedemptionMemberRegionEligibilityChecker implements MemberRegionEligibilityChecker {

    private final IRedemptionEligibilityPolicyService eligibilityPolicyService;

    @Override
    public boolean isEligible(String tenantId, String currencyCode, String countryCode, String stateCode, String channel) {
        return eligibilityPolicyService.isEligible(tenantId, currencyCode, countryCode, stateCode, channel);
    }
}
