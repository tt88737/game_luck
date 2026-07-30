# H5 Wallet Exchange Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a C-side H5 wallet exchange experience that uses the wallet exchange runtime API completed in Phase 21.

**Architecture:** Keep the exchange workflow on the existing `/wallet` page. The page loads balances, ledger rows, and exchange options; the user selects a rule, enters source amount, sees estimated fee/target amount, submits `POST /api/client/wallet/exchange/orders`, then the page refreshes wallet balances and ledger.

**Tech Stack:** Vue 3, Vite, TypeScript, existing H5 CSS utility classes, backend `/api/client/wallet` APIs.

---

### Task 1: Type/API Contract

**Files:**
- Modify: `h5/src/types/client.ts`
- Modify: `h5/src/api/client.ts`
- Test: `npm --prefix h5 run build`

- [x] **Step 1: Write compile-failing usage first**

Modify `h5/src/views/WalletView.vue` to call:

```ts
clientApi.walletExchangeOptions()
clientApi.submitWalletExchange(ruleId, amount, idempotencyKey)
```

Expected: `npm --prefix h5 run build` fails because those methods and response types do not exist.

- [x] **Step 2: Implement types and API methods**

Add `ClientExchangeOption` and `ClientExchangeOrder` in `h5/src/types/client.ts`, then add API methods in `h5/src/api/client.ts`.

- [x] **Step 3: Verify compile GREEN**

Run `npm --prefix h5 run build`. Expected: TypeScript and Vite build pass.

### Task 2: Wallet Page Exchange Workflow

**Files:**
- Modify: `h5/src/views/WalletView.vue`
- Modify: `h5/src/style.css` if shared layout helpers are needed
- Test: `npm --prefix h5 run build`

- [x] **Step 1: Add wallet exchange state**

Add exchange option loading, selected rule, amount form, estimate, submitting, success, error, and insufficient balance state.

- [x] **Step 2: Add wallet exchange UI**

Add one exchange panel between balances and ledger. Use existing `redeem-panel`, `inline-form`, `reward-pills`, `error-text`, and `success-text` patterns. Do not add nested cards.

- [x] **Step 3: Refresh after success**

After successful submit, refresh wallet accounts and ledger, and display the last exchange order status.

- [x] **Step 4: Verify build**

Run `npm --prefix h5 run build`. Expected: build passes.

### Task 3: Runtime UI Smoke

**Files:**
- No source changes expected unless smoke exposes a bug.

- [x] **Step 1: Start or reuse backend**

Use backend on `http://localhost:8080`.

- [x] **Step 2: Start or reuse H5 dev server**

Run or reuse `npm --prefix h5 run dev -- --host 127.0.0.1 --port 5174`.

- [x] **Step 3: Verify UI**

Open `/wallet` in a browser-capable tool when available. Verify desktop and mobile layout, no overlapping text, exchange success/failure states, and wallet refresh.

Verified with Python Playwright against `http://127.0.0.1:5174/wallet`:
- Registered H5 smoke member `h5_exchange_ui_20260717164743`.
- Injected `gameluck.client.token`, loaded wallet balances and exchange options.
- Submitted `10.00 GC -> SC`; page displayed exchange success, refreshed balances, and showed the exchange ledger row.
- Captured screenshots:
  - `h5-wallet-exchange-screens/wallet-desktop-logged-in-success.png`
  - `h5-wallet-exchange-screens/wallet-mobile-viewport-success.png`
- Mobile verification confirmed the exchange input height is `42px` and the core exchange controls/estimates are visible above the tabbar.
