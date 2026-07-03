# Promotion Reward V1 Design

## Goal

Build a P0 promotion center that lets operators configure simulated wallet rewards and claim them for a member without code changes.

## Scope

- Admin can create, update, enable, disable, query, and delete simulated reward configurations.
- Admin can trigger a claim for a member from the promotion page.
- Claiming credits the member wallet through `IWalletCoreService.credit`.
- A member can claim the same promotion only once per tenant.
- Claim records are queryable for audit and troubleshooting.

## Out Of Scope

- C-side activity landing page.
- Rule engines such as level, VIP, channel, KYC, region, or risk conditions.
- Scheduled campaigns and automatic reward distribution.
- Real payment provider or external callback integration.

## Domain Model

### `gl_promotion_reward`

Stores configurable reward campaigns.

- `tenant_id`: RuoYi tenant boundary.
- `promotion_no`: generated business number.
- `promotion_name`: operator-facing name.
- `currency_code`: wallet currency, default `SC`.
- `reward_amount`: wallet credit amount.
- `status`: `ACTIVE` or `INACTIVE`.
- `start_time`, `end_time`: optional active window.
- `version`, `del_flag`: optimistic lock and logical delete.

### `gl_promotion_claim`

Stores member claim records.

- `claim_no`: generated business number.
- `promotion_id`, `promotion_no`: claimed promotion.
- `member_id`: claim member.
- `currency_code`, `reward_amount`: copied from promotion at claim time.
- `status`: `SUCCESS` or `FAILED`.
- `wallet_transaction_no`: wallet transaction created by credit.
- `idempotency_key`: `promotion:claim:{tenantId}:{promotionNo}:{memberId}`.
- `fail_reason`: wallet or validation failure.

## Business Rules

- Only `ACTIVE` promotions can be claimed.
- If `start_time` exists, current time must be greater than or equal to it.
- If `end_time` exists, current time must be less than or equal to it.
- `reward_amount` must be greater than `0`.
- Claim idempotency is enforced by unique key `(tenant_id, promotion_id, member_id)`.
- Repeated claim returns the existing claim record and does not call wallet credit again.
- Wallet credit uses:
  - `sourceType = PROMOTION`
  - `businessNo = claimNo`
  - `idempotencyKey = promotion:claim:{tenantId}:{promotionNo}:{memberId}`
  - `releaseMode = NONE`

## Admin UI

- Page: `promotion/reward/index`.
- Filters: promotion name, currency, status.
- Table: name, number, currency, amount, active window, status, created time, operations.
- Operations:
  - Add / edit / delete reward configuration.
  - Enable / disable reward configuration.
  - Claim for member via modal.
  - View claim records in a drawer.
- Button permissions use `promotion:reward:*`.
- Directory/page icons must use local SVG icons only. Function menu icons must be `#`.

## Encoding And Generation Constraints

- New files must be UTF-8.
- SQL file must be imported with `backend/script/bin/import-sql-utf8.ps1`.
- Do not pipe SQL through PowerShell.
- New menu names use English in SQL to avoid seed-script mojibake.
- Run a mojibake scan on all new promotion files before completion.
