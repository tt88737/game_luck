# 包网平台底座进度

## 2026-06-25

- 与用户讨论了单人开发、框架选择和 AI 代码规范风险。
- 对比了 GameLuck Backend Base、Vue3、Nuxt、uni-app、Flutter、Cocos 的适用边界。
- 明确用户业务是包网平台，参考 tangluck.com，涉及 Social Casino / Sweepstakes / 真金扩展。
- 确认底层技术路线：
  - B 端后台：GameLuck Backend Base
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
- 用户确认开始引入 GameLuck Backend Base。
- 从 `GameLuck backend base source` 克隆上游 `5.X` 分支，导入 commit `e49f02f89e17ee5a4cc14048af99cc83d72872a7`。
- 将上游源码复制到 `backend/`，未复制上游 `.git` 目录。
- 创建上游记录文件：`docs/upstream/gameluck-vue-plus.md`。
- 验证 `backend/pom.xml`、`backend/gameluck-admin`、`backend/gameluck-common`、`backend/gameluck-modules` 存在。
- 当前环境未安装 `mvn` 命令，暂时无法执行 Maven 构建验证。
- 用户同意执行后端环境基线检查。
- 检查结果：Java 17 可用，Maven 不可用，Docker 可用但 Compose 不可用，MySQL 8.0.46 客户端可用，Redis 端口 6379 可访问但未配置密码。
- 检查 GameLuck dev 配置：默认 MySQL `localhost:3306/gameluck_vue root/root`，Redis 密码 `gameluck123`，Spring Boot Admin Client 和 SnailJob 默认启用。
- 当前 `gameluck_vue` 数据库不存在，`9090`、`17888`、`8800`、`9000` 端口未运行相关服务。
- 创建环境基线文档：`docs/implementation/backend-environment-baseline.md`。
- 用户确认新增本地启动配置。
- 创建 `backend/gameluck-admin/src/main/resources/application-local.yml`，用于本机最小启动，禁用 Spring Boot Admin Client 和 SnailJob，并覆盖 Redis 空密码。
- 创建 `docs/implementation/backend-local-startup.md`，记录 Maven、数据库、SQL 导入、构建和启动步骤。
- 用户选择自动处理环境。
- 下载并校验 Apache Maven 3.9.16，解压到 `C:\tools\apache-maven-3.9.16`。
- 创建数据库 `gameluck_vue`，导入 `gameluck_vue_5.X.sql`、`ry_job.sql`、`ry_workflow.sql`。
- 验证核心表 `sys_user`、`sys_tenant`、`sj_group_config`、`flow_definition` 存在。
- 执行 `mvn clean package -Plocal -DskipTests`，构建成功，产物为 `backend/gameluck-admin/target/gameluck-admin.jar`。
- 用户反馈 `localhost:8080` 未启动。
- 首次 Maven `spring-boot:run` 失败原因：未使用 `-am`，导致无法解析本仓库内模块依赖。
- Jar 启动首次失败原因：本机 Redis 无密码，而应用配置要求 `gameluck123`。
- 将 `application-local.yml` 的 Redis 密码明确设为 `gameluck123`，并给当前 Redis 实例设置临时密码。
- 补充 `snail-job.port: 28080`，避免示例 Job 读取占位符失败。
- 使用 `java -jar gameluck-admin\target\gameluck-admin.jar --spring.profiles.active=local` 启动成功。
- 验证 `localhost:8080` TCP 连接成功，`GET /` 返回 200。

## 2026-06-25 Admin UI

- Imported upstream GameLuck Admin UI into `admin-ui/` from `https://github.com/GameLuck/GameLuck Admin UI`, branch `5.X`, commit `d0d451967676707021b9857df529c395b27e90a7`.
- Added upstream record `docs/upstream/GameLuck Admin UI.md`.
- Changed `admin-ui/.env.development` dev port from `80` to `5173` to avoid local Windows port permission/conflict issues.
- Ran `pnpm install` successfully in `admin-ui/`.
- Started `pnpm dev`; Admin UI is available at `http://localhost:5173/`.
- Verified `GET http://localhost:5173/` returns 200 and backend `GET http://localhost:8080/` returns 200.
- Ran `pnpm build:dev` successfully. Only large chunk warnings were reported by Vite.
- Added `docs/implementation/admin-ui-local-startup.md` for local startup and troubleshooting.
