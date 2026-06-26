# GameLuck

GameLuck 是一个面向包网平台业务的全栈项目。当前阶段重点建设 B 端后台、后端基础服务和后续 C 端接入能力。

## 项目定位

- 平台后台：租户、用户、角色、菜单、参数、日志、文件、任务、代码生成等基础管理能力。
- 后端服务：基于 Java、Spring Boot、MySQL、Redis 的模块化单体架构。
- 后台前端：基于 Vue 3、TypeScript、Vite、Element Plus 的管理后台。
- 后续扩展：预留 Vue3 H5、Flutter App、多币种钱包中心和 Cocos 游戏接入边界。

## 目录结构

```text
backend/      后端服务和后台接口
admin-ui/     后台管理前端
docs/         项目规划、环境、接口和实施文档
```

## 技术栈

- 后端：Java 17、Spring Boot 3、MyBatis-Plus、Sa-Token、MySQL、Redis
- 前端：Vue 3、TypeScript、Vite、Element Plus、Pinia
- 构建：Maven、pnpm

## 本地启动

后端：

```powershell
cd backend
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-admin -am package -Plocal -DskipTests
java -jar gameluck-admin\target\gameluck-admin.jar --spring.profiles.active=local
```

前端：

```powershell
cd admin-ui
pnpm install
pnpm dev
```

默认访问：

```text
前端: 本机 5173 端口
后端: 本机 8080 端口
```

## 本地依赖

- MySQL：默认库名 `gameluck_vue`，账号 `root`，密码 `root`
- Redis 或 Memurai：默认地址 `localhost:6379`，密码 `gameluck123`

实际配置以 `backend/gameluck-admin/src/main/resources/application-local.yml` 为准。

## 开发约定

- 新业务模块优先放在独立业务边界内，避免直接污染公共模块。
- 公共模块只沉淀稳定能力，不放具体业务流程。
- 后台菜单、权限、接口、数据表变更要同步记录。
- AI 生成代码必须经过人工 review、构建验证和必要的运行验证。

## 当前状态

- 后端模块已统一使用 `gameluck-*` 命名。
- 后台品牌已替换为 GameLuck。
- 当前阶段以后台基础能力和后续业务边界为主，减少无关演示入口。
