# C端购买闭环设计

日期：2026-07-16

## 1. 目标

在现有购买产品底座上，补齐 C 端最小购买闭环：

- C 端可以看到后台启用的购买产品。
- 用户可以选择一个产品并发起购买。
- 当前阶段不接真实支付网关，使用“模拟支付成功”完成履约。
- 履约后生成购买订单、发放快照、钱包账变、释放记录和流水任务。
- H5 页面能展示购买结果，并能通过钱包页看到余额变化。

这个阶段的重点不是支付通道，而是验证模型是否正确：`购买产品 -> 购买订单 -> 发放快照 -> 钱包入账 -> 流水义务`。

## 2. 范围

### 本阶段做

- 新增 C 端购买接口：
  - `GET /api/client/purchase/offers`
  - `POST /api/client/purchase/orders/pay`
- 新增 C 端购买服务 `ClientPurchaseService`。
- 新增购买订单 Mapper。
- 支持后台已启用购买产品的 C 端展示。
- 支持登录用户模拟购买成功。
- 支持幂等键，避免重复点击生成重复入账。
- 支持购买成功后多发放项逐笔入账。
- 支持 GC 无流水、SC 赠送按倍数或固定金额生成流水义务。
- H5 新增购买/商店页面。
- H5 导航新增“购买”入口。

### 本阶段不做

- 不接 Stripe、PayPal、MoonPay、加密支付等真实支付通道。
- 不处理支付回调签名。
- 不做支付失败、退款、拒付、风控审核流。
- 不做复杂优惠叠加。
- 不做真实金额扣款。
- 不做 C 端购物车。

这些能力要在购买闭环稳定后单独设计。

## 3. 产品边界

购买产品属于 payment/purchase 域，不属于 promotion 域。

后台运营配置：

- 产品名称
- 支付币种和金额
- 发放项
- 每个发放项的流水要求
- 游戏核销范围
- 用户范围、地区范围、限购方式、启停状态

C 端展示业务语言，不展示技术字段：

- 支付 `$10`
- 获得 `10,000 GC`
- 赠送 `1 SC`
- `SC 需完成 10x 游戏流水后可兑换`
- `GC 当前不可提款或兑换`

C 端不展示：

- `fundPropertyCode`
- `sourceType`
- `releaseMode`
- `turnoverTaskNo`
- 内部系统 ID

## 4. 后端设计

### 4.1 新增对象

`ClientPurchaseOfferVo`

用于 C 端购买产品卡片。

字段：

| 字段 | 说明 |
| --- | --- |
| `offerId` | 购买产品 ID |
| `offerNo` | 产品编号 |
| `offerName` | 产品名称 |
| `offerType` | 产品类型 |
| `payCurrencyCode` | 支付币种 |
| `payAmount` | 支付金额 |
| `grantItems` | C 端发放项展示 |
| `limitText` | 限购说明 |
| `wageringText` | 流水说明 |

`ClientPurchaseGrantItemVo`

字段：

| 字段 | 说明 |
| --- | --- |
| `grantType` | 发放类型 |
| `currencyCode` | 发放币种 |
| `grantAmount` | 发放金额 |
| `wageringMode` | 流水模式 |
| `requiredTurnover` | 所需流水金额 |
| `wageringMultiplier` | 流水倍数 |
| `gameScopeType` | 可核销游戏范围类型 |
| `gameScopeValue` | 可核销游戏范围值 |

`ClientPurchasePayBo`

字段：

| 字段 | 说明 |
| --- | --- |
| `offerId` | 购买产品 ID |
| `idempotencyKey` | C 端幂等键 |

`ClientPurchaseOrderVo`

字段：

| 字段 | 说明 |
| --- | --- |
| `orderId` | 订单 ID |
| `orderNo` | 订单编号 |
| `offerId` | 产品 ID |
| `offerName` | 产品名称 |
| `payCurrencyCode` | 支付币种 |
| `payAmount` | 支付金额 |
| `status` | 订单状态 |
| `grantItems` | 本次发放快照 |
| `createdAt` | 创建时间 |
| `creditedAt` | 入账时间 |

### 4.2 订单状态

沿用 `gl_purchase_order.status`：

| 状态 | 含义 |
| --- | --- |
| `PENDING` | 已创建，待支付 |
| `PAID` | 支付成功，待入账 |
| `CREDITED` | 已完成钱包入账 |
| `FAILED` | 履约失败 |
| `CANCELLED` | 已取消 |

本阶段模拟支付成功，正常路径为：

`PENDING -> PAID -> CREDITED`

如果钱包入账失败：

`PENDING -> PAID -> FAILED`

失败原因写入 `fail_reason`。

### 4.3 购买履约流程

`POST /api/client/purchase/orders/pay` 的处理流程：

1. 从 Authorization 解析 C 端会员。
2. 校验 `offerId` 和 `idempotencyKey`。
3. 查询购买产品，要求：
   - `tenant_id = 当前租户`
   - `status = '0'`
   - `del_flag = '0'`
   - 当前时间在 `start_time/end_time` 范围内，空值表示不限
4. 查询产品发放项，要求至少一条。
5. 根据 `idempotencyKey` 查询是否已有订单：
   - 如果已有且属于当前会员，直接返回已有订单结果。
   - 如果已有但参数不一致，返回幂等冲突。
6. 创建 `gl_purchase_order`，状态 `PENDING`。
7. 本阶段立即模拟支付成功，将订单更新为 `PAID`。
8. 调用已有 `snapshotPaidOrderGrants()` 生成订单发放快照和 `WalletCreditBo`。
9. 逐笔调用 `IWalletCoreService.credit()`。
10. 将每个钱包交易号写回对应发放快照。
11. 全部成功后订单更新为 `CREDITED`。
12. 返回 `ClientPurchaseOrderVo`。

事务要求：

- 订单、快照、钱包入账必须在同一事务中完成。
- 任意一笔入账失败，订单置为 `FAILED`，并抛出业务异常。
- 钱包核心本身有幂等保护，购买服务也要用订单幂等键保护入口。

### 4.4 流水计算

沿用购买产品发放项配置：

| 模式 | 规则 |
| --- | --- |
| `NONE` | 所需流水为 0 |
| `FIXED` | 所需流水 = 固定流水金额 |
| `MULTIPLIER` | 所需流水 = 发放金额 * 流水倍数 |
| `COMBINED_MULTIPLIER` | 本阶段不开放，后端拒绝 |

钱包入账时：

- `requiredTurnover = 0` 时，释放记录应为可释放状态。
- `requiredTurnover > 0` 时，释放记录应锁定，并生成流水任务。

### 4.5 限购和范围

第一阶段只严格处理：

- 产品启停
- 生效时间
- 地区/人群字段保留在返回值或查询条件中，但不做复杂过滤

`purchaseLimitType` 的深度执行暂缓：

- `FIRST_ONLY`
- `DAILY_ONCE`
- `TOTAL_ONCE`
- `PERIOD_LIMIT`

原因：限购需要用户购买历史、支付失败订单、退款订单、渠道差异一起定义。本阶段先验证购买履约闭环，不提前固化复杂限购。

## 5. H5 设计

### 5.1 页面

新增页面：

`/purchase`

导航名称：

`购买`

页面主要区域：

- 顶部余额摘要：GC、SC
- 购买产品列表
- 产品卡片：
  - 支付金额
  - 获得 GC
  - 赠送 SC
  - 流水说明
  - 限购说明
  - 购买按钮
- 购买结果提示：
  - 订单号
  - 本次发放内容
  - 余额可去钱包查看

### 5.2 页面状态

必须覆盖：

- 未登录：提示登录后购买，按钮跳转登录
- 加载中：产品和余额加载状态
- 空数据：暂无可购买产品
- 购买中：按钮禁用，防止重复点击
- 购买成功：展示订单号和发放结果
- 购买失败：展示后端业务错误
- 钱包加载失败：购买产品仍可展示，但余额区域提示失败

### 5.3 文案

页面不解释技术字段，只解释用户关心的结果：

- `支付 $10`
- `获得 10,000 GC`
- `赠送 1 SC`
- `SC 需完成 10x 游戏流水后可兑换`
- `GC 当前不可提款或兑换`
- `购买成功，奖励已发放到钱包`

## 6. 验收标准

1. 后台启用购买产品后，H5 `/purchase` 能展示该产品。
2. 未登录用户不能购买，会看到登录引导。
3. 登录用户点击购买后，生成一条 `gl_purchase_order`。
4. 购买成功后订单状态为 `CREDITED`。
5. 生成对应 `gl_purchase_order_grant_snapshot`。
6. GC 发放进入钱包余额，默认不生成锁定流水。
7. SC 赠送进入钱包余额，并按配置生成流水义务。
8. 重复提交同一个 `idempotencyKey` 不重复入账。
9. H5 钱包页能看到购买后的余额变化。
10. C 端接口和 H5 页面不暴露 `fundPropertyCode`。

## 7. 风险和后续扩展

| 风险 | 当前处理 |
| --- | --- |
| 真实支付回调重复 | 本阶段先用订单幂等和钱包幂等验证基础能力，真实支付阶段再接回调签名 |
| 限购规则复杂 | 保留字段，后续单独实现限购引擎 |
| 支付失败/退款 | 本阶段不做，后续支付订单模块扩展 |
| 地区和人群过滤 | 字段已存在，后续接 geo/risk/user segment |
| 多币种支付 | 当前字段支持，第一阶段用 USD/GC/SC 示例验证 |

## 8. 自查

- 无未完成占位内容。
- 没有把购买逻辑放回 promotion。
- 没有让 C 端或普通运营理解 `fundPropertyCode`。
- 第一阶段范围可独立验收。
- 后续真实支付、限购、退款、风控均有扩展位置。
