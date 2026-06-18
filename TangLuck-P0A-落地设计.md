# Tang Luck P0-A 落地设计

## 背景
当前目录只有 Tang Luck 交付文档，没有源码工程。已确认技术栈采用 Java Spring Boot + Vue 3 + MySQL，目标是把 P0-A 做成可运行、可演示、可测试的合规活动运营最小闭环。

## 范围
P0-A 实现以下能力：
- C 端注册、登录、当前用户、合规文档入口。
- 州级合规控制，按 `state_code` 拦截注册、SC 发放和灰态功能。
- GC/SC 双钱包，所有余额变化必须写 `wallet_ledger`。
- 注册赠送、每日登录、每日任务、Coupon 领取。
- SC 策略：`gc_only`、`default_small_sc`、`legal_required`、`sc_blocked`。
- 后台基础看板、用户列表、钱包流水、活动配置、发布/暂停、发奖记录、风险事件、审计日志。
- 商店、兑换、AMOE、客服在 P0-A 提供页面入口或灰态能力，不接真实支付、KYC、打款。

## 非范围
- 不接真实支付商。
- 不创建真实购买订单。
- 不开放真实兑换和打款。
- 不做自动风控评分系统，只做可演示的风险等级和发奖降级。
- 不做 App 上架集成，只保留审核材料和合规入口。

## 总体架构
采用单仓库双工程：
- `backend/`：Spring Boot 3 + Java 17 + Gradle Wrapper，提供 REST API、业务服务、Flyway 迁移和测试。
- `frontend/`：Vue 3 + Vite + TypeScript，提供 C 端移动视图和后台运营视图。
- `docker-compose.yml`：启动 MySQL 8，供本地开发使用。

后端以“账务一致性”和“发布阻断”为核心边界。所有发奖由 Promotion 服务调用 Wallet 服务，Wallet 服务在同一事务内更新钱包余额并写 ledger。后台写操作通过 Audit 服务记录操作人、对象、前后值和原因。

## 后端模块
### Auth
负责注册、登录、当前用户。
- 注册校验年龄、条款确认、地区可注册。
- 注册成功创建 GC/SC 两个钱包。
- 密码使用 BCrypt。
- P0-A 使用 JWT Bearer Token。

### Compliance
负责州级配置和文档版本。
- `compliance_regions` 决定注册、游戏、购买、SC 发放、兑换、AMOE 是否允许。
- `compliance_documents` 返回 Terms、Rules、Privacy、No Purchase、AMOE。

### Wallet
负责钱包账户和流水。
- `wallet_accounts` 保存 GC/SC 余额和冻结余额。
- `wallet_ledger` 保存所有 credit/debit/freeze/unfreeze 变化。
- `idempotency_key` 唯一，重复请求返回原结果，不重复入账。

### Promotion
负责活动、任务、Coupon 和发奖。
- `promotion_campaigns` 管理注册赠送、每日登录、每日任务。
- `promotion_claims` 防止同用户同周期重复领取。
- `promotion_reward_grants` 保存每个奖励项的发放状态。
- 发布活动必须校验地区、预算、规则版本；发 SC 时必须校验 `legal_approval_id`。

### Risk
P0-A 以用户 `risk_level` 和 `risk_events` 为主。
- `low` 正常发奖。
- `manual_review` 默认按活动 `risk_action` 降级，通常只发 GC。
- `blocked` 拦截领取。

### Admin
负责后台 API。
- 看板汇总注册、领取、SC 发放、风控事件。
- 用户、流水、活动、发奖、风险、审计查询。
- 活动新增、编辑、发布、暂停。

## 前端设计
前端不是营销页，而是两个工作视图：
- C 端：移动端形态，突出钱包余额、可领取奖励、任务、Coupon、规则入口和流水。
- 后台：运营工具形态，左侧导航、表格、筛选、状态标签、详情区域、发布阻断提示和审计记录。

颜色和布局以当前高保真原型为参考，但实现时避免过度装饰。后台页面必须以密集表格、筛选、状态和审计为主。

## 数据流
1. 用户注册提交邮箱、密码、生日、州和条款版本。
2. 后端校验地区和年龄，创建用户、条款日志、GC 钱包、SC 钱包。
3. 用户领取活动时，Promotion 检查活动状态、地区、预算、周期唯一性和风险动作。
4. Promotion 生成 claim 和 grants，调用 Wallet 入账。
5. Wallet 用幂等 key 更新余额并写 ledger。
6. 后台查询用户、活动、流水、审计和看板数据。

## 错误处理
API 统一返回：
```json
{
  "code": "REGION_BLOCKED",
  "message": "This feature is not available in your region.",
  "trace_id": "trc_20260618_0001",
  "details": {
    "state_code": "WA"
  }
}
```

核心错误码包括：`REGION_BLOCKED`、`AGE_NOT_ALLOWED`、`CONSENT_REQUIRED`、`EMAIL_EXISTS`、`CLAIM_DUPLICATED`、`CAMPAIGN_NOT_ACTIVE`、`BUDGET_EXHAUSTED`、`SC_POLICY_BLOCKED`、`LEGAL_APPROVAL_REQUIRED`、`IDEMPOTENCY_CONFLICT`。

## 测试策略
后端使用 TDD：
- 注册服务测试：成功、重复邮箱、未成年、未同意条款、禁止州。
- 钱包服务测试：GC/SC 入账、幂等、防重复 ledger。
- 活动服务测试：正常领取、重复领取、风险用户只发 GC、禁止州、缺法务审批发布阻断。
- 管理端测试：发布活动写审计、发布阻断返回错误码。

前端使用组件和 E2E 验证：
- C 端关键状态：正常、风险、禁止州、领取成功、重复领取失败。
- 后台关键状态：表格加载、空数据、发布阻断、审计可见。
- 使用 Playwright 检查桌面和移动截图。

## 本地运行
- `docker compose up -d mysql` 启动 MySQL。
- `cd backend && .\gradlew bootRun` 启动 API。
- `cd frontend && npm install && npm run dev` 启动前端。

## 主要风险
- 合规规则需要外部律师确认，P0-A 只能实现配置和阻断机制。
- Maven 未安装，因此后端使用 Gradle Wrapper。
- 当前没有 Git 仓库，提交步骤在初始化 Git 前只能作为计划记录。
