# GameLuck Admin UI

`admin-ui/` 是 GameLuck 的后台管理前端，用于平台管理、租户配置、系统权限、日志审计、文件管理和基础运营配置。

## 技术栈

- Vue 3
- TypeScript
- Vite
- Element Plus
- Pinia
- UnoCSS

## 本地启动

```powershell
pnpm install
pnpm dev
```

默认访问：

```text
本机 5173 端口
```

## 构建

```powershell
pnpm build:dev
pnpm build:prod
```

## 后端代理

开发环境默认代理到本机后端：

```text
本机 8080 端口
```

如果后端端口调整，需要同步修改前端环境配置。

## 开发约定

- 页面文案优先使用中文，面向平台运营人员表达。
- 新页面放在对应业务目录下，避免把业务代码放进通用组件目录。
- 通用组件只沉淀稳定、可复用的 UI 能力。
- 菜单、路由、接口权限需要和后端配置保持一致。
- 当前阶段以后台实际业务为主，减少无关演示页和外部入口。
