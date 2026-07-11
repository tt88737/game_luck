# Phase 0 Dictionary and H5 I18n Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Establish the first platform-level `gl_*` business dictionaries and add a minimal Chinese/English i18n foundation for the H5 client.

**Architecture:** Reuse the existing GameLuck/RuoYi `sys_dict_type` and `sys_dict_data` tables for business statuses and display tags. Add a small dependency-free H5 i18n module that exposes a reactive locale, a `t()` function, and a language switcher in the global shell.

**Tech Stack:** MySQL seed SQL, Vue 3 Composition API, TypeScript, Vite, existing GameLuck admin dictionary APIs.

---

## File Map

| File | Responsibility |
| --- | --- |
| `backend/script/sql/gameluck_platform_dict.sql` | Seed the first `gl_*` platform dictionaries with idempotent inserts. |
| `h5/src/i18n/messages.ts` | Store H5 Chinese and English copy resources. |
| `h5/src/i18n/index.ts` | Expose reactive locale state, `setLocale()`, and `t()`. |
| `h5/src/App.vue` | Use H5 i18n for global navigation and add a language switcher. |
| `docs/implementation/phase-0-dictionary-i18n.md` | Document dictionary import and H5 i18n usage rules. |
| `progress.md` | Record completed Phase 0 work and verification output. |

## Task 1: Add Platform Dictionary SQL

**Files:**
- Create: `backend/script/sql/gameluck_platform_dict.sql`

- [ ] **Step 1: Create dictionary seed SQL**

Create idempotent SQL for these dictionary types:

```text
gl_common_status
gl_yes_no
gl_member_status
gl_kyc_status
gl_geo_status
gl_risk_decision
gl_currency_type
gl_wallet_account_status
gl_wallet_biz_type
gl_wallet_freeze_status
gl_promotion_type
gl_promotion_status
gl_reward_claim_status
gl_game_status
gl_game_session_status
gl_deposit_status
gl_redemption_status
```

Use `INSERT ... SELECT ... WHERE NOT EXISTS` so repeated imports do not duplicate rows.

- [ ] **Step 2: Verify SQL shape**

Run:

```powershell
rg -n "gl_redemption_status|gl_wallet_biz_type|WHERE NOT EXISTS" backend/script/sql/gameluck_platform_dict.sql
```

Expected: matching rows exist for both dictionary types and idempotent insert guards.

## Task 2: Add H5 I18n Foundation

**Files:**
- Create: `h5/src/i18n/messages.ts`
- Create: `h5/src/i18n/index.ts`
- Modify: `h5/src/App.vue`

- [ ] **Step 1: Add message resources**

Create `messages.ts` with `zh-CN` and `en-US` resources for the global app shell:

```text
brandFallback, navMain, navWallet, navGames, navPromotions, navRedemptions, navMobile, navHome, navHelp, actionLogin, actionLogout, languageLabel
```

- [ ] **Step 2: Add dependency-free i18n runtime**

Create `index.ts` using Vue `ref` and `computed`, with:

```text
locale
currentLocale
setLocale(nextLocale)
t(key)
```

Persist selected locale in `localStorage` key `gameluck:h5:locale`.

- [ ] **Step 3: Wire App shell to i18n**

Update `App.vue` to:

1. Import `currentLocale`, `setLocale`, and `t`.
2. Replace global navigation text with `t()`.
3. Add a compact language selector.
4. Keep existing routing and session behavior unchanged.

- [ ] **Step 4: Verify H5 build**

Run:

```powershell
npm --prefix h5 run build
```

Expected: exit code 0.

## Task 3: Add Documentation

**Files:**
- Create: `docs/implementation/phase-0-dictionary-i18n.md`

- [ ] **Step 1: Document import and usage rules**

Document:

1. How to import `gameluck_platform_dict.sql`.
2. Why statuses live in dictionary tables but state transitions stay in backend services.
3. How H5 should add new copy keys.
4. That future C-side pages should migrate visible copy to `t()`.

- [ ] **Step 2: Verify documentation references**

Run:

```powershell
rg -n "gameluck_platform_dict.sql|状态流转|gameluck:h5:locale" docs/implementation/phase-0-dictionary-i18n.md
```

Expected: all three terms are present.

## Task 4: Final Verification

**Files:**
- Modify: `progress.md`

- [ ] **Step 1: Run verification**

Run:

```powershell
rg -n "gl_redemption_status|gl_wallet_biz_type|WHERE NOT EXISTS" backend/script/sql/gameluck_platform_dict.sql
npm --prefix h5 run build
rg -n "gameluck_platform_dict.sql|状态流转|gameluck:h5:locale" docs/implementation/phase-0-dictionary-i18n.md
git diff --stat
```

Expected:

1. SQL search returns matching dictionary and guard rows.
2. H5 build exits 0.
3. Documentation search returns the required terms.
4. Diff includes only the intended Phase 0 files plus progress log.

- [ ] **Step 2: Update progress**

Append a dated Phase 0 entry to `progress.md` with files changed and verification commands.

## Self-Review

- Spec coverage: covers Phase 0 dictionary SQL, H5 i18n skeleton, documentation, and verification.
- Placeholder scan: no `TBD`, `TODO`, or unresolved implementation markers.
- Type consistency: `Locale`, `MessageKey`, `currentLocale`, `setLocale`, and `t` are defined before use.
