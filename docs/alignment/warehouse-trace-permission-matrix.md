# 仓储/溯源链路权限矩阵

> 范围：仓储管理（库存概览、入库、出库、调拨、盘点、报损、调整、预警、补货、报表、库位）与溯源管理（追溯查询、原料追溯码、食品追溯码、临期预警、召回、质量记录、检验记录、标签模板、供应商追溯）
> 用途：梳理角色-页面-操作三级权限控制现状，为后续权限收口、按钮级 `v-permission` 改造及路由守卫增强提供依据
> 生成日期：2026-07-30

---

## 1. 权限模型说明

### 1.1 访问级别定义

本项目权限矩阵采用 4 级访问控制：

| 级别 | 英文 | 含义 | 在仓储/溯源链路中的表现 |
|-----|------|------|----------------------|
| 完全访问 | `FULL` | 可查看、新增、编辑、删除、审批、执行 | 可执行全部操作 |
| 只读访问 | `READ_ONLY` | 仅可查看，不可编辑 | 仅可查看列表、详情、报表、追溯链 |
| 受限访问 | `LIMITED` | 仅可访问部分数据（如本门店/本部门/本人） | 可查看本范围数据，可提交本人单据，不可审批他人数据 |
| 隐藏 | `HIDDEN` | 完全不可见 | 菜单、按钮、路由均不显示 |

### 1.2 角色定义

| 角色代码 | 角色名称 | 数据范围特征 |
|---------|---------|------------|
| `owner` / `admin` | 老板 / 超级管理员 | 全部数据 |
| `ops_director` | 运营总监 | 全公司数据，重点监控 |
| `warehouse_manager` | 仓储部经理 | 仓储/库存相关数据 |
| `finance_director` | 财务总监 | 财务相关数据 |
| `store_manager` | 店长 | 本门店数据 |
| `department_manager` | 部门经理 | 本部门数据 |
| `team_leader` | 组长 | 本组/本部门数据 |
| `employee` | 普通员工 | 仅本人数据 |

### 1.3 已注册的权限标识符

#### 1.3.1 仓储管理权限（来源：`frontend/src/utils/permissions.ts`）

| 权限标识符 | 说明 |
|-----------|------|
| `warehouse:manage` | 管理仓储相关功能（总开关） |
| `warehouse:view` | 查看仓储信息 |
| `warehouse:overview:view` | 查看库存概览 |
| `warehouse:detail:view` | 查看库存明细 |
| `warehouse:warning:manage` | 管理库存预警 |
| `warehouse:consumption:manage` | 管理库存消耗 |
| `warehouse:loss:manage` | 管理库存报损 |
| `warehouse:check:manage` | 管理库存盘点 |
| `warehouse:transfer:manage` | 管理库存调拨 |
| `warehouse:log:view` | 查看库存日志 |
| `warehouse:settings:manage` | 管理仓库设置 |
| `warehouse:store:manage` | 管理门店库存 |

#### 1.3.2 溯源管理权限

| 权限标识符 | 说明 |
|-----------|------|
| — | `[待修复]` `frontend/src/utils/permissions.ts` 中未注册任何溯源管理权限标识符 |

---

## 2. 菜单/页面级权限矩阵

### 2.1 仓储管理菜单

来源：`frontend/src/modules/warehouse/menu.ts`

| 页面（菜单） | owner / admin | ops_director | warehouse_manager | finance_director | store_manager | department_manager | team_leader | employee |
|------------|---------------|--------------|-------------------|------------------|---------------|-------------------|-------------|----------|
| 仓储管理（菜单组） | FULL | FULL | FULL | READ_ONLY | LIMITED | LIMITED | LIMITED | HIDDEN |
| 库存概览 | FULL | FULL | FULL | READ_ONLY | LIMITED | READ_ONLY | READ_ONLY | HIDDEN |
| 库存管理 | FULL | READ_ONLY | FULL | HIDDEN | LIMITED | READ_ONLY | READ_ONLY | HIDDEN |
| 库存入库 | FULL | READ_ONLY | FULL | HIDDEN | LIMITED | HIDDEN | HIDDEN | HIDDEN |
| 库存出库 | FULL | FULL | FULL | HIDDEN | LIMITED | LIMITED | LIMITED | HIDDEN |
| 库存调拨 | FULL | FULL | FULL | HIDDEN | LIMITED | LIMITED | HIDDEN | HIDDEN |
| 库存调整 | FULL | FULL | FULL | READ_ONLY | LIMITED | LIMITED | HIDDEN | HIDDEN |
| 库存报损 | FULL | READ_ONLY | FULL | HIDDEN | LIMITED | READ_ONLY | READ_ONLY | HIDDEN |
| 库存盘点 | FULL | READ_ONLY | FULL | HIDDEN | LIMITED | READ_ONLY | READ_ONLY | HIDDEN |
| 库存预警 | FULL | FULL | FULL | READ_ONLY | LIMITED | READ_ONLY | READ_ONLY | HIDDEN |
| 库位管理 | FULL | READ_ONLY | FULL | HIDDEN | READ_ONLY | READ_ONLY | HIDDEN | HIDDEN |
| 门店库存查看 | FULL | READ_ONLY | READ_ONLY | HIDDEN | FULL | LIMITED | LIMITED | HIDDEN |
| 库存报表 | FULL | FULL | FULL | FULL | READ_ONLY | READ_ONLY | READ_ONLY | HIDDEN |
| 智能补货建议 | FULL | FULL | FULL | HIDDEN | LIMITED | HIDDEN | HIDDEN | HIDDEN |

### 2.2 溯源管理菜单

来源：`frontend/src/modules/traceability/menu.ts`

| 页面（菜单） | owner / admin | ops_director | warehouse_manager | finance_director | store_manager | department_manager | team_leader | employee |
|------------|---------------|--------------|-------------------|------------------|---------------|-------------------|-------------|----------|
| 食品追溯（菜单组） | FULL | FULL | READ_ONLY | READ_ONLY | LIMITED | LIMITED | READ_ONLY | HIDDEN |
| 临期预警 | FULL | FULL | FULL | READ_ONLY | FULL | LIMITED | READ_ONLY | HIDDEN |
| 追溯查询 | FULL | FULL | READ_ONLY | READ_ONLY | READ_ONLY | READ_ONLY | READ_ONLY | HIDDEN |
| 原料追溯 | FULL | FULL | FULL | READ_ONLY | LIMITED | READ_ONLY | READ_ONLY | HIDDEN |
| 食品追溯 | FULL | FULL | READ_ONLY | HIDDEN | LIMITED | HIDDEN | HIDDEN | HIDDEN |
| 追溯链展示 | FULL | FULL | READ_ONLY | READ_ONLY | READ_ONLY | READ_ONLY | READ_ONLY | HIDDEN |
| 供应商追溯 | FULL | FULL | READ_ONLY | READ_ONLY | READ_ONLY | READ_ONLY | READ_ONLY | HIDDEN |
| 检验记录 | FULL | FULL | FULL | READ_ONLY | LIMITED | READ_ONLY | READ_ONLY | HIDDEN |
| 标签模板 | FULL | FULL | READ_ONLY | HIDDEN | HIDDEN | HIDDEN | HIDDEN | HIDDEN |
| 召回管理 | FULL | FULL | HIDDEN | HIDDEN | HIDDEN | HIDDEN | HIDDEN | HIDDEN |
| 质量追溯 | FULL | FULL | HIDDEN | HIDDEN | HIDDEN | HIDDEN | HIDDEN | HIDDEN |

### 2.3 菜单级权限现状

- `[待修复]` 当前 `warehouseMenu`（`frontend/src/modules/warehouse/menu.ts`）未配置任何 `permissions` 或 `roles` 字段，所有登录用户均可看到全部仓储子菜单。
- `[待修复]` 当前 `traceabilityMenu`（`frontend/src/modules/traceability/menu.ts`）未配置任何 `permissions` 或 `roles` 字段，所有登录用户均可看到全部溯源子菜单。
- `[待修复]` 路由配置中未对仓储/溯源路由设置 `meta.permissions` 或 `meta.roles`，前端路由守卫无法基于角色或权限标识符拦截。

---

## 3. 操作级权限矩阵

### 3.1 库存概览 `WarehouseOverview.vue`

| 操作 | owner / admin | ops_director | warehouse_manager | store_manager | department_manager | team_leader | employee |
|-----|---------------|--------------|-------------------|---------------|-------------------|-------------|----------|
| 查看统计卡片/图表 | FULL | FULL | FULL | LIMITED | READ_ONLY | READ_ONLY | HIDDEN |
| 切换仓库/时间范围 | FULL | FULL | FULL | LIMITED | READ_ONLY | READ_ONLY | HIDDEN |
| 查看 TOP10 预警 | FULL | FULL | FULL | LIMITED | READ_ONLY | READ_ONLY | HIDDEN |
| 快捷跳转出库/调拨/盘点 | FULL | FULL | FULL | LIMITED | READ_ONLY | READ_ONLY | HIDDEN |

### 3.2 库存管理 `Inventory.vue`

| 操作 | owner / admin | ops_director | warehouse_manager | store_manager | department_manager | team_leader | employee |
|-----|---------------|--------------|-------------------|---------------|-------------------|-------------|----------|
| 查看列表/详情 | FULL | READ_ONLY | FULL | LIMITED | READ_ONLY | READ_ONLY | HIDDEN |
| 锁定库存 | FULL | HIDDEN | FULL | HIDDEN | HIDDEN | HIDDEN | HIDDEN |
| 解锁库存 | FULL | HIDDEN | FULL | HIDDEN | HIDDEN | HIDDEN | HIDDEN |
| 扣减库存 | FULL | HIDDEN | FULL | HIDDEN | HIDDEN | HIDDEN | HIDDEN |
| 增加库存 | FULL | HIDDEN | FULL | HIDDEN | HIDDEN | HIDDEN | HIDDEN |

### 3.3 库存入库 `InventoryStockin.vue`

| 操作 | owner / admin | ops_director | warehouse_manager | store_manager | department_manager | team_leader | employee |
|-----|---------------|--------------|-------------------|---------------|-------------------|-------------|----------|
| 查看待收货/已确认列表 | FULL | READ_ONLY | FULL | LIMITED | HIDDEN | HIDDEN | HIDDEN |
| 确认收货 | FULL | HIDDEN | FULL | LIMITED | HIDDEN | HIDDEN | HIDDEN |
| 查看到货详情 | FULL | READ_ONLY | FULL | LIMITED | HIDDEN | HIDDEN | HIDDEN |

### 3.4 库存出库 `InventoryOutbound.vue`

| 操作 | owner / admin | ops_director | warehouse_manager | store_manager | department_manager | team_leader | employee |
|-----|---------------|--------------|-------------------|---------------|-------------------|-------------|----------|
| 查看列表/详情 | FULL | FULL | FULL | LIMITED | LIMITED | LIMITED | HIDDEN |
| 新建出库单 | FULL | FULL | FULL | LIMITED | LIMITED | HIDDEN | HIDDEN |
| 快捷扫码出库 | FULL | FULL | FULL | LIMITED | LIMITED | HIDDEN | HIDDEN |
| 审批出库单 | FULL | FULL | FULL | HIDDEN | HIDDEN | HIDDEN | HIDDEN |
| 执行出库 | FULL | FULL | FULL | HIDDEN | HIDDEN | HIDDEN | HIDDEN |

### 3.5 库存调拨 `InventoryTransfer.vue`

| 操作 | owner / admin | ops_director | warehouse_manager | store_manager | department_manager | team_leader | employee |
|-----|---------------|--------------|-------------------|---------------|-------------------|-------------|----------|
| 查看列表/详情 | FULL | FULL | FULL | LIMITED | LIMITED | HIDDEN | HIDDEN |
| 新建调拨单 | FULL | FULL | FULL | LIMITED | HIDDEN | HIDDEN | HIDDEN |
| 审批调拨单 | FULL | FULL | FULL | HIDDEN | HIDDEN | HIDDEN | HIDDEN |
| 确认收货 | FULL | FULL | FULL | LIMITED | HIDDEN | HIDDEN | HIDDEN |

### 3.6 库存盘点 `InventoryCheck.vue`

| 操作 | owner / admin | ops_director | warehouse_manager | store_manager | department_manager | team_leader | employee |
|-----|---------------|--------------|-------------------|---------------|-------------------|-------------|----------|
| 查看盘点记录/计划 | FULL | READ_ONLY | FULL | LIMITED | READ_ONLY | READ_ONLY | HIDDEN |
| 新建盘点单 | FULL | HIDDEN | FULL | LIMITED | HIDDEN | HIDDEN | HIDDEN |
| 审核盘点单 | FULL | HIDDEN | FULL | HIDDEN | HIDDEN | HIDDEN | HIDDEN |
| 生成调整单 | FULL | HIDDEN | FULL | HIDDEN | HIDDEN | HIDDEN | HIDDEN |
| 新建盘点计划 | FULL | HIDDEN | FULL | HIDDEN | HIDDEN | HIDDEN | HIDDEN |
| 编辑盘点计划 | FULL | HIDDEN | FULL | HIDDEN | HIDDEN | HIDDEN | HIDDEN |
| 启用/停用计划 | FULL | HIDDEN | FULL | HIDDEN | HIDDEN | HIDDEN | HIDDEN |
| 立即执行计划 | FULL | HIDDEN | FULL | HIDDEN | HIDDEN | HIDDEN | HIDDEN |

### 3.7 库存报损 `InventoryLoss.vue`

| 操作 | owner / admin | ops_director | warehouse_manager | store_manager | department_manager | team_leader | employee |
|-----|---------------|--------------|-------------------|---------------|-------------------|-------------|----------|
| 查看列表/详情 | FULL | READ_ONLY | FULL | LIMITED | READ_ONLY | READ_ONLY | HIDDEN |
| 新建报损单 | FULL | HIDDEN | FULL | LIMITED | READ_ONLY | HIDDEN | HIDDEN |
| 审批报损单 | FULL | HIDDEN | FULL | HIDDEN | HIDDEN | HIDDEN | HIDDEN |
| 处理报损单 | FULL | HIDDEN | FULL | HIDDEN | HIDDEN | HIDDEN | HIDDEN |

### 3.8 库存调整 `InventoryAdjust.vue`

| 操作 | owner / admin | ops_director | warehouse_manager | store_manager | department_manager | team_leader | employee |
|-----|---------------|--------------|-------------------|---------------|-------------------|-------------|----------|
| 查看列表/详情 | FULL | FULL | FULL | LIMITED | LIMITED | HIDDEN | HIDDEN |
| 新建调整单 | FULL | FULL | FULL | LIMITED | HIDDEN | HIDDEN | HIDDEN |
| 审批调整单 | FULL | FULL | FULL | HIDDEN | HIDDEN | HIDDEN | HIDDEN |
| 执行调整 | FULL | FULL | FULL | HIDDEN | HIDDEN | HIDDEN | HIDDEN |

### 3.9 库存预警 `InventoryWarning.vue` / 智能补货 `SmartRestock.vue`

| 操作 | owner / admin | ops_director | warehouse_manager | store_manager | department_manager | team_leader | employee |
|-----|---------------|--------------|-------------------|---------------|-------------------|-------------|----------|
| 查看预警记录 | FULL | FULL | FULL | LIMITED | READ_ONLY | READ_ONLY | HIDDEN |
| 处理预警 | FULL | FULL | FULL | LIMITED | HIDDEN | HIDDEN | HIDDEN |
| 手动生成预警 | FULL | FULL | FULL | HIDDEN | HIDDEN | HIDDEN | HIDDEN |
| 跳转智能补货 | FULL | FULL | FULL | LIMITED | HIDDEN | HIDDEN | HIDDEN |
| 查看采购建议 | FULL | FULL | FULL | LIMITED | HIDDEN | HIDDEN | HIDDEN |
| 提交采购建议 | FULL | FULL | FULL | HIDDEN | HIDDEN | HIDDEN | HIDDEN |

### 3.10 库位管理 `InventoryLocation.vue`

| 操作 | owner / admin | ops_director | warehouse_manager | store_manager | department_manager | team_leader | employee |
|-----|---------------|--------------|-------------------|---------------|-------------------|-------------|----------|
| 查看库位列表 | FULL | READ_ONLY | FULL | READ_ONLY | READ_ONLY | HIDDEN | HIDDEN |
| 新建库位 | FULL | HIDDEN | FULL | HIDDEN | HIDDEN | HIDDEN | HIDDEN |
| 编辑库位 | FULL | HIDDEN | FULL | HIDDEN | HIDDEN | HIDDEN | HIDDEN |
| 启用/停用库位 | FULL | HIDDEN | FULL | HIDDEN | HIDDEN | HIDDEN | HIDDEN |
| 批量启用/停用 | FULL | HIDDEN | FULL | HIDDEN | HIDDEN | HIDDEN | HIDDEN |

### 3.11 门店库存查看 `StoreInventory.vue`

| 操作 | owner / admin | ops_director | warehouse_manager | store_manager | department_manager | team_leader | employee |
|-----|---------------|--------------|-------------------|---------------|-------------------|-------------|----------|
| 查看本门店库存 | FULL | READ_ONLY | READ_ONLY | FULL | LIMITED | LIMITED | HIDDEN |
| 库存调整 | FULL | HIDDEN | HIDDEN | FULL | LIMITED | HIDDEN | HIDDEN |
| 批量调拨 | FULL | HIDDEN | HIDDEN | FULL | HIDDEN | HIDDEN | HIDDEN |
| 批量报损 | FULL | HIDDEN | HIDDEN | FULL | HIDDEN | HIDDEN | HIDDEN |

### 3.12 库存报表 `InventoryReport.vue`

| 操作 | owner / admin | ops_director | warehouse_manager | finance_director | store_manager | department_manager | team_leader | employee |
|-----|---------------|--------------|-------------------|------------------|---------------|-------------------|-------------|----------|
| 查看趋势/分类/成本报表 | FULL | FULL | FULL | FULL | READ_ONLY | READ_ONLY | HIDDEN | HIDDEN |
| 导出报表 | FULL | FULL | FULL | FULL | HIDDEN | HIDDEN | HIDDEN | HIDDEN |

### 3.13 追溯查询 `TraceQuery.vue` / 追溯链 `TraceChainView.vue`

| 操作 | owner / admin | ops_director | warehouse_manager | store_manager | department_manager | team_leader | employee |
|-----|---------------|--------------|-------------------|---------------|-------------------|-------------|----------|
| 扫码/输入追溯码查询 | FULL | FULL | READ_ONLY | READ_ONLY | READ_ONLY | READ_ONLY | HIDDEN |
| 查看追溯链 | FULL | FULL | READ_ONLY | READ_ONLY | READ_ONLY | READ_ONLY | HIDDEN |
| 查看节点详情 | FULL | FULL | READ_ONLY | READ_ONLY | READ_ONLY | READ_ONLY | HIDDEN |

### 3.14 原料追溯码 `MaterialTraceCode.vue`

| 操作 | owner / admin | ops_director | warehouse_manager | store_manager | department_manager | team_leader | employee |
|-----|---------------|--------------|-------------------|---------------|-------------------|-------------|----------|
| 查看原料追溯码 | FULL | FULL | FULL | LIMITED | READ_ONLY | READ_ONLY | HIDDEN |
| 批量生成 | FULL | FULL | FULL | LIMITED | HIDDEN | HIDDEN | HIDDEN |
| 单条打印 | FULL | FULL | FULL | LIMITED | READ_ONLY | HIDDEN | HIDDEN |
| 批量打印 | FULL | FULL | FULL | LIMITED | HIDDEN | HIDDEN | HIDDEN |
| 导出 | FULL | FULL | FULL | LIMITED | HIDDEN | HIDDEN | HIDDEN |
| 删除 | FULL | FULL | FULL | HIDDEN | HIDDEN | HIDDEN | HIDDEN |

### 3.15 食品追溯码 `FoodTraceCode.vue`

| 操作 | owner / admin | ops_director | warehouse_manager | store_manager | department_manager | team_leader | employee |
|-----|---------------|--------------|-------------------|---------------|-------------------|-------------|----------|
| 查看食品追溯码 | FULL | FULL | READ_ONLY | LIMITED | READ_ONLY | HIDDEN | HIDDEN |
| 生成追溯码 | FULL | FULL | HIDDEN | LIMITED | HIDDEN | HIDDEN | HIDDEN |
| 打印标签 | FULL | FULL | READ_ONLY | LIMITED | HIDDEN | HIDDEN | HIDDEN |
| 批量打印 | FULL | FULL | READ_ONLY | LIMITED | HIDDEN | HIDDEN | HIDDEN |
| 导出 | FULL | FULL | READ_ONLY | LIMITED | HIDDEN | HIDDEN | HIDDEN |
| 删除 | FULL | FULL | HIDDEN | HIDDEN | HIDDEN | HIDDEN | HIDDEN |

### 3.16 临期预警 `ExpiryWarning.vue`

| 操作 | owner / admin | ops_director | warehouse_manager | store_manager | department_manager | team_leader | employee |
|-----|---------------|--------------|-------------------|---------------|-------------------|-------------|----------|
| 查看临期/过期列表 | FULL | FULL | FULL | FULL | LIMITED | READ_ONLY | HIDDEN |
| 报损处理 | FULL | FULL | FULL | FULL | LIMITED | HIDDEN | HIDDEN |
| 退货处理 | FULL | FULL | FULL | LIMITED | HIDDEN | HIDDEN | HIDDEN |
| 跳转补货 | FULL | FULL | FULL | FULL | LIMITED | HIDDEN | HIDDEN |

### 3.17 召回管理 `RecallManagement.vue`

| 操作 | owner / admin | ops_director | warehouse_manager | store_manager | department_manager | team_leader | employee |
|-----|---------------|--------------|-------------------|---------------|-------------------|-------------|----------|
| 分析影响范围 | FULL | FULL | HIDDEN | HIDDEN | HIDDEN | HIDDEN | HIDDEN |
| 执行召回 | FULL | FULL | HIDDEN | HIDDEN | HIDDEN | HIDDEN | HIDDEN |
| 查看召回结果 | FULL | FULL | HIDDEN | HIDDEN | HIDDEN | HIDDEN | HIDDEN |

### 3.18 质量追溯 `TraceabilityQuality.vue`

| 操作 | owner / admin | ops_director | warehouse_manager | store_manager | department_manager | team_leader | employee |
|-----|---------------|--------------|-------------------|---------------|-------------------|-------------|----------|
| 查看质量记录 | FULL | FULL | HIDDEN | HIDDEN | HIDDEN | HIDDEN | HIDDEN |
| 新增/编辑质量记录 | FULL | FULL | HIDDEN | HIDDEN | HIDDEN | HIDDEN | HIDDEN |
| 删除质量记录 | FULL | FULL | HIDDEN | HIDDEN | HIDDEN | HIDDEN | HIDDEN |
| 处理异常 | FULL | FULL | HIDDEN | HIDDEN | HIDDEN | HIDDEN | HIDDEN |
| 导入/导出 | FULL | FULL | HIDDEN | HIDDEN | HIDDEN | HIDDEN | HIDDEN |

### 3.19 检验记录 `TraceabilityInspection.vue`

| 操作 | owner / admin | ops_director | warehouse_manager | store_manager | department_manager | team_leader | employee |
|-----|---------------|--------------|-------------------|---------------|-------------------|-------------|----------|
| 查看检验记录 | FULL | FULL | FULL | LIMITED | READ_ONLY | READ_ONLY | HIDDEN |
| 新增/编辑检验记录 | FULL | FULL | FULL | LIMITED | HIDDEN | HIDDEN | HIDDEN |
| 删除检验记录 | FULL | FULL | FULL | HIDDEN | HIDDEN | HIDDEN | HIDDEN |
| 上传检验报告 | FULL | FULL | FULL | LIMITED | HIDDEN | HIDDEN | HIDDEN |
| 导出 | FULL | FULL | FULL | LIMITED | HIDDEN | HIDDEN | HIDDEN |

### 3.20 标签模板 `LabelTemplate.vue`

| 操作 | owner / admin | ops_director | warehouse_manager | store_manager | department_manager | team_leader | employee |
|-----|---------------|--------------|-------------------|---------------|-------------------|-------------|----------|
| 查看标签模板 | FULL | FULL | READ_ONLY | HIDDEN | HIDDEN | HIDDEN | HIDDEN |
| 新增模板 | FULL | FULL | HIDDEN | HIDDEN | HIDDEN | HIDDEN | HIDDEN |
| 编辑模板 | FULL | FULL | HIDDEN | HIDDEN | HIDDEN | HIDDEN | HIDDEN |
| 预览打印 | FULL | FULL | READ_ONLY | HIDDEN | HIDDEN | HIDDEN | HIDDEN |
| 设为默认 | FULL | FULL | HIDDEN | HIDDEN | HIDDEN | HIDDEN | HIDDEN |
| 删除模板 | FULL | FULL | HIDDEN | HIDDEN | HIDDEN | HIDDEN | HIDDEN |

### 3.21 供应商追溯 `SupplierTrace.vue`

| 操作 | owner / admin | ops_director | warehouse_manager | store_manager | department_manager | team_leader | employee |
|-----|---------------|--------------|-------------------|---------------|-------------------|-------------|----------|
| 查询供应商追溯 | FULL | FULL | READ_ONLY | READ_ONLY | READ_ONLY | READ_ONLY | HIDDEN |
| 查看批次/质检/召回/统计 | FULL | FULL | READ_ONLY | READ_ONLY | READ_ONLY | READ_ONLY | HIDDEN |

---

## 4. 权限标识符与操作的映射建议

### 4.1 仓储管理建议补充的权限标识符

| 操作场景 | 建议权限标识符 | 备注 |
|---------|--------------|------|
| 进入仓储管理菜单 | `warehouse:view` 或 `warehouse:manage` | 作为菜单显示的最小权限 |
| 库存出库新建/编辑 | `warehouse:outbound:create` / `warehouse:outbound:update` | 当前未注册，建议补充 |
| 库存出库审批/执行 | `warehouse:outbound:approve` / `warehouse:outbound:execute` | 当前未注册，建议补充 |
| 库存调拨新建/编辑 | `warehouse:transfer:create` / `warehouse:transfer:update` | 当前未注册，建议补充 |
| 库存调拨审批/收货 | `warehouse:transfer:approve` / `warehouse:transfer:receive` | 当前未注册，建议补充 |
| 库存盘点新建/审核 | `warehouse:check:create` / `warehouse:check:approve` | 当前未注册，建议补充 |
| 库存盘点计划管理 | `warehouse:check:plan:manage` | 当前未注册，建议补充 |
| 库存报损新建/处理 | `warehouse:loss:create` / `warehouse:loss:process` | 当前未注册，建议补充 |
| 库存调整新建/执行 | `warehouse:adjust:create` / `warehouse:adjust:execute` | 当前未注册，建议补充 |
| 库存预警处理 | `warehouse:warning:handle` | 当前仅 `warehouse:warning:manage`，建议细化 |
| 库位新建/编辑/启用停用 | `warehouse:location:manage` | 当前未注册，建议补充 |
| 智能补货提交 | `warehouse:consumption:manage` | 已存在，但当前页面未绑定 |
| 库存报表导出 | `warehouse:report:export` | 当前未注册，建议补充 |

### 4.2 溯源管理建议新增的权限标识符

| 操作场景 | 建议权限标识符 | 备注 |
|---------|--------------|------|
| 进入溯源管理菜单 | `traceability:view` 或 `traceability:manage` | `[待修复]` 当前完全缺失 |
| 原料追溯码生成 | `traceability:material:generate` | `[待修复]` 当前完全缺失 |
| 原料追溯码打印/导出/删除 | `traceability:material:print` / `export` / `delete` | `[待修复]` 当前完全缺失 |
| 食品追溯码生成 | `traceability:food:generate` | `[待修复]` 当前完全缺失 |
| 食品追溯码打印/导出/删除 | `traceability:food:print` / `export` / `delete` | `[待修复]` 当前完全缺失 |
| 临期预警处理 | `traceability:expiry:handle` | `[待修复]` 当前完全缺失 |
| 召回分析与执行 | `traceability:recall:analyze` / `traceability:recall:execute` | `[待修复]` 当前完全缺失 |
| 质量记录增删改 | `traceability:quality:manage` | `[待修复]` 当前完全缺失 |
| 检验记录增删改 | `traceability:inspection:manage` | `[待修复]` 当前完全缺失 |
| 标签模板管理 | `traceability:label:manage` | `[待修复]` 当前完全缺失 |
| 供应商追溯查看 | `traceability:supplier:view` | `[待修复]` 当前完全缺失 |

---

## 5. 当前代码权限控制现状

### 5.1 页面级权限检查

| 页面 | 当前控制方式 | 是否使用权限指令 | 结论 |
|-----|------------|----------------|------|
| WarehouseOverview.vue | 无 | 否 | `[待修复]` |
| Inventory.vue | 无 | 否 | `[待修复]` |
| InventoryStockin.vue | 无 | 否 | `[待修复]` |
| InventoryOutbound.vue | 仅按状态显示按钮 | 否 | `[待修复]` |
| InventoryTransfer.vue | 仅按状态显示按钮 | 否 | `[待修复]` |
| InventoryCheck.vue | 仅按状态显示按钮 | 否 | `[待修复]` |
| InventoryLoss.vue | 仅按状态显示按钮 | 否 | `[待修复]` |
| InventoryAdjust.vue | 仅按状态显示按钮 | 否 | `[待修复]` |
| InventoryWarning.vue | 无 | 否 | `[待修复]` |
| SmartRestock.vue | 无 | 否 | `[待修复]` |
| InventoryReport.vue | 无 | 否 | `[待修复]` |
| InventoryLocation.vue | 无 | 否 | `[待修复]` |
| StoreInventory.vue | 无 | 否 | `[待修复]` |
| TraceQuery.vue | 无 | 否 | `[待修复]` |
| TraceChainView.vue | 无 | 否 | `[待修复]` |
| MaterialTraceCode.vue | 无 | 否 | `[待修复]` |
| FoodTraceCode.vue | 无 | 否 | `[待修复]` |
| ExpiryWarning.vue | 无 | 否 | `[待修复]` |
| RecallManagement.vue | 无 | 否 | `[待修复]` |
| TraceabilityQuality.vue | 无 | 否 | `[待修复]` |
| TraceabilityInspection.vue | 无 | 否 | `[待修复]` |
| LabelTemplate.vue | 无 | 否 | `[待修复]` |
| SupplierTrace.vue | 无 | 否 | `[待修复]` |

### 5.2 已存在的权限工具

- `usePermission.ts` 已提供 `canManageWarehouse`：`isAdmin || hasPermission('warehouse:manage')`。
- `[待修复]` `usePermission.ts` 未提供 `canManageTraceability` 或任何溯源相关权限计算属性。
- `usePermission.ts` 已提供 `hasPermission`、`hasAnyPermission`、`hasRole`、`hasAnyRole`。
- 项目规则要求按钮级权限使用 `v-permission` 指令。

### 5.3 路由与菜单配置

| 模块 | 路由 `meta.domain` | 路由 `meta.permissions` | 菜单 `permissions` |
|-----|-------------------|------------------------|-------------------|
| 仓储管理 | 已配置 `warehouse` | 未配置 | 未配置 |
| 溯源管理 | 已配置 `traceability` | 未配置 | 未配置 |

---

## 6. 主要问题汇总

1. `[待修复]` 溯源管理在 `frontend/src/utils/permissions.ts` 中完全未注册权限标识符，无法做任何细粒度权限控制。
2. `[待修复]` 仓储/溯源所有视图的操作按钮均未使用 `v-permission`、`hasPermission` 或 `hasRole`，任何能进入页面的用户均可点击创建、审批、执行、删除等敏感操作。
3. `[待修复]` 路由守卫仅检查域访问级别（`FULL/READ_ONLY/LIMITED/HIDDEN`），但页面内部未根据级别禁用写操作，`READ_ONLY` 角色仍可执行写入。
4. `[待修复]` `warehouseMenu` 与 `traceabilityMenu` 均未配置 `permissions`，侧边栏无法按角色隐藏子菜单。
5. `[待修复]` 仓储/溯源路由均未配置 `meta.permissions` / `meta.roles`，路由守卫无法基于权限标识符拦截越权访问。
6. `[待修复]` 仓储权限标识符粒度不足，缺少 `warehouse:outbound:create`、`warehouse:check:approve` 等细粒度标识符，现有 `warehouse:manage` 作为总开关难以支撑最小权限原则。
7. `[待修复]` `usePermission.ts` 缺少 `canManageTraceability` 等溯源相关计算属性，业务组件无法快速判断溯源操作权限。
8. `[待修复]` 数据范围控制（本门店/本部门/本人）当前未在仓储/溯源视图中落地，所有用户默认看到全量数据。

---

## 7. 改造建议

### 7.1 短期（最小改动）

1. 在 `frontend/src/utils/permissions.ts` 中注册溯源管理基础权限：
   - `traceability:manage`、`traceability:view`
   - `traceability:material:generate/print/export/delete`
   - `traceability:food:generate/print/export/delete`
   - `traceability:expiry:handle`
   - `traceability:recall:analyze/execute`
   - `traceability:quality:manage`
   - `traceability:inspection:manage`
   - `traceability:label:manage`
   - `traceability:supplier:view`
2. 在 `usePermission.ts` 中补充 `canManageTraceability` 计算属性。
3. 在仓储/溯源页面头部按钮和操作列按钮上增加 `v-permission` 指令，至少区分查看/编辑/审批/删除/执行等权限。
4. 为 `warehouseMenu` 和 `traceabilityMenu` 增加 `permissions` 字段，至少区分 `*:manage` 和 `*:view`。

### 7.2 中期

1. 仓储/溯源路由统一增加 `meta: { permissions: [...], roles: [...] }`，配合路由守卫实现越权拦截。
2. 补充仓储细粒度权限标识符（`warehouse:outbound:*`、`warehouse:transfer:*`、`warehouse:check:*`、`warehouse:loss:*`、`warehouse:adjust:*`、`warehouse:location:*` 等）。
3. 在审批型单据（出库、调拨、盘点、报损、调整）中增加“取消/撤回”操作，并补充 `rejected` / `cancelled` 状态与对应权限。
4. 将数据范围（`ALL / COMPANY / DEPARTMENT / STORE / SELF`）与仓储/溯源单据的 `storeId` / `departmentId` / `createBy` 字段联动，实现行级数据隔离。

### 7.3 长期

1. 建立仓储/溯源域独立的 RBAC 权限模板（参考 `useDomainPermission.ts` 中的 `businessDomains`），支持在「系统管理-权限模板」中动态配置。
2. 将溯源管理权限纳入 `permissionDescriptions` 与 `generatePermissionTree()`，确保角色管理页面可正确展示与分配。
3. 审批类操作统一走审批工作流 API，并在审批节点配置中绑定细粒度权限标识符。
