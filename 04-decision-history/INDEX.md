# ============================================================================
# Foundation Decision Recon — 审议索引
# ============================================================================
# 决策编号: DEC-001 ~ DEC-005
# 生成日期: 2026-09-09
# 基于: Project Master Map 静态分析
# ============================================================================

meta:
  id: FOUNDATION-DECISION-RECON-001
  type: decision_index
  scope: foundation_decisions
  total_decisions: 5
  by_status:
    OPEN: 5
  by_priority:
    P0: 1
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

decision_dependencies:
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
    name: "Foundation Stabilization"
    decisions: [DEC-001]
    deadline: "2026-09-16"
    description: "Warehouse 是 P0 级阻断，必须优先决策"
  phase_2:
    name: "Foundation Clarification"
    decisions: [DEC-002, DEC-003, DEC-004]
    deadline: "2026-09-23"
    description: "Unit/PaymentMethod/Customer 是 P1 级，可分阶段处理"
  phase_3:
    name: "Foundation Enhancement"
    decisions: [DEC-005]
    deadline: "2026-09-30"
    description: "Price 是 P2 级，可最后处理"

summary_table:
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
