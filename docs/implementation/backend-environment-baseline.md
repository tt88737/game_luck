# 后端环境基线检查

## 1. 检查时间

2026-06-25

## 2. 当前结论

当前机器可以作为 GameLuck-Vue-Plus 后端开发环境，后端已经完成 Maven 构建验证。

主要阻塞：

- Maven 已安装到 `C:\tools\apache-maven-3.9.16`，当前会话配置 PATH 后可用。
- GameLuck 默认数据库 `ry-vue` 已创建并导入基础 SQL。
- Redis 服务在 `6379` 端口可访问，但当前 Redis 未配置密码；本项目 `application-local.yml` 已覆盖为空密码。
- Docker 已安装，但没有 `docker compose` / `docker-compose` 命令。
- GameLuck dev 配置默认启用 Spring Boot Admin Client 和 SnailJob，但本机 `9090`、`17888`、`8800` 端口未运行对应服务。

## 3. 工具检查结果

| 工具 | 状态 | 结果 |
| --- | --- | --- |
| Java | 可用 | OpenJDK 17.0.9 |
| Maven | 可用 | Apache Maven 3.9.16，路径 `C:\tools\apache-maven-3.9.16` |
| Docker | 可用 | Docker 29.5.2 |
| Docker Compose v2 | 不可用 | `docker compose` 不存在 |
| Docker Compose v1 | 不可用 | `docker-compose` 不存在 |
| MySQL Client | 可用 | MySQL 8.0.46 |
| Redis Server 命令 | 不可用 | `redis-server` 命令不存在 |
| Redis CLI | 不可用 | `redis-cli` 命令不存在 |

## 4. 服务检查结果

| 服务 / 端口 | 状态 | 说明 |
| --- | --- | --- |
| MySQL `localhost:3306` | 可连接 | `root/root` 可执行 `SELECT VERSION()` |
| 数据库 `ry-vue` | 已创建 | 已导入 GameLuck 基础 SQL |
| Redis `localhost:6379` | 可连接 | TCP 可连接，RESP `PING` 返回 `PONG` |
| Redis 密码 | 已处理 | 当前 Redis 已设置临时密码 `gameluck123` |
| 后端 `localhost:8080` | 已运行 | TCP 连接成功，根路径返回 200 |
| Spring Boot Admin `localhost:9090` | 未运行 | 端口不通 |
| SnailJob `localhost:17888` | 未运行 | 端口不通 |
| SnailJob UI `localhost:8800` | 未运行 | 端口不通 |
| MinIO `localhost:9000` | 未运行 | 端口不通 |

## 5. GameLuck 配置摘要

配置文件：

```text
backend/gameluck-admin/src/main/resources/application.yml
backend/gameluck-admin/src/main/resources/application-dev.yml
backend/pom.xml
```

关键配置：

| 项 | 值 |
| --- | --- |
| Java 版本 | 17 |
| GameLuck 版本 | 5.6.2 |
| Spring Boot | 3.5.15 |
| 默认 profile | Maven profile 决定，dev profile 对应 `application-dev.yml` |
| HTTP 端口 | 8080 |
| MySQL URL | `jdbc:mysql://localhost:3306/ry-vue` |
| MySQL 用户 | `root` |
| MySQL 密码 | `root` |
| Redis Host | `localhost` |
| Redis Port | `6379` |
| Redis Password | `gameluck123` |
| Tenant | enabled |
| API decrypt | enabled |
| Spring Boot Admin Client | enabled |
| SnailJob | enabled |

## 6. 已完成的启动前置工作

- 下载 Apache Maven 3.9.16 binary zip。
- 校验 Maven zip 的 SHA512。
- 解压 Maven 到 `C:\tools\apache-maven-3.9.16`。
- 当前会话中配置 `MAVEN_HOME` 和 PATH 后，`mvn -version` 可用。
- 创建数据库 `ry-vue`。
- 导入：
  - `backend/script/sql/ry_vue_5.X.sql`
  - `backend/script/sql/ry_job.sql`
  - `backend/script/sql/ry_workflow.sql`
- 验证核心表存在：
  - `sys_user`
  - `sys_tenant`
  - `sj_group_config`
  - `flow_definition`
- 执行后端构建：

```powershell
mvn clean package -Plocal -DskipTests
```

构建结果：

```text
BUILD SUCCESS
Total time: 02:10 min
```

产物：

```text
backend/gameluck-admin/target/gameluck-admin.jar
```

- 设置当前 Redis 实例密码为 `gameluck123`。
- 使用 jar 启动后端：

```powershell
java -jar gameluck-admin\target\gameluck-admin.jar --spring.profiles.active=local
```

- 验证 `localhost:8080` 端口和根路径响应成功。

## 7. 最短启动路径

### 7.1 当前会话设置 Maven

如果新开 PowerShell 后 `mvn` 不可用，先执行：

```powershell
$env:MAVEN_HOME='C:\tools\apache-maven-3.9.16'
$env:Path="$env:MAVEN_HOME\bin;$env:Path"
```

### 7.2 构建

```powershell
cd backend
mvn clean package -Plocal -DskipTests
```

### 7.3 启动

```powershell
mvn -pl gameluck-admin spring-boot:run -Plocal
```

## 8. 建议下一步

下一步不要直接修改 `application-dev.yml`。建议新增项目本地配置：

```text
backend/gameluck-admin/src/main/resources/application-local.yml
```

并使用 Maven `local` profile 启动，降低对上游 dev 配置的侵入。

本地 profile 建议覆盖：

- Redis 密码为空或匹配当前本机 Redis。
- 禁用 Spring Boot Admin Client。
- 禁用 SnailJob。
- 保留 MySQL `localhost:3306/ry-vue root/root`。

## 9. 当前不可验证项

当前已验证：

- Maven 依赖解析。
- 后端编译。

已验证：

- `gameluck-admin` 启动。
- `localhost:8080` 应用响应。

仍未验证：

- 登录流程。

登录流程需要前端或 API 登录参数配合验证。
