# Tang Luck V1 正式上线实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 Tang Luck 从演示闭环升级为正式上线 V1，全链路覆盖注册、登录、合规、钱包、活动、购买、KYC、兑换、后台运营、审计和中英双语。

**Architecture:** 后端按正式领域模块和 provider abstraction 组织，前端先建立全局 i18n，再逐步把 C 端与 B 端页面改为生产文案和正式状态机。外部供应商暂用 `manual` provider，但数据库和接口保持正式接入形态。

**Tech Stack:** Spring Boot、JPA、Flyway、MySQL、Vue 3、Pinia、Vue Router、Vitest、Playwright。

---

## Phase 1: i18n 基础设施和用户可见文案治理

- [x] 新增前端 i18n 语言检测测试：默认 `en`，浏览器 `zh-CN` 时返回 `zh-CN`。
- [x] 实现 `frontend/src/i18n/index.ts`，提供 `detectLocale`、`setLocale`、`t`。
- [x] 新增 `frontend/src/i18n/messages.ts`，维护 `en` 和 `zh-CN` 文案。
- [x] 将注册、商店、KYC、兑换、后台 P1 页面接入 i18n，保留其他页面后续逐步治理。
- [x] 移除主要用户链路可见 `demo`、`sandbox`、`P0-A preview` 文案。
- [x] Playwright 增加英文默认和中文浏览器语言验证。

## Phase 2: Auth 与 Compliance 正式化

- [x] 增加登录接口与测试。
- [x] 前端增加登录页和会话状态。
- [x] 注册/登录错误码和文案接入 i18n。
- [ ] 合规文档页面正式化，保留版本和签署记录。

## Phase 3: Wallet 正式账本

- [ ] 扩展钱包账本方向：`credit`、`debit`、`freeze`、`unfreeze`、`settle`、`refund`。
- [ ] 增加冻结、解冻、结算、退款的后端测试。
- [ ] 钱包页面显示可用、冻结、来源、交易历史。

## Phase 4: Payment 正式订单

- [ ] 将当前 P1 purchase 改为正式 payment 模块。
- [ ] 订单状态覆盖 `created`、`payment_pending`、`paid`、`failed`、`cancelled`、`refunded`。
- [ ] 增加 provider abstraction：`manual` provider 只作为本地实现。
- [ ] 增加支付回调幂等测试。
- [ ] 前端商店显示正式购买状态，不显示 sandbox。

## Phase 5: KYC 正式审核

- [ ] 扩展 KYC 表结构：provider、provider reference、失败原因、审核备注。
- [ ] 状态覆盖 `submitted`、`provider_review`、`manual_review`、`approved`、`rejected`、`resubmission_required`。
- [ ] 后台 KYC 页面支持筛选、详情、通过、拒绝、要求补资料。

## Phase 6: Redemption 正式兑换

- [ ] 扩展兑换状态机：`submitted`、`risk_review`、`approved`、`payout_processing`、`paid`、`failed`、`rejected`、`cancelled`。
- [ ] 创建兑换时校验 KYC、地区、SC 可用余额。
- [ ] 拒绝/取消解冻，支付成功结算扣减。
- [ ] 后台兑换页面支持审核、拒绝原因、重试和审计。

## Phase 7: Admin 正式运营后台

- [ ] 增加用户管理、订单管理、KYC 审核、兑换审核、活动管理、审计页面。
- [ ] 所有后台页面包含筛选、状态标签、空态、错误态、操作反馈。
- [ ] 高风险操作写入审计日志。

## Phase 8: 验收与发布

- [ ] 后端全量测试：`.\gradlew.bat --no-daemon test`
- [ ] 前端单测：`npm run test -- --run --pool=threads --maxWorkers=1`
- [ ] 前端构建：`npm run build`
- [ ] Playwright：`npx playwright test`
- [ ] 更新 README。
- [ ] 提交并推送到 GitHub。
