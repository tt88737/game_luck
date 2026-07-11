# Client Register Wallet Bonus Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let an H5 player register with compliance consent fields, log in with their password, receive GC/SC wallets, and get a registration bonus through wallet ledger entries.

**Architecture:** Extend the existing `gameluck-member` client auth service into a small registration orchestrator. Keep wallet balance changes inside `IWalletCoreService.credit()`, store member password hashes in `gl_member_profile.password_hash`, and keep H5 changes scoped to the registration page and API client.

**Tech Stack:** Java Spring Boot, MyBatis Plus, Hutool BCrypt, MySQL, Vue 3, TypeScript, Vite.

---

## File Map

| File | Responsibility |
| --- | --- |
| `backend/gameluck-modules/gameluck-member/pom.xml` | Add dependencies on `gameluck-wallet` for registration bonus credits. |
| `backend/gameluck-modules/gameluck-member/src/main/java/com/gameluck/member/domain/MemberProfile.java` | Add password and compliance profile fields. |
| `backend/gameluck-modules/gameluck-member/src/main/java/com/gameluck/member/client/domain/bo/ClientRegisterBo.java` | Request body for H5 registration. |
| `backend/gameluck-modules/gameluck-member/src/main/java/com/gameluck/member/client/service/ClientAuthService.java` | Register member, hash password, credit GC/SC bonuses, and issue token. |
| `backend/gameluck-modules/gameluck-member/src/main/java/com/gameluck/member/client/controller/ClientAuthController.java` | Expose `POST /api/client/auth/register`. |
| `backend/gameluck-modules/gameluck-member/src/test/java/com/gameluck/member/client/service/ClientAuthServiceTest.java` | TDD tests for register and password login behavior. |
| `backend/script/sql/gameluck_client_register.sql` | Add `gl_member_profile` columns and default wallet rules for register bonus. |
| `h5/src/types/client.ts` | Add registration request/response types. |
| `h5/src/api/client.ts` | Add `clientApi.register()`. |
| `h5/src/stores/session.ts` | Add `register()` action that stores token and member. |
| `h5/src/views/RegisterView.vue` | Replace placeholder with real registration form and compliance checkboxes. |
| `progress.md` | Record work and verification. |

## Task 1: Backend TDD for Client Registration

**Files:**
- Modify: `backend/gameluck-modules/gameluck-member/src/test/java/com/gameluck/member/client/service/ClientAuthServiceTest.java`

- [x] Add tests that prove:
  - duplicate username is rejected
  - successful registration inserts a member, calls wallet credit for GC and SC registration bonuses, and returns a token
  - login accepts stored BCrypt password hashes
  - legacy demo members without `passwordHash` still accept `Demo123456`

- [x] Run:

```powershell
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-modules/gameluck-member -am -Plocal -DskipTests=false "-Dtest=ClientAuthServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected before implementation: fail because registration API and constructor dependencies do not exist.

## Task 2: Implement Backend Registration

**Files:**
- Modify: `backend/gameluck-modules/gameluck-member/pom.xml`
- Modify: `backend/gameluck-modules/gameluck-member/src/main/java/com/gameluck/member/domain/MemberProfile.java`
- Create: `backend/gameluck-modules/gameluck-member/src/main/java/com/gameluck/member/client/domain/bo/ClientRegisterBo.java`
- Modify: `backend/gameluck-modules/gameluck-member/src/main/java/com/gameluck/member/client/service/ClientAuthService.java`
- Modify: `backend/gameluck-modules/gameluck-member/src/main/java/com/gameluck/member/client/controller/ClientAuthController.java`
- Create: `backend/script/sql/gameluck_client_register.sql`

- [x] Add `gameluck-wallet` dependency to member module.
- [x] Add member fields: `passwordHash`, `countryCode`, `stateCode`, `ageConfirmed`, `termsAccepted`, `privacyAccepted`, `sweepstakesRulesAccepted`.
- [x] Add `ClientRegisterBo` with validation annotations.
- [x] Implement `register()` in `ClientAuthService`.
- [x] Add `POST /api/client/auth/register`.
- [x] Add SQL with schema guards for new fields and wallet rules for `REGISTER_BONUS`.
- [x] Run the targeted backend test and expect pass.

## Task 3: H5 Registration Flow

**Files:**
- Modify: `h5/src/types/client.ts`
- Modify: `h5/src/api/client.ts`
- Modify: `h5/src/stores/session.ts`
- Modify: `h5/src/views/RegisterView.vue`

- [x] Add register request type.
- [x] Add API call to `/api/client/auth/register`.
- [x] Add session `register()` action.
- [x] Replace placeholder registration page with username, nickname, password, country/state, age, terms, privacy, and rules confirmations.
- [x] On success, route to `/wallet`.
- [x] Run:

```powershell
npm --prefix h5 run build
```

Expected: exit code 0.

## Task 4: Verification and Progress

- [x] Run:

```powershell
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-modules/gameluck-member -am -Plocal -DskipTests=false "-Dtest=ClientAuthServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
npm --prefix h5 run build
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-admin -am compile -Plocal -DskipTests
```

- [x] Update `progress.md`.

## Self-Review

- Scope is one vertical slice: H5 register -> backend member -> wallet credit -> token -> wallet page.
- The plan keeps wallet mutation inside wallet core service.
- The plan does not introduce real KYC, real payment, or real redemption.
