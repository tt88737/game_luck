package com.gameluck.member.compliance;

import com.gameluck.member.domain.MemberProfile;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MemberComplianceContext {

    private String tenantId;

    private MemberProfile member;

    private MemberComplianceAction action;

    private String currencyCode;

    private String channel;
}
