# Tang Luck 文档分析发现

## 目录结构
- 根目录包含 `TangLuck可运营可评审可验收最终PRD.html` 和 `TangLuck最终交付包/`。
- `TangLuck最终交付包/` 内包含总入口、15 天冲刺表、接口草案、数据库设计、页面原型说明、高保真原型、QA 矩阵、验收清单、法务/支付/App 材料等。
- 尚未发现 `package.json`、后端项目、数据库迁移、Docker 配置等源码工程文件。

## P0-A 开发目标
- 15 天交付：注册、条款、钱包、活动、任务、Coupon、SC 合规开关、No Purchase Necessary、AMOE 静态入口、后台配置、审计、看板。
- 验收底线：注册、钱包、ledger、注册赠送、每日登录真实可跑；合规入口可访问；后台可配置活动并查看发奖记录；发奖和后台操作可追溯；风险用户只发 GC 或拦截。

## 后端接口范围
- Base URL：`/api/v1`。
- C 端 P0-A：`/auth/register`、`/auth/login`、`/me`、`/compliance/documents`、`/wallet/summary`、`/wallet/ledger`、`/campaigns`、`/campaigns/{campaign_id}/claim`、`/tasks/daily`、`/tasks/{task_id}/progress`、`/tasks/{task_id}/claim`、`/coupon/claim`。
- 后台 P0-A：`/admin/dashboard/summary`、`/admin/users`、`/admin/users/{user_id}`、`/admin/wallet/ledger`、`/admin/campaigns`、`/admin/campaigns/{campaign_id}`、`/admin/campaigns/{campaign_id}/publish`、`/admin/campaigns/{campaign_id}/pause`、`/admin/reward-grants`、`/admin/risk/events`、`/admin/audit-logs`。

## 数据模型
- P0-A 核心表：`users`、`user_consent_logs`、`compliance_regions`、`compliance_documents`、`wallet_accounts`、`wallet_ledger`、`promotion_campaigns`、`promotion_claims`、`promotion_reward_grants`、`daily_task_progress`、`coupon_codes`、`risk_events`、`audit_logs`。
- 关键原则：钱包变化必须写 `wallet_ledger`；配置版本化；后台操作写 `audit_logs`; 发奖、任务领奖、Coupon、支付回调等需要幂等。

## 页面范围
- C 端：注册、首页、钱包、活动中心、商店灰态、兑换灰态、AMOE 静态说明、客服入口。
- 后台：BI 看板、用户管理、钱包流水、活动配置、商品包配置灰态、兑换审核灰态、AMOE 审核、客服工单、审计日志。

## 测试重点
- 注册：成功、重复邮箱、未勾选条款、未成年、禁止州。
- 钱包/活动：GC/SC 入账、流水、幂等、防重复、过期、预算用尽、禁止州。
- SC 策略：`gc_only`、`default_small_sc`、`legal_required`、`sc_blocked`。
- 后台：活动新增、编辑、发布、暂停、审计。
