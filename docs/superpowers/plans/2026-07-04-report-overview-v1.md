# Report Overview V1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a B-side report overview page and API for core MVP metrics.

**Architecture:** Create a read-only `gameluck-report` module that aggregates existing MVP business tables through MyBatis mapper SQL. Wire the module into Maven and `gameluck-admin`, then add a RuoYi admin page that calls `/report/overview/summary`.

**Tech Stack:** Java 17, Spring Boot, MyBatis Plus, RuoYi Vue3 admin, Element Plus, TypeScript.

---

### Task 1: Red Test

**Files:**
- Create: `backend/gameluck-modules/gameluck-report/src/test/java/com/gameluck/report/service/impl/ReportOverviewServiceImplTest.java`

- [x] **Step 1: Write focused service test**

Test `ReportOverviewServiceImpl` with a fake mapper that returns deterministic aggregate rows. Assert member count, wallet totals, deposit amount, game net amount, promotion reward amount, and redemption status counts.

- [x] **Step 2: Run test and verify RED**

Run:

```powershell
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -f backend\pom.xml -pl gameluck-modules/gameluck-report -am -Plocal -DskipTests=false "-Dtest=ReportOverviewServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected: fail because `gameluck-report` does not exist yet.

### Task 2: Backend Module

**Files:**
- Create: `backend/gameluck-modules/gameluck-report/pom.xml`
- Create: `backend/gameluck-modules/gameluck-report/src/main/java/com/gameluck/report/domain/vo/*.java`
- Create: `backend/gameluck-modules/gameluck-report/src/main/java/com/gameluck/report/mapper/ReportOverviewMapper.java`
- Create: `backend/gameluck-modules/gameluck-report/src/main/resources/mapper/report/ReportOverviewMapper.xml`
- Create: `backend/gameluck-modules/gameluck-report/src/main/java/com/gameluck/report/service/IReportOverviewService.java`
- Create: `backend/gameluck-modules/gameluck-report/src/main/java/com/gameluck/report/service/impl/ReportOverviewServiceImpl.java`
- Create: `backend/gameluck-modules/gameluck-report/src/main/java/com/gameluck/report/controller/ReportOverviewController.java`
- Modify: `backend/pom.xml`
- Modify: `backend/gameluck-modules/pom.xml`
- Modify: `backend/gameluck-admin/pom.xml`

- [x] **Step 1: Add Maven module wiring**
- [x] **Step 2: Add VO classes**
- [x] **Step 3: Add mapper and XML aggregate SQL**
- [x] **Step 4: Add service and controller**
- [x] **Step 5: Run focused test and verify GREEN**

### Task 3: SQL Menu

**Files:**
- Modify: `backend/script/sql/gameluck_wallet.sql`

- [x] **Step 1: Add report menu rows**

Add:

- `2000 Report Center`, icon `chart`
- `2001 Overview`, component `report/overview/index`, icon `chart`, permission `report:overview:list`
- `2011 Report Overview Query`, icon `#`, permission `report:overview:query`

- [x] **Step 2: Import SQL with UTF-8 script**

Run:

```powershell
.\backend\script\bin\import-sql-utf8.ps1 -SqlPath backend\script\sql\gameluck_wallet.sql
```

### Task 4: Frontend Page

**Files:**
- Create: `admin-ui/src/api/report/overview/index.ts`
- Create: `admin-ui/src/api/report/overview/types.ts`
- Create: `admin-ui/src/views/report/overview/index.vue`

- [x] **Step 1: Add API wrapper and types**
- [x] **Step 2: Add overview page**
- [x] **Step 3: Use English UI labels and Element Plus cards/tables**
- [x] **Step 4: Add loading and empty fallback states**

### Task 5: Verification And Commit

**Files:**
- Verify all report files.

- [x] **Step 1: Run focused report test**
- [x] **Step 2: Run backend package**
- [x] **Step 3: Run menu icon check**
- [x] **Step 4: Run frontend production build**
- [x] **Step 5: Run mojibake scan on new report files**
- [x] **Step 6: Commit with `feat(report): add overview metrics`**
