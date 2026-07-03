# Member Profile V1 Design

## Goal

Build a P0 member center for B-side operators to create, query, edit, freeze, disable, and reactivate member profiles.

## Scope

- Store member master data independently from system admin users.
- Provide member list, detail, create, update, status update, and logical delete APIs.
- Provide an admin page for member profile operations.
- Seed RuoYi menus with local icons only.

## Out Of Scope

- C-side registration and login.
- Passwords, sessions, OAuth, KYC, bank cards, addresses, referrals, VIP levels, and labels.
- Wallet account creation. Wallet accounts remain lazy-created by wallet operations.
- Risk engine and manual balance adjustment.

## Domain Model

### `gl_member_profile`

- `tenant_id`: RuoYi tenant boundary.
- `member_no`: generated business number.
- `username`: unique login/display account within tenant.
- `nickname`: operator-facing display name.
- `status`: `ACTIVE`, `FROZEN`, or `DISABLED`.
- `risk_level`: `NORMAL`, `WATCH`, `HIGH`.
- `register_channel`: channel identifier, default `ADMIN`.
- `last_login_time`: reserved for C-side login integration.
- `remark`: operator note.

## Business Rules

- Username is required and unique within a tenant.
- New member default status is `ACTIVE`.
- New member default risk level is `NORMAL`.
- Status update accepts only `ACTIVE`, `FROZEN`, or `DISABLED`.
- Risk level accepts only `NORMAL`, `WATCH`, or `HIGH`.
- Member delete is logical delete through `del_flag`.

## Admin UI

- Page: `member/profile/index`.
- Filters: username, nickname, status, risk level, register channel.
- Table: member number, username, nickname, status, risk level, channel, last login time, created time.
- Operations:
  - Add member.
  - Edit profile.
  - Freeze / disable / reactivate.
  - View detail.
  - Delete profile.
- High-risk status changes require confirmation.

## Encoding And Menu Constraints

- New files must be UTF-8.
- SQL menu names use English to avoid seed-script mojibake.
- Directory/page icons must exist in `admin-ui/src/assets/icons/svg`.
- Function menu icons must be `#`.
- Run `pnpm --dir admin-ui check:menu-icons` before completion.
