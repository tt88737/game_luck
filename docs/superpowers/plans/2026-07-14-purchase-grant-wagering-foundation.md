# Purchase Grant Wagering Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a clean purchase/grant/wagering foundation so normal purchase, first purchase, purchase campaigns, discount offers, recall offers, GC grants, SC bonuses, and future real-money deposits share the same extensible model.

**Architecture:** Keep wallet as the ledger and wagering executor. Add a purchase domain that owns offer configuration, grant item snapshots, and order fulfillment. Stop expanding the generic promotion reward page into purchase/deposit logic; promotion remains for non-payment rewards such as daily login and generic grants.

**Tech Stack:** Java 17, Spring Boot, MyBatis Plus, MySQL, Vue 3, Vite, Element Plus, Vitest, Maven.

---

## File Structure

### Existing Files To Modify

- `backend/script/sql/gameluck_wallet.sql`
  - Add purchase offer, grant item, purchase order, and order grant snapshot tables.
  - Add menu seeds for `支付/购买中心 -> 购买产品`.
  - Add fund property seeds for purchase grants and purchase bonuses.
- `backend/script/sql/gameluck_platform_dict.sql`
  - Add purchase offer type/status/grant type/wagering mode dict values if dict-driven UI is used.
- `backend/gameluck-modules/gameluck-promotion/src/main/java/com/gameluck/promotion/...`
  - Stop using promotion reward as purchase/deposit configuration. Keep daily login and generic reward only.
- `admin-ui/src/views/promotion/reward/index.vue`
  - Remove visible fund-property display from operator form.
  - Change default add activity type to `GENERAL`.
  - Add operator-facing warning that purchase/recharge offers are configured in purchase center, not this page.
- `admin-ui/src/lang/zh_CN.ts`
  - Add menu/page wording only if existing `tt()` helper cannot cover it.
- `admin-ui/src/lang/en_US.ts`
  - Add English wording only if existing `tt()` helper cannot cover it.

### New Backend Files

- `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/PurchaseOffer.java`
  - Purchase offer aggregate root.
- `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/PurchaseOfferGrantItem.java`
  - Configured grant item per offer.
- `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/PurchaseOrder.java`
  - Member purchase order.
- `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/PurchaseOrderGrantSnapshot.java`
  - Immutable grant/wagering snapshot generated at order success.
- `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/bo/PurchaseOfferBo.java`
- `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/bo/PurchaseOfferGrantItemBo.java`
- `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/vo/PurchaseOfferVo.java`
- `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/domain/vo/PurchaseOfferGrantItemVo.java`
- `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/mapper/PurchaseOfferMapper.java`
- `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/mapper/PurchaseOfferGrantItemMapper.java`
- `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/IPurchaseOfferService.java`
- `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/service/impl/PurchaseOfferServiceImpl.java`
- `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/controller/PurchaseOfferController.java`
- `backend/gameluck-modules/gameluck-payment/src/test/java/com/gameluck/payment/service/impl/PurchaseOfferServiceImplTest.java`

### New Frontend Files

- `admin-ui/src/api/payment/purchaseOffer/index.ts`
- `admin-ui/src/api/payment/purchaseOffer/types.ts`
- `admin-ui/src/views/payment/purchase-offer/index.vue`

---

## Task 1: Stop Promotion Reward From Carrying Purchase Logic

**Files:**
- Modify: `admin-ui/src/views/promotion/reward/index.vue`
- Test: `admin-ui` build and i18n guard

- [ ] **Step 1: Write the expected UI behavior**

Promotion reward form must satisfy:

```text
1. Add dialog defaults to 活动类型 = 普通奖励 / GENERAL.
2. Operator form does not show 资金属性.
3. Operator sees short warning:
   充值、首充、折扣、召回等购买类活动，请在支付/购买中心配置。
4. Reward item fields remain:
   币种、奖励金额、流水要求、固定流水金额/流水倍数、有效天数、游戏范围。
```

- [ ] **Step 2: Modify the initial form default**

In `admin-ui/src/views/promotion/reward/index.vue`, keep:

```ts
const initFormData: PromotionRewardForm = {
  promotionType: 'GENERAL',
  currencyCode: 'SC',
  claimCycle: 'ONCE',
  status: 'INACTIVE'
};
```

Verify no later watcher changes add dialog to `DAILY_LOGIN`.

- [ ] **Step 3: Remove visible fund-property block**

Delete this block from the reward item grid:

```vue
<div class="reward-field reward-field-auto">
  <span class="reward-field-label">{{ tt('资金属性') }}</span>
  <div class="reward-auto-property">
    <span>{{ fundPropertyText(item.fundPropertyCode) }}</span>
    <small>{{ tt('系统自动匹配') }}</small>
  </div>
</div>
```

Keep `fundPropertyCode` in payload normalization because backend still needs a system snapshot:

```ts
fundPropertyCode: item.fundPropertyCode || defaultFundPropertyCode(promotionType),
```

- [ ] **Step 4: Replace the warning copy**

Change alert title to:

```vue
:title="tt('配置奖励金额和流水要求。充值、首充、折扣、召回等购买类活动，请在支付/购买中心配置。')"
```

- [ ] **Step 5: Remove unused visible-property helper code**

If no other references remain, delete:

```ts
const fundPropertyText = (propertyCode?: string) => {
  const code = propertyCode || defaultFundPropertyCode(form.value.promotionType);
  const template = fundPropertyList.value.find((item) => item.propertyCode === code);
  if (template?.propertyName) return template.propertyName;
  if (code === 'DAILY_REWARD') return tt('每日奖励');
  if (code === 'ACTIVITY_REWARD') return tt('活动奖励');
  return code;
};
```

Also delete `.reward-field-auto` and `.reward-auto-property` CSS if unused.

- [ ] **Step 6: Run frontend verification**

Run:

```powershell
pnpm --dir admin-ui build:dev
```

Expected:

```text
Menu icon check passed.
i18n check passed.
✓ built
```

- [ ] **Step 7: Commit**

```powershell
git add admin-ui/src/views/promotion/reward/index.vue
git commit -m "fix: keep purchase logic out of promotion rewards"
```

---

## Task 2: Add Purchase Foundation Tables And Seeds

**Files:**
- Modify: `backend/script/sql/gameluck_wallet.sql`
- Modify: `backend/script/sql/gameluck_platform_dict.sql`
- Test: MySQL schema import/query

- [ ] **Step 1: Add purchase offer table**

Add to `backend/script/sql/gameluck_wallet.sql` after payment/deposit tables:

```sql
CREATE TABLE IF NOT EXISTS gl_purchase_offer (
  id BIGINT NOT NULL COMMENT 'Primary key',
  tenant_id VARCHAR(20) NOT NULL DEFAULT '000000' COMMENT 'Tenant id',
  offer_no VARCHAR(64) NOT NULL COMMENT 'Offer no',
  offer_name VARCHAR(128) NOT NULL COMMENT 'Offer name',
  offer_type VARCHAR(32) NOT NULL DEFAULT 'STANDARD' COMMENT 'STANDARD,FIRST_PURCHASE,CAMPAIGN,DISCOUNT,RECALL',
  pay_currency_code VARCHAR(32) NOT NULL DEFAULT 'USD' COMMENT 'Payment currency',
  pay_amount DECIMAL(20,6) NOT NULL DEFAULT 0 COMMENT 'Payment amount',
  user_scope_type VARCHAR(32) NOT NULL DEFAULT 'ALL' COMMENT 'ALL,NEW_USER,RECALL,TAG',
  user_scope_value VARCHAR(512) DEFAULT NULL COMMENT 'User scope value',
  region_scope_type VARCHAR(32) NOT NULL DEFAULT 'ALL' COMMENT 'ALL,COUNTRY,STATE',
  region_scope_value VARCHAR(512) DEFAULT NULL COMMENT 'Region scope value',
  purchase_limit_type VARCHAR(32) NOT NULL DEFAULT 'NONE' COMMENT 'NONE,FIRST_ONLY,DAILY_ONCE,TOTAL_ONCE,PERIOD_LIMIT',
  stackable CHAR(1) NOT NULL DEFAULT '1' COMMENT 'Stackable: 0 yes, 1 no',
  status CHAR(1) NOT NULL DEFAULT '1' COMMENT 'Status: 0 enabled, 1 disabled',
  sort_order INT NOT NULL DEFAULT 0 COMMENT 'Sort order',
  start_time DATETIME DEFAULT NULL COMMENT 'Start time',
  end_time DATETIME DEFAULT NULL COMMENT 'End time',
  remark VARCHAR(500) DEFAULT NULL COMMENT 'Remark',
  version INT NOT NULL DEFAULT 0 COMMENT 'Version',
  del_flag CHAR(1) NOT NULL DEFAULT '0' COMMENT 'Delete flag',
  create_dept BIGINT DEFAULT NULL COMMENT 'Create department',
  create_by BIGINT DEFAULT NULL COMMENT 'Created by',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  update_by BIGINT DEFAULT NULL COMMENT 'Updated by',
  update_time DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
  PRIMARY KEY (id),
  UNIQUE KEY uk_gl_purchase_offer_01 (tenant_id, offer_no),
  KEY idx_gl_purchase_offer_01 (tenant_id, offer_type, status, sort_order),
  KEY idx_gl_purchase_offer_02 (tenant_id, start_time, end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Purchase offer';
```

- [ ] **Step 2: Add purchase grant item table**

```sql
CREATE TABLE IF NOT EXISTS gl_purchase_offer_grant_item (
  id BIGINT NOT NULL COMMENT 'Primary key',
  tenant_id VARCHAR(20) NOT NULL DEFAULT '000000' COMMENT 'Tenant id',
  offer_id BIGINT NOT NULL COMMENT 'Offer id',
  grant_type VARCHAR(32) NOT NULL COMMENT 'PURCHASE_GRANT,PURCHASE_BONUS,DEPOSIT_PRINCIPAL,DEPOSIT_BONUS',
  currency_code VARCHAR(32) NOT NULL COMMENT 'Grant currency',
  grant_amount DECIMAL(20,6) NOT NULL DEFAULT 0 COMMENT 'Grant amount',
  fund_property_code VARCHAR(64) NOT NULL COMMENT 'System fund property code',
  wagering_mode VARCHAR(32) NOT NULL DEFAULT 'NONE' COMMENT 'NONE,FIXED,MULTIPLIER,COMBINED_MULTIPLIER',
  wagering_required_amount DECIMAL(20,6) NOT NULL DEFAULT 0 COMMENT 'Fixed wagering amount',
  wagering_multiplier DECIMAL(10,4) NOT NULL DEFAULT 0 COMMENT 'Wagering multiplier',
  game_scope_type VARCHAR(32) NOT NULL DEFAULT 'ALL' COMMENT 'ALL,CATEGORY,PROVIDER,GAME',
  game_scope_value VARCHAR(512) DEFAULT NULL COMMENT 'Game scope value',
  wagering_expire_days INT NOT NULL DEFAULT 0 COMMENT 'Wagering expiry days',
  sort_order INT NOT NULL DEFAULT 0 COMMENT 'Sort order',
  remark VARCHAR(500) DEFAULT NULL COMMENT 'Remark',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  update_time DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
  PRIMARY KEY (id),
  KEY idx_gl_purchase_offer_grant_item_01 (tenant_id, offer_id, sort_order),
  KEY idx_gl_purchase_offer_grant_item_02 (tenant_id, currency_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Purchase offer grant item';
```

- [ ] **Step 3: Add purchase order and grant snapshot tables**

```sql
CREATE TABLE IF NOT EXISTS gl_purchase_order (
  id BIGINT NOT NULL COMMENT 'Primary key',
  tenant_id VARCHAR(20) NOT NULL DEFAULT '000000' COMMENT 'Tenant id',
  purchase_order_no VARCHAR(64) NOT NULL COMMENT 'Purchase order no',
  offer_id BIGINT DEFAULT NULL COMMENT 'Offer id',
  offer_no VARCHAR(64) DEFAULT NULL COMMENT 'Offer no snapshot',
  member_id BIGINT NOT NULL COMMENT 'Member id',
  pay_currency_code VARCHAR(32) NOT NULL COMMENT 'Payment currency',
  pay_amount DECIMAL(20,6) NOT NULL DEFAULT 0 COMMENT 'Payment amount',
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING,PAID,CREDITED,FAILED,CANCELLED',
  idempotency_key VARCHAR(128) NOT NULL COMMENT 'Idempotency key',
  fail_reason VARCHAR(500) DEFAULT NULL COMMENT 'Failure reason',
  paid_time DATETIME DEFAULT NULL COMMENT 'Paid time',
  credited_time DATETIME DEFAULT NULL COMMENT 'Credited time',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  update_time DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
  PRIMARY KEY (id),
  UNIQUE KEY uk_gl_purchase_order_01 (tenant_id, purchase_order_no),
  UNIQUE KEY uk_gl_purchase_order_02 (tenant_id, idempotency_key),
  KEY idx_gl_purchase_order_01 (tenant_id, member_id, status, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Purchase order';

CREATE TABLE IF NOT EXISTS gl_purchase_order_grant_snapshot (
  id BIGINT NOT NULL COMMENT 'Primary key',
  tenant_id VARCHAR(20) NOT NULL DEFAULT '000000' COMMENT 'Tenant id',
  purchase_order_id BIGINT NOT NULL COMMENT 'Purchase order id',
  purchase_order_no VARCHAR(64) NOT NULL COMMENT 'Purchase order no',
  member_id BIGINT NOT NULL COMMENT 'Member id',
  grant_type VARCHAR(32) NOT NULL COMMENT 'Grant type',
  currency_code VARCHAR(32) NOT NULL COMMENT 'Grant currency',
  grant_amount DECIMAL(20,6) NOT NULL DEFAULT 0 COMMENT 'Grant amount',
  fund_property_code VARCHAR(64) NOT NULL COMMENT 'Fund property code',
  wallet_transaction_no VARCHAR(64) DEFAULT NULL COMMENT 'Wallet transaction no',
  turnover_task_no VARCHAR(64) DEFAULT NULL COMMENT 'Turnover task no',
  wagering_mode VARCHAR(32) NOT NULL DEFAULT 'NONE' COMMENT 'Wagering mode snapshot',
  required_turnover DECIMAL(20,6) NOT NULL DEFAULT 0 COMMENT 'Required turnover snapshot',
  game_scope_type VARCHAR(32) NOT NULL DEFAULT 'ALL' COMMENT 'Game scope type',
  game_scope_value VARCHAR(512) DEFAULT NULL COMMENT 'Game scope value',
  rule_snapshot JSON DEFAULT NULL COMMENT 'Rule snapshot',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  PRIMARY KEY (id),
  KEY idx_gl_purchase_order_grant_snapshot_01 (tenant_id, purchase_order_no),
  KEY idx_gl_purchase_order_grant_snapshot_02 (tenant_id, member_id, currency_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Purchase order grant snapshot';
```

- [ ] **Step 4: Add fund property seeds**

Extend `gl_wallet_fund_property_template` seeds with:

```sql
(1900000000000001010, '000000', 'PURCHASE_GRANT_GC', '购买获得GC', 'PURCHASE', 'NONE', 0, 0.0000, 'ALL', NULL, '0', 100, '购买产品发放的GC，当前不可提不可兑，默认不需要流水。', NOW()),
(1900000000000001011, '000000', 'PURCHASE_BONUS_SC', '购买赠送SC', 'PURCHASE', 'MULTIPLIER', 0, 10.0000, 'ALL', NULL, '0', 110, '购买产品赠送的SC，默认需要10倍流水。', NOW())
```

- [ ] **Step 5: Add menu seed**

Add under top-level `支付中心` or create `支付/购买中心` after confirming existing menu wording. Minimal first phase can add child under `支付中心`:

```sql
(1910, '购买产品', 1900, 2, 'purchase-offer', 'payment/purchase-offer/index', '', 1, 0, 'C', '0', '0', 'payment:purchaseOffer:list', 'shopping', 103, 1, NOW(), NULL, NULL, '购买产品配置菜单'),
(1916, '购买产品查询', 1910, 1, '#', '', '', 1, 0, 'F', '0', '0', 'payment:purchaseOffer:query', '#', 103, 1, NOW(), NULL, NULL, ''),
(1917, '购买产品新增', 1910, 2, '#', '', '', 1, 0, 'F', '0', '0', 'payment:purchaseOffer:add', '#', 103, 1, NOW(), NULL, NULL, ''),
(1918, '购买产品编辑', 1910, 3, '#', '', '', 1, 0, 'F', '0', '0', 'payment:purchaseOffer:edit', '#', 103, 1, NOW(), NULL, NULL, '')
```

- [ ] **Step 6: Import SQL locally**

Run:

```powershell
.\backend\script\bin\import-sql-utf8.ps1 -SqlPath backend\script\sql\gameluck_wallet.sql
```

Expected:

```text
Import completed
```

- [ ] **Step 7: Verify schema**

Run:

```powershell
@'
SHOW TABLES LIKE 'gl_purchase_%';
SELECT property_code, property_name, default_turnover_mode, default_turnover_multiplier
FROM gl_wallet_fund_property_template
WHERE property_code IN ('PURCHASE_GRANT_GC', 'PURCHASE_BONUS_SC')
ORDER BY property_code;
SELECT menu_name, path, perms FROM sys_menu WHERE menu_id IN (1910,1916,1917,1918) ORDER BY menu_id;
'@ | mysql -uroot -proot gameluck_vue
```

Expected:

```text
gl_purchase_offer
gl_purchase_offer_grant_item
gl_purchase_order
gl_purchase_order_grant_snapshot
PURCHASE_GRANT_GC
PURCHASE_BONUS_SC
购买产品
```

- [ ] **Step 8: Commit**

```powershell
git add backend/script/sql/gameluck_wallet.sql backend/script/sql/gameluck_platform_dict.sql
git commit -m "feat: add purchase grant wagering schema"
```

---

## Task 3: Backend Purchase Offer CRUD

**Files:**
- Create backend payment domain, bo, vo, mapper, service, controller files listed in File Structure.
- Test: `PurchaseOfferServiceImplTest`

- [ ] **Step 1: Write failing service test**

Create `backend/gameluck-modules/gameluck-payment/src/test/java/com/gameluck/payment/service/impl/PurchaseOfferServiceImplTest.java`:

```java
package com.gameluck.payment.service.impl;

import com.gameluck.payment.domain.PurchaseOffer;
import com.gameluck.payment.domain.PurchaseOfferGrantItem;
import com.gameluck.payment.domain.bo.PurchaseOfferBo;
import com.gameluck.payment.mapper.PurchaseOfferGrantItemMapper;
import com.gameluck.payment.mapper.PurchaseOfferMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PurchaseOfferServiceImplTest {

    @Test
    void insertStandardGcScOfferStoresGrantItemsWithSystemFundProperties() {
        PurchaseOfferMapper offerMapper = mock(PurchaseOfferMapper.class);
        PurchaseOfferGrantItemMapper grantItemMapper = mock(PurchaseOfferGrantItemMapper.class);
        when(offerMapper.insert(any(PurchaseOffer.class))).thenReturn(1);
        when(grantItemMapper.insert(any(PurchaseOfferGrantItem.class))).thenReturn(1);
        PurchaseOfferServiceImpl service = new PurchaseOfferServiceImpl(offerMapper, grantItemMapper);

        PurchaseOfferBo bo = new PurchaseOfferBo();
        bo.setOfferNo("PO-STARTER-10");
        bo.setOfferName("Starter Pack");
        bo.setOfferType("STANDARD");
        bo.setPayCurrencyCode("USD");
        bo.setPayAmount(new BigDecimal("10.000000"));
        bo.setStatus("1");
        bo.setGrantItems(List.of(
            PurchaseOfferBo.grantItem("PURCHASE_GRANT", "GC", new BigDecimal("10000.000000"), "NONE", BigDecimal.ZERO),
            PurchaseOfferBo.grantItem("PURCHASE_BONUS", "SC", new BigDecimal("1.000000"), "MULTIPLIER", new BigDecimal("10"))
        ));

        int rows = service.insertByBo(bo);

        assertEquals(1, rows);
        ArgumentCaptor<PurchaseOfferGrantItem> captor = ArgumentCaptor.forClass(PurchaseOfferGrantItem.class);
        verify(grantItemMapper, times(2)).insert(captor.capture());
        List<PurchaseOfferGrantItem> items = captor.getAllValues();
        assertEquals("PURCHASE_GRANT_GC", items.get(0).getFundPropertyCode());
        assertEquals("NONE", items.get(0).getWageringMode());
        assertEquals("PURCHASE_BONUS_SC", items.get(1).getFundPropertyCode());
        assertEquals("MULTIPLIER", items.get(1).getWageringMode());
        assertEquals(new BigDecimal("10"), items.get(1).getWageringMultiplier());
    }
}
```

- [ ] **Step 2: Run test to verify RED**

Run:

```powershell
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-modules/gameluck-payment -am -DskipTests=false "-Dprofiles.active=local" "-Dtest=PurchaseOfferServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected:

```text
Compilation failure: cannot find symbol PurchaseOffer...
```

- [ ] **Step 3: Implement minimal domain classes**

Create fields matching SQL names and local project Lombok patterns:

```java
@Data
@TableName("gl_purchase_offer")
public class PurchaseOffer extends TenantEntity {
    @TableId(value = "id")
    private Long id;
    private String offerNo;
    private String offerName;
    private String offerType;
    private String payCurrencyCode;
    private BigDecimal payAmount;
    private String userScopeType;
    private String userScopeValue;
    private String regionScopeType;
    private String regionScopeValue;
    private String purchaseLimitType;
    private String stackable;
    private String status;
    private Integer sortOrder;
    private Date startTime;
    private Date endTime;
    private String remark;
}
```

For grant item:

```java
@Data
@TableName("gl_purchase_offer_grant_item")
public class PurchaseOfferGrantItem {
    @TableId(value = "id")
    private Long id;
    private String tenantId;
    private Long offerId;
    private String grantType;
    private String currencyCode;
    private BigDecimal grantAmount;
    private String fundPropertyCode;
    private String wageringMode;
    private BigDecimal wageringRequiredAmount;
    private BigDecimal wageringMultiplier;
    private String gameScopeType;
    private String gameScopeValue;
    private Integer wageringExpireDays;
    private Integer sortOrder;
    private String remark;
}
```

- [ ] **Step 4: Implement mapper interfaces**

```java
public interface PurchaseOfferMapper extends BaseMapperPlus<PurchaseOffer, PurchaseOfferVo> {
}

public interface PurchaseOfferGrantItemMapper extends BaseMapper<PurchaseOfferGrantItem> {
}
```

- [ ] **Step 5: Implement BO/VO**

`PurchaseOfferBo` must include nested grant items and a static test helper:

```java
@Data
public class PurchaseOfferBo extends BaseEntity {
    private Long id;
    private String offerNo;
    private String offerName;
    private String offerType;
    private String payCurrencyCode;
    private BigDecimal payAmount;
    private String userScopeType;
    private String userScopeValue;
    private String regionScopeType;
    private String regionScopeValue;
    private String purchaseLimitType;
    private String stackable;
    private String status;
    private Integer sortOrder;
    private Date startTime;
    private Date endTime;
    private String remark;
    private List<PurchaseOfferGrantItemBo> grantItems;

    public static PurchaseOfferGrantItemBo grantItem(String grantType, String currencyCode, BigDecimal grantAmount, String wageringMode, BigDecimal wageringMultiplier) {
        PurchaseOfferGrantItemBo item = new PurchaseOfferGrantItemBo();
        item.setGrantType(grantType);
        item.setCurrencyCode(currencyCode);
        item.setGrantAmount(grantAmount);
        item.setWageringMode(wageringMode);
        item.setWageringMultiplier(wageringMultiplier);
        item.setGameScopeType("ALL");
        return item;
    }
}
```

- [ ] **Step 6: Implement service**

`PurchaseOfferServiceImpl.insertByBo` must:

1. Validate offer name and at least one grant item.
2. Insert offer.
3. Normalize each grant item fund property:
   - `PURCHASE_GRANT + GC -> PURCHASE_GRANT_GC`
   - `PURCHASE_BONUS + SC -> PURCHASE_BONUS_SC`
   - fallback: `grantType + "_" + currencyCode`
4. For `NONE`, set wagering amounts to zero.
5. Insert grant items.

Minimal helper:

```java
private String resolveFundPropertyCode(PurchaseOfferGrantItemBo item) {
    if ("PURCHASE_GRANT".equals(item.getGrantType()) && "GC".equals(item.getCurrencyCode())) {
        return "PURCHASE_GRANT_GC";
    }
    if ("PURCHASE_BONUS".equals(item.getGrantType()) && "SC".equals(item.getCurrencyCode())) {
        return "PURCHASE_BONUS_SC";
    }
    return item.getGrantType() + "_" + item.getCurrencyCode();
}
```

- [ ] **Step 7: Run test to verify GREEN**

Run the same Maven command.

Expected:

```text
Tests run: 1, Failures: 0, Errors: 0
BUILD SUCCESS
```

- [ ] **Step 8: Add controller CRUD**

Create `/payment/purchase-offer` endpoints:

```java
@SaCheckPermission("payment:purchaseOffer:list")
@GetMapping("/list")
public TableDataInfo<PurchaseOfferVo> list(PurchaseOfferBo bo, PageQuery pageQuery)

@SaCheckPermission("payment:purchaseOffer:query")
@GetMapping("/{id}")
public R<PurchaseOfferVo> getInfo(@PathVariable Long id)

@SaCheckPermission("payment:purchaseOffer:add")
@PostMapping
public R<Void> add(@Validated @RequestBody PurchaseOfferBo bo)

@SaCheckPermission("payment:purchaseOffer:edit")
@PutMapping
public R<Void> edit(@Validated @RequestBody PurchaseOfferBo bo)
```

- [ ] **Step 9: Run payment module tests**

```powershell
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-modules/gameluck-payment -am -DskipTests=false "-Dprofiles.active=local" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

- [ ] **Step 10: Commit**

```powershell
git add backend/gameluck-modules/gameluck-payment
git commit -m "feat: add purchase offer backend"
```

---

## Task 4: Backend Purchase Fulfillment Snapshot

**Files:**
- Create: `PurchaseOrder.java`
- Create: `PurchaseOrderGrantSnapshot.java`
- Add service method to simulate paid order fulfillment.
- Test: `PurchaseOfferServiceImplTest`

- [ ] **Step 1: Write failing test**

Add test:

```java
@Test
void paidPurchaseCreatesGrantSnapshotsWithoutChangingOfferConfig() {
    // Given an enabled offer with GC no wagering and SC 10x wagering.
    // When service simulates a paid purchase for member 1001.
    // Then snapshots contain required turnover 0 for GC and 10 for SC.
}
```

Use mocks and captors for `PurchaseOrderGrantSnapshotMapper`.

- [ ] **Step 2: Implement snapshot entities and mapper**

Create `PurchaseOrderGrantSnapshotMapper extends BaseMapper<PurchaseOrderGrantSnapshot>`.

- [ ] **Step 3: Implement calculation**

Rules:

```text
NONE -> requiredTurnover = 0
FIXED -> requiredTurnover = wageringRequiredAmount
MULTIPLIER -> requiredTurnover = grantAmount * wageringMultiplier
COMBINED_MULTIPLIER -> defer to order-level grouping in later phase; first phase rejects this mode with ServiceException
```

- [ ] **Step 4: Wire wallet credit request as future integration seam**

Do not call a real payment gateway. First phase service can expose:

```java
public List<WalletCreditBo> buildWalletCreditsForPaidOrder(PurchaseOrder order, List<PurchaseOfferGrantItem> items)
```

Each `WalletCreditBo` must include:

```java
currencyCode
amount
fundPropertyCode
sourceType = "PURCHASE"
businessNo = purchaseOrderNo
turnoverRequiredAmount
turnoverMultiplier
gameScopeType
gameScopeValue
turnoverExpireTime
```

- [ ] **Step 5: Run tests**

```powershell
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-modules/gameluck-payment,gameluck-modules/gameluck-wallet -am -DskipTests=false "-Dprofiles.active=local" "-Dtest=PurchaseOfferServiceImplTest,WalletCoreServiceImplTest,WalletTurnoverTaskServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

- [ ] **Step 6: Commit**

```powershell
git add backend/gameluck-modules/gameluck-payment
git commit -m "feat: snapshot purchase grants for wallet credit"
```

---

## Task 5: Admin Purchase Offer API And Page

**Files:**
- Create: `admin-ui/src/api/payment/purchaseOffer/index.ts`
- Create: `admin-ui/src/api/payment/purchaseOffer/types.ts`
- Create: `admin-ui/src/views/payment/purchase-offer/index.vue`
- Modify: language files only if needed

- [ ] **Step 1: Create API types**

```ts
export interface PurchaseOfferGrantItem {
  id?: string | number;
  grantType: 'PURCHASE_GRANT' | 'PURCHASE_BONUS' | 'DEPOSIT_PRINCIPAL' | 'DEPOSIT_BONUS';
  currencyCode: string;
  grantAmount: number;
  wageringMode: 'NONE' | 'FIXED' | 'MULTIPLIER';
  wageringRequiredAmount?: number;
  wageringMultiplier?: number;
  gameScopeType?: 'ALL' | 'CATEGORY' | 'PROVIDER' | 'GAME';
  gameScopeValue?: string;
  wageringExpireDays?: number;
}

export interface PurchaseOfferVO extends BaseEntity {
  id: string | number;
  offerNo: string;
  offerName: string;
  offerType: string;
  payCurrencyCode: string;
  payAmount: number;
  userScopeType: string;
  regionScopeType: string;
  purchaseLimitType: string;
  stackable: string;
  status: string;
  sortOrder: number;
  startTime?: string;
  endTime?: string;
  grantItems?: PurchaseOfferGrantItem[];
}
```

- [ ] **Step 2: Create API functions**

```ts
export function listPurchaseOffer(query: PurchaseOfferQuery): AxiosPromise<PurchaseOfferVO[]> {
  return request({ url: '/payment/purchase-offer/list', method: 'get', params: query });
}

export function getPurchaseOffer(id: string | number): AxiosPromise<PurchaseOfferVO> {
  return request({ url: '/payment/purchase-offer/' + id, method: 'get' });
}

export function addPurchaseOffer(data: PurchaseOfferForm) {
  return request({ url: '/payment/purchase-offer', method: 'post', data });
}

export function updatePurchaseOffer(data: PurchaseOfferForm) {
  return request({ url: '/payment/purchase-offer', method: 'put', data });
}
```

- [ ] **Step 3: Build B-side page**

Page layout:

```text
Filters: 产品名称, 类型, 支付币种, 状态
Table: 产品编号, 产品名称, 类型, 支付金额, 发放内容, 用户范围, 状态, 时间, 操作
Dialog:
  基础信息
  支付设置
  发放项
  流水要求
  适用范围
  时间和状态
```

Operator wording:

```text
GC 当前不可提不可兑，默认不需要流水。
SC 赠送可配置流水要求，完成后才可兑换/兑付。
资金属性由系统按发放类型自动生成，运营无需配置。
```

- [ ] **Step 4: Implement grant item editor defaults**

Default starter pack:

```ts
const defaultGrantItems = (): PurchaseOfferGrantItem[] => [
  {
    grantType: 'PURCHASE_GRANT',
    currencyCode: 'GC',
    grantAmount: 10000,
    wageringMode: 'NONE',
    gameScopeType: 'ALL'
  },
  {
    grantType: 'PURCHASE_BONUS',
    currencyCode: 'SC',
    grantAmount: 1,
    wageringMode: 'MULTIPLIER',
    wageringMultiplier: 10,
    gameScopeType: 'ALL'
  }
];
```

- [ ] **Step 5: Validate form**

Rules:

```text
offerName required
payAmount > 0
at least one grant item
grantAmount > 0
MULTIPLIER requires wageringMultiplier > 0
FIXED requires wageringRequiredAmount > 0
gameScopeType != ALL requires gameScopeValue
```

- [ ] **Step 6: Run frontend build**

```powershell
pnpm --dir admin-ui build:dev
```

- [ ] **Step 7: Commit**

```powershell
git add admin-ui/src/api/payment/purchaseOffer admin-ui/src/views/payment/purchase-offer
git commit -m "feat: add purchase offer admin page"
```

---

## Task 6: End-To-End Local Verification

**Files:**
- No new files unless fixing defects found by verification.

- [ ] **Step 1: Run backend targeted tests**

```powershell
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-modules/gameluck-payment,gameluck-modules/gameluck-wallet,gameluck-modules/gameluck-promotion -am -DskipTests=false "-Dprofiles.active=local" "-Dtest=PurchaseOfferServiceImplTest,DepositOrderServiceImplTest,WalletCoreServiceImplTest,WalletTurnoverTaskServiceImplTest,PromotionRewardServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected:

```text
BUILD SUCCESS
```

- [ ] **Step 2: Run frontend build**

```powershell
pnpm --dir admin-ui build:dev
```

Expected:

```text
Menu icon check passed.
i18n check passed.
✓ built
```

- [ ] **Step 3: Verify DB seeds**

```powershell
@'
SELECT offer_no, offer_name, offer_type, pay_currency_code, pay_amount, status
FROM gl_purchase_offer
ORDER BY sort_order, offer_no;

SELECT property_code, property_name
FROM gl_wallet_fund_property_template
WHERE property_code IN ('PURCHASE_GRANT_GC', 'PURCHASE_BONUS_SC');
'@ | mysql -uroot -proot gameluck_vue
```

- [ ] **Step 4: Manual B-side smoke**

Open:

```text
http://127.0.0.1:5173
```

Verify:

```text
支付中心 -> 购买产品 visible
新增购买产品 opens dialog
Default grant items are GC no wagering + SC 10x wagering
No fundPropertyCode visible to operator
Save creates/updates offer
促销奖励 no longer suggests purchase/recharge configuration
```

- [ ] **Step 5: Commit verification fixes**

If any fixes were needed:

```powershell
git add <fixed-files>
git commit -m "fix: stabilize purchase offer foundation"
```

---

## Self-Review

Spec coverage:

- Currency capability is preserved through existing wallet currency and policy tables.
- Purchase offer, grant item, order, and grant snapshot are covered by Tasks 2-4.
- Operator-facing B-side purchase page is covered by Task 5.
- Promotion reward stop-loss is covered by Task 1.
- Wallet wagering snapshot integration seam is covered by Task 4.
- Full real payment gateway and true-money withdrawal are intentionally out of scope for this plan.

Placeholder scan:

- No TBD/TODO placeholders.
- Deferred `COMBINED_MULTIPLIER` is explicitly rejected in first phase with `ServiceException`.

Type consistency:

- `wageringMode`, `grantType`, `fundPropertyCode`, `gameScopeType`, and `turnoverRequiredAmount` names are consistent with current wallet concepts while keeping purchase-domain names in purchase tables.
