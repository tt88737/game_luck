# Redemption Eligibility Policy Design

## Goal

Move C-side redemption region eligibility from hardcoded Java checks into an operator-managed policy table, while keeping the existing member-status, risk, age, and agreement gates intact.

## Scope

This phase adds a minimal redemption eligibility policy surface:

- Backend policy table and CRUD API under the redemption module.
- Runtime policy check before redemption order creation.
- Admin UI page under Redemption Center for operators to list, add, edit, and inspect policies.
- Local SQL seed for denied US states currently hardcoded in `ClientRedemptionService`.

This phase does not add a full KYC provider workflow, document upload, third-party screening, or multi-step approval workflow. Those remain later compliance phases.

## Policy Model

Table: `gl_redemption_eligibility_policy`

Fields:

- `tenant_id`: tenant scope, default `000000`.
- `policy_name`: operator-facing name.
- `currency_code`: redemption currency, first use is `SC`.
- `country_code`: optional country condition. Blank means any country.
- `state_code`: optional state/province condition. Blank means any state.
- `channel`: optional channel condition. Blank means any channel.
- `effect`: `ALLOW` or `DENY`.
- `priority`: higher priority evaluated first.
- `status`: `0` enabled, `1` disabled.
- `start_time`, `end_time`: optional active window.
- `remark`, audit fields, version, delete flag.

Matching rules:

- Runtime loads enabled, non-deleted policies for tenant and currency.
- A policy matches when country, state, and channel are blank or equal to the member/request context, case-insensitive.
- Policies outside the active time window are ignored.
- Highest priority wins. For equal priority, `DENY` wins over `ALLOW`.
- If no policy matches, default is allow. This preserves current behavior outside the existing denied states.

Seed policy:

- Four enabled `DENY` policies for `SC`, channel `h5`, country `US`, states `WA`, `ID`, `NV`, and `MI`.

## Backend Flow

`ClientRedemptionService.request(...)` keeps the existing gate order:

1. Resolve member from token.
2. Validate member exists.
3. Validate member status is `ACTIVE`.
4. Validate risk is not `HIGH`.
5. Validate age and agreements.
6. Validate currency is supported.
7. Evaluate `RedemptionEligibilityPolicyService`.
8. Create redemption order only if eligible.

The policy service returns an eligibility decision instead of throwing directly. `ClientRedemptionService` maps a denied result to the existing i18n key `client.redemption.region.blocked` for now, so H5 behavior remains unchanged.

## Admin UI

Add page: `admin-ui/src/views/redemption/eligibility-policy/index.vue`

Navigation:

- Parent: `兑换中心`
- Menu: `兑换资格策略`
- Route component: `redemption/eligibility-policy/index`
- Permissions:
  - `redemption:eligibilityPolicy:list`
  - `redemption:eligibilityPolicy:query`
  - `redemption:eligibilityPolicy:add`
  - `redemption:eligibilityPolicy:edit`

Page behavior:

- Filters: policy name, currency, country, state, channel, effect, status.
- Table: policy name, currency, country/state, channel, effect tag, priority, status, active window, remark.
- Dialog form: policy name, currency, country, state, channel, effect, priority, status, active window, remark.
- Empty, loading, filtered empty, disabled status, and edit states use the existing Element Plus admin patterns.

## Verification

Backend:

- TDD unit tests verify:
  - Deny policy blocks before order creation.
  - Allow policy with higher priority permits a previously denied region.
  - Disabled or expired deny policies are ignored.
  - Existing non-policy gates still block before policy evaluation effects matter.
- Focused Maven tests:
  - `ClientRedemptionServiceTest`
  - new `RedemptionEligibilityPolicyServiceImplTest`
  - existing `RedemptionOrderServiceImplTest`

Frontend:

- Admin UI build passes.
- Playwright runtime smoke logs in, opens `兑换资格策略`, verifies seeded `US/WA` deny policy, creates or edits a test policy, and confirms list rendering.

Runtime:

- Import local SQL.
- Restart backend from rebuilt jar.
- H5 `US/WA` redemption remains blocked with `当前地区暂不支持兑换`.
- A runtime policy override smoke may create a higher priority `ALLOW` policy for a test state and verify redemption proceeds to pending order when other member gates pass.

## Self Review

- No placeholders remain.
- Scope is limited to redemption eligibility policy, not full KYC.
- Default allow preserves current regions except seeded denied states.
- Admin and backend permission names are consistent.
