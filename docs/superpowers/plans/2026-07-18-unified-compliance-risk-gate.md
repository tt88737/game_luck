# Unified Compliance/Risk Gate Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a shared backend compliance/risk gate and refactor C-side redemption to use it while preserving current block order and messages.

**Architecture:** Put action/decision/context types and the gate service in `gameluck-member`, because the core input is `MemberProfile`. Keep region policy behind a small `MemberRegionEligibilityChecker` bridge interface implemented in `gameluck-redemption`, avoiding direct member-module dependency on redemption internals.

**Tech Stack:** Spring Boot, Java 17, Lombok, JUnit 5, Mockito, Maven local profile.

---

## File Structure

- Create `backend/gameluck-modules/gameluck-member/src/main/java/com/gameluck/member/compliance/MemberComplianceAction.java`
  - Enum of supported C-side action codes.
- Create `backend/gameluck-modules/gameluck-member/src/main/java/com/gameluck/member/compliance/MemberComplianceReason.java`
  - Enum of stable allow/deny reason codes.
- Create `backend/gameluck-modules/gameluck-member/src/main/java/com/gameluck/member/compliance/MemberComplianceContext.java`
  - Evaluation input object.
- Create `backend/gameluck-modules/gameluck-member/src/main/java/com/gameluck/member/compliance/MemberComplianceDecision.java`
  - Evaluation output object.
- Create `backend/gameluck-modules/gameluck-member/src/main/java/com/gameluck/member/compliance/MemberRegionEligibilityChecker.java`
  - Bridge interface for region/channel/currency policy checks.
- Create `backend/gameluck-modules/gameluck-member/src/main/java/com/gameluck/member/service/IMemberComplianceGateService.java`
  - Gate service interface.
- Create `backend/gameluck-modules/gameluck-member/src/main/java/com/gameluck/member/service/impl/MemberComplianceGateServiceImpl.java`
  - Shared gate implementation.
- Create `backend/gameluck-modules/gameluck-member/src/test/java/com/gameluck/member/service/impl/MemberComplianceGateServiceImplTest.java`
  - Unit coverage for action-specific decisions.
- Create `backend/gameluck-modules/gameluck-redemption/src/main/java/com/gameluck/redemption/service/impl/RedemptionMemberRegionEligibilityChecker.java`
  - Adapter from the member bridge to `IRedemptionEligibilityPolicyService`.
- Modify `backend/gameluck-modules/gameluck-redemption/src/main/java/com/gameluck/redemption/client/service/ClientRedemptionService.java`
  - Replace local status/risk/consent/KYC/region checks with shared gate decision.
- Modify `backend/gameluck-modules/gameluck-redemption/src/test/java/com/gameluck/redemption/client/service/ClientRedemptionServiceTest.java`
  - Update constructor and mocks to use `IMemberComplianceGateService`.
- Modify `progress.md`
  - Record implementation and verification results.
- Modify `task_plan.md`
  - Mark Phase 37 complete only after verification passes.

Do not commit in this workspace unless the user explicitly asks for a git commit.

---

### Task 1: Shared Compliance Gate Domain And Tests

**Files:**
- Create: `backend/gameluck-modules/gameluck-member/src/main/java/com/gameluck/member/compliance/MemberComplianceAction.java`
- Create: `backend/gameluck-modules/gameluck-member/src/main/java/com/gameluck/member/compliance/MemberComplianceReason.java`
- Create: `backend/gameluck-modules/gameluck-member/src/main/java/com/gameluck/member/compliance/MemberComplianceContext.java`
- Create: `backend/gameluck-modules/gameluck-member/src/main/java/com/gameluck/member/compliance/MemberComplianceDecision.java`
- Create: `backend/gameluck-modules/gameluck-member/src/main/java/com/gameluck/member/compliance/MemberRegionEligibilityChecker.java`
- Create: `backend/gameluck-modules/gameluck-member/src/main/java/com/gameluck/member/service/IMemberComplianceGateService.java`
- Create: `backend/gameluck-modules/gameluck-member/src/test/java/com/gameluck/member/service/impl/MemberComplianceGateServiceImplTest.java`

- [x] **Step 1: Create failing tests for member/status/risk/KYC/region/action behavior**

Create `MemberComplianceGateServiceImplTest.java`:

```java
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
        verify(regionChecker, never()).isEligible(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
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
        verify(regionChecker, never()).isEligible(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
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
    void purchasePayDoesNotRequireKycOrRegionPolicy() {
        MemberRegionEligibilityChecker regionChecker = mock(MemberRegionEligibilityChecker.class);
        MemberComplianceGateServiceImpl service = new MemberComplianceGateServiceImpl(regionChecker);
        MemberProfile member = eligibleMember();
        member.setKycStatus("NOT_STARTED");

        MemberComplianceDecision decision = service.evaluate(context(member, MemberComplianceAction.PURCHASE_PAY, "USD"));

        assertTrue(decision.isAllowed());
        assertEquals(MemberComplianceReason.ALLOWED.name(), decision.getReasonCode());
        verify(regionChecker, never()).isEligible(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
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
```

- [x] **Step 2: Run tests to verify they fail because classes do not exist**

Run:

```powershell
$env:MAVEN_OPTS='-Xmx768m -XX:MaxMetaspaceSize=256m -XX:ReservedCodeCacheSize=128m'
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-modules/gameluck-member -am -Plocal -DskipTests=false "-Dtest=MemberComplianceGateServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" -DforkCount=0 test
```

Expected: compilation fails with missing `MemberCompliance*` and `MemberRegionEligibilityChecker` classes.

- [x] **Step 3: Add compliance action enum**

Create `MemberComplianceAction.java`:

```java
package com.gameluck.member.compliance;

public enum MemberComplianceAction {
    REDEMPTION_REQUEST,
    PURCHASE_PAY,
    SC_GRANT,
    GAME_LAUNCH,
    AMOE_REQUEST
}
```

- [x] **Step 4: Add compliance reason enum**

Create `MemberComplianceReason.java`:

```java
package com.gameluck.member.compliance;

public enum MemberComplianceReason {
    MEMBER_NOT_EXISTS,
    MEMBER_INACTIVE,
    RISK_BLOCKED,
    AGE_REQUIRED,
    AGREEMENTS_REQUIRED,
    KYC_REQUIRED,
    REGION_BLOCKED,
    ALLOWED
}
```

- [x] **Step 5: Add compliance context**

Create `MemberComplianceContext.java`:

```java
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
```

- [x] **Step 6: Add compliance decision**

Create `MemberComplianceDecision.java`:

```java
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

    public static MemberComplianceDecision allow(MemberComplianceContext context, String currencyCode, String countryCode, String stateCode, String channel) {
        return base(context, MemberComplianceReason.ALLOWED, currencyCode, countryCode, stateCode, channel)
            .allowed(true)
            .build();
    }

    public static MemberComplianceDecision deny(MemberComplianceContext context, MemberComplianceReason reason, String messageKey,
                                                String currencyCode, String countryCode, String stateCode, String channel) {
        return base(context, reason, currencyCode, countryCode, stateCode, channel)
            .allowed(false)
            .messageKey(messageKey)
            .build();
    }

    private static MemberComplianceDecisionBuilder base(MemberComplianceContext context, MemberComplianceReason reason,
                                                        String currencyCode, String countryCode, String stateCode, String channel) {
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
```

- [x] **Step 7: Add region bridge interface**

Create `MemberRegionEligibilityChecker.java`:

```java
package com.gameluck.member.compliance;

@FunctionalInterface
public interface MemberRegionEligibilityChecker {

    boolean isEligible(String tenantId, String currencyCode, String countryCode, String stateCode, String channel);
}
```

- [x] **Step 8: Add gate service interface**

Create `IMemberComplianceGateService.java`:

```java
package com.gameluck.member.service;

import com.gameluck.member.compliance.MemberComplianceContext;
import com.gameluck.member.compliance.MemberComplianceDecision;

public interface IMemberComplianceGateService {

    MemberComplianceDecision evaluate(MemberComplianceContext context);
}
```

### Task 2: Shared Compliance Gate Implementation

**Files:**
- Create: `backend/gameluck-modules/gameluck-member/src/main/java/com/gameluck/member/service/impl/MemberComplianceGateServiceImpl.java`
- Test: `backend/gameluck-modules/gameluck-member/src/test/java/com/gameluck/member/service/impl/MemberComplianceGateServiceImplTest.java`

- [x] **Step 1: Implement the gate service**

Create `MemberComplianceGateServiceImpl.java`:

```java
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
            return deny(safeContext, MemberComplianceReason.MEMBER_NOT_EXISTS, "client.redemption.member.not.exists", currencyCode, countryCode, stateCode, channel);
        }
        if (!"ACTIVE".equals(StringUtils.blankToDefault(member.getStatus(), ""))) {
            return deny(safeContext, MemberComplianceReason.MEMBER_INACTIVE, "client.redemption.member.inactive", currencyCode, countryCode, stateCode, channel);
        }
        if ("HIGH".equals(StringUtils.blankToDefault(member.getRiskLevel(), ""))) {
            return deny(safeContext, MemberComplianceReason.RISK_BLOCKED, "client.redemption.risk.blocked", currencyCode, countryCode, stateCode, channel);
        }
        if (requiresAge(safeContext.getAction(), currencyCode) && !Boolean.TRUE.equals(member.getAgeConfirmed())) {
            return deny(safeContext, MemberComplianceReason.AGE_REQUIRED, "client.redemption.age.required", currencyCode, countryCode, stateCode, channel);
        }
        if (requiresAgreements(safeContext.getAction(), currencyCode)
            && (!Boolean.TRUE.equals(member.getTermsAccepted())
            || !Boolean.TRUE.equals(member.getPrivacyAccepted())
            || !Boolean.TRUE.equals(member.getSweepstakesRulesAccepted()))) {
            return deny(safeContext, MemberComplianceReason.AGREEMENTS_REQUIRED, "client.redemption.agreements.required", currencyCode, countryCode, stateCode, channel);
        }
        if (requiresKyc(safeContext.getAction()) && !"APPROVED".equals(StringUtils.blankToDefault(member.getKycStatus(), "NOT_STARTED"))) {
            return deny(safeContext, MemberComplianceReason.KYC_REQUIRED, "client.redemption.kyc.required", currencyCode, countryCode, stateCode, channel);
        }
        if (requiresRegionPolicy(safeContext.getAction())
            && !regionEligibilityChecker.isEligible(tenantId, currencyCode, countryCode, stateCode, channel)) {
            return deny(safeContext, MemberComplianceReason.REGION_BLOCKED, "client.redemption.region.blocked", currencyCode, countryCode, stateCode, channel);
        }
        return MemberComplianceDecision.allow(safeContext, currencyCode, countryCode, stateCode, channel);
    }

    private MemberComplianceDecision deny(MemberComplianceContext context, MemberComplianceReason reason, String messageKey,
                                          String currencyCode, String countryCode, String stateCode, String channel) {
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
```

- [x] **Step 2: Run shared gate tests**

Run:

```powershell
$env:MAVEN_OPTS='-Xmx768m -XX:MaxMetaspaceSize=256m -XX:ReservedCodeCacheSize=128m'
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-modules/gameluck-member -am -Plocal -DskipTests=false "-Dtest=MemberComplianceGateServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" -DforkCount=0 test
```

Expected: `MemberComplianceGateServiceImplTest` passes.

### Task 3: Redemption Region Bridge

**Files:**
- Create: `backend/gameluck-modules/gameluck-redemption/src/main/java/com/gameluck/redemption/service/impl/RedemptionMemberRegionEligibilityChecker.java`
- Test: `backend/gameluck-modules/gameluck-member/src/test/java/com/gameluck/member/service/impl/MemberComplianceGateServiceImplTest.java`

- [x] **Step 1: Add redemption bridge implementation**

Create `RedemptionMemberRegionEligibilityChecker.java`:

```java
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
```

- [x] **Step 2: Compile member and redemption modules**

Run:

```powershell
$env:MAVEN_OPTS='-Xmx768m -XX:MaxMetaspaceSize=256m -XX:ReservedCodeCacheSize=128m'
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-modules/gameluck-member,gameluck-modules/gameluck-redemption -am -Plocal -DskipTests compile
```

Expected: compile succeeds, proving the bridge does not create a module cycle.

### Task 4: Refactor C-Side Redemption To Use Shared Gate

**Files:**
- Modify: `backend/gameluck-modules/gameluck-redemption/src/main/java/com/gameluck/redemption/client/service/ClientRedemptionService.java`
- Modify: `backend/gameluck-modules/gameluck-redemption/src/test/java/com/gameluck/redemption/client/service/ClientRedemptionServiceTest.java`

- [x] **Step 1: Update redemption service constructor dependency**

In `ClientRedemptionService.java`, remove:

```java
import com.gameluck.common.core.utils.StringUtils;
import com.gameluck.redemption.service.IRedemptionEligibilityPolicyService;
```

Add:

```java
import com.gameluck.member.compliance.MemberComplianceAction;
import com.gameluck.member.compliance.MemberComplianceContext;
import com.gameluck.member.compliance.MemberComplianceDecision;
import com.gameluck.member.service.IMemberComplianceGateService;
```

Replace field:

```java
private final IRedemptionEligibilityPolicyService eligibilityPolicyService;
```

with:

```java
private final IMemberComplianceGateService complianceGateService;
```

- [x] **Step 2: Replace direct gate methods with shared decision call**

In `request(...)`, replace:

```java
validateRedemptionGate(member);
if (!SUPPORTED_CURRENCY.equals(bo.getCurrencyCode())) {
    throw new ServiceException(MessageUtils.message("client.redemption.currency.unsupported"));
}
validateRedemptionPolicy(member, SUPPORTED_CURRENCY);
```

with:

```java
MemberComplianceDecision decision = complianceGateService.evaluate(MemberComplianceContext.builder()
    .tenantId(TENANT_ID)
    .member(member)
    .action(MemberComplianceAction.REDEMPTION_REQUEST)
    .currencyCode(SUPPORTED_CURRENCY)
    .channel("h5")
    .build());
if (!decision.isAllowed()) {
    throw new ServiceException(MessageUtils.message(decision.getMessageKey()));
}
if (!SUPPORTED_CURRENCY.equals(bo.getCurrencyCode())) {
    throw new ServiceException(MessageUtils.message("client.redemption.currency.unsupported"));
}
```

Delete these private methods because the shared gate now owns them:

```java
private void validateRedemptionGate(MemberProfile member) { ... }
private void validateRedemptionPolicy(MemberProfile member, String currencyCode) { ... }
private String normalizeRegion(String value) { ... }
```

- [x] **Step 3: Update redemption service tests to mock the shared gate**

In `ClientRedemptionServiceTest.java`, remove import:

```java
import com.gameluck.redemption.service.IRedemptionEligibilityPolicyService;
```

Add imports:

```java
import com.gameluck.member.compliance.MemberComplianceContext;
import com.gameluck.member.compliance.MemberComplianceDecision;
import com.gameluck.member.compliance.MemberComplianceReason;
import com.gameluck.member.service.IMemberComplianceGateService;
```

Replace each service construction that currently creates `IRedemptionEligibilityPolicyService eligibilityPolicyService = mock(...)` and passes it to the constructor. Use:

```java
IMemberComplianceGateService complianceGateService = mock(IMemberComplianceGateService.class);
when(complianceGateService.evaluate(any(MemberComplianceContext.class))).thenReturn(allowDecision());
ClientRedemptionService service = new ClientRedemptionService(mapper, orderService, memberMapper, complianceGateService, tokenService);
```

Add helper methods near the existing helpers:

```java
private MemberComplianceDecision allowDecision() {
    return MemberComplianceDecision.builder()
        .allowed(true)
        .reasonCode(MemberComplianceReason.ALLOWED.name())
        .messageKey(null)
        .build();
}

private MemberComplianceDecision denyDecision(String messageKey, MemberComplianceReason reason) {
    return MemberComplianceDecision.builder()
        .allowed(false)
        .reasonCode(reason.name())
        .messageKey(messageKey)
        .build();
}
```

Update `assertGateFailure(...)` to use the shared gate denial instead of mutating direct member fields:

```java
private ServiceException assertGateFailure(MemberProfile member, String messageKey, MemberComplianceReason reason) {
    IRedemptionOrderService orderService = mock(IRedemptionOrderService.class);
    MemberProfileMapper memberMapper = mock(MemberProfileMapper.class);
    IMemberComplianceGateService complianceGateService = mock(IMemberComplianceGateService.class);
    ClientTokenService tokenService = new ClientTokenService();
    ClientRedemptionService service = new ClientRedemptionService(
        mock(RedemptionOrderMapper.class), orderService, memberMapper, complianceGateService, tokenService);
    when(memberMapper.selectClientMember("000000", 1001L)).thenReturn(member);
    when(complianceGateService.evaluate(any(MemberComplianceContext.class))).thenReturn(denyDecision(messageKey, reason));
    ClientRedemptionRequestBo bo = redemptionRequest("SC");

    ServiceException exception = assertThrows(ServiceException.class,
        () -> service.request("Bearer " + tokenService.issue(1001L), bo));

    verify(orderService, never()).insertByBo(any(RedemptionOrderBo.class));
    return exception;
}
```

Then update the existing gate tests:

```java
ServiceException exception = assertGateFailure(member, "client.redemption.member.inactive", MemberComplianceReason.MEMBER_INACTIVE);
```

Use corresponding reason enums for risk, age, agreements, KYC, and region.

- [x] **Step 4: Add assertion that redemption passes loaded member to the shared gate**

In `requestCreatesScRedemptionForCurrentMember`, add an `ArgumentCaptor<MemberComplianceContext>`:

```java
ArgumentCaptor<MemberComplianceContext> contextCaptor = ArgumentCaptor.forClass(MemberComplianceContext.class);
verify(complianceGateService).evaluate(contextCaptor.capture());
MemberComplianceContext context = contextCaptor.getValue();
assertEquals("000000", context.getTenantId());
assertEquals("SC", context.getCurrencyCode());
assertEquals("h5", context.getChannel());
assertEquals(member.getId(), context.getMember().getId());
assertEquals("REDEMPTION_REQUEST", context.getAction().name());
```

Make sure this test stores `eligibleMember()` in a local variable named `member` before stubbing mapper.

- [x] **Step 5: Run redemption tests**

Run:

```powershell
$env:MAVEN_OPTS='-Xmx768m -XX:MaxMetaspaceSize=256m -XX:ReservedCodeCacheSize=128m'
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-modules/gameluck-redemption -am -Plocal -DskipTests=false "-Dtest=ClientRedemptionServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" -DforkCount=0 test
```

Expected: `ClientRedemptionServiceTest` passes and still verifies no order creation on denial.

### Task 5: Focused Verification And Runtime Smoke

**Files:**
- Modify: `progress.md`
- Modify: `task_plan.md`

- [x] **Step 1: Run focused backend tests**

Run:

```powershell
$env:MAVEN_OPTS='-Xmx768m -XX:MaxMetaspaceSize=256m -XX:ReservedCodeCacheSize=128m'
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-modules/gameluck-member,gameluck-modules/gameluck-redemption -am -Plocal -DskipTests=false "-Dtest=MemberComplianceGateServiceImplTest,ClientRedemptionServiceTest,MemberProfileServiceImplTest,ClientAuthServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" -DforkCount=0 test
```

Expected: all specified tests pass.

- [x] **Step 2: Package backend**

If a Java backend process is locking `backend/gameluck-admin/target/gameluck-admin.jar`, stop only that process after verifying its command line points to the local jar. Then run:

```powershell
$env:MAVEN_OPTS='-Xmx768m -XX:MaxMetaspaceSize=256m -XX:ReservedCodeCacheSize=128m'
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-admin -am package -Plocal -DskipTests
```

Expected: `BUILD SUCCESS`.

- [x] **Step 3: Restart backend from refreshed jar and check health**

Run:

```powershell
java -jar backend\gameluck-admin\target\gameluck-admin.jar --spring.profiles.active=local --captcha.enable=false
```

Verify:

```powershell
Invoke-WebRequest -Uri http://localhost:8080/ -UseBasicParsing
```

Expected: HTTP `200`.

- [x] **Step 4: Runtime smoke for KYC and risk ordering**

Use the existing direct HTTP + MySQL smoke style:

1. Register a new H5 user in `US/CA` with all agreements true.
2. Call `POST /api/client/redemptions/request` for `SC 1.00`.
3. Expected: business `code=500`, message `client.redemption.kyc.required` localized, and zero redemption orders.
4. Update `gl_member_profile.kyc_status='APPROVED'` for that member.
5. Call redemption again.
6. Expected: business `code=200` and one redemption order.
7. Update `gl_member_profile.risk_level='HIGH'` for that member.
8. Call redemption again.
9. Expected: business `code=500`, message `client.redemption.risk.blocked`, and order count does not increase.

- [x] **Step 5: Static check that redemption no longer owns duplicated gates**

Run:

```powershell
rg -n "validateRedemptionGate|validateRedemptionPolicy|member\\.getRiskLevel|member\\.getKycStatus|member\\.getAgeConfirmed|member\\.getTermsAccepted|member\\.getPrivacyAccepted|member\\.getSweepstakesRulesAccepted|eligibilityPolicyService" backend\gameluck-modules\gameluck-redemption\src\main\java\com\gameluck\redemption\client\service\ClientRedemptionService.java
```

Expected: no matches.

- [x] **Step 6: Final whitespace check**

Run:

```powershell
git diff --check
```

Expected: exit code `0`; CRLF warnings are acceptable.

- [x] **Step 7: Record completion**

Update `progress.md` with:

- Files created and modified.
- Focused backend test result.
- Backend package result.
- Runtime smoke result.
- Static duplicate-gate scan result.
- `git diff --check` result.

Update `task_plan.md` Phase 37 from `in_progress` to `complete` only after all required verification passes.

## Self Review

- Spec coverage: tasks cover shared decision types, action-specific rules, bridge interface, redemption refactor, tests, package, runtime smoke, and duplicate-gate static scan.
- Scope control: plan integrates only redemption and shared gate tests; purchase, promotion, game, and AMOE remain defined by the shared action model but are not wired into production flows in this phase.
- Placeholder scan: no intentional TODO/TBD placeholders are left.
- Type consistency: action, reason, context, decision, and service names match across tasks.
