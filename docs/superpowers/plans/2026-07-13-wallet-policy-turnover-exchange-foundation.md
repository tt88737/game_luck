# Wallet Policy Turnover Exchange Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the wallet foundation for extensible currencies, user/group/region currency visibility, fund property templates, turnover snapshots, and future currency exchange without forcing operators to maintain technical wallet rules per activity.

**Architecture:** Keep `gl_wallet_currency`, `gl_wallet_rule`, `gl_wallet_account`, `gl_wallet_transaction`, and `gl_wallet_release` as the current compatible core. Add thin foundation tables and services around them: fund property templates define default money behavior, currency policies calculate C-side visibility/usability, turnover tasks store immutable snapshots, and exchange rules/orders reserve future conversion capability. Business modules such as deposit and promotion will reference these services instead of asking operators to configure wallet rules directly.

**Tech Stack:** Java 17, Spring Boot, MyBatis-Plus, MySQL 8, Vue 3 admin-ui, existing GameLuck i18n/dict/menu patterns.

---

## Current Findings

- `gl_wallet_currency` already stores base currency ability flags: credit, debit, freeze, withdraw, exchange, negative.
- `gl_wallet_rule` currently stores technical `currency_code + source_type` behavior. It should remain as a system default/fallback, not an ordinary operator activity configuration page.
- `gl_wallet_release` already stores `required_turnover`, `completed_turnover`, `release_status`, and `metadata`. It can continue to support the current release flow, but a clearer `gl_wallet_turnover_task` table is needed for activity/deposit/exchange snapshots and game-scope matching.
- `DepositOrderServiceImpl` currently credits the deposit amount as one `DEPOSIT` wallet transaction. It does not split principal and bonus.
- `PromotionRewardServiceImpl` currently credits each `reward_items` item with `source_type = DAILY_REWARD`. It does not persist per-item turnover/game-scope snapshots.
- `gl_member_profile` already has `country_code` and `state_code`, which are enough for the first currency visibility policy.

## File Structure

### SQL and dictionaries

- Modify `backend/script/sql/gameluck_wallet.sql`
  - Add guarded columns to `gl_wallet_currency`: `deposit_enabled`, `play_enabled`, `exchange_in_enabled`, `exchange_out_enabled`.
  - Add `gl_wallet_fund_property_template`.
  - Add `gl_wallet_currency_policy`.
  - Add `gl_wallet_turnover_task`.
  - Add `gl_wallet_exchange_rule`.
  - Add `gl_wallet_exchange_order`.
  - Seed GC/SC/RC base currency defaults, fund property defaults, policy defaults, and exchange-disabled defaults.
- Modify `backend/script/sql/gameluck_platform_dict.sql`
  - Add dict values for fund properties, policy actions, turnover status, game scope type, exchange rule status, exchange order status, and rate type.

### Backend wallet domain

- Create `backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/domain/WalletFundPropertyTemplate.java`.
- Create `backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/domain/WalletCurrencyPolicy.java`.
- Create `backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/domain/WalletTurnoverTask.java`.
- Create `backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/domain/WalletExchangeRule.java`.
- Create `backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/domain/WalletExchangeOrder.java`.
- Create matching `bo`, `vo`, `mapper`, `service`, `service/impl`, and controller classes following existing wallet module naming.

### Backend wallet service API

- Modify `WalletCreditBo`
  - Add `fundPropertyCode`.
  - Add `turnoverMultiplier`.
  - Add `turnoverRequiredAmount`.
  - Add `gameScopeType`.
  - Add `gameScopeValue`.
  - Add `sourceId`.
  - Add `ruleSnapshot`.
- Modify `WalletCoreServiceImpl`
  - Keep existing `gl_wallet_release` behavior.
  - Create `gl_wallet_turnover_task` when a credit requires turnover.
  - Persist snapshot values from business modules.
- Add `IWalletCurrencyPolicyService`
  - Method `listClientCurrencies(Long memberId, String channel)`.
  - Method `assertCurrencyUsable(Long memberId, String currencyCode, String action, String channel)`.
- Add `IWalletTurnoverTaskService`
  - Method `createFromCredit(...)`.
  - Method `applyValidTurnover(...)`.

### Business module integration

- Modify `DepositOrderServiceImpl`
  - First phase: keep existing deposit flow, but send `fundPropertyCode = DEPOSIT_PRINCIPAL` and `turnoverMultiplier = 1`.
  - Later phase: add deposit activity bonus split with `DEPOSIT_BONUS`.
- Modify `PromotionRewardServiceImpl`
  - Parse optional reward item fields: `fundPropertyCode`, `turnoverMultiplier`, `gameScopeType`, `gameScopeValue`, `turnoverExpireDays`.
  - Default daily reward items to `ACTIVITY_REWARD`.

### C-side API

- Modify or extend `ClientWalletController`
  - Add `GET /api/client/wallet/currencies`.
  - Return only currencies the current member can see.
  - Include action flags: deposit, withdraw, exchange, play.

### Admin UI

- First phase: keep current Wallet Rule page but relabel it as system-level technical policy.
- Add admin pages after backend foundation:
  - `admin-ui/src/views/wallet/currency-policy/index.vue`
  - `admin-ui/src/views/wallet/fund-property/index.vue`
  - `admin-ui/src/views/wallet/exchange-rule/index.vue`
  - `admin-ui/src/views/wallet/turnover-task/index.vue`

---

## Phase 1: Database Foundation and Seeds

**Files:**
- Modify: `backend/script/sql/gameluck_wallet.sql`
- Modify: `backend/script/sql/gameluck_platform_dict.sql`

- [x] **Step 1: Add guarded currency ability columns**

Add guarded `ALTER TABLE` blocks to `gameluck_wallet.sql`:

```sql
SET @db_name := DATABASE();

SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'gl_wallet_currency' AND COLUMN_NAME = 'deposit_enabled'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE gl_wallet_currency ADD COLUMN deposit_enabled CHAR(1) NOT NULL DEFAULT ''1'' COMMENT ''Deposit capable: 0 yes, 1 no'' AFTER freeze_enabled',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'gl_wallet_currency' AND COLUMN_NAME = 'play_enabled'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE gl_wallet_currency ADD COLUMN play_enabled CHAR(1) NOT NULL DEFAULT ''1'' COMMENT ''Play capable: 0 yes, 1 no'' AFTER exchange_enabled',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'gl_wallet_currency' AND COLUMN_NAME = 'exchange_in_enabled'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE gl_wallet_currency ADD COLUMN exchange_in_enabled CHAR(1) NOT NULL DEFAULT ''1'' COMMENT ''Exchange-in capable: 0 yes, 1 no'' AFTER exchange_enabled',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'gl_wallet_currency' AND COLUMN_NAME = 'exchange_out_enabled'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE gl_wallet_currency ADD COLUMN exchange_out_enabled CHAR(1) NOT NULL DEFAULT ''1'' COMMENT ''Exchange-out capable: 0 yes, 1 no'' AFTER exchange_in_enabled',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
```

- [x] **Step 2: Create fund property template table**

```sql
CREATE TABLE IF NOT EXISTS gl_wallet_fund_property_template (
  id BIGINT NOT NULL COMMENT 'Primary key',
  tenant_id VARCHAR(20) NOT NULL DEFAULT '000000' COMMENT 'Tenant id',
  property_code VARCHAR(64) NOT NULL COMMENT 'Fund property code',
  property_name VARCHAR(128) NOT NULL COMMENT 'Fund property name',
  default_source_type VARCHAR(64) NOT NULL COMMENT 'Default wallet source type',
  default_release_mode VARCHAR(32) NOT NULL DEFAULT 'IMMEDIATE' COMMENT 'Default release mode',
  default_turnover_multiplier DECIMAL(10,4) NOT NULL DEFAULT 0 COMMENT 'Default turnover multiplier',
  default_game_scope_type VARCHAR(32) NOT NULL DEFAULT 'ALL' COMMENT 'ALL, CATEGORY, PROVIDER, GAME',
  default_game_scope_value VARCHAR(1000) DEFAULT NULL COMMENT 'Default game scope value',
  withdraw_enabled CHAR(1) NOT NULL DEFAULT '1' COMMENT 'Withdraw capable: 0 yes, 1 no',
  exchange_enabled CHAR(1) NOT NULL DEFAULT '1' COMMENT 'Exchange capable: 0 yes, 1 no',
  status CHAR(1) NOT NULL DEFAULT '0' COMMENT 'Status: 0 enabled, 1 disabled',
  sort_order INT NOT NULL DEFAULT 0 COMMENT 'Sort order',
  remark VARCHAR(500) DEFAULT NULL COMMENT 'Remark',
  create_time DATETIME DEFAULT NULL COMMENT 'Create time',
  update_time DATETIME DEFAULT NULL COMMENT 'Update time',
  version INT NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version',
  del_flag CHAR(1) NOT NULL DEFAULT '0' COMMENT 'Delete flag: 0 normal, 1 deleted',
  PRIMARY KEY (id),
  UNIQUE KEY uk_gl_wallet_fund_property_template_01 (tenant_id, property_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Wallet fund property template';
```

- [x] **Step 3: Create currency visibility policy table**

```sql
CREATE TABLE IF NOT EXISTS gl_wallet_currency_policy (
  id BIGINT NOT NULL COMMENT 'Primary key',
  tenant_id VARCHAR(20) NOT NULL DEFAULT '000000' COMMENT 'Tenant id',
  policy_name VARCHAR(128) NOT NULL COMMENT 'Policy name',
  currency_code VARCHAR(32) NOT NULL COMMENT 'Currency code',
  member_tag VARCHAR(64) DEFAULT NULL COMMENT 'Member tag condition',
  vip_level VARCHAR(32) DEFAULT NULL COMMENT 'VIP level condition',
  country_code VARCHAR(16) DEFAULT NULL COMMENT 'Country condition',
  state_code VARCHAR(32) DEFAULT NULL COMMENT 'State condition',
  channel VARCHAR(32) DEFAULT NULL COMMENT 'Channel condition',
  visible_enabled CHAR(1) NOT NULL DEFAULT '0' COMMENT 'Visible: 0 yes, 1 no',
  deposit_enabled CHAR(1) NOT NULL DEFAULT '1' COMMENT 'Deposit: 0 yes, 1 no',
  withdraw_enabled CHAR(1) NOT NULL DEFAULT '1' COMMENT 'Withdraw: 0 yes, 1 no',
  exchange_enabled CHAR(1) NOT NULL DEFAULT '1' COMMENT 'Exchange: 0 yes, 1 no',
  play_enabled CHAR(1) NOT NULL DEFAULT '1' COMMENT 'Play: 0 yes, 1 no',
  priority INT NOT NULL DEFAULT 0 COMMENT 'Higher priority wins; deny remains strict',
  status CHAR(1) NOT NULL DEFAULT '0' COMMENT 'Status: 0 enabled, 1 disabled',
  start_time DATETIME DEFAULT NULL COMMENT 'Start time',
  end_time DATETIME DEFAULT NULL COMMENT 'End time',
  remark VARCHAR(500) DEFAULT NULL COMMENT 'Remark',
  create_time DATETIME DEFAULT NULL COMMENT 'Create time',
  update_time DATETIME DEFAULT NULL COMMENT 'Update time',
  version INT NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version',
  del_flag CHAR(1) NOT NULL DEFAULT '0' COMMENT 'Delete flag: 0 normal, 1 deleted',
  PRIMARY KEY (id),
  KEY idx_gl_wallet_currency_policy_01 (tenant_id, currency_code, status, priority),
  KEY idx_gl_wallet_currency_policy_02 (tenant_id, country_code, state_code, channel)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Wallet currency visibility and action policy';
```

- [x] **Step 4: Create turnover task table**

```sql
CREATE TABLE IF NOT EXISTS gl_wallet_turnover_task (
  id BIGINT NOT NULL COMMENT 'Primary key',
  tenant_id VARCHAR(20) NOT NULL DEFAULT '000000' COMMENT 'Tenant id',
  turnover_task_no VARCHAR(64) NOT NULL COMMENT 'Turnover task no',
  member_id BIGINT NOT NULL COMMENT 'Member id',
  currency_code VARCHAR(32) NOT NULL COMMENT 'Currency code',
  fund_property_code VARCHAR(64) NOT NULL COMMENT 'Fund property code',
  source_type VARCHAR(64) NOT NULL COMMENT 'Wallet source type',
  source_id VARCHAR(128) DEFAULT NULL COMMENT 'Source id',
  business_no VARCHAR(128) NOT NULL COMMENT 'Business no',
  wallet_transaction_no VARCHAR(64) DEFAULT NULL COMMENT 'Wallet transaction no',
  reward_amount DECIMAL(20,6) NOT NULL DEFAULT 0 COMMENT 'Reward amount',
  required_turnover DECIMAL(20,6) NOT NULL DEFAULT 0 COMMENT 'Required turnover',
  completed_turnover DECIMAL(20,6) NOT NULL DEFAULT 0 COMMENT 'Completed turnover',
  game_scope_type VARCHAR(32) NOT NULL DEFAULT 'ALL' COMMENT 'ALL, CATEGORY, PROVIDER, GAME',
  game_scope_value VARCHAR(1000) DEFAULT NULL COMMENT 'Allowed game scope',
  rule_snapshot JSON DEFAULT NULL COMMENT 'Immutable rule snapshot',
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING, COMPLETED, EXPIRED, CANCELLED',
  expire_time DATETIME DEFAULT NULL COMMENT 'Expire time',
  complete_time DATETIME DEFAULT NULL COMMENT 'Complete time',
  remark VARCHAR(500) DEFAULT NULL COMMENT 'Remark',
  create_time DATETIME DEFAULT NULL COMMENT 'Create time',
  update_time DATETIME DEFAULT NULL COMMENT 'Update time',
  version INT NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version',
  del_flag CHAR(1) NOT NULL DEFAULT '0' COMMENT 'Delete flag: 0 normal, 1 deleted',
  PRIMARY KEY (id),
  UNIQUE KEY uk_gl_wallet_turnover_task_01 (tenant_id, turnover_task_no),
  KEY idx_gl_wallet_turnover_task_01 (tenant_id, member_id, currency_code, status),
  KEY idx_gl_wallet_turnover_task_02 (tenant_id, business_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Wallet turnover task snapshot';
```

- [x] **Step 5: Create exchange rule and order tables**

```sql
CREATE TABLE IF NOT EXISTS gl_wallet_exchange_rule (
  id BIGINT NOT NULL COMMENT 'Primary key',
  tenant_id VARCHAR(20) NOT NULL DEFAULT '000000' COMMENT 'Tenant id',
  rule_name VARCHAR(128) NOT NULL COMMENT 'Exchange rule name',
  from_currency_code VARCHAR(32) NOT NULL COMMENT 'Source currency code',
  to_currency_code VARCHAR(32) NOT NULL COMMENT 'Target currency code',
  rate_type VARCHAR(32) NOT NULL DEFAULT 'FIXED' COMMENT 'FIXED, TIERED, ACTIVITY',
  rate_value DECIMAL(20,8) NOT NULL COMMENT 'Target amount per one source unit',
  min_from_amount DECIMAL(20,6) NOT NULL DEFAULT 0 COMMENT 'Minimum source amount',
  max_from_amount DECIMAL(20,6) NOT NULL DEFAULT 0 COMMENT 'Maximum source amount, 0 unlimited',
  daily_from_limit DECIMAL(20,6) NOT NULL DEFAULT 0 COMMENT 'Daily source amount limit, 0 unlimited',
  fee_type VARCHAR(32) NOT NULL DEFAULT 'NONE' COMMENT 'NONE, FIXED, PERCENT',
  fee_value DECIMAL(20,6) NOT NULL DEFAULT 0 COMMENT 'Fee value',
  turnover_required CHAR(1) NOT NULL DEFAULT '1' COMMENT 'Target turnover required: 0 yes, 1 no',
  turnover_multiplier DECIMAL(10,4) NOT NULL DEFAULT 0 COMMENT 'Target turnover multiplier',
  game_scope_type VARCHAR(32) NOT NULL DEFAULT 'ALL' COMMENT 'ALL, CATEGORY, PROVIDER, GAME',
  game_scope_value VARCHAR(1000) DEFAULT NULL COMMENT 'Allowed game scope',
  country_code VARCHAR(16) DEFAULT NULL COMMENT 'Country condition',
  state_code VARCHAR(32) DEFAULT NULL COMMENT 'State condition',
  member_tag VARCHAR(64) DEFAULT NULL COMMENT 'Member tag condition',
  channel VARCHAR(32) DEFAULT NULL COMMENT 'Channel condition',
  status CHAR(1) NOT NULL DEFAULT '0' COMMENT 'Status: 0 enabled, 1 disabled',
  start_time DATETIME DEFAULT NULL COMMENT 'Start time',
  end_time DATETIME DEFAULT NULL COMMENT 'End time',
  remark VARCHAR(500) DEFAULT NULL COMMENT 'Remark',
  create_time DATETIME DEFAULT NULL COMMENT 'Create time',
  update_time DATETIME DEFAULT NULL COMMENT 'Update time',
  version INT NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version',
  del_flag CHAR(1) NOT NULL DEFAULT '0' COMMENT 'Delete flag: 0 normal, 1 deleted',
  PRIMARY KEY (id),
  KEY idx_gl_wallet_exchange_rule_01 (tenant_id, from_currency_code, to_currency_code, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Wallet currency exchange rule';

CREATE TABLE IF NOT EXISTS gl_wallet_exchange_order (
  id BIGINT NOT NULL COMMENT 'Primary key',
  tenant_id VARCHAR(20) NOT NULL DEFAULT '000000' COMMENT 'Tenant id',
  exchange_order_no VARCHAR(64) NOT NULL COMMENT 'Exchange order no',
  member_id BIGINT NOT NULL COMMENT 'Member id',
  exchange_rule_id BIGINT NOT NULL COMMENT 'Exchange rule id',
  from_currency_code VARCHAR(32) NOT NULL COMMENT 'Source currency code',
  from_amount DECIMAL(20,6) NOT NULL COMMENT 'Source amount',
  to_currency_code VARCHAR(32) NOT NULL COMMENT 'Target currency code',
  to_amount DECIMAL(20,6) NOT NULL COMMENT 'Target amount',
  fee_amount DECIMAL(20,6) NOT NULL DEFAULT 0 COMMENT 'Fee amount',
  debit_transaction_no VARCHAR(64) DEFAULT NULL COMMENT 'Debit wallet transaction no',
  credit_transaction_no VARCHAR(64) DEFAULT NULL COMMENT 'Credit wallet transaction no',
  turnover_task_no VARCHAR(64) DEFAULT NULL COMMENT 'Generated turnover task no',
  rule_snapshot JSON DEFAULT NULL COMMENT 'Immutable rule snapshot',
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING, SUCCESS, FAILED, CANCELLED',
  fail_reason VARCHAR(500) DEFAULT NULL COMMENT 'Fail reason',
  create_time DATETIME DEFAULT NULL COMMENT 'Create time',
  update_time DATETIME DEFAULT NULL COMMENT 'Update time',
  version INT NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version',
  del_flag CHAR(1) NOT NULL DEFAULT '0' COMMENT 'Delete flag: 0 normal, 1 deleted',
  PRIMARY KEY (id),
  UNIQUE KEY uk_gl_wallet_exchange_order_01 (tenant_id, exchange_order_no),
  KEY idx_gl_wallet_exchange_order_01 (tenant_id, member_id, status, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Wallet currency exchange order';
```

- [x] **Step 6: Seed fund property templates**

Seed these `property_code` values:

| property_code | name | default_source_type | release mode | multiplier | scope |
| --- | --- | --- | --- | --- | --- |
| DEPOSIT_PRINCIPAL | 充值本金 | DEPOSIT | IMMEDIATE | 1 | ALL |
| DEPOSIT_BONUS | 充值赠送 | PROMOTION | AFTER_TURNOVER | 10 | ALL |
| ACTIVITY_REWARD | 活动奖励 | PROMOTION | AFTER_TURNOVER | 0 | ALL |
| DAILY_REWARD | 每日奖励 | DAILY_REWARD | IMMEDIATE | 0 | ALL |
| COMMISSION | 返佣奖励 | COMMISSION | AFTER_TURNOVER | 1 | ALL |
| GAME_PROFIT | 游戏派彩 | GAME_PROFIT | AFTER_TURNOVER | 1 | ALL |
| GAME_REFUND | 游戏退款 | GAME_REFUND | IMMEDIATE | 0 | ALL |
| MANUAL_ADJUST | 人工调整 | MANUAL_ADJUST | MANUAL_REVIEW | 0 | ALL |
| EXCHANGE_IN | 兑换入账 | EXCHANGE | IMMEDIATE | 0 | ALL |

- [x] **Step 7: Import SQL locally**

Run:

```powershell
.\backend\script\bin\import-sql-utf8.ps1 -SqlPath backend\script\sql\gameluck_wallet.sql
```

Expected:

```text
Import completed
```

- [x] **Step 8: Verify new tables and seeds**

Run:

```powershell
mysql -uroot -proot -D gameluck_vue -e "SHOW TABLES LIKE 'gl_wallet_%'; SELECT property_code, default_source_type FROM gl_wallet_fund_property_template ORDER BY sort_order; SELECT currency_code, deposit_enabled, withdraw_enabled, exchange_in_enabled, exchange_out_enabled, play_enabled FROM gl_wallet_currency ORDER BY sort_order;"
```

Expected:

- The output includes `gl_wallet_fund_property_template`, `gl_wallet_currency_policy`, `gl_wallet_turnover_task`, `gl_wallet_exchange_rule`, and `gl_wallet_exchange_order`.
- Fund property rows include `DEPOSIT_PRINCIPAL`, `DEPOSIT_BONUS`, `ACTIVITY_REWARD`, `DAILY_REWARD`, and `EXCHANGE_IN`.

---

## Phase 2: Backend Domain and C-Side Currency Policy

**Files:**
- Create wallet domain/bo/vo/mapper/service classes for `WalletCurrencyPolicy`.
- Modify `ClientWalletController`.
- Test: `backend/gameluck-modules/gameluck-wallet/src/test/java/com/gameluck/wallet/service/impl/WalletCurrencyPolicyServiceImplTest.java`

- [x] **Step 1: Write failing test for country policy hiding a currency**

Test intent:

```java
@Test
@Tag("local")
void listClientCurrenciesHidesCurrencyDeniedByCountryPolicy() {
    // Given member 1001 has country_code = "US" and state_code = "WA".
    // Given SC exists and is enabled.
    // Given a policy for SC + US + visible_enabled = disabled.
    // When listClientCurrencies(1001L, "H5") is called.
    // Then GC is returned and SC is not returned.
}
```

- [x] **Step 2: Implement minimal policy matching**

Rules:

- Start from enabled `gl_wallet_currency` rows.
- Load member `country_code` and `state_code`.
- Apply enabled matching policies by currency.
- Empty condition means "all".
- More restrictive flag wins: if a matching policy disables visibility, hide the currency.
- Return C-side action flags after combining base currency flags and policy flags.

- [x] **Step 3: Add C-side API**

Endpoint:

```text
GET /api/client/wallet/currencies
```

Response fields:

```json
{
  "currencyCode": "SC",
  "currencyName": "Sweep Coin",
  "scaleNum": 6,
  "depositEnabled": false,
  "withdrawEnabled": true,
  "exchangeEnabled": true,
  "playEnabled": true
}
```

- [x] **Step 4: Verify policy tests**

Run:

```powershell
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-modules/gameluck-wallet -am -DskipTests=false "-Dprofiles.active=local" "-Dtest=WalletCurrencyPolicyServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected:

```text
BUILD SUCCESS
Tests run: >=1, Failures: 0, Errors: 0
```

---

## Phase 3: Turnover Task Snapshot Foundation

**Files:**
- Create `WalletTurnoverTask` domain/mapper/service.
- Modify `WalletCreditBo`.
- Modify `WalletCoreServiceImpl`.
- Test: `WalletCoreServiceImplTest`.

- [x] **Step 1: Write failing test for credit creating turnover task**

Test intent:

```java
@Test
@Tag("local")
void creditCreatesTurnoverTaskWhenRequiredAmountIsPositive() {
    // Given a credit request for SC ACTIVITY_REWARD amount 20.
    // Given turnoverRequiredAmount is 200.
    // Given game scope type is GAME and value is "slot-001,slot-002".
    // When credit succeeds.
    // Then a wallet transaction is successful.
    // And gl_wallet_turnover_task receives one snapshot row.
    // And the snapshot stores required_turnover = 200 and game_scope_value.
}
```

- [x] **Step 2: Extend `WalletCreditBo`**

Fields:

```java
private String fundPropertyCode;
private BigDecimal turnoverMultiplier;
private BigDecimal turnoverRequiredAmount;
private String gameScopeType;
private String gameScopeValue;
private String sourceId;
private String ruleSnapshot;
private Date turnoverExpireTime;
```

- [x] **Step 3: Create turnover task when credit succeeds**

Creation rules:

- If `turnoverRequiredAmount > 0`, create task using that value.
- Else if `turnoverMultiplier > 0`, create task amount `creditAmount * turnoverMultiplier`.
- Else do not create task.
- Store `businessNo`, `walletTransactionNo`, `sourceType`, `sourceId`, `fundPropertyCode`, game scope, and snapshot.

- [x] **Step 4: Keep existing release flow compatible**

Do not remove `gl_wallet_release` writes. Current H5/admin pages depend on it.

- [x] **Step 5: Verify wallet core tests**

Run:

```powershell
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-modules/gameluck-wallet -am -DskipTests=false "-Dprofiles.active=local" "-Dtest=WalletCoreServiceImplTest,WalletRuleServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected:

```text
BUILD SUCCESS
```

---

## Phase 4: Business Module Hook Points

**Files:**
- Modify `DepositOrderServiceImpl`.
- Modify `PromotionRewardServiceImpl`.
- Tests: `DepositOrderServiceImplTest`, `PromotionRewardServiceImplTest`.

- [x] **Step 1: Deposit principal uses `DEPOSIT_PRINCIPAL`**

For simulated deposit success, set:

```java
creditBo.setFundPropertyCode("DEPOSIT_PRINCIPAL");
creditBo.setTurnoverMultiplier(BigDecimal.ONE);
creditBo.setGameScopeType("ALL");
creditBo.setSourceId(order.getId().toString());
```

- [x] **Step 2: Promotion reward items accept optional turnover fields**

Reward item JSON supports:

```json
{
  "currencyCode": "SC",
  "rewardAmount": "20.000000",
  "fundPropertyCode": "ACTIVITY_REWARD",
  "turnoverMultiplier": "10",
  "gameScopeType": "GAME",
  "gameScopeValue": "slot-001,slot-002",
  "turnoverExpireDays": 7
}
```

- [x] **Step 3: Preserve old reward items**

If old reward items only contain `currencyCode` and `rewardAmount`, default to:

- `fundPropertyCode = DAILY_REWARD` for daily login.
- `turnoverMultiplier = null`, so current wallet rule fallback remains effective.
- `gameScopeType = ALL`.

- [x] **Step 4: Verify payment and promotion tests**

Run:

```powershell
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-admin -am -DskipTests=false "-Dprofiles.active=local" "-Dtest=DepositOrderServiceImplTest,PromotionRewardServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected:

```text
BUILD SUCCESS
```

---

## Phase 5: Exchange Rule Skeleton

**Files:**
- Create `WalletExchangeRule` and `WalletExchangeOrder` CRUD backend.
- Add admin menu SQL only after backend compiles.
- Do not expose C-side exchange execution until rules and audit pages are usable.

- [x] **Step 1: Add exchange rule list/detail CRUD**

Minimum required fields:

- from currency
- to currency
- rate type
- rate value
- min/max/daily limit
- fee type/value
- turnover multiplier
- game scope
- country/state/member tag/channel
- status

- [x] **Step 2: Add rule validation**

Validation rules:

- `from_currency_code != to_currency_code`.
- `rate_value > 0`.
- If `fee_type = PERCENT`, `fee_value >= 0` and `fee_value <= 100`.
- If `turnover_required = enabled`, `turnover_multiplier >= 0`.

- [x] **Step 3: Add exchange option query service**

Service method:

```java
List<ClientExchangeOptionVo> listOptions(Long memberId, String channel)
```

This only returns options. It does not execute exchange yet.

- [x] **Step 4: Verify exchange rule tests**

Run:

```powershell
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-modules/gameluck-wallet -am -DskipTests=false "-Dprofiles.active=local" "-Dtest=WalletExchangeRuleServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected:

```text
BUILD SUCCESS
```

---

## Phase 6: Admin UI Surface

**Files:**
- Create `admin-ui/src/api/wallet/currencyPolicy/index.ts`.
- Create `admin-ui/src/api/wallet/fundProperty/index.ts`.
- Create `admin-ui/src/api/wallet/exchangeRule/index.ts`.
- Create `admin-ui/src/views/wallet/currency-policy/index.vue`.
- Create `admin-ui/src/views/wallet/fund-property/index.vue`.
- Create `admin-ui/src/views/wallet/exchange-rule/index.vue`.
- Modify i18n files used by admin-ui.

- [x] **Step 1: Add fund property read-only page**

Operators can view default fund properties, but editing requires a high-risk permission.

- [x] **Step 2: Add currency policy page**

Operators can configure visibility/action policies by currency, country/state, tag, channel, and priority.

- [x] **Step 3: Add exchange rule page**

Operators can create disabled exchange rules first, then enable after review.

- [x] **Step 4: Keep wallet rule page as advanced**

Rename page copy to indicate it is a system-level fallback policy, not an activity setup page.

- [x] **Step 5: Verify admin UI**

Run:

```powershell
pnpm --dir admin-ui check:i18n
$env:NODE_OPTIONS='--max-old-space-size=4096'; pnpm --dir admin-ui build:dev
```

Expected:

```text
check:i18n passed
build completed
```

---

## Acceptance Checklist

- [ ] Adding a new currency requires only currency definition plus default generated/seeded policy, not manual per-activity wallet rules.
- [ ] C-side currency list is calculated by backend using member country/state and policy rows.
- [ ] Operators configure activity/deposit business rules; they do not need to create wallet rules for every activity.
- [ ] Credits with turnover requirements create immutable turnover task snapshots.
- [ ] Existing wallet release, wallet transaction, daily reward, deposit, and redemption behavior remains compatible.
- [ ] Exchange tables and admin rule configuration exist, but C-side exchange execution is not enabled until a later controlled phase.
- [ ] All money-affecting configuration has tenant id, status, audit-friendly timestamps, and soft delete/version fields.

## Verification Commands

Run before claiming the foundation is complete:

```powershell
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-modules/gameluck-wallet -am -DskipTests=false "-Dprofiles.active=local" "-Dsurefire.failIfNoSpecifiedTests=false" test
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-admin -am compile -Plocal -DskipTests
pnpm --dir admin-ui check:i18n
git diff --check
```

## Execution Notes

- Do not remove the current `gl_wallet_rule` flow in this phase. It remains the compatibility fallback.
- Do not make exchange execution available to C-side users in this phase.
- Do not overwrite locally configured GC/SC wallet rules when importing seed SQL; only add missing defaults or guarded schema changes.
- Preserve current uncommitted admin-ui wallet rule drawer changes unless the user explicitly asks to discard them.
