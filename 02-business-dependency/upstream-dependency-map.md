# Upstream 依赖地图 - Food Traceability System

> 版本: 1.0 | 生成日期: 2026-09-09 | 基于: Project Master Map 静态分析

---

## 一、Upstream 依赖追溯总览

| # | 核心业务能力 | Input | Source | Master | Foundation | 追溯深度 |
|---|-------------|-------|--------|--------|------------|----------|
| 1 | Purchase Order | Material + Supplier | material_archives + suppliers | Material + Supplier | ROOT | 2 层 |
| 2 | Order | Food + Store + Customer | foods + stores_new + members | Food + Store | ROOT | 2 层 |
| 3 | Inventory | Material + Warehouse | material_archives + ? | Material | ROOT | 2 层 |
| 4 | StoreInventory | Food + Store | foods + stores_new | Food + Store | ROOT | 2 层 |
| 5 | Voucher | AccountingSubject | accounting_subjects | AccountingSubject | ROOT | 1 层 |
| 6 | Payable | Supplier + Payment | suppliers + payment | Supplier | ROOT | 1 层 |
| 7 | Receivable | Customer + Receipt | members + receipt | Member | ROOT | 1 层 |
| 8 | FinanceRecord | Voucher + AccountingSubject | finance_vouchers + accounting_subjects | Voucher + AccountingSubject | ROOT | 2 层 |
| 9 | FundFlow | BankAccount + Voucher | bank_accounts + finance_vouchers | BankAccount | ROOT | 1 层 |
| 10 | Receipt | Customer + Receivable | members + receivables | Member | ROOT | 2 层 |
| 11 | Payment | Supplier + Payable | suppliers + payables | Supplier | ROOT | 2 层 |
| 12 | TaxRecord | Voucher + AccountingSubject | finance_vouchers + accounting_subjects | Voucher + AccountingSubject | ROOT | 2 层 |
| 13 | MaterialTraceCode | Material + Supplier + Batch | material_archives + suppliers + ? | Material + Supplier | ROOT | 2 层 |
| 14 | InvoiceReimbursement | Employee + Invoice | employees + ? | Employee | ROOT | 1 层 |
| 15 | DishRecipe | Food + Material | foods + material_archives | Food + Material | ROOT | 2 层 |
| 15 | DishCombo | Food | foods | Food | ROOT | 1 层 |

---

## 二、核心业务能力详细追溯

### 1. Purchase Order (采购订单)

#### 追溯链
```
Purchase Order
  └─► Input: Material (物料) + Supplier (供应商)
      └─► Source: material_archives + suppliers
          └─► Master: Material + Supplier
              └─► Foundation: ROOT (无上游依赖)
```

#### 依赖分析

| 维度 | 详情 |
|------|------|
| **核心业务能力** | 采购订单管理 |
| **Input** | Material (物料) + Supplier (供应商) |
| **Source** | material_archives + suppliers |
| **Master** | Material (ROOT Foundation) + Supplier (ROOT Foundation) |
| **Foundation** | ROOT (无上游 Foundation 依赖) |
| **追溯深度** | 2 层 |
| **最终根节点** | Material (ROOT) + Supplier (ROOT) |

#### 完整链路
```
Material (ROOT) ──┐
                  ├──► Purchase Order ──► PurchaseStockin ──► Inventory
Supplier (ROOT) ──┘                                              │
                                                                 ▼
                                                          Payable ──► Payment ──► FundFlow
```

#### Evidence
- db-reality-map.md §6: purchase_orders 表引用 supplier_id, material_id
- business-object-map.md §6-7: Payable.Dependencies 包含 Supplier, Payment
- project-master-registry.yaml §procurement: purchase_orders 索引包含 supplier_id

---

### 2. Order (订单)

#### 追溯链
```
Order
  └─► Input: Food (菜品) + Store (门店) + Customer (客户)
      └─► Source: foods + stores_new + members
          └─► Master: Food + Store + Member
              └─► Foundation: ROOT (Food 依赖 Category, Store 依赖 Organization)
```

#### 依赖分析

| 维度 | 详情 |
|------|------|
| **核心业务能力** | 订单管理 |
| **Input** | Food (菜品) + Store (门店) + Customer (客户) |
| **Source** | foods + stores_new + members |
| **Master** | Food (依赖 Category) + Store (依赖 Organization) + Member |
| **Foundation** | Category (ROOT) + Organization (ROOT) |
| **追溯深度** | 3 层 |
| **最终根节点** | Category (ROOT) + Organization (ROOT) |

#### 完整链路
```
Category (ROOT) ──► Food ──┐
                           ├──► Order ──► FinanceRecord
Organization (ROOT) ──► Store ──┘         │
                                          ▼
Member ──► Order ──► Receivable ──► Receipt ──► FundFlow
```

#### Evidence
- business-object-map.md §5: Order.Dependencies 包含 Food, Store, Customer, Payment
- db-reality-map.md §8: orders 表索引包含 store_id
- truth-conflict-map.md §5: 订单状态机三套并存

---

### 3. Inventory (库存)

#### 追溯链
```
Inventory
  └─► Input: Material (物料) + Warehouse (仓库)
      └─► Source: material_archives + ? (MISSING)
          └─► Master: Material + Warehouse
              └─► Foundation: ROOT (Material 依赖 Category, Warehouse 缺失)
```

#### 依赖分析

| 维度 | 详情 |
|------|------|
| **核心业务能力** | 中央库存管理 |
| **Input** | Material (物料) + Warehouse (仓库) |
| **Source** | material_archives + ? (MISSING) |
| **Master** | Material (依赖 Category) + Warehouse (MISSING FOUNDATION) |
| **Foundation** | Category (ROOT) + Warehouse (MISSING) |
| **追溯深度** | 2 层 |
| **最终根节点** | Category (ROOT) |

#### 完整链路
```
Category (ROOT) ──► Material ──► Inventory ──► Order
                        │
Warehouse (MISSING) ────┘
```

#### Evidence
- db-reality-map.md §5: inventory 表索引包含 material_id, warehouse_id
- project-master-registry.yaml §inventory: inventory 表结构
- foundation-map.md §10: Warehouse 无独立表

---

### 4. StoreInventory (门店库存)

#### 追溯链
```
StoreInventory
  └─► Input: Food (菜品) + Store (门店)
      └─► Source: foods + stores_new
          └─► Master: Food + Store
              └─► Foundation: ROOT (Food 依赖 Category, Store 依赖 Organization)
```

#### 依赖分析

| 维度 | 详情 |
|------|------|
| **核心业务能力** | 门店库存管理 |
| **Input** | Food (菜品) + Store (门店) |
| **Source** | foods + stores_new |
| **Master** | Food (依赖 Category) + Store (依赖 Organization) |
| **Foundation** | Category (ROOT) + Organization (ROOT) |
| **追溯深度** | 3 层 |
| **最终根节点** | Category (ROOT) + Organization (ROOT) |

#### 完整链路
```
Category (ROOT) ──► Food ──┐
                           ├──► StoreInventory ──► Order
Organization (ROOT) ──► Store ──┘
```

#### Evidence
- business-object-map.md §4: StoreInventory.Dependencies 包含 Food, Store
- db-reality-map.md §5: store_inventory 表索引包含 store_id, material_id
- project-master-registry.yaml §inventory: store_inventory 表结构

---

### 5. Voucher (凭证)

#### 追溯链
```
Voucher
  └─► Input: AccountingSubject (会计科目)
      └─► Source: accounting_subjects
          └─► Master: AccountingSubject
              └─► Foundation: ROOT (无上游依赖)
```

#### 依赖分析

| 维度 | 详情 |
|------|------|
| **核心业务能力** | 财务凭证管理 |
| **Input** | AccountingSubject (会计科目) |
| **Source** | accounting_subjects |
| **Master** | AccountingSubject |
| **Foundation** | ROOT (无上游 Foundation 依赖) |
| **追溯深度** | 1 层 |
| **最终根节点** | AccountingSubject (ROOT) |

#### 完整链路
```
AccountingSubject (ROOT) ──► Voucher ──► FinanceRecord
                                        │
                                        ▼
                                   FundFlow ──► Report
```

#### Evidence
- business-object-map.md §6: Voucher.Dependencies 包含 AccountingSubject, VoucherDetail
- db-reality-map.md §7: finance_vouchers 表引用 accounting_subjects
- truth-conflict-map.md §3: 科目余额双轨已裁决

---

### 6. Payable (应付)

#### 追溯链
```
Payable
  └─► Input: Supplier (供应商) + Payment (付款)
      └─► Source: suppliers + payment
          └─► Master: Supplier + Payment
              └─► Foundation: ROOT (Supplier 无上游依赖)
```

#### 依赖分析

| 维度 | 详情 |
|------|------|
| **核心业务能力** | 应付账款管理 |
| **Input** | Supplier (供应商) + Payment (付款) |
| **Source** | suppliers + payment |
| **Master** | Supplier + Payment |
| **Foundation** | ROOT (Supplier 无上游 Foundation 依赖) |
| **追溯深度** | 1 层 |
| **最终根节点** | Supplier (ROOT) |

#### 完整链路
```
Supplier (ROOT) ──► Payable ──► Payment ──► FundFlow
```

#### Evidence
- business-object-map.md §7: Payable.Dependencies 包含 Supplier, Payment, Voucher
- db-reality-map.md §7: payables 表索引包含 supplier_id
- project-master-registry.yaml §finance: payables 表结构

---

### 7. Receivable (应收)

#### 追溯链
```
Receivable
  └─► Input: Customer (客户) + Receipt (收款)
      └─► Source: members + receipt
          └─► Master: Member + Receipt
              └─► Foundation: ROOT (Member 无上游依赖)
```

#### 依赖分析

| 维度 | 详情 |
|------|------|
| **核心业务能力** | 应收账款管理 |
| **Input** | Customer (客户) + Receipt (收款) |
| **Source** | members + receipt |
| **Master** | Member + Receipt |
| **Foundation** | ROOT (Member 无上游 Foundation 依赖) |
| **追溯深度** | 1 层 |
| **最终根节点** | Member (ROOT) |

#### 完整链路
```
Member (ROOT) ──► Receivable ──► Receipt ──► FundFlow
```

#### Evidence
- business-object-map.md §8: Receivable.Dependencies 包含 Customer, Receipt, Voucher
- db-reality-map.md §7: receivables 表索引可能包含 customer_id
- project-master-registry.yaml §finance: receivables 表结构

---

### 8. FinanceRecord (财务流水)

#### 追溯链
```
FinanceRecord
  └─► Input: Voucher (凭证) + AccountingSubject (科目)
      └─► Source: finance_vouchers + accounting_subjects
          └─► Master: Voucher + AccountingSubject
              └─► Foundation: ROOT (Voucher 依赖 AccountingSubject)
```

#### 依赖分析

| 维度 | 详情 |
|------|------|
| **核心业务能力** | 财务流水记录 |
| **Input** | Voucher (凭证) + AccountingSubject (科目) |
| **Source** | finance_vouchers + accounting_subjects |
| **Master** | Voucher (依赖 AccountingSubject) + AccountingSubject |
| **Foundation** | AccountingSubject (ROOT) |
| **追溯深度** | 2 层 |
| **最终根节点** | AccountingSubject (ROOT) |

#### 完整链路
```
AccountingSubject (ROOT) ──► Voucher ──► FinanceRecord
                                        │
                                        ▼
                                   FinanceReport
```

#### Evidence
- business-object-map.md §11: FinanceRecord.Dependencies 包含 Voucher, AccountingSubject
- db-reality-map.md §7: finance_records 表引用 voucher_id, accounting_subject_id
- truth-conflict-map.md §10: finance_record 口径冲突 (分 vs 元)

---

### 9. FundFlow (资金流水)

#### 追溯链
```
FundFlow
  └─► Input: BankAccount (银行账户) + Voucher (凭证)
      └─► Source: bank_accounts + finance_vouchers
          └─► Master: BankAccount + Voucher
              └─► Foundation: ROOT (BankAccount 无上游依赖)
```

#### 依赖分析

| 维度 | 详情 |
|------|------|
| **核心业务能力** | 资金流动记录 |
| **Input** | BankAccount (银行账户) + Voucher (凭证) |
| **Source** | bank_accounts + finance_vouchers |
| **Master** | BankAccount + Voucher |
| **Foundation** | ROOT (BankAccount 无上游 Foundation 依赖) |
| **追溯深度** | 1 层 |
| **最终根节点** | BankAccount (ROOT) |

#### 完整链路
```
BankAccount (ROOT) ──► FundFlow ──► CashFlowReport
```

#### Evidence
- business-object-map.md §12: FundFlow.Dependencies 包含 BankAccount, Voucher
- db-reality-map.md §7: fund_flows 表引用 bank_account_id
- project-master-registry.yaml §finance: fund_flows 表结构

---

### 10. Receipt (收款)

#### 追溯链
```
Receipt
  └─► Input: Customer (客户) + Receivable (应收)
      └─► Source: members + receivables
          └─► Master: Member + Receivable
              └─► Foundation: ROOT (Member 无上游依赖)
```

#### 依赖分析

| 维度 | 详情 |
|------|------|
| **核心业务能力** | 收款记录管理 |
| **Input** | Customer (客户) + Receivable (应收) |
| **Source** | members + receivables |
| **Master** | Member + Receivable |
| **Foundation** | ROOT (Member 无上游 Foundation 依赖) |
| **追溯深度** | 2 层 |
| **最终根节点** | Member (ROOT) |

#### 完整链路
```
Member (ROOT) ──► Receivable ──► Receipt ──► FundFlow
```

#### Evidence
- business-object-map.md §13: Receipt.Dependencies 包含 Customer, Receivable, BankAccount
- db-reality-map.md §7: receipt 表索引包含 receivable_id
- project-master-registry.yaml §finance: receipt 表结构

---

### 11. Payment (付款)

#### 追溯链
```
Payment
  └─► Input: Supplier (供应商) + Payable (应付)
      └─► Source: suppliers + payables
          └─► Master: Supplier + Payable
              └─► Foundation: ROOT (Supplier 无上游依赖)
```

#### 依赖分析

| 维度 | 详情 |
|------|------|
| **核心业务能力** | 付款记录管理 |
| **Input** | Supplier (供应商) + Payable (应付) |
| **Source** | suppliers + payables |
| **Master** | Supplier + Payable |
| **Foundation** | ROOT (Supplier 无上游 Foundation 依赖) |
| **追溯深度** | 2 层 |
| **最终根节点** | Supplier (ROOT) |

#### 完整链路
```
Supplier (ROOT) ──► Payable ──► Payment ──► FundFlow
```

#### Evidence
- business-object-map.md §14: Payment.Dependencies 包含 Supplier, Payable, BankAccount
- db-reality-map.md §7: payment 表索引包含 payable_id
- project-master-registry.yaml §finance: payment 表结构

---

### 12. TaxRecord (税务记录)

#### 追溯链
```
TaxRecord
  └─► Input: Voucher (凭证) + AccountingSubject (科目)
      └─► Source: finance_vouchers + accounting_subjects
          └─► Master: Voucher + AccountingSubject
              └─► Foundation: ROOT (Voucher 依赖 AccountingSubject)
```

#### 依赖分析

| 维度 | 详情 |
|------|------|
| **核心业务能力** | 税务记录管理 |
| **Input** | Voucher (凭证) + AccountingSubject (科目) |
| **Source** | finance_vouchers + accounting_subjects |
| **Master** | Voucher (依赖 AccountingSubject) + AccountingSubject |
| **Foundation** | AccountingSubject (ROOT) |
| **追溯深度** | 2 层 |
| **最终根节点** | AccountingSubject (ROOT) |

#### 完整链路
```
AccountingSubject (ROOT) ──► Voucher ──► TaxRecord ──► TaxDeclaration
```

#### Evidence
- business-object-map.md §15: TaxRecord.Dependencies 包含 Voucher, AccountingSubject
- db-reality-map.md §7: tax_record 表
- truth-conflict-map.md §1: 金额单位冲突 (元 vs 分)

---

### 13. MaterialTraceCode (物料追溯码)

#### 追溯链
```
MaterialTraceCode
  └─► Input: Material (物料) + Supplier (供应商) + Batch (批次)
      └─► Source: material_archives + suppliers + ?
          └─► Master: Material + Supplier
              └─► Foundation: ROOT (Material 依赖 Category, Supplier 无依赖)
```

#### 依赖分析

| 维度 | 详情 |
|------|------|
| **核心业务能力** | 物料追溯码管理 |
| **Input** | Material (物料) + Supplier (供应商) + Batch (批次) |
| **Source** | material_archives + suppliers + ? |
| **Master** | Material (依赖 Category) + Supplier |
| **Foundation** | Category (ROOT) + Supplier (ROOT) |
| **追溯深度** | 2 层 |
| **最终根节点** | Category (ROOT) + Supplier (ROOT) |

#### 完整链路
```
Category (ROOT) ──► Material ──┐
                               ├──► MaterialTraceCode ──► TraceabilityService
Supplier (ROOT) ────────────────┘
```

#### Evidence
- business-object-map.md §10: MaterialTraceCode.Dependencies 包含 Material, Batch, Supplier
- db-reality-map.md §10: material_trace_code 表索引包含 material_id, batch_no
- project-master-registry.yaml §traceability: material_trace_code 表结构

---

### 14. InvoiceReimbursement (发票报销)

#### 追溯链
```
InvoiceReimbursement
  └─► Input: Employee (员工) + Invoice (发票)
      └─► Source: employees + ?
          └─► Master: Employee
              └─► Foundation: ROOT (Employee 依赖 Organization)
```

#### 依赖分析

| 维度 | 详情 |
|------|------|
| **核心业务能力** | 发票报销管理 |
| **Input** | Employee (员工) + Invoice (发票) |
| **Source** | employees + ? |
| **Master** | Employee (依赖 Organization) |
| **Foundation** | Organization (ROOT) |
| **追溯深度** | 2 层 |
| **最终根节点** | Organization (ROOT) |

#### 完整链路
```
Organization (ROOT) ──► Employee ──► InvoiceReimbursement ──► Payment ──► FundFlow
```

#### Evidence
- business-object-map.md §16: InvoiceReimbursement.Dependencies 包含 Employee, Invoice, Voucher
- db-reality-map.md §3: employees 表
- event-job-map.md §7: InvoiceReimbursementApprovedEvent

---

### 15. DishRecipe (菜品配方)

#### 追溯链
```
DishRecipe
  └─► Input: Food (菜品) + Material (物料)
      └─► Source: foods + material_archives
          └─► Master: Food + Material
              └─► Foundation: ROOT (Food 依赖 Category, Material 依赖 Category)
```

#### 依赖分析

| 维度 | 详情 |
|------|------|
| **核心业务能力** | 菜品配方管理 (BOM) |
| **Input** | Food (菜品) + Material (物料) |
| **Source** | foods + material_archives |
| **Master** | Food (依赖 Category) + Material (依赖 Category) |
| **Foundation** | Category (ROOT) |
| **追溯深度** | 2 层 |
| **最终根节点** | Category (ROOT) |

#### 完整链路
```
Category (ROOT) ──► Food ──┐
                           ├──► DishRecipe ──► ProductionService
Category (ROOT) ──► Material ──┘
```

#### Evidence
- db-reality-map.md §4: dish_recipes 表索引包含 food_id, material_id
- project-master-registry.yaml §products: dish_recipes 表结构
- business-object-map.md §1-2: Food.Dependencies 包含 Material

---

### 16. DishCombo (菜品套餐)

#### 追溯链
```
DishCombo
  └─► Input: Food (菜品)
      └─► Source: foods
          └─► Master: Food
              └─► Foundation: ROOT (Food 依赖 Category)
```

#### 依赖分析

| 维度 | 详情 |
|------|------|
| **核心业务能力** | 菜品套餐组合管理 |
| **Input** | Food (菜品) |
| **Source** | foods |
| **Master** | Food (依赖 Category) |
| **Foundation** | Category (ROOT) |
| **追溯深度** | 1 层 |
| **最终根节点** | Category (ROOT) |

#### 完整链路
```
Category (ROOT) ──► Food ──► DishCombo ──► Order
```

#### Evidence
- db-reality-map.md §4: dish_combos 表索引包含 combo_id, food_id
- project-master-registry.yaml §products: dish_combos 表结构
- api-map.md §2: /v1/dish-combos 路由

---

## 三、Upstream 依赖层级图

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        Upstream 依赖层级全景图                                │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  Layer 0 (ROOT Foundation)                                                  │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │  Organization  Role  Permission  Category  AccountingSubject       │   │
│  │  Supplier      Member        BankAccount                          │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                              │                                              │
│                              ▼                                              │
│  Layer 1 (Depends on ROOT)                                                  │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │  Store    Employee    Food      Material    Voucher    Payable     │   │
│  │  (→Org)   (→Org)     (→Cat)    (→Cat)     (→Acct)   (→Sup)      │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                              │                                              │
│                              ▼                                              │
│  Layer 2 (Depends on Layer 1)                                               │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │  Inventory  StoreInventory  Order  Receivable  Payment  Receipt   │   │
│  │  (→Mat)     (→Food+Store)   (→Food+Store) (→Mem) (→Sup)  (→Mem) │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                              │                                              │
│                              ▼                                              │
│  Layer 3 (Depends on Layer 2)                                               │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │  FinanceRecord  FundFlow  TaxRecord  MaterialTraceCode            │   │
│  │  (→Voucher)     (→Bank)   (→Voucher) (→Material+Supplier)        │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 四、核心业务链路

### 4.1 采购链路 (Procurement Chain)

```
Supplier (ROOT) ──► PurchaseRequest ──► PurchaseOrder ──► PurchaseStockin
     │                                      │                    │
     │                                      │                    ▼
     │                                      │              Inventory (中央仓)
     │                                      │                    │
     │                                      ▼                    │
     │                               Payable ──► Payment ──► FundFlow
     │                                      │
     │                                      ▼
     └────────────────────────────► MaterialTraceCode ──► TraceabilityService
```

**Foundation ROOT 节点**: Supplier, Category (通过 Material)

---

### 4.2 销售链路 (Sales Chain)

```
Category (ROOT) ──► Food ──► StoreInventory ──► Order
                         │         │                │
                         │         │                ▼
                         │         │         Receivable ──► Receipt ──► FundFlow
                         │         │
                         │         └─► Inventory (扣减)
                         │
                         └─► MaterialTraceCode ──► TraceabilityService
```

**Foundation ROOT 节点**: Category, Organization (通过 Store)

---

### 4.3 财务链路 (Finance Chain)

```
AccountingSubject (ROOT) ──► Voucher ──► FinanceRecord ──► FinanceReport
                                  │
                                  ▼
                            TaxRecord ──► TaxDeclaration
                                  │
                                  ▼
                            FundFlow ──► CashFlowReport
```

**Foundation ROOT 节点**: AccountingSubject, BankAccount

---

### 4.4 HR 链路 (HR Chain)

```
Organization (ROOT) ──► Department ──► Employee ──► Attendance
                                   │         │
                                   │         ▼
                                   │    Salary ──► FinanceRecord
                                   │
                                   └─► InvoiceReimbursement ──► Payment ──► FundFlow
```

**Foundation ROOT 节点**: Organization

---

### 4.5 溯源链路 (Traceability Chain)

```
Category (ROOT) ──► Material ──► MaterialTraceCode ──► TraceabilityService
                         │              │
Supplier (ROOT) ─────────┘              ▼
                                MaterialConsumption
                                        │
                                        ▼
Category (ROOT) ──► Food ──► FoodTraceCode ──► TraceScanRecord
```

**Foundation ROOT 节点**: Category, Supplier

---

## 五、Missing Foundation 追溯

### 5.1 Warehouse (仓库)

| 维度 | 详情 |
|------|------|
| **缺失原因** | 无独立表，通过 `inventory.warehouse_id` 引用 |
| **影响范围** | 库存管理 (Inventory) |
| **追溯状态** | MISSING FOUNDATION |
| **可能来源** | 硬编码、配置项、或已废弃的 `stores` 表 |
| **建议** | 确认是否需要独立的仓库管理，或确认为配置项 |

---

### 5.2 Unit (单位)

| 维度 | 详情 |
|------|------|
| **缺失原因** | 无独立表，可能是枚举值 |
| **影响范围** | 物料管理 (Material)、菜品管理 (Food) |
| **追溯状态** | MISSING FOUNDATION |
| **可能来源** | 字段枚举、或已废弃的配置表 |
| **建议** | 确认是否需要动态单位管理，或确认为枚举 |

---

### 5.3 PaymentMethod (支付方式)

| 维度 | 详情 |
|------|------|
| **缺失原因** | 无独立表，可能是枚举值 |
| **影响范围** | 支付流程 (Payment)、订单管理 (Order) |
| **追溯状态** | MISSING FOUNDATION |
| **可能来源** | 字段枚举 (现金/微信/支付宝/会员支付) |
| **建议** | 确认是否需要动态支付方式管理，或确认为枚举 |

---

### 5.4 Customer (客户)

| 维度 | 详情 |
|------|------|
| **缺失原因** | 无独立表，可能与 Member 合并 |
| **影响范围** | 订单管理 (Order)、财务管理 (Receivable, Receipt) |
| **追溯状态** | UNKNOWN |
| **可能来源** | Member 表、或 Order 表字段 |
| **建议** | 确认 Customer 与 Member 的边界关系 |

---

### 5.5 Price (价格)

| 维度 | 详情 |
|------|------|
| **缺失原因** | 无独立表，分散存储在各业务表中 |
| **影响范围** | 订单管理 (Order)、库存管理 (Inventory) |
| **追溯状态** | UNKNOWN |
| **可能来源** | foods.price, material_archives.price 等字段 |
| **建议** | 暂不处理，保持现状 |

---

## 六、外部系统依赖

### 6.1 已识别的外部系统

| 外部系统 | 依赖方向 | 依赖对象 | Evidence |
|----------|----------|----------|----------|
| 微信支付 | IN | Payment (支付回调) | api-map.md §6, frontend-map.md §5 |
| 支付宝 | IN | Payment (支付回调) | api-map.md §6, frontend-map.md §5 |
| 企业微信 | IN | User (用户同步) | api-map.md §1 (公开端点) |
| OCR 引擎 | IN | InvoiceReimbursement (发票识别) | legacy-map.md §6 |
| SMTP 服务 | OUT | Notification (邮件发送) | legacy-map.md §6 |
| RabbitMQ | IN/OUT | Event (事件驱动) | project-master-map.md §1 |

### 6.2 外部系统追溯

```
微信支付/支付宝 ──► PaymentService ──► Order ──► Inventory
                                              │
                                              ▼
                                         Receivable ──► FundFlow
```

**追溯状态**: EXTERNAL SYSTEM (支付网关)

---

## 七、依赖关系矩阵

### 7.1 Foundation 对象依赖矩阵

| 依赖方 \ 被依赖方 | Organization | Role | Permission | Category | Supplier | Member | AccountingSubject | BankAccount |
|------------------|--------------|------|------------|----------|----------|--------|-------------------|-------------|
| **Store** | ✅ | - | - | - | - | - | - | - |
| **Employee** | ✅ | - | - | - | - | - | - | - |
| **Food** | - | - | - | ✅ | - | - | - | - |
| **Material** | - | - | - | ✅ | - | - | - | - |
| **Order** | ✅ | - | - | - | - | ✅ | - | - |
| **Inventory** | - | - | - | ✅ | - | - | - | - |
| **StoreInventory** | ✅ | - | - | - | - | - | - | - |
| **Voucher** | - | - | - | - | - | - | ✅ | - |
| **Payable** | - | - | - | - | ✅ | - | - | - |
| **Receivable** | - | - | - | - | - | ✅ | - | - |
| **Payment** | - | - | - | - | ✅ | - | - | ✅ |
| **Receipt** | - | - | - | - | - | ✅ | - | ✅ |
| **FundFlow** | - | - | - | - | - | - | - | ✅ |
| **FinanceRecord** | - | - | - | - | - | - | ✅ | - |
| **TaxRecord** | - | - | - | - | - | - | ✅ | - |
| **MaterialTraceCode** | - | - | - | ✅ | ✅ | - | - | - |
| **InvoiceReimbursement** | ✅ | - | - | - | - | - | - | - |
| **DishRecipe** | - | - | - | ✅ | - | - | - | - |
| **DishCombo** | - | - | - | - | - | - | - | - |

### 7.2 依赖统计

| Foundation 对象 | 被依赖次数 | 依赖方 |
|----------------|-----------|--------|
| **Category** | 7 | Food, Material, Inventory, StoreInventory, MaterialTraceCode, DishRecipe, DishCombo |
| **Organization** | 5 | Store, Employee, Order, StoreInventory, InvoiceReimbursement |
| **Supplier** | 3 | Payable, Payment, MaterialTraceCode |
| **Member** | 3 | Order, Receivable, Receipt |
| **AccountingSubject** | 3 | Voucher, FinanceRecord, TaxRecord |
| **BankAccount** | 3 | Payment, Receipt, FundFlow |
| **Role** | 0 | (通过 Permission 间接影响) |
| **Permission** | 0 | (通过 Aspect 间接影响) |

---

## 八、关键发现

### 8.1 高扇出 Foundation 对象 (被多个业务依赖)

| Foundation 对象 | 扇出数 | 影响范围 | 风险等级 |
|----------------|--------|----------|----------|
| **Category** | 7 | 所有产品相关业务 | HIGH |
| **Organization** | 5 | 门店、员工、订单、财务 | HIGH |
| **Supplier** | 3 | 采购、财务、追溯 | MEDIUM |
| **Member** | 3 | 订单、财务 | MEDIUM |
| **AccountingSubject** | 3 | 所有财务业务 | MEDIUM |
| **BankAccount** | 3 | 收付款、资金流水 | MEDIUM |

### 8.2 缺失 Foundation 导致的追溯断裂

| 缺失对象 | 断裂位置 | 影响范围 | 严重程度 |
|----------|----------|----------|----------|
| **Warehouse** | Inventory → ? | 库存管理 | HIGH |
| **Unit** | Material → ? | 物料/菜品管理 | MEDIUM |
| **PaymentMethod** | Payment → ? | 支付流程 | MEDIUM |
| **Customer** | Order → ? | 订单/财务 | LOW |

### 8.3 需要 Product Decision 的追溯项

| 编号 | 决策项 | 影响范围 | 状态 |
|------|--------|----------|------|
| PD-XXX-1 | Warehouse 管理模式 (独立表 vs 配置项) | 库存管理 | 待决 |
| PD-XXX-2 | Unit 管理模式 (独立表 vs 枚举) | 物料/菜品管理 | 待决 |
| PD-XXX-3 | PaymentMethod 管理模式 (独立表 vs 枚举) | 支付流程 | 待决 |
| PD-XXX-4 | Customer 与 Member 的边界关系 | 订单/财务 | 待决 |

---

## 九、建议行动项

### 9.1 高优先级 (P0)

1. **确认 Warehouse 管理模式**
   - 当前通过 `inventory.warehouse_id` 引用，无独立表
   - 需确认是否需要独立的仓库管理
   - 建议：创建 `warehouses` 表或确认为配置项

2. **确认 Unit 管理模式**
   - 当前无独立表，可能是枚举值
   - 需确认是否需要动态单位管理
   - 建议：创建 `units` 表或确认为枚举

3. **确认 PaymentMethod 管理模式**
   - 当前无独立表，可能是枚举值
   - 需确认是否需要动态支付方式管理
   - 建议：创建 `payment_methods` 表或确认为枚举

### 9.2 中优先级 (P1)

4. **确认 Customer 与 Member 的关系**
   - 当前 Customer 无独立表，可能与 Member 合并
   - 需确认是否需要独立的客户管理
   - 建议：明确 Customer 与 Member 的边界

5. **统一金额单位**
   - 当前存在分/元双轨 (冲突1)
   - 需在所有跨表查询处添加单位转换
   - 建议：逐步统一为「分」

### 9.3 低优先级 (P2)

6. **统一状态机**
   - 当前订单状态机三套并存 (冲突5)
   - 需建立统一的状态枚举
   - 建议：分阶段统一

7. **清理 Legacy 冲突**
   - `stores` → `stores_new` 迁移
   - `food` → `foods` 迁移
   - `food_category` → `food_categories` 迁移
   - `account_balance` → `accounting_subjects.balance` 迁移

---

## 十、Foundation Dependency Order (业务依赖顺序)

基于实际证据推导的业务依赖顺序：

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        Foundation Dependency Order                          │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  1. Organization (ROOT)                                                    │
│     - 部门、职位、职级定义                                                  │
│     - 所有业务的组织基础                                                    │
│                                                                             │
│  2. Role + Permission (ROOT)                                               │
│     - 角色、权限定义                                                        │
│     - 权限控制基础                                                          │
│                                                                             │
│  3. Category (ROOT)                                                        │
│     - 菜品分类、物料分类                                                    │
│     - 产品管理基础                                                          │
│                                                                             │
│  4. AccountingSubject (ROOT)                                               │
│     - 会计科目定义                                                          │
│     - 财务管理基础                                                          │
│                                                                             │
│  5. BankAccount (ROOT)                                                     │
│     - 银行账户定义                                                          │
│     - 资金管理基础                                                          │
│                                                                             │
│  6. Supplier (ROOT)                                                        │
│     - 供应商定义                                                            │
│     - 采购管理基础                                                          │
│                                                                             │
│  7. Member (ROOT)                                                          │
│     - 会员定义                                                              │
│     - 营销管理基础                                                          │
│                                                                             │
│  8. Store (依赖 Organization)                                              │
│     - 门店定义                                                              │
│     - 门店级业务基础                                                        │
│                                                                             │
│  9. Employee (依赖 Organization)                                           │
│     - 员工定义                                                              │
│     - HR 管理基础                                                           │
│                                                                             │
│  10. Food (依赖 Category)                                                  │
│      - 菜品定义                                                            │
│      - 产品管理核心                                                         │
│                                                                             │
│  11. Material (依赖 Category)                                              │
│      - 物料定义                                                            │
│      - 库存管理基础                                                         │
│                                                                             │
│  12. Inventory (依赖 Material + Warehouse)                                 │
│      - 中央库存                                                            │
│      - 库存管理核心                                                         │
│                                                                             │
│  13. StoreInventory (依赖 Food + Store)                                    │
│      - 门店库存                                                            │
│      - 门店运营核心                                                         │
│                                                                             │
│  14. Order (依赖 Food + Store + Member)                                    │
│      - 订单管理                                                            │
│      - 销售核心                                                             │
│                                                                             │
│  15. Payable (依赖 Supplier)                                               │
│      - 应付账款                                                            │
│      - 财务管理核心                                                         │
│                                                                             │
│  16. Receivable (依赖 Member)                                              │
│      - 应收账款                                                            │
│      - 财务管理核心                                                         │
│                                                                             │
│  17. Voucher (依赖 AccountingSubject)                                      │
│      - 财务凭证                                                            │
│      - 财务核算核心                                                         │
│                                                                             │
│  18. Payment (依赖 Supplier + BankAccount)                                 │
│      - 付款记录                                                            │
│      - 资金流出核心                                                         │
│                                                                             │
│  19. Receipt (依赖 Member + BankAccount)                                   │
│      - 收款记录                                                            │
│      - 资金流入核心                                                         │
│                                                                             │
│  20. FinanceRecord (依赖 Voucher + AccountingSubject)                      │
│      - 财务流水                                                            │
│      - 财务记录核心                                                         │
│                                                                             │
│  21. FundFlow (依赖 BankAccount + Voucher)                                 │
│      - 资金流水                                                            │
│      - 资金管理核心                                                         │
│                                                                             │
│  22. TaxRecord (依赖 Voucher + AccountingSubject)                          │
│      - 税务记录                                                            │
│      - 税务管理核心                                                         │
│                                                                             │
│  23. MaterialTraceCode (依赖 Material + Supplier)                          │
│      - 物料追溯码                                                          │
│      - 溯源管理核心                                                         │
│                                                                             │
│  24. InvoiceReimbursement (依赖 Employee)                                  │
│      - 发票报销                                                            │
│      - 费用管理核心                                                         │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

*生成时间: 2026-09-09*
*数据来源: Project Master Map 静态分析*
