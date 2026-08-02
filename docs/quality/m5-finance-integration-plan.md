# M5 财务联动里程碑实施计划

> 目标：在已打通的采购-仓储-库存主链路基础上，完成采购到财务的最小闭环联动，使采购订单确认后能够生成应付结算单据，财务经理可审批付款，付款完成后联动更新结算单状态与应付余额。
> 更新日期：2026-07-23

---

## 一、目标

1. 补齐采购订单确认 → 采购结算单生成的自动/半自动触发能力。
2. 让财务经理（`finance_manager` / `finance_director`）能够查看并审批采购结算单。
3. 付款完成后，采购结算单状态与应付账款余额同步更新。
4. 不破坏现有采购-仓储-库存链路，优先采用现有真实接口，仅做必要的最小改造。

---

## 二、调研结论

### 2.1 现有财务相关模块与接口状态

| 模块 | 主要后端文件 | 接口路径 | 当前状态 | 备注 |
|------|-------------|----------|----------|------|
| 采购结算单 | `controller/purchase/PurchaseSettlementController.java` | `/v1/purchase/settlements/**` | **真实可用** | 已具备分页、详情、创建、更新、删除、确认结算（pay/complete）、发票申请/收票接口 |
| 应付账款 | `controller/finance/PayableController.java` | `/v1/finance/payables/**` | **真实可用** | 已具备 CRUD、分页、确认付款；`PayableService` 已提供入库/收货/退货场景的联动入口 |
| 付款单 | `controller/finance/PaymentController.java` | `/v1/finance/payments/**` | **真实可用** | 登记付款已实现四账联动（应付账款 + 银行账户 + 资金流水 + 会计凭证） |
| 财务审批 | `controller/finance/FinanceApprovalController.java` | `/v1/finance/approvals/**` | 骨架/部分可用 | 存在审批查询接口，但 M5 最小范围可暂不依赖 |
| 发票报销 | `controller/finance/InvoiceReimbursementController.java` | `/v1/finance/invoice-reimbursements/**` | 骨架/部分可用 | M5 可延后 |
| 财务报表 | `controller/finance/FinanceStatisticsController.java` | `/v1/finance/statistics/**` | 骨架/部分可用 | M5 可延后 |

> 结论：M5 所需核心能力（采购结算、应付账款、付款单）后端 Controller 与 Service 已基本实现，无需从零开发；主要缺口在**触发时机、权限分配、接口权限注解补全**。

### 2.2 采购 → 财务数据流转现状

```
采购申请 → 采购订单 → 采购订单审批/确认下单
                                │
                                ▼
                      ┌─────────────────────┐
                      │  当前：无自动联动    │
                      │  需新增：生成采购结算单  │
                      └─────────────────────┘
                                │
                ┌───────────────┼───────────────┐
                ▼               ▼               ▼
          采购入库确认      收货确认           采购退货
                │               │               │
                ▼               ▼               ▼
        PayableService    PayableService    PayableService
        .createForStockin  .createForReceiptConfirmation  .createRedPayableForReturn
                │               │               │
                └───────────────┴───────────────┘
                                │
                                ▼
                         应付账款表（payables）
                                │
                                ▼
              PaymentController.registerPayment
              四账联动：
              payables + bank_accounts + fund_flows + finance_vouchers
```

关键发现：

- 采购入库确认（`PurchaseStockinServiceImpl.confirmStockin`）已同事务调用 `PayableService.createForStockin` 生成应付账款，幂等键为 `AP` + 0 填充的 `stockinId`。
- 收货确认（`ReceiptConfirmationServiceImpl`）同样会生成应付账款。
- 采购退货会生成红字应付单。
- **采购订单确认下单目前不会自动生成采购结算单**，这是 M5 必须补齐的环节。

### 2.3 现有财务相关表结构

| 表名 | 作用 | 关键字段 |
|------|------|----------|
| `purchase_settlements` | 采购结算单 | `settlement_id`, `settlement_no`, `order_id`, `order_no`, `supplier_id`, `total_amount`, `paid_amount`, `unpaid_amount`, `due_date`, `status`, `invoice_status`, `deleted` |
| `payables` | 应付账款 | `payable_id`, `payable_no`, `stockin_id`, `order_id`, `supplier_id`, `amount`, `paid_amount`, `balance_amount`, `status`, `due_date`, `deleted` |
| `payments` | 付款单 | `payment_id`, `payment_no`, `payable_id`, `payment_amount`, `bank_account_id`, `voucher_id`, `fund_flow_id`, `status`, `deleted` |
| `bank_accounts` | 银行账户 | `account_id`, `account_name`, `balance`, `status`, `deleted` |
| `fund_flows` | 资金流水 | `flow_id`, `flow_no`, `account_id`, `flow_direction`, `flow_category`, `amount`, `voucher_id`, `deleted` |
| `finance_vouchers` | 会计凭证 | `voucher_id`, `voucher_no`, `debit_amount`, `credit_amount`, `status`, `deleted` |
| `accounting_subjects` | 会计科目 | `subject_id`, `subject_code`, `subject_name`, `deleted` |

> 结论：M5 不需要新建核心财务表，现有表已足够支撑最小闭环。如需增强可追溯性，可在 `purchase_settlements` 增加 `payable_id` 关联或在 `payments` 增加 `settlement_id` 关联（可选，见第 4 节）。

---

## 三、M5 最小可验收范围

### 3.1 业务闭环

1. **触发**：采购订单经 `finance_manager` 审批通过并确认下单后，系统自动生成一张采购结算单（或允许采购员/财务经理手动创建）。
2. **查看**：财务经理进入「采购结算」列表，可查看待结算单据。
3. **审批/付款**：财务经理对结算单执行付款确认（可登记凭证号），系统自动/半自动完成对应应付账款的付款登记。
4. **状态同步**：付款完成后，采购结算单状态从 `pending` → `finance_reviewing` / `completed`，应付账款余额同步减少。

### 3.2 包含与不包含

| 包含 | 不包含（M5+） |
|------|--------------|
| 采购结算单自动生成/手动创建 | 发票 OCR 识别与验真 |
| 采购结算单列表与详情查询 | 财务报表与经营分析 |
| 结算单付款确认与凭证号登记 | 预算控制与超预算预警 |
| 应付账款付款登记（四账联动） | 成本核算与加权平均成本 |
| 付款后状态回写 | 月末结账与损益结转 |
| 财务经理权限分配 | 税务管理与电子发票全生命周期 |

---

## 四、接口清单

### 4.1 已有接口（可直接使用）

| 接口 | 方法 | 路径 | 当前权限 | M5 用法 |
|------|------|------|----------|---------|
| 分页查询采购结算 | GET | `/v1/purchase/settlements` | `isAuthenticated()` | 财务经理查看待结算列表 |
| 采购结算详情 | GET | `/v1/purchase/settlements/{id}` | `isAuthenticated()` | 查看结算单详情 |
| 创建采购结算 | POST | `/v1/purchase/settlements` | `admin/finance_manager/purchaser` | 手动创建结算单 |
| 更新采购结算 | PUT | `/v1/purchase/settlements/{id}` | `admin/finance_manager/purchaser` | 修改待结算单据 |
| 确认结算 | POST | `/v1/purchase/settlements/{id}/settle?action=pay\|complete&voucherNo=xxx` | `admin/finance_manager` | 财务经理确认付款/完成 |
| 分页查询应付账款 | GET | `/v1/finance/payables` | `finance:payable:query` | 财务经理查看应付 |
| 应付账款详情 | GET | `/v1/finance/payables/{id}` | `finance:payable:query` | 查看应付详情 |
| 确认付款 | POST | `/v1/finance/payables/{id}/payment?amount=xxx` | `finance:payable:approve` | 登记付款金额 |
| 登记付款（四账联动） | POST | `/v1/finance/payments` | 无注解 | 出纳/财务登记付款 |
| 查询付款历史 | GET | `/v1/finance/payments/by-payable/{payableId}` | 无注解 | 查看某笔应付的付款记录 |

### 4.2 需新增/修改的接口

| 变更类型 | 接口 | 路径 | 说明 |
|----------|------|------|------|
| **新增 Service 方法** | 采购订单确认时生成结算单 | 无新端点 | 在 `PurchaseOrderServiceImpl.confirmOrder` 中同事务调用 `PurchaseSettlementService.createSettlement`，幂等键建议为 `orderId` |
| **新增/修改 DTO** | 采购结算创建 DTO | 无新端点 | 提供 `fromPurchaseOrder(PurchaseOrder order)` 构造方法，自动填充 `order_id`、`order_no`、`supplier_id`、`total_amount`、`due_date = 下单日+30天` |
| **新增权限注解** | 付款登记 | POST `/v1/finance/payments` | 当前无 `@PreAuthorize`，需补充 `finance:payment:create` 或 `finance:payable:approve` 权限校验 |
| **新增权限注解** | 查询付款历史 | GET `/v1/finance/payments/by-payable/{payableId}` | 当前无 `@PreAuthorize`，需补充 `finance:payment:query` 或 `finance:payable:query` |
| **新增权限注解** | 付款单详情 | GET `/v1/finance/payments/{paymentId}` | 当前无 `@PreAuthorize`，需补充 `finance:payment:query` |
| **可选：新增状态回写** | 付款完成后回写结算单 | 无新端点 | 在 `PaymentServiceImpl.registerPayment` 成功后，根据 `payable.order_id` 查找对应 `purchase_settlements` 并调用 `settle(id, "pay", paymentNo)`，实现结算单自动到 `completed` |

---

## 五、表结构建议

### 5.1 无需修改的表

- `purchase_settlements`
- `payables`
- `payments`
- `bank_accounts`
- `fund_flows`
- `finance_vouchers`
- `accounting_subjects`

### 5.2 可选增强（建议 M5 实施）

为提升可追溯性，建议在 `payments` 表新增 `settlement_id` 字段：

```sql
ALTER TABLE payments
ADD COLUMN settlement_id BIGINT,
ADD CONSTRAINT fk_payments_settlement
    FOREIGN KEY (settlement_id) REFERENCES purchase_settlements(settlement_id);
```

> 影响：需同步修改 `Payment` 实体、`PaymentCreateDTO`、`PaymentVO`、对应 Mapper XML（如存在）以及 `PaymentServiceImpl.registerPayment` 的回写逻辑。

---

## 六、权限配置

### 6.1 当前权限缺口

代码确认：

- `BaseDatabaseInitializer.insertRolePermissions()` 只为 `ROLE_OWNER`、`ROLE_ADMIN`、`ROLE_PURCHASE_MANAGER`、`ROLE_DEPARTMENT_MANAGER`、`ROLE_WAREHOUSE_MANAGER`、`ROLE_STORE_MANAGER`、`ROLE_EMPLOYEE` 分配了权限。
- **`ROLE_FINANCE_MANAGER` 和 `ROLE_FINANCE_DIRECTOR` 未分配任何权限**。
- 真实验收环境创建的 `emp-c`（财务经理）因此无法访问任何财务接口。

### 6.2 M5 需补充的权限映射

在 `BaseDatabaseInitializer.insertRolePermissions()` 中为财务角色新增：

```java
rolePermissionMap.put("ROLE_FINANCE_DIRECTOR", Arrays.asList(
    // 采购结算
    "PERM_PURCHASE_ORDER_VIEW", "PERM_PURCHASE_ARRIVAL_VIEW",
    "purchase:settlement:view", "purchase:settlement:create", "purchase:settlement:edit",
    "purchase:settlement:delete", "purchase:settlement:approve",
    // 应付/付款
    "finance:payable:create", "finance:payable:query", "finance:payable:update",
    "finance:payable:delete", "finance:payable:approve",
    "finance:payment:create", "finance:payment:query", "finance:payment:edit",
    "finance:payment:delete", "finance:payment:void",
    // 银行账户/资金流水/凭证查看
    "finance:bank:query", "finance:record:view", "finance:voucher:query",
    "finance:approval:view", "finance:approval:approve"
));

rolePermissionMap.put("ROLE_FINANCE_MANAGER", Arrays.asList(
    // 采购结算（可审批付款）
    "PERM_PURCHASE_ORDER_VIEW", "PERM_PURCHASE_ARRIVAL_VIEW",
    "purchase:settlement:view", "purchase:settlement:create", "purchase:settlement:edit",
    "purchase:settlement:approve",
    // 应付/付款
    "finance:payable:query", "finance:payable:approve",
    "finance:payment:create", "finance:payment:query",
    "finance:bank:query", "finance:record:view", "finance:voucher:query"
));
```

### 6.3 接口权限注解调整

| 文件 | 当前注解 | 建议调整 |
|------|----------|----------|
| `PaymentController.java:33` | 无 | `@PreAuthorize("hasAuthority('finance:payment:create') or hasAuthority('finance:payable:approve')")` |
| `PaymentController.java:40` | 无 | `@PreAuthorize("hasAuthority('finance:payment:query')")` |
| `PaymentController.java:47` | 无 | `@PreAuthorize("hasAuthority('finance:payment:query')")` |

> 注意：`AdminPermissions.ALL` 中未包含 `finance:payment:create/query/edit/delete/void`，若使用 `hasAuthority` 展开逻辑，需同步将这些权限码加入 `AdminPermissions.ALL`。

---

## 七、验收标准

### 7.1 功能验收

| 编号 | 验收项 | 通过标准 |
|------|--------|----------|
| M5-01 | 采购结算单自动生成 | 采购订单确认下单后，`purchase_settlements` 表出现一条与该 `order_id` 关联的待结算记录，金额与订单一致 |
| M5-02 | 手动创建结算单 | 有权限用户可通过 `/v1/purchase/settlements` 手动创建结算单，编号自动生成 |
| M5-03 | 财务经理查看列表 | `emp-c`（财务经理）登录后可查看采购结算列表与应付账款列表，无 403 |
| M5-04 | 财务经理审批付款 | 财务经理对结算单调用 `/settle?action=pay&voucherNo=xxx` 后，结算单状态变为 `finance_reviewing` 或 `completed`，`paid_amount = total_amount` |
| M5-05 | 付款登记四账联动 | 通过 `/v1/finance/payments` 登记付款后，`payables.balance_amount` 减少、`bank_accounts.balance` 减少、`fund_flows` 与 `finance_vouchers` 各新增一条记录 |
| M5-06 | 付款后结算单回写 | 付款完成后，对应采购结算单状态同步更新为 `completed`（或实现双向状态一致） |

### 7.2 权限验收

| 编号 | 验收项 | 通过标准 |
|------|--------|----------|
| M5-P01 | 财务经理权限 | `emp-c` 拥有 `finance:payable:query/approve`、`finance:payment:create/query`、`purchase:settlement:view/create/edit/approve` |
| M5-P02 | 财务总监权限 | 财务总监拥有财务经理全部权限，并额外拥有 `finance:voucher:query`、`finance:approval:view/approve` |
| M5-P03 | 普通员工隔离 | 普通员工无法查看/操作采购结算与应付账款接口 |

### 7.3 回归验收

- 采购-仓储-库存主链路（M1-M4）全部 P0 用例仍通过。
- 无 500 错误，核心接口响应 < 1s。
- 后端 `mvn compile` 与前段 `npm run build` 均通过。

---

## 八、风险点与应对

| 风险 | 影响 | 应对措施 |
|------|------|----------|
| `finance_manager` / `finance_director` 角色无权限 | **高** | M5 首要任务：在 `BaseDatabaseInitializer.insertRolePermissions()` 中补充权限映射 |
| `PaymentController` 登记付款无权限注解 | **高** | 补充 `@PreAuthorize`，并同步更新 `AdminPermissions.ALL` |
| 采购订单确认时未生成结算单 | **高** | 在 `PurchaseOrderServiceImpl.confirmOrder` 中同事务调用 `PurchaseSettlementService.createSettlement`，使用 `orderId` 做幂等 |
| 采购结算单与应付账款/付款单状态不同步 | **中** | 在 `PaymentServiceImpl.registerPayment` 成功后，根据 `payable.order_id` 反向更新 `purchase_settlements`；或在前端分两步调用 |
| 金额单位混淆（元/分） | **中** | 前端使用 `DataConverter` 转换；后端统一以分为单位存储；验收时核对数据库实际值 |
| 付款金额超过银行账户余额 | **低** | 现有 `PaymentServiceImpl.registerPayment` 已做余额校验，无需额外改造 |
| 部分付款与结算单状态机冲突 | **低** | M5 最小范围建议先支持全额付款；部分付款场景延后处理 |

---

## 九、任务拆分与排期（建议 2-3 天）

| 编号 | 任务 | 预计工时 | 依赖 |
|------|------|----------|------|
| M5-T01 | 为 `finance_manager` / `finance_director` 分配权限，并验证 `emp-c` 可登录访问财务菜单 | 0.5 天 | 无 |
| M5-T02 | 为 `PaymentController` 补充权限注解，并同步 `AdminPermissions.ALL` | 0.5 天 | M5-T01 |
| M5-T03 | 实现采购订单确认后自动生成采购结算单（含幂等） | 0.5 天 | 无 |
| M5-T04 | 实现付款完成后回写采购结算单状态 | 0.5 天 | M5-T03 |
| M5-T05 | 前端对接：采购结算列表、详情、付款确认按钮 | 0.5 天 | M5-T01 ~ M5-T04 |
| M5-T06 | 端到端验收：从采购订单确认 → 结算单生成 → 付款 → 四账联动 | 0.5 天 | M5-T01 ~ M5-T05 |

---

## 十、附录：关键代码位置

| 文件 | 作用 |
|------|------|
| `backend/src/main/java/com/foodtraceability/controller/purchase/PurchaseSettlementController.java` | 采购结算单接口 |
| `backend/src/main/java/com/foodtraceability/service/impl/PurchaseSettlementServiceImpl.java` | 采购结算单业务逻辑 |
| `backend/src/main/java/com/foodtraceability/controller/finance/PayableController.java` | 应付账款接口 |
| `backend/src/main/java/com/foodtraceability/controller/finance/PaymentController.java` | 付款单接口 |
| `backend/src/main/java/com/foodtraceability/service/finance/impl/PaymentServiceImpl.java` | 四账联动实现 |
| `backend/src/main/java/com/foodtraceability/service/impl/PurchaseOrderServiceImpl.java` | 采购订单确认逻辑 |
| `backend/src/main/java/com/foodtraceability/config/initializer/BaseDatabaseInitializer.java` | 角色权限种子数据 |
| `backend/src/main/java/com/foodtraceability/security/constants/AdminPermissions.java` | admin 权限通配展开集合 |
| `frontend/src/api/purchase/settlement.ts` | 前端采购结算 API |
| `frontend/src/api/finance/payable.ts` | 前端应付账款 API |
| `frontend/src/api/finance/payment.ts` | 前端付款单 API |
