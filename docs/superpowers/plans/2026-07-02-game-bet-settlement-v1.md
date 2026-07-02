# Game Bet Settlement v1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a minimal admin-side simulated game bet flow that debits wallet balance for bets and credits wallet balance for payouts.

**Architecture:** Add a new `gameluck-game` module that owns simulated bet order state and depends on `gameluck-wallet` for accounting. Game services never update wallet balances directly; successful bet placement calls `IWalletCoreService.debit`, and successful settlement calls `IWalletCoreService.credit`.

**Tech Stack:** Java 17, Spring Boot, MyBatis-Plus, MySQL, GameLuck Admin conventions, Vue 3, Element Plus, TypeScript.

---

## File Structure

Backend module:

- Create `backend/gameluck-modules/gameluck-game/pom.xml`
- Modify `backend/gameluck-modules/pom.xml`
- Modify `backend/pom.xml`
- Modify `backend/gameluck-admin/pom.xml`
- Create `backend/gameluck-modules/gameluck-game/src/main/java/com/gameluck/game/domain/GameBetOrder.java`
- Create `backend/gameluck-modules/gameluck-game/src/main/java/com/gameluck/game/domain/bo/GameBetOrderBo.java`
- Create `backend/gameluck-modules/gameluck-game/src/main/java/com/gameluck/game/domain/vo/GameBetOrderVo.java`
- Create `backend/gameluck-modules/gameluck-game/src/main/java/com/gameluck/game/enums/GameBetOrderStatus.java`
- Create `backend/gameluck-modules/gameluck-game/src/main/java/com/gameluck/game/mapper/GameBetOrderMapper.java`
- Create `backend/gameluck-modules/gameluck-game/src/main/resources/mapper/game/GameBetOrderMapper.xml`
- Create `backend/gameluck-modules/gameluck-game/src/main/java/com/gameluck/game/service/IGameBetOrderService.java`
- Create `backend/gameluck-modules/gameluck-game/src/main/java/com/gameluck/game/service/impl/GameBetOrderServiceImpl.java`
- Create `backend/gameluck-modules/gameluck-game/src/main/java/com/gameluck/game/controller/GameBetOrderController.java`
- Modify `backend/script/sql/gameluck_wallet.sql`

Frontend:

- Create `admin-ui/src/api/game/bet/types.ts`
- Create `admin-ui/src/api/game/bet/index.ts`
- Create `admin-ui/src/views/game/bet/index.vue`

## Task 1: SQL Schema And Menu

- [x] **Step 1: Append table `gl_game_bet_order` to `backend/script/sql/gameluck_wallet.sql`**

Use `utf8mb4`, Snowflake `BIGINT` primary key, unique keys for `(tenant_id, bet_order_no)`, `(tenant_id, bet_idempotency_key)`, and `(tenant_id, settle_idempotency_key)`.

- [x] **Step 2: Append menu seed**

Use these menu values:

```text
1920 游戏交易 icon=shopping
1921 模拟下注订单 icon=list component=game/bet/index
1931 查询 game:bet:query
1932 新增 game:bet:add
1933 模拟下注 game:bet:place
1934 模拟结算 game:bet:settle
```

Button menu icon must be `#`.

- [x] **Step 3: Run menu icon guard**

```powershell
pnpm --dir admin-ui check:menu-icons
```

Expected: exit `0`.

- [x] **Step 4: Import SQL safely**

```powershell
.\backend\script\bin\import-sql-utf8.ps1 -SqlPath backend\script\sql\gameluck_wallet.sql
```

Expected: SQL import exits `0`; do not use `Get-Content | mysql`.

## Task 2: Backend Module Skeleton

- [x] **Step 1: Create `gameluck-game` POM**

Dependencies must include common core, mybatis, log, web, tenant, security, and `gameluck-wallet`.

- [x] **Step 2: Register module**

Add `gameluck-game` to `backend/gameluck-modules/pom.xml`, dependency management in `backend/pom.xml`, and admin dependency in `backend/gameluck-admin/pom.xml`.

- [x] **Step 3: Compile skeleton**

```powershell
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-modules/gameluck-game -am compile -Plocal -DskipTests
```

Expected: `BUILD SUCCESS`.

## Task 3: Domain, Mapper, And Contract

- [x] **Step 1: Create status enum**

Values:

```java
PENDING, BET_SUCCESS, BET_FAILED, SETTLED, SETTLE_FAILED
```

- [x] **Step 2: Create entity, BO, VO**

Fields must match the SQL table. BO must validate `memberId`, `currencyCode`, `betAmount`, and `payoutAmount`. Default currency is `SC`.

- [x] **Step 3: Create mapper and XML**

Add `selectByIdForUpdate(Long id)` using:

```sql
SELECT * FROM gl_game_bet_order WHERE id = #{id} AND del_flag = '0' FOR UPDATE
```

- [x] **Step 4: Create service contract**

Required methods:

```java
TableDataInfo<GameBetOrderVo> queryPageList(GameBetOrderBo bo, PageQuery pageQuery);
GameBetOrderVo queryById(Long id);
Boolean insertByBo(GameBetOrderBo bo);
GameBetOrderVo placeBet(Long id);
GameBetOrderVo settle(Long id);
```

## Task 4: Service And Controller

- [x] **Step 1: Implement create order**

Generate:

```text
betOrderNo = GB + snowflake
betIdempotencyKey = game:bet:{betOrderNo}
settleIdempotencyKey = game:settle:{betOrderNo}
status = PENDING
currencyCode default = SC
gameCode default = SIMULATED
roundNo default = ROUND + snowflake
```

- [x] **Step 2: Implement `placeBet`**

Lock order. Only `PENDING` can place. Call wallet debit:

```text
memberId = order.memberId
currencyCode = order.currencyCode
amount = order.betAmount
sourceType = GAME_BET
businessNo = order.betOrderNo
idempotencyKey = order.betIdempotencyKey
```

If wallet transaction status is `SUCCESS`, set order status `BET_SUCCESS`; otherwise set `BET_FAILED`.

- [x] **Step 3: Implement `settle`**

Lock order. Only `BET_SUCCESS` can settle. Call wallet credit:

```text
memberId = order.memberId
currencyCode = order.currencyCode
amount = order.payoutAmount
sourceType = GAME_PROFIT
businessNo = order.betOrderNo
idempotencyKey = order.settleIdempotencyKey
```

If wallet transaction status is `SUCCESS`, set order status `SETTLED`; otherwise set `SETTLE_FAILED`.

- [x] **Step 4: Implement controller**

Routes:

```http
GET /game/bet/list
GET /game/bet/{id}
POST /game/bet
POST /game/bet/{id}/place
POST /game/bet/{id}/settle
```

Add `@SaCheckPermission` and `@Log` for create, place, and settle.

## Task 5: Admin UI

- [x] **Step 1: Create API types and wrapper**

Files:

```text
admin-ui/src/api/game/bet/types.ts
admin-ui/src/api/game/bet/index.ts
```

Functions:

```text
listGameBet
getGameBet
addGameBet
placeGameBet
settleGameBet
```

- [x] **Step 2: Create Vue page**

File:

```text
admin-ui/src/views/game/bet/index.vue
```

The page must use dense RuoYi table style, filters, pagination, status tags, detail dialog, and icon-only action buttons with tooltips.

- [x] **Step 3: Add confirmation dialogs**

`place` and `settle` must show `proxy?.$modal.confirm(...)` before calling API.

## Task 6: Verification

- [x] **Step 1: Backend compile**

```powershell
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-admin -am compile -Plocal -DskipTests
```

Expected: `BUILD SUCCESS`.

- [x] **Step 2: Frontend build**

```powershell
pnpm --dir admin-ui build:prod
```

Expected: icon guard passes first, then Vite build exits `0`.

- [x] **Step 3: Database closure check**

After creating one order, placing the bet, and settling it, run:

```powershell
mysql --default-character-set=utf8mb4 -uroot -proot gameluck_vue -e "select bet_order_no, member_id, currency_code, bet_amount, payout_amount, status, bet_wallet_transaction_no, settle_wallet_transaction_no from gl_game_bet_order order by create_time desc limit 5; select member_id, currency_code, available_balance from gl_wallet_account where currency_code='SC' order by update_time desc limit 5; select business_no, operation, source_type, amount, status from gl_wallet_transaction where source_type in ('GAME_BET','GAME_PROFIT') order by create_time desc limit 10; select business_no, release_mode, release_status from gl_wallet_release where source_type='GAME_PROFIT' order by create_time desc limit 5;"
```

Expected:

- Order status is `SETTLED`.
- Wallet has `DEBIT / GAME_BET / SUCCESS`.
- Wallet has `CREDIT / GAME_PROFIT / SUCCESS`.
- Wallet release follows wallet rule center for `SC + GAME_PROFIT`.

## Self-Review

- Spec coverage: create order, place bet, settle payout, idempotency, admin page, menu, and verification are covered.
- Scope limit: real game provider, callback signing, cancellation, and retry are explicitly out of v1.
- Guardrails: SQL import must use UTF-8 script; menu icons must pass `check:menu-icons`; wallet balance changes must go through `IWalletCoreService`.
