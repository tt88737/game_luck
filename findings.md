# Tang Luck 前后台配套差距发现

## 文档要求

### 交付包验收清单
- 每个活动必须有奖励、预算、地区、SC 策略。
- 商店只能表达购买 GC，不能出现购买 SC。
- 兑换必须覆盖 KYC、冻结、审核状态。
- 后台必须覆盖 RBAC、活动配置、SC 策略、钱包账本、幂等、审计、风控、看板。
- No Purchase Necessary / AMOE 必须独立可访问。

### 活动配置参数表
- 活动发布阻断：未配置地区、预算、规则版本、发 SC 无 `legal_approval_id`、文案违规、预算低于已领取量。
- SC 策略：`gc_only`、`default_small_sc`、`legal_required`、`sc_blocked`。
- 风控动作：`pass`、`gc_only`、`delay`、`manual_review`、`block`。

### 页面原型说明
- C 端：注册、首页、钱包、活动中心、商店、兑换、AMOE、客服。
- 后台：用户管理、钱包流水、活动配置、商品包配置、兑换审核、AMOE 审核、客服工单、BI 看板、审计日志。

### 数据库说明
- 钱包不可只存余额，所有变化必须进入 `wallet_ledger`。
- 配置必须版本化：活动、规则、商品包、地区策略。
- 后台操作必须审计。
- 支付和发奖必须幂等。
- 地区策略后端强校验。

## 当前代码现状

### 已有能力
- C 端：注册、登录、Lobby、钱包、活动、商店、KYC、兑换。
- 后端：auth、compliance documents/regions、wallet、promotion、coupon、P1 purchase/KYC/redemption。
- 后台：dashboard、campaign create/publish/pause、audit logs、P1 聚合运营页。
- 测试：后端单测、前端 Vitest、Playwright e2e。

### 主要不配套点
1. C 端 Lobby 的 Featured games 是前端写死，没有后台游戏/大厅配置。
2. C 端商店使用 `/purchase/packages`，但后台没有商品包配置、上下架、地区、价格、provider、审计。
3. 购买订单创建后直接入账 GC，没有正式支付状态机和回调/退款/失败路径。
4. KYC 后台只有 approve，没有 reject、resubmission、provider reference、审核原因、审计。
5. 兑换后台只能列表展示，不能 approve/reject/payout/retry/cancel，缺解冻/结算闭环。
6. 活动后台只有一个写死草稿行，缺列表查询、编辑、预算、发布检查结果、配置版本。
7. 地区合规、法务文档目前没有后台配置页，C 端依赖种子数据。
8. 用户管理、钱包流水查询、风控事件、AMOE、客服工单未形成后台模块。
9. `/admin/p1` 聚合页不符合正式运营后台的信息架构。
10. i18n 覆盖不完整，部分前后台页面仍有硬编码英文或乱码符号。

## 技术重排结论
- 后续应先补后台配置中枢和管理接口，再让 C 端从配置 API 渲染。
- 开发顺序应按闭环排序：合规/运营基础 -> 活动闭环 -> 商品/支付闭环 -> KYC/兑换闭环 -> AMOE/客服 -> Dashboard/RBAC -> C 端配置化。
