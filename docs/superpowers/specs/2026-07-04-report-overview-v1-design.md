# Report Overview V1 Design

## Goal

Add a P0 B-side report center that gives operators one overview page for core MVP business metrics.

## Scope

P0 includes:

- Backend module `gameluck-report`.
- API `GET /report/overview/summary`.
- Admin page `report/overview/index`.
- Menu seed `Report Center / Overview`.
- Real-time aggregation from existing MVP tables.

P0 excludes:

- BI drill-down.
- Scheduled snapshot tables.
- Export.
- Charts that require extra visualization dependencies.
- Tenant comparison.

## Metrics

The summary API returns:

- member count from `gl_member_profile`.
- wallet account count and wallet available/frozen totals from `gl_wallet_account`.
- deposit order count and successful deposit amount from `gl_payment_deposit_order`.
- game order count, total bet amount, total payout amount, and net game amount from `gl_game_bet_order`.
- promotion claim count and successful reward amount from `gl_promotion_claim`.
- redemption order count, pending count, approved count, rejected count, and approved amount from `gl_redemption_order`.

All metrics are scoped by the current tenant id.

## Backend Design

`gameluck-report` owns read-only reporting APIs. It does not mutate wallet, member, payment, game, promotion, or redemption data.

`ReportOverviewMapper` contains focused aggregate queries. `ReportOverviewServiceImpl` composes those rows into `ReportOverviewSummaryVo`.

The controller uses:

- route: `/report/overview/summary`
- permission: `report:overview:query`
- response: `R<ReportOverviewSummaryVo>`

## Frontend Design

The admin page is a dense B-side overview:

- top refresh toolbar.
- KPI cards for members, wallet accounts, deposits, game orders, promotion claims, and redemptions.
- section tables for wallet, payment, game, promotion, and redemption state.
- loading and empty fallback states.
- English UI text to avoid the recurring mojibake issue.

The page uses existing Element Plus components only.

## Menu Rules

- Directory/page menu icon: `chart`, verified in `admin-ui/src/assets/icons/svg/chart.svg`.
- Function menu icon: `#`.
- Menu names are English.

## Verification

- TDD focused test for mapper-backed service composition.
- Maven focused test for `gameluck-report`.
- Backend package for `gameluck-admin`.
- SQL import through `backend/script/bin/import-sql-utf8.ps1`.
- `pnpm --dir admin-ui check:menu-icons`.
- `pnpm --dir admin-ui build:prod`.
- Mojibake scan on new report files.

