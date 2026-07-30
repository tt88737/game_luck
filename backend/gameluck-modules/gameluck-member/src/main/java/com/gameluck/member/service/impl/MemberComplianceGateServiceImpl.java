package com.gameluck.member.service.impl;

import com.gameluck.common.core.utils.StringUtils;
import com.gameluck.member.compliance.MemberComplianceAction;
import com.gameluck.member.compliance.MemberComplianceContext;
import com.gameluck.member.compliance.MemberComplianceDecision;
import com.gameluck.member.compliance.MemberComplianceReason;
import com.gameluck.member.compliance.MemberRegionEligibilityChecker;
import com.gameluck.member.domain.MemberProfile;
import com.gameluck.member.service.IMemberComplianceGateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class MemberComplianceGateServiceImpl implements IMemberComplianceGateService {

    private static final String DEFAULT_TENANT_ID = "000000";
    private static final String DEFAULT_CURRENCY = "SC";
    private static final String DEFAULT_CHANNEL = "h5";

    private final MemberRegionEligibilityChecker regionEligibilityChecker;

    @Override
    public MemberComplianceDecision evaluate(MemberComplianceContext context) {
        MemberComplianceContext safeContext = context == null ? MemberComplianceContext.builder().build() : context;
        String tenantId = StringUtils.blankToDefault(safeContext.getTenantId(), DEFAULT_TENANT_ID);
        String currencyCode = normalizeCurrency(safeContext.getCurrencyCode());
        String channel = normalizeChannel(safeContext.getChannel());
        MemberProfile member = safeContext.getMember();
        String countryCode = normalizeRegion(member == null ? null : member.getCountryCode());
        String stateCode = normalizeRegion(member == null ? null : member.getStateCode());

        if (member == null) {
            return deny(safeContext, MemberComplianceReason.MEMBER_NOT_EXISTS,
                memberMessage(safeContext.getAction(), "member.not.exists"),
                currencyCode, countryCode, stateCode, channel);
        }
        if (!"ACTIVE".equals(StringUtils.blankToDefault(member.getStatus(), ""))) {
            return deny(safeContext, MemberComplianceReason.MEMBER_INACTIVE,
                memberMessage(safeContext.getAction(), "member.inactive"),
                currencyCode, countryCode, stateCode, channel);
        }
        if ("HIGH".equals(StringUtils.blankToDefault(member.getRiskLevel(), ""))) {
            return deny(safeContext, MemberComplianceReason.RISK_BLOCKED,
                memberMessage(safeContext.getAction(), "risk.blocked"),
                currencyCode, countryCode, stateCode, channel);
        }
        if (requiresAge(safeContext.getAction(), currencyCode) && !Boolean.TRUE.equals(member.getAgeConfirmed())) {
            return deny(safeContext, MemberComplianceReason.AGE_REQUIRED, "client.redemption.age.required",
                currencyCode, countryCode, stateCode, channel);
        }
        if (requiresAgreements(safeContext.getAction(), currencyCode)
            && (!Boolean.TRUE.equals(member.getTermsAccepted())
            || !Boolean.TRUE.equals(member.getPrivacyAccepted())
            || !Boolean.TRUE.equals(member.getSweepstakesRulesAccepted()))) {
            return deny(safeContext, MemberComplianceReason.AGREEMENTS_REQUIRED, "client.redemption.agreements.required",
                currencyCode, countryCode, stateCode, channel);
        }
        if (requiresKyc(safeContext.getAction())
            && !"APPROVED".equals(StringUtils.blankToDefault(member.getKycStatus(), "NOT_STARTED"))) {
            return deny(safeContext, MemberComplianceReason.KYC_REQUIRED, "client.redemption.kyc.required",
                currencyCode, countryCode, stateCode, channel);
        }
        if (requiresRegionPolicy(safeContext.getAction())
            && !regionEligibilityChecker.isEligible(tenantId, currencyCode, countryCode, stateCode, channel)) {
            return deny(safeContext, MemberComplianceReason.REGION_BLOCKED, "client.redemption.region.blocked",
                currencyCode, countryCode, stateCode, channel);
        }
        return MemberComplianceDecision.allow(safeContext, currencyCode, countryCode, stateCode, channel);
    }

    private MemberComplianceDecision deny(MemberComplianceContext context, MemberComplianceReason reason,
                                          String messageKey, String currencyCode, String countryCode,
                                          String stateCode, String channel) {
        return MemberComplianceDecision.deny(context, reason, messageKey, currencyCode, countryCode, stateCode, channel);
    }

    private boolean requiresAge(MemberComplianceAction action, String currencyCode) {
        return MemberComplianceAction.REDEMPTION_REQUEST.equals(action)
            || MemberComplianceAction.SC_GRANT.equals(action)
            || MemberComplianceAction.AMOE_REQUEST.equals(action)
            || (MemberComplianceAction.GAME_LAUNCH.equals(action) && "SC".equals(currencyCode));
    }

    private boolean requiresAgreements(MemberComplianceAction action, String currencyCode) {
        return requiresAge(action, currencyCode);
    }

    private boolean requiresKyc(MemberComplianceAction action) {
        return MemberComplianceAction.REDEMPTION_REQUEST.equals(action);
    }

    private boolean requiresRegionPolicy(MemberComplianceAction action) {
        return MemberComplianceAction.REDEMPTION_REQUEST.equals(action)
            || MemberComplianceAction.AMOE_REQUEST.equals(action);
    }

    private String memberMessage(MemberComplianceAction action, String suffix) {
        String prefix = MemberComplianceAction.PURCHASE_PAY.equals(action)
            ? "client.purchase."
            : "client.redemption.";
        return prefix + suffix;
    }

    private String normalizeCurrency(String value) {
        return StringUtils.blankToDefault(value, DEFAULT_CURRENCY).trim().toUpperCase();
    }

    private String normalizeChannel(String value) {
        return StringUtils.blankToDefault(value, DEFAULT_CHANNEL).trim().toLowerCase();
    }

    private String normalizeRegion(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }
}
