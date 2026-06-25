# 后端本地启动说明

## 1. 目标

用最少依赖启动 `backend/ruoyi-admin`，先验证 RuoYi-Vue-Plus 后端能在本机跑起来。

本地启动使用：

```text
Maven profile: local
Spring profile: local
配置文件: backend/ruoyi-admin/src/main/resources/application-local.yml
```

## 2. 当前机器状态

已确认：

- Java 17 可用。
- MySQL 8.0.46 可用，`root/root` 能连接。
- Redis `localhost:6379` 可连接，但无密码。
- Docker 可用，但 Docker Compose 不可用。

仍需处理：

- Maven 不可用，需要安装或加入 PATH。
- 数据库 `ry-vue` 尚未创建。
- 基础 SQL 尚未导入。

## 3. 安装 Maven

安装后必须确认：

```powershell
mvn -version
```

期望看到 Maven 版本、Java 17 路径。

## 4. 创建数据库

在项目根目录执行：

```powershell
mysql -uroot -proot -e "CREATE DATABASE IF NOT EXISTS `ry-vue` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;"
```

验证：

```powershell
mysql -uroot -proot -N -e "SHOW DATABASES LIKE 'ry-vue';"
```

期望输出：

```text
ry-vue
```

## 5. 导入基础 SQL

执行：

```powershell
mysql -uroot -proot ry-vue < backend\script\sql\ry_vue_5.X.sql
mysql -uroot -proot ry-vue < backend\script\sql\ry_job.sql
mysql -uroot -proot ry-vue < backend\script\sql\ry_workflow.sql
```

验证核心表：

```powershell
mysql -uroot -proot -N ry-vue -e "SHOW TABLES LIKE 'sys_user'; SHOW TABLES LIKE 'sys_tenant'; SHOW TABLES LIKE 'sj_group_config';"
```

## 6. 本地配置说明

新增配置文件：

```text
backend/ruoyi-admin/src/main/resources/application-local.yml
```

它覆盖：

- 禁用 Spring Boot Admin Client。
- 禁用 SnailJob。
- 使用本机 MySQL：`localhost:3306/ry-vue root/root`。
- 使用本机 Redis：`localhost:6379`，空密码。

这样可以避开本机未启动的服务：

```text
localhost:9090  Spring Boot Admin
localhost:17888 SnailJob
localhost:8800  SnailJob UI
```

## 7. 构建后端

进入后端目录：

```powershell
cd backend
```

构建：

```powershell
mvn clean package -Plocal -DskipTests
```

期望：

```text
BUILD SUCCESS
```

## 8. 启动后端

方式一：Maven 启动

```powershell
mvn -pl ruoyi-admin spring-boot:run -Plocal
```

方式二：IDE 启动

启动类：

```text
backend/ruoyi-admin/src/main/java/org/dromara/DromaraApplication.java
```

运行参数需要确保使用 `local` profile。Maven 构建时会把 `@profiles.active@` 替换为 `local`。

## 9. 启动验证

端口验证：

```powershell
Test-NetConnection -ComputerName localhost -Port 8080
```

接口验证：

```powershell
curl http://localhost:8080
```

如果返回登录相关页面、接口响应或 401/404 等应用级响应，说明后端进程已经启动。

## 10. 常见问题

### 10.1 Maven 不存在

现象：

```text
mvn : The term 'mvn' is not recognized
```

处理：

- 安装 Maven。
- 配置 `MAVEN_HOME`。
- 将 `%MAVEN_HOME%\bin` 加入 PATH。
- 重新打开 PowerShell。

### 10.2 Redis 密码错误

本项目本地配置已将 Redis 密码设置为空。

如果仍看到 Redis AUTH 错误，确认启动使用的是 `local` profile，而不是 `dev` profile。

### 10.3 数据库不存在

现象：

```text
Unknown database 'ry-vue'
```

处理：

```powershell
mysql -uroot -proot -e "CREATE DATABASE IF NOT EXISTS `ry-vue` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;"
```

### 10.4 表不存在

现象：

```text
Table 'ry-vue.sys_user' doesn't exist
```

处理：重新执行基础 SQL 导入。

## 11. 下一步

后端构建和启动通过后，再继续：

1. 创建 `backend/docs/business-modules.md`。
2. 添加钱包 SQL 草案到 `backend/script/sql/package_wallet_001.sql`。
3. 开始 `wallet-center` 后端模块实现。
