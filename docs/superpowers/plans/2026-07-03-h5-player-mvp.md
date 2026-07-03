# H5 Player MVP Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Create a standalone Vue3 H5 player shell with routes for login, register, wallet, games, promotions, redemptions, and help.

**Architecture:** Add a separate Vite Vue TypeScript app under `h5/` so C-side player pages do not mix into the RuoYi `admin-ui/`. Use simple static data and focused route components for the first MVP, keeping the structure ready for later API integration.

**Tech Stack:** Vue 3, Vite, TypeScript, Vue Router, CSS.

---

### Task 1: Scaffold H5 Project

**Files:**
- Create: `h5/`

- [x] **Step 1: Scaffold Vue TypeScript app**

Run:

```powershell
npm create vite@latest h5 -- --template vue-ts
```

Expected: `h5/package.json`, `h5/src/main.ts`, and Vite project files are created.

- [x] **Step 2: Install Vue Router**

Run:

```powershell
npm install --prefix h5 vue-router@4
```

Expected: dependencies install successfully.

### Task 2: Add Routes And Static Player Pages

**Files:**
- Modify: `h5/src/main.ts`
- Modify: `h5/src/App.vue`
- Create: `h5/src/router/index.ts`
- Create: `h5/src/data/demo.ts`
- Create: `h5/src/views/HomeView.vue`
- Create: `h5/src/views/LoginView.vue`
- Create: `h5/src/views/RegisterView.vue`
- Create: `h5/src/views/WalletView.vue`
- Create: `h5/src/views/GamesView.vue`
- Create: `h5/src/views/PromotionsView.vue`
- Create: `h5/src/views/RedemptionsView.vue`
- Create: `h5/src/views/HelpView.vue`

- [x] **Step 1: Configure Vue Router**

Create `h5/src/router/index.ts` with routes for `/`, `/login`, `/register`, `/wallet`, `/games`, `/promotions`, `/redemptions`, and `/help`.

- [x] **Step 2: Add static demo data**

Create `h5/src/data/demo.ts` with wallet balances, ledger rows, games, promotions, redemptions, and player state flags.

- [x] **Step 3: Replace starter app**

Update `h5/src/main.ts` to install the router and update `h5/src/App.vue` to render the app shell and `<RouterView />`.

- [x] **Step 4: Add route views**

Create the route view files listed above. Each page must show the main user task and at least one concrete state such as logged out, insufficient balance, KYC required, processing, success, failed, or disabled.

### Task 3: Add Product Styling And Route Docs

**Files:**
- Modify: `h5/src/style.css`
- Create: `h5/ROUTES.md`

- [x] **Step 1: Replace default styles**

Update `h5/src/style.css` with responsive player-facing styles. Verify the palette does not read as a one-note purple or blue gradient theme.

- [x] **Step 2: Add route documentation**

Create `h5/ROUTES.md`:

```markdown
# H5 Routes

| Route | Purpose |
| --- | --- |
| / | Player home |
| /login | Player login |
| /register | Player register |
| /wallet | Wallet balances and ledger |
| /games | Game list |
| /promotions | Promotion list |
| /redemptions | Redemption request |
| /help | Help and policy pages |
```

### Task 4: Verify And Commit

**Files:**
- Verify: `h5/`
- Verify: `docs/superpowers/specs/2026-07-03-h5-player-mvp-design.md`
- Verify: `docs/superpowers/plans/2026-07-03-h5-player-mvp.md`

- [x] **Step 1: Build the H5 app**

Run:

```powershell
npm run build --prefix h5
```

Expected: Vite build exits `0`.

- [x] **Step 2: Inspect git status**

Run:

```powershell
git status --short
```

Expected: only `h5/` and the two H5 documentation files are changed.

- [x] **Step 3: Commit**

Run:

```powershell
git add h5 docs\superpowers\specs\2026-07-03-h5-player-mvp-design.md docs\superpowers\plans\2026-07-03-h5-player-mvp.md
git commit -m "feat(h5): scaffold vue3 player h5"
```

Expected: commit succeeds.
