# Daily Login Reward Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a configurable daily login reward where B-side configures `GC 100 + SC 1`, H5 members claim it once per day, and wallet credits plus claim records are auditable.

**Architecture:** Reuse the existing `gameluck-promotion` module and extend `gl_promotion_reward/gl_promotion_claim` for reward type, daily cycle, claim date, and multi-currency reward snapshots. H5 calls a dedicated daily-login endpoint, while B-side extends the current promotion reward page instead of adding a new module. Wallet credits continue to go through `IWalletCoreService.credit` with source type `DAILY_REWARD`.

**Tech Stack:** Java 17, Spring Boot, MyBatis Plus, MySQL, Vue 3, Vite, Element Plus, existing GameLuck i18n and wallet service.

---

## File Map

- Modify: `backend/script/sql/gameluck_wallet.sql`
  - Add daily-login columns and seed default `GC 100 + SC 1` reward.
- Create: `backend/script/sql/gameluck_daily_login_reward.sql`
  - Guarded migration for existing local and deployed databases.
- Modify: `backend/gameluck-modules/gameluck-promotion/src/main/java/com/gameluck/promotion/domain/PromotionReward.java`
  - Add promotion type, claim cycle, daily limit, and reward items JSON fields.
- Modify: `backend/gameluck-modules/gameluck-promotion/src/main/java/com/gameluck/promotion/domain/PromotionClaim.java`
  - Add claim date, reward snapshot, and wallet transaction snapshot fields.
- Modify: `backend/gameluck-modules/gameluck-promotion/src/main/java/com/gameluck/promotion/domain/bo/PromotionRewardBo.java`
  - Accept promotion type and reward items.
- Modify: `backend/gameluck-modules/gameluck-promotion/src/main/java/com/gameluck/promotion/domain/vo/PromotionRewardVo.java`
  - Return promotion type and reward items.
- Modify: `backend/gameluck-modules/gameluck-promotion/src/main/java/com/gameluck/promotion/domain/vo/PromotionClaimVo.java`
  - Return claim date and reward snapshot fields.
- Create: `backend/gameluck-modules/gameluck-promotion/src/main/java/com/gameluck/promotion/domain/bo/PromotionRewardItemBo.java`
  - Form item for one currency reward row.
- Create: `backend/gameluck-modules/gameluck-promotion/src/main/java/com/gameluck/promotion/client/domain/vo/ClientDailyLoginRewardVo.java`
  - H5 daily login state response.
- Modify: `backend/gameluck-modules/gameluck-promotion/src/main/java/com/gameluck/promotion/mapper/PromotionRewardMapper.java`
  - Add active daily-login reward query.
- Modify: `backend/gameluck-modules/gameluck-promotion/src/main/resources/mapper/promotion/PromotionRewardMapper.xml`
  - Add active daily-login SQL and exclude daily-login rows from generic H5 list.
- Modify: `backend/gameluck-modules/gameluck-promotion/src/main/java/com/gameluck/promotion/mapper/PromotionClaimMapper.java`
  - Add daily claim lookup by claim date.
- Modify: `backend/gameluck-modules/gameluck-promotion/src/main/resources/mapper/promotion/PromotionClaimMapper.xml`
  - Add daily claim SQL.
- Modify: `backend/gameluck-modules/gameluck-promotion/src/main/java/com/gameluck/promotion/service/IPromotionRewardService.java`
  - Add `dailyLoginReward` and `claimDailyLoginReward`.
- Modify: `backend/gameluck-modules/gameluck-promotion/src/main/java/com/gameluck/promotion/service/impl/PromotionRewardServiceImpl.java`
  - Implement multi-currency reward parsing and daily once logic.
- Modify: `backend/gameluck-modules/gameluck-promotion/src/main/java/com/gameluck/promotion/client/controller/ClientPromotionController.java`
  - Add H5 daily-login endpoints.
- Modify: `backend/gameluck-modules/gameluck-promotion/src/test/java/com/gameluck/promotion/service/impl/PromotionRewardServiceImplTest.java`
  - Prove `GC 100 + SC 1` daily reward credits twice and duplicate claim does not credit again.
- Modify: `backend/gameluck-modules/gameluck-promotion/src/test/java/com/gameluck/promotion/client/service/ClientPromotionServiceTest.java`
  - Prove H5 daily state returns configured reward items.
- Modify: `h5/src/types/client.ts`
  - Add daily login reward response types.
- Modify: `h5/src/api/client.ts`
  - Add daily login reward API calls.
- Modify: `h5/src/i18n/messages.ts`
  - Add H5 Chinese and English daily reward copy.
- Modify: `h5/src/views/PromotionsView.vue`
  - Show top daily login reward block and claim button.
- Modify: `admin-ui/src/api/promotion/reward/types.ts`
  - Add promotion type and reward item fields.
- Modify: `admin-ui/src/views/promotion/reward/index.vue`
  - Add activity type filter/form and multi-currency reward editor.
- Modify: `admin-ui/src/lang/zh_CN.ts`
  - Add B-side daily login reward copy.
- Modify: `admin-ui/src/lang/en_US.ts`
  - Add English copy.
- Modify: `progress.md`
  - Record verification evidence.

---

## Task 1: Backend Daily Login Model And Tests

**Files:**
- Modify: `backend/gameluck-modules/gameluck-promotion/src/test/java/com/gameluck/promotion/service/impl/PromotionRewardServiceImplTest.java`
- Modify: `backend/gameluck-modules/gameluck-promotion/src/main/java/com/gameluck/promotion/domain/PromotionReward.java`
- Modify: `backend/gameluck-modules/gameluck-promotion/src/main/java/com/gameluck/promotion/domain/PromotionClaim.java`
- Create: `backend/gameluck-modules/gameluck-promotion/src/main/java/com/gameluck/promotion/domain/bo/PromotionRewardItemBo.java`

- [ ] **Step 1: Add failing service tests for daily multi-currency claim**

Add one test to `PromotionRewardServiceImplTest`:

```java
@Test
@Tag("local")
void dailyLoginRewardCreditsConfiguredGcAndScOncePerDay() {
    PromotionRewardMapper rewardMapper = mock(PromotionRewardMapper.class);
    PromotionClaimMapper claimMapper = mock(PromotionClaimMapper.class);
    IWalletCoreService walletCoreService = mock(IWalletCoreService.class);
    PromotionRewardServiceImpl service = new PromotionRewardServiceImpl(rewardMapper, claimMapper, walletCoreService);

    PromotionReward reward = activeReward();
    reward.setPromotionType("DAILY_LOGIN");
    reward.setClaimCycle("DAILY");
    reward.setDailyClaimLimit(1);
    reward.setRewardItems("[{\"currencyCode\":\"GC\",\"rewardAmount\":\"100.000000\"},{\"currencyCode\":\"SC\",\"rewardAmount\":\"1.000000\"}]");
    when(rewardMapper.selectActiveDailyLoginReward("000000")).thenReturn(reward);
    when(claimMapper.selectDailyClaim("000000", 10L, 1001L, java.time.LocalDate.now())).thenReturn(null);
    when(claimMapper.insert(any(PromotionClaim.class))).thenReturn(1);
    when(claimMapper.updateById(any(PromotionClaim.class))).thenReturn(1);

    WalletTransaction gc = new WalletTransaction();
    gc.setTransactionNo("WT_GC_DAILY");
    gc.setStatus(WalletTransactionStatus.SUCCESS.name());
    WalletTransaction sc = new WalletTransaction();
    sc.setTransactionNo("WT_SC_DAILY");
    sc.setStatus(WalletTransactionStatus.SUCCESS.name());
    when(walletCoreService.credit(any())).thenReturn(gc, sc);

    PromotionClaimVo claim = service.claimDailyLoginReward(1001L);

    assertEquals("SUCCESS", claim.getStatus());
    assertEquals("DAILY_LOGIN", claim.getPromotionType());
    assertEquals("WT_GC_DAILY,WT_SC_DAILY", claim.getWalletTransactionNo());
    ArgumentCaptor<WalletCreditBo> creditCaptor = ArgumentCaptor.forClass(WalletCreditBo.class);
    verify(walletCoreService, org.mockito.Mockito.times(2)).credit(creditCaptor.capture());
    assertEquals("GC", creditCaptor.getAllValues().get(0).getCurrencyCode());
    assertEquals(new BigDecimal("100.000000"), creditCaptor.getAllValues().get(0).getAmount());
    assertEquals("SC", creditCaptor.getAllValues().get(1).getCurrencyCode());
    assertEquals(new BigDecimal("1.000000"), creditCaptor.getAllValues().get(1).getAmount());
    assertEquals("DAILY_REWARD", creditCaptor.getAllValues().get(0).getSourceType());
}
```

Add another test:

```java
@Test
@Tag("local")
void dailyLoginRewardDuplicateReturnsExistingClaimWithoutCredit() {
    PromotionRewardMapper rewardMapper = mock(PromotionRewardMapper.class);
    PromotionClaimMapper claimMapper = mock(PromotionClaimMapper.class);
    IWalletCoreService walletCoreService = mock(IWalletCoreService.class);
    PromotionRewardServiceImpl service = new PromotionRewardServiceImpl(rewardMapper, claimMapper, walletCoreService);

    PromotionReward reward = activeReward();
    reward.setPromotionType("DAILY_LOGIN");
    reward.setClaimCycle("DAILY");
    when(rewardMapper.selectActiveDailyLoginReward("000000")).thenReturn(reward);
    PromotionClaim existing = new PromotionClaim();
    existing.setId(99L);
    existing.setPromotionId(10L);
    existing.setMemberId(1001L);
    existing.setStatus("SUCCESS");
    existing.setWalletTransactionNo("WT_EXISTING");
    existing.setClaimDate(java.time.LocalDate.now());
    when(claimMapper.selectDailyClaim("000000", 10L, 1001L, java.time.LocalDate.now())).thenReturn(existing);

    PromotionClaimVo claim = service.claimDailyLoginReward(1001L);

    assertEquals(99L, claim.getId());
    assertEquals("WT_EXISTING", claim.getWalletTransactionNo());
    verifyNoInteractions(walletCoreService);
}
```

- [ ] **Step 2: Run backend tests and verify failure**

Run:

```powershell
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-modules/gameluck-promotion -am -Plocal -DskipTests=false "-Dtest=PromotionRewardServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected: compilation fails because `claimDailyLoginReward`, `promotionType`, `claimCycle`, `dailyClaimLimit`, `rewardItems`, and `claimDate` do not exist yet.

- [ ] **Step 3: Add domain fields**

In `PromotionReward.java`, add:

```java
private String promotionType;

private String claimCycle;

private Integer dailyClaimLimit;

private String rewardItems;
```

In `PromotionClaim.java`, add:

```java
private String promotionType;

private java.time.LocalDate claimDate;

private String rewardSnapshot;
```

In `PromotionClaimVo.java`, add the same `promotionType`, `claimDate`, and `rewardSnapshot` fields.

- [ ] **Step 4: Add reward item BO**

Create `PromotionRewardItemBo.java`:

```java
package com.gameluck.promotion.domain.bo;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PromotionRewardItemBo {

    @NotBlank(message = "{promotion.reward.currency.required}")
    private String currencyCode;

    @NotNull(message = "{promotion.reward.amount.required}")
    @DecimalMin(value = "0.000001", message = "{promotion.reward.amount.positive}")
    private BigDecimal rewardAmount;
}
```

- [ ] **Step 5: Commit backend model test scaffold**

Run tests again after implementation in Task 2. Do not commit this task alone if compilation is still failing.

---

## Task 2: Backend Daily Claim Implementation

**Files:**
- Modify: `backend/gameluck-modules/gameluck-promotion/src/main/java/com/gameluck/promotion/domain/bo/PromotionRewardBo.java`
- Modify: `backend/gameluck-modules/gameluck-promotion/src/main/java/com/gameluck/promotion/domain/vo/PromotionRewardVo.java`
- Modify: `backend/gameluck-modules/gameluck-promotion/src/main/java/com/gameluck/promotion/mapper/PromotionRewardMapper.java`
- Modify: `backend/gameluck-modules/gameluck-promotion/src/main/resources/mapper/promotion/PromotionRewardMapper.xml`
- Modify: `backend/gameluck-modules/gameluck-promotion/src/main/java/com/gameluck/promotion/mapper/PromotionClaimMapper.java`
- Modify: `backend/gameluck-modules/gameluck-promotion/src/main/resources/mapper/promotion/PromotionClaimMapper.xml`
- Modify: `backend/gameluck-modules/gameluck-promotion/src/main/java/com/gameluck/promotion/service/IPromotionRewardService.java`
- Modify: `backend/gameluck-modules/gameluck-promotion/src/main/java/com/gameluck/promotion/service/impl/PromotionRewardServiceImpl.java`
- Modify: `backend/gameluck-modules/gameluck-promotion/src/test/java/com/gameluck/promotion/service/impl/PromotionRewardServiceImplTest.java`

- [ ] **Step 1: Extend BO and VO**

In `PromotionRewardBo.java`, add:

```java
private String promotionType;

private String claimCycle;

private Integer dailyClaimLimit;

private java.util.List<PromotionRewardItemBo> rewardItems;
```

In `PromotionRewardVo.java`, add:

```java
private String promotionType;

private String claimCycle;

private Integer dailyClaimLimit;

private String rewardItems;
```

- [ ] **Step 2: Add mapper methods**

In `PromotionRewardMapper.java`, add:

```java
PromotionReward selectActiveDailyLoginReward(@Param("tenantId") String tenantId);
```

In `PromotionRewardMapper.xml`, add:

```xml
<select id="selectActiveDailyLoginReward" resultType="com.gameluck.promotion.domain.PromotionReward">
    SELECT *
    FROM gl_promotion_reward
    WHERE tenant_id = #{tenantId}
      AND promotion_type = 'DAILY_LOGIN'
      AND status = 'ACTIVE'
      AND del_flag = '0'
      AND (start_time IS NULL OR start_time &lt;= NOW())
      AND (end_time IS NULL OR end_time &gt;= NOW())
    ORDER BY create_time DESC
    LIMIT 1
</select>
```

Also update `selectClientActiveRewards` to exclude daily login rows from the generic promotion list:

```sql
AND (promotion_type IS NULL OR promotion_type <> 'DAILY_LOGIN')
```

In `PromotionClaimMapper.java`, add:

```java
PromotionClaim selectDailyClaim(@Param("tenantId") String tenantId,
                                @Param("promotionId") Long promotionId,
                                @Param("memberId") Long memberId,
                                @Param("claimDate") java.time.LocalDate claimDate);
```

In `PromotionClaimMapper.xml`, add:

```xml
<select id="selectDailyClaim" resultType="com.gameluck.promotion.domain.PromotionClaim">
    SELECT *
    FROM gl_promotion_claim
    WHERE tenant_id = #{tenantId}
      AND promotion_id = #{promotionId}
      AND member_id = #{memberId}
      AND claim_date = #{claimDate}
      AND del_flag = '0'
    LIMIT 1
</select>
```

- [ ] **Step 3: Extend service interface**

In `IPromotionRewardService.java`, add:

```java
ClientDailyLoginRewardVo dailyLoginReward(Long memberId);

PromotionClaimVo claimDailyLoginReward(Long memberId);
```

Add import:

```java
import com.gameluck.promotion.client.domain.vo.ClientDailyLoginRewardVo;
```

- [ ] **Step 4: Implement reward item JSON helpers**

In `PromotionRewardServiceImpl.java`, add imports:

```java
import com.gameluck.common.json.utils.JsonUtils;
import com.gameluck.promotion.client.domain.vo.ClientDailyLoginRewardVo;
import com.gameluck.promotion.domain.bo.PromotionRewardItemBo;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Objects;
import java.util.stream.Collectors;
```

Add constants:

```java
private static final String DAILY_LOGIN_TYPE = "DAILY_LOGIN";
private static final String DAILY_CYCLE = "DAILY";
private static final String DAILY_REWARD_SOURCE = "DAILY_REWARD";
```

Add helper:

```java
private List<PromotionRewardItemBo> rewardItems(PromotionReward reward) {
    if (StringUtils.isNotBlank(reward.getRewardItems())) {
        List<PromotionRewardItemBo> parsed = JsonUtils.parseArray(reward.getRewardItems(), PromotionRewardItemBo.class);
        if (parsed != null && !parsed.isEmpty()) {
            return parsed.stream()
                .filter(item -> StringUtils.isNotBlank(item.getCurrencyCode()) && item.getRewardAmount() != null)
                .peek(item -> item.setRewardAmount(normalizePositive(item.getRewardAmount())))
                .toList();
        }
    }
    PromotionRewardItemBo item = new PromotionRewardItemBo();
    item.setCurrencyCode(reward.getCurrencyCode());
    item.setRewardAmount(reward.getRewardAmount());
    return List.of(item);
}
```

- [ ] **Step 5: Implement daily claim**

Add to `PromotionRewardServiceImpl.java`:

```java
@Override
@Transactional(rollbackFor = Exception.class)
public PromotionClaimVo claimDailyLoginReward(Long memberId) {
    String tenantId = currentTenantId();
    PromotionReward reward = rewardMapper.selectActiveDailyLoginReward(tenantId);
    if (reward == null) {
        throw new ServiceException(MessageUtils.message("promotion.daily.not.configured"));
    }
    validateClaimable(reward);
    LocalDate today = LocalDate.now();
    PromotionClaim existing = claimMapper.selectDailyClaim(tenantId, reward.getId(), memberId, today);
    if (existing != null) {
        return BeanUtil.toBean(existing, PromotionClaimVo.class);
    }

    List<PromotionRewardItemBo> items = rewardItems(reward);
    Date now = new Date();
    PromotionClaim claim = new PromotionClaim();
    claim.setId(IdUtil.getSnowflakeNextId());
    claim.setTenantId(tenantId);
    claim.setClaimNo("PC" + IdUtil.getSnowflakeNextIdStr());
    claim.setPromotionId(reward.getId());
    claim.setPromotionNo(reward.getPromotionNo());
    claim.setPromotionName(reward.getPromotionName());
    claim.setPromotionType(DAILY_LOGIN_TYPE);
    claim.setMemberId(memberId);
    claim.setCurrencyCode(items.get(0).getCurrencyCode());
    claim.setRewardAmount(items.get(0).getRewardAmount());
    claim.setClaimDate(today);
    claim.setRewardSnapshot(JsonUtils.toJsonString(items));
    claim.setStatus(PromotionClaimStatus.SUCCESS.name());
    claim.setIdempotencyKey("promotion:daily-login:" + tenantId + ":" + reward.getPromotionNo() + ":" + memberId + ":" + today);
    claim.setVersion(0);
    claim.setDelFlag(SystemConstants.NORMAL);
    claim.setCreateTime(now);
    claim.setUpdateTime(now);
    claimMapper.insert(claim);

    List<String> transactionNos = new ArrayList<>();
    for (PromotionRewardItemBo item : items) {
        WalletCreditBo creditBo = buildDailyCreditBo(claim, item);
        WalletTransaction transaction = walletCoreService.credit(creditBo);
        transactionNos.add(transaction.getTransactionNo());
        if (!WalletTransactionStatus.SUCCESS.name().equals(transaction.getStatus())) {
            claim.setStatus(PromotionClaimStatus.FAILED.name());
            claim.setFailReason(StringUtils.substring(transaction.getFailReason(), 0, 500));
            break;
        }
    }
    claim.setWalletTransactionNo(String.join(",", transactionNos));
    claim.setUpdateTime(new Date());
    claimMapper.updateById(claim);
    return BeanUtil.toBean(claim, PromotionClaimVo.class);
}
```

Add helper:

```java
private WalletCreditBo buildDailyCreditBo(PromotionClaim claim, PromotionRewardItemBo item) {
    WalletCreditBo bo = new WalletCreditBo();
    bo.setIdempotencyKey(claim.getIdempotencyKey() + ":" + item.getCurrencyCode());
    bo.setMemberId(claim.getMemberId());
    bo.setCurrencyCode(item.getCurrencyCode());
    bo.setSourceType(DAILY_REWARD_SOURCE);
    bo.setBusinessNo(claim.getClaimNo());
    bo.setAmount(item.getRewardAmount());
    bo.setRemark(MessageUtils.message("promotion.wallet.remark.daily.login"));
    return bo;
}
```

- [ ] **Step 6: Run backend tests**

Run:

```powershell
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-modules/gameluck-promotion -am -Plocal -DskipTests=false "-Dtest=PromotionRewardServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected: tests pass.

- [ ] **Step 7: Commit backend service**

```powershell
git add backend/gameluck-modules/gameluck-promotion/src/main/java/com/gameluck/promotion backend/gameluck-modules/gameluck-promotion/src/main/resources/mapper/promotion backend/gameluck-modules/gameluck-promotion/src/test/java/com/gameluck/promotion/service/impl/PromotionRewardServiceImplTest.java
git commit -m "feat(promotion): support daily login reward claims"
```

---

## Task 3: SQL Migration And Seed

**Files:**
- Modify: `backend/script/sql/gameluck_wallet.sql`
- Create: `backend/script/sql/gameluck_daily_login_reward.sql`

- [ ] **Step 1: Add guarded migration SQL**

Create `backend/script/sql/gameluck_daily_login_reward.sql` with guarded MySQL DDL:

```sql
SET @schema_name = DATABASE();

SET @sql = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE gl_promotion_reward ADD COLUMN promotion_type VARCHAR(64) NOT NULL DEFAULT ''GENERAL'' COMMENT ''Promotion type'' AFTER promotion_name',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'gl_promotion_reward' AND COLUMN_NAME = 'promotion_type'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE gl_promotion_reward ADD COLUMN claim_cycle VARCHAR(32) NOT NULL DEFAULT ''ONCE'' COMMENT ''Claim cycle'' AFTER reward_amount',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'gl_promotion_reward' AND COLUMN_NAME = 'claim_cycle'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE gl_promotion_reward ADD COLUMN daily_claim_limit INT NOT NULL DEFAULT 1 COMMENT ''Daily claim limit'' AFTER claim_cycle',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'gl_promotion_reward' AND COLUMN_NAME = 'daily_claim_limit'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE gl_promotion_reward ADD COLUMN reward_items JSON DEFAULT NULL COMMENT ''Reward item snapshot'' AFTER daily_claim_limit',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'gl_promotion_reward' AND COLUMN_NAME = 'reward_items'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE gl_promotion_claim ADD COLUMN promotion_type VARCHAR(64) DEFAULT NULL COMMENT ''Promotion type'' AFTER promotion_name',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'gl_promotion_claim' AND COLUMN_NAME = 'promotion_type'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE gl_promotion_claim ADD COLUMN claim_date DATE DEFAULT NULL COMMENT ''Claim date'' AFTER reward_amount',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'gl_promotion_claim' AND COLUMN_NAME = 'claim_date'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE gl_promotion_claim ADD COLUMN reward_snapshot JSON DEFAULT NULL COMMENT ''Reward snapshot'' AFTER claim_date',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'gl_promotion_claim' AND COLUMN_NAME = 'reward_snapshot'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

INSERT INTO gl_wallet_rule
(id, tenant_id, currency_code, source_type, rule_name, credit_enabled, debit_enabled, withdraw_enabled, exchange_enabled, release_mode, turnover_required, default_required_turnover, status, sort_order, remark, create_time)
VALUES
(1900000000000000611, '000000', 'GC', 'DAILY_REWARD', 'GC daily login reward', '0', '1', '1', '1', 'IMMEDIATE', '1', 0, '0', 11, 'Daily login GC reward.', NOW()),
(1900000000000000612, '000000', 'SC', 'DAILY_REWARD', 'SC daily login reward', '0', '1', '0', '1', 'IMMEDIATE', '1', 0, '0', 12, 'Daily login SC reward.', NOW())
ON DUPLICATE KEY UPDATE
  rule_name = VALUES(rule_name),
  credit_enabled = VALUES(credit_enabled),
  release_mode = VALUES(release_mode),
  status = VALUES(status),
  update_time = NOW();

INSERT INTO gl_promotion_reward
(id, tenant_id, promotion_no, promotion_name, promotion_type, currency_code, reward_amount, claim_cycle, daily_claim_limit, reward_items, status, start_time, end_time, remark, create_time)
VALUES
(1900000000000000901, '000000', 'PR-DAILY-LOGIN-DEFAULT', '每日登录奖励', 'DAILY_LOGIN', 'GC', 100.000000, 'DAILY', 1,
 JSON_ARRAY(JSON_OBJECT('currencyCode', 'GC', 'rewardAmount', '100.000000'), JSON_OBJECT('currencyCode', 'SC', 'rewardAmount', '1.000000')),
 'ACTIVE', NULL, NULL, 'Default configurable daily login reward.', NOW())
ON DUPLICATE KEY UPDATE
  promotion_name = VALUES(promotion_name),
  promotion_type = VALUES(promotion_type),
  reward_items = VALUES(reward_items),
  status = VALUES(status),
  update_time = NOW();
```

- [ ] **Step 2: Mirror DDL in `gameluck_wallet.sql`**

Update `CREATE TABLE gl_promotion_reward` and `CREATE TABLE gl_promotion_claim` with the same new columns.

Add the seed rows from Step 1 into the existing wallet SQL seed section.

- [ ] **Step 3: Import SQL locally**

Run:

```powershell
.\backend\script\bin\import-sql-utf8.ps1 -SqlPath backend\script\sql\gameluck_daily_login_reward.sql
```

Expected: import exits 0.

- [ ] **Step 4: Verify database**

Run:

```powershell
mysql --default-character-set=utf8mb4 -uroot -proot gameluck_vue -e "select promotion_no,promotion_type,reward_items,status from gl_promotion_reward where promotion_no='PR-DAILY-LOGIN-DEFAULT'; select currency_code,source_type,rule_name from gl_wallet_rule where source_type='DAILY_REWARD';"
```

Expected: one `DAILY_LOGIN` reward and two `DAILY_REWARD` wallet rules.

- [ ] **Step 5: Commit SQL**

```powershell
git add backend/script/sql/gameluck_wallet.sql backend/script/sql/gameluck_daily_login_reward.sql
git commit -m "chore(promotion): seed daily login reward"
```

---

## Task 4: H5 Daily Login Reward API And UI

**Files:**
- Create: `backend/gameluck-modules/gameluck-promotion/src/main/java/com/gameluck/promotion/client/domain/vo/ClientDailyLoginRewardVo.java`
- Modify: `backend/gameluck-modules/gameluck-promotion/src/main/java/com/gameluck/promotion/client/controller/ClientPromotionController.java`
- Modify: `backend/gameluck-modules/gameluck-promotion/src/test/java/com/gameluck/promotion/client/service/ClientPromotionServiceTest.java`
- Modify: `h5/src/types/client.ts`
- Modify: `h5/src/api/client.ts`
- Modify: `h5/src/i18n/messages.ts`
- Modify: `h5/src/views/PromotionsView.vue`

- [ ] **Step 1: Add backend VO**

Create `ClientDailyLoginRewardVo.java`:

```java
package com.gameluck.promotion.client.domain.vo;

import com.gameluck.promotion.domain.bo.PromotionRewardItemBo;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class ClientDailyLoginRewardVo {
    private Long promotionId;
    private String promotionName;
    private String promotionType;
    private LocalDate claimDate;
    private Boolean canClaim;
    private String claimStatus;
    private List<PromotionRewardItemBo> rewardItems;
    private String claimNo;
    private String walletTransactionNo;
}
```

- [ ] **Step 2: Implement `dailyLoginReward` service method**

In `PromotionRewardServiceImpl`, implement:

```java
@Override
public ClientDailyLoginRewardVo dailyLoginReward(Long memberId) {
    String tenantId = currentTenantId();
    PromotionReward reward = rewardMapper.selectActiveDailyLoginReward(tenantId);
    ClientDailyLoginRewardVo vo = new ClientDailyLoginRewardVo();
    LocalDate today = LocalDate.now();
    vo.setClaimDate(today);
    if (reward == null) {
        vo.setCanClaim(false);
        vo.setClaimStatus("NOT_CONFIGURED");
        vo.setRewardItems(List.of());
        return vo;
    }
    PromotionClaim claim = claimMapper.selectDailyClaim(tenantId, reward.getId(), memberId, today);
    vo.setPromotionId(reward.getId());
    vo.setPromotionName(reward.getPromotionName());
    vo.setPromotionType(DAILY_LOGIN_TYPE);
    vo.setRewardItems(rewardItems(reward));
    vo.setCanClaim(claim == null);
    vo.setClaimStatus(claim == null ? "UNCLAIMED" : claim.getStatus());
    if (claim != null) {
        vo.setClaimNo(claim.getClaimNo());
        vo.setWalletTransactionNo(claim.getWalletTransactionNo());
    }
    return vo;
}
```

- [ ] **Step 3: Add H5 endpoints**

In `ClientPromotionController.java`, add:

```java
@GetMapping("/daily-login")
public R<ClientDailyLoginRewardVo> dailyLoginReward(@RequestHeader(value = "Authorization", required = false) String authorization) {
    Long memberId = clientTokenService.requireMemberId(authorization);
    return R.ok(promotionRewardService.dailyLoginReward(memberId));
}

@PostMapping("/daily-login/claim")
public R<ClientDailyLoginRewardVo> claimDailyLoginReward(@RequestHeader(value = "Authorization", required = false) String authorization) {
    Long memberId = clientTokenService.requireMemberId(authorization);
    promotionRewardService.claimDailyLoginReward(memberId);
    return R.ok(promotionRewardService.dailyLoginReward(memberId));
}
```

Use the existing injected `ClientTokenService`; add it if this controller currently only injects `ClientPromotionService`.

- [ ] **Step 4: Add H5 types and API**

In `h5/src/types/client.ts`, add:

```ts
export interface ClientRewardItem {
  currencyCode: string
  rewardAmount: string
}

export interface ClientDailyLoginReward {
  promotionId?: number
  promotionName?: string
  promotionType?: string
  claimDate: string
  canClaim: boolean
  claimStatus: string
  rewardItems: ClientRewardItem[]
  claimNo?: string
  walletTransactionNo?: string
}
```

In `h5/src/api/client.ts`, add:

```ts
dailyLoginReward: () => request<ClientDailyLoginReward>('/api/client/promotions/daily-login'),
claimDailyLoginReward: () =>
  request<ClientDailyLoginReward>('/api/client/promotions/daily-login/claim', {
    method: 'POST'
  }),
```

- [ ] **Step 5: Update H5 UI**

In `PromotionsView.vue`, add state:

```ts
const dailyReward = ref<ClientDailyLoginReward | null>(null)
const claimingDaily = ref(false)
```

Add loader:

```ts
async function loadDailyReward() {
  if (!sessionState.member) return
  dailyReward.value = await clientApi.dailyLoginReward()
}
```

Update `loadPromotions()` to call `await loadDailyReward()` before loading generic promotions.

Add claim method:

```ts
async function claimDailyReward() {
  claimingDaily.value = true
  error.value = ''
  success.value = ''
  try {
    dailyReward.value = await clientApi.claimDailyLoginReward()
    success.value = t('dailyReward.claimSuccess')
    await session.loadWallet()
  } catch (err) {
    error.value = err instanceof Error ? err.message : t('dailyReward.claimFailed')
  } finally {
    claimingDaily.value = false
  }
}
```

Use the existing session wallet refresh method; if absent, reload wallet through the current wallet API helper used by wallet view.

- [ ] **Step 6: Add H5 i18n copy**

In `h5/src/i18n/messages.ts`, add under both languages:

```ts
dailyRewardTitle: '每日登录奖励',
dailyRewardSubtitle: '每天登录可领取一次',
dailyRewardClaim: '领取今日奖励',
dailyRewardClaimed: '今日已领取',
dailyRewardClaiming: '领取中',
dailyRewardClaimSuccess: '每日登录奖励已领取',
dailyRewardClaimFailed: '领取失败'
```

English:

```ts
dailyRewardTitle: 'Daily Login Reward',
dailyRewardSubtitle: 'Claim once per day',
dailyRewardClaim: 'Claim today',
dailyRewardClaimed: 'Claimed today',
dailyRewardClaiming: 'Claiming',
dailyRewardClaimSuccess: 'Daily login reward claimed',
dailyRewardClaimFailed: 'Claim failed'
```

- [ ] **Step 7: Verify H5 build**

Run:

```powershell
npm --prefix h5 run build
```

Expected: build exits 0.

- [ ] **Step 8: Commit H5 slice**

```powershell
git add backend/gameluck-modules/gameluck-promotion/src/main/java/com/gameluck/promotion/client backend/gameluck-modules/gameluck-promotion/src/main/java/com/gameluck/promotion/service backend/gameluck-modules/gameluck-promotion/src/test/java/com/gameluck/promotion/client/service/ClientPromotionServiceTest.java h5/src
git commit -m "feat(h5): add daily login reward claim"
```

---

## Task 5: B-Side Config And Claim Display

**Files:**
- Modify: `admin-ui/src/api/promotion/reward/types.ts`
- Modify: `admin-ui/src/views/promotion/reward/index.vue`
- Modify: `admin-ui/src/lang/zh_CN.ts`
- Modify: `admin-ui/src/lang/en_US.ts`

- [ ] **Step 1: Extend Admin API types**

In `admin-ui/src/api/promotion/reward/types.ts`, add:

```ts
export interface PromotionRewardItem {
  currencyCode: string;
  rewardAmount: number;
}
```

Add the following fields to `PromotionRewardVO`, `PromotionRewardForm`, and `PromotionRewardQuery`:

```ts
promotionType?: string;
claimCycle?: string;
dailyClaimLimit?: number;
rewardItems?: PromotionRewardItem[] | string;
```

Add to claim VO:

```ts
promotionType?: string;
claimDate?: string;
rewardSnapshot?: string;
```

- [ ] **Step 2: Add B-side form controls**

In `admin-ui/src/views/promotion/reward/index.vue`, add activity type select in query and form:

```vue
<el-form-item :label="t('promotionReward.fields.promotionType')" prop="promotionType">
  <el-select v-model="queryParams.promotionType" :placeholder="t('promotionReward.placeholders.promotionType')" clearable class="!w-150px">
    <el-option :label="t('promotionReward.types.GENERAL')" value="GENERAL" />
    <el-option :label="t('promotionReward.types.DAILY_LOGIN')" value="DAILY_LOGIN" />
  </el-select>
</el-form-item>
```

In the edit dialog, add a reward item editor:

```vue
<el-form-item :label="t('promotionReward.fields.rewardItems')">
  <div class="reward-items-editor">
    <div v-for="(item, index) in form.rewardItems" :key="index" class="reward-item-row">
      <el-select v-model="item.currencyCode" class="!w-120px">
        <el-option label="GC" value="GC" />
        <el-option label="SC" value="SC" />
      </el-select>
      <el-input-number v-model="item.rewardAmount" :precision="6" :min="0.000001" />
      <el-button icon="Delete" circle @click="removeRewardItem(index)" />
    </div>
    <el-button icon="Plus" @click="addRewardItem">{{ t('promotionReward.actions.addRewardItem') }}</el-button>
  </div>
</el-form-item>
```

Add helpers:

```ts
const addRewardItem = () => {
  const items = Array.isArray(form.value.rewardItems) ? form.value.rewardItems : [];
  form.value.rewardItems = [...items, { currencyCode: 'GC', rewardAmount: 1 }];
};

const removeRewardItem = (index: number) => {
  const items = Array.isArray(form.value.rewardItems) ? [...form.value.rewardItems] : [];
  items.splice(index, 1);
  form.value.rewardItems = items;
};
```

- [ ] **Step 3: Normalize submit payload**

Before add/update calls, ensure:

```ts
const payload = {
  ...form.value,
  promotionType: form.value.promotionType || 'GENERAL',
  claimCycle: form.value.promotionType === 'DAILY_LOGIN' ? 'DAILY' : 'ONCE',
  dailyClaimLimit: form.value.promotionType === 'DAILY_LOGIN' ? 1 : undefined
};
```

Use `payload` for `addPromotionReward(payload)` and `updatePromotionReward(payload)`.

- [ ] **Step 4: Add i18n keys**

Add Chinese:

```ts
promotionType: '活动类型',
rewardItems: '奖励配置',
claimDate: '领取日期'
```

Add type labels:

```ts
types: {
  GENERAL: '普通奖励',
  DAILY_LOGIN: '每日登录'
}
```

Add English equivalents:

```ts
promotionType: 'Activity Type',
rewardItems: 'Reward Items',
claimDate: 'Claim Date'
```

```ts
types: {
  GENERAL: 'General Reward',
  DAILY_LOGIN: 'Daily Login'
}
```

- [ ] **Step 5: Verify Admin UI**

Run:

```powershell
pnpm --dir admin-ui check:i18n
pnpm --dir admin-ui build:dev
```

Expected: both exit 0.

- [ ] **Step 6: Commit B-side config**

```powershell
git add admin-ui/src/api/promotion/reward/types.ts admin-ui/src/views/promotion/reward/index.vue admin-ui/src/lang/zh_CN.ts admin-ui/src/lang/en_US.ts
git commit -m "feat(admin): configure daily login rewards"
```

---

## Task 6: End-To-End Verification And Progress

**Files:**
- Modify: `progress.md`

- [ ] **Step 1: Compile backend**

Run:

```powershell
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-admin -am compile -Plocal -DskipTests
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 2: Package backend**

Run:

```powershell
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-admin -am package -Plocal -DskipTests
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 3: Restart backend**

Run:

```powershell
Start-Process -FilePath java -ArgumentList @('-jar','gameluck-admin\target\gameluck-admin.jar','--spring.profiles.active=local') -WorkingDirectory 'C:\codex\project\backend' -WindowStyle Hidden
```

Expected: backend starts on `http://localhost:8080`.

- [ ] **Step 4: Runtime smoke claim**

Use an existing H5 member token or register/login a new member, then call:

```http
GET /api/client/promotions/daily-login
POST /api/client/promotions/daily-login/claim
GET /api/client/wallet/accounts
```

Expected:

- Before claim: `canClaim=true`.
- Claim response: `claimStatus=SUCCESS`.
- Second claim: `canClaim=false`, no extra wallet transaction.
- Wallet increased by `GC 100` and `SC 1`.

- [ ] **Step 5: Database verification**

Run:

```powershell
mysql --default-character-set=utf8mb4 -uroot -proot gameluck_vue -e "select claim_no,promotion_type,member_id,claim_date,status,wallet_transaction_no,reward_snapshot from gl_promotion_claim where promotion_type='DAILY_LOGIN' order by create_time desc limit 5; select currency_code,source_type,amount,status from gl_wallet_transaction where source_type='DAILY_REWARD' order by create_time desc limit 10;"
```

Expected: one claim row for today and two successful wallet transaction rows for the claim.

- [ ] **Step 6: Update progress**

Append to `progress.md`:

```markdown
## 2026-07-11 Daily Login Reward

- Added configurable daily login reward with default `GC 100 + SC 1`.
- H5 can query and claim the daily login reward once per day.
- Wallet credits use `DAILY_REWARD` and create auditable wallet transactions.
- B-side promotion reward configuration supports activity type and reward item configuration.
- Runtime smoke confirmed first claim succeeds and duplicate same-day claim does not credit again.
```

- [ ] **Step 7: Commit verification notes**

```powershell
git add progress.md
git commit -m "docs(progress): record daily login reward verification"
```

---

## Acceptance Checklist

- [ ] B-side can configure a `DAILY_LOGIN` reward.
- [ ] Default seed creates `GC 100 + SC 1`.
- [ ] H5 reads the configured reward from the backend.
- [ ] H5 claim credits both GC and SC.
- [ ] Duplicate same-day claim does not credit again.
- [ ] Wallet transactions use `DAILY_REWARD`.
- [ ] Claim records include claim date and reward snapshot.
- [ ] Backend promotion tests pass.
- [ ] H5 build passes.
- [ ] Admin i18n check and build pass.
- [ ] Backend compile/package pass.

## Self-Review

- Spec coverage: all design requirements map to Tasks 1-6.
- Placeholder scan: no `TBD`, `TODO`, or vague implementation-only steps remain.
- Type consistency: `promotionType`, `claimCycle`, `dailyClaimLimit`, `rewardItems`, `claimDate`, and `rewardSnapshot` are used consistently across domain, BO, VO, API, SQL, and UI.
- Scope check: this is one vertical slice and intentionally excludes streak, VIP, KYC, geo, risk, inbox, and CMS.
