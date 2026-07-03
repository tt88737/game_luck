# Redemption Order v1 Design

## Goal

Build the first simulated redemption workflow for the admin backend:

- Create a redemption request for a member and currency.
- Freeze wallet balance when the request is submitted.
- Approve the request and settle the frozen amount.
- Reject the request and release the frozen amount.
- Query and operate redemption requests in the B-side admin UI.

This version does not connect to a real payout provider.

## Boundaries

In scope:

- `redemption-center` backend module under `backend/gameluck-modules`.
- `gl_redemption_order` table and menu seed SQL.
- Wallet core methods: `freeze`, `unfreeze`, `settle`.
- Admin APIs under `/redemption/order`.
- Admin page under `admin-ui/src/views/redemption/order`.

Out of scope:

- Real payout channel integration.
- Batch approval.
- Risk/KYC review workflow.
- Player H5 redemption entry.
- Exchange rate conversion between currencies.

## State Machine

| Status | Meaning | Next Actions |
| --- | --- | --- |
| PENDING | Request submitted, wallet amount frozen | approve, reject |
| APPROVED | Admin approved, frozen amount settled | mark paid later |
| REJECTED | Admin rejected, frozen amount released | none |
| FAILED | Wallet operation failed | manual investigation |

## Wallet Flow

Submit request:

1. Create `gl_redemption_order` with status `PENDING`.
2. Call `IWalletCoreService.freeze`.
3. Store `freezeNo`, `freezeWalletTransactionNo`, and `freezeIdempotencyKey`.

Approve:

1. Lock redemption order by id.
2. Require status `PENDING`.
3. Call `IWalletCoreService.settle` with the original `freezeNo`.
4. Set status `APPROVED`, record approve user/time/reason.

Reject:

1. Lock redemption order by id.
2. Require status `PENDING`.
3. Call `IWalletCoreService.unfreeze` with the original `freezeNo`.
4. Set status `REJECTED`, record reject user/time/reason.

All wallet calls use deterministic idempotency keys based on `redemptionOrderNo`.

## Data Model

`gl_redemption_order` stores:

- order identity: `id`, `tenant_id`, `redemption_order_no`
- member and amount: `member_id`, `currency_code`, `amount`
- method info: `redemption_method`, `account_ref`
- status and failure: `status`, `fail_reason`
- wallet links: `freeze_no`, `freeze_wallet_transaction_no`, `settle_wallet_transaction_no`, `release_wallet_transaction_no`
- idempotency keys: `freeze_idempotency_key`, `settle_idempotency_key`, `release_idempotency_key`
- audit fields: approve/reject user, time, reason, create/update fields, version, del flag

## Admin UI

The page is a B-side operational table:

- Filters: order no, member id, currency, status, create time.
- Table: order no, member id, currency, amount, status tag, freeze no, wallet transaction links, create time.
- Actions: detail, approve, reject.
- Approve/reject require confirmation and reason.
- No delete and no direct balance operations.

## Verification

- Backend compile succeeds.
- Focused unit tests cover status validation and wallet call boundaries.
- SQL import uses `backend/script/bin/import-sql-utf8.ps1`.
- Frontend menu icon check passes.
- Frontend production build succeeds.
- Smoke test covers list/create/approve/reject endpoints with simulated local data.
