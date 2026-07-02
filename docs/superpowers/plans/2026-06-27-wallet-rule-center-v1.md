# Wallet Rule Center v1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a rule-driven wallet configuration layer so GC / SC / RC behavior can be controlled by tenant, currency, and source type instead of hard-coded wallet inputs.

**Architecture:** Add `gl_wallet_rule` as the policy layer above wallet core accounting. Wallet core keeps balances, ledgers, releases, and freezes; wallet rules decide whether a source is allowed and which release mode applies. The first version uses simple per-source rules and avoids a generic expression engine.

**Tech Stack:** Java 17, Spring Boot, MyBatis-Plus, MySQL, Vue 3, Element Plus, TypeScript.

---

## File Structure

- Create `backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/domain/WalletRule.java`: rule entity mapped to `gl_wallet_rule`.
- Create `backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/domain/bo/WalletRuleBo.java`: query and edit request object.
- Create `backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/domain/vo/WalletRuleVo.java`: admin response object.
- Create `backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/mapper/WalletRuleMapper.java`: MyBatis mapper.
- Create `backend/gameluck-modules/gameluck-wallet/src/main/resources/mapper/wallet/WalletRuleMapper.xml`: mapper XML placeholder and rule lookup SQL if needed.
- Create `backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/service/IWalletRuleService.java`: admin CRUD/query and core rule resolution contract.
- Create `backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/service/impl/WalletRuleServiceImpl.java`: rule query/update and source matching logic.
- Create `backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/controller/WalletRuleController.java`: admin API at `/wallet/rule`.
- Modify `backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/domain/bo/WalletCreditBo.java`: make `releaseMode` and `requiredTurnover` optional compatibility fields.
- Modify `backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/service/impl/WalletCoreServiceImpl.java`: resolve credit behavior from `IWalletRuleService`.
- Modify `backend/script/sql/gameluck_wallet.sql`: add `gl_wallet_rule` table, default GC / SC / RC rules, and admin menu seed.
- Create `admin-ui/src/api/wallet/rule/index.ts`: wallet rule API wrapper.
- Create `admin-ui/src/api/wallet/rule/types.ts`: wallet rule TS types.
- Create `admin-ui/src/views/wallet/rule/index.vue`: wallet rule admin page.

## Rule Semantics

Rule matching key:

- `tenant_id`
- `currency_code`
- `source_type`

Default source types:

- `GAME_PROFIT`: game profit source. SC can be exchangeable here.
- `DEPOSIT`: cash deposit source. RC can be withdrawable here.
- `PROMOTION`: campaign or bonus source. Default requires review or no exchange.
- `MANUAL_ADJUST`: back-office correction. Default manual review.

Core fields:

- `credit_enabled`: whether this source can credit this currency.
- `debit_enabled`: whether this source can debit this currency.
- `withdraw_enabled`: whether resulting value can be withdrawn.
- `exchange_enabled`: whether resulting value can be exchanged.
- `release_mode`: `IMMEDIATE`, `AFTER_TURNOVER`, `NEVER`, `MANUAL_REVIEW`.
- `turnover_required`: whether a business turnover value must be supplied by the caller.
- `default_required_turnover`: fixed required turnover amount used when caller does not supply one.
- `status`: `0` enabled, `1` disabled.

Important boundary:

- Wallet rules do not calculate a turnover multiplier from bet amount or deposit amount.
- If a business module needs `amount * multiplier`, it calculates the final required turnover and passes it to wallet. Wallet validates whether that value is required and records it.

---

## Task 1: Database Rule Table And Seeds

**Files:**
- Modify: `backend/script/sql/gameluck_wallet.sql`

- [x] **Step 1: Add rule table SQL**

Append this table after `gl_wallet_currency`:

```sql
CREATE TABLE IF NOT EXISTS gl_wallet_rule (
  id BIGINT NOT NULL COMMENT 'Primary key',
  tenant_id VARCHAR(20) NOT NULL DEFAULT '000000' COMMENT 'Tenant id',
  currency_code VARCHAR(32) NOT NULL COMMENT 'Currency code',
  source_type VARCHAR(64) NOT NULL COMMENT 'Source type',
  rule_name VARCHAR(128) NOT NULL COMMENT 'Rule name',
  credit_enabled CHAR(1) NOT NULL DEFAULT '1' COMMENT 'Credit allowed: 0 yes, 1 no',
  debit_enabled CHAR(1) NOT NULL DEFAULT '1' COMMENT 'Debit allowed: 0 yes, 1 no',
  withdraw_enabled CHAR(1) NOT NULL DEFAULT '1' COMMENT 'Withdraw capable: 0 yes, 1 no',
  exchange_enabled CHAR(1) NOT NULL DEFAULT '1' COMMENT 'Exchange capable: 0 yes, 1 no',
  release_mode VARCHAR(32) NOT NULL DEFAULT 'NEVER' COMMENT 'Release mode',
  turnover_required CHAR(1) NOT NULL DEFAULT '1' COMMENT 'Business turnover required: 0 yes, 1 no',
  default_required_turnover DECIMAL(20,6) NOT NULL DEFAULT 0 COMMENT 'Default required turnover',
  status CHAR(1) NOT NULL DEFAULT '0' COMMENT 'Status: 0 enabled, 1 disabled',
  sort_order INT NOT NULL DEFAULT 0 COMMENT 'Sort order',
  remark VARCHAR(500) DEFAULT NULL COMMENT 'Remark',
  create_dept BIGINT DEFAULT NULL COMMENT 'Create department',
  create_by BIGINT DEFAULT NULL COMMENT 'Created by',
  create_time DATETIME DEFAULT NULL COMMENT 'Create time',
  update_by BIGINT DEFAULT NULL COMMENT 'Updated by',
  update_time DATETIME DEFAULT NULL COMMENT 'Update time',
  version INT NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version',
  del_flag CHAR(1) NOT NULL DEFAULT '0' COMMENT 'Delete flag: 0 normal, 1 deleted',
  PRIMARY KEY (id),
  UNIQUE KEY uk_gl_wallet_rule_01 (tenant_id, currency_code, source_type),
  KEY idx_gl_wallet_rule_01 (tenant_id, currency_code, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Wallet source rule';
```

- [x] **Step 2: Add default rule seeds**

Append after the default currency seed:

```sql
INSERT INTO gl_wallet_rule
(id, tenant_id, currency_code, source_type, rule_name, credit_enabled, debit_enabled, withdraw_enabled, exchange_enabled, release_mode, turnover_required, default_required_turnover, status, sort_order, remark, create_time)
VALUES
(1900000000000000101, '000000', 'GC', 'GAME_PROFIT', 'GC game profit', '0', '0', '1', '1', 'NEVER', '1', 0, '0', 1, 'GC is a play currency and is not withdrawable or exchangeable.', NOW()),
(1900000000000000102, '000000', 'SC', 'GAME_PROFIT', 'SC game profit', '0', '0', '1', '0', 'AFTER_TURNOVER', '0', 0, '0', 2, 'SC is exchangeable only for game profit source after configured conditions.', NOW()),
(1900000000000000103, '000000', 'SC', 'PROMOTION', 'SC promotion', '0', '0', '1', '1', 'MANUAL_REVIEW', '0', 0, '0', 3, 'Promotional SC requires review by default.', NOW()),
(1900000000000000104, '000000', 'RC', 'DEPOSIT', 'RC deposit', '0', '0', '0', '1', 'IMMEDIATE', '1', 0, '0', 4, 'RC deposit can be withdrawable immediately unless tenant changes the rule.', NOW()),
(1900000000000000105, '000000', 'RC', 'MANUAL_ADJUST', 'RC manual adjustment', '0', '0', '0', '1', 'MANUAL_REVIEW', '1', 0, '0', 5, 'Manual RC adjustment requires review by default.', NOW())
ON DUPLICATE KEY UPDATE
  rule_name = VALUES(rule_name),
  credit_enabled = VALUES(credit_enabled),
  debit_enabled = VALUES(debit_enabled),
  withdraw_enabled = VALUES(withdraw_enabled),
  exchange_enabled = VALUES(exchange_enabled),
  release_mode = VALUES(release_mode),
  turnover_required = VALUES(turnover_required),
  default_required_turnover = VALUES(default_required_turnover),
  status = VALUES(status),
  sort_order = VALUES(sort_order),
  remark = VALUES(remark),
  update_time = NOW();
```

- [x] **Step 3: Add wallet rule menu seed**

Add a new child menu under wallet center:

```sql
INSERT INTO sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES
(1806, '钱包规则', 1800, 6, 'rule', 'wallet/rule/index', '', 1, 0, 'C', '0', '0', 'wallet:rule:list', 'setting', 103, 1, NOW(), NULL, NULL, '钱包来源规则菜单'),
(1817, '规则查询', 1806, 1, '#', '', '', 1, 0, 'F', '0', '0', 'wallet:rule:query', '#', 103, 1, NOW(), NULL, NULL, ''),
(1818, '规则新增', 1806, 2, '#', '', '', 1, 0, 'F', '0', '0', 'wallet:rule:add', '#', 103, 1, NOW(), NULL, NULL, ''),
(1819, '规则编辑', 1806, 3, '#', '', '', 1, 0, 'F', '0', '0', 'wallet:rule:edit', '#', 103, 1, NOW(), NULL, NULL, '')
ON DUPLICATE KEY UPDATE
  menu_name = VALUES(menu_name),
  parent_id = VALUES(parent_id),
  order_num = VALUES(order_num),
  path = VALUES(path),
  component = VALUES(component),
  perms = VALUES(perms),
  icon = VALUES(icon),
  remark = VALUES(remark),
  update_time = NOW();
```

- [x] **Step 4: Apply SQL locally**

Run:

```powershell
mysql -uroot -proot gameluck_vue < backend\script\sql\gameluck_wallet.sql
```

Expected: exit code `0`.

- [x] **Step 5: Verify table and seeds**

Run:

```powershell
mysql -uroot -proot gameluck_vue -e "select currency_code, source_type, release_mode, turnover_required, status from gl_wallet_rule order by sort_order;"
```

Expected: five rows for GC / SC / RC.

---

## Task 2: Backend Rule Model And Admin Query API

**Files:**
- Create: `backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/domain/WalletRule.java`
- Create: `backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/domain/bo/WalletRuleBo.java`
- Create: `backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/domain/vo/WalletRuleVo.java`
- Create: `backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/mapper/WalletRuleMapper.java`
- Create: `backend/gameluck-modules/gameluck-wallet/src/main/resources/mapper/wallet/WalletRuleMapper.xml`
- Create: `backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/service/IWalletRuleService.java`
- Create: `backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/service/impl/WalletRuleServiceImpl.java`
- Create: `backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/controller/WalletRuleController.java`

- [x] **Step 1: Create rule entity, BO, VO, mapper, and XML**

Follow existing `WalletCurrency` patterns. Entity fields must match `gl_wallet_rule`. BO and VO must include all admin fields.

- [x] **Step 2: Write failing service query test if module test setup supports it**

Create a focused test for rule lookup by tenant/currency/source. If wallet module cannot run tests without extra setup, document that and use backend compile as the verification gate.

Verification note: wallet module currently has no dedicated test harness in this workspace; backend compile and local SQL seed verification were used as the gate for this step.

- [x] **Step 3: Implement `IWalletRuleService`**

Expose:

```java
TableDataInfo<WalletRuleVo> queryPageList(WalletRuleBo bo, PageQuery pageQuery);
WalletRuleVo queryById(Long id);
List<WalletRuleVo> queryList(WalletRuleBo bo);
Boolean insertByBo(WalletRuleBo bo);
Boolean updateByBo(WalletRuleBo bo);
WalletRuleVo resolveCreditRule(String tenantId, String currencyCode, String sourceType);
```

- [x] **Step 4: Implement `WalletRuleServiceImpl`**

Rules:

- Query filters by `tenantId`, `currencyCode`, `sourceType`, `status`.
- Sort by `currencyCode`, `sortOrder`, `sourceType`.
- `resolveCreditRule` must return enabled rule where `creditEnabled = '0'`.
- If no rule exists, throw `ServiceException("钱包规则不存在")`.
- If rule exists but disabled or credit disabled, throw `ServiceException("钱包规则未启用或不允许入账")`.

- [x] **Step 5: Implement `WalletRuleController`**

Endpoints:

- `GET /wallet/rule/list` with `wallet:rule:list`.
- `GET /wallet/rule/{id}` with `wallet:rule:query`.
- `POST /wallet/rule` with `wallet:rule:add`.
- `PUT /wallet/rule` with `wallet:rule:edit`.

- [x] **Step 6: Compile backend**

Run:

```powershell
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-admin -am compile -Plocal -DskipTests
```

Expected: `BUILD SUCCESS`.

---

## Task 3: Connect Rules To Wallet Credit

**Files:**
- Modify: `backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/domain/bo/WalletCreditBo.java`
- Modify: `backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/service/impl/WalletCoreServiceImpl.java`

- [x] **Step 1: Make compatibility credit fields optional**

Remove `@NotBlank` from `releaseMode` and `@NotNull` from `requiredTurnover`. Keep `@DecimalMin("0")` on `requiredTurnover` when present.

- [x] **Step 2: Inject `IWalletRuleService` into `WalletCoreServiceImpl`**

Add constructor dependency:

```java
private final IWalletRuleService walletRuleService;
```

- [x] **Step 3: Resolve credit behavior from rules**

In `credit`:

- Resolve tenant id.
- Load rule by tenant/currency/source.
- Use `rule.releaseMode` as the default release mode.
- If caller supplies `releaseMode`, it must match the rule. If not, throw `ServiceException("入账释放模式与钱包规则不一致")`.
- If `turnoverRequired = '0'`, required turnover must be greater than or equal to `0`. If caller does not supply it, use `defaultRequiredTurnover`.
- If `turnoverRequired = '1'`, use caller value when present, otherwise `defaultRequiredTurnover`.

- [x] **Step 4: Keep wallet accounting unchanged**

Do not change:

- account creation
- idempotency behavior
- balance update
- transaction insert/update
- release record insert/update

- [x] **Step 5: Compile backend**

Run:

```powershell
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-admin -am compile -Plocal -DskipTests
```

Expected: `BUILD SUCCESS`.

---

## Task 4: Admin UI Wallet Rule Page

**Files:**
- Create: `admin-ui/src/api/wallet/rule/index.ts`
- Create: `admin-ui/src/api/wallet/rule/types.ts`
- Create: `admin-ui/src/views/wallet/rule/index.vue`

- [x] **Step 1: Create TypeScript types**

`RuleVO`, `RuleForm`, and `RuleQuery` fields must mirror backend `WalletRuleVo`.

- [x] **Step 2: Create API wrapper**

Functions:

```ts
listRule(query)
getRule(id)
addRule(data)
updateRule(data)
```

- [x] **Step 3: Create rule page**

Page capabilities:

- Search by currency, source type, status.
- Table columns: currency, source, rule name, release mode, turnover required, withdraw, exchange, status.
- Add/edit dialog for all configurable fields.
- Use switches with existing `0 = yes/enabled`, `1 = no/disabled` convention.

- [x] **Step 4: Build frontend**

Run:

```powershell
pnpm --dir admin-ui build:prod
```

Expected: exit code `0`.

---

## Task 5: Local Verification And Restart

**Files:**
- No source changes expected unless verification exposes a defect.

- [x] **Step 1: Backend compile**

Run:

```powershell
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-admin -am compile -Plocal -DskipTests
```

Expected: `BUILD SUCCESS`.

- [x] **Step 2: Frontend build**

Run:

```powershell
pnpm --dir admin-ui build:prod
```

Expected: exit code `0`.

- [x] **Step 3: Repackage backend**

Stop port `8080` first, then run:

```powershell
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-admin -am package -Plocal -DskipTests
```

Expected: `BUILD SUCCESS`.

- [x] **Step 4: Restart backend**

Run:

```powershell
Start-Process -FilePath 'C:\Program Files\Java\jdk-17.0.9+8\bin\java.exe' -ArgumentList @('-jar','gameluck-admin\target\gameluck-admin.jar','--spring.profiles.active=local') -WorkingDirectory 'C:\codex\project\backend' -WindowStyle Hidden
```

- [x] **Step 5: Verify ports**

Run:

```powershell
Get-NetTCPConnection -State Listen | Where-Object { $_.LocalPort -in 8080,5173,9090 } | Select-Object LocalAddress,LocalPort,OwningProcess
```

Expected: `8080`, `5173`, and `9090` are listening.

---

## Self-Review

Spec coverage:

- GC / SC / RC source-driven configuration: Task 1 seeds and Task 2 rule model.
- SC game-profit-only exchange behavior: Task 1 `SC + GAME_PROFIT` default exchange enabled.
- RC configurable withdrawal / turnover behavior: Task 1 `RC + DEPOSIT` and `RC + MANUAL_ADJUST`, editable in Task 4.
- Avoid putting multiplier calculation inside wallet: Rule Semantics boundary and Task 3 only consumes final turnover value.
- Admin control: Task 4 page and Task 1 menu permissions.
- Wallet core preservation: Task 3 explicitly keeps accounting behavior unchanged.
