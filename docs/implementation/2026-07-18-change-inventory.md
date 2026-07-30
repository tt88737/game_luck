# 2026-07-18 工作区变更分组清单

## 目的

当前工作区包含多阶段累计变更。本文把变更拆成可评审、可提交、可归档的组，避免一次性提交时混入本地运行日志、截图证据或 crash dump。

## 建议提交组

### 1. Member 基础与合规字段

建议纳入代码提交：

- `backend/gameluck-modules/gameluck-member/src/main/java/com/gameluck/member/client/service/ClientAuthService.java`
- `backend/gameluck-modules/gameluck-member/src/main/java/com/gameluck/member/service/MemberIdGenerator.java`
- `backend/gameluck-modules/gameluck-member/src/main/java/com/gameluck/member/service/impl/MemberProfileServiceImpl.java`
- `backend/gameluck-modules/gameluck-member/src/test/java/com/gameluck/member/client/service/ClientAuthServiceTest.java`
- `backend/gameluck-modules/gameluck-member/src/test/java/com/gameluck/member/service/impl/MemberProfileServiceImplTest.java`
- `backend/script/sql/gameluck_member_public_id.sql`

用途：会员公开 ID、注册默认资料、风险等级和后续 KYC/合规基础字段。

### 2. Wallet 兑换、流水与释放能力

建议纳入代码提交：

- `backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/client/controller/ClientWalletController.java`
- `backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/client/domain/bo/ClientExchangeOrderBo.java`
- `backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/client/domain/vo/ClientExchangeOrderVo.java`
- `backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/client/service/ClientWalletService.java`
- `backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/controller/WalletExchangeOrderController.java`
- `backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/domain/bo/WalletExchangeOrderBo.java`
- `backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/domain/vo/WalletExchangeOrderVo.java`
- `backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/mapper/WalletExchangeOrderMapper.java`
- `backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/mapper/WalletTurnoverTaskMapper.java`
- `backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/service/IWalletExchangeOrderAdminService.java`
- `backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/service/IWalletExchangeOrderService.java`
- `backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/service/IWalletTurnoverTaskService.java`
- `backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/service/impl/WalletCoreServiceImpl.java`
- `backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/service/impl/WalletExchangeOrderAdminServiceImpl.java`
- `backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/service/impl/WalletExchangeOrderServiceImpl.java`
- `backend/gameluck-modules/gameluck-wallet/src/main/java/com/gameluck/wallet/service/impl/WalletTurnoverTaskServiceImpl.java`
- `backend/gameluck-modules/gameluck-wallet/src/main/resources/mapper/wallet/WalletTurnoverTaskMapper.xml`
- Wallet 相关测试文件

用途：C 端币种兑换执行、B 端兑换订单可见性、流水释放和钱包幂等能力。

### 3. Payment / Purchase 履约基础

建议纳入代码提交：

- `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/client/service/ClientPurchaseService.java`
- `backend/gameluck-modules/gameluck-payment/src/main/java/com/gameluck/payment/mapper/PurchaseOrderMapper.java`
- `backend/gameluck-modules/gameluck-payment/src/test/java/com/gameluck/payment/client/service/ClientPurchaseServiceTest.java`
- 已存在的 `payment` domain/controller/service/admin-ui purchase/deposit 相关文件如无遗漏，应和本组一起提交。

用途：购买产品展示、模拟支付成功、购买限购、钱包入账和流水要求生成。

### 4. Redemption 合规门禁与资格策略

建议纳入代码提交：

- `backend/gameluck-modules/gameluck-redemption/pom.xml`
- `backend/gameluck-modules/gameluck-redemption/src/main/java/com/gameluck/redemption/client/service/ClientRedemptionService.java`
- `backend/gameluck-modules/gameluck-redemption/src/main/java/com/gameluck/redemption/controller/RedemptionEligibilityPolicyController.java`
- `backend/gameluck-modules/gameluck-redemption/src/main/java/com/gameluck/redemption/domain/RedemptionEligibilityPolicy.java`
- `backend/gameluck-modules/gameluck-redemption/src/main/java/com/gameluck/redemption/domain/bo/RedemptionEligibilityPolicyBo.java`
- `backend/gameluck-modules/gameluck-redemption/src/main/java/com/gameluck/redemption/domain/vo/RedemptionEligibilityPolicyVo.java`
- `backend/gameluck-modules/gameluck-redemption/src/main/java/com/gameluck/redemption/mapper/RedemptionEligibilityPolicyMapper.java`
- `backend/gameluck-modules/gameluck-redemption/src/main/java/com/gameluck/redemption/service/IRedemptionEligibilityPolicyService.java`
- `backend/gameluck-modules/gameluck-redemption/src/main/java/com/gameluck/redemption/service/impl/RedemptionEligibilityPolicyServiceImpl.java`
- `backend/gameluck-modules/gameluck-redemption/src/test/java/com/gameluck/redemption/client/service/ClientRedemptionServiceTest.java`
- `backend/gameluck-modules/gameluck-redemption/src/test/java/com/gameluck/redemption/service/impl/RedemptionEligibilityPolicyServiceImplTest.java`

用途：把兑换 denied-region 从硬编码迁移到可配置策略，并保持兑换订单创建前阻断。

### 5. Admin UI 运营页面与多语言

建议纳入代码提交：

- `admin-ui/src/api/redemption/eligibilityPolicy/`
- `admin-ui/src/api/wallet/exchangeOrder/`
- `admin-ui/src/views/redemption/eligibility-policy/`
- `admin-ui/src/views/wallet/exchange-order/`
- `admin-ui/src/lang/en_US.ts`
- `admin-ui/src/lang/zh_CN.ts`
- `admin-ui/src/utils/businessLabels.ts`
- `admin-ui/src/utils/i18nTitle.ts`

用途：B 端兑换资格策略、钱包兑换订单、业务标签和 i18n 可见性。

### 6. H5 玩家端购买与钱包体验

建议纳入代码提交：

- `h5/src/api/client.ts`
- `h5/src/types/client.ts`
- `h5/src/views/WalletView.vue`
- `h5/src/style.css`
- 如 `PurchaseView.vue`、router、i18n 已经是前序未提交新增/变更，也应与本组一起检查并提交。

用途：C 端购买产品、钱包兑换、余额刷新和玩家端闭环入口。

### 7. SQL 与 i18n 资源

建议纳入代码提交：

- `backend/script/sql/gameluck_wallet.sql`
- `backend/gameluck-admin/src/main/resources/i18n/messages.properties`
- `backend/gameluck-admin/src/main/resources/i18n/messages_zh_CN.properties`
- `backend/gameluck-admin/src/main/resources/i18n/messages_en_US.properties`

用途：菜单、字典、策略种子、后端提示文案和 SQL 幂等导入。

### 8. 设计、计划和进度文档

建议纳入文档提交：

- `docs/superpowers/specs/2026-07-17-client-redemption-compliance-gate-design.md`
- `docs/superpowers/specs/2026-07-18-redemption-eligibility-policy-design.md`
- `docs/superpowers/plans/2026-07-17-admin-wallet-exchange-order.md`
- `docs/superpowers/plans/2026-07-17-client-redemption-compliance-gate.md`
- `docs/superpowers/plans/2026-07-17-h5-wallet-exchange.md`
- `docs/superpowers/plans/2026-07-17-purchase-limit-enforcement.md`
- `docs/superpowers/plans/2026-07-17-wallet-exchange-runtime.md`
- `docs/superpowers/plans/2026-07-18-redemption-eligibility-policy.md`
- `docs/implementation/2026-07-18-delivery-status-and-next-plan.md`
- `docs/implementation/2026-07-18-change-inventory.md`
- `task_plan.md`
- `progress.md`

用途：保留决策依据、验收步骤和阶段恢复上下文。

## 已归档的本地证据

这些目录已经移动到 `artifacts/2026-07-18/`，作为本地验收证据保留。`artifacts/` 已加入 `.gitignore`，不会混入业务代码提交。

| 目录 | 文件数 | 用途 |
| --- | ---: | --- |
| `artifacts/2026-07-18/admin-member-compliance-screens/` | 5 | 会员合规字段 Admin UI 验证 |
| `artifacts/2026-07-18/admin-redemption-policy-screens/` | 8 | 兑换资格策略页面和 H5 deny/allow 初始验证 |
| `artifacts/2026-07-18/admin-redemption-policy-crud-screens/` | 2 | 兑换资格策略 API CRUD 验证 |
| `artifacts/2026-07-18/admin-redemption-policy-form-screens/` | 9 | 兑换资格策略 Admin 表单验证 |
| `artifacts/2026-07-18/admin-redemption-policy-operlog-screens/` | 4 | 操作日志可见性验证 |
| `artifacts/2026-07-18/admin-ui-runtime-screens/` | 11 | 钱包兑换订单 Admin UI 验证 |
| `artifacts/2026-07-18/h5-redemption-gate-screens/` | 6 | H5 兑换合规门禁验证 |
| `artifacts/2026-07-18/h5-redemption-policy-screens/` | 4 | H5 兑换资格策略 deny/allow 验证 |
| `artifacts/2026-07-18/h5-wallet-exchange-screens/` | 6 | H5 钱包兑换体验验证 |

## 建议清理或忽略的本地运行产物

运行日志：

- `admin-ui-5173*.log`
- `backend-8080*.log`
- `h5-5174*.log`
- `backend-runtime-logs/*.log`

Crash dump：

- `backend/hs_err_pid15796.mdmp`
- `backend/hs_err_pid7056.mdmp`
- `backend/hs_err_pid9228.mdmp`
- `backend/gameluck-modules/gameluck-wallet/hs_err_pid15024.mdmp`
- `backend/gameluck-modules/gameluck-wallet/hs_err_pid7364.mdmp`

这些 `.mdmp` 当前均为 0 字节。`*.log`、`*.mdmp` 和 `backend-runtime-logs/` 已加入 `.gitignore`，不会出现在待提交列表中。

## 推荐提交顺序

1. 文档与计划：先提交 specs/plans/progress，固定阶段上下文。
2. SQL 与 i18n：提交菜单、字典、消息资源，确保本地环境可复现。
3. Member + Wallet：提交会员基础、钱包兑换、流水释放。
4. Payment + H5 purchase/wallet：提交购买履约和玩家端入口。
5. Redemption policy + Admin UI：提交兑换资格策略、B 端页面和运行 gate。
6. 最后单独处理证据与日志：证据已归档到 ignored `artifacts/`，日志和 dump 默认不进代码提交。

## 下一步建议

在正式进入 KYC 或支付真实化前，可以按上面的提交顺序拆分 review。当前截图证据、日志和 dump 已通过 `.gitignore` 排除，不会污染待提交代码清单。
