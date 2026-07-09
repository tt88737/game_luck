# 国际化开发守门规则

## 目标

后续开发不能把用户可见文案直接写死在页面、接口返回、异常消息或校验消息里。新增文案必须进入统一国际化通道，并通过自动检查。

## 前端规则

- 页面、组件、弹窗、提示、表格列名、placeholder、确认文案使用 `tt('中文源文案')` 或已有 `t('i18n.key')`。
- 路由标题继续走 `translateTitle()` / `i18nTitle.ts`。
- 新增公共中文源文案时，补到 `admin-ui/src/utils/i18nText.ts` 的英文映射。
- 不要直接写 `ElMessage.success('中文')`、`label="中文"`、`placeholder="中文"`。

## 后端规则

- 接口返回、异常、校验、导入导出错误、SSE/WebSocket 消息使用 `MessageUtils.message('key')`。
- Bean Validation 注解使用 `{message.key}`，并在三套资源文件同步补 key。
- 新增 key 必须同时写入：
  - `backend/gameluck-admin/src/main/resources/i18n/messages.properties`
  - `backend/gameluck-admin/src/main/resources/i18n/messages_zh_CN.properties`
  - `backend/gameluck-admin/src/main/resources/i18n/messages_en_US.properties`
- `@Log(title = "...")` 仍受 Java 注解常量限制，显示层通过前端 `tt(scope.row.title)` 兜底翻译。
- Excel 表头属于导入导出协议文案，后续如要完全多语言，需要单独改造 Excel 注解解析，不在普通 UI/API 守门范围内。

## 自动检查

本地提交或打包前运行：

```powershell
pnpm --dir admin-ui check:i18n
```

`build:dev` 和 `build:prod` 已自动串联该检查：

```powershell
pnpm --dir admin-ui build:dev
```

检查内容：

- 前端 `admin-ui/src` 中未通过 `tt()` / `t()` / `translateTitle()` 的中文文案。
- 后端 Java 中未通过 `MessageUtils` / `{i18n.key}` 的中文运行时文案。
- 后端三套 `messages*.properties` key 是否一致。
- 后端默认资源文件是否存在重复 key。
