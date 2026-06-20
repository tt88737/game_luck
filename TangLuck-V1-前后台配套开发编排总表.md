# Tang Luck V1 前后台配套开发编排总表

## 1. 编排原则

从现在开始，开发不再按“先前台、再后台、最后补接口”的方式推进，而是按业务闭环推进。每个闭环必须同时回答四个问题：

| 问题 | 必须有的产物 |
| --- | --- |
| 用户看到什么 | C 端页面、状态、错误提示、下一步动作 |
| 运营怎么配置/处理 | B 端列表、筛选、详情、操作按钮、原因填写 |
| 后端如何兜底 | API、状态机、幂等、地区/KYC/风险校验 |
| 出问题怎么追溯 | wallet ledger、audit log、业务单状态、错误码 |

## 2. 总体执行顺序

| 顺序 | 闭环 | 为什么先做 | 完成后可见效果 |
| --- | --- | --- | --- |
| 0 | 基线保护 | 防止在错误基线上继续叠代码 | 当前测试、构建、e2e 可重复运行 |
| 1 | Admin Shell + Audit + 权限上下文 | 后续所有后台模块共用 | 后台从演示页变成正式运营框架 |
| 2 | Regions + Legal Docs | 所有 SC、购买、兑换、AMOE 都依赖合规配置 | 后台能控制州级能力和法务文档版本 |
| 3 | Campaigns + Lobby Config | 先解决 C 端 Lobby/活动写死问题 | C 端大厅和活动来自后台配置 |
| 4 | Product Packages + Orders | Store 必须可运营，且不能直接入账 | 后台配置商品包，订单有状态机 |
| 5 | KYC Review | 兑换前置条件，必须先正规化 | 后台能通过、拒绝、要求补资料 |
| 6 | Redemption Review + Wallet Freeze/Settle | 最高风险资金链路 | 冻结、拒绝解冻、成功结算全可追溯 |
| 7 | Users + Wallet Ledger + Risk | 运营查询和风控支撑 | 后台能查用户资产、流水和风险事件 |
| 8 | AMOE + Support | 合规免费路径和客服闭环 | 用户有免费申请/工单，后台可处理 |
| 9 | 全链路验收 + README | 进入可交付状态 | C/B/API/DB 全链路可验收 |

## 3. 第一批冲刺范围

第一批只做 4 个闭环，目标是把“前台展示”变成“后台可运营”。

### Sprint 1：后台基础壳、权限上下文、审计服务

**后端**
- `AdminOperatorContext`：从 header 读取 operator id、role、permissions；本地默认 `ops_admin`。
- `AdminAuditService`：统一写 `audit_logs`，支持 before/after/reason/ip。
- 现有 `AdminCampaignService` 和 P1 审核动作改用统一审计服务。

**前端**
- 抽 `AdminLayout.vue` 和 `AdminNav.vue`。
- 所有 admin 页面接入统一导航。
- 明确 `/admin/p1` 为临时聚合页文案，不作为正式模块入口。

**验收**
- 后台每个页面都有一致导航。
- 发布活动、暂停活动、KYC 审核动作写审计。
- 无权限场景至少有后端测试覆盖。

**建议提交**
- `feat: add admin shell and audit context`

### Sprint 2：地区合规与法务文档配置闭环

**后端**
- `/api/v1/admin/regions`：列表、编辑州级配置。
- `/api/v1/admin/legal-documents`：列表、新建版本、发布、归档。
- 注册、购买、活动领取、兑换前统一走 `ComplianceService` 功能开关校验。

**前端**
- `/admin/regions`：州级功能开关表。
- `/admin/legal-documents`：文档版本管理。
- C 端注册页继续读取 active 文档；错误提示走 i18n。

**验收**
- 禁止州不能注册/购买/兑换。
- 后台修改地区配置后，C 端行为变化。
- 发布文档后，注册页读取新版本。
- 后台编辑写审计。

**建议提交**
- `feat: add compliance admin configuration`

### Sprint 3：活动与 Lobby 配置化闭环

**后端**
- `/admin/campaigns` 支持真实列表、编辑、发布检查、领取记录。
- 新增 `lobby_cards` 表。
- `/admin/lobby-cards` 管理大厅卡片。
- `/lobby` 聚合返回钱包、活动、任务、卡片、KYC/兑换状态摘要。

**前端**
- AppHome 不再写死 Featured games，从 `/lobby` 渲染。
- `/admin/campaigns` 不再使用前端内置 `OPS_SC_BONUS` 假数据。
- 新增 `/admin/lobby` 管理卡片排序、状态、跳转。

**验收**
- 后台下线某张 Lobby 卡片，C 端不显示。
- 活动缺 `legal_approval_id` 不能发布。
- 活动发布后 C 端 Activity/Lobby 可见。
- 风险用户领取含 SC 活动时按 `risk_action` 降级。

**建议提交**
- `feat: configure lobby and campaigns from admin`

### Sprint 4：商品包与订单管理闭环

**后端**
- 扩展 `product_packages`：地区、provider、状态、排序、时间、法务 ID。
- `/admin/product-packages` 支持 CRUD、上下架。
- 扩展 `purchase_orders` 状态机：`created`、`payment_pending`、`paid`、`failed`、`cancelled`、`refunded`。
- manual provider 只作为本地 provider，但流程仍走 payment pending -> paid -> fulfill。

**前端**
- `/admin/packages` 管理 GC 商品包。
- `/admin/orders` 查询订单、状态和 provider reference。
- C 端 Store 显示真实订单状态，不再出现 sandbox 文案。

**验收**
- 后台下架商品后 C 端 Store 不显示。
- 禁购州不能下单。
- 订单未 paid 不入账。
- paid fulfillment 写 GC ledger。
- 重复 idempotency 不重复入账。

**建议提交**
- `feat: add product package and order operations`

## 4. 第二批冲刺范围

### Sprint 5：KYC 审核闭环

- 后台 KYC 列表、详情、approve、reject、request resubmission。
- C 端显示拒绝原因和补资料入口。
- provider reference、review note、reviewed_by、reviewed_at 入库。
- 所有审核动作写 audit。

### Sprint 6：兑换审核和钱包冻结结算

- WalletService 补 `unfreeze`、`settleFrozen`、`refund`。
- 后台 Redemption 列表、详情、approve、reject、mark paid、mark failed。
- 拒绝释放冻结；paid 结算扣减冻结。
- C 端显示请求历史和状态说明。

### Sprint 7：用户、钱包流水、风控

- 后台 Users、Wallet Ledger、Risk Events。
- 用户详情关联钱包、订单、KYC、兑换、活动领取。
- 风险动作影响活动发奖和兑换审核。

## 5. 第三批冲刺范围

### Sprint 8：AMOE

- C 端 AMOE 申请。
- 后台 AMOE 审核。
- 审核通过可按规则发奖，必须写 ledger 和 audit。
- 频率限制、地区限制、法务文档入口完整。

### Sprint 9：Support

- C 端工单入口。
- 后台工单列表、详情、处理。
- 业务单关联：订单、KYC、兑换、活动领取。
- 客服补偿只能 GC，必须审批和审计。

### Sprint 10：全链路验收

- 正常用户：注册 -> 活动 -> 商店 -> KYC -> 兑换。
- 禁止州：注册/购买/兑换被后端拦截。
- 风险用户：SC 发放降级或进入人工审核。
- 后台配置：地区、文档、活动、商品包变化能影响 C 端。
- 审计：所有后台写操作可查。

## 6. 并行策略

| 可并行 | 不可并行 |
| --- | --- |
| 后台 UI 壳和后端审计服务 | C 端配置化必须等对应后台/API 完成 |
| Regions 页面和 Legal Docs 页面 | Store 正式化必须等商品包 admin API |
| KYC UI 和 KYC 后端字段扩展可并行设计 | Redemption 审核必须等 WalletService 结算能力 |
| Users 查询页和 Wallet Ledger 查询页 | AMOE 发奖必须等活动/钱包/审计稳定 |

## 7. 每个 Sprint 的完成门槛

每个 Sprint 都必须满足：

- 后端单测覆盖正常和至少一个失败场景。
- 前端 Vitest 覆盖关键显示或操作。
- Playwright 至少覆盖一条 C/B 联动路径。
- 新增文案全部进入 i18n。
- 所有后台写操作可在 Audit Logs 查到。
- 提交前运行对应测试，提交后推送。

## 8. 当前立即执行建议

现在不要继续改 C 端 Lobby 样式。下一步应从 Sprint 1 开始：

1. 建立后台正式导航和布局组件。
2. 建立最小 admin operator context。
3. 建立统一 audit service。
4. 把现有活动发布/暂停和 KYC approve 接入统一审计。
5. 补测试并提交。

这样后续每个后台模块都能接在同一套正式骨架上。
