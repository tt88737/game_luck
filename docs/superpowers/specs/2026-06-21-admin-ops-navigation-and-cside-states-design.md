# Admin Ops Navigation And C-Side States Design

## Context

The C-side has been realigned to the production tab model:

```text
Store | Promo | Lobby | Inbox | Me
```

The admin side still exposes a flat operations list. That makes the back office feel like a collection of pages rather than a production operations console mapped to the C-side business loop. The product routing document requires B-side modules to be grouped by business domain and to show how each configuration area affects C-side routes and gates.

## Problem

Current admin navigation mixes users, compliance, lobby, campaigns, games, orders, KYC, redemption, wallet, notifications, AMOE, support, and audit logs in one flat list.

This creates three product problems:

1. Operators cannot immediately see which module controls Store, Promo, Lobby, Inbox, or Me.
2. High-risk modules such as Redemption, KYC/Risk, Compliance, and Audit Logs do not stand out as operational control areas.
3. Planned or partial modules are not clearly separated from live operating modules.

## Goals

1. Group admin navigation according to the production routing document:
   - Dashboard
   - User & Guest
   - Store & Packages
   - Promo & Rewards
   - Game Lobby
   - Inbox & Notification
   - Wallet & Ledger
   - Redemption
   - KYC & Risk
   - Compliance
   - CMS / Rules / AMOE
   - Support
   - BI Reports
   - Audit Logs
   - System / RBAC
2. Keep existing admin routes working; this slice is a navigation and operational framing improvement.
3. Add visible C-side impact labels in the admin nav so operators can see the mapping to Store, Promo, Lobby, Inbox, and Me.
4. Separate live modules from planned modules without pretending unsupported capabilities are complete.
5. Preserve i18n coverage for English and Chinese.
6. Verify desktop and mobile admin navigation screenshots.

## Non-Goals

1. This slice does not implement full RBAC.
2. This slice does not add new backend tables for BI reports, support tickets, or AMOE applications.
3. This slice does not rebuild every admin page.
4. This slice does not change the C-side route model delivered in the previous step.

## Admin Navigation Design

`AdminNav.vue` should change from a flat list to grouped sections.

Each group contains:

```ts
{
  titleKey: string
  impactKey: string
  items: Array<{
    to: string
    key: string
    status: 'live' | 'planned'
  }>
}
```

Group behavior:

1. The group title is always visible.
2. The C-side impact label is visible below the title, for example `Impacts Store`.
3. Live links route to existing pages.
4. Planned links route to an existing planned-module page only when that route already exists.
5. Planned items use a subdued visual state and should not look like completed modules.

## Initial Group Mapping

| Group | C-side impact | Live items | Planned items |
| --- | --- | --- | --- |
| Dashboard | Operations overview | `/admin` | BI summary |
| User & Guest | Login, guest conversion, account state | `/admin/users` | guest sessions, binding history |
| Store & Packages | Store | `/admin/packages`, `/admin/orders` | payment config |
| Promo & Rewards | Promo | `/admin/campaigns`, `/admin/activity-dashboard` | VIP, referral, tournament |
| Game Lobby | Lobby | `/admin/lobby`, `/admin/games`, `/admin/game-rounds` | providers, jackpot config |
| Inbox & Notification | Inbox | `/admin/notifications` | templates, delivery logs |
| Wallet & Ledger | Me > Wallet | `/admin/wallet-ledger` | SC lots, playthrough config |
| Redemption | Me > Redeem | `/admin/redemptions` | eligibility config, payout providers |
| KYC & Risk | Me > KYC and route gates | `/admin/kyc` | risk queue, device graph |
| Compliance | Store, Promo, Lobby, Redeem gates | `/admin/regions` | SC policy switches |
| CMS / Rules / AMOE | Promo AMOE and Me Legal | `/admin/legal-documents`, `/admin/amoe` | AMOE workflow |
| Support | Me > Support | `/admin/support` | ticket queue |
| Audit Logs | Compliance traceability | `/admin/audit-logs` | export controls |
| System / RBAC | Admin access | none | roles, permissions |

## UI Rules

1. B-side remains dense and operational.
2. No hero sections, decorative marketing cards, or oversized empty modules.
3. The left navigation should remain scannable on desktop.
4. Mobile admin nav should not consume the whole viewport; it should support grouped scrolling with readable labels.
5. Planned items must be visually distinct from live links.
6. Text must not overflow group labels or route links.

## Testing

Add tests before implementation:

1. Admin navigation renders required group names.
2. Admin navigation renders C-side impact labels.
3. Live links include routes for Store, Promo, Lobby, Inbox, Wallet, Redemption, KYC, Compliance, AMOE, Support, and Audit Logs.
4. Planned items are marked with planned visual state.
5. Chinese locale renders grouped navigation labels.
6. Playwright checks desktop and mobile admin navigation screenshots.

## Acceptance Criteria

1. `/admin` shows grouped navigation instead of one flat list.
2. Operators can see which B-side group affects which C-side tab or account route.
3. Existing admin page routes still work.
4. Planned modules are visibly marked and do not look fully live.
5. English and Chinese i18n keys are present for group and impact labels.
6. Unit tests, build, Playwright, and browser screenshot checks pass.
