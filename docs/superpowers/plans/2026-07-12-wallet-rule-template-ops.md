# Wallet Rule Template Ops Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build wallet rule default templates, operator-friendly rule management, source-type canonicalization, and manual adjustment operation-level release strategy.

**Architecture:** Keep wallet core accounting unchanged. Add a template layer inside `gameluck-wallet` that can preview and seed missing default rules without overwriting existing rows, then make admin UI consume standard source options and operator-readable form sections. Manual adjustment is implemented as an admin wallet operation that calls wallet core with `MANUAL_ADJUST` and explicit release strategy.

**Tech Stack:** Java 17, Spring Boot, MyBatis-Plus, JUnit 5, Mockito, Vue 3, TypeScript, Element Plus, Vitest, MySQL SQL seed scripts.

---

## File Structure

- Create `backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/domain/vo/WalletRuleTemplateVo.java`: preview row returned by default-template APIs.
- Create `backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/domain/bo/WalletManualAdjustBo.java`: admin manual adjustment request.
- Modify `backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/service/IWalletRuleService.java`: add default template and source canonicalization contracts.
- Modify `backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/service/impl/WalletRuleServiceImpl.java`: implement template list, missing preview, idempotent seeding, and canonical source mapping.
- Modify `backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/controller/WalletRuleController.java`: expose `GET /wallet/rule/default/preview` and `POST /wallet/rule/default/seed`.
- Create `backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/service/IWalletManualAdjustService.java`: manual adjustment service contract.
- Create `backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/service/impl/WalletManualAdjustServiceImpl.java`: validate manual adjustment strategy and call wallet core.
- Create `backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/controller/WalletManualAdjustController.java`: expose admin adjustment endpoint.
- Modify `backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/service/impl/WalletCoreServiceImpl.java`: allow release override only for `MANUAL_ADJUST`.
- Create `backend/gameluck-modules/gameluck-wallet/src/test/java/com/gameluck/wallet/service/impl/WalletRuleServiceImplTest.java`: add focused service tests for preview and seed.
- Modify `backend/gameluck-modules/gameluck-wallet/src/test/java/com/gameluck/wallet/service/impl/WalletCoreServiceImplTest.java`: add manual adjustment override tests.
- Create `backend/gameluck-modules/gameluck-wallet/src/test/java/com/gameluck/wallet/service/impl/WalletManualAdjustServiceImplTest.java`: add manual adjustment strategy tests.
- Modify `backend/script/sql/gameluck_wallet.sql`: complete default rules and add menu permissions.
- Modify `backend/script/sql/gameluck_platform_dict.sql`: add standard source values and keep aliases non-default.
- Modify `backend/gameluck-admin/src/main/resources/i18n/messages.properties`, `messages_zh_CN.properties`, `messages_en_US.properties`: add backend messages.
- Modify `admin-ui/src/utils/businessLabels.ts`: add standard source options, historical labels, and rule template helpers.
- Modify `admin-ui/src/utils/businessLabels.test.ts`: verify standard options and historical display.
- Modify `admin-ui/src/api/wallet/rule/types.ts`: add template preview types.
- Modify `admin-ui/src/api/wallet/rule/index.ts`: add preview and seed APIs.
- Modify `admin-ui/src/views/wallet/rule/index.vue`: add template preview/seed UI and operator-friendly form sections.
- Create `admin-ui/src/api/wallet/manualAdjust/types.ts`: manual adjustment API types.
- Create `admin-ui/src/api/wallet/manualAdjust/index.ts`: manual adjustment API wrapper.
- Create `admin-ui/src/views/wallet/manual-adjust/index.vue`: manual adjustment admin page.
- Modify `admin-ui/src/utils/i18nText.ts`: add required Chinese-to-English UI text mappings.

---

## Task 1: Backend Default Templates And Source Canonicalization

**Files:**
- Create: `backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/domain/vo/WalletRuleTemplateVo.java`
- Modify: `backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/service/IWalletRuleService.java`
- Modify: `backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/service/impl/WalletRuleServiceImpl.java`
- Modify: `backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/controller/WalletRuleController.java`
- Create: `backend/gameluck-modules/gameluck-wallet/src/test/java/com/gameluck/wallet/service/impl/WalletRuleServiceImplTest.java`

- [ ] **Step 1: Write failing tests for template preview, idempotent seed, and canonical source**

Create `WalletRuleServiceImplTest.java` with these tests:

```java
package com.gameluck.wallet.service.impl;

import com.gameluck.wallet.domain.WalletRule;
import com.gameluck.wallet.domain.vo.WalletRuleTemplateVo;
import com.gameluck.wallet.mapper.WalletRuleMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WalletRuleServiceImplTest {

    @Test
    void previewMissingDefaultRulesMarksExistingRowsWithoutCreating() {
        WalletRuleMapper mapper = mock(WalletRuleMapper.class);
        when(mapper.selectList(any())).thenReturn(List.of(existing("000000", "SC", "PROMOTION")));
        WalletRuleServiceImpl service = new WalletRuleServiceImpl(mapper);

        List<WalletRuleTemplateVo> rows = service.previewMissingDefaultRules("000000");

        assertTrue(rows.stream().anyMatch(row ->
            "SC".equals(row.getCurrencyCode())
                && "PROMOTION".equals(row.getSourceType())
                && Boolean.TRUE.equals(row.getExists())
                && Boolean.FALSE.equals(row.getWillCreate())));
        assertTrue(rows.stream().anyMatch(row ->
            "GC".equals(row.getCurrencyCode())
                && "REGISTER_BONUS".equals(row.getSourceType())
                && Boolean.FALSE.equals(row.getExists())
                && Boolean.TRUE.equals(row.getWillCreate())));
        verify(mapper, never()).insert(any());
    }

    @Test
    void seedMissingDefaultRulesOnlyInsertsMissingRows() {
        WalletRuleMapper mapper = mock(WalletRuleMapper.class);
        when(mapper.selectList(any())).thenReturn(List.of(existing("000000", "SC", "PROMOTION")));
        when(mapper.insert(any())).thenReturn(1);
        WalletRuleServiceImpl service = new WalletRuleServiceImpl(mapper);

        int created = service.seedMissingDefaultRules("000000");

        assertTrue(created > 0);
        verify(mapper, atLeastOnce()).insert(any(WalletRule.class));
    }

    @Test
    void canonicalSourceTypeMapsHistoricalAliases() {
        WalletRuleServiceImpl service = new WalletRuleServiceImpl(mock(WalletRuleMapper.class));

        assertEquals("GAME_PROFIT", service.canonicalSourceType("GAME_PAYOUT"));
        assertEquals("MANUAL_ADJUST", service.canonicalSourceType("ADJUSTMENT"));
        assertEquals("MANUAL_ADJUST", service.canonicalSourceType("ADJUST"));
        assertEquals("DEPOSIT", service.canonicalSourceType("DEPOSIT"));
    }

    private WalletRule existing(String tenantId, String currencyCode, String sourceType) {
        WalletRule rule = new WalletRule();
        rule.setTenantId(tenantId);
        rule.setCurrencyCode(currencyCode);
        rule.setSourceType(sourceType);
        return rule;
    }
}
```

- [ ] **Step 2: Run tests and verify they fail**

Run:

```powershell
cd backend
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-modules/gameluck-wallet -Dtest=WalletRuleServiceImplTest test -Plocal
```

Expected: compile failure because `WalletRuleTemplateVo`, `previewMissingDefaultRules`, `seedMissingDefaultRules`, and `canonicalSourceType` do not exist.

- [ ] **Step 3: Create `WalletRuleTemplateVo`**

```java
package com.gameluck.wallet.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class WalletRuleTemplateVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String currencyCode;
    private String sourceType;
    private String sourceLabel;
    private String ruleName;
    private String creditEnabled;
    private String debitEnabled;
    private String withdrawEnabled;
    private String exchangeEnabled;
    private String releaseMode;
    private String turnoverRequired;
    private BigDecimal defaultRequiredTurnover;
    private String status;
    private Integer sortOrder;
    private String remark;
    private Boolean exists;
    private Boolean willCreate;
}
```

- [ ] **Step 4: Extend `IWalletRuleService`**

Add:

```java
List<WalletRuleTemplateVo> listDefaultTemplates();

List<WalletRuleTemplateVo> previewMissingDefaultRules(String tenantId);

int seedMissingDefaultRules(String tenantId);

String canonicalSourceType(String sourceType);
```

- [ ] **Step 5: Implement default templates in `WalletRuleServiceImpl`**

Add constants and template construction:

```java
private static final String DEFAULT_TENANT_ID = "000000";

private static final Map<String, String> SOURCE_ALIASES = Map.of(
    "GAME_PAYOUT", "GAME_PROFIT",
    "ADJUST", "MANUAL_ADJUST",
    "ADJUSTMENT", "MANUAL_ADJUST"
);

private static final List<WalletRuleTemplateVo> DEFAULT_TEMPLATES = List.of(
    template("GC", "REGISTER_BONUS", "GC registration bonus", "0", "1", "1", "1", "IMMEDIATE", "1", 0, 21, "Registration GC reward."),
    template("SC", "REGISTER_BONUS", "SC registration bonus", "0", "1", "1", "1", "IMMEDIATE", "1", 0, 22, "Registration SC reward."),
    template("GC", "DAILY_REWARD", "GC daily login reward", "0", "1", "1", "1", "IMMEDIATE", "1", 0, 31, "Daily login GC reward."),
    template("SC", "DAILY_REWARD", "SC daily login reward", "0", "1", "1", "1", "IMMEDIATE", "1", 0, 32, "Daily login SC reward."),
    template("GC", "PROMOTION", "GC promotion", "0", "1", "1", "1", "IMMEDIATE", "1", 0, 41, "Promotion GC reward."),
    template("SC", "PROMOTION", "SC promotion", "0", "1", "1", "1", "AFTER_TURNOVER", "0", 0, 42, "Promotion SC reward after turnover."),
    template("RC", "DEPOSIT", "RC deposit", "0", "0", "0", "1", "IMMEDIATE", "1", 0, 51, "RC deposit can be withdrawable immediately."),
    template("GC", "GAME_PROFIT", "GC game profit", "0", "0", "1", "1", "NEVER", "1", 0, 61, "GC is not withdrawable or exchangeable."),
    template("SC", "GAME_PROFIT", "SC game profit", "0", "0", "1", "0", "AFTER_TURNOVER", "0", 0, 62, "SC game profit can be exchanged after conditions."),
    template("SC", "GAME_REFUND", "SC game refund", "0", "0", "1", "0", "IMMEDIATE", "0", 0, 71, "SC refund returns original stake immediately."),
    template("GC", "MANUAL_ADJUST", "GC manual adjustment", "0", "0", "1", "1", "IMMEDIATE", "1", 0, 81, "Manual GC adjustment."),
    template("SC", "MANUAL_ADJUST", "SC manual adjustment", "0", "0", "1", "0", "MANUAL_REVIEW", "1", 0, 82, "Manual SC adjustment uses operation strategy."),
    template("RC", "MANUAL_ADJUST", "RC manual adjustment", "0", "0", "0", "1", "MANUAL_REVIEW", "1", 0, 83, "Manual RC adjustment uses operation strategy.")
);
```

Implement methods:

```java
@Override
public List<WalletRuleTemplateVo> listDefaultTemplates() {
    return DEFAULT_TEMPLATES.stream().map(this::copyTemplate).toList();
}

@Override
public List<WalletRuleTemplateVo> previewMissingDefaultRules(String tenantId) {
    String resolvedTenantId = StringUtils.blankToDefault(tenantId, DEFAULT_TENANT_ID);
    Set<String> existingKeys = baseMapper.selectList(Wrappers.lambdaQuery(WalletRule.class)
            .eq(WalletRule::getTenantId, resolvedTenantId))
        .stream()
        .map(rule -> ruleKey(rule.getCurrencyCode(), canonicalSourceType(rule.getSourceType())))
        .collect(Collectors.toSet());
    return DEFAULT_TEMPLATES.stream().map(template -> {
        WalletRuleTemplateVo row = copyTemplate(template);
        boolean exists = existingKeys.contains(ruleKey(row.getCurrencyCode(), row.getSourceType()));
        row.setExists(exists);
        row.setWillCreate(!exists);
        return row;
    }).toList();
}

@Override
public int seedMissingDefaultRules(String tenantId) {
    String resolvedTenantId = StringUtils.blankToDefault(tenantId, DEFAULT_TENANT_ID);
    int created = 0;
    for (WalletRuleTemplateVo row : previewMissingDefaultRules(resolvedTenantId)) {
        if (!Boolean.TRUE.equals(row.getWillCreate())) {
            continue;
        }
        WalletRule add = new WalletRule();
        add.setId(IdUtil.getSnowflakeNextId());
        add.setTenantId(resolvedTenantId);
        add.setCurrencyCode(row.getCurrencyCode());
        add.setSourceType(row.getSourceType());
        add.setRuleName(row.getRuleName());
        add.setCreditEnabled(row.getCreditEnabled());
        add.setDebitEnabled(row.getDebitEnabled());
        add.setWithdrawEnabled(row.getWithdrawEnabled());
        add.setExchangeEnabled(row.getExchangeEnabled());
        add.setReleaseMode(row.getReleaseMode());
        add.setTurnoverRequired(row.getTurnoverRequired());
        add.setDefaultRequiredTurnover(row.getDefaultRequiredTurnover());
        add.setStatus(row.getStatus());
        add.setSortOrder(row.getSortOrder());
        add.setRemark(row.getRemark());
        add.setVersion(0);
        add.setDelFlag(SystemConstants.NORMAL);
        add.setCreateTime(new Date());
        add.setUpdateTime(new Date());
        created += baseMapper.insert(add);
    }
    return created;
}

@Override
public String canonicalSourceType(String sourceType) {
    if (StringUtils.isBlank(sourceType)) {
        return sourceType;
    }
    return SOURCE_ALIASES.getOrDefault(sourceType, sourceType);
}
```

Add helper methods:

```java
private static WalletRuleTemplateVo template(String currencyCode, String sourceType, String ruleName,
                                             String creditEnabled, String debitEnabled, String withdrawEnabled,
                                             String exchangeEnabled, String releaseMode, String turnoverRequired,
                                             int defaultRequiredTurnover, int sortOrder, String remark) {
    WalletRuleTemplateVo vo = new WalletRuleTemplateVo();
    vo.setCurrencyCode(currencyCode);
    vo.setSourceType(sourceType);
    vo.setSourceLabel(sourceType);
    vo.setRuleName(ruleName);
    vo.setCreditEnabled(creditEnabled);
    vo.setDebitEnabled(debitEnabled);
    vo.setWithdrawEnabled(withdrawEnabled);
    vo.setExchangeEnabled(exchangeEnabled);
    vo.setReleaseMode(releaseMode);
    vo.setTurnoverRequired(turnoverRequired);
    vo.setDefaultRequiredTurnover(BigDecimal.valueOf(defaultRequiredTurnover));
    vo.setStatus(ENABLED);
    vo.setSortOrder(sortOrder);
    vo.setRemark(remark);
    vo.setExists(false);
    vo.setWillCreate(true);
    return vo;
}

private WalletRuleTemplateVo copyTemplate(WalletRuleTemplateVo source) {
    WalletRuleTemplateVo target = new WalletRuleTemplateVo();
    BeanUtil.copyProperties(source, target);
    return target;
}

private String ruleKey(String currencyCode, String sourceType) {
    return currencyCode + ":" + sourceType;
}
```

Import `BeanUtil`, `Map`, `Set`, `Collectors`.

- [ ] **Step 6: Add controller endpoints**

In `WalletRuleController` add:

```java
@SaCheckPermission("wallet:rule:list")
@GetMapping("/default/preview")
public R<List<WalletRuleTemplateVo>> previewDefaultRules() {
    return R.ok(walletRuleService.previewMissingDefaultRules(TenantHelper.getTenantId()));
}

@SaCheckPermission("wallet:rule:seed")
@Log(title = "钱包规则", businessType = BusinessType.INSERT)
@PostMapping("/default/seed")
public R<Integer> seedDefaultRules() {
    return R.ok(walletRuleService.seedMissingDefaultRules(TenantHelper.getTenantId()));
}
```

- [ ] **Step 7: Run wallet rule tests**

Run:

```powershell
cd backend
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-modules/gameluck-wallet -Dtest=WalletRuleServiceImplTest test -Plocal
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 8: Commit**

```powershell
git add backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/domain/vo/WalletRuleTemplateVo.java `
  backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/service/IWalletRuleService.java `
  backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/service/impl/WalletRuleServiceImpl.java `
  backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/controller/WalletRuleController.java `
  backend/gameluck-modules/gameluck-wallet/src/test/java/com/gameluck/wallet/service/impl/WalletRuleServiceImplTest.java
git commit -m "feat(wallet): add default rule templates"
```

---

## Task 2: SQL Seeds, Permissions, And Backend Messages

**Files:**
- Modify: `backend/script/sql/gameluck_wallet.sql`
- Modify: `backend/script/sql/gameluck_platform_dict.sql`
- Modify: `backend/gameluck-admin/src/main/resources/i18n/messages.properties`
- Modify: `backend/gameluck-admin/src/main/resources/i18n/messages_zh_CN.properties`
- Modify: `backend/gameluck-admin/src/main/resources/i18n/messages_en_US.properties`

- [ ] **Step 1: Add missing default wallet rule SQL rows**

In the `INSERT INTO gl_wallet_rule` block, add missing rows for:

```sql
(1900000000000000201, '000000', 'GC', 'REGISTER_BONUS', 'GC registration bonus', '0', '1', '1', '1', 'IMMEDIATE', '1', 0, '0', 21, 'Registration GC reward.', NOW()),
(1900000000000000202, '000000', 'SC', 'REGISTER_BONUS', 'SC registration bonus', '0', '1', '1', '1', 'IMMEDIATE', '1', 0, '0', 22, 'Registration SC reward.', NOW()),
(1900000000000000401, '000000', 'GC', 'PROMOTION', 'GC promotion', '0', '1', '1', '1', 'IMMEDIATE', '1', 0, '0', 41, 'Promotion GC reward.', NOW()),
(1900000000000000801, '000000', 'GC', 'MANUAL_ADJUST', 'GC manual adjustment', '0', '0', '1', '1', 'IMMEDIATE', '1', 0, '0', 81, 'Manual GC adjustment.', NOW()),
(1900000000000000802, '000000', 'SC', 'MANUAL_ADJUST', 'SC manual adjustment', '0', '0', '1', '0', 'MANUAL_REVIEW', '1', 0, '0', 82, 'Manual SC adjustment uses operation strategy.', NOW())
```

Change existing `SC + PROMOTION` row from `MANUAL_REVIEW` to `AFTER_TURNOVER`.

- [ ] **Step 2: Add wallet rule seed permission and manual adjust menu**

Add permission row under wallet rule menu:

```sql
(1820, '补齐默认规则', 1806, 4, '#', '', '', 1, 0, 'F', '0', '0', 'wallet:rule:seed', '#', 103, 1, NOW(), NULL, NULL, '')
```

Add manual adjust menu:

```sql
(1807, '人工调账', 1800, 7, 'manual-adjust', 'wallet/manual-adjust/index', '', 1, 0, 'C', '0', '0', 'wallet:manualAdjust:add', 'edit', 103, 1, NOW(), NULL, NULL, '后台人工调账菜单'),
(1821, '人工调账操作', 1807, 1, '#', '', '', 1, 0, 'F', '0', '0', 'wallet:manualAdjust:add', '#', 103, 1, NOW(), NULL, NULL, '')
```

- [ ] **Step 3: Update wallet source dictionary seeds**

Ensure `gl_wallet_biz_type` includes standard rows for `GAME_PROFIT`, `MANUAL_ADJUST`, `GAME_REFUND`, `PROMOTION`, `TURNOVER`.

Keep `GAME_PAYOUT`, `ADJUST`, and `ADJUSTMENT` rows if present, but set their default flag to `N` and remarks to indicate historical alias.

- [ ] **Step 4: Add backend messages**

Add to `messages_zh_CN.properties` and default `messages.properties`:

```properties
wallet.manual.adjust.amount.positive=调账金额必须大于0
wallet.manual.adjust.turnover.positive=选择需要流水时，流水金额必须大于0
wallet.manual.adjust.strategy.invalid=人工调账资金策略无效
wallet.manual.adjust.release.override.only=只有人工调账允许覆盖到账策略
```

Add to `messages_en_US.properties`:

```properties
wallet.manual.adjust.amount.positive=Manual adjustment amount must be greater than 0
wallet.manual.adjust.turnover.positive=Turnover amount must be greater than 0 when turnover is required
wallet.manual.adjust.strategy.invalid=Invalid manual adjustment fund strategy
wallet.manual.adjust.release.override.only=Only manual adjustment can override release strategy
```

- [ ] **Step 5: Compile backend**

Run:

```powershell
cd backend
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-admin -am compile -Plocal -DskipTests
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 6: Commit**

```powershell
git add backend/script/sql/gameluck_wallet.sql backend/script/sql/gameluck_platform_dict.sql `
  backend/gameluck-admin/src/main/resources/i18n/messages.properties `
  backend/gameluck-admin/src/main/resources/i18n/messages_zh_CN.properties `
  backend/gameluck-admin/src/main/resources/i18n/messages_en_US.properties
git commit -m "feat(wallet): seed default rule templates"
```

---

## Task 3: Frontend Source Options And Historical Labels

**Files:**
- Modify: `admin-ui/src/utils/businessLabels.ts`
- Modify: `admin-ui/src/utils/businessLabels.test.ts`

- [ ] **Step 1: Write failing frontend label tests**

Update `businessLabels.test.ts` to assert:

```ts
expect(businessLabel('sourceType', 'GAME_PAYOUT')).toBe('游戏派奖（历史来源）');
expect(businessLabel('sourceType', 'ADJUSTMENT')).toBe('人工调账（历史来源）');
expect(businessLabel('sourceType', 'ADJUST')).toBe('人工调账（历史来源）');
expect(businessOptions('sourceType').filter((item) => item.label === '游戏派奖')).toHaveLength(1);
expect(businessOptions('sourceType').filter((item) => item.label === '人工调账')).toHaveLength(1);
expect(businessOptions('walletRuleSourceType')).not.toContainEqual({ label: '游戏派奖（历史来源）', value: 'GAME_PAYOUT' });
```

- [ ] **Step 2: Run tests and verify they fail**

Run:

```powershell
pnpm --dir admin-ui exec vitest run src/utils/businessLabels.test.ts
```

Expected: FAIL because `walletRuleSourceType` category and historical labels do not exist.

- [ ] **Step 3: Add option categories**

In `BusinessLabelCategory`, add:

```ts
| 'walletRuleSourceType'
```

Split source labels into standard and historical:

```ts
const standardSourceTypes: LabelItem[] = [
  { value: 'REGISTER_BONUS', label: '注册赠送' },
  { value: 'DAILY_REWARD', label: '每日奖励' },
  { value: 'TASK_REWARD', label: '任务奖励' },
  { value: 'DEPOSIT', label: '充值' },
  { value: 'GAME_BET', label: '游戏投注' },
  { value: 'GAME_PROFIT', label: '游戏派奖' },
  { value: 'GAME_REFUND', label: '游戏退款' },
  { value: 'GAME_SETTLE', label: '游戏结算' },
  { value: 'REDEMPTION', label: '兑换' },
  { value: 'PROMOTION', label: '活动奖励' },
  { value: 'MANUAL_ADJUST', label: '人工调账' },
  { value: 'TURNOVER', label: '流水调整' }
];

const historicalSourceTypes: LabelItem[] = [
  { value: 'GAME_PAYOUT', label: '游戏派奖（历史来源）' },
  { value: 'ADJUST', label: '人工调账（历史来源）' },
  { value: 'ADJUSTMENT', label: '人工调账（历史来源）' }
];
```

Set:

```ts
sourceType: [...standardSourceTypes, ...historicalSourceTypes],
walletRuleSourceType: standardSourceTypes,
```

- [ ] **Step 4: Run label tests**

Run:

```powershell
pnpm --dir admin-ui exec vitest run src/utils/businessLabels.test.ts
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add admin-ui/src/utils/businessLabels.ts admin-ui/src/utils/businessLabels.test.ts
git commit -m "feat(admin): standardize wallet source labels"
```

---

## Task 4: Admin Wallet Rule Template UI And Operator-Friendly Form

**Files:**
- Modify: `admin-ui/src/api/wallet/rule/types.ts`
- Modify: `admin-ui/src/api/wallet/rule/index.ts`
- Modify: `admin-ui/src/views/wallet/rule/index.vue`
- Modify: `admin-ui/src/utils/i18nText.ts`

- [ ] **Step 1: Add API types**

Add to `types.ts`:

```ts
export interface RuleTemplateVO {
  currencyCode: string;
  sourceType: string;
  sourceLabel: string;
  ruleName: string;
  creditEnabled: string;
  debitEnabled: string;
  withdrawEnabled: string;
  exchangeEnabled: string;
  releaseMode: string;
  turnoverRequired: string;
  defaultRequiredTurnover: number;
  status: string;
  sortOrder: number;
  remark: string;
  exists: boolean;
  willCreate: boolean;
}
```

- [ ] **Step 2: Add API wrappers**

Add to `index.ts`:

```ts
import { RuleForm, RuleQuery, RuleTemplateVO, RuleVO } from './types';

export function previewDefaultRules(): AxiosPromise<RuleTemplateVO[]> {
  return request({
    url: '/wallet/rule/default/preview',
    method: 'get'
  });
}

export function seedDefaultRules(): AxiosPromise<number> {
  return request({
    url: '/wallet/rule/default/seed',
    method: 'post'
  });
}
```

- [ ] **Step 3: Update source options in rule page**

Change:

```ts
const sourceOptions = businessOptions('sourceType', tt);
```

to:

```ts
const sourceOptions = businessOptions('walletRuleSourceType', tt);
```

- [ ] **Step 4: Add preview/seed drawer**

Add a toolbar button:

```vue
<el-button v-hasPermi="['wallet:rule:seed']" type="success" plain icon="Plus" @click="openDefaultRulePreview">
  {{ tt('补齐默认规则') }}
</el-button>
```

Add drawer:

```vue
<el-drawer v-model="defaultRuleDrawer.visible" :title="tt('默认规则预览')" size="760px">
  <el-alert :title="tt('只会新增缺失规则，不会覆盖已有规则')" type="info" show-icon class="mb-3" />
  <el-table v-loading="defaultRuleDrawer.loading" :data="defaultRuleRows" border>
    <el-table-column :label="tt('币种')" prop="currencyCode" width="80" />
    <el-table-column :label="tt('来源')" min-width="120">
      <template #default="scope">{{ businessLabel('sourceType', scope.row.sourceType, tt) }}</template>
    </el-table-column>
    <el-table-column :label="tt('到账策略')" min-width="140">
      <template #default="scope">{{ businessLabel('walletReleaseMode', scope.row.releaseMode, tt) }}</template>
    </el-table-column>
    <el-table-column :label="tt('默认流水')" prop="defaultRequiredTurnover" width="100" />
    <el-table-column :label="tt('处理方式')" width="120">
      <template #default="scope">
        <el-tag :type="scope.row.willCreate ? 'warning' : 'success'">
          {{ scope.row.willCreate ? tt('将创建') : tt('已存在') }}
        </el-tag>
      </template>
    </el-table-column>
  </el-table>
  <template #footer>
    <el-button @click="defaultRuleDrawer.visible = false">{{ tt('取消') }}</el-button>
    <el-button type="primary" :disabled="missingDefaultRuleCount === 0" @click="confirmSeedDefaultRules">
      {{ tt('确认补齐') }}
    </el-button>
  </template>
</el-drawer>
```

Add script:

```ts
const defaultRuleRows = ref<RuleTemplateVO[]>([]);
const defaultRuleDrawer = reactive({ visible: false, loading: false });
const missingDefaultRuleCount = computed(() => defaultRuleRows.value.filter((row) => row.willCreate).length);

const openDefaultRulePreview = async () => {
  defaultRuleDrawer.visible = true;
  defaultRuleDrawer.loading = true;
  try {
    const res = await previewDefaultRules();
    defaultRuleRows.value = res.data;
  } finally {
    defaultRuleDrawer.loading = false;
  }
};

const confirmSeedDefaultRules = async () => {
  await seedDefaultRules();
  proxy?.$modal.msgSuccess(tt('默认规则已补齐'));
  defaultRuleDrawer.visible = false;
  await getList();
};
```

- [ ] **Step 5: Rework rule form into business sections**

Replace the flat form with section headers:

```vue
<el-divider content-position="left">{{ tt('基础信息') }}</el-divider>
<!-- currencyCode, sourceType, ruleName, status -->

<el-divider content-position="left">{{ tt('资金方向') }}</el-divider>
<!-- creditEnabled, debitEnabled with operator copy -->

<el-divider content-position="left">{{ tt('资金用途') }}</el-divider>
<!-- withdrawEnabled, exchangeEnabled with helper text -->

<el-divider content-position="left">{{ tt('到账策略') }}</el-divider>
<!-- releaseMode -->

<el-divider content-position="left">{{ tt('流水要求') }}</el-divider>
<!-- turnoverRequired, defaultRequiredTurnover -->
```

Use labels:

```vue
<el-form-item :label="tt('增加余额')">
<el-form-item :label="tt('扣减余额')">
<el-form-item :label="tt('可提现')">
<el-form-item :label="tt('可兑换')">
<el-form-item :label="tt('业务提供流水')">
```

- [ ] **Step 6: Add i18n text mappings**

Add Chinese text keys used above to `admin-ui/src/utils/i18nText.ts`, including English values:

```ts
补齐默认规则: 'Complete Default Rules',
默认规则预览: 'Default Rule Preview',
只会新增缺失规则，不会覆盖已有规则: 'Only missing rules will be created. Existing rules will not be overwritten.',
将创建: 'Will Create',
已存在: 'Exists',
确认补齐: 'Confirm Seed',
默认规则已补齐: 'Default rules completed',
基础信息: 'Basic Info',
资金方向: 'Fund Direction',
资金用途: 'Fund Usage',
到账策略: 'Release Strategy',
流水要求: 'Turnover Requirement',
增加余额: 'Increase Balance',
扣减余额: 'Decrease Balance',
可提现: 'Withdrawable',
可兑换: 'Exchangeable',
业务提供流水: 'Business Provides Turnover'
```

- [ ] **Step 7: Run admin checks**

Run:

```powershell
pnpm --dir admin-ui check:i18n
pnpm --dir admin-ui build:dev
```

Expected: both pass. Existing chunk-size warnings are acceptable.

- [ ] **Step 8: Commit**

```powershell
git add admin-ui/src/api/wallet/rule/types.ts admin-ui/src/api/wallet/rule/index.ts `
  admin-ui/src/views/wallet/rule/index.vue admin-ui/src/utils/i18nText.ts
git commit -m "feat(admin): add wallet rule template UI"
```

---

## Task 5: Manual Adjustment Strategy Backend

**Files:**
- Create: `backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/domain/bo/WalletManualAdjustBo.java`
- Create: `backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/service/IWalletManualAdjustService.java`
- Create: `backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/service/impl/WalletManualAdjustServiceImpl.java`
- Create: `backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/controller/WalletManualAdjustController.java`
- Modify: `backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/service/impl/WalletCoreServiceImpl.java`
- Modify: `backend/gameluck-modules/gameluck-wallet/src/test/java/com/gameluck/wallet/service/impl/WalletCoreServiceImplTest.java`
- Create: `backend/gameluck-modules/gameluck-wallet/src/test/java/com/gameluck/wallet/service/impl/WalletManualAdjustServiceImplTest.java`

- [ ] **Step 1: Add wallet core override tests**

Add two tests to `WalletCoreServiceImplTest`. Use the existing mapper mocks in the file and add imports for `WalletRelease`, `WalletRuleVo`, `ServiceException`, `ArgumentCaptor`, and `assertThrows`.

```java
@Test
void manualAdjustCanOverrideRuleReleaseMode() {
    WalletAccountMapper accountMapper = mock(WalletAccountMapper.class);
    WalletTransactionMapper transactionMapper = mock(WalletTransactionMapper.class);
    WalletReleaseMapper releaseMapper = mock(WalletReleaseMapper.class);
    WalletFreezeMapper freezeMapper = mock(WalletFreezeMapper.class);
    IWalletRuleService ruleService = mock(IWalletRuleService.class);
    WalletCoreServiceImpl service = new WalletCoreServiceImpl(
        accountMapper, transactionMapper, releaseMapper, freezeMapper, ruleService);

    WalletRuleVo rule = new WalletRuleVo();
    rule.setReleaseMode("MANUAL_REVIEW");
    rule.setDefaultRequiredTurnover(BigDecimal.ZERO);
    rule.setTurnoverRequired("1");
    when(ruleService.resolveCreditRule(eq("000000"), eq("RC"), eq("MANUAL_ADJUST"))).thenReturn(rule);
    when(transactionMapper.selectByIdempotencyKey(eq("000000"), eq("manual-adjust:MA1"))).thenReturn(null);

    WalletAccount account = new WalletAccount();
    account.setAvailableBalance(BigDecimal.ZERO);
    account.setFrozenBalance(BigDecimal.ZERO);
    when(accountMapper.selectByBizKeyForUpdate(eq("000000"), eq(1001L), eq("RC"))).thenReturn(account);

    WalletCreditBo bo = new WalletCreditBo();
    bo.setIdempotencyKey("manual-adjust:MA1");
    bo.setMemberId(1001L);
    bo.setCurrencyCode("RC");
    bo.setSourceType("MANUAL_ADJUST");
    bo.setBusinessNo("MA1");
    bo.setAmount(new BigDecimal("10.000000"));
    bo.setReleaseMode("IMMEDIATE");
    bo.setRequiredTurnover(BigDecimal.ZERO);

    service.credit(bo);

    ArgumentCaptor<WalletRelease> releaseCaptor = ArgumentCaptor.forClass(WalletRelease.class);
    verify(releaseMapper).insert(releaseCaptor.capture());
    assertEquals("IMMEDIATE", releaseCaptor.getValue().getReleaseMode());
}

@Test
void nonManualAdjustCannotOverrideRuleReleaseMode() {
    WalletAccountMapper accountMapper = mock(WalletAccountMapper.class);
    WalletTransactionMapper transactionMapper = mock(WalletTransactionMapper.class);
    WalletReleaseMapper releaseMapper = mock(WalletReleaseMapper.class);
    WalletFreezeMapper freezeMapper = mock(WalletFreezeMapper.class);
    IWalletRuleService ruleService = mock(IWalletRuleService.class);
    WalletCoreServiceImpl service = new WalletCoreServiceImpl(
        accountMapper, transactionMapper, releaseMapper, freezeMapper, ruleService);

    WalletRuleVo rule = new WalletRuleVo();
    rule.setReleaseMode("MANUAL_REVIEW");
    rule.setDefaultRequiredTurnover(BigDecimal.ZERO);
    rule.setTurnoverRequired("1");
    when(ruleService.resolveCreditRule(eq("000000"), eq("SC"), eq("PROMOTION"))).thenReturn(rule);

    WalletCreditBo bo = new WalletCreditBo();
    bo.setIdempotencyKey("promotion:P1");
    bo.setMemberId(1001L);
    bo.setCurrencyCode("SC");
    bo.setSourceType("PROMOTION");
    bo.setBusinessNo("P1");
    bo.setAmount(new BigDecimal("1.000000"));
    bo.setReleaseMode("IMMEDIATE");
    bo.setRequiredTurnover(BigDecimal.ZERO);

    assertThrows(ServiceException.class, () -> service.credit(bo));
}
```

Use existing mock mapper pattern from the file. Capture `WalletRelease` with `ArgumentCaptor<WalletRelease>` and assert `getReleaseMode()`.

- [ ] **Step 2: Run tests and verify failure**

Run:

```powershell
cd backend
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-modules/gameluck-wallet -Dtest=WalletCoreServiceImplTest test -Plocal
```

Expected: override test fails because `resolveReleaseMode` currently rejects all release-mode mismatch.

- [ ] **Step 3: Modify `resolveReleaseMode`**

Change logic:

```java
private WalletReleaseMode resolveReleaseMode(WalletCreditBo bo, WalletRuleVo rule) {
    WalletReleaseMode ruleMode = parseReleaseMode(rule.getReleaseMode());
    if (StringUtils.isBlank(bo.getReleaseMode())) {
        return ruleMode;
    }
    WalletReleaseMode requestMode = parseReleaseMode(bo.getReleaseMode());
    if (requestMode == ruleMode) {
        return ruleMode;
    }
    if (!StringUtils.equals("MANUAL_ADJUST", bo.getSourceType())) {
        throw new ServiceException(MessageUtils.message("wallet.manual.adjust.release.override.only"));
    }
    return requestMode;
}
```

- [ ] **Step 4: Create `WalletManualAdjustBo`**

```java
@Data
public class WalletManualAdjustBo {
    @NotNull(message = "{member.id.required}")
    private Long memberId;

    @NotBlank(message = "{wallet.currency.required}")
    private String currencyCode;

    @NotNull(message = "{wallet.amount.required}")
    @DecimalMin(value = "0.000001", message = "{wallet.manual.adjust.amount.positive}")
    private BigDecimal amount;

    @NotBlank(message = "{wallet.manual.adjust.strategy.invalid}")
    private String strategy;

    @DecimalMin(value = "0", message = "{wallet.required.turnover.nonnegative}")
    private BigDecimal requiredTurnover;

    private Long operatorId;

    @NotBlank(message = "{wallet.business.no.required}")
    private String reason;
}
```

Supported `strategy` values:

- `IMMEDIATE`
- `AFTER_TURNOVER`
- `MANUAL_REVIEW`

- [ ] **Step 5: Create service and controller**

`IWalletManualAdjustService`:

```java
WalletTransaction adjust(WalletManualAdjustBo bo);
```

`WalletManualAdjustServiceImpl`:

```java
@RequiredArgsConstructor
@Service
public class WalletManualAdjustServiceImpl implements IWalletManualAdjustService {

    private static final String SOURCE_TYPE = "MANUAL_ADJUST";

    private final IWalletCoreService walletCoreService;

    @Override
    public WalletTransaction adjust(WalletManualAdjustBo bo) {
        WalletCreditBo creditBo = new WalletCreditBo();
        creditBo.setMemberId(bo.getMemberId());
        creditBo.setCurrencyCode(bo.getCurrencyCode());
        creditBo.setAmount(bo.getAmount());
        creditBo.setSourceType(SOURCE_TYPE);
        creditBo.setBusinessNo("MA" + IdUtil.getSnowflakeNextIdStr());
        creditBo.setIdempotencyKey("manual-adjust:" + creditBo.getBusinessNo());
        creditBo.setReleaseMode(resolveReleaseMode(bo));
        creditBo.setRequiredTurnover(resolveRequiredTurnover(bo));
        creditBo.setOperatorId(bo.getOperatorId());
        creditBo.setRemark(bo.getReason());
        return walletCoreService.credit(creditBo);
    }

    private String resolveReleaseMode(WalletManualAdjustBo bo) {
        return switch (bo.getStrategy()) {
            case "IMMEDIATE" -> "IMMEDIATE";
            case "AFTER_TURNOVER" -> "AFTER_TURNOVER";
            case "MANUAL_REVIEW" -> "MANUAL_REVIEW";
            default -> throw new ServiceException(MessageUtils.message("wallet.manual.adjust.strategy.invalid"));
        };
    }

    private BigDecimal resolveRequiredTurnover(WalletManualAdjustBo bo) {
        if (!"AFTER_TURNOVER".equals(bo.getStrategy())) {
            return BigDecimal.ZERO;
        }
        if (bo.getRequiredTurnover() == null || bo.getRequiredTurnover().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ServiceException(MessageUtils.message("wallet.manual.adjust.turnover.positive"));
        }
        return bo.getRequiredTurnover();
    }
}
```

Controller:

```java
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/wallet/manual-adjust")
public class WalletManualAdjustController extends BaseController {

    private final IWalletManualAdjustService walletManualAdjustService;

    @SaCheckPermission("wallet:manualAdjust:add")
    @Log(title = "人工调账", businessType = BusinessType.INSERT)
    @PostMapping
    public R<WalletTransaction> adjust(@Validated @RequestBody WalletManualAdjustBo bo) {
        return R.ok(walletManualAdjustService.adjust(bo));
    }
}
```

- [ ] **Step 6: Add manual adjustment service tests**

Create `WalletManualAdjustServiceImplTest`:

```java
package com.gameluck.wallet.service.impl;

import com.gameluck.common.core.exception.ServiceException;
import com.gameluck.wallet.domain.WalletTransaction;
import com.gameluck.wallet.domain.bo.WalletCreditBo;
import com.gameluck.wallet.domain.bo.WalletManualAdjustBo;
import com.gameluck.wallet.service.IWalletCoreService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WalletManualAdjustServiceImplTest {

@Test
void afterTurnoverRequiresPositiveTurnover() {
    IWalletCoreService walletCoreService = mock(IWalletCoreService.class);
    WalletManualAdjustServiceImpl service = new WalletManualAdjustServiceImpl(walletCoreService);

    WalletManualAdjustBo bo = baseBo();
    bo.setStrategy("AFTER_TURNOVER");
    bo.setRequiredTurnover(BigDecimal.ZERO);

    assertThrows(ServiceException.class, () -> service.adjust(bo));
}

@Test
void immediateBuildsManualAdjustCreditBo() {
    IWalletCoreService walletCoreService = mock(IWalletCoreService.class);
    when(walletCoreService.credit(any())).thenReturn(new WalletTransaction());
    WalletManualAdjustServiceImpl service = new WalletManualAdjustServiceImpl(walletCoreService);

    WalletManualAdjustBo bo = baseBo();
    bo.setStrategy("IMMEDIATE");

    service.adjust(bo);

    ArgumentCaptor<WalletCreditBo> captor = ArgumentCaptor.forClass(WalletCreditBo.class);
    verify(walletCoreService).credit(captor.capture());
    assertEquals("MANUAL_ADJUST", captor.getValue().getSourceType());
    assertEquals("IMMEDIATE", captor.getValue().getReleaseMode());
    assertEquals(new BigDecimal("0"), captor.getValue().getRequiredTurnover());
}

@Test
void manualReviewBuildsReviewCreditBo() {
    IWalletCoreService walletCoreService = mock(IWalletCoreService.class);
    when(walletCoreService.credit(any())).thenReturn(new WalletTransaction());
    WalletManualAdjustServiceImpl service = new WalletManualAdjustServiceImpl(walletCoreService);

    WalletManualAdjustBo bo = baseBo();
    bo.setStrategy("MANUAL_REVIEW");

    service.adjust(bo);

    ArgumentCaptor<WalletCreditBo> captor = ArgumentCaptor.forClass(WalletCreditBo.class);
    verify(walletCoreService).credit(captor.capture());
    assertEquals("MANUAL_REVIEW", captor.getValue().getReleaseMode());
    assertEquals("manual fix", captor.getValue().getRemark());
}

private WalletManualAdjustBo baseBo() {
    WalletManualAdjustBo bo = new WalletManualAdjustBo();
    bo.setMemberId(1001L);
    bo.setCurrencyCode("RC");
    bo.setAmount(new BigDecimal("10.000000"));
    bo.setReason("manual fix");
    return bo;
}
}
```

Use `ArgumentCaptor<WalletCreditBo>` and assert source type, release mode, turnover, amount, and reason.

- [ ] **Step 7: Run wallet tests**

Run:

```powershell
cd backend
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-modules/gameluck-wallet -Dtest=WalletCoreServiceImplTest,WalletManualAdjustServiceImplTest test -Plocal
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 8: Commit**

```powershell
git add backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/domain/bo/WalletManualAdjustBo.java `
  backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/service/IWalletManualAdjustService.java `
  backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/service/impl/WalletManualAdjustServiceImpl.java `
  backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/controller/WalletManualAdjustController.java `
  backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/service/impl/WalletCoreServiceImpl.java `
  backend/gameluck-modules/gameluck-wallet/src/test/java/com/gameluck/wallet/service/impl/WalletCoreServiceImplTest.java `
  backend/gameluck-modules/gameluck-wallet/src/test/java/com/gameluck/wallet/service/impl/WalletManualAdjustServiceImplTest.java
git commit -m "feat(wallet): add manual adjustment strategy"
```

---

## Task 6: Manual Adjustment Admin Page

**Files:**
- Create: `admin-ui/src/api/wallet/manualAdjust/types.ts`
- Create: `admin-ui/src/api/wallet/manualAdjust/index.ts`
- Create: `admin-ui/src/views/wallet/manual-adjust/index.vue`
- Modify: `admin-ui/src/utils/i18nText.ts`

- [ ] **Step 1: Add API types**

```ts
export interface ManualAdjustForm {
  memberId?: string | number;
  currencyCode?: string;
  amount?: number;
  strategy?: 'IMMEDIATE' | 'AFTER_TURNOVER' | 'MANUAL_REVIEW';
  requiredTurnover?: number;
  reason?: string;
}
```

- [ ] **Step 2: Add API wrapper**

```ts
import request from '@/utils/request';
import { ManualAdjustForm } from './types';

export function manualAdjust(data: ManualAdjustForm) {
  return request({
    url: '/wallet/manual-adjust',
    method: 'post',
    data
  });
}
```

- [ ] **Step 3: Create page**

Create a simple operational form with:

- member id input
- currency selector `GC/SC/RC`
- amount input
- strategy segmented radio:
  - `IMMEDIATE`: 无流水，立即到账
  - `AFTER_TURNOVER`: 需要流水
  - `MANUAL_REVIEW`: 人工审核
- required turnover input, visible only for `AFTER_TURNOVER`
- reason textarea
- submit button

Validation:

```ts
memberId: [{ required: true, message: tt('请输入会员ID'), trigger: 'blur' }],
currencyCode: [{ required: true, message: tt('请选择币种'), trigger: 'change' }],
amount: [{ required: true, message: tt('请输入调账金额'), trigger: 'blur' }],
strategy: [{ required: true, message: tt('请选择资金策略'), trigger: 'change' }],
reason: [{ required: true, message: tt('请输入调账原因'), trigger: 'blur' }]
```

Before submit, if `strategy === 'AFTER_TURNOVER'`, require `requiredTurnover > 0`.

- [ ] **Step 4: Add i18n text mappings**

Add mappings:

```ts
人工调账: 'Manual Adjustment',
调账金额: 'Adjustment Amount',
资金策略: 'Fund Strategy',
无流水，立即到账: 'No Turnover, Immediate',
需要流水: 'Require Turnover',
人工审核: 'Manual Review',
请输入调账金额: 'Please enter adjustment amount',
请选择资金策略: 'Please select fund strategy',
请输入调账原因: 'Please enter adjustment reason',
调账成功: 'Adjustment submitted successfully'
```

- [ ] **Step 5: Run admin build**

Run:

```powershell
pnpm --dir admin-ui check:i18n
pnpm --dir admin-ui build:dev
```

Expected: both pass.

- [ ] **Step 6: Commit**

```powershell
git add admin-ui/src/api/wallet/manualAdjust/types.ts admin-ui/src/api/wallet/manualAdjust/index.ts `
  admin-ui/src/views/wallet/manual-adjust/index.vue admin-ui/src/utils/i18nText.ts
git commit -m "feat(admin): add manual adjustment page"
```

---

## Task 7: Full Verification

**Files:**
- No source changes unless verification exposes a defect.

- [ ] **Step 1: Run backend focused tests**

```powershell
cd backend
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-modules/gameluck-wallet -Dtest=WalletRuleServiceImplTest,WalletCoreServiceImplTest,WalletManualAdjustServiceImplTest test -Plocal
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 2: Run backend compile**

```powershell
cd backend
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-admin -am compile -Plocal -DskipTests
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 3: Run frontend tests and build**

```powershell
pnpm --dir admin-ui exec vitest run src/utils/businessLabels.test.ts
pnpm --dir admin-ui check:i18n
pnpm --dir admin-ui build:dev
```

Expected: all pass. Existing chunk-size warnings are acceptable.

- [ ] **Step 4: Check worktree**

```powershell
git status --short
```

Expected: only intentional changes remain. Do not commit `admin-ui/.eslintrc-auto-import.json` if it changes due to build generation.

- [ ] **Step 5: If verification exposes a defect, fix it and create a narrowly scoped commit**

Use `git status --short` to list changed files. Add only the files changed for that defect, then commit with:

```powershell
git commit -m "fix(wallet): complete rule template verification"
```

Do not commit `admin-ui/.eslintrc-auto-import.json` if it appears only because of frontend build generation.

---

## Self-Review

Spec coverage:

- Default wallet rule templates: Task 1 and Task 2.
- One-click missing-rule completion without overwrite: Task 1 service and Task 4 UI.
- Source type canonicalization and duplicate dropdown cleanup: Task 1 and Task 3.
- Operator-friendly wallet rule form: Task 4.
- Manual adjustment strategy per operation: Task 5 and Task 6.
- Chinese and English copy: Task 2, Task 4, Task 6.
- No historical ledger rewrite: Task 1 canonicalization is display/query support only; no task updates old rows.
- Validation and permissions: Task 1 controller permission, Task 2 SQL permission, Task 5 controller permission.

Known boundary:

- The plan does not add a generic rule engine, strategy versioning, or approval workflow. That is intentionally outside this stage.
