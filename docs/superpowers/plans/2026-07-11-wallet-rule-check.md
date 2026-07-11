# Wallet Rule Check Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add wallet rule checks to promotion reward configuration so operators can see missing reward funding rules before enabling an activity, while H5 users receive friendly unavailable messages.

**Architecture:** Reuse the existing promotion and wallet modules. Promotion owns the activity-to-wallet-source mapping and exposes a check endpoint for Admin UI. Wallet rule resolution stays inside wallet services, and promotion enable/save operations perform backend enforcement before setting `ACTIVE`.

**Tech Stack:** Spring Boot Java, MyBatis Plus, Vue 3 Admin UI, Element Plus, H5 Vue/Vite, MySQL-backed wallet rules.

---

## File Map

- Modify `backend/gameluck-modules/gameluck-promotion/pom.xml`
  - Already depends on wallet; keep wallet dependency for rule checks.
- Create `backend/gameluck-modules/gameluck-promotion/src/main/java/com/gameluck/promotion/domain/bo/PromotionWalletRuleCheckBo.java`
  - Request body for the Admin UI check endpoint.
- Create `backend/gameluck-modules/gameluck-promotion/src/main/java/com/gameluck/promotion/domain/vo/PromotionWalletRuleCheckVo.java`
  - Per-reward-item check result returned to Admin UI.
- Modify `backend/gameluck-modules/gameluck-promotion/src/main/java/com/gameluck/promotion/service/IPromotionRewardService.java`
  - Add wallet rule check contract.
- Modify `backend/gameluck-modules/gameluck-promotion/src/main/java/com/gameluck/promotion/service/impl/PromotionRewardServiceImpl.java`
  - Add check implementation, activity source mapping, and enable/save enforcement.
- Modify `backend/gameluck-modules/gameluck-promotion/src/main/java/com/gameluck/promotion/controller/PromotionRewardController.java`
  - Add `/promotion/reward/wallet-rule/check` endpoint.
- Modify `backend/gameluck-admin/src/main/resources/i18n/messages.properties`
- Modify `backend/gameluck-admin/src/main/resources/i18n/messages_zh_CN.properties`
- Modify `backend/gameluck-admin/src/main/resources/i18n/messages_en_US.properties`
  - Add backend business messages.
- Modify `backend/gameluck-modules/gameluck-promotion/src/test/java/com/gameluck/promotion/service/impl/PromotionRewardServiceImplTest.java`
  - Add focused rule-check and enable-blocking tests.
- Modify `admin-ui/src/api/promotion/reward/types.ts`
  - Add check request/result types.
- Modify `admin-ui/src/api/promotion/reward/index.ts`
  - Add API client function.
- Modify `admin-ui/src/views/promotion/reward/index.vue`
  - Add wallet rule check panel, loading state, and enable guard text.
- Modify `admin-ui/src/lang/zh_CN.ts`
- Modify `admin-ui/src/lang/en_US.ts`
  - Add Admin UI i18n text.
- Modify `h5/src/i18n/messages.ts`
  - Add friendly reward unavailable copy if not already present.
- Modify `h5/src/views/PromotionsView.vue`
  - Map wallet rule backend errors to friendly player-facing copy.

---

### Task 1: Backend Check Types And Service Contract

**Files:**
- Create: `backend/gameluck-modules/gameluck-promotion/src/main/java/com/gameluck/promotion/domain/bo/PromotionWalletRuleCheckBo.java`
- Create: `backend/gameluck-modules/gameluck-promotion/src/main/java/com/gameluck/promotion/domain/vo/PromotionWalletRuleCheckVo.java`
- Modify: `backend/gameluck-modules/gameluck-promotion/src/main/java/com/gameluck/promotion/service/IPromotionRewardService.java`

- [ ] **Step 1: Add request BO**

Create `PromotionWalletRuleCheckBo.java`:

```java
package com.gameluck.promotion.domain.bo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class PromotionWalletRuleCheckBo {

    private String promotionType;

    private String currencyCode;

    private BigDecimal rewardAmount;

    private List<PromotionRewardItemBo> rewardItems;
}
```

- [ ] **Step 2: Add response VO**

Create `PromotionWalletRuleCheckVo.java`:

```java
package com.gameluck.promotion.domain.vo;

import lombok.Data;

@Data
public class PromotionWalletRuleCheckVo {

    private String currencyCode;

    private String sourceType;

    private String status;

    private String message;

    private String ruleName;

    private String releaseMode;

    private String creditEnabled;
}
```

- [ ] **Step 3: Add service contract**

In `IPromotionRewardService.java`, add imports:

```java
import com.gameluck.promotion.domain.bo.PromotionWalletRuleCheckBo;
import com.gameluck.promotion.domain.vo.PromotionWalletRuleCheckVo;
```

Add method:

```java
List<PromotionWalletRuleCheckVo> checkWalletRules(PromotionWalletRuleCheckBo bo);
```

- [ ] **Step 4: Run compile check**

Run:

```powershell
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-modules/gameluck-promotion -am -Plocal -DskipTests compile
```

Expected: compile fails because implementation and controller are not added yet, or passes if the interface has no compiling consumers. Continue to Task 2.

---

### Task 2: Backend Rule Check Implementation And Enforcement

**Files:**
- Modify: `backend/gameluck-modules/gameluck-promotion/src/main/java/com/gameluck/promotion/service/impl/PromotionRewardServiceImpl.java`
- Modify: `backend/gameluck-admin/src/main/resources/i18n/messages.properties`
- Modify: `backend/gameluck-admin/src/main/resources/i18n/messages_zh_CN.properties`
- Modify: `backend/gameluck-admin/src/main/resources/i18n/messages_en_US.properties`
- Test: `backend/gameluck-modules/gameluck-promotion/src/test/java/com/gameluck/promotion/service/impl/PromotionRewardServiceImplTest.java`

- [ ] **Step 1: Write failing tests**

In `PromotionRewardServiceImplTest.java`, add imports:

```java
import com.gameluck.promotion.domain.bo.PromotionWalletRuleCheckBo;
import com.gameluck.promotion.domain.vo.PromotionWalletRuleCheckVo;
import com.gameluck.wallet.domain.vo.WalletRuleVo;
import com.gameluck.wallet.service.IWalletRuleService;
```

Update service helper signatures to include `IWalletRuleService`:

```java
private PromotionRewardServiceImpl service(PromotionRewardMapper rewardMapper, PromotionClaimMapper claimMapper,
                                           IWalletCoreService walletCoreService) {
    return service(rewardMapper, claimMapper, walletCoreService, mock(MemberProfileMapper.class), mock(IWalletRuleService.class));
}

private PromotionRewardServiceImpl service(PromotionRewardMapper rewardMapper, PromotionClaimMapper claimMapper,
                                           IWalletCoreService walletCoreService, MemberProfileMapper memberProfileMapper) {
    return service(rewardMapper, claimMapper, walletCoreService, memberProfileMapper, mock(IWalletRuleService.class));
}

private PromotionRewardServiceImpl service(PromotionRewardMapper rewardMapper, PromotionClaimMapper claimMapper,
                                           IWalletCoreService walletCoreService, MemberProfileMapper memberProfileMapper,
                                           IWalletRuleService walletRuleService) {
    return new PromotionRewardServiceImpl(rewardMapper, claimMapper, walletCoreService, memberProfileMapper, walletRuleService);
}
```

Add tests:

```java
@Test
@Tag("local")
void checkWalletRulesReportsMissingDailyRewardRule() {
    PromotionRewardMapper rewardMapper = mock(PromotionRewardMapper.class);
    PromotionClaimMapper claimMapper = mock(PromotionClaimMapper.class);
    IWalletCoreService walletCoreService = mock(IWalletCoreService.class);
    IWalletRuleService walletRuleService = mock(IWalletRuleService.class);
    PromotionRewardServiceImpl service = service(rewardMapper, claimMapper, walletCoreService, mock(MemberProfileMapper.class), walletRuleService);

    PromotionRewardItemBo gc = new PromotionRewardItemBo();
    gc.setCurrencyCode("GC");
    gc.setRewardAmount(new BigDecimal("100.000000"));
    PromotionRewardItemBo sc = new PromotionRewardItemBo();
    sc.setCurrencyCode("SC");
    sc.setRewardAmount(new BigDecimal("1.000000"));
    PromotionWalletRuleCheckBo bo = new PromotionWalletRuleCheckBo();
    bo.setPromotionType("DAILY_LOGIN");
    bo.setRewardItems(List.of(gc, sc));

    WalletRuleVo gcRule = new WalletRuleVo();
    gcRule.setCurrencyCode("GC");
    gcRule.setSourceType("DAILY_REWARD");
    gcRule.setRuleName("GC daily reward");
    gcRule.setStatus("0");
    gcRule.setCreditEnabled("0");
    when(walletRuleService.resolveCreditRule("000000", "GC", "DAILY_REWARD")).thenReturn(gcRule);
    when(walletRuleService.resolveCreditRule("000000", "SC", "DAILY_REWARD")).thenReturn(null);

    List<PromotionWalletRuleCheckVo> result = service.checkWalletRules(bo);

    assertEquals(2, result.size());
    assertEquals("READY", result.get(0).getStatus());
    assertEquals("MISSING", result.get(1).getStatus());
    assertEquals("SC", result.get(1).getCurrencyCode());
}

@Test
@Tag("local")
void enablingActiveDailyRewardFailsWhenWalletRuleMissing() {
    PromotionRewardMapper rewardMapper = mock(PromotionRewardMapper.class);
    PromotionClaimMapper claimMapper = mock(PromotionClaimMapper.class);
    IWalletCoreService walletCoreService = mock(IWalletCoreService.class);
    IWalletRuleService walletRuleService = mock(IWalletRuleService.class);
    PromotionRewardServiceImpl service = service(rewardMapper, claimMapper, walletCoreService, mock(MemberProfileMapper.class), walletRuleService);

    PromotionReward reward = activeReward();
    reward.setPromotionType("DAILY_LOGIN");
    reward.setRewardItems("[{\"currencyCode\":\"SC\",\"rewardAmount\":\"1.000000\"}]");
    when(rewardMapper.selectByIdForUpdate(10L)).thenReturn(reward);
    when(walletRuleService.resolveCreditRule("000000", "SC", "DAILY_REWARD")).thenReturn(null);

    ServiceException exception = assertThrows(ServiceException.class, () -> service.updateStatus(10L, "ACTIVE"));

    assertEquals("promotion.wallet.rule.check.failed", exception.getMessage());
    verify(rewardMapper, never()).updateById(any(PromotionReward.class));
}
```

- [ ] **Step 2: Run failing tests**

Run:

```powershell
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-modules/gameluck-promotion -am -Plocal -DskipTests=false "-Dtest=PromotionRewardServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected: FAIL because new types/methods/constructor are incomplete.

- [ ] **Step 3: Inject wallet rule service**

In `PromotionRewardServiceImpl.java`, add imports:

```java
import com.gameluck.promotion.domain.bo.PromotionWalletRuleCheckBo;
import com.gameluck.promotion.domain.vo.PromotionWalletRuleCheckVo;
import com.gameluck.wallet.domain.vo.WalletRuleVo;
import com.gameluck.wallet.service.IWalletRuleService;
```

Add field after `walletCoreService`:

```java
private final IWalletRuleService walletRuleService;
```

- [ ] **Step 4: Add constants and public check method**

Add constants:

```java
private static final String RULE_READY = "READY";
private static final String RULE_MISSING = "MISSING";
private static final String RULE_INACTIVE = "INACTIVE";
private static final String RULE_CREDIT_DISABLED = "CREDIT_DISABLED";
```

Add method:

```java
@Override
public List<PromotionWalletRuleCheckVo> checkWalletRules(PromotionWalletRuleCheckBo bo) {
    String tenantId = currentTenantId();
    String sourceType = walletSourceType(bo.getPromotionType());
    return checkWalletRules(tenantId, sourceType, rewardItemsForCheck(bo));
}
```

Add helpers:

```java
private String walletSourceType(String promotionType) {
    return DAILY_LOGIN_TYPE.equals(promotionType) ? DAILY_REWARD_SOURCE : SOURCE_TYPE;
}

private List<PromotionRewardItemBo> rewardItemsForCheck(PromotionWalletRuleCheckBo bo) {
    if (DAILY_LOGIN_TYPE.equals(bo.getPromotionType())) {
        return bo.getRewardItems() == null ? List.of() : bo.getRewardItems();
    }
    PromotionRewardItemBo item = new PromotionRewardItemBo();
    item.setCurrencyCode(StringUtils.blankToDefault(bo.getCurrencyCode(), DEFAULT_CURRENCY));
    item.setRewardAmount(bo.getRewardAmount());
    return List.of(item);
}

private List<PromotionWalletRuleCheckVo> checkWalletRules(String tenantId, String sourceType, List<PromotionRewardItemBo> items) {
    return items.stream()
        .filter(item -> item != null && StringUtils.isNotBlank(item.getCurrencyCode()))
        .map(item -> checkWalletRule(tenantId, sourceType, item.getCurrencyCode()))
        .toList();
}

private PromotionWalletRuleCheckVo checkWalletRule(String tenantId, String sourceType, String currencyCode) {
    PromotionWalletRuleCheckVo vo = new PromotionWalletRuleCheckVo();
    vo.setCurrencyCode(currencyCode);
    vo.setSourceType(sourceType);
    WalletRuleVo rule = walletRuleService.resolveCreditRule(tenantId, currencyCode, sourceType);
    if (rule == null) {
        vo.setStatus(RULE_MISSING);
        vo.setMessage(MessageUtils.message("promotion.wallet.rule.missing"));
        return vo;
    }
    vo.setRuleName(rule.getRuleName());
    vo.setReleaseMode(rule.getReleaseMode());
    vo.setCreditEnabled(rule.getCreditEnabled());
    if (!"0".equals(rule.getStatus())) {
        vo.setStatus(RULE_INACTIVE);
        vo.setMessage(MessageUtils.message("promotion.wallet.rule.inactive"));
        return vo;
    }
    if (!"0".equals(rule.getCreditEnabled())) {
        vo.setStatus(RULE_CREDIT_DISABLED);
        vo.setMessage(MessageUtils.message("promotion.wallet.rule.credit.disabled"));
        return vo;
    }
    vo.setStatus(RULE_READY);
    vo.setMessage(MessageUtils.message("promotion.wallet.rule.ready"));
    return vo;
}
```

- [ ] **Step 5: Add enable enforcement**

In `insertByBo`, after `validateRewardStatus(add.getStatus());`, add:

```java
validateWalletRulesWhenActive(add);
```

In `updateByBo`, before `return rewardMapper.updateById(update) > 0;`, add:

```java
String nextStatus = StringUtils.blankToDefault(update.getStatus(), reward.getStatus());
if (PromotionRewardStatus.ACTIVE.name().equals(nextStatus)) {
    PromotionReward merged = BeanUtil.toBean(reward, PromotionReward.class);
    BeanUtil.copyProperties(update, merged, false);
    validateWalletRulesWhenActive(merged);
}
```

In `updateStatus`, after `validateRewardStatus(status);` and `PromotionReward reward = lockReward(id);`, add:

```java
reward.setStatus(status);
validateWalletRulesWhenActive(reward);
```

Keep the existing update block, but remove any duplicate `reward.setStatus(status)` if necessary.

Add helpers:

```java
private void validateWalletRulesWhenActive(PromotionReward reward) {
    if (!PromotionRewardStatus.ACTIVE.name().equals(reward.getStatus())) {
        return;
    }
    String sourceType = walletSourceType(reward.getPromotionType());
    List<PromotionWalletRuleCheckVo> checks = checkWalletRules(currentTenantId(), sourceType, rewardItems(reward));
    boolean failed = checks.stream().anyMatch(check -> !RULE_READY.equals(check.getStatus()));
    if (failed) {
        throw new ServiceException(MessageUtils.message("promotion.wallet.rule.check.failed"));
    }
}
```

- [ ] **Step 6: Add i18n messages**

Add to `messages.properties` and `messages_zh_CN.properties`:

```properties
promotion.wallet.rule.ready=钱包规则可发放
promotion.wallet.rule.missing=钱包规则不存在
promotion.wallet.rule.inactive=钱包规则已停用
promotion.wallet.rule.credit.disabled=钱包规则禁止入账
promotion.wallet.rule.check.failed=请先补齐钱包规则后再启用活动
```

Add to `messages_en_US.properties`:

```properties
promotion.wallet.rule.ready=Wallet rule is ready.
promotion.wallet.rule.missing=Wallet rule does not exist.
promotion.wallet.rule.inactive=Wallet rule is inactive.
promotion.wallet.rule.credit.disabled=Wallet rule does not allow credit.
promotion.wallet.rule.check.failed=Complete wallet rules before enabling the promotion.
```

- [ ] **Step 7: Run tests**

Run:

```powershell
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-modules/gameluck-promotion -am -Plocal -DskipTests=false "-Dtest=PromotionRewardServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected: PASS.

- [ ] **Step 8: Commit backend service changes**

Run:

```powershell
git add backend/gameluck-modules/gameluck-promotion backend/gameluck-admin/src/main/resources/i18n
git commit -m "feat(promotion): enforce wallet rule checks"
```

---

### Task 3: Backend Admin Check Endpoint

**Files:**
- Modify: `backend/gameluck-modules/gameluck-promotion/src/main/java/com/gameluck/promotion/controller/PromotionRewardController.java`

- [ ] **Step 1: Add imports**

```java
import com.gameluck.promotion.domain.bo.PromotionWalletRuleCheckBo;
import com.gameluck.promotion.domain.vo.PromotionWalletRuleCheckVo;
import java.util.List;
```

- [ ] **Step 2: Add endpoint**

Add before the existing `claim` endpoint:

```java
@SaCheckPermission("promotion:reward:query")
@PostMapping("/wallet-rule/check")
public R<List<PromotionWalletRuleCheckVo>> checkWalletRules(@RequestBody PromotionWalletRuleCheckBo bo) {
    return R.ok(promotionRewardService.checkWalletRules(bo));
}
```

- [ ] **Step 3: Compile backend**

Run:

```powershell
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-admin -am compile -Plocal -DskipTests
```

Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit endpoint**

Run:

```powershell
git add backend/gameluck-modules/gameluck-promotion/src/main/java/com/gameluck/promotion/controller/PromotionRewardController.java
git commit -m "feat(promotion): expose wallet rule check endpoint"
```

---

### Task 4: Admin UI Wallet Rule Check Panel

**Files:**
- Modify: `admin-ui/src/api/promotion/reward/types.ts`
- Modify: `admin-ui/src/api/promotion/reward/index.ts`
- Modify: `admin-ui/src/views/promotion/reward/index.vue`
- Modify: `admin-ui/src/lang/zh_CN.ts`
- Modify: `admin-ui/src/lang/en_US.ts`

- [ ] **Step 1: Add frontend types**

In `types.ts`, add:

```ts
export interface PromotionWalletRuleCheckForm {
  promotionType?: string;
  currencyCode?: string;
  rewardAmount?: number;
  rewardItems?: PromotionRewardItem[] | string;
}

export interface PromotionWalletRuleCheckVO {
  currencyCode: string;
  sourceType: string;
  status: 'READY' | 'MISSING' | 'INACTIVE' | 'CREDIT_DISABLED';
  message: string;
  ruleName?: string;
  releaseMode?: string;
  creditEnabled?: string;
}
```

- [ ] **Step 2: Add API function**

In `index.ts`, extend import and add:

```ts
import {
  PromotionClaimForm,
  PromotionClaimQuery,
  PromotionClaimVO,
  PromotionRewardForm,
  PromotionRewardQuery,
  PromotionRewardVO,
  PromotionWalletRuleCheckForm,
  PromotionWalletRuleCheckVO
} from './types';
```

Add function:

```ts
export function checkPromotionWalletRules(data: PromotionWalletRuleCheckForm): AxiosPromise<PromotionWalletRuleCheckVO[]> {
  return request({
    url: '/promotion/reward/wallet-rule/check',
    method: 'post',
    data
  });
}
```

- [ ] **Step 3: Add i18n text**

In `zh_CN.ts` under `promotionReward`, add:

```ts
walletRuleCheck: {
  title: '钱包规则体检',
  subtitle: '启用活动前必须保证每个奖励项都有可入账的钱包规则',
  sourceType: '来源类型',
  status: '规则状态',
  empty: '配置奖励后自动检查钱包规则',
  loading: '正在检查钱包规则',
  goRule: '去配置钱包规则',
  enableBlocked: '请先补齐钱包规则后再启用活动',
  statuses: {
    READY: '可发放',
    MISSING: '规则不存在',
    INACTIVE: '规则已停用',
    CREDIT_DISABLED: '禁止入账'
  }
}
```

In `en_US.ts` under `promotionReward`, add equivalent:

```ts
walletRuleCheck: {
  title: 'Wallet Rule Check',
  subtitle: 'Each reward item needs an enabled wallet rule that allows credit before activation.',
  sourceType: 'Source Type',
  status: 'Rule Status',
  empty: 'Wallet rules are checked after reward items are configured.',
  loading: 'Checking wallet rules',
  goRule: 'Configure wallet rules',
  enableBlocked: 'Complete wallet rules before enabling this promotion.',
  statuses: {
    READY: 'Ready',
    MISSING: 'Missing',
    INACTIVE: 'Inactive',
    CREDIT_DISABLED: 'Credit disabled'
  }
}
```

- [ ] **Step 4: Add UI state and API import**

In `index.vue`, import `checkPromotionWalletRules` and `PromotionWalletRuleCheckVO`.

Add state:

```ts
const walletRuleChecks = ref<PromotionWalletRuleCheckVO[]>([]);
const walletRuleChecking = ref(false);
const walletRuleCheckFailed = computed(() => walletRuleChecks.value.some((item) => item.status !== 'READY'));
```

Add helpers:

```ts
const walletRuleStatusType = (status: string) => {
  if (status === 'READY') return 'success';
  if (status === 'MISSING') return 'danger';
  return 'warning';
};

const walletRuleStatusLabel = (status: string) => {
  return t(`promotionReward.walletRuleCheck.statuses.${status}`) || status;
};

const checkWalletRules = async () => {
  const payload = normalizePayload();
  walletRuleChecking.value = true;
  try {
    const res = await checkPromotionWalletRules({
      promotionType: payload.promotionType,
      currencyCode: payload.currencyCode,
      rewardAmount: payload.rewardAmount,
      rewardItems: payload.rewardItems
    });
    walletRuleChecks.value = res.data || [];
  } finally {
    walletRuleChecking.value = false;
  }
};

const openWalletRulePage = () => {
  proxy?.$tab.openPage(t('route.walletRules'), '/wallet/rule');
};
```

- [ ] **Step 5: Add form panel**

In the reward dialog form, after reward amount/reward items and before status, add:

```vue
<el-form-item :label="t('promotionReward.walletRuleCheck.title')">
  <div class="wallet-rule-check">
    <div class="wallet-rule-check__header">
      <span>{{ t('promotionReward.walletRuleCheck.subtitle') }}</span>
      <el-button link type="primary" :loading="walletRuleChecking" @click="checkWalletRules">{{ t('common.refresh') }}</el-button>
    </div>
    <el-empty v-if="!walletRuleChecking && !walletRuleChecks.length" :description="t('promotionReward.walletRuleCheck.empty')" :image-size="48" />
    <el-skeleton v-else-if="walletRuleChecking" animated :rows="2" />
    <div v-else class="wallet-rule-check__list">
      <div v-for="item in walletRuleChecks" :key="`${item.currencyCode}-${item.sourceType}`" class="wallet-rule-check__row">
        <span class="wallet-rule-check__coin">{{ item.currencyCode }}</span>
        <span class="wallet-rule-check__source">{{ item.sourceType }}</span>
        <el-tag :type="walletRuleStatusType(item.status)">{{ walletRuleStatusLabel(item.status) }}</el-tag>
        <span class="wallet-rule-check__message">{{ item.message }}</span>
        <el-button v-if="item.status !== 'READY'" link type="primary" @click="openWalletRulePage">
          {{ t('promotionReward.walletRuleCheck.goRule') }}
        </el-button>
      </div>
    </div>
    <el-alert
      v-if="walletRuleCheckFailed && form.status === 'ACTIVE'"
      class="mt-2"
      type="warning"
      :closable="false"
      :title="t('promotionReward.walletRuleCheck.enableBlocked')"
    />
  </div>
</el-form-item>
```

Add scoped CSS:

```scss
.wallet-rule-check {
  width: 100%;
}

.wallet-rule-check__header,
.wallet-rule-check__row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.wallet-rule-check__header {
  justify-content: space-between;
  margin-bottom: 8px;
  color: #606266;
}

.wallet-rule-check__list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.wallet-rule-check__row {
  min-height: 34px;
  padding: 6px 8px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 6px;
}

.wallet-rule-check__coin {
  width: 42px;
  font-weight: 600;
}

.wallet-rule-check__source {
  min-width: 120px;
  color: #606266;
}

.wallet-rule-check__message {
  flex: 1;
  min-width: 120px;
}
```

- [ ] **Step 6: Wire automatic checks**

In `handleAdd`, after dialog is opened:

```ts
walletRuleChecks.value = [];
nextTick(() => checkWalletRules());
```

In `handleUpdate`, after `dialog.visible = true;`:

```ts
nextTick(() => checkWalletRules());
```

Add watcher:

```ts
watch(
  () => [form.value.promotionType, form.value.currencyCode, form.value.rewardAmount, JSON.stringify(form.value.rewardItems || [])],
  () => {
    if (dialog.visible) {
      checkWalletRules();
    }
  }
);
```

- [ ] **Step 7: Guard active submit**

In `submitForm`, before calling `addPromotionReward` or `updatePromotionReward`, add:

```ts
await checkWalletRules();
if (payload.status === 'ACTIVE' && walletRuleCheckFailed.value) {
  proxy?.$modal.msgError(t('promotionReward.walletRuleCheck.enableBlocked'));
  return;
}
```

- [ ] **Step 8: Run frontend checks**

Run:

```powershell
pnpm --dir admin-ui check:i18n
$env:NODE_OPTIONS='--max-old-space-size=4096'; pnpm --dir admin-ui build:dev
```

Expected: both pass.

- [ ] **Step 9: Commit Admin UI**

Run:

```powershell
git add admin-ui/src/api/promotion/reward admin-ui/src/views/promotion/reward/index.vue admin-ui/src/lang/zh_CN.ts admin-ui/src/lang/en_US.ts
git commit -m "feat(admin): show promotion wallet rule checks"
```

---

### Task 5: H5 Friendly Wallet Rule Error

**Files:**
- Modify: `h5/src/i18n/messages.ts`
- Modify: `h5/src/views/PromotionsView.vue`

- [ ] **Step 1: Confirm current H5 error handling**

Run:

```powershell
rg -n "dailyRewardUnavailable|error|claim|toast|message" h5/src/views h5/src/api h5/src/i18n/messages.ts
```

Expected: claim failure display logic is in `h5/src/views/PromotionsView.vue`, using `error.value` in `claimDailyReward()` and `claim()`.

- [ ] **Step 2: Add i18n copy**

In `messages.ts`, add keys if missing:

```ts
rewardTemporarilyUnavailable: '奖励暂不可领取，请稍后再试',
```

For English:

```ts
rewardTemporarilyUnavailable: 'Reward is temporarily unavailable. Please try again later.',
```

- [ ] **Step 3: Map backend wallet-rule failures to friendly copy**

In `PromotionsView.vue`, after the `success` ref, add:

```ts
const isWalletRuleError = (message?: string) => {
  if (!message) return false;
  return message.includes('钱包规则') || message.includes('Wallet rule') || message.includes('wallet rule');
};

const friendlyClaimError = (err: unknown, fallbackKey: 'dailyRewardClaimFailed' | 'promotionClaimFailed') => {
  const rawMessage = err instanceof Error ? err.message : '';
  if (isWalletRuleError(rawMessage)) {
    return t('rewardTemporarilyUnavailable');
  }
  return rawMessage || t(fallbackKey);
};
```

In `claimDailyReward()`, replace the catch block assignment with:

```ts
error.value = friendlyClaimError(err, 'dailyRewardClaimFailed');
```

In `claim(promotion)`, replace the catch block assignment with:

```ts
error.value = friendlyClaimError(err, 'promotionClaimFailed');
```

In `messages.ts`, add `promotionClaimFailed` if missing:

```ts
promotionClaimFailed: '领取失败',
```

For English:

```ts
promotionClaimFailed: 'Claim failed',
```

- [ ] **Step 4: Build H5**

Run:

```powershell
npm --prefix h5 run build
```

Expected: build passes.

- [ ] **Step 5: Commit H5**

Run:

```powershell
git add h5/src/i18n/messages.ts h5/src/views/PromotionsView.vue
git commit -m "fix(h5): hide wallet rule errors from players"
```

---

### Task 6: Final Verification And Package

**Files:**
- No source edits expected unless verification finds a defect.

- [ ] **Step 1: Check worktree**

Run:

```powershell
git status --short
```

Expected: only known generated noise may remain: `admin-ui/.eslintrc-auto-import.json`.

- [ ] **Step 2: Run backend tests**

Run:

```powershell
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-modules/gameluck-promotion -am -Plocal -DskipTests=false "-Dtest=PromotionRewardServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected: BUILD SUCCESS.

- [ ] **Step 3: Run backend compile/package**

Run:

```powershell
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-admin -am compile -Plocal -DskipTests
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-admin -am package -Plocal -DskipTests
```

Expected: BUILD SUCCESS for both.

- [ ] **Step 4: Run frontend builds**

Run:

```powershell
pnpm --dir admin-ui check:i18n
$env:NODE_OPTIONS='--max-old-space-size=4096'; pnpm --dir admin-ui build:dev
npm --prefix h5 run build
```

Expected: all pass.

- [ ] **Step 5: Runtime smoke**

Restart backend using the newly packaged jar:

```powershell
Get-Process java -ErrorAction SilentlyContinue | Where-Object { $_.Path -like '*java*' } | Select-Object Id,ProcessName,StartTime
```

Stop only the local backend process that is running `gameluck-admin.jar`, then start:

```powershell
Start-Process -FilePath java -ArgumentList @('-jar','gameluck-admin\target\gameluck-admin.jar','--spring.profiles.active=local') -WorkingDirectory backend -WindowStyle Hidden
```

Use Admin UI to verify:

- Open promotion reward edit dialog.
- Configure daily login reward with a currency/source that has no wallet rule.
- Confirm wallet rule check panel shows missing rule.
- Try to enable; expect backend error and no status change.
- Add or enable matching wallet rule.
- Reopen reward dialog; expect all rows ready.
- Enable reward; expect success.

- [ ] **Step 6: Final commit if verification required fixes**

If runtime smoke required a fix, add the exact files changed by that fix. For example, if the backend enforcement needed a correction:

```powershell
git add backend/gameluck-modules/gameluck-promotion/src/main/java/com/gameluck/promotion/service/impl/PromotionRewardServiceImpl.java
git commit -m "fix(promotion): complete wallet rule check flow"
```

If no fixes were required, do not create an empty commit.
