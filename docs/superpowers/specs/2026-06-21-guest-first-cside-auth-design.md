# Guest-First C-Side Auth Design

## Goal

Make the C-side feel like a production consumer product: `/` and `/app` open the playable lobby by default, registration and login appear as modals, and anonymous visitors can play as guests before binding email, phone, or third-party identity to become formal users.

## Current Problems

- `/app` shows a registration prompt when no `userId` exists, so the first screen is not a real lobby.
- `/app/register` and `/app/login` are standalone pages and occupy bottom navigation, which makes auth feel like the product rather than a supporting action.
- Most C-side pages require `session.userId`, so the app has no real guest mode.
- Registering today creates a new user; it does not preserve a guest wallet, slot rounds, activity progress, or inbox rewards.

## Product Behavior

### Default Entry

- `/` redirects to `/app`.
- `/app` always renders the lobby.
- If the browser has no user session, the frontend creates a guest account automatically before loading wallet, lobby, activity, slots, and inbox data.
- The top area shows account state:
  - Guest: "Guest" state with `Bind account` and `Sign in`.
  - Formal user: email or account label with wallet status and account actions.

### Guest Mode

Guest users can use the same core C-side paths as formal users:

- `/app`
- `/app/slots/:gameCode`
- `/app/activity`
- `/app/inbox`
- `/app/wallet`

Guest users can receive GC, spin slots, build task progress, claim GC task rewards, and claim inbox GC rewards.

Guest users are blocked or prompted to bind when the operation requires real identity or compliance:

- purchase/store
- redemption
- KYC submission
- phone/email-dependent security actions

### Bind And Upgrade

Binding upgrades the current guest user instead of creating a separate account.

Supported first release:

- Email + password binding
- Existing email login

Reserved for later:

- Phone binding
- Third-party identity binding

When a guest binds email:

- Existing `users.id` remains unchanged.
- Wallet accounts remain unchanged.
- Slot rounds remain linked to the same `user_id`.
- Activity progress remains linked to the same `user_id`.
- Reward inbox remains linked to the same `user_id`.
- User status changes from `guest` to `active`.
- Email and password hash are populated.
- Required legal documents are accepted during binding.

When a user signs in with an existing formal account while currently guest:

- Frontend switches session to the existing formal account.
- Guest data is not merged in this phase.
- UI should clearly say "Sign in switches to your existing account." Merge can be a later compliance-sensitive feature.

## Backend Design

### User Model

Use the existing `users.status` field:

- `guest`: anonymous playable account
- `active`: formal account

Guest users get:

- generated email such as `guest_<uuid>@guest.tangluck.local`
- generated password hash
- default `countryCode=US`, `stateCode=CA` for local MVP
- wallet accounts created exactly like registered users

### APIs

Add:

- `POST /api/v1/auth/guest`
  - Creates or returns a guest account.
  - Accepts `{ deviceId, countryCode, stateCode, utmSource }`.
  - Returns the same shape as register/login.

- `POST /api/v1/auth/bind-email`
  - Requires `X-User-Id` for the current guest.
  - Accepts email, password, birthDate, region, acceptedDocuments.
  - Validates that the current user is `guest`.
  - Validates email uniqueness against formal users.
  - Updates the same user row to `active`.
  - Returns the same shape as register/login.

Keep:

- `POST /api/v1/auth/register` for direct formal account creation, but C-side primary flow should use bind-email when a guest session exists.
- `POST /api/v1/auth/login` for formal account sign-in.

### Data Integrity

- `users.email` remains unique.
- Guest generated emails must never collide.
- Binding must not create new wallet accounts.
- Binding must write user consent logs for accepted legal documents.

## Frontend Design

### Route Architecture

C-side routes:

- `/app`: lobby home
- `/app/slots/:gameCode`: playable slots
- `/app/activity`: activity center
- `/app/inbox`: reward inbox
- `/app/wallet`: wallet and ledger
- `/app/store`: requires formal account; opens auth modal when guest
- `/app/redemption`: requires formal account/KYC; opens auth modal when guest
- `/app/kyc`: requires formal account; opens auth modal when guest

Compatibility routes:

- `/app/register`: redirect to `/app?auth=register`
- `/app/login`: redirect to `/app?auth=login`

### App Shell

Introduce a C-side shell component:

- `AppShell.vue`
- Holds top account bar, bottom nav, auth modal, and `<RouterView />`.
- Bottom nav becomes: Home, Slots, Activity, Inbox, Wallet.
- Register/Login are removed from bottom nav.

### Auth Modal

Create `AuthModal.vue` with tabs:

- Bind account
- Sign in

Bind form includes:

- email
- password
- birth date
- state
- legal document acceptance

Sign-in form includes:

- email
- password

The modal is triggered by:

- top account bar
- `?auth=register`
- `?auth=login`
- formal-only actions from Store, Redemption, and KYC

### Guest Session Boot

Session store adds:

- `accountType`: `guest | formal`
- `isGuest`
- `ensureGuestSession()`
- `bindEmail(...)`

On app mount:

- If existing session exists, hydrate it.
- If no session exists and route is C-side, create guest session before rendering user-scoped C-side data.
- Admin routes do not create guest sessions.

## UI Direction

The lobby should feel like an actual app home, not a marketing or registration page:

- show wallet band immediately
- show playable Slots card as the first gameplay object
- show activity/inbox state and claimable indicators
- show account state in a compact top bar
- keep auth as a modal overlay, not a separate main destination

## Testing

Backend:

- guest account creation returns wallet and token
- guest can spin and keep ledger
- bind email upgrades same user id and preserves wallet balance
- duplicate email binding fails

Frontend:

- `/app` without session creates guest session and renders lobby
- register route redirects to auth modal
- login route redirects to auth modal
- bottom nav excludes register/login and includes Slots/Inbox
- binding email keeps current user id and switches account state

E2E:

- fresh browser lands on lobby as guest
- guest opens Slots and sees wallet
- auth modal opens from account bar
- bind-email closes modal and shows formal state

## Non-Goals

- Phone binding implementation
- Third-party OAuth implementation
- Guest-to-existing-account merge
- Full KYC redesign
