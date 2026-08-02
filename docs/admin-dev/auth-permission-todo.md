# 登录认证与权限控制 — 待开发事项

> 生成日期：2026-06-13
> 来源：SDD 004-auth-permission-hardening 审查报告
> 状态标记：[待开发] / [进行中] / [已完成]

---

## 一、Critical 级别（安全关键）

### C-01 登录功能硬编码为开发模式，忽略用户输入
- **状态**: [待开发]
- **文件**: `frontend/src/views/login/LoginPage.vue` L387
- **问题**: `handleLogin` 无论输入什么用户名密码，都直接调用 `generateAdminToken()` 生成管理员Token，完全忽略 `loginForm.username/password`
- **影响**: 任何用户名/密码组合都能登录成功，核心认证功能失效
- **前置条件**: 后端登录API `POST /api/v1/auth/login` 开发完成
- **修复方案**:
  1. 调用真实后端登录API
  2. 仅开发模式且后端不可用时回退到 `generateAdminToken()`
  3. 根据后端返回的角色信息动态设置权限

### C-02 JWT Token解析不验证签名，存在Token伪造风险
- **状态**: [待开发]
- **文件**: `frontend/src/stores/permission.ts` L582-593
- **问题**: `parseJWTPayload` 仅做 base64 解码，攻击者可构造任意 payload 的 JWT
- **影响**: 前端信任伪造Token导致UI层显示管理员功能，域权限矩阵、菜单可见性等前端安全控制全部失效
- **前置条件**: 后端Token验证接口 `GET /api/v1/auth/verify` 开发完成
- **修复方案**:
  1. 前端至少验证 `exp` 过期字段（已在C-03中部分实现）
  2. 生产环境通过后端接口验证Token有效性
  3. 关键操作（如权限变更）需后端二次校验

### C-04 无Token刷新机制，401直接强制登出
- **状态**: [待开发]
- **文件**: `frontend/src/api/request.ts` L144-147
- **问题**: 项目规范要求"Token过期时自动使用Refresh Token刷新"，但实际实现中完全没有刷新逻辑，401直接 `clearAuthAndRedirect()`
- **影响**: 用户正在填写表单时Token过期导致数据丢失；多标签页场景下一个标签页过期导致全部被登出
- **前置条件**: 后端Refresh Token接口 `POST /api/v1/auth/refresh` 开发完成
- **修复方案**:
  1. 收到401时，先尝试用 `refresh_token` 刷新
  2. 成功后重试原请求
  3. 失败才跳转登录页
  4. 处理并发请求的刷新去重（多个请求同时401时只刷新一次）

---

## 二、Major 级别（规范违反/功能缺陷）

### M-01 LoginPage.vue 严重超限（1770行）
- **状态**: [待开发]
- **文件**: `frontend/src/views/login/LoginPage.vue`
- **问题**: 文件总计1770行，超过Vue组件硬上限1500行。`<style>` 部分约818行
- **修复方案**:
  1. 将注册页面提取为独立组件 `RegisterPage.vue`
  2. 将动画状态机逻辑提取到 `composables/useAuthAnimation.ts`
  3. 将验证码逻辑提取到 `composables/useCaptcha.ts`
  4. 将样式提取到 `styles/login.scss`

### M-02 LoginPage.vue 13处 `:deep()` 样式覆盖
- **状态**: [待开发]
- **文件**: `frontend/src/views/login/LoginPage.vue` L1430-1703
- **问题**: 项目规范禁止业务页面使用 `:deep()` 覆盖 Element Plus 组件样式
- **修复方案**: 将所有 Element Plus 样式覆盖迁移到 `src/styles/global/_element-overrides.scss`，使用 `.login-page` 作为作用域选择器

### M-03 LoginPage.vue 36处硬编码颜色值
- **状态**: [待开发]
- **文件**: `frontend/src/views/login/LoginPage.vue`
- **问题**: 包含 `#ffffff`、`rgba(0,0,0,0.35)`、`rgba(26,26,46,0.1)` 等硬编码颜色，深色主题适配会失败
- **修复方案**: 将所有硬编码颜色替换为 `--fts-*` CSS变量。Canvas验证码中的颜色需通过 `getComputedStyle` 获取变量值

### M-04 LoginPage.vue 2处 `!important`
- **状态**: [待开发]
- **文件**: `frontend/src/views/login/LoginPage.vue` L1757-1758
- **问题**: 项目规范禁止使用 `!important`
- **修复方案**: 使用更高特异性的选择器替代

### M-05 多文件 `console.log/info` 调试代码未清理
- **状态**: [待开发]
- **涉及文件**:
  - `permission.ts` L401, L686 — `console.log`
  - `request.ts` L206 — `console.info`
- **修复方案**: 替换为 `logger` 工具或 `console.warn/error`

### M-06 permission.ts 1151行超过预警线
- **状态**: [待开发]
- **文件**: `frontend/src/stores/permission.ts`
- **问题**: 超过TS文件500行预警线，包含用户信息管理、菜单配置、审计日志等多个职责
- **修复方案**: 拆分为：
  - `stores/permission/user-info.ts` — 用户信息、角色检查、权限检查
  - `stores/permission/menu-config.ts` — 菜单注册、覆盖、模板
  - `stores/permission/audit-log.ts` — 审计日志
  - `stores/permission/index.ts` — 组合导出

### M-07 AuditLogEntry 接口定义在Store内部无法外部导入
- **状态**: [待开发]
- **文件**: `frontend/src/stores/permission.ts` L117-130
- **问题**: 接口定义在 `defineStore` 回调函数内部，其他组件无法导入复用
- **修复方案**: 将 `AuditLogEntry` 提取到 `types/permission.ts` 中独立导出

### M-08 审计日志仅存内存，刷新即丢失
- **状态**: [待开发]
- **文件**: `frontend/src/stores/permission.ts` L131
- **问题**: `auditLogs` 是纯内存的 `ref<AuditLogEntry[]>([])`，页面刷新后所有日志丢失
- **前置条件**: 后端审计日志API开发完成（长期方案）
- **修复方案**:
  - 短期：持久化到 localStorage
  - 长期：通过API写入后端数据库，前端仅负责展示

### M-14 ApprovalWorkflowTab.vue 2处未注释的 `any` 类型
- **状态**: [待开发]
- **文件**: `frontend/src/views/system/components/ApprovalWorkflowTab.vue` L12, L38
- **问题**: `currentRecord = ref<any>(null)` 和 `function showDetail(record: any)` 违反TypeScript规范
- **修复方案**: 定义 `ApprovalRecord` 接口替代 `any`

### M-15 OperationAudit.vue 使用 `el-tag` 显示状态
- **状态**: [待开发]
- **文件**: `frontend/src/views/system/OperationAudit.vue` L88, L91
- **问题**: 项目规范禁止使用 `el-tag` 显示状态，必须使用 `StatusTag`
- **修复方案**: 替换为 `<StatusTag>` 组件

### M-16 TopNavbar.vue 面包屑硬编码"员工管理"
- **状态**: [待开发]
- **文件**: `frontend/src/components/layout/TopNavbar.vue` L125
- **问题**: 面包屑第二项硬编码，不随路由变化
- **修复方案**: 从当前路由的 `meta.title` 动态生成面包屑

### M-17 TopNavbar.vue 通知徽章硬编码数字 3
- **状态**: [待开发]
- **文件**: `frontend/src/components/layout/TopNavbar.vue` L139
- **问题**: 通知数量硬编码为3
- **修复方案**: 从通知Store或API获取实际数量，或初期使用0并隐藏徽章

### M-18 clearUserInfo 与 logout 功能重复且不一致
- **状态**: [待开发]
- **文件**: `frontend/src/stores/permission.ts` L452-488
- **问题**: `clearUserInfo` 不清除Token，`logout` 清除。误用 `clearUserInfo` 会导致Token残留
- **修复方案**: 合并为单一 `logout` 方法，移除 `clearUserInfo` 或将其标记为内部方法

---

## 三、Minor 级别（体验优化/代码整洁）

### m-01 验证码仅4位纯数字，前端生成无安全意义
- **状态**: [待开发]
- **文件**: `frontend/src/views/login/LoginPage.vue` L257
- **修复方案**: 验证码由后端生成和校验

### m-02 邀请码验证永远成功，缺少Issue追踪号
- **状态**: [待开发]
- **文件**: `frontend/src/views/login/LoginPage.vue` L340-361
- **修复方案**: 对接后端邀请码验证API

### m-03 验证码输入框缺少 `focused` 状态class
- **状态**: [待开发]
- **文件**: `frontend/src/views/login/LoginPage.vue` L652
- **修复方案**: 添加 `captchaFocused` 状态并绑定到class

### m-04 `route.query.redirect` 类型断言不安全
- **状态**: [待开发]
- **文件**: `frontend/src/views/login/LoginPage.vue` L408
- **修复方案**: 处理 `string | string[]` 类型

### m-08 通配符匹配算法边界缺陷
- **状态**: [待开发]
- **文件**: `frontend/src/directives/permission.ts` L110-133
- **问题**: `product:food:*` 会匹配 `product:food`（拥有子权限通配符不等于拥有父级权限）
- **修复方案**: 匹配完成后增加长度校验

### m-11 PermissionCodeTab.vue 硬编码颜色 `#fff`
- **状态**: [待开发]
- **文件**: `frontend/src/views/system/components/PermissionCodeTab.vue` L243
- **修复方案**: 替换为 `var(--fts-text-inverse, #fff)`

### m-12 DomainPermissionTab.vue 硬编码 `rgba(0, 0, 0, ...)`
- **状态**: [待开发]
- **文件**: `frontend/src/views/system/components/DomainPermissionTab.vue` L954, L1196
- **修复方案**: 使用 `var(--fts-shadow-sm)` / `var(--fts-shadow-md)` 等 CSS 变量

### m-13 DomainPermissionTab.vue 1518行超过硬上限
- **状态**: [待开发]
- **文件**: `frontend/src/views/system/components/DomainPermissionTab.vue`
- **修复方案**: 拆分为 DomainMatrixTable / RoleCardList / MenuPreviewPanel

### m-14 UserOverrideTab.vue DOMAIN_TO_MENU_GROUP_MAP 与Store重复定义
- **状态**: [待开发]
- **文件**: `frontend/src/views/system/components/UserOverrideTab.vue` L216-231
- **修复方案**: 提取到 `constants/permission.ts` 统一导出

### m-15 UserOverrideTab.vue 未使用的 UserPermissionDetailRow 接口
- **状态**: [待开发]
- **文件**: `frontend/src/views/system/components/UserOverrideTab.vue` L374
- **修复方案**: 移除该接口，直接使用 `UserPermissionOverride`

### m-16 AuditLogTab.vue detailData 类型推断过于复杂
- **状态**: [待开发]
- **文件**: `frontend/src/views/system/components/AuditLogTab.vue` L104
- **修复方案**: 将 `AuditLogEntry` 提取到 `types/permission.ts` 后直接使用

### m-17 TabBar.vue 右键菜单未监听 Esc 键关闭
- **状态**: [待开发]
- **文件**: `frontend/src/components/layout/TabBar.vue` L159-177
- **修复方案**: 添加 `keydown.escape` 监听和 `onUnmounted` 清理

### m-18 devAuth.ts 缺少 REGION_MANAGER/AUDITOR/OWNER 的Token生成函数
- **状态**: [待开发]
- **文件**: `frontend/src/utils/devAuth.ts`
- **修复方案**: 补充 `generateRegionManagerToken` 和 `generateAuditorToken` 函数

### m-19 devAuth.ts console.log 未受DEV环境守卫保护
- **状态**: [待开发]
- **文件**: `frontend/src/utils/devAuth.ts` L194-375
- **修复方案**: 在每个 `setXxxTokenForDev` 函数开头添加环境检查

### m-20 全局 100+处 `:deep()` 需迁移到全局样式文件
- **状态**: [待开发]
- **修复方案**: 迁移到 `src/styles/global/_element-overrides.scss`

### m-21 全局 80+处硬编码颜色需替换为CSS变量
- **状态**: [待开发]
- **修复方案**: 逐个替换为 `--fts-*` CSS变量

### m-22 24+个孤岛工具函数/composable需清理
- **状态**: [待开发]
- **修复方案**: 确认未使用后删除，或标记为保留并添加注释

---

## 四、已修复事项（本次SDD流水线完成）

| 编号 | 问题 | 修复提交 |
|------|------|---------|
| C-03 | Token过期未检查 | permission.ts — initFromToken 添加 exp 检查 |
| C-05 | v-permission removeChild不可恢复 | directives/permission.ts — display:none替代 |
| C-06 | localStorage权限可篡改 | directives/permission.ts — 移除getLegacyPermissions |
| C-07 | 权限码三方不一致 | CategoryManagement/DishCombo — update→edit；Mock manage→CRUD拆分 |
| M-09 | setDevDefaultUser无生产环境保护 | permission.ts — 添加PROD检查 |
| M-10 | enableMockMode无生产环境保护 | request.ts — 添加PROD检查 |
| M-11 | /forgot-password未在白名单 | guards.ts — 补全白名单 |
| M-13 | App.vue白名单不支持子路由 | App.vue — startsWith替代includes |
| M-19 | devAuth缺少BOM/成本权限 | devAuth.ts — 补充product:bom/cost权限 |
| m-05 | permissions as [] 类型断言 | guards.ts — as string[] |
| m-06 | mapRoles缺少REGION_MANAGER/AUDITOR | permission.ts — 补充映射 |
| F-008 | BomManagement等缺少v-permission | 3个页面补充权限控制 |
| F-011 | v-permission文档注释product:food | directives/permission.ts — 替换为product:dish |
