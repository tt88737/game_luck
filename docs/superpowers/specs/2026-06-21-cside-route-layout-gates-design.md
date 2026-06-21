# C-Side Route Layout Gates Design

## Context

TangLuck's product routing document requires the C-side primary tabs to be:

```text
Store | Promo | Lobby | Inbox | Me
```

The current frontend still exposes the older `/app` shell with bottom tabs:

```text
Home | Slots | Activity | Inbox | Wallet
```

This makes the app feel like a demo flow instead of a production sweeps lobby. The first implementation slice will realign the C-side app shell, primary routes, and account aggregation page with the product document while keeping old `/app/*` links working through redirects.

## Goals

1. Make `/lobby` the default C-side entry after opening `/`.
2. Replace the bottom navigation with `Store | Promo | Lobby | Inbox | Me`.
3. Move wallet, redemption, KYC, legal, and support entry points under `Me`.
4. Rename the user-facing activity center concept to `Promo`.
5. Keep existing implementation components where possible, but expose them through production-grade route names.
6. Keep compatibility redirects for old `/app/*` routes so previous links and tests do not hard-break.

## Non-Goals

1. This slice does not rebuild every B-side admin module.
2. This slice does not add real payment provider integration.
3. This slice does not add new backend redemption or KYC rules beyond existing endpoints.
4. This slice does not copy APK assets or competitor UI. The APK remains only a layout density reference.

## Route Design

### Canonical C-side routes

| Route | Page | Component strategy |
| --- | --- | --- |
| `/` | Entry | Redirect to `/lobby` |
| `/store` | Store | Reuse `AppStore.vue` |
| `/promo` | Promo | Reuse and rename intent of `AppActivity.vue` |
| `/lobby` | Lobby | Reuse `AppHome.vue` as the lobby home |
| `/lobby/slots/:gameCode` | Slot play | Reuse `AppSlots.vue` |
| `/inbox` | Inbox | Reuse `AppInbox.vue` |
| `/me` | Account center | New `AppMe.vue` aggregation page |
| `/me/wallet` | Wallet | Reuse `AppWallet.vue` |
| `/me/redeem` | Redemption | Reuse `AppRedemption.vue` |
| `/me/kyc` | KYC | Reuse `AppKyc.vue` |
| `/me/legal` | Legal center | New lightweight legal/support entry section inside `AppMe.vue` or a focused page if the current codebase pattern supports it |

### Compatibility redirects

| Old route | New route |
| --- | --- |
| `/app` | `/lobby` |
| `/app/store` | `/store` |
| `/app/activity` | `/promo` |
| `/app/wallet` | `/me/wallet` |
| `/app/redemption` | `/me/redeem` |
| `/app/kyc` | `/me/kyc` |
| `/app/inbox` | `/inbox` |
| `/app/slots/:gameCode` | `/lobby/slots/:gameCode` |
| `/app/register` | `/lobby?auth=register` |
| `/app/login` | `/lobby?auth=login` |

## App Shell Design

`AppShell.vue` remains the shared C-side shell. It should:

1. Render the TangLuck brand link to `/lobby`.
2. Keep guest boot and auth modal behavior.
3. Use bottom navigation links:
   - `/store`
   - `/promo`
   - `/lobby`
   - `/inbox`
   - `/me`
4. Keep login/register as modal actions, not standalone first-screen pages.
5. Use route query `auth=register|login` on any canonical route to open the auth modal.

The bottom nav must not contain `Redeem`, `Wallet`, `Slots`, or `Activity` as first-level tabs.

## Me Page Design

`AppMe.vue` is the account and compliance aggregation page. It should be useful for both guests and formal users.

Visible sections:

1. Account state:
   - Guest mode or formal account.
   - Bind account / sign in actions for guests.
2. Wallet and redemption:
   - Link to `/me/wallet`.
   - Link to `/me/redeem`.
   - Explain that redeem eligibility depends on KYC, region, risk, playthrough, and minimum threshold.
3. Verification:
   - Link to `/me/kyc`.
4. Legal and support:
   - AMOE / No Purchase Necessary.
   - Rules and Terms links using existing compliance document links where available.
   - Support entry that clearly routes users to the current support/contact surface when implemented, and otherwise opens the auth/contact action area without pretending a live ticket system exists.

Guest users can open `Me`, see account state, and start bind/login. Formal-only actions keep using existing page-level gates.

## Promo Naming

`AppActivity.vue` can remain as the file name for now to avoid a broad mechanical rename. Its route, nav label, and visible product framing should become `Promo`.

The Promo page must include:

1. Daily bonus/task entry.
2. Coupon entry.
3. Campaign list.
4. AMOE / No Purchase Necessary entry.

## Testing

Add or update tests before implementation:

1. Router tests verify canonical route mapping and compatibility redirects.
2. Shell tests verify bottom nav contains `Store`, `Promo`, `Lobby`, `Inbox`, `Me`.
3. Shell tests verify bottom nav does not contain `Slots`, `Activity`, `Wallet`, or `Redeem`.
4. Me page tests verify guest account actions and links to wallet, redeem, KYC, legal/support.
5. Existing page tests are updated from old `/app/*` links to canonical route paths where visible copy or links change.

## Acceptance Criteria

1. Opening `/` redirects to `/lobby`.
2. C-side bottom nav exactly reflects `Store | Promo | Lobby | Inbox | Me`.
3. `Redeem` is reachable through `Me`, not bottom nav.
4. Old `/app/*` URLs redirect to their new canonical routes.
5. Guest mode can browse all five primary tabs.
6. Register/login appear as modals from C-side shell actions.
7. Unit tests, build, and browser checks pass for desktop and mobile layouts.
