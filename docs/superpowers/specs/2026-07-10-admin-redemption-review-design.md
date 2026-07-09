# Phase 4 B端兑换审核后台闭环设计

## 背景

Phase 3 已完成玩家端活动领取与兑换申请流程。玩家提交兑换后，后台已有基础兑换订单模块：

- `GET /redemption/order/list`
- `GET /redemption/order/{id}`
- `POST /redemption/order`
- `POST /redemption/order/{id}/approve`
- `POST /redemption/order/{id}/reject`
- `admin-ui/src/views/redemption/order/index.vue`

现有实现可以创建待审核订单、冻结钱包、审核通过后结算冻结金额、审核拒绝后释放冻结金额。Phase 4 不重新建设兑换模块，而是把现有 B 端审核能力补成运营可用、可追溯、可验收的闭环。

## 目标

运营人员进入兑换订单后台后，默认能聚焦待审核订单，查看订单、会员、币种、金额、收款备注、冻结/结算/释放钱包流水、失败原因和审核记录，并能对待审核订单执行通过或拒绝。系统必须避免重复审核、错误状态审核、拒绝无原因、按钮重复提交和钱包状态不一致。

## 非目标

- 不接入真实出款服务商。
- 不实现 KYC、风控评分、人工复核多级流转。
- 不新增玩家端兑换功能，玩家端已在 Phase 3 完成。
- 不改造钱包中心的核心记账模型，只通过现有冻结、结算、释放能力闭环。

## 方案选择

### 方案 A：加固现有审核页和服务

在现有 `redemption/order` 后台页、Controller、Service、测试和 i18n 上补齐审核闭环。默认状态筛选为 `PENDING`，增加状态快捷筛选、审核原因校验、提交中状态、详情追溯字段、后端审核约束测试和构建验证。

优点是范围小，沿用当前模块和权限，能最快形成可验收闭环。缺点是暂不覆盖复杂风控和多级审批。

### 方案 B：新建独立审核工作台

保留现有订单页，再新增一个专门的审核工作台页面。该页面只展示待审订单，后续可以承载风控信息、KYC、出款渠道状态。

优点是扩展空间更大。缺点是会与现有订单页产生重复维护，当前阶段增加菜单、权限和页面成本。

### 方案 C：先补后端规则，前端只做轻微调整

只补后端审核原因、重复审核和钱包失败保护，前端保持基本可用。

优点是最省实现量。缺点是运营体验仍然偏弱，不能很好支撑 B 端验收。

## 推荐方案

采用方案 A。当前已有订单页和权限点，Phase 4 的核心价值是把已有流程打磨成可交付后台闭环。独立审核工作台留到引入真实出款、KYC 或多级审批后再做。

## 后端设计

### 审核约束

- `approve(id, reason)` 只允许处理 `PENDING` 订单。
- `reject(id, reason)` 只允许处理 `PENDING` 订单。
- 拒绝必须提供非空 `auditReason`，后端兜底校验，前端同步校验。
- 审核通过可以不填原因，但如果填写必须保存。
- 钱包结算或释放返回非 `SUCCESS` 时，订单不得更新为终态，接口返回业务异常。
- 审核成功后写入：
  - `status`
  - `settleWalletTransactionNo` 或 `releaseWalletTransactionNo`
  - `auditTime`
  - `auditReason`
  - `updateTime`

### 审计与幂等边界

当前实现通过订单状态防止重复审核。Phase 4 保持该边界，不新增审核流水表。后续真实出款阶段再考虑独立 `redemption_audit_log` 或多级审批表。

### 测试覆盖

在 `RedemptionOrderServiceImplTest` 中补齐以下服务级用例：

- 非 `PENDING` 订单拒绝时不调用钱包。
- 非 `PENDING` 订单通过时不调用钱包。
- 拒绝原因为空时返回业务异常且不调用钱包。
- 通过审核成功后写入 `APPROVED`、结算交易号、审核时间和原因。
- 拒绝审核成功后写入 `REJECTED`、释放交易号、审核时间和原因。
- 钱包操作失败时抛业务异常且不更新订单为终态。

## 前端设计

### 列表与筛选

`admin-ui/src/views/redemption/order/index.vue` 继续作为兑换订单运营页。页面默认筛选 `status=PENDING`，让运营进入页面后优先处理待审订单。保留订单号、会员 ID、币种、状态筛选，并新增状态快捷筛选按钮或分段控件：

- 待审核
- 已通过
- 已拒绝
- 失败
- 全部

切换快捷筛选时重置到第一页并刷新列表。

### 表格与操作

表格保持密集运营工具风格，不做营销化卡片。核心列保留：

- 订单号
- 会员 ID
- 币种
- 金额
- 方式
- 状态
- 冻结单号
- 冻结交易
- 结算交易
- 释放交易
- 创建时间
- 操作

审核通过和审核拒绝按钮只在 `PENDING` 状态展示。提交审核时禁用确认按钮，避免重复点击。

### 详情与审核弹窗

详情弹窗展示订单追溯信息：

- 基础信息：订单号、状态、会员 ID、币种、金额、方式、账户备注、备注。
- 钱包信息：冻结单号、冻结交易、结算交易、释放交易。
- 审核信息：审核时间、审核原因、失败原因。

审核弹窗按动作区分：

- 通过：审核原因可选，提示运营可填写备注。
- 拒绝：审核原因必填，空值不允许提交。

### i18n

所有新增文案写入 `admin-ui/src/lang/zh_CN.ts` 和 `admin-ui/src/lang/en_US.ts`，并通过现有 `t(...)` 使用。不得新增硬编码可见中文。

## 权限与菜单

继续使用现有权限点：

- `redemption:order:query`
- `redemption:order:add`
- `redemption:order:approve`
- `redemption:order:reject`

Phase 4 不新增菜单和权限。实施时需要确认本地 SQL seed 已包含兑换菜单与按钮权限。

## 错误处理

- 后端业务异常继续返回现有统一 `R.fail` 风格。
- 前端审核提交失败时保留弹窗，让运营可调整原因或重试。
- 前端列表加载失败沿用全局请求错误提示，不吞掉异常。
- 钱包失败原因继续写入订单 `failReason` 的场景只限创建冻结失败；审核阶段钱包失败不把订单推进到终态。

## 验收标准

- 默认进入兑换订单页展示待审核订单。
- 切换状态快捷筛选可以正确刷新列表。
- 非待审订单不展示通过/拒绝按钮。
- 拒绝审核不填写原因时，前端阻止提交，后端也拒绝空原因。
- 通过审核成功后，订单状态为 `APPROVED`，冻结金额被结算，记录结算交易号。
- 拒绝审核成功后，订单状态为 `REJECTED`，冻结金额释放回可用余额，记录释放交易号。
- 重复审核同一订单时返回业务异常，不再次调用钱包。
- 前端 i18n 检查通过。
- 后端兑换模块定向测试通过。
- Admin UI 构建通过。

## 验证计划

- `C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-modules/gameluck-redemption -am -Plocal -DskipTests=false "-Dtest=RedemptionOrderServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `pnpm --dir admin-ui check:i18n`
- `pnpm --dir admin-ui build:dev`
- `C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-admin -am compile -Plocal -DskipTests`
- 可选本地烟测：登录后台，创建或复用一笔 `PENDING` 兑换订单，分别验证通过与拒绝路径。
