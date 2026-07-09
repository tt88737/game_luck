# Business Module Mapping

| Business Module | Backend Location | Responsibility |
| --- | --- | --- |
| tenant-center | `backend/gameluck-modules/gameluck-system` | Tenant, tenant packages, client access, shared system configuration |
| channel-center | reserved module under `backend/gameluck-modules` | Channel feature switches, traffic source policy, entry configuration |
| member-center | `backend/gameluck-modules/gameluck-member` | Player member profile, status, risk level, register channel |
| wallet-center | `backend/gameluck-modules/gameluck-wallet` | Currency, account, transaction ledger, freeze, release, settlement rules |
| payment-center | `backend/gameluck-modules/gameluck-payment` | Simulated deposit orders and future payment provider integration |
| game-center | `backend/gameluck-modules/gameluck-game` | Game order lifecycle, launch/callback boundary, bet and payout operations |
| promotion-center | `backend/gameluck-modules/gameluck-promotion` | Promotion reward configuration, claim records, member reward flow |
| redemption-center | `backend/gameluck-modules/gameluck-redemption` | Redemption request, freeze, audit approval, reject release, settlement |
| report-center | `backend/gameluck-modules/gameluck-report` | MVP operational overview and cross-module report aggregation |
| audit-center | reserved module under `backend/gameluck-modules` | Sensitive operation review, audit trail, manual review workflow |

## Rules

- Business modules do not update wallet balances directly; they call wallet-center services.
- Tenant and permission boundaries continue to use GameLuck system and security mechanisms.
- Reserved modules must be added only when a concrete workflow requires code, database tables, and API contracts.
- New modules must document menu IDs, permission keys, tables, and cross-module calls before implementation.
