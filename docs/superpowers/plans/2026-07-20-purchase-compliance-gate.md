# Purchase Compliance Gate Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** Route every new C-side purchase order through the unified member compliance gate while preserving idempotent retries and keeping purchase free of KYC and redemption-region requirements.

**Architecture:** The member module remains the owner of compliance decisions and action-specific message keys. The payment module loads the authenticated member, builds a `PURCHASE_PAY` context after resolving the offer, and stops before order/payment/wallet side effects when denied. Existing orders remain the first-return path for idempotency.

**Tech Stack:** Java 17, Spring Boot, Maven multi-module build, MyBatis Plus, JUnit 5, Mockito, GameLuck i18n bundles, MySQL local profile.

---

## File Structure

- Modify `backend/gameluck-modules/gameluck-member/src/test/java/com/gameluck/member/service/impl/MemberComplianceGateServiceImplTest.java`
  - Prove purchase decisions use purchase messages and do not require KYC, agreements, age, or region policy.
- Modify `backend/gameluck-modules/gameluck-member/src/main/java/com/gameluck/member/service/impl/MemberComplianceGateServiceImpl.java`
  - Select member/risk message keys by action without changing shared reason ordering.
- Modify `backend/gameluck-modules/gameluck-payment/pom.xml`
  - Add the payment-to-member module dependency required for the shared gate and member mapper.
- Modify `backend/gameluck-modules/gameluck-payment/src/test/java/com/gameluck/payment/client/service/ClientPurchaseServiceTest.java`
  - Cover denied side effects, allowed context, and idempotent bypass.
- Modify `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/client/service/ClientPurchaseService.java`
  - Load the member, evaluate `PURCHASE_PAY`, and throw the decision message before limits and mutations.
- Modify `backend/gameluck-admin/src/main/resources/i18n/messages.properties`
- Modify `backend/gameluck-admin/src/main/resources/i18n/messages_zh_CN.properties`
- Modify `backend/gameluck-admin/src/main/resources/i18n/messages_en_US.properties`
  - Add localized purchase member/risk messages.
- Modify `docs/superpowers/plans/2026-07-20-purchase-compliance-gate.md`
  - Track completed steps.
- Modify `progress.md`
  - Record implementation and fresh verification evidence.
- Modify `task_plan.md`
  - Mark Phase 40 complete only after all verification passes.

Do not create a Git commit unless the user explicitly requests one. The workspace already contains multiple uncommitted phases.

### Task 1: Purchase-Specific Compliance Decisions

**Files:**
- Test: `backend/gameluck-modules/gameluck-member/src/test/java/com/gameluck/member/service/impl/MemberComplianceGateServiceImplTest.java`
- Modify: `backend/gameluck-modules/gameluck-member/src/main/java/com/gameluck/member/service/impl/MemberComplianceGateServiceImpl.java`

- [x] **Step 1: Write failing purchase message tests**

Add three focused tests. Use the existing `context(...)` and `eligibleMember()` helpers:

```java
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
    assertEquals("client.purchase.risk.blocked", decision.getMessageKey());
}
```

- [x] **Step 2: Strengthen the existing purchase allowance test**

Rename `purchasePayDoesNotRequireKycOrRegionPolicy` to `purchasePayDoesNotRequireKycAgeAgreementsOrRegionPolicy` and set all non-purchase requirements to false:

```java
member.setKycStatus("NOT_STARTED");
member.setAgeConfirmed(false);
member.setTermsAccepted(false);
member.setPrivacyAccepted(false);
member.setSweepstakesRulesAccepted(false);
```

Keep the allow assertion and `verify(regionChecker, never()).isEligible(...)` assertion.

- [x] **Step 3: Run the member test and verify RED**

Run:

```powershell
$env:MAVEN_OPTS='-Xmx768m -XX:MaxMetaspaceSize=256m -XX:ReservedCodeCacheSize=128m'
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-modules/gameluck-member -am -Plocal -DskipTests=false "-Dtest=MemberComplianceGateServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" -DforkCount=0 test
```

Expected: the three new tests fail because the gate returns `client.redemption.*` message keys.

- [x] **Step 4: Implement action-specific message selection**

Replace the three hardcoded common denial message arguments with a helper:

```java
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
```

Add:

```java
private String memberMessage(MemberComplianceAction action, String suffix) {
    String prefix = MemberComplianceAction.PURCHASE_PAY.equals(action)
        ? "client.purchase."
        : "client.redemption.";
    return prefix + suffix;
}
```

This deliberately keeps all other actions on their existing messages in Phase 40.

- [x] **Step 5: Run the member test and verify GREEN**

Run the Step 3 Maven command again.

Expected: all `MemberComplianceGateServiceImplTest` tests pass, including the existing redemption message assertions.

### Task 2: Payment Module Dependency And Purchase Gate Tests

**Files:**
- Modify: `backend/gameluck-modules/gameluck-payment/pom.xml`
- Test: `backend/gameluck-modules/gameluck-payment/src/test/java/com/gameluck/payment/client/service/ClientPurchaseServiceTest.java`

- [x] **Step 1: Add the member module dependency**

Add after the wallet dependency:

```xml
<dependency>
    <groupId>com.gameluck</groupId>
    <artifactId>gameluck-member</artifactId>
</dependency>
```

- [x] **Step 2: Extend the test factory with compliance dependencies**

Import `MemberComplianceAction`, `MemberComplianceContext`, `MemberComplianceDecision`, `MemberProfile`, `MemberProfileMapper`, and `IMemberComplianceGateService`.

Change the helper signature and constructor call to:

```java
private ClientPurchaseService service(PurchaseOfferMapper offerMapper,
                                      PurchaseOfferGrantItemMapper itemMapper,
                                      PurchaseOrderMapper orderMapper,
                                      IPurchasePaymentEventService paymentEventService,
                                      MemberProfileMapper memberProfileMapper,
                                      IMemberComplianceGateService complianceGateService) {
    PurchaseOfferServiceImpl purchaseOfferService = new PurchaseOfferServiceImpl(
        offerMapper, itemMapper, mock(PurchaseOrderGrantSnapshotMapper.class));
    return new ClientPurchaseService(
        new ClientTokenService(), offerMapper, itemMapper, orderMapper,
        purchaseOfferService, paymentEventService, memberProfileMapper, complianceGateService);
}
```

Update existing test calls with `mock(MemberProfileMapper.class)` and `allowingGate()` where a new order reaches the offer/limit path. Idempotent-return tests may use a mocked gate and must verify it is never called.

Add helpers:

```java
private IMemberComplianceGateService allowingGate() {
    IMemberComplianceGateService gate = mock(IMemberComplianceGateService.class);
    when(gate.evaluate(any(MemberComplianceContext.class))).thenAnswer(invocation ->
        MemberComplianceDecision.allow(invocation.getArgument(0), "USD", "US", "CA", "h5"));
    return gate;
}

private MemberProfile activeMember() {
    MemberProfile member = new MemberProfile();
    member.setId(1001L);
    member.setTenantId("000000");
    member.setStatus("ACTIVE");
    member.setRiskLevel("NORMAL");
    return member;
}
```

- [x] **Step 3: Write the denied-side-effect test**

Add a parameterized helper or three small tests. The high-risk case must contain these assertions:

```java
MemberProfileMapper memberMapper = mock(MemberProfileMapper.class);
IMemberComplianceGateService gate = mock(IMemberComplianceGateService.class);
PurchaseOrderMapper orderMapper = mock(PurchaseOrderMapper.class);
IPurchasePaymentEventService eventService = mock(IPurchasePaymentEventService.class);
when(orderMapper.selectByIdempotencyKey("000000", "idem-risk")).thenReturn(null);
when(offerMapper.selectById(100L)).thenReturn(offer(100L));
when(memberMapper.selectById(1001L)).thenReturn(activeMember());
when(gate.evaluate(any(MemberComplianceContext.class))).thenReturn(
    MemberComplianceDecision.builder()
        .allowed(false)
        .action(MemberComplianceAction.PURCHASE_PAY.name())
        .reasonCode("RISK_BLOCKED")
        .messageKey("client.purchase.risk.blocked")
        .build());

ServiceException ex = assertThrows(ServiceException.class,
    () -> service.pay("Bearer " + new ClientTokenService().issue(1001L), payBo(100L, "idem-risk")));

assertEquals("client.purchase.risk.blocked", ex.getMessage());
verify(orderMapper, never()).insert(any(PurchaseOrder.class));
verify(eventService, never()).applyEvent(any(PurchasePaymentCallbackBo.class));
verify(itemMapper, never()).selectList(any(Wrapper.class));
```

Add `purchaseDeniedBeforeSideEffects(...)` and call it from three tests for `client.purchase.member.not.exists`, `client.purchase.member.inactive`, and `client.purchase.risk.blocked`:

```java
private void purchaseDeniedBeforeSideEffects(MemberComplianceReason reason, String messageKey, String idempotencyKey) {
    PurchaseOfferMapper offerMapper = mock(PurchaseOfferMapper.class);
    PurchaseOfferGrantItemMapper itemMapper = mock(PurchaseOfferGrantItemMapper.class);
    PurchaseOrderMapper orderMapper = mock(PurchaseOrderMapper.class);
    IPurchasePaymentEventService eventService = mock(IPurchasePaymentEventService.class);
    MemberProfileMapper memberMapper = mock(MemberProfileMapper.class);
    IMemberComplianceGateService gate = mock(IMemberComplianceGateService.class);
    when(orderMapper.selectByIdempotencyKey("000000", idempotencyKey)).thenReturn(null);
    when(offerMapper.selectById(100L)).thenReturn(offer(100L));
    when(memberMapper.selectById(1001L)).thenReturn(activeMember());
    when(gate.evaluate(any(MemberComplianceContext.class))).thenReturn(MemberComplianceDecision.builder()
        .allowed(false)
        .action(MemberComplianceAction.PURCHASE_PAY.name())
        .reasonCode(reason.name())
        .messageKey(messageKey)
        .build());
    ClientPurchaseService service = service(
        offerMapper, itemMapper, orderMapper, eventService, memberMapper, gate);

    ServiceException ex = assertThrows(ServiceException.class,
        () -> service.pay("Bearer " + new ClientTokenService().issue(1001L), payBo(100L, idempotencyKey)));

    assertEquals(messageKey, ex.getMessage());
    verify(orderMapper, never()).insert(any(PurchaseOrder.class));
    verify(eventService, never()).applyEvent(any(PurchasePaymentCallbackBo.class));
    verify(itemMapper, never()).selectList(any(Wrapper.class));
}
```

- [x] **Step 4: Capture and assert the allowed context**

In `simulatedPayCreatesOrderSnapshotsAndCreditsWallet`, return `activeMember()` from `memberMapper`, capture the gate context, and assert:

```java
ArgumentCaptor<MemberComplianceContext> contextCaptor = ArgumentCaptor.forClass(MemberComplianceContext.class);
verify(gate).evaluate(contextCaptor.capture());
MemberComplianceContext context = contextCaptor.getValue();
assertEquals(MemberComplianceAction.PURCHASE_PAY, context.getAction());
assertEquals("000000", context.getTenantId());
assertEquals(1001L, context.getMember().getId());
assertEquals("USD", context.getCurrencyCode());
assertEquals("h5", context.getChannel());
```

- [x] **Step 5: Assert idempotent retries bypass member and gate lookup**

In `repeatedIdempotencyKeyReturnsExistingOrderWithoutCreditingAgain`, retain explicit `memberMapper` and `gate` mocks and assert:

```java
verify(memberMapper, never()).selectById(any(Long.class));
verify(gate, never()).evaluate(any(MemberComplianceContext.class));
```

- [x] **Step 6: Run the payment test and verify RED**

Run:

```powershell
$env:MAVEN_OPTS='-Xmx768m -XX:MaxMetaspaceSize=256m -XX:ReservedCodeCacheSize=128m'
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-modules/gameluck-payment -am -Plocal -DskipTests=false "-Dtest=ClientPurchaseServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" -DforkCount=0 test
```

Expected: compilation fails because `ClientPurchaseService` does not yet accept or call the member mapper and compliance gate.

### Task 3: Purchase Service Integration

**Files:**
- Modify: `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/client/service/ClientPurchaseService.java`
- Test: `backend/gameluck-modules/gameluck-payment/src/test/java/com/gameluck/payment/client/service/ClientPurchaseServiceTest.java`

- [x] **Step 1: Add member and compliance imports and fields**

Add imports for:

```java
import com.gameluck.member.compliance.MemberComplianceAction;
import com.gameluck.member.compliance.MemberComplianceContext;
import com.gameluck.member.compliance.MemberComplianceDecision;
import com.gameluck.member.domain.MemberProfile;
import com.gameluck.member.mapper.MemberProfileMapper;
import com.gameluck.member.service.IMemberComplianceGateService;
```

Add constructor-injected fields after the existing payment event service:

```java
private final MemberProfileMapper memberProfileMapper;
private final IMemberComplianceGateService complianceGateService;
```

- [x] **Step 2: Evaluate compliance before purchase limits and mutations**

In `pay(...)`, retain the current idempotency block unchanged. Immediately after `requireAvailableOffer(...)`, add:

```java
validatePurchaseCompliance(tenantId, memberId, offer);
```

Add:

```java
private void validatePurchaseCompliance(String tenantId, Long memberId, PurchaseOffer offer) {
    MemberProfile member = memberProfileMapper.selectById(memberId);
    MemberComplianceDecision decision = complianceGateService.evaluate(MemberComplianceContext.builder()
        .tenantId(tenantId)
        .member(member)
        .action(MemberComplianceAction.PURCHASE_PAY)
        .currencyCode(offer.getPayCurrencyCode())
        .channel("h5")
        .build());
    if (!decision.isAllowed()) {
        throw new ServiceException(MessageUtils.message(decision.getMessageKey()));
    }
}
```

- [x] **Step 3: Run the payment test and verify GREEN**

Run the Task 2 Step 6 command again.

Expected: all `ClientPurchaseServiceTest` tests pass.

- [x] **Step 4: Run the cross-module focused regression**

Run:

```powershell
$env:MAVEN_OPTS='-Xmx768m -XX:MaxMetaspaceSize=256m -XX:ReservedCodeCacheSize=128m'
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-modules/gameluck-member,gameluck-modules/gameluck-payment -am -Plocal -DskipTests=false "-Dtest=MemberComplianceGateServiceImplTest,ClientPurchaseServiceTest,PurchasePaymentEventServiceImplTest,PurchaseOrderServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" -DforkCount=0 test
```

Expected: all selected member/payment tests pass with zero failures and zero errors.

### Task 4: Localized Purchase Messages

**Files:**
- Modify: `backend/gameluck-admin/src/main/resources/i18n/messages.properties`
- Modify: `backend/gameluck-admin/src/main/resources/i18n/messages_zh_CN.properties`
- Modify: `backend/gameluck-admin/src/main/resources/i18n/messages_en_US.properties`

- [x] **Step 1: Add default and Chinese messages**

Add the same escaped values to `messages.properties` and `messages_zh_CN.properties`:

```properties
client.purchase.member.not.exists=\u4f1a\u5458\u4e0d\u5b58\u5728
client.purchase.member.inactive=\u8d26\u53f7\u72b6\u6001\u6682\u4e0d\u53ef\u8d2d\u4e70
client.purchase.risk.blocked=\u8d26\u53f7\u98ce\u9669\u7b49\u7ea7\u6682\u4e0d\u53ef\u8d2d\u4e70
```

- [x] **Step 2: Add English messages**

Add to `messages_en_US.properties`:

```properties
client.purchase.member.not.exists=Member does not exist.
client.purchase.member.inactive=The account status does not allow purchases.
client.purchase.risk.blocked=The account risk level does not allow purchases.
```

- [x] **Step 3: Verify bundle consistency**

Run:

```powershell
pnpm --dir admin-ui check:i18n
```

Expected: i18n guard passes with no missing or duplicate backend keys.

### Task 5: Package And Runtime Smoke

**Files:**
- Modify: `progress.md`
- Modify: `task_plan.md`
- Modify: `docs/superpowers/plans/2026-07-20-purchase-compliance-gate.md`

- [x] **Step 1: Package the backend**

Stop only the local Java process whose command line contains `gameluck-admin\\target\\gameluck-admin.jar` if it locks the jar. Then run:

```powershell
$env:MAVEN_OPTS='-Xmx768m -XX:MaxMetaspaceSize=256m -XX:ReservedCodeCacheSize=128m'
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-admin -am package -Plocal -DskipTests
```

Expected: reactor ends with `BUILD SUCCESS` and produces `backend/gameluck-admin/target/gameluck-admin.jar`.

- [x] **Step 2: Start the refreshed backend**

Start the jar with the local profile and verify:

```powershell
(Invoke-WebRequest -UseBasicParsing http://127.0.0.1:8080/ -TimeoutSec 5).StatusCode
```

Expected: `200`.

- [x] **Step 3: Create the allowed purchase baseline**

Register or reuse a dedicated local C-side member. Set `status='ACTIVE'` and `risk_level='NORMAL'`. Submit `POST /api/client/purchase/orders/pay` with a fresh idempotency key and an available offer.

Expected:

- API business code `200`;
- returned status `CREDITED`;
- one matching purchase order;
- one processed simulated payment event.

- [x] **Step 4: Verify a new high-risk purchase is side-effect free**

Set the same member's `risk_level='HIGH'`. Record before-counts for:

```text
gl_purchase_order
gl_purchase_payment_event
gl_purchase_order_grant_snapshot
gl_wallet_transaction
gl_wallet_turnover_task
```

Submit the same offer with a new idempotency key.

Expected:

- API returns the localized `client.purchase.risk.blocked` message;
- all five after-counts equal their before-counts for the denied request/member scope.

- [x] **Step 5: Verify idempotent retry bypasses current risk**

Repeat the successful request from Step 3 with its original idempotency key while the member remains `HIGH`.

Expected:

- API returns the same purchase order number and `CREDITED` status;
- order and payment event counts do not increase.

- [x] **Step 6: Run the final focused regression again**

Run the Task 3 Step 4 Maven command after runtime work.

Expected: zero failures and zero errors.

- [x] **Step 7: Run the whitespace check**

Run:

```powershell
git diff --check
```

Expected: exit code `0`; CRLF replacement warnings are acceptable.

- [x] **Step 8: Record completion**

Update `progress.md` with exact test counts, package result, runtime member/order/idempotency keys, side-effect counts, health status, and whitespace result. Mark every executable checkbox in this plan complete. Change Phase 40 in `task_plan.md` from `in_progress` to `complete` only after all prior steps pass.

## Self Review

- Spec coverage: tasks cover action-specific messages, purchase gate integration, idempotent bypass, excluded KYC/age/agreement/region checks, denied side effects, localized H5 errors, focused tests, package, runtime smoke, and whitespace verification.
- Scope control: no purchase region policy, KYC rule, real provider, or H5 redesign is introduced.
- Type consistency: the plan uses existing `MemberComplianceContext`, `MemberComplianceDecision`, `MemberProfileMapper`, `IMemberComplianceGateService`, and `ClientPurchaseService` names and signatures.
- Completeness scan: no deferred or unspecified implementation steps remain.
