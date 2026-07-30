package com.gameluck.member.service;

import com.gameluck.member.compliance.MemberComplianceContext;
import com.gameluck.member.compliance.MemberComplianceDecision;

public interface IMemberComplianceGateService {

    MemberComplianceDecision evaluate(MemberComplianceContext context);
}
