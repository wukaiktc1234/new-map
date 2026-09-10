# 系统问题注册表 — Systemic Problem Register

> **评审编号**: PROJECT-MASTER-REVIEW-001
> **版本**: 1.0.0
> **生成日期**: 2026-09-09
> **状态**: ACTIVE
> **范围**: 食品溯源系统全域系统性问题归并

---

## 一、注册表概览

| 统计项 | 数量 |
|--------|------|
| Root Cause 总数 | 13 |
| Critical | 4 |
| High | 5 |
| Medium | 4 |
| Low | 0 |

| 分类 | 数量 | 编号范围 |
|------|------|----------|
| Identity/Truth Source 冲突 | 3 | RC-001 ~ RC-003 |
| 数据模型问题 | 3 | RC-004 ~ RC-006 |
| Foundation 缺失 | 5 | RC-007 ~ RC-011 |
| Cross-Domain 问题 | 3 | RC-012 ~ RC-014 |
| Legacy 问题 | 3 | RC-015 ~ RC-017 |

---

## 二、Root Cause 详细注册

---

### RC-001: food/foods 双写与身份冲突

```yaml
root_cause_id: RC-001
title: food/foods 双写与身份冲突
description: >
  菜品实体存在 food 表 (旧) 和 foods 表 (新) 双表并存，
  导致下单、退款、超时等操作同时写入两表，身份归属不明确。
symptoms:
  - 下单时同时写入 food 和 foods 两表
  - food 表数据与 foods 表数据可能不一致
  - 新旧模块混用导致查询结果不可预测
  - 部分消费者仍引用已废弃的 food 表
affected_objects:
  - Food (菜品)
  - FoodCategory (菜品分类)
affected_domains:
  - Management Core
  - POS
  - Inventory
upstream: Management Core (FoodService.create)
downstream:
  - OrderService
  - InventoryService
  - MenuService
truth_source: foods (VERIFIED)
current_behavior: >
  下单、退款、超时等操作同时写 food 和 foods 两表；
  food 表为旧版兼容，foods 表为新版真相源。
expected_behavior: >
  foods 为唯一真相源；food 表写入在旧模块迁移后废弃。
related_broken_chains:
  - BC-006 (food 与 foods 表并存)
  - BC-012 (food 与 foods 真相源冲突)
  - BC-015 (food 与 foods 重复)
related_lineage_risks:
  - food/foods 双写导致数据不一致
  - Food Created Bloodline 分叉
related_conflicts:
  - CONFLICT-ID-001 (productId Wrong Identity)
  - truth-conflict-map §冲突7
evidence:
  - truth-conflict-map.md §冲突7: food + foods 双写
  - broken-chain-map.md §BC-006, BC-012, BC-015
  - data-lineage-map.md §2.1: Food 创建血缘分叉
  - legacy-map.md: food 标记为 DEPRECATED
confidence: HIGH
severity: critical
```

---

### RC-002: product/material 边界不清与遗留列名

```yaml
root_cause_id: RC-002
title: product/material 边界不清与遗留列名冲突
description: >
  物料实体存在语义与列名的双重冲突：material_archives 为真相语义，
  但 orders/order_items 等多表中 product_id 列实际指向物料而非菜品。
  product 表为遗留表，与 material_archives 语义重叠。
symptoms:
  - orders 表中 product_id 列名指向物料而非菜品
  - 开发者误以为 product_id 指向 foods 表
  - product 遗留表与 material_archives 语义边界模糊
  - 物料分类表 material_categories 与旧表可能并存
affected_objects:
  - Material (物料)
  - Product (遗留产品)
affected_domains:
  - Management Core
  - Inventory
  - Finance
upstream: Management Core (MaterialService)
downstream:
  - OrderService (product_id 列)
  - InventoryService
  - TraceabilityService
truth_source: material_archives (VERIFIED)
current_behavior: >
  product_id 列名暂保留兼容，但语义指向物料；
  product 遗留表未清理。
expected_behavior: >
  新代码一律使用 material_id 语义；
  product 遗留表迁移后废弃。
related_broken_chains:
  - BC-007 (material 分类表并存)
related_lineage_risks:
  - product_id 列名语义混淆
  - 物料身份追踪可能错位
related_conflicts:
  - CONFLICT-ID-001 (productId Wrong Identity)
  - truth-conflict-map §冲突2
evidence:
  - truth-conflict-map.md §冲突2: product_id vs material_id
  - broken-chain-map.md §BC-007
  - legacy-map.md: product 标记为 DEPRECATED
confidence: HIGH
severity: high
```

---

### RC-003: stores/stores_new 双表与门店身份冲突

```yaml
root_cause_id: RC-003
title: stores/stores_new 双表与门店身份冲突
description: >
  门店实体存在 stores 表 (旧) 和 stores_new 表 (新) 并存，
  字段结构不同，读错表导致门店信息不一致。
symptoms:
  - 新旧代码查询不同的门店表
  - 门店地址、经营范围、状态等字段可能不一致
  - 部分遗留模块仍读取 stores 表
affected_objects:
  - Store (门店)
affected_domains:
  - Management Core
  - POS
  - Kitchen
  - Employee
  - Mini Program
  - Inventory
upstream: Management Core (StoreService)
downstream:
  - OrderService
  - InventoryService
  - StoreInventoryService
  - DataScopeAspect
truth_source: stores_new (VERIFIED)
current_behavior: >
  stores_new 为唯一真相源；stores 表仅用于兼容旧查询。
expected_behavior: >
  新代码必须查询 stores_new；
  stores 表在旧模块迁移后废弃。
related_broken_chains:
  - BC-020 (stores 与 stores_new 重复)
related_lineage_risks:
  - 门店信息读取可能指向错误表
related_conflicts:
  - truth-conflict-map §冲突4
  - CONFLICT-ID-002 (warehouseId Identity Loss)
evidence:
  - truth-conflict-map.md §冲突4
  - broken-chain-map.md §BC-020
  - foundation-map.md §2: Store Truth Source = stores_new
confidence: HIGH
severity: high
```

---

### RC-004: 金额单位双轨（分/元）

```yaml
root_cause_id: RC-004
title: 金额单位双轨（分/元）
description: >
  新表统一用「分」存储金额，但 tax_record、account_balance 等遗留表仍用「元」；
  finance_records 表注释中金额单位标注混乱。跨表 JOIN/汇总时单位不一致，
  导致金额差 100 倍。
symptoms:
  - 跨表 JOIN 金额差 100 倍
  - finance_records 注释标注混乱（有标分有标元）
  - tax_record 使用元单位
  - account_balance 使用元单位
affected_objects:
  - 所有财务对象 (Payable, Receivable, FundFlow, FinanceRecord, Voucher)
  - TaxRecord
  - AccountBalance
affected_domains:
  - Finance
  - Management Core
  - POS
upstream: 各业务模块创建交易记录
downstream:
  - FinanceReportService
  - FundFlowService
  - 税务报表
truth_source: 新表以「分」为准 (DECIDED)
current_behavior: >
  新表用分；遗留表用元；
  finance_records 注释错误未修正。
expected_behavior: >
  统一为「分」单位；
  读 tax_record/account_balance 时显式转换；
  修正 finance_records 注释。
related_broken_chains:
  - BC-010 (凭证 → 财务报表继承断裂)
related_lineage_risks:
  - 财务数据跨表汇总可能出错
  - 税务报表金额可能偏差 100 倍
related_conflicts:
  - CONFLICT-STAT-001 (统计数字冲突)
  - truth-conflict-map §冲突1
  - truth-conflict-map §冲突10
evidence:
  - truth-conflict-map.md §冲突1: 金额单位双轨
  - truth-conflict-map.md §冲突10: finance_record 注释分实元
  - cross-domain-coordination-matrix.md §5.2: 金额单位双轨风险
confidence: HIGH
severity: critical
```

---

### RC-005: 订单状态三套并存

```yaml
root_cause_id: RC-005
title: 订单状态三套并存
description: >
  订单实体存在 order_status、payment_status、legacy 三套状态字段并存，
  状态值和语义不一致，状态转换逻辑分散，难以保证一致性。
symptoms:
  - order_status: 0-6 (7个状态)
  - payment_status: 0-3 (4个状态)
  - legacy: -1, 0-7 (9个状态)
  - 状态转换逻辑分散在多个服务中
  - sales_order 表状态与 order_status 无映射
affected_objects:
  - Order (订单)
  - SalesOrder (销售订单)
affected_domains:
  - Management Core
  - POS
  - Kitchen
  - Employee
  - Mini Program
upstream: POS, MiniProgram (订单创建)
downstream:
  - Kitchen (出餐状态)
  - Finance (应收生成)
  - Inventory (库存扣减)
truth_source: 三套状态机并存 (PADR DECIDED)
current_behavior: >
  三套状态机并存，每个字段有独立生命周期；
  sales_order 状态无映射关系。
expected_behavior: >
  代码中明确区分使用哪个状态字段；
  建立 sales_order 与 order_status 的映射。
related_broken_chains:
  - BC-027 (订单状态三表并存)
  - BC-029 (sales_order 无映射关系)
related_lineage_risks:
  - 订单全流程状态可能不一致
related_conflicts:
  - truth-conflict-map §冲突5
  - CONFLICT-XDO-001 (Order 多端写入)
evidence:
  - truth-conflict-map.md §冲突5
  - broken-chain-map.md §BC-027, BC-029
  - state-machine-map.md
confidence: HIGH
severity: critical
```

---

### RC-006: 凭证状态前后端映射错位

```yaml
root_cause_id: RC-006
title: 凭证状态前后端映射错位
description: >
  凭证状态在后端存储 0-3 (DRAFT/APPROVED/POSTED/VOID)，
  前端展示 1-4，映射关系固定但不直观。新开发者易混淆映射导致
  状态显示或操作错误。
symptoms:
  - 后端状态: 0=DRAFT, 1=APPROVED, 2=POSTED, 3=VOID
  - 前端展示: 1=DRAFT, 2=APPROVED, 3=POSTED, 4=VOID
  - 新开发者混淆映射
affected_objects:
  - Voucher (凭证)
affected_domains:
  - Finance
upstream: FinanceService (凭证创建)
downstream:
  - FinanceReportService
  - 前端展示层
truth_source: 后端 0-3 为准 (RESOLVED)
current_behavior: >
  后端以 0-3 为准；前端映射层单独处理（已实现）。
expected_behavior: >
  映射逻辑封装在前端 adapter 层；
  统一前后端状态码（建议统一为 0-3）。
related_broken_chains:
  - BC-028 (凭证状态前后端错位)
related_lineage_risks:
  - 凭证过账后报表更新可能因状态错位失败
related_conflicts:
  - truth-conflict-map §冲突6
evidence:
  - truth-conflict-map.md §冲突6
  - broken-chain-map.md §BC-028
confidence: HIGH
severity: medium
```

---

### RC-007: Warehouse Foundation 缺失

```yaml
root_cause_id: RC-007
title: Warehouse Foundation 缺失
description: >
  Warehouse 作为 Foundation Object 被识别但无独立真相源表，
  仅通过 inventory.warehouse_id 引用。置信度 LOW，
  分类为 Embedded/Config 但未明确决策。
symptoms:
  - Foundation-map 标记 Warehouse 为 PARTIAL, LOW confidence
  - Warehouse 无独立表，依赖 inventory 表字段
  - Warehouse → Inventory 下游关系未在 downstream 中明确声明
  - 数据同步可能遗漏
affected_objects:
  - Warehouse (仓库)
  - Inventory (库存)
affected_domains:
  - Inventory
  - Management Core
upstream: Management Core (仓库配置)
downstream:
  - InventoryService
  - StoreInventoryService
  - DataScopeAspect
truth_source: 未确立
current_behavior: >
  Warehouse 无独立真相源，依赖 inventory 表。
expected_behavior: >
  确认 Warehouse 独立 Foundation 或嵌入式分类；
  明确 Warehouse → Inventory 的下游关系。
related_broken_chains:
  - BC-009 (采购 → 应付继承断裂，涉及仓库)
related_lineage_risks:
  - warehouseId Identity Loss (CONFLICT-ID-002)
related_conflicts:
  - CONFLICT-ID-002 (warehouseId Identity Loss)
  - CONFLICT-USDS-001 (Warehouse → Inventory 闭合 Gap)
  - CONFLICT-MF-001 (Warehouse 分类不明确)
evidence:
  - foundation-map.md §10: Warehouse PARTIAL, LOW
  - project-master-map-gap-analysis.md §2.1
  - broken-chain-map.md §BC-009
confidence: MEDIUM
severity: high
```

---

### RC-008: Unit Foundation 缺失

```yaml
root_cause_id: RC-008
title: Unit Foundation 缺失
description: >
  Unit (计量单位) 作为基础概念存在但无独立 Foundation Object，
  分类为 Embedded/Config 但未明确决策。影响物料和菜品的
  计量单位管理。
symptoms:
  - Unit 无独立 Foundation Object 定义
  - 计量单位散落在各业务表中
  - 物料和菜品的单位管理缺乏统一标准
affected_objects:
  - Unit (计量单位)
  - Material (物料)
  - Food (菜品)
affected_domains:
  - Management Core
  - Inventory
upstream: Management Core (单位配置)
downstream:
  - MaterialService
  - FoodService
  - InventoryService
truth_source: 未确立
current_behavior: >
  Unit 无独立 Foundation，嵌入在各业务模块中。
expected_behavior: >
  确认 Unit 独立 Foundation 或嵌入式分类；
  统一计量单位管理。
related_broken_chains: []
related_lineage_risks:
  - 物料和菜品的单位可能不一致
related_conflicts:
  - CONFLICT-MF-001 (Unit 分类不明确)
evidence:
  - project-master-map-gap-analysis.md §2.6
  - foundation-map.md (Unit 未列出为独立 Foundation)
confidence: MEDIUM
severity: medium
```

---

### RC-009: PaymentMethod Foundation 缺失

```yaml
root_cause_id: RC-009
title: PaymentMethod Foundation 缺失
description: >
  PaymentMethod (支付方式) 作为基础概念存在但无独立 Foundation Object，
  分类为 Embedded/Config 但未明确决策。影响收付款流程的
  支付方式管理。
symptoms:
  - PaymentMethod 无独立 Foundation Object 定义
  - 支付方式散落在各业务表中
  - 收付款流程缺乏统一的支付方式管理
affected_objects:
  - PaymentMethod (支付方式)
  - Payment (付款)
  - Receipt (收款)
affected_domains:
  - Finance
  - POS
upstream: Management Core (支付方式配置)
downstream:
  - PaymentService
  - ReceiptService
  - FundFlowService
truth_source: 未确立
current_behavior: >
  PaymentMethod 无独立 Foundation，嵌入在财务模块中。
expected_behavior: >
  确认 PaymentMethod 独立 Foundation 或嵌入式分类；
  统一支付方式管理。
related_broken_chains: []
related_lineage_risks:
  - 支付方式管理缺乏统一标准
related_conflicts:
  - CONFLICT-MF-001 (PaymentMethod 分类不明确)
evidence:
  - project-master-map-gap-analysis.md §2.6
  - foundation-map.md (PaymentMethod 未列出为独立 Foundation)
confidence: MEDIUM
severity: medium
```

---

### RC-010: Customer Foundation 缺失与产品决策依赖

```yaml
root_cause_id: RC-010
title: Customer Foundation 缺失与产品决策依赖
description: >
  Customer 的 Foundation 分类需要产品层面的决策，
  当前状态导致架构设计方向不明确。影响客户域的整体设计，
  可能需要重新评估域边界。
symptoms:
  - Customer Foundation 分类未决
  - 客户域设计方向不明确
  - 可能影响域边界划分
affected_objects:
  - Customer (客户)
  - Member (会员)
affected_domains:
  - Management Core
  - Mini Program
  - POS
upstream: Management Core (客户管理)
downstream:
  - MemberService
  - OrderService
  - MarketingService
truth_source: 未确立
current_behavior: >
  Customer Foundation 分类需要 Product Decision，当前未决。
expected_behavior: >
  产品决策明确 Customer Foundation 分类；
  更新架构设计。
related_broken_chains: []
related_lineage_risks:
  - 客户域设计可能需要重构
related_conflicts:
  - CONFLICT-MF-002 (Customer 需要 Product Decision)
  - PD-033 (反向链空白，待决)
evidence:
  - project-master-map-gap-analysis.md §2.6
  - project-master-map-conflict-registry.yaml §CONFLICT-MF-002
confidence: MEDIUM
severity: high
```

---

### RC-011: Price Foundation 缺失

```yaml
root_cause_id: RC-011
title: Price Foundation 缺失
description: >
  Price 被识别为 Embedded 模式，需要确认此分类是否正确，
  以及是否需要调整其在架构中的定位。影响价格相关的域设计
  和定价策略的灵活性。
symptoms:
  - Price 为 Embedded 模式，无独立 Foundation
  - 价格管理嵌入在各业务模块中
  - 定价策略灵活性受限
affected_objects:
  - Price (价格)
  - Food (菜品价格)
  - Material (物料价格)
affected_domains:
  - Management Core
  - POS
  - Finance
upstream: Management Core (价格配置)
downstream:
  - OrderService (价格计算)
  - FinanceService (成本计算)
  - PricingService
truth_source: foods.price (VERIFIED)
current_behavior: >
  Price 为 Embedded 模式，嵌入在 Foods 表中。
expected_behavior: >
  评估 Price 业务复杂度；
  确认 Embedded 模式是否合适；
  必要时调整分类。
related_broken_chains: []
related_lineage_risks:
  - 价格管理可能需要独立 Foundation
related_conflicts:
  - CONFLICT-MF-003 (Price Embedded 模式)
evidence:
  - project-master-map-gap-analysis.md §2.6
  - project-master-map-conflict-registry.yaml §CONFLICT-MF-003
confidence: MEDIUM
severity: medium
```

---

### RC-012: 多端写入冲突与数据所有权不明

```yaml
root_cause_id: RC-012
title: 多端写入冲突与数据所有权不明
description: >
  部分业务对象（如 Order）在多个终端都有写入权限，
  但主数据所有权未明确定义，可能导致数据冲突和不一致。
  POS 和 MiniProgram 都可以创建订单，写入冲突解决策略缺失。
symptoms:
  - Order 在 POS 和 MiniProgram 都有 OWNER 角色
  - 多端写入时主数据所有权不明确
  - 数据冲突和覆盖风险
  - 财务对账可能出错
affected_objects:
  - Order (订单)
  - StoreInventory (门店库存)
affected_domains:
  - POS
  - Mini Program
  - Management Core
  - Finance
upstream: POS, MiniProgram (多端写入)
downstream:
  - Finance (应收生成)
  - Inventory (库存扣减)
  - Kitchen (出餐)
truth_source: 需明确
current_behavior: >
  Order 在 POS 和 MiniProgram 都有写入权限，
  主数据所有权未定义。
expected_behavior: >
  明确 Order 的主数据源；
  定义写入冲突解决策略；
  在 ownership-matrix 中记录。
related_broken_chains:
  - BC-027 (订单状态三表并存)
related_lineage_risks:
  - 多端写入可能导致数据冲突
  - 订单状态可能不一致
related_conflicts:
  - CONFLICT-XDO-001 (Order 多端写入所有权不明确)
evidence:
  - project-master-map-conflict-registry.yaml §CONFLICT-XDO-001
  - cross-domain-coordination-matrix.md §5.1: Order OWNER = POS, MiniProgram
confidence: HIGH
severity: critical
```

---

### RC-013: 数据所有权不明确与跨域协调风险

```yaml
root_cause_id: RC-013
title: 数据所有权不明确与跨域协调风险
description: >
  跨域场景下对象 Ownership 规则不清晰，下游系统反向定义上游事实，
  形成复杂的双向依赖关系。事件驱动架构存在事件丢失风险，
  数据一致性挑战增加。
symptoms:
  - 下游系统反向更新上游数据
  - 事件驱动存在丢失风险
  - 跨域数据同步可能延迟或失败
  - 数据所有权分散
affected_objects:
  - 所有跨域业务对象
affected_domains:
  - Management Core
  - POS
  - Kitchen
  - Employee
  - Mini Program
  - Receiving Mobile
  - Inventory
  - Finance
upstream: 各业务域
downstream: 各业务域
truth_source: 需建立数据所有权清单
current_behavior: >
  数据所有权分散；事件驱动存在丢失风险；
  跨域数据同步依赖事件可靠性。
expected_behavior: >
  建立数据所有权清单；
  加强事件可靠性（持久化、重试、死信队列）；
  优化跨域缓存。
related_broken_chains:
  - BC-008 (订单 → 财务流水继承断裂)
  - BC-009 (采购 → 应付继承断裂)
  - BC-010 (凭证 → 财务报表继承断裂)
related_lineage_risks:
  - 数据血缘追踪可能不完整
  - 跨域数据同步风险
related_conflicts:
  - CONFLICT-USDS-001 (Upstream/Downstream 闭合 Gap)
  - cross-domain-coordination-matrix.md §6
evidence:
  - cross-domain-coordination-matrix.md §3: 反向依赖分析
  - cross-domain-coordination-matrix.md §6: 跨域协调风险矩阵
  - broken-chain-map.md §BC-008, BC-009, BC-010
confidence: HIGH
severity: high
```

---

### RC-014: 事件链断裂与继承缺失

```yaml
root_cause_id: RC-014
title: 事件链断裂与继承缺失
description: >
  核心业务事件链存在断裂，AutoVoucherService 零调用点，
  订单完成/采购入库后无法自动生成凭证，导致财务数据不完整。
  数据继承链路（订单→财务流水、采购→应付、凭证→报表）
  存在断裂风险。
symptoms:
  - AutoVoucherService 两个方法存在但无调用点
  - 订单完成后无法自动生成销售凭证
  - 采购入库后无法自动生成采购凭证
  - FinanceVoucherService 根包为 no-op stub
affected_objects:
  - AutoVoucherService
  - FinanceVoucherService
  - Order
  - Purchase
  - Voucher
affected_domains:
  - Finance
  - Management Core
upstream: OrderCompletedEvent, PurchaseStockInEvent
downstream:
  - VoucherService
  - FinanceReportService
truth_source: 事件消费者缺失
current_behavior: >
  AutoVoucherService 零调用点；
  凭证无法自动生成；
  财务数据不完整。
expected_behavior: >
  在 OrderCompletedEvent 监听器中调用 generateSalesVoucher；
  在 PurchaseStockInEvent 监听器中调用 generatePurchaseVoucher；
  完成凭证自动生成链路。
related_broken_chains:
  - BC-001 (AutoVoucherService 零调用点)
  - BC-002 (FinanceVoucherService 根包 no-op stub)
  - BC-004 (AutoVoucherService 未接入采购流程)
  - BC-005 (AutoVoucherService 未接入订单流程)
  - BC-008 (订单 → 财务流水继承断裂)
  - BC-009 (采购 → 应付继承断裂)
related_lineage_risks:
  - 凭证自动生成链路断裂
  - 财务数据继承不完整
related_conflicts: []
evidence:
  - broken-chain-map.md §BC-001, BC-002, BC-004, BC-005
  - broken-chain-map.md §BC-008, BC-009
confidence: HIGH
severity: high
```

---

### RC-015: 遗留表残留

```yaml
root_cause_id: RC-015
title: 遗留表残留
description: >
  大量遗留数据库表未清理，包括 orders_legacy、food、product、
  voucher_header、voucher_line、stores、dish_combo、dish_recipe 等。
  部分为死体系（voucher_header/voucher_line），完全无消费者。
symptoms:
  - 8+ 遗留表未清理
  - voucher_header/voucher_line 为死体系
  - 遗留表与新表可能数据不一致
  - 维护成本高
affected_objects:
  - orders_legacy
  - food
  - product
  - voucher_header
  - voucher_line
  - stores
  - dish_combo
  - dish_recipe
  - finance_record
  - device_status_log
affected_domains:
  - 全业务
upstream: 遗留系统
downstream: 无（已废弃）
truth_source: 新表为准
current_behavior: >
  遗留表未清理，与新表并存。
expected_behavior: >
  迁移数据后删除遗留表；
  消除死体系。
related_broken_chains:
  - BC-015 ~ BC-022 (Duplicate Master)
  - BC-022 (voucher_header 死体系)
related_lineage_risks:
  - 遗留表数据可能污染新表
related_conflicts:
  - truth-conflict-map §冲突3 (accounting_subjects.balance vs account_balance)
evidence:
  - legacy-map.md §1: Legacy 数据库表
  - broken-chain-map.md §BC-015 ~ BC-022
confidence: HIGH
severity: medium
```

---

### RC-016: 遗留服务残留

```yaml
root_cause_id: RC-016
title: 遗留服务残留
description: >
  8个 Legacy 财务服务实现存在但无消费者，包括
  legacyAccountingSubjectServiceImpl、legacyCostAllocationServiceImpl 等。
  13个硬件控制器全部废弃但未清理。7个 Legacy 接口未清理。
symptoms:
  - 8个 Legacy 财务服务实现无消费者
  - 13个硬件控制器全部废弃
  - 7个 Legacy 接口未清理
  - 可能与新服务冲突
affected_objects:
  - legacyAccountingSubjectServiceImpl
  - legacyCostAllocationServiceImpl
  - legacyElectronicVoucherServiceImpl
  - legacyFinanceApprovalServiceImpl
  - legacyFinancePrintServiceImpl
  - legacyFinanceVoucherServiceImpl
  - legacyFinanceReportServiceImpl
  - legacyPrintTemplateDataServiceImpl
  - 13个硬件控制器
affected_domains:
  - Finance
  - Management Core
upstream: 遗留系统
downstream: 无（已废弃）
truth_source: 新服务为准
current_behavior: >
  Legacy 服务和控制器未清理。
expected_behavior: >
  确认是否仍被需要；
  如不需要，删除 Legacy 服务和控制器。
related_broken_chains:
  - BC-003 (遗留财务服务零消费者)
  - BC-030 (遗留财务服务零消费者)
  - BC-031 (遗留硬件控制器全部废弃)
related_lineage_risks:
  - Legacy 服务可能与新服务冲突
related_conflicts: []
evidence:
  - legacy-map.md §2, §3
  - broken-chain-map.md §BC-003, BC-030, BC-031
confidence: HIGH
severity: medium
```

---

### RC-017: Dead Code 与隐藏机制

```yaml
root_cause_id: RC-017
title: Dead Code 与隐藏 Fallback 机制
description: >
  多处 Dead Code 未清理，包括 DatabaseFixConfig 死方法、
  TableInitConfig.createMemberTable()、PrintTemplateDataServiceImpl Stub 空实现、
  OrderMapper 12个 orders_legacy 统计方法、7个 DataService 缓存空实现。
  6个隐藏 Fallback 机制（OCR 引擎降级、硬件不可用 fallback 等）。
  3处直接数据库写入操作绕过 ORM。
symptoms:
  - DatabaseFixConfig 包含死方法
  - TableInitConfig.createMemberTable() 废弃
  - PrintTemplateDataServiceImpl 为 Stub 空实现
  - OrderMapper 包含12个 orders_legacy 统计方法
  - 7个 DataService 缓存空实现
  - 6个隐藏 Fallback 机制
  - 3处直接数据库写入操作
affected_objects:
  - DatabaseFixConfig
  - TableInitConfig
  - PrintTemplateDataServiceImpl
  - OrderMapper
  - 7个 DataService
affected_domains:
  - Management Core
  - Finance
upstream: 遗留开发
downstream: 无（Dead Code）
truth_source: 无
current_behavior: >
  Dead Code 和隐藏机制未清理。
expected_behavior: >
  清理 Dead Code；
  评估隐藏 Fallback 机制必要性；
  替换直接数据库写入为 ORM 操作。
related_broken_chains:
  - BC-002 (FinanceVoucherService 根包 no-op stub)
related_lineage_risks:
  - Dead Code 可能被误用
  - 隐藏机制可能导致不可预测的行为
related_conflicts: []
evidence:
  - legacy-map.md §4, §5, §6, §7
confidence: HIGH
severity: medium
```

---

## 三、Root Cause 关系图

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                           Root Cause 关系图                                      │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│  ┌──────────────┐     ┌──────────────┐     ┌──────────────┐                     │
│  │  RC-001      │     │  RC-002      │     │  RC-003      │                     │
│  │  food/foods  │     │  product/    │     │  stores/     │                     │
│  │  双写        │     │  material    │     │  stores_new  │                     │
│  └──────┬───────┘     └──────┬───────┘     └──────┬───────┘                     │
│         │                    │                    │                              │
│         └────────────────────┼────────────────────┘                              │
│                              │                                                   │
│                    ┌─────────▼─────────┐                                        │
│                    │  RC-015 遗留表残留 │                                        │
│                    └─────────┬─────────┘                                        │
│                              │                                                   │
│         ┌────────────────────┼────────────────────┐                              │
│         │                    │                    │                              │
│  ┌──────▼───────┐     ┌──────▼───────┐     ┌──────▼───────┐                    │
│  │  RC-004      │     │  RC-005      │     │  RC-006      │                    │
│  │  金额双轨    │     │  状态三套    │     │  凭证错位    │                    │
│  └──────┬───────┘     └──────┬───────┘     └──────┬───────┘                    │
│         │                    │                    │                              │
│         └────────────────────┼────────────────────┘                              │
│                              │                                                   │
│                    ┌─────────▼─────────┐                                        │
│                    │  RC-013 数据所有权 │                                        │
│                    │  不明确           │                                        │
│                    └─────────┬─────────┘                                        │
│                              │                                                   │
│         ┌────────────────────┼────────────────────┐                              │
│         │                    │                    │                              │
│  ┌──────▼───────┐     ┌──────▼───────┐     ┌──────▼───────┐                    │
│  │  RC-007      │     │  RC-012      │     │  RC-014      │                    │
│  │  Warehouse   │     │  多端写入    │     │  事件链断裂  │                    │
│  │  缺失        │     │  冲突        │     │              │                    │
│  └──────┬───────┘     └──────┬───────┘     └──────┬───────┘                    │
│         │                    │                    │                              │
│         └────────────────────┼────────────────────┘                              │
│                              │                                                   │
│                    ┌─────────▼─────────┐                                        │
│                    │  RC-016 遗留服务  │                                        │
│                    │  RC-017 Dead Code │                                        │
│                    └───────────────────┘                                        │
│                                                                                 │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

## 四、优先级矩阵

| 优先级 | Root Cause | 理由 | 截止日期 |
|--------|------------|------|----------|
| **P0 (立即)** | RC-001 food/foods 双写 | 数据一致性核心风险 | 2026-09-14 |
| **P0 (立即)** | RC-004 金额单位双轨 | 财务数据 100 倍偏差风险 | 2026-09-14 |
| **P0 (立即)** | RC-012 多端写入冲突 | 数据冲突核心风险 | 2026-09-14 |
| **P1 (本周)** | RC-002 product/material 边界不清 | 身份追踪错位 | 2026-09-20 |
| **P1 (本周)** | RC-003 stores/stores_new 双表 | 门店信息不一致 | 2026-09-20 |
| **P1 (本周)** | RC-005 订单状态三套并存 | 订单全流程混乱 | 2026-09-20 |
| **P1 (本周)** | RC-010 Customer Foundation 缺失 | 需产品决策 | 2026-09-20 |
| **P1 (本周)** | RC-014 事件链断裂 | 财务数据不完整 | 2026-09-20 |
| **P2 (下周)** | RC-007 Warehouse 缺失 | Foundation 不完整 | 2026-09-30 |
| **P2 (下周)** | RC-013 数据所有权不明 | 跨域协调风险 | 2026-09-30 |
| **P2 (下周)** | RC-015 遗留表残留 | 维护成本 | 2026-09-30 |
| **P2 (下周)** | RC-016 遗留服务残留 | 维护成本 | 2026-09-30 |
| **P2 (下周)** | RC-017 Dead Code | 维护成本 | 2026-09-30 |
| **P3 (持续)** | RC-008 Unit 缺失 | 架构完善 | 2026-10-15 |
| **P3 (持续)** | RC-009 PaymentMethod 缺失 | 架构完善 | 2026-10-15 |
| **P3 (持续)** | RC-011 Price 缺失 | 架构完善 | 2026-10-15 |
| **P3 (持续)** | RC-006 凭证状态错位 | 已有映射层 | 2026-10-15 |

---

## 五、修复路径

### Phase 1: 紧急修复 (2026-09-09 ~ 2026-09-14)

1. **RC-001**: 确认 foods 为唯一真相源，制定 food 表废弃计划
2. **RC-004**: 扫描所有跨表金额查询，添加单位转换注释
3. **RC-012**: 明确 Order 主数据源，定义写入冲突解决策略

### Phase 2: 高优先级修复 (2026-09-14 ~ 2026-09-20)

1. **RC-002**: 统一 material_id 语义，标记 product_id 为技术债
2. **RC-003**: 确认 stores_new 为唯一真相源，制定 stores 表废弃计划
3. **RC-005**: 明确三套状态机的使用场景，建立映射关系
4. **RC-010**: 组织产品决策会议，明确 Customer Foundation 分类
5. **RC-014**: 接入 AutoVoucherService 到事件监听器

### Phase 3: 中优先级修复 (2026-09-20 ~ 2026-09-30)

1. **RC-007**: 评估 Warehouse 独立 Foundation 或嵌入式分类
2. **RC-013**: 建立数据所有权清单，加强事件可靠性
3. **RC-015 ~ RC-017**: 清理遗留表、服务和 Dead Code

### Phase 4: 架构完善 (2026-10-01 ~ 2026-10-15)

1. **RC-008 ~ RC-011**: 评估 Unit、PaymentMethod、Price 的 Foundation 分类
2. **RC-006**: 统一前后端状态码

---

## 六、待产品决策项

| # | 决策编号 | 决策项 | 状态 | 关联 Root Cause |
|---|----------|--------|------|-----------------|
| 1 | PD-033 | 反向链空白 | ❌ **待决** | RC-013, RC-014 |
| 2 | PD-031 | 科目余额双轨 | ✅ **已决** | - |
| 3 | NEW | Customer Foundation 分类 | ❌ **待决** | RC-010 |
| 4 | NEW | Warehouse Foundation 分类 | ❌ **待决** | RC-007 |
| 5 | NEW | Unit Foundation 分类 | ❌ **待决** | RC-008 |
| 6 | NEW | PaymentMethod Foundation 分类 | ❌ **待决** | RC-009 |
| 7 | NEW | Price Foundation 分类 | ❌ **待决** | RC-011 |

---

## 七、维护说明

1. 本文件随系统问题修复进展更新
2. 新增 Root Cause 需同时更新「Root Cause 详细注册」和「优先级矩阵」
3. Root Cause 状态变更（OPEN → IN_PROGRESS → RESOLVED → CLOSED）需更新
4. 产品决策结果需更新「待产品决策项」
5. 本文件数据来源：
   - truth-conflict-map.md
   - broken-chain-map.md
   - legacy-map.md
   - cross-domain-coordination-matrix.md
   - foundation-map.md
   - data-lineage-map.md
   - project-master-map-conflict-registry.yaml
   - project-master-map-gap-analysis.md

---

*生成时间: 2026-09-09*
*评审编号: PROJECT-MASTER-REVIEW-001*
