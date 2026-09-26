# POS 菜单 / 库存 / 分类 三项诊断报告（阶段 1：诊断）

- **日期**：2026-09-26
- **阶段**：阶段 1（诊断，只读）— 未改代码 / 未改 DB；全部发现记入本报告，未顺手修（PG-005）
- **范围**：① 库存详情变动记录查询 ② POS 菜单库存显示链路 ③ POS 分类来源
- **证据规范**：结论均附 file:line 或活体 DB 实测；依赖生产数据的结论标 PROVISIONAL
- **交接条件**：待 Owner 审过（PG-005 阶段 1 → 阶段 2 门禁）

---

## 诊断 1：库存详情变动记录查询

### 链路
`frontend/src/views/warehouse/StoreInventory.vue` 详情弹窗 `handleView`（L350）→ `loadTransactionRecords(materialId, warehouseId)`（L361）→ `inventoryLogApi.getPage`
→ `frontend/src/api/warehouse/inventory-log.ts` L66-78 `toBackendLogQuery`：`materialId→productId`、`size→pageSize`（**参数名匹配，不是参数丢失问题**）
→ `GET /v1/inventory/logs/page` → `InventoryLogController.getInventoryLogPage`（L69-90）
→ `InventoryLogServiceImpl.getInventoryLogPage`（L49-51）
→ `InventoryLogMapper.selectInventoryLogPage`（接口 L29-35，6 个 `@Param` 全部绑定）

### WHERE 检查
`backend/src/main/resources/mapper/InventoryLogMapper.xml` L4-7：

```sql
SELECT * FROM inventory_log
ORDER BY create_time DESC
```

**整个 WHERE 子句不存在**。mapper 接口的 6 个 `@Param`（productId / warehouseId / operationType / operatorId / startTime / endTime）全部绑定但 XML 里一个未引用 → 详情弹窗传的 productId + warehouseId 被**静默丢弃**。

### JOIN 检查
单表查询，**无 JOIN** → "一对多行重复"假设在此路径不成立。问题不是行重复，是**每一行都是别的物料的**。

### 根因判断
库存详情的"变动记录"实际展示**全局库存日志**（仅 `create_time DESC` + 分页）。

### 修复方向（未实施）
`InventoryLogMapper.xml` 的 `selectInventoryLogPage` 补动态 `<where>`：`deleted=0` + product_id / warehouse_id / operation_type / operator_id / create_time 区间（参数名现成）。
现成模板：同目录 `StoreInventoryLogMapper.xml` L6-19（同款动态 where）。
其他调用方影响：
- `InventoryConsumptionController.java` L47 传 operationType="out" → 语义保留
- `WarehouseOverview.vue` L271 不传参 → "全局最近日志"语义恰好维持

**验证约束**：本地 `inventory_log` 当前 0 行（SQL 层结论确定性成立；行级验证需生产数据或新造测试数据）。

---

## 诊断 2：POS 菜单库存显示链路

### 双轨三条路径（POS 菜单不是单一数据源）

| 页面 | 菜单数据源 | 库存显示 |
|---|---|---|
| **Home.vue**（主 POS 屏，`useMenu` L71-82） | `/v1/product-center/foods/on-sale` + `/v1/product-center/combos/on-sale` + `/v1/product-center/categories/tree/enabled` → **新 `foods` 表** | `FoodVO.stock` = **`foods.stock` 自有字段**（`FoodServiceImpl.convertToVO` L721 `vo.setStock(food.getStock())`） |
| **Order.vue**（L333-336） | `posApi.getFullMenu()` → `/v1/pos/api/menu` → `PosApiServiceImpl.getAllDishes`（L76-79）→ **legacy `food` 表** | 无库存字段（`PosDishDTO` 无 stock，不显示） |
| **CustomerOrder.vue**（L340-342） | 直接 `/v1/pos/api/menu` → **legacy `food` 表** | 无库存字段（不显示） |

三个页面均为路由在册页面（`frontend-pos/src/router/index.ts` L14 / L26 / L44）。

### 菜单库存到底读哪个表
- **主 POS 屏显示库存 = 新 `foods` 表的 stock 字段**，不读 store_inventory；下单时 `FoodNewMapper.deductStock`（L54，`UPDATE foods SET stock = stock - ...`）同步扣减，所以前端库存会动
- Order / CustomerOrder 两页**不显示库存**（读 legacy `food`，DTO 无该字段）
- `store_inventory` 是**原料库存**，只在下单扣料时使用，**任何菜单显示路径都不读它**

### 与 F6（P1-NEW-FOOD-LEGACY-SYNC-001）对比
- F6 病灶 = 产品中心新建菜品只写 `foods`、不写 legacy `food` → Order/CustomerOrder 两页菜单看不到新菜 + 下单 legacy 扣减报误导性"库存不足"
- **F6-A**（双写 `syncLegacyFood`，`FoodServiceImpl` L133-179）已覆盖菜单路径：新菜创建 / 更新 / 改状态后回填 legacy → 两页菜单即时完整
- **F6-B**（下单自愈）只护下单路径、不护菜单——但菜单洞已被 A 堵上
- **结论：不需要扩展 F6 到菜单显示路径**

### 观察项（登记，未修）
1. `syncLegacyFood` 是 try-catch **吞异常仅 warn**（L176-178）→ 双写失败时 Order/CustomerOrder 菜单再次缺菜，且无告警通道
2. 主页菜单"低库存"（stock<10，`useMenu.ts` L62-64）基于 `foods.stock`（菜品自身可售数），与原料库存 store_inventory 是**两个概念**；若 Owner 期望菜单显示原料库存，是新需求，不是 F6 残留

---

## 诊断 3：POS 分类来源

### 两张独立分类表（活体 DB 实测）

| 表 | 内容 | 本地行数 |
|---|---|---|
| `food_categories`（新，产品中心） | 热菜/凉菜/主食/汤类/饮品/甜品 + **蔬菜(745)/肉食(746)** + 1 个 e2e 测试类 | 9 |
| `food_category`（legacy，单数） | 12 列（category_id / category_name / category_code / parent_id / ...，information_schema 实测） | **0**（PROVISIONAL：生产未核实） |

两表之间**无同步机制**。

### 菜品分类完整数据流
1. **新建菜品**：`FoodCreateDTO.categoryId`（Long）→ `foods.category_id` → `food_categories`。POS 分类在产品中心分类树选择（`/v1/product-center/categories/tree/enabled`，`CategoryController` L76）
2. **F6 双写**：`syncLegacyFood`（L135-145）把 **category_name 中文名**（如"蔬菜"）写进 legacy `food.food_category`；无分类 → 字面量"未分类"
3. **菜单分类展示**：
   - Home.vue 主页：分类树 = `food_categories` → **自洽**（菜品与分类同源）
   - Order/CustomerOrder：分类列表 = `food_category` 表（`PosApiServiceImpl.getCategories` L59-71，按 categoryCode 返回）→ 本地 0 行 = **无分类页签**；菜品 categoryId = `food.food_category`（名字"蔬菜"）→ `categoryMap`（key = category_code，L81-82）查不到 → 全部显示"其他"（L89）

### 当前断点（3 处）
1. **legacy `food_category` 表无数据**（本地 0 行；PROVISIONAL：生产未核实）→ legacy 菜单无分类页签
2. **命名空间错位**：`food.food_category` 存的是**名字**（活体实测 distinct = 蔬菜/主食），legacy 菜单匹配逻辑假设的是 **code**（categoryMap 按 categoryCode 建索引；`getDishesByCategory` L104 按 code 精确 eq）→ 即使表有数据，code≠name 依然不匹配
3. **双表互不同步**：产品中心新建的分类（蔬菜 745 / 肉食 746 即此类）永远进不了 `food_category` → legacy POS 菜单永远看不到新分类

### 修复方向（未实施）
- **根治**：统一分类源（Order/CustomerOrder 也切产品中心 `food_categories` + `foods`，即消掉 legacy 菜单双轨）
- **短期补丁**：`food_categories → food_category` 同步 + `syncLegacyFood` 改写 code 命名空间
- legacy 表当前 0 行，短期补丁收益有限 → **建议先裁定 legacy 菜单页去留**（是否仍作为独立 POS 入口使用），再决定修哪边

---

## 阶段 1 登记清单（未顺手修）

| # | 发现 | 归属 |
|---|---|---|
| 1 | `InventoryLogMapper.xml` selectInventoryLogPage 无 WHERE（6 个 @Param 全未用） | 诊断 1 根因 |
| 2 | POS 菜单双轨三页（3 页面 2 数据源，库存仅主屏显示且来自 foods.stock） | 诊断 2 |
| 3 | `syncLegacyFood` try-catch 吞异常仅 warn，双写失败无告警 | 诊断 2 观察 1 |
| 4 | `foods.stock`（菜品可售数）与 store_inventory（原料库存）语义不同 | 诊断 2 观察 2 |
| 5 | legacy `food_category` 表 0 行（PROVISIONAL） | 诊断 3 断点 1 |
| 6 | `food.food_category` 存名字、legacy 菜单匹配假设 code | 诊断 3 断点 2 |
| 7 | `food_category` / `food_categories` 双表无同步 | 诊断 3 断点 3 |

**关联**：PG-003（food_category legacy schema 史：P1-POS-MENU-500-001）/ P1-NEW-FOOD-LEGACY-SYNC-001（F6）/ P1-COMBO-LEGACY-CLEANUP-001（套餐新表）

**活体 DB 快照（2026-09-26 本地 food_traceability）**：
- food（legacy 菜品）8 行 / foods（新菜品）8 行
- food_category 0 行 / food_categories 9 行
- inventory_log 0 行
- `food.food_category` distinct = 蔬菜、主食（均为名字）
