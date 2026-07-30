package com.gameluck.member.compliance;

@FunctionalInterface
public interface MemberRegionEligibilityChecker {

    boolean isEligible(String tenantId, String currencyCode, String countryCode, String stateCode, String channel);
}
