# 食品溯源系统 - 项目地图

## 一、项目概览

- **项目名称**: 食品溯源系统 (Food Traceability System)
- **技术栈**: Spring Boot 3.2.0 + Vue.js 3.5.x 前后端分离
- **目标用户**: 个体餐饮企业
- **服务地址**:
  - 后端: http://localhost:8081/api
  - 前端: http://localhost:3000/ （开发端口可能变化，当前为3002）
- **数据库**: PostgreSQL 18（生产）, H2（开发）
- **缓存**: Redis（生产）, 本地Caffeine（开发）
- **消息队列**: RabbitMQ

---

## 二、目录结构地图

### 2.1 后端目录 (`backend/src/main/java/com/foodtraceability/`)

| 目录 | 职责 | 关键文件 |
|------|------|---------|
| `controller/` | 控制器层，接收HTTP请求 | `FoodController.java`, `AuthController.java`, `PurchaseStockinController.java` |
| `service/` | 业务逻辑层接口与实现 | `service/impl/` 下为实现类 |
| `dataservice/` | 数据访问+缓存层 | `FoodDataService.java` |
| `mapper/` | MyBatis Plus数据访问层 | `FoodNewMapper.java` |
| `entity/` | 实体类 | `FoodNew.java`, `Inventory.java` |
| `dto/` | 数据传输对象 | `FoodCreateDTO.java`, `FoodVO.java` |
| `config/` | 配置类 | `SecurityConfig.java`, `MybatisPlusConfig.java` |
| `common/` | 公共类 | `Result.java`, `ErrorCode.java` |
| `security/` | 安全相关 | `JwtUtils.java`, `filter/` |
| `utils/` | 工具类 |  |

### 2.2 前端目录 (`frontend/src/`)

| 目录 | 职责 | 关键文件 |
|------|------|---------|
| `api/` | API接口封装 | `request.ts`, `product/food.ts` |
| `components/` | 公共组件 | `core/` (基础层), `business/` (业务层) |
| `composables/` | Vue组合式函数 | `useCrudTable.ts`, `useForm.ts` |
| `config/` | 配置文件 |  |
| `layouts/` | 页面布局 |  |
| `router/` | 路由配置 | `index.ts`, `guards.ts` |
| `stores/` | Pinia状态管理 | `permission.ts`, `layout.ts` |
| `styles/` | 样式文件 | `global/_element-overrides.scss` |
| `types/` | TypeScript类型定义 | `product.ts`, `purchase.ts` |
| `utils/` | 工具函数 |  |
| `views/` | 页面视图 | `product/`, `purchase/`, `warehouse/` 等 |

---

## 三、业务模块地图

### 3.1 产品中心 (`/product`)

| 页面 | 路由 | 文件 | 状态 |
|------|------|------|------|
| 菜品管理 | `/product/food` | `views/product/FoodManagement.vue` | ✅ 正常 |
| 菜品分类 | `/product/category` | `views/product/CategoryManagement.vue` | ✅ 正常 |
| 套餐管理 | `/product/combo` | `views/product/DishCombo.vue` | ✅ 正常 |
| 菜品定价 | `/product/pricing` | `views/product/DishPricing.vue` | ✅ 正常 |
| 菜品成本分析 | `/product/cost-analysis` | `views/product/DishCostAnalysis.vue` | ✅ 正常 |

**后端对应**:
- Controller: `FoodController.java` (`/v1/product-center/foods`)
- Service: `FoodServiceImpl.java`
- Entity: `FoodNew.java` (表: `foods`)
- 配方: `DishRecipeNew.java` (表: `dish_recipes`)

### 3.2 采购管理 (`/purchase`)

| 页面 | 路由 | 文件 | 状态 |
|------|------|------|------|
| 采购订单 | `/purchase/orders` | `views/purchase/PurchaseOrder.vue` | ✅ 已修复 |
| 商品档案 | `/purchase/archive` | `views/purchase/PurchaseArchive.vue` | ✅ 已修复 |
| 商品分类 | `/purchase/material-category` | `views/purchase/MaterialCategory.vue` | ✅ 已修复 |
| 采购计划 | `/purchase/plan` | `views/purchase/PurchasePlan.vue` | ✅ 已修复 |
| 采购收货 | `/purchase/stockin` | `views/purchase/PurchaseStockin.vue` | ✅ 已修复 |
| 采购合同 | `/purchase/contract` | `views/purchase/PurchaseContract.vue` | ✅ 已修复 |
| 电子合同 | `/purchase/electronic-contract` | `views/purchase/ElectronicContract.vue` | ✅ 已修复 |
| 采购申请 | `/purchase/request` | `views/purchase/PurchaseRequest.vue` | ✅ 已修复 |
| 采购结算 | `/purchase/settlement` | `views/purchase/PurchaseSettlement.vue` | ✅ 已修复 |
| 采购报表 | `/purchase/report` | `views/purchase/PurchaseReport.vue` | ✅ 已修复 |
| 采购数据分析 | `/purchase/analysis` | `views/purchase/PurchaseAnalysis.vue` | ✅ 正常 |
| 供应商档案 | `/purchase/supplier` | `views/purchase/SupplierArchive.vue` | ✅ 已修复 |
| 物资需求提报 | `/purchase/material-request` | `views/purchase/MaterialRequest.vue` | ✅ 已修复 |

**后端对应**:
- 采购收货: `PurchaseStockinServiceImpl.java` → 入库后调用 `inventoryService.increaseInventory()`
- 采购订单: `PurchaseOrderServiceImpl.java`

### 3.3 仓储管理 (`/warehouse`)

| 页面 | 路由 | 文件 | 状态 |
|------|------|------|------|
| 库存概览 | `/warehouse/overview` | `views/warehouse/WarehouseOverview.vue` | ✅ 已修复 |
| 门店库存查看 | `/warehouse/store-inventory` | `views/warehouse/StoreInventory.vue` | ✅ 已修复 |
| 库存管理 | `/warehouse/inventory` | `views/product/Inventory.vue` | ✅ 正常 |
| 库存预警 | `/warehouse/warning` | `views/warehouse/InventoryWarning.vue` | ✅ 正常 |
| 库存盘点 | `/warehouse/check` | `views/warehouse/InventoryCheck.vue` | ✅ 正常 |
| 库存报表 | `/warehouse/report` | `views/warehouse/InventoryReport.vue` | ✅ 正常 |
| 库位管理 | `/warehouse/location` | `views/warehouse/InventoryLocation.vue` | ✅ 正常 |
| 库存调拨 | `/warehouse/transfer` | `views/warehouse/InventoryTransfer.vue` | ✅ 正常 |
| 库存出库 | `/warehouse/outbound` | `views/warehouse/InventoryOutbound.vue` | ✅ 正常 |
| 库存调整 | `/warehouse/adjust` | `views/warehouse/InventoryAdjust.vue` | ✅ 正常 |
| 库存报损 | `/warehouse/inventory-loss` | `views/warehouse/InventoryLoss.vue` | ✅ 正常 |
| 智能补货建议 | `/warehouse/smart-restock` | `views/warehouse/SmartRestock.vue` | ✅ 正常 |

**后端对应**:
- 库存服务: `InventoryServiceImpl.java` (表: `inventory`)
- 库存扣减: `InventoryDeductionServiceImpl.java` (支持追溯码模式和配方模式)
- 门店库存: `StoreInventoryServiceImpl.java`

### 3.4 订单管理 (`/order`)

| 页面 | 路由 | 文件 | 状态 |
|------|------|------|------|
| 订单查询 | `/order/query` | `views/order/OrderQuery.vue` | ❌ 损坏 |
| 订单统计 | `/order/statistics` | `views/order/OrderStatistics.vue` | ❌ 损坏 |
| 退款管理 | `/order/refund` | `views/order/OrderRefund.vue` | ❌ 损坏 |
| 预约管理 | `/order/reservation` | `views/order/OrderReservation.vue` | ❌ 损坏 |

### 3.5 溯源管理 (`/traceability`)

| 页面 | 路由 | 文件 | 状态 |
|------|------|------|------|
| 追溯查询 | `/traceability/query` | `views/traceability/TraceQuery.vue` | ❌ 损坏 |
| 原料追溯 | `/traceability/material-code` | `views/traceability/MaterialTraceCode.vue` | ❌ 损坏 |
| 食品追溯 | `/traceability/food-trace-code` | `views/traceability/FoodTraceCode.vue` | ❌ 损坏 |
| 追溯链展示 | `/traceability/chain` | `views/traceability/TraceChainView.vue` | ✅ 正常 |
| 临期预警 | `/traceability/expiry-warning` | `views/traceability/ExpiryWarning.vue` | ✅ 正常 |
| 召回管理 | `/traceability/recall` | `views/traceability/RecallManagement.vue` | ✅ 正常 |
| 质量追溯 | `/traceability/quality` | `views/traceability/TraceabilityQuality.vue` | ❌ 损坏 |
| 标签模板 | `/traceability/label-template` | `views/traceability/LabelTemplate.vue` | ✅ 正常 |
| 供应商追溯 | `/traceability/supplier` | `views/traceability/SupplierTrace.vue` | ✅ 正常 |
| 检验记录 | `/traceability/inspection` | `views/traceability/TraceabilityInspection.vue` | ❌ 损坏 |

### 3.6 其他模块

| 模块 | 页面数 | 损坏数 | 目录 |
|------|--------|--------|------|
| 运营中心 | 5+1 | 1 | `views/operations/` |
| 门店运营 | 10 | 8 | `views/store-ops/` |
| 会员管理 | 6 | 3 | `views/marketing/` |
| 财务中心 | 13 | 0 | `views/finance/` |
| 资产管理 | 7 | 6 | `views/asset/` |
| 人事管理 | 18 | 11 | `views/hr/` |
| 设备管理 | 4 | 3 | `views/device/` |
| 系统管理 | 4 | 4 | `views/system/` |

---

## 四、数据业务链条地图

### 4.1 采购 → 库存 链条

```
供应商 → 采购申请 → 采购订单 → 采购收货(质检)
                                    ↓
                              原料入库(inventory表)
                                    ↓
                        库存成本更新(unit_cost / total_cost)
                                    ↓
                              门店库存同步
                                    ↓
                              应付账款创建
```

**关键服务**:
- `PurchaseStockinServiceImpl.confirmStockin()` → 确认入库
- `InventoryServiceImpl.increaseInventory()` → 增加库存
- `StoreInventoryServiceImpl.increaseStock()` → 门店库存

**已完成优化（2026-07-07）**:
- ✅ 为门店库存增加 `unit_cost` 和 `total_cost` 成本字段
- ✅ 实现加权平均成本计算法
- ✅ 采购入库时同步传入单位成本到门店库存
- ✅ 库存调整接口支持传入单位成本
- ✅ 出库时返回出库成本，便于成本结转

### 4.2 菜品成本 链条

```
原料库存成本(unit_cost)
        ↓
菜品配方(BOM) - dish_recipes表
        ↓
菜品成本价(cost_price) = Σ(原料单价 × 配方用量)
        ↓
菜品售价(sale_price) - 成本价 = 毛利
```

**现状问题**:
- ❌ 配方中的原料单价是手动填写的，不是从库存成本自动获取
- ❌ 库存成本变化时，菜品成本不会自动更新

### 4.3 销售 → 库存扣减 链条

```
顾客点餐 → 销售订单 → 后厨出餐
                        ↓
                库存扣减(两种模式)
                ├─ 模式A: 追溯码扣减（精确到批次）
                └─ 模式B: 配方扣减（按BOM用量）
                        ↓
                库存数量减少 → 库存成本结转
```

**关键服务**:
- `InventoryDeductionServiceImpl.deductByTraceCode()` - 追溯码模式
- `InventoryDeductionServiceImpl.deductByRecipe()` - 配方模式

**现状问题**:
- ❌ 与订单流程的集成度不明确
- ❌ 菜品库存(foods.stock字段)是静态的，不随销售变化

---

## 五、核心数据表地图

### 5.1 产品相关

| 表名 | 实体类 | 说明 |
|------|--------|------|
| `foods` | `FoodNew` | 菜品主表 |
| `food_categories` | `FoodCategoryNew` | 菜品分类 |
| `dish_recipes` | `DishRecipeNew` | 菜品配方(BOM) |
| `dish_combos` | `DishComboNew` | 套餐 |
| `combo_ingredients` | `ComboIngredient` | 套餐明细 |

### 5.2 采购相关

| 表名 | 实体类 | 说明 |
|------|--------|------|
| `purchase_orders` | `PurchaseOrder` | 采购订单 |
| `purchase_order_items` | `PurchaseOrderItem` | 采购订单明细 |
| `purchase_stockins` | `PurchaseStockin` | 采购入库单 |
| `purchase_stockin_items` | `PurchaseStockinItem` | 入库明细 |
| `suppliers` | `Supplier` | 供应商 |
| `material_archives` | `MaterialArchive` | 物资/原料档案 |
| `material_categories` | `MaterialCategory` | 物资分类 |

### 5.3 库存相关

| 表名 | 实体类 | 说明 |
|------|--------|------|
| `inventory` | `Inventory` | 原料库存主表 |
| `inventory_transactions` | `InventoryTransaction` | 库存流水 |
| `store_inventory` | `StoreInventory` | 门店库存 |
| `inventory_checks` | `InventoryCheck` | 盘点单 |
| `inventory_warnings` | `InventoryWarning` | 库存预警 |
| `inventory_locations` | `InventoryLocation` | 库位 |
| `dish_inventory` | `DishInventory` | 菜品库存关联 |

### 5.4 订单相关

| 表名 | 实体类 | 说明 |
|------|--------|------|
| `orders` | `OrderNew` | 订单主表 |
| `order_items` | `OrderItemNew` | 订单明细 |

---

## 六、快速索引

### 6.1 常用文件快速查找

| 需求 | 文件路径 |
|------|---------|
| 登录接口 | `backend/.../controller/AuthController.java` |
| JWT工具 | `backend/.../security/utils/JwtUtils.java` |
| 全局异常 | `backend/.../common/GlobalExceptionHandler.java` |
| 统一响应 | `backend/.../common/Result.java` |
| 前端请求封装 | `frontend/src/api/request.ts` |
| 路由配置 | `frontend/src/router/index.ts` |
| 权限守卫 | `frontend/src/router/guards.ts` |
| 核心表格组件 | `frontend/src/components/core/DataTable.vue` |
| 状态标签组件 | `frontend/src/components/core/StatusTag.vue` |
| CRUD组合函数 | `frontend/src/composables/useCrudTable.ts` |
| Element Plus样式覆盖 | `frontend/src/styles/global/_element-overrides.scss` |

### 6.2 常用API路径

| 模块 | API前缀 | Controller |
|------|---------|------------|
| 认证 | `/v1/auth` | `AuthController` |
| 菜品 | `/v1/product-center/foods` | `FoodController` |
| 采购收货 | `/v1/purchase/stockin` | `PurchaseStockinController` |
| 库存 | `/v1/warehouse/inventory` | `InventoryController` |

---

## 七、已知问题清单

### 7.1 已修复

| 问题 | 修复时间 | 文件 |
|------|---------|------|
| JWT密钥长度不足（HS512需要64字节） | 2026-07-06 | `JwtUtils.java` |
| PostgreSQL端口错误（54321被迅雷占用） | 2026-07-06 | `application-pg.yml` |
| 前端63个文件损坏（乱码、标签不闭合等） | 2026-07-07 | `docs/frontend-repair-record.md` |
| 门店库存缺少成本字段 | 2026-07-07 | `StoreInventory.java`, `store_inventory`表 |
| 门店库存未实现加权平均成本法 | 2026-07-07 | `StoreInventoryServiceImpl.java` |
| 采购入库未同步成本到门店库存 | 2026-07-07 | `PurchaseStockinServiceImpl.java` |
| Kitchen端端口配置冲突（package.json覆盖vite配置） | 2026-07-07 | `frontend-kitchen/package.json` |
| 登录页验证码不预加载（用户体验问题） | 2026-07-07 | `LoginPage.vue` |
| 订单完成时未扣减门店库存 | 2026-07-07 | `OrderNewServiceImpl.java` |

### 7.2 前端文件修复

已全部修复，详见：`docs/frontend-repair-record.md`

### 7.3 待优化 - 数据业务链条

- [x] 门店库存增加成本字段 ✅ 2026-07-07
- [x] 实现加权平均成本计算法 ✅ 2026-07-07
- [x] 采购入库同步成本到门店库存 ✅ 2026-07-07
- [ ] 销售订单完成时自动扣减门店库存并结转成本
- [ ] 菜品成本应从库存成本自动计算，而非手动填写
- [ ] 订单与库存扣减的深度集成（BOM配方自动扣减）
- [ ] 成本核算方法完善（移动加权平均/FIFO）

### 7.4 关于决策看板页面

**结论：不是孤岛页面**

- 路由存在：`/operations/decision-board` → `views/operations/DecisionBoard.vue`
- 菜单配置存在：运营中心 → 经营分析
- 显示控制：由权限模板控制，不同模板显示不同模块
  - `centralized-single`（集中式单店模式）：包含 operations 域 ✅
  - `standard-chain`（标准连锁模式）：包含 operations 域 ✅
  - `large-chain`（大型连锁模式）：包含 operations 域 ✅
- 管理员默认可以看到所有模块

**相关文件**：
- 页面：`views/operations/DecisionBoard.vue`
- 菜单：`modules/operations/menu.ts`
- 路由：`router/index.ts`
- 权限模板：`stores/permission.ts` → `getFallbackTemplates()`

---

## 八、开发规范速查

| 规范 | 文档位置 |
|------|---------|
| 项目规则总览 | `.trae/rules/project_rules.md` |
| 完整规范体系 | `docs/spec/` 目录（共16个文档） |
| API接口设计 | `docs/spec/06-API接口设计.md` |
| 数据库设计 | `docs/spec/05-数据库设计.md` |
| 前端页面设计 | `docs/spec/07-前端页面设计.md` |
| 组件与样式规范 | `docs/spec/08-组件与样式规范.md` |
| 数据转换器规范 | `docs/spec/16-数据转换器规范.md` |
| 编码规范 | `docs/spec/13-编码规范.md` |
| 开发效率API规范 | `docs/spec/14-开发效率API规范.md` |
