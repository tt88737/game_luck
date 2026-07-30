# Redemption Eligibility Policy Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** Replace hardcoded C-side redemption denied regions with operator-managed redemption eligibility policies.

**Architecture:** Add a redemption-module policy table, mapper, service, controller, admin UI page, and SQL menu seed. `ClientRedemptionService` keeps member compliance gates but delegates country/state/channel redemption eligibility to the new policy service before order creation.

**Tech Stack:** Spring Boot, MyBatis Plus, Java unit tests with JUnit/Mockito, MySQL, Vue 3, Element Plus, Vite, Playwright runtime smoke.

---

### Task 1: Backend Policy Domain And Decision Service

**Files:**
- Create: `backend/gameluck-modules/gameluck-redemption/src/main/java/com/gameluck/redemption/domain/RedemptionEligibilityPolicy.java`
- Create: `backend/gameluck-modules/gameluck-redemption/src/main/java/com/gameluck/redemption/domain/bo/RedemptionEligibilityPolicyBo.java`
- Create: `backend/gameluck-modules/gameluck-redemption/src/main/java/com/gameluck/redemption/domain/vo/RedemptionEligibilityPolicyVo.java`
- Create: `backend/gameluck-modules/gameluck-redemption/src/main/java/com/gameluck/redemption/mapper/RedemptionEligibilityPolicyMapper.java`
- Create: `backend/gameluck-modules/gameluck-redemption/src/main/java/com/gameluck/redemption/service/IRedemptionEligibilityPolicyService.java`
- Create: `backend/gameluck-modules/gameluck-redemption/src/main/java/com/gameluck/redemption/service/impl/RedemptionEligibilityPolicyServiceImpl.java`
- Create: `backend/gameluck-modules/gameluck-redemption/src/test/java/com/gameluck/redemption/service/impl/RedemptionEligibilityPolicyServiceImplTest.java`

- [x] Write RED tests for deny, allow override, disabled policy ignored, and expired policy ignored.
- [x] Run focused Maven test and confirm it fails because the service does not exist.
- [x] Implement domain, mapper, interface, and service.
- [x] Run focused Maven test and confirm it passes.

### Task 2: Wire Runtime Redemption Gate

**Files:**
- Modify: `backend/gameluck-modules/gameluck-redemption/src/main/java/com/gameluck/redemption/client/service/ClientRedemptionService.java`
- Modify: `backend/gameluck-modules/gameluck-redemption/src/test/java/com/gameluck/redemption/client/service/ClientRedemptionServiceTest.java`

- [x] Update `ClientRedemptionServiceTest` constructors and add RED cases showing policy denial blocks order creation and higher-priority allow permits a seeded denied region.
- [x] Run focused Maven test and confirm it fails for missing service constructor behavior.
- [x] Inject `IRedemptionEligibilityPolicyService` into `ClientRedemptionService`.
- [x] Remove hardcoded `isDeniedRegion(...)`.
- [x] Evaluate policy after currency validation and before order creation.
- [x] Run focused redemption tests and confirm they pass.

### Task 3: Backend CRUD Controller And SQL

**Files:**
- Create: `backend/gameluck-modules/gameluck-redemption/src/main/java/com/gameluck/redemption/controller/RedemptionEligibilityPolicyController.java`
- Modify: `backend/script/sql/gameluck_wallet.sql`
- Modify: backend i18n files if validation messages need new keys.

- [x] Add controller endpoints `GET /redemption/eligibility-policy/list`, `GET /redemption/eligibility-policy/{id}`, `POST /redemption/eligibility-policy`, and `PUT /redemption/eligibility-policy`.
- [x] Add table DDL and idempotent seed rows for `US/WA`, `US/ID`, `US/NV`, and `US/MI`.
- [x] Add menu rows under Redemption Center for `鍏戞崲璧勬牸绛栫暐`.
- [x] Run backend compile.

### Task 4: Admin UI Page

**Files:**
- Create: `admin-ui/src/api/redemption/eligibilityPolicy/index.ts`
- Create: `admin-ui/src/api/redemption/eligibilityPolicy/types.ts`
- Create: `admin-ui/src/views/redemption/eligibility-policy/index.vue`

- [x] Add API wrappers and TypeScript types.
- [x] Build a dense B-side table page with filters, tags, pagination, and add/edit dialog.
- [x] Use existing `tt(...)`, Element Plus, permission button, and `right-toolbar` patterns.
- [x] Run Admin UI menu icon check and build.

### Task 5: Local Runtime Wiring And Smoke

**Files:**
- Modify: `task_plan.md`
- Modify: `progress.md`

- [x] Import updated SQL into local MySQL.
- [x] Rebuild backend jar and restart local backend.
- [x] Verify unauthenticated policy endpoint is protected and authenticated Admin UI can list seeded policies.
- [x] Register H5 `US/WA` member and verify redemption is denied with no order.
- [x] Create or update a higher-priority `ALLOW` test policy for a safe test state and verify redemption can create a pending order.
- [x] Capture screenshots and JSON evidence.
- [x] Run `git diff --check`.
- [x] Mark Phase 29 complete.

## Self Review

- The plan covers backend domain, runtime behavior, CRUD, Admin UI, SQL, and runtime smoke.
- There are no placeholder tasks.
- Names are consistent: `RedemptionEligibilityPolicy`, `/redemption/eligibility-policy`, and `redemption:eligibilityPolicy:*`.
