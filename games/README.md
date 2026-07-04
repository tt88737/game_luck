# Games

This directory is reserved for Cocos Creator games and activity mini-games.

Rules:

- Games do not modify wallet balance directly.
- Games receive launch session context from `game-center`.
- Bets and payouts go through provider callback APIs.
- Every callback must include `tenantId`, `memberId`, `roundNo`, `currencyCode`, `amount`, and `idempotencyKey`.

