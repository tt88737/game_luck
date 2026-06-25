# 包网平台底座进度

## 2026-06-25

- 与用户讨论了单人开发、框架选择和 AI 代码规范风险。
- 对比了 RuoYi-Vue-Plus、Vue3、Nuxt、uni-app、Flutter、Cocos 的适用边界。
- 明确用户业务是包网平台，参考 tangluck.com，涉及 Social Casino / Sweepstakes / 真金扩展。
- 确认底层技术路线：
  - B 端后台：RuoYi-Vue-Plus
  - C 端 H5 / 官网 / 活动页 / PWA：Vue3 + Vite
  - 玩家 App：Flutter
  - 自研游戏 / 活动小游戏：预留 Cocos Creator 接入
  - 后端：Spring Boot / Java
  - 钱包：多币种钱包中心
- 检查 `C:\codex\project`，目录为空。
- 检查 git 状态，发现当前目录不是 git 仓库。
- 创建规划文件：`task_plan.md`、`findings.md`、`progress.md`。
- 创建架构设计文档：`docs/superpowers/specs/2026-06-25-platform-architecture-design.md`。
- 完成设计自查：未发现 TODO/TBD 占位符；UTF-8 内容读取正常。
- 更新 `task_plan.md`，当前进入用户评审阶段。
- 用户确认继续生成第一阶段 MVP 实施计划。
- 创建 `AI_RULES.md`。
- 创建实施文档：
  - `docs/implementation/phase-1-mvp-plan.md`
  - `docs/implementation/module-breakdown.md`
  - `docs/implementation/db-draft.md`
  - `docs/implementation/api-draft.md`
- 创建执行计划：`docs/superpowers/plans/2026-06-25-phase-1-mvp.md`。
- 用户选择继续细化钱包设计。
- 创建钱包中心设计文档：`docs/superpowers/specs/2026-06-25-wallet-center-design.md`。
- 补充 `docs/implementation/db-draft.md`：新增 `wallet_transaction` 和 `wallet_manual_review`。
- 补充 `docs/implementation/api-draft.md`：新增钱包交易查询和人工冲正接口草案。
- 用户确认开始引入 RuoYi-Vue-Plus。
- 从 `https://github.com/dromara/RuoYi-Vue-Plus` 克隆上游 `5.X` 分支，导入 commit `e49f02f89e17ee5a4cc14048af99cc83d72872a7`。
- 将上游源码复制到 `backend/`，未复制上游 `.git` 目录。
- 创建上游记录文件：`docs/upstream/ruoyi-vue-plus.md`。
- 验证 `backend/pom.xml`、`backend/ruoyi-admin`、`backend/ruoyi-common`、`backend/ruoyi-modules` 存在。
- 当前环境未安装 `mvn` 命令，暂时无法执行 Maven 构建验证。
