# 2026-07-18 交付状态与下一阶段建议

## 当前交付状态

本轮已经把玩家端、钱包、购买、兑换、后台审核、报表和合规门禁推进到可本地演示的纵向闭环。当前重点不是继续横向铺新模块，而是把已形成的闭环收敛成可运营、可验收、可继续扩展的 P1 基线。

已完成的关键能力：

| 能力 | 当前状态 | 主要证据 |
| --- | --- | --- |
| C 端账号与钱包 | 已接入注册、登录、钱包余额、钱包流水、币种兑换 | `h5/src/views/*`、钱包相关测试与 H5 运行截图 |
| 钱包中心 | 已支持多币种、账变、冻结、流水要求、币种兑换规则和订单可见性 | `backend/gameluck-modules/gameluck-wallet`、`admin-ui/src/views/wallet/*` |
| 购买/模拟支付 | 已有购买产品配置、C 端购买 API、H5 购买页、模拟履约和限购基础 | `backend/gameluck-modules/gameluck-payment`、`h5/src/views/PurchaseView.vue`、`admin-ui/src/views/payment/*` |
| 兑换闭环 | 已支持 C 端提交、钱包冻结、B 端审核、结算/解冻、审核列表和操作日志 | `backend/gameluck-modules/gameluck-redemption`、`admin-ui/src/views/redemption/*` |
| 兑换资格策略 | 已从 Java 硬编码迁移到 B 端可配置策略，支持地区、渠道、币种、优先级和 allow/deny | `docs/superpowers/specs/2026-07-18-redemption-eligibility-policy-design.md` |
| 运营可见性 | 已验证兑换资格策略 CRUD、Admin 表单、操作日志、SQL 幂等和构建可交付 | `admin-redemption-policy-*` evidence folders、`progress.md` |

## 最新验证基线

2026-07-18 Phase 34 的本地交付验证结果：

| 检查项 | 结果 |
| --- | --- |
| H5 build | `npm --prefix h5 run build` 通过 |
| 后端 package | `mvn -pl gameluck-admin -am package -Plocal -DskipTests` 通过 |
| 后端运行 | `http://localhost:8080/` 返回 200 |
| Admin UI 运行 | `http://localhost:5173/` 返回 200 |
| H5 运行 | `http://127.0.0.1:5174/` 返回 200 |
| 兑换资格策略接口保护 | 未登录访问返回业务 `code=401` |
| whitespace 检查 | `git diff --check` 无 whitespace error，仅有 CRLF 提示 |

后端当前由新 jar 启动，Java PID 为 `16292`。运行日志位于 `backend-runtime-logs/`。

## 当前风险与处理建议

| 风险 | 影响 | 建议 |
| --- | --- | --- |
| 工作区累计变更很多 | 后续提交、回滚、验收会变难 | 先做一次变更分组和 release note，不急着继续大改 |
| 真实 KYC 尚未接入 | 兑换只能用轻量资料字段和风险等级拦截 | 下一阶段做 KYC 状态机和 B 端人工维护，不先接第三方 |
| 地区策略目前先覆盖兑换 | SC 发放、游戏进入、购买、AMOE 仍需统一地区策略 | 抽象平台级 compliance/risk gate，逐步替换单点判断 |
| 支付仍是模拟履约 | 不能代表真实支付回调、退款、拒付、对账 | 先完善订单状态、回调幂等、失败/退款状态，再接真实通道 |
| 证据目录和日志较多 | repo 可读性下降，doctor 也可能继续提示历史/产物较多 | 保留本轮验收证据，后续单独做归档或清理策略 |

## 推荐下一阶段顺序

### Phase 35：交付清单与变更分组

目标：把当前大工作区拆成可评审的变更组，形成一份面向提交/验收的清单。

交付：

- 变更按 wallet、payment、redemption、member、h5、admin-ui、docs/evidence 分组。
- 标出必须进入代码提交的文件、仅作为本地证据的目录、可后续清理的日志。
- 给出建议提交顺序，降低一次性提交过大的风险。

### Phase 36：KYC 状态机与人工维护骨架

目标：补齐兑换前置条件中最关键的 KYC 状态，而不是只依赖演示字段。

建议范围：

- 后端新增 KYC 状态字段或独立 KYC 记录表。
- B 端会员详情可维护 KYC 状态、原因和有效期。
- C 端 bootstrap/profile 返回 KYC 状态。
- 兑换 gate 使用 KYC 状态阻断未通过用户。
- 操作日志记录 KYC 状态变更。

不建议本阶段接真实 KYC 供应商，避免把 provider、回调、文件上传、人工复核一次性拉大。

### Phase 37：统一 Compliance/Risk Gate

目标：把地区、KYC、风险等级、渠道、币种策略整理成统一服务，减少购买、SC 发放、游戏、兑换各自写判断。

建议先覆盖：

- `redemption_allowed`
- `purchase_allowed`
- `sc_grant_allowed`
- `game_allowed`
- `amoe_allowed`

验收重点是同一个用户在同一个地区下，各业务入口给出一致的 allow/deny 结果。

### Phase 38：支付订单真实化前置

目标：在接真实支付前，把当前模拟购买履约改造成可承载真实回调的订单模型。

建议范围：

- 订单状态补齐 `CREATED / PENDING / PAID / FAILED / CANCELLED / REFUNDED / CHARGEBACK`。
- 回调幂等表或唯一 key。
- 支付失败、取消、退款、拒付对钱包和流水要求的影响定义。
- B 端订单详情展示履约快照、钱包交易、失败原因。

## 推荐立即执行

下一步优先做 Phase 35，不改业务代码，只做变更分组和交付清单。当前工作区已经足够大，先把“哪些是代码、哪些是证据、哪些是本地运行日志”分清楚，再继续 KYC 或支付会更稳。
