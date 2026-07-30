# Wallet Exchange Runtime Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the C-side wallet exchange execution path from an enabled exchange rule into a persisted exchange order plus wallet debit/credit transactions.

**Architecture:** Keep exchange execution inside the wallet module. The client endpoint authenticates the member, validates an enabled rule, calculates source amount, fee, and target amount, persists `gl_wallet_exchange_order`, debits the source wallet through `IWalletCoreService`, credits the target wallet through `IWalletCoreService`, then marks the order `SUCCESS` or `FAILED`.

**Tech Stack:** Java 17, Spring Boot, MyBatis Plus, JUnit 5, Mockito, MySQL.

---

### Task 1: Service Contract and Happy Path

**Files:**
- Create: `backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/client/domain/bo/ClientExchangeOrderBo.java`
- Create: `backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/client/domain/vo/ClientExchangeOrderVo.java`
- Create: `backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/service/impl/WalletExchangeOrderServiceImpl.java`
- Create: `backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/service/IWalletExchangeOrderService.java`
- Modify: `backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/client/service/ClientWalletService.java`
- Modify: `backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/client/controller/ClientWalletController.java`
- Test: `backend/gameluck-modules/gameluck-wallet/src/test/java/com/gameluck/wallet/service/impl/WalletExchangeOrderServiceImplTest.java`

- [x] **Step 1: Write failing test**

Add a test proving that an enabled fixed-rate rule debits `fromAmount + feeAmount`, credits calculated target amount, and inserts/updates an exchange order.

- [x] **Step 2: Verify RED**

Run:

```powershell
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-modules/gameluck-wallet -am -Plocal -DskipTests=false "-Dtest=WalletExchangeOrderServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected: compilation failure because `WalletExchangeOrderServiceImpl` does not exist.

- [x] **Step 3: Implement minimal service**

Implement only `FIXED` rate, `NONE`/`FIXED`/`PERCENT` fee calculation, order creation, wallet debit, wallet credit, and success/failure order status.

- [x] **Step 4: Verify GREEN**

Run the same focused Maven command. Expected: PASS.

### Task 2: Validation and Failure Paths

**Files:**
- Modify: `WalletExchangeOrderServiceImplTest.java`
- Modify: `WalletExchangeOrderServiceImpl.java`
- Modify: backend i18n property files under `backend/gameluck-admin/src/main/resources/i18n/`

- [x] **Step 1: Write failing tests**

Cover disabled/missing rule, min/max amount, insufficient source balance, and unsupported rate type.

- [x] **Step 2: Verify RED**

Run the focused wallet exchange test and confirm the new tests fail for missing behavior.

- [x] **Step 3: Implement validation**

Reject invalid rule/amount/rate before wallet mutation. When wallet debit returns `FAILED`, mark the exchange order `FAILED` and do not credit the target wallet.

- [x] **Step 4: Verify GREEN**

Run the focused wallet exchange test and confirm all pass.

### Task 3: API and Regression

**Files:**
- Modify: `ClientWalletController.java`
- Modify: `ClientWalletService.java`
- Modify: `ClientWalletServiceTest.java`

- [x] **Step 1: Add client service/controller path**

Expose `POST /api/client/wallet/exchange/orders`, taking `exchangeRuleId`, `fromAmount`, and optional `idempotencyKey`.

- [x] **Step 2: Run focused wallet tests**

Run wallet client and exchange tests.

- [x] **Step 3: Run cross-module regression**

Run wallet/payment/promotion/game focused regression, backend package, and runtime smoke when feasible.
