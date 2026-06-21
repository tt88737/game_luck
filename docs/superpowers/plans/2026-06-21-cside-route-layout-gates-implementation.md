# C-Side Route Layout Gates Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rework the TangLuck C-side app to use production routes and bottom tabs `Store | Promo | Lobby | Inbox | Me`, with old `/app/*` URLs redirected safely.

**Architecture:** Keep `AppShell.vue` as the shared C-side wrapper and move canonical user routes to top-level route groups. Reuse existing pages for Store, Promo, Lobby, Inbox, Wallet, KYC, Redemption, and add a focused `AppMe.vue` aggregator for account, wallet, redeem, KYC, legal, and support entry points.

**Tech Stack:** Vue 3, Vue Router, Pinia, Vitest, Vue Test Utils, Playwright.

---

## File Map

- Modify: `frontend/src/router/index.ts`
  - Define canonical C-side routes under `AppShell`.
  - Add compatibility redirects from `/app/*`.
- Modify: `frontend/src/views/app/AppShell.vue`
  - Change brand and bottom nav to production route model.
- Create: `frontend/src/views/app/AppMe.vue`
  - Account and compliance aggregation page.
- Modify: `frontend/src/views/app/AppHome.vue`
  - Update internal links from `/app/*` to canonical routes.
- Modify: `frontend/src/views/app/AppActivity.vue`
  - Update visible framing from Activity to Promo where needed and expose AMOE entry.
- Modify: `frontend/src/i18n/messages.ts`
  - Add `nav.promo`, `nav.lobby`, `nav.me`, and `me.*` copy in English and Chinese.
- Modify: `frontend/src/views/app/P1Pages.test.ts`
  - Update shell expectations and add `AppMe` tests.
- Create: `frontend/src/router/routes.test.ts`
  - Test canonical route redirects and compatibility redirects.
- Modify: Playwright specs if they assert old bottom nav labels or `/app/*` paths.

---

### Task 1: Router Canonical Routes And Redirect Tests

**Files:**
- Create: `frontend/src/router/routes.test.ts`
- Modify: `frontend/src/router/index.ts`

- [ ] **Step 1: Write the failing route tests**

Create `frontend/src/router/routes.test.ts`:

```ts
import { describe, expect, it } from 'vitest'
import { router } from './index'

describe('C-side production routes', () => {
  it('redirects root and legacy app entry to lobby', async () => {
    await router.push('/')
    await router.isReady()
    expect(router.currentRoute.value.path).toBe('/lobby')

    await router.push('/app')
    await router.isReady()
    expect(router.currentRoute.value.path).toBe('/lobby')
  })

  it('redirects legacy app routes to canonical production routes', async () => {
    const redirects: Array<[string, string]> = [
      ['/app/store', '/store'],
      ['/app/activity', '/promo'],
      ['/app/wallet', '/me/wallet'],
      ['/app/redemption', '/me/redeem'],
      ['/app/kyc', '/me/kyc'],
      ['/app/inbox', '/inbox'],
      ['/app/slots/lucky_slots', '/lobby/slots/lucky_slots'],
    ]

    for (const [from, to] of redirects) {
      await router.push(from)
      await router.isReady()
      expect(router.currentRoute.value.path).toBe(to)
    }
  })

  it('opens auth modal through canonical lobby query redirects', async () => {
    await router.push('/app/register')
    await router.isReady()
    expect(router.currentRoute.value.path).toBe('/lobby')
    expect(router.currentRoute.value.query.auth).toBe('register')

    await router.push('/app/login')
    await router.isReady()
    expect(router.currentRoute.value.path).toBe('/lobby')
    expect(router.currentRoute.value.query.auth).toBe('login')
  })
})
```

- [ ] **Step 2: Run the route test and verify RED**

Run:

```bash
cd frontend
npm run test -- --run src/router/routes.test.ts --pool=threads --maxWorkers=1
```

Expected: FAIL because `/` and `/app` still resolve to old `/app` routes and canonical routes do not exist.

- [ ] **Step 3: Implement router changes**

Update `frontend/src/router/index.ts`:

```ts
const AppMe = () => import('../views/app/AppMe.vue')

const legacyRedirect = (path: string) => ({ path })

// routes:
{ path: '/', redirect: '/lobby' },
{
  path: '/',
  component: AppShell,
  children: [
    { path: 'store', component: AppStore },
    { path: 'promo', component: AppActivity },
    { path: 'lobby', component: AppHome },
    { path: 'lobby/slots/:gameCode', component: AppSlots },
    { path: 'inbox', component: AppInbox },
    { path: 'me', component: AppMe },
    { path: 'me/wallet', component: AppWallet },
    { path: 'me/redeem', component: AppRedemption },
    { path: 'me/kyc', component: AppKyc },
  ],
},
{ path: '/app', redirect: '/lobby' },
{ path: '/app/register', redirect: { path: '/lobby', query: { auth: 'register' } } },
{ path: '/app/login', redirect: { path: '/lobby', query: { auth: 'login' } } },
{ path: '/app/store', redirect: legacyRedirect('/store') },
{ path: '/app/activity', redirect: legacyRedirect('/promo') },
{ path: '/app/wallet', redirect: legacyRedirect('/me/wallet') },
{ path: '/app/redemption', redirect: legacyRedirect('/me/redeem') },
{ path: '/app/kyc', redirect: legacyRedirect('/me/kyc') },
{ path: '/app/inbox', redirect: legacyRedirect('/inbox') },
{ path: '/app/slots/:gameCode', redirect: to => `/lobby/slots/${String(to.params.gameCode)}` },
```

- [ ] **Step 4: Run route test and verify GREEN**

Run:

```bash
cd frontend
npm run test -- --run src/router/routes.test.ts --pool=threads --maxWorkers=1
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/router/index.ts frontend/src/router/routes.test.ts
git commit -m "feat: add cside canonical route redirects"
```

---

### Task 2: Shell Bottom Navigation

**Files:**
- Modify: `frontend/src/views/app/P1Pages.test.ts`
- Modify: `frontend/src/views/app/AppShell.vue`
- Modify: `frontend/src/i18n/messages.ts`

- [ ] **Step 1: Update the shell test first**

In `frontend/src/views/app/P1Pages.test.ts`, change the shell test expectations:

```ts
expect(wrapper.text()).toContain('Store')
expect(wrapper.text()).toContain('Promo')
expect(wrapper.text()).toContain('Lobby')
expect(wrapper.text()).toContain('Inbox')
expect(wrapper.text()).toContain('Me')
expect(wrapper.find('.bottom-nav').text()).not.toContain('Slots')
expect(wrapper.find('.bottom-nav').text()).not.toContain('Activity')
expect(wrapper.find('.bottom-nav').text()).not.toContain('Wallet')
expect(wrapper.find('.bottom-nav').text()).not.toContain('Redeem')
expect(wrapper.find('a[href="/lobby"]').exists()).toBe(true)
```

- [ ] **Step 2: Run the shell test and verify RED**

Run:

```bash
cd frontend
npm run test -- --run src/views/app/P1Pages.test.ts -t "renders the C-side shell" --pool=threads --maxWorkers=1
```

Expected: FAIL because nav still uses old labels and routes.

- [ ] **Step 3: Implement shell nav and i18n labels**

In `frontend/src/views/app/AppShell.vue`:

```vue
<RouterLink class="brand-mark" to="/lobby">Tang Luck</RouterLink>
...
<nav class="bottom-nav" aria-label="App navigation">
  <RouterLink to="/store">{{ $t('nav.store') }}</RouterLink>
  <RouterLink to="/promo">{{ $t('nav.promo') }}</RouterLink>
  <RouterLink to="/lobby">{{ $t('nav.lobby') }}</RouterLink>
  <RouterLink to="/inbox">{{ $t('nav.inbox') }}</RouterLink>
  <RouterLink to="/me">{{ $t('nav.me') }}</RouterLink>
</nav>
```

In `frontend/src/i18n/messages.ts`, add English and Chinese keys:

```ts
'nav.promo': 'Promo',
'nav.lobby': 'Lobby',
'nav.me': 'Me',
```

```ts
'nav.promo': '促销',
'nav.lobby': '大厅',
'nav.me': '我的',
```

- [ ] **Step 4: Run the shell test and verify GREEN**

Run:

```bash
cd frontend
npm run test -- --run src/views/app/P1Pages.test.ts -t "renders the C-side shell" --pool=threads --maxWorkers=1
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/views/app/AppShell.vue frontend/src/views/app/P1Pages.test.ts frontend/src/i18n/messages.ts
git commit -m "feat: align cside bottom navigation"
```

---

### Task 3: Me Aggregation Page

**Files:**
- Modify: `frontend/src/views/app/P1Pages.test.ts`
- Create: `frontend/src/views/app/AppMe.vue`
- Modify: `frontend/src/i18n/messages.ts`

- [ ] **Step 1: Write the failing Me page test**

Add to `frontend/src/views/app/P1Pages.test.ts`:

```ts
import AppMe from './AppMe.vue'
```

Add test:

```ts
it('renders Me as account wallet compliance aggregation for guests', async () => {
  localStorage.setItem('tangluck_user_id', '77')
  localStorage.setItem('tangluck_account_type', 'guest')

  const wrapper = mount(AppMe, { global })
  await flushPromises()

  expect(wrapper.text()).toContain('Guest mode')
  expect(wrapper.text()).toContain('Bind account')
  expect(wrapper.text()).toContain('Wallet')
  expect(wrapper.text()).toContain('Redeem')
  expect(wrapper.text()).toContain('KYC')
  expect(wrapper.text()).toContain('AMOE')
  expect(wrapper.find('a[href="/me/wallet"]').exists()).toBe(true)
  expect(wrapper.find('a[href="/me/redeem"]').exists()).toBe(true)
  expect(wrapper.find('a[href="/me/kyc"]').exists()).toBe(true)
  expect(wrapper.find('a[href="/promo/amoe"]').exists()).toBe(true)
})
```

- [ ] **Step 2: Run the Me test and verify RED**

Run:

```bash
cd frontend
npm run test -- --run src/views/app/P1Pages.test.ts -t "renders Me" --pool=threads --maxWorkers=1
```

Expected: FAIL because `AppMe.vue` does not exist.

- [ ] **Step 3: Implement AppMe and i18n keys**

Create `frontend/src/views/app/AppMe.vue` with:

```vue
<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink } from 'vue-router'
import { useSessionStore } from '../../stores/session'

const session = useSessionStore()
const accountTitle = computed(() => session.isGuest ? 'Guest mode' : (session.email || `Account ${session.userId}`))

function openAuth(mode: 'register' | 'login') {
  window.dispatchEvent(new CustomEvent('open-auth-modal', { detail: { mode } }))
}
</script>

<template>
  <main class="app-screen me-screen">
    <header class="app-header">
      <div>
        <p class="eyebrow">{{ $t('me.eyebrow') }}</p>
        <h1>{{ $t('nav.me') }}</h1>
      </div>
    </header>

    <section class="section-block me-account-panel">
      <div>
        <span class="status-tag" :class="{ active: !session.isGuest }">{{ accountTitle }}</span>
        <p>{{ session.isGuest ? $t('me.guestBody') : $t('me.formalBody') }}</p>
      </div>
      <div v-if="session.isGuest" class="me-action-row">
        <button type="button" @click="openAuth('register')">{{ $t('auth.bindAccount') }}</button>
        <button type="button" class="secondary" @click="openAuth('login')">{{ $t('auth.signIn') }}</button>
      </div>
    </section>

    <section class="section-block">
      <div class="section-title">
        <h2>{{ $t('me.walletTitle') }}</h2>
        <span>{{ $t('me.walletHint') }}</span>
      </div>
      <div class="me-link-grid">
        <RouterLink to="/me/wallet"><strong>{{ $t('common.wallet') }}</strong><span>{{ $t('me.walletBody') }}</span></RouterLink>
        <RouterLink to="/me/redeem"><strong>{{ $t('nav.redeem') }}</strong><span>{{ $t('me.redeemBody') }}</span></RouterLink>
        <RouterLink to="/me/kyc"><strong>{{ $t('common.kyc') }}</strong><span>{{ $t('me.kycBody') }}</span></RouterLink>
      </div>
    </section>

    <section class="section-block">
      <div class="section-title">
        <h2>{{ $t('me.legalTitle') }}</h2>
        <span>{{ $t('me.legalHint') }}</span>
      </div>
      <div class="me-link-grid">
        <RouterLink to="/promo/amoe"><strong>AMOE</strong><span>{{ $t('home.amoeNoPurchase') }}</span></RouterLink>
        <a href="/legal/rules-v1"><strong>{{ $t('home.rules') }}</strong><span>{{ $t('home.requiredLinks') }}</span></a>
        <a href="mailto:support@tangluck.local"><strong>{{ $t('me.supportTitle') }}</strong><span>{{ $t('me.supportBody') }}</span></a>
      </div>
    </section>
  </main>
</template>
```

Add English and Chinese `me.*` i18n keys.

- [ ] **Step 4: Run the Me test and verify GREEN**

Run:

```bash
cd frontend
npm run test -- --run src/views/app/P1Pages.test.ts -t "renders Me" --pool=threads --maxWorkers=1
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/views/app/AppMe.vue frontend/src/views/app/P1Pages.test.ts frontend/src/i18n/messages.ts
git commit -m "feat: add cside me aggregation page"
```

---

### Task 4: Canonical Internal Links And Promo Naming

**Files:**
- Modify: `frontend/src/views/app/AppHome.vue`
- Modify: `frontend/src/views/app/AppActivity.vue`
- Modify: `frontend/src/i18n/messages.ts`
- Modify: tests that assert old copy or links

- [ ] **Step 1: Update tests for canonical internal links and Promo copy**

In existing page tests, assert:

```ts
expect(wrapper.find('a[href="/store"]').exists()).toBe(true)
expect(wrapper.find('a[href="/promo"]').exists()).toBe(true)
expect(wrapper.find('a[href="/me/redeem"]').exists()).toBe(true)
expect(wrapper.text()).toContain('Promo')
expect(wrapper.text()).not.toContain('Activity center')
```

- [ ] **Step 2: Run relevant tests and verify RED**

Run:

```bash
cd frontend
npm run test -- --run src/views/app/P1Pages.test.ts src/views/app/AppHome.test.ts --pool=threads --maxWorkers=1
```

Expected: FAIL while old `/app/*` links and Activity labels remain.

- [ ] **Step 3: Update links and labels**

Replace visible C-side internal links:

```text
/app/store -> /store
/app/activity -> /promo
/app/redemption -> /me/redeem
/app/wallet -> /me/wallet
/app/kyc -> /me/kyc
/app/slots/lucky_slots -> /lobby/slots/lucky_slots
```

Update user-facing keys so Activity surfaces as Promo:

```ts
'activity.title': 'Promo',
'activity.subtitle': 'Daily bonus, tasks, coupon, campaigns, and no-purchase entry.',
```

- [ ] **Step 4: Run relevant tests and verify GREEN**

Run:

```bash
cd frontend
npm run test -- --run src/views/app/P1Pages.test.ts src/views/app/AppHome.test.ts --pool=threads --maxWorkers=1
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/views/app/AppHome.vue frontend/src/views/app/AppActivity.vue frontend/src/i18n/messages.ts frontend/src/views/app/P1Pages.test.ts frontend/src/views/app/AppHome.test.ts
git commit -m "feat: migrate cside links to production routes"
```

---

### Task 5: Full Verification And Browser Acceptance

**Files:**
- Modify: `progress.md`, `findings.md`, `task_plan.md` if present and used in this repo.
- No production code unless verification exposes a bug; if so, write a failing test first.

- [ ] **Step 1: Run backend tests**

```bash
cd backend
.\gradlew.bat --no-daemon test
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 2: Run frontend unit tests**

```bash
cd frontend
npm run test -- --run --pool=threads --maxWorkers=1
```

Expected: all tests pass.

- [ ] **Step 3: Run frontend build**

```bash
cd frontend
npm run build
```

Expected: build succeeds.

- [ ] **Step 4: Run Playwright**

```bash
cd frontend
npx playwright test
```

Expected: all specs pass or any failure is documented with a fix/test.

- [ ] **Step 5: Browser screenshot acceptance**

Run the app on an available local port and inspect:

```text
/store
/promo
/lobby
/inbox
/me
```

Check desktop and mobile:

1. Bottom tabs are visible and ordered `Store | Promo | Lobby | Inbox | Me`.
2. `/lobby` is default.
3. Login/register are modal actions.
4. `Me` contains Wallet, Redeem, KYC, AMOE, Rules, Support entry points.
5. No obvious text overlap or broken responsive layout.

- [ ] **Step 6: Update progress docs**

Record:

```text
C-side route layout gates completed.
Canonical tabs: Store | Promo | Lobby | Inbox | Me.
Legacy /app routes redirect to canonical production routes.
Verification commands and results.
```

- [ ] **Step 7: Commit and push**

```bash
git add .
git reset -- Tang+Luck_+Casino+Slots_1.0.51_APKPure.xapk artifacts TangLuck最终交付包/TangLuck完整可上线产品C端B端路由布局与门禁设计.md
git commit -m "feat: align cside route layout gates"
git push origin develop/p0a
```

Expected: commit succeeds and push updates `origin/develop/p0a`.

---

## Self-Review

- Spec coverage: canonical routes, compatibility redirects, bottom tabs, Me page, Promo naming, tests, and browser verification are covered.
- Placeholder scan: no implementation step relies on undefined future work.
- Type consistency: routes use existing Vue Router and Vue Test Utils patterns in this repo.
