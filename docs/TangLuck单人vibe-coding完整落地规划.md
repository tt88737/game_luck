# TangLuck 单人 Vibe Coding 完整落地规划

## 1. 项目定位

本项目定位为基于当前 GameLuck 底座复刻 TangLuck 的 Sweepstakes Social Casino 产品原型，并逐步演进为可运营、可配置、可审计的包网平台。

第一目标不是一次性复刻 TangLuck 全量能力，而是先跑通可演示、可运营、可验收的核心闭环：

```text
注册/登录 -> GC/SC 钱包 -> 活动奖励 -> 游戏大厅 -> 模拟游戏账变 -> Store -> 模拟充值 -> 兑换申请 -> 后台审核 -> BI 看板
```

单人开发必须坚持纵向切片，不按模块横向铺开。每个切片都要同时覆盖 C 端展示、后端规则、B 端运营、状态字典、国际化、审计和验收。

## 2. 当前底座能力

| 能力 | 当前底座 | 规划结论 |
| --- | --- | --- |
| 后端 | Spring Boot / GameLuck Backend Base | 继续作为核心业务后端和 B 端 API 底座 |
| B 端 | Vue3 / Vite / Element Plus | 继续作为运营后台，不重做 |
| C 端 H5 | Vue3 / Vite | 作为 TangLuck 玩家端 H5 / PWA 优先交付 |
| 数据库 | MySQL | 继续使用，所有业务表默认带 `tenant_id` |
| 缓存 | Redis | 继续承载登录、字典、幂等、会话等缓存 |
| 字典 | `sys_dict_type` / `sys_dict_data` | 作为业务状态、类型、标签、颜色的统一来源 |
| 国际化 | 后端 i18n 资源、B 端 `tt()` / `t()` | 扩展到 H5，默认支持中文和英文 |
| 钱包 | 已有多币种钱包雏形 | 继续作为所有余额变化的唯一入口 |
| 报表 | 已有 Overview / Trends 雏形 | 继续补注册、钱包、充值、兑换、活动、游戏指标 |

## 3. 平台级开发约束

这些约束是后续所有模块的默认规则，不在每个模块重复描述。

### 3.1 状态与字典

1. 所有业务状态、类型、来源、决策结果必须进入 `sys_dict_type` / `sys_dict_data`。
2. 字典负责展示和运营维护，不负责状态流转规则。
3. 状态流转必须由后端 service 控制，不能只依赖前端按钮或字典配置。
4. API 返回状态 code，不返回硬编码中文状态文案。
5. B 端表格状态展示统一使用 `DictTag` 或统一 formatter。
6. 新增字典 SQL 必须使用 UTF-8 导入脚本，避免中文被导入为问号。

字典命名规则：

```text
gl_模块_字段
```

示例：

```text
gl_redemption_status
gl_wallet_biz_type
gl_risk_decision
```

状态值规则：

```text
PENDING / SUCCESS / FAILED / APPROVED / REJECTED
```

状态值使用英文常量，显示文案通过字典和国际化解决。

### 3.2 国际化

默认支持中文和英文。

1. B 端页面、弹窗、表格列名、按钮、placeholder、确认提示走 `tt()` 或 `t()`。
2. B 端公共中文源文案补充到 `admin-ui/src/utils/i18nText.ts`。
3. B 端路由标题继续走 `translateTitle()` / `i18nTitle.ts`。
4. 后端异常、校验、接口提示走 `MessageUtils.message('key')`。
5. 后端新增 key 必须同时写入：
   - `backend/gameluck-admin/src/main/resources/i18n/messages.properties`
   - `backend/gameluck-admin/src/main/resources/i18n/messages_zh_CN.properties`
   - `backend/gameluck-admin/src/main/resources/i18n/messages_en_US.properties`
6. H5 后续建立独立 `zh-CN` / `en-US` 语言包，玩家可见文案不得从后端中文拼接。
7. GC/SC、No Purchase Necessary、AMOE、Official Rules、Privacy、Responsible Gaming 等合规文案必须统一维护。

### 3.3 钱包

1. 所有 GC/SC/RC/bonus 余额变化必须走 wallet service。
2. 业务模块禁止直接修改钱包账户余额。
3. 每次余额变化必须写钱包流水。
4. 钱包流水必须包含 `tenant_id`、`member_id`、`currency_code`、`biz_type`、`biz_no`、`idempotency_key`。
5. 充值成功、活动奖励、游戏投注、游戏派彩、兑换冻结、兑换结算、兑换解冻必须支持幂等。
6. 钱包失败时，业务订单不得推进到成功终态。

### 3.4 审计

以下行为必须审计：

| 场景 | 审计内容 |
| --- | --- |
| 后台人工审核 | 操作人、前状态、后状态、原因、时间、IP |
| 钱包调账 | 业务类型、业务单号、变更前后余额、幂等键 |
| 兑换审核 | 审核结果、拒绝原因、冻结/结算/解冻交易号 |
| KYC 状态调整 | 原状态、新状态、原因、操作人 |
| 风控处理 | 触发场景、决策、原因码、处理结果 |
| 活动配置变更 | 配置前后快照、生效时间、操作人 |

### 3.5 合规底线

本规划不是法律意见，正式上线前需要法务、支付、KYC、地区合规、应用商店审核确认。

产品实现必须先满足以下底线：

1. 不能表达购买 SC。
2. Store 只能表达购买 GC。
3. SC bonus 是赠品、奖励或免费路径权益。
4. No Purchase Necessary、AMOE、Official Rules、Privacy、Responsible Gaming 必须可达。
5. 禁止地区用户不能领取 SC、进入 SC Game 或提交兑换。
6. KYC 未通过不能完成兑换。
7. 所有 GC/SC 变化必须可追溯。
8. 兑换、KYC、风控、后台人工操作必须可审计。

## 4. 第一批统一字典规划

### 4.1 通用状态

| 字典类型 | 中文名称 | 值 | 用途 |
| --- | --- | --- | --- |
| `gl_common_status` | 通用启停状态 | `ENABLED` / `DISABLED` | 活动、配置、渠道、规则启停 |
| `gl_yes_no` | 业务是否 | `Y` / `N` | 是否默认、是否可见、是否推荐 |

### 4.2 会员与合规

| 字典类型 | 中文名称 | 值 |
| --- | --- | --- |
| `gl_member_status` | 会员状态 | `ACTIVE` / `FROZEN` / `BANNED` / `CLOSED` |
| `gl_kyc_status` | KYC 状态 | `NOT_STARTED` / `PENDING` / `APPROVED` / `REJECTED` |
| `gl_geo_status` | 地区检查状态 | `PASS` / `BLOCKED` / `UNKNOWN` |
| `gl_risk_decision` | 风控决策 | `PASS` / `CHALLENGE` / `REVIEW` / `BLOCK` |

### 4.3 钱包

| 字典类型 | 中文名称 | 值 |
| --- | --- | --- |
| `gl_currency_type` | 币种类型 | `VIRTUAL` / `SWEEPSTAKES` / `CASH` / `BONUS` |
| `gl_wallet_account_status` | 钱包账户状态 | `NORMAL` / `FROZEN` |
| `gl_wallet_biz_type` | 钱包业务类型 | `REGISTER_BONUS` / `DAILY_REWARD` / `TASK_REWARD` / `GAME_BET` / `GAME_PAYOUT` / `DEPOSIT` / `REDEMPTION` / `ADJUSTMENT` |
| `gl_wallet_freeze_status` | 钱包冻结状态 | `FROZEN` / `SETTLED` / `RELEASED` |

### 4.4 活动

| 字典类型 | 中文名称 | 值 |
| --- | --- | --- |
| `gl_promotion_type` | 活动类型 | `REGISTER_BONUS` / `DAILY_LOGIN` / `DAILY_MISSION` / `ONLINE_REWARD` / `WELCOME_OFFER` / `REFILL` |
| `gl_promotion_status` | 活动状态 | `DRAFT` / `ENABLED` / `DISABLED` / `EXPIRED` |
| `gl_reward_claim_status` | 奖励领取状态 | `PENDING` / `SUCCESS` / `FAILED` / `DUPLICATE` |

### 4.5 游戏、充值、兑换

| 字典类型 | 中文名称 | 值 |
| --- | --- | --- |
| `gl_game_status` | 游戏状态 | `ENABLED` / `MAINTENANCE` / `DISABLED` |
| `gl_game_session_status` | 游戏会话状态 | `STARTED` / `ENDED` / `FAILED` |
| `gl_deposit_status` | 充值订单状态 | `PENDING` / `SUCCESS` / `FAILED` / `CANCELLED` |
| `gl_redemption_status` | 兑换订单状态 | `PENDING` / `APPROVED` / `REJECTED` / `PAID` / `FAILED` |

## 5. 模块边界

| 模块 | P0 职责 | 后续扩展 |
| --- | --- | --- |
| member-center | 注册、登录、资料、状态、KYC 状态 | 会员等级、标签、生命周期 |
| wallet-center | 币种、账户、流水、冻结、结算、幂等 | 汇率、清结算、调账审批 |
| promotion-center | 注册赠送、每日登录、每日任务、奖励发放 | 在线奖励、VIP、复杂活动规则 |
| game-center | 游戏列表、模拟供应商、游戏会话、回调账变 | 多供应商、分类、维护、RTP 报表 |
| payment-center | GC 商品包、模拟充值订单、模拟成功 | 真实支付、回调验签、拒付、对账 |
| redemption-center | 兑换申请、冻结、后台审核、结算/解冻 | 自动出款、多级审核、失败重试 |
| risk-center | 地区、KYC、黑名单、风控决策骨架 | 设备指纹、规则引擎、评分模型 |
| cms-center | 规则页、Banner、弹窗、公告 | 多语言 CMS、活动落地页 |
| report-center | 注册、钱包、游戏、充值、兑换、活动指标 | 留存、渠道、ROI、漏斗 |
| audit-center | 敏感操作日志 | 审计检索、导出、告警 |

## 6. 阶段路线图

### Phase 0：平台约束固化

目标：让后续 AI 开发不会散。

交付：

1. 状态字典 SQL。
2. B 端字典使用规范。
3. H5 国际化骨架。
4. API 状态返回规范。
5. 钱包账变类型规范。
6. 审计日志规范。
7. 单人切片开发模板。

验收：

1. 新增业务状态能在 B 端通过字典展示。
2. B 端新增页面能通过 i18n 检查。
3. 后端新增异常能同时输出中英文资源。
4. 后续模块开发能引用统一状态表，不重新定义状态。

### Phase 1：P0-A 合规活动最小闭环

目标：注册后能拿奖励，钱包可信，后台可查。

交付：

1. 会员注册/登录。
2. GC/SC 钱包账户。
3. 注册赠送。
4. 每日登录奖励。
5. 每日任务基础版。
6. Rewards Center。
7. 钱包流水查询。
8. B 端用户查询。
9. B 端钱包查询。
10. B 端奖励发放记录。
11. No Purchase Necessary / AMOE / Official Rules / Privacy 静态页。
12. BI 基础看板。

验收路径：

```text
用户注册 -> 获得 GC/SC -> 查看钱包 -> 领取每日奖励 -> 查看钱包流水 -> 后台查询会员、钱包、奖励记录、BI 指标
```

### Phase 2：P0-B 游戏与兑换闭环

目标：产品开始像 TangLuck，形成玩和兑的主链路。

交付：

1. H5 五 tab：Lobby / Store / Activity / Exchange / Me。
2. 游戏列表、最近、收藏。
3. 模拟游戏启动。
4. 模拟投注和派彩回调。
5. GC/SC 切换。
6. 兑换申请。
7. SC 冻结。
8. B 端兑换审核。
9. 通过后结算冻结金额。
10. 拒绝后解冻。
11. 兑换审计。

验收路径：

```text
用户进大厅 -> 启动游戏 -> 投注/派彩改钱包 -> 提交兑换 -> 后台通过/拒绝 -> 钱包冻结、结算、解冻正确
```

### Phase 3：Store、模拟支付、KYC/风控骨架

目标：补齐商业主链，不直接接真实三方。

交付：

1. GC 商品包配置。
2. Store 购买 GC。
3. SC bonus 展示。
4. 模拟支付订单。
5. B 端模拟支付成功。
6. 支付成功钱包入账。
7. KYC 状态手动维护。
8. 地区限制规则。
9. RiskApi 骨架。
10. AMOE 申请入口。

验收路径：

```text
用户选择 GC 包 -> 创建订单 -> 后台模拟成功 -> 钱包入账 -> KYC/Geo/Risk 能阻断兑换
```

### Phase 4：TangLuck 活动体验复刻

目标：开始复刻 TangLuck 的活动体验和运营玩法。

优先顺序：

1. Login Bonus。
2. Daily Mission。
3. Online Rewards。
4. Rewards Center。
5. Welcome Offer。
6. Refill / Newbie Charge。
7. Jackpot 展示。
8. Tang Club。
9. Wheel / Plinko / Blazing Challenge。

每个活动必须按同一模板开发：

```text
B 端配置 -> C 端展示 -> 用户参与 -> 资格校验 -> 奖励发放 -> 钱包流水 -> 领取记录 -> BI 统计
```

### Phase 5：运营增强

目标：从 demo 变成可运营产品。

交付：

1. 活动人群规则。
2. Banner / Popup / CMS。
3. Inbox 消息。
4. 客服入口。
5. 风控事件列表。
6. 审计查询。
7. 数据趋势。
8. 用户生命周期看板。
9. 活动 ROI。
10. 渠道统计。

## 7. 时间规划

### 15 天版本

目标：完成 P0-A，可演示合规活动最小闭环。

| 天数 | 任务 |
| --- | --- |
| Day 1 | 固化字典、国际化、状态约束 |
| Day 2 | 梳理现有接口、表、页面和菜单 |
| Day 3-4 | 注册、会员资料、钱包账户 |
| Day 5 | 注册赠送、钱包流水 |
| Day 6-7 | H5 五 tab 壳子和钱包展示 |
| Day 8-9 | 每日登录奖励、每日任务基础版 |
| Day 10 | Rewards Center |
| Day 11-12 | B 端用户、钱包、奖励记录查询 |
| Day 13 | 合规静态页 |
| Day 14 | BI 基础看板 |
| Day 15 | 全链路烟测、修 UI、整理演示路径 |

### 30 天版本

目标：完成 P0-B，具备游戏和兑换闭环。

新增：

1. 游戏大厅。
2. 模拟游戏账变。
3. 兑换申请。
4. B 端兑换审核。
5. 钱包冻结、结算、解冻。
6. 兑换审计。

### 60 天版本

目标：完成 Store、模拟支付、KYC/风控骨架和核心活动复刻。

新增：

1. Store 商品包。
2. 模拟充值。
3. KYC 状态。
4. 地区限制。
5. RiskApi。
6. Online Rewards。
7. Welcome Offer。
8. TangLuck 风格活动入口。

## 8. 单人 Vibe Coding 工作法

每天只做一个纵向切片。

标准切片格式：

```text
今天目标：
一个用户动作 + 一个后台能力 + 一个状态变化 + 一个验收路径

必须产出：
1. PRD 小节
2. 字典/状态定义
3. DB/API 变化
4. 后端实现
5. B 端页面或查询
6. H5 页面或入口
7. 测试/烟测路径
```

切片完成标准：

1. 用户能完成动作。
2. 后端状态正确变化。
3. 钱包或订单有记录。
4. B 端能查到。
5. 错误状态有提示。
6. 中文和英文文案都可维护。
7. 有最少一条可重复执行的烟测路径。

## 9. AI 开发提示词模板

### 9.1 产品切片提示词

```text
你是资深产品经理和技术负责人。
基于当前 GameLuck/TangLuck 复刻项目，为【模块/切片名称】输出可开发规格。

必须包含：
1. C 端用户流程
2. B 端运营流程
3. 状态字典
4. 字段表
5. API 列表
6. 钱包账变类型
7. 审计要求
8. 国际化文案要求
9. 验收清单

约束：
- 状态必须进入 sys_dict_type/sys_dict_data
- API 返回状态 code，不返回中文 label
- 后端提示走 MessageUtils
- B 端文案走 tt()/t()
- 钱包改账只能走 wallet service
```

### 9.2 开发实施提示词

```text
按照已确认的规格，实现【切片名称】。

要求：
1. 先读取现有模块模式
2. 复用当前项目代码风格
3. 不做无关重构
4. 补 SQL、后端、B 端、H5 中必要部分
5. 状态使用统一字典
6. 文案支持中文和英文
7. 完成后运行相关构建或测试
8. 给出烟测路径
```

### 9.3 自查提示词

```text
请按资深工程负责人视角审查本次切片。

重点检查：
1. 是否绕过钱包直接改余额
2. 是否有硬编码中文文案
3. 是否遗漏字典状态
4. 是否缺少幂等
5. 是否缺少审计
6. 是否状态流转不严谨
7. 是否 B 端无法运营查询
8. 是否 H5 无错误/空/阻断状态
9. 是否验收路径不可执行
```

## 10. 第一阶段任务拆分

Phase 0 和 Phase 1 的建议执行顺序：

| 顺序 | 切片 | 目标 |
| --- | --- | --- |
| 1 | 平台字典种子 | 新增第一批 `gl_*` 字典 |
| 2 | H5 国际化骨架 | H5 支持中文/英文切换或默认语言资源 |
| 3 | 会员注册资料补齐 | 注册采集地区、年龄确认、条款确认 |
| 4 | 钱包账户创建 | 注册后创建 GC/SC 钱包 |
| 5 | 注册赠送 | 注册成功异步或同步发放奖励 |
| 6 | 钱包流水查询 | H5 和 B 端都能查钱包变化 |
| 7 | 每日登录奖励 | 每日一次领取，重复领取拦截 |
| 8 | 每日任务基础版 | 配置任务、完成任务、领取奖励 |
| 9 | Rewards Center | H5 展示奖励记录和可领取状态 |
| 10 | 合规静态页 | No Purchase Necessary / AMOE / Rules / Privacy |
| 11 | B 端奖励查询 | 运营能查发放结果和失败原因 |
| 12 | BI 基础看板 | 注册、奖励、钱包变动汇总 |

## 11. 下一步执行建议

下一步先做 Phase 0，不直接开始业务页面。

推荐第一张任务卡：

```text
任务：平台字典与国际化约束落地

范围：
1. 新增第一批 gl_* 字典 SQL
2. 确认 B 端页面可通过 useDict/DictTag 使用
3. 为 H5 建立 zh-CN/en-US 语言资源结构
4. 补充开发守门说明

不做：
1. 不改钱包业务逻辑
2. 不新增活动业务
3. 不重构现有 i18n 体系

验收：
1. 字典可导入本地数据库
2. B 端可查询字典
3. H5 有统一文案入口
4. 构建或 i18n 检查通过
```
