# Daily Login Reward Design

## Objective

Build the next Phase 1 vertical slice: a configurable daily login reward that lets an H5 player claim `GC 100 + SC 1` once per day, credits both rewards through the wallet service, and leaves B-side operators with configurable reward amounts and queryable claim records.

## Scope

This slice covers:

- B-side configurable daily login reward activity.
- H5 display of today's daily login reward state.
- H5 claim action.
- Wallet credit for configured GC and SC rewards.
- Claim records for audit and operations.
- Chinese and English copy through the existing i18n structure.

This slice does not cover:

- Consecutive-login streak rewards.
- VIP or member-level segmentation.
- Geo, KYC, or risk blocking.
- Real push notifications or inbox.
- Calendar-style reward UI.

## Product Rules

- The first seeded activity is "每日登录奖励" / "Daily Login Reward".
- Default reward is `GC 100` plus `SC 1`.
- The reward must be configurable from the B-side, not hardcoded in H5.
- A member can claim the configured daily login reward once per platform day.
- The platform day uses backend server date for the first version.
- Claiming credits wallet balances only through `IWalletCoreService.credit`.
- Wallet source type is `DAILY_REWARD`.
- Duplicate same-day claims return an already-claimed state rather than creating another wallet transaction.
- API returns state codes, not Chinese display labels.

## Data Model

Reuse the existing promotion center tables and extend them only where the daily behavior needs structure.

`gl_promotion_reward` additions:

- `promotion_type`: identifies `DAILY_LOGIN`, `REGISTER_BONUS`, and future reward types.
- `daily_claim_limit`: first version uses `1` for daily login.
- `claim_cycle`: first version supports `DAILY` and `ONCE`.
- `reward_items`: JSON array for multi-currency rewards, for example:

```json
[
  { "currencyCode": "GC", "rewardAmount": "100.000000" },
  { "currencyCode": "SC", "rewardAmount": "1.000000" }
]
```

Existing `currency_code` and `reward_amount` stay for backward compatibility and list display. For multi-currency daily login, the first reward item mirrors into those columns so old pages do not break.

`gl_promotion_claim` additions:

- `claim_date`: backend server date used for daily uniqueness.
- `reward_snapshot`: JSON snapshot of the configured reward items at claim time.

Add a unique key for daily login:

```text
tenant_id + promotion_id + member_id + claim_date
```

Existing one-time reward uniqueness must remain valid for old promotion rewards.

## Backend Design

Add a daily claim path under the existing promotion module instead of creating a new module.

Key service behavior:

1. Find the active `DAILY_LOGIN` reward for the current tenant.
2. Check whether the member has a successful claim for today.
3. If already claimed, return today's reward state with `canClaim=false`.
4. If not claimed, create one claim record with `PENDING`.
5. Credit each configured reward item through the wallet service using source type `DAILY_REWARD`.
6. Store wallet transaction numbers and reward snapshot.
7. Mark the claim `SUCCESS` only when all configured credits succeed.
8. If a credit fails, mark the claim `FAILED` and return the failure state.

For first version, one claim record can store multiple wallet transaction numbers as a comma-separated string in the existing `wallet_transaction_no` field or a JSON snapshot field. The implementation plan should prefer a JSON `wallet_transaction_snapshot` field if the current column is not enough.

## API Design

H5 APIs:

```http
GET /api/client/promotions/daily-login
POST /api/client/promotions/daily-login/claim
```

Response shape:

```json
{
  "promotionId": 1900000000000000901,
  "promotionName": "Daily Login Reward",
  "promotionType": "DAILY_LOGIN",
  "claimDate": "2026-07-11",
  "canClaim": true,
  "claimStatus": "UNCLAIMED",
  "rewardItems": [
    { "currencyCode": "GC", "rewardAmount": "100.000000" },
    { "currencyCode": "SC", "rewardAmount": "1.000000" }
  ],
  "claimNo": null,
  "walletTransactionNos": []
}
```

B-side APIs reuse existing reward configuration and claim list endpoints, with the reward form extended to support promotion type and reward items.

## B-Side Design

Extend `Promotion Center / Promotion Rewards`:

- Add `活动类型` filter and form field.
- Add `奖励配置` editor for one or more currency reward rows.
- Default creation flow can create a daily login reward with:
  - type `DAILY_LOGIN`
  - cycle `DAILY`
  - daily limit `1`
  - reward items `GC 100`, `SC 1`
- Claim records show claim date and reward snapshot.

The B-side must remain operationally readable:

- status uses readable labels.
- source/type values do not leak raw enum text.
- no hardcoded Chinese outside i18n.

## H5 Design

Extend the existing rewards/promotions screen:

- Show a daily login reward block at the top.
- Display configured rewards as compact currency chips.
- Show states:
  - `可领取`
  - `已领取`
  - `领取中`
  - `领取失败`
  - `请先登录`
- On success, refresh wallet and reward state.

The existing generic promotion list can stay below the daily login block.

## Acceptance Checklist

- B-side can configure the daily login reward as `GC 100 + SC 1`.
- H5 shows the configured reward amounts from the backend.
- Logged-in member can claim once today.
- A second claim on the same day does not create another wallet transaction.
- Wallet transactions use source type `DAILY_REWARD`.
- B-side claim record shows member, claim date, status, reward snapshot, and wallet transaction numbers.
- Admin i18n check passes.
- H5 build passes.
- Backend promotion tests pass.
- Runtime smoke confirms wallet balances increase by GC 100 and SC 1 after claim.

## Self-Review

- No placeholder requirements remain.
- Scope is one vertical slice and does not include streak, VIP, KYC, geo, or risk segmentation.
- Data model keeps existing promotion tables and adds only the fields required for configurable daily rewards.
- Wallet mutation remains behind the existing wallet service.
