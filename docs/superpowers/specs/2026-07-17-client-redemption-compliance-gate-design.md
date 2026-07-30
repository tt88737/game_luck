# Client Redemption Compliance Gate Design

## Goal

Add a lightweight compliance gate before H5 users can submit SC redemption requests. The gate uses the member profile fields that already exist today and does not pretend to implement full KYC.

## Scope

This phase covers only `POST /api/client/redemptions`.

In scope:
- Require the member profile to exist.
- Require member status `ACTIVE`.
- Block member risk level `HIGH`.
- Require `ageConfirmed`, `termsAccepted`, `privacyAccepted`, and `sweepstakesRulesAccepted`.
- Block a small first-pass denied region list for SC redemption: `US-WA`, `US-ID`, `US-NV`, `US-MI`.
- Keep existing SC-only currency validation.
- Return localized backend messages for each blocked reason.
- Keep existing successful redemption behavior unchanged.

Out of scope:
- Real KYC provider integration.
- New KYC review tables or admin KYC workflow.
- GeoIP or device based region detection.
- Changing B-side redemption approval/reject behavior.
- Changing wallet freeze, settle, or release semantics.

## Data Model

No schema change is required in this phase.

The gate reads existing `gl_member_profile` columns:
- `status`
- `risk_level`
- `country_code`
- `state_code`
- `age_confirmed`
- `terms_accepted`
- `privacy_accepted`
- `sweepstakes_rules_accepted`

The current member token still resolves to `gl_member_profile.id`.

## Service Design

`ClientRedemptionService.request(...)` will:

1. Resolve `memberId` from the client token.
2. Load the member from `MemberProfileMapper.selectClientMember("000000", memberId)`.
3. Validate the member gate before creating any redemption order.
4. Validate `currencyCode == "SC"` as today.
5. Create the same simulated pending redemption order as today.

Validation order:
1. Missing member: `client.redemption.member.not.exists`
2. Non-active status: `client.redemption.member.inactive`
3. High risk: `client.redemption.risk.blocked`
4. Age not confirmed: `client.redemption.age.required`
5. Required agreements missing: `client.redemption.agreements.required`
6. Denied region: `client.redemption.region.blocked`
7. Unsupported currency: `client.redemption.currency.unsupported`

Denied region matching:
- Normalize country and state by trim + uppercase.
- Block when `countryCode == "US"` and `stateCode` is one of `WA`, `ID`, `NV`, `MI`.
- Empty region is not blocked in this phase because earlier smoke/demo registrations may not have complete region data.

## Testing

Add focused unit tests in `ClientRedemptionServiceTest`:
- Existing successful SC request still creates a pending order.
- Missing member is rejected.
- Disabled member is rejected.
- High-risk member is rejected.
- Missing age confirmation is rejected.
- Missing required agreements are rejected.
- Denied state is rejected.
- Unsupported currency remains rejected.

The tests should use real `ClientRedemptionService` and mocked mapper/order-service dependencies, matching existing test style.

## Runtime Verification

After implementation:
- Run focused redemption tests.
- Run backend compile.
- Optionally run H5 runtime smoke with a blocked local member if the backend is already running.

## Risks

This is a conservative gate. It blocks only clear local-profile failures and known denied regions. Full KYC remains a separate phase because the current database has no KYC status column or provider workflow.
