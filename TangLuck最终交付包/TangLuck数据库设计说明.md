# Tang Luck 数据库设计说明

## 1. 文档定位

本文用于把 P0-A 15 天版本的核心数据模型细化到字段级，帮助后端、QA、数据和审计评审。字段类型以 MySQL / PostgreSQL 通用表达为主，正式开发时可按技术栈调整。

## 2. 设计原则

| 原则 | 说明 |
| --- | --- |
| 钱包不可只存余额 | 所有变化必须进入 `wallet_ledger` |
| 配置必须版本化 | 活动、规则、商品包、地区策略都要有版本 |
| 后台操作必须审计 | 运营、客服、风控、法务动作都进入 `audit_logs` |
| 支付和发奖必须幂等 | 使用业务唯一 key 防重复 |
| 地区策略后端强校验 | 前端隐藏不等于安全 |
| SC 高风险字段必留痕 | SC 发放、冻结、扣减、解冻必须可追溯 |

## 3. P0-A 核心表清单

| 表 | 用途 | P0-A |
| --- | --- | --- |
| `users` | 用户基础信息 | 是 |
| `user_consent_logs` | 条款确认 | 是 |
| `compliance_regions` | 州级合规开关 | 是 |
| `compliance_documents` | 规则文档 CMS | 是 |
| `wallet_accounts` | 钱包余额 | 是 |
| `wallet_ledger` | 钱包流水 | 是 |
| `promotion_campaigns` | 活动配置 | 是 |
| `promotion_claims` | 活动领取 | 是 |
| `promotion_reward_grants` | 发奖记录 | 是 |
| `daily_task_progress` | 每日任务进度 | 是 |
| `coupon_codes` | Coupon 批次和 code | 是 |
| `risk_events` | 风控事件 | 是 |
| `audit_logs` | 审计日志 | 是 |
| `product_packages` | GC 商品包 | 预留 |
| `purchase_orders` | 购买订单 | P1 |
| `payment_events` | 支付回调 | P1 |
| `redemption_requests` | 兑换申请 | P0-B/P1 |
| `amoe_requests` | AMOE 申请 | P0-B |
| `support_tickets` | 客服工单 | P0-B |

## 4. 字段级设计

### 4.1 `users`

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `id` | bigint / uuid | 是 | 用户 ID |
| `email` | varchar(255) | 是 | 唯一 |
| `password_hash` | varchar(255) | 是 | 密码哈希 |
| `birth_date` | date | 是 | 年龄判断 |
| `country_code` | char(2) | 是 | 例如 `US` |
| `state_code` | varchar(8) | 是 | 美国州级控制 |
| `status` | varchar(32) | 是 | `active`、`restricted`、`blocked` |
| `risk_level` | varchar(32) | 是 | `low`、`medium`、`high`、`manual_review` |
| `device_id` | varchar(128) | 否 | 首次注册设备 |
| `register_ip` | varchar(64) | 否 | 注册 IP |
| `created_at` | timestamp | 是 | 创建时间 |
| `updated_at` | timestamp | 是 | 更新时间 |

索引：

| 索引 | 规则 |
| --- | --- |
| `uk_users_email` | `email unique` |
| `idx_users_region` | `country_code, state_code` |
| `idx_users_risk` | `risk_level, status` |

### 4.2 `user_consent_logs`

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `id` | bigint / uuid | 是 | 记录 ID |
| `user_id` | bigint / uuid | 是 | 用户 ID |
| `document_type` | varchar(64) | 是 | `terms`、`sweepstakes_rules`、`privacy`、`amoe` |
| `version` | varchar(64) | 是 | 文档版本 |
| `accepted_at` | timestamp | 是 | 确认时间 |
| `ip` | varchar(64) | 否 | IP |
| `device_id` | varchar(128) | 否 | 设备 |

索引：`user_id, document_type, version unique`。

### 4.3 `compliance_regions`

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `id` | bigint / uuid | 是 | 配置 ID |
| `country_code` | char(2) | 是 | 国家 |
| `state_code` | varchar(8) | 是 | 州 |
| `registration_allowed` | boolean | 是 | 是否允许注册 |
| `game_allowed` | boolean | 是 | 是否允许游戏 |
| `purchase_allowed` | boolean | 是 | 是否允许购买 GC |
| `sc_grant_allowed` | boolean | 是 | 是否允许发 SC |
| `redemption_allowed` | boolean | 是 | 是否允许兑换 |
| `amoe_allowed` | boolean | 是 | 是否允许 AMOE |
| `requires_legal_review` | boolean | 是 | 是否需复核 |
| `status` | varchar(32) | 是 | `active`、`blocked`、`review_required` |
| `legal_approval_id` | varchar(128) | 否 | 法务确认 ID |
| `updated_at` | timestamp | 是 | 更新时间 |

索引：`country_code, state_code unique`。

### 4.4 `compliance_documents`

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `id` | bigint / uuid | 是 | 文档 ID |
| `document_type` | varchar(64) | 是 | 文档类型 |
| `version` | varchar(64) | 是 | 版本 |
| `title` | varchar(255) | 是 | 标题 |
| `content_url` | varchar(512) | 是 | 文档地址 |
| `effective_at` | timestamp | 是 | 生效时间 |
| `status` | varchar(32) | 是 | `draft`、`active`、`archived` |
| `legal_approval_id` | varchar(128) | 否 | 法务确认 |

索引：`document_type, version unique`。

### 4.5 `wallet_accounts`

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `id` | bigint / uuid | 是 | 钱包 ID |
| `user_id` | bigint / uuid | 是 | 用户 ID |
| `currency` | varchar(8) | 是 | `GC`、`SC` |
| `balance` | decimal(20,4) | 是 | 可用余额；GC 小数位固定为 0 |
| `frozen_balance` | decimal(20,4) | 是 | 冻结余额 |
| `status` | varchar(32) | 是 | `active`、`frozen`、`closed` |
| `created_at` | timestamp | 是 | 创建时间 |
| `updated_at` | timestamp | 是 | 更新时间 |

索引：`user_id, currency unique`。

### 4.6 `wallet_ledger`

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `id` | bigint / uuid | 是 | 流水 ID |
| `user_id` | bigint / uuid | 是 | 用户 ID |
| `wallet_account_id` | bigint / uuid | 是 | 钱包 ID |
| `currency` | varchar(8) | 是 | `GC`、`SC` |
| `direction` | varchar(16) | 是 | `credit`、`debit`、`freeze`、`unfreeze` |
| `amount` | decimal(20,4) | 是 | 变动金额 |
| `balance_after` | decimal(20,4) | 是 | 变动后可用余额 |
| `frozen_after` | decimal(20,4) | 是 | 变动后冻结余额 |
| `business_type` | varchar(64) | 是 | `register_bonus`、`daily_login`、`coupon`、`redemption_freeze` |
| `business_id` | varchar(128) | 是 | 业务单 ID |
| `idempotency_key` | varchar(255) | 是 | 幂等 key |
| `status` | varchar(32) | 是 | `posted`、`reversed`、`failed` |
| `metadata_json` | json/text | 否 | 额外信息 |
| `created_at` | timestamp | 是 | 创建时间 |

索引：

| 索引 | 规则 |
| --- | --- |
| `uk_ledger_idempotency` | `idempotency_key unique` |
| `idx_ledger_user_time` | `user_id, created_at` |
| `idx_ledger_business` | `business_type, business_id` |

### 4.7 `promotion_campaigns`

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `id` | bigint / uuid | 是 | 活动 ID |
| `campaign_code` | varchar(64) | 是 | 唯一 code |
| `name` | varchar(255) | 是 | 活动名称 |
| `campaign_type` | varchar(64) | 是 | `register`、`daily_login`、`daily_task`、`coupon`、`invite`、`leaderboard`、`wheel` |
| `status` | varchar(32) | 是 | `draft`、`reviewing`、`active`、`paused`、`ended`、`rejected` |
| `eligible_regions_json` | json/text | 是 | 开放州 |
| `blocked_regions_json` | json/text | 否 | 禁止州 |
| `reward_policy_json` | json/text | 是 | 奖励配置 |
| `sc_strategy` | varchar(32) | 是 | `gc_only`、`default_small_sc`、`legal_required`、`sc_blocked` |
| `daily_budget_cap_json` | json/text | 是 | 日预算 |
| `user_period_cap` | int | 是 | 用户周期上限 |
| `period_type` | varchar(32) | 是 | `once`、`daily`、`weekly` |
| `rules_version` | varchar(64) | 是 | 规则版本 |
| `legal_approval_id` | varchar(128) | 否 | 法务审批 |
| `risk_action` | varchar(32) | 是 | `pass`、`gc_only`、`delay`、`manual_review`、`block` |
| `starts_at` | timestamp | 是 | 开始时间 |
| `ends_at` | timestamp | 否 | 结束时间 |
| `created_by` | bigint / uuid | 是 | 创建人 |
| `updated_by` | bigint / uuid | 是 | 更新人 |

索引：`campaign_code unique`、`campaign_type, status`。

### 4.8 `promotion_claims`

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `id` | bigint / uuid | 是 | 领取 ID |
| `user_id` | bigint / uuid | 是 | 用户 ID |
| `campaign_id` | bigint / uuid | 是 | 活动 ID |
| `period_key` | varchar(64) | 是 | `once` 或 `2026-06-17` |
| `status` | varchar(32) | 是 | `pending`、`granted`、`delayed`、`rejected`、`failed` |
| `risk_action` | varchar(32) | 是 | 风控动作 |
| `idempotency_key` | varchar(255) | 是 | 幂等 key |
| `created_at` | timestamp | 是 | 领取时间 |

索引：`user_id, campaign_id, period_key unique`、`idempotency_key unique`。

### 4.9 `promotion_reward_grants`

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `id` | bigint / uuid | 是 | 发奖 ID |
| `claim_id` | bigint / uuid | 是 | 领取 ID |
| `user_id` | bigint / uuid | 是 | 用户 ID |
| `currency` | varchar(8) | 是 | `GC`、`SC` |
| `amount` | decimal(20,4) | 是 | 奖励数量 |
| `ledger_id` | bigint / uuid | 否 | 成功后关联 ledger |
| `status` | varchar(32) | 是 | `pending`、`posted`、`rejected`、`failed` |
| `reject_reason` | varchar(255) | 否 | 拒绝原因 |
| `created_at` | timestamp | 是 | 创建时间 |

### 4.10 `daily_task_progress`

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `id` | bigint / uuid | 是 | 任务进度 ID |
| `user_id` | bigint / uuid | 是 | 用户 ID |
| `task_code` | varchar(64) | 是 | 任务 code |
| `period_key` | varchar(64) | 是 | 日期 |
| `progress` | int | 是 | 当前进度 |
| `target` | int | 是 | 目标值 |
| `status` | varchar(32) | 是 | `in_progress`、`completed`、`claimed` |
| `claimed_at` | timestamp | 否 | 领取时间 |

索引：`user_id, task_code, period_key unique`。

### 4.11 `coupon_codes`

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `id` | bigint / uuid | 是 | Coupon ID |
| `code` | varchar(64) | 是 | 唯一 code |
| `batch_id` | varchar(64) | 是 | 批次 |
| `reward_policy_json` | json/text | 是 | 奖励配置 |
| `max_total_claims` | int | 是 | 总领取上限 |
| `max_user_claims` | int | 是 | 单用户上限 |
| `eligible_regions_json` | json/text | 是 | 地区 |
| `sc_strategy` | varchar(32) | 是 | 默认 `gc_only` |
| `status` | varchar(32) | 是 | `active`、`paused`、`expired` |
| `starts_at` | timestamp | 是 | 开始时间 |
| `ends_at` | timestamp | 是 | 结束时间 |

索引：`code unique`。

### 4.12 `risk_events`

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `id` | bigint / uuid | 是 | 风控事件 ID |
| `user_id` | bigint / uuid | 是 | 用户 ID |
| `event_type` | varchar(64) | 是 | `multi_account`、`device_repeat`、`ip_risk`、`region_mismatch` |
| `risk_level` | varchar(32) | 是 | `low`、`medium`、`high` |
| `action` | varchar(32) | 是 | `pass`、`gc_only`、`manual_review`、`block` |
| `related_business_type` | varchar(64) | 否 | 关联业务 |
| `related_business_id` | varchar(128) | 否 | 关联业务 ID |
| `metadata_json` | json/text | 否 | 命中详情 |
| `created_at` | timestamp | 是 | 创建时间 |

### 4.13 `audit_logs`

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `id` | bigint / uuid | 是 | 审计 ID |
| `operator_id` | bigint / uuid | 是 | 操作人 |
| `operator_role` | varchar(64) | 是 | 角色 |
| `action` | varchar(64) | 是 | `campaign_publish`、`redemption_review` |
| `target_type` | varchar(64) | 是 | 对象类型 |
| `target_id` | varchar(128) | 是 | 对象 ID |
| `before_json` | json/text | 否 | 前值 |
| `after_json` | json/text | 否 | 后值 |
| `reason` | varchar(512) | 否 | 操作原因 |
| `ip` | varchar(64) | 否 | IP |
| `created_at` | timestamp | 是 | 操作时间 |

索引：`operator_id, created_at`、`target_type, target_id`。

## 5. P1/P0-B 预留表关键字段

| 表 | P0-A 处理 | P0-B/P1 必补字段 |
| --- | --- | --- |
| `product_packages` | 可建表灰态 | `gc_amount`、`price_usd`、`sc_bonus_enabled`、`legal_approval_id`、`eligible_regions_json` |
| `purchase_orders` | 不开放真实订单 | `provider`、`provider_order_id`、`status`、`amount_usd`、`fulfilled_at` |
| `payment_events` | 不开放真实回调 | `provider_event_id unique`、`event_type`、`payload_hash` |
| `redemption_requests` | P0-B 开放 | `sc_amount`、`frozen_ledger_id`、`kyc_status`、`risk_status`、`review_reason` |
| `amoe_requests` | P0-B 开放 | `request_method`、`frequency_key`、`review_status`、`grant_ledger_id` |
| `support_tickets` | P0-B 开放 | `category`、`related_business_type`、`related_business_id`、`sla_due_at` |

## 6. 状态枚举

| 对象 | 状态 |
| --- | --- |
| 活动 | `draft`、`reviewing`、`active`、`paused`、`ended`、`rejected` |
| 领取 | `pending`、`granted`、`delayed`、`rejected`、`failed` |
| 发奖 | `pending`、`posted`、`rejected`、`failed` |
| 钱包流水 | `posted`、`reversed`、`failed` |
| 购买订单 | `created`、`paying`、`paid`、`fulfilled`、`failed`、`expired`、`refunded`、`chargeback` |
| 兑换 | `draft`、`kyc_required`、`reviewing`、`approved`、`paying`、`paid`、`failed`、`rejected`、`closed` |
| AMOE | `submitted`、`reviewing`、`approved`、`rejected`、`granted`、`closed` |
| 工单 | `open`、`pending_user`、`pending_internal`、`resolved`、`closed`、`escalated` |

## 7. 账务一致性要求

| 场景 | 要求 |
| --- | --- |
| 发 GC/SC | 同事务更新 `wallet_accounts` 和写入 `wallet_ledger` |
| 重复发奖请求 | 命中 `idempotency_key unique`，返回原结果 |
| 兑换冻结 | `direction=freeze`，增加 `frozen_balance`，减少可用或单独记录冻结 |
| 兑换拒绝释放 | `direction=unfreeze`，释放冻结 |
| 后台补偿 | P0-A 客服只能补 GC；补偿必须写 audit |

## 8. QA 数据库验收重点

| 测试 | 通过标准 |
| --- | --- |
| 注册创建钱包 | 每个用户有 GC、SC 两个钱包 |
| 发奖幂等 | 重复 key 不产生第二条 ledger |
| 活动重复领取 | `promotion_claims` 唯一约束生效 |
| SC 拦截 | 风险用户不生成 SC ledger 或生成 rejected grant |
| 后台发布活动 | `audit_logs` 有前后值 |
| 地区限制 | 禁止州用户无法生成 SC 发奖流水 |
