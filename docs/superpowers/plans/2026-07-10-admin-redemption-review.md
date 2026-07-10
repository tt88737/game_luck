# Phase 4 Admin Redemption Review Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Harden the existing B-side redemption order page and backend service into an operator-ready review closed loop.

**Architecture:** Keep the current `gameluck-redemption` module and `admin-ui` redemption order page. Add focused service-level regression tests first, tighten backend audit rules, then improve the existing Vue page with pending-first filtering, quick status filters, reject reason validation, submit locking, and i18n-safe copy.

**Tech Stack:** Java 17, Spring Boot, MyBatis Plus, JUnit 5, Mockito, Vue 3, TypeScript, Element Plus, vue-i18n, pnpm.

---

## File Structure

- Modify: `backend/gameluck-modules/gameluck-redemption/src/test/java/com/gameluck/redemption/service/impl/RedemptionOrderServiceImplTest.java`
  - Expands service-level coverage for approval, rejection, wallet failures, and audit reason validation.
- Modify: `backend/gameluck-modules/gameluck-redemption/src/main/java/com/gameluck/redemption/service/impl/RedemptionOrderServiceImpl.java`
  - Adds backend reject-reason guard and keeps wallet failure from advancing orders to terminal status.
- Modify: `backend/gameluck-admin/src/main/resources/i18n/messages.properties`
  - Adds default backend message key for reject reason validation.
- Modify: `backend/gameluck-admin/src/main/resources/i18n/messages_zh_CN.properties`
  - Adds Chinese backend message key for reject reason validation.
- Modify: `backend/gameluck-admin/src/main/resources/i18n/messages_en_US.properties`
  - Adds English backend message key for reject reason validation.
- Modify: `admin-ui/src/views/redemption/order/index.vue`
  - Adds pending default filter, status quick filter, audit form validation, and submit locking.
- Modify: `admin-ui/src/lang/zh_CN.ts`
  - Adds Chinese frontend labels and validation copy.
- Modify: `admin-ui/src/lang/en_US.ts`
  - Adds English frontend labels and validation copy.
- Modify: `progress.md`
  - Logs execution progress and verification results.
- Modify: `task_plan.md`
  - Marks Phase 4 complete after implementation and verification.

## Task 1: Backend Review Rule Tests

**Files:**
- Modify: `backend/gameluck-modules/gameluck-redemption/src/test/java/com/gameluck/redemption/service/impl/RedemptionOrderServiceImplTest.java`

- [x] **Step 1: Replace the test file with expanded failing coverage**

Use this complete file content:

```java
package com.gameluck.redemption.service.impl;

import com.gameluck.common.core.exception.ServiceException;
import com.gameluck.redemption.domain.RedemptionOrder;
import com.gameluck.redemption.enums.RedemptionOrderStatus;
import com.gameluck.redemption.mapper.RedemptionOrderMapper;
import com.gameluck.wallet.domain.WalletTransaction;
import com.gameluck.wallet.enums.WalletTransactionStatus;
import com.gameluck.wallet.service.IWalletCoreService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RedemptionOrderServiceImplTest {

    @Test
    @Tag("local")
    void rejectApprovedOrderDoesNotCallWalletAgain() {
        RedemptionOrderMapper mapper = mock(RedemptionOrderMapper.class);
        IWalletCoreService walletCoreService = mock(IWalletCoreService.class);
        RedemptionOrderServiceImpl service = new RedemptionOrderServiceImpl(mapper, walletCoreService);

        RedemptionOrder order = pendingOrder();
        order.setStatus(RedemptionOrderStatus.APPROVED.name());
        when(mapper.selectByIdForUpdate(1L)).thenReturn(order);

        ServiceException exception = assertThrows(ServiceException.class, () -> service.reject(1L, "duplicate reject"));

        assertEquals("redemption.order.only.pending.allowed", exception.getMessage());
        verifyNoInteractions(walletCoreService);
    }

    @Test
    @Tag("local")
    void approveRejectedOrderDoesNotCallWalletAgain() {
        RedemptionOrderMapper mapper = mock(RedemptionOrderMapper.class);
        IWalletCoreService walletCoreService = mock(IWalletCoreService.class);
        RedemptionOrderServiceImpl service = new RedemptionOrderServiceImpl(mapper, walletCoreService);

        RedemptionOrder order = pendingOrder();
        order.setStatus(RedemptionOrderStatus.REJECTED.name());
        when(mapper.selectByIdForUpdate(1L)).thenReturn(order);

        ServiceException exception = assertThrows(ServiceException.class, () -> service.approve(1L, "duplicate approve"));

        assertEquals("redemption.order.only.pending.allowed", exception.getMessage());
        verifyNoInteractions(walletCoreService);
    }

    @Test
    @Tag("local")
    void rejectBlankReasonDoesNotCallWallet() {
        RedemptionOrderMapper mapper = mock(RedemptionOrderMapper.class);
        IWalletCoreService walletCoreService = mock(IWalletCoreService.class);
        RedemptionOrderServiceImpl service = new RedemptionOrderServiceImpl(mapper, walletCoreService);

        RedemptionOrder order = pendingOrder();
        when(mapper.selectByIdForUpdate(1L)).thenReturn(order);

        ServiceException exception = assertThrows(ServiceException.class, () -> service.reject(1L, "  "));

        assertEquals("redemption.audit.reject.reason.required", exception.getMessage());
        verifyNoInteractions(walletCoreService);
        verify(mapper, never()).updateById(any());
    }

    @Test
    @Tag("local")
    void approvePendingOrderSettlesFreezeAndWritesAuditFields() {
        RedemptionOrderMapper mapper = mock(RedemptionOrderMapper.class);
        IWalletCoreService walletCoreService = mock(IWalletCoreService.class);
        RedemptionOrderServiceImpl service = new RedemptionOrderServiceImpl(mapper, walletCoreService);

        RedemptionOrder order = pendingOrder();
        when(mapper.selectByIdForUpdate(1L)).thenReturn(order);
        when(walletCoreService.settle(any())).thenReturn(successTransaction("WT_SETTLE_1"));

        service.approve(1L, "approved by ops");

        assertEquals(RedemptionOrderStatus.APPROVED.name(), order.getStatus());
        assertEquals("WT_SETTLE_1", order.getSettleWalletTransactionNo());
        assertEquals("approved by ops", order.getAuditReason());
        assertNull(order.getFailReason());
        assertNotNull(order.getAuditTime());
        assertNotNull(order.getUpdateTime());
        verify(mapper).updateById(order);
    }

    @Test
    @Tag("local")
    void rejectPendingOrderReleasesFreezeAndWritesAuditFields() {
        RedemptionOrderMapper mapper = mock(RedemptionOrderMapper.class);
        IWalletCoreService walletCoreService = mock(IWalletCoreService.class);
        RedemptionOrderServiceImpl service = new RedemptionOrderServiceImpl(mapper, walletCoreService);

        RedemptionOrder order = pendingOrder();
        when(mapper.selectByIdForUpdate(1L)).thenReturn(order);
        when(walletCoreService.unfreeze(any())).thenReturn(successTransaction("WT_RELEASE_1"));

        service.reject(1L, "account mismatch");

        assertEquals(RedemptionOrderStatus.REJECTED.name(), order.getStatus());
        assertEquals("WT_RELEASE_1", order.getReleaseWalletTransactionNo());
        assertEquals("account mismatch", order.getAuditReason());
        assertNull(order.getFailReason());
        assertNotNull(order.getAuditTime());
        assertNotNull(order.getUpdateTime());
        verify(mapper).updateById(order);
    }

    @Test
    @Tag("local")
    void approveWalletFailureDoesNotUpdateOrderToApproved() {
        RedemptionOrderMapper mapper = mock(RedemptionOrderMapper.class);
        IWalletCoreService walletCoreService = mock(IWalletCoreService.class);
        RedemptionOrderServiceImpl service = new RedemptionOrderServiceImpl(mapper, walletCoreService);

        RedemptionOrder order = pendingOrder();
        when(mapper.selectByIdForUpdate(1L)).thenReturn(order);
        when(walletCoreService.settle(any())).thenReturn(failedTransaction());

        ServiceException exception = assertThrows(ServiceException.class, () -> service.approve(1L, "approve"));

        assertEquals("redemption.wallet.operation.fail", exception.getMessage());
        assertEquals(RedemptionOrderStatus.PENDING.name(), order.getStatus());
        verify(mapper, never()).updateById(any());
    }

    @Test
    @Tag("local")
    void rejectWalletFailureDoesNotUpdateOrderToRejected() {
        RedemptionOrderMapper mapper = mock(RedemptionOrderMapper.class);
        IWalletCoreService walletCoreService = mock(IWalletCoreService.class);
        RedemptionOrderServiceImpl service = new RedemptionOrderServiceImpl(mapper, walletCoreService);

        RedemptionOrder order = pendingOrder();
        when(mapper.selectByIdForUpdate(1L)).thenReturn(order);
        when(walletCoreService.unfreeze(any())).thenReturn(failedTransaction());

        ServiceException exception = assertThrows(ServiceException.class, () -> service.reject(1L, "reject"));

        assertEquals("redemption.wallet.operation.fail", exception.getMessage());
        assertEquals(RedemptionOrderStatus.PENDING.name(), order.getStatus());
        verify(mapper, never()).updateById(any());
    }

    private RedemptionOrder pendingOrder() {
        RedemptionOrder order = new RedemptionOrder();
        order.setId(1L);
        order.setRedemptionOrderNo("RD_TEST_1");
        order.setMemberId(1001L);
        order.setCurrencyCode("RC");
        order.setAmount(new BigDecimal("1.000000"));
        order.setStatus(RedemptionOrderStatus.PENDING.name());
        order.setFreezeNo("WF_TEST_1");
        order.setSettleIdempotencyKey("redemption:settle:RD_TEST_1");
        order.setReleaseIdempotencyKey("redemption:release:RD_TEST_1");
        return order;
    }

    private WalletTransaction successTransaction(String transactionNo) {
        WalletTransaction transaction = new WalletTransaction();
        transaction.setTransactionNo(transactionNo);
        transaction.setStatus(WalletTransactionStatus.SUCCESS.name());
        return transaction;
    }

    private WalletTransaction failedTransaction() {
        WalletTransaction transaction = new WalletTransaction();
        transaction.setTransactionNo("WT_FAILED_1");
        transaction.setStatus(WalletTransactionStatus.FAILED.name());
        transaction.setFailReason("wallet failed");
        return transaction;
    }
}
```

- [x] **Step 2: Run the focused test and verify it fails for the missing reject-reason rule**

Run:

```powershell
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-modules/gameluck-redemption -am -Plocal -DskipTests=false "-Dtest=RedemptionOrderServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected: FAIL on `rejectBlankReasonDoesNotCallWallet` because `reject()` does not yet validate blank reasons.

- [x] **Step 3: Commit the failing tests**

```powershell
git add backend/gameluck-modules/gameluck-redemption/src/test/java/com/gameluck/redemption/service/impl/RedemptionOrderServiceImplTest.java
git commit -m "test(redemption): cover admin review edge cases"
```

## Task 2: Backend Reject Reason Guard

**Files:**
- Modify: `backend/gameluck-modules/gameluck-redemption/src/main/java/com/gameluck/redemption/service/impl/RedemptionOrderServiceImpl.java`
- Modify: `backend/gameluck-admin/src/main/resources/i18n/messages.properties`
- Modify: `backend/gameluck-admin/src/main/resources/i18n/messages_zh_CN.properties`
- Modify: `backend/gameluck-admin/src/main/resources/i18n/messages_en_US.properties`

- [x] **Step 1: Add the reject reason guard**

In `RedemptionOrderServiceImpl.reject`, insert `requireRejectReason(reason);` after `requirePending(order);`:

```java
RedemptionOrder order = lockOrder(id);
requirePending(order);
requireRejectReason(reason);
WalletTransaction transaction = walletCoreService.unfreeze(
    buildFreezeBo(order, order.getReleaseIdempotencyKey(), order.getFreezeNo()));
```

Add this private method near `requirePending`:

```java
private void requireRejectReason(String reason) {
    if (StringUtils.isBlank(reason)) {
        throw new ServiceException(MessageUtils.message("redemption.audit.reject.reason.required"));
    }
}
```

- [x] **Step 2: Add backend i18n keys**

Append this line to `backend/gameluck-admin/src/main/resources/i18n/messages.properties`:

```properties
redemption.audit.reject.reason.required=拒绝原因不能为空
```

Append this line to `backend/gameluck-admin/src/main/resources/i18n/messages_zh_CN.properties`:

```properties
redemption.audit.reject.reason.required=拒绝原因不能为空
```

Append this line to `backend/gameluck-admin/src/main/resources/i18n/messages_en_US.properties`:

```properties
redemption.audit.reject.reason.required=Rejection reason is required.
```

- [x] **Step 3: Run the focused backend test and verify it passes**

Run:

```powershell
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-modules/gameluck-redemption -am -Plocal -DskipTests=false "-Dtest=RedemptionOrderServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected: BUILD SUCCESS.

- [x] **Step 4: Run backend compile**

Run:

```powershell
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-admin -am compile -Plocal -DskipTests
```

Expected: BUILD SUCCESS.

- [x] **Step 5: Commit backend implementation**

```powershell
git add backend/gameluck-modules/gameluck-redemption/src/main/java/com/gameluck/redemption/service/impl/RedemptionOrderServiceImpl.java backend/gameluck-admin/src/main/resources/i18n/messages.properties backend/gameluck-admin/src/main/resources/i18n/messages_zh_CN.properties backend/gameluck-admin/src/main/resources/i18n/messages_en_US.properties
git commit -m "fix(redemption): require reason for rejected reviews"
```

## Task 3: Admin UI Review Workflow

**Files:**
- Modify: `admin-ui/src/views/redemption/order/index.vue`

- [x] **Step 1: Add status quick filters to the table header**

Inside the `<template #header>` row, after the add button column and before `right-toolbar`, add:

```vue
<el-col :span="12">
  <el-segmented v-model="queryParams.status" :options="statusFilterOptions" @change="handleStatusFilterChange" />
</el-col>
```

- [x] **Step 2: Make the audit form validate reject reason**

Change the audit dialog form from:

```vue
<el-form :model="auditForm" label-width="90px">
```

to:

```vue
<el-form ref="auditFormRef" :model="auditForm" :rules="auditRules" label-width="90px">
```

Change the confirm button from:

```vue
<el-button :type="auditDialog.action === 'approve' ? 'success' : 'danger'" @click="submitAudit">{{ t('common.confirm') }}</el-button>
```

to:

```vue
<el-button :type="auditDialog.action === 'approve' ? 'success' : 'danger'" :loading="auditSubmitting" @click="submitAudit">{{ t('common.confirm') }}</el-button>
```

- [x] **Step 3: Add state and computed options in the script**

After `const orderFormRef = ref<ElFormInstance>();`, add:

```ts
const auditFormRef = ref<ElFormInstance>();
```

After `const detail = ref<Partial<RedemptionOrderVO>>({});`, add:

```ts
const auditSubmitting = ref(false);
```

Change the initial `queryParams` status from empty string to `PENDING`:

```ts
const queryParams = ref<RedemptionOrderQuery>({
  pageNum: 1,
  pageSize: 10,
  redemptionOrderNo: '',
  memberId: '',
  currencyCode: '',
  status: 'PENDING'
});
```

After `const auditTitle = computed(...)`, add:

```ts
const statusFilterOptions = computed(() => [
  { label: t('redemptionOrder.filters.pending'), value: 'PENDING' },
  { label: t('redemptionOrder.filters.approved'), value: 'APPROVED' },
  { label: t('redemptionOrder.filters.rejected'), value: 'REJECTED' },
  { label: t('redemptionOrder.filters.failed'), value: 'FAILED' },
  { label: t('redemptionOrder.filters.all'), value: '' }
]);

const auditRules = computed(() => ({
  auditReason: [
    {
      validator: (_rule: unknown, value: string, callback: (error?: Error) => void) => {
        if (auditDialog.action === 'reject' && !String(value || '').trim()) {
          callback(new Error(t('redemptionOrder.rules.rejectReason')));
          return;
        }
        callback();
      },
      trigger: 'blur'
    }
  ]
}));
```

- [x] **Step 4: Add quick filter handler and reset behavior**

Add this function after `handleQuery`:

```ts
const handleStatusFilterChange = () => {
  queryParams.value.pageNum = 1;
  getList();
};
```

Change `resetQuery` to restore the pending default:

```ts
const resetQuery = () => {
  queryFormRef.value?.resetFields();
  queryParams.value.status = 'PENDING';
  handleQuery();
};
```

- [x] **Step 5: Harden audit submission**

Change `openAudit` to clear validation:

```ts
const openAudit = (row: RedemptionOrderVO, action: 'approve' | 'reject') => {
  auditForm.id = row.id;
  auditForm.redemptionOrderNo = row.redemptionOrderNo;
  auditForm.auditReason = '';
  auditDialog.action = action;
  auditDialog.visible = true;
  nextTick(() => auditFormRef.value?.clearValidate());
};
```

Replace `submitAudit` with:

```ts
const submitAudit = async () => {
  if (!auditForm.id || !auditDialog.action || auditSubmitting.value) {
    return;
  }
  const valid = await auditFormRef.value?.validate().catch(() => false);
  if (valid === false) {
    return;
  }

  const actionText = auditDialog.action === 'approve' ? t('redemptionOrder.actions.approve') : t('redemptionOrder.actions.reject');
  await proxy?.$modal.confirm(t('redemptionOrder.confirm.audit', { action: actionText }));
  auditSubmitting.value = true;
  try {
    if (auditDialog.action === 'approve') {
      await approveRedemptionOrder(auditForm.id, auditForm);
    } else {
      await rejectRedemptionOrder(auditForm.id, auditForm);
    }
    proxy?.$modal.msgSuccess(t('common.success.operate'));
    auditDialog.visible = false;
    await getList();
  } finally {
    auditSubmitting.value = false;
  }
};
```

- [x] **Step 6: Run frontend build to catch TypeScript and template errors**

Run:

```powershell
pnpm --dir admin-ui build:dev
```

Expected before i18n additions: this may fail if new `redemptionOrder.filters.*` or `redemptionOrder.rules.rejectReason` keys are missing from the i18n guard. Continue to Task 4.

## Task 4: Admin UI i18n Additions

**Files:**
- Modify: `admin-ui/src/lang/zh_CN.ts`
- Modify: `admin-ui/src/lang/en_US.ts`

- [x] **Step 1: Add Chinese frontend keys**

In `admin-ui/src/lang/zh_CN.ts`, inside `redemptionOrder`, add this block after `status`:

```ts
filters: {
  pending: '待审核',
  approved: '已通过',
  rejected: '已拒绝',
  failed: '失败',
  all: '全部'
},
```

Inside `redemptionOrder.rules`, add:

```ts
rejectReason: '审核拒绝时必须填写原因'
```

Keep commas valid. The final `rules` object should include `memberId`, `currency`, `amount`, and `rejectReason`.

- [x] **Step 2: Add English frontend keys**

In `admin-ui/src/lang/en_US.ts`, inside `redemptionOrder`, add this block after `status`:

```ts
filters: {
  pending: 'Pending',
  approved: 'Approved',
  rejected: 'Rejected',
  failed: 'Failed',
  all: 'All'
},
```

Inside `redemptionOrder.rules`, add:

```ts
rejectReason: 'A rejection reason is required'
```

Keep commas valid. The final `rules` object should include `memberId`, `currency`, `amount`, and `rejectReason`.

- [x] **Step 3: Run i18n guard**

Run:

```powershell
pnpm --dir admin-ui check:i18n
```

Expected: command passes with no missing-key or hardcoded visible Chinese failures.

- [x] **Step 4: Run frontend build**

Run:

```powershell
pnpm --dir admin-ui build:dev
```

Expected: build passes. Existing Vite large chunk warnings are acceptable.

- [x] **Step 5: Commit frontend workflow changes**

```powershell
git add admin-ui/src/views/redemption/order/index.vue admin-ui/src/lang/zh_CN.ts admin-ui/src/lang/en_US.ts
git commit -m "feat(admin): focus redemption review workflow"
```

## Task 5: Final Verification And Planning Closure

**Files:**
- Modify: `progress.md`
- Modify: `task_plan.md`

- [x] **Step 1: Run full targeted verification**

Run:

```powershell
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-modules/gameluck-redemption -am -Plocal -DskipTests=false "-Dtest=RedemptionOrderServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
pnpm --dir admin-ui check:i18n
pnpm --dir admin-ui build:dev
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-admin -am compile -Plocal -DskipTests
```

Expected:

```text
RedemptionOrderServiceImplTest: BUILD SUCCESS
admin-ui check:i18n: pass
admin-ui build:dev: pass
gameluck-admin compile: BUILD SUCCESS
```

- [x] **Step 2: Optional runtime smoke when backend and Admin UI are running**

Use the existing local startup flow. In the browser, log into Admin UI and open the redemption order page.

Expected manual checks:

```text
Default status filter is Pending.
Switching status filters reloads the table.
Approved, rejected, and failed rows do not show approve/reject action buttons.
Reject without reason is blocked by the form.
Approve succeeds for a pending order and writes a settlement transaction number.
Reject succeeds for a pending order with reason and writes a release transaction number.
```

- [x] **Step 3: Update progress log**

Append this section to `progress.md`:

```markdown
- Completed Phase 4 admin redemption review closed-loop hardening:
  - Added service tests for duplicate review prevention, reject reason validation, approval success, rejection success, and wallet failure protection.
  - Added backend reject reason validation and i18n messages.
  - Updated admin redemption order page with pending default filter, status quick filters, reject reason validation, and submit locking.
  - Verification passed:
    - `C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-modules/gameluck-redemption -am -Plocal -DskipTests=false "-Dtest=RedemptionOrderServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
    - `pnpm --dir admin-ui check:i18n`
    - `pnpm --dir admin-ui build:dev`
    - `C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-admin -am compile -Plocal -DskipTests`
```

- [x] **Step 4: Mark Phase 4 complete in `task_plan.md`**

Change this row:

```markdown
| 15. Phase 4 B端兑换审核后台闭环 | in_progress | 加固后台兑换审核列表、详情、通过/拒绝、审核原因和钱包闭环验收 | docs/superpowers/specs/2026-07-10-admin-redemption-review-design.md |
```

to:

```markdown
| 15. Phase 4 B端兑换审核后台闭环 | complete | 加固后台兑换审核列表、详情、通过/拒绝、审核原因和钱包闭环验收 | docs/superpowers/specs/2026-07-10-admin-redemption-review-design.md、docs/superpowers/plans/2026-07-10-admin-redemption-review.md |
```

- [x] **Step 5: Commit closure docs**

```powershell
git add progress.md task_plan.md
git commit -m "docs: close admin redemption review phase"
```

- [x] **Step 6: Check final worktree**

Run:

```powershell
git status --short
```

Expected: only unrelated pre-existing files may remain, such as `h5/package-lock.json`. Do not include unrelated files in Phase 4 commits.

## Self-Review

- Spec coverage: backend constraints, reject reason validation, default pending focus, quick filters, submit locking, i18n, permissions, and verification are covered by Tasks 1-5.
- Completeness scan: this plan does not contain incomplete marker text.
- Type consistency: frontend uses existing `RedemptionOrderQuery.status`, existing `RedemptionOrderForm.auditReason`, and existing API functions. Backend uses existing `RedemptionOrderServiceImpl` signatures and existing wallet transaction statuses.
