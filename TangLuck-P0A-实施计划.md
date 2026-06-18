# Tang Luck P0-A Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a runnable Tang Luck P0-A application with Spring Boot backend, Vue 3 frontend, MySQL schema, seeded demo data, and verifiable registration, wallet, campaign, task, coupon, admin, audit, and compliance flows.

**Architecture:** Use a monorepo with `backend/` and `frontend/`. The backend owns compliance, wallet ledger consistency, idempotency, promotion issuing, risk downgrades, admin audit, and REST API contracts. The frontend consumes those APIs through typed clients and provides a C-side mobile app view plus B-side admin operations view.

**Tech Stack:** Java 17, Spring Boot 3, Gradle Wrapper, Flyway, MySQL 8, JUnit 5, Testcontainers optional later, Vue 3, Vite, TypeScript, Pinia, Vue Router, Playwright.

---

## File Structure

```text
gameluck/
  backend/
    build.gradle
    settings.gradle
    gradlew
    gradlew.bat
    src/main/java/com/tangluck/
      TangLuckApplication.java
      common/
      auth/
      compliance/
      wallet/
      promotion/
      admin/
      risk/
    src/main/resources/
      application.yml
      db/migration/V1__init_schema.sql
      db/migration/V2__seed_demo_data.sql
    src/test/java/com/tangluck/
  frontend/
    package.json
    vite.config.ts
    src/
      main.ts
      App.vue
      router/
      stores/
      api/
      views/app/
      views/admin/
      components/
  docker-compose.yml
  docs/superpowers/specs/2026-06-18-tangluck-p0a-design.md
```

## Task 1: Initialize Repository And Runtime Skeleton

**Files:**
- Create: `.gitignore`
- Create: `docker-compose.yml`
- Create: `backend/settings.gradle`
- Create: `backend/build.gradle`
- Create: `backend/src/main/java/com/tangluck/TangLuckApplication.java`
- Create: `backend/src/main/resources/application.yml`
- Create: `frontend/package.json`
- Create: `frontend/vite.config.ts`
- Create: `frontend/index.html`
- Create: `frontend/src/main.ts`
- Create: `frontend/src/App.vue`

- [ ] **Step 1: Create a Git repository if missing**

Run:
```powershell
if (-not (Test-Path .git)) { git init }
git status --short
```

Expected: repository exists and only current project files are listed as untracked or modified.

- [ ] **Step 2: Add ignore rules**

Create `.gitignore`:
```gitignore
.idea/
.vscode/
*.iml
.DS_Store
Thumbs.db

backend/.gradle/
backend/build/
backend/out/

frontend/node_modules/
frontend/dist/
frontend/.vite/
frontend/playwright-report/
frontend/test-results/

.env
.env.*
!.env.example
```

- [ ] **Step 3: Add local MySQL compose file**

Create `docker-compose.yml`:
```yaml
services:
  mysql:
    image: mysql:8.4
    container_name: tangluck-mysql
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: tangluck
      MYSQL_USER: tangluck
      MYSQL_PASSWORD: tangluck
    ports:
      - "3306:3306"
    volumes:
      - tangluck_mysql:/var/lib/mysql
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-uroot", "-proot"]
      interval: 5s
      timeout: 3s
      retries: 20

volumes:
  tangluck_mysql:
```

- [ ] **Step 4: Generate backend Gradle project**

Use Spring Initializr or Gradle wrapper generation. If internet is available:
```powershell
Invoke-WebRequest "https://start.spring.io/starter.zip?type=gradle-project&language=java&bootVersion=3.3.6&baseDir=backend&groupId=com.tangluck&artifactId=tangluck&name=tangluck&packageName=com.tangluck&javaVersion=17&dependencies=web,validation,security,data-jpa,flyway,mysql,lombok" -OutFile backend.zip
Expand-Archive backend.zip -DestinationPath .
Remove-Item backend.zip
```

Expected: `backend/gradlew.bat` exists.

- [ ] **Step 5: Add minimal backend application**

Create `backend/src/main/java/com/tangluck/TangLuckApplication.java`:
```java
package com.tangluck;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TangLuckApplication {
    public static void main(String[] args) {
        SpringApplication.run(TangLuckApplication.class, args);
    }
}
```

- [ ] **Step 6: Add backend configuration**

Create `backend/src/main/resources/application.yml`:
```yaml
server:
  port: 8080

spring:
  application:
    name: tangluck
  datasource:
    url: jdbc:mysql://localhost:3306/tangluck?useUnicode=true&characterEncoding=utf8&serverTimezone=UTC
    username: tangluck
    password: tangluck
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
  flyway:
    enabled: true
    locations: classpath:db/migration

tangluck:
  jwt-secret: "local-dev-change-me-local-dev-change-me"
  jwt-ttl-minutes: 720
```

- [ ] **Step 7: Create Vue app skeleton**

Run:
```powershell
npm create vite@latest frontend -- --template vue-ts
cd frontend
npm install
```

Expected: `frontend/package.json` exists and `npm run build` succeeds after Step 8.

- [ ] **Step 8: Verify skeletons**

Run:
```powershell
cd backend
.\gradlew.bat test
cd ..\frontend
npm run build
```

Expected: backend tests pass; frontend builds.

## Task 2: Database Schema And Seed Data

**Files:**
- Create: `backend/src/main/resources/db/migration/V1__init_schema.sql`
- Create: `backend/src/main/resources/db/migration/V2__seed_demo_data.sql`
- Test: `backend/src/test/java/com/tangluck/schema/FlywayMigrationTest.java`

- [ ] **Step 1: Write failing migration test**

Create `backend/src/test/java/com/tangluck/schema/FlywayMigrationTest.java`:
```java
package com.tangluck.schema;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class FlywayMigrationTest {
    @Test
    void applicationContextLoadsWithFlywayMigrations() {
    }
}
```

- [ ] **Step 2: Run test and verify it fails before migrations exist**

Run:
```powershell
cd backend
.\gradlew.bat test --tests com.tangluck.schema.FlywayMigrationTest
```

Expected: FAIL because JPA validation or Flyway has no schema for required entities once entities are added in later tasks. If it passes at this early skeleton stage, keep it as a smoke test and verify failure in Task 3 entity tests.

- [ ] **Step 3: Add schema migration**

Create `backend/src/main/resources/db/migration/V1__init_schema.sql` with tables from `findings.md`: `users`, `user_consent_logs`, `compliance_regions`, `compliance_documents`, `wallet_accounts`, `wallet_ledger`, `promotion_campaigns`, `promotion_claims`, `promotion_reward_grants`, `daily_task_progress`, `coupon_codes`, `risk_events`, `audit_logs`. Use `bigint auto_increment` IDs, `decimal(20,4)` wallet amounts, `json` columns for policy fields, and unique indexes on email, wallet user/currency, ledger idempotency, campaign claim period, coupon code, document type/version, and region.

- [ ] **Step 4: Add seed data**

Create `backend/src/main/resources/db/migration/V2__seed_demo_data.sql` with:
```sql
insert into compliance_regions(country_code, state_code, registration_allowed, game_allowed, purchase_allowed, sc_grant_allowed, redemption_allowed, amoe_allowed, requires_legal_review, status, legal_approval_id, updated_at)
values
('US','CA', true, true, false, true, false, true, false, 'active', 'LEGAL-2026-0617-CA', utc_timestamp()),
('US','TX', true, true, false, true, false, true, false, 'active', 'LEGAL-2026-0617-TX', utc_timestamp()),
('US','WA', false, false, false, false, false, false, true, 'blocked', null, utc_timestamp());

insert into compliance_documents(document_type, version, title, content_url, effective_at, status, legal_approval_id)
values
('terms', 'terms-v1', 'Terms of Use', '/legal/terms-v1', utc_timestamp(), 'active', 'LEGAL-2026-0617-DOC'),
('sweepstakes_rules', 'rules-v1', 'Sweepstakes Rules', '/legal/rules-v1', utc_timestamp(), 'active', 'LEGAL-2026-0617-DOC'),
('privacy', 'privacy-v1', 'Privacy Policy', '/legal/privacy-v1', utc_timestamp(), 'active', 'LEGAL-2026-0617-DOC'),
('amoe', 'amoe-v1', 'AMOE / No Purchase Necessary', '/legal/amoe-v1', utc_timestamp(), 'active', 'LEGAL-2026-0617-DOC');
```

- [ ] **Step 5: Run migration test**

Run:
```powershell
cd backend
.\gradlew.bat test --tests com.tangluck.schema.FlywayMigrationTest
```

Expected: PASS.

## Task 3: Backend Common Error, Trace, And API Envelope

**Files:**
- Create: `backend/src/main/java/com/tangluck/common/api/ApiError.java`
- Create: `backend/src/main/java/com/tangluck/common/api/ErrorCode.java`
- Create: `backend/src/main/java/com/tangluck/common/api/BusinessException.java`
- Create: `backend/src/main/java/com/tangluck/common/api/GlobalExceptionHandler.java`
- Test: `backend/src/test/java/com/tangluck/common/api/GlobalExceptionHandlerTest.java`

- [ ] **Step 1: Write failing controller advice test**

Create a test that calls a test controller throwing `new BusinessException(ErrorCode.REGION_BLOCKED, "This feature is not available in your region.")` and asserts HTTP 403 with JSON fields `code`, `message`, `trace_id`, and `details`.

- [ ] **Step 2: Run test to verify RED**

Run:
```powershell
cd backend
.\gradlew.bat test --tests com.tangluck.common.api.GlobalExceptionHandlerTest
```

Expected: FAIL because `BusinessException` and handler do not exist.

- [ ] **Step 3: Implement common API error handling**

Implement `ErrorCode` enum with all P0-A codes, `BusinessException` with code/details, `ApiError` record, and `GlobalExceptionHandler` mapping `REGION_BLOCKED`, `AGE_NOT_ALLOWED`, `SC_POLICY_BLOCKED` to 403; `EMAIL_EXISTS`, `CLAIM_DUPLICATED`, `IDEMPOTENCY_CONFLICT` to 409; `RISK_REVIEW_REQUIRED` to 202; default business errors to 400.

- [ ] **Step 4: Run test to verify GREEN**

Run:
```powershell
cd backend
.\gradlew.bat test --tests com.tangluck.common.api.GlobalExceptionHandlerTest
```

Expected: PASS.

## Task 4: Auth, Compliance, And Wallet Creation

**Files:**
- Create backend classes under `auth/`, `compliance/`, `wallet/`
- Test: `backend/src/test/java/com/tangluck/auth/AuthServiceTest.java`
- Test: `backend/src/test/java/com/tangluck/auth/AuthControllerTest.java`

- [ ] **Step 1: Write failing registration service tests**

Cover:
- CA adult with required documents creates user and two wallets.
- Duplicate email returns `EMAIL_EXISTS`.
- Birth date under 18 returns `AGE_NOT_ALLOWED`.
- Missing `terms`, `sweepstakes_rules`, or `privacy` returns `CONSENT_REQUIRED`.
- WA user returns `REGION_BLOCKED`.

- [ ] **Step 2: Run RED**

Run:
```powershell
cd backend
.\gradlew.bat test --tests com.tangluck.auth.AuthServiceTest
```

Expected: FAIL because service classes do not exist.

- [ ] **Step 3: Implement entities, repositories, service, and controller**

Implement `User`, `UserConsentLog`, `ComplianceRegion`, `ComplianceDocument`, `WalletAccount`; repositories; `AuthService.register`; `AuthController` endpoints:
- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `GET /api/v1/me`
- `GET /api/v1/compliance/documents`

- [ ] **Step 4: Run GREEN**

Run:
```powershell
cd backend
.\gradlew.bat test --tests com.tangluck.auth.AuthServiceTest --tests com.tangluck.auth.AuthControllerTest
```

Expected: PASS.

## Task 5: Wallet Ledger And Idempotency

**Files:**
- Create backend wallet ledger classes under `wallet/`
- Test: `backend/src/test/java/com/tangluck/wallet/WalletServiceTest.java`
- Test: `backend/src/test/java/com/tangluck/wallet/WalletControllerTest.java`

- [ ] **Step 1: Write failing wallet tests**

Cover:
- Crediting `10000 GC` updates `wallet_accounts.balance` and writes one `wallet_ledger`.
- Crediting `"0.50" SC` keeps decimal precision.
- Reusing the same `idempotency_key` returns the original ledger and does not create a second ledger.
- `GET /api/v1/wallet/summary` returns GC, SC, frozen SC, redeemable SC, and SC source summary.
- `GET /api/v1/wallet/ledger?currency=SC` returns paged ledger rows.

- [ ] **Step 2: Run RED**

Run:
```powershell
cd backend
.\gradlew.bat test --tests com.tangluck.wallet.WalletServiceTest
```

Expected: FAIL because wallet credit logic does not exist.

- [ ] **Step 3: Implement wallet ledger service and API**

Implement transactional `WalletService.credit(userId, currency, amount, businessType, businessId, idempotencyKey)` with unique idempotency handling. Add wallet controller endpoints.

- [ ] **Step 4: Run GREEN**

Run:
```powershell
cd backend
.\gradlew.bat test --tests com.tangluck.wallet.WalletServiceTest --tests com.tangluck.wallet.WalletControllerTest
```

Expected: PASS.

## Task 6: Campaign Claim, Task Claim, Coupon Claim

**Files:**
- Create backend classes under `promotion/`
- Test: `backend/src/test/java/com/tangluck/promotion/PromotionServiceTest.java`
- Test: `backend/src/test/java/com/tangluck/promotion/CouponServiceTest.java`

- [ ] **Step 1: Write failing promotion tests**

Cover:
- Normal CA user claims register bonus and receives `10000 GC + 0.50 SC`.
- Same user claims same campaign again and gets `CLAIM_DUPLICATED`.
- `manual_review` user claims daily login and receives GC only.
- WA or blocked region gets `REGION_BLOCKED`.
- Campaign with SC and missing `legal_approval_id` cannot publish and returns `LEGAL_APPROVAL_REQUIRED`.
- `WELCOME500` coupon grants `500 GC` once.

- [ ] **Step 2: Run RED**

Run:
```powershell
cd backend
.\gradlew.bat test --tests com.tangluck.promotion.PromotionServiceTest --tests com.tangluck.promotion.CouponServiceTest
```

Expected: FAIL because promotion logic does not exist.

- [ ] **Step 3: Implement promotion services and controllers**

Implement endpoints:
- `GET /api/v1/campaigns`
- `POST /api/v1/campaigns/{campaign_id}/claim`
- `GET /api/v1/tasks/daily`
- `POST /api/v1/tasks/{task_id}/progress`
- `POST /api/v1/tasks/{task_id}/claim`
- `POST /api/v1/coupon/claim`

- [ ] **Step 4: Run GREEN**

Run:
```powershell
cd backend
.\gradlew.bat test --tests com.tangluck.promotion.PromotionServiceTest --tests com.tangluck.promotion.CouponServiceTest
```

Expected: PASS.

## Task 7: Admin APIs, Publish Blocking, And Audit Logs

**Files:**
- Create backend classes under `admin/`
- Test: `backend/src/test/java/com/tangluck/admin/AdminCampaignServiceTest.java`
- Test: `backend/src/test/java/com/tangluck/admin/AdminControllerTest.java`

- [ ] **Step 1: Write failing admin tests**

Cover:
- `POST /api/v1/admin/campaigns` creates draft campaign.
- Publish blocks when no regions, no budget, no rules version, or SC activity has no legal approval.
- Successful publish changes status to `active` and creates `audit_logs`.
- Pause changes status to `paused` and creates `audit_logs`.
- Dashboard summary returns registrations, claims, SC total, risk events.

- [ ] **Step 2: Run RED**

Run:
```powershell
cd backend
.\gradlew.bat test --tests com.tangluck.admin.AdminCampaignServiceTest
```

Expected: FAIL because admin services do not exist.

- [ ] **Step 3: Implement admin services and controllers**

Implement endpoints listed in `findings.md` under `/api/v1/admin/*`. P0-A can use simple admin token role parsing while preserving permission names in method names and tests.

- [ ] **Step 4: Run GREEN**

Run:
```powershell
cd backend
.\gradlew.bat test --tests com.tangluck.admin.AdminCampaignServiceTest --tests com.tangluck.admin.AdminControllerTest
```

Expected: PASS.

## Task 8: Frontend API Client, Router, And State

**Files:**
- Create: `frontend/src/api/http.ts`
- Create: `frontend/src/api/contracts.ts`
- Create: `frontend/src/stores/session.ts`
- Create: `frontend/src/router/index.ts`
- Modify: `frontend/src/main.ts`
- Test: `frontend/src/api/http.test.ts`

- [ ] **Step 1: Write failing frontend API tests**

Test that API client attaches Bearer token, parses JSON success responses, and surfaces backend error code/message for `REGION_BLOCKED`.

- [ ] **Step 2: Run RED**

Run:
```powershell
cd frontend
npm run test -- --run src/api/http.test.ts
```

Expected: FAIL because Vitest and API client are not configured.

- [ ] **Step 3: Add Vitest and implement API client**

Install:
```powershell
npm install -D vitest @vue/test-utils jsdom
```

Implement typed `apiGet` and `apiPost`, session store, and router with routes `/app`, `/app/wallet`, `/app/activity`, `/admin`, `/admin/campaigns`, `/admin/wallet-ledger`, `/admin/audit-logs`.

- [ ] **Step 4: Run GREEN**

Run:
```powershell
cd frontend
npm run test -- --run src/api/http.test.ts
```

Expected: PASS.

## Task 9: C-Side Vue Pages

**Files:**
- Create views under `frontend/src/views/app/`
- Create shared components under `frontend/src/components/`
- Test: `frontend/src/views/app/AppHome.test.ts`

- [ ] **Step 1: Write failing C-side page tests**

Cover:
- Home shows GC/SC balances, welcome bonus, daily login, tasks, Coupon, Terms/Rules/No Purchase/AMOE links.
- Wallet shows ledger rows and SC source explanation.
- Region restricted state displays a clear blocked message.
- Duplicate claim error displays backend `CLAIM_DUPLICATED` message.

- [ ] **Step 2: Run RED**

Run:
```powershell
cd frontend
npm run test -- --run src/views/app/AppHome.test.ts
```

Expected: FAIL because pages do not exist.

- [ ] **Step 3: Implement C-side pages**

Build dense mobile-first pages based on `TangLuck高保真原型.html`, using real API calls and explicit loading, empty, error, disabled, success, and failure states.

- [ ] **Step 4: Run GREEN**

Run:
```powershell
cd frontend
npm run test -- --run src/views/app/AppHome.test.ts
```

Expected: PASS.

## Task 10: Admin Vue Pages

**Files:**
- Create views under `frontend/src/views/admin/`
- Test: `frontend/src/views/admin/AdminCampaigns.test.ts`

- [ ] **Step 1: Write failing admin page tests**

Cover:
- Campaign list renders filters, status tags, SC strategy, budget, legal approval ID.
- Publish with missing legal approval displays blocking reason.
- Audit log table shows operator, action, target, before/after, time, IP.
- Dashboard shows registrations, claims, SC total, risk events.

- [ ] **Step 2: Run RED**

Run:
```powershell
cd frontend
npm run test -- --run src/views/admin/AdminCampaigns.test.ts
```

Expected: FAIL because admin pages do not exist.

- [ ] **Step 3: Implement admin pages**

Use operational layouts: left nav, tables, filters, status tags, detail drawers, publish confirmation, and audit timeline. Avoid marketing hero sections and decorative card-heavy layout.

- [ ] **Step 4: Run GREEN**

Run:
```powershell
cd frontend
npm run test -- --run src/views/admin/AdminCampaigns.test.ts
```

Expected: PASS.

## Task 11: End-To-End Verification

**Files:**
- Create: `frontend/playwright.config.ts`
- Create: `frontend/e2e/p0a-demo.spec.ts`
- Modify: `README.md`

- [ ] **Step 1: Write Playwright P0-A demo spec**

Cover:
- Normal user registers in CA, claims register bonus, daily login, rules task, Coupon, then sees wallet ledger.
- Risk user receives GC only for daily login.
- Blocked WA user sees region restriction.
- Admin publishes an SC campaign without legal approval and sees blocking error; restoring approval allows publish and creates audit log.

- [ ] **Step 2: Run E2E and verify RED if servers are not up**

Run:
```powershell
cd frontend
npx playwright test e2e/p0a-demo.spec.ts
```

Expected before servers are running: FAIL with connection refused. After starting backend and frontend: PASS.

- [ ] **Step 3: Add README runbook**

Create `README.md` with:
```markdown
# Tang Luck P0-A

## Run locally
1. `docker compose up -d mysql`
2. `cd backend && .\gradlew.bat bootRun`
3. `cd frontend && npm install && npm run dev`

## Verify
- Backend: `cd backend && .\gradlew.bat test`
- Frontend unit: `cd frontend && npm run test -- --run`
- Frontend build: `cd frontend && npm run build`
- E2E: `cd frontend && npx playwright test`

## P0-A boundaries
- No real payment.
- No real redemption payout.
- SC is not sold.
- AMOE and No Purchase Necessary must remain visible.
```

- [ ] **Step 4: Full verification**

Run:
```powershell
docker compose up -d mysql
cd backend
.\gradlew.bat test
cd ..\frontend
npm run test -- --run
npm run build
npx playwright test
```

Expected: all tests pass, UI screenshots show no blank or broken C-side/admin pages.

## Self-Review

- Spec coverage: P0-A backend, wallet ledger, idempotency, compliance, promotion, admin, audit, C-side pages, B-side pages, and verification are covered.
- Scope control: Payment, KYC, true redemption, App submission, P0-B/P1/P2 features are excluded except for gray-state UI or documentation.
- Type consistency: API paths match `findings.md` and design spec.
- Risk: `V1__init_schema.sql` is intentionally summarized at table/index level because SQL is long; implementation must translate the database design document exactly before Task 2 passes.
