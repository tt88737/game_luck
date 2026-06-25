# 数据库草案

## 1. 设计原则

- 所有业务表必须带 `tenant_id`，平台级配置表除外。
- 金额字段使用整数最小单位或 `decimal(24, 8)`，具体在落库前统一。
- 钱包流水不可物理删除。
- 真金、兑换、冻结、人工调账必须可审计。
- 账务变更必须有业务单号和幂等键。

## 2. 平台和租户

### 2.1 `tenant_brand`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | 主键 |
| tenant_id | bigint | 租户 ID |
| brand_code | varchar(64) | 品牌编码 |
| brand_name | varchar(128) | 品牌名称 |
| domain | varchar(255) | 主域名 |
| logo_url | varchar(512) | Logo |
| theme_code | varchar(64) | 主题编码 |
| status | varchar(32) | enabled / disabled |
| created_at | datetime | 创建时间 |
| updated_at | datetime | 更新时间 |

### 2.2 `tenant_channel_config`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | 主键 |
| tenant_id | bigint | 租户 ID |
| channel_code | varchar(32) | h5 / pwa / android_apk / google_play / ios_appstore |
| feature_key | varchar(64) | 功能键 |
| enabled | tinyint | 是否启用 |
| config_json | json | 扩展配置 |
| updated_at | datetime | 更新时间 |

## 3. 钱包

### 3.1 `wallet_currency_config`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | 主键 |
| currency_code | varchar(32) | GC / SC / RC / BONUS |
| currency_name | varchar(64) | 币种名称 |
| currency_type | varchar(32) | virtual / sweepstakes / cash / bonus |
| decimal_scale | int | 小数位 |
| platform_enabled | tinyint | 平台是否启用 |
| created_at | datetime | 创建时间 |
| updated_at | datetime | 更新时间 |

### 3.2 `tenant_currency_config`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | 主键 |
| tenant_id | bigint | 租户 ID |
| currency_code | varchar(32) | 币种编码 |
| enabled | tinyint | 是否启用 |
| rechargeable | tinyint | 是否可充值 |
| withdrawable | tinyint | 是否可兑换 / 提现 |
| playable | tinyint | 是否可下注 |
| bonus | tinyint | 是否奖励币 |
| min_withdraw_amount | decimal(24,8) | 最小兑换金额 |
| max_withdraw_amount | decimal(24,8) | 最大兑换金额 |
| config_json | json | 扩展规则 |
| updated_at | datetime | 更新时间 |

### 3.3 `member_wallet_account`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | 主键 |
| tenant_id | bigint | 租户 ID |
| member_id | bigint | 会员 ID |
| currency_code | varchar(32) | 币种编码 |
| available_balance | decimal(24,8) | 可用余额 |
| frozen_balance | decimal(24,8) | 冻结余额 |
| status | varchar(32) | normal / frozen |
| version | bigint | 乐观锁版本 |
| created_at | datetime | 创建时间 |
| updated_at | datetime | 更新时间 |

唯一约束：

```text
tenant_id + member_id + currency_code
```

### 3.4 `member_wallet_ledger`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | 主键 |
| tenant_id | bigint | 租户 ID |
| member_id | bigint | 会员 ID |
| currency_code | varchar(32) | 币种编码 |
| account_id | bigint | 钱包账户 ID |
| direction | varchar(16) | credit / debit / freeze / unfreeze / settle |
| amount | decimal(24,8) | 变动金额 |
| before_available | decimal(24,8) | 变动前可用余额 |
| after_available | decimal(24,8) | 变动后可用余额 |
| before_frozen | decimal(24,8) | 变动前冻结余额 |
| after_frozen | decimal(24,8) | 变动后冻结余额 |
| biz_type | varchar(64) | payment / game_bet / game_payout / promotion / redemption / adjustment |
| biz_no | varchar(128) | 业务单号 |
| idempotency_key | varchar(128) | 幂等键 |
| remark | varchar(512) | 备注 |
| created_at | datetime | 创建时间 |

唯一约束：

```text
tenant_id + idempotency_key
```

### 3.5 `wallet_transaction`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | 主键 |
| tenant_id | bigint | 租户 ID |
| transaction_no | varchar(128) | 钱包交易单号 |
| idempotency_key | varchar(128) | 幂等键 |
| member_id | bigint | 会员 ID |
| currency_code | varchar(32) | 币种 |
| operation | varchar(32) | credit / debit / freeze / unfreeze / settle / adjust / reverse |
| amount | decimal(24,8) | 金额 |
| status | varchar(32) | pending / success / failed / reversed |
| biz_type | varchar(64) | 业务类型 |
| biz_no | varchar(128) | 业务单号 |
| origin_transaction_no | varchar(128) | 原交易单号，冲正时使用 |
| request_hash | varchar(128) | 请求关键字段哈希 |
| fail_code | varchar(64) | 失败码 |
| fail_reason | varchar(512) | 失败原因 |
| created_at | datetime | 创建时间 |
| updated_at | datetime | 更新时间 |

唯一约束：

```text
tenant_id + idempotency_key
tenant_id + transaction_no
```

### 3.6 `wallet_freeze_record`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | 主键 |
| tenant_id | bigint | 租户 ID |
| member_id | bigint | 会员 ID |
| currency_code | varchar(32) | 币种编码 |
| freeze_no | varchar(128) | 冻结单号 |
| amount | decimal(24,8) | 冻结金额 |
| status | varchar(32) | frozen / settled / released |
| source_type | varchar(64) | redemption / risk / manual |
| source_no | varchar(128) | 来源单号 |
| created_at | datetime | 创建时间 |
| updated_at | datetime | 更新时间 |

### 3.7 `wallet_manual_review`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | 主键 |
| tenant_id | bigint | 租户 ID |
| review_no | varchar(128) | 人工处理单号 |
| source_type | varchar(64) | reverse / adjust / reconciliation |
| source_no | varchar(128) | 来源单号 |
| reason | varchar(512) | 原因 |
| status | varchar(32) | pending / processing / resolved / rejected |
| created_at | datetime | 创建时间 |
| updated_at | datetime | 更新时间 |

## 4. 游戏

### 4.1 `game_provider`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | 主键 |
| tenant_id | bigint | 租户 ID |
| provider_code | varchar(64) | 供应商编码 |
| provider_name | varchar(128) | 供应商名称 |
| status | varchar(32) | enabled / disabled |
| config_json | json | 接入配置 |

### 4.2 `game_catalog`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | 主键 |
| tenant_id | bigint | 租户 ID |
| provider_code | varchar(64) | 供应商编码 |
| game_code | varchar(64) | 游戏编码 |
| game_name | varchar(128) | 游戏名称 |
| supported_currencies | json | 支持币种 |
| status | varchar(32) | enabled / maintenance / disabled |

### 4.3 `game_round`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | 主键 |
| tenant_id | bigint | 租户 ID |
| member_id | bigint | 会员 ID |
| provider_code | varchar(64) | 供应商编码 |
| game_code | varchar(64) | 游戏编码 |
| round_no | varchar(128) | 局号 |
| currency_code | varchar(32) | 币种 |
| bet_amount | decimal(24,8) | 下注金额 |
| payout_amount | decimal(24,8) | 派彩金额 |
| status | varchar(32) | betting / settled / cancelled |
| created_at | datetime | 创建时间 |
| updated_at | datetime | 更新时间 |

## 5. 兑换

### 5.1 `redemption_order`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | 主键 |
| tenant_id | bigint | 租户 ID |
| member_id | bigint | 会员 ID |
| redemption_no | varchar(128) | 兑换单号 |
| currency_code | varchar(32) | 币种 |
| amount | decimal(24,8) | 兑换金额 |
| freeze_no | varchar(128) | 冻结单号 |
| status | varchar(32) | pending / approved / rejected / paid / failed |
| reject_reason | varchar(512) | 拒绝原因 |
| created_at | datetime | 创建时间 |
| updated_at | datetime | 更新时间 |

## 6. 审计

### 6.1 `audit_operation_log`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | 主键 |
| tenant_id | bigint | 租户 ID |
| operator_id | bigint | 操作人 ID |
| operator_type | varchar(32) | admin / system |
| action | varchar(128) | 操作动作 |
| target_type | varchar(64) | 目标类型 |
| target_id | varchar(128) | 目标 ID |
| before_json | json | 变更前 |
| after_json | json | 变更后 |
| ip | varchar(64) | IP |
| created_at | datetime | 创建时间 |
