# C-Side Product Polish Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Tighten the guest-first C-side product experience with localized account/auth/guest-gate copy, explicit shell states, route-compatible modal auth tests, and browser verification.

**Architecture:** Keep the existing backend contracts and C-side route shell. Add missing i18n keys, replace hard-coded C-side copy with `$t(...)`, improve `AppShell` boot/error states, and update tests around the production modal-auth route behavior.

**Tech Stack:** Vue 3, Vue Router, Pinia, Vitest, Playwright, Spring Boot verification only.

---

## File Structure

- Modify `frontend/src/i18n/messages.ts`: add `account.*`, `auth.*`, `guestGate.*`, `nav.inbox` keys to both `en` and `zh-CN`.
- Modify `frontend/src/views/app/AppShell.vue`: use localized labels, explicit loading/error states, retry button, and localized nav.
- Modify `frontend/src/components/AuthModal.vue`: replace hard-coded modal labels with i18n keys.
- Modify `frontend/src/views/app/AppStore.vue`: replace hard-coded guest gate copy with i18n.
- Modify `frontend/src/views/app/AppKyc.vue`: replace hard-coded guest gate copy with i18n.
- Modify `frontend/src/views/app/AppRedemption.vue`: replace hard-coded guest gate copy with i18n.
- Modify `frontend/src/views/app/P1Pages.test.ts`: add/adjust tests for localized shell and guest gates.
- Modify `frontend/src/components/AuthModal.test.ts`: assert localized auth modal copy.
- Modify `frontend/e2e/p0a-demo.spec.ts`: assert new localized shell/auth route behavior where needed.
- Modify `progress.md`, `findings.md`, `task_plan.md`: record completion and verification evidence.

## Task 1: Localized Account And Auth Copy

**Files:**
- Modify: `frontend/src/i18n/messages.ts`
- Test: `frontend/src/i18n/i18n.test.ts`

- [ ] **Step 1: Write failing i18n tests**

Add assertions in `frontend/src/i18n/i18n.test.ts`:

```ts
it('contains C-side account and auth polish keys in both locales', () => {
  for (const locale of ['en', 'zh-CN'] as const) {
    i18n.setLocale(locale)
    expect(i18n.t('account.guest')).toBeTruthy()
    expect(i18n.t('account.loading')).toBeTruthy()
    expect(i18n.t('account.retry')).toBeTruthy()
    expect(i18n.t('auth.bindAccount')).toBeTruthy()
    expect(i18n.t('auth.signInSwitchHint')).toBeTruthy()
    expect(i18n.t('guestGate.storeTitle')).toBeTruthy()
    expect(i18n.t('guestGate.kycBody')).toBeTruthy()
    expect(i18n.t('guestGate.redemptionBody')).toBeTruthy()
    expect(i18n.t('nav.inbox')).toBeTruthy()
  }
})
```

- [ ] **Step 2: Run RED**

Run:

```powershell
cd frontend
npm run test -- --run src/i18n/i18n.test.ts --pool=threads --maxWorkers=1
```

Expected: FAIL because the new keys return missing-key output or empty values.

- [ ] **Step 3: Add i18n keys**

Add English keys:

```ts
'account.guest': 'Guest',
'account.loading': 'Starting session',
'account.retry': 'Retry',
'account.sessionError': 'Session could not start.',
'account.formal': 'Account',
'auth.bindAccount': 'Bind account',
'auth.signIn': 'Sign in',
'auth.close': 'Close',
'auth.signInSwitchHint': 'Sign in switches to your existing account.',
'guestGate.storeTitle': 'Bind account before purchase',
'guestGate.storeBody': 'Guest play is available, but purchases require a verified account before payment starts.',
'guestGate.kycTitle': 'Bind account before identity review',
'guestGate.kycBody': 'Identity verification is tied to a formal account and cannot be submitted as a guest.',
'guestGate.redemptionTitle': 'Bind account before redemption',
'guestGate.redemptionBody': 'Redemption requests require a formal account, KYC review, and operational approval.',
'nav.inbox': 'Inbox',
```

Add Chinese keys:

```ts
'account.guest': '游客',
'account.loading': '正在启动会话',
'account.retry': '重试',
'account.sessionError': '会话启动失败。',
'account.formal': '账号',
'auth.bindAccount': '绑定账号',
'auth.signIn': '登录',
'auth.close': '关闭',
'auth.signInSwitchHint': '登录会切换到已有正式账号。',
'guestGate.storeTitle': '购买前请先绑定账号',
'guestGate.storeBody': '游客可以继续体验游戏，但购买需要先绑定正式账号。',
'guestGate.kycTitle': '身份审核前请先绑定账号',
'guestGate.kycBody': '身份审核必须关联正式账号，游客状态不能提交。',
'guestGate.redemptionTitle': '兑换前请先绑定账号',
'guestGate.redemptionBody': '兑换请求需要正式账号、KYC 审核和运营审批。',
'nav.inbox': '消息',
```

- [ ] **Step 4: Run GREEN**

Run the same i18n test command. Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add frontend/src/i18n/messages.ts frontend/src/i18n/i18n.test.ts
git commit -m "feat: add cside polish i18n keys"
```

## Task 2: AppShell Explicit Account States

**Files:**
- Modify: `frontend/src/views/app/AppShell.vue`
- Test: `frontend/src/views/app/P1Pages.test.ts`

- [ ] **Step 1: Write failing shell tests**

In `P1Pages.test.ts`, add or adjust tests to assert:

```ts
expect(wrapper.text()).toContain('Guest')
expect(wrapper.text()).not.toContain('User ')
expect(wrapper.find('.bottom-nav').text()).toContain('Inbox')
```

Add a boot failure test:

```ts
it('shows retry when guest session boot fails', async () => {
  vi.spyOn(globalThis, 'fetch').mockImplementation(() => json({ message: 'guest failed' }, 500))
  const wrapper = mount(AppShell, { global: shellGlobal() })
  await flushPromises()
  expect(wrapper.text()).toContain('Session could not start')
  expect(wrapper.text()).toContain('Retry')
})
```

- [ ] **Step 2: Run RED**

Run:

```powershell
cd frontend
npm run test -- --run src/views/app/P1Pages.test.ts --pool=threads --maxWorkers=1
```

Expected: FAIL because `AppShell` still uses hard-coded fallback states and lacks retry UI.

- [ ] **Step 3: Implement shell states**

Update `AppShell.vue`:

```ts
const booting = ref(false)
const accountLabel = computed(() => {
  if (booting.value && !session.userId) return i18n.t('account.loading')
  if (session.isGuest) return i18n.t('account.guest')
  if (session.email) return session.email
  if (session.userId) return `${i18n.t('account.formal')} ${session.userId}`
  return i18n.t('account.loading')
})

async function bootGuest() {
  booting.value = true
  bootError.value = ''
  try {
    await session.ensureGuestSession()
  } catch {
    bootError.value = i18n.t('account.sessionError')
  } finally {
    booting.value = false
  }
}
```

Use template labels:

```vue
<button v-if="bootError" type="button" class="small-action" @click="bootGuest">
  {{ $t('account.retry') }}
</button>
<button v-if="session.isGuest" type="button" class="small-action" @click="openAuth('register')">
  {{ $t('auth.bindAccount') }}
</button>
<button type="button" class="small-action ghost" @click="openAuth('login')">
  {{ $t('auth.signIn') }}
</button>
<RouterLink to="/app/inbox">{{ $t('nav.inbox') }}</RouterLink>
```

- [ ] **Step 4: Run GREEN**

Run the same P1Pages test command. Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add frontend/src/views/app/AppShell.vue frontend/src/views/app/P1Pages.test.ts
git commit -m "feat: polish cside account shell states"
```

## Task 3: Localized Auth Modal And Guest Gates

**Files:**
- Modify: `frontend/src/components/AuthModal.vue`
- Modify: `frontend/src/views/app/AppStore.vue`
- Modify: `frontend/src/views/app/AppKyc.vue`
- Modify: `frontend/src/views/app/AppRedemption.vue`
- Test: `frontend/src/components/AuthModal.test.ts`
- Test: `frontend/src/views/app/P1Pages.test.ts`

- [ ] **Step 1: Write failing component tests**

Update tests to assert localized labels:

```ts
expect(wrapper.text()).toContain('Bind account')
expect(wrapper.text()).toContain('Sign in')
expect(wrapper.text()).toContain('Sign in switches to your existing account.')
```

For guest gates:

```ts
expect(wrapper.text()).toContain('Bind account before purchase')
expect(wrapper.text()).toContain('Guest play is available')
expect(wrapper.text()).toContain('Bind account before identity review')
expect(wrapper.text()).toContain('Bind account before redemption')
```

- [ ] **Step 2: Run RED**

Run:

```powershell
cd frontend
npm run test -- --run src/components/AuthModal.test.ts src/views/app/P1Pages.test.ts --pool=threads --maxWorkers=1
```

Expected: FAIL where hard-coded copy is missing the new hint/title pattern.

- [ ] **Step 3: Replace hard-coded labels**

In `AuthModal.vue`, replace:

```vue
Bind account
Sign in
x
```

with:

```vue
{{ $t('auth.bindAccount') }}
{{ $t('auth.signIn') }}
{{ $t('auth.close') }}
```

Add login hint:

```vue
<p v-if="mode === 'login'" class="notice">{{ $t('auth.signInSwitchHint') }}</p>
```

In Store/KYC/Redemption guest gates, use:

```vue
<strong>{{ $t('guestGate.storeTitle') }}</strong>
<span>{{ $t('guestGate.storeBody') }}</span>
<button>{{ $t('auth.bindAccount') }}</button>
```

and equivalent `kyc*` / `redemption*` keys.

- [ ] **Step 4: Run GREEN**

Run the same component test command. Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add frontend/src/components/AuthModal.vue frontend/src/components/AuthModal.test.ts frontend/src/views/app/AppStore.vue frontend/src/views/app/AppKyc.vue frontend/src/views/app/AppRedemption.vue frontend/src/views/app/P1Pages.test.ts
git commit -m "feat: localize cside auth and guest gates"
```

## Task 4: E2E And Browser Acceptance

**Files:**
- Modify: `frontend/e2e/p0a-demo.spec.ts`
- Modify: `progress.md`
- Modify: `findings.md`
- Modify: `task_plan.md`

- [ ] **Step 1: Update Playwright assertions**

Update e2e to assert:

```ts
await expect(page.getByText('Guest')).toBeVisible()
await expect(page.locator('[data-test="auth-bind-submit"]')).toBeVisible()
await expect(page.locator('[data-test="auth-login-submit"]')).toBeVisible()
await expect(page.getByText('Bind account before purchase')).toBeVisible()
```

For Chinese browser locale, assert at least one shell label:

```ts
await expect(chinesePage.getByText('游客')).toBeVisible()
```

- [ ] **Step 2: Run full verification**

Run:

```powershell
cd backend
.\gradlew.bat --no-daemon test
cd ..\frontend
npm run test -- --run --pool=threads --maxWorkers=1
npm run build
npx playwright test
```

Expected:

- Backend: `BUILD SUCCESSFUL`
- Vitest: all files pass
- Build: `built`
- Playwright: all tests pass

- [ ] **Step 3: Browser screenshots**

Start or reuse a fresh frontend port, then capture desktop and mobile screenshots for:

- `/app`
- `/app?auth=register`
- `/app?auth=login`
- `/app/store`

Save under `artifacts/cside-polish/`.

- [ ] **Step 4: Update progress docs**

Append completion evidence to:

- `progress.md`
- `findings.md`
- `task_plan.md`

- [ ] **Step 5: Commit and push**

```powershell
git add frontend/e2e/p0a-demo.spec.ts progress.md findings.md task_plan.md
git commit -m "docs: record cside product polish completion"
git push origin develop/p0a
```

## Self-Review

- Spec coverage: i18n, AppShell states, AuthModal, guest gates, E2E, browser acceptance are covered.
- Placeholder scan: no TBD/TODO placeholders.
- Type consistency: key names are consistent across tasks.
- Non-goals: no backend redesign, phone binding, OAuth, B-side expansion, or guest merge included.
