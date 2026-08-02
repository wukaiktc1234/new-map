# 门店运营模块待落地功能设计

> 创建时间：2026-07-13
> 状态：规划中（暂不实施，待后续 SDD 流水线调度）
> 关联模块：门店运营（store-ops）、采购管理（purchase）、仓储管理（warehouse）、产品中心（product）

---

## 一、背景

当前门店运营模块的"门店库存"页面（[StoreInventory.vue](../../frontend/src/views/store-ops/StoreInventory.vue)）中存在 4 个"功能开发中"占位（第 432/441/450/459 行）。同时，"集中式单店模式"权限模板已存在（[V20260630_003__create_permission_templates_table.sql](../../backend/src/main/resources/db/migration/V20260630_003__create_permission_templates_table.sql)），但门店运营尚未提供"采购收货入库"的简洁运行入口。

本文档记录待落地方案，便于后续 SDD Pipeline 调度实现。

---

## 二、当前系统运转逻辑

### 2.1 模块菜单结构

```
产品中心（/product）
  ├─ 菜品管理 /product/list          权限: product:manage
  ├─ 套餐管理 /product/combo         权限: product:manage
  ├─ 产品定价 /product/pricing       权限: product:manage
  ├─ 配方管理(BOM) /product/bom     权限: product:bom:manage
  └─ 菜品成本分析 /product/sales-analysis  权限: product:view

采购管理（/purchase）
  ├─ 商品档案 /purchase/archive            权限: purchase:archive:manage
  ├─ 采购订单 /purchase/orders             权限: purchase:order:manage
  ├─ 采购入库 /purchase/stockin            权限: purchase:stockin:manage
  └─ 供应商管理 /purchase/supplier          权限: purchase:supplier:manage

仓储管理（/warehouse）
  ├─ 库存概览 /warehouse/overview           权限: warehouse:overview:view
  ├─ 库存管理 /warehouse/inventory         权限: warehouse:inventory:view
  ├─ 入库管理 /warehouse/inbound            权限: warehouse:inbound:manage
  ├─ 出库管理 /warehouse/outbound           权限: warehouse:outbound:manage
  ├─ 库存盘点 /warehouse/check              权限: warehouse:check:manage
  ├─ 库存调拨 /warehouse/transfer           权限: warehouse:transfer:manage
  └─ 门店库存查看 /warehouse/store-inventory 权限: warehouse:store:manage

门店运营（/store-ops）
  ├─ 门店库存 /store-management/store-inventory （含 4 个"功能开发中"占位）
  └─ 物资需求 /store-management/material-request
```

### 2.2 数据流（已实现部分）

```
采购订单(/purchase/orders)
    ↓
采购入库(/purchase/stockin)  写入 purchase_stockins 表
    ↓
MaterialTraceCodeServiceImpl.updateStoreInventory()
    ↓
store_inventory 表（按 store_id + material_id 维度）
    ↓
    ├─→ 仓储模块"门店库存查看"(/warehouse/store-inventory) 只读统筹视图
    └─→ 门店运营"门店库存"(/store-management/store-inventory) 操作视图

菜品管理表单的"原料明细"(recipes 字段) ←→ 商品档案(/purchase/archive)
套餐管理 ←→ 菜品管理
菜品成本分析 ←→ store_inventory 表（拉取成本）
```

### 2.3 双向流通设计落地情况

| 设计要点 | 当前状态 | 说明 |
|---------|---------|------|
| 菜品 ↔ 采购原料关联 | ✅ 已实现 | FoodManagement.vue 原料明细区块 |
| 套餐 ↔ 菜品关联 | ✅ 已实现 | /product/combo |
| 权限模板管理 | ✅ 已实现 | permission_templates 表 |
| 集中式单店模式模板 | ✅ 已存在 | 隐藏采购/仓储/HR 等，聚焦核心营业 |
| 门店库存与仓储同分类 | ✅ 已实现 | /warehouse/store-inventory |
| 仓储独立查看门店库存 | ✅ 已实现 | warehouse/StoreInventory.vue 只读 |
| 采购入库 → 门店库存同步 | ✅ 已实现 | MaterialTraceCodeServiceImpl 自动写入 |
| 门店库存根据入库拉取成本 | ⚠️ 数据流已通 | 但门店运营 4 个操作功能为占位 |
| 集中式单店模式简洁采购入库 | ❌ 未落地 | 见下方方案 |

---

## 三、待落地方案 A：门店库存 4 个占位功能

[StoreInventory.vue:432/441/450/459](../../frontend/src/views/store-ops/StoreInventory.vue#L432) 的 4 个"功能开发中"占位：

| 函数 | 功能 | 当前状态 |
|------|------|---------|
| handleStockCheck | 库存盘点 | ElMessage.info('库存盘点功能开发中') |
| handleBatchMaterialRequest | 批量要货申请 | ElMessage.info('批量要货申请功能开发中') |
| handleBatchAdjust | 批量调整 | ElMessage.info('批量调整功能开发中') |
| handleBatchScrap | 批量报损 | ElMessage.info('批量报损功能开发中') |

### 方案对比

#### 方案 A1：引导跳转（最小改动，推荐先实施）

- 库存盘点 → 跳转 `/warehouse/check`（仓储模块已有完整功能）
- 要货申请 → 跳转 `/purchase/request`（采购申请页面）
- 批量调整/批量报损 → 引导到 `/warehouse/inventory` 操作

**优点**：复用现有功能，无新增后端 API
**缺点**：跨模块跳转，用户体验稍弱

#### 方案 A2：门店专属实现（中等改动）

在门店库存页面实现：
- 要货申请：复用 `/v1/purchase/requests` API（POST 创建采购申请，类型为门店要货）
- 库存盘点：新增 `store_inventory_checks` 表 + 后端 API + 前端盘点单对话框
- 批量调整：调用 `PUT /v1/store-inventory/{id}` 调整库存数量 + 记录日志
- 批量报损：调用 `POST /v1/store-inventory/scraps` 报损 + 扣减库存

**优点**：门店独立闭环
**缺点**：需新增后端表/API，工作量大

#### 方案 A3：仅实现门店专属的 2 个

- 实现：要货申请、库存盘点（门店专属盘点单）
- 引导：批量调整/批量报损 → 仓储模块

---

## 四、待落地方案 B：集中式单店模式的简洁采购入库

### 设计目标

> "允许在'集中式单店模式'门店运营模块拥有采购收货入库的简洁运行模块"

让单店模式用户无需进入复杂的采购管理模块，即可在门店运营内完成入库操作。

### 方案对比

#### 方案 B1：新增门店简洁入库页面（独立模块）

- 新增页面 `frontend/src/views/store-ops/StoreSimpleStockin.vue`
- 新增路由 `/store-ops/simple-stockin`
- 新增权限 `store:stockin:manage`
- 调用现有 API：`POST /v1/purchase/stockins`（复用后端逻辑）
- 表单字段简化：仅保留供应商、物资、数量、门店、批次号

**优点**：用户路径短，权限隔离清晰
**缺点**：新增页面，与采购入库功能重复

#### 方案 B2：复用现有采购入库页面（按模板控制显隐）

- 不新增页面，使用 `/purchase/stockin`
- 权限模板新增字段 `simple_mode: boolean`
- PurchaseStockin.vue 根据 `simple_mode` 切换 UI：
  - 简化模式：隐藏合同/结算/计划等字段
  - 完整模式：显示所有字段

**优点**：零新增页面，逻辑统一
**缺点**：UI 切换逻辑复杂，权限模板需扩展

---

## 五、后续实施建议

1. **优先级**：方案 A1（引导跳转）> 方案 B1（简洁入库）> 方案 A2/A3（门店专属功能）
2. **建议路径**：
   - 第一步：实施方案 A1，10 分钟内可完成（仅修改 4 个函数为路由跳转）
   - 第二步：通过 SDD Pipeline 启动方案 B1 的开发（`/sdd-run store-simple-stockin 门店简洁采购入库`）
   - 第三步：根据业务需要决定是否实施方案 A2 或 A3
3. **关联规范**：
   - [项目规则指南](../../.trae/rules/project_rules.md) 第二十八条（智能体技能增强体系）
   - [docs/spec/07-前端页面设计.md](../spec/07-前端页面设计.md)
   - [docs/spec/06-API接口设计.md](../spec/06-API接口设计.md)

---

## 六、相关代码索引

### 6.1 前端
- 门店库存页面：[frontend/src/views/store-ops/StoreInventory.vue](../../frontend/src/views/store-ops/StoreInventory.vue)
- 仓储门店库存查看：[frontend/src/views/warehouse/StoreInventory.vue](../../frontend/src/views/warehouse/StoreInventory.vue)
- 菜品管理（含原料明细）：[frontend/src/views/product/FoodManagement.vue](../../frontend/src/views/product/FoodManagement.vue)
- 菜品表单 converter：[frontend/src/api/product/converters.ts](../../frontend/src/api/product/converters.ts)
- 采购入库 API：[frontend/src/api/purchase/stockin.ts](../../frontend/src/api/purchase/stockin.ts)

### 6.2 后端
- 菜单结构：[backend/src/main/java/com/foodtraceability/service/impl/MenuServiceImpl.java](../../backend/src/main/java/com/foodtraceability/service/impl/MenuServiceImpl.java)
- 库存更新逻辑：[backend/src/main/java/com/foodtraceability/service/impl/MaterialTraceCodeServiceImpl.java](../../backend/src/main/java/com/foodtraceability/service/impl/MaterialTraceCodeServiceImpl.java#L119)
- 权限模板初始化：[backend/src/main/resources/db/migration/V20260630_003__create_permission_templates_table.sql](../../backend/src/main/resources/db/migration/V20260630_003__create_permission_templates_table.sql)
- 门店库存表：[backend/src/main/resources/db/migration/V20260704_004__create_store_inventory_table.sql](../../backend/src/main/resources/db/migration/V20260704_004__create_store_inventory_table.sql)

### 6.3 数据库表
- `purchase_stockins` — 采购入库单
- `store_inventory` — 门店库存（store_id + material_id 唯一）
- `store_inventory_log` — 门店库存变更日志
- `permission_templates` — 权限模板（含集中式单店模式）
- `materials` — 商品档案（采购物资）
- `foods` / `food_recipes` — 菜品及原料明细
