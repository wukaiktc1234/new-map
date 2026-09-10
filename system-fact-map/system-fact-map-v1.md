# System AS-IS Fact Map V1（全项目事实地图汇总与冲突对账）

> - 文档编号：FACT-MAP-V1
> - 日期：2026-09-08
> - 输入：V2（全系统逆向建模）+ 四域审查（产品/库存/订单/财务）+ PADR + GIA + CDDR + 跨域冲突汇总 + 产品决策池 + 历史审计（ETM/KL/AUDIT/BFA/OBS）+ DB/API/Event/State/Permission/Data Scope 事实
> - 角色：架构总控（会话1）
> - 约束：**只归并，不审计；只确认，不修复；不新增产品规则；不写代码。**
> - 状态字典：VERIFIED（证据确认）/ PARTIAL（部分证据）/ UNVERIFIED（待确认）/ CONFLICT（不同来源矛盾）/ LEGACY（历史遗留不活跃）/ BLOCKED_PRODUCT_DECISION（等待产品决策）

---

## 0. 文档约定

### 0.1 事实分类四层（全文标记责任层）

| 层 | 标记 | 说明 |
|---|---|---|
| DB | `[DB]` | 数据库表/列/索引/migration |
| API | `[API]` | Controller/Service/Mapper 接口 |
| Domain | `[Domain]` | 业务规则/状态机/跨域事件/生命周期 |
| UI | `[UI]` | 前端页面/路由/组件/菜单/角色可见性 |

### 0.2 来源缩写（全文引用）

| 缩写 | 来源 |
|---|---|
| V2 | `docs/product/product-architecture-map-v2.md`（1093 行，14 章） |
| INV | `docs/product/product-domain-review-inventory.md`（213 行） |
| ORD | `docs/product/product-domain-review-order.md`（220 行） |
| FIN | `docs/product/product-domain-review-finance.md`（238 行） |
| PC | `docs/product/product-domain-review-product.md`（179 行） |
| CDDR | `docs/product/cross-domain-design-review.md`（260 行） |
| GCDAR | `docs/product/cross-domain-architecture-review.md`（160 行） |
| PADR | `docs/product/product-architecture-decision-record.md`（195 行） |
| CONFLICT | `docs/product/cross-domain-conflict-summary.md`（55 行） |
| PD | `product-decision-backlog.md`（179 行，PD-001~039） |
| ETM | `docs/quality/entity-table-mapping-audit.md`（107 条） |
| KL | `production-known-limitations.md`（KL-001~055+） |
| AUDIT | `production-audit-report.md`（前端 664 文件） |
| BFA | `docs/design/business-flow-analysis.md` |

---

## 1. Business Object Truth Map（业务对象真相图）

> 方法：从 DB 表 + 后端实体 + API + 前端页面反向发现，标记是否为真相源。

### 1.1 核心业务对象（真相源已裁决 = VERIFIED）

| 对象 | 真相源 | 证据 | 遗留/待迁移 | PADR 裁决 |
|---|---|---|---|---|
| 菜品 | `foods`（BIGINT food_id） | V2 §1.A [E1]；PC 审查 | `food` 旧表（VARCHAR food_code，POS 消费） | PADR §九-A：**foods 为真** |
| 物料 | `material_archives`（BIGINT material_id） | V2 §1.A [E1] | `product` 表（只读遗留，无写入方） | PADR §九-A：**material_id 为真相语义** |
| 库存（仓） | `inventory`（inventory_id） | V2 §1.B [E1]；INV 审查 | product_id 列（保留不删，ETM-106 收敛） | 语义真相=物料×位置 |
| 库存（店） | `store_inventory`（store_id+material_id UK） | V2 §1.B [E1]；INV 审查 | product_id 列（列名漂移，值为 material_id） | 语义真相=物料×位置 |
| 订单 | `orders`（BIGINT，分） | V2 §1.B [E1]；ORD 审查 | `orders_legacy`（DECIMAL 元，POS 历史）；`sales_order` 实体无表 | PADR §九-A：**orders 为真** |
| 凭证 | `finance_vouchers`（新体系） | FIN 审查 | `voucher_header`（死体系，无业务调用）；根包 stub（假链路） | 3 通道为真 |
| 应付 | `payables`（幂等 AP） | FIN 审查 [E1] | — | — |
| 应收 | `receivables`（AR+orderNo） | FIN 审查 [E1] | `receivable`（根包双轨，ETM-057） | — |
| 科目 | `accounting_subjects` | FIN 审查 [E1] | `accounting_subject`（单复数并存） | — |
| 物料追溯 | `material_trace_code` | V2 §1.B [E1] | `inventory.batch_no`（非独立对象） | INV §1.1：批次=属性 |
| 财务流水 | `finance_records`（分） | FIN 审查 [E1] | POS 元 vs 新表分 | Level 2 当前真相 |
| 资金流水 | `fund_flows` | FIN 审查 [E1] | **无 status 字段**（PD-028 已决：增 status，同 Batch0-方案2 批 C） | — |
| 收款 | `receipt`（四账联动） | FIN 审查 [E1] | — | — |
| 付款 | `payment`（四账联动） | FIN 审查 [E1] | — | — |
| 税 | `tax_record`（元） | FIN 审查 [E1] | — | PD-001 缴税幂等已决 |
| 报销 | `invoice_reimbursement` | FIN 审查 [E1] | 三表无 migration（ETM-018~020） | — |

### 1.2 状态/属性类对象（非独立）

| 对象 | 判定 | 证据 | 说明 |
|---|---|---|---|
| 菜品分类 | **属性**（food_categories，非独立业务对象） | PC 审查 | 并入菜品对象详情 |
| 定价 | **属性**（分散在 foods.sale_price / product_pricing_history / material_archives.reference_price） | V2 §1.A | 无通用 price 表，属性分散 |
| 成本 | **属性**（unit_cost 分，最新入库价法；注释与实现不符） | INV/ORD/FIN 审查 | PD-032 已决：最新入库价=当前口径 |
| 订单状态 | **属性**（order_status 0-6 / payment_status 0-3） | ORD §2.2 | — |
| 预警 | **派生监控**（规则+调度生成） | INV §1.1 | 非独立对象 |
| 盘点差异 | **派生记录**（不改库存） | INV §1.1 | 需手工建调整单（PD-030 拟改为自动/确认后） |

### 1.3 死体系/假链路（LEGACY）

| 对象 | 判定 | 证据 |
|---|---|---|
| `voucher_header` | **死体系**（无业务调用，仅红冲复制） | FIN §1.1 |
| 根包 `FinanceVoucherService` | **假链路**（全 no-op stub，SalesOrderServiceImpl 调用但不落库） | FIN §1.1 |
| `food`（旧菜品表） | **遗留**（POS 扫码仍在消费） | V2 §1.A [E1]/[E4]；ORD §3 |
| `product`（遗留产品表） | **只读遗留**（无写入方） | V2 §1.A [E4] |
| `sales_order` 实体 | **虚实体**（无表，String 状态无映射） | ORD §1.1；PD-015 |

---

## 2. DB Truth Map（数据库真相图）

### 2.1 核心表映射（已核实 = VERIFIED）

| 域 | 表（真相源） | 实体 | 迁移 | 状态 |
|---|---|---|---|---|
| 产品 | `foods` / `food_categories` / `dish_recipes` / `dish_combos` / `combo_ingredients` | FoodNew / DishRecipe / DishCombo | V6.0.0 | VERIFIED |
| 产品（遗留） | `food` / `product` | Food / Product | V1.0.0.100 | LEGACY |
| 物料 | `material_archives` / `material_categories` | MaterialArchive | V20260629_004/005 | VERIFIED |
| 库存 | `inventory` / `store_inventory` | Inventory / StoreInventory | V6.0.0 / V20260817_001 | VERIFIED（ETM-106 列漂移） |
| 订单 | `orders` / `order_items` / `order_payment_records` | OrderNew / OrderItem | V6.0.0 | VERIFIED |
| 订单（遗留） | `orders_legacy` / `order_items_legacy` | — | V1.0.0.100 | LEGACY |
| 财务-应付 | `payables` / `payment` / `bank_accounts` | Payable / Payment / BankAccount | V20260718_001 / V20260625_001 | VERIFIED |
| 财务-应收 | `receivables` / `receipt` | Receivable / Receipt | finance_tables.sql | VERIFIED |
| 财务-凭证 | `finance_vouchers` / `accounting_subjects` / `accounting_periods` | FinanceVoucher / AccountingSubject | V20260718_001 | VERIFIED |
| 财务-遗留 | `voucher_header` / `voucher_line` / `voucher_idempotent` | Voucher | V20260405:390 | LEGACY |
| 财务-余额 | `account_balance` | AccountBalance | — | CONFLICT（元口径，人工刷新，PD-031 已决冻结停用） |
| 财务-流水 | `fund_flows` / `finance_records` | FundFlow / FinanceRecord | — | VERIFIED（fund_flows 无 status，PD-028 已决补 status） |
| 财务-税 | `tax_record` / `tax_rate_configs` | TaxRecord / TaxRateConfig | V20260811_001 | VERIFIED |
| 采购 | `purchase_orders` / `purchase_arrivals` / `receipt_confirmations` / `purchase_stockins` / `purchase_returns` | PurchaseOrder / PurchaseArrival / ReceiptConfirmation | 多版本 | VERIFIED |
| 门店 | `stores` / `stores_new` | StoreNew | V20260707_002 / V9.0.0 | CONFLICT（双表并存） |
| 权限 | `roles` / `permissions` / `role_permissions` / `user_roles` | Role / Permission | V1.0.0.100 | VERIFIED |
| 权限（无 migration） | `role_stores` / `role_departments` | RoleStore / RoleDepartment | — | UNVERIFIED（ETM-044/045） |

### 2.2 迁移覆盖缺口（ETM 标注 = PARTIAL/UNVERIFIED）

| 表 | 判定 | 说明 |
|---|---|---|
| `inventory_category` / `inventory_unit` | **无 migration 建表** | ETM-033/034 |
| `supplier_evaluations` | **无 migration 建表** | ETM-050 |
| `purchase_plan_item` / `plan_item` | **无 migration 建表** | ETM-049/024 |
| `requisition_requests` / `travel_requests` / `swap_request_details` | **无 migration 建表** | ETM-021/022/023 |
| `invoice_reimbursement` + 2 表 | **无 migration 建表** | ETM-018~020 |
| `receivables`（非 migration） | **仅 finance_tables.sql** | ETM-057 |

### 2.3 双口径（CONFLICT）

| 项 | 口径 A | 口径 B | 判定 |
|---|---|---|---|
| 金额单位 | 分（新表/库存/财务主表） | 元（legacy/tax_record/account_balance） | CONFLICT（系统性双口径，PD-031/032 已部分裁决） |
| product_id vs material_id | material_id（真相语义） | product_id（遗留列名/别名穿透） | CONFLICT（G2 双口径，ETM-106 收敛） |
| 科目余额 | accounting_subjects.balance（分，自动） | account_balance（元，人工） | CONFLICT（PD-031 已决：以 accounting_subjects 为准，account_balance 冻结停用） |
| stores vs stores_new | stores_new（新真相） | stores（旧） | CONFLICT（V20260723_001 已重指） |

---

## 3. API Truth Map（API 真相图）

### 3.1 核心 API 链路（已证实 = VERIFIED）

| 域 | API 链路 | 写入表 | 读取 | 审计 |
|---|---|---|---|---|
| 产品 | /v1/product-center/foods | foods | FoodNew→foods | ⚠️ 发布动作无 @AuditLog |
| 库存 | /v1/inventory/* + /v1/store-inventory | inventory / store_inventory | ✅ | transactions 只写不读 |
| 订单（新） | /v1/orders | orders / order_items | ✅ | sys_operation_logs |
| 订单（遗留） | /v1/pos/orders/* | orders_legacy / food / foods | ✅ | — |
| 财务-应付 | /v1/finance/payables | payables | ✅ | 可追溯（stockin_id） |
| 财务-付款 | /v1/finance/payments | payment + 凭证 + fund_flow + 应付核销 | ✅ | 四账联动 |
| 财务-收款 | /v1/finance/receipts | receipt + 凭证 + fund_flow + 应收核销 | ✅ | 四账联动 |
| 财务-凭证 | /v1/finance/vouchers | finance_vouchers | ✅ | 3 通道写入 |

### 3.2 零消费/断链 API（E2/E4 = UNVERIFIED/LEGACY）

| API | 状态 | 说明 |
|---|---|---|
| /v1/self-purchase/*（6 方法） | **无前端消费**（PD-006） | 权限归属未定义 |
| /v1/tasks/*（12 方法） | **无前端消费**（PD-006） | 权限归属未定义 |
| /v1/plan-items/*（7 方法） | **无前端消费**（PD-006） | 权限归属未定义 |
| /v1/appeals/*（5 方法） | **无前端消费**（PD-007） | 域归属待确认 |
| AutoVoucherService.generateSalesVoucher | **零调用点** | 凭证级销售链路未接线 |
| AutoVoucherService.generatePurchaseVoucher | **零调用点** | 凭证级采购链路未接线 |
| FinanceVoucherService（根包） | **no-op stub** | SalesOrderServiceImpl 调用但不落库 |
| updateBalancesFromVoucher | **无外部调用方** | VoucherPostedEvent 无发布方/监听器 |

---

## 4. Event / Side Effect Map（事件/副作用图）

> 来源：CDDR §二（核心事件链）+ 四域审查（事件证据）。

### 4.1 业务事件

| 事件 | 产生域 | 责任域 | 消费域 | 幂等 | 审计 | 失败处理 | 状态 |
|---|---|---|---|---|---|---|---|
| 订单创建（POS） | 订单 | 订单 | 库存（锁/扣） | 创建幂等 | sys_operation_logs | food 失败回滚；foods 失败容忍 | VERIFIED |
| 订单创建（管理端） | 订单 | 订单 | — | 创建幂等 | sys_operation_logs | 取消（0/1，已支付未退款 TODO） | VERIFIED |
| 订单完成 | 订单 | 订单+库存+财务 | 库存（BOM 扣）+ 财务（收入/应收） | 事务幂等 | ⚠️ | 物料不足整单回滚 | VERIFIED |
| 支付成功 | 订单 | 订单 | 财务（流水） | CAS 幂等 | payment_records | CAS 重试容错 | VERIFIED |
| 采购入库 | 采购 | 库存+财务 | 库存+应付 | 幂等 AP | 单据审计 | 事务回滚 | VERIFIED |
| 收货确认 | 采购 | 库存+财务 | 库存+应付 | 幂等 AP | 单据审计 | 事务回滚 | VERIFIED |
| 退款 | 订单 | 订单+财务 | 财务（流水）+ 库存（回补） | — | refund_records | 取消已支付=仅 log TODO（#3 演进） | PARTIAL（反向链空白） |
| 缴税 | 财务 | 财务 | 税 | 幂等（PD-001） | — | — | VERIFIED |
| 日结/工资/报销事件 | 门店/HR | 财务 | 凭证（3 通道之一） | — | — | — | VERIFIED |

### 4.2 事件断链/空白

| 事件 | 状态 | 说明 |
|---|---|---|
| 发布动作 | **无审计** | AuditLogAspect=@annotation，发布方法未标注 |
| createTableOrder | **不发事件** | 仅 WebSocket，无 Spring Event |
| 取消已支付 | **仅 log TODO 退款** | KL-053 家族（PD-033 待决） |
| 销售凭证 | **generateSalesVoucher 零调用** | 凭证级销售链路未接线（PADR-005 Level 3 演进） |
| 采购凭证 | **generatePurchaseVoucher 零调用** | 凭证级采购链路未接线 |
| 收款冲正 | **无端点** | PD-033 待决 |

---

## 5. State Machine Map（状态机图）

### 5.1 订单状态机（三套并存 = CONFLICT）

| 模型 | 状态值 | 表 | 判定 |
|---|---|---|---|
| 新表 order_status | 0待确认→1已确认→2已完成→3已取消；4部分退款 5全额退款 6待评价 | orders | VERIFIED |
| 新表 payment_status | 0未支付 1部分 2已支付 3已退款 | orders | VERIFIED |
| legacy status | -1支付中 0待支付 1已支付 2待配送 3配送中 4已完成 5已取消 6退款中 7已退款 | orders_legacy | VERIFIED |
| sales_order | pending/preparing/completed/delivered（String，与 order_status 无映射） | 无表（虚实体） | LEGACY（PD-015 已决两阶段迁移） |

### 5.2 凭证状态机

```
DRAFT(0) → APPROVED(1) → POSTED(2) → VOID(3)
POSTED → unpost（反过账：反向更新 accounting_subjects.balance）
```
- 前端映射错位：后端 0-3 vs 前端 VoucherStatusMap 1-4（Batch0-方案3 已列，P1-UI-FIN-002 待执行）
- account_balance（元，人工）：无自动联动（PD-031 已决冻结停用）

### 5.3 库存单据状态机

| 单据 | 状态 | 证据 |
|---|---|---|
| 调拨 | 0草稿→1待审→2已批→3完成/4取消 | INV §1.1 |
| 盘点 | 0待盘→1盘点中→2已审→3完成 | INV §1.1 |
| 调整 | pending→approved→completed | INV §1.1 |
| 报损 | 审批+处理 | INV §1.1 |
| 出库 | 审批+执行 | INV §1.1 |

### 5.4 采购链状态

| 单据 | 状态 | 证据 |
|---|---|---|
| 采购申请 | 0草稿→1待审→2已批→3已取消 | V2 |
| 采购订单 | 0待确认→1已确认→2已完成→3已取消 | V2 |
| 收货确认 | 0待收→1已收→2已拒 | V2 |

### 5.5 CAS 支付状态机

```
POS: 0(待支付) → -1(处理中/冲突) → 1(已支付)
     -1 冲突=幂等容错（重试或恢复 0）
```
- LEGACY: status -1=支付中（orders_legacy 独有）

---

## 6. Permission / Data Scope Map（权限/数据范围图）

### 6.1 权限基础设施（VERIFIED）

| 层 | 事实 | 证据 | 状态 |
|---|---|---|---|
| 角色（roles） | 16 角色（部署基线） | V2 §1.A [E1]；ETM | VERIFIED |
| 权限（permissions） | @PreAuthorize('domain:action') 模型 | AUDIT §3 | VERIFIED |
| 域可见性 | DOMAIN_VISIBLE_ROLES 前端路由守卫 | AUDIT §3.5 | VERIFIED |
| 员工可见性 | employee 仅 workspace/store-ops 放行 | AUDIT §3.5 | VERIFIED |
| 权限模板 | permission_templates（@Table） | V2 §1.A [E1] | VERIFIED |
| 用户覆盖 | user_permission_overrides | V2 §1.A [E1] | VERIFIED |

### 6.2 数据范围（Data Scope）

| 范围 | 实现 | 证据 | 状态 |
|---|---|---|---|
| store_id 绑定 | users 表 store_id 列（add_store_id_to_users migration） | V2 §0.4 ⑥ [E3] | VERIFIED（隐式） |
| role_stores | **无 migration 建表** | ETM-044 | UNVERIFIED |
| role_departments | **无 migration 建表** | ETM-045 | UNVERIFIED |
| 门店经营范围 | **不存在**（全局统一菜单，无 storeId 作用域） | V2 §13.9；PC 审查；PD-022 已决 | 不存在（演进 PADR-002） |

### 6.3 权限缺口（BLOCKED_PRODUCT_DECISION）

| 项 | 状态 | 说明 |
|---|---|---|
| 裸接口权限（25 方法） | PD-006 待产品 | SelfPurchase/Task/PlanItem |
| 申诉接口域归属（5 方法） | PD-007 待产品 | 域归属+受理角色 |
| 供应商 H5 外部认证（6 端点） | PD-008 待产品 | 外部身份域 |
| 通用审批权限模型（13 方法） | PD-009 待产品 | 审批归属+权限模型 |
| 退款审批身份归因 | PD-010 待产品 | approveUserId 客户端提交 |
| 发布动作审计 | **无 @AuditLog** | 审计缺口（非权限，但归属审计层） |

---

## 7. Management Frontend Entry Map（管理端前端入口图）

> 方法：从前端路由/菜单组件反向发现，与 UTM 任务 ID 对齐。

### 7.1 域级路由入口

| 域 | 路由前缀 | 页面数 | 证据 |
|---|---|---|---|
| 产品 | /product-center/* | 5（菜品/套餐/分类/定价/成本） | V2 §2.A；FIA §二 |
| 库存 | /warehouse/* + /store-management/store-inventory | 12+ | V2 §2.B；INV §3.1 |
| 订单 | /order/* | 4（查询/统计/退款/预约） | V2 §2.B；ORD §3.1 |
| 财务 | /finance/* | 9（应付/付款/应收/收款/凭证/科目/期间/缴税/资金流水） | V2 §2.B；FIN §3 |
| 采购 | /purchase/* | 14 | V2 §2.B |
| 门店运营 | /store-management/* | 9 | V2 §2.B |
| HR | /hr/* | 多页 | V2 §2.A |
| 组织 | /system/* | 权限/角色/组织 | V2 §2.A |

### 7.2 概念重叠/职责冲突（CONFLICT）

| 页名 | 冲突 | 判定 |
|---|---|---|
| 「门店库存」两页 | warehouse/store-inventory（仓库维度）vs store-ops/store-inventory（门店维度）同名不同对象 | CONFLICT（INV §3.1） |
| 入库/收货 5 入口 | warehouse/store-ops/purchase/mobile/首页 同一底层操作跨 4 菜单 | CONFLICT（INV §3.2） |
| 库存页面承担跨域职责 | categoryApi（产品域）/supplierApi（采购域）/employeeApi（HR 域）出现在库存页 | CONFLICT（INV §3.2） |

### 7.3 页面层级标注

按 FIA（最终信息架构），当前导航骨架：
- 主工作台（门店经营/区域管理 EVOLUTION/集团治理）
- 域级入口（产品/库存/订单/财务/采购/组织）
- 区域管理=EVOLUTION（大型连锁阶段启用）

---

## 8. Legacy Dependency Map（遗留依赖图）

### 8.1 遗留对象依赖关系

| 遗留对象 | 被谁依赖 | 依赖类型 | 迁移方案 |
|---|---|---|---|
| `food`（旧菜品表） | POS 扫码链路（/v1/pos/api/*）；下单双写 food+foods；退款回补 food+foods；超时回补 | API+写入+迁移 | G1 POS 双源统一（Batch0-方案1） |
| `orders_legacy` | POS 收银/扫码/支付/退款；超时任务；POS 支付流水 finance_record | API+写入 | PD-015 两阶段迁移 |
| `voucher_header` | 根包 stub（SalesOrderServiceImpl 调用但不落库） | 假链路 | 冻结停用（不删除） |
| `product`（遗留） | inventory.product_id FK（值为 material_id） | FK 引用 | ETM-106 保留不删，语义收敛 |
| `sales_order` 实体 | SalesOrderServiceImpl（映射 orders 表） | 实体映射 | PD-015 两阶段（阶段一禁新增引用） |
| `stores`（旧） | stores_new 已重指（V20260723_001） | 双表 | 已部分迁移 |

### 8.2 双写/一致性缺口

| 缺口 | 详情 | 状态 |
|---|---|---|
| food+foods 双写 | 下单/退款/超时同时扣/回补两表；启动同步 foods→food | CONFLICT（Batch0-方案1） |
| inventory+store_inventory 双写 | 采购入库/调拨/收货按类型双写 | VERIFIED（INV §1.2） |
| 成本双写 | persistOrderCost（主事务）+ recordOrderCost（异步）同事件 | CONFLICT（待确认） |
| finance_record 口径 | 注释分实元；POS 写入源为元 BigDecimal | CONFLICT（Batch0-方案2 批 B） |

---

## 9. Conflict Map（冲突图——提取自跨域冲突汇总）

> 完整冲突点见 `cross-domain-conflict-summary.md`（12 点）。本图提取状态分布。

### 9.1 裁决已定（VERIFIED/CONFLICT resolved）

| # | 冲突 | 结论 | PADR/决策 |
|---|---|---|---|
| 1 | 可售口径 | POS 可售=菜品层 foods.stock；物料库存不参与 | PADR-003（D8） |
| 2 | 记账口径 | 双口径均流水级；凭证=Level 3 演进 | PADR-005（Level 2） |
| 4 | 成本口径 | 分口径一致；双写待确认 | VERIFIED |
| 5 | 订单成立 | 成立=写行 status=0；三套状态模型 | PD-015 两阶段 |
| 6 | 应付触发 | 同事务幂等；链路完整 | VERIFIED |
| 8 | 门店经营范围 | 库存≠经营范围；不把库存当替代物 | PADR-002（D2） |
| 11 | 订单状态机 | 三套并存 | PD-015/017~021 |

### 9.2 需产品决策（BLOCKED_PRODUCT_DECISION）

| # | 冲突 | 决策方 | 决策状态 |
|---|---|---|---|
| 7 | 反向链空白（取消退款/凭证红冲/收款冲正） | 产品+财务 | PD-033 待决 |
| 10 | 科目余额双轨 | 财务 | PD-031 已决（accounting_subjects 为准，account_balance 冻结） |

### 9.3 技术整改（与产品决策隔离）

| # | 冲突 | 方案 | 状态 |
|---|---|---|---|
| 3 | 金额单位双轨 | Batch0-方案2 批 A/B/C/D | 批 A/B 已列 |
| 9 | POS 双源 | Batch0-方案1 G1 | 待执行 |
| 12 | 凭证状态映射 | Batch0-方案3 | P1-UI-FIN-002 待执行 |

---

## 10. Product Decision Dependency Map（产品决策依赖图）

### 10.1 决策池状态分布

| 状态 | 数量 | 关键项 |
|---|---|---|
| **已决策** | 16 | PD-001~004/011/014~016/022~032/036~037 |
| **待产品** | 7 | PD-005/006/007/008/009/010/033/034/035 |
| **待评估** | 2 | PD-012/013 |

### 10.2 关键决策依赖链

```
PD-022(千店千面) → PD-024(门店经营范围 PADR-002) → D2 实现方案（演进）
                → PD-025(售罄与库存分离 PADR-003) → D8 实现方案（演进）
PD-005/PADR-005(Level 2) → Level 3 复评（大型连锁阶段）
                        → 销售凭证链路（#2 相关）= 演进
PD-030(扣减分层) → P1-STOCK-001 架构级单独立项（跨域）
PD-031(科目余额) → account_balance 冻结 → 数据血缘专项
PD-032(计价方法) → 最新入库价为当前口径 → 移动加权平均=Level 3
PD-033(收款冲正) → 决策前不猜测实现
PD-028(fund_flows status) → 同 Batch0-方案2 批 C 落库
```

### 10.3 BLOCKED 项清单（不纳入 Sprint 通过数）

| PD | 说明 | 状态 |
|---|---|---|
| PD-006 | 裸接口权限归属（25 方法） | 待产品 |
| PD-007 | 申诉域归属（5 方法） | 待产品 |
| PD-008 | 供应商 H5 外部认证 | 待产品 |
| PD-009 | 通用审批权限模型 | 待产品 |
| PD-010 | 退款审批身份归因 | 待产品 |
| PD-012 | 自动签名存证 | 待评估 |
| PD-013 | APK 分发模式 | 待评估 |
| PD-033 | 收款冲正 | 待产品 |
| PD-034 | 预算审批流归属 | 待产品 |
| PD-035 | 导出规则 | 待产品 |

---

## 11. UI-AS-IS-001 五项新发现评判

> 每项判断：是真实新问题？已有问题的另一个表现？产品决策冲突？legacy？regression？应由哪一层负责？

### 11.1 锚点 ①：今日销售概览失败

- **判定**：**已有问题的另一个表现**（非新问题）。
- **证据**：KL-054 已记录 `SalesOrder` 表不存在；DashboardServiceImpl.getTodaySalesOverview → BadSqlGrammarException → catch 降级。OIC-BE 修复后（72f0d5a）已改 code:500 明确错误态（[OBS] R-4）。
- **责任层**：**Domain**（PD-015 SalesOrder 与 OrderNew 同表合并——已决策两阶段方案；Dashboard 需读 orders 而非 sales_order）。
- **分类**：已有问题 + 架构遗留（sales_order 虚实体无表）。

### 11.2 锚点 ②：采购订单无收货仓库选择

- **判定**：**真实新问题**（[E2→E4]——DB 有 warehouse_id 列，UI 无字段）。
- **证据**：PurchaseOrder.vue 表单无 warehouse 字段；DB 有 warehouse_id 列（V20260731_003 补）；收货确认侧用 row.storeId||row.warehouseId 显示。
- **责任层**：**Domain**（业务规则未定义采购阶段是否定仓）+ **UI**（字段缺失）。
- **分类**：已有 DB 列+缺失 UI 字段 → 业务规则缺失（PD 候选，V2 锚点② 已标注）。**非新问题，V2 已发现但未给责任层判定——本图补全责任层=Domain（业务规则待产品决策）+ UI（字段补建依赖规则）**。

### 11.3 锚点 ③：订单详情「下单」报订单 ID 缺失

- **判定**：**待复核**（代码级定位未完成）。
- **证据**：管理端 OrderQuery.vue 详情抽屉仅「关闭」与打印，未见「下单」按钮。锚点可能来自 POS 下单/预订转单链路。
- **责任层**：**待复核**（若来自 POS 则属于 UI/Domain 交互问题；若来自预订转单则属于 Domain 跨域动作问题）。
- **分类**：锚点待复核（V2 已标注如实登记，不猜）。

### 11.4 锚点 ④：财务应付账款银行卡显示「？？」

- **判定**：**已有问题的另一个表现**（字段映射兜底）。
- **证据**：FinancePayable.vue 状态映射兜底为 label:'未知'；PaymentHistoryDialog.vue bankAccountName||'-'；「？？」为运行时字段映射缺失/未知值渲染。
- **责任层**：**UI**（映射兜底值需优化显示）+ **Domain**（bank_account 数据完整性——bank_accounts 表数据是否齐全待确认）。
- **分类**：字段映射兜底问题（P1-UI-FIN-004 应付列表调整已覆盖异常状态醒目标注）。

### 11.5 锚点 ⑤：商品/供应商/价格基础主数据未充分进入主链

- **判定**：**已有问题的另一个表现**（V2 已发现，产品补充意图重审后修正）。
- **证据**：V2 §13（§13.1~13.7）已将原「三套商品表并存」修正为「合理分层（material_archives 全局物料 + foods 销售菜品）」；实现不足点=POS 双源+下单双写+product 遗留+门店范围缺失。suppliers 已接入采购主链；价格无通用 price 表（分散）。
- **责任层**：**Domain**（商品分层已裁决为合理分层，PADR §九-A；双源/G1 为技术整改 Batch0-方案1）。
- **分类**：已有问题+合理分层+实现不足（非新问题，V2 §13 已完整重审）。

### 11.6 五项总结

| # | 判定 | 责任层 | 是否新问题 |
|---|---|---|---|
| ① 今日销售概览 | 已有问题的另一个表现（KL-054） | Domain（PD-015） | 否 |
| ② 采购无仓库选择 | 真实新问题（E2→E4，V2 已标注） | Domain（业务规则待产品）+ UI | 否（V2 已发现，本图补责任层） |
| ③ 订单详情 ID 缺失 | 待复核 | 待复核 | 待确认 |
| ④ 银行卡「？？」 | 已有问题的另一个表现 | UI（映射兜底）+ Domain（数据完整性） | 否 |
| ⑤ 商品未进主链 | 已有问题的另一个表现（V2 §13 已重审） | Domain（合理分层+技术整改） | 否 |

> **结论：UI-AS-IS-001 五项新发现中无一为全新问题；均已在 V2/域审查/PADR 中有对应事实记录。本图补全了责任层归属（V2 未做责任层判定的部分）。**

---

*本 Fact Map 为唯一 AS-IS 事实底座，所有后续设计/整改/决策以此为据。*
*文档生成：架构总控（会话1）· 2026-09-08 · 来源：V2 + 四域审查 + PADR + GIA + CDDR + 冲突汇总 + 产品决策池 + 历史审计*
