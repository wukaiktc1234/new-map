# 采购链路权限矩阵

> 范围：采购申请、采购计划、采购订单、到货登记、采购结算及相关基础档案（商品分类、商品档案、供应商档案）
> 用途：为后续代码改造提供角色-页面-操作三级权限控制依据
> 生成日期：2026-07-30

---

## 1. 权限模型说明

### 1.1 访问级别定义

本项目权限矩阵采用 4 级访问控制：

| 级别 | 英文 | 含义 | 在采购链路中的表现 |
|-----|------|------|------------------|
| 完全访问 | `FULL` | 可查看、新增、编辑、删除、审批 | 可执行全部操作 |
| 只读访问 | `READ_ONLY` | 仅可查看，不可编辑 | 仅可查看列表、详情、报表 |
| 受限访问 | `LIMITED` | 仅可访问部分数据（如本门店/本部门/本人） | 可查看本范围数据，可提交申请，不可审批他人数据 |
| 隐藏 | `HIDDEN` | 完全不可见 | 菜单、按钮、路由均不显示 |

### 1.2 角色定义

| 角色代码 | 角色名称 | 数据范围特征 |
|---------|---------|------------|
| `owner` / `admin` | 老板 / 超级管理员 | 全部数据 |
| `ops_director` | 运营总监 | 全公司数据，重点监控 |
| `purchase_manager` | 采购部经理 | 采购域全部数据 |
| `finance_director` | 财务总监 | 财务相关数据 |
| `warehouse_manager` | 仓储部经理 | 仓储/到货相关数据 |
| `store_manager` | 店长 | 本门店数据 |
| `department_manager` | 部门经理 | 本部门数据 |
| `team_leader` | 组长 | 本组/本部门数据 |
| `employee` | 普通员工 | 仅本人数据 |

### 1.3 已注册的采购权限标识符

来源：`frontend/src/utils/permissions.ts`

| 权限标识符 | 说明 |
|-----------|------|
| `purchase:manage` | 管理采购相关功能（总开关） |
| `purchase:view` | 查看采购信息 |
| `purchase:overview:view` | 查看采购概览 |
| `purchase:plan:manage` | 管理采购计划 |
| `purchase:order:manage` | 管理采购订单 |
| `purchase:contract:manage` | 管理采购合同 |
| `purchase:stockin:manage` | 管理采购入库/到货 |
| `purchase:return:manage` | 管理采购退货 |
| `purchase:supplier:manage` | 管理供应商 |
| `purchase:statistics:view` | 查看采购统计 |

---

## 2. 菜单/页面级权限矩阵

| 页面（菜单） | owner / admin | ops_director | purchase_manager | finance_director | warehouse_manager | store_manager | department_manager | team_leader | employee |
|------------|---------------|--------------|------------------|------------------|-------------------|---------------|-------------------|-------------|----------|
| 采购管理（菜单组） | FULL | FULL | FULL | READ_ONLY | READ_ONLY | LIMITED | LIMITED | LIMITED | HIDDEN |
| 商品分类 | FULL | READ_ONLY | FULL | HIDDEN | HIDDEN | READ_ONLY | READ_ONLY | HIDDEN | HIDDEN |
| 商品档案 | FULL | READ_ONLY | FULL | HIDDEN | READ_ONLY | READ_ONLY | READ_ONLY | HIDDEN | HIDDEN |
| 供应商档案 | FULL | READ_ONLY | FULL | READ_ONLY | READ_ONLY | READ_ONLY | READ_ONLY | HIDDEN | HIDDEN |
| 物资需求提报 | FULL | FULL | FULL | HIDDEN | HIDDEN | LIMITED | LIMITED | LIMITED | LIMITED |
| 采购申请 | FULL | FULL | FULL | HIDDEN | HIDDEN | LIMITED | LIMITED | LIMITED | LIMITED |
| 采购计划 | FULL | FULL | FULL | HIDDEN | HIDDEN | LIMITED | LIMITED | LIMITED | HIDDEN |
| 采购订单 | FULL | FULL | FULL | READ_ONLY | READ_ONLY | LIMITED | LIMITED | LIMITED | HIDDEN |
| 到货登记 | FULL | READ_ONLY | READ_ONLY | HIDDEN | FULL | LIMITED | LIMITED | LIMITED | HIDDEN |
| 采购退货 | FULL | READ_ONLY | FULL | HIDDEN | FULL | LIMITED | HIDDEN | HIDDEN | HIDDEN |
| 采购合同 | FULL | READ_ONLY | FULL | READ_ONLY | HIDDEN | READ_ONLY | READ_ONLY | HIDDEN | HIDDEN |
| 电子合同 | FULL | READ_ONLY | FULL | READ_ONLY | HIDDEN | READ_ONLY | READ_ONLY | HIDDEN | HIDDEN |
| 签署链接 | FULL | READ_ONLY | FULL | HIDDEN | HIDDEN | READ_ONLY | READ_ONLY | HIDDEN | HIDDEN |
| 采购结算 | FULL | READ_ONLY | READ_ONLY | FULL | HIDDEN | READ_ONLY | HIDDEN | HIDDEN | HIDDEN |
| 采购报表 | FULL | FULL | FULL | FULL | READ_ONLY | READ_ONLY | READ_ONLY | HIDDEN | HIDDEN |
| 采购数据分析 | FULL | FULL | FULL | FULL | READ_ONLY | READ_ONLY | READ_ONLY | HIDDEN | HIDDEN |

### 菜单级权限现状

- `[待修复]` 当前 `purchaseMenu`（`frontend/src/modules/purchase/menu.ts`）未配置任何 `permissions` 或 `roles` 字段，所有登录用户均可看到全部采购子菜单。
- `[待修复]` 路由配置中未对采购路由设置 `meta.permissions` 或 `meta.roles`，前端路由守卫无法基于角色拦截。

---

## 3. 操作级权限矩阵

### 3.1 采购申请（PurchaseRequest.vue）

| 操作 | owner / admin | ops_director | purchase_manager | store_manager | department_manager | team_leader | employee |
|-----|---------------|--------------|------------------|---------------|-------------------|-------------|----------|
| 查看列表/详情 | FULL | FULL | FULL | LIMITED | LIMITED | LIMITED | LIMITED |
| 新增申请 | FULL | FULL | FULL | LIMITED | LIMITED | LIMITED | LIMITED |
| 编辑（草稿/拒绝） | FULL | FULL | FULL | LIMITED（本人/本部门） | LIMITED（本部门） | LIMITED（本组） | LIMITED（仅本人） |
| 提交审批 | FULL | FULL | FULL | LIMITED | LIMITED | LIMITED | LIMITED |
| 批量提交 | FULL | FULL | FULL | LIMITED | LIMITED | LIMITED | HIDDEN |
| 审批通过/拒绝 | FULL | FULL | FULL | HIDDEN | HIDDEN | HIDDEN | HIDDEN |
| 生成采购订单 | FULL | FULL | FULL | HIDDEN | HIDDEN | HIDDEN | HIDDEN |
| 删除 | FULL | FULL | FULL | LIMITED（本人/本部门） | LIMITED（本部门） | LIMITED（本组） | LIMITED（仅本人） |

### 3.2 采购计划（PurchasePlan.vue）

| 操作 | owner / admin | ops_director | purchase_manager | store_manager | department_manager | team_leader | employee |
|-----|---------------|--------------|------------------|---------------|-------------------|-------------|----------|
| 查看列表/详情 | FULL | FULL | FULL | LIMITED | LIMITED | LIMITED | HIDDEN |
| 新增计划 | FULL | FULL | FULL | HIDDEN | HIDDEN | HIDDEN | HIDDEN |
| 编辑（草稿） | FULL | FULL | FULL | HIDDEN | HIDDEN | HIDDEN | HIDDEN |
| 提交审批 | FULL | FULL | FULL | HIDDEN | HIDDEN | HIDDEN | HIDDEN |
| 删除（草稿/拒绝） | FULL | FULL | FULL | HIDDEN | HIDDEN | HIDDEN | HIDDEN |
| 审批通过/拒绝 | FULL | FULL | FULL | HIDDEN | HIDDEN | HIDDEN | HIDDEN |
| 生成计划 | FULL | FULL | FULL | HIDDEN | HIDDEN | HIDDEN | HIDDEN |
| 导出 | FULL | FULL | FULL | HIDDEN | HIDDEN | HIDDEN | HIDDEN |

### 3.3 采购订单（PurchaseOrder.vue）

| 操作 | owner / admin | ops_director | purchase_manager | warehouse_manager | store_manager | department_manager | team_leader | employee |
|-----|---------------|--------------|------------------|-------------------|---------------|-------------------|-------------|----------|
| 查看列表/详情 | FULL | FULL | FULL | READ_ONLY | LIMITED | LIMITED | LIMITED | HIDDEN |
| 新建订单 | FULL | FULL | FULL | HIDDEN | HIDDEN | HIDDEN | HIDDEN | HIDDEN |
| 从申请生成 | FULL | FULL | FULL | HIDDEN | HIDDEN | HIDDEN | HIDDEN | HIDDEN |
| 编辑（草稿） | FULL | FULL | FULL | HIDDEN | HIDDEN | HIDDEN | HIDDEN | HIDDEN |
| 提交审批 | FULL | FULL | FULL | HIDDEN | HIDDEN | HIDDEN | HIDDEN | HIDDEN |
| 审批通过/驳回 | FULL | FULL | FULL | HIDDEN | HIDDEN | HIDDEN | HIDDEN | HIDDEN |
| 确认下单 | FULL | FULL | FULL | HIDDEN | HIDDEN | HIDDEN | HIDDEN | HIDDEN |
| 收货（跳转到货） | FULL | FULL | FULL | FULL | LIMITED | HIDDEN | HIDDEN | HIDDEN |
| 链路追溯 | FULL | FULL | FULL | READ_ONLY | READ_ONLY | READ_ONLY | READ_ONLY | HIDDEN |
| 删除（草稿） | FULL | FULL | FULL | HIDDEN | HIDDEN | HIDDEN | HIDDEN | HIDDEN |
| 终止订单 | FULL | FULL | FULL | HIDDEN | HIDDEN | HIDDEN | HIDDEN | HIDDEN |

### 3.4 到货登记（PurchaseStockin.vue）

| 操作 | owner / admin | ops_director | purchase_manager | warehouse_manager | store_manager | team_leader | employee |
|-----|---------------|--------------|------------------|-------------------|---------------|-------------|----------|
| 查看列表/详情 | FULL | READ_ONLY | READ_ONLY | FULL | LIMITED | LIMITED | HIDDEN |
| 新增到货单 | FULL | FULL | FULL | FULL | LIMITED | HIDDEN | HIDDEN |
| 编辑物流 | FULL | FULL | FULL | FULL | LIMITED（本人） | HIDDEN | HIDDEN |
| 关闭到货单 | FULL | FULL | FULL | FULL | HIDDEN | HIDDEN | HIDDEN |

### 3.5 采购结算（PurchaseSettlement.vue）

| 操作 | owner / admin | ops_director | purchase_manager | finance_director | store_manager | team_leader | employee |
|-----|---------------|--------------|------------------|------------------|---------------|-------------|----------|
| 查看列表/详情 | FULL | READ_ONLY | READ_ONLY | FULL | READ_ONLY | HIDDEN | HIDDEN |
| 新增结算 | FULL | FULL | FULL | FULL | HIDDEN | HIDDEN | HIDDEN |
| 编辑结算 | FULL | FULL | FULL | FULL | HIDDEN | HIDDEN | HIDDEN |
| 确认付款 | FULL | FULL | READ_ONLY | FULL | HIDDEN | HIDDEN | HIDDEN |
| 标记完成 | FULL | FULL | READ_ONLY | FULL | HIDDEN | HIDDEN | HIDDEN |
| 批量结算 | FULL | FULL | READ_ONLY | FULL | HIDDEN | HIDDEN | HIDDEN |
| 查看付款记录 | FULL | READ_ONLY | READ_ONLY | FULL | READ_ONLY | HIDDEN | HIDDEN |

### 3.6 基础档案

| 页面 | 操作 | owner / admin | ops_director | purchase_manager | finance_director | warehouse_manager | store_manager | department_manager |
|-----|-----|---------------|--------------|------------------|------------------|-------------------|---------------|-------------------|
| 商品分类 | 查看 | FULL | READ_ONLY | FULL | HIDDEN | HIDDEN | READ_ONLY | READ_ONLY |
| 商品分类 | 新增/编辑/删除 | FULL | HIDDEN | FULL | HIDDEN | HIDDEN | HIDDEN | HIDDEN |
| 商品档案 | 查看 | FULL | READ_ONLY | FULL | HIDDEN | READ_ONLY | READ_ONLY | READ_ONLY |
| 商品档案 | 新增/编辑/删除 | FULL | HIDDEN | FULL | HIDDEN | HIDDEN | HIDDEN | HIDDEN |
| 供应商档案 | 查看 | FULL | READ_ONLY | FULL | READ_ONLY | READ_ONLY | READ_ONLY | READ_ONLY |
| 供应商档案 | 新增/编辑/删除 | FULL | HIDDEN | FULL | HIDDEN | HIDDEN | HIDDEN | HIDDEN |

---

## 4. 权限标识符与操作的映射建议

| 操作场景 | 建议权限标识符 | 备注 |
|---------|--------------|------|
| 进入采购管理菜单 | `purchase:view` 或 `purchase:manage` | 作为菜单显示的最小权限 |
| 新增/编辑采购申请 | `purchase:request:create` / `purchase:request:update` | 当前未注册，建议补充 |
| 审批采购申请 | `purchase:request:approve` | 当前未注册，建议补充 |
| 新增/编辑采购计划 | `purchase:plan:manage` | 已存在 |
| 新增/编辑采购订单 | `purchase:order:manage` | 已存在 |
| 采购订单审批 | `purchase:order:approve` | 当前未注册，建议补充 |
| 到货登记操作 | `purchase:stockin:manage` | 已存在 |
| 采购结算操作 | `purchase:settlement:manage` | 当前未注册，建议补充 |
| 查看采购报表 | `purchase:statistics:view` | 已存在 |
| 供应商管理 | `purchase:supplier:manage` | 已存在 |

---

## 5. 当前代码权限控制现状

### 5.1 页面级权限检查

| 页面 | 当前控制方式 | 是否使用权限指令 | 结论 |
|-----|------------|----------------|------|
| PurchaseRequest.vue | 仅按状态显示按钮 | 否 | `[待修复]` |
| PurchasePlan.vue | 仅按状态显示按钮 | 否 | `[待修复]` |
| PurchaseOrder.vue | 仅按状态显示按钮 | 否 | `[待修复]` |
| PurchaseStockin.vue | 仅按状态显示按钮 | 否 | `[待修复]` |
| PurchaseSettlement.vue | 仅按状态显示按钮 | 否 | `[待修复]` |

### 5.2 已存在的权限工具

- `usePermission.ts` 已提供 `canManagePurchase`：`isAdmin || hasPermission('purchase:manage')`。
- `usePermission.ts` 已提供 `hasPermission`、`hasAnyPermission`、`hasRole`、`hasAnyRole`。
- 项目规则要求按钮级权限使用 `v-permission` 指令。

### 5.3 主要问题

1. `[待修复]` 所有采购页面均未使用 `v-permission`、`hasPermission` 或 `hasRole` 进行权限控制，任何登录用户均可看到全部操作按钮（仅受单据状态限制）。
2. `[待修复]` 缺少细粒度权限标识符，如 `purchase:request:approve`、`purchase:order:approve`、`purchase:settlement:manage` 等。
3. `[待修复]` `purchaseMenu` 未配置 `permissions`，侧边栏无法按角色隐藏子菜单。
4. `[待修复]` 采购路由未配置 `meta.permissions`/`meta.roles`，路由守卫无法拦截越权访问。
5. `[待修复]` 数据范围控制（本门店/本部门/本人）当前仅通过 `mineOnly` _checkbox 实现，未与后端 `dataScope` 联动。

---

## 6. 改造建议

### 6.1 短期（最小改动）

1. 在 `frontend/src/utils/permissions.ts` 补充采购细粒度权限：
   - `purchase:request:create/update/delete/approve`
   - `purchase:plan:create/update/delete/approve`
   - `purchase:order:create/update/delete/approve`
   - `purchase:stockin:create/update/close`
   - `purchase:settlement:create/update/pay/complete`
2. 在采购页面头部按钮和操作列按钮上增加 `v-permission` 指令，区分查看/编辑/审批/删除等权限。
3. 为 `purchaseMenu` 增加 `permissions` 字段，至少区分 `purchase:manage` 和 `purchase:view`。

### 6.2 中期

1. 采购路由统一增加 `meta: { permissions: [...], roles: [...] }`，配合路由守卫实现越权拦截。
2. 将 `mineOnly` 逻辑改为基于后端返回的 `dataScope` 自动过滤，减少用户手动勾选。
3. 审批类操作统一走 `approvalWorkflowApi`，并在审批节点配置中绑定权限标识符。

### 6.3 长期

1. 建立采购域独立的 RBAC 权限模板（参考 `useDomainPermission.ts` 中的 `businessDomains`），支持在「系统管理-权限模板」中动态配置。
2. 将数据范围（ALL / COMPANY / DEPARTMENT / STORE / SELF）与采购单据的 `storeId` / `departmentId` / `createBy` 字段联动，实现行级数据隔离。
