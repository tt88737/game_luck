# Member Profile V1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a member center with B-side member profile management.

**Architecture:** Implement a new `gameluck-member` backend module following the existing business-module pattern. Member profile state is stored in `gl_member_profile`, exposed through RuoYi permission APIs, and rendered as a table-first admin page.

**Tech Stack:** Java 17, Spring Boot, MyBatis-Plus, RuoYi Vue Plus, Vue 3, Element Plus, TypeScript, MySQL.

---

### Task 1: Backend Test

**Files:**
- Create: `backend/gameluck-modules/gameluck-member/src/test/java/com/gameluck/member/service/impl/MemberProfileServiceImplTest.java`

- [ ] **Step 1: Write failing tests**

Cover default creation, duplicate username guard, and invalid status guard.

- [ ] **Step 2: Run test to verify failure**

Run:

```powershell
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -f backend\pom.xml -pl gameluck-modules/gameluck-member -am -Plocal -DskipTests=false "-Dtest=MemberProfileServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected: fail because `gameluck-member` does not exist yet.

### Task 2: Backend Module

**Files:**
- Create module directory: `backend/gameluck-modules/gameluck-member`
- Modify: `backend/pom.xml`
- Modify: `backend/gameluck-modules/pom.xml`
- Modify: `backend/gameluck-admin/pom.xml`

- [ ] **Step 1: Add Maven module and dependency wiring**
- [ ] **Step 2: Add entity, BO, VO, mapper, service, controller, and enums**
- [ ] **Step 3: Implement username uniqueness and status validation**
- [ ] **Step 4: Run focused test until passing**

### Task 3: SQL Seed

**Files:**
- Modify: `backend/script/sql/gameluck_wallet.sql`

- [ ] **Step 1: Add `gl_member_profile` table**
- [ ] **Step 2: Add seed member for local smoke**
- [ ] **Step 3: Add RuoYi menu seed using ids `1980-1995`**
- [ ] **Step 4: Verify menu icons use local `user` and function icon `#`**

### Task 4: Admin UI

**Files:**
- Create: `admin-ui/src/api/member/profile/index.ts`
- Create: `admin-ui/src/api/member/profile/types.ts`
- Create: `admin-ui/src/views/member/profile/index.vue`

- [ ] **Step 1: Add typed API wrappers**
- [ ] **Step 2: Add table, filters, form modal, detail dialog, and status actions**
- [ ] **Step 3: Keep Chinese UI copy valid UTF-8**
- [ ] **Step 4: Use existing Element Plus and RuoYi conventions**

### Task 5: Verification

- [ ] **Step 1: Run focused member Maven test**
- [ ] **Step 2: Run backend package**
- [ ] **Step 3: Import SQL with `backend/script/bin/import-sql-utf8.ps1`**
- [ ] **Step 4: Verify database table, seed member, and menu rows**
- [ ] **Step 5: Run `pnpm --dir admin-ui check:menu-icons`**
- [ ] **Step 6: Run `pnpm --dir admin-ui build:prod`**
- [ ] **Step 7: Run mojibake scan on new member files**

### Task 6: Commit

- [ ] **Step 1: Review `git diff`**
- [ ] **Step 2: Commit with `feat(member): add member profile management`**
