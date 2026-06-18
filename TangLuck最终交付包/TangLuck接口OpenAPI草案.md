# Tang Luck 接口 OpenAPI 草案

## 1. 文档定位

本文用于把 P0-A 15 天版本的核心接口从“功能清单”细化到前后端、QA 可以评审的接口契约草案。正式开发前，后端仍需把本文转成完整 `openapi.yaml` 并补充鉴权、字段类型、枚举和错误响应。

## 2. 通用规则

| 项目 | 标准 |
| --- | --- |
| Base URL | `/api/v1` |
| 鉴权 | C 端使用 `Authorization: Bearer <user_token>`；后台使用 `Authorization: Bearer <admin_token>` + RBAC |
| 幂等 | 发奖、任务领奖、Coupon 领取、支付回调、兑换冻结、客服补偿必须传 `Idempotency-Key` |
| 时间 | 服务端使用 UTC ISO8601，例如 `2026-06-17T08:00:00Z` |
| 金额 | GC 使用整数；SC 使用 decimal string，例如 `"0.50"` |
| 地区 | 美国必须传或识别 `state_code`，不允许只用 `country=US` 判断 |
| 错误格式 | `code`、`message`、`trace_id`、`details` |
| 审计 | 后台写操作必须生成 `audit_logs` |

## 3. 通用错误结构

```json
{
  "code": "REGION_BLOCKED",
  "message": "This feature is not available in your region.",
  "trace_id": "trc_20260617_0001",
  "details": {
    "state_code": "WA",
    "feature": "sc_grant"
  }
}
```

## 4. C 端 P0-A 接口

| 接口 | 方法 | 用途 | 幂等 | P0-A |
| --- | --- | --- | --- | --- |
| `/auth/register` | POST | 注册、年龄确认、条款确认、创建钱包 | 否 | 是 |
| `/auth/login` | POST | 登录 | 否 | 是 |
| `/me` | GET | 当前用户、地区、风险摘要 | 否 | 是 |
| `/compliance/documents` | GET | 获取 Terms/Rules/Privacy/AMOE | 否 | 是 |
| `/wallet/summary` | GET | 钱包余额、冻结、SC 来源摘要 | 否 | 是 |
| `/wallet/ledger` | GET | 钱包流水 | 否 | 是 |
| `/campaigns` | GET | 活动列表 | 否 | 是 |
| `/campaigns/{campaign_id}/claim` | POST | 活动领取 | 是 | 是 |
| `/tasks/daily` | GET | 每日任务列表 | 否 | 是 |
| `/tasks/{task_id}/progress` | POST | 上报任务进度 | 是 | 是 |
| `/tasks/{task_id}/claim` | POST | 任务领奖 | 是 | 是 |
| `/coupon/claim` | POST | Coupon 领取 | 是 | 是 |
| `/support/tickets` | POST | 创建客服工单 | 是 | P0-B |
| `/amoe/requests` | POST | AMOE 申请 | 是 | P0-B |
| `/redemptions` | POST | 提交兑换申请 | 是 | P0-B/P1 |
| `/purchase/packages` | GET | GC 商品包列表 | 否 | P1 |
| `/purchase/orders` | POST | 创建购买订单 | 是 | P1 |

## 5. 后台 P0-A 接口

| 接口 | 方法 | 用途 | 权限 |
| --- | --- | --- | --- |
| `/admin/dashboard/summary` | GET | 注册、领取、SC、风控、兑换摘要 | `dashboard.read` |
| `/admin/users` | GET | 用户列表、地区、风险、钱包摘要 | `user.read` |
| `/admin/users/{user_id}` | GET | 用户详情 | `user.read` |
| `/admin/wallet/ledger` | GET | 钱包流水查询 | `wallet.read` |
| `/admin/campaigns` | GET | 活动列表 | `campaign.read` |
| `/admin/campaigns` | POST | 新建活动 | `campaign.write` |
| `/admin/campaigns/{campaign_id}` | PATCH | 编辑活动 | `campaign.write` |
| `/admin/campaigns/{campaign_id}/publish` | POST | 发布活动，执行阻断校验 | `campaign.publish` |
| `/admin/campaigns/{campaign_id}/pause` | POST | 暂停活动 | `campaign.publish` |
| `/admin/reward-grants` | GET | 发奖记录 | `reward.read` |
| `/admin/risk/events` | GET | 风控事件 | `risk.read` |
| `/admin/audit-logs` | GET | 审计日志 | `audit.read` |

## 6. 核心接口详情

### 6.1 POST `/auth/register`

请求：

```json
{
  "email": "ava@example.com",
  "password": "StrongPass123!",
  "birth_date": "1996-04-12",
  "country_code": "US",
  "state_code": "CA",
  "accepted_documents": [
    { "document_type": "terms", "version": "terms-v1" },
    { "document_type": "sweepstakes_rules", "version": "rules-v1" },
    { "document_type": "privacy", "version": "privacy-v1" }
  ],
  "utm_source": "demo",
  "device_id": "dev_abc123"
}
```

响应：

```json
{
  "user": {
    "user_id": "usr_10001",
    "email": "ava@example.com",
    "country_code": "US",
    "state_code": "CA",
    "risk_level": "low",
    "status": "active"
  },
  "wallet": {
    "gc_balance": 0,
    "sc_balance": "0.00",
    "sc_frozen": "0.00"
  },
  "token": "jwt_user_token"
}
```

校验规则：

| 场景 | 返回 |
| --- | --- |
| 未满年龄 | `AGE_NOT_ALLOWED` |
| 未勾选必需文档 | `CONSENT_REQUIRED` |
| 禁止州 | `REGION_BLOCKED` |
| 邮箱重复 | `EMAIL_EXISTS` |

### 6.2 GET `/wallet/summary`

响应：

```json
{
  "wallet": {
    "gc_balance": 128500,
    "sc_balance": "3.25",
    "sc_frozen": "0.00",
    "sc_redeemable": "3.25"
  },
  "sc_source_summary": [
    { "source": "register_bonus", "amount": "0.50" },
    { "source": "daily_login", "amount": "0.05" },
    { "source": "amoe", "amount": "0.00" }
  ],
  "notices": [
    "Sweeps Coins are not sold.",
    "No Purchase Necessary / AMOE is available."
  ]
}
```

### 6.3 GET `/wallet/ledger`

Query：

| 参数 | 必填 | 说明 |
| --- | --- | --- |
| `currency` | 否 | `GC`、`SC` |
| `business_type` | 否 | `register_bonus`、`daily_login`、`coupon` |
| `page` | 否 | 默认 `1` |
| `page_size` | 否 | 默认 `20`，最大 `100` |

响应：

```json
{
  "items": [
    {
      "ledger_id": "led_9001",
      "currency": "SC",
      "amount": "0.50",
      "direction": "credit",
      "business_type": "register_bonus",
      "business_id": "claim_1001",
      "status": "posted",
      "created_at": "2026-06-17T08:00:00Z"
    }
  ],
  "page": 1,
  "page_size": 20,
  "total": 1
}
```

### 6.4 POST `/campaigns/{campaign_id}/claim`

Header：

| Header | 必填 | 说明 |
| --- | --- | --- |
| `Idempotency-Key` | 是 | 建议 `campaign:{campaign_id}:user:{user_id}:period:{period_key}` |

响应：

```json
{
  "claim_id": "claim_1001",
  "campaign_id": "cmp_register_bonus",
  "status": "granted",
  "risk_action": "pass",
  "rewards": [
    { "currency": "GC", "amount": 10000 },
    { "currency": "SC", "amount": "0.50" }
  ],
  "ledger_ids": ["led_9001", "led_9002"]
}
```

阻断规则：

| 场景 | 返回 |
| --- | --- |
| 重复领取 | `CLAIM_DUPLICATED` |
| 活动已结束 | `CAMPAIGN_NOT_ACTIVE` |
| 预算用尽 | `BUDGET_EXHAUSTED` |
| 地区禁止 | `REGION_BLOCKED` |
| SC 策略不允许 | `SC_POLICY_BLOCKED` |
| 需要法务审批但缺少审批号 | `LEGAL_APPROVAL_REQUIRED` |

### 6.5 POST `/coupon/claim`

请求：

```json
{
  "code": "WELCOME500",
  "device_id": "dev_abc123"
}
```

响应：

```json
{
  "coupon_claim_id": "cpn_claim_1001",
  "status": "granted",
  "rewards": [
    { "currency": "GC", "amount": 500 }
  ]
}
```

### 6.6 POST `/admin/campaigns`

请求：

```json
{
  "name": "Daily Login",
  "campaign_type": "daily_login",
  "eligible_regions": ["CA", "TX", "NJ"],
  "blocked_regions": ["WA"],
  "reward_policy": [
    { "currency": "GC", "amount": 1000 },
    { "currency": "SC", "amount": "0.05" }
  ],
  "sc_strategy": "default_small_sc",
  "daily_budget_cap": {
    "GC": 2000000,
    "SC": "200.00"
  },
  "user_period_cap": 1,
  "period_type": "daily",
  "rules_version": "rules-v1",
  "legal_approval_id": "LEGAL-2026-0617-CA",
  "risk_action": "gc_only"
}
```

响应：

```json
{
  "campaign_id": "cmp_daily_login",
  "status": "draft",
  "publish_check": {
    "passed": true,
    "blocking_reasons": []
  }
}
```

### 6.7 POST `/admin/campaigns/{campaign_id}/publish`

发布前阻断：

| 阻断项 | 规则 |
| --- | --- |
| 无地区配置 | `eligible_regions` 和 `blocked_regions` 不能都为空 |
| 无规则版本 | `rules_version` 必填 |
| 无预算 | `daily_budget_cap` 必填 |
| 发 SC 无审批 | `sc_strategy != gc_only` 时必须校验 `legal_approval_id` |
| 随机/转盘/排行榜高价值奖品 | 必须 `sc_strategy=legal_required` 且审批通过 |

响应：

```json
{
  "campaign_id": "cmp_daily_login",
  "status": "active",
  "published_at": "2026-06-17T08:00:00Z",
  "audit_log_id": "aud_7001"
}
```

## 7. P1 预留接口

| 接口 | 方法 | 当前处理 |
| --- | --- | --- |
| `/purchase/packages` | GET | P0-A 可返回灰态商品包，不创建订单 |
| `/purchase/orders` | POST | P1 开放，未开放时返回 `PAYMENT_NOT_ALLOWED` |
| `/payment/webhook/{provider}` | POST | P1 开放，必须幂等 |
| `/redemptions` | POST | P0-B/P1 开放，P0-A 返回灰态说明 |
| `/amoe/requests` | POST | P0-B 开放，P0-A 使用静态说明页 |

## 8. 错误码

| 错误码 | HTTP | 场景 |
| --- | --- | --- |
| `REGION_BLOCKED` | 403 | 地区不可用 |
| `AGE_NOT_ALLOWED` | 403 | 年龄不符合 |
| `CONSENT_REQUIRED` | 400 | 未确认条款 |
| `EMAIL_EXISTS` | 409 | 邮箱已注册 |
| `CLAIM_DUPLICATED` | 409 | 重复领取 |
| `CAMPAIGN_NOT_ACTIVE` | 400 | 活动未开始、已暂停或已结束 |
| `BUDGET_EXHAUSTED` | 409 | 活动预算用尽 |
| `SC_POLICY_BLOCKED` | 403 | SC 策略拦截 |
| `LEGAL_APPROVAL_REQUIRED` | 400 | 缺少法务审批 |
| `PAYMENT_NOT_ALLOWED` | 403 | 不允许购买 |
| `KYC_REQUIRED` | 403 | 需要 KYC |
| `REDEMPTION_NOT_ALLOWED` | 403 | 不允许兑换 |
| `RISK_REVIEW_REQUIRED` | 202 | 进入人工审核 |
| `IDEMPOTENCY_CONFLICT` | 409 | 幂等 key 与请求内容冲突 |

## 9. QA 接口验收重点

| 场景 | 预期 |
| --- | --- |
| 同一活动重复领取 | 第二次返回 `CLAIM_DUPLICATED`，不新增 ledger |
| 风险用户领取含 SC 活动 | 根据 `risk_action` 只发 GC、延迟或拒绝 |
| 禁止州用户领取 | 返回 `REGION_BLOCKED` |
| 活动缺 `legal_approval_id` 发布 | 返回 `LEGAL_APPROVAL_REQUIRED` |
| Coupon 撞库 | 达到频率上限后返回风控错误或进入人工审核 |
| 后台发布活动 | 生成 `audit_logs` |
