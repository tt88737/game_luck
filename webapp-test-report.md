# Webapp Test Report

## Summary

Browser validation completed against the local running app.

- Base URL: `http://127.0.0.1:5175`
- Backend: `http://localhost:8092`
- Devices: desktop 1440x900, mobile Pixel 5
- Languages: English default, Chinese browser locale
- Mutation: allowed in local development

## Covered

- Registration form and legal checks.
- Home page after registration.
- Store package list and purchase action.
- KYC submit and admin approval.
- Redemption request after KYC approval.
- Wallet page after flow.
- Admin dashboard, campaigns, and P1 operations.
- Direct route access while logged out for user-scoped pages.

## Evidence

- Raw run data: `artifacts/browser-validation/browser-validation.json`
- Screenshots: `artifacts/browser-validation/*.png`

## Result

Final browser run recorded:

- 0 console errors.
- 0 failed HTTP responses.
- 0 failed Playwright route loads.
- 0 detected user-visible `demo`, `sandbox`, or `P0-A preview` terms on covered routes.

## Remaining Risk

- This is not full production readiness. Payment, KYC, payout provider abstraction and full formal state machines remain in the V1 implementation plan.
- Some non-P1 pages still need complete zh-CN localization.
