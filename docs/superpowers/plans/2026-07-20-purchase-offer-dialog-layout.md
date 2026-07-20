# Purchase Offer Dialog Layout Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the purchase offer create/edit dialog readable and stable by replacing compressed grant rows with a responsive three-column form layout and a fixed action footer.

**Architecture:** Keep all behavior in the existing Vue SFC and preserve its model, validation, dictionaries, and API calls. Add semantic layout classes around existing Element Plus form controls, then implement desktop, medium, and narrow breakpoints in scoped SCSS. Verify compilation and the real rendered dialog rather than introducing brittle unit tests for CSS geometry.

**Tech Stack:** Vue 3, TypeScript, Element Plus, SCSS, Vite, Playwright/browser screenshots

---

## File Map

- Modify: `admin-ui/src/views/payment/purchase-offer/index.vue` - dialog markup, responsive form layout, scroll region, grant item presentation, and footer positioning.
- Create during verification: `docs/implementation/purchase-offer-dialog-1440.png` - wide desktop evidence.
- Create during verification: `docs/implementation/purchase-offer-dialog-1366.png` - standard desktop evidence.
- Create during verification: `docs/implementation/purchase-offer-dialog-narrow.png` - narrow viewport evidence.

### Task 1: Restructure the dialog layout

**Files:**
- Modify: `admin-ui/src/views/payment/purchase-offer/index.vue:81-255`

- [ ] **Step 1: Capture the current behavior before editing**

Open `http://localhost:5173/payment/purchase-offer`, click the add action, and confirm the dialog contains the existing base, payment, grant, scope, and validity sections. Record that the current grant controls are compressed at `1440x900`; this is the visual failing case supplied in the requirement.

- [ ] **Step 2: Widen the dialog and mark the form as a vertical-label layout**

Change the dialog and form opening tags to:

```vue
<el-dialog
  v-model="dialog.visible"
  :title="dialog.title"
  width="min(1200px, calc(100vw - 32px))"
  append-to-body
  class="purchase-offer-dialog"
>
  <!-- keep the existing alert unchanged -->
  <el-form ref="offerFormRef" :model="form" :rules="rules" label-position="top" class="offer-form">
```

Do not change any `v-model`, `prop`, rules, dictionary options, or event handlers.

- [ ] **Step 3: Convert ordinary sections to a semantic grid**

For base information, payment settings, applicable scope, and validity, replace each `el-row`/`el-col` wrapper with this structure while retaining the existing `el-form-item` contents:

```vue
<div class="form-grid">
  <div class="form-grid__item">...</div>
</div>
```

Use `form-grid__item--wide` only for fields that require two columns, such as product name or a range value. Keep each field in source order so keyboard navigation matches visual order.

- [ ] **Step 4: Convert each grant row to a responsive grant grid**

Replace the grant `el-row` and `el-col` wrappers with:

```vue
<div class="grant-grid">
  <div class="grant-grid__item">...</div>
  <div v-if="item.gameScopeType !== 'ALL'" class="grant-grid__item grant-grid__item--wide">...</div>
</div>
```

Keep conditional fields adjacent to the control that enables them. Add `title` or an Element Plus tooltip naming the delete icon, preserve `v-if="canRemoveGrant"`, and keep `removeGrant(index)` unchanged.

- [ ] **Step 5: Run formatter and inspect the diff**

Run:

```powershell
pnpm --dir admin-ui exec prettier --write src/views/payment/purchase-offer/index.vue
git diff --check -- admin-ui/src/views/payment/purchase-offer/index.vue
git diff -- admin-ui/src/views/payment/purchase-offer/index.vue
```

Expected: Prettier exits `0`, `git diff --check` reports no whitespace errors, and the diff contains layout-only template changes with no model or API changes.

### Task 2: Implement responsive sizing and fixed actions

**Files:**
- Modify: `admin-ui/src/views/payment/purchase-offer/index.vue:462-508`

- [ ] **Step 1: Add dialog shell and scroll-region styles**

Replace the current dialog body rule with scoped deep selectors equivalent to:

```scss
.purchase-offer-dialog {
  :deep(.el-dialog) {
    display: flex;
    max-height: calc(100vh - 32px);
    flex-direction: column;
    margin: 16px auto;
  }

  :deep(.el-dialog__body) {
    min-height: 0;
    overflow-y: auto;
    padding: 16px 20px;
  }

  :deep(.el-dialog__footer) {
    flex: 0 0 auto;
    border-top: 1px solid var(--el-border-color-lighter);
    padding: 12px 20px 16px;
  }
}
```

Keep header and footer outside the scrolling body so actions remain accessible.

- [ ] **Step 2: Add stable three-column grids and control sizing**

Add:

```scss
.form-grid,
.grant-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0 20px;
}

.form-grid__item,
.grant-grid__item {
  min-width: 0;
}

.form-grid__item--wide,
.grant-grid__item--wide {
  grid-column: span 2;
}

.offer-form :deep(.el-form-item) {
  margin-bottom: 18px;
}

.offer-form :deep(.el-select),
.offer-form :deep(.el-input-number),
.offer-form :deep(.el-date-editor) {
  width: 100%;
}
```

Preserve the existing section and grant row visual hierarchy, using no additional nested card component.

- [ ] **Step 3: Add medium and narrow breakpoints**

Add:

```scss
@media (max-width: 1200px) {
  .form-grid,
  .grant-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .purchase-offer-dialog {
    :deep(.el-dialog__body) {
      padding-inline: 14px;
    }
  }

  .form-grid,
  .grant-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .form-grid__item--wide,
  .grant-grid__item--wide {
    grid-column: auto;
  }
}
```

Remove the old blanket `.el-col` media rule because the new grid owns responsive behavior.

- [ ] **Step 4: Run the production build**

Run:

```powershell
pnpm --dir admin-ui build:prod
```

Expected: command exits `0` with a completed Vite production build and no Vue template or SCSS errors.

### Task 3: Verify the real workflow and visual states

**Files:**
- Verify: `admin-ui/src/views/payment/purchase-offer/index.vue`
- Create: `docs/implementation/purchase-offer-dialog-1440.png`
- Create: `docs/implementation/purchase-offer-dialog-1366.png`
- Create: `docs/implementation/purchase-offer-dialog-narrow.png`

- [ ] **Step 1: Ensure the application services are available**

Run:

```powershell
Invoke-WebRequest -UseBasicParsing http://localhost:8080/ -TimeoutSec 10
Invoke-WebRequest -UseBasicParsing http://localhost:5173/ -TimeoutSec 10
```

Expected: both endpoints return HTTP `200`. If either service is absent, run `./scripts/start-admin-local.ps1 -SkipPackage` before continuing.

- [ ] **Step 2: Verify the `1440x900` default state**

Open the purchase offer page, open the add dialog, and capture `docs/implementation/purchase-offer-dialog-1440.png`. Confirm:

- three readable columns are visible;
- select values and six-decimal amounts are not clipped;
- the two default grant items remain visually separate;
- the footer buttons remain visible while the body scrolls;
- no horizontal scrollbar exists.

- [ ] **Step 3: Verify the `1366x768` conditional-field state**

At `1366x768`, switch one grant to fixed wagering and another to multiplier wagering, then switch one game scope away from `ALL`. Capture `docs/implementation/purchase-offer-dialog-1366.png`. Confirm dynamic fields wrap into the grid without squeezing adjacent controls or overlapping validation space.

- [ ] **Step 4: Verify narrow responsive behavior**

Use a viewport narrower than `768px`, open the same dialog, and capture `docs/implementation/purchase-offer-dialog-narrow.png`. Confirm fields render in one column, the dialog remains within the viewport, and cancel/confirm remain reachable.

- [ ] **Step 5: Verify interactions and validation**

Add a grant item, remove one, submit with a required field empty, and dismiss the dialog. Confirm the list updates without layout shifts, validation text stays below its field, delete remains disabled when only one item remains, and no console error is emitted.

- [ ] **Step 6: Run final checks and commit**

Run:

```powershell
pnpm --dir admin-ui build:prod
git diff --check -- admin-ui/src/views/payment/purchase-offer/index.vue
git status --short
```

Expected: build exits `0`, no whitespace error is reported, and only the intended Vue file plus new verification screenshots are included in the implementation commit.

Commit only the implementation-owned paths:

```powershell
git add admin-ui/src/views/payment/purchase-offer/index.vue docs/implementation/purchase-offer-dialog-1440.png docs/implementation/purchase-offer-dialog-1366.png docs/implementation/purchase-offer-dialog-narrow.png
git commit -m "fix: improve purchase offer dialog layout"
```
