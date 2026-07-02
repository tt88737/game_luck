# Admin UI 本地启动说明

## 1. 目标

启动 GameLuck Admin UI，用于访问当前后端管理系统。

```text
前端目录: admin-ui/
后端地址: http://localhost:8080
前端地址: http://localhost:5173
```

## 2. 环境要求

已验证当前机器：

```text
Node.js: v22.21.0
npm: 11.6.2
pnpm: 10.0.0
```

`admin-ui/package.json` 要求：

```text
node >=20.19.0
npm >=8.19.0
```

## 3. 本地配置

开发环境配置文件：

```text
admin-ui/.env.development
```

关键配置：

```text
VITE_APP_BASE_API = '/dev-api'
VITE_APP_PORT = 5173
VITE_APP_ENCRYPT = true
VITE_APP_CLIENT_ID = 'e5cd7e4891bf95d1d19206ce24a7b32e'
```

说明：

- `VITE_APP_BASE_API` 使用 `/dev-api`。
- `admin-ui/vite.config.ts` 会把 `/dev-api` 代理到 `http://localhost:8080`。
- 前后端加密开关保持开启，当前前端 RSA 配置与后端默认配置匹配。

## 4. 安装依赖

```powershell
cd C:\codex\project\admin-ui
pnpm install
```

## 5. 启动前端

```powershell
cd C:\codex\project\admin-ui
pnpm dev
```

访问：

```text
http://localhost:5173/
```

## 6. 后端要求

前端依赖后端先启动：

```powershell
cd C:\codex\project\backend
java -jar gameluck-admin\target\gameluck-admin.jar --spring.profiles.active=local
```

## 7. 构建验证

```powershell
cd C:\codex\project\admin-ui
pnpm build:prod
```

## 8. 常见问题

### 访问 8080 不是前端页面

`8080` 是后端地址。管理后台前端地址是：

```text
http://localhost:5173/
```

### 前端页面能打开但接口失败

先确认后端是否运行：

```powershell
Invoke-WebRequest -UseBasicParsing http://localhost:8080/
```

如果 Redis 重启过，后端可能因为 Redis 密码或旧缓存异常导致登录失败。当前本地后端配置使用 Redis 密码：

```text
gameluck123
```

### 端口 5173 被占用

修改：

```text
admin-ui/.env.development
```

调整：

```text
VITE_APP_PORT = 新端口
```

然后重新执行：

```powershell
pnpm dev
```
