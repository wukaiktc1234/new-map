# ============================================================================
# Decision History — 审议索引
# ============================================================================
# 决策编号: DEC-001 ~ DEC-005, PD-CANONICAL-001
# 生成日期: 2026-09-10
# 基于: Project Master Map 静态分析 + BUSINESS-ITEM-CANONICAL-SEMANTIC-REASSESSMENT-001
# ============================================================================

meta:
  id: DECISION-HISTORY-INDEX-001
  type: decision_index
  scope: foundation_decisions + product_decisions
  total_decisions: 6
  by_status:
    OPEN: 5
    RECOMMENDED: 1
  by_priority:
    P0: 2
    P1: 3
    P2: 1

decisions:
  DEC-001:
    title: "Warehouse 对象归属与定义"
    file: "DEC-001-Warehouse-Decision.yaml"
    priority: P0
    blocking: true
    question: "Warehouse 是 Independent Foundation / Store Location / Inventory Location / Other？"
    recommendation: "E_hybrid — 中央仓独立管理 + 门店仓隐含在 Store 中"
    affected_domains: [Inventory, Procurement, HR]
   阻断能力: "库存管理、仓库间调拨、库存盘点"

  DEC-002:
    title: "Unit 对象归属与定义"
    file: "DEC-002-Unit-Decision.yaml"
    priority: P1
    blocking: false
    question: "Unit 是 Independent Master / Material Attribute / Product Attribute / Configuration / Embedded Enumeration？"
    recommendation: "A_enum — 当前保持枚举，未来按需升级"
    affected_domains: [Product, Inventory, Procurement]
    阻断能力: "物料/菜品管理一致性"

  DEC-003:
    title: "PaymentMethod 对象归属与定义"
    file: "DEC-003-PaymentMethod-Decision.yaml"
    priority: P1
    blocking: false
    question: "PaymentMethod 是 Master Data / Configuration / Enum / Finance Policy / External Payment Provider？"
    recommendation: "A_enum — 当前保持枚举，未来按需升级"
    affected_domains: [POS, Finance, Marketing, Order]
    阻断能力: "支付流程完整性"

  DEC-004:
    title: "Customer 与 Member 的关系"
    file: "DEC-004-CustomerMember-Decision.yaml"
    priority: P1
    blocking: false
    question: "Customer = Member ? Customer ≠ Member ? Customer ⊂ Member ? Customer ⊃ Member ?"
    recommendation: "E_guest_member — 散客作为 GUEST 类型存储在 members 表"
    affected_domains: [Order, Finance, Marketing, Mini Program]
    阻断能力: "客户管理和财务应收"

  DEC-005:
    title: "Price 对象归属与定义"
    file: "DEC-005-Price-Decision.yaml"
    priority: P2
    blocking: false
    question: "Price 是 Product Attribute / Store Operating Scope Data / Price Master / Price Policy / Derived Selling Price？"
    recommendation: "A_inline — 当前保持内联，未来按需升级"
    affected_domains: [Product, Order, Procurement, Marketing, Finance]
    阻断能力: "价格历史和策略管理"

  PD-CANONICAL-001:
    title: "Canonical Business Item 定义"
    file: "PD-CANONICAL-001-Decision.yaml"
    priority: P0
    blocking: true
    question: "Business Item 是否应该拥有独立 Canonical Identity？"
    recommendation: "B_unified_canonical — 引入统一 Canonical Business Item Identity 层"
    affected_domains: [POS, Procurement, Inventory, Recipe, Reporting, Marketing]
    阻断能力: "所有后续 Product / Architecture 语义决策 (PD-CANONICAL-002~006, AD-IDENTITY-001, DE-PK-001, DE-SKU-001)"
    decision_status: "RECOMMENDED (NOT CONFIRMED)"
    evidence_status: "VERIFIED"

decision_dependencies:
  - from: PD-CANONICAL-001
    to: PD-CANONICAL-002
    description: "Canonical Identity 层是 Product 语义决策的前提"
  - from: PD-CANONICAL-001
    to: PD-CANONICAL-003
    description: "Canonical Identity 层是 Food 语义决策的前提"
  - from: PD-CANONICAL-001
    to: PD-CANONICAL-004
    description: "Canonical Identity 层是 Material 语义决策的前提"
  - from: PD-CANONICAL-001
    to: AD-IDENTITY-001
    description: "Canonical Identity 定义是 Identity Storage 设计的前提"
  - from: PD-CANONICAL-001
    to: DE-PK-001
    description: "Canonical Identity 定义是 Primary Key 设计的前提"
  - from: DEC-001
    to: DEC-005
    description: "Warehouse 管理模式影响库存成本计算，进而影响价格体系"
  - from: DEC-002
    to: DEC-005
    description: "Unit 管理模式影响成本换算，进而影响价格计算"
  - from: DEC-004
    to: DEC-003
    description: "Customer/Member 关系影响会员支付方式"

execution_order:
  phase_1:
    name: "Canonical Identity Foundation"
    decisions: [PD-CANONICAL-001]
    deadline: "2026-09-10"
    description: "PD-CANONICAL-001 是 P0 级阻断，必须优先确认"
    status: "RECOMMENDED (NOT CONFIRMED)"
  phase_2:
    name: "Foundation Stabilization"
    decisions: [DEC-001]
    deadline: "2026-09-16"
    description: "Warehouse 是 P0 级阻断，必须优先决策"
  phase_3:
    name: "Product Semantic Decisions"
    decisions: [PD-CANONICAL-002, PD-CANONICAL-003, PD-CANONICAL-004, PD-CANONICAL-006]
    deadline: "2026-09-17"
    description: "Product 语义决策，依赖 PD-CANONICAL-001 确认"
  phase_4:
    name: "Architecture Decisions"
    decisions: [AD-IDENTITY-001, AD-LOCATION-001, AD-UOM-001, AD-INVENTORY-001, AD-INVENTORY-002]
    deadline: "2026-09-23"
    description: "Architecture 决策，依赖 Product Decisions"
  phase_5:
    name: "Foundation Clarification"
    decisions: [DEC-002, DEC-003, DEC-004]
    deadline: "2026-09-23"
    description: "Unit/PaymentMethod/Customer 是 P1 级，可分阶段处理"
  phase_6:
    name: "Foundation Enhancement"
    decisions: [DEC-005]
    deadline: "2026-09-30"
    description: "Price 是 P2 级，可最后处理"
  phase_7:
    name: "Engineering Decisions"
    decisions: [DE-PK-001, DE-SKU-001, DE-STORAGE-001, DE-MIGRATION-001]
    deadline: "2026-09-30"
    description: "Engineering 决策，依赖 Architecture Decisions"

summary_table:
  - decision: PD-CANONICAL-001
    what: "Canonical Business Item"
    is_what: "Unified Canonical Identity (B_unified_canonical)"
    needs_independent_table: "是（canonical_business_item）"
    recommendation: "引入统一 Canonical Business Item Identity 层，所有子类型共享 UUID + canonical_name + domain_tags"
    decision_status: "RECOMMENDED (NOT CONFIRMED)"
  - decision: DEC-001
    what: "Warehouse"
    is_what: "Independent Foundation (E_hybrid)"
    needs_independent_table: "是（中央仓）"
    recommendation: "创建 warehouses 表（仅中央仓）"
  - decision: DEC-002
    what: "Unit"
    is_what: "Embedded Enumeration (A_enum)"
    needs_independent_table: "否（当前）"
    recommendation: "保持枚举，文档化单位规范"
  - decision: DEC-003
    what: "PaymentMethod"
    is_what: "Enum (A_enum)"
    needs_independent_table: "否（当前）"
    recommendation: "保持枚举，文档化支付方式"
  - decision: DEC-004
    what: "Customer/Member"
    is_what: "Customer ⊃ Member (E_guest_member)"
    needs_independent_table: "否（散客作为 GUEST 类型）"
    recommendation: "members 表增加 memberType 字段"
  - decision: DEC-005
    what: "Price"
    is_what: "Product Attribute (A_inline)"
    needs_independent_table: "否（当前）"
    recommendation: "保持内联，验证快照机制"
