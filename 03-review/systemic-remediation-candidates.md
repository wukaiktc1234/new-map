# 系统治理候选清单 — Systemic Remediation Candidates

> **评审编号**: PROJECT-MASTER-REVIEW-001
> **版本**: 1.0.0
> **生成日期**: 2026-09-09
> **状态**: CANDIDATE (非 Engineering Task)
> **范围**: 基于 systemic-problem-register.md 中 Root Cause 的治理候选

---

## 一、候选总览

| 统计项 | 数量 |
|--------|------|
| 候选总数 | 17 |
| P0 (阻断核心/破坏Truth/严重跨域) | 3 |
| P1 (核心能力缺失/高风险数据冲突) | 6 |
| P2 (局部业务问题) | 6 |
| P3 (Legacy/Cleanup/Optimization) | 2 |

| 分类 | 候选编号 | 数量 |
|------|----------|------|
| Identity/Truth Source 冲突 | SR-001 ~ SR-006 | 6 |
| Foundation 缺失 | SR-007 ~ SR-011 | 5 |
| Cross-Domain 问题 | SR-012 ~ SR-014 | 3 |
| Legacy 问题 | SR-015 ~ SR-017 | 3 |

---

## 二、Dependency-aware Priority 链

```
Foundation ──→ Master Data ──→ Identity ──→ Truth Source ──→ Cross-Domain Contract ──→ Legacy Cleanup
    │              │              │              │                  │                      │
 SR-007~011     SR-001        SR-002        SR-003~006        SR-012~014             SR-015~017
 Warehouse      Product/      订单状态机    金额/凭证/        数据所有权/            遗留表/
 Unit           Material/                   双写解决          事件可靠性/            服务/
 PaymentMethod  Food                                                多端写入            Dead Code
 Customer
 Price
```

**执行顺序**: Foundation → Master Data → Identity → Truth Source → Cross-Domain Contract → Legacy Cleanup

---

## 三、治理候选详细清单

---

### SR-001: 统一 Product/Material/Food Identity

```yaml
candidate_id: SR-001
root_cause:
  - RC-001: food/foods 双写与身份冲突
  - RC-002: product/material 边界不清与遗留列名
why_it_matters: >
  菜品(food/foods)和物料(material_archives)的 Identity Boundary 混乱，
  导致下单、退款、库存等核心操作同时写入新旧两表，身份归属不明确。
  orders 表中 product_id 列名指向物料而非菜品，开发者误以为指向 foods 表，
  造成数据追踪错位。
affected_domains:
  - Management Core
  - POS
  - Inventory
  - Finance
required_decision: >
  1. 确认 foods 为菜品唯一 Truth Source
  2. 确认 material_archives 为物料唯一 Truth Source
  3. 制定 food 表废弃计划（待旧模块迁移完成）
  4. 新代码一律使用 material_id 语义替代 product_id
likely_remediation_scope: >
  - 食品溯源系统全域商品身份统一
  - 涉及 OrderService、InventoryService、MenuService 等核心服务
  - 数据库迁移 + 代码改造
dependencies:
  - SR-007 (Warehouse Foundation — 库存体系依赖仓库基础)
  - SR-011 (Price Foundation — 价格体系依赖商品身份)
priority: P0
blocked_by:
  - RC-001 food/foods 双写状态: OPEN
  - RC-002 product/material 边界状态: OPEN
depends_on: []
unlocks:
  - SR-002 (订单状态机 — 订单依赖商品身份)
  - SR-005 (inventory/store_inventory 双写 — 库存依赖商品身份)
  - SR-006 (成本双写 — 成本依赖商品身份)
  - SR-013 (数据所有权 — 跨域依赖商品身份)
```

---

### SR-002: 统一订单状态机

```yaml
candidate_id: SR-002
root_cause:
  - RC-005: 订单状态三套并存
why_it_matters: >
  订单实体存在 order_status (0-6)、payment_status (0-3)、legacy (-1, 0-7)
  三套状态字段并存，状态值和语义不一致。sales_order 表状态与 order_status
  无映射关系。状态转换逻辑分散在多个服务中，难以保证一致性。
affected_domains:
  - Management Core
  - POS
  - Kitchen
  - Employee
  - Mini Program
  - Finance
required_decision: >
  1. 明确三套状态机的使用场景和生命周期
  2. 建立 sales_order 与 order_status 的映射关系
  3. 确定状态转换的唯一入口
likely_remediation_scope: >
  - 订单全流程状态管理统一
  - 涉及 OrderService、KitchenService、FinanceService 等
  - 状态机重构 + 映射层建立
dependencies:
  - SR-001 (商品 Identity — 订单依赖商品身份)
  - SR-012 (多端写入 — 订单多端写入需先明确所有权)
priority: P1
blocked_by:
  - RC-005 订单状态三套并存状态: OPEN
  - SR-001 (商品 Identity 统一)
  - SR-012 (多端写入冲突解决)
depends_on:
  - SR-001
  - SR-012
unlocks:
  - SR-014 (事件链断裂 — 事件监听依赖订单状态)
```

---

### SR-003: 统一金额单位

```yaml
candidate_id: SR-003
root_cause:
  - RC-004: 金额单位双轨（分/元）
why_it_matters: >
  新表统一用「分」存储金额，但 tax_record、account_balance 等遗留表仍用「元」；
  finance_records 表注释中金额单位标注混乱。跨表 JOIN/汇总时单位不一致，
  导致金额差 100 倍。税务报表金额可能偏差 100 倍。
affected_domains:
  - Finance
  - Management Core
  - POS
required_decision: >
  1. 确认「分」为唯一金额单位标准
  2. 制定 tax_record/account_balance 的迁移或转换方案
  3. 修正 finance_records 注释
likely_remediation_scope: >
  - 财务模块金额单位统一
  - 涉及 FinanceReportService、FundFlowService、税务报表
  - 数据迁移 + 注释修正 + 转换层
dependencies:
  - SR-010 (Customer Foundation — 财务依赖客户基础)
  - SR-009 (PaymentMethod Foundation — 支付依赖支付方式基础)
priority: P0
blocked_by:
  - RC-004 金额单位双轨状态: OPEN
depends_on:
  - SR-010
  - SR-009
unlocks:
  - SR-006 (成本双写 — 成本依赖金额单位统一)
  - SR-014 (事件链断裂 — 凭证生成依赖金额单位)
```

---

### SR-004: 解决凭证状态映射

```yaml
candidate_id: SR-004
root_cause:
  - RC-006: 凭证状态前后端映射错位
why_it_matters: >
  凭证状态在后端存储 0-3 (DRAFT/APPROVED/POSTED/VOID)，
  前端展示 1-4，映射关系固定但不直观。新开发者易混淆映射导致
  状态显示或操作错误。虽已有 adapter 层处理，但映射逻辑散落。
affected_domains:
  - Finance
required_decision: >
  1. 评估是否统一前后端状态码为 0-3
  2. 确认 adapter 层映射逻辑的封装位置
  3. 更新开发者文档
likely_remediation_scope: >
  - 凭证状态码统一
  - 前端 adapter 层重构
  - 开发者文档更新
dependencies:
  - SR-003 (金额单位 — 凭证依赖金额单位统一)
priority: P2
blocked_by:
  - RC-006 凭证状态前后端映射错位状态: OPEN
  - SR-003 (金额单位统一)
depends_on:
  - SR-003
unlocks: []
```

---

### SR-005: 解决 inventory/store_inventory 双写

```yaml
candidate_id: SR-005
root_cause:
  - RC-013 (部分): 多端写入冲突与数据所有权不明
why_it_matters: >
  采购入库、调拨、收货等操作按类型双写 inventory 和 store_inventory 两表，
  但双写事务边界不一致，可能导致中央仓/门店仓库存不同步。
  跨域数据同步风险高。
affected_domains:
  - Inventory
  - Management Core
  - POS
required_decision: >
  1. 确认 inventory/store_inventory 各自独立管理（中央仓 vs 门店仓）
  2. 确保所有双写路径在同一事务内
  3. 监控异步任务成功率
likely_remediation_scope: >
  - 库存双写事务边界统一
  - 涉及 InventoryService、StoreInventoryService、PurchaseService
  - 事务重构 + 监控告警
dependencies:
  - SR-001 (商品 Identity — 库存依赖商品身份)
  - SR-007 (Warehouse Foundation — 库存依赖仓库基础)
priority: P1
blocked_by:
  - SR-001 (商品 Identity 统一)
  - SR-007 (Warehouse Foundation 建立)
depends_on:
  - SR-001
  - SR-007
unlocks: []
```

---

### SR-006: 解决成本双写

```yaml
candidate_id: SR-006
root_cause:
  - RC-013 (部分): 数据所有权不明确与跨域协调风险
why_it_matters: >
  persistOrderCost (主事务) 和 recordOrderCost (异步) 存在成本双写，
  异步任务失败可能导致成本数据不完整。补偿机制不完善。
affected_domains:
  - Finance
  - Management Core
required_decision: >
  1. 评估成本双写的必要性
  2. 如果保留双写，确保异步任务有补偿机制
  3. 监控异步任务成功率
likely_remediation_scope: >
  - 成本记录事务统一
  - 涉及 FinanceService、OrderService
  - 事务重构 + 补偿机制 + 监控
dependencies:
  - SR-003 (金额单位 — 成本依赖金额单位统一)
  - SR-001 (商品 Identity — 成本依赖商品身份)
priority: P1
blocked_by:
  - SR-003 (金额单位统一)
  - SR-001 (商品 Identity 统一)
depends_on:
  - SR-003
  - SR-001
unlocks: []
```

---

### SR-007: 建立 Warehouse Foundation

```yaml
candidate_id: SR-007
root_cause:
  - RC-007: Warehouse Foundation 缺失
why_it_matters: >
  Warehouse 作为 Foundation Object 被识别但无独立真相源表，
  仅通过 inventory.warehouse_id 引用。置信度 LOW，
  分类为 Embedded/Config 但未明确决策。影响库存管理和仓库间调拨。
affected_domains:
  - Inventory
  - Management Core
required_decision: >
  1. 确认 Warehouse 独立 Foundation 或嵌入式分类
  2. 如独立 Foundation，建立 Warehouse 真相源表
  3. 明确 Warehouse → Inventory 的下游关系
likely_remediation_scope: >
  - 仓库基础数据模型建立
  - 涉及 InventoryService、StoreInventoryService、DataScopeAspect
  - 数据模型设计 + 迁移
dependencies: []
priority: P2
blocked_by:
  - RC-007 Warehouse Foundation 缺失状态: OPEN
depends_on: []
unlocks:
  - SR-001 (商品 Identity — 库存依赖仓库基础)
  - SR-005 (inventory/store_inventory 双写 — 库存依赖仓库基础)
```

---

### SR-008: 建立 Unit Foundation

```yaml
candidate_id: SR-008
root_cause:
  - RC-008: Unit Foundation 缺失
why_it_matters: >
  Unit (计量单位) 作为基础概念存在但无独立 Foundation Object，
  分类为 Embedded/Config 但未明确决策。影响物料和菜品的
  计量单位管理，可能导致单位不一致。
affected_domains:
  - Management Core
  - Inventory
required_decision: >
  1. 确认 Unit 独立 Foundation 或嵌入式分类
  2. 如独立 Foundation，建立 Unit 真相源表
  3. 统一计量单位管理标准
likely_remediation_scope: >
  - 计量单位基础数据模型建立
  - 涉及 MaterialService、FoodService、InventoryService
  - 数据模型设计 + 迁移
dependencies: []
priority: P2
blocked_by:
  - RC-008 Unit Foundation 缺失状态: OPEN
depends_on: []
unlocks:
  - SR-001 (商品 Identity — 商品依赖单位基础)
```

---

### SR-009: 建立 PaymentMethod Foundation

```yaml
candidate_id: SR-009
root_cause:
  - RC-009: PaymentMethod Foundation 缺失
why_it_matters: >
  PaymentMethod (支付方式) 作为基础概念存在但无独立 Foundation Object，
  分类为 Embedded/Config 但未明确决策。影响收付款流程的
  支付方式管理，可能导致支付方式管理缺乏统一标准。
affected_domains:
  - Finance
  - POS
required_decision: >
  1. 确认 PaymentMethod 独立 Foundation 或嵌入式分类
  2. 如独立 Foundation，建立 PaymentMethod 真相源表
  3. 统一支付方式管理标准
likely_remediation_scope: >
  - 支付方式基础数据模型建立
  - 涉及 PaymentService、ReceiptService、FundFlowService
  - 数据模型设计 + 迁移
dependencies: []
priority: P2
blocked_by:
  - RC-009 PaymentMethod Foundation 缺失状态: OPEN
depends_on: []
unlocks:
  - SR-003 (金额单位 — 支付依赖支付方式基础)
```

---

### SR-010: 建立 Customer Foundation

```yaml
candidate_id: SR-010
root_cause:
  - RC-010: Customer Foundation 缺失与产品决策依赖
why_it_matters: >
  Customer 的 Foundation 分类需要产品层面的决策，
  当前状态导致架构设计方向不明确。影响客户域的整体设计，
  可能需要重新评估域边界。
affected_domains:
  - Management Core
  - Mini Program
  - POS
required_decision: >
  1. 产品决策明确 Customer Foundation 分类
  2. 更新架构设计
  3. 评估域边界是否需要调整
likely_remediation_scope: >
  - 客户基础数据模型建立
  - 涉及 MemberService、OrderService、MarketingService
  - 产品决策 + 架构设计 + 数据模型
dependencies: []
priority: P1
blocked_by:
  - RC-010 Customer Foundation 缺失状态: OPEN
  - PD-033 (反向链空白，待决)
depends_on: []
unlocks:
  - SR-003 (金额单位 — 财务依赖客户基础)
  - SR-002 (订单状态 — 订单依赖客户基础)
```

---

### SR-011: 建立 Price Foundation

```yaml
candidate_id: SR-011
root_cause:
  - RC-011: Price Foundation 缺失
why_it_matters: >
  Price 被识别为 Embedded 模式，需要确认此分类是否正确，
  以及是否需要调整其在架构中的定位。影响价格相关的域设计
  和定价策略的灵活性。
affected_domains:
  - Management Core
  - POS
  - Finance
required_decision: >
  1. 评估 Price 业务复杂度
  2. 确认 Embedded 模式是否合适
  3. 必要时调整分类
likely_remediation_scope: >
  - 价格基础数据模型评估
  - 涉及 OrderService、FinanceService、PricingService
  - 架构评估 + 必要时重构
dependencies: []
priority: P3
blocked_by:
  - RC-011 Price Foundation 缺失状态: OPEN
depends_on: []
unlocks:
  - SR-001 (商品 Identity — 商品依赖价格基础)
```

---

### SR-012: 明确跨域数据所有权

```yaml
candidate_id: SR-012
root_cause:
  - RC-012: 多端写入冲突与数据所有权不明
why_it_matters: >
  部分业务对象（如 Order）在多个终端都有写入权限，
  但主数据所有权未明确定义，可能导致数据冲突和不一致。
  POS 和 MiniProgram 都可以创建订单，写入冲突解决策略缺失。
affected_domains:
  - POS
  - Mini Program
  - Management Core
  - Finance
required_decision: >
  1. 明确 Order 的主数据源
  2. 定义写入冲突解决策略
  3. 在 ownership-matrix 中记录
likely_remediation_scope: >
  - 跨域数据所有权体系建立
  - 涉及 OrderService、StoreInventoryService、FinanceService
  - 架构设计 + 冲突策略 + 文档
dependencies:
  - SR-001 (商品 Identity — 跨域依赖商品身份)
priority: P0
blocked_by:
  - RC-012 多端写入冲突状态: OPEN
  - SR-001 (商品 Identity 统一)
depends_on:
  - SR-001
unlocks:
  - SR-002 (订单状态机 — 订单状态依赖所有权明确)
  - SR-005 (inventory/store_inventory 双写 — 库存依赖所有权明确)
```

---

### SR-013: 加强事件驱动可靠性

```yaml
candidate_id: SR-013
root_cause:
  - RC-013: 数据所有权不明确与跨域协调风险
  - RC-014: 事件链断裂与继承缺失
why_it_matters: >
  跨域场景下对象 Ownership 规则不清晰，下游系统反向定义上游事实，
  形成复杂的双向依赖关系。事件驱动架构存在事件丢失风险，
  数据一致性挑战增加。AutoVoucherService 零调用点，
  订单完成/采购入库后无法自动生成凭证，导致财务数据不完整。
affected_domains:
  - Management Core
  - POS
  - Kitchen
  - Employee
  - Mini Program
  - Receiving Mobile
  - Inventory
  - Finance
required_decision: >
  1. 建立数据所有权清单
  2. 加强事件可靠性（持久化、重试、死信队列）
  3. 优化跨域缓存
  4. 接入 AutoVoucherService 到事件监听器
likely_remediation_scope: >
  - 事件驱动架构可靠性提升
  - 涉及所有跨域业务对象
  - 事件基础设施 + 消费者接入 + 监控
dependencies:
  - SR-012 (数据所有权 — 事件依赖所有权明确)
  - SR-002 (订单状态 — 事件监听依赖订单状态)
  - SR-003 (金额单位 — 凭证生成依赖金额单位)
priority: P1
blocked_by:
  - RC-013 数据所有权不明确状态: OPEN
  - RC-014 事件链断裂状态: OPEN
  - SR-012 (数据所有权明确)
  - SR-002 (订单状态机统一)
  - SR-003 (金额单位统一)
depends_on:
  - SR-012
  - SR-002
  - SR-003
unlocks: []
```

---

### SR-014: 解决多端写入冲突

```yaml
candidate_id: SR-014
root_cause:
  - RC-012 (部分): 多端写入冲突与数据所有权不明
why_it_matters: >
  Order 在 POS 和 MiniProgram 都有写入权限，
  主数据所有权未定义。多端写入时可能导致数据冲突和覆盖，
  财务对账可能出错。
affected_domains:
  - POS
  - Mini Program
  - Management Core
  - Finance
required_decision: >
  1. 明确 Order 的主数据源（POS 或 MiniProgram）
  2. 定义写入冲突解决策略（Last Write Wins / 悲观锁 / 乐观锁）
  3. 在 ownership-matrix 中记录
likely_remediation_scope: >
  - 多端写入冲突解决
  - 涉及 OrderService、POS、MiniProgram
  - 冲突策略 + 代码改造 + 测试
dependencies:
  - SR-012 (数据所有权 — 多端写入依赖所有权明确)
  - SR-002 (订单状态 — 多端写入依赖状态机统一)
priority: P0
blocked_by:
  - RC-012 多端写入冲突状态: OPEN
  - SR-012 (数据所有权明确)
  - SR-002 (订单状态机统一)
depends_on:
  - SR-012
  - SR-002
unlocks: []
```

---

### SR-015: 清理 Legacy 数据库表

```yaml
candidate_id: SR-015
root_cause:
  - RC-015: 遗留表残留
why_it_matters: >
  大量遗留数据库表未清理，包括 orders_legacy、food、product、
  voucher_header、voucher_line、stores、dish_combo、dish_recipe 等。
  部分为死体系（voucher_header/voucher_line），完全无消费者。
  维护成本高，遗留表数据可能污染新表。
affected_domains:
  - 全业务
required_decision: >
  1. 确认各遗留表是否仍有消费者
  2. 制定数据迁移计划
  3. 迁移完成后删除遗留表
likely_remediation_scope: >
  - 遗留数据库表清理
  - 涉及全业务模块
  - 数据迁移 + 表删除 + 验证
dependencies:
  - SR-001 (商品 Identity — 遗留表清理依赖商品身份统一)
  - SR-002 (订单状态 — 遗留表清理依赖订单状态统一)
  - SR-003 (金额单位 — 遗留表清理依赖金额单位统一)
priority: P2
blocked_by:
  - RC-015 遗留表残留状态: OPEN
  - SR-001 (商品 Identity 统一)
  - SR-002 (订单状态机统一)
  - SR-003 (金额单位统一)
depends_on:
  - SR-001
  - SR-002
  - SR-003
unlocks: []
```

---

### SR-016: 清理 Legacy 服务

```yaml
candidate_id: SR-016
root_cause:
  - RC-016: 遗留服务残留
why_it_matters: >
  8个 Legacy 财务服务实现存在但无消费者，包括
  legacyAccountingSubjectServiceImpl、legacyCostAllocationServiceImpl 等。
  13个硬件控制器全部废弃但未清理。7个 Legacy 接口未清理。
  可能与新服务冲突。
affected_domains:
  - Finance
  - Management Core
required_decision: >
  1. 确认 Legacy 服务是否仍被需要
  2. 如不需要，删除 Legacy 服务和控制器
  3. 清理 Legacy 接口
likely_remediation_scope: >
  - Legacy 服务和控制器清理
  - 涉及 Finance、Management Core
  - 服务删除 + 控制器删除 + 接口清理
dependencies:
  - SR-015 (遗留表清理 — 服务清理依赖表清理)
priority: P2
blocked_by:
  - RC-016 遗留服务残留状态: OPEN
  - SR-015 (遗留表清理)
depends_on:
  - SR-015
unlocks: []
```

---

### SR-017: 清理 Dead Code

```yaml
candidate_id: SR-017
root_cause:
  - RC-017: Dead Code 与隐藏机制
why_it_matters: >
  多处 Dead Code 未清理，包括 DatabaseFixConfig 死方法、
  TableInitConfig.createMemberTable()、PrintTemplateDataServiceImpl Stub 空实现、
  OrderMapper 12个 orders_legacy 统计方法、7个 DataService 缓存空实现。
  6个隐藏 Fallback 机制（OCR 引擎降级、硬件不可用 fallback 等）。
  3处直接数据库写入操作绕过 ORM。
affected_domains:
  - Management Core
  - Finance
required_decision: >
  1. 清理 Dead Code
  2. 评估隐藏 Fallback 机制必要性
  3. 替换直接数据库写入为 ORM 操作
likely_remediation_scope: >
  - Dead Code 清理
  - 涉及 Management Core、Finance
  - 代码删除 + 机制评估 + ORM 替换
dependencies:
  - SR-016 (Legacy 服务清理 — Dead Code 清理依赖服务清理)
priority: P2
blocked_by:
  - RC-017 Dead Code 与隐藏机制状态: OPEN
  - SR-016 (Legacy 服务清理)
depends_on:
  - SR-016
unlocks: []
```

---

## 四、依赖关系图

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                           治理候选依赖关系图                                      │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│  Foundation Layer                                                               │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐              │
│  │ SR-007   │ │ SR-008   │ │ SR-009   │ │ SR-010   │ │ SR-011   │              │
│  │Warehouse │ │  Unit    │ │Payment   │ │ Customer │ │  Price   │              │
│  └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘              │
│       │            │            │            │            │                      │
│       └────────────┼────────────┼────────────┼────────────┘                      │
│                    │            │            │                                    │
│  Master Data Layer │            │            │                                    │
│  ┌─────────────────▼────────────▼────────────▼────────────┐                      │
│  │                    SR-001                              │                      │
│  │         Product/Material/Food Identity                 │                      │
│  └───────────────────────┬────────────────────────────────┘                      │
│                          │                                                        │
│  Identity Layer         │                                                        │
│  ┌───────────────────────▼────────────────────────────────┐                      │
│  │                    SR-002                              │                      │
│  │              订单状态机统一                             │                      │
│  └───────────────────────┬────────────────────────────────┘                      │
│                          │                                                        │
│  Truth Source Layer      │                                                        │
│  ┌───────────┬───────────┼───────────┬───────────┐                                │
│  │ SR-003    │ │ SR-004   │ │ SR-005   │ │ SR-006   │                            │
│  │ 金额单位  │ │ 凭证状态 │ │ inv/store│ │ 成本双写 │                            │
│  └─────┬─────┘ └──────────┘ └──────────┘ └──────────┘                            │
│        │                                                                          │
│  Cross-Domain Layer                                                               │
│  ┌─────▼─────┐ ┌──────────┐ ┌──────────┐                                        │
│  │ SR-012    │ │ SR-013   │ │ SR-014   │                                        │
│  │ 数据所有权│ │ 事件可靠 │ │ 多端写入 │                                        │
│  └─────┬─────┘ └──────────┘ └──────────┘                                        │
│        │                                                                          │
│  Legacy Cleanup Layer                                                             │
│  ┌─────▼─────┐ ┌──────────┐ ┌──────────┐                                        │
│  │ SR-015    │ │ SR-016   │ │ SR-017   │                                        │
│  │ 遗留表    │ │ 遗留服务 │ │ Dead Code│                                        │
│  └───────────┘ └──────────┘ └──────────┘                                        │
│                                                                                 │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

## 五、Dependency-aware Priority 排序

| 序号 | 候选 ID | 候选名称 | 优先级 | 依赖层 | 阻塞项 | 解锁项 |
|------|---------|----------|--------|--------|--------|--------|
| 1 | SR-007 | Warehouse Foundation | P2 | Foundation | RC-007 | SR-001, SR-005 |
| 2 | SR-008 | Unit Foundation | P2 | Foundation | RC-008 | SR-001 |
| 3 | SR-009 | PaymentMethod Foundation | P2 | Foundation | RC-009 | SR-003 |
| 4 | SR-010 | Customer Foundation | P1 | Foundation | RC-010, PD-033 | SR-003, SR-002 |
| 5 | SR-011 | Price Foundation | P3 | Foundation | RC-011 | SR-001 |
| 6 | SR-001 | 统一 Product/Material/Food Identity | P0 | Master Data | RC-001, RC-002 | SR-002, SR-005, SR-006, SR-013 |
| 7 | SR-002 | 统一订单状态机 | P1 | Identity | RC-005, SR-001, SR-012 | SR-014 |
| 8 | SR-003 | 统一金额单位 | P0 | Truth Source | RC-004, SR-010, SR-009 | SR-004, SR-006, SR-014 |
| 9 | SR-004 | 解决凭证状态映射 | P2 | Truth Source | RC-006, SR-003 | — |
| 10 | SR-005 | 解决 inventory/store_inventory 双写 | P1 | Truth Source | SR-001, SR-007 | — |
| 11 | SR-006 | 解决成本双写 | P1 | Truth Source | SR-003, SR-001 | — |
| 12 | SR-012 | 明确跨域数据所有权 | P0 | Cross-Domain | RC-012, SR-001 | SR-002, SR-005 |
| 13 | SR-013 | 加强事件驱动可靠性 | P1 | Cross-Domain | RC-013, RC-014, SR-012, SR-002, SR-003 | — |
| 14 | SR-014 | 解决多端写入冲突 | P0 | Cross-Domain | RC-012, SR-012, SR-002 | — |
| 15 | SR-015 | 清理 Legacy 数据库表 | P2 | Legacy | RC-015, SR-001, SR-002, SR-003 | SR-016 |
| 16 | SR-016 | 清理 Legacy 服务 | P2 | Legacy | RC-016, SR-015 | SR-017 |
| 17 | SR-017 | 清理 Dead Code | P2 | Legacy | RC-017, SR-016 | — |

---

## 六、执行路径建议

### Phase 0: Foundation 建立 (前置条件)

1. **SR-010**: Customer Foundation — 需产品决策 (PD-033)
2. **SR-007**: Warehouse Foundation — 确认独立或嵌入式
3. **SR-009**: PaymentMethod Foundation — 确认独立或嵌入式
4. **SR-008**: Unit Foundation — 确认独立或嵌入式
5. **SR-011**: Price Foundation — 评估业务复杂度

### Phase 1: Master Data 统一 (核心)

1. **SR-001**: 统一 Product/Material/Food Identity — P0 紧急

### Phase 2: Identity & Truth Source (高优先级)

1. **SR-002**: 统一订单状态机 — P1
2. **SR-003**: 统一金额单位 — P0 紧急
3. **SR-012**: 明确跨域数据所有权 — P0 紧急
4. **SR-005**: 解决 inventory/store_inventory 双写 — P1
5. **SR-006**: 解决成本双写 — P1

### Phase 3: Cross-Domain Contract (高优先级)

1. **SR-014**: 解决多端写入冲突 — P0 紧急
2. **SR-013**: 加强事件驱动可靠性 — P1
3. **SR-004**: 解决凭证状态映射 — P2

### Phase 4: Legacy Cleanup (中优先级)

1. **SR-015**: 清理 Legacy 数据库表 — P2
2. **SR-016**: 清理 Legacy 服务 — P2
3. **SR-017**: 清理 Dead Code — P2

---

## 七、维护说明

1. 本文件随系统治理候选状态更新
2. 候选状态变更（CANDIDATE → APPROVED → IN_PROGRESS → RESOLVED → CLOSED）需更新
3. 新增候选需同时更新「候选总览」和「Dependency-aware Priority 排序」
4. 产品决策结果需更新「待产品决策项」
5. 本文件数据来源：
   - systemic-problem-register.md
   - truth-conflict-map.md
   - broken-chain-map.md
   - legacy-map.md
   - cross-domain-coordination-matrix.md
   - foundation-map.md
   - data-lineage-map.md
   - business-data-flow-map.md

---

*生成时间: 2026-09-09*
*评审编号: PROJECT-MASTER-REVIEW-001*
*注意: 本文件仅为治理候选清单，非 Engineering Task*
