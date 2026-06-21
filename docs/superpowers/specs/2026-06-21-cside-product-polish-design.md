# C-Side Product Polish Design

## Goal

Make the existing guest-first C-side flow feel closer to a production product by tightening language coverage, account-state behavior, route compatibility, formal-action guidance, and browser verification.

## Current Gaps

- Several C-side strings added during the guest-first work are hard-coded in Vue files instead of `messages.ts`.
- The account bar can fall back to generic labels such as `User` when guest boot fails or before hydration completes.
- Store, KYC, and Redemption guest gates work functionally, but their copy is not localized and the action reason is not consistent.
- Legacy `AppRegister.vue` and `AppLogin.vue` still exist and have tests around standalone pages, even though the production path is modal auth.
- Browser verification found that a stale dev server can make an old page look reachable, so acceptance must check visible product state, not only HTTP 200.

## Scope

This improvement is intentionally frontend-heavy. Backend guest and bind-email APIs are already implemented and verified. The work should not add phone binding, OAuth, guest merge, new admin modules, or new payment/KYC backend behavior.

## Product Behavior

### Account Bar

The C-side shell should clearly show one of these states:

- Loading guest session
- Guest account
- Formal account
- Session error with retry

The shell should not show a vague `User` fallback. When boot fails, the visible message should explain that the session could not start and offer retry instead of leaving the user with an inert page.

### Auth Modal

The modal remains the only primary C-side auth surface.

- `Bind account` upgrades the current guest.
- `Sign in` switches to an existing account.
- Legal document loading, submit disabled state, backend errors, and close behavior remain visible and tested.
- Modal copy must use i18n keys in English and Chinese.

### Routes

Compatibility routes stay:

- `/app/register` redirects to `/app?auth=register`
- `/app/login` redirects to `/app?auth=login`

The old page components may remain in the repo for now, but tests should verify the production route behavior rather than standalone page UX.

### Guest Gates

Store, KYC, and Redemption should use one shared product language pattern:

- What the user can still do as guest.
- Why this action requires binding.
- A single `Bind account` action that opens the auth modal.

Guest gates should not call privileged APIs before binding.

### Navigation

Bottom navigation remains:

- Home
- Slots
- Activity
- Inbox
- Wallet

All labels should come from i18n. Register and Sign in must not return to the bottom nav.

## Technical Design

### i18n

Add new keys under clear namespaces:

- `account.*` for guest/formal/session states.
- `auth.*` for modal tab and action copy.
- `guestGate.*` for Store/KYC/Redemption binding prompts.
- `nav.inbox` for bottom navigation.

Existing English and Chinese locale objects must both receive the same keys.

### AppShell

Update `AppShell.vue` to:

- derive `accountLabel` from explicit account state;
- show `account.loading` while boot is in progress;
- show `account.guest` for guests;
- show formal email for formal users;
- show retry control when `ensureGuestSession()` fails;
- use i18n for account actions and nav labels.

### AuthModal

Update `AuthModal.vue` to:

- replace hard-coded labels with i18n;
- keep `data-test` selectors stable;
- expose backend errors in the current locale wrapper where applicable;
- keep forms compact and mobile-safe.

### Formal-Only Pages

Update `AppStore.vue`, `AppKyc.vue`, and `AppRedemption.vue` to:

- use shared i18n keys for guest gates;
- keep privileged APIs disabled for guests;
- continue dispatching `open-auth-modal` with `{ mode: 'register' }`.

## Testing

### Unit And Component Tests

Add or update Vitest coverage for:

- shell renders localized guest state and no generic `User` fallback;
- bottom nav uses localized `Inbox`;
- auth modal renders localized Bind account / Sign in tabs;
- Store/KYC/Redemption guest gates render localized copy and do not call privileged APIs.

### E2E

Update Playwright checks to assert:

- fresh `/app` browser shows guest account state;
- `/app?auth=register` opens bind modal;
- `/app?auth=login` opens sign-in modal;
- Chinese browser locale renders C-side shell labels in Chinese;
- Store guest gate is visible before binding.

### Browser Acceptance

Use a newly started dev server port for acceptance, not a possibly stale existing port. Verify desktop and mobile screenshots for:

- `/app`
- `/app?auth=register`
- `/app?auth=login`
- `/app/store` as guest

Acceptance must inspect visible text/state and network failures, not only HTTP 200.

## Non-Goals

- Backend API redesign
- Phone binding
- Third-party OAuth
- Guest data merge into existing account
- B-side module expansion
- Full visual redesign of Slots mechanics

## Rollout

This can ship as a small frontend polish release on top of the guest-first auth work. It preserves current backend contracts and route compatibility, so rollback is limited to frontend assets if needed.
