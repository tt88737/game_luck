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
## Sprint 2 发现
- `V2__seed_demo_data.sql` 中 CA/TX/NJ 的 `purchase_allowed=false` 与 P1 商店购买闭环冲突；已通过 `V6__enable_purchase_in_allowed_regions.sql` 将允许运营州的购买开关设为 true。
- 后台合规配置测试会真实修改 `compliance_regions`，必须隔离测试上下文，否则后续 P1 购买回归会被 CA purchase=false 污染。
- 法务文档发布需要同时处理旧 active 版本归档、公开文档接口返回新 active 版本、后台审计落点三个验收点。

## Sprint 3 发现
- `AppHome` 原先同时请求 `/campaigns` 和 `/tasks/daily`，且 Featured games 在前端写死；正式产品需要由 `/lobby` 聚合接口提供页面运营内容。
- `/admin/campaigns` 原后台页面使用前端硬编码 `OPS_SC_BONUS`，已改为读取真实后台活动列表。
- 修改全局配置类测试容易污染共享 H2 context；P1 闭环测试已在执行前刷新上下文，避免被其它后台配置测试影响。

## Sprint 4 发现
- 原购买链路创建订单后直接把 GC 记入钱包，不符合正式支付流程；已改为 `payment_pending`，后台确认支付后才发放 GC。
- 商品包不应只作为 C 端静态展示数据；后台必须能查看、上下架并审计商品包配置变化，C 端只展示 active 商品包。
- `P1OperationsDto` 仍可作为短期订单运营列表数据源，但长期应拆出独立订单查询接口，支持状态筛选、支付 provider callback、退款和失败重试。
- 可变后台配置测试会污染共享测试上下文，购买闭环测试必须显式恢复 CA purchaseAllowed 等前置条件。
- 移动端后台不能只依赖横向大表；窄屏下操作列会被裁切。后台全局表格已增加移动端卡片化展示，确保高风险操作按钮在移动视口内可见。

## 后台占位模块修复发现
- 后台主导航不能暴露“看起来可用但实际只显示 Planned”的模块；正式产品里这会被视为功能不可用。
- 用户、钱包流水、KYC、兑换已经有业务表和部分 C 端链路，优先接为真实后台查询页，比继续保留 `/admin/p1` 聚合页更符合正式运营后台的信息架构。
- AMOE 与 Support 当前缺少独立表、状态机和后台接口，仍不能伪装为可用模块；后续应按独立闭环补 AMOE 申请/审核和客服工单，再替换占位页。

## Sprint 5 发现
- KYC 拒绝不能只是后台状态变化，C 端必须能读取 rejected 原因并允许重新提交；现有 `submitKyc` 的 replace 行为可复用为 resubmission。
- 兑换账本至少包含三类 SC 流水：奖励发放、兑换冻结、拒绝解冻或出款扣减。测试预期不能只看兑换相关两条流水，否则会漏算真实资金来源。
- 兑换出款必须从 frozen SC 扣减，不能直接扣可用余额；拒绝必须释放 frozen SC，二者都需要幂等 idempotency key。
- 后台移动端表格操作按钮已可见，但顶部 admin nav 在窄屏仍偏高，后续应改为折叠菜单或模块分组导航。

## Sprint 6 发现
- i18n 缺口不只在 C 端；B 端导航、看板、表头、按钮、空状态、错误提示同样属于正式上线范围，必须和 C 端一起验收。
- 当前 `messages.ts` 中文资源本身是 UTF-8 正常中文；PowerShell 默认输出会显示乱码，判断源文件内容应以 Node/构建/浏览器渲染为准。
- 业务状态值、订单号、活动 code、供应商返回原因、法务文档标题属于后端或运营数据，不应强行前端翻译；静态 UI 文案必须进入 i18n。
- Playwright 已覆盖默认英语、`zh-CN` 下 C 端注册页和 B 端看板；后续新增模块需要同步增加中英文浏览器语言断言。
## 2026-06-21 B1-B3 Slots Productization 发现
- Slots 下注需要普通 GC debit 能力，原钱包服务只有 credit/freeze/unfreeze/redeemFrozen；已补 `WalletService.debit(...)` 并保持幂等账变。
- 本地 H2 对 `json` 列和 JPA String 读取会出现字符串包装差异；Slots reels 读取已兼容 textual JSON 和数组 JSON。
- 活动任务进度返回 BigDecimal，前端和测试都应按数值/金额型处理，不按整数假设。
- 钱包 ledger 查询历史参数使用 `business_type`，前端和测试更常用 `businessType`；已在后端兼容两种参数名。
- 浏览器验收不能用伪 token，否则会触发 401；应通过真实注册接口创建测试用户并注入 `tangluck_user_id`。
- 8092 页面真实验收必须同时检查网络 4xx/5xx、页面可见错误和截图，而不是只看页面是否能打开。

## 2026-06-21 Guest-first C-side Auth Findings
- C 端注册/登录作为主页面会破坏正式产品体验；更合理的是游客自动进入大厅，登录和绑定作为全局账号弹窗出现。
- 游客不是“未登录空状态”，应有真实后端 userId、token、钱包和可玩链路；绑定邮箱后必须保留同一 userId，避免丢失钱包、游戏、活动和通知状态。
- Store/KYC/Redemption 不应在游客态直接调用高风险接口；应展示绑定账号原因和入口，绑定后复用同一正式链路。
- 单独打开页面返回 200 不等于验收通过；本次真实浏览器验收发现旧后端 bootRun 进程会让 `/auth/guest` 返回 500，重启后端并复测接口 200 后才完成浏览器截图验收。
- 8092 已有旧 Node 服务监听，最终使用 8093 启动最新前端进行验收，避免把旧 dev server 误认为当前实现。

## 2026-06-21 C-side Product Polish Findings
- C 端新增文案必须同步进入 i18n；硬编码 `Guest`、`Bind account`、`Inbox` 会导致中文浏览器下产品体验割裂。
- `AppShell` 不应在会话失败时显示 `User` fallback；失败状态需要明确文案和 Retry 操作。
- 浏览器验收中 PowerShell 管道会影响中文字符串传递；中文页面验收应以实际页面文本和截图为准，避免脚本字符串编码误判。
- 继续使用新端口做验收可以规避旧 Vite/Node 服务带来的假阳性。

## 2026-06-21 C-side Route Layout Gates Findings
- The previous `Home | Slots | Activity | Inbox | Wallet` bottom nav conflicted with the product routing document and made redemption/wallet too prominent for a sweeps product.
- `Redeem` should remain reachable, but through `Me > Redeem`; this keeps eligibility explanation, KYC, region, risk, playthrough, and threshold gates visible before action.
- Keeping `/app/*` as compatibility redirects avoids breaking old links while making `/store`, `/promo`, `/lobby`, `/inbox`, and `/me` the canonical user-facing routes.
- `AppActivity.vue` can remain the implementation file temporarily, but user-facing route and copy must say `Promo`; a mechanical filename rename is lower priority than product route correctness.
- Browser verification needs to assert both desktop and mobile bottom tabs because this layout is fixed-position and easy to regress.

## 2026-06-21 Admin Ops Navigation Grouping Findings
- A flat admin menu hides the relationship between B-side operations and C-side product surfaces; grouped navigation makes the Store/Promo/Lobby/Inbox/Me control model visible.
- Planned modules should be visible enough to show the operating roadmap, but must not be styled as live links unless the route has a real working page.
- Mobile admin navigation can become unusable if every module is a top-level tab; grouped scrollable sections keep the page usable without pretending mobile is the primary B-side workflow.
- C-side impact labels are a low-cost way to prevent future drift between backend configuration modules and customer-facing routes.
