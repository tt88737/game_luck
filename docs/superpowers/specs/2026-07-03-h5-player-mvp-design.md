# H5 Player MVP Design

## Goal

Build the first C-side player H5 shell as a separate Vue3 app under `h5/`. It gives players a product-first entry to wallet, games, promotions, redemption, login, registration, and help pages.

## Scope

P0 includes:

- A standalone `h5/` Vite Vue TypeScript app.
- Routes for `/`, `/login`, `/register`, `/wallet`, `/games`, `/promotions`, `/redemptions`, and `/help`.
- Static demo data for wallet balance, games, promotions, redemption status, and history.
- A responsive player-facing layout for mobile and desktop.
- A route document in `h5/ROUTES.md`.

P0 excludes:

- Real player authentication.
- Real backend API integration.
- Payment, KYC, risk, or region enforcement.
- App packaging, PWA manifest, and push notifications.

## Architecture

The player H5 app is isolated from `admin-ui/`. It is a separate Vite project because the admin UI is the B-side RuoYi backend frontend and should not contain player pages.

The first version uses local route components and static data. Later API integration can replace the static data behind small composables or API modules without changing the route structure.

## Routes

| Route | Purpose |
| --- | --- |
| `/` | Player home with wallet summary and primary entry points |
| `/login` | Player login placeholder |
| `/register` | Player registration placeholder |
| `/wallet` | Wallet balance and ledger preview |
| `/games` | Simulated game list |
| `/promotions` | Promotion reward list |
| `/redemptions` | Redemption request and status preview |
| `/help` | Rules, support, and policy entry |

## UI Requirements

- The first screen must show the player action surface, not a marketing landing page.
- The home page must expose balance, wallet, games, promotions, and redemption entry points.
- Mobile layout must fit around 390px width without text overlap.
- Desktop layout must stay readable around 1366px width.
- Use concrete player-facing states: logged out, processing, insufficient balance, KYC required, disabled, success, and failed.
- Avoid one-note purple/blue gradients, decorative blobs, or generic marketing copy.

## Verification

- `npm install --prefix h5`
- `npm run build --prefix h5`
- Visual inspection through the local Vite dev server at mobile and desktop widths when browser tooling is available.
- `git status --short` before commit to keep the commit scoped.

