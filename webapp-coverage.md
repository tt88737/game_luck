# Webapp Coverage

## Scope

- Base URL: `http://127.0.0.1:5175`
- Backend: `http://localhost:8092`
- Environment: local development
- Role: user and ops admin through local UI
- Mutation allowed: yes, local development records only
- Devices: desktop 1440x900, mobile 390x844
- Languages: English default, Chinese via browser locale

## Routes

| Route | Desktop | Mobile | Language | Status |
| --- | --- | --- | --- | --- |
| `/app/register` | covered | covered | en / zh-CN | covered |
| `/app` | covered | covered | en / zh-CN partial | covered |
| `/app/store` | covered | covered | en / zh-CN | covered |
| `/app/kyc` | covered | covered | en / zh-CN | covered |
| `/app/redemption` | covered | covered | en / zh-CN | covered |
| `/app/wallet` | covered | covered | en / zh-CN partial | covered |
| `/admin` | covered | covered | en / zh-CN partial | covered |
| `/admin/campaigns` | covered | covered | en / zh-CN partial | covered |
| `/admin/p1` | covered | covered | en / zh-CN | covered |

## Flow Coverage

| Flow | Status | Evidence |
| --- | --- | --- |
| Register CA user | covered | `artifacts/browser-validation/flow-register-filled.png`, `flow-home-after-register.png` |
| Store purchase | covered | `flow-store-before-buy.png`, `flow-store-after-buy.png` |
| KYC submit | covered | `flow-kyc-before-submit.png`, `flow-kyc-after-submit.png` |
| Admin KYC approval | covered | `flow-admin-p1-before-approve.png`, `flow-admin-p1-after-approve.png` |
| Redemption request | covered | `flow-redemption-ready.png`, `flow-redemption-after-submit.png` |
| Wallet ledger after flow | covered | `flow-wallet-after-flow.png` |

## Notes

- Browser run captured desktop, mobile, and zh-CN locale pages.
- No failed network responses were recorded in the final browser validation run.
- No user-visible `demo`, `sandbox`, or `P0-A preview` text was detected in the final browser validation run.
