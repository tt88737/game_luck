# Phase 0 字典与 H5 国际化落地说明

## 目标

Phase 0 先固化两个平台级基础能力：

1. 业务状态、类型、决策结果统一进入 `sys_dict_type` / `sys_dict_data`。
2. H5 建立中文和英文文案入口，后续页面按业务切片逐步迁移到 `t()`。

这一步不改变钱包、活动、兑换、游戏等业务逻辑，只为后续 TangLuck 复刻提供统一维护入口。

## 字典 SQL

新增文件：

```text
backend/script/sql/gameluck_platform_dict.sql
```

导入方式：

```powershell
.\backend\script\bin\import-sql-utf8.ps1 -SqlPath backend\script\sql\gameluck_platform_dict.sql
```

不要使用下面这种方式导入包含中文的 SQL：

```powershell
Get-Content backend\script\sql\gameluck_platform_dict.sql | mysql ...
```

PowerShell 管道可能造成中文转码错误，导致数据库保存为问号。

## 字典职责边界

字典表负责：

- B 端筛选项
- B 端表格标签
- 状态颜色
- 运营可读文案
- 统一状态值清单

状态流转由后端 service 负责，不能交给字典表或前端按钮决定。

例如 `gl_redemption_status` 可以定义：

```text
PENDING / APPROVED / REJECTED / PAID / FAILED
```

但是否允许 `PENDING -> APPROVED`，以及通过后是否调用钱包结算，必须由兑换服务控制。

## 第一批字典范围

本次新增第一批 `gl_*` 字典：

- `gl_common_status`
- `gl_yes_no`
- `gl_member_status`
- `gl_kyc_status`
- `gl_geo_status`
- `gl_risk_decision`
- `gl_currency_type`
- `gl_wallet_account_status`
- `gl_wallet_biz_type`
- `gl_wallet_freeze_status`
- `gl_promotion_type`
- `gl_promotion_status`
- `gl_reward_claim_status`
- `gl_game_status`
- `gl_game_session_status`
- `gl_deposit_status`
- `gl_redemption_status`

后续新增模块状态时继续使用 `gl_模块_字段` 命名，不在业务表、前端页面或接口里临时造中文状态。

## H5 国际化

新增文件：

```text
h5/src/i18n/messages.ts
h5/src/i18n/index.ts
```

当前支持：

```text
zh-CN
en-US
```

语言选择保存到浏览器：

```text
gameluck:h5:locale
```

页面使用方式：

```ts
import { t } from '../i18n'

t('navWallet')
```

新增玩家可见文案时：

1. 先在 `h5/src/i18n/messages.ts` 增加中文和英文 key。
2. 页面使用 `t('key')`。
3. 不从后端返回中文拼接页面文案。
4. 不在页面中新增硬编码中文，历史页面文案按后续业务切片逐步迁移。

## 验收

Phase 0 当前验收命令：

```powershell
rg -n "gl_redemption_status|gl_wallet_biz_type|WHERE NOT EXISTS" backend/script/sql/gameluck_platform_dict.sql
npm --prefix h5 run build
rg -n "gameluck_platform_dict.sql|状态流转|gameluck:h5:locale" docs/implementation/phase-0-dictionary-i18n.md
```
