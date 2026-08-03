# 包网平台底座调研记录

## 2026-07-30 Phase 46 Planning Findings

- Phase 45 already provides `PaymentSettlementBatch`, `PaymentSettlementBatchMapper`, string-safe settlement VOs, a `/payment/settlement` controller, and an Admin settlement detail workbench that Phase 46 can link to without duplicating command behavior.
- Phase 46 should add a dedicated read-only report query service and mapper contract over `gl_payment_settlement_batch`; it must not add report behavior to `PaymentSettlementServiceImpl`, which owns create/query/calculate/close workflows.
- Existing Admin patterns are under `admin-ui/src/api/payment/paymentSettlement`, `admin-ui/src/views/payment/payment-settlement`, and `admin-ui/scripts/check-payment-settlement-contract.mjs`.
- Phase 45 menu metadata occupies page ID `2033` and permissions `20331` through `20334` in `backend/script/sql/gameluck_wallet.sql`; the Phase 46 plan must allocate a distinct sibling page and three distinct permissions.
- Phase 45 runtime evidence is automated by `admin-ui/scripts/phase45-payment-settlement-runtime.mjs`; Phase 46 should use a separate script and preserve the read-only source snapshot proof.
## 已确认事实

- 用户希望一个人启动项目，但担心框架选择和 AI 代码规范导致后期难维护。
- 业务方向参考 tangluck.com，属于包网平台，面向 Social Casino / Sweepstakes Casino / 真金扩展场景。
- 平台需要支持定制化，不同租户、品牌、渠道、地区可能有不同钱包、支付、兑换、活动和游戏配置。
- 钱包不能只按双币设计，需要支持 GC、SC、RC 以及后续扩展币种。
- 用户最终认可的技术路线是：
  - GameLuck Backend Base
  - Vue3 H5
  - Flutter App
  - 预留 Cocos 游戏接入

## 技术判断

### GameLuck Backend Base

适合作为 B 端后台和运营中台底座，承载租户、权限、菜单、字典、日志、报表、审核、配置等能力。

不建议直接用于玩家 C 端页面。

### Vue3 + Vite

适合官网、H5、PWA、活动页、下载页、支付页、规则页、帮助中心。

相比 Flutter Web，更适合 SEO、第三方脚本、KYC/支付 SDK、H5 游戏嵌入、品牌快速定制。

### Flutter

适合长期玩家 App，包括游戏大厅、钱包、活动、VIP、任务、个人中心、消息、KYC。

Flutter Web 可用，但不建议作为主要 H5/官网/PWA 技术。

### Cocos Creator

适合自研游戏、小游戏活动、转盘、刮刮卡、互动玩法。

不建议承担登录、钱包、支付、兑换、普通业务页面。

## 架构原则

- B 端、H5、App、游戏引擎按职责拆分，不追求一个技术栈覆盖所有端。
- 多端共用后端 API、认证体系、配置中心、钱包中心。
- 钱包中心独立，不允许业务模块直接修改余额。
- 租户、品牌、渠道、地区、币种、功能开关必须配置化。
- AI 只能在明确模块内开发，不允许随意修改框架核心。

## 待用户后续确认

- 第一阶段是否需要先做真钱 RC，还是先保留 RC 配置但不上线。
- 是否需要代理/渠道分销模块。
- 目标市场国家或地区。
- 是否需要 App Store / Google Play 上架，还是先 H5/PWA 和 APK 分发。
- 是否已有第三方游戏供应商、支付、KYC、出款服务商。
## 2026-07-24 Phase 42 Chargeback Review Discovery

- Phase 41 intentionally made `CHARGEBACK_REVIEW` and `REFUND_REVIEW` terminal automated outcomes and explicitly excluded operator collection/write-off actions.
- The existing `gl_purchase_reversal` model stores `REVIEW_REQUIRED` and `review_reason`, but has no disposition status, reviewer, review note, resolution time, retry count, or loss/write-off audit fields.
- Admin currently exposes recovery details only inside purchase-order detail. There is no review queue, review mutation API, or dedicated review permission; all existing manual purchase actions share `payment:purchaseOrder:manual`.
- Phase 42 should add a dedicated recovery-review workbench and a deterministic resolution state machine without changing the original payment-event idempotency boundary.

## 2026-07-28 Phase 44 Runtime Discovery

- `PaymentReconciliationPlatformDataSource` originally hardcoded `duplicatePriorStatementEvidence=false`, so the matcher-supported `DUPLICATE_PROVIDER_RECORD` outcome was unreachable in production runtime.
- The production fix queries provider record IDs from other tenant-matched `COMPLETED` batches, excludes the current batch, and batches the lookup once per execution chunk rather than adding per-line queries.
- Runtime reconciliation remains read-only toward payment, reversal, member-risk, turnover, and wallet state; only reconciliation batches, lines, issues, and append-only action logs changed during acceptance.
# 2026-08-03 Phase 48 Discovery

- Phase 47 is complete on `feat/payment-settlement-instruction-review` and remains ten commits ahead of `main`; no Phase 48 design or implementation plan exists.
- Phase 45-47 already provide immutable settlement batches, grouped reports/CSV, and one operational instruction per closed batch with payable review and externally evidenced terminal outcomes.
- The clearest next local-only operations gap is instruction aging and exception visibility: operators can process individual instructions, but there is no due-date policy, aging bucket, overdue queue, exception summary, or per-currency exposure view.
- Phase 48 should continue to exclude real bank/provider execution, treasury movement, wallet mutation, accounting-ledger posting, and storage of full bank details.
- Alternative bounded directions remain evidence reconciliation for externally completed outcomes, or finance-oriented instruction export; scope requires user confirmation before design.
- User selected instruction aging and exception monitoring. SLA uses tenant-configurable natural-day thresholds: payable timing starts at approval, receivable timing starts at instruction creation, with system defaults when no tenant override exists.
- Monitoring covers both terminal-deadline exposure (`APPROVED` payable and `OPEN` receivable) and workflow stalls (`DRAFT`, `REJECTED`, and `PENDING_REVIEW`).
- Phase 48 will not add claim, acknowledge, ignore, or snooze state; source instruction progress resolves the derived exception automatically.
- Selected architecture persists only SLA policy configuration and derives `NORMAL`, `DUE_SOON`, `OVERDUE`, and `STALLED` results at query time. Scheduled snapshots and write-back to Phase 47 instructions were rejected to avoid synchronization and state-machine coupling.
- Approved UTC natural-day semantics: an event on day D with an N-day SLA is due through `23:59:59 UTC` on D+N and becomes overdue on the next UTC day.
- Approved APIs are a list endpoint returning full-filter summaries plus paged rows, and versioned policy read/update endpoints. Monitoring permissions remain separate from Phase 47 command permissions.
- Amount summaries remain per currency, IDs and money remain strings, and the UI must not imply owner assignment that Phase 47 does not model.
