# GameLuck Backend

`backend/` 是 GameLuck 的后端服务工程，负责后台管理接口、认证授权、系统配置、日志、文件、任务、代码生成等基础能力。

## 模块说明

```text
gameluck-admin/      后端启动模块
gameluck-common/     公共能力模块
gameluck-modules/    系统、任务、代码生成等业务模块
gameluck-extend/     监控和扩展服务模块
script/              SQL、Docker 和环境脚本
```

## 主要能力

- 用户、角色、菜单、部门、岗位、字典、参数管理
- 登录日志、操作日志、在线用户、缓存监控
- 文件配置、文件上传、对象存储接入
- 定时任务基础能力
- 代码生成器
- 多租户、数据权限、接口权限、操作审计等后台基础能力

## 本地配置

```text
MySQL: localhost:3306/gameluck_vue
MySQL 用户: root
MySQL 密码: root
Redis: localhost:6379
Redis 密码: gameluck123
```

配置文件：

```text
gameluck-admin/src/main/resources/application-local.yml
```

## 构建和启动

```powershell
C:\tools\apache-maven-3.9.16\bin\mvn.cmd -pl gameluck-admin -am package -Plocal -DskipTests
java -jar gameluck-admin\target\gameluck-admin.jar --spring.profiles.active=local
```

启动成功后，后端默认监听：

```text
本机 8080 端口
```

## 开发边界

- `gameluck-common-*` 只放通用能力，不写具体业务流程。
- 平台业务优先放在独立业务模块，避免和系统基础模块强耦合。
- 修改权限、菜单、字典、参数时，需要同步 SQL 或迁移脚本。
- 涉及钱包、游戏、会员、充值、提现等核心业务时，先设计领域边界和数据流，再写代码。
