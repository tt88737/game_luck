# Tang Luck V1 Production Track

Tang Luck is a runnable Spring Boot + Vue 3 + MySQL implementation for registration, compliance documents, dual wallet ledger, promotional claims, coupon claims, admin campaign operations, audit logs, and the V1 production operating workflow.

P1 adds a minimal closed loop for:

- GC product packages and purchase orders.
- KYC application submission and admin approval.
- Redemption request creation after KYC approval, with SC frozen for manual review.
- C-side pages for Store, KYC, Redemption, Wallet, and Activity.
- B-side `P1 Ops` page for purchase, KYC, and redemption queues.

## Run locally

1. `docker compose up -d mysql`
2. `cd backend && .\gradlew.bat bootRun --args="--server.port=8092"`
3. `cd frontend && npm install && npm run dev -- --host 127.0.0.1 --port 5175 --strictPort`

Open:

- Backend landing: `http://localhost:8092`
- C-side app: `http://127.0.0.1:5175/app/register`
- Admin console: `http://127.0.0.1:5175/admin/p1`

## Verify

- Backend: `cd backend && .\gradlew.bat test`
- Frontend unit: `cd frontend && npm run test -- --run --pool=threads --maxWorkers=1`
- Frontend build: `cd frontend && npm run build`
- E2E smoke: `cd frontend && npx playwright test`

## V1 provider boundaries

- No real payment integration.
- No real redemption payout.
- SC is not sold.
- KYC is a local review state only; no real vendor is connected.
- Local development can use a `manual` provider until real payment, KYC, and payout providers are configured.
- Redemption requests create a manual review record and freeze SC, but no payout is sent.
- AMOE and No Purchase Necessary links must remain visible.

## Demo notes

- The backend currently uses `X-User-Id` as a P0-A local user context header for wallet and claim endpoints.
- The frontend stores `tangluck_user_id` and `tangluck_token` in `localStorage` for local development calls.
- Playwright E2E mocks backend API responses to verify page rendering without depending on live payment, redemption, or external services.
