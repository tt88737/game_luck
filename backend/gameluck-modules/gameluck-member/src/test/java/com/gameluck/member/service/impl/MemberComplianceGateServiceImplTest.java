package com.gameluck.member.service.impl;

import com.gameluck.member.compliance.MemberComplianceAction;
import com.gameluck.member.compliance.MemberComplianceContext;
import com.gameluck.member.compliance.MemberComplianceDecision;
import com.gameluck.member.compliance.MemberComplianceReason;
import com.gameluck.member.compliance.MemberRegionEligibilityChecker;
import com.gameluck.member.domain.MemberProfile;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MemberComplianceGateServiceImplTest {

    @Test
    @Tag("local")
    void missingMemberReturnsNotExists() {
        MemberRegionEligibilityChecker regionChecker = mock(MemberRegionEligibilityChecker.class);
        MemberComplianceGateServiceImpl service = new MemberComplianceGateServiceImpl(regionChecker);

        MemberComplianceDecision decision = service.evaluate(context(null, MemberComplianceAction.REDEMPTION_REQUEST, "SC"));

        assertFalse(decision.isAllowed());
        assertEquals(MemberComplianceReason.MEMBER_NOT_EXISTS.name(), decision.getReasonCode());
        assertEquals("client.redemption.member.not.exists", decision.getMessageKey());
        verify(regionChecker, never()).isEligible(any(), any(), any(), any(), any());
    }

    @Test
    @Tag("local")
    void inactiveMemberReturnsInactive() {
        MemberComplianceGateServiceImpl service = new MemberComplianceGateServiceImpl(mock(MemberRegionEligibilityChecker.class));
        MemberProfile member = eligibleMember();
        member.setStatus("DISABLED");

        MemberComplianceDecision decision = service.evaluate(context(member, MemberComplianceAction.REDEMPTION_REQUEST, "SC"));

        assertFalse(decision.isAllowed());
        assertEquals(MemberComplianceReason.MEMBER_INACTIVE.name(), decision.getReasonCode());
        assertEquals("client.redemption.member.inactive", decision.getMessageKey());
    }

    @Test
    @Tag("local")
    void highRiskMemberReturnsRiskBlocked() {
        MemberComplianceGateServiceImpl service = new MemberComplianceGateServiceImpl(mock(MemberRegionEligibilityChecker.class));
        MemberProfile member = eligibleMember();
        member.setRiskLevel("HIGH");

        MemberComplianceDecision decision = service.evaluate(context(member, MemberComplianceAction.REDEMPTION_REQUEST, "SC"));

        assertFalse(decision.isAllowed());
        assertEquals(MemberComplianceReason.RISK_BLOCKED.name(), decision.getReasonCode());
        assertEquals("client.redemption.risk.blocked", decision.getMessageKey());
    }

    @Test
    @Tag("local")
    void missingMemberForPurchaseUsesPurchaseMessage() {
        MemberComplianceGateServiceImpl service = new MemberComplianceGateServiceImpl(mock(MemberRegionEligibilityChecker.class));

        MemberComplianceDecision decision = service.evaluate(context(null, MemberComplianceAction.PURCHASE_PAY, "USD"));

        assertFalse(decision.isAllowed());
        assertEquals(MemberComplianceReason.MEMBER_NOT_EXISTS.name(), decision.getReasonCode());
        assertEquals("client.purchase.member.not.exists", decision.getMessageKey());
    }

    @Test
    @Tag("local")
    void inactiveMemberForPurchaseUsesPurchaseMessage() {
        MemberComplianceGateServiceImpl service = new MemberComplianceGateServiceImpl(mock(MemberRegionEligibilityChecker.class));
        MemberProfile member = eligibleMember();
        member.setStatus("DISABLED");

        MemberComplianceDecision decision = service.evaluate(context(member, MemberComplianceAction.PURCHASE_PAY, "USD"));

        assertFalse(decision.isAllowed());
        assertEquals(MemberComplianceReason.MEMBER_INACTIVE.name(), decision.getReasonCode());
        assertEquals("client.purchase.member.inactive", decision.getMessageKey());
    }

    @Test
    @Tag("local")
    void highRiskMemberForPurchaseUsesPurchaseMessage() {
        MemberComplianceGateServiceImpl service = new MemberComplianceGateServiceImpl(mock(MemberRegionEligibilityChecker.class));
        MemberProfile member = eligibleMember();
        member.setRiskLevel("HIGH");

        MemberComplianceDecision decision = service.evaluate(context(member, MemberComplianceAction.PURCHASE_PAY, "USD"));

        assertFalse(decision.isAllowed());
        assertEquals(MemberComplianceReason.RISK_BLOCKED.name(), decision.getReasonCode());
        assertEquals("client.purchase.risk.blocked", decision.getMessageKey());
    }

    @Test
    @Tag("local")
    void redemptionRequiresAgeAndAgreementsBeforeKyc() {
        MemberComplianceGateServiceImpl service = new MemberComplianceGateServiceImpl(mock(MemberRegionEligibilityChecker.class));
        MemberProfile member = eligibleMember();
        member.setAgeConfirmed(false);
        member.setKycStatus("NOT_STARTED");

        MemberComplianceDecision decision = service.evaluate(context(member, MemberComplianceAction.REDEMPTION_REQUEST, "SC"));

        assertFalse(decision.isAllowed());
        assertEquals(MemberComplianceReason.AGE_REQUIRED.name(), decision.getReasonCode());
        assertEquals("client.redemption.age.required", decision.getMessageKey());
    }

    @Test
    @Tag("local")
    void redemptionRequiresKycApprovedBeforeRegionPolicy() {
        MemberRegionEligibilityChecker regionChecker = mock(MemberRegionEligibilityChecker.class);
        MemberComplianceGateServiceImpl service = new MemberComplianceGateServiceImpl(regionChecker);
        MemberProfile member = eligibleMember();
        member.setKycStatus("PENDING");

        MemberComplianceDecision decision = service.evaluate(context(member, MemberComplianceAction.REDEMPTION_REQUEST, "SC"));

        assertFalse(decision.isAllowed());
        assertEquals(MemberComplianceReason.KYC_REQUIRED.name(), decision.getReasonCode());
        assertEquals("client.redemption.kyc.required", decision.getMessageKey());
        verify(regionChecker, never()).isEligible(any(), any(), any(), any(), any());
    }

    @Test
    @Tag("local")
    void redemptionDeniedByRegionPolicyReturnsRegionBlocked() {
        MemberRegionEligibilityChecker regionChecker = mock(MemberRegionEligibilityChecker.class);
        MemberComplianceGateServiceImpl service = new MemberComplianceGateServiceImpl(regionChecker);
        MemberProfile member = eligibleMember();
        member.setCountryCode("us");
        member.setStateCode("wa");
        when(regionChecker.isEligible("000000", "SC", "US", "WA", "h5")).thenReturn(false);

        MemberComplianceDecision decision = service.evaluate(context(member, MemberComplianceAction.REDEMPTION_REQUEST, "SC"));

        assertFalse(decision.isAllowed());
        assertEquals(MemberComplianceReason.REGION_BLOCKED.name(), decision.getReasonCode());
        assertEquals("client.redemption.region.blocked", decision.getMessageKey());
        assertEquals("US", decision.getCountryCode());
        assertEquals("WA", decision.getStateCode());
        assertEquals("h5", decision.getChannel());
    }

    @Test
    @Tag("local")
    void purchasePayDoesNotRequireKycAgeAgreementsOrRegionPolicy() {
        MemberRegionEligibilityChecker regionChecker = mock(MemberRegionEligibilityChecker.class);
        MemberComplianceGateServiceImpl service = new MemberComplianceGateServiceImpl(regionChecker);
        MemberProfile member = eligibleMember();
        member.setKycStatus("NOT_STARTED");
        member.setAgeConfirmed(false);
        member.setTermsAccepted(false);
        member.setPrivacyAccepted(false);
        member.setSweepstakesRulesAccepted(false);

        MemberComplianceDecision decision = service.evaluate(context(member, MemberComplianceAction.PURCHASE_PAY, "USD"));

        assertTrue(decision.isAllowed());
        assertEquals(MemberComplianceReason.ALLOWED.name(), decision.getReasonCode());
        verify(regionChecker, never()).isEligible(any(), any(), any(), any(), any());
    }

    @Test
    @Tag("local")
    void scGrantRequiresAgreementsButNotKyc() {
        MemberComplianceGateServiceImpl service = new MemberComplianceGateServiceImpl(mock(MemberRegionEligibilityChecker.class));
        MemberProfile member = eligibleMember();
        member.setKycStatus("NOT_STARTED");
        member.setPrivacyAccepted(false);

        MemberComplianceDecision decision = service.evaluate(context(member, MemberComplianceAction.SC_GRANT, "SC"));

        assertFalse(decision.isAllowed());
        assertEquals(MemberComplianceReason.AGREEMENTS_REQUIRED.name(), decision.getReasonCode());
    }

    @Test
    @Tag("local")
    void gameLaunchWithScRequiresAgreementsButGcDoesNot() {
        MemberComplianceGateServiceImpl service = new MemberComplianceGateServiceImpl(mock(MemberRegionEligibilityChecker.class));
        MemberProfile member = eligibleMember();
        member.setSweepstakesRulesAccepted(false);

        MemberComplianceDecision scDecision = service.evaluate(context(member, MemberComplianceAction.GAME_LAUNCH, "SC"));
        MemberComplianceDecision gcDecision = service.evaluate(context(member, MemberComplianceAction.GAME_LAUNCH, "GC"));

        assertFalse(scDecision.isAllowed());
        assertEquals(MemberComplianceReason.AGREEMENTS_REQUIRED.name(), scDecision.getReasonCode());
        assertTrue(gcDecision.isAllowed());
    }

    private MemberComplianceContext context(MemberProfile member, MemberComplianceAction action, String currencyCode) {
        return MemberComplianceContext.builder()
            .tenantId("000000")
            .member(member)
            .action(action)
            .currencyCode(currencyCode)
            .channel("H5")
            .build();
    }

    private MemberProfile eligibleMember() {
        MemberProfile member = new MemberProfile();
        member.setId(1001L);
        member.setTenantId("000000");
        member.setStatus("ACTIVE");
        member.setRiskLevel("NORMAL");
        member.setKycStatus("APPROVED");
        member.setCountryCode("US");
        member.setStateCode("CA");
        member.setAgeConfirmed(true);
        member.setTermsAccepted(true);
        member.setPrivacyAccepted(true);
        member.setSweepstakesRulesAccepted(true);
        return member;
    }
}
