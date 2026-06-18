# Tang Luck 开发进度

## 2026-06-18
- 盘点当前目录，确认交付文档齐全但暂无源码工程。
- 阅读总入口、接口草案、数据库设计说明、页面原型说明、15 天冲刺表和 QA 测试矩阵。
- 建立 `task_plan.md`、`findings.md`、`progress.md` 作为后续落地开发记录。
- 用户确认采用 `Java Spring Boot + Vue 3 + MySQL`。
- 检查本地环境：Java 17 可用，Node 22/npm 11 可用，未安装 Maven，因此后端采用 Gradle Wrapper。
- 写入设计文档 `TangLuck-P0A-落地设计.md`。
- 写入实施计划 `TangLuck-P0A-实施计划.md`，覆盖后端骨架、数据库、注册钱包、活动发奖、后台审计、前端 C 端/后台和 E2E 验证。
- 用户要求交付文档放在当前目录根下，已从 `docs/superpowers/` 移动到根目录。
- 创建开发分支 `develop/p0a`，开始执行 `TangLuck-P0A-实施计划.md`。
- 新增 `docker-compose.yml`，定义本地 MySQL 8 开发服务。
- 生成 Spring Boot 3.5.15 后端骨架和 Vite/Vue 3 前端骨架；原计划中的 Spring Boot 3.3.6 已被 Spring Initializr 拒绝。
- 后端骨架测试首次失败，根因是默认 `@SpringBootTest` 连接本地 MySQL 账号失败；新增 H2 测试 profile 让基线测试独立运行。
- 完成 P0-A 核心数据库迁移和演示种子数据，包含 13 张核心表、地区/合规文档/活动/Coupon 初始数据。
- 验证通过：`backend .\gradlew.bat test`，`frontend npm run build`。
- 完成统一 API 错误模型、业务错误码、业务异常和全局异常处理；覆盖 `REGION_BLOCKED` 等 P0-A 错误码到 HTTP 状态映射。
- 验证通过：`backend .\gradlew.bat test`，`frontend npm run build`。
- 完成注册服务、合规地区校验、条款确认日志、GC/SC 钱包创建、合规文档查询接口和基础 Security 配置。
- 验证通过：`backend .\gradlew.bat test`，`frontend npm run build`。
