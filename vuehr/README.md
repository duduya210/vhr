菜单项数据加载成功之后，在前端有几个可以存放的地方：

1. sessionStorage
2. localStorage
**3. vuex**

## 前端界面开发

在 `vuehr` 目录运行：

```powershell
npm ci
npm run dev
```

开发预览地址为 `http://localhost:8080/`，接口代理到 `http://localhost:8081/`，需要先启动 `VhrApplication`。

## 更新主服务中的页面

```powershell
npm run test:ui
npm run build:web
```

`build:web` 先完成生产构建，再将产物复制到 `vhr-web/src/main/resources/static`。如果已有 `target/classes`，也会同步到运行时资源目录；不会删除其他静态资源。构建资源使用后端已经放行的 `/js/ui/` 路径。

重新启动 `VhrApplication`，访问 `http://localhost:8081/index.html`，必要时按 `Ctrl+F5` 强制刷新。邮件服务不需要因界面改版重新启动。

本轮界面包括：登录页、可折叠导航与工作台、员工列表、完整资料抽屉及分组编辑表单。工作台入口来自当前用户菜单权限，不展示虚构统计数据。`test:ui` 验证组件交互逻辑，不替代浏览器视觉检查或真实接口联调。

## 功能可用性与操作反馈

- `src/utils/featureAvailability.js` 维护页面的上线状态。侧栏与工作台只显示已上线且包含在当前用户菜单中的功能；旧链接访问未上线功能时显示说明页。此配置不授予后端权限，功能完成后再将对应项改为 `true`。
- 基础信息中的奖惩规则保留“待上线”提示并禁用。
- 员工工资账套只有点击“保存”且选择确实变化时才提交；取消、关闭和未修改都不会保存。保存期间禁止重复提交，失败保留选择并显示提示。
- 员工导入按现有后端支持限制为 `.xls`、非空且不超过 1 MB。HTTP 成功仍需业务状态为 `200` 才显示成功并刷新列表；业务错误、登录失效、权限不足和网络异常分别提示。当前接口不提供逐行错误及导入数量，因此页面不会编造这些结果。
