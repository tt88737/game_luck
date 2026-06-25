# 包网平台底座规划

## 目标

为包网平台建立可长期维护的技术底座设计，覆盖 B 端后台、C 端 H5、玩家 App、自研游戏接入、多币种钱包、租户配置、渠道开关和 AI 开发边界。

## 当前技术路线

- B 端后台：RuoYi-Vue-Plus
- C 端 H5 / 官网 / 活动页 / PWA：Vue3 + Vite
- 玩家 App：Flutter
- 自研游戏 / 活动小游戏：预留 Cocos Creator 接入
- 核心后端：Spring Boot / Java
- 钱包体系：多币种钱包中心
- 数据库：MySQL
- 缓存：Redis

## 阶段计划

| 阶段 | 状态 | 目标 | 交付物 |
| --- | --- | --- | --- |
| 1. 技术路线确认 | complete | 明确底座组合和各端边界 | 本文件、findings.md |
| 2. 架构设计文档 | complete | 固化模块边界、钱包模型、渠道开关、AI 规则 | docs/superpowers/specs/2026-06-25-platform-architecture-design.md |
| 3. 设计自查 | complete | 检查范围、矛盾、遗漏和模糊项 | 已确认无 TODO/TBD 占位符，文档编码正常 |
| 4. 用户评审 | complete | 等待业务方确认设计方向 | 用户已确认继续生成第一阶段实施计划 |
| 5. 实施计划 | in_progress | 拆分第一阶段 MVP 开发任务 | docs/superpowers/plans/2026-06-25-phase-1-mvp.md |
| 6. 钱包中心细化设计 | complete | 固化钱包状态机、幂等、冻结/结算/冲正规则 | docs/superpowers/specs/2026-06-25-wallet-center-design.md |
| 7. 引入 RuoYi-Vue-Plus | complete | 将上游底座导入 backend 并记录来源 | backend/、docs/upstream/ruoyi-vue-plus.md |
| 8. 后端环境基线检查 | complete | 检查 JDK、Maven、Docker、MySQL、Redis 和 RuoYi 配置 | docs/implementation/backend-environment-baseline.md |
| 9. 本地启动配置 | complete | 新增 local profile 配置和本地启动说明 | application-local.yml、docs/implementation/backend-local-startup.md |
| 10. 后端构建验证 | complete | 安装 Maven、初始化数据库、构建 backend | Maven 3.9.16、ry-vue、BUILD SUCCESS |
| 11. 后端启动验证 | complete | 启动 ruoyi-admin 并验证 8080 | java -jar 启动成功，GET / 返回 200 |

## 关键决策

| 决策 | 结论 | 原因 |
| --- | --- | --- |
| 后台底座 | RuoYi-Vue-Plus | 权限、菜单、租户、日志、后台基础能力成熟 |
| H5 技术 | Vue3 + Vite | 当前团队更容易掌握，适合活动页、官网、PWA、支付页 |
| App 技术 | Flutter | 长期 App 体验、工程边界、动画和复杂交互更稳 |
| Web 是否用 Flutter | 不作为主 Web/H5 技术 | SEO、活动页、支付/KYC/追踪脚本、H5 游戏嵌入不如 Vue3 灵活 |
| 游戏引擎 | 预留 Cocos | 仅用于自研游戏和活动小游戏，不承担普通页面 |
| 钱包设计 | 多币种配置化 | 不写死 GC/SC/RC，支持真金和后续币种扩展 |

## 风险与待确认

| 风险 | 影响 | 当前处理 |
| --- | --- | --- |
| 真金 / 兑换 / Sweepstakes 合规 | 影响上架、支付、地区准入 | 设计中加入地区、KYC、渠道开关和审计 |
| 包网定制复杂度高 | 容易导致代码分叉 | 使用租户、品牌、渠道、币种配置中心控制 |
| AI 代码边界失控 | 后期难维护 | 在设计文档中加入 AI 开发规则 |
| 多端重复逻辑 | App 和 H5 行为不一致 | 统一 API、统一配置、统一钱包中心 |

## 错误记录

| 时间 | 错误 | 处理 |
| --- | --- | --- |
| 2026-06-25 | 当前目录不是 git 仓库，无法提交设计文档 | 继续创建文件，不擅自初始化 git |
