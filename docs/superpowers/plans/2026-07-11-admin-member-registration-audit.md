# Admin Member Registration Audit Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let B-side operators verify H5 registration quality by viewing and filtering registered members by country, state, compliance consent, and last login.

**Architecture:** Reuse the existing `gameluck-member` admin list/detail API and Admin UI member profile page. Expose the registration compliance fields already stored on `gl_member_profile`, add country/state query filters, and keep this slice read-focused except for existing status operations.

**Tech Stack:** Java Spring Boot, MyBatis Plus, Vue 3, TypeScript, Element Plus, vue-i18n.

---

## File Map

| File | Responsibility |
| --- | --- |
| `backend/gameluck-modules/gameluck-member/src/main/java/com/gameluck/member/domain/bo/MemberProfileBo.java` | Add country/state query fields. |
| `backend/gameluck-modules/gameluck-member/src/main/java/com/gameluck/member/domain/vo/MemberProfileVo.java` | Expose country/state and compliance consent fields. |
| `backend/gameluck-modules/gameluck-member/src/main/java/com/gameluck/member/service/impl/MemberProfileServiceImpl.java` | Add country/state filters to the list wrapper. |
| `backend/gameluck-modules/gameluck-member/src/test/java/com/gameluck/member/service/impl/MemberProfileServiceImplTest.java` | Prove list filtering includes country/state conditions. |
| `admin-ui/src/api/member/profile/types.ts` | Add frontend types for country/state and consent fields. |
| `admin-ui/src/views/member/profile/index.vue` | Add filters, table columns, and detail fields for registration verification. |
| `admin-ui/src/lang/zh_CN.ts` | Add Chinese labels/placeholders for new fields. |
| `admin-ui/src/lang/en_US.ts` | Add English labels/placeholders for new fields. |
| `progress.md` | Record the slice and verification. |

## Task 1: Backend Query and VO Fields

- [x] Add a failing unit test showing `countryCode` and `stateCode` are included in the member list query wrapper.
- [x] Add `countryCode` and `stateCode` to `MemberProfileBo`.
- [x] Add `countryCode`, `stateCode`, `ageConfirmed`, `termsAccepted`, `privacyAccepted`, and `sweepstakesRulesAccepted` to `MemberProfileVo`.
- [x] Add country/state filters in `MemberProfileServiceImpl.buildQueryWrapper`.
- [x] Run targeted member tests.

## Task 2: Admin UI Registration Verification Fields

- [x] Add frontend TypeScript fields for country/state and compliance consent booleans.
- [x] Add country/state filters to the member profile search form.
- [x] Add table columns for country/state and compact compliance consent tags.
- [x] Add detail dialog fields for country/state and each consent field.
- [x] Add Chinese and English i18n labels/placeholders.
- [x] Run Admin i18n check and build.

## Task 3: Verification and Progress

- [x] Run backend member tests.
- [x] Run backend compile.
- [x] Run Admin i18n check.
- [x] Run Admin build.
- [x] Update `progress.md`.
