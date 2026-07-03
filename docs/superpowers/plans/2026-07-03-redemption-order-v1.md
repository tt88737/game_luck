# Redemption Order v1 Implementation Plan

> Required workflow: implement task by task, keep changes scoped, and verify after each backend/frontend phase.

## Goal

Add a simulated redemption workflow that freezes wallet balance on request, settles frozen balance on approval, and releases frozen balance on rejection.

## Task 1: Wallet Freeze API

- [x] Add `WalletFreezeBo` operation fields or new operation BOs as needed.
- [x] Add `freeze`, `unfreeze`, and `settle` methods to `IWalletCoreService`.
- [x] Implement wallet account row locking and transaction records for freeze/unfreeze/settle.
- [x] Keep idempotency based on `tenant_id + idempotency_key`.
- [x] Add focused tests for invalid state and idempotent wallet boundaries.
- [x] Run wallet/game focused tests and backend compile.

## Task 2: Redemption Backend

- [x] Add `gameluck-redemption` module and wire it into Maven and `gameluck-admin`.
- [x] Add `gl_redemption_order` SQL table and menu entries.
- [x] Add entity, BO, VO, mapper, XML, service, and controller.
- [x] Implement create, approve, reject, list, detail APIs.
- [x] Use wallet `freeze`, `settle`, `unfreeze`; do not update wallet account directly.
- [x] Add focused service tests for approve/reject status validation and wallet calls.
- [x] Run backend compile and targeted tests.

## Task 3: Admin UI

- [x] Add `admin-ui/src/api/redemption/order` API wrappers and types.
- [x] Add `admin-ui/src/views/redemption/order/index.vue`.
- [x] Use table, filters, status tags, detail dialog, approve/reject confirmation with reason.
- [x] Use existing SVG icon `money` for redemption menu and `#` for button menus.
- [x] Run `pnpm --dir admin-ui check:menu-icons`.
- [x] Run `pnpm --dir admin-ui build:prod`.

## Task 4: Local Verification

- [x] Import SQL with `backend/script/bin/import-sql-utf8.ps1`.
- [x] Create or reuse a member wallet balance.
- [x] Create redemption order and confirm wallet freeze record exists.
- [x] Approve one order and confirm frozen amount is settled.
- [x] Reject one order and confirm frozen amount is released.
- [x] Query admin list endpoint.

## Task 5: Handoff

- [x] Scan changed files for mojibake patterns.
- [x] Update `progress.md`.
- [x] Commit docs, backend, frontend, and verification notes in focused commits.
