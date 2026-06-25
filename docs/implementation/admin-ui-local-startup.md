# Admin UI 本地启动说明

## 1. 目标

启动 GameLuck-Vue-Plus 官方前端 `plus-ui`，用于访问当前后端管理系统。

本地目录：

```text
admin-ui/
```

后端地址：

```text
http://localhost:8080
```

前端地址：

```text
http://localhost:5173
```

## 2. 上游来源

前端来自：

```text
https://github.com/JavaLionLi/plus-ui
branch: 5.X
commit: d0d451967676707021b9857df529c395b27e90a7
```

上游记录：

```text
docs/upstream/plus-ui.md
```

## 3. 环境要求

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

## 4. 本地配置

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

- 上游默认端口为 `80`，本地开发改为 `5173`，避免 Windows 端口权限和占用问题。
- `VITE_APP_BASE_API` 使用 `/dev-api`。
- `admin-ui/vite.config.ts` 已将 `/dev-api` 代理到 `http://localhost:8080`。
- 前后端加密开关保持开启，当前前端 RSA 配置与后端默认配置匹配。

## 5. 安装依赖

```powershell
cd C:\codex\project\admin-ui
pnpm install
```

已验证安装成功。

安装时可能看到：

```text
The following dependencies have build scripts that were ignored: @parcel/watcher, esbuild
```

当前不影响 Vite 启动和构建。

## 6. 启动前端

```powershell
cd C:\codex\project\admin-ui
pnpm dev
```

访问：

```text
http://localhost:5173/
```

已验证：

```text
GET http://localhost:5173/ 返回 200
```

## 7. 后端要求

前端依赖后端先启动：

```powershell
cd C:\codex\project\backend
java -jar gameluck-admin\target\gameluck-admin.jar --spring.profiles.active=local
```

已验证：

```text
GET http://localhost:8080/ 返回 200
```

## 8. 构建验证

```powershell
cd C:\codex\project\admin-ui
pnpm build:dev
```

已验证：

```text
vite build --mode development 成功
```

构建输出存在大 chunk 警告，这是上游管理端常见体积提示，不影响当前本地启动。

## 9. 常见问题

### 9.1 访问 8080 不是前端页面

`8080` 是后端地址，只返回后端欢迎信息。管理端前端地址是：

```text
http://localhost:5173/
```

### 9.2 前端页面能打开但接口失败

先确认后端是否运行：

```powershell
Invoke-WebRequest -UseBasicParsing http://localhost:8080/
```

如果 Redis 重启过，后端可能因为 Redis 密码不一致启动失败。当前本地后端配置使用 Redis 密码：

```text
gameluck123
```

### 9.3 端口 5173 被占用

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
