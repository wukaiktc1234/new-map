# 人事 / 资产 / 设备模块权限对齐矩阵

> 范围：人事管理、资产管理、设备管理三个一级菜单及其子功能。
> 仅做梳理，未修改源码。`[待修复]` 标注权限缺失、权限与菜单/动作不匹配、硬编码未注册权限等问题。

---

## 1. 当前权限定义（`frontend/src/utils/permissions.ts`）

### 1.1 人事管理（HR）

| 权限码 | 描述 | 覆盖动作（推断） | 注册状态 |
|--------|------|-----------------|----------|
| `hr:manage` | 管理人事相关功能 | 全部人事动作 | ✅ |
| `hr:view` | 查看人事信息 | 全部人事查询/详情 | ✅ |
| `hr:employee:manage` | 管理员工 | 员工增删改、批量调整、调动、统计 | ✅ |
| `hr:organization:manage` | 管理组织架构 | 部门/岗位增删改、启用禁用、排序 | ✅ |
| `hr:attendance:manage` | 管理考勤 | 考勤/排班增删改、确认、同步 | ✅ |
| `hr:salary:manage` | 管理薪资 | 薪资增删改、确认、批次操作 | ✅ |
| `hr:recruitment:manage` | 管理招聘 | 职位/简历/面试/录用增删改 | ✅ |
| `hr:health:manage` | 管理健康证 | 健康证增删改、审核、报销、提醒 | ✅ |
| `hr:contract:manage` | 管理合同 | 合同/合同模板增删改、签署、终止等 | ✅ |
| `hr:probation:manage` | 管理试用 | 试用期相关（当前前端无独立 API） | ✅ |
| `hr:analytics:view` | 查看人事分析 | 人事统计、分析页面 | ✅ |

### 1.2 资产管理（Asset）

| 权限码 | 描述 | 覆盖动作（推断） | 注册状态 |
|--------|------|-----------------|----------|
| `asset:manage` | 管理资产相关功能 | 全部资产动作 | ✅ |
| `asset:view` | 查看资产信息 | 全部资产查询/详情 | ✅ |
| `asset:ledger:view` | 查看资产台账 | 资产主数据查询 | ✅ |
| `asset:category:manage` | 管理资产分类 | 分类增删改 | ✅ |
| `asset:depreciation:manage` | 管理资产折旧 | 折旧增删改、计算、批量计算 | ✅ |
| `asset:inventory:manage` | 管理资产盘点 | 盘点增删改、开始/完成/取消、录入结果 | ✅ |
| `asset:maintenance:manage` | 管理资产维护 | 维修增删改、开始/完成 | ✅ |
| `asset:disposal:manage` | 管理资产处置 | 处置增删改、审批、执行 | ✅ |
| `asset:report:view` | 查看资产报表 | 资产概览、报表页面 | ✅ |
| `asset:transfer:manage` | 管理资产调拨 | 调拨增删改、审批、完成 | ❌ 缺失 | 调拨功能存在但无对应权限码。`[待修复]` |

### 1.3 设备管理（Device）

| 权限码 | 描述 | 覆盖动作（推断） | 注册状态 |
|--------|------|-----------------|----------|
| `device:manage` | 管理设备 | 设备增删改、状态更新、模拟操作 | ❌ 缺失 | 设备管理模块无任何权限码注册。`[待修复]` |
| `device:view` | 查看设备 | 设备查询、监控、历史、告警查询 | ❌ 缺失 | 同上。`[待修复]` |
| `device:alert:manage` | 管理设备告警 | 告警处理、批量处理 | ❌ 缺失 | 同上。`[待修复]` |

---

## 2. 菜单项与权限映射

### 2.1 人事管理菜单（`frontend/src/modules/hr/menu.ts`）

| 菜单项 | 路由 | 可见角色 | 推荐权限码 | 当前权限码 | 问题 |
|--------|------|---------|------------|------------|------|
| 员工管理 | `/hr/employee` | OWNER, ADMIN, HR_DIRECTOR | `hr:employee:manage` | `hr:employee:manage` | ✅ |
| 组织架构 | `/hr/organization` | OWNER, ADMIN, HR_DIRECTOR | `hr:organization:manage` | `hr:organization:manage` | ✅ |
| 岗位管理 | `/hr/position` | OWNER, ADMIN, HR_DIRECTOR | `hr:organization:manage` | `hr:organization:manage` | ✅ |
| 考勤排班 | `/hr/attendance` | OWNER, ADMIN, HR_DIRECTOR | `hr:attendance:manage` | `hr:attendance:manage` | ✅ |
| 招聘管理 | `/hr/recruitment` | OWNER, ADMIN, HR_DIRECTOR | `hr:recruitment:manage` | `hr:recruitment:manage` | ✅ |
| 入职办理 | `/hr/onboarding` | OWNER, ADMIN, HR_DIRECTOR | `hr:onboarding:manage` | — | 权限码缺失，实际依赖 `hr:employee:manage`。`[待修复]` |
| 培训发展 | `/hr/training` | OWNER, ADMIN, HR_DIRECTOR | `hr:training:manage` | — | 权限码缺失，实际依赖 `hr:employee:manage`。`[待修复]` |
| 健康证管理 | `/hr/health-certificate` | OWNER, ADMIN, HR_DIRECTOR | `hr:health:manage` | `hr:health:manage` | ✅ |
| 合同智能管理 | `/hr/contract-dashboard` | OWNER, ADMIN, HR_DIRECTOR | `hr:contract:manage` / `hr:analytics:view` | `hr:contract:manage` | ⚠️ 无独立权限 |
| 合同管理 | `/hr/contract` | OWNER, ADMIN, HR_DIRECTOR | `hr:contract:manage` | `hr:contract:manage` | ✅ |
| 合同模板 | `/hr/contract-template` | OWNER, ADMIN, HR_DIRECTOR | `hr:contract:manage` | `hr:contract:manage` | ✅ |
| 合同模板库 | `/hr/contract-template-library` | OWNER, ADMIN, HR_DIRECTOR | `hr:contract:manage` | `hr:contract:manage` | ⚠️ 与合同模板共用一个权限 |
| 薪资管理 | `/hr/salary` | OWNER, ADMIN, HR_DIRECTOR | `hr:salary:manage` | `hr:salary:manage` | ✅ |
| 合规审批 | `/hr/approval` | OWNER, ADMIN, HR_DIRECTOR | `hr:approval:manage` | — | 权限码缺失，实际依赖各业务模块权限或 `hr:manage`。`[待修复]` |
| 知识库智能 | `/hr/knowledge-intelligence` | OWNER, ADMIN, HR_DIRECTOR | `hr:knowledge:manage` | — | 权限码缺失，实际依赖 `hr:employee:manage`。`[待修复]` |
| 人事分析 | `/hr/analytics` | OWNER, ADMIN, HR_DIRECTOR | `hr:analytics:view` | `hr:analytics:view` | ✅ |
| 员工画像 | `/hr/employee-intelligence` | OWNER, ADMIN, HR_DIRECTOR | `hr:analytics:view` / `hr:employee:view` | `hr:analytics:view` | ⚠️ 无独立查看权限 |
| 邀请码管理 | `/hr/invitation-code` | OWNER, ADMIN, HR_DIRECTOR | `hr:employee:manage` / `hr:onboarding:manage` | — | 权限码缺失。`[待修复]` |
| 配置中心 | `/hr/config-center` | OWNER, ADMIN, HR_DIRECTOR | `hr:manage` | `hr:manage` | ✅ |

### 2.2 资产管理菜单（`frontend/src/modules/asset/menu.ts`）

| 菜单项 | 路由 | 可见角色 | 推荐权限码 | 当前权限码 | 问题 |
|--------|------|---------|------------|------------|------|
| 资产概览 | `/asset/overview` | OWNER, ADMIN | `asset:view` / `asset:report:view` | `asset:view` | ✅ |
| 资产台账 | `/asset/ledger` | OWNER, ADMIN | `asset:ledger:view` / `asset:manage` | `asset:ledger:view` | ✅ |
| 资产分类 | `/asset/category` | OWNER, ADMIN | `asset:category:manage` | `asset:category:manage` | ✅ |
| 资产折旧 | `/asset/depreciation` | OWNER, ADMIN | `asset:depreciation:manage` | `asset:depreciation:manage` | ✅ |
| 资产盘点 | `/asset/inventory-check` | OWNER, ADMIN | `asset:inventory:manage` | `asset:inventory:manage` | ✅ |
| 资产维护 | `/asset/maintenance` | OWNER, ADMIN | `asset:maintenance:manage` | `asset:maintenance:manage` | ✅ |
| 资产处置 | `/asset/disposal` | OWNER, ADMIN | `asset:disposal:manage` | `asset:disposal:manage` | ✅ |
| 资产报表 | `/asset/report` | OWNER, ADMIN | `asset:report:view` | `asset:report:view` | ✅ |
| 资产调拨（无菜单入口） | — | — | `asset:transfer:manage` | — | 有 API 文件和类型定义，但无菜单入口和权限码。`[待修复]` |

### 2.3 设备管理菜单（`frontend/src/modules/device/menu.ts`）

| 菜单项 | 路由 | 可见角色 | 推荐权限码 | 当前权限码 | 问题 |
|--------|------|---------|------------|------------|------|
| 设备列表 | `/device/list` | OWNER, ADMIN | `device:manage` / `device:view` | — | 设备管理模块整体无权限码注册，菜单仅靠角色控制。`[待修复]` |
| 设备监控 | `/device/monitor` | OWNER, ADMIN | `device:view` | — | 同上。`[待修复]` |
| 设备告警 | `/device/alerts` | OWNER, ADMIN | `device:alert:manage` / `device:view` | — | 同上。`[待修复]` |
| 状态历史 | `/device/status-history` | OWNER, ADMIN | `device:view` | — | 同上。`[待修复]` |

---

## 3. 权限与动作细粒度对照

### 3.1 人事管理

| 功能域 | 动作 | 推荐权限码 | 当前实际权限码 | 说明 |
|--------|------|------------|----------------|------|
| 员工 | 查看 | `hr:employee:view` | `hr:view` / `hr:employee:manage` | 缺少只读权限，查看与编辑共用 `hr:employee:manage`。`[待修复]` |
| 员工 | 新增/编辑/删除 | `hr:employee:manage` | `hr:employee:manage` | ✅ |
| 部门 | 查看 | `hr:organization:view` | `hr:view` / `hr:organization:manage` | 缺少只读权限。`[待修复]` |
| 部门 | 新增/编辑/删除/排序 | `hr:organization:manage` | `hr:organization:manage` | ✅ |
| 岗位 | 查看 | `hr:organization:view` | `hr:view` / `hr:organization:manage` | 缺少只读权限。`[待修复]` |
| 岗位 | 新增/编辑/删除 | `hr:organization:manage` | `hr:organization:manage` | ✅ |
| 考勤 | 查看 | `hr:attendance:view` | `hr:view` / `hr:attendance:manage` | 缺少只读权限。`[待修复]` |
| 考勤 | 新增/编辑/删除/确认/同步 | `hr:attendance:manage` | `hr:attendance:manage` | ✅ |
| 薪资 | 查看 | `hr:salary:view` | `hr:view` / `hr:salary:manage` | 缺少只读权限。`[待修复]` |
| 薪资 | 新增/编辑/删除/发放/批量发放 | `hr:salary:manage` | `hr:salary:manage` | ✅ |
| 招聘 | 查看 | `hr:recruitment:view` | `hr:view` / `hr:recruitment:manage` | 缺少只读权限。`[待修复]` |
| 招聘 | 新增/编辑/删除/发布/面试/录用 | `hr:recruitment:manage` | `hr:recruitment:manage` | ✅ |
| 合同 | 查看 | `hr:contract:view` | `hr:view` / `hr:contract:manage` | 缺少只读权限。`[待修复]` |
| 合同 | 新增/编辑/删除/签署/终止/续签 | `hr:contract:manage` | `hr:contract:manage` | ✅ |
| 健康证 | 查看 | `hr:health:view` | `hr:view` / `hr:health:manage` | 缺少只读权限。`[待修复]` |
| 健康证 | 新增/编辑/删除/审核/报销 | `hr:health:manage` | `hr:health:manage` | ✅ |
| 入职办理 | 全部 | `hr:onboarding:manage` | `hr:employee:manage` | 权限码缺失。`[待修复]` |
| 培训发展 | 全部 | `hr:training:manage` | `hr:employee:manage` | 权限码缺失。`[待修复]` |
| 知识库 | 全部 | `hr:knowledge:manage` | `hr:employee:manage` | 权限码缺失。`[待修复]` |
| 合同智能/员工画像/邀请码/合规审批 | 全部 | 对应独立权限 | `hr:contract:manage` / `hr:analytics:view` / `hr:employee:manage` / `hr:manage` | 权限粒度粗，多个菜单共用一个权限。`[待修复]` |

### 3.2 资产管理

| 功能域 | 动作 | 推荐权限码 | 当前实际权限码 | 说明 |
|--------|------|------------|----------------|------|
| 资产主数据 | 查看 | `asset:view` / `asset:ledger:view` | `asset:view` / `asset:ledger:view` | ✅ |
| 资产主数据 | 新增/编辑/删除 | `asset:manage` | `asset:manage` | ✅ |
| 资产主数据 | 维修/报废 | `asset:maintenance:manage` / `asset:disposal:manage` | — | 前端 API 未暴露，权限未使用。`[待修复]` |
| 资产分类 | 查看 | `asset:view` / `asset:category:view` | `asset:view` / `asset:category:manage` | 缺少只读权限。`[待修复]` |
| 资产分类 | 新增/编辑/删除 | `asset:category:manage` | `asset:category:manage` | ✅ |
| 折旧 | 查看 | `asset:view` / `asset:depreciation:view` | `asset:view` / `asset:depreciation:manage` | 缺少只读权限。`[待修复]` |
| 折旧 | 新增/编辑/删除/计算 | `asset:depreciation:manage` | `asset:depreciation:manage` | ✅ |
| 盘点 | 查看 | `asset:view` / `asset:inventory:view` | `asset:view` / `asset:inventory:manage` | 缺少只读权限。`[待修复]` |
| 盘点 | 新增/编辑/开始/完成/取消 | `asset:inventory:manage` | `asset:inventory:manage` | ✅ |
| 维护 | 查看 | `asset:view` / `asset:maintenance:view` | `asset:view` / `asset:maintenance:manage` | 缺少只读权限。`[待修复]` |
| 维护 | 新增/编辑/开始/完成 | `asset:maintenance:manage` | `asset:maintenance:manage` | ✅ |
| 处置 | 查看 | `asset:view` / `asset:disposal:view` | `asset:view` / `asset:disposal:manage` | 缺少只读权限。`[待修复]` |
| 处置 | 新增/编辑/审批/执行 | `asset:disposal:manage` | `asset:disposal:manage` | ✅ |
| 调拨 | 全部 | `asset:transfer:manage` | — | 权限码缺失，功能也未实现。`[待修复]` |
| 报表 | 查看 | `asset:report:view` | `asset:report:view` | ✅ |

### 3.3 设备管理

| 功能域 | 动作 | 推荐权限码 | 当前实际权限码 | 说明 |
|--------|------|------------|----------------|------|
| 设备主数据 | 查看 | `device:view` | — | 未注册权限码。`[待修复]` |
| 设备主数据 | 新增/编辑/删除/状态更新 | `device:manage` | — | 未注册权限码。`[待修复]` |
| 设备监控 | 查看 | `device:view` | — | 未注册权限码。`[待修复]` |
| 设备监控 | 模拟上线/离线 | `device:manage` | — | 未注册权限码。`[待修复]` |
| 设备告警 | 查看 | `device:view` / `device:alert:view` | — | 未注册权限码。`[待修复]` |
| 设备告警 | 处理/批量处理 | `device:alert:manage` / `device:manage` | — | 未注册权限码。`[待修复]` |
| 状态历史 | 查看 | `device:view` | — | 未注册权限码。`[待修复]` |

---

## 4. 角色与菜单可见性矩阵

| 角色 | HR 菜单 | Asset 菜单 | Device 菜单 | 说明 |
|------|---------|------------|-------------|------|
| OWNER | ✅ | ✅ | ✅ | 全部可见 |
| ADMIN | ✅ | ✅ | ✅ | 全部可见 |
| HR_DIRECTOR | ✅ | ❌ | ❌ | 仅 HR 菜单 |
| OPS_DIRECTOR | ❌ | ❌ | ❌ | 未授权 |
| FINANCE_DIRECTOR | ❌ | ❌ | ❌ | 未授权（理论上应有资产/薪资查看权限）。`[待修复]` |
| STORE_MANAGER | ❌ | ❌ | ❌ | 未授权（门店场景下应有部分查看/排班权限）。`[待修复]` |
| EMPLOYEE | ❌ | ❌ | ❌ | 未授权 |

---

## 5. 权限使用审计

### 5.1 已注册权限的使用情况

| 权限码 | 是否在组件/视图中使用 | 使用位置 | 结论 |
|--------|----------------------|----------|------|
| `hr:manage` | 仅菜单推断 | `hrMenu` | 使用 |
| `hr:view` | 仅菜单推断 | `hrMenu` | 使用 |
| `hr:employee:manage` | 仅菜单推断 | `hrMenu` | 使用 |
| `hr:organization:manage` | 菜单 + 组件推断 | `hrMenu`, `OrgTreeNode.vue` | 使用 |
| `hr:attendance:manage` | 菜单推断 | `hrMenu` | 使用 |
| `hr:salary:manage` | 菜单推断 | `hrMenu` | 使用 |
| `hr:recruitment:manage` | 菜单推断 | `hrMenu` | 使用 |
| `hr:health:manage` | 菜单推断 | `hrMenu` | 使用 |
| `hr:contract:manage` | 菜单推断 | `hrMenu` | 使用 |
| `hr:probation:manage` | 未明确使用 | — | 已注册但无独立功能对应。`[待修复]` |
| `hr:analytics:view` | 菜单推断 | `hrMenu` | 使用 |
| `asset:manage` | 菜单推断 | `assetMenu` | 使用 |
| `asset:view` | 菜单推断 | `assetMenu` | 使用 |
| `asset:ledger:view` | 菜单推断 | `assetMenu` | 使用 |
| `asset:category:manage` | 菜单推断 | `assetMenu` | 使用 |
| `asset:depreciation:manage` | 菜单推断 | `assetMenu` | 使用 |
| `asset:inventory:manage` | 菜单推断 | `assetMenu` | 使用 |
| `asset:maintenance:manage` | 菜单推断 | `assetMenu` | 使用 |
| `asset:disposal:manage` | 菜单推断 | `assetMenu` | 使用 |
| `asset:report:view` | 菜单推断 | `assetMenu` | 使用 |

### 5.2 硬编码但未注册权限

| 权限码 | 使用位置 | 问题 |
|--------|----------|------|
| `department:delete` | `frontend/src/views/hr/components/OrgTreeNode.vue` | 组件中直接校验 `department:delete`，但 `systemModules` 未注册该权限，非 ADMIN 用户永远无法删除部门。`[待修复]` |

---

## 6. 跨模块权限问题汇总

| 问题类别 | 影响范围 | 严重程度 | 说明 |
|---------|---------|----------|------|
| 设备管理模块权限完全缺失 | Device 全部菜单/动作 | P0 | `systemModules` 无 `device:*` 任何权限码，设备模块仅靠 `visibleRoles` 控制菜单，页面内按钮无权限约束。`[待修复]` |
| 调拨功能权限缺失 | Asset 调拨 | P1 | 有 `transferApi` 和类型定义，但无菜单入口、无 `asset:transfer:manage` 权限码。`[待修复]` |
| 多个 HR 子菜单无独立权限 | 入职办理、培训发展、知识库、合规审批、邀请码管理、合同智能管理、员工画像 | P1 | 这些菜单实际依赖 `hr:employee:manage` / `hr:contract:manage` / `hr:analytics:view` / `hr:manage`，权限粒度粗，无法做到按功能授权。`[待修复]` |
| 缺少只读权限 | HR/Asset 各子模块 | P2 | 所有模块均只有 `manage` 和 `view` 两级，子功能缺少独立的 `:view` 权限，导致查看与编辑共用同一权限。`[待修复]` |
| 已注册权限无对应功能 | `hr:probation:manage` | P2 | 注册了试用期管理权限，但前端无独立的试用期管理页面和 API。`[待修复]` |
| 组件硬编码权限未注册 | `department:delete` | P2 | `OrgTreeNode.vue` 使用了未注册的权限码，功能权限控制失效。`[待修复]` |
| 角色授权粒度粗 | FINANCE_DIRECTOR / STORE_MANAGER 等 | P2 | 财务总监、店长等角色未被授予任何 HR/Asset/Device 菜单权限，与业务场景不符。`[待修复]` |
