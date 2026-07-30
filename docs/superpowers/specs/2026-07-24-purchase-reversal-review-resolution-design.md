# Purchase Reversal Review Resolution Design

## Goal

为 `REFUND_REVIEW` 和 `CHARGEBACK_REVIEW` 追偿案件提供独立的后台审核工作台，让审核员能够在余额补足后再次执行全额追偿，或确认每币种损失并结案，同时保持钱包、订单、流水任务、会员风险和操作日志可审计。

## Scope

本阶段包括：

- 支付中心新增“拒付审核”工作台，同时处理退款追偿和拒付追偿待复核案件。
- 支持“再次全额追偿”和“确认损失并结案”两种处置。
- 新增独立处置状态、审核字段、重试字段和操作审计表。
- 继续使用钱包原子多币种扣款，不允许负余额或部分扣款。
- 增加独立菜单和查询、重试、确认损失权限。
- 在购买订单详情提供前往审核入口，但不重复放置处置按钮。
- 完成数据库、后端、Admin、自动化测试和运行时验收。

本阶段不包括自动定时重试、外部支付渠道交互、钱包负余额、跨币种损失汇总或汇率换算、自动解除拒付风险，以及通用资金审核中心。

## Core Decisions

### Separate Review Workbench

新增独立“拒付审核”菜单，而不是把审核按钮继续堆叠在购买订单详情。工作台复用现有追偿单、明细、购买订单、支付事件和钱包服务，不复制账务模型。菜单名称沿用业务人员常用的“拒付审核”，列表同时包含 `REFUND` 和 `CHARGEBACK`，并明确展示追偿类型。

### No Cross-Currency Loss Total

GC、SC 等币种不可直接相加。确认损失时，以每条追偿明细的最终 `shortfall_amount` 作为损失依据，主单不保存跨币种损失合计。界面以 `GC 100 / SC 1` 形式展示短缺摘要。

### Preserve Original Event Boundary

原支付事件的 `event_key` 继续作为首次退款或拒付事件的幂等边界。人工审核处置使用独立请求键和操作日志，不修改、伪造或重复提交原支付事件。

## Domain Model

### Disposition Status

在 `gl_purchase_reversal` 新增 `disposition_status`：

| Value | Meaning |
| --- | --- |
| `PENDING_REVIEW` | 待人工复核，可执行处置 |
| `RECOVERY_COMPLETED` | 人工再次全额追偿成功 |
| `LOSS_ACCEPTED` | 已确认剩余短缺为损失并结案 |

现有 `status` 保留首次自动追偿执行结果。首次足额追偿仍为 `COMPLETED`，无需进入工作台；首次不足为 `REVIEW_REQUIRED / PENDING_REVIEW`；人工追偿成功后为 `COMPLETED / RECOVERY_COMPLETED`；确认损失后保持 `REVIEW_REQUIRED / LOSS_ACCEPTED`。

### Reversal Review Fields

`gl_purchase_reversal` 新增：

| Field | Purpose |
| --- | --- |
| `disposition_status` | 人工处置状态 |
| `reviewed_by` | 最终处置人用户 ID |
| `reviewed_name` | 最终处置人显示名称快照 |
| `review_note` | 最终审核意见，确认损失时必填 |
| `resolved_time` | 最终结案时间 |
| `retry_count` | 人工重试次数，包括余额仍不足的尝试 |
| `last_retry_time` | 最近一次重试时间 |
| `version` | 乐观锁版本；数据库行锁仍是主要并发边界 |

历史 `REVIEW_REQUIRED` 数据通过幂等迁移补齐 `PENDING_REVIEW`。已完成追偿不进入人工审核队列。

### Operation Audit

新增 `gl_purchase_reversal_review_log`：

| Field | Purpose |
| --- | --- |
| `id`, `tenant_id` | 主键和租户隔离 |
| `operation_no` | 操作业务号 |
| `reversal_id`, `reversal_no` | 关联追偿案件 |
| `request_key` | 人工请求幂等键 |
| `operation_type` | `RETRY_INSUFFICIENT`、`RETRY_COMPLETED`、`LOSS_ACCEPTED` |
| `before_status`, `after_status` | 处置前后状态 |
| `operator_id`, `operator_name` | 操作人审计快照 |
| `review_note` | 操作意见 |
| `snapshot_json` | 每币种 required、available、recovered、shortfall 快照 |
| `create_time` | 操作时间 |

唯一键为 `(tenant_id, request_key)` 和 `(tenant_id, operation_no)`；索引覆盖追偿单号和创建时间。

## State Rules

只有 `status=REVIEW_REQUIRED`、`disposition_status=PENDING_REVIEW` 且购买订单为 `REFUND_REVIEW` 或 `CHARGEBACK_REVIEW` 的案件允许处置。

`RECOVERY_COMPLETED` 和 `LOSS_ACCEPTED` 均为终态。重复相同 `request_key` 返回原操作结果；不同请求键对已结案案件返回本地化的“案件已处置”错误，不产生钱包、订单或日志副作用。

## Retry Full Recovery

“再次全额追偿”在一个事务中执行：

1. 按租户和追偿单号锁定追偿主单，校验状态并锁定购买订单。
2. 加载原追偿明细，以全部 `required_amount` 构造多币种请求。
3. 调用钱包 `batchDebit`；业务号使用追偿单号，币种幂等键固定为 `purchase-reversal-review:{reversalNo}:{currencyCode}`。
4. 任一币种不足时，不写钱包交易、不改变余额；刷新全部明细的 available/shortfall，增加重试次数，写 `RETRY_INSUFFICIENT` 日志并保持待复核。
5. 全部足额时，更新明细 recovered、shortfall 和钱包交易号；取消仍为 `PENDING` 的购买流水任务；更新追偿和处置状态；将订单更新为 `REFUNDED` 或 `CHARGEBACK`；写 `RETRY_COMPLETED` 日志。

人工重试不再次更新会员风险，也不创建第二条支付事件。

## Accept Loss

“确认损失并结案”在一个事务中执行：

1. 锁定追偿主单并校验仍可处置。
2. 要求 `review_note` 去除首尾空格后长度为 1 至 500。
3. 重新读取钱包余额，仅刷新每币种 available/shortfall，不执行扣款。
4. 更新为 `LOSS_ACCEPTED`，保存审核人、意见和结案时间。
5. 写入 `LOSS_ACCEPTED` 日志及每币种最终损失快照。
6. 不取消流水任务，不改变钱包余额，不生成钱包交易。
7. 购买订单保持 review 状态，由处置状态表达“已结案但未追回”。

拒付案件继续保持会员 `risk_level=HIGH`；风险解除必须走独立会员风险治理流程。

## Backend Boundaries

新增 `IPurchaseReversalReviewService`，负责查询工作台、读取详情、重试追偿和确认损失。Controller 不直接操作 mapper 或钱包。

```text
GET  /payment/purchase-reversal-review/list
GET  /payment/purchase-reversal-review/{reversalNo}
POST /payment/purchase-reversal-review/{reversalNo}/retry
POST /payment/purchase-reversal-review/{reversalNo}/accept-loss
```

两个 POST 均要求客户端生成的稳定 `requestKey`；确认损失必须提供 `reviewNote`。当前登录用户由服务端读取，不接受客户端传入操作人。

余额不足是正常业务结果，返回最新明细，不抛出导致回滚的异常。案件不存在、已处置、订单状态不一致、明细非法、请求键冲突、钱包幂等冲突及数据库异常均回滚。

## Permissions And Menu

| Permission | Purpose |
| --- | --- |
| `payment:reversalReview:list` | 查看审核列表 |
| `payment:reversalReview:query` | 查看审核详情 |
| `payment:reversalReview:retry` | 再次全额追偿 |
| `payment:reversalReview:acceptLoss` | 确认损失结案 |

菜单位于“支付中心”下，名称为“拒付审核”。查询人员不自动获得资金处置权限；重试和确认损失可分别授权。

## Admin Experience

页面默认展示 `PENDING_REVIEW`，提供“待复核 / 已追回 / 已确认损失”分段筛选。查询条件包括追偿单号、购买订单号、会员 ID、追偿类型、处置状态和创建时间。

列表展示追偿单号、订单号、会员、追偿类型、逐币种短缺摘要、会员风险、等待时长、处置状态和创建时间。详情使用宽抽屉展示订单、支付事件、发放快照、逐币种追偿数据、会员风险和审核历史。

待复核案件显示“再次全额追偿”和“确认损失并结案”。前者余额仍不足时只刷新短缺；后者必须填写意见，并在二次确认中逐币种展示损失。已结案案件完全只读。

购买订单详情在 `PENDING_REVIEW` 时显示“前往拒付审核”链接，仅负责导航。移动端抽屉单列显示，表格自身横向滚动，不产生页面级横向溢出。

## Testing

后端覆盖租户筛选、余额仍不足、全额追偿成功、一个币种不足时全不扣、确认损失无钱包副作用、请求重放、并发处置、终态保护、拒付风险保持和完整日志。

Admin 覆盖三种状态筛选、逐币种短缺摘要、四项权限、余额不足刷新、两种结案结果、审计历史，以及桌面和 390px 移动布局。

运行时验收包括：补足 Phase 41 的拒付测试订单后再次追偿；创建另一短缺案件并确认损失；重放请求键和模拟第二审核员；连续导入 SQL 两次；执行后端测试、Admin 检查与构建、桌面/移动截图及 `git diff --check`。

## Acceptance Criteria

- 待复核退款和拒付案件可集中查询。
- 审核员只能再次全额追偿或确认逐币种损失。
- 多币种追偿全有或全无，永不部分扣款或产生负余额。
- 确认损失不改变钱包、钱包交易或流水任务。
- 请求重放和并发处置具有稳定幂等性。
- 每次重试和最终处置均有操作人、意见、状态和逐币种快照。
- 拒付风险不会因结案自动解除。
- 订单、追偿和处置状态表达一致。
- 权限、租户隔离、SQL 幂等、测试、构建和运行时验收全部通过。
