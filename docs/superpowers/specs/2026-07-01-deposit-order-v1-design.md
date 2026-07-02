# 充值订单 v1 设计

## 1. 目标

充值订单 v1 用于把当前钱包中心跑成最小业务闭环：

```text
后台创建模拟充值订单 -> 模拟支付成功 -> 调用钱包入账 -> 生成账变流水和释放记录
```

本阶段只做模拟支付，不接真实三方支付渠道，不处理签名验签、渠道回调、风控拦截和对账文件。

## 2. 模块边界

### 2.1 payment-center 负责

- 充值订单创建、查询和状态流转。
- 保存订单金额、会员、币种、支付方式、订单状态和支付时间。
- 保证同一充值订单只能成功一次。
- 在订单支付成功时调用 `wallet-center` 入账。

### 2.2 wallet-center 负责

- 会员钱包账户创建和余额增加。
- 幂等账变流水。
- 按钱包规则生成释放记录。
- 禁止其他模块直接改余额。

### 2.3 本阶段不做

- 真实支付渠道配置。
- 三方支付回调验签。
- 充值优惠、赠送彩金、渠道费率。
- 充值限额、KYC、地区风控。
- C 端充值页。
- 自动对账和退款。

## 3. 订单状态

充值订单使用简单状态机：

| 状态 | 含义 | 可执行动作 |
| --- | --- | --- |
| PENDING | 待支付 | 模拟支付成功、取消 |
| SUCCESS | 已支付并已入账 | 只允许查看 |
| CANCELLED | 已取消 | 只允许查看 |
| FAILED | 支付失败或入账失败 | 只允许查看，后续版本再做重试 |

v1 只实现 `PENDING -> SUCCESS` 和 `PENDING -> CANCELLED`。

## 4. 数据表

新增表：`gl_payment_deposit_order`

核心字段：

```text
id
tenant_id
deposit_order_no
member_id
currency_code
amount
pay_method
pay_channel
status
wallet_transaction_no
wallet_idempotency_key
pay_time
remark
create_dept
create_by
create_time
update_by
update_time
version
del_flag
```

唯一约束：

```text
tenant_id + deposit_order_no
tenant_id + wallet_idempotency_key
```

默认币种使用 `RC`。后续如果开放其他可充值币种，由币种配置和支付配置共同控制。

## 5. 入账规则

模拟支付成功时，`payment-center` 调用：

```text
IWalletCoreService.credit(WalletCreditBo)
```

参数约定：

```text
memberId = deposit.memberId
currencyCode = deposit.currencyCode
amount = deposit.amount
sourceType = DEPOSIT
businessNo = deposit.depositOrderNo
idempotencyKey = deposit:success:{depositOrderNo}
```

`releaseMode` 和 `requiredTurnover` 不由充值订单强行传入，优先由钱包规则中心解析。当前默认规则为：

```text
RC + DEPOSIT -> IMMEDIATE
```

因此充值成功后应产生：

- 钱包账户 `available_balance` 增加。
- 一条 `CREDIT / DEPOSIT / SUCCESS` 账变。
- 一条 `RELEASED / IMMEDIATE` 释放记录。

## 6. 幂等规则

充值成功必须有两层幂等：

1. 订单层：只有 `PENDING` 订单允许执行模拟支付成功。`SUCCESS` 订单再次执行直接返回当前订单，不重复调用钱包。
2. 钱包层：`wallet_idempotency_key = deposit:success:{depositOrderNo}`，即使重复调用钱包也不能重复入账。

如果钱包入账失败，订单标记为 `FAILED`，记录失败原因。v1 不自动重试，避免重复入账风险。

## 7. 后台页面

新增菜单：`支付中心 / 充值订单`

页面能力：

- 查询条件：订单号、会员 ID、币种、状态、创建时间。
- 表格字段：订单号、会员 ID、币种、金额、支付方式、状态、钱包交易号、支付时间、创建时间。
- 操作：
  - 新增模拟订单。
  - 对 `PENDING` 订单执行“模拟支付成功”。
  - 对 `PENDING` 订单执行“取消”。
  - 查看详情。

高风险操作要求：

- “模拟支付成功”必须弹确认框。
- 成功后展示订单状态和钱包交易号。
- `SUCCESS`、`CANCELLED`、`FAILED` 状态不展示成功或取消按钮。

## 8. 接口

后台接口：

```http
GET /payment/deposit/list
GET /payment/deposit/{id}
POST /payment/deposit
POST /payment/deposit/{id}/simulate-success
POST /payment/deposit/{id}/cancel
```

权限标识：

```text
payment:deposit:list
payment:deposit:query
payment:deposit:add
payment:deposit:simulate
payment:deposit:cancel
```

## 9. 验收标准

- 后台可以创建一笔 `RC` 模拟充值订单。
- `PENDING` 订单可以模拟支付成功。
- 成功后订单变为 `SUCCESS`，记录 `wallet_transaction_no`。
- 钱包账户余额增加对应金额。
- 账变流水可在“账变流水”页面查到。
- 释放记录可在“释放记录”页面查到，状态为 `RELEASED`。
- 同一订单重复点击模拟成功不会重复增加余额。
- 后端编译通过，前端构建通过。

## 10. 风险控制

- v1 不接真实支付，避免误触真实资金链路。
- 不允许绕过钱包中心改余额。
- 订单成功和钱包入账放在同一业务事务内。
- 后台按钮仅作为模拟联调入口，后续真实支付接入时改为渠道回调驱动。
