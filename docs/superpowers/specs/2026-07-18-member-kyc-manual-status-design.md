# Member KYC Manual Status Design

## Goal

Add a lightweight KYC status foundation that operators can maintain manually and that C-side redemption uses as a real eligibility gate. This replaces the current placeholder behavior where C-side member responses always expose `kycStatus = NOT_STARTED` and redemption checks do not require an approved KYC state.

## Scope

This phase includes:

- Add persistent KYC fields to `gl_member_profile`.
- Return the persisted KYC status from C-side member APIs.
- Let Admin operators view, filter, and update KYC status from the existing Member Profile page.
- Record KYC updates through the existing Admin operation log mechanism.
- Require `kyc_status = APPROVED` before creating a C-side redemption order.
- Add focused backend and Admin UI tests plus local runtime smoke.

This phase does not include:

- Third-party KYC provider integration.
- KYC document upload.
- Provider callback handling.
- Multi-step KYC review workflow.
- Separate KYC case assignment queues.
- Automatic risk scoring.

## Data Model

Use the existing `gl_member_profile` table because this phase has exactly one current KYC state per member and no document/review history requirement.

New columns:

| Column | Type | Default | Purpose |
| --- | --- | --- | --- |
| `kyc_status` | `VARCHAR(32)` | `NOT_STARTED` | Current KYC state. |
| `kyc_review_reason` | `VARCHAR(512)` | `NULL` | Operator-facing reason or note for the latest status update. |
| `kyc_reviewed_by` | `VARCHAR(64)` | `NULL` | Operator username for the latest manual update. |
| `kyc_review_time` | `DATETIME` | `NULL` | Time of the latest manual update. |

Allowed `kyc_status` values reuse the existing `gl_kyc_status` dictionary:

- `NOT_STARTED`
- `PENDING`
- `APPROVED`
- `REJECTED`
- `EXPIRED`

Default behavior:

- New H5-registered members start with `NOT_STARTED`.
- Existing members get `NOT_STARTED` through SQL default/backfill.
- Demo or smoke users may be manually updated to `APPROVED` for redemption tests.

## Backend Behavior

### Member Domain

Add the new fields to:

- `MemberProfile`
- `MemberProfileBo`
- `MemberProfileVo`
- `ClientMemberVo`

`ClientAuthService.toClientMember(...)` returns `member.kycStatus`, defaulting to `NOT_STARTED` only if the database value is blank.

### Admin Member Profile

Keep using existing endpoints:

- `GET /member/profile/list`
- `GET /member/profile/{id}`
- `POST /member/profile`
- `PUT /member/profile`

The existing `@Log(title = "Member profile edit", businessType = BusinessType.UPDATE)` remains the audit source for KYC updates. No new controller is required in this phase.

Validation rules:

- Blank KYC status is normalized to `NOT_STARTED`.
- Non-blank KYC status must be one of the allowed enum values.
- If status is `REJECTED` or `EXPIRED`, `kyc_review_reason` should be accepted and shown, but this phase does not force a reason to avoid blocking legacy import/edit flows.
- On Admin edit where `kyc_status` changes, set `kyc_reviewed_by` and `kyc_review_time`.
- If the update payload keeps the same KYC status, do not overwrite review metadata unless the review reason changes.

### Redemption Gate

`ClientRedemptionService.validateRedemptionGate(...)` adds a KYC check after account/risk/consent checks and before region policy evaluation:

- If `kyc_status != APPROVED`, throw `client.redemption.kyc.required`.
- The redemption order and wallet freeze must not be created.

This keeps the gate conservative: a member must be active, not high risk, have compliance confirmations, pass KYC, and pass redemption eligibility policy before order creation.

## Admin UI Behavior

Update the existing `admin-ui/src/views/member/profile/index.vue`.

List page:

- Add a KYC Status filter using `gl_kyc_status`.
- Add a KYC Status column beside Risk Level.
- Show status with `DictTag` or equivalent tag mapping.

Edit dialog:

- Add KYC Status select.
- Add KYC Review Reason textarea.
- Keep the existing compact member edit dialog rather than creating a new page.

Detail dialog:

- Show KYC Status.
- Show KYC Review Reason.
- Show KYC Reviewed By.
- Show KYC Review Time.

Type updates:

- Add the fields to `MemberProfileVO`, `MemberProfileForm`, and `MemberProfileQuery`.

## H5 Behavior

No new H5 page is required in this phase.

Existing C-side member responses should return the real `kycStatus`. H5 pages that already display profile/session data can consume the updated field without new navigation.

Redemption submit failure should continue using the backend error message. If KYC is not approved, the page displays the returned message, matching the existing denied-region behavior.

## SQL

Add idempotent DDL to `backend/script/sql/gameluck_wallet.sql`:

- Add the four `gl_member_profile` KYC columns if missing.
- Backfill blank/null `kyc_status` to `NOT_STARTED`.

Ensure the existing `gl_kyc_status` dictionary values include:

- `NOT_STARTED`
- `PENDING`
- `APPROVED`
- `REJECTED`
- `EXPIRED`

If `EXPIRED` is missing from current SQL, add it idempotently.

## I18n

Backend message keys:

- `client.redemption.kyc.required`
- `member.kyc.status.invalid`

Admin UI:

- Add member profile labels/placeholders for KYC fields.
- Use existing dictionary display for `gl_kyc_status`.

## Testing

Backend tests:

- `MemberProfileServiceImplTest`
  - Inserts default `kycStatus = NOT_STARTED`.
  - Rejects invalid KYC status.
  - Updates KYC metadata when status changes.
- `ClientAuthServiceTest`
  - Returns persisted `kycStatus` in login/current member response.
  - Defaults blank status to `NOT_STARTED`.
- `ClientRedemptionServiceTest`
  - Rejects member with `NOT_STARTED`.
  - Rejects member with `PENDING`.
  - Allows KYC-approved member to continue to existing policy/wallet path.

Admin UI checks:

- `pnpm --dir admin-ui check:i18n`
- `pnpm --dir admin-ui check:menu-icons`

Build/runtime checks:

- Focused Maven tests for member and redemption.
- Backend package if implementation touches compiled modules broadly.
- Runtime smoke:
  - Register H5 user.
  - Verify initial KYC status is `NOT_STARTED`.
  - Attempt redemption and receive KYC block.
  - Update member KYC to `APPROVED` from Admin or API.
  - Attempt redemption again in an allowed region and reach the existing redemption path.

## Risks

| Risk | Handling |
| --- | --- |
| KYC is treated as legally complete when it is only manual status | UI and docs call it a manual status foundation, not provider verification. |
| Existing users lack KYC status | SQL default/backfill sets `NOT_STARTED`. |
| Admin edit unintentionally clears review metadata | Service only overwrites metadata on meaningful KYC status/reason changes. |
| Gate blocks all current smoke redemption users | Runtime smoke should explicitly approve the chosen test user before testing non-KYC paths. |

## Acceptance Criteria

- C-side member API returns persisted `kycStatus`.
- New members default to `NOT_STARTED`.
- Admin member page can filter, view, and edit KYC status and reason.
- Admin operation log records the edit action.
- Redemption rejects non-approved KYC before order creation.
- Existing denied-region policy still works after KYC-approved users reach region validation.
- Focused backend tests pass.
- Admin UI i18n/menu checks pass.
- `git diff --check` has no whitespace errors.
