# Slots Productization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build B1-B3 Slots productization: GC-only five-reel Slots, backend-controlled rounds, wallet ledger integration, activity task linkage, reward inbox, and B-side operations pages.

**Architecture:** Add focused backend packages for `slots`, `activity`, and `notifications` while reusing existing `wallet`, `admin audit`, and auth patterns. Frontend adds C-side Slots and inbox surfaces plus B-side games, game rounds, notifications, and activity dashboard pages using existing Vue route and table patterns.

**Tech Stack:** Spring Boot, Spring Data JPA, Flyway, H2 tests, Vue 3, Pinia, Vitest, Playwright.

---

## File Structure

Backend create/modify:

- Create `backend/src/main/java/com/tangluck/slots/SlotGame.java`: slot game configuration entity.
- Create `backend/src/main/java/com/tangluck/slots/SlotRound.java`: persisted spin round entity.
- Create `backend/src/main/java/com/tangluck/slots/SlotGameRepository.java`: game lookup and admin listing.
- Create `backend/src/main/java/com/tangluck/slots/SlotRoundRepository.java`: user/admin round history queries.
- Create `backend/src/main/java/com/tangluck/slots/SlotDtos.java`: public/admin slot DTOs and requests.
- Create `backend/src/main/java/com/tangluck/slots/SlotService.java`: spin transaction, wallet ledger integration, audit, and task progress hook.
- Create `backend/src/main/java/com/tangluck/slots/SlotController.java`: C-side and B-side endpoints.
- Create `backend/src/main/java/com/tangluck/activity/ActivityTask.java`: task configuration.
- Create `backend/src/main/java/com/tangluck/activity/ActivityTaskProgress.java`: per-user task progress.
- Create `backend/src/main/java/com/tangluck/activity/ActivityTaskRepository.java`: task config queries.
- Create `backend/src/main/java/com/tangluck/activity/ActivityTaskProgressRepository.java`: progress lookup.
- Create `backend/src/main/java/com/tangluck/activity/ActivityDtos.java`: activity summary and task DTOs.
- Create `backend/src/main/java/com/tangluck/activity/ActivityService.java`: spin progress, task claiming, admin dashboard.
- Create `backend/src/main/java/com/tangluck/activity/ActivityController.java`: activity summary, task claim, admin dashboard.
- Create `backend/src/main/java/com/tangluck/notifications/RewardInboxItem.java`: reward inbox entity.
- Create `backend/src/main/java/com/tangluck/notifications/RewardInboxRepository.java`: inbox queries.
- Create `backend/src/main/java/com/tangluck/notifications/NotificationDtos.java`: inbox DTOs and admin request.
- Create `backend/src/main/java/com/tangluck/notifications/NotificationService.java`: inbox query, claim, manual grant, expiry.
- Create `backend/src/main/java/com/tangluck/notifications/NotificationController.java`: C-side and B-side notification endpoints.
- Create `backend/src/main/resources/db/migration/V10__slots_activity_notifications.sql`: schema and seed data.
- Create tests:
  - `backend/src/test/java/com/tangluck/slots/SlotControllerTest.java`
  - `backend/src/test/java/com/tangluck/activity/ActivityControllerTest.java`
  - `backend/src/test/java/com/tangluck/notifications/NotificationControllerTest.java`

Frontend create/modify:

- Modify `frontend/src/api/contracts.ts`: add Slots, activity summary, inbox, admin DTOs.
- Modify `frontend/src/router/index.ts`: add C/B routes.
- Modify `frontend/src/components/AdminNav.vue`: add Games, Game Rounds, Notifications, Activity Dashboard.
- Modify `frontend/src/i18n/messages.ts`: add C/B strings in English and Chinese.
- Modify `frontend/src/views/app/AppHome.vue`: make game cards primary and link to Slots.
- Create `frontend/src/views/app/AppSlots.vue`: C-side five-reel Slots page.
- Modify `frontend/src/views/app/AppActivity.vue`: task board and claim states.
- Create `frontend/src/views/app/AppInbox.vue`: reward inbox.
- Create `frontend/src/views/admin/AdminGames.vue`: game configuration.
- Create `frontend/src/views/admin/AdminGameRounds.vue`: round query page.
- Create `frontend/src/views/admin/AdminNotifications.vue`: reward inbox admin/manual grant.
- Create `frontend/src/views/admin/AdminActivityDashboard.vue`: activity metrics.
- Add/modify tests:
  - `frontend/src/views/app/P1Pages.test.ts`
  - `frontend/src/views/admin/AdminCampaigns.test.ts`
  - `frontend/e2e/p0a-demo.spec.ts`

## Task 1: B1 Backend Slots Core

**Files:**
- Create: backend slot package files listed above.
- Modify: `backend/src/main/resources/db/migration/V10__slots_activity_notifications.sql`
- Test: `backend/src/test/java/com/tangluck/slots/SlotControllerTest.java`

- [ ] **Step 1: Write failing backend tests**

Test names:

```java
@Test void activeGamesAreVisibleToPlayers()
@Test void spinDebitsGcCreditsPayoutAndRecordsRound()
@Test void insufficientGcDoesNotCreateRound()
@Test void adminCanPauseGameAndAuditIsWritten()
```

- [ ] **Step 2: Run RED**

Run:

```powershell
cd backend
.\gradlew.bat --no-daemon test --tests com.tangluck.slots.SlotControllerTest
```

Expected: FAIL because slot endpoints/classes do not exist.

- [ ] **Step 3: Implement minimal B1 backend**

Implement:

- `GET /api/v1/slots/games`
- `POST /api/v1/slots/{gameCode}/spin`
- `GET /api/v1/slots/rounds`
- `GET /api/v1/admin/games`
- `PATCH /api/v1/admin/games/{gameCode}`
- `GET /api/v1/admin/game-rounds`

Spin uses GC only and idempotency key `X-Idempotency-Key`. Use a deterministic backend result profile for tests:

- bet `10` returns multiplier `2.00`, payout `20`
- otherwise multiplier `0.00`, payout `0`

- [ ] **Step 4: Run GREEN**

Run the same target test. Expected: PASS.

- [ ] **Step 5: Commit B1 backend**

```powershell
git add backend/src/main/java/com/tangluck/slots backend/src/main/resources/db/migration/V10__slots_activity_notifications.sql backend/src/test/java/com/tangluck/slots/SlotControllerTest.java
git commit -m "feat: add slots backend loop"
```

## Task 2: B1 Frontend Slots Core

**Files:**
- Modify: `frontend/src/api/contracts.ts`, `frontend/src/router/index.ts`, `frontend/src/components/AdminNav.vue`, `frontend/src/i18n/messages.ts`, `frontend/src/views/app/AppHome.vue`
- Create: `frontend/src/views/app/AppSlots.vue`, `frontend/src/views/admin/AdminGames.vue`, `frontend/src/views/admin/AdminGameRounds.vue`
- Test: `frontend/src/views/app/P1Pages.test.ts`, `frontend/src/views/admin/AdminCampaigns.test.ts`

- [ ] **Step 1: Write failing frontend tests**

Add tests that mount `AppSlots`, `AdminGames`, and `AdminGameRounds`:

```ts
expect(wrapper.text()).toContain('Lucky Slots')
expect(fetchMock).toHaveBeenCalledWith('/api/v1/slots/lucky_slots/spin', expect.objectContaining({ method: 'POST' }))
expect(wrapper.text()).toContain('round_1001')
expect(wrapper.text()).toContain('20 GC')
```

- [ ] **Step 2: Run RED**

Run:

```powershell
cd frontend
npm run test -- --run src/views/app/P1Pages.test.ts src/views/admin/AdminCampaigns.test.ts --pool=threads --maxWorkers=1
```

Expected: FAIL because pages/routes are missing.

- [ ] **Step 3: Implement C-side and B-side B1 pages**

Implement:

- Slots game page with five stable reels, bet selector, spin button, result panel, and recent history.
- Admin games table with activate/pause controls.
- Admin game rounds table with user, game, bet, payout, multiplier, status, ledgers, time.

- [ ] **Step 4: Run GREEN**

Run target frontend tests. Expected: PASS.

- [ ] **Step 5: Commit B1 frontend**

```powershell
git add frontend/src/api/contracts.ts frontend/src/router/index.ts frontend/src/components/AdminNav.vue frontend/src/i18n/messages.ts frontend/src/views/app/AppHome.vue frontend/src/views/app/AppSlots.vue frontend/src/views/admin/AdminGames.vue frontend/src/views/admin/AdminGameRounds.vue frontend/src/views/app/P1Pages.test.ts frontend/src/views/admin/AdminCampaigns.test.ts
git commit -m "feat: add slots frontend loop"
```

## Task 3: B2 Backend Activity Tasks

**Files:**
- Create: backend activity package files listed above.
- Modify: `SlotService` to update task progress after successful spin.
- Test: `backend/src/test/java/com/tangluck/activity/ActivityControllerTest.java`

- [ ] **Step 1: Write failing backend tests**

Test names:

```java
@Test void spinUpdatesSpinCountBetAmountAndWinAmountProgress()
@Test void claimCompletedTaskCreditsGcOnce()
@Test void adminActivityDashboardShowsCompletionAndGrantTotals()
```

- [ ] **Step 2: Run RED**

Run:

```powershell
cd backend
.\gradlew.bat --no-daemon test --tests com.tangluck.activity.ActivityControllerTest
```

Expected: FAIL because activity APIs are missing.

- [ ] **Step 3: Implement B2 backend**

Implement:

- `GET /api/v1/player/activity-summary`
- `POST /api/v1/player/tasks/{taskCode}/claim`
- `GET /api/v1/admin/activity-dashboard`

Seed tasks:

- `daily_spin_10`: `spin_count`, target `10`, reward `1000 GC`
- `bet_5000_gc`: `bet_amount`, target `5000`, reward `2500 GC`
- `win_1000_gc`: `win_amount`, target `1000`, reward `1500 GC`

- [ ] **Step 4: Run GREEN**

Run target backend test. Expected: PASS.

- [ ] **Step 5: Commit B2 backend**

```powershell
git add backend/src/main/java/com/tangluck/activity backend/src/main/java/com/tangluck/slots/SlotService.java backend/src/test/java/com/tangluck/activity/ActivityControllerTest.java
git commit -m "feat: link slots to activity tasks"
```

## Task 4: B2 Frontend Activity Tasks

**Files:**
- Modify: `frontend/src/api/contracts.ts`, `frontend/src/i18n/messages.ts`, `frontend/src/views/app/AppActivity.vue`, `frontend/src/views/app/AppSlots.vue`
- Create/modify: `frontend/src/views/admin/AdminActivityDashboard.vue`
- Test: `frontend/src/views/app/P1Pages.test.ts`, `frontend/src/views/admin/AdminCampaigns.test.ts`

- [ ] **Step 1: Write failing frontend tests**

Add tests for:

- task progress appears on Slots page
- claimable task can be claimed from activity center
- admin activity dashboard renders metrics

- [ ] **Step 2: Run RED**

Run target frontend tests. Expected: FAIL.

- [ ] **Step 3: Implement B2 frontend**

Implement:

- task progress panel on Slots page
- activity center states: in progress, claimable, completed, expired
- admin activity dashboard table/metrics

- [ ] **Step 4: Run GREEN**

Run target frontend tests. Expected: PASS.

- [ ] **Step 5: Commit B2 frontend**

```powershell
git add frontend/src/api/contracts.ts frontend/src/i18n/messages.ts frontend/src/views/app/AppActivity.vue frontend/src/views/app/AppSlots.vue frontend/src/views/admin/AdminActivityDashboard.vue frontend/src/views/app/P1Pages.test.ts frontend/src/views/admin/AdminCampaigns.test.ts
git commit -m "feat: add activity task surfaces"
```

## Task 5: B3 Backend Reward Inbox

**Files:**
- Create: backend notifications package files listed above.
- Test: `backend/src/test/java/com/tangluck/notifications/NotificationControllerTest.java`

- [ ] **Step 1: Write failing backend tests**

Test names:

```java
@Test void playerCanClaimInboxRewardOnce()
@Test void expiredInboxRewardCannotBeClaimed()
@Test void adminCanIssueManualGrantAndAuditIsWritten()
```

- [ ] **Step 2: Run RED**

Run:

```powershell
cd backend
.\gradlew.bat --no-daemon test --tests com.tangluck.notifications.NotificationControllerTest
```

Expected: FAIL because notification APIs are missing.

- [ ] **Step 3: Implement B3 backend**

Implement:

- `GET /api/v1/player/notifications`
- `POST /api/v1/player/notifications/{id}/claim`
- `GET /api/v1/admin/notifications`
- `POST /api/v1/admin/notifications/manual-grant`
- `POST /api/v1/admin/notifications/{id}/expire`

- [ ] **Step 4: Run GREEN**

Run target backend test. Expected: PASS.

- [ ] **Step 5: Commit B3 backend**

```powershell
git add backend/src/main/java/com/tangluck/notifications backend/src/test/java/com/tangluck/notifications/NotificationControllerTest.java
git commit -m "feat: add reward inbox backend"
```

## Task 6: B3 Frontend Engagement

**Files:**
- Modify: `frontend/src/api/contracts.ts`, `frontend/src/router/index.ts`, `frontend/src/components/AdminNav.vue`, `frontend/src/i18n/messages.ts`, `frontend/src/views/app/AppHome.vue`, `frontend/src/views/app/AppActivity.vue`
- Create: `frontend/src/views/app/AppInbox.vue`, `frontend/src/views/admin/AdminNotifications.vue`
- Test: `frontend/src/views/app/P1Pages.test.ts`, `frontend/src/views/admin/AdminCampaigns.test.ts`, `frontend/e2e/p0a-demo.spec.ts`

- [ ] **Step 1: Write failing frontend tests**

Add tests for:

- lobby shows campaign popup/claimable badge from activity summary
- inbox renders claimable/claimed/expired states
- admin notifications can issue manual grant

- [ ] **Step 2: Run RED**

Run target frontend tests. Expected: FAIL.

- [ ] **Step 3: Implement B3 frontend**

Implement:

- activity popup and claimable badges
- reward inbox page
- admin notifications page
- e2e routes for Slots, activity, inbox, games, game rounds, notifications

- [ ] **Step 4: Run GREEN**

Run target frontend tests. Expected: PASS.

- [ ] **Step 5: Commit B3 frontend**

```powershell
git add frontend/src/api/contracts.ts frontend/src/router/index.ts frontend/src/components/AdminNav.vue frontend/src/i18n/messages.ts frontend/src/views/app/AppHome.vue frontend/src/views/app/AppActivity.vue frontend/src/views/app/AppInbox.vue frontend/src/views/admin/AdminNotifications.vue frontend/src/views/app/P1Pages.test.ts frontend/src/views/admin/AdminCampaigns.test.ts frontend/e2e/p0a-demo.spec.ts
git commit -m "feat: add reward inbox surfaces"
```

## Task 7: Full Verification And Final Push

**Files:**
- Modify: `progress.md`, `findings.md`, `task_plan.md`

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

Capture desktop and mobile screenshots for:

- C-side lobby
- C-side Slots page
- C-side Activity page
- C-side Inbox page
- B-side Games
- B-side Game Rounds
- B-side Activity Dashboard
- B-side Notifications

- [ ] **Step 4: Update docs**

Append B1-B3 implementation notes, findings, and verification evidence to `progress.md`, `findings.md`, and `task_plan.md`.

- [ ] **Step 5: Commit and push**

```powershell
git add progress.md findings.md task_plan.md
git commit -m "docs: record slots productization completion"
git push origin develop/p0a
```

## Self-Review

- Spec coverage: B1 game loop, B2 tasks, B3 inbox/engagement, backend data, frontend surfaces, admin operations, testing, and browser verification are all mapped to tasks.
- Placeholder scan: no TBD/TODO placeholders remain.
- Type consistency: planned API names use `slots`, `player/activity-summary`, `player/notifications`, and admin route names consistently.
