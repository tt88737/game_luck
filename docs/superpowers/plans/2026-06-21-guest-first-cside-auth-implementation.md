# Guest-First C-Side Auth Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the C-side default to a playable guest lobby, move registration/login into modals, and let guests bind email to become formal users without losing wallet or gameplay state.

**Architecture:** Add backend guest and bind-email auth endpoints while preserving the existing user/wallet model. Add a C-side `AppShell` that owns guest session boot, top account state, bottom navigation, and `AuthModal`, then nest all app routes under that shell.

**Tech Stack:** Spring Boot, Spring Data JPA, Flyway, Vue 3, Pinia, Vue Router, Vitest, Playwright.

---

## File Structure

Backend:

- Modify `backend/src/main/java/com/tangluck/auth/AuthDtos.java`: add guest and bind-email request DTOs plus `accountType` in auth response.
- Modify `backend/src/main/java/com/tangluck/auth/AuthService.java`: add `createGuest(...)`, `bindEmail(...)`, and auth response account type mapping.
- Modify `backend/src/main/java/com/tangluck/auth/AuthController.java`: expose `POST /api/v1/auth/guest` and `POST /api/v1/auth/bind-email`.
- Modify `backend/src/main/java/com/tangluck/auth/User.java`: add methods for guest creation and binding.
- Modify `backend/src/test/java/com/tangluck/auth/AuthControllerTest.java`: add guest and bind-email tests.

Frontend:

- Modify `frontend/src/api/contracts.ts`: add `accountType`, `GuestRequest`, `BindEmailRequest`.
- Modify `frontend/src/stores/session.ts`: add `accountType`, `isGuest`, `ensureGuestSession`, `bindEmail`.
- Create `frontend/src/components/AuthModal.vue`: bind/sign-in modal.
- Create `frontend/src/views/app/AppShell.vue`: C-side top bar, auth modal, bottom nav, nested router view.
- Modify `frontend/src/router/index.ts`: nest app routes under shell, redirect `/app/register` and `/app/login` to modal query.
- Modify `frontend/src/views/app/AppHome.vue`: remove logged-out registration gate, assume shell has ensured guest session.
- Modify `frontend/src/views/app/AppStore.vue`, `AppKyc.vue`, `AppRedemption.vue`: formal-only actions show auth modal or clear prompt when guest.
- Modify tests:
  - `frontend/src/stores/session.test.ts`
  - `frontend/src/views/app/AppHome.test.ts`
  - `frontend/src/views/app/AppRegister.test.ts`
  - `frontend/src/views/app/AppLogin.test.ts`
  - `frontend/src/views/app/P1Pages.test.ts`
  - `frontend/e2e/p0a-demo.spec.ts`

## Task 1: Backend Guest And Bind Email

**Files:**
- Modify: `backend/src/main/java/com/tangluck/auth/AuthDtos.java`
- Modify: `backend/src/main/java/com/tangluck/auth/AuthService.java`
- Modify: `backend/src/main/java/com/tangluck/auth/AuthController.java`
- Modify: `backend/src/main/java/com/tangluck/auth/User.java`
- Test: `backend/src/test/java/com/tangluck/auth/AuthControllerTest.java`

- [ ] **Step 1: Write failing backend tests**

Add tests:

```java
@Test void guestAccountGetsWalletAndCanBeHydrated()
@Test void bindEmailUpgradesGuestWithoutChangingUserId()
@Test void bindEmailRejectsDuplicateFormalEmail()
```

Assertions:

- `POST /api/v1/auth/guest` returns `user.status=guest`, `accountType=guest`, `wallet.gcBalance`.
- `POST /api/v1/auth/bind-email` with `X-User-Id` returns the same `userId`, `user.status=active`, `accountType=formal`.
- Wallet summary before and after bind uses same user id and keeps balance.
- Duplicate email returns `EMAIL_EXISTS`.

- [ ] **Step 2: Run RED**

Run:

```powershell
cd backend
.\gradlew.bat --no-daemon test --tests com.tangluck.auth.AuthControllerTest
```

Expected: FAIL because guest/bind endpoints do not exist.

- [ ] **Step 3: Implement backend guest and bind-email**

Implement:

- `GuestRequest(deviceId, countryCode, stateCode, utmSource)`
- `BindEmailRequest(email, password, birthDate, countryCode, stateCode, acceptedDocuments)`
- `AuthResponse` gains `accountType`
- guest generated email format: `guest_<uuid>@guest.tangluck.local`
- guest status: `guest`
- formal status after bind: `active`
- binding updates same user row and writes consent logs.

- [ ] **Step 4: Run GREEN**

Run same target test. Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add backend/src/main/java/com/tangluck/auth backend/src/test/java/com/tangluck/auth/AuthControllerTest.java
git commit -m "feat: add guest account binding backend"
```

## Task 2: Frontend Session And Auth Modal

**Files:**
- Modify: `frontend/src/api/contracts.ts`
- Modify: `frontend/src/stores/session.ts`
- Create: `frontend/src/components/AuthModal.vue`
- Test: `frontend/src/stores/session.test.ts`
- Test: `frontend/src/views/app/AppRegister.test.ts`
- Test: `frontend/src/views/app/AppLogin.test.ts`

- [ ] **Step 1: Write failing frontend tests**

Add tests:

```ts
it('creates a guest session when no session exists')
it('binds email and keeps the same user id')
it('renders register and login inside the auth modal')
```

Expected behavior:

- `ensureGuestSession()` calls `/api/v1/auth/guest`.
- `bindEmail()` calls `/api/v1/auth/bind-email`.
- Auth modal has `Bind account` and `Sign in` tabs.

- [ ] **Step 2: Run RED**

Run:

```powershell
cd frontend
npm run test -- --run src/stores/session.test.ts src/views/app/AppRegister.test.ts src/views/app/AppLogin.test.ts --pool=threads --maxWorkers=1
```

Expected: FAIL.

- [ ] **Step 3: Implement session and modal**

Implement:

- `accountType`
- `isGuest`
- `ensureGuestSession`
- `bindEmail`
- `AuthModal.vue` with bind/sign-in tabs, legal docs loading, error/success states.

- [ ] **Step 4: Run GREEN**

Run same target tests. Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add frontend/src/api/contracts.ts frontend/src/stores/session.ts frontend/src/components/AuthModal.vue frontend/src/stores/session.test.ts frontend/src/views/app/AppRegister.test.ts frontend/src/views/app/AppLogin.test.ts
git commit -m "feat: add guest session auth modal"
```

## Task 3: C-Side Shell And Route Restructure

**Files:**
- Create: `frontend/src/views/app/AppShell.vue`
- Modify: `frontend/src/router/index.ts`
- Modify: `frontend/src/views/app/AppHome.vue`
- Modify: `frontend/src/views/app/AppSlots.vue`
- Modify: `frontend/src/views/app/AppActivity.vue`
- Modify: `frontend/src/views/app/AppInbox.vue`
- Modify: `frontend/src/views/app/AppWallet.vue`
- Test: `frontend/src/views/app/AppHome.test.ts`
- Test: `frontend/src/views/app/P1Pages.test.ts`

- [ ] **Step 1: Write failing route and shell tests**

Add assertions:

- `/app/register` redirects to `/app?auth=register`.
- `/app/login` redirects to `/app?auth=login`.
- bottom nav contains Home, Slots, Activity, Inbox, Wallet.
- bottom nav does not contain Register or Sign in.
- `AppHome` renders lobby for guest user.

- [ ] **Step 2: Run RED**

Run:

```powershell
cd frontend
npm run test -- --run src/views/app/AppHome.test.ts src/views/app/P1Pages.test.ts --pool=threads --maxWorkers=1
```

Expected: FAIL.

- [ ] **Step 3: Implement shell and nested routes**

Implement:

- `AppShell.vue` calls `session.ensureGuestSession()` on C-side load.
- shell owns top account bar and auth modal.
- shell owns one bottom nav.
- app child pages remove duplicated bottom nav where practical.
- compatibility routes redirect to modal query.

- [ ] **Step 4: Run GREEN**

Run same target tests. Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add frontend/src/views/app/AppShell.vue frontend/src/router/index.ts frontend/src/views/app/AppHome.vue frontend/src/views/app/AppSlots.vue frontend/src/views/app/AppActivity.vue frontend/src/views/app/AppInbox.vue frontend/src/views/app/AppWallet.vue frontend/src/views/app/AppHome.test.ts frontend/src/views/app/P1Pages.test.ts
git commit -m "feat: make cside guest first shell"
```

## Task 4: Formal-Only Guards And Production Layout Polish

**Files:**
- Modify: `frontend/src/views/app/AppStore.vue`
- Modify: `frontend/src/views/app/AppKyc.vue`
- Modify: `frontend/src/views/app/AppRedemption.vue`
- Modify: `frontend/src/style.css`
- Test: `frontend/src/views/app/P1Pages.test.ts`

- [ ] **Step 1: Write failing guard tests**

Add tests:

- guest opening Store sees bind prompt before purchase.
- guest opening KYC sees bind prompt.
- guest opening Redemption sees bind prompt.

- [ ] **Step 2: Run RED**

Run:

```powershell
cd frontend
npm run test -- --run src/views/app/P1Pages.test.ts --pool=threads --maxWorkers=1
```

Expected: FAIL.

- [ ] **Step 3: Implement formal-only guards and layout polish**

Implement:

- compact account top bar
- guest badge
- auth modal trigger buttons
- stable bottom nav sizing
- C-side layout no longer shows registration as main content

- [ ] **Step 4: Run GREEN**

Run target tests. Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add frontend/src/views/app/AppStore.vue frontend/src/views/app/AppKyc.vue frontend/src/views/app/AppRedemption.vue frontend/src/style.css frontend/src/views/app/P1Pages.test.ts
git commit -m "feat: gate formal cside actions"
```

## Task 5: Full Verification And Push

**Files:**
- Modify: `progress.md`
- Modify: `findings.md`
- Modify: `task_plan.md`

- [ ] **Step 1: Run backend full suite**

```powershell
cd backend
.\gradlew.bat --no-daemon test
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 2: Run frontend full suite**

```powershell
cd frontend
npm run test -- --run --pool=threads --maxWorkers=1
npm run build
npx playwright test
```

Expected: all pass.

- [ ] **Step 3: Browser screenshots**

Capture desktop and mobile screenshots:

- `/app` fresh browser guest lobby
- `/app?auth=register`
- `/app?auth=login`
- `/app/slots/lucky_slots`
- `/app/inbox`
- `/app/store` as guest

- [ ] **Step 4: Update docs**

Append implementation notes and verification evidence to progress/findings/task plan.

- [ ] **Step 5: Commit and push**

```powershell
git add progress.md findings.md task_plan.md
git commit -m "docs: record guest first cside auth completion"
git push origin develop/p0a
```

## Self-Review

- Spec coverage: guest creation, bind-email, modal auth, route restructure, formal-only guards, tests, screenshots are all covered.
- Placeholder scan: no TBD/TODO placeholders.
- Type consistency: `accountType`, `guest`, `formal`, `GuestRequest`, `BindEmailRequest`, `AuthModal`, and route query names are consistent.
