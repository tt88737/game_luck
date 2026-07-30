# 包网平台底座规划

## 目标

为包网平台建立可长期维护的技术底座设计，覆盖 B 端后台、C �?H5、玩�?App、自研游戏接入、多币种钱包、租户配置、渠道开关和 AI 开发边界�?

## 当前技术路�?

- B 端后台：GameLuck Backend Base
- C �?H5 / 官网 / 活动�?/ PWA：Vue3 + Vite
- 玩家 App：Flutter
- 自研游戏 / 活动小游戏：预留 Cocos Creator 接入
- 核心后端：Spring Boot / Java
- 钱包体系：多币种钱包中心
- 数据库：MySQL
- 缓存：Redis

## 阶段计划

| 阶段 | 状�?| 目标 | 交付�?|
| --- | --- | --- | --- |
| 1. 技术路线确�?| complete | 明确底座组合和各端边�?| 本文件、findings.md |
| 2. 架构设计文档 | complete | 固化模块边界、钱包模型、渠道开关、AI 规则 | docs/superpowers/specs/2026-06-25-platform-architecture-design.md |
| 3. 设计自查 | complete | 检查范围、矛盾、遗漏和模糊�?| 已确认无 TODO/TBD 占位符，文档编码正常 |
| 4. 用户评审 | complete | 等待业务方确认设计方�?| 用户已确认继续生成第一阶段实施计划 |
| 5. 实施计划 | complete | 第一阶段 MVP 开发任务已完成、验证并推�?| docs/superpowers/plans/2026-06-25-phase-1-mvp.md |
| 6. 钱包中心细化设计 | complete | 固化钱包状态机、幂等、冻�?结算/冲正规则 | docs/superpowers/specs/2026-06-25-wallet-center-design.md |
| 7. 引入 GameLuck Backend Base | complete | 将上游底座导�?backend 并记录来�?| backend/、docs/upstream/gameluck-vue-plus.md |
| 8. 后端环境基线检�?| complete | 检�?JDK、Maven、Docker、MySQL、Redis �?GameLuck 配置 | docs/implementation/backend-environment-baseline.md |
| 9. 本地启动配置 | complete | 新增 local profile 配置和本地启动说�?| application-local.yml、docs/implementation/backend-local-startup.md |
| 10. 后端构建验证 | complete | 安装 Maven、初始化数据库、构�?backend | Maven 3.9.16、gameluck_vue、BUILD SUCCESS |
| 11. 后端启动验证 | complete | 启动 gameluck-admin 并验�?8080 | java -jar 启动成功，GET / 返回 200 |
| 12. Admin UI 全站多语言 | complete | 统一菜单、首页、公共组件和业务页面文案的中英文切换 | 已完成前后端全站多语言、后�?i18n key 守门、前�?build �?i18n 检�?|
| 13. Phase 2 玩家�?API �?H5 接入 | complete | 建立 C �?bootstrap、demo 登录、钱包、游戏大�?API 并接�?H5 | docs/superpowers/specs/2026-07-09-player-client-api-h5-design.md、docs/superpowers/plans/2026-07-09-player-client-api-h5.md |
| 14. Phase 3 玩家端奖励与兑换 H5 流程 | complete | 建立 C 端活动奖励领取、兑换申�?API 并接�?H5 | docs/superpowers/specs/2026-07-10-client-promotion-redemption-h5-design.md、docs/superpowers/plans/2026-07-10-client-promotion-redemption-h5.md |
| 15. Phase 4 B端兑换审核后台闭�?| complete | 加固后台兑换审核列表、详情、通过/拒绝、审核原因和钱包闭环验收 | docs/superpowers/specs/2026-07-10-admin-redemption-review-design.md、docs/superpowers/plans/2026-07-10-admin-redemption-review.md |
| 16. Phase 5 B端每日趋势看�?| complete | 新增 Report Center / Trends，支持最�?7/30 天全业务经营趋势查询 | docs/superpowers/specs/2026-07-10-report-daily-trends-design.md、docs/superpowers/plans/2026-07-10-report-daily-trends.md |

## 关键决策

| 决策 | 结论 | 原因 |
| --- | --- | --- |
| 后台底座 | GameLuck Backend Base | 权限、菜单、租户、日志、后台基础能力成熟 |
| H5 技�?| Vue3 + Vite | 当前团队更容易掌握，适合活动页、官网、PWA、支付页 |
| App 技�?| Flutter | 长期 App 体验、工程边界、动画和复杂交互更稳 |
| Web 是否�?Flutter | 不作为主 Web/H5 技�?| SEO、活动页、支�?KYC/追踪脚本、H5 游戏嵌入不如 Vue3 灵活 |
| 游戏引擎 | 预留 Cocos | 仅用于自研游戏和活动小游戏，不承担普通页�?|
| 钱包设计 | 多币种配置化 | 不写�?GC/SC/RC，支持真金和后续币种扩展 |

## 风险与待确认

| 风险 | 影响 | 当前处理 |
| --- | --- | --- |
| 真金 / 兑换 / Sweepstakes 合规 | 影响上架、支付、地区准�?| 设计中加入地区、KYC、渠道开关和审计 |
| 包网定制复杂度高 | 容易导致代码分叉 | 使用租户、品牌、渠道、币种配置中心控�?|
| AI 代码边界失控 | 后期难维�?| 在设计文档中加入 AI 开发规�?|
| 多端重复逻辑 | App �?H5 行为不一�?| 统一 API、统一配置、统一钱包中心 |

## 错误记录

| 时间 | 错误 | 处理 |
| --- | --- | --- |
| 2026-06-25 | 当前目录不是 git 仓库，无法提交设计文�?| 继续创建文件，不擅自初始�?git |
## Current Recovery Pointer

| Phase | Status | Goal | Artifact |
| --- | --- | --- | --- |
| 17. Wallet policy / turnover / exchange foundation | complete | Consolidate wallet credit policy snapshots, fund property templates, currency visibility policies, turnover tasks, exchange rule skeleton, and deprecate misleading wallet-rule operator surface | docs/superpowers/plans/2026-07-13-wallet-policy-turnover-exchange-foundation.md |
| 18. Client purchase fulfillment runtime smoke | complete | Verified C-side purchase offer listing, simulated paid purchase, wallet credit, release lock, valid turnover release, and turnover task completion in local runtime | docs/superpowers/plans/2026-07-16-client-purchase-fulfillment.md |
| 19. Purchase limit enforcement foundation | complete | Enforced first purchase, total once, and daily once purchase limits before order creation and wallet credit | docs/superpowers/plans/2026-07-17-purchase-limit-enforcement.md |
| 20. Member ID / System ID landing | complete | Generated public GL member IDs for new members, normalized old visible member numbers, and kept internal System ID relationships unchanged | docs/superpowers/plans/2026-07-11-member-id-system-id.md |
| 21. Wallet exchange runtime closed loop | complete | Added C-side wallet exchange order execution using exchange rules, wallet debit/credit, fee calculation, daily limits, exchange order records, and runtime smoke verification | docs/superpowers/plans/2026-07-17-wallet-exchange-runtime.md |
| 22. H5 wallet exchange experience | complete | Added C-side H5 wallet exchange option display, amount entry, submit flow, balance refresh, and UI verification | docs/superpowers/plans/2026-07-17-h5-wallet-exchange.md |
| 23. Admin wallet exchange order visibility | complete | Add B-side wallet exchange order query page, transaction links, backend list endpoint, and verification | docs/superpowers/plans/2026-07-17-admin-wallet-exchange-order.md |
| 24. Admin wallet exchange menu/runtime wiring | complete | Imported local B-side exchange order menu rows, aligned wallet menu order, rebuilt and restarted backend jar, and verified the new route is active | Local DB + backend runtime verification |
| 25. Admin wallet exchange UI runtime smoke | complete | Verified local Admin UI login, wallet exchange order menu visibility, list loading, order data display, and wallet transaction link navigation | Playwright runtime verification on http://localhost:5173 |
| 26. Client redemption compliance gate | complete | Block C-side redemption requests for missing member, inactive/high-risk accounts, missing age/agreement confirmations, and denied regions before order creation | docs/superpowers/plans/2026-07-17-client-redemption-compliance-gate.md |
| 27. H5 redemption compliance gate runtime smoke | complete | Verified H5 redemption page surfaces the backend denied-region block and creates no redemption order | Playwright runtime verification on http://127.0.0.1:5174/redemptions |
| 28. Admin member compliance visibility runtime smoke | complete | Verified B-side member profile list and detail expose the member compliance fields used by the redemption gate | Playwright runtime verification on http://localhost:5173/member/profile |
| 29. Redemption eligibility policy configuration | complete | Move redemption denied-region logic from hardcoded Java checks into operator-managed backend/Admin policy configuration | docs/superpowers/plans/2026-07-18-redemption-eligibility-policy.md |
| 30. Redemption eligibility policy CRUD runtime smoke | complete | Verified authenticated Admin add/detail/edit APIs can maintain disabled redemption eligibility policies without changing live gates | Playwright/API runtime verification on http://localhost:5173 |
| 31. Redemption eligibility policy Admin form runtime smoke | complete | Verified operators can create, filter, open, and edit eligibility policies through the actual Admin UI form | Playwright UI runtime verification on http://localhost:5173 |
| 32. Redemption eligibility policy operation log visibility | complete | Verified policy add/edit actions are traceable in Admin operation logs with operator, URL, method, request params, and success result | Playwright UI runtime verification on http://localhost:5173/system/log/operlog |
| 33. Redemption eligibility policy configuration integrity | complete | Verified no production hardcoded denied-region logic remains, SQL seed is idempotent, temporary smoke policies are disabled, and runtime US/WA denial still holds | Static scan, MySQL verification, Maven/frontend checks |
| 34. Full build refresh / deliverable build verification | complete | Rebuilt H5 and backend deliverables, resolved backend jar lock by stopping the prior Java runtime, restarted backend from the refreshed jar, and verified local service reachability | H5 build, Maven package, runtime health checks, endpoint auth check, git diff whitespace check |
| 35. Delivery status and change inventory | complete | Consolidated current delivery status, recommended next implementation phases, grouped dirty worktree changes, and separated code/docs/evidence/runtime artifacts | docs/implementation/2026-07-18-delivery-status-and-next-plan.md, docs/implementation/2026-07-18-change-inventory.md |
| 36. Member KYC manual status foundation | complete | Add persistent manual KYC status for members, expose it to C-side APIs, let Admin maintain it, and require approval before redemption order creation | docs/superpowers/specs/2026-07-18-member-kyc-manual-status-design.md |
| 37. Unified Compliance/Risk Gate | complete | Consolidate member status, risk, KYC, consent, region, channel, and currency decisions behind one backend gate, with redemption as the first integrated caller | docs/superpowers/specs/2026-07-18-unified-compliance-risk-gate-design.md |
| 38. Purchase payment realization foundation | complete | Prepare simulated C-side purchase orders for real payment callback integration with provider/session fields and idempotent payment events | docs/superpowers/specs/2026-07-18-purchase-payment-realization-foundation-design.md |
| 39. Admin purchase order operations | complete | Add B-side purchase order visibility, payment event audit trail, and controlled manual payment outcome marking without wallet reversal | docs/superpowers/specs/2026-07-18-admin-purchase-order-operations-design.md |

| 40. Purchase compliance gate | complete | Route new C-side purchase orders through the unified member compliance gate without requiring KYC or reusing redemption region rules | docs/superpowers/specs/2026-07-20-purchase-compliance-gate-design.md |
| 41. Purchase refund and chargeback recovery | completed | Recover all purchase grant assets atomically, create review cases for shortfalls, cancel pending turnover tasks, and raise chargeback risk | docs/superpowers/specs/2026-07-20-purchase-refund-chargeback-recovery-design.md |
| 42. Purchase reversal review resolution | completed   | Add a dedicated Admin review workbench for retrying full multi-currency recovery or accepting per-currency loss with complete audit records | docs/superpowers/specs/2026-07-24-purchase-reversal-review-resolution-design.md |
| 43. Payment provider adapter and simulated checkout | completed | Add a provider-neutral payment boundary, hosted simulated checkout, signed webhooks, and operational event visibility | docs/superpowers/specs/2026-07-25-payment-provider-adapter-simulated-checkout-design.md |
| 44. Payment reconciliation and discrepancy review | completed | Import Provider CSV statements, reconcile event identity/amount/currency/status, and resolve discrepancies without mutating payment or wallet state | docs/superpowers/specs/2026-07-28-payment-reconciliation-design.md |

## Phase 45 Recovery Pointer

| Phase | Status | Goal | Artifact |
| --- | --- | --- | --- |
| 45. Payment settlement batch and financial summary | completed | Aggregate immutable payment events into deterministic financial summaries and close batches only with current reconciliation evidence | docs/superpowers/plans/2026-07-29-payment-settlement-batch-financial-summary.md |
