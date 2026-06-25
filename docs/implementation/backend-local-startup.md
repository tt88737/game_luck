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

已处理：

- Maven 3.9.16 已下载并解压到 `C:\tools\apache-maven-3.9.16`。
- 数据库 `ry-vue` 已创建。
- 基础 SQL 已导入。

## 3. 安装 Maven

安装后必须确认：

```powershell
mvn -version
```

期望看到 Maven 版本、Java 17 路径。

当前会话临时配置方式：

```powershell
$env:MAVEN_HOME='C:\tools\apache-maven-3.9.16'
$env:Path="$env:MAVEN_HOME\bin;$env:Path"
mvn -version
```

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
- 使用本机 Redis：`localhost:6379`，密码 `ruoyi123`。

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

已验证结果：

```text
BUILD SUCCESS
Total time: 02:10 min
```

## 8. 启动后端

方式一：Maven 启动

```powershell
mvn -pl ruoyi-admin -am spring-boot:run -Plocal
```

注意：直接执行 `mvn -pl ruoyi-admin spring-boot:run -Plocal` 会因为没有 `-am` 而找不到本仓库内的兄弟模块依赖。

方式二：Jar 启动，当前推荐

```powershell
cd C:\codex\project\backend
java -jar ruoyi-admin\target\ruoyi-admin.jar --spring.profiles.active=local
```

方式三：IDE 启动

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

已验证结果：

```text
localhost:8080 TCP 连接成功
GET http://localhost:8080 返回 200
响应内容：欢迎使用RuoYi-Vue-Plus后台管理框架，请通过前端地址访问。
```

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

本项目本地配置使用 Redis 密码 `ruoyi123`。

如果本机 Redis 没有密码，会看到类似：

```text
ERR AUTH <password> called without any password configured
```

处理方式：

```powershell
# 用 TCP RESP 给当前 Redis 实例设置临时密码
$client = [System.Net.Sockets.TcpClient]::new('127.0.0.1', 6379)
$stream = $client.GetStream()
$payload = "*4`r`n`$6`r`nCONFIG`r`n`$3`r`nSET`r`n`$11`r`nrequirepass`r`n`$8`r`nruoyi123`r`n"
$bytes = [System.Text.Encoding]::ASCII.GetBytes($payload)
$stream.Write($bytes,0,$bytes.Length)
$client.Close()
```

这是当前运行实例的设置；Redis 重启后是否保留取决于 Redis 自身配置。

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

后端构建和启动已通过。下一步继续：

1. 创建 `backend/docs/business-modules.md`。
2. 添加钱包 SQL 草案到 `backend/script/sql/package_wallet_001.sql`。
3. 开始 `wallet-center` 后端模块实现。
