# Promotion Reward V1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a configurable promotion reward center with idempotent wallet credit claims and an admin page.

**Architecture:** Implement a new `gameluck-promotion` module following the existing `gameluck-redemption` module shape. Reward configuration and claim audit are stored in dedicated tables, and claim execution delegates wallet mutation to `IWalletCoreService.credit`.

**Tech Stack:** Java 17, Spring Boot, MyBatis-Plus, RuoYi Vue Plus, Vue 3, Element Plus, TypeScript, MySQL.

---

### Task 1: Backend Test

**Files:**
- Create: `backend/gameluck-modules/gameluck-promotion/src/test/java/com/gameluck/promotion/service/impl/PromotionRewardServiceImplTest.java`

- [ ] **Step 1: Write failing tests**

Cover first claim, repeat claim, and inactive promotion.

- [ ] **Step 2: Run test to verify failure**

Run:

```powershell
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -f backend\pom.xml -pl gameluck-modules/gameluck-promotion -am -Plocal -DskipTests=false "-Dtest=PromotionRewardServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected: fail because `gameluck-promotion` does not exist yet.

### Task 2: Backend Module

**Files:**
- Create module directory: `backend/gameluck-modules/gameluck-promotion`
- Modify: `backend/pom.xml`
- Modify: `backend/gameluck-modules/pom.xml`
- Modify: `backend/gameluck-admin/pom.xml`

- [ ] **Step 1: Add Maven module and dependency wiring**
- [ ] **Step 2: Add domain, BO, VO, mapper, service, controller, and enum files**
- [ ] **Step 3: Implement idempotent claim flow**
- [ ] **Step 4: Run focused test until passing**

### Task 3: SQL Seed

**Files:**
- Modify: `backend/script/sql/gameluck_wallet.sql`

- [ ] **Step 1: Add `gl_promotion_reward` table**
- [ ] **Step 2: Add `gl_promotion_claim` table**
- [ ] **Step 3: Add RuoYi menu seed using ids `1960-1976`**
- [ ] **Step 4: Verify directory/page icons exist locally and function icons are `#`**

### Task 4: Admin UI

**Files:**
- Create: `admin-ui/src/api/promotion/reward/index.ts`
- Create: `admin-ui/src/api/promotion/reward/types.ts`
- Create: `admin-ui/src/views/promotion/reward/index.vue`

- [ ] **Step 1: Add typed API wrappers**
- [ ] **Step 2: Add B-side table, filters, form modal, claim modal, and claim drawer**
- [ ] **Step 3: Keep all UI copy valid UTF-8 Chinese**
- [ ] **Step 4: Use existing Element Plus and RuoYi conventions**

### Task 5: Verification

- [ ] **Step 1: Run focused promotion Maven test**
- [ ] **Step 2: Run backend compile/package**
- [ ] **Step 3: Import SQL with `backend/script/bin/import-sql-utf8.ps1`**
- [ ] **Step 4: Run promotion runtime smoke if backend is available**
- [ ] **Step 5: Run `pnpm --dir admin-ui check:menu-icons`**
- [ ] **Step 6: Run `pnpm --dir admin-ui build:prod`**
- [ ] **Step 7: Run mojibake scan on new promotion files**

### Task 6: Commit

- [ ] **Step 1: Review `git diff`**
- [ ] **Step 2: Commit with `feat(promotion): add simulated reward claims`**
