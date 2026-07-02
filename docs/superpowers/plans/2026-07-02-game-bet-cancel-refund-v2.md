# Game Bet Cancel Refund v2 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** Add admin-side cancellation and refund for simulated game bet orders in `BET_SUCCESS` state.

**Architecture:** Extend the existing `gameluck-game` module and `gl_game_bet_order` table with refund metadata. Cancellation is owned by the game module, but all balance changes go through `IWalletCoreService.credit` with `sourceType = GAME_REFUND`; the game module must not update wallet balances directly.

**Tech Stack:** Java 17, Spring Boot, MyBatis-Plus, MySQL, GameLuck Admin conventions, Vue 3, Element Plus, TypeScript.

---

## File Structure

Backend:

- Modify `backend/script/sql/gameluck_wallet.sql`
- Modify `backend/gameluck-modules/gameluck-game/src/main/java/com/gameluck/game/domain/GameBetOrder.java`
- Modify `backend/gameluck-modules/gameluck-game/src/main/java/com/gameluck/game/domain/vo/GameBetOrderVo.java`
- Modify `backend/gameluck-modules/gameluck-game/src/main/java/com/gameluck/game/enums/GameBetOrderStatus.java`
- Modify `backend/gameluck-modules/gameluck-game/src/main/java/com/gameluck/game/service/IGameBetOrderService.java`
- Modify `backend/gameluck-modules/gameluck-game/src/main/java/com/gameluck/game/service/impl/GameBetOrderServiceImpl.java`
- Modify `backend/gameluck-modules/gameluck-game/src/main/java/com/gameluck/game/controller/GameBetOrderController.java`

Frontend:

- Modify `admin-ui/src/api/game/bet/types.ts`
- Modify `admin-ui/src/api/game/bet/index.ts`
- Modify `admin-ui/src/views/game/bet/index.vue`

Verification:

- Run SQL import with `backend/script/bin/import-sql-utf8.ps1`
- Run menu icon guard
- Run backend compile
- Run frontend build
- Run API and SQL closure checks

## Task 1: SQL Schema, Wallet Rule, And Menu

- [x] **Step 1: Add refund columns and index to `gl_game_bet_order`**

Modify `backend/script/sql/gameluck_wallet.sql` table definition for `gl_game_bet_order` by adding the three columns after `settle_wallet_transaction_no`:

```sql
  refund_wallet_transaction_no varchar(64) DEFAULT NULL COMMENT '閫€娆鹃挶鍖呬氦鏄撳彿',
  refund_idempotency_key varchar(128) DEFAULT NULL COMMENT '閫€娆惧箓绛夐敭',
  cancel_time datetime DEFAULT NULL COMMENT '鍙栨秷鏃堕棿',
```

Add a fourth unique key after `uk_gl_game_bet_order_03`:

```sql
  UNIQUE KEY uk_gl_game_bet_order_04 (tenant_id, refund_idempotency_key),
```

- [x] **Step 2: Add idempotent ALTER statements for existing local databases**

Append this block after the `gl_game_bet_order` table definition in `backend/script/sql/gameluck_wallet.sql`:

```sql
SET @db_name := DATABASE();
SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'gl_game_bet_order' AND COLUMN_NAME = 'refund_wallet_transaction_no'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE gl_game_bet_order ADD COLUMN refund_wallet_transaction_no varchar(64) DEFAULT NULL COMMENT ''閫€娆鹃挶鍖呬氦鏄撳彿'' AFTER settle_wallet_transaction_no',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'gl_game_bet_order' AND COLUMN_NAME = 'refund_idempotency_key'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE gl_game_bet_order ADD COLUMN refund_idempotency_key varchar(128) DEFAULT NULL COMMENT ''閫€娆惧箓绛夐敭'' AFTER refund_wallet_transaction_no',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'gl_game_bet_order' AND COLUMN_NAME = 'cancel_time'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE gl_game_bet_order ADD COLUMN cancel_time datetime DEFAULT NULL COMMENT ''鍙栨秷鏃堕棿'' AFTER refund_idempotency_key',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists := (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'gl_game_bet_order' AND INDEX_NAME = 'uk_gl_game_bet_order_04'
);
SET @sql := IF(@idx_exists = 0,
  'ALTER TABLE gl_game_bet_order ADD UNIQUE KEY uk_gl_game_bet_order_04 (tenant_id, refund_idempotency_key)',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
```

- [x] **Step 3: Add wallet rule seed for `SC + GAME_REFUND`**

In the existing `gl_wallet_rule` seed block in `backend/script/sql/gameluck_wallet.sql`, add:

```sql
(1900000000000000105, '000000', 'SC', 'GAME_REFUND', 'SC game refund', '0', '0', '1', '0', 'IMMEDIATE', '0', 0, '0', 5, 'SC refund returns original stake immediately.', NOW())
```

Use `ON DUPLICATE KEY UPDATE` style consistent with the existing rule seed block if the block already has duplicate-key handling.

- [x] **Step 4: Add menu permission seed**

Append menu seed for the cancel button in `backend/script/sql/gameluck_wallet.sql`:

```sql
INSERT INTO sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES
(1935, '妯℃嫙涓嬫敞鍙栨秷閫€娆?, 1921, 5, '#', '', '', 1, 0, 'F', '0', '0', 'game:bet:cancel', '#', 1, NOW(), '妯℃嫙涓嬫敞鍙栨秷閫€娆?)
ON DUPLICATE KEY UPDATE
  menu_name = VALUES(menu_name),
  parent_id = VALUES(parent_id),
  order_num = VALUES(order_num),
  perms = VALUES(perms),
  icon = VALUES(icon),
  status = VALUES(status);
```

- [x] **Step 5: Run menu icon guard**

Run:

```powershell
pnpm --dir admin-ui check:menu-icons
```

Expected: exit `0`, output includes `Menu icon check passed`.

- [x] **Step 6: Import SQL using UTF-8 script**

Run:

```powershell
.\backend\script\bin\import-sql-utf8.ps1 -SqlPath backend\script\sql\gameluck_wallet.sql
```

Expected: exit `0`. Do not import SQL with `Get-Content | mysql`.

- [x] **Step 7: Verify schema and menu**

Run:

```powershell
mysql --default-character-set=utf8mb4 -uroot -proot gameluck_vue -e "show columns from gl_game_bet_order like 'refund%'; show columns from gl_game_bet_order like 'cancel_time'; show index from gl_game_bet_order where Key_name='uk_gl_game_bet_order_04'; select currency_code, source_type, release_mode, status from gl_wallet_rule where currency_code='SC' and source_type='GAME_REFUND'; select menu_id, menu_name, perms, icon from sys_menu where menu_id=1935;"
```

Expected:

- `refund_wallet_transaction_no` exists
- `refund_idempotency_key` exists
- `cancel_time` exists
- unique index `uk_gl_game_bet_order_04` exists
- wallet rule `SC / GAME_REFUND / IMMEDIATE / 0` exists
- menu `1935 / 妯℃嫙涓嬫敞鍙栨秷閫€娆?/ game:bet:cancel / #` exists

## Task 2: Backend Domain And Contract

- [x] **Step 1: Extend `GameBetOrder` entity**

Modify `backend/gameluck-modules/gameluck-game/src/main/java/com/gameluck/game/domain/GameBetOrder.java` by adding fields after `settleWalletTransactionNo`:

```java
    private String refundWalletTransactionNo;
    private String refundIdempotencyKey;
    private Date cancelTime;
```

- [x] **Step 2: Extend `GameBetOrderVo`**

Modify `backend/gameluck-modules/gameluck-game/src/main/java/com/gameluck/game/domain/vo/GameBetOrderVo.java` by adding fields after `settleWalletTransactionNo`:

```java
    private String refundWalletTransactionNo;
    private String refundIdempotencyKey;
    private Date cancelTime;
```

- [x] **Step 3: Add `CANCELLED` status**

Modify `backend/gameluck-modules/gameluck-game/src/main/java/com/gameluck/game/enums/GameBetOrderStatus.java` to:

```java
package com.gameluck.game.enums;

public enum GameBetOrderStatus {
    PENDING,
    BET_SUCCESS,
    BET_FAILED,
    SETTLED,
    SETTLE_FAILED,
    CANCELLED
}
```

- [x] **Step 4: Add service contract**

Modify `backend/gameluck-modules/gameluck-game/src/main/java/com/gameluck/game/service/IGameBetOrderService.java` by adding after `settle(Long id);`:

```java
    GameBetOrderVo cancel(Long id);
```

- [x] **Step 5: Compile backend module**

Run:

```powershell
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-modules/gameluck-game -am compile -Plocal -DskipTests
```

Expected: fails before Task 3 because `GameBetOrderServiceImpl` does not implement `cancel(Long id)`. This is the expected red check proving the contract change is active.

## Task 3: Backend Cancel Refund Logic And API

- [x] **Step 1: Generate refund idempotency key on create**

Modify `insertByBo` in `backend/gameluck-modules/gameluck-game/src/main/java/com/gameluck/game/service/impl/GameBetOrderServiceImpl.java`.

After:

```java
        add.setSettleIdempotencyKey(settleIdempotencyKey(orderNo));
```

Add:

```java
        add.setRefundIdempotencyKey(refundIdempotencyKey(orderNo));
```

- [x] **Step 2: Prevent settlement after cancellation**

Keep the current `settle(Long id)` status guard as `requireStatus(order, GameBetOrderStatus.BET_SUCCESS);`. This already rejects `CANCELLED`. No extra branch is needed.

- [x] **Step 3: Add `cancel(Long id)` implementation**

Add this method to `GameBetOrderServiceImpl` after `settle(Long id)`:

```java
    @Override
    @Transactional(rollbackFor = Exception.class)
    public GameBetOrderVo cancel(Long id) {
        GameBetOrder order = lockOrder(id);
        if (GameBetOrderStatus.CANCELLED.name().equals(order.getStatus())) {
            return BeanUtil.toBean(order, GameBetOrderVo.class);
        }
        requireStatus(order, GameBetOrderStatus.BET_SUCCESS);

        Date now = new Date();
        WalletTransaction transaction = walletCoreService.credit(buildRefundCreditBo(order));
        order.setRefundWalletTransactionNo(transaction.getTransactionNo());
        order.setCancelTime(now);
        order.setUpdateTime(now);
        if (WalletTransactionStatus.SUCCESS.name().equals(transaction.getStatus())) {
            order.setStatus(GameBetOrderStatus.CANCELLED.name());
            order.setFailReason(null);
        } else {
            order.setFailReason(StringUtils.substring(transaction.getFailReason(), 0, 500));
        }
        baseMapper.updateById(order);
        return BeanUtil.toBean(order, GameBetOrderVo.class);
    }
```

- [x] **Step 4: Add refund wallet request builder**

Add this helper to `GameBetOrderServiceImpl` after `buildCreditBo(GameBetOrder order)`:

```java
    private WalletCreditBo buildRefundCreditBo(GameBetOrder order) {
        WalletCreditBo creditBo = new WalletCreditBo();
        creditBo.setMemberId(order.getMemberId());
        creditBo.setCurrencyCode(order.getCurrencyCode());
        creditBo.setAmount(order.getBetAmount());
        creditBo.setSourceType("GAME_REFUND");
        creditBo.setBusinessNo(order.getBetOrderNo());
        creditBo.setIdempotencyKey(order.getRefundIdempotencyKey());
        creditBo.setRemark("Simulated game bet refund");
        return creditBo;
    }
```

- [x] **Step 5: Add refund idempotency helper**

Add this helper to `GameBetOrderServiceImpl` after `settleIdempotencyKey(String orderNo)`:

```java
    private String refundIdempotencyKey(String orderNo) {
        return "game:refund:" + orderNo;
    }
```

- [x] **Step 6: Add controller endpoint**

Modify `backend/gameluck-modules/gameluck-game/src/main/java/com/gameluck/game/controller/GameBetOrderController.java`.

Add this method after `settle(@PathVariable Long id)`:

```java
    @SaCheckPermission("game:bet:cancel")
    @Log(title = "Cancel simulated game bet", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/cancel")
    public R<GameBetOrderVo> cancel(@PathVariable Long id) {
        return R.ok(gameBetOrderService.cancel(id));
    }
```

- [x] **Step 7: Compile backend module**

Run:

```powershell
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-modules/gameluck-game -am compile -Plocal -DskipTests
```

Expected: `BUILD SUCCESS`.

## Task 4: Frontend API And Page

- [x] **Step 1: Extend TypeScript VO**

Modify `admin-ui/src/api/game/bet/types.ts` by adding fields after `settleWalletTransactionNo`:

```ts
  refundWalletTransactionNo: string;
  refundIdempotencyKey: string;
  cancelTime: string;
```

- [x] **Step 2: Add cancel API wrapper**

Modify `admin-ui/src/api/game/bet/index.ts` by adding:

```ts
export function cancelGameBet(id: string | number): AxiosPromise<GameBetOrderVO> {
  return request({
    url: '/game/bet/' + id + '/cancel',
    method: 'post'
  });
}
```

- [x] **Step 3: Import cancel API in page**

Modify the import in `admin-ui/src/views/game/bet/index.vue` from:

```ts
import { addGameBet, getGameBet, listGameBet, placeGameBet, settleGameBet } from '@/api/game/bet';
```

To:

```ts
import { addGameBet, cancelGameBet, getGameBet, listGameBet, placeGameBet, settleGameBet } from '@/api/game/bet';
```

- [x] **Step 4: Add `CANCELLED` status option**

In the status select in `admin-ui/src/views/game/bet/index.vue`, add:

```vue
<el-option label="宸插彇娑? value="CANCELLED" />
```

- [x] **Step 5: Add refund transaction column**

In the table, add after settlement transaction number:

```vue
<el-table-column label="閫€娆句氦鏄撳彿" align="center" prop="refundWalletTransactionNo" min-width="180" show-overflow-tooltip />
```

- [x] **Step 6: Add cancel button**

In the action column, add after the settle button:

```vue
<el-tooltip v-if="scope.row.status === 'BET_SUCCESS'" content="鍙栨秷閫€娆? placement="top">
  <el-button v-hasPermi="['game:bet:cancel']" link type="warning" icon="RefreshLeft" @click="handleCancelBet(scope.row)"></el-button>
</el-tooltip>
```

- [x] **Step 7: Add detail fields**

In the detail dialog descriptions, add after settlement transaction number:

```vue
<el-descriptions-item label="閫€娆句氦鏄撳彿">{{ detail.refundWalletTransactionNo }}</el-descriptions-item>
<el-descriptions-item label="閫€娆惧箓绛夐敭">{{ detail.refundIdempotencyKey }}</el-descriptions-item>
<el-descriptions-item label="鍙栨秷鏃堕棿">{{ detail.cancelTime }}</el-descriptions-item>
```

- [x] **Step 8: Update status label and tag type**

Modify `statusLabel` map in `index.vue` to include:

```ts
    CANCELLED: '宸插彇娑?
```

Modify `statusType` map in `index.vue` to include:

```ts
    CANCELLED: 'info'
```

- [x] **Step 9: Add cancel handler**

Add this function after `handleSettle` in `admin-ui/src/views/game/bet/index.vue`:

```ts
const handleCancelBet = async (row: GameBetOrderVO) => {
  await proxy?.$modal.confirm('纭鍙栨秷璇ユā鎷熶笅娉ㄨ鍗曞苟閫€鍥炰笅娉ㄩ噾棰濓紵');
  await cancelGameBet(row.id);
  proxy?.$modal.msgSuccess('鍙栨秷閫€娆惧畬鎴?);
  await getList();
};
```

- [x] **Step 10: Run frontend type/build verification**

Run:

```powershell
pnpm --dir admin-ui build:prod
```

Expected: `check:menu-icons` runs first and passes; Vite build exits `0`. Existing chunk-size warnings are acceptable.

## Task 5: Full Verification And Closure

- [x] **Step 1: Run full backend compile**

Run:

```powershell
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-admin -am compile -Plocal -DskipTests
```

Expected: `BUILD SUCCESS`.

- [x] **Step 2: Package backend**

Run:

```powershell
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-admin -am package -Plocal -DskipTests
```

Expected: `BUILD SUCCESS` and `backend/gameluck-admin/target/gameluck-admin.jar` updated.

- [x] **Step 3: Restart backend jar**

Stop existing process on port `8080` if present:

```powershell
$conn = Get-NetTCPConnection -State Listen -LocalPort 8080 -ErrorAction SilentlyContinue
if ($conn) { Stop-Process -Id $conn.OwningProcess -Force }
```

Start new jar:

```powershell
Start-Process -FilePath "java" -ArgumentList @("-jar", "gameluck-admin\target\gameluck-admin.jar", "--spring.profiles.active=local") -WorkingDirectory "C:\codex\project\backend" -WindowStyle Hidden
Start-Sleep -Seconds 25
Get-NetTCPConnection -State Listen -LocalPort 8080 -ErrorAction SilentlyContinue | Select-Object LocalAddress,LocalPort,OwningProcess
```

Expected: port `8080` is listening.

- [x] **Step 4: Verify route exists**

Run unauthenticated route check:

```powershell
Invoke-WebRequest -UseBasicParsing http://localhost:8080/game/bet/list -TimeoutSec 10
```

Expected: HTTP `200` with business `401`, or an authenticated `200` if a token is supplied. HTTP `404` and HTTP `500` are failures.

- [x] **Step 5: Run API closure test**

Use an authenticated admin token from the local UI session. If no token is available, log in through `admin-ui` first and copy the `Authorization` bearer token from the browser network panel.

Prepare `SC` balance through wallet core:

```powershell
$headers=@{Authorization="Bearer <TOKEN>"; clientid='e5cd7e4891bf95d1d19206ce24a7b32e'}
$body=@{idempotencyKey=('test:sc:promo:' + (Get-Date -Format yyyyMMddHHmmss)); memberId=1001; currencyCode='SC'; sourceType='PROMOTION'; businessNo=('TESTPROMO' + (Get-Date -Format yyyyMMddHHmmss)); amount=50; remark='prepare SC balance for game refund test'} | ConvertTo-Json
Invoke-WebRequest -UseBasicParsing http://localhost:8080/wallet/core/credit -Method Post -Headers $headers -ContentType 'application/json' -Body $body -TimeoutSec 10
```

Create order, place bet, then cancel:

```powershell
$body=@{memberId=1001; currencyCode='SC'; gameCode='SIMULATED'; betAmount=10; payoutAmount=0; remark='cancel refund closure test'} | ConvertTo-Json
Invoke-WebRequest -UseBasicParsing http://localhost:8080/game/bet -Method Post -Headers $headers -ContentType 'application/json' -Body $body -TimeoutSec 10
$list=Invoke-WebRequest -UseBasicParsing "http://localhost:8080/game/bet/list?pageNum=1&pageSize=1&memberId=1001&currencyCode=SC" -Headers $headers -TimeoutSec 10
$id=($list.Content | ConvertFrom-Json).rows[0].id
Invoke-WebRequest -UseBasicParsing "http://localhost:8080/game/bet/$id/place" -Method Post -Headers $headers -TimeoutSec 10
Invoke-WebRequest -UseBasicParsing "http://localhost:8080/game/bet/$id/cancel" -Method Post -Headers $headers -TimeoutSec 10
Invoke-WebRequest -UseBasicParsing "http://localhost:8080/game/bet/$id/cancel" -Method Post -Headers $headers -TimeoutSec 10
```

Expected:

- first cancel returns `status = CANCELLED`
- second cancel returns the same order and does not create another `GAME_REFUND` transaction

- [x] **Step 6: Run SQL closure check**

Run:

```powershell
mysql --default-character-set=utf8mb4 -uroot -proot gameluck_vue -e "select bet_order_no, member_id, currency_code, bet_amount, payout_amount, status, bet_wallet_transaction_no, settle_wallet_transaction_no, refund_wallet_transaction_no from gl_game_bet_order order by create_time desc limit 5; select business_no, operation, source_type, amount, status from gl_wallet_transaction where source_type in ('GAME_BET','GAME_PROFIT','GAME_REFUND') order by create_time desc limit 10; select member_id, currency_code, available_balance from gl_wallet_account where currency_code='SC' order by update_time desc limit 5;"
```

Expected:

- latest test order status is `CANCELLED`
- latest test order has `refund_wallet_transaction_no`
- wallet has `DEBIT / GAME_BET / SUCCESS / 10.000000`
- wallet has exactly one `CREDIT / GAME_REFUND / SUCCESS / 10.000000` for that `business_no`
- member `1001 / SC` balance returns to the pre-bet amount after cancellation

## Self-Review

- Spec coverage: SQL columns, unique key, wallet rule, menu permission, backend status, service method, controller route, frontend button/detail fields, and closure verification are covered.
- Scope limit: no `SETTLED` reversal, no `SETTLE_FAILED` retry, no `PENDING` close, no real provider callback, no batch refund.
- Type consistency: Java fields use `refundWalletTransactionNo`, `refundIdempotencyKey`, `cancelTime`; SQL uses matching snake_case columns; TypeScript uses the same camelCase properties.
- Guardrails: SQL import uses `import-sql-utf8.ps1`; menu icons are guarded by `check:menu-icons`; all wallet balance changes go through `IWalletCoreService.credit`.

## Execution Notes

- 2026-07-02: SQL import, menu icon guard, frontend production build, backend compile/package, backend restart, unauthenticated route check, authenticated API closure, and SQL closure check completed.
- API closure order: `GB2072670483144630272`; first cancel and repeated cancel both returned `CANCELLED` with the same refund transaction `WT2072670484574887937`.
- SQL closure confirmed one `DEBIT / GAME_BET / SUCCESS / 10.000000`, one `CREDIT / GAME_REFUND / SUCCESS / 10.000000`, and member `1001 / SC` balance remained `108.000000` after refund.
