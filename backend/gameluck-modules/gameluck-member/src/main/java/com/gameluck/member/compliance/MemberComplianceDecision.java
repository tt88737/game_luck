package com.gameluck.member.compliance;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MemberComplianceDecision {

    private boolean allowed;

    private String action;

    private String messageKey;

    private String reasonCode;

    private Long memberId;

    private String currencyCode;

    private String countryCode;

    private String stateCode;

    private String channel;

    public static MemberComplianceDecision allow(MemberComplianceContext context, String currencyCode,
                                                 String countryCode, String stateCode, String channel) {
        return base(context, MemberComplianceReason.ALLOWED, currencyCode, countryCode, stateCode, channel)
            .allowed(true)
            .build();
    }

    public static MemberComplianceDecision deny(MemberComplianceContext context, MemberComplianceReason reason,
                                                String messageKey, String currencyCode, String countryCode,
                                                String stateCode, String channel) {
        return base(context, reason, currencyCode, countryCode, stateCode, channel)
            .allowed(false)
            .messageKey(messageKey)
            .build();
    }

    private static MemberComplianceDecisionBuilder base(MemberComplianceContext context, MemberComplianceReason reason,
                                                        String currencyCode, String countryCode, String stateCode,
                                                        String channel) {
        return MemberComplianceDecision.builder()
            .action(context == null || context.getAction() == null ? null : context.getAction().name())
            .reasonCode(reason.name())
            .memberId(context == null || context.getMember() == null ? null : context.getMember().getId())
            .currencyCode(currencyCode)
            .countryCode(countryCode)
            .stateCode(stateCode)
            .channel(channel);
    }
}
