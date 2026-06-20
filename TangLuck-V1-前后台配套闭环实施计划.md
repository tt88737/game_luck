# Tang Luck V1 前后台配套闭环实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 Tang Luck 从“前台链路可跑 + 后台聚合演示”重构为“前台能力与后台配置/审核/审计配套”的正式上线产品。

**Architecture:** 以后台配置和状态机为中心推进。每个阶段都同时交付 DB/API/B 端/C 端/测试，确保 C 端没有孤立功能，后台没有只能展示不能操作的假闭环。

**Tech Stack:** Spring Boot、JPA、Flyway、MySQL/H2、Vue 3、Pinia、Vue Router、Vitest、Playwright。

---

## Phase 0：计划确认与基线保护

- [ ] 运行当前基线验证：`backend\gradlew.bat --no-daemon test`、`frontend npm run test -- --run --pool=threads --maxWorkers=1`、`frontend npm run build`、`frontend npx playwright test`。
- [ ] 确认未跟踪的 `Tang+Luck_+Casino+Slots_1.0.51_APKPure.xapk` 和 `artifacts/` 不进入提交。
- [ ] 给现有 `/admin/p1` 标记为临时聚合页，后续逐步被正式模块替代。

## Phase 1：后台基础壳与权限/审计底座

**目标：** 后台先变成正式运营系统骨架。

- [ ] 新增最小管理员上下文：`operatorId`、`operatorRole`、权限码，先用 header/mock provider，后续可接真实登录。
- [ ] 抽取后台导航组件：Dashboard、Users、Regions、Legal Docs、Lobby、Campaigns、Packages、Orders、KYC、Redemptions、Wallet Ledger、AMOE、Support、Audit Logs。
- [ ] 增加统一审计服务 `AdminAuditService`，后台写操作不再各服务手写 audit。
- [ ] 前端后台页面统一 loading、empty、error、filtered empty、success 状态。
- [ ] 测试：后台导航渲染、无权限响应、审计服务写入。

## Phase 2：合规配置闭环

**目标：** 地区和法务文档不再只靠种子数据。

- [ ] 后端新增 `/admin/regions`：列表、编辑州级开关、记录 `legal_approval_id`、审计。
- [ ] 后端新增 `/admin/legal-documents`：列表、新建版本、发布、归档、审计。
- [ ] C 端 `/compliance/documents` 继续只返回 active 文档。
- [ ] 注册、活动、商店、兑换、AMOE 后端统一调用地区策略校验。
- [ ] 前端新增 Admin Regions 和 Legal Docs 页面。
- [ ] 测试：禁止州注册/购买/兑换拦截；发布文档后 C 端读取新版本；后台编辑写审计。

## Phase 3：活动、任务、Coupon、Lobby 配置闭环

**目标：** C 端 Lobby 和 Activity 完全由后台配置驱动。

- [ ] 后端补 `/admin/campaigns` GET/PATCH，返回真实活动列表，不再前端写死草稿。
- [ ] 发布检查返回 `publish_check.blocking_reasons`，覆盖地区、预算、规则版本、SC 法务审批。
- [ ] 新增 `lobby_cards` 表和 `/admin/lobby-cards` CRUD：标题、类型、排序、状态、跳转、地区、关联活动。
- [ ] 新增 C 端 `/lobby` API，返回钱包摘要、卡片、活动、任务、KYC/兑换状态摘要。
- [ ] AppHome 改为消费 `/lobby`，移除写死 Featured games。
- [ ] 后台 Campaigns 页面支持筛选、编辑、发布、暂停、查看领取记录。
- [ ] 测试：活动缺法务 ID 无法发布；Lobby 卡片后台下线后 C 端不显示；风险用户 SC 降级。

## Phase 4：商品包与订单闭环

**目标：** Store 由后台商品包配置驱动，订单走正式状态机。

- [ ] 扩展 `product_packages`：状态、地区、provider、legal_approval_id、sort_order、starts_at、ends_at，移除用户可见 `sandbox` 表达。
- [ ] 新增 `/admin/product-packages`：CRUD、上下架、审计。
- [ ] 扩展 `purchase_orders`：`created`、`payment_pending`、`paid`、`failed`、`cancelled`、`refunded`，增加 provider reference。
- [ ] 新增 payment provider interface 和 manual provider；购买不再创建即无条件入账，必须经过 paid/fulfillment。
- [ ] 后台 Orders 页面支持筛选、详情、失败原因、退款标记。
- [ ] C 端 Store 显示订单状态和失败提示。
- [ ] 测试：禁购州不能下单；重复 idempotency 不重复入账；paid 后才写 GC ledger。

## Phase 5：KYC 审核闭环

**目标：** KYC 从单个 approve 按钮升级为正式审核台。

- [ ] 扩展 `kyc_applications`：provider、provider_reference、status、reject_reason、review_note、reviewed_by、reviewed_at。
- [ ] 后端新增 `/admin/kyc/applications`：列表、筛选、详情。
- [ ] 后端新增 `/admin/kyc/{userId}/approve|reject|request-resubmission`，全部写审计。
- [ ] C 端 KYC 显示 rejected/resubmission_required 的原因和重新提交入口。
- [ ] 后台 KYC 页面支持待审队列、详情、通过、拒绝、补资料。
- [ ] 测试：拒绝后 C 端显示原因；补资料后状态回到 submitted/manual_review；审核写审计。

## Phase 6：兑换审核与钱包冻结闭环

**目标：** 兑换从“提交后冻结”升级为完整审核、解冻、结算、打款状态机。

- [ ] 扩展 WalletService：`freeze`、`unfreeze`、`settleFrozen`、`refund` 全部有 ledger。
- [ ] 扩展 `redemption_requests`：risk_status、review_reason、payout_provider、provider_reference、reviewed_by、paid_at、failed_reason。
- [ ] 后端新增 `/admin/redemptions` 列表/详情。
- [ ] 后端新增 `/admin/redemptions/{id}/approve|reject|mark-processing|mark-paid|mark-failed|cancel`。
- [ ] 拒绝/取消必须解冻 SC；paid 必须结算扣减冻结 SC。
- [ ] C 端 Redemption 显示历史请求和每个状态下一步。
- [ ] 测试：KYC 未通过不能提交；SC 不足不能提交；拒绝解冻；paid 结算；重复请求幂等。

## Phase 7：用户、钱包、风控、AMOE、客服闭环

**目标：** 补齐正式运营需要的横向模块。

- [ ] Users：用户列表、详情、地区、风险等级、钱包摘要、订单/KYC/兑换关联。
- [ ] Wallet Ledger：全局流水查询，按用户、币种、业务类型、idempotency key 筛选。
- [ ] Risk Events：风险事件列表、关联业务、处理动作。
- [ ] AMOE：C 端申请入口、后台审核、频率限制、发奖入账和审计。
- [ ] Support：C 端工单、后台处理、关联业务单；客服补偿仅允许 GC，且要审批和审计。
- [ ] 测试：AMOE 审核发奖写 ledger；客服补偿写 audit；风险用户领奖降级。

## Phase 8：C 端产品化收口

**目标：** C 端不再像演示页，所有状态都有真实来源。

- [ ] AppHome/Lobby 从 `/lobby` 渲染，展示后台配置卡片、活动、任务、KYC、兑换限制。
- [ ] Activity 页面展示活动详情、规则版本、领取状态、预算/地区限制提示。
- [ ] Wallet 页面展示可用、冻结、来源、历史筛选。
- [ ] Store 页面展示商品上下架、订单状态、失败/取消/退款。
- [ ] KYC/Redemption/AMOE/Support 页面补齐空态、失败态、处理中、成功态。
- [ ] 所有新增文案接入 i18n，默认英文，浏览器 `zh-*` 自动中文。

## Phase 9：验收、文档、推送

- [ ] 后端全量测试：`cd backend; .\gradlew.bat --no-daemon test`
- [ ] 前端单测：`cd frontend; npm run test -- --run --pool=threads --maxWorkers=1`
- [ ] 前端构建：`cd frontend; npm run build`
- [ ] Playwright：`cd frontend; npx playwright test`
- [ ] 浏览器截图验收：桌面 1366x900、移动 390x844，覆盖 C 端和 Admin 核心页。
- [ ] 更新 README：本地启动、测试账号、闭环验收路径。
- [ ] 提交并推送，不提交 APK、`artifacts/`、截图产物。

## 第一批建议执行范围

为了避免再次出现“前台先跑、后台没配套”，第一批开发只做下面四个闭环：

1. Phase 1 后台基础壳与审计底座。
2. Phase 2 地区和法务文档配置。
3. Phase 3 活动 + Lobby 配置化。
4. Phase 4 商品包 + 订单管理。

这四个完成后，C 端 Lobby、Activity、Store 才算从“页面展示”变为“后台可运营”。
