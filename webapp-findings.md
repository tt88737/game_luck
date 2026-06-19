# Webapp Findings

## Fixed During This Pass

### P1: Admin Campaigns auto-created duplicate campaign on page open

- Severity: P1
- Module: Admin Campaigns
- Evidence: backend log showed repeated `Duplicate entry 'OPS_SC_BONUS'` and browser console showed HTTP 500.
- Root cause: `AdminCampaigns.vue` posted `/api/v1/admin/campaigns` inside `onMounted`.
- Fix: removed automatic POST; `Create draft` now requires explicit operator click.
- Verification: `AdminCampaigns.test.ts`, final browser validation has no failed responses.

### P1: User-scoped pages called APIs while logged out

- Severity: P1
- Module: KYC, Redemption, Wallet
- Evidence: direct route browser pass showed 500 responses from `/api/v1/kyc/status`, `/api/v1/wallet/summary`, `/api/v1/wallet/ledger`.
- Root cause: pages called APIs requiring `X-User-Id` before checking local session state.
- Fix: logged-out state now renders a register CTA and skips user-scoped API calls.
- Verification: `P1Pages.test.ts`, final browser validation has no failed responses.

### P2: Product and operations data still displayed sandbox/demo labels

- Severity: P2
- Module: Store, Admin P1
- Evidence: browser validation flagged visible `Sandbox` and `P1 Demo User`.
- Root cause: historical seed/runtime data from previous P1 sandbox implementation.
- Fix: added Flyway V4/V5 migrations and changed KYC form defaults.
- Verification: final browser validation reports `hasDemoWords: false` for all covered routes.

## Open

- Some older pages are only partially localized in zh-CN: home, wallet, admin dashboard, campaign, and audit pages still contain English headings. This is tracked in `TangLuck-V1-实施计划.md`.
