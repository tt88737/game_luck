# 第一阶段 MVP 实施计划

## 1. 目标

第一阶段只完成包网平台最小闭环，不追求完整商业化能力。

最小闭环定义：

```text
平台能创建租户和品牌
品牌能配置渠道和币种
玩家能注册登录
玩家拥有多币种钱包
模拟游戏能下注和派彩
活动能发奖励
玩家能提交兑换申请
后台能审核兑换
后台能查看基础报表和审计记录
```

## 2. 不做范围

P0 不做：

- 完整 BI 平台。
- 复杂代理分销。
- 自动风控模型。
- 多支付服务商深度集成。
- App Store / Google Play 正式上架。
- 大规模自研游戏内容。
- 完整 KYC 服务商集成。
- 真实出款自动化。

## 3. 阶段拆分

| 阶段 | 目标 | 结果 |
| --- | --- | --- |
| P0-1 | 引入并固定 RuoYi-Vue-Plus 底座 | 后台能启动，基础权限可用 |
| P0-2 | 建立业务模块骨架 | tenant、channel、member、wallet、game 等模块边界明确 |
| P0-3 | 完成多币种钱包核心 | 币种、账户、账变、冻结、结算可用 |
| P0-4 | 完成模拟游戏闭环 | 启动游戏、下注、派彩、幂等回调可验证 |
| P0-5 | 完成活动奖励和兑换审核 | 奖励入账、兑换冻结、审核结算、失败解冻可验证 |
| P0-6 | 完成 Vue3 H5 MVP | 登录、钱包、游戏列表、活动、兑换入口 |
| P0-7 | 完成 Flutter App MVP 骨架 | 登录、首页、钱包、游戏列表、个人中心 |
| P0-8 | 完成基础报表和审计 | 后台可查关键数据和敏感操作 |

## 4. 推荐仓库结构

当前仓库建议发展为 monorepo：

```text
game_luck/
  backend/                 RuoYi-Vue-Plus 二开后端和后台前端
  h5/                      Vue3 + Vite C 端 H5
  app/                     Flutter 玩家 App
  games/                   Cocos 自研游戏和小游戏预留
  docs/
    implementation/
    superpowers/
  scripts/                 本地启动、数据库初始化、验证脚本
  AI_RULES.md
```

## 5. 底座引入策略

RuoYi-Vue-Plus 建议采用“复制入仓 + 记录 upstream”的方式，而不是 git submodule。

原因：

- 便于单仓库提交和部署。
- 便于 AI 在明确边界内修改。
- 便于后续记录与上游的差异。
- 避免 submodule 对单人开发造成额外操作成本。

引入后建议保留文件：

```text
docs/upstream/ruoyi-vue-plus.md
```

记录：

- 上游仓库地址。
- 引入日期。
- 引入 commit。
- 本项目允许修改的目录。
- 本项目禁止修改的目录。

## 6. 第一批开发顺序

### 6.1 文档和规则

先完成：

- `AI_RULES.md`
- `docs/implementation/module-breakdown.md`
- `docs/implementation/db-draft.md`
- `docs/implementation/api-draft.md`

### 6.2 后端底座

再完成：

- 拉取 RuoYi-Vue-Plus。
- 跑通 MySQL、Redis、后台登录。
- 固定基础配置。
- 建立业务模块包结构。

### 6.3 钱包优先

优先实现 `wallet-center`：

- 币种配置。
- 租户币种配置。
- 会员钱包账户。
- 钱包账变流水。
- 幂等交易单。
- 冻结、解冻、结算。

### 6.4 模拟闭环

使用模拟游戏供应商和模拟支付 / 兑换流程验证：

- 游戏下注扣款。
- 游戏派彩入账。
- 活动奖励入账。
- 兑换申请冻结。
- 审核失败解冻。
- 审核成功结算。

## 7. 验收标准

P0 完成时必须满足：

- 后台可创建租户和品牌。
- 后台可配置渠道开关。
- 后台可配置 GC、SC、RC 三个币种。
- 玩家可注册登录。
- 玩家可查看多币种余额。
- 钱包每次变更都有流水。
- 重复游戏回调不会重复入账或扣账。
- 兑换申请会冻结余额。
- 兑换审核失败会解冻余额。
- 兑换审核成功会结算冻结金额。
- 后台可查询账变、订单、兑换申请、审计日志。

## 8. 提交节奏

建议每个阶段至少一个提交：

```text
docs: add implementation docs
chore: import ruoyi-vue-plus upstream
feat(wallet): add currency and account model
feat(wallet): add ledger and transaction idempotency
feat(game): add mock provider callbacks
feat(redemption): add freeze and settlement flow
feat(h5): add player wallet MVP
feat(app): add flutter app shell
```
