# Tang Luck V1 正式上线设计

## 1. 目标

Tang Luck V1 要从演示系统升级为可正式上线的产品工程形态，覆盖 C 端用户完整链路和 B 端运营完整链路。

V1 的外部支付、KYC、打款供应商可以先使用 `manual` 或 `mock` provider 运行，但产品接口、数据库、状态机、前端文案、后台操作、测试验收必须按正式上线标准实现。

## 2. 语言与本地化

- 默认语言为英语。
- 当浏览器语言以 `zh` 开头时，自动使用中文。
- 后期必须支持用户手动切换语言，因此前端所有可见文案必须通过 i18n key 输出。
- 第一阶段支持 `en` 和 `zh-CN`。
- 后台与 C 端都必须接入同一套 i18n 基础设施。

## 3. C 端正式链路

### 3.1 注册与登录

- 注册必须包含年龄校验、地区合规校验、必签法务文档、邮箱唯一校验。
- 登录必须返回正式会话 token，并为后续 API 提供用户上下文。
- 前端不再依赖“演示账号”表达。

### 3.2 合规

- 地区配置决定注册、游戏、购买、SC 发放、兑换、AMOE 能力。
- 法务文档必须有版本、类型、生效时间和用户签署记录。
- 用户端必须可查看 Terms、Privacy、Sweepstakes Rules、AMOE。

### 3.3 钱包

- 钱包必须支持 GC 与 SC。
- 账本方向必须支持 `credit`、`debit`、`freeze`、`unfreeze`、`settle`、`refund`。
- 所有余额变动必须有业务类型、业务 ID、幂等键和审计时间。
- SC 不得通过购买直接销售。

### 3.4 活动与奖励

- 活动奖励必须走正式活动配置、规则版本、预算、风控和审计。
- Coupon、任务、邀请、AMOE 都必须归入奖励体系，并记录来源。
- SC 奖励必须受地区、法务、风控策略控制。

### 3.5 购买

- GC 商品包为正式商品。
- 订单状态必须至少覆盖 `created`、`payment_pending`、`paid`、`failed`、`cancelled`、`refunded`。
- 支付回调必须幂等处理。
- 本地环境可以用 `manual` provider 模拟支付成功，但 API 和数据库字段必须与真实 provider 接入兼容。

### 3.6 KYC

- KYC 状态必须覆盖 `not_started`、`submitted`、`provider_review`、`manual_review`、`approved`、`rejected`、`resubmission_required`。
- 必须记录 provider、provider reference、失败原因、审核备注和更新时间。
- 后台人工审核是正式能力，不是演示按钮。

### 3.7 兑换

- 兑换状态必须覆盖 `submitted`、`risk_review`、`approved`、`payout_processing`、`paid`、`failed`、`rejected`、`cancelled`。
- 创建兑换必须要求 KYC approved、地区允许、SC 可用余额足够。
- 创建后冻结 SC；拒绝或取消要解冻；支付成功要结算扣减。
- 打款 provider 暂未接入时使用 `manual` provider，但保留正式 provider reference 和失败重试能力。

## 4. B 端正式链路

后台必须从演示页升级为运营控制台：

- Dashboard：注册、购买、KYC、兑换、活动、风险指标。
- 用户管理：用户状态、地区、风险等级、钱包摘要、操作记录。
- 活动管理：活动创建、发布、暂停、预算、规则版本、审计。
- 订单管理：订单筛选、状态、provider reference、退款/失败记录。
- KYC 审核：资料、状态、provider 返回、人工审核、拒绝原因。
- 兑换审核：SC 来源、冻结记录、风险状态、通过/拒绝/重试。
- 审计日志：所有高风险操作必须写入审计。

## 5. 技术边界

- 后端继续使用 Spring Boot、JPA、Flyway、MySQL。
- 前端继续使用 Vue 3、Vue Router、Pinia、Vitest、Playwright。
- 新增 provider abstraction，但不引入真实供应商 SDK，直到密钥和供应商确认。
- 保留现有测试能力，新增生产状态机测试。

## 6. 验收标准

- 用户可以用正式文案完成注册、登录、查看钱包、购买 GC、提交 KYC、申请兑换。
- 后台可以查看并处理订单、KYC、兑换和审计。
- 英文为默认语言；中文浏览器自动显示中文。
- 无用户可见 `demo`、`sandbox`、`P0-A preview` 作为产品文案。
- 后端、前端单测、构建和 Playwright E2E 全部通过。
