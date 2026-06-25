# 后端环境基线检查

## 1. 检查时间

2026-06-25

## 2. 当前结论

当前机器可以作为 RuoYi-Vue-Plus 后端开发环境，但还不能直接构建和启动。

主要阻塞：

- Maven 未安装或未加入 PATH。
- RuoYi 默认数据库 `ry-vue` 尚不存在。
- Redis 服务在 `6379` 端口可访问，但当前 Redis 未配置密码；RuoYi dev 配置要求密码 `ruoyi123`。
- Docker 已安装，但没有 `docker compose` / `docker-compose` 命令。
- RuoYi dev 配置默认启用 Spring Boot Admin Client 和 SnailJob，但本机 `9090`、`17888`、`8800` 端口未运行对应服务。

## 3. 工具检查结果

| 工具 | 状态 | 结果 |
| --- | --- | --- |
| Java | 可用 | OpenJDK 17.0.9 |
| Maven | 不可用 | `mvn` 命令不存在 |
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
| 数据库 `ry-vue` | 不存在 | `SHOW DATABASES LIKE 'ry-vue'` 无结果 |
| Redis `localhost:6379` | 可连接 | TCP 可连接，RESP `PING` 返回 `PONG` |
| Redis 密码 | 不匹配 | 当前 Redis 未配置密码，RuoYi dev 配置要求 `ruoyi123` |
| 后端 `localhost:8080` | 未运行 | 端口不通 |
| Spring Boot Admin `localhost:9090` | 未运行 | 端口不通 |
| SnailJob `localhost:17888` | 未运行 | 端口不通 |
| SnailJob UI `localhost:8800` | 未运行 | 端口不通 |
| MinIO `localhost:9000` | 未运行 | 端口不通 |

## 5. RuoYi 配置摘要

配置文件：

```text
backend/ruoyi-admin/src/main/resources/application.yml
backend/ruoyi-admin/src/main/resources/application-dev.yml
backend/pom.xml
```

关键配置：

| 项 | 值 |
| --- | --- |
| Java 版本 | 17 |
| RuoYi 版本 | 5.6.2 |
| Spring Boot | 3.5.15 |
| 默认 profile | Maven profile 决定，dev profile 对应 `application-dev.yml` |
| HTTP 端口 | 8080 |
| MySQL URL | `jdbc:mysql://localhost:3306/ry-vue` |
| MySQL 用户 | `root` |
| MySQL 密码 | `root` |
| Redis Host | `localhost` |
| Redis Port | `6379` |
| Redis Password | `ruoyi123` |
| Tenant | enabled |
| API decrypt | enabled |
| Spring Boot Admin Client | enabled |
| SnailJob | enabled |

## 6. 最短启动路径

### 6.1 必须先解决

1. 安装 Maven 或配置 Maven 到 PATH。
2. 创建并初始化 MySQL 数据库 `ry-vue`。
3. 处理 Redis 密码不一致问题。

### 6.2 推荐本地处理方式

优先本地直接跑，不依赖 Docker Compose：

1. 安装 Maven。
2. 创建数据库：

```powershell
mysql -uroot -proot -e "CREATE DATABASE IF NOT EXISTS `ry-vue` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;"
```

3. 导入基础 SQL：

```powershell
mysql -uroot -proot ry-vue < backend\script\sql\ry_vue_5.X.sql
mysql -uroot -proot ry-vue < backend\script\sql\ry_job.sql
mysql -uroot -proot ry-vue < backend\script\sql\ry_workflow.sql
```

4. 二选一处理 Redis：

方案 A：给本地 Redis 配置密码 `ruoyi123`。

方案 B：新增本地 profile 覆盖 Redis 密码为空，不直接改上游 `application-dev.yml`。

5. 如果暂时不启动监控中心和 SnailJob，建议新增本地 profile 覆盖：

```yaml
spring.boot.admin.client:
  enabled: false

snail-job:
  enabled: false
```

6. 构建：

```powershell
cd backend
mvn clean package -Pdev -DskipTests
```

7. 启动：

```powershell
mvn -pl ruoyi-admin spring-boot:run -Pdev
```

## 7. 建议下一步

下一步不要直接修改 `application-dev.yml`。建议新增项目本地配置：

```text
backend/ruoyi-admin/src/main/resources/application-local.yml
```

并使用 Maven `local` profile 启动，降低对上游 dev 配置的侵入。

本地 profile 建议覆盖：

- Redis 密码为空或匹配当前本机 Redis。
- 禁用 Spring Boot Admin Client。
- 禁用 SnailJob。
- 保留 MySQL `localhost:3306/ry-vue root/root`。

## 8. 当前不可验证项

由于 Maven 不可用，暂时不能验证：

- Maven 依赖解析。
- 后端编译。
- 单元测试。
- `ruoyi-admin` 启动。

这些需要在 Maven 可用后重新验证。
