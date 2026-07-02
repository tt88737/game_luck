# 模拟游戏下注取消退款 v2 设计

## 1. 目标

本阶段在 v1 的“模拟下注扣款 + 派彩入账”基础上，补齐后台取消退款闭环：

```text
BET_SUCCESS 订单 -> 后台取消退款 -> 钱包入账退回下注金额 -> 订单变为 CANCELLED
```

v2 只处理“已扣款、未结算”的模拟注单取消，不处理已结算订单冲正，不处理真实游戏厂商回调，不处理批量取消。

## 2. 范围

本阶段包含：

- 后端订单状态新增 `CANCELLED`
- 后端订单表新增退款相关字段
- 后端新增取消退款服务方法和接口
- 钱包退款必须通过 `IWalletCoreService.credit`
- 后台页面新增“取消退款”按钮、状态展示、详情字段
- SQL 菜单新增 `game:bet:cancel` 权限

本阶段不包含：

- `SETTLED` 订单冲正
- `SETTLE_FAILED` 派彩重试
- `PENDING` 订单关闭
- 真实游戏厂商取消回调
- 批量退款
- 人工审核流

## 3. 状态机

现有状态：

| 状态 | 含义 | v2 允许动作 |
| --- | --- | --- |
| PENDING | 待下注 | 模拟下注扣款 |
| BET_SUCCESS | 已扣款，未结算 | 模拟结算派彩；取消退款 |
| BET_FAILED | 扣款失败 | 仅查询 |
| SETTLED | 已结算 | 仅查询 |
| SETTLE_FAILED | 结算失败 | 仅查询 |
| CANCELLED | 已取消退款 | 仅查询 |

新增流转：

```text
BET_SUCCESS -> CANCELLED
```

禁止流转：

```text
PENDING -> CANCELLED
SETTLED -> CANCELLED
BET_FAILED -> CANCELLED
SETTLE_FAILED -> CANCELLED
CANCELLED -> BET_SUCCESS
CANCELLED -> SETTLED
```

## 4. 钱包调用约定

取消退款使用钱包入账能力，不能直接修改 `gl_wallet_account`。

```text
operation = CREDIT
sourceType = GAME_REFUND
businessNo = betOrderNo
amount = betAmount
idempotencyKey = game:refund:{betOrderNo}
remark = Simulated game bet refund
```

退款金额固定等于原下注金额 `betAmount`。本阶段不允许前端输入退款金额，避免出现部分退款和人工金额错误。

`GAME_REFUND` 的钱包释放规则需要写入规则中心。建议默认：

```text
SC + GAME_REFUND -> IMMEDIATE
```

原因：退款是返还本金，不应产生新的流水锁定或审核。

## 5. 数据结构

修改表：`gl_game_bet_order`

新增字段：

```text
refund_wallet_transaction_no varchar(64)  退款钱包交易号
refund_idempotency_key       varchar(128) 退款幂等键
cancel_time                  datetime     取消退款时间
```

新增唯一约束：

```text
tenant_id + refund_idempotency_key
```

字段写入规则：

- 创建订单时生成 `refundIdempotencyKey = game:refund:{betOrderNo}`
- 取消退款成功后写入 `refundWalletTransactionNo`
- 取消退款成功后写入 `cancelTime`
- 取消退款成功后状态改为 `CANCELLED`

## 6. 后端接口

新增接口：

```http
POST /game/bet/{id}/cancel
```

权限标识：

```text
game:bet:cancel
```

服务方法：

```java
GameBetOrderVo cancel(Long id);
```

接口行为：

- 加行锁读取订单
- 仅允许 `BET_SUCCESS`
- 调用 `IWalletCoreService.credit`
- 钱包交易 `SUCCESS` 后订单改为 `CANCELLED`
- 重复取消 `CANCELLED` 订单时直接返回订单，不重复退款
- 其他状态调用取消接口时抛出业务异常

## 7. 后台页面

页面仍使用现有“模拟下注订单”列表，不新增菜单页面。

列表增强：

- 状态筛选增加 `CANCELLED`
- 状态标签增加“已取消”
- 交易号列增加退款交易号
- `BET_SUCCESS` 行显示“取消退款”图标按钮
- 操作按钮必须有 tooltip
- 取消退款必须二次确认

详情增强：

- 展示退款交易号
- 展示退款幂等键
- 展示取消时间

## 8. 菜单与图标约束

新增按钮权限菜单：

```text
menu_id = 1935
parent_id = 1921
menu_name = 模拟下注取消退款
perms = game:bet:cancel
icon = #
menu_type = F
```

约束：

- 目录图标必须使用 `admin-ui/src/assets/icons/svg` 中存在的图标
- 按钮菜单图标必须为 `#`
- 每次构建前必须通过 `pnpm --dir admin-ui check:menu-icons`

## 9. 验收标准

准备一笔 `SC` 余额后：

1. 创建一笔 `SC` 模拟下注订单
2. 执行下注扣款，订单变为 `BET_SUCCESS`
3. 执行取消退款，订单变为 `CANCELLED`
4. 钱包流水出现：

```text
DEBIT  / GAME_BET    / SUCCESS / betAmount
CREDIT / GAME_REFUND / SUCCESS / betAmount
```

5. 钱包余额最终等于下注前余额
6. 重复调用取消接口不会重复入账
7. `SETTLED` 订单调用取消接口返回业务错误
8. 后端编译通过
9. 前端生产构建通过
10. 菜单图标守卫通过

验收 SQL：

```sql
select bet_order_no, member_id, currency_code, bet_amount, payout_amount, status,
       bet_wallet_transaction_no, settle_wallet_transaction_no, refund_wallet_transaction_no
from gl_game_bet_order
order by create_time desc
limit 5;

select business_no, operation, source_type, amount, status
from gl_wallet_transaction
where source_type in ('GAME_BET', 'GAME_PROFIT', 'GAME_REFUND')
order by create_time desc
limit 10;

select member_id, currency_code, available_balance
from gl_wallet_account
where currency_code = 'SC'
order by update_time desc
limit 5;
```

## 10. 自检

- 本规格只覆盖 `BET_SUCCESS -> CANCELLED`，没有引入已结算冲正。
- 退款金额固定使用 `betAmount`，不开放前端输入金额。
- 退款通过钱包核心服务，符合钱包中心约束。
- 后台只新增按钮和字段，不新增页面。
- 菜单图标约束沿用现有 `check:menu-icons` 守卫。
