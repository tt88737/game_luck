# 后端本地启动说明

## 1. 目标

用最少依赖启动 `backend/gameluck-admin`，验证 GameLuck 后端能在本机跑起来。

本地启动使用：

```text
Maven profile: local
Spring profile: local
配置文件: backend/gameluck-admin/src/main/resources/application-local.yml
```

## 2. 本地依赖

- Java 17
- Maven 3.9.16，默认路径 `C:\tools\apache-maven-3.9.16`
- MySQL `localhost:3306`，账号 `root`，密码 `root`
- Redis / Memurai `localhost:6379`，密码 `gameluck123`
- 数据库 `gameluck_vue`

## 3. 创建数据库

```powershell
mysql -uroot -proot -e "CREATE DATABASE IF NOT EXISTS gameluck_vue DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;"
```

验证：

```powershell
mysql -uroot -proot -N -e "SHOW DATABASES LIKE 'gameluck_vue';"
```

## 4. 导入基础 SQL

```powershell
.\backend\script\bin\import-sql-utf8.ps1 -SqlPath backend\script\sql\gameluck_vue_5.X.sql
.\backend\script\bin\import-sql-utf8.ps1 -SqlPath backend\script\sql\ry_job.sql
.\backend\script\bin\import-sql-utf8.ps1 -SqlPath backend\script\sql\ry_workflow.sql
.\backend\script\bin\import-sql-utf8.ps1 -SqlPath backend\script\sql\gameluck_wallet.sql
```

含中文菜单、字典、备注的 SQL 不要使用 `Get-Content ... | mysql` 导入，避免中文被 PowerShell 转码成 `?`。详见 `docs/implementation/sql-import-encoding.md`。

## 5. 本地配置

配置文件：

```text
backend/gameluck-admin/src/main/resources/application-local.yml
```

该配置覆盖：

- 禁用 Spring Boot Admin Client
- 禁用 SnailJob
- 使用本机 MySQL：`localhost:3306/gameluck_vue root/root`
- 使用本机 Redis：`localhost:6379`，密码 `gameluck123`

## 6. 构建后端

```powershell
cd C:\codex\project\backend
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-admin -am package -Plocal -DskipTests
```

期望结果：

```text
BUILD SUCCESS
```

## 7. 启动后端

推荐使用 jar 启动：

```powershell
cd C:\codex\project\backend
java -jar gameluck-admin\target\gameluck-admin.jar --spring.profiles.active=local
```

也可以使用 Maven 启动：

```powershell
cd C:\codex\project\backend
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-admin -am spring-boot:run -Plocal
```

## 8. 验证

端口验证：

```powershell
Test-NetConnection -ComputerName localhost -Port 8080
```

接口验证：

```powershell
Invoke-WebRequest -UseBasicParsing http://localhost:8080/
```

租户接口验证：

```powershell
Invoke-WebRequest -UseBasicParsing http://localhost:8080/auth/tenant/list -Headers @{clientid='e5cd7e4891bf95d1d19206ce24a7b32e'}
```

## 9. 常见问题

### Maven 不存在

临时设置：

```powershell
$env:MAVEN_HOME='C:\tools\apache-maven-3.9.16'
$env:Path="$env:MAVEN_HOME\bin;$env:Path"
```

### Redis 旧缓存导致登录未知异常

如果日志出现旧包名反序列化错误，例如 `org.dromara.system.domain.vo.SysClientVo`，清空当前 Redis 库并重启后端。

### 数据库不存在

```powershell
mysql -uroot -proot -e "CREATE DATABASE IF NOT EXISTS gameluck_vue DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;"
```
