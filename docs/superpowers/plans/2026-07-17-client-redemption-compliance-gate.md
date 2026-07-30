# Client Redemption Compliance Gate Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a lightweight member-profile compliance gate before H5 users can submit SC redemption requests.

**Architecture:** Keep the gate inside `ClientRedemptionService` so the C-side request is blocked before any redemption order or wallet freeze is created. Reuse `MemberProfileMapper.selectClientMember(...)` and existing profile fields; do not add schema or full KYC workflow in this phase.

**Tech Stack:** Java 17, Spring Boot, MyBatis Plus, JUnit 5, Mockito, Maven.

---

### Task 1: Add RED tests for C-side redemption gate

**Files:**
- Modify: `backend/gameluck-modules/gameluck-redemption/src/test/java/com/gameluck/redemption/client/service/ClientRedemptionServiceTest.java`

- [ ] **Step 1: Add mapper and member imports**

Add imports:

```java
import com.gameluck.member.domain.MemberProfile;
import com.gameluck.member.mapper.MemberProfileMapper;

import static org.mockito.Mockito.never;
```

- [ ] **Step 2: Update existing service construction to include member mapper**

Each `new ClientRedemptionService(...)` call should pass `MemberProfileMapper` between the redemption order service and token service.

Example:

```java
MemberProfileMapper memberMapper = mock(MemberProfileMapper.class);
when(memberMapper.selectClientMember("000000", 1001L)).thenReturn(eligibleMember());
ClientRedemptionService service = new ClientRedemptionService(mapper, orderService, memberMapper, tokenService);
```

- [ ] **Step 3: Add failing tests**

Add tests for:
- `requestRejectsMissingMember`
- `requestRejectsInactiveMember`
- `requestRejectsHighRiskMember`
- `requestRejectsMissingAgeConfirmation`
- `requestRejectsMissingAgreements`
- `requestRejectsDeniedRegion`

Each blocked test should assert the expected message key and verify `orderService.insertByBo(...)` was never called.

- [ ] **Step 4: Run RED**

Run:

```powershell
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-modules/gameluck-redemption -am -Plocal -DskipTests=false "-Dtest=ClientRedemptionServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" -DforkCount=0 test
```

Expected: compile failure because `ClientRedemptionService` does not yet accept `MemberProfileMapper` and gate behavior is missing.

### Task 2: Implement minimal backend gate

**Files:**
- Modify: `backend/gameluck-modules/gameluck-redemption/src/main/java/com/gameluck/redemption/client/service/ClientRedemptionService.java`
- Modify: `backend/gameluck-admin/src/main/resources/i18n/messages.properties`
- Modify: `backend/gameluck-admin/src/main/resources/i18n/messages_zh_CN.properties`
- Modify: `backend/gameluck-admin/src/main/resources/i18n/messages_en_US.properties`

- [ ] **Step 1: Inject member mapper**

Add:

```java
import com.gameluck.member.domain.MemberProfile;
import com.gameluck.member.mapper.MemberProfileMapper;
```

Add constructor field:

```java
private final MemberProfileMapper memberProfileMapper;
```

- [ ] **Step 2: Add gate validation before order creation**

In `request(...)`, load and validate member immediately after resolving `memberId`:

```java
MemberProfile member = memberProfileMapper.selectClientMember(TENANT_ID, memberId);
validateRedemptionGate(member);
```

Add helper methods:

```java
private void validateRedemptionGate(MemberProfile member) {
    if (member == null) {
        throw new ServiceException(MessageUtils.message("client.redemption.member.not.exists"));
    }
    if (!"ACTIVE".equals(member.getStatus())) {
        throw new ServiceException(MessageUtils.message("client.redemption.member.inactive"));
    }
    if ("HIGH".equals(member.getRiskLevel())) {
        throw new ServiceException(MessageUtils.message("client.redemption.risk.blocked"));
    }
    if (!Boolean.TRUE.equals(member.getAgeConfirmed())) {
        throw new ServiceException(MessageUtils.message("client.redemption.age.required"));
    }
    if (!Boolean.TRUE.equals(member.getTermsAccepted())
        || !Boolean.TRUE.equals(member.getPrivacyAccepted())
        || !Boolean.TRUE.equals(member.getSweepstakesRulesAccepted())) {
        throw new ServiceException(MessageUtils.message("client.redemption.agreements.required"));
    }
    if (isDeniedRegion(member)) {
        throw new ServiceException(MessageUtils.message("client.redemption.region.blocked"));
    }
}

private boolean isDeniedRegion(MemberProfile member) {
    String country = normalizeRegion(member.getCountryCode());
    String state = normalizeRegion(member.getStateCode());
    return "US".equals(country) && ("WA".equals(state) || "ID".equals(state) || "NV".equals(state) || "MI".equals(state));
}

private String normalizeRegion(String value) {
    return value == null ? "" : value.trim().toUpperCase();
}
```

- [ ] **Step 3: Add i18n keys**

Add keys to default and Chinese files:

```properties
client.redemption.member.not.exists=会员不存在
client.redemption.member.inactive=账号状态不可兑换
client.redemption.risk.blocked=账号风险等级暂不可兑换
client.redemption.age.required=请先确认您已达到法定年龄
client.redemption.agreements.required=请先接受服务条款、隐私政策和抽奖规则
client.redemption.region.blocked=当前地区暂不支持兑换
```

Add English keys:

```properties
client.redemption.member.not.exists=Member does not exist.
client.redemption.member.inactive=This account status is not eligible for redemption.
client.redemption.risk.blocked=This account risk level is not eligible for redemption.
client.redemption.age.required=Please confirm you meet the legal age requirement first.
client.redemption.agreements.required=Please accept the Terms, Privacy Policy, and Sweepstakes Rules first.
client.redemption.region.blocked=Redemption is not available in your current region.
```

- [ ] **Step 4: Run GREEN**

Run the same focused Maven command from Task 1.

Expected: `BUILD SUCCESS`.

### Task 3: Verify broader backend compatibility

**Files:**
- No code changes.

- [ ] **Step 1: Run redemption focused service tests**

Run:

```powershell
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-modules/gameluck-redemption -am -Plocal -DskipTests=false "-Dtest=ClientRedemptionServiceTest,RedemptionOrderServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" -DforkCount=0 test
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 2: Run backend compile**

Run:

```powershell
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-admin -am compile -Plocal -DskipTests
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 3: Run whitespace check**

Run:

```powershell
git diff --check
```

Expected: no whitespace errors; CRLF warnings are acceptable in this workspace.

### Task 4: Record Phase 26

**Files:**
- Modify: `task_plan.md`
- Modify: `progress.md`

- [ ] **Step 1: Add Phase 26 to task plan**

Add:

```markdown
| 26. Client redemption compliance gate | complete | Block C-side redemption requests for missing member, inactive/high-risk accounts, missing age/agreement confirmations, and denied regions before order creation | docs/superpowers/plans/2026-07-17-client-redemption-compliance-gate.md |
```

- [ ] **Step 2: Add progress notes**

Record the RED command, GREEN command, compile result, and any runtime observations.

---

## Self-Review

- Spec coverage: The plan covers service validation, i18n, focused tests, backend compile, and planning-file updates.
- Placeholder scan: No placeholder implementation steps remain.
- Type consistency: The new constructor field and test setup both use `MemberProfileMapper`.
