# 模拟游戏下注/派彩 v1 设计

## 1. 目标

本阶段用于把钱包中心从“充值入账”推进到“游戏扣款 + 游戏收益入账”闭环：

```text
后台创建模拟下注订单 -> 钱包扣款 -> 后台结算派彩 -> 钱包入账 -> 查询订单、余额、流水和释放记录
```

v1 只做后台模拟联调，不接真实游戏厂商、不处理游戏回调签名、不做注单撤销、不做复杂输赢算法。

## 2. 模块边界

新增 `gameluck-game` 模块，负责模拟游戏注单状态、下注金额、派彩金额和钱包交易号。

`gameluck-game` 只能通过 `IWalletCoreService` 调用钱包：

- 下注扣款调用 `debit`
- 派彩入账调用 `credit`
- 不允许直接更新 `gl_wallet_account`
- 不允许直接插入钱包流水

## 3. 状态机

| 状态 | 含义 | 允许动作 |
| --- | --- | --- |
| PENDING | 待下注 | 执行下注扣款 |
| BET_SUCCESS | 已扣款，待结算 | 执行派彩结算 |
| BET_FAILED | 扣款失败 | 仅查询 |
| SETTLED | 已结算 | 仅查询 |
| SETTLE_FAILED | 派彩失败 | 仅查询，后续版本再做重试 |

v1 不做 `CANCELLED`，避免下注撤销带来冲正和回滚规则扩散。

## 4. 钱包调用约定

下注扣款：

```text
operation = DEBIT
sourceType = GAME_BET
businessNo = betOrderNo
idempotencyKey = game:bet:{betOrderNo}
```

派彩入账：

```text
operation = CREDIT
sourceType = GAME_PROFIT
businessNo = betOrderNo
idempotencyKey = game:settle:{betOrderNo}
```

`GAME_PROFIT` 的释放规则继续由钱包规则中心决定。当前 SQL 已有：

```text
SC + GAME_PROFIT -> AFTER_TURNOVER
GC + GAME_PROFIT -> NEVER
```

v1 默认使用 `SC`，便于验证“游戏收益需要流水释放”的场景。

## 5. 数据表

新增表：`gl_game_bet_order`

核心字段：

```text
id
tenant_id
bet_order_no
member_id
currency_code
game_code
round_no
bet_amount
payout_amount
net_amount
status
bet_wallet_transaction_no
settle_wallet_transaction_no
bet_idempotency_key
settle_idempotency_key
bet_time
settle_time
fail_reason
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
tenant_id + bet_order_no
tenant_id + bet_idempotency_key
tenant_id + settle_idempotency_key
```

## 6. 后台页面

新增菜单：

```text
游戏交易 / 模拟下注订单
```

页面能力：

- 查询：注单号、会员 ID、币种、游戏编码、局号、状态、创建时间
- 新增模拟下注订单
- 对 `PENDING` 订单执行“模拟下注扣款”
- 对 `BET_SUCCESS` 订单执行“模拟结算派彩”
- 查看详情

高风险动作：

- “模拟下注扣款”必须二次确认
- “模拟结算派彩”必须二次确认
- 状态不允许前端绕过，后端必须校验

## 7. 后台接口

```http
GET /game/bet/list
GET /game/bet/{id}
POST /game/bet
POST /game/bet/{id}/place
POST /game/bet/{id}/settle
```

权限标识：

```text
game:bet:list
game:bet:query
game:bet:add
game:bet:place
game:bet:settle
```

## 8. 验收标准

- 后台可以创建一笔 `SC` 模拟下注订单。
- `PENDING` 订单可以执行下注扣款。
- 扣款成功后订单变为 `BET_SUCCESS`，记录下注钱包交易号。
- 钱包账户余额减少下注金额。
- 钱包流水出现 `DEBIT / GAME_BET / SUCCESS`。
- `BET_SUCCESS` 订单可以执行派彩结算。
- 结算成功后订单变为 `SETTLED`，记录派彩钱包交易号。
- 钱包账户余额增加派彩金额。
- 钱包流水出现 `CREDIT / GAME_PROFIT / SUCCESS`。
- 同一订单重复下注或重复结算不会重复扣款或重复派彩。
- 后端编译通过，前端构建通过，菜单图标校验通过。

## 9. 本阶段不做

- 真实游戏厂商接入
- 游戏回调验签
- 注单撤销或冲正
- 批量结算
- 派彩重试
- C 端游戏页面
