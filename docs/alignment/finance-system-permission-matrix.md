# 全量数据对齐治理 · 权限矩阵

> 范围：会员营销、财务中心、系统管理、印章/签约、运营决策  
> 说明：梳理各模块路由域、菜单可见角色、按钮级权限码、系统预设权限码的分布与映射；缺失、未落地、命名不统一处已用 `[待修复]` 标注。

---

## 一、跨模块权限控制方式对齐

| 控制维度 | 技术实现 | 会员营销 | 财务中心 | 系统管理 | 印章/签约 | 运营决策 | 对齐结论 |
|---------|---------|---------|---------|---------|----------|---------|---------|
| 路由域权限 | `router/index.ts` 的 `meta.domain` + `guards.ts` 域检查 | `domain: 'member'` | `domain: 'finance'` | `domain: 'system'` | `domain: 'system'`（非 seal） | `domain: 'operations'` | 印章模块未独立域，挂载在 system 域下 `[待修复]` |
| 菜单可见性 | `visibleRoles`（MenuGroupConfig） | 未设置（全员可见） | OWNER/ADMIN/FINANCE_DIRECTOR | OWNER/ADMIN | OWNER/ADMIN/HR/Finance/Ops 总监 | OWNER/ADMIN/OPS_DIRECTOR | 会员菜单未限制角色 `[待修复]` |
| 子菜单规模档 | `scaleLevel` | 无 | 有 | 无 | 无 | 无 | 仅财务中心按企业规模分层展示 |
| 按钮级权限 | `v-permission` 指令 | 未使用 | 未使用 | 未使用 | 未使用 | 未使用 | 四个模块均未对接按钮权限 `[待修复]` |
| 接口级权限 | 后端 `@RequiresPermission` 等 | 未知 | 未知 | 未知 | 未知 | 未知 | 前端未明确后端权限注解清单 `[待修复]` |

---

## 二、路由/域权限矩阵

### 2.1 会员营销域（`member`）

| 路由 | 页面组件 | 菜单标题 | 路由域 | 菜单可见角色 | 按钮权限声明 | 问题 |
|-----|---------|---------|-------|------------|-------------|------|
| `/marketing/member-overview` | `MemberOverview.vue` | 会员概览 | `member` | 未限制 | 无 | `[待修复]` 未配置按钮权限 |
| `/marketing/member-list` | `MemberList.vue` | 会员列表 | `member` | 未限制 | 无 | `[待修复]` 未配置按钮权限 |
| `/marketing/member-detail/:id` | `MemberDetail.vue` | 会员详情 | `member` | 未限制 | 无 | `[待修复]` 未配置按钮权限 |
| `/marketing/member-level` | `MemberLevel.vue` | 会员等级 | `member` | 未限制 | 无 | `[待修复]` 未配置按钮权限 |
| `/marketing/recharge` | `RechargeManage.vue` | 储值管理 | `member` | 未限制 | 无 | `[待修复]` 未配置按钮权限 |
| `/marketing/recharge-settings` | `RechargeSettings.vue` | 储值系统设置 | `member` | 未限制 | 无 | `[待修复]` 未配置按钮权限 |

### 2.2 财务中心域（`finance`）

| 路由 | 页面组件 | 菜单标题 | 路由域 | 规模档位 | 按钮权限声明 | 问题 |
|-----|---------|---------|-------|---------|-------------|------|
| `/finance/ledger` | `FinanceLedger.vue` | 财务总账 | `finance` | standard+ | 无 | `[待修复]` 未配置按钮权限 |
| `/finance/subject` | `FinanceSubject.vue` | 会计科目 | `finance` | standard+ | 无 | `[待修复]` 未配置按钮权限 |
| `/finance/period` | `FinancePeriod.vue` | 会计期间 | `finance` | standard+ | 无 | `[待修复]` 未配置按钮权限 |
| `/finance/receivable` | `FinanceReceivable.vue` | 应收账款 | `finance` | standard+ | 无 | `[待修复]` 未配置按钮权限 |
| `/finance/payable` | `FinancePayable.vue` | 应付账款 | `finance` | standard+ | 无 | `[待修复]` 未配置按钮权限 |
| `/finance/cost` | `FinanceCost.vue` | 成本管理 | `finance` | standard+ | 无 | `[待修复]` 未配置按钮权限 |
| `/finance/tax` | `FinanceTax.vue` | 税务管理 | `finance` | standard+ | 无 | `[待修复]` 未配置按钮权限 |
| `/finance/invoice-reimbursement` | `InvoiceReimbursement.vue` | 发票报销 | `finance` | standard+ | 无 | `[待修复]` 未配置按钮权限 |
| `/finance/report` | `FinanceReport.vue` | 财务报表 | `finance` | standard+ | 无 | `[待修复]` 未配置按钮权限 |
| `/finance/budget` | `FinanceBudget.vue` | 预算管理 | `finance` | chain-standard+ | 无 | `[待修复]` 未配置按钮权限 |
| `/finance/fund` | `FinanceFund.vue` | 资金管理 | `finance` | chain-standard+ | 无 | `[待修复]` 未配置按钮权限 |
| `/finance/approval` | `FinanceApproval.vue` | 财务审批 | `finance` | chain-standard+ | 无 | `[待修复]` 未配置按钮权限 |
| `/finance/auto-voucher` | `AutoVoucher.vue` | 自动凭证管理 | `finance` | chain-enterprise | 无 | `[待修复]` 未配置按钮权限 |

### 2.3 系统管理域（`system`）

| 路由 | 页面组件 | 菜单标题 | 路由域 | 菜单可见角色 | 按钮权限声明 | 问题 |
|-----|---------|---------|-------|------------|-------------|------|
| `/system/permission-center` | `PermissionCenter.vue` | 权限中心 | `system` | OWNER/ADMIN | 页面内部使用 `system:role:*`、`system:user:*`、`system:permission:*` 等权限码进行角色分配 | 仅角色管理页有权限树，其他页面未使用 `v-permission` |
| `/system/permission` | `SystemSettings.vue` | 系统配置 | `system` | OWNER/ADMIN | 无 | `[待修复]` 未配置按钮权限 |
| `/system/ai-model-config` | `AIModelConfig.vue` | AI模型配置 | `system` | OWNER/ADMIN | 无 | `[待修复]` 未配置按钮权限 |
| `/system/operation-audit` | `OperationAudit.vue` | 操作审计 | `system` | OWNER/ADMIN | 无 | `[待修复]` 未配置按钮权限 |
| `/seal/management` | `SealManagement.vue` | 印章管理 | `system` | OWNER/ADMIN/HR/Finance/Ops 总监 | 无 | `[待修复]` 印章路由未独立 `seal` 域，且未配置按钮权限 |

### 2.4 运营决策域（`operations`）

| 路由 | 页面组件 | 菜单标题 | 路由域 | 菜单可见角色 | 按钮权限声明 | 问题 |
|-----|---------|---------|-------|------------|-------------|------|
| `/operations` | `OperationsDashboard.vue` | 运营总览 | `operations` | OWNER/ADMIN/OPS_DIRECTOR | 无 | `[待修复]` 未配置按钮权限 |
| `/operations/store-archive` | `StoreArchive.vue` | 门店档案 | `operations` | OWNER/ADMIN/OPS_DIRECTOR | 无 | `[待修复]` 未配置按钮权限 |
| `/operations/live-monitor` | `LiveMonitor.vue` | 实时监控 | `operations` | OWNER/ADMIN/OPS_DIRECTOR | 无 | `[待修复]` 未配置按钮权限 |
| `/operations/decision-board` | `DecisionBoard.vue` | 经营分析 | `operations` | OWNER/ADMIN/OPS_DIRECTOR | 无 | `[待修复]` 未配置按钮权限 |
| `/operations/strategy-workshop` | `StrategyWorkshop.vue` | 运营策略 | `operations` | OWNER/ADMIN/OPS_DIRECTOR | 无 | `[待修复]` 未配置按钮权限 |
| `/operations/alert-command-center` | `AlertCommandCenter.vue` | 预警管理 | `operations` | OWNER/ADMIN/OPS_DIRECTOR | 无 | `[待修复]` 未配置按钮权限 |
| `/operations/reports` | `ReportCenter.vue` | 经营报表 | `operations` | OWNER/ADMIN/OPS_DIRECTOR | 无 | `[待修复]` 未配置按钮权限 |

---

## 三、按钮级权限码（`v-permission`）使用现状

### 3.1 全局指令约定

- 指令文件：`frontend/src/directives/permission.ts`
- 权限数据源：`usePermissionStore.userInfo.permissions`，开发环境未检测到权限时降级为 `['*']`
- 支持精确匹配、模块通配符（`module:*`）、多级通配符（`module:sub:*`）
- 支持隐藏模式（默认）与禁用模式（`.disable`）

### 3.2 目标四个模块 `v-permission` 使用情况

| 模块 | 文件数 | 使用 `v-permission` 的文件数 | 示例权限码 | 结论 |
|-----|-------|-------------------------|-----------|------|
| 会员营销 | 6+ | 0 | — | 完全未对接按钮权限 `[待修复]` |
| 财务中心 | 12+ | 0 | — | 完全未对接按钮权限 `[待修复]` |
| 系统管理 | 10+ | 0（PermissionCenter 内部仅展示权限树，未用指令控制 UI） | — | 页面按钮未用 `v-permission` 控制 `[待修复]` |
| 印章/签约 | 2+ | 0 | — | 完全未对接按钮权限 `[待修复]` |
| 运营决策 | 7+ | 0 | — | 完全未对接按钮权限 `[待修复]` |

### 3.3 项目内实际使用 `v-permission` 的模块（对比参考）

| 模块 | 文件 | 权限码示例 | 说明 |
|-----|------|-----------|------|
| 产品中心 | `DishCostAnalysis.vue` | `product:cost:export` | 有实际按钮权限控制 |
| 产品中心 | `CategoryManagement.vue` | `product:category:create` | 有实际按钮权限控制 |
| 产品中心 | `DishCombo.vue` | `product:combo:create` | 有实际按钮权限控制 |
| 产品中心 | `DishPricing.vue` | `product:pricing:batch`、`product:pricing:edit` | 有实际按钮权限控制 |

> 结论：按钮权限控制仅在产品中心落地，目标四个模块均未使用，存在明显的权限控制空白 `[待修复]`。

---

## 四、系统预设权限码清单

### 4.1 `frontend/src/utils/permissions.ts` 中定义的权限码

| 模块 | 权限码 | 中文含义 | 是否在目标模块中使用 | 状态 |
|-----|-------|---------|------------------|------|
| 营销中心 | `marketing:manage` | 管理营销相关功能 | 否 | `[待修复]` 未使用 |
| 营销中心 | `marketing:view` | 查看营销信息 | 否 | `[待修复]` 未使用 |
| 营销中心 | `marketing:member:manage` | 管理会员 | 否 | `[待修复]` 未使用 |
| 营销中心 | `marketing:promotion:manage` | 管理营销活动 | 否（目标模块无营销活动页） | `[待修复]` 页面缺失/权限码悬空 |
| 财务管理 | `finance:manage` | 管理财务相关功能 | 否 | `[待修复]` 未使用 |
| 财务管理 | `finance:view` | 查看财务信息 | 否 | `[待修复]` 未使用 |
| 财务管理 | `finance:income:manage` | 管理收入 | 否 | `[待修复]` 未使用 |
| 财务管理 | `finance:expense:manage` | 管理支出 | 否 | `[待修复]` 未使用 |
| 财务管理 | `finance:profit:view` | 查看利润分析 | 否 | `[待修复]` 未使用 |
| 财务管理 | `finance:report:view` | 查看财务报表 | 否 | `[待修复]` 未使用 |
| 财务管理 | `finance:tax:manage` | 管理税务 | 否 | `[待修复]` 未使用 |
| 财务管理 | `finance:invoice:manage` | 管理发票 | 否 | `[待修复]` 未使用 |
| 财务管理 | `finance:warning:manage` | 管理财务预警 | 否（页面缺失） | `[待修复]` 页面缺失/权限码悬空 |
| 财务管理 | `finance:approval:manage` | 管理财务审批 | 否 | `[待修复]` 未使用 |
| 财务管理 | `finance:forecast:view` | 查看财务预测 | 否（页面缺失） | `[待修复]` 页面缺失/权限码悬空 |
| 系统设置 | `system:manage` | 管理系统设置 | 否 | `[待修复]` 未使用 |
| 系统设置 | `system:view` | 查看系统信息 | 否 | `[待修复]` 未使用 |
| 系统设置 | `user:manage` | 管理用户 | 否 | `[待修复]` 未使用 |
| 系统设置 | `role:manage` | 管理角色 | 否 | `[待修复]` 未使用 |
| 系统设置 | `menu:manage` | 管理菜单 | 否 | `[待修复]` 未使用 |
| 系统设置 | `operation:log:view` | 查看操作日志 | 否 | `[待修复]` 未使用 |

### 4.2 `RoleManagementTab.vue` 中权限分配树定义的权限码

| 一级 | 二级 | 权限码 | 中文含义 | 是否在后端/前端有对应接口 |
|-----|------|-------|---------|----------------------|
| 系统管理 | 角色管理 | `system:role:read` | 查看角色 | 前端有 roleApi，未校验此码 |
| 系统管理 | 角色管理 | `system:role:create` | 创建角色 | 前端有 roleApi，未校验此码 |
| 系统管理 | 角色管理 | `system:role:update` | 更新角色 | 前端有 roleApi，未校验此码 |
| 系统管理 | 角色管理 | `system:role:delete` | 删除角色 | 前端有 roleApi，未校验此码 |
| 系统管理 | 角色管理 | `system:role:update-status` | 状态切换 | 前端有 roleApi，未校验此码 |
| 系统管理 | 角色管理 | `system:role:assign-permissions` | 分配权限 | 前端有 roleApi，未校验此码 |
| 系统管理 | 用户管理 | `system:user:read` | 查看用户 | 前端有 userApi，未校验此码 |
| 系统管理 | 用户管理 | `system:user:create` | 创建用户 | 前端有 userApi，未校验此码 |
| 系统管理 | 用户管理 | `system:user:update` | 更新用户 | 前端有 userApi，未校验此码 |
| 系统管理 | 用户管理 | `system:user:delete` | 删除用户 | 前端有 userApi，未校验此码 |
| 系统管理 | 权限管理 | `system:permission:read` | 查看权限 | 前端有 permissionApi，未校验此码 |
| 系统管理 | 权限管理 | `system:permission:config` | 配置权限 | 前端有 permissionApi，未校验此码 |
| 食品溯源 | — | `food:*` | 全部 | 未明确对应页面 |
| 食品溯源 | — | `food:read` | 查看 | 未明确对应页面 |
| 食品溯源 | — | `food:create` | 创建 | 未明确对应页面 |
| 食品溯源 | — | `food:update` | 更新 | 未明确对应页面 |
| 溯源记录 | — | `trace:*` | 全部 | 未明确对应页面 |
| 溯源记录 | — | `trace:read` | 查看 | 未明确对应页面 |
| 溯源记录 | — | `trace:create` | 创建 | 未明确对应页面 |
| 审计日志 | — | `audit:read` | 查看审计日志 | 未校验此码 |
| 审计日志 | — | `audit:export` | 导出审计日志 | 未校验此码 |

> 问题：权限分配树仅覆盖系统管理、溯源、审计，缺少会员营销、财务中心、印章/签约、运营决策的权限码 `[待修复]`。

---

## 五、角色-域权限矩阵（路由守卫层）

来源：`frontend/src/router/guards.ts` 中的 `getRoleDomainAccessLevel`

| 角色 | workspace | store-ops | product | order | operations | purchase | warehouse | member | finance | hr | traceability | device | asset | system |
|-----|-----------|-----------|---------|-------|------------|----------|-----------|--------|---------|----|--------------|--------|-------|--------|
| OWNER / ADMIN | FULL | FULL | FULL | FULL | FULL | FULL | FULL | FULL | FULL | FULL | FULL | FULL | FULL | FULL |
| OPS_DIRECTOR | FULL | FULL | FULL | FULL | FULL | READ_ONLY | READ_ONLY | FULL | READ_ONLY | READ_ONLY | READ_ONLY | READ_ONLY | READ_ONLY | READ_ONLY |
| FINANCE_DIRECTOR | FULL | READ_ONLY | READ_ONLY | READ_ONLY | READ_ONLY | READ_ONLY | READ_ONLY | READ_ONLY | FULL | READ_ONLY | READ_ONLY | READ_ONLY | READ_ONLY | READ_ONLY |
| HR_DIRECTOR | FULL | READ_ONLY | READ_ONLY | READ_ONLY | READ_ONLY | READ_ONLY | READ_ONLY | READ_ONLY | READ_ONLY | FULL | READ_ONLY | READ_ONLY | READ_ONLY | READ_ONLY |
| STORE_MANAGER | FULL | FULL | FULL | FULL | READ_ONLY | READ_ONLY | READ_ONLY | FULL | READ_ONLY | READ_ONLY | READ_ONLY | READ_ONLY | READ_ONLY | READ_ONLY |
| TEAM_LEADER | FULL | HIDDEN | HIDDEN | HIDDEN | HIDDEN | HIDDEN | HIDDEN | HIDDEN | HIDDEN | HIDDEN | HIDDEN | HIDDEN | HIDDEN | HIDDEN |
| REGION_MANAGER | FULL | HIDDEN | HIDDEN | HIDDEN | HIDDEN | HIDDEN | HIDDEN | HIDDEN | HIDDEN | HIDDEN | HIDDEN | HIDDEN | HIDDEN | HIDDEN |
| AUDITOR | FULL | HIDDEN | HIDDEN | HIDDEN | HIDDEN | HIDDEN | HIDDEN | HIDDEN | HIDDEN | HIDDEN | HIDDEN | HIDDEN | HIDDEN | HIDDEN |
| EMPLOYEE | FULL | HIDDEN | HIDDEN | HIDDEN | HIDDEN | HIDDEN | HIDDEN | HIDDEN | HIDDEN | HIDDEN | HIDDEN | HIDDEN | HIDDEN | HIDDEN |
| SCHEDULER | FULL | HIDDEN | HIDDEN | HIDDEN | HIDDEN | HIDDEN | HIDDEN | HIDDEN | HIDDEN | HIDDEN | HIDDEN | HIDDEN | HIDDEN | HIDDEN |

> 说明：`FULL` 表示可读写，`READ_ONLY` 表示仅可查看页面但无法确定按钮是否禁用（因为按钮级权限未落地），`HIDDEN` 表示不可访问。

---

## 六、菜单-角色可见性矩阵

| 一级菜单 | 可见角色 | 子菜单规模控制 | 与路由域是否一致 | 问题 |
|---------|---------|---------------|----------------|------|
| 会员管理 | 未设置（全员） | 无 | 是（`member`） | `[待修复]` 未限制角色，所有登录用户可见 |
| 财务中心 | OWNER / ADMIN / FINANCE_DIRECTOR | 有（standard/chain-standard/chain-enterprise） | 是（`finance`） | 财务总监外的角色（如 ops_director）在 guards 中 finance 域为 READ_ONLY，但菜单不可见，存在菜单与路由权限不一致 `[待修复]` |
| 系统管理 | OWNER / ADMIN | 无 | 是（`system`） | 印章路由也挂在 system 域，但印章菜单对更多角色可见 |
| 电子签章 | OWNER / ADMIN / HR_DIRECTOR / FINANCE_DIRECTOR / OPS_DIRECTOR | 无 | 否（路由域为 `system`，菜单组 id 为 `seal`） | `[待修复]` 路由域与菜单组不一致，且 system 域对 HR/Finance/Ops 总监在 guards 中为 READ_ONLY，理论上可访问，但菜单显示而页面按钮未控 |
| 运营中心 | OWNER / ADMIN / OPS_DIRECTOR | 无 | 是（`operations`） | ops_director 外角色在 guards 中 operations 域为 READ_ONLY，但菜单不可见，存在不一致 `[待修复]` |

---

## 七、推荐的目标模块按钮权限码

基于现有页面动作，建议补充以下按钮权限码（与字段矩阵、动作矩阵保持一致）：

### 7.1 会员营销

| 页面 | 建议权限码 | 对应动作 |
|-----|-----------|---------|
| 会员列表 | `member:list:create` | 新增会员 |
| 会员列表 | `member:list:edit` | 编辑会员 |
| 会员列表 | `member:list:delete` | 删除会员 |
| 会员列表 | `member:list:recharge` | 会员充值 |
| 会员列表 | `member:list:level-adjust` | 调整等级 |
| 会员列表 | `member:list:status-toggle` | 冻结/启用 |
| 会员列表 | `member:list:import` | 批量导入 |
| 会员列表 | `member:list:export` | 导出 |
| 会员等级 | `member:level:create` | 新建等级 |
| 会员等级 | `member:level:edit` | 编辑等级 |
| 会员等级 | `member:level:delete` | 删除等级 |
| 储值管理 | `member:recharge:create` | 新增充值 |
| 储值管理 | `member:recharge:refund` | 退款 |

### 7.2 财务中心

| 页面 | 建议权限码 | 对应动作 |
|-----|-----------|---------|
| 财务总账/凭证 | `finance:voucher:create` | 新增凭证 |
| 财务总账/凭证 | `finance:voucher:edit` | 编辑凭证 |
| 财务总账/凭证 | `finance:voucher:approve` | 审核/反审核 |
| 财务总账/凭证 | `finance:voucher:post` | 过账/反过账 |
| 财务总账/凭证 | `finance:voucher:void` | 作废 |
| 应收账款 | `finance:receivable:create` | 新增应收 |
| 应收账款 | `finance:receivable:collect` | 收款 |
| 应收账款 | `finance:receivable:write-off` | 核销 |
| 应付账款 | `finance:payable:create` | 新增应付 |
| 应付账款 | `finance:payable:pay` | 付款 |
| 发票报销 | `finance:invoice:create` | 新增发票/报销 |
| 发票报销 | `finance:invoice:approve` | 审批 |
| 预算管理 | `finance:budget:create` | 新增预算 |
| 预算管理 | `finance:budget:edit` | 编辑预算 |
| 预算管理 | `finance:budget:actual-update` | 更新实际金额 |
| 资金管理 | `finance:fund:transfer` | 资金调拨 |

### 7.3 系统管理

| 页面 | 建议权限码 | 对应动作 |
|-----|-----------|---------|
| 权限中心-角色 | `system:role:create/update/delete/assign-permissions`（已有） | 角色 CRUD |
| 权限中心-用户 | `system:user:create/update/delete`（已有） | 用户 CRUD |
| 权限中心-权限码 | `system:permission:create/update/delete/sync` | 权限码管理 |
| 操作审计 | `system:audit:read/export` | 查看/导出日志 |
| AI模型配置 | `system:ai-config:manage` | AI 配置 |

### 7.4 印章/签约

| 页面 | 建议权限码 | 对应动作 |
|-----|-----------|---------|
| 印章管理 | `seal:seal:create/update/delete` | 印章 CRUD |
| 印章管理 | `seal:usage:create` | 用印申请 |
| 印章管理 | `seal:usage:approve` | 用印审批 |

### 7.5 运营决策

| 页面 | 建议权限码 | 对应动作 |
|-----|-----------|---------|
| 经营分析 | `operations:decision:view` | 查看分析 |
| 经营报表 | `operations:report:view/export` | 查看/导出报表 |
| 预警管理 | `operations:alert:view/handle` | 查看/处理预警 |
| 运营策略 | `operations:strategy:view/edit` | 查看/编辑策略 |

---

## 八、核心权限问题汇总

| 序号 | 问题 | 影响范围 | 严重程度 | 修复建议 |
|-----|------|---------|---------|---------|
| 1 | 目标四个模块均未使用 `v-permission` 指令控制按钮 | 会员/财务/系统/印章/运营 | 高 | 为各页面按钮补充 `v-permission` 或 `.disable` |
| 2 | `frontend/src/utils/permissions.ts` 中定义的权限码与页面实际动作脱节 | 营销/财务/系统 | 高 | 清理悬空权限码，按页面动作重新对齐 |
| 3 | 印章路由挂载在 `system` 域，未独立 `seal` 域 | 印章/签约 | 中 | 路由 `meta.domain` 改为 `seal`，guards 增加 seal 域配置 |
| 4 | 会员管理菜单未限制 `visibleRoles` | 会员营销 | 中 | 补充 `visibleRoles: [OWNER, ADMIN, OPS_DIRECTOR, STORE_MANAGER]` |
| 5 | 菜单 `visibleRoles` 与路由守卫域矩阵存在不一致 | 财务/运营/印章 | 中 | 统一菜单可见性与域访问级别 |
| 6 | `RoleManagementTab.vue` 权限分配树缺少会员/财务/印章/运营权限码 | 权限中心 | 高 | 扩展权限树，覆盖目标模块 |
| 7 | 财务中心按规模档位分层，但缺少对应的域权限细分 | 财务中心 | 低 | 在权限模板中按规模控制财务子菜单 |
| 8 | 操作审计/AI配置/系统配置页面按钮无权限控制 | 系统管理 | 中 | 补充按钮权限码 |
| 9 | 后端接口级权限注解清单未明确 | 全模块 | 高 | 后端补充 `@RequiresPermission` 并与前端权限码对齐 |
| 10 | 开发环境 `v-permission` 降级为 `['*']`，测试时无法发现权限遗漏 | 全局 | 中 | 增加权限测试模式或明确测试方案 |

---

## 九、产出文件路径

- `P:\my-new-project\docs\alignment\finance-system-field-matrix.md`
- `P:\my-new-project\docs\alignment\finance-system-state-machine.md`
- `P:\my-new-project\docs\alignment\finance-system-action-matrix.md`
- `P:\my-new-project\docs\alignment\finance-system-permission-matrix.md`（本文件）
