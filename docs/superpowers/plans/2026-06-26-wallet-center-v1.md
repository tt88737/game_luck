# Wallet Center v1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the first wallet-center backend and admin UI foundation for GC / SC / RC multi-currency accounts, ledger records, release records, and freeze records.

**Architecture:** Add a new `gameluck-wallet` module under `backend/gameluck-modules`, expose it through `gameluck-admin`, and keep wallet logic separate from system framework code. Wallet v1 stores accounting facts and release state only; business modules pass `releaseMode` and `requiredTurnover` into wallet operations.

**Tech Stack:** Java 17, Spring Boot, MyBatis-Plus, MySQL, RuoYi-Vue-Plus conventions, Vue 3, Element Plus, TypeScript.

---

## File Structure

Create backend module:

- `backend/gameluck-modules/gameluck-wallet/pom.xml`: wallet module dependencies.
- `backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/domain/*`: MyBatis entities.
- `backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/domain/bo/*`: request/query BOs.
- `backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/domain/vo/*`: response VOs.
- `backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/enums/*`: wallet enum definitions.
- `backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/mapper/*Mapper.java`: mapper interfaces.
- `backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/service/*`: service contracts.
- `backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/service/impl/*`: service implementations.
- `backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/controller/*`: admin query/config controllers.
- `backend/gameluck-modules/gameluck-wallet/src/main/resources/mapper/wallet/*Mapper.xml`: mapper XML placeholders.

Modify backend:

- `backend/gameluck-modules/pom.xml`: add `gameluck-wallet` module.
- `backend/gameluck-admin/pom.xml`: add `gameluck-wallet` dependency.
- `backend/script/sql/gameluck_wallet.sql`: wallet schema and seed data.

Create frontend:

- `admin-ui/src/api/wallet/currency/index.ts`
- `admin-ui/src/api/wallet/currency/types.ts`
- `admin-ui/src/api/wallet/account/index.ts`
- `admin-ui/src/api/wallet/account/types.ts`
- `admin-ui/src/api/wallet/transaction/index.ts`
- `admin-ui/src/api/wallet/transaction/types.ts`
- `admin-ui/src/api/wallet/release/index.ts`
- `admin-ui/src/api/wallet/release/types.ts`
- `admin-ui/src/api/wallet/freeze/index.ts`
- `admin-ui/src/api/wallet/freeze/types.ts`
- `admin-ui/src/views/wallet/currency/index.vue`
- `admin-ui/src/views/wallet/account/index.vue`
- `admin-ui/src/views/wallet/transaction/index.vue`
- `admin-ui/src/views/wallet/release/index.vue`
- `admin-ui/src/views/wallet/freeze/index.vue`

---

## Task 1: Database Schema And Menu Seed

**Files:**
- Create: `backend/script/sql/gameluck_wallet.sql`

- [x] **Step 1: Create wallet SQL script**

Create `backend/script/sql/gameluck_wallet.sql` with:

```sql
CREATE TABLE IF NOT EXISTS gl_wallet_currency (
  id BIGINT NOT NULL COMMENT '主键',
  tenant_id VARCHAR(20) NOT NULL DEFAULT '000000' COMMENT '租户编号',
  currency_code VARCHAR(32) NOT NULL COMMENT '币种编码',
  currency_name VARCHAR(64) NOT NULL COMMENT '币种名称',
  scale_num TINYINT NOT NULL DEFAULT 6 COMMENT '小数位',
  enabled CHAR(1) NOT NULL DEFAULT '0' COMMENT '启用状态 0启用 1停用',
  credit_enabled CHAR(1) NOT NULL DEFAULT '0' COMMENT '允许入账 0允许 1禁止',
  debit_enabled CHAR(1) NOT NULL DEFAULT '0' COMMENT '允许扣账 0允许 1禁止',
  freeze_enabled CHAR(1) NOT NULL DEFAULT '0' COMMENT '允许冻结 0允许 1禁止',
  withdraw_enabled CHAR(1) NOT NULL DEFAULT '1' COMMENT '具备提现能力 0是 1否',
  exchange_enabled CHAR(1) NOT NULL DEFAULT '1' COMMENT '具备兑换能力 0是 1否',
  negative_allowed CHAR(1) NOT NULL DEFAULT '1' COMMENT '允许负数 0允许 1禁止',
  sort_order INT NOT NULL DEFAULT 0 COMMENT '排序',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  create_dept BIGINT DEFAULT NULL COMMENT '创建部门',
  create_by BIGINT DEFAULT NULL COMMENT '创建者',
  create_time DATETIME DEFAULT NULL COMMENT '创建时间',
  update_by BIGINT DEFAULT NULL COMMENT '更新者',
  update_time DATETIME DEFAULT NULL COMMENT '更新时间',
  version INT NOT NULL DEFAULT 0 COMMENT '乐观锁',
  del_flag CHAR(1) NOT NULL DEFAULT '0' COMMENT '删除标志 0存在 1删除',
  PRIMARY KEY (id),
  UNIQUE KEY uk_gl_wallet_currency_01 (tenant_id, currency_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='钱包币种配置';

CREATE TABLE IF NOT EXISTS gl_wallet_account (
  id BIGINT NOT NULL COMMENT '主键',
  tenant_id VARCHAR(20) NOT NULL COMMENT '租户编号',
  member_id BIGINT NOT NULL COMMENT '会员ID',
  currency_code VARCHAR(32) NOT NULL COMMENT '币种编码',
  available_balance DECIMAL(20,6) NOT NULL DEFAULT 0 COMMENT '可用余额',
  frozen_balance DECIMAL(20,6) NOT NULL DEFAULT 0 COMMENT '冻结余额',
  status CHAR(1) NOT NULL DEFAULT '0' COMMENT '账户状态 0正常 1冻结 2禁用',
  create_dept BIGINT DEFAULT NULL,
  create_by BIGINT DEFAULT NULL,
  create_time DATETIME DEFAULT NULL,
  update_by BIGINT DEFAULT NULL,
  update_time DATETIME DEFAULT NULL,
  version INT NOT NULL DEFAULT 0,
  del_flag CHAR(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (id),
  UNIQUE KEY uk_gl_wallet_account_01 (tenant_id, member_id, currency_code),
  KEY idx_gl_wallet_account_01 (tenant_id, member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员钱包账户';

CREATE TABLE IF NOT EXISTS gl_wallet_transaction (
  id BIGINT NOT NULL COMMENT '主键',
  tenant_id VARCHAR(20) NOT NULL COMMENT '租户编号',
  transaction_no VARCHAR(64) NOT NULL COMMENT '钱包交易号',
  idempotency_key VARCHAR(128) NOT NULL COMMENT '幂等键',
  member_id BIGINT NOT NULL COMMENT '会员ID',
  currency_code VARCHAR(32) NOT NULL COMMENT '币种编码',
  operation VARCHAR(32) NOT NULL COMMENT '操作类型',
  source_type VARCHAR(64) NOT NULL COMMENT '来源类型',
  business_no VARCHAR(128) NOT NULL COMMENT '业务单号',
  amount DECIMAL(20,6) NOT NULL COMMENT '金额',
  balance_before DECIMAL(20,6) NOT NULL COMMENT '变更前可用余额',
  balance_after DECIMAL(20,6) NOT NULL COMMENT '变更后可用余额',
  frozen_before DECIMAL(20,6) NOT NULL COMMENT '变更前冻结余额',
  frozen_after DECIMAL(20,6) NOT NULL COMMENT '变更后冻结余额',
  release_mode VARCHAR(32) DEFAULT NULL COMMENT '释放模式',
  required_turnover DECIMAL(20,6) DEFAULT NULL COMMENT '所需流水',
  request_hash VARCHAR(128) NOT NULL COMMENT '请求参数哈希',
  status VARCHAR(32) NOT NULL COMMENT '交易状态',
  fail_code VARCHAR(64) DEFAULT NULL COMMENT '失败编码',
  fail_reason VARCHAR(500) DEFAULT NULL COMMENT '失败原因',
  create_time DATETIME DEFAULT NULL,
  update_time DATETIME DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_gl_wallet_transaction_01 (tenant_id, transaction_no),
  UNIQUE KEY uk_gl_wallet_transaction_02 (tenant_id, idempotency_key),
  KEY idx_gl_wallet_transaction_01 (tenant_id, member_id, currency_code),
  KEY idx_gl_wallet_transaction_02 (tenant_id, business_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='钱包账变流水';

CREATE TABLE IF NOT EXISTS gl_wallet_release (
  id BIGINT NOT NULL COMMENT '主键',
  tenant_id VARCHAR(20) NOT NULL COMMENT '租户编号',
  release_no VARCHAR(64) NOT NULL COMMENT '释放记录号',
  member_id BIGINT NOT NULL COMMENT '会员ID',
  currency_code VARCHAR(32) NOT NULL COMMENT '币种编码',
  source_type VARCHAR(64) NOT NULL COMMENT '来源类型',
  business_no VARCHAR(128) NOT NULL COMMENT '业务单号',
  amount DECIMAL(20,6) NOT NULL COMMENT '入账金额',
  released_amount DECIMAL(20,6) NOT NULL DEFAULT 0 COMMENT '已释放金额',
  consumed_amount DECIMAL(20,6) NOT NULL DEFAULT 0 COMMENT '已消费释放金额',
  required_turnover DECIMAL(20,6) NOT NULL DEFAULT 0 COMMENT '所需流水',
  completed_turnover DECIMAL(20,6) NOT NULL DEFAULT 0 COMMENT '完成流水',
  release_mode VARCHAR(32) NOT NULL COMMENT '释放模式',
  release_status VARCHAR(32) NOT NULL COMMENT '释放状态',
  metadata JSON DEFAULT NULL COMMENT '扩展信息',
  create_time DATETIME DEFAULT NULL,
  update_time DATETIME DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_gl_wallet_release_01 (tenant_id, release_no),
  KEY idx_gl_wallet_release_01 (tenant_id, member_id, currency_code, release_status),
  KEY idx_gl_wallet_release_02 (tenant_id, business_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='钱包释放记录';

CREATE TABLE IF NOT EXISTS gl_wallet_freeze (
  id BIGINT NOT NULL COMMENT '主键',
  tenant_id VARCHAR(20) NOT NULL COMMENT '租户编号',
  freeze_no VARCHAR(64) NOT NULL COMMENT '冻结单号',
  member_id BIGINT NOT NULL COMMENT '会员ID',
  currency_code VARCHAR(32) NOT NULL COMMENT '币种编码',
  amount DECIMAL(20,6) NOT NULL COMMENT '冻结金额',
  source_type VARCHAR(64) NOT NULL COMMENT '来源类型',
  business_no VARCHAR(128) NOT NULL COMMENT '业务单号',
  status VARCHAR(32) NOT NULL COMMENT '冻结状态',
  create_time DATETIME DEFAULT NULL,
  update_time DATETIME DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_gl_wallet_freeze_01 (tenant_id, freeze_no),
  KEY idx_gl_wallet_freeze_01 (tenant_id, member_id, currency_code),
  KEY idx_gl_wallet_freeze_02 (tenant_id, business_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='钱包冻结记录';

INSERT INTO gl_wallet_currency
(id, tenant_id, currency_code, currency_name, scale_num, enabled, credit_enabled, debit_enabled, freeze_enabled, withdraw_enabled, exchange_enabled, negative_allowed, sort_order, remark, create_time)
VALUES
(1900000000000000001, '000000', 'GC', 'Gold Coin', 6, '0', '0', '0', '0', '1', '1', '1', 1, '金币，默认不可提现', NOW()),
(1900000000000000002, '000000', 'SC', 'Sweep Coin', 6, '0', '0', '0', '0', '1', '0', '1', 2, '奖励币，默认具备兑换能力', NOW()),
(1900000000000000003, '000000', 'RC', 'Real Cash', 6, '0', '0', '0', '0', '0', '1', '1', 3, '真金，默认具备提现能力', NOW())
ON DUPLICATE KEY UPDATE currency_name = VALUES(currency_name), update_time = NOW();
```

- [x] **Step 2: Apply SQL locally**

Run:

```powershell
mysql -uroot -proot gameluck_vue < backend\script\sql\gameluck_wallet.sql
```

Expected: command exits with status `0`, and `SHOW TABLES LIKE 'gl_wallet_%';` returns five wallet tables.

- [x] **Step 3: Commit database script**

```powershell
git add backend\script\sql\gameluck_wallet.sql
git commit -m "feat(wallet): add wallet database schema"
```

---

## Task 2: Backend Module Skeleton

**Files:**
- Create: `backend/gameluck-modules/gameluck-wallet/pom.xml`
- Modify: `backend/gameluck-modules/pom.xml`
- Modify: `backend/gameluck-admin/pom.xml`

- [x] **Step 1: Add `gameluck-wallet` Maven module**

Create `backend/gameluck-modules/gameluck-wallet/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <groupId>com.gameluck</groupId>
        <artifactId>gameluck-modules</artifactId>
        <version>${revision}</version>
    </parent>
    <modelVersion>4.0.0</modelVersion>
    <artifactId>gameluck-wallet</artifactId>
    <description>gameluck-wallet 钱包中心</description>

    <dependencies>
        <dependency>
            <groupId>com.gameluck</groupId>
            <artifactId>gameluck-common-core</artifactId>
        </dependency>
        <dependency>
            <groupId>com.gameluck</groupId>
            <artifactId>gameluck-common-mybatis</artifactId>
        </dependency>
        <dependency>
            <groupId>com.gameluck</groupId>
            <artifactId>gameluck-common-log</artifactId>
        </dependency>
        <dependency>
            <groupId>com.gameluck</groupId>
            <artifactId>gameluck-common-web</artifactId>
        </dependency>
        <dependency>
            <groupId>com.gameluck</groupId>
            <artifactId>gameluck-common-tenant</artifactId>
        </dependency>
        <dependency>
            <groupId>com.gameluck</groupId>
            <artifactId>gameluck-common-security</artifactId>
        </dependency>
        <dependency>
            <groupId>com.gameluck</groupId>
            <artifactId>gameluck-common-excel</artifactId>
        </dependency>
    </dependencies>
</project>
```

- [x] **Step 2: Register module in parent POM**

Modify `backend/gameluck-modules/pom.xml`:

```xml
<modules>
    <module>gameluck-generator</module>
    <module>gameluck-job</module>
    <module>gameluck-system</module>
    <module>gameluck-wallet</module>
</modules>
```

- [x] **Step 3: Add admin dependency**

Modify `backend/gameluck-admin/pom.xml`, after `gameluck-job`:

```xml
<dependency>
    <groupId>com.gameluck</groupId>
    <artifactId>gameluck-wallet</artifactId>
</dependency>
```

- [x] **Step 4: Verify module compiles**

Run:

```powershell
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-modules/gameluck-wallet -am compile -Plocal -DskipTests
```

Expected: `BUILD SUCCESS`.

- [x] **Step 5: Commit module skeleton**

```powershell
git add backend\gameluck-modules\pom.xml backend\gameluck-admin\pom.xml backend\gameluck-modules\gameluck-wallet\pom.xml
git commit -m "feat(wallet): add wallet backend module"
```

---

## Task 3: Domain Models, Enums, And Mappers

**Files:**
- Create: wallet entity, BO, VO, enum, mapper files.

- [x] **Step 1: Create enums**

Create:

```text
backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/enums/WalletOperation.java
backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/enums/WalletReleaseMode.java
backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/enums/WalletReleaseStatus.java
backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/enums/WalletTransactionStatus.java
backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/enums/WalletFreezeStatus.java
```

Use values:

```java
public enum WalletOperation { CREDIT, DEBIT, FREEZE, UNFREEZE, SETTLE, ADJUST, REVERSE }
public enum WalletReleaseMode { IMMEDIATE, AFTER_TURNOVER, NEVER, MANUAL_REVIEW }
public enum WalletReleaseStatus { RELEASED, LOCKED, NEVER, REVIEWING, REJECTED, CONSUMED }
public enum WalletTransactionStatus { PENDING, SUCCESS, FAILED, REVERSED }
public enum WalletFreezeStatus { FROZEN, SETTLED, RELEASED }
```

- [x] **Step 2: Create entity classes**

Create entities named:

```text
WalletCurrency
WalletAccount
WalletTransaction
WalletRelease
WalletFreeze
```

Each entity:

- package: `com.gameluck.wallet.domain`
- annotation: `@TableName("gl_wallet_*")`
- extends `BaseEntity` when the table has audit columns.
- uses `BigDecimal` for all money fields.
- uses `@TableId(value = "id")`.
- uses `@TableLogic` on `delFlag` for currency/account.

- [x] **Step 3: Create BO and VO classes**

For each entity create:

```text
WalletCurrencyBo / WalletCurrencyVo
WalletAccountBo / WalletAccountVo
WalletTransactionBo / WalletTransactionVo
WalletReleaseBo / WalletReleaseVo
WalletFreezeBo / WalletFreezeVo
```

BOs contain query fields used by list pages. VOs contain fields displayed by admin pages.

- [x] **Step 4: Create mapper interfaces**

Create:

```java
public interface WalletCurrencyMapper extends BaseMapperPlus<WalletCurrency, WalletCurrencyVo> {}
public interface WalletAccountMapper extends BaseMapperPlus<WalletAccount, WalletAccountVo> {}
public interface WalletTransactionMapper extends BaseMapperPlus<WalletTransaction, WalletTransactionVo> {}
public interface WalletReleaseMapper extends BaseMapperPlus<WalletRelease, WalletReleaseVo> {}
public interface WalletFreezeMapper extends BaseMapperPlus<WalletFreeze, WalletFreezeVo> {}
```

- [x] **Step 5: Create mapper XML placeholders**

Create XML files under `src/main/resources/mapper/wallet` with:

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
    PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
    "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.gameluck.wallet.mapper.WalletCurrencyMapper">
</mapper>
```

Repeat for each mapper namespace.

- [x] **Step 6: Compile**

Run:

```powershell
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-modules/gameluck-wallet -am compile -Plocal -DskipTests
```

Expected: `BUILD SUCCESS`.

- [x] **Step 7: Commit model layer**

```powershell
git add backend\gameluck-modules\gameluck-wallet
git commit -m "feat(wallet): add wallet domain models"
```

---

## Task 4: Query Services And Admin Controllers

**Files:**
- Create service interfaces and implementations.
- Create admin controllers.

- [x] **Step 1: Create service interfaces**

Create:

```text
IWalletCurrencyService
IWalletAccountService
IWalletTransactionService
IWalletReleaseService
IWalletFreezeService
```

Each interface exposes:

```java
TableDataInfo<WalletCurrencyVo> queryPageList(WalletCurrencyBo bo, PageQuery pageQuery);
WalletCurrencyVo queryById(Long id);
List<WalletCurrencyVo> queryList(WalletCurrencyBo bo);
```

For `WalletCurrency`, also expose:

```java
Boolean updateByBo(WalletCurrencyBo bo);
```

- [x] **Step 2: Create service implementations**

Implement query wrappers with MyBatis-Plus:

```java
LambdaQueryWrapper<WalletCurrency> lqw = Wrappers.lambdaQuery();
lqw.eq(StringUtils.isNotBlank(bo.getCurrencyCode()), WalletCurrency::getCurrencyCode, bo.getCurrencyCode());
lqw.like(StringUtils.isNotBlank(bo.getCurrencyName()), WalletCurrency::getCurrencyName, bo.getCurrencyName());
lqw.eq(StringUtils.isNotBlank(bo.getEnabled()), WalletCurrency::getEnabled, bo.getEnabled());
lqw.orderByAsc(WalletCurrency::getSortOrder);
```

Use `baseMapper.selectVoPage(pageQuery.build(), lqw)` and `TableDataInfo.build(page)`.

- [x] **Step 3: Create controllers**

Create controllers:

```text
WalletCurrencyController -> /wallet/currency
WalletAccountController -> /wallet/account
WalletTransactionController -> /wallet/transaction
WalletReleaseController -> /wallet/release
WalletFreezeController -> /wallet/freeze
```

Permissions:

```text
wallet:currency:list
wallet:currency:query
wallet:currency:edit
wallet:account:list
wallet:account:query
wallet:transaction:list
wallet:transaction:query
wallet:release:list
wallet:freeze:list
```

Only `WalletCurrencyController` has `PUT /wallet/currency` in v1. No manual balance adjustment endpoints.

- [x] **Step 4: Compile admin**

Run:

```powershell
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-admin -am compile -Plocal -DskipTests
```

Expected: `BUILD SUCCESS`.

- [x] **Step 5: Commit query API layer**

```powershell
git add backend\gameluck-modules\gameluck-wallet
git commit -m "feat(wallet): add wallet admin query APIs"
```

---

## Task 5: Wallet Core Service v1

**Files:**
- Create: `WalletCreditBo`
- Create: `WalletDebitBo`
- Create: `WalletTurnoverBo`
- Create: `IWalletCoreService`
- Create: `WalletCoreServiceImpl`
- Create: `WalletCoreController`

- [ ] **Step 1: Create operation request BOs**

Create:

```java
public class WalletCreditBo {
    @NotNull private Long memberId;
    @NotBlank private String currencyCode;
    @NotNull @DecimalMin("0.000001") private BigDecimal amount;
    @NotBlank private String sourceType;
    @NotBlank private String businessNo;
    @NotBlank private String idempotencyKey;
    @NotBlank private String releaseMode;
    @NotNull @DecimalMin("0") private BigDecimal requiredTurnover;
}
```

Create similar `WalletDebitBo` with no release fields. Create `WalletTurnoverBo` with `validTurnoverAmount`.

- [ ] **Step 2: Create core service contract**

`IWalletCoreService`:

```java
WalletTransactionVo credit(WalletCreditBo bo);
WalletTransactionVo debit(WalletDebitBo bo);
void addValidTurnover(WalletTurnoverBo bo);
```

- [ ] **Step 3: Implement `credit`**

Minimum behavior:

1. Check `tenant_id + idempotency_key`.
2. Create account if missing.
3. Lock account row for update using mapper method.
4. Increase available balance.
5. Insert transaction.
6. Insert release record based on `releaseMode`.

`IMMEDIATE` creates `RELEASED`; `AFTER_TURNOVER` creates `LOCKED`; `NEVER` creates `NEVER`; `MANUAL_REVIEW` creates `REVIEWING`.

- [ ] **Step 4: Implement `debit`**

Minimum behavior:

1. Check idempotency.
2. Lock account row.
3. Reject if available balance is insufficient.
4. Decrease available balance.
5. Insert transaction.

- [ ] **Step 5: Implement `addValidTurnover`**

Minimum behavior:

1. Find `LOCKED` release records for member/currency ordered by create time.
2. Apply `validTurnoverAmount` until exhausted.
3. When `completed_turnover >= required_turnover`, set status to `RELEASED` and `released_amount = amount`.

- [ ] **Step 6: Add internal test controller**

Create `/wallet/core/credit`, `/wallet/core/debit`, `/wallet/core/turnover` with `@SaCheckPermission("wallet:core:test")`. This is temporary for local verification and must be reviewed before production.

- [ ] **Step 7: Compile**

Run:

```powershell
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-admin -am compile -Plocal -DskipTests
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 8: Commit core service**

```powershell
git add backend\gameluck-modules\gameluck-wallet
git commit -m "feat(wallet): add wallet core operations"
```

---

## Task 6: Admin UI Pages

**Files:**
- Create wallet API files and Vue pages listed in File Structure.

- [ ] **Step 1: Create TypeScript API wrappers**

Example `admin-ui/src/api/wallet/currency/index.ts`:

```ts
import request from '@/utils/request';
import { CurrencyForm, CurrencyQuery, CurrencyVO } from './types';
import { AxiosPromise } from 'axios';

export function listCurrency(query: CurrencyQuery): AxiosPromise<CurrencyVO[]> {
  return request({ url: '/wallet/currency/list', method: 'get', params: query });
}

export function getCurrency(id: string | number): AxiosPromise<CurrencyVO> {
  return request({ url: '/wallet/currency/' + id, method: 'get' });
}

export function updateCurrency(data: CurrencyForm) {
  return request({ url: '/wallet/currency', method: 'put', data });
}
```

- [ ] **Step 2: Create currency page**

Create `admin-ui/src/views/wallet/currency/index.vue` with:

- search by `currencyCode`, `currencyName`, `enabled`.
- table columns for code, name, scale, enabled, credit/debit/freeze/withdraw/exchange switches.
- edit dialog for capability switches only.
- no delete button.

- [ ] **Step 3: Create account and ledger query pages**

Create:

```text
wallet/account
wallet/transaction
wallet/release
wallet/freeze
```

All are query-only pages with search form, table, pagination, and no balance-changing buttons.

- [ ] **Step 4: Build frontend**

Run:

```powershell
pnpm --dir admin-ui build:prod
```

Expected: build exits with status `0`.

- [ ] **Step 5: Commit frontend**

```powershell
git add admin-ui\src\api\wallet admin-ui\src\views\wallet
git commit -m "feat(wallet): add wallet admin pages"
```

---

## Task 7: Verification And Handoff

**Files:**
- Modify if needed: `README.md`

- [ ] **Step 1: Backend compile**

Run:

```powershell
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-admin -am compile -Plocal -DskipTests
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 2: Frontend build**

Run:

```powershell
pnpm --dir admin-ui build:prod
```

Expected: build exits with status `0`.

- [ ] **Step 3: Local smoke test**

Start backend and visit:

```text
GET /wallet/currency/list
GET /wallet/account/list
GET /wallet/transaction/list
GET /wallet/release/list
GET /wallet/freeze/list
```

Expected: each endpoint returns a normal `R`/table response after login.

- [ ] **Step 4: Document startup and SQL**

If `README.md` does not mention wallet SQL, add:

```text
Wallet Center:
- Run backend/script/sql/gameluck_wallet.sql on gameluck_vue.
- Default currencies: GC, SC, RC.
- Wallet v1 admin pages are query/config only. Manual balance adjustment is intentionally not enabled.
```

- [ ] **Step 5: Final commit**

```powershell
git status --short
git add README.md
git commit -m "docs(wallet): document wallet setup"
```

Skip the commit if `README.md` was not changed.

---

## Self-Review

Spec coverage:

- Multi-currency GC / SC / RC: Task 1 seed data and Task 3 models.
- Business rules outside wallet: Task 5 accepts `releaseMode` and `requiredTurnover`; no `turnover_multiplier` in wallet config.
- Wallet facts and balance control: Tasks 1, 3, 5.
- Admin query pages: Tasks 4 and 6.
- No high-risk manual adjustment in v1: Tasks 4 and 6 explicitly exclude it.

Known follow-up after v1:

- Replace temporary `/wallet/core/*` test endpoints with internal service calls once payment/game/promotion modules exist.
- Add formal integration tests after the first member module exists.
- Add menu SQL once final backend permissions and route names are confirmed in the running admin database.
