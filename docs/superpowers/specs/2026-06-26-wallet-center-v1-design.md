# 钱包中心 v1 设计

## 1. 文档信息

| 项目 | 内容 |
| --- | --- |
| 文档日期 | 2026-06-26 |
| 所属系统 | GameLuck 包网平台 |
| 模块 | wallet-center |
| 阶段 | Phase 1 / 钱包账务内核 |

## 2. 设计结论

钱包中心采用“多币种规则驱动 + 业务规则外置”的设计。

钱包中心负责账务事实、余额控制、幂等、冻结、释放状态和审计，不负责计算充值流水倍数、活动流水倍数、游戏盈利是否可兑换、代理佣金是否可提等业务策略。

业务中心在调用钱包前完成规则计算，并把结果作为入账属性传给钱包。钱包只按传入属性记账、锁定、释放和校验。

## 3. 系统边界

### 3.1 钱包中心负责

- 币种基础能力配置。
- 会员多币种账户。
- 可用余额、冻结余额、账务流水。
- 入账、扣账、冻结、解冻、结算冻结金额。
- 幂等防重复。
- 可提现 / 可兑换释放状态。
- 流水完成进度记录。
- 人工调整和异常处理审计。

### 3.2 钱包中心不负责

- 充值几倍流水。
- 活动奖励几倍流水。
- SC 哪些来源允许兑换。
- RC 充值本金是否免流水。
- 哪个游戏投注算有效流水。
- 哪个地区、设备、会员标签触发风控。
- 提现审核和打款。

这些规则属于充值中心、活动中心、游戏中心、风控中心、提现中心。

## 4. 多币种模型

系统默认初始化三种币种，但底层不写死：

| 币种 | 默认定位 | 默认能力 |
| --- | --- | --- |
| GC | Gold Coin / 金币 | 默认不可提现，可配置是否充值、投注、兑换 |
| SC | Sweep Coin / 奖励币 | 默认可兑换，但释放条件由业务传入 |
| RC | Real Cash / 真金 | 默认可提现，但提现条件由业务传入 |

后期新增币种时，只新增币种配置和业务侧规则，不改钱包核心账务代码。

## 5. 币种基础能力

钱包只保存币种底层能力，不保存业务倍数。

建议字段：

```text
currency_code
currency_name
scale
enabled
credit_enabled
debit_enabled
freeze_enabled
withdraw_enabled
exchange_enabled
negative_allowed
sort_order
remark
```

说明：

- `withdraw_enabled` 表示该币种是否具备提现能力。
- `exchange_enabled` 表示该币种是否具备兑换能力。
- 是否满足提现 / 兑换条件，不由这里判断。
- 流水倍数不放在钱包币种配置中。

## 6. 入账属性

业务系统调用钱包入账时，必须传入本次入账的释放属性。

核心字段：

```text
tenant_id
member_id
currency_code
amount
source_type
business_no
idempotency_key
release_mode
required_turnover
metadata
```

`release_mode`：

| 值 | 含义 |
| --- | --- |
| IMMEDIATE | 立即可提现 / 可兑换 |
| AFTER_TURNOVER | 满足流水后可提现 / 可兑换 |
| NEVER | 永不可提现 / 兑换 |
| MANUAL_REVIEW | 需要人工审核后释放 |

`required_turnover` 是业务系统计算后的结果，不是钱包通过倍数计算出来的结果。

示例：

```text
充值中心：充值 100 RC，规则计算 required_turnover = 100，release_mode = AFTER_TURNOVER
活动中心：赠送 20 SC，规则计算 required_turnover = 200，release_mode = AFTER_TURNOVER
游戏中心：盈利 50 SC，规则计算 required_turnover = 0，release_mode = IMMEDIATE
后台赠送：赠送 1000 GC，规则计算 required_turnover = 0，release_mode = NEVER
```

## 7. 流水进度

钱包可以记录和更新流水完成进度，但有效流水由业务系统判断。

游戏投注完成后，游戏中心或风控中心调用钱包：

```text
member_id
currency_code
valid_turnover_amount
business_no
idempotency_key
```

钱包根据 `valid_turnover_amount` 更新待释放记录的 `completed_turnover`。

当：

```text
completed_turnover >= required_turnover
```

钱包将对应金额从待释放状态改为可提现 / 可兑换状态。

## 8. 可释放记录

每笔可能影响提现或兑换的入账，都应生成可释放记录。

建议字段：

```text
release_no
tenant_id
member_id
currency_code
source_type
business_no
amount
released_amount
required_turnover
completed_turnover
release_mode
release_status
metadata
```

`release_status`：

| 值 | 含义 |
| --- | --- |
| RELEASED | 已释放，可提现 / 可兑换 |
| LOCKED | 待流水或待条件满足 |
| NEVER | 永不可释放 |
| REVIEWING | 人工审核中 |
| REJECTED | 审核拒绝 |
| CONSUMED | 已被提现 / 兑换消费 |

提现或兑换时，只能使用 `RELEASED` 且未消费的金额。

## 9. 余额模型

账户余额建议保持简单稳定：

```text
available_balance
frozen_balance
version
status
```

不要在账户表写死：

```text
gc_balance
sc_balance
rc_balance
sc_withdrawable
rc_turnover
```

可提现 / 可兑换金额从释放记录汇总得到，或后期通过汇总表优化。

账户唯一约束：

```text
tenant_id + member_id + currency_code
```

## 10. 账变流水

所有余额变化必须写账变流水。

建议来源枚举：

```text
DEPOSIT
GAME_BET
GAME_WIN
GAME_REFUND
PROMOTION
TASK_REWARD
MANUAL_GRANT
MANUAL_DEDUCT
EXCHANGE_IN
EXCHANGE_OUT
WITHDRAW_FREEZE
WITHDRAW_SUCCESS
WITHDRAW_REJECT
ADJUSTMENT
REVERSAL
```

建议方向枚举：

```text
CREDIT
DEBIT
FREEZE
UNFREEZE
SETTLE
ADJUST
REVERSE
```

## 11. 提现和兑换边界

提现中心不重新计算历史规则。

提现中心只向钱包查询：

```text
当前可提现金额
当前可兑换金额
当前冻结金额
本次申请是否可冻结
```

提现申请通过后：

1. 提现中心请求钱包冻结。
2. 审核通过后请求钱包结算冻结金额。
3. 审核拒绝后请求钱包解冻。

钱包不负责打款，也不负责提现审核。

## 12. 幂等规则

所有改变余额、冻结、释放状态的接口必须带：

```text
tenant_id
idempotency_key
```

唯一约束：

```text
tenant_id + idempotency_key
```

重复请求处理：

- 原请求成功：返回原成功结果。
- 原请求失败：返回原失败结果。
- 原请求处理中：返回处理中。
- 关键参数不一致：返回幂等冲突。

关键参数包括：

```text
tenant_id
member_id
currency_code
amount
source_type
business_no
operation
```

## 13. 数据表建议

第一阶段建议表：

```text
gl_wallet_currency
gl_wallet_account
gl_wallet_transaction
gl_wallet_release
gl_wallet_freeze
```

第二阶段再补：

```text
gl_wallet_manual_review
gl_wallet_daily_snapshot
gl_wallet_reconciliation
```

## 14. 第一阶段后台页面

第一版后台只做查询和基础配置，不开放高风险人工改账。

页面：

- 币种配置。
- 会员钱包账户。
- 账变流水。
- 释放记录。
- 冻结记录。

暂不做：

- 人工加款。
- 人工扣款。
- 提现审核。
- 活动规则配置。
- 充值规则配置。

## 15. 验收标准

第一阶段完成后应满足：

- 支持 GC / SC / RC 三种默认币种。
- 支持后续新增币种，不改账户和流水表结构。
- 支持入账时传入 `release_mode` 和 `required_turnover`。
- 支持投注后更新有效流水进度。
- 支持查询会员各币种余额、冻结金额、释放状态。
- 支持提现 / 兑换前查询可释放金额。
- 所有余额变动都有账变流水。
- 所有余额变动接口具备幂等能力。

## 16. 关键约束

- 钱包中心是账务内核，不是业务规则中心。
- 流水倍数属于业务策略，不属于钱包基础配置。
- 业务系统必须在调用钱包前计算好释放属性。
- 提现系统只消费钱包的可释放结果，不重算历史规则。
- 任何模块不得绕过钱包中心直接修改余额。
- 账变流水和释放记录不得物理删除。
