# 员工奖惩与奖惩规则

## 使用步骤

1. 重启 `VhrApplication`，由项目现有 Flyway 执行 `V4__employee_rewards_and_rules.sql`。迁移新增规则表、扩展奖惩与日志字段，保留历史记录及旧的奖惩分。
2. 在 `http://localhost:8081/index.html` 按 `Ctrl+F5` 刷新。
3. 打开“系统管理 → 基础信息设置 → 奖惩规则”，新增奖励或处罚项目，填写默认金额、说明，并启用。后续可编辑或停用规则。
4. 打开“人事管理 → 员工奖惩”，搜索在职员工和启用的规则，核对实际金额、发生日期，填写原因和备注后确认登记。
5. 列表可按姓名/工号、类型、状态、发生日期筛选并分页；详情包含规则快照、实际金额、操作人与时间。
6. 需要撤销时点击记录的“撤销”，填写原因并确认。原记录会保留为“已撤销”，再次登记需发起新记录。
7. “系统管理 → 操作日志管理”可查询规则新增/修改、奖惩登记/撤销及其详情。

本轮不把奖惩金额计入工资，没有审批、预约生效、批量登记、历史修改或物理删除功能。

## 权限和数据约定

- 员工奖惩要求菜单 `/per/ec`（原始菜单 ID 10）；规则维护要求 `/sys/basic`（ID 23）；日志查询要求 `/sys/log`（ID 25）。前后端均检查权限，不会自动给账号分配权限。
- 只拥有员工奖惩权限的账号可搜索启用规则用于登记，不能维护规则。规则初始为空，需要管理员自行创建，不预置真实业务奖惩。
- 0 表示奖励、1 表示处罚；金额始终为非负数，范围 0–99999999.99，最多两位小数。后端使用 BigDecimal，数据库使用 DECIMAL(10,2)。
- 原有 `ecPoint` 是奖惩分，与新增金额独立。旧记录金额为空时显示“历史未记录”，不能当作 0 元，也不转换积分为金额。
- 发生日期范围为 1900-01-01 至服务端当天；原因必填，原因与备注各最多 255 字符。服务端按 Asia/Shanghai 记录操作时间。
- 登记时保存员工姓名、工号、部门名，以及规则名称、说明、类型、版本和默认金额快照。后续修改人员/规则不会重写这些快照。
- 已生效记录通过撤销保留原始内容，额外保存撤销原因、操作人及时间，不提供删除接口。旧记录迁移为已生效，历史缺失信息保持为空。

## 一致性与重试

- 规则写入、奖惩登记、撤销各自与操作日志处于同一事务；日志失败时业务修改也回滚。
- 规则编辑使用版本号校验；登记时再次锁定并检查规则状态及版本，避免沿用已停用或过期规则。
- 每次写操作使用 UUID 请求标识，日志保存请求摘要和结果编号。重试相同请求返回已完成结果；同一标识不能用于不同内容或其他操作人。
- 网络异常后保留原 URL、方法和请求内容，按钮显示“重试确认结果”，可安全核实原请求；此时新操作按钮会重新打开待确认操作。
- 待确认请求保存在当前页面内存中。刷新页面或切换页面前，应先核实结果；重新进入后可通过记录与日志查证。
- 撤销锁定记录并检查状态，两个操作人同时撤销时只有一次成功。规则停用、员工离职后仍允许撤销既有记录。

## 接口

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| GET | `/system/basic/ec/` | 规则分页查询，支持 keyword/type/enabled |
| POST | `/system/basic/ec/` | 新增规则 |
| PUT | `/system/basic/ec/{id}` | 编辑规则（包含启停），需 version |
| GET | `/personnel/ec/` | 奖惩分页查询，支持 keyword/type/status/startDate/endDate |
| GET | `/personnel/ec/employees?keyword=...` | 在职员工搜索，最多 30 条 |
| GET | `/personnel/ec/rules?keyword=...&page=1` | 启用规则搜索，每页 30 条 |
| POST | `/personnel/ec/` | 奖惩登记 |
| POST | `/personnel/ec/{id}/cancel` | 撤销奖惩 |

分页参数 page 从 1 开始，size 范围 1–100；日期格式 YYYY-MM-DD，开始/结束日期均包含当天。状态为 ACTIVE 或 CANCELLED。

规则写入字段：name、type、amount、description、enabled、version（编辑必填）、requestId。

登记字段：employeeId、ruleId、expectedRuleVersion、date、amount、reason、remark、requestId。类型和规则快照由服务端读取，不接受客户端指定操作人。

撤销字段：reason、requestId。

## 验证

在仓库根目录使用 JDK 21 运行隔离测试（不启动业务应用，不连接真实 MySQL/RabbitMQ）：

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-21'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
mvn.cmd -B -ntp -f vhr/pom.xml -pl vhrserver/vhr-web -am '-Dtest=EmployeeEcTests,EcControllerTests,EmployeeTransferTests,TransferAccessTests,TransferControllerTests' '-Dsurefire.failIfNoSpecifiedTests=false' test
```

在 `vuehr` 目录执行 `npm run test:ui` 和 `npm run build:web`。

后端测试使用真实 MyBatis SQL、Spring 事务和 H2 内存数据库，执行 V3/V4 原始迁移脚本；覆盖金额精度、快照、版本冲突、停用校验、撤销、重复提交、并发撤销、日志失败回滚、筛选及权限。H2 不能替代真实 MySQL 联调；前端组件测试和构建也不替代浏览器视觉验收。
