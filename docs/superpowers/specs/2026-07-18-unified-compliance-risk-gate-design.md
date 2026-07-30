# Unified Compliance/Risk Gate Design

## Goal

Create a shared backend compliance/risk gate that gives C-side business flows one consistent way to evaluate member status, risk level, KYC state, region policy, channel, and currency before creating orders, granting SC, launching games, or processing AMOE-related actions.

This phase is a consolidation layer over existing rules. It is not a configurable rule engine and does not add a new Admin configuration page.

## Scope

This phase includes:

- Add a backend service that returns a structured allow/deny decision for named C-side actions.
- Support these first action codes:
  - `REDEMPTION_REQUEST`
  - `PURCHASE_PAY`
  - `SC_GRANT`
  - `GAME_LAUNCH`
  - `AMOE_REQUEST`
- Reuse existing member profile fields:
  - `status`
  - `risk_level`
  - `kyc_status`
  - `country_code`
  - `state_code`
  - age and agreement confirmations
- Reuse the existing redemption eligibility policy service for region/channel/currency matching where the first action needs it.
- Refactor C-side redemption to use the shared gate while preserving existing error messages and order creation behavior.
- Add focused backend tests that prove the shared gate returns consistent decisions and redemption still blocks before order creation.

This phase does not include:

- New Admin rule configuration screens.
- A general-purpose rule DSL.
- Third-party KYC, fraud, sanctions, or geolocation providers.
- New compliance history tables.
- Replacing every business gate in one pass.
- Real AMOE workflow implementation.

## Decision Model

New action enum:

| Action | First Behavior |
| --- | --- |
| `REDEMPTION_REQUEST` | Requires active member, not high risk, age and agreements confirmed, KYC approved, region policy allowed for `SC` and `h5`. |
| `PURCHASE_PAY` | Requires active member and not high risk. KYC is not required in this phase. Region policy is not applied in this phase. |
| `SC_GRANT` | Requires active member, not high risk, and age/agreements confirmed. KYC is not required for promotional grant in this phase. |
| `GAME_LAUNCH` | Requires active member and not high risk. If currency is `SC`, also requires age/agreements confirmed. |
| `AMOE_REQUEST` | Requires active member, not high risk, age/agreements confirmed, and region policy allowed for `SC` and `h5`. KYC is not required in this phase unless the action later creates redemption. |

The defaults are intentionally conservative for redemption and AMOE, and lighter for purchase/game so the existing demo flow is not over-blocked.

## Result Contract

The gate returns a decision object rather than throwing directly.

Fields:

| Field | Type | Purpose |
| --- | --- | --- |
| `allowed` | `boolean` | Whether the action may continue. |
| `action` | enum/string | The evaluated action code. |
| `messageKey` | `String` | Existing backend i18n key used by caller if denied. |
| `reasonCode` | `String` | Stable machine-readable reason. |
| `memberId` | `Long` | Member id evaluated, when available. |
| `currencyCode` | `String` | Currency context after normalization. |
| `countryCode` | `String` | Uppercase country code after normalization. |
| `stateCode` | `String` | Uppercase state/province code after normalization. |
| `channel` | `String` | Lowercase channel code after normalization. |

Reason codes:

- `MEMBER_NOT_EXISTS`
- `MEMBER_INACTIVE`
- `RISK_BLOCKED`
- `AGE_REQUIRED`
- `AGREEMENTS_REQUIRED`
- `KYC_REQUIRED`
- `REGION_BLOCKED`
- `ALLOWED`

Message key mapping:

| Reason | Message Key |
| --- | --- |
| `MEMBER_NOT_EXISTS` | `client.redemption.member.not.exists` for redemption; generic member missing key may be added later when other callers need custom wording. |
| `MEMBER_INACTIVE` | `client.redemption.member.inactive` |
| `RISK_BLOCKED` | `client.redemption.risk.blocked` |
| `AGE_REQUIRED` | `client.redemption.age.required` |
| `AGREEMENTS_REQUIRED` | `client.redemption.agreements.required` |
| `KYC_REQUIRED` | `client.redemption.kyc.required` |
| `REGION_BLOCKED` | `client.redemption.region.blocked` |

For this phase, callers may use the redemption message keys even when the gate is reused by purchase/game tests. If a caller exposes a different product surface later, it can map the same `reasonCode` to a caller-specific message key.

## Backend Architecture

Create a new module package under `gameluck-member` because member compliance depends primarily on `MemberProfile` and is consumed by multiple business modules:

- `com.gameluck.member.compliance.MemberComplianceAction`
- `com.gameluck.member.compliance.MemberComplianceDecision`
- `com.gameluck.member.compliance.MemberComplianceContext`
- `com.gameluck.member.service.IMemberComplianceGateService`
- `com.gameluck.member.service.impl.MemberComplianceGateServiceImpl`

The service accepts either a loaded `MemberProfile` or a member id context. First implementation should use loaded `MemberProfile` for low blast radius:

```java
MemberComplianceDecision evaluate(MemberComplianceContext context);
```

`MemberComplianceContext` includes:

- `MemberProfile member`
- `MemberComplianceAction action`
- `String tenantId`
- `String currencyCode`
- `String channel`

Dependencies:

- `IRedemptionEligibilityPolicyService` is used for action types that need region policy. This introduces a member-module dependency on redemption only if added directly, which is not desirable.

To keep module boundaries clean, define a small bridge interface in the member module:

```java
public interface MemberRegionEligibilityChecker {
    boolean isEligible(String tenantId, String currencyCode, String countryCode, String stateCode, String channel);
}
```

Then implement it in the redemption module using the existing `IRedemptionEligibilityPolicyService`. In tests, mock the bridge interface.

If cross-module Spring scanning or dependency direction makes the bridge awkward, keep the first implementation inside the redemption module as `ClientComplianceGateService` and document moving it to member/common later. The preferred implementation is still the bridge because purchase, promotion, and game should not depend on redemption internals.

## Evaluation Order

For every action:

1. Member exists.
2. Member status is `ACTIVE`.
3. Risk level is not `HIGH`.
4. If the action requires age, `ageConfirmed` must be true.
5. If the action requires agreements, `termsAccepted`, `privacyAccepted`, and `sweepstakesRulesAccepted` must be true.
6. If the action requires KYC, `kycStatus` must be `APPROVED`.
7. If the action requires region policy, call the region eligibility checker.
8. Return `allowed`.

This order preserves current redemption behavior: invalid account state and consent issues are reported before KYC or region blocks, and no order is created when denied.

## First Integration

### Redemption

`ClientRedemptionService.request(...)` should:

1. Resolve member from token.
2. Build `MemberComplianceContext` with:
   - action `REDEMPTION_REQUEST`
   - currency `SC`
   - channel `h5`
   - loaded member
3. Call the shared gate.
4. Throw `ServiceException(MessageUtils.message(decision.getMessageKey()))` if denied.
5. Keep unsupported currency validation and order creation behavior unchanged.

After this refactor, `ClientRedemptionService` should no longer contain direct checks for status, risk, age, agreements, KYC, or region policy.

### Purchase, SC Grant, Game, AMOE

This phase defines action behavior and adds direct unit tests for the shared gate. Full integration into purchase, promotion, game, and AMOE entrypoints can be done incrementally after redemption proves the shared service shape.

If scope allows after redemption integration:

- Add purchase pay precheck before idempotency-sensitive order creation.
- Add game launch precheck before returning launch response.
- Add promotion claim precheck only for SC grant flows.

If time or dependency risk is high, leave these as follow-up tasks with tests against the shared gate only.

## I18n

No new C-side messages are required if existing redemption message keys are reused.

If purchase/game integration needs caller-specific wording in a later phase, add keys then, for example:

- `client.purchase.member.ineligible`
- `client.game.member.ineligible`
- `client.promotion.sc.grant.blocked`

Do not add unused i18n keys in this phase.

## Testing

### Shared Gate Unit Tests

Create tests for:

- Missing member returns `MEMBER_NOT_EXISTS`.
- Inactive member returns `MEMBER_INACTIVE`.
- High-risk member returns `RISK_BLOCKED`.
- Redemption requires age and agreements.
- Redemption requires `kycStatus=APPROVED`.
- Redemption calls region checker after member/risk/KYC pass.
- Region checker denial returns `REGION_BLOCKED`.
- Purchase pay does not require KYC.
- SC grant requires age and agreements.
- Game launch with `SC` requires age and agreements.
- Game launch with `GC` does not require KYC.

### Redemption Regression Tests

Update `ClientRedemptionServiceTest` so it proves:

- Denied gate decision throws the decision message key.
- Denied gate decision creates no redemption order.
- Approved gate decision continues to existing currency/order path.

### Build Checks

Run:

```powershell
$env:MAVEN_OPTS='-Xmx768m -XX:MaxMetaspaceSize=256m -XX:ReservedCodeCacheSize=128m'
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-modules/gameluck-member,gameluck-modules/gameluck-redemption -am -Plocal -DskipTests=false "-Dtest=MemberComplianceGateServiceImplTest,ClientRedemptionServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" -DforkCount=0 test
```

If purchase/game/promotion integrations are added in this phase, include their focused tests in the command.

## Runtime Smoke

Use the existing local runtime:

- Backend: `http://localhost:8080`
- Admin UI: `http://localhost:5173`
- H5: `http://127.0.0.1:5174`

Smoke path:

1. Register an H5 user in `US/CA`.
2. Confirm redemption is blocked while KYC is `NOT_STARTED`.
3. Approve KYC.
4. Confirm redemption reaches existing order creation path.
5. Set risk to `HIGH`.
6. Confirm redemption is blocked by risk before region policy/order creation.
7. Restore the smoke member if needed.

No new UI screenshot is required unless purchase/game/promotion flows are visibly changed.

## Risks

| Risk | Handling |
| --- | --- |
| Member module depending on redemption policy creates an awkward dependency | Use `MemberRegionEligibilityChecker` bridge interface and implement it in the redemption module. |
| Reusing redemption message keys outside redemption may sound wrong | First phase only integrates redemption; other action tests validate decision behavior without exposing messages. |
| Refactor changes existing redemption block order | Keep explicit evaluation order and regression tests for risk, consent, KYC, and region. |
| Large dirty worktree makes review hard | Keep Phase 37 files narrowly scoped and update `progress.md` after design and plan. |

## Acceptance Criteria

- A shared compliance/risk gate service exists with action-based decisions.
- Unit tests cover status, risk, age, agreements, KYC, region, and action-specific requirements.
- C-side redemption uses the shared gate and no longer owns duplicated member/KYC/region checks.
- Redemption still blocks before order creation for inactive, high-risk, missing consent, non-approved KYC, and denied region.
- Existing focused member/redemption Maven tests pass.
- Backend package still succeeds if compiled modules are touched broadly.
- `git diff --check` has no whitespace errors.

## Self Review

- No placeholder requirements are left.
- Scope is limited to a backend consolidation layer and first redemption integration.
- The design avoids a rule engine and Admin page in this phase.
- Module dependency risk is called out with a bridge-interface mitigation.
