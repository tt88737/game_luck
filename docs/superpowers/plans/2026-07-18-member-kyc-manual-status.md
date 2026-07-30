# Member KYC Manual Status Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add persistent manual KYC status to member profiles, expose it to C-side APIs, let Admin maintain it, and require approved KYC before C-side redemption order creation.

**Architecture:** Reuse `gl_member_profile` as the single source for current KYC status in this phase. Extend existing member profile domain/API/Admin UI instead of creating a new KYC module, and add the redemption gate inside `ClientRedemptionService` before region policy evaluation and order creation.

**Tech Stack:** Spring Boot, MyBatis Plus, Java unit tests with JUnit/Mockito, MySQL idempotent SQL, Vue 3, Element Plus, Vue i18n, Vite checks.

---

### Task 1: Backend Member KYC Domain And Service Rules

**Files:**
- Create: `backend/gameluck-modules/gameluck-member/src/main/java/com/gameluck/member/enums/MemberKycStatus.java`
- Modify: `backend/gameluck-modules/gameluck-member/src/main/java/com/gameluck/member/domain/MemberProfile.java`
- Modify: `backend/gameluck-modules/gameluck-member/src/main/java/com/gameluck/member/domain/bo/MemberProfileBo.java`
- Modify: `backend/gameluck-modules/gameluck-member/src/main/java/com/gameluck/member/domain/vo/MemberProfileVo.java`
- Modify: `backend/gameluck-modules/gameluck-member/src/main/java/com/gameluck/member/service/impl/MemberProfileServiceImpl.java`
- Test: `backend/gameluck-modules/gameluck-member/src/test/java/com/gameluck/member/service/impl/MemberProfileServiceImplTest.java`

- [ ] **Step 1: Write failing tests for default and invalid KYC status**

Add these tests to `MemberProfileServiceImplTest`:

```java
@Test
@Tag("local")
void insertMemberDefaultsKycStatusToNotStarted() {
    MemberProfileMapper mapper = mock(MemberProfileMapper.class);
    MemberProfileServiceImpl service = new MemberProfileServiceImpl(mapper, new MemberIdGenerator());
    when(mapper.selectByUsername("000000", "kyc_alice")).thenReturn(null);
    when(mapper.insert(any(MemberProfile.class))).thenReturn(1);

    MemberProfileBo bo = createBo("kyc_alice");
    bo.setKycStatus(null);

    Boolean result = service.insertByBo(bo);

    assertEquals(Boolean.TRUE, result);
    ArgumentCaptor<MemberProfile> memberCaptor = ArgumentCaptor.forClass(MemberProfile.class);
    verify(mapper).insert(memberCaptor.capture());
    assertEquals("NOT_STARTED", memberCaptor.getValue().getKycStatus());
}

@Test
@Tag("local")
void invalidKycStatusCannotBeInserted() {
    MemberProfileMapper mapper = mock(MemberProfileMapper.class);
    MemberProfileServiceImpl service = new MemberProfileServiceImpl(mapper, new MemberIdGenerator());
    when(mapper.selectByUsername("000000", "kyc_bad")).thenReturn(null);
    MemberProfileBo bo = createBo("kyc_bad");
    bo.setKycStatus("MANUAL_OK");

    ServiceException exception = assertThrows(ServiceException.class, () -> service.insertByBo(bo));

    assertEquals("member.kyc.status.invalid", exception.getMessage());
    verify(mapper, never()).insert(any(MemberProfile.class));
}
```

- [ ] **Step 2: Write failing test for KYC review metadata update**

Add this test to `MemberProfileServiceImplTest`:

```java
@Test
@Tag("local")
void updateKycStatusStoresReviewMetadata() {
    MemberProfileMapper mapper = mock(MemberProfileMapper.class);
    MemberProfileServiceImpl service = new MemberProfileServiceImpl(mapper, new MemberIdGenerator());
    MemberProfile existing = new MemberProfile();
    existing.setId(88L);
    existing.setTenantId("000000");
    existing.setUsername("kyc_review");
    existing.setKycStatus("PENDING");
    when(mapper.selectById(88L)).thenReturn(existing);
    when(mapper.selectByUsername("000000", "kyc_review")).thenReturn(existing);
    when(mapper.updateById(any(MemberProfile.class))).thenReturn(1);

    MemberProfileBo bo = createBo("kyc_review");
    bo.setId(88L);
    bo.setKycStatus("APPROVED");
    bo.setKycReviewReason("Manual review passed");

    Boolean result = service.updateByBo(bo);

    assertEquals(Boolean.TRUE, result);
    ArgumentCaptor<MemberProfile> memberCaptor = ArgumentCaptor.forClass(MemberProfile.class);
    verify(mapper).updateById(memberCaptor.capture());
    MemberProfile update = memberCaptor.getValue();
    assertEquals("APPROVED", update.getKycStatus());
    assertEquals("Manual review passed", update.getKycReviewReason());
    assertEquals("admin", update.getKycReviewedBy());
    assertTrue(update.getKycReviewTime() != null);
}
```

- [ ] **Step 3: Run tests to verify they fail**

Run:

```powershell
$env:MAVEN_OPTS='-Xmx768m -XX:MaxMetaspaceSize=256m -XX:ReservedCodeCacheSize=128m'
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-modules/gameluck-member -am -Plocal -DskipTests=false "-Dtest=MemberProfileServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" -DforkCount=0 test
```

Expected: compilation fails because `kycStatus`, `kycReviewReason`, `kycReviewedBy`, and `kycReviewTime` do not exist yet.

- [ ] **Step 4: Add `MemberKycStatus` enum**

Create `MemberKycStatus.java`:

```java
package com.gameluck.member.enums;

/**
 * Manual KYC status for member profiles.
 */
public enum MemberKycStatus {
    NOT_STARTED,
    PENDING,
    APPROVED,
    REJECTED,
    EXPIRED
}
```

- [ ] **Step 5: Add KYC fields to member domain, BO, and VO**

In `MemberProfile`, add after `riskLevel`:

```java
private String kycStatus;

private String kycReviewReason;

private String kycReviewedBy;

private Date kycReviewTime;
```

In `MemberProfileBo`, add after `riskLevel`:

```java
private String kycStatus;

private String kycReviewReason;

private String kycReviewedBy;

private Date kycReviewTime;
```

In `MemberProfileVo`, add after `riskLevel`:

```java
private String kycStatus;

private String kycReviewReason;

private String kycReviewedBy;

private Date kycReviewTime;
```

- [ ] **Step 6: Implement service defaults, validation, filtering, and metadata**

Modify `MemberProfileServiceImpl`:

Add import:

```java
import com.gameluck.member.enums.MemberKycStatus;
```

Add constants:

```java
private static final String DEFAULT_KYC_REVIEWED_BY = "admin";
```

In `insertByBo(...)`, after setting `riskLevel`:

```java
add.setKycStatus(StringUtils.blankToDefault(bo.getKycStatus(), MemberKycStatus.NOT_STARTED.name()));
validateKycStatus(add.getKycStatus());
```

In `updateByBo(...)`, after risk validation:

```java
String normalizedKycStatus = StringUtils.blankToDefault(bo.getKycStatus(), MemberKycStatus.NOT_STARTED.name());
validateKycStatus(normalizedKycStatus);
```

After `MemberProfile update = BeanUtil.toBean(...)`:

```java
update.setKycStatus(normalizedKycStatus);
boolean kycChanged = !normalizedKycStatus.equals(StringUtils.blankToDefault(member.getKycStatus(), MemberKycStatus.NOT_STARTED.name()));
boolean reasonChanged = !StringUtils.equals(StringUtils.trim(bo.getKycReviewReason()), StringUtils.trim(member.getKycReviewReason()));
if (kycChanged || reasonChanged) {
    update.setKycReviewedBy(DEFAULT_KYC_REVIEWED_BY);
    update.setKycReviewTime(new Date());
}
```

In `buildQueryWrapper(...)`, after risk filter:

```java
lqw.eq(StringUtils.isNotBlank(bo.getKycStatus()), MemberProfile::getKycStatus, bo.getKycStatus());
```

Add helper:

```java
private void validateKycStatus(String kycStatus) {
    try {
        MemberKycStatus.valueOf(kycStatus);
    } catch (Exception ex) {
        throw new ServiceException(MessageUtils.message("member.kyc.status.invalid"));
    }
}
```

- [ ] **Step 7: Run member service tests**

Run the same Maven command from Step 3.

Expected: `MemberProfileServiceImplTest` passes.

### Task 2: C-Side Member KYC Response

**Files:**
- Modify: `backend/gameluck-modules/gameluck-member/src/main/java/com/gameluck/member/client/service/ClientAuthService.java`
- Test: `backend/gameluck-modules/gameluck-member/src/test/java/com/gameluck/member/client/service/ClientAuthServiceTest.java`

- [ ] **Step 1: Write failing tests for persisted and default KYC status**

Add to `ClientAuthServiceTest`:

```java
@Test
@Tag("local")
void loginReturnsPersistedKycStatus() {
    MemberProfileMapper mapper = mock(MemberProfileMapper.class);
    IWalletCoreService walletCoreService = mock(IWalletCoreService.class);
    ClientAuthService service = new ClientAuthService(mapper, new ClientTokenService(), walletCoreService, new MemberIdGenerator());
    MemberProfile member = member();
    member.setKycStatus("APPROVED");
    when(mapper.selectByUsername("000000", "demo_player")).thenReturn(member);
    ClientLoginBo bo = new ClientLoginBo();
    bo.setUsername("demo_player");
    bo.setPassword("Demo123456");

    ClientLoginVo result = service.login(bo);

    assertEquals("APPROVED", result.getMember().getKycStatus());
}

@Test
@Tag("local")
void loginDefaultsBlankKycStatusToNotStarted() {
    MemberProfileMapper mapper = mock(MemberProfileMapper.class);
    IWalletCoreService walletCoreService = mock(IWalletCoreService.class);
    ClientAuthService service = new ClientAuthService(mapper, new ClientTokenService(), walletCoreService, new MemberIdGenerator());
    MemberProfile member = member();
    member.setKycStatus("");
    when(mapper.selectByUsername("000000", "demo_player")).thenReturn(member);
    ClientLoginBo bo = new ClientLoginBo();
    bo.setUsername("demo_player");
    bo.setPassword("Demo123456");

    ClientLoginVo result = service.login(bo);

    assertEquals("NOT_STARTED", result.getMember().getKycStatus());
}
```

- [ ] **Step 2: Run tests to verify current hardcoded behavior fails**

Run:

```powershell
$env:MAVEN_OPTS='-Xmx768m -XX:MaxMetaspaceSize=256m -XX:ReservedCodeCacheSize=128m'
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-modules/gameluck-member -am -Plocal -DskipTests=false "-Dtest=ClientAuthServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" -DforkCount=0 test
```

Expected: persisted `APPROVED` test fails because `toClientMember(...)` still hardcodes `NOT_STARTED`.

- [ ] **Step 3: Return persisted KYC status**

In `ClientAuthService`, replace:

```java
vo.setKycStatus("NOT_STARTED");
```

with:

```java
vo.setKycStatus(StringUtils.blankToDefault(member.getKycStatus(), "NOT_STARTED"));
```

In `register(...)`, after setting risk level:

```java
member.setKycStatus("NOT_STARTED");
```

- [ ] **Step 4: Run ClientAuthService tests**

Run the command from Step 2.

Expected: `ClientAuthServiceTest` passes.

### Task 3: Redemption Gate Requires Approved KYC

**Files:**
- Modify: `backend/gameluck-modules/gameluck-redemption/src/main/java/com/gameluck/redemption/client/service/ClientRedemptionService.java`
- Test: `backend/gameluck-modules/gameluck-redemption/src/test/java/com/gameluck/redemption/client/service/ClientRedemptionServiceTest.java`

- [ ] **Step 1: Write failing KYC gate tests**

Add to `ClientRedemptionServiceTest`:

```java
@Test
@Tag("local")
void requestRejectsNotStartedKycMember() {
    MemberProfile member = eligibleMember();
    member.setKycStatus("NOT_STARTED");

    ServiceException exception = assertGateFailure(member);

    assertEquals("client.redemption.kyc.required", exception.getMessage());
}

@Test
@Tag("local")
void requestRejectsPendingKycMember() {
    MemberProfile member = eligibleMember();
    member.setKycStatus("PENDING");

    ServiceException exception = assertGateFailure(member);

    assertEquals("client.redemption.kyc.required", exception.getMessage());
}
```

Update `eligibleMember()` to set approved KYC:

```java
member.setKycStatus("APPROVED");
```

- [ ] **Step 2: Run redemption tests to verify failure**

Run:

```powershell
$env:MAVEN_OPTS='-Xmx768m -XX:MaxMetaspaceSize=256m -XX:ReservedCodeCacheSize=128m'
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-modules/gameluck-redemption -am -Plocal -DskipTests=false "-Dtest=ClientRedemptionServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" -DforkCount=0 test
```

Expected: new KYC tests fail because no KYC gate exists.

- [ ] **Step 3: Implement KYC check**

In `ClientRedemptionService.validateRedemptionGate(...)`, after agreement checks and before returning:

```java
if (!"APPROVED".equals(StringUtils.blankToDefault(member.getKycStatus(), "NOT_STARTED"))) {
    throw new ServiceException(MessageUtils.message("client.redemption.kyc.required"));
}
```

If `StringUtils` is not already imported, add:

```java
import com.gameluck.common.core.utils.StringUtils;
```

- [ ] **Step 4: Run redemption tests**

Run the command from Step 2.

Expected: `ClientRedemptionServiceTest` passes.

### Task 4: SQL And Backend I18n

**Files:**
- Modify: `backend/script/sql/gameluck_wallet.sql`
- Modify: `backend/script/sql/gameluck_platform_dict.sql`
- Modify: `backend/gameluck-admin/src/main/resources/i18n/messages.properties`
- Modify: `backend/gameluck-admin/src/main/resources/i18n/messages_zh_CN.properties`
- Modify: `backend/gameluck-admin/src/main/resources/i18n/messages_en_US.properties`

- [ ] **Step 1: Add KYC columns to `CREATE TABLE`**

In `gl_member_profile` DDL after `risk_level`:

```sql
  kyc_status VARCHAR(32) NOT NULL DEFAULT 'NOT_STARTED' COMMENT 'KYC status',
  kyc_review_reason VARCHAR(512) DEFAULT NULL COMMENT 'KYC review reason',
  kyc_reviewed_by VARCHAR(64) DEFAULT NULL COMMENT 'KYC reviewed by',
  kyc_review_time DATETIME DEFAULT NULL COMMENT 'KYC review time',
```

Add index:

```sql
  KEY idx_gl_member_profile_03 (tenant_id, kyc_status)
```

Make sure the preceding index line has a trailing comma.

- [ ] **Step 2: Add idempotent ALTER statements after table creation**

Append after `CREATE TABLE IF NOT EXISTS gl_member_profile (...)`:

```sql
SET @schema_name = DATABASE();

SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'gl_member_profile' AND column_name = 'kyc_status') = 0,
  'ALTER TABLE gl_member_profile ADD COLUMN kyc_status VARCHAR(32) NOT NULL DEFAULT ''NOT_STARTED'' COMMENT ''KYC status'' AFTER risk_level',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'gl_member_profile' AND column_name = 'kyc_review_reason') = 0,
  'ALTER TABLE gl_member_profile ADD COLUMN kyc_review_reason VARCHAR(512) DEFAULT NULL COMMENT ''KYC review reason'' AFTER kyc_status',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'gl_member_profile' AND column_name = 'kyc_reviewed_by') = 0,
  'ALTER TABLE gl_member_profile ADD COLUMN kyc_reviewed_by VARCHAR(64) DEFAULT NULL COMMENT ''KYC reviewed by'' AFTER kyc_review_reason',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 'gl_member_profile' AND column_name = 'kyc_review_time') = 0,
  'ALTER TABLE gl_member_profile ADD COLUMN kyc_review_time DATETIME DEFAULT NULL COMMENT ''KYC review time'' AFTER kyc_reviewed_by',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE gl_member_profile
SET kyc_status = 'NOT_STARTED'
WHERE kyc_status IS NULL OR kyc_status = '';
```

- [ ] **Step 3: Ensure dictionary has `EXPIRED`**

If `backend/script/sql/gameluck_platform_dict.sql` already includes `EXPIRED`, leave it unchanged. If missing, add an idempotent row:

```sql
INSERT INTO sys_dict_data
(dict_code, tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, create_dept, create_by, create_time, update_by, update_time, remark)
SELECT 21013, '000000', 5, '已过期', 'EXPIRED', 'gl_kyc_status', '', 'info', 'N', 103, 1, SYSDATE(), NULL, NULL, 'KYC已过期'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE tenant_id = '000000' AND dict_type = 'gl_kyc_status' AND dict_value = 'EXPIRED');
```

- [ ] **Step 4: Add backend messages**

Add to all three backend i18n files:

`messages.properties` and `messages_zh_CN.properties`:

```properties
member.kyc.status.invalid=会员KYC状态无效
client.redemption.kyc.required=请先完成KYC认证后再申请兑换
```

`messages_en_US.properties`:

```properties
member.kyc.status.invalid=Invalid member KYC status
client.redemption.kyc.required=Please complete KYC verification before requesting redemption
```

- [ ] **Step 5: Import SQL locally**

Run:

```powershell
cmd /c "mysql -uroot -p123456 gameluck_vue < backend\script\sql\gameluck_wallet.sql"
```

Expected: import succeeds without duplicate column errors.

### Task 5: Admin UI KYC Maintenance

**Files:**
- Modify: `admin-ui/src/api/member/profile/types.ts`
- Modify: `admin-ui/src/views/member/profile/index.vue`
- Modify: `admin-ui/src/lang/zh_CN.ts`
- Modify: `admin-ui/src/lang/en_US.ts`

- [ ] **Step 1: Add TypeScript fields**

In `MemberProfileVO`, add:

```ts
kycStatus: string;
kycReviewReason: string;
kycReviewedBy: string;
kycReviewTime: string;
```

In `MemberProfileForm`, add:

```ts
kycStatus?: string;
kycReviewReason?: string;
kycReviewedBy?: string;
kycReviewTime?: string;
```

In `MemberProfileQuery`, add:

```ts
kycStatus?: string;
```

- [ ] **Step 2: Use KYC dictionary in member profile page**

In `index.vue`, add `gl_kyc_status`:

```ts
const { gl_kyc_status } = toRefs<any>(proxy?.useDict('gl_kyc_status'));
```

If there is no existing `toRefs` import needed because Vue auto-imports are configured, follow the local style and do not add explicit imports.

- [ ] **Step 3: Add KYC filter**

Add after risk filter:

```vue
<el-form-item :label="t('memberProfile.fields.kycStatus')" prop="kycStatus">
  <el-select v-model="queryParams.kycStatus" :placeholder="t('memberProfile.placeholders.kycStatus')" clearable class="!w-140px">
    <el-option v-for="item in gl_kyc_status" :key="item.value" :label="item.label" :value="item.value" />
  </el-select>
</el-form-item>
```

Add `kycStatus: ''` to `queryParams`.

- [ ] **Step 4: Add KYC table column**

Add after Risk Level column:

```vue
<el-table-column :label="t('memberProfile.fields.kycStatus')" align="center" prop="kycStatus" width="120">
  <template #default="scope">
    <dict-tag :options="gl_kyc_status" :value="scope.row.kycStatus" />
  </template>
</el-table-column>
```

- [ ] **Step 5: Add KYC edit controls**

Add to dialog after risk level:

```vue
<el-form-item :label="t('memberProfile.fields.kycStatus')" prop="kycStatus">
  <el-select v-model="form.kycStatus" :placeholder="t('memberProfile.placeholders.kycStatus')" class="w-full">
    <el-option v-for="item in gl_kyc_status" :key="item.value" :label="item.label" :value="item.value" />
  </el-select>
</el-form-item>
<el-form-item :label="t('memberProfile.fields.kycReviewReason')" prop="kycReviewReason">
  <el-input v-model="form.kycReviewReason" type="textarea" :rows="2" :placeholder="t('memberProfile.placeholders.kycReviewReason')" />
</el-form-item>
```

Add `kycStatus: 'NOT_STARTED'` to `initFormData`.

Add validation:

```ts
kycStatus: [{ required: true, message: t('memberProfile.rules.kycStatus'), trigger: 'change' }]
```

- [ ] **Step 6: Add KYC detail fields**

Add to detail dialog after risk:

```vue
<el-descriptions-item :label="t('memberProfile.fields.kycStatus')">
  <dict-tag :options="gl_kyc_status" :value="detail.kycStatus" />
</el-descriptions-item>
<el-descriptions-item :label="t('memberProfile.fields.kycReviewedBy')">{{ detail.kycReviewedBy }}</el-descriptions-item>
<el-descriptions-item :label="t('memberProfile.fields.kycReviewTime')">{{ detail.kycReviewTime }}</el-descriptions-item>
<el-descriptions-item :label="t('memberProfile.fields.kycReviewReason')" :span="2">{{ detail.kycReviewReason }}</el-descriptions-item>
```

- [ ] **Step 7: Add Admin UI translations**

In both `zh_CN.ts` and `en_US.ts`, extend `memberProfile.fields`, `placeholders`, and `rules`.

Chinese:

```ts
kycStatus: 'KYC状态',
kycReviewReason: 'KYC备注',
kycReviewedBy: 'KYC操作人',
kycReviewTime: 'KYC操作时间'
```

```ts
kycStatus: '请选择KYC状态',
kycReviewReason: '请输入KYC审核备注'
```

```ts
kycStatus: 'KYC状态不能为空'
```

English:

```ts
kycStatus: 'KYC Status',
kycReviewReason: 'KYC Note',
kycReviewedBy: 'KYC Operator',
kycReviewTime: 'KYC Time'
```

```ts
kycStatus: 'Select KYC status',
kycReviewReason: 'Enter KYC review note'
```

```ts
kycStatus: 'KYC status is required'
```

- [ ] **Step 8: Run Admin UI checks**

Run:

```powershell
pnpm --dir admin-ui check:i18n
pnpm --dir admin-ui check:menu-icons
```

Expected: both pass.

### Task 6: Focused Verification And Runtime Smoke

**Files:**
- Modify: `task_plan.md`
- Modify: `progress.md`

- [ ] **Step 1: Run focused backend tests**

Run:

```powershell
$env:MAVEN_OPTS='-Xmx768m -XX:MaxMetaspaceSize=256m -XX:ReservedCodeCacheSize=128m'
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-modules/gameluck-member,gameluck-modules/gameluck-redemption -am -Plocal -DskipTests=false "-Dtest=MemberProfileServiceImplTest,ClientAuthServiceTest,ClientRedemptionServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" -DforkCount=0 test
```

Expected: all specified tests pass.

- [ ] **Step 2: Build H5 if frontend types changed**

Run:

```powershell
npm --prefix h5 run build
```

Expected: build passes. If H5 files were not changed in implementation, this still confirms C-side type compatibility.

- [ ] **Step 3: Package backend**

Stop the running Java backend if it locks `gameluck-admin.jar`, then run:

```powershell
$env:MAVEN_OPTS='-Xmx768m -XX:MaxMetaspaceSize=256m -XX:ReservedCodeCacheSize=128m'
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-admin -am package -Plocal -DskipTests
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 4: Restart backend and verify health**

Run backend from the new jar:

```powershell
java -jar backend\gameluck-admin\target\gameluck-admin.jar --spring.profiles.active=local --captcha.enable=false
```

Verify:

```powershell
Invoke-WebRequest -Uri http://localhost:8080/ -UseBasicParsing
```

Expected: HTTP 200.

- [ ] **Step 5: Runtime smoke KYC block and approval**

Use existing H5/API smoke approach:

1. Register a new H5 user in an allowed state such as `US/CA`.
2. Confirm DB row has `kyc_status = NOT_STARTED`.
3. Submit `POST /api/client/redemptions/request` for `SC 1.00`.
4. Expected: business `code=500`, message for `client.redemption.kyc.required`, and no redemption order.
5. Update the same member to `kyc_status = APPROVED` via Admin UI or authenticated Admin API.
6. Submit redemption again in `US/CA`.
7. Expected: request reaches existing redemption path and creates a pending order if wallet balance and other gates pass.

- [ ] **Step 6: Verify Admin operation log**

Open `/system/log/operlog` and filter title `Member profile edit`.

Expected: latest edit record includes `PUT /member/profile`, operator `admin`, success status, and request params with KYC fields.

- [ ] **Step 7: Final whitespace check**

Run:

```powershell
git diff --check
```

Expected: no whitespace errors; CRLF warnings are acceptable.

- [ ] **Step 8: Record Phase 36 completion**

Update `progress.md` with:

- Files changed.
- SQL import result.
- Focused backend test result.
- Admin UI check result.
- H5 build result.
- Runtime smoke result.
- Operation log result.
- `git diff --check` result.

Update `task_plan.md` Phase 36 from `in_progress` to `complete` only after all required verification passes.

## Self Review

- Spec coverage: this plan covers persistent KYC fields, C-side response, Admin maintenance, redemption gate, SQL/i18n, tests, runtime smoke, and operation log visibility.
- Scope control: this plan does not add provider integration, upload, callback handling, or multi-step review workflow.
- Type consistency: field names are consistently `kycStatus`, `kycReviewReason`, `kycReviewedBy`, `kycReviewTime` in Java/TypeScript and `kyc_status`, `kyc_review_reason`, `kyc_reviewed_by`, `kyc_review_time` in SQL.
- Placeholder scan: no incomplete placeholder markers are intentionally left in the plan.
