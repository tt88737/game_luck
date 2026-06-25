# API 草案

## 1. 通用约定

所有 C 端接口必须带租户上下文。

推荐上下文来源：

```text
Host / domain -> tenant_id
Authorization -> member_id
X-Channel-Code -> h5 / pwa / android_apk / google_play / ios_appstore
X-Brand-Code -> brand_code
```

统一响应：

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

统一错误：

```json
{
  "code": 400001,
  "message": "wallet balance insufficient",
  "data": {
    "field": "amount"
  }
}
```

## 2. 配置接口

### 2.1 获取站点启动配置

```http
GET /api/client/bootstrap
```

返回：

```json
{
  "tenantId": "10001",
  "brandCode": "demo",
  "channelCode": "h5",
  "theme": {
    "logoUrl": "https://cdn.example.com/logo.png",
    "primaryColor": "#16A34A"
  },
  "features": {
    "gcEnabled": true,
    "scEnabled": true,
    "rcEnabled": false,
    "rechargeEnabled": true,
    "redemptionEnabled": true,
    "kycEnabled": true
  },
  "currencies": [
    {
      "currencyCode": "GC",
      "currencyName": "Gold Coin",
      "decimalScale": 2,
      "playable": true,
      "rechargeable": true,
      "withdrawable": false
    }
  ]
}
```

## 3. 会员接口

### 3.1 注册

```http
POST /api/client/auth/register
```

请求：

```json
{
  "email": "player@example.com",
  "password": "Password123!",
  "inviteCode": "ABC123"
}
```

### 3.2 登录

```http
POST /api/client/auth/login
```

请求：

```json
{
  "email": "player@example.com",
  "password": "Password123!"
}
```

返回：

```json
{
  "accessToken": "token",
  "expiresIn": 7200,
  "member": {
    "memberId": "20001",
    "email": "player@example.com",
    "status": "normal",
    "kycStatus": "not_submitted"
  }
}
```

## 4. 钱包接口

### 4.1 查询钱包余额

```http
GET /api/client/wallet/accounts
```

返回：

```json
[
  {
    "currencyCode": "GC",
    "availableBalance": "1000.00",
    "frozenBalance": "0.00",
    "decimalScale": 2
  },
  {
    "currencyCode": "SC",
    "availableBalance": "10.00",
    "frozenBalance": "0.00",
    "decimalScale": 2
  }
]
```

### 4.2 查询账变

```http
GET /api/client/wallet/ledgers?currencyCode=SC&pageNum=1&pageSize=20
```

返回字段：

```json
{
  "records": [
    {
      "ledgerId": "90001",
      "currencyCode": "SC",
      "direction": "credit",
      "amount": "5.00",
      "afterAvailable": "15.00",
      "bizType": "promotion",
      "createdAt": "2026-06-25T12:00:00Z"
    }
  ],
  "total": 1
}
```

## 5. 游戏接口

### 5.1 游戏列表

```http
GET /api/client/games?currencyCode=GC
```

返回：

```json
[
  {
    "providerCode": "mock",
    "gameCode": "mock-slot-001",
    "gameName": "Mock Slot",
    "status": "enabled",
    "supportedCurrencies": ["GC", "SC"]
  }
]
```

### 5.2 启动游戏

```http
POST /api/client/games/launch
```

请求：

```json
{
  "providerCode": "mock",
  "gameCode": "mock-slot-001",
  "currencyCode": "GC"
}
```

返回：

```json
{
  "launchUrl": "https://game.example.com/launch?token=mock",
  "sessionNo": "GS202606250001"
}
```

### 5.3 模拟供应商下注回调

```http
POST /api/provider/mock/bet
```

请求：

```json
{
  "tenantId": "10001",
  "memberId": "20001",
  "roundNo": "R202606250001",
  "currencyCode": "GC",
  "amount": "10.00",
  "idempotencyKey": "mock-bet-R202606250001"
}
```

规则：

- 同一个 `tenantId + idempotencyKey` 重复请求只能扣款一次。
- 余额不足返回明确错误码。

### 5.4 模拟供应商派彩回调

```http
POST /api/provider/mock/payout
```

请求：

```json
{
  "tenantId": "10001",
  "memberId": "20001",
  "roundNo": "R202606250001",
  "currencyCode": "GC",
  "amount": "18.00",
  "idempotencyKey": "mock-payout-R202606250001"
}
```

## 6. 活动接口

### 6.1 活动列表

```http
GET /api/client/promotions
```

### 6.2 领取活动奖励

```http
POST /api/client/promotions/{promotionId}/claim
```

规则：

- 后端判断是否可领取。
- 奖励通过 `wallet-center` 入账。
- 重复领取返回已领取状态，不重复入账。

## 7. 兑换接口

### 7.1 提交兑换申请

```http
POST /api/client/redemptions
```

请求：

```json
{
  "currencyCode": "SC",
  "amount": "50.00",
  "method": "gift_card"
}
```

规则：

- 创建申请时冻结钱包余额。
- KYC 未通过时返回错误。
- 币种不可兑换时返回错误。

### 7.2 后台审核通过

```http
POST /api/admin/redemptions/{redemptionNo}/approve
```

规则：

- 审核通过后结算冻结金额。
- 写审计日志。

### 7.3 后台审核拒绝

```http
POST /api/admin/redemptions/{redemptionNo}/reject
```

请求：

```json
{
  "reason": "KYC information incomplete"
}
```

规则：

- 审核拒绝后解冻余额。
- 写审计日志。

## 8. 后台配置接口

后台接口跟随 RuoYi-Vue-Plus 风格，P0 至少需要：

```text
租户品牌配置 CRUD
渠道开关 CRUD
币种配置 CRUD
游戏列表 CRUD
会员查询
钱包账户查询
账变查询
兑换审核
审计日志查询
```

## 9. 错误码草案

| 错误码 | 说明 |
| --- | --- |
| 400001 | 钱包余额不足 |
| 400002 | 币种未启用 |
| 400003 | 渠道功能未启用 |
| 400004 | 会员状态不可用 |
| 400005 | KYC 未通过 |
| 400006 | 重复业务请求 |
| 400007 | 兑换金额不满足规则 |
| 400008 | 游戏维护中 |
| 400009 | 地区限制 |
