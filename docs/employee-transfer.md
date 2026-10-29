# 员工调动与操作日志

## 使用

1. 重启 `VhrApplication`，等待启动完成。项目现有 Flyway 会执行 `V3__employee_transfer_audit.sql`，为调动表、日志表增加快照、操作人、精确时间和请求标识；保留历史数据，不修改 V1/V2。
2. 打开 `http://localhost:8081/index.html`，按 `Ctrl+F5` 刷新。前端产物由 `vuehr` 的 `npm run build:web` 同步到主服务静态资源目录。
3. 在“人事管理 → 员工调动”点击“发起调动”，搜索在职员工，选择目标部门、职位，填写原因，确认立即生效。
4. 在员工基本资料中查看最新部门、职位；在调动列表和“系统管理 → 操作日志管理”查看记录及详情。

账号需分别具有 `/per/mv`、`/sys/log` 菜单权限（原始菜单 ID 为 13、25）。本次实现不自动授予权限；有调动权限而没有日志权限的账号仍可查看调动记录。更改角色权限后重新登录以刷新菜单。

## 行为与边界

- 本轮支持立即调动，不含审批、预约生效、撤销或编辑历史记录。
- 至少改变部门或职位之一；仅允许在职员工、启用的目标部门及职位，原因必填且最多 255 字符，备注最多 255 字符。
- 操作人从服务端当前登录身份读取，时间由服务端按 `Asia/Shanghai` 生成。员工姓名、工号、原部门/职位、目标部门/职位及操作人姓名均保存快照。
- 员工档案更新、调动记录和日志写入处于同一事务；任何一步失败均回滚。员工行锁与原部门/职位校验阻止并发调动覆盖刚发生的变化。
- 每次提交使用唯一请求标识。网络异常时，窗口保留原请求，点击“重试确认结果”会核实或完成原操作，避免重复生成调动和日志。不要通过修改数据再次提交来确认上一次结果；可先查看记录。
- 调动查询支持姓名/工号、调动前后部门、日期；日志查询支持关键词、操作人和日期，日期范围包含结束当天。分页保留已提交的筛选条件。
- 旧记录缺少的历史部门名称、操作人等显示“未记录”，不会以当前部门名称冒充历史快照。
- 日志页面提供只读查询及详情，现已覆盖员工调动、奖惩登记与撤销、奖惩规则变更；奖惩功能详见 [员工奖惩说明](employee-rewards.md)。
- 员工、操作员删除仍遵守现有数据库外键约束；本次不改变其他模块的删除规则。

## 接口

| 方法 | 路径 | 用途 | 菜单权限 |
| --- | --- | --- | --- |
| GET | `/personnel/remove/` | 分页调动记录 | `/per/mv` |
| GET | `/personnel/remove/employees?keyword=...` | 在职员工姓名/工号搜索，最多 30 条 | `/per/mv` |
| GET | `/personnel/remove/options` | 部门与职位选项 | `/per/mv` |
| POST | `/personnel/remove/` | 立即调动 | `/per/mv` |
| GET | `/system/log/` | 分页操作日志 | `/sys/log` |

分页参数：`page`（从 1 开始）、`size`（1–100）、`keyword`、`startDate`、`endDate`（日期格式 `YYYY-MM-DD`）。调动列表另支持 `departmentId`，日志列表另支持 `operator`。

提交字段：`employeeId`、`expectedDepartmentId`、`expectedPositionId`、`departmentId`、`positionId`、`reason`、`remark`、`requestId`（UUID）。原部门/职位来自员工查询结果；请求不接受客户端指定操作人。

## 开发验证

在仓库根目录运行以下指定测试，仅使用隔离的内存数据库及模拟 HTTP 请求，不启动应用上下文、不连接业务 MySQL/RabbitMQ：

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-21'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
mvn.cmd -B -ntp -f vhr/pom.xml -pl vhrserver/vhr-web -am '-Dtest=EmployeeTransferTests,TransferAccessTests,TransferControllerTests' '-Dsurefire.failIfNoSpecifiedTests=false' test
```

在 `vuehr` 目录运行：

```powershell
npm run test:ui
npm run build:web
```

后端测试执行真实 MyBatis SQL、V3 迁移及 Spring 事务，覆盖成功、回滚、幂等、并发冲突、过滤、权限及 HTTP 参数绑定。H2 的 MySQL 兼容模式不能替代真实 MySQL 联调。前端测试验证组件交互和错误处理，生产构建验证 Vue 模板；不替代浏览器视觉验收。
