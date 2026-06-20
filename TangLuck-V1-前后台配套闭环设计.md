# Tang Luck V1 前后台配套闭环设计

## 1. 设计目标

当前系统已经能跑通注册、登录、钱包、活动、商店、KYC、兑换和基础后台，但结构上仍偏“前台先做页面、后台补一个聚合演示页”。正式上线产品不能这样做。

V1 后续开发必须改为“配置驱动 + 审核闭环 + 审计兜底”：

- C 端看到的活动、商品包、任务、大厅推荐、兑换资格、AMOE 入口，都必须来自后台配置或后台审核状态。
- B 端必须能配置、发布、暂停、审核、查询和追溯这些能力。
- 后端必须用状态机、幂等、钱包账本、地区合规和审计日志兜底，不能只靠前端隐藏按钮。
- 每条用户主链路都要能在后台找到对应记录和操作入口。

## 2. 当前问题判断

### 2.1 主要不配套点

| C 端能力 | 当前状态 | 缺失的后台配套 |
| --- | --- | --- |
| Lobby Featured games | 前端写死 `Lucky Slots` 等卡片 | 大厅配置、排序、上下线、地区可见、跳转目标 |
| 活动与 Daily Bonus | 有活动领取 API | 活动列表查询、编辑、预算、发布检查、规则版本、法务审批、领取记录 |
| Store GC 商品包 | C 端可买，后端直接入账 | 商品包配置、价格、地区、provider、订单状态、退款/失败、审计 |
| KYC | C 端提交，后台只能 approve | KYC 队列、详情、reject、resubmission、provider reference、审核原因、审计 |
| Redemption | C 端提交并冻结 | 后台 approve/reject/payout/retry/cancel、解冻/结算、原因、审计 |
| 钱包 | C 端可看 ledger | 后台钱包流水查询、用户钱包摘要、补偿审批 |
| 地区合规 | 后端有种子配置 | 后台地区开关、法务 ID、功能维度开关、审计 |
| 法务文档 | 注册可读取 | 后台文档版本、发布、归档、重签策略 |
| AMOE | 只有规则入口 | AMOE 申请、审核、发奖、频率限制 |
| 客服 | 未形成模块 | 工单、业务关联、补偿审批、客服话术约束 |

### 2.2 根因

原计划按技术阶段推进：先 Auth，再 Wallet，再 Payment/KYC/Redemption，再 Admin。这样会导致 C 端页面先出现能力，但后台只能事后拼接。正式产品应反过来：先定义后台配置与状态机，再开放 C 端入口。

## 3. 目标信息架构

### 3.1 C 端

| 页面 | 数据来源 | 后台配套 |
| --- | --- | --- |
| Register/Login | auth + compliance documents | 法务文档、地区合规 |
| Lobby | lobby config + wallet + campaigns + tasks | 大厅配置、活动配置、任务配置 |
| Wallet | wallet summary + ledger | 用户钱包、钱包流水、补偿审批 |
| Activity | campaigns + task configs | 活动配置、预算、领取记录 |
| Store | product packages + purchase orders | 商品包配置、订单管理、支付事件 |
| KYC | kyc status + application | KYC 审核台、provider 配置 |
| Redemption | wallet + kyc + redemption status | 兑换审核、打款处理、风控 |
| AMOE | legal document + request status | AMOE 审核、发奖 |
| Support | tickets + FAQ | 客服工单、补偿审批 |

### 3.2 B 端

后台拆成正式运营模块，不再以 `/admin/p1` 聚合页作为长期入口：

| 后台模块 | 核心能力 |
| --- | --- |
| Dashboard | 注册、活动领取、SC 发放、订单、KYC、兑换、风控摘要 |
| Users | 用户列表、地区、风险等级、钱包摘要、KYC/兑换/订单关联 |
| Regions | 州级注册、游戏、购买、SC 发放、兑换、AMOE 开关 |
| Legal Docs | Terms、Privacy、Rules、AMOE 文档版本发布 |
| Lobby Config | C 端大厅卡片、排序、跳转、地区可见、状态 |
| Campaigns | 活动 CRUD、预算、SC 策略、发布阻断、领取记录 |
| Product Packages | GC 包配置、价格、地区、provider、上下架 |
| Orders | 订单查询、状态流转、provider reference、退款/失败 |
| KYC Review | 审核队列、详情、通过、拒绝、补资料、provider 信息 |
| Redemption Review | 冻结记录、SC 来源、审核、拒绝、打款、失败重试 |
| Wallet Ledger | 全局流水查询、业务来源、幂等 key |
| AMOE Review | 申请、频率、审核、发奖 |
| Support Tickets | 工单、业务关联、补偿审批 |
| Audit Logs | 所有后台写操作前后值 |

## 4. 后端边界

### 4.1 模块拆分

| 模块 | 职责 |
| --- | --- |
| `auth` | 用户注册、登录、会话、用户上下文 |
| `compliance` | 地区策略、法务文档、功能开关 |
| `wallet` | 余额、冻结、解冻、结算、退款、账本 |
| `promotion` | 活动配置、任务、Coupon、领取、发奖 |
| `lobby` | 大厅卡片配置和公开展示 API |
| `commerce` | 商品包、订单、支付 provider、支付事件 |
| `kyc` | KYC 申请、provider 状态、人工审核 |
| `redemption` | 兑换申请、冻结、审核、打款状态 |
| `amoe` | AMOE 申请、审核、发奖 |
| `support` | 客服工单、业务关联、补偿申请 |
| `admin` | 后台聚合 API、审计、权限 |

### 4.2 状态机最低要求

| 对象 | 状态 |
| --- | --- |
| Campaign | `draft`、`active`、`paused`、`ended`、`rejected` |
| Product Package | `draft`、`active`、`paused`、`archived` |
| Purchase Order | `created`、`payment_pending`、`paid`、`failed`、`cancelled`、`refunded` |
| KYC | `not_started`、`submitted`、`provider_review`、`manual_review`、`approved`、`rejected`、`resubmission_required` |
| Redemption | `submitted`、`risk_review`、`approved`、`payout_processing`、`paid`、`failed`、`rejected`、`cancelled` |
| AMOE | `submitted`、`reviewing`、`approved`、`rejected`、`granted`、`closed` |
| Support Ticket | `open`、`pending_user`、`pending_internal`、`resolved`、`closed`、`escalated` |

## 5. 开发原则

1. 每开放一个 C 端入口，必须同时提供后台配置或审核入口。
2. 每个后台写操作必须写 `audit_logs`。
3. 钱包余额变化只能通过 `WalletService`，必须写 `wallet_ledger`。
4. 发奖、支付回调、兑换冻结、补偿必须幂等。
5. 地区、KYC、风险、法务审批必须后端强校验。
6. 所有新增页面可见文案必须进入 i18n。
7. APK 只作为布局参考，不复用其图片、品牌素材或业务逻辑。

## 6. 新的开发排序

### Phase 1：后台基础与配置中枢

先补后台信息架构、导航、RBAC 最小模型、审计统一工具、列表/筛选/空态规范。之后所有模块都接入这套后台壳。

### Phase 2：合规配置闭环

实现地区策略和法务文档后台。C 端注册、商店、活动、兑换、AMOE 都读取这些配置。

### Phase 3：活动与 Lobby 配置闭环

后台配置活动、任务、Coupon、Lobby 卡片；C 端 Lobby 和 Activity 从 API 渲染，不再写死。

### Phase 4：商品包与订单闭环

后台配置 GC 商品包，C 端商店读取；订单走正式状态机和 provider abstraction。

### Phase 5：KYC 审核闭环

C 端提交 KYC，后台按队列审核，支持通过、拒绝、补资料、provider 信息和审计。

### Phase 6：兑换审核闭环

C 端提交兑换并冻结 SC，后台审核、拒绝解冻、通过打款、失败重试、成功结算扣减。

### Phase 7：用户、钱包、风控、客服、AMOE

补全用户管理、钱包流水、风控事件、AMOE 申请审核、客服工单和补偿审批。

### Phase 8：全链路验收与上线材料

按“正常用户、风险用户、禁止州、重复请求、后台配置阻断、KYC、兑换、审计”做完整 e2e。

## 7. 验收口径

任一业务闭环完成时，必须满足：

- C 端能看到状态和下一步动作。
- B 端能配置或审核该状态。
- 后端 API 有状态机和错误码。
- 数据库能追溯配置、业务单、钱包流水和审计日志。
- 测试覆盖正常、失败、重复、权限或地区阻断。
- Playwright 能跑通 C 端和 B 端联动。
