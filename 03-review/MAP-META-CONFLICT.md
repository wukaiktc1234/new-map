# MAP-META-CONFLICT — 地图一致性冲突报告

> 生成日期: 2026-09-09
> 范围: project-master-map/ 全量地图文件交叉校验
> 状态: **待处理**

---

## 一、统计数字冲突总览

### 1.1 Foundation Objects: 15 vs 19

| 来源文件 | 声称数量 | 实际列出数量 |
|----------|----------|--------------|
| `project-master-map-summary.md` | **15** | — |
| `foundation-map.md` | — | **19** |
| `project-master-registry.yaml` | — | **18** |
| `project-master-map-gap-analysis.md` | — | 19 (引用 foundation-map) |
| `capability-readiness-map.md` | — | 11 (仅引用被 Capability 依赖的) |

**根因分析**：

| 类别 | 对象 | summary 计入? | foundation-map 计入? | registry 计入? |
|------|------|---------------|---------------------|----------------|
| ROOT | Organization | ✅ | ✅ | ✅ |
| ROOT | Role | ✅ | ✅ | ✅ |
| ROOT | Permission | ✅ | ✅ | ✅ |
| ROOT | Category | ✅ | ✅ | ✅ |
| ROOT | AccountingSubject | ✅ | ✅ | ✅ |
| ROOT | BankAccount | ✅ | ✅ | ✅ |
| VERIFIED | Store | ✅ | ✅ | ✅ |
| VERIFIED | Department | — | ✅ | ✅ |
| VERIFIED | Employee | ✅ | ✅ | ✅ |
| VERIFIED | Position | — | ✅ | ✅ |
| VERIFIED | Food | ✅ | ✅ | ✅ |
| VERIFIED | Material | ✅ | ✅ | ✅ |
| VERIFIED | Supplier | ✅ | ✅ | ✅ |
| VERIFIED | Member | — | ✅ | ✅ |
| MISSING | Warehouse | — | ✅ | ✅ |
| MISSING | Unit | — | ✅ | ✅ |
| MISSING | PaymentMethod | — | ✅ | ✅ |
| MISSING | Customer | — | ✅ | ✅ |
| MISSING | Price | — | ✅ | ✅ |

**差异说明**：
- `summary` 声称 15 个，实际只统计了 "12 VERIFIED + 2 PARTIAL + 3 MISSING + 2 UNKNOWN" = 19，但其描述文本自相矛盾
- `foundation-map` 列出了 19 个完整的 Foundation 对象
- `registry` 列出了 18 个（Department, Position, Member 被计入但 summary 未提及）

**统一建议**：采用 **19** 作为 Foundation Objects 总数，其中 6 ROOT + 8 VERIFIED + 5 MISSING。

---

### 1.2 Pages: 200+ vs 170+

| 来源文件 | 声称数量 | 统计口径 |
|----------|----------|----------|
| `project-master-map-summary.md` | **200+** | 管理后台 100+ + POS 11 + Kitchen 4 + Employee 30+ + MiniProgram 24 + Mobile 1 |
| `project-master-map.md` 关键指标 | **170+** | 仅管理后台路由 100+ |
| `project-master-map.md` 前端终端矩阵 | **170+** | 管理后台 100+ + Employee 30+ + MiniProgram 24 + POS 11 + Kitchen 4 + Receiving 1 |

**根因分析**：
- `summary` 的 200+ 包含所有 6 个终端的路由/页面
- `project-master-map.md` 关键指标写 170+，但其前端终端矩阵加总 = 100+30+24+11+4+1 = **170**
- 差异约 30，可能来自 `summary` 将管理后台的子页面/附属页面也计入

**统一建议**：明确统计口径为「前端路由/页面总数」，统一为 **170+**（不含附属页面）或 **200+**（含附属页面）。建议采用 **170+** 作为路由数，**200+** 作为含子页面的总数。

---

### 1.3 其他统计数字对比

| 指标 | project-master-map.md | summary | registry | 一致性 |
|------|----------------------|---------|----------|--------|
| Controller | 154 | 154 | — | ✅ 一致 |
| Service | 293 | — | — | — 单一来源 |
| Entity | 326 | — | — | — 单一来源 |
| Mapper | 302 | — | — | — 单一来源 |
| Migration | 160 | 160 | — | ✅ 一致 |
| 领域事件 | 22 | 22 (Events) | 10 (Events) | ⚠️ 不一致 |
| 定时任务 | 6 | 6 (Scheduled) | 5 (Schedulers) | ⚠️ 不一致 |
| 事件监听器 | 10 | 10 | — | ✅ 一致 |
| 权限码 | 180 | 180 | — | ✅ 一致 |
| 数据范围级别 | 8 | 8 | — | ✅ 一致 |
| 业务对象 | — | 16 (Core) | 16 (business_objects) | ✅ 一致 |
| Foundation Objects | — | 15 | 18 | ⚠️ 不一致 |
| Broken Chains | — | 31 | — | — 单一来源 |
| Legacy Components | — | 36 | — | — 单一来源 |

**需关注的差异**：
1. 领域事件：summary 22 vs registry 10（registry 仅列出命名事件，summary 含全部事件类）
2. 定时任务：summary 6 vs registry 5（registry 仅列出 Scheduler，summary 含所有定时任务）
3. Foundation Objects：15 vs 18 vs 19（已详细分析）

---

## 二、对象数量/分类/名称冲突

### 2.1 三张地图的对象覆盖范围

| 地图 | 对象数 | 覆盖范围 |
|------|--------|----------|
| `business-object-map.md` | **28** | 16 核心 + 5 遗留（food, product, orders_legacy, voucher_header, sales_order）+ 7 隐含 |
| `foundation-map.md` | **19** | 6 ROOT + 8 VERIFIED + 5 MISSING |
| `master-data-source-map.md` | **15** | 15 个主数据源 |
| `project-master-registry.yaml` | **16** | 16 个 business_objects |
| `upstream-dependency-map.md` | **24** | 24 个 Foundation Dependency Order 条目 |

### 2.2 对象归属矩阵

| 对象 | business-object | foundation | master-data-source | upstream-dependency | 应归属 |
|------|----------------|------------|-------------------|--------------------|--------| 
| Food | ✅ VERIFIED | ✅ VERIFIED | ✅ VERIFIED | ✅ Layer 1 | 三者均应包含 |
| Material | ✅ VERIFIED | ✅ VERIFIED | ✅ VERIFIED | ✅ Layer 1 | 三者均应包含 |
| Inventory | ✅ VERIFIED | ❌ 不在 foundation | ✅ VERIFIED | ✅ Layer 2 | BOM + MDS |
| StoreInventory | ✅ VERIFIED | ❌ 不在 foundation | ✅ VERIFIED | ✅ Layer 2 | BOM + MDS |
| Order | ✅ VERIFIED | ❌ 不在 foundation | ✅ VERIFIED | ✅ Layer 2 | BOM + MDS |
| Voucher | ✅ VERIFIED | ❌ 不在 foundation | ✅ VERIFIED | ✅ Layer 2 | BOM + MDS |
| Payable | ✅ VERIFIED | ❌ 不在 foundation | ❌ 不在 MDS | ✅ Layer 2 | BOM only |
| Receivable | ✅ VERIFIED | ❌ 不在 foundation | ❌ 不在 MDS | ✅ Layer 2 | BOM only |
| AccountingSubject | ✅ VERIFIED | ✅ ROOT | ✅ VERIFIED | ✅ ROOT | 三者均应包含 |
| MaterialTraceCode | ✅ VERIFIED | ❌ 不在 foundation | ❌ 不在 MDS | ✅ Layer 3 | BOM only |
| FinanceRecord | ✅ VERIFIED | ❌ 不在 foundation | ❌ 不在 MDS | ✅ Layer 3 | BOM only |
| FundFlow | ✅ VERIFIED | ❌ 不在 foundation | ❌ 不在 MDS | ✅ Layer 3 | BOM only |
| Receipt | ✅ VERIFIED | ❌ 不在 foundation | ❌ 不在 MDS | ✅ Layer 2 | BOM only |
| Payment | ✅ VERIFIED | ❌ 不在 foundation | ❌ 不在 MDS | ✅ Layer 2 | BOM only |
| TaxRecord | ✅ VERIFIED | ❌ 不在 foundation | ❌ 不在 MDS | ✅ Layer 3 | BOM only |
| InvoiceReimbursement | ✅ VERIFIED | ❌ 不在 foundation | ❌ 不在 MDS | ✅ Layer 3 | BOM only |
| **Supplier** | ❌ 不在 BOM | ✅ VERIFIED | ✅ VERIFIED | ✅ ROOT | Foundation + MDS |
| **Store** | ❌ 不在 BOM | ✅ VERIFIED | ✅ VERIFIED | ✅ Layer 1 | Foundation + MDS |
| **Employee** | ❌ 不在 BOM | ✅ VERIFIED | ✅ VERIFIED | ✅ Layer 1 | Foundation + MDS |
| **Member** | ❌ 不在 BOM | ✅ VERIFIED | ✅ VERIFIED | ✅ ROOT | Foundation + MDS |
| **Department** | ❌ 不在 BOM | ✅ VERIFIED | ✅ VERIFIED | — | Foundation + MDS |
| **Position** | ❌ 不在 BOM | ✅ VERIFIED | ✅ VERIFIED | — | Foundation + MDS |
| **BankAccount** | ❌ 不在 BOM | ✅ ROOT | ✅ VERIFIED | ✅ ROOT | Foundation + MDS |
| **Category** | ❌ 不在 BOM | ✅ ROOT | ❌ 不在 MDS | ✅ ROOT | Foundation only |
| **Role** | ❌ 不在 BOM | ✅ ROOT | ❌ 不在 MDS | — | Foundation only |
| **Permission** | ❌ 不在 BOM | ✅ ROOT | ❌ 不在 MDS | — | Foundation only |
| Warehouse | ❌ 不在 BOM | ✅ MISSING | ✅ 待确认 | ✅ MISSING | Foundation + MDS |
| Unit | ❌ 不在 BOM | ✅ MISSING | ❌ 不在 MDS | ✅ MISSING | Foundation only |
| PaymentMethod | ❌ 不在 BOM | ✅ MISSING | ❌ 不在 MDS | ✅ MISSING | Foundation only |
| Customer | ❌ 不在 BOM | ✅ MISSING | ❌ 不在 MDS | ✅ MISSING | Foundation only |
| Price | ❌ 不在 BOM | ✅ MISSING | ❌ 不在 MDS | ✅ MISSING | Foundation only |
| **DishRecipe** | ❌ 不在 BOM | ❌ 不在 foundation | ❌ 不在 MDS | ✅ Layer 1 | upstream only |
| **DishCombo** | ❌ 不在 BOM | ❌ 不在 foundation | ❌ 不在 MDS | ✅ Layer 1 | upstream only |

### 2.3 关键发现

1. **BOM 缺少 Foundation 级对象**：Supplier, Store, Employee, Member, Department, Position, BankAccount, Category, Role, Permission 在 `business-object-map.md` 中缺失
2. **Foundation 缺少业务对象**：Inventory, StoreInventory, Order, Voucher, Payable, Receivable 等核心业务对象不在 `foundation-map.md` 中
3. **MDS 缺少财务对象**：Payable, Receivable, MaterialTraceCode, FinanceRecord, FundFlow, Receipt, Payment, TaxRecord, InvoiceReimbursement 不在 `master-data-source-map.md` 中
4. **DishRecipe/DishCombo 仅在 upstream-dependency-map** 中出现，其他地图未收录

---

## 三、Truth Source / Owner 一致性检查

### 3.1 同一对象 Truth Source 对比

| 对象 | BOM Truth Source | MDS Canonical Source | truth-conflict-map | Registry Truth Source | 一致性 |
|------|-----------------|---------------------|--------------------|-----------------------|--------|
| Food | `foods表` | `foods表` | `foods` (PADR §九-A) | `foods` | ✅ 一致 |
| Material | `material_archives表` | `material_archives表` | `material_archives` | `material_archives` | ✅ 一致 |
| Inventory | `inventory表` | `inventory表` | `inventory` | — | ✅ 一致 |
| StoreInventory | `store_inventory表` | `store_inventory表` | `store_inventory` | — | ✅ 一致 |
| Order | `orders表` | `orders表` (V6) | `orders` (PADR §九-A) | — | ✅ 一致 |
| Voucher | `finance_vouchers表` | `finance_vouchers表` | `finance_vouchers` | — | ✅ 一致 |
| AccountingSubject | `accounting_subjects表` | `accounting_subjects表` | `accounting_subjects` (PD-031) | `accounting_subjects` | ✅ 一致 |
| **Member** | — | `members表` | — | — | ✅ 一致 (仅 MDS/Registry) |
| **Department** | — | `departments表` | — | `departments` | ✅ 一致 |
| **Position** | — | `positions表` | — | `positions` | ✅ 一致 |

**结论**：所有已跨地图引用的对象 Truth Source 均一致。

### 3.2 Owner 一致性检查

| 对象 | BOM Owner | data-lineage Owner | Registry Module | 一致性 |
|------|-----------|-------------------|-----------------|--------|
| Food | Management Core | Management Core | backend | ✅ 一致 |
| Material | Management Core | Management Core | backend | ✅ 一致 |
| Order | POS, MiniProgram | POS, MiniProgram | backend | ✅ 一致 |
| Inventory | Inventory模块 | Inventory模块 | backend | ✅ 一致 |
| StoreInventory | POS | POS | backend | ✅ 一致 |
| Voucher | Finance模块 | Finance模块 | backend | ✅ 一致 |
| Employee | Management Core | Management Core | backend | ✅ 一致 |
| Supplier | Management Core | Management Core | backend | ✅ 一致 |
| Store | Management Core | Management Core | backend | ✅ 一致 |
| Member | Management Core | Management Core | backend | ✅ 一致 |
| BankAccount | Finance模块 | Finance模块 | backend | ✅ 一致 |
| AccountingSubject | Finance模块 | Finance模块 | backend | ✅ 一致 |

**结论**：所有已定义 Owner 的对象均一致。

---

## 四、Foundation / Capability 层级一致性

### 4.1 层级定义对比

| 层级定义来源 | 层级结构 |
|-------------|----------|
| **project-master-map-gap-analysis.md** | Foundation → Master Data → Capability → Transaction → Settlement → Reporting |
| **upstream-dependency-map.md** Layer 0-3 | Layer 0 (ROOT Foundation) → Layer 1 (Depends on ROOT) → Layer 2 (Depends on Layer 1) → Layer 3 (Depends on Layer 2) |
| **downstream-impact-map.md** | Foundation → Capability → Transaction → Settlement → Reporting |
| **capability-readiness-map.md** | Foundation → Capability → Transaction |
| **foundation-map.md** | ROOT → VERIFIED → MISSING |

### 4.2 层级定义冲突

| 冲突点 | 描述 | 影响 |
|--------|------|------|
| ROOT vs Layer 0 | foundation-map 使用 ROOT/VERIFIED/MISSING 分类；upstream-dependency-map 使用 Layer 0/1/2/3 分层 | 语义不同但不矛盾 |
| Foundation 定义不一致 | gap-analysis 称 Foundation 包含 "Master Data"；upstream 将 Master 和 Foundation 合并在 Layer 0/1 | 概念边界模糊 |
| Capability 层级位置 | downstream 将 Capability 放在 Foundation 之后；upstream 将 Capability 视为 Layer 2+ | 位置不同 |

**统一建议**：
```
Layer 0: ROOT Foundation (Organization, Role, Permission, Category, AccountingSubject, BankAccount)
Layer 1: Foundation (Store, Department, Employee, Position, Food, Material, Supplier, Member)
Layer 2: Master Data / Business Object (Inventory, StoreInventory, Order, Voucher, Payable, Receivable, etc.)
Layer 3: Derived / Transaction (FinanceRecord, FundFlow, TaxRecord, MaterialTraceCode, etc.)
Layer 4: Reporting / Analytics
```

---

## 五、跨地图其他冲突

### 5.1 DishRecipe/DishCombo 归属不明

- `upstream-dependency-map.md` 将 DishRecipe 和 DishCombo 列为独立的 "核心业务能力"（#15 和 #16）
- `business-object-map.md` **未收录** DishRecipe 和 DishCombo
- `project-master-registry.yaml` 的 `database_tables` 中有 `dish_recipes` 和 `dish_combos`，但 `business_objects` 中未列出
- `db-reality-map.md` 中有 dish_recipes 和 dish_combos 表

**建议**：将 DishRecipe 和 DishCombo 补充到 `business-object-map.md` 中。

### 5.2 Member 对象归属

- `foundation-map.md` 将 Member 列为 VERIFIED Foundation
- `business-object-map.md` **未收录** Member
- `master-data-source-map.md` 收录 Member
- `upstream-dependency-map.md` 将 Member 列为 ROOT Foundation (Layer 0)
- `downstream-impact-map.md` 未将 Member 列为 Foundation

**建议**：Member 应同时出现在 BOM 和 Foundation 中。

### 5.3 Supplier 对象归属

- `foundation-map.md` 将 Supplier 列为 VERIFIED Foundation
- `business-object-map.md` **未收录** Supplier
- `master-data-source-map.md` 收录 Supplier
- `upstream-dependency-map.md` 将 Supplier 列为 ROOT Foundation

**建议**：Supplier 应同时出现在 BOM 和 Foundation 中。

### 5.4 Warehouse 状态不一致

| 文件 | Warehouse 状态 |
|------|---------------|
| `foundation-map.md` | MISSING |
| `master-data-source-map.md` | "未发现独立主表" / "待确认" |
| `upstream-dependency-map.md` | MISSING FOUNDATION |
| `capability-readiness-map.md` | "✅ READY"（inventory表有warehouse_id） |
| `downstream-impact-map.md` | 中影响 Foundation |
| `project-master-registry.yaml` | MISSING (confidence: LOW) |

**冲突**：capability-readiness-map 将 Warehouse 标为 READY，但其他所有地图都标记为 MISSING。

**建议**：统一为 "MISSING — 无独立表，通过 inventory.warehouse_id 引用"。

---

## 六、统一建议汇总

### 6.1 统一 Foundation Objects 数量

**推荐值：19 个**

| 类别 | 数量 | 对象 |
|------|------|------|
| ROOT | 6 | Organization, Role, Permission, Category, AccountingSubject, BankAccount |
| VERIFIED | 8 | Store, Department, Employee, Position, Food, Material, Supplier, Member |
| MISSING | 5 | Warehouse, Unit, PaymentMethod, Customer, Price |

**需更新的文件**：
- `project-master-map-summary.md`：15 → 19
- `project-master-map-gap-analysis.md`：已正确引用 19，无需修改
- `project-master-map-conflict-registry.yaml`：已记录此冲突

### 6.2 统一 Pages 数量

**推荐值：170+（路由数）/ 200+（含子页面）**

**需更新的文件**：
- `project-master-map.md` 关键指标：明确标注 "170+ 路由" 或统一为 "200+ 页面"
- `project-master-map-summary.md`：保持 200+，但注明包含所有终端

### 6.3 统一对象归属

**推荐归属规则**：
- **business-object-map.md**：收录所有有独立 DB 表的业务对象（含 Foundation 级和业务级）
- **foundation-map.md**：仅收录 Foundation 级对象（ROOT + VERIFIED + MISSING）
- **master-data-source-map.md**：仅收录有 Canonical Source 的主数据
- **upstream-dependency-map.md**：收录所有被依赖追踪的对象

**需补充到 business-object-map.md 的对象**：
1. Supplier（供应商）
2. Store（门店）
3. Employee（员工）
4. Member（会员）
5. Department（部门）
6. Position（职位）
7. BankAccount（银行账户）
8. Category（分类）
9. Role（角色）
10. Permission（权限）
11. DishRecipe（菜品配方）
12. DishCombo（菜品套餐）

### 6.4 统一 Warehouse 状态

**推荐**：将 `capability-readiness-map.md` 中 Warehouse 状态从 ✅ READY 改为 ⚠️ PARTIAL，并添加说明。

### 6.5 统一层级定义

**推荐标准层级**：
```
Layer 0: ROOT Foundation — 无上游依赖的根对象
Layer 1: Foundation — 依赖 ROOT 的基础对象
Layer 2: Business Object — 核心业务对象
Layer 3: Derived Object — 衍生/汇总对象
Layer 4: Reporting — 报表/分析
```

---

## 七、需更新的文件清单

| 文件 | 需修改内容 | 优先级 |
|------|-----------|--------|
| `project-master-map-summary.md` | Foundation Objects 15 → 19；补充统计口径说明 | P1 |
| `project-master-map.md` | Pages 170+ 明确为路由数；补充 Foundation 19 说明 | P1 |
| `business-object-map.md` | 补充 12 个 Foundation 级对象（Supplier, Store, Employee 等） | P1 |
| `capability-readiness-map.md` | Warehouse 状态 ✅ → ⚠️ PARTIAL | P2 |
| `foundation-map.md` | 确认 19 个对象列表完整 | P2 |
| `project-master-map-conflict-registry.yaml` | 补充本报告发现的新冲突 | P1 |
| `project-master-map-consistency-report.md` | 更新为最新的冲突清单 | P1 |

---

*报告生成时间: 2026-09-09*
*数据来源: project-master-map/ 全量地图文件交叉校验*
