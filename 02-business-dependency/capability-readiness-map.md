# 食品溯源系统 - Capability Readiness 地图

> 最后更新：2026-09-09

---

## 一、Capability Readiness 总览

| Capability | 依赖的 Foundation | 状态 | 缺失项 |
|------------|-------------------|------|--------|
| **订单管理** | Food, Store, Customer, Permission, DataScope | ✅ READY | 无 |
| **库存管理** | Material, Warehouse, Store, Permission, DataScope | ✅ READY | 无 |
| **采购管理** | Supplier, Material, Warehouse, Store, Permission, DataScope | ⚠️ PARTIAL | SupplierPortal H5无认证 |
| **财务管理** | AccountingSubject, Store, Permission, DataScope | ⚠️ PARTIAL | AutoVoucherService零调用 |
| **溯源管理** | Material, Food, Supplier, Permission, DataScope | ✅ READY | 无 |
| **人力资源** | Employee, Department, Store, Permission, DataScope | ✅ READY | 无 |
| **会员营销** | Customer, Store, Permission, DataScope | ✅ READY | 无 |
| **设备管理** | Device, Store, Permission, DataScope | ⚠️ PARTIAL | 遗留硬件控制器废弃 |
| **报表分析** | 所有业务数据 | ⚠️ PARTIAL | 月结调度依赖财务数据 |

---

## 二、核心 Capability 详细分析

### 2.1 订单管理 (Order Management)

**状态**: ✅ READY

| 依赖项 | 状态 | 说明 |
|--------|------|------|
| Food (菜品) | ✅ READY | foods表正常，API完整 |
| Store (门店) | ✅ READY | stores_new表正常，API完整 |
| Customer (客户) | ✅ READY | members表正常，API完整 |
| Permission (权限) | ✅ READY | order:read/write/cancel 权限码存在 |
| DataScope (数据范围) | ✅ READY | 按门店、日期、客户、状态过滤 |

**完整链路**:
```
Food ──→ OrderService ──→ OrderRepository ──→ orders表
  │         │
  │         ├──→ OrderCreatedEvent ──→ InventoryService
  │         ├──→ OrderCompletedEvent ──→ FinanceService
  │         └──→ OrderRefundEvent ──→ InventoryService, FinanceService
  │
Store ──→ DataScope ──→ store_id 过滤
```

**证据**:
- API: `/v1/orders` (GET/POST/PUT/DELETE)
- 前端: `src/views/order/OrderList.vue`, `src/views/food/FoodForm.vue`
- 事件: OrderCreatedEvent, OrderCompletedEvent, OrderRefundEvent
- 权限: order:read, order:write, order:cancel

**风险项**:
- ⚠️ 订单状态三表并存 (order_status, payment_status, legacy)
- ⚠️ sales_order 虚实体无映射关系

---

### 2.2 库存管理 (Inventory Management)

**状态**: ✅ READY

| 依赖项 | 状态 | 说明 |
|--------|------|------|
| Material (物料) | ✅ READY | material_archives表正常 |
| Warehouse (仓库) | ✅ READY | inventory表正常 |
| Store (门店) | ✅ READY | store_inventory表正常 |
| Permission (权限) | ✅ READY | inventory:read/write/transfer 权限码存在 |
| DataScope (数据范围) | ✅ READY | 按仓库、物料、批次过滤 |

**完整链路**:
```
Material ──→ InventoryService ──→ InventoryRepository ──→ inventory表
  │              │
  │              ├──→ StockInEvent ──→ 库存增加
  │              ├──→ StockOutEvent ──→ 库存减少
  │              └──→ LowStockEvent ──→ 预警通知
  │
Store ──→ StoreInventoryService ──→ store_inventory表
```

**证据**:
- API: `/v1/inventory` (GET/POST/PUT)
- 前端: `src/views/inventory/InventoryList.vue`
- 事件: StockInEvent, StockOutEvent, LowStockEvent
- 权限: inventory:read, inventory:write, inventory:transfer

**风险项**:
- ⚠️ 库存单据状态风格不统一 (数字/英文/中文)
- ⚠️ 门店库存与中央库存同步策略待定

---

### 2.3 采购管理 (Procurement Management)

**状态**: ⚠️ PARTIAL

| 依赖项 | 状态 | 说明 |
|--------|------|------|
| Supplier (供应商) | ⚠️ PARTIAL | suppliers表正常，但H5端点无认证 |
| Material (物料) | ✅ READY | material_archives表正常 |
| Warehouse (仓库) | ✅ READY | 库存表正常 |
| Store (门店) | ✅ READY | 门店表正常 |
| Permission (权限) | ⚠️ PARTIAL | purchase:read/write 存在，但H5端点无权限 |
| DataScope (数据范围) | ✅ READY | 按供应商、日期、状态过滤 |

**完整链路**:
```
Supplier ──→ PurchaseService ──→ PurchaseOrderRepository ──→ purchase_orders表
  │              │
  │              ├──→ PurchaseStockInEvent ──→ InventoryService
  │              └──→ ReceiptConfirmationCompletedEvent ──→ PayableService
  │
Material ──→ MaterialRepository ──→ material_archives表
```

**证据**:
- API: `/v1/purchase` (GET/POST/PUT)
- 前端: 采购管理界面
- 事件: PurchaseStockInEvent, ReceiptConfirmationCompletedEvent
- 权限: purchase:read, purchase:write

**缺失项**:
1. **SupplierPortal H5 无认证** (PD-008)
   - 6个端点: `/h5/contract`, `/h5/view`, `/h5/verify`, `/h5/sign`, `/h5/reject`, `/h5/...`
   - 问题: 外部供应商无系统账号，H5端点无JWT认证
   - 影响: 供应商无法安全访问合同和订单

2. **自采接口无前端消费**
   - 端点: `/v1/self-purchase/*` (6方法)
   - 问题: 无前端消费，可能是废弃接口

---

### 2.4 财务管理 (Financial Management)

**状态**: ⚠️ PARTIAL

| 依赖项 | 状态 | 说明 |
|--------|------|------|
| AccountingSubject (科目) | ✅ READY | accounting_subjects表正常 |
| Store (门店) | ✅ READY | 门店表正常 |
| Permission (权限) | ✅ READY | finance:read/write/approve 权限码存在 |
| DataScope (数据范围) | ✅ READY | 按期间、类型、部门过滤 |

**完整链路**:
```
AccountingSubject ──→ VoucherService ──→ VoucherRepository ──→ finance_vouchers表
  │                      │
  │                      ├──→ VoucherPostedEvent ──→ ReportService
  │                      └──→ AutoVoucherService (零调用)
  │
Store ──→ FinanceService ──→ FinanceRecordRepository ──→ finance_records表
```

**证据**:
- API: `/v1/finance` (GET/POST/PUT)
- 前端: `src/views/finance/VoucherList.vue`
- 事件: VoucherPostedEvent, PaymentCompletedEvent
- 权限: finance:read, finance:write, finance:approve

**缺失项**:
1. **AutoVoucherService 零调用点**
   - 方法: `generateSalesVoucher`, `generatePurchaseVoucher`
   - 问题: 未接入订单/采购流程
   - 影响: 财务凭证无法自动生成

2. **FinanceVoucherService 根包 no-op stub**
   - 问题: 空实现，未接入业务流程
   - 影响: 功能缺失

3. **凭证状态前后端错位**
   - 后端: 0-3 (DRAFT/APPROVED/POSTED/VOID)
   - 前端: 1-4 (VoucherStatusMap)
   - 影响: 状态显示和转换不一致

---

### 2.5 溯源管理 (Traceability Management)

**状态**: ✅ READY

| 依赖项 | 状态 | 说明 |
|--------|------|------|
| Material (物料) | ✅ READY | material_archives表正常 |
| Food (菜品) | ✅ READY | foods表正常 |
| Supplier (供应商) | ✅ READY | suppliers表正常 |
| Permission (权限) | ✅ READY | material-trace-code:read/write 权限码存在 |
| DataScope (数据范围) | ✅ READY | 按物料、批次、供应商过滤 |

**完整链路**:
```
Material ──→ MaterialTraceCodeService ──→ material_trace_code表
  │              │
  │              └──→ TraceabilityService ──→ 溯源查询
  │
Food ──→ FoodTraceCodeService ──→ food_trace_code表
  │              │
  │              └──→ TraceabilityService ──→ 溯源查询
  │
Supplier ──→ 供应商溯源码 ──→ TraceabilityService
```

**证据**:
- API: `/v1/traceability` (GET/POST)
- 前端: `src/views/traceability/MaterialTraceCodeList.vue`
- 权限: material-trace-code:read, material-trace-code:write

**风险项**:
- ⚠️ 溯源链路依赖Material和Supplier，任一中断影响溯源完整性

---

### 2.6 人力资源 (Human Resources)

**状态**: ✅ READY

| 依赖项 | 状态 | 说明 |
|--------|------|------|
| Employee (员工) | ✅ READY | employees表正常 |
| Department (部门) | ✅ READY | departments表正常 |
| Store (门店) | ✅ READY | stores_new表正常 |
| Permission (权限) | ✅ READY | hr:read/write 权限码存在 |
| DataScope (数据范围) | ✅ READY | 按部门、门店过滤 |

**完整链路**:
```
Employee ──→ EmployeeService ──→ EmployeeRepository ──→ employees表
  │              │
  │              ├──→ SalaryService ──→ salary_records表
  │              ├──→ AttendanceService ──→ attendance_record表
  │              └──→ OnboardingService ──→ onboarding_archive表
  │
Department ──→ DepartmentService ──→ departments表
```

**证据**:
- API: `/v1/employees` (GET/POST/PUT)
- 前端: Employee终端
- 权限: hr:read, hr:write

---

### 2.7 会员营销 (Member Marketing)

**状态**: ✅ READY

| 依赖项 | 状态 | 说明 |
|--------|------|------|
| Customer (客户) | ✅ READY | members表正常 |
| Store (门店) | ✅ READY | stores_new表正常 |
| Permission (权限) | ✅ READY | marketing:read/write 权限码存在 |
| DataScope (数据范围) | ✅ READY | 按门店、日期、状态过滤 |

**完整链路**:
```
Customer ──→ MemberService ──→ MemberRepository ──→ members表
  │              │
  │              ├──→ MemberLevelService ──→ member_level表
  │              ├──→ CouponService ──→ coupon_template表
  │              └──→ RechargeService ──→ recharge_record表
```

**证据**:
- API: `/v1/members` (GET/POST/PUT)
- 前端: 营销管理界面
- 权限: marketing:read, marketing:write

---

### 2.8 设备管理 (Device Management)

**状态**: ⚠️ PARTIAL

| 依赖项 | 状态 | 说明 |
|--------|------|------|
| Device (设备) | ✅ READY | devices表正常 |
| Store (门店) | ✅ READY | stores_new表正常 |
| Permission (权限) | ✅ READY | device:read/write 权限码存在 |
| DataScope (数据范围) | ✅ READY | 按门店过滤 |

**完整链路**:
```
Device ──→ DeviceService ──→ DeviceRepository ──→ devices表
  │              │
  │              ├──→ DeviceStatusLogService ──→ device_status_logs表
  │              └──→ DeviceTemplateService ──→ device_templates表
```

**证据**:
- API: `/v1/devices` (GET/POST/PUT)
- 权限: device:read, device:write

**缺失项**:
1. **遗留硬件控制器废弃** (13个)
   - WeighingDeviceController, TTSController, TakeoutLockerController等
   - 问题: 已废弃但未清理
   - 影响: 代码维护成本

2. **设备状态日志双表并存**
   - device_status_log (旧) 和 device_status_logs (新)
   - 问题: 数据可能不一致
   - 影响: 状态日志查询不准确

---

### 2.9 报表分析 (Reporting & Analytics)

**状态**: ⚠️ PARTIAL

| 依赖项 | 状态 | 说明 |
|--------|------|------|
| 所有业务数据 | ⚠️ PARTIAL | 依赖财务数据准确性 |
| Permission (权限) | ✅ READY | report:read 权限码存在 |
| DataScope (数据范围) | ✅ READY | 按门店、部门、日期过滤 |

**完整链路**:
```
Order ──→ ReportService ──→ 营业报表
  │
Purchase ──→ ReportService ──→ 采购报表
  │
Finance ──→ ReportService ──→ 财务报表
  │
Inventory ──→ ReportService ──→ 库存报表
```

**缺失项**:
1. **月结调度依赖财务数据准确性**
   - MonthlyClosingScheduler 依赖 VoucherPostedEvent
   - 问题: 如果事件中断，月结数据不准确
   - 影响: 报表数据错误

2. **AutoVoucherService 零调用**
   - 问题: 财务凭证无法自动生成
   - 影响: 报表数据不完整

---

## 三、Capability 依赖矩阵

### 3.1 Foundation → Capability 映射

| Foundation | Capabilities |
|------------|--------------|
| Food | 订单管理, 库存管理, 溯源管理 |
| Material | 库存管理, 采购管理, 溯源管理 |
| Store | 订单管理, 库存管理, 采购管理, 财务管理, 人力资源, 会员营销, 设备管理 |
| Supplier | 采购管理, 溯源管理 |
| AccountingSubject | 财务管理, 报表分析 |
| Employee | 人力资源 |
| Customer | 订单管理, 会员营销 |
| Category | 订单管理, 库存管理, 采购管理 |
| Warehouse | 库存管理, 采购管理 |
| Permission | 所有Capabilities |
| DataScope | 所有Capabilities |

### 3.2 Capability → Transaction 映射

| Capability | Transactions |
|------------|--------------|
| 订单管理 | POS点餐, 小程序点餐, 外卖订单, 桌位管理 |
| 库存管理 | 入库, 出库, 调拨, 盘点, 报损 |
| 采购管理 | 采购申请, 采购订单, 收货确认, 退货 |
| 财务管理 | 凭证创建, 凭证过账, 付款, 收款, 税务 |
| 溯源管理 | 物料追溯码, 菜品追溯码, 溯源查询 |
| 人力资源 | 员工管理, 薪资管理, 考勤管理, 排班管理 |
| 会员营销 | 会员管理, 优惠券, 充值, 积分 |
| 设备管理 | 设备注册, 状态监控, 模板管理 |

---

## 四、Capability 中断影响评估

### 4.1 高影响 Capability (中断导致业务瘫痪)

| Capability | 中断影响 | 恢复时间 | 业务损失 |
|------------|----------|----------|----------|
| 订单管理 | 门店无法收银 | 4-8小时 | 门店停业、客户流失 |
| 库存管理 | 库存无法管理 | 4-8小时 | 生产停滞、断供 |
| 财务管理 | 财务无法核算 | 8-16小时 | 财务瘫痪、合规风险 |

### 4.2 中影响 Capability (中断导致部分功能异常)

| Capability | 中断影响 | 恢复时间 | 业务损失 |
|------------|----------|----------|----------|
| 采购管理 | 采购无法执行 | 4-8小时 | 原材料断供 |
| 人力资源 | HR功能异常 | 2-4小时 | 员工管理混乱 |
| 会员营销 | 会员功能异常 | 2-4小时 | 客户服务中断 |

### 4.3 低影响 Capability (中断影响有限)

| Capability | 中断影响 | 恢复时间 | 业务损失 |
|------------|----------|----------|----------|
| 设备管理 | 设备功能异常 | 1-2小时 | 设备管理不便 |
| 报表分析 | 报表无法生成 | 2-4小时 | 决策支持缺失 |

---

## 五、关键发现

### 5.1 高风险发现

1. **AutoVoucherService 零调用** - 财务凭证无法自动生成，影响报表准确性
2. **SupplierPortal H5 无认证** - 外部供应商无法安全访问，存在安全风险
3. **凭证状态前后端错位** - 状态显示和转换不一致，可能导致操作错误

### 5.2 中风险发现

1. **订单状态三表并存** - 状态转换可能不一致，影响订单全流程
2. **库存单据状态风格不一** - 状态定义混乱，影响库存管理
3. **设备状态日志双表并存** - 数据可能不一致，影响设备监控

### 5.3 低风险发现

1. **遗留代码未清理** - 8个Legacy服务实现、13个废弃控制器
2. **死代码未清理** - DatabaseFixConfig 包含死方法
3. **隐藏Fallback机制** - 6个降级机制可能掩盖问题

---

*生成时间：2026-09-09*
*数据来源：代码库静态分析 + 业务对象地图 + API地图 + 权限地图*
