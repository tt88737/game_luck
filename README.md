# Tang Luck P0-A

Tang Luck P0-A is a runnable Spring Boot + Vue 3 + MySQL implementation for registration, compliance documents, dual wallet ledger, promotional claims, coupon claims, admin campaign operations, audit logs, and demo frontend pages.

## Run locally

1. `docker compose up -d mysql`
2. `cd backend && .\gradlew.bat bootRun`
3. `cd frontend && npm install && npm run dev`

## Verify

- Backend: `cd backend && .\gradlew.bat test`
- Frontend unit: `cd frontend && npm run test -- --run --pool=threads --maxWorkers=1`
- Frontend build: `cd frontend && npm run build`
- E2E smoke: `cd frontend && npx playwright test`

## P0-A boundaries

- No real payment integration.
- No real redemption payout.
- SC is not sold.
- KYC, store launch, and app store submission are out of P0-A runtime scope.
- AMOE and No Purchase Necessary links must remain visible.

## Demo notes

- The backend currently uses `X-User-Id` as a P0-A local user context header for wallet and claim endpoints.
- The frontend stores `tangluck_user_id` and `tangluck_token` in `localStorage` for local demo calls.
- Playwright E2E mocks backend API responses to verify page rendering without depending on live payment, redemption, or external services.
