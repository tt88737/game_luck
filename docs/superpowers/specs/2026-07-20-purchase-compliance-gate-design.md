# Purchase Compliance Gate Design

## Goal

Integrate C-side purchase payment with the existing unified member compliance gate so new purchase orders are not created for missing, inactive, or high-risk members.

This phase preserves purchase idempotency, does not require KYC for purchasing, and does not reuse redemption-specific region rules.

## Scope

This phase includes:

- Load the current member before creating a new purchase order.
- Evaluate `MemberComplianceAction.PURCHASE_PAY` through `IMemberComplianceGateService`.
- Block missing members, non-`ACTIVE` members, and members whose risk level is `HIGH`.
- Preserve the current successful simulated purchase and wallet credit flow after the gate allows the request.
- Return existing orders for repeated idempotent requests without re-evaluating current member risk.
- Add focused service tests and a local runtime smoke covering denied and allowed purchases.

This phase excludes:

- KYC requirements for purchase.
- Age or agreement requirements for purchase.
- Purchase-specific country, state, channel, or currency policy configuration.
- Reuse of redemption eligibility policies for purchase.
- Real payment provider integration.
- H5 layout or workflow changes beyond displaying the backend business error through the existing error path.

## Current State

`MemberComplianceAction.PURCHASE_PAY` already exists. The shared gate currently applies common member existence, account status, and high-risk checks to every action. KYC is limited to redemption, while age, agreements, and region policies apply only to their configured actions.

`ClientPurchaseService.pay(...)` currently:

1. Resolves the authenticated member id and tenant.
2. Returns an existing order for a matching idempotency key.
3. Loads the purchase offer.
4. Enforces purchase limits.
5. Creates a pending order.
6. Applies a simulated successful payment event and credits the configured grants.

The missing step is shared compliance evaluation before a new order is created.

## Backend Design

### Integration Point

Extend `ClientPurchaseService` with:

- `MemberProfileMapper`
- `IMemberComplianceGateService`

The compliance check runs after the idempotency lookup and available-offer lookup, but before purchase-limit enforcement and before any order, payment event, or wallet mutation.

Loading the offer first provides the actual payment currency for the compliance context. The offer query is read-only and creates no business record.

### Compliance Context

Build the context with:

```text
action       = PURCHASE_PAY
tenantId     = current tenant id
member       = member profile loaded by authenticated member id
currencyCode = offer.payCurrencyCode
channel      = h5
```

The channel remains the current C-side H5 default. This phase does not introduce client-controlled channel input.

### Decision Handling

When the decision is denied, throw `ServiceException` using the decision's localized `messageKey`.

The existing shared gate determines the first failure in this order:

1. Member does not exist.
2. Member status is not `ACTIVE`.
3. Member risk level is `HIGH`.

For `PURCHASE_PAY`, the gate does not require KYC, age confirmation, agreement acceptance, or region eligibility.

The shared gate must select action-appropriate message keys. `PURCHASE_PAY` decisions use purchase-specific keys instead of the current redemption wording:

- `client.purchase.member.not.exists`
- `client.purchase.member.inactive`
- `client.purchase.risk.blocked`

The reason enum and decision order remain shared. Other actions keep their current messages.

### Idempotency Semantics

The existing idempotency lookup remains before compliance evaluation.

If the same tenant, member, offer, and idempotency key already produced an order, a retry returns that order even if the member's current risk or status has changed. This preserves the meaning of idempotency: retries observe the original operation result rather than creating a new compliance decision for an already-created order.

Idempotency conflicts continue to use the existing conflict error.

### Transaction And Side Effects

A denied compliance decision must occur before:

- purchase order insertion;
- payment event insertion;
- purchase grant snapshot insertion;
- wallet credit;
- turnover task creation.

The existing `@Transactional(rollbackFor = Exception.class)` boundary remains unchanged.

## Error Handling

Add localized purchase-specific messages for missing member, inactive account, and high-risk blocking. This prevents a purchase request from displaying redemption-specific wording. H5 continues to display the backend business message through its existing request error handling.

## Testing

### Unit Tests

Extend `ClientPurchaseServiceTest` to cover:

- a missing member decision blocks purchase before order creation;
- an inactive member decision blocks purchase before order creation;
- a high-risk decision blocks purchase before order creation;
- an allowed decision continues through simulated payment and wallet credit;
- an existing idempotent order is returned without invoking the compliance gate;
- the gate receives `PURCHASE_PAY`, the loaded member, tenant id, offer payment currency, and `h5` channel.

Tests must verify that denied decisions do not insert an order or invoke the payment event service.

Keep the focused member compliance gate tests to confirm `PURCHASE_PAY` does not require KYC, age, agreements, or region policy.
They must also assert that denied `PURCHASE_PAY` decisions return the purchase-specific message keys while redemption decisions retain their existing keys.

### Runtime Smoke

Use a dedicated local member and purchase offer:

1. Set the member to `ACTIVE` with normal risk and submit a new purchase; verify the order reaches `CREDITED`.
2. Set the member risk level to `HIGH` and submit with a new idempotency key; verify the API returns the risk error.
3. Verify no order, payment event, grant snapshot, wallet transaction, or turnover task was created for the denied request.
4. Repeat the first successful idempotency key after the member becomes high risk; verify the original credited order is returned.

## Acceptance Criteria

- Every new C-side purchase payment request passes through `IMemberComplianceGateService`.
- Missing, inactive, and high-risk members cannot create new purchase orders.
- Purchase does not require KYC, age confirmation, agreements, or redemption region eligibility.
- Existing idempotent orders are returned without re-evaluating the gate.
- Denied requests create no payment or wallet side effects.
- Existing successful simulated purchases continue to reach `CREDITED`.
- Focused backend tests, backend package, runtime smoke, and `git diff --check` pass.
