# Report Daily Trends Design

## Goal

Add a Phase 5 B-side `Report Center / Trends` page that shows daily all-business operating trends for the most recent 7 or 30 days.

This page extends the existing `Report Overview` module with a time dimension. It is a read-only operating dashboard for quick trend review, not a BI drill-down or finance reconciliation tool.

## Scope

Included:

- Backend API: `GET /report/trends/daily?range=7|30`.
- Backend service and mapper under the existing `gameluck-report` module.
- Admin UI page: `admin-ui/src/views/report/trends/index.vue`.
- Admin API wrappers under `admin-ui/src/api/report/trends/`.
- Menu seed under `Report Center / Trends`.
- Permission: `report:trends:query`.
- Daily trend rows for recent 7 or 30 days.
- Continuous date output: missing dates are returned as zero rows.
- Top period totals derived from the returned daily rows.

Excluded:

- New chart dependency.
- Hourly or monthly trend granularity.
- Export.
- BI drill-down by member, currency, channel, game, or promotion.
- Tenant comparison.
- Finance reconciliation between wallet ledger and business orders.
- New anomaly or risk rules.

## Metrics

Each daily row contains:

- `reportDate`
- `memberCount`: member profiles created on that date.
- `depositOrderCount`
- `successfulDepositAmount`
- `gameOrderCount`
- `totalBetAmount`
- `totalPayoutAmount`
- `netGameAmount`
- `promotionClaimCount`
- `successfulRewardAmount`
- `redemptionOrderCount`
- `pendingRedemptionCount`
- `approvedRedemptionAmount`

Metric source tables:

- `gl_member_profile`
- `gl_payment_deposit_order`
- `gl_game_bet_order`
- `gl_promotion_claim`
- `gl_redemption_order`

All queries are scoped by the current tenant id and `del_flag = '0'`.

Status filters follow the current `Report Overview` metric meanings:

- successful deposits: `gl_payment_deposit_order.status = 'SUCCESS'`
- successful rewards: `gl_promotion_claim.status = 'SUCCESS'`
- pending redemptions: `gl_redemption_order.status = 'PENDING'`
- approved redemption amount: `gl_redemption_order.status = 'APPROVED'`

## Backend Design

`gameluck-report` remains the owner of read-only report APIs. It must not mutate wallet, member, payment, game, promotion, or redemption data.

Add:

- `ReportDailyTrendVo`
- `ReportDailyTrendQueryBo`
- `ReportTrendMapper`
- `ReportTrendService`
- `ReportTrendController`

Controller:

- route: `/report/trends/daily`
- method: `GET`
- permission: `report:trends:query`
- query parameter: `range`
- accepted range values: `7`, `30`
- invalid or missing range falls back to `7`

Mapper aggregation uses `DATE(create_time)` for daily grouping. It can use separate focused SQL methods per business source, or a single union-style query if that stays readable. The service composes source aggregates into one continuous date sequence.

Service responsibilities:

- resolve current tenant id.
- normalize `range`.
- compute inclusive date window ending on the current local date.
- create zero-value rows for every date in the window.
- merge aggregate results by `reportDate`.
- normalize null numeric values to `0`.
- return rows ordered by date descending for the admin table.

## Frontend Design

The page is a B-side operating tool inside the existing admin shell.

Route and menu:

- parent menu: existing `Report Center`
- page menu name: `Trends`
- component: `report/trends/index`
- icon: `chart`
- function menu icon: `#`

Page layout:

- top toolbar with page title, subtitle, range segmented select, and refresh button.
- period total KPI row for deposits, game net amount, rewards, approved redemptions, and pending redemptions.
- daily trend table ordered by date descending.

Table columns:

- date
- new members
- deposit orders
- successful deposit amount
- game orders
- bet amount
- payout amount
- net game amount
- promotion claims
- successful reward amount
- redemption orders
- pending redemptions
- approved redemption amount

UI rules:

- use existing Element Plus components only.
- use i18n keys in `zh_CN.ts` and `en_US.ts`.
- no marketing hero, decorative gradients, or card-heavy landing page layout.
- amount columns are right-aligned and rendered with fixed precision.
- loading, empty, error, and no-permission states must be handled.
- range switch reloads data without a full page refresh.

## Data Flow

1. Operator opens `Report Center / Trends`.
2. Admin UI calls `GET /report/trends/daily?range=7`.
3. Backend resolves tenant id and date window.
4. Mapper reads daily aggregates from existing business tables.
5. Service fills missing dates and returns daily rows.
6. Admin UI computes period totals from returned rows and renders KPI row plus table.
7. Operator switches to `30` days or clicks refresh; the same API is called with the selected range.

## Error Handling

Backend:

- missing or invalid `range` uses `7`.
- null aggregate amounts return zero.
- no business data returns a full zero-filled date sequence.
- permission is enforced with `@SaCheckPermission("report:trends:query")`.

Frontend:

- show loading during request.
- show empty state only if the API returns no rows, which should be rare because the backend fills dates.
- show localized error message if request fails.
- hide refresh and data access behind `report:trends:query` permission.

## Testing

Backend tests:

- invalid range defaults to 7 days.
- 30-day range returns 30 continuous rows.
- missing dates are zero-filled.
- aggregate rows merge into the correct date.
- null amounts normalize to zero.

Verification commands:

```powershell
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-modules/gameluck-report -am -Plocal -DskipTests=false "-Dtest=ReportTrendServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-admin -am compile -Plocal -DskipTests
pnpm --dir admin-ui check:i18n
pnpm --dir admin-ui check:menu-icons
pnpm --dir admin-ui build:dev
```

Runtime smoke:

- backend responds to `GET /report/trends/daily?range=7` for an authenticated admin user with permission.
- Admin UI opens `Report Center / Trends`.
- range switch between 7 and 30 days updates the table.
- no visible text overlap at desktop width.

## Acceptance Criteria

- `Report Center / Trends` appears under the existing report menu.
- Authorized users can query recent 7-day and 30-day daily trends.
- Unauthorized users cannot call the API.
- Returned daily rows are continuous and ordered by date descending.
- Existing `Report Overview` behavior remains unchanged.
- No new third-party frontend chart dependency is introduced.
- Static verification and focused backend tests pass.
