# Admin Ops Navigation And C-Side States Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rework the admin navigation into production operating groups mapped to the C-side Store, Promo, Lobby, Inbox, and Me routes.

**Architecture:** Keep existing admin pages and routes. Replace the flat `AdminNav.vue` item array with grouped navigation metadata, add i18n keys for group names and C-side impact labels, and adjust CSS for desktop/mobile grouped navigation. Add unit and Playwright coverage before implementation.

**Tech Stack:** Vue 3, Vue Router, Vue Test Utils, Vitest, Playwright, CSS.

---

## File Map

- Modify: `frontend/src/components/AdminNav.vue`
  - Change flat nav to grouped business-domain sections.
- Modify: `frontend/src/i18n/messages.ts`
  - Add English and Chinese admin group names, impact labels, and planned status copy.
- Modify: `frontend/src/style.css`
  - Style grouped desktop left nav and compact mobile grouped nav.
- Modify: `frontend/src/views/admin/AdminCampaigns.test.ts`
  - Add tests for grouped admin navigation and Chinese locale.
- Modify: `frontend/e2e/p0a-demo.spec.ts`
  - Update assertions for grouped nav where needed.
- Create: `frontend/e2e/admin-ops-navigation.spec.ts`
  - Browser screenshot and route-link checks for desktop/mobile grouped nav.
- Modify: `progress.md`, `task_plan.md`, `findings.md`
  - Record completion and verification results.

---

### Task 1: Admin Navigation Unit Tests

**Files:**
- Modify: `frontend/src/views/admin/AdminCampaigns.test.ts`
- Modify: `frontend/src/components/AdminNav.vue`
- Modify: `frontend/src/i18n/messages.ts`

- [ ] **Step 1: Write failing grouped nav test**

Add assertions to the existing `uses the formal admin navigation across operations modules` test:

```ts
expect(wrapper.text()).toContain('User & Guest')
expect(wrapper.text()).toContain('Store & Packages')
expect(wrapper.text()).toContain('Promo & Rewards')
expect(wrapper.text()).toContain('Game Lobby')
expect(wrapper.text()).toContain('Inbox & Notification')
expect(wrapper.text()).toContain('Wallet & Ledger')
expect(wrapper.text()).toContain('Redemption')
expect(wrapper.text()).toContain('KYC & Risk')
expect(wrapper.text()).toContain('Compliance')
expect(wrapper.text()).toContain('CMS / Rules / AMOE')
expect(wrapper.text()).toContain('System / RBAC')
expect(wrapper.text()).toContain('Impacts Store')
expect(wrapper.text()).toContain('Impacts Promo')
expect(wrapper.text()).toContain('Impacts Lobby')
expect(wrapper.text()).toContain('Impacts Inbox')
expect(wrapper.text()).toContain('Impacts Me > Wallet')
expect(wrapper.findAll('.admin-nav-group').length).toBeGreaterThan(8)
expect(wrapper.findAll('.planned-nav-item').length).toBeGreaterThan(0)
```

- [ ] **Step 2: Update Chinese locale nav test first**

In the existing Chinese test, add:

```ts
expect(wrapper.text()).toContain('用户与游客')
expect(wrapper.text()).toContain('商店与商品包')
expect(wrapper.text()).toContain('促销与奖励')
expect(wrapper.text()).toContain('影响 Store')
expect(wrapper.text()).toContain('影响 Me > Wallet')
```

- [ ] **Step 3: Run tests and verify RED**

Run:

```bash
cd frontend
npm run test -- --run src/views/admin/AdminCampaigns.test.ts -t "formal admin navigation|localizes admin shell" --pool=threads --maxWorkers=1
```

Expected: FAIL because `AdminNav.vue` is still a flat list and i18n keys do not exist.

- [ ] **Step 4: Implement grouped navigation metadata**

Update `frontend/src/components/AdminNav.vue` with:

```vue
<script setup lang="ts">
import { RouterLink } from 'vue-router'

type NavItem = {
  to?: string
  key: string
  status?: 'live' | 'planned'
}

const navGroups: Array<{ titleKey: string; impactKey: string; items: NavItem[] }> = [
  { titleKey: 'admin.nav.dashboard', impactKey: 'admin.impact.operations', items: [{ to: '/admin', key: 'admin.dashboard' }, { key: 'admin.nav.biSummary', status: 'planned' }] },
  { titleKey: 'admin.nav.userGuest', impactKey: 'admin.impact.account', items: [{ to: '/admin/users', key: 'admin.users' }, { key: 'admin.nav.guestSessions', status: 'planned' }, { key: 'admin.nav.bindingHistory', status: 'planned' }] },
  { titleKey: 'admin.nav.storePackages', impactKey: 'admin.impact.store', items: [{ to: '/admin/packages', key: 'admin.packages' }, { to: '/admin/orders', key: 'admin.orders' }, { key: 'admin.nav.paymentConfig', status: 'planned' }] },
  { titleKey: 'admin.nav.promoRewards', impactKey: 'admin.impact.promo', items: [{ to: '/admin/campaigns', key: 'admin.campaigns' }, { to: '/admin/activity-dashboard', key: 'admin.activityCenter' }, { key: 'admin.nav.vipReferralTournament', status: 'planned' }] },
  { titleKey: 'admin.nav.gameLobby', impactKey: 'admin.impact.lobby', items: [{ to: '/admin/lobby', key: 'admin.lobby' }, { to: '/admin/games', key: 'admin.games' }, { to: '/admin/game-rounds', key: 'admin.gameRounds' }, { key: 'admin.nav.providerJackpot', status: 'planned' }] },
  { titleKey: 'admin.nav.inboxNotification', impactKey: 'admin.impact.inbox', items: [{ to: '/admin/notifications', key: 'admin.notifications' }, { key: 'admin.nav.messageTemplates', status: 'planned' }] },
  { titleKey: 'admin.nav.walletLedger', impactKey: 'admin.impact.wallet', items: [{ to: '/admin/wallet-ledger', key: 'admin.walletLedger' }, { key: 'admin.nav.scLotsPlaythrough', status: 'planned' }] },
  { titleKey: 'admin.nav.redemption', impactKey: 'admin.impact.redeem', items: [{ to: '/admin/redemptions', key: 'admin.redemptionRequests' }, { key: 'admin.nav.redemptionConfig', status: 'planned' }] },
  { titleKey: 'admin.nav.kycRisk', impactKey: 'admin.impact.kycRisk', items: [{ to: '/admin/kyc', key: 'admin.kycApplications' }, { key: 'admin.nav.riskQueue', status: 'planned' }] },
  { titleKey: 'admin.nav.compliance', impactKey: 'admin.impact.compliance', items: [{ to: '/admin/regions', key: 'admin.regions' }, { key: 'admin.nav.scPolicy', status: 'planned' }] },
  { titleKey: 'admin.nav.cmsRulesAmoe', impactKey: 'admin.impact.legal', items: [{ to: '/admin/legal-documents', key: 'admin.legalDocs' }, { to: '/admin/amoe', key: 'nav.amoe' }] },
  { titleKey: 'admin.nav.support', impactKey: 'admin.impact.support', items: [{ to: '/admin/support', key: 'admin.support' }] },
  { titleKey: 'admin.nav.auditLogs', impactKey: 'admin.impact.audit', items: [{ to: '/admin/audit-logs', key: 'admin.auditLogs' }] },
  { titleKey: 'admin.nav.systemRbac', impactKey: 'admin.impact.system', items: [{ key: 'admin.nav.rolesPermissions', status: 'planned' }] },
]
</script>

<template>
  <aside class="admin-nav" aria-label="Admin navigation">
    <strong>{{ $t('admin.operations') }}</strong>
    <section v-for="group in navGroups" :key="group.titleKey" class="admin-nav-group">
      <div class="admin-nav-group-title">
        <span>{{ $t(group.titleKey) }}</span>
        <small>{{ $t(group.impactKey) }}</small>
      </div>
      <template v-for="item in group.items" :key="`${group.titleKey}-${item.key}`">
        <RouterLink v-if="item.to" :to="item.to">{{ $t(item.key) }}</RouterLink>
        <span v-else class="planned-nav-item">{{ $t(item.key) }} · {{ $t('admin.nav.planned') }}</span>
      </template>
    </section>
  </aside>
</template>
```

- [ ] **Step 5: Add i18n keys**

Add English and Chinese keys in `frontend/src/i18n/messages.ts`:

```ts
'admin.nav.dashboard': 'Dashboard',
'admin.nav.userGuest': 'User & Guest',
'admin.nav.storePackages': 'Store & Packages',
'admin.nav.promoRewards': 'Promo & Rewards',
'admin.nav.gameLobby': 'Game Lobby',
'admin.nav.inboxNotification': 'Inbox & Notification',
'admin.nav.walletLedger': 'Wallet & Ledger',
'admin.nav.redemption': 'Redemption',
'admin.nav.kycRisk': 'KYC & Risk',
'admin.nav.compliance': 'Compliance',
'admin.nav.cmsRulesAmoe': 'CMS / Rules / AMOE',
'admin.nav.support': 'Support',
'admin.nav.auditLogs': 'Audit Logs',
'admin.nav.systemRbac': 'System / RBAC',
'admin.nav.planned': 'planned',
'admin.nav.biSummary': 'BI summary',
'admin.nav.guestSessions': 'Guest sessions',
'admin.nav.bindingHistory': 'Binding history',
'admin.nav.paymentConfig': 'Payment config',
'admin.nav.vipReferralTournament': 'VIP / Referral / Tournament',
'admin.nav.providerJackpot': 'Providers / Jackpot',
'admin.nav.messageTemplates': 'Templates / Delivery logs',
'admin.nav.scLotsPlaythrough': 'SC lots / Playthrough',
'admin.nav.redemptionConfig': 'Eligibility / Payout config',
'admin.nav.riskQueue': 'Risk queue / Device graph',
'admin.nav.scPolicy': 'SC policy switches',
'admin.nav.rolesPermissions': 'Roles / Permissions',
'admin.impact.operations': 'Operations overview',
'admin.impact.account': 'Impacts account state',
'admin.impact.store': 'Impacts Store',
'admin.impact.promo': 'Impacts Promo',
'admin.impact.lobby': 'Impacts Lobby',
'admin.impact.inbox': 'Impacts Inbox',
'admin.impact.wallet': 'Impacts Me > Wallet',
'admin.impact.redeem': 'Impacts Me > Redeem',
'admin.impact.kycRisk': 'Impacts Me > KYC and route gates',
'admin.impact.compliance': 'Impacts Store, Promo, Lobby, Redeem gates',
'admin.impact.legal': 'Impacts Promo AMOE and Me Legal',
'admin.impact.support': 'Impacts Me > Support',
'admin.impact.audit': 'Compliance traceability',
'admin.impact.system': 'Admin access',
```

- [ ] **Step 6: Run tests and verify GREEN**

Run:

```bash
cd frontend
npm run test -- --run src/views/admin/AdminCampaigns.test.ts -t "formal admin navigation|localizes admin shell" --pool=threads --maxWorkers=1
```

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add frontend/src/components/AdminNav.vue frontend/src/i18n/messages.ts frontend/src/views/admin/AdminCampaigns.test.ts
git commit -m "feat: group admin navigation by operating domain"
```

---

### Task 2: Admin Nav Styling

**Files:**
- Modify: `frontend/src/style.css`
- Test: `frontend/src/views/admin/AdminCampaigns.test.ts`

- [ ] **Step 1: Add style expectations to unit test**

In the grouped nav test, keep:

```ts
expect(wrapper.findAll('.admin-nav-group').length).toBeGreaterThan(8)
expect(wrapper.findAll('.planned-nav-item').length).toBeGreaterThan(0)
```

These fail until markup exists and prove the styling hooks are present.

- [ ] **Step 2: Implement CSS for grouped nav**

Update `frontend/src/style.css`:

```css
.admin-nav {
  gap: 10px;
  overflow-y: auto;
}

.admin-nav-group {
  display: grid;
  gap: 4px;
  padding: 8px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: #fbfcfa;
}

.admin-nav-group-title {
  display: grid;
  gap: 2px;
  padding: 2px 4px 6px;
}

.admin-nav-group-title span {
  color: var(--ink);
  font-size: 13px;
  font-weight: 900;
}

.admin-nav-group-title small,
.planned-nav-item {
  color: var(--muted);
  font-size: 11px;
  line-height: 1.25;
}

.planned-nav-item {
  padding: 8px 10px;
  border-radius: 8px;
  background: var(--surface-soft);
  font-weight: 700;
}
```

Adjust mobile:

```css
@media (max-width: 760px) {
  .admin-nav {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    max-height: 260px;
  }

  .admin-nav-group {
    min-width: 0;
  }
}
```

- [ ] **Step 3: Run targeted unit tests**

Run:

```bash
cd frontend
npm run test -- --run src/views/admin/AdminCampaigns.test.ts -t "formal admin navigation|localizes admin shell" --pool=threads --maxWorkers=1
```

Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add frontend/src/style.css frontend/src/views/admin/AdminCampaigns.test.ts
git commit -m "style: make admin navigation operationally grouped"
```

---

### Task 3: Browser Verification

**Files:**
- Create: `frontend/e2e/admin-ops-navigation.spec.ts`
- Modify: `frontend/e2e/p0a-demo.spec.ts` if existing assertions need the new group labels.

- [ ] **Step 1: Write Playwright grouped nav spec**

Create `frontend/e2e/admin-ops-navigation.spec.ts`:

```ts
import { expect, test } from '@playwright/test'

test.beforeEach(async ({ page }) => {
  await page.route('/api/v1/admin/dashboard/summary', async (route) => {
    await route.fulfill({ json: { registrations: 12, claims: 8, scGranted: '3.5000', riskEvents: 1 } })
  })
})

test('admin grouped operations navigation maps B-side modules to C-side impact', async ({ page }, testInfo) => {
  await page.goto('/admin')

  await expect(page.getByText('Store & Packages')).toBeVisible()
  await expect(page.getByText('Impacts Store')).toBeVisible()
  await expect(page.getByText('Promo & Rewards')).toBeVisible()
  await expect(page.getByText('Impacts Promo')).toBeVisible()
  await expect(page.getByText('Game Lobby')).toBeVisible()
  await expect(page.getByText('Impacts Lobby')).toBeVisible()
  await expect(page.getByText('Inbox & Notification')).toBeVisible()
  await expect(page.getByText('Impacts Inbox')).toBeVisible()
  await expect(page.getByText('Wallet & Ledger')).toBeVisible()
  await expect(page.getByText('Impacts Me > Wallet')).toBeVisible()
  await expect(page.getByText('Roles / Permissions')).toBeVisible()
  await page.screenshot({ path: `../artifacts/admin-ops-navigation/${testInfo.project.name}.png`, fullPage: true })
})
```

- [ ] **Step 2: Run Playwright spec and verify RED or GREEN**

Run before implementation if Task 1 has not been implemented; otherwise run now:

```bash
cd frontend
npx playwright test e2e/admin-ops-navigation.spec.ts
```

Expected after implementation: PASS and screenshots generated.

- [ ] **Step 3: Run complete Playwright**

Run:

```bash
cd frontend
npx playwright test
```

Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add frontend/e2e/admin-ops-navigation.spec.ts frontend/e2e/p0a-demo.spec.ts
git commit -m "test: verify grouped admin operations navigation"
```

---

### Task 4: Full Verification And Push

**Files:**
- Modify: `progress.md`
- Modify: `task_plan.md`
- Modify: `findings.md`

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

Expected: all specs pass.

- [ ] **Step 5: Update progress files**

Append:

```text
Admin operations navigation grouped by business domain.
B-side groups now show C-side impact labels.
Verification passed: backend tests, frontend tests, build, Playwright.
Screenshots: artifacts/admin-ops-navigation/.
```

- [ ] **Step 6: Commit and push**

```bash
git add progress.md task_plan.md findings.md
git commit -m "docs: record admin ops navigation grouping"
git push origin develop/p0a
```

Expected: remote `develop/p0a` updates successfully.

---

## Self-Review

- Spec coverage: grouped admin nav, C-side impact labels, planned module distinction, i18n, styling, browser verification, and full verification are covered.
- Placeholder scan: no task contains undefined implementation work.
- Type consistency: `NavItem` status values and CSS class names match test expectations.
