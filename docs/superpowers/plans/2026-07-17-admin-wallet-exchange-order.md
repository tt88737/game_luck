# Admin Wallet Exchange Order Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a B-side read-only wallet exchange order page so operators can query C-side wallet exchange orders and audit linked debit/credit transactions.

**Architecture:** Follow existing wallet admin list patterns. Backend adds `WalletExchangeOrderBo`, service, and controller under `/wallet/exchange-order`; the service supports filters by order number, member ID/member number, currencies, status, transaction numbers, and time range. Admin UI adds API wrappers and a table page under `wallet/exchange-order/index`, with transaction links into the wallet transaction page.

**Tech Stack:** Spring Boot, MyBatis Plus, GameLuck `TableDataInfo`, Vue 3, Element Plus, existing admin-ui request and i18n helper utilities.

---

### Task 1: Backend Query Contract

**Files:**
- Create: `backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/domain/bo/WalletExchangeOrderBo.java`
- Create: `backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/service/IWalletExchangeOrderAdminService.java`
- Create: `backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/service/impl/WalletExchangeOrderAdminServiceImpl.java`
- Create: `backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/controller/WalletExchangeOrderController.java`
- Test: `backend/gameluck-modules/gameluck-wallet/src/test/java/com/gameluck/wallet/service/impl/WalletExchangeOrderAdminServiceImplTest.java`

- [x] **Step 1: Write failing service test**

Add a focused test that constructs `WalletExchangeOrderAdminServiceImpl` with mocked `WalletExchangeOrderMapper` and `JdbcTemplate`, calls `queryPageList`, and captures the generated wrapper. The test must prove member number, order number, currency, status, transaction number, and date filters are accepted.

- [x] **Step 2: Verify RED**

Run:

```powershell
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-modules/gameluck-wallet -am -Plocal -DskipTests=false "-Dtest=WalletExchangeOrderAdminServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" -DforkCount=0 test
```

Expected: compilation failure because the admin service and BO do not exist.

- [x] **Step 3: Implement minimal backend query**

Create the BO, service interface, service implementation, and controller. Reuse `MemberNoQueryHelper` like `WalletTransactionServiceImpl`.

- [x] **Step 4: Verify GREEN**

Run the same focused Maven command. Expected: Maven `BUILD SUCCESS`.

### Task 2: Admin UI Exchange Order Page

**Files:**
- Create: `admin-ui/src/api/wallet/exchangeOrder/types.ts`
- Create: `admin-ui/src/api/wallet/exchangeOrder/index.ts`
- Create: `admin-ui/src/views/wallet/exchange-order/index.vue`
- Modify if needed: `admin-ui/src/utils/i18nText.ts`
- Modify if needed: `admin-ui/src/lang/zh_CN.ts`
- Modify if needed: `admin-ui/src/lang/en_US.ts`

- [x] **Step 1: Add API wrappers and types**

Expose `listExchangeOrder` and `getExchangeOrder` using `/wallet/exchange-order/list` and `/wallet/exchange-order/{id}`.

- [x] **Step 2: Add read-only page**

Build a B-side table with filters: exchange order number, member ID, from/to currency, status, debit transaction, credit transaction, and create time range. Table columns must show member ID, exchange direction, from amount, fee, to amount, status, linked transaction numbers, fail reason, and create time.

- [x] **Step 3: Link transaction numbers**

Debit and credit transaction numbers link to `/wallet/transaction?transactionNo=...` so operators can inspect wallet ledger effects.

- [x] **Step 4: Verify admin build**

Run:

```powershell
$env:NODE_OPTIONS='--max-old-space-size=4096'; pnpm --dir admin-ui build:dev
```

Expected: Vite build passes with only existing chunk-size warnings.

### Task 3: Menu/Data Wiring And Runtime Smoke

**Files:**
- Modify: `backend/script/sql/gameluck_wallet.sql`
- Test: backend compile/package and optional runtime endpoint call.

- [x] **Step 1: Add menu SQL**

Add wallet exchange order menu and permissions under the wallet menu area:
- `wallet:exchangeOrder:list`
- `wallet:exchangeOrder:query`

- [x] **Step 2: Verify backend compile**

Run:

```powershell
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-admin -am compile -Plocal -DskipTests
```

Expected: Maven `BUILD SUCCESS`.

- [ ] **Step 3: Runtime endpoint smoke**

Using the running backend, call `GET /wallet/exchange-order/list?pageNum=1&pageSize=10` with an admin token if available. Expected: response `code=200` or table payload with rows, and recent H5 exchange smoke orders are visible.

Status: not run in this pass because no reusable admin token was available locally. Backend `GET /` returned 200 and the endpoint is covered by focused service tests plus admin compile.

### Task 4: Final Verification

- [x] **Step 1: Run focused wallet tests**

Run the wallet focused test command including `WalletExchangeOrderAdminServiceImplTest` and existing wallet exchange tests.

- [x] **Step 2: Run admin-ui build**

Run `pnpm --dir admin-ui build:dev`.

- [x] **Step 3: Run whitespace check**

Run `git diff --check`. Expected: no whitespace errors; CRLF warnings are acceptable in this workspace.

- [x] **Step 4: Update planning files**

Mark Phase 23 complete in `task_plan.md` only after backend tests, admin build, and whitespace check pass.
