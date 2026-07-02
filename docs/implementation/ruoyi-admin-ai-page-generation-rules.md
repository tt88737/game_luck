# RuoYi 后台页面 AI 生成规范通用版

## 1. 适用范围

本规范适用于 RuoYi、RuoYi-Vue、RuoYi-Vue-Plus 及其二开后台项目。

用于约束 AI 生成后台管理页面、接口、菜单、权限、SQL 和基础联调流程。

默认后台页面是运营管理工具，不是官网、营销页、活动页或展示页。

## 2. 页面结构约束

后台页面默认结构：

- 查询区域
- 操作按钮区域
- 数据表格
- 分页
- 新增 / 编辑弹窗
- 详情弹窗或抽屉
- 高风险操作确认弹窗

禁止默认生成：

- 大面积 Hero 区
- 营销文案
- 渐变背景
- 装饰性卡片堆叠
- 无业务意义的数据看板
- 只有说明文字、没有管理动作的页面

## 3. 菜单约束

新增页面必须同步明确：

- `menu_name`：菜单名称
- `parent_id`：父级菜单
- `order_num`：排序
- `path`：路由路径
- `component`：前端组件路径
- `menu_type`：目录 / 菜单 / 按钮
- `perms`：权限标识
- `icon`：菜单图标
- `visible`：是否显示
- `status`：是否启用

菜单 SQL 示例：

```sql
INSERT INTO sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES
(2000, '订单中心', 0, 10, 'order', NULL, '', 1, 0, 'M', '0', '0', '', 'shopping', 1, NOW(), '订单中心目录'),
(2001, '订单管理', 2000, 1, 'order', 'business/order/index', '', 1, 0, 'C', '0', '0', 'business:order:list', 'list', 1, NOW(), '订单管理菜单'),
(2011, '订单查询', 2001, 1, '#', '', '', 1, 0, 'F', '0', '0', 'business:order:query', '#', 1, NOW(), ''),
(2012, '订单新增', 2001, 2, '#', '', '', 1, 0, 'F', '0', '0', 'business:order:add', '#', 1, NOW(), ''),
(2013, '订单编辑', 2001, 3, '#', '', '', 1, 0, 'F', '0', '0', 'business:order:edit', '#', 1, NOW(), ''),
(2014, '订单删除', 2001, 4, '#', '', '', 1, 0, 'F', '0', '0', 'business:order:remove', '#', 1, NOW(), '');
```

## 4. 权限约束

权限标识推荐格式：

```text
模块:资源:list
模块:资源:query
模块:资源:add
模块:资源:edit
模块:资源:remove
模块:资源:export
模块:资源:import
模块:资源:audit
模块:资源:approve
模块:资源:reject
```

前端按钮必须绑定权限：

```vue
<el-button v-hasPermi="['business:order:add']" type="primary" icon="Plus">新增</el-button>
```

后端接口必须绑定权限：

```java
@SaCheckPermission("business:order:add")
@PostMapping
public R<Void> add(@Validated @RequestBody OrderBo bo) {
    return toAjax(orderService.insertByBo(bo));
}
```

禁止：

- 前端有按钮但后端接口无权限控制
- 后端有权限但前端按钮无权限控制
- 多个业务动作共用一个模糊权限
- 用 `list` 权限覆盖新增、编辑、删除、高风险操作

## 5. 图标约束

菜单图标必须使用项目中已存在的图标。

生成菜单 SQL 前，应检查图标目录：

```text
src/assets/icons/svg
```

如果图标不存在：

- 优先改用已有图标
- 不要随意填写不存在的 icon
- 不要为一个菜单临时引入整套图标库

强制约束：

- `icon` 字段只能填写 `src/assets/icons/svg/*.svg` 中已经存在的文件名，不带 `.svg` 后缀。
- 按钮权限菜单 `menu_type = 'F'` 的 `icon` 统一使用 `#`。
- 不允许填写 `pay` 这类项目中不存在的图标名；支付、充值、提现、财务类菜单默认使用 `money`。
- 每次新增或修改菜单 SQL 后，必须执行 `pnpm --dir admin-ui check:menu-icons`，校验通过后才能继续打包或导入 SQL。

常用图标建议：

| 场景 | 建议图标 |
| --- | --- |
| 列表 / 流水 | list |
| 用户 / 会员 | user |
| 金额 / 财务 | money |
| 支付 / 充值 / 提现 | money |
| 订单 | shopping / list |
| 配置 | system / switch |
| 规则 | slider |
| 锁定 / 冻结 | lock |
| 审核 / 校验 | validCode |
| 日志 | log |
| 监控 | monitor |

## 6. 查询区约束

查询区默认放在页面顶部。

常用查询字段：

- 业务编号
- 名称
- 用户 ID / 会员 ID
- 状态
- 类型
- 创建时间

必须包含：

- 搜索按钮
- 重置按钮
- `@keyup.enter="handleQuery"`
- `resetFields()`

查询条件超过 6 个时，应考虑折叠或高级查询，不要让页面首屏被查询表单占满。

## 7. 表格约束

B 端管理页默认使用表格，不使用卡片列表替代表格。

表格必须具备：

- `v-loading`
- 分页
- 操作列
- 状态标签
- 长字段省略提示
- 金额右对齐
- 时间字段固定宽度

推荐写法：

```vue
<el-table v-loading="loading" border :data="dataList">
  <el-table-column label="业务编号" prop="businessNo" min-width="180" show-overflow-tooltip />
  <el-table-column label="金额" prop="amount" align="right" width="120" />
  <el-table-column label="状态" prop="status" align="center" width="100">
    <template #default="scope">
      <el-tag :type="statusType(scope.row.status)">
        {{ statusLabel(scope.row.status) }}
      </el-tag>
    </template>
  </el-table-column>
  <el-table-column label="创建时间" prop="createTime" align="center" width="170" />
  <el-table-column label="操作" align="center" width="160" fixed="right" />
</el-table>
```

## 8. 状态展示约束

状态字段不能只展示后端原始值。

必须转换为用户能理解的中文文案。

示例：

| 原始值 | 展示文案 | Tag 类型 |
| --- | --- | --- |
| PENDING | 待处理 | warning |
| SUCCESS | 成功 | success |
| FAILED | 失败 | danger |
| CANCELLED | 已取消 | info |
| DISABLED | 已禁用 | info |
| ENABLED | 已启用 | success |
| REVIEWING | 审核中 | warning |
| REJECTED | 已拒绝 | danger |

推荐：

```ts
const statusLabel = (status?: string) => {
  const map: Record<string, string> = {
    PENDING: '待处理',
    SUCCESS: '成功',
    FAILED: '失败',
    CANCELLED: '已取消'
  };
  return status ? map[status] || status : '';
};
```

## 9. 表单约束

新增 / 编辑默认使用弹窗。

表单必须具备：

- `ref`
- `rules`
- 必填校验
- 提交前 `validate`
- 取消时重置表单

字段组件建议：

| 字段类型 | 组件 |
| --- | --- |
| 文本 | `el-input` |
| 长文本 | `el-input type="textarea"` |
| 金额 | `el-input-number` |
| 状态 | `el-select` / `el-switch` |
| 类型 | `el-select` |
| 时间 | `el-date-picker` |
| 开关 | `el-switch` |
| 多选 | `el-checkbox-group` / `el-select multiple` |

金额字段推荐：

```vue
<el-input-number
  v-model="form.amount"
  :precision="2"
  :min="0"
  class="w-full"
/>
```

## 10. 高风险操作约束

以下操作必须二次确认：

- 删除
- 禁用
- 审核通过
- 审核拒绝
- 重置密码
- 强制下线
- 入账
- 扣款
- 冻结
- 解冻
- 发放奖励
- 支付成功
- 结算

推荐写法：

```ts
const handleApprove = async (row: OrderVO) => {
  await proxy?.$modal.confirm('确认审核通过该记录？');
  await approveOrder(row.id);
  proxy?.$modal.msgSuccess('操作成功');
  await getList();
};
```

禁止高风险操作：

- 无确认弹窗
- 无权限标识
- 无操作日志
- 无状态校验
- 前端隐藏按钮但后端不校验状态

## 11. 后端分层约束

Controller 只负责：

- 接收参数
- 权限注解
- 日志注解
- 返回结果

Service 负责：

- 业务流程
- 状态流转
- 幂等判断
- 事务边界
- 跨模块调用

Mapper 负责：

- 单表查询
- 必要的自定义 SQL
- 行锁查询

禁止：

- Controller 里写复杂业务逻辑
- Controller 直接调用多个 Mapper 拼业务流程
- 前端直接决定核心业务规则
- 绕过 Service 直接改关键业务数据

## 12. 后端接口约束

常规接口：

```http
GET /business/order/list
GET /business/order/{id}
POST /business/order
PUT /business/order
DELETE /business/order/{ids}
POST /business/order/export
```

高风险动作接口：

```http
POST /business/order/{id}/approve
POST /business/order/{id}/reject
POST /business/order/{id}/cancel
POST /business/order/{id}/enable
POST /business/order/{id}/disable
```

高风险动作不要全部塞进 `PUT /business/order`。

## 13. 数据库约束

新增业务表建议包含：

```text
id
tenant_id
business_no
status
remark
create_dept
create_by
create_time
update_by
update_time
version
del_flag
```

必须根据业务增加：

- 唯一索引
- 查询索引
- 状态索引
- 时间索引

涉及幂等的业务必须有唯一键：

```text
tenant_id + business_no
tenant_id + idempotency_key
```

## 14. SQL 编码约束

SQL 文件必须使用 UTF-8。

导入含中文菜单、中文按钮、中文备注的 SQL 时，必须确认数据库连接字符集为 `utf8mb4`。

如果项目提供 UTF-8 导入脚本，必须使用导入脚本。

禁止使用容易破坏中文的管道导入方式，例如：

```powershell
Get-Content xxx.sql | mysql ...
```

如果出现中文乱码，排查顺序：

1. SQL 文件是否 UTF-8
2. 导入命令是否破坏编码
3. MySQL 连接是否指定 `utf8mb4`
4. 表和库字符集是否 `utf8mb4`
5. 前端接口返回是否已经是乱码

禁止盲目二次转码。

## 15. 前端文件约束

推荐文件结构：

```text
src/api/business/order/index.ts
src/api/business/order/types.ts
src/views/business/order/index.vue
```

`types.ts` 定义：

```ts
export interface OrderVO extends BaseEntity {}
export interface OrderForm {}
export interface OrderQuery extends PageQuery {}
```

`index.ts` 定义：

```ts
listOrder
getOrder
addOrder
updateOrder
delOrder
```

页面文件必须包含：

- `getList`
- `handleQuery`
- `resetQuery`
- `handleAdd`
- `handleUpdate`
- `submitForm`
- `cancel`

按业务需要增加：

- `handleDetail`
- `handleApprove`
- `handleReject`
- `handleExport`

## 16. 后端文件约束

推荐文件结构：

```text
domain/Order.java
domain/bo/OrderBo.java
domain/vo/OrderVo.java
mapper/OrderMapper.java
resources/mapper/business/OrderMapper.xml
service/IOrderService.java
service/impl/OrderServiceImpl.java
controller/OrderController.java
```

命名约束：

- Entity：`Xxx`
- BO：`XxxBo`
- VO：`XxxVo`
- Mapper：`XxxMapper`
- Service：`IXxxService`
- ServiceImpl：`XxxServiceImpl`
- Controller：`XxxController`

## 17. 日志与审计约束

新增、编辑、删除、高风险操作必须加操作日志。

示例：

```java
@Log(title = "订单管理", businessType = BusinessType.INSERT)
```

常用类型：

```text
INSERT
UPDATE
DELETE
EXPORT
GRANT
FORCE
```

高风险业务建议在业务表中记录：

- 操作人
- 操作时间
- 审核人
- 审核时间
- 拒绝原因
- 失败原因

## 18. 验证约束

每个新增页面完成后必须验证：

- 后端编译通过
- 前端构建通过
- SQL 导入成功
- 菜单显示正常
- 图标显示正常
- 权限按钮显示正常
- 列表接口正常
- 查询和重置正常
- 新增 / 编辑 / 删除正常
- 高风险操作有确认弹窗
- 状态展示正确
- 空数据不报错
- 中文无乱码
- 浏览器控制台无明显错误

推荐命令：

```powershell
mvn -pl gameluck-admin -am compile -DskipTests
pnpm build:prod
```

具体命令以项目实际脚本为准。

## 19. AI 生成顺序

AI 生成一个 RuoYi 后台页面时，默认按以下顺序：

1. 明确业务对象、状态、权限和菜单位置
2. 生成数据表 SQL
3. 生成菜单和按钮权限 SQL
4. 生成 Entity / BO / VO
5. 生成 Mapper / XML
6. 生成 Service / ServiceImpl
7. 生成 Controller
8. 生成前端 API types
9. 生成前端 API index
10. 生成 Vue 页面
11. 执行后端编译
12. 执行前端构建
13. 导入 SQL
14. 验证菜单、接口和页面

禁止只生成 Vue 页面，不生成接口、权限、菜单和 SQL。

## 20. AI 输出要求

AI 完成页面后，必须说明：

- 新增了哪些文件
- 修改了哪些文件
- 新增了哪些接口
- 新增了哪些权限
- 新增了哪些菜单
- 是否涉及数据库变更
- 是否涉及高风险操作
- 如何验证
- 哪些验证已通过
- 哪些验证未执行及原因

不得只回复“已完成”。
