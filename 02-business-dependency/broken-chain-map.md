# 食品溯源系统 - Broken Chain Map

> 最后更新：2026-09-09

---

## 一、Broken Chain 总览

| 问题类型 | 数量 | 严重程度 | 影响范围 |
|----------|------|----------|----------|
| Foundation exists → downstream unavailable | 3 | 🔴 高 | 订单、库存、财务 |
| Master exists → capability not connected | 2 | 🟡 中 | 财务、采购 |
| Identity lost | 2 | 🔴 高 | 菜品、物料 |
| Data inheritance missing | 4 | 🔴 高 | 财务、溯源 |
| Wrong Truth Source | 3 | 🟡 中 | 菜品、财务、设备 |
| Duplicate Master | 8 | 🟡 中 | 全业务 |
| Missing configuration | 1 | 🟡 中 | 供应商 |
| Missing permission | 2 | 🔴 高 | 安全 |
| Scope conflict | 1 | 🟡 中 | 数据范围 |
| State conflict | 3 | 🔴 高 | 订单、凭证、库存 |
| Legacy interruption | 2 | 🟡 中 | 财务、设备 |

**总计**: 31 个 Broken Chain

---

## 二、Foundation exists → downstream unavailable

### BC-001: AutoVoucherService 零调用点

**问题描述**: AutoVoucherService 的两个方法 `generateSalesVoucher` 和 `generatePurchaseVoucher` 存在但无任何调用点。

**完整链路**:
```
Order (Foundation) 
    → OrderService (Capability)
        → OrderCompletedEvent (Event)
            → AutoVoucherService.generateSalesVoucher (Dead End)
                → 无调用点 ❌

Purchase (Foundation)
    → PurchaseService (Capability)
        → PurchaseStockInEvent (Event)
            → AutoVoucherService.generatePurchaseVoucher (Dead End)
                → 无调用点 ❌
```

**影响**:
- 订单完成后无法自动生成销售凭证
- 采购入库后无法自动生成采购凭证
- 财务报表数据不完整

**证据**:
- 文件: `AutoVoucherService.java`
- 方法: `generateSalesVoucher()`, `generatePurchaseVoucher()`
- 调用点: 0

**修复建议**:
1. 在 OrderCompletedEvent 监听器中调用 `generateSalesVoucher`
2. 在 PurchaseStockInEvent 监听器中调用 `generatePurchaseVoucher`

---

### BC-002: FinanceVoucherService 根包 no-op stub

**问题描述**: FinanceVoucherService 位于根包，是空实现的 no-op stub。

**完整链路**:
```
AccountingSubject (Foundation)
    → VoucherService (Capability)
        → FinanceVoucherService (根包, no-op stub)
            → 空实现 ❌
```

**影响**:
- 财务凭证服务功能缺失
- 可能与 Finance 模块的 VoucherService 冲突

**证据**:
- 文件: `FinanceVoucherService.java` (根包)
- 实现: 空方法体

**修复建议**:
1. 删除根包的 FinanceVoucherService
2. 或完成其实现并接入业务流程

---

### BC-003: 遗留财务服务零消费者

**问题描述**: 8个 Legacy 财务服务实现存在但无消费者。

**完整链路**:
```
AccountingSubject (Foundation)
    → legacyAccountingSubjectServiceImpl (Legacy)
        → 无消费者 ❌

CostAllocation (Master)
    → legacyCostAllocationServiceImpl (Legacy)
        → 无消费者 ❌

ElectronicVoucher (Master)
    → legacyElectronicVoucherServiceImpl (Legacy)
        → 无消费者 ❌

FinanceApproval (Master)
    → legacyFinanceApprovalServiceImpl (Legacy)
        → 无消费者 ❌

FinancePrint (Master)
    → legacyFinancePrintServiceImpl (Legacy)
        → 无消费者 ❌

FinanceVoucher (Master)
    → legacyFinanceVoucherServiceImpl (Legacy)
        → 无消费者 ❌

FinanceReport (Master)
    → legacyFinanceReportServiceImpl (Legacy)
        → 无消费者 ❌

PrintTemplateData (Master)
    → legacyPrintTemplateDataServiceImpl (Stub)
        → 空实现 ❌
```

**影响**:
- 代码维护成本高
- 可能与新服务冲突

**证据**:
- 文件: `legacyAccountingSubjectServiceImpl.java` 等8个文件
- 调用点: 0

**修复建议**:
1. 确认这些服务是否仍被需要
2. 如果不需要，删除这些 Legacy 服务

---

## 三、Master exists → capability not connected

### BC-004: AutoVoucherService 未接入采购流程

**问题描述**: AutoVoucherService 的 `generatePurchaseVoucher` 方法未接入采购流程。

**完整链路**:
```
Supplier (Master)
    → PurchaseService (Capability)
        → PurchaseStockInEvent (Event)
            → AutoVoucherService.generatePurchaseVoucher (Dead End)
                → 未连接 ❌
```

**影响**:
- 采购入库后无法自动生成采购凭证
- 财务数据不完整

**证据**:
- 方法: `AutoVoucherService.generatePurchaseVoucher()`
- 调用点: 0

**修复建议**:
1. 在 PurchaseStockInEvent 监听器中调用此方法

---

### BC-005: AutoVoucherService 未接入订单流程

**问题描述**: AutoVoucherService 的 `generateSalesVoucher` 方法未接入订单流程。

**完整链路**:
```
Food (Master)
    → OrderService (Capability)
        → OrderCompletedEvent (Event)
            → AutoVoucherService.generateSalesVoucher (Dead End)
                → 未连接 ❌
```

**影响**:
- 订单完成后无法自动生成销售凭证
- 财务数据不完整

**证据**:
- 方法: `AutoVoucherService.generateSalesVoucher()`
- 调用点: 0

**修复建议**:
1. 在 OrderCompletedEvent 监听器中调用此方法

---

## 四、Identity lost

### BC-006: food 表与 foods 表并存

**问题描述**: `food` 表 (旧) 和 `foods` 表 (新) 并存，身份混乱。

**完整链路**:
```
Food (Identity)
    ├──→ food表 (旧, DEPRECATED)
    │       → 无消费者 ❌
    │
    └──→ foods表 (新, VERIFIED)
            → OrderService
            → InventoryService
            → MenuService
```

**影响**:
- 数据可能不一致
- 查询可能返回错误结果
- 维护成本高

**证据**:
- 表: `food` (旧), `foods` (新)
- Entity: `food` (旧), `Food` (新)
- 业务对象地图标记: food为LEGACY, Food为VERIFIED

**修复建议**:
1. 迁移 `food` 表数据到 `foods` 表
2. 删除 `food` 表和对应的 Entity

---

### BC-007: material 无双表问题，但分类表并存

**问题描述**: 虽然物料表已统一为 `material_archives`，但分类表 `material_categories` 和可能的旧表并存。

**完整链路**:
```
Material (Identity)
    → material_archives表 (VERIFIED)
        → InventoryService
        → TraceabilityService
        → ProductionService

MaterialCategory (Identity)
    → material_categories表 (可能存在旧表)
        → 未知消费者
```

**影响**:
- 分类数据可能不一致
- 维护成本高

**证据**:
- 表: `material_categories`
- 未发现明确的旧表，但需确认

**修复建议**:
1. 确认是否存在旧的分类表
2. 如果存在，迁移数据并删除旧表

---

## 五、Data inheritance missing

### BC-008: 订单 → 财务流水继承断裂

**问题描述**: 订单完成后，财务流水记录可能不完整。

**完整链路**:
```
Order (Foundation)
    → OrderCompletedEvent (Event)
        → FinanceService (Capability)
            → FinanceRecordRepository (Repository)
                → finance_records表 (Target)
                    → 数据继承 ❌ (可能不完整)
```

**影响**:
- 财务报表数据不准确
- 资金流水记录不完整

**证据**:
- 事件: OrderCompletedEvent
- 消费者: FinanceService
- 但 AutoVoucherService 零调用，凭证未自动生成

**修复建议**:
1. 确保 OrderCompletedEvent 监听器正确生成财务记录
2. 接入 AutoVoucherService 自动生成凭证

---

### BC-009: 采购 → 应付继承断裂

**问题描述**: 采购入库后，应付账款可能不自动生成。

**完整链路**:
```
Purchase (Foundation)
    → PurchaseStockInEvent (Event)
        → PayableService (Capability)
            → PayableRepository (Repository)
                → payables表 (Target)
                    → 数据继承 ❌ (可能不完整)
```

**影响**:
- 应付账款数据不准确
- 付款流程延迟

**证据**:
- 事件: PurchaseStockInEvent
- 消费者: PayableService
- 但 AutoVoucherService 零调用，凭证未自动生成

**修复建议**:
1. 确保 PurchaseStockInEvent 监听器正确生成应付记录
2. 接入 AutoVoucherService 自动生成凭证

---

### BC-010: 凭证 → 财务报表继承断裂

**问题描述**: 凭证过账后，财务报表可能不自动更新。

**完整链路**:
```
Voucher (Foundation)
    → VoucherPostedEvent (Event)
        → ReportService (Capability)
            → 报表更新 ❌ (可能不完整)
```

**影响**:
- 财务报表数据不准确
- 月结数据错误

**证据**:
- 事件: VoucherPostedEvent
- 消费者: ReportService
- 但凭证状态前后端错位，可能导致状态不一致

**修复建议**:
1. 修复凭证状态前后端错位问题
2. 确保 VoucherPostedEvent 监听器正确更新报表

---

### BC-011: 溯源链路数据继承断裂

**问题描述**: 溯源链路中，物料追溯码到菜品追溯码的继承可能不完整。

**完整链路**:
```
Material (Foundation)
    → MaterialTraceCode (Master)
        → TraceabilityService (Capability)
            → FoodTraceCode (Master)
                → 溯源查询 ❌ (可能不完整)
```

**影响**:
- 溯源链路不完整
- 食品安全合规风险

**证据**:
- 物料追溯码: material_trace_code表
- 菜品追溯码: food_trace_code表
- 但继承关系可能不完整

**修复建议**:
1. 确保物料追溯码正确关联到菜品追溯码
2. 完善溯源链路的完整性检查

---

## 六、Wrong Truth Source

### BC-012: food 表与 foods 表真相源冲突

**问题描述**: `food` 表和 `foods` 表都是菜品的真相源，冲突。

**完整链路**:
```
Food (Truth Source)
    ├──→ food表 (旧, DEPRECATED)
    │       → 无消费者 ❌
    │
    └──→ foods表 (新, VERIFIED)
            → OrderService
            → InventoryService
            → MenuService
```

**影响**:
- 数据可能不一致
- 查询可能返回错误结果

**证据**:
- 表: `food` (旧), `foods` (新)
- 业务对象地图标记: food为LEGACY, Food为VERIFIED

**修复建议**:
1. 确认 `foods` 表为唯一真相源
2. 删除 `food` 表和对应的 Entity

---

### BC-013: finance_record 与 finance_records 真相源冲突

**问题描述**: `finance_record` 表和 `finance_records` 表都是财务流水的真相源，冲突。

**完整链路**:
```
FinanceRecord (Truth Source)
    ├──→ finance_record表 (旧, DEPRECATED)
    │       → 无消费者 ❌
    │
    └──→ finance_records表 (新, VERIFIED)
            → FinanceService
            → FinanceReportService
```

**影响**:
- 财务数据可能不一致
- 报表数据错误

**证据**:
- 表: `finance_record` (旧), `finance_records` (新)
- db-reality-map标记: 重复表问题

**修复建议**:
1. 确认 `finance_records` 表为唯一真相源
2. 删除 `finance_record` 表和对应的 Entity

---

### BC-014: device_status_log 与 device_status_logs 真相源冲突

**问题描述**: `device_status_log` 表和 `device_status_logs` 表都是设备状态日志的真相源，冲突。

**完整链路**:
```
DeviceStatusLog (Truth Source)
    ├──→ device_status_log表 (旧, DEPRECATED)
    │       → 未知消费者 ❌
    │
    └──→ device_status_logs表 (新, VERIFIED)
            → DeviceStatusLogService
```

**影响**:
- 设备状态日志可能不一致
- 设备监控数据错误

**证据**:
- 表: `device_status_log` (旧), `device_status_logs` (新)
- db-reality-map标记: 重复表问题

**修复建议**:
1. 确认 `device_status_logs` 表为唯一真相源
2. 删除 `device_status_log` 表和对应的 Entity

---

## 七、Duplicate Master

### BC-015: food 与 foods 重复

**问题描述**: `food` 和 `foods` 表重复，定义相同的业务对象。

**完整链路**:
```
Food (Business Object)
    ├──→ food表 (旧)
    │       → Entity: food
    │       → 无API
    │       → 无前端
    │
    └──→ foods表 (新)
            → Entity: Food
            → API: /v1/foods
            → 前端: FoodList.vue, FoodForm.vue
```

**影响**:
- 数据可能不一致
- 维护成本高

**修复建议**:
1. 迁移 `food` 表数据到 `foods` 表
2. 删除 `food` 表和对应的 Entity

---

### BC-016: food_category 与 food_categories 重复

**问题描述**: `food_category` 和 `food_categories` 表重复。

**完整链路**:
```
FoodCategory (Business Object)
    ├──→ food_category表 (旧)
    │       → 未知消费者 ❌
    │
    └──→ food_categories表 (新)
            → FoodCategoryService
            → API: /v1/food-categories
```

**影响**:
- 分类数据可能不一致
- 维护成本高

**修复建议**:
1. 迁移 `food_category` 表数据到 `food_categories` 表
2. 删除 `food_category` 表和对应的 Entity

---

### BC-017: dish_combo 与 dish_combos 重复

**问题描述**: `dish_combo` 和 `dish_combos` 表重复。

**完整链路**:
```
DishCombo (Business Object)
    ├──→ dish_combo表 (旧)
    │       → 未知消费者 ❌
    │
    └──→ dish_combos表 (新)
            → DishComboService
            → API: /v1/dish-combos
```

**影响**:
- 套餐数据可能不一致
- 维护成本高

**修复建议**:
1. 迁移 `dish_combo` 表数据到 `dish_combos` 表
2. 删除 `dish_combo` 表和对应的 Entity

---

### BC-018: dish_recipe 与 dish_recipes 重复

**问题描述**: `dish_recipe` 和 `dish_recipes` 表重复。

**完整链路**:
```
DishRecipe (Business Object)
    ├──→ dish_recipe表 (旧)
    │       → 未知消费者 ❌
    │
    └──→ dish_recipes表 (新)
            → DishRecipeService
            → API: /v1/dish-recipes
```

**影响**:
- 配方数据可能不一致
- 维护成本高

**修复建议**:
1. 迁移 `dish_recipe` 表数据到 `dish_recipes` 表
2. 删除 `dish_recipe` 表和对应的 Entity

---

### BC-019: finance_record 与 finance_records 重复

**问题描述**: `finance_record` 和 `finance_records` 表重复。

**完整链路**:
```
FinanceRecord (Business Object)
    ├──→ finance_record表 (旧)
    │       → 未知消费者 ❌
    │
    └──→ finance_records表 (新)
            → FinanceRecordService
            → API: /v1/finance-records
```

**影响**:
- 财务数据可能不一致
- 报表数据错误

**修复建议**:
1. 迁移 `finance_record` 表数据到 `finance_records` 表
2. 删除 `finance_record` 表和对应的 Entity

---

### BC-020: stores 与 stores_new 重复

**问题描述**: `stores` 和 `stores_new` 表重复。

**完整链路**:
```
Store (Business Object)
    ├──→ stores表 (旧)
    │       → 未知消费者 ❌
    │
    └──→ stores_new表 (新)
            → StoreService
            → API: /v1/stores
```

**影响**:
- 门店数据可能不一致
- 维护成本高

**修复建议**:
1. 迁移 `stores` 表数据到 `stores_new` 表
2. 删除 `stores` 表和对应的 Entity

---

### BC-021: orders (V1) 与 orders (V6) 版本冲突

**问题描述**: `orders` 表在 V1.0.0.100 和 V6.0.0 中均存在定义，版本冲突。

**完整链路**:
```
Order (Business Object)
    ├──→ orders表 (V1.0.0.100)
    │       → 可能残留数据 ❌
    │
    └──→ orders表 (V6.0.0)
            → OrderService
            → API: /v1/orders
```

**影响**:
- 表结构变更可能导致数据丢失
- 版本冲突可能导致数据不一致

**修复建议**:
1. 确认 V6 已完全覆盖 V1
2. 删除 V1 的残留数据和定义

---

### BC-022: voucher_header 与 finance_vouchers 重复

**问题描述**: `voucher_header` 表和 `finance_vouchers` 表重复，前者为死体系。

**完整链路**:
```
Voucher (Business Object)
    ├──→ voucher_header表 (死体系, DEPRECATED)
    │       → 无消费者 ❌
    │
    └──→ finance_vouchers表 (新, VERIFIED)
            → VoucherService
            → API: /v1/finance/vouchers
```

**影响**:
- 凭证数据可能不一致
- 维护成本高

**修复建议**:
1. 删除 `voucher_header` 表和对应的 Entity

---

## 八、Missing configuration

### BC-023: SupplierPortal H5 端点无认证配置

**问题描述**: SupplierPortalController 的 6 个 H5 端点无认证配置。

**完整链路**:
```
Supplier (Master)
    → SupplierPortalController (Capability)
        → /h5/contract (Endpoint)
            → 无JWT认证 ❌
        → /h5/view (Endpoint)
            → 无JWT认证 ❌
        → /h5/verify (Endpoint)
            → 无JWT认证 ❌
        → /h5/sign (Endpoint)
            → 无JWT认证 ❌
        → /h5/reject (Endpoint)
            → 无JWT认证 ❌
        → /h5/... (Endpoint)
            → 无JWT认证 ❌
```

**影响**:
- 外部供应商无法安全访问
- 安全风险

**证据**:
- 控制器: SupplierPortalController
- 端点: 6个H5端点
- SecurityConfig 白名单: 未包含

**修复建议**:
1. 实现 Token-based 认证或短信验证码认证
2. 或将这些端点加入 SecurityConfig 白名单（不推荐）

---

## 九、Missing permission

### BC-024: 25个裸接口方法无权限保护

**问题描述**: 25个接口方法无 @RequiresPermission 注解保护。

**完整链路**:
```
用户请求
    → Spring Security (认证)
        → PermissionAspect (授权)
            → 无 @RequiresPermission 注解 ❌
                → 直接访问业务逻辑
```

**影响**:
- 安全漏洞
- 未授权访问风险

**证据**:
- 接口语解: PD-006
- 方法数: 25
- 域: self-purchase (6), plan-items (7), tasks (12)

**修复建议**:
1. 为每个方法添加 @RequiresPermission 注解
2. 确定每个方法的权限码归属

---

### BC-025: 退款审批身份归因不明确

**问题描述**: 退款审批人身份归因不明确，审计追踪不完整。

**完整链路**:
```
Order (Foundation)
    → OrderRefundEvent (Event)
        → FinanceService (Capability)
            → 退款审批
                → 审批人身份 ❌ (归因不明确)
```

**影响**:
- 审计追踪不完整
- 合规风险

**证据**:
- 问题: PD-010
- 影响: 退款审批人身份归因不明确

**修复建议**:
1. 明确退款审批人身份归属逻辑
2. 记录审批人信息

---

## 十、Scope conflict

### BC-026: role_stores 表无 migration

**问题描述**: `role_stores` 表存在但无 Flyway 迁移脚本。

**完整链路**:
```
用户请求
    → DataScopeAspect (数据范围过滤)
        → role_stores表 (数据范围绑定)
            → 无migration ❌
                → 数据范围可能不一致
```

**影响**:
- 数据范围过滤可能不准确
- 数据泄露风险

**证据**:
- 表: role_stores
- 迁移: 无
- 权限地图标记: 无migration

**修复建议**:
1. 创建 Flyway 迁移脚本
2. 确保 role_stores 表结构一致

---

## 十一、State conflict

### BC-027: 订单状态三表并存

**问题描述**: 订单状态在 `order_status`, `payment_status`, `legacy` 三表中并存，冲突。

**完整链路**:
```
Order (Business Object)
    ├──→ order_status表
    │       → 状态: 0-6 (7个状态)
    │
    ├──→ payment_status表
    │       → 状态: 0-3 (4个状态)
    │
    └──→ legacy表
            → 状态: -1, 0-7 (9个状态)
```

**影响**:
- 状态转换可能不一致
- 订单全流程混乱

**证据**:
- 状态机地图: 订单状态机（三套并存 = CONFLICT）
- 冲突: 状态值和语义不一致

**修复建议**:
1. 统一订单状态枚举
2. 建立状态映射关系
3. 考虑废弃 legacy 状态

---

### BC-028: 凭证状态前后端错位

**问题描述**: 凭证状态在前后端不一致，后端 0-3，前端 1-4。

**完整链路**:
```
Voucher (Business Object)
    ├──→ 后端状态
    │       → 0: DRAFT, 1: APPROVED, 2: POSTED, 3: VOID
    │
    └──→ 前端状态
            → 1: DRAFT, 2: APPROVED, 3: POSTED, 4: VOID
```

**影响**:
- 状态显示不一致
- 状态转换可能出错

**证据**:
- 状态机地图: 凭证状态机 - 前端映射错位
- 冲突: 前后端状态码 +1 错位

**修复建议**:
1. 统一前后端状态码（建议统一为 0-3）
2. 建立状态转换中间层

---

### BC-029: sales_order 无映射关系

**问题描述**: `sales_order` 表的状态与 `order_status` 完全无映射。

**完整链路**:
```
Order (Business Object)
    ├──→ order_status表
    │       → 状态: 0-6
    │
    └──→ sales_order表
            → 状态: pending, preparing, completed, delivered
            → 无映射关系 ❌
```

**影响**:
- 销售订单状态无法与主订单同步
- 数据不一致

**证据**:
- 状态机地图: sales_order（销售订单表）
- 冲突: 与 order_status 无映射关系

**修复建议**:
1. 建立 sales_order 与 order_status 的映射关系
2. 或废弃 sales_order 表

---

## 十二、Legacy interruption

### BC-030: 遗留财务服务零消费者

**问题描述**: 8个 Legacy 财务服务实现存在但无消费者。

**完整链路**:
```
财务业务 (Legacy)
    → Legacy服务实现
        → 无消费者 ❌
            → 功能中断
```

**影响**:
- 代码维护成本高
- 可能与新服务冲突

**证据**:
- 文件: 8个Legacy服务实现
- legacy-map: 已标记为废弃

**修复建议**:
1. 确认这些服务是否仍被需要
2. 如果不需要，删除这些 Legacy 服务

---

### BC-031: 遗留硬件控制器全部废弃

**问题描述**: 13个硬件控制器全部废弃，但未清理。

**完整链路**:
```
硬件设备 (Legacy)
    → 遗留硬件控制器
        → 无消费者 ❌
            → 功能中断
```

**影响**:
- 代码维护成本高
- 可能与新控制器冲突

**证据**:
- 文件: 13个硬件控制器
- legacy-map: 已标记为废弃

**修复建议**:
1. 删除这些遗留硬件控制器

---

## 十三、Broken Chain 修复优先级

### 13.1 高优先级 (P0)

| 编号 | 问题 | 影响 | 修复建议 |
|------|------|------|----------|
| BC-001 | AutoVoucherService 零调用 | 财务凭证无法自动生成 | 接入事件监听器 |
| BC-004 | AutoVoucherService 未接入采购 | 采购凭证无法自动生成 | 接入事件监听器 |
| BC-005 | AutoVoucherService 未接入订单 | 销售凭证无法自动生成 | 接入事件监听器 |
| BC-024 | 25个裸接口无权限保护 | 安全漏洞 | 添加权限注解 |
| BC-027 | 订单状态三表并存 | 订单全流程混乱 | 统一状态枚举 |

### 13.2 中优先级 (P1)

| 编号 | 问题 | 影响 | 修复建议 |
|------|------|------|----------|
| BC-006 | food 与 foods 表并存 | 数据不一致 | 迁移并删除旧表 |
| BC-012 | food 与 foods 真相源冲突 | 数据不一致 | 确认真相源 |
| BC-013 | finance_record 与 finance_records 冲突 | 财务数据不一致 | 迁移并删除旧表 |
| BC-014 | device_status_log 与 device_status_logs 冲突 | 设备日志不一致 | 迁移并删除旧表 |
| BC-023 | SupplierPortal H5 无认证 | 安全风险 | 实现认证机制 |
| BC-026 | role_stores 无 migration | 数据范围不一致 | 创建迁移脚本 |
| BC-028 | 凭证状态前后端错位 | 状态显示不一致 | 统一状态码 |

### 13.3 低优先级 (P2)

| 编号 | 问题 | 影响 | 修复建议 |
|------|------|------|----------|
| BC-002 | FinanceVoucherService 根包 stub | 功能缺失 | 删除或完成实现 |
| BC-003 | 遗留财务服务零消费者 | 维护成本高 | 删除 Legacy 服务 |
| BC-015-BC-022 | 其他重复表 | 维护成本高 | 迁移并删除旧表 |
| BC-030 | 遗留财务服务 | 维护成本高 | 删除 Legacy 服务 |
| BC-031 | 遗留硬件控制器 | 维护成本高 | 删除废弃控制器 |

---

*生成时间：2026-09-09*
*数据来源：代码库静态分析 + 业务对象地图 + API地图 + 状态机地图 + 遗留地图*
