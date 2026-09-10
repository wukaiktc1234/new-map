 > **DOCUMENT STATUS:** ACTIVE_REFERENCE
 > **SOURCE TASK:** PD-CANONICAL-001-DECISION-SCOPE-FINALIZATION-001

 # ============================================================================
 # PD-CANONICAL-001: Canonical Business Identity Semantics
 # ============================================================================
 # 决策编号: PD-CANONICAL-001
 # 决策标题: Canonical Business Identity 业务语义
 # 决策类别: PRODUCT_DECISION
 # 生成日期: 2026-09-10
 # 决策状态: RECOMMENDED (NOT CONFIRMED)
 # 优先级: P0 (阻塞 PD-CANONICAL-002~006 产品语义决策)
 # ============================================================================
 # SCOPE: 仅业务语义层，不含任何 Implementation / Storage / Engineering 内容
 # ============================================================================

 ## 一、决策概览

 | 字段 | 值 |
 |------|-----|
 | **决策ID** | PD-CANONICAL-001 |
 | **决策标题** | Canonical Business Identity 业务语义 |
 | **决策类别** | PRODUCT_DECISION |
 | **决策状态** | 🟡 RECOMMENDED (NOT CONFIRMED) |
 | **Evidence Status** | ✅ VERIFIED |
 | **执行状态** | 🔒 BLOCKED (等待 Product Owner 确认) |
 | **优先级** | P0 |
 | **负责人** | Product Owner |
 | **Scope** | 仅业务语义，不含 Implementation |

 ---

 ## 二、Decision Question (Corrected)

 **核心问题：Canonical Business Identity 的正式业务语义是什么？**

 具体包括：
 1. Identity 是否存在？
 2. Identity 的业务定义
 3. Identity 稳定性原则
 4. Same Identity 判断规则
 5. Different Identity 判断规则
 6. Identity 与 Context / Capability / Role / Profile / Relationship 的关系
 7. Identity 与 Packaging / UOM / SKU / Barcode / Supplier / Batch / Store 的关系
 8. Identity 生命周期语义

 **注意：本决策不涉及任何实现细节（表名、UUID、PK、Migration、Service、API）。**

 ---

 ## 三、3 Business Invariants (不变量)

 ### Invariant 1: 多角色共存 (Multi-Capability Coexistence)

 > 同一个业务对象可以同时承担：Sellable + Purchasable + Stockable + Consumable + Recipe Component

 **Canonical Identity 必须独立于任何单一 Capability。**

 **Evidence**: Coca-Cola 330ml: POS 销售 ✅ + 采购 ✅ + 库存 ✅ + 配方成分 ✅

 ### Invariant 2: Identity 稳定性 (Identity Stability)

 > Business Identity 不因使用场景（POS / Procurement / Inventory / Recipe / Reporting）变化而变化

 **同一物理实体在所有 Context 中必须可识别为同一个 Canonical Identity。**

 **Evidence**: Coca-Cola 在 POS 是 food_id=101, 在 Procurement 是 material_id=205 — 当前实现违反 Invariant 2

 ### Invariant 3: 概念分离 (Concept Separation)

 > Identity / Role / Capability / Profile / Context 不得互相混淆

 **每个概念必须有独立的 ontology ownership。**

 **Evidence**: 当前 material 表混合了 Identity + Procurement Profile + Inventory Profile — 违反 Invariant 3

 ---

 ## 四、选项分析

 ### Option A: Type-specific Entity

 | 维度 | 评估 |
 |------|------|
 | Invariant 1 | ❌ FAIL |
 | Invariant 2 | ❌ FAIL |
 | Invariant 3 | ❌ FAIL |
 | Verdict | 根本问题未解决 |

 ### Option B: Unified Canonical Business Identity ⭐ RECOMMENDED

 | 维度 | 评估 |
 |------|------|
 | Invariant 1 | ✅ PASS |
 | Invariant 2 | ✅ PASS |
 | Invariant 3 | ✅ PASS |
 | Verdict | 满足所有 Business Invariant |

 ### Option C: Hybrid

 | 维度 | 评估 |
 |------|------|
 | Invariant 1 | ⚠️ PARTIAL |
 | Invariant 2 | ⚠️ PARTIAL |
 | Invariant 3 | ❌ FAIL |
 | Verdict | Decision Boundary 模糊 |

 **验证总结：3/3 Invariant 全部 PASS for Option B**

 ---

 ## 五、Recommendation

 **推荐方案**: Option B — Unified Canonical Business Identity

 **推荐理由**:

 1. **满足 3 个 Business Invariant**: 只有 Option B 能同时满足多角色共存、Identity 稳定性、概念分离
 2. **10 个业务案例全部验证通过**: 每个案例都指向 Canonical Identity 的必要性
 3. **当前现实已证明不足**: 各模块自行维护 Identity 导致跨模块统计复杂、数据冗余
 4. **与 L2-001~L2-010 收敛结论一致**: Capability / Profile / Context 已被识别为独立概念

 **Recommendation Type**: Canonical Business Identity 作为业务语义层 (NOT 作为 table / UUID / implementation)

 **Confidence**: HIGH

 **Status**: RECOMMENDED (NOT CONFIRMED — 等待 Product Owner 正式确认)

 ---

 ## 六、Decision Scope (18 items — SEMANTICS ONLY)

 | # | Scope Item | Status | Conclusion |
 |---|------------|--------|------------|
 | DS-001 | Canonical Business Identity 是否存在 | ✅ RESOLVED | 存在。Option B — 统一 Canonical Business Identity 作为业务语义层 |
 | DS-002 | Identity 的业务定义 | ✅ RESOLVED | 一个业务物品的内在标识，独立于任何使用场景 |
 | DS-003 | Identity 稳定性原则 | ✅ RESOLVED | Business Identity 不因使用场景变化而变化 |
 | DS-004 | Same Identity 判断规则 | ✅ RESOLVED | Brand + Product Family + Specification 相同 = Same Identity |
 | DS-005 | Different Identity 判断规则 | ✅ RESOLVED | Brand / Product Family / Specification 任一不同 = Different Identity |
 | DS-006 | Identity 与 Context 的关系 | ✅ RESOLVED | Identity 独立于 Context |
 | DS-007 | Identity 与 Capability 的关系 | ✅ RESOLVED | Identity 独立于 Capability |
 | DS-008 | Identity 与 Role 的关系 | ✅ RESOLVED | Identity 独立于 Role |
 | DS-009 | Identity 与 Profile 的关系 | ✅ RESOLVED | Identity 独立于 Profile |
 | DS-010 | Identity 与 Relationship 的关系 | ✅ RESOLVED | Identity 独立于 Relationship |
 | DS-011 | Identity 与 Packaging 的关系 | ✅ RESOLVED | Packaging 是 NOT_IDENTITY_BOUNDARY |
 | DS-012 | Identity 与 UOM 的关系 | ✅ RESOLVED | UOM 是 NOT_IDENTITY_BOUNDARY |
 | DS-013 | Identity 与 SKU 的关系 | ✅ RESOLVED | SKU 是 NOT_IDENTITY_BOUNDARY |
 | DS-014 | Identity 与 Barcode 的关系 | ✅ RESOLVED | Barcode 是 NOT_IDENTITY_BOUNDARY |
 | DS-015 | Identity 与 Supplier 的关系 | ✅ RESOLVED | Supplier 是 NOT_IDENTITY_BOUNDARY |
 | DS-016 | Identity 与 Batch/Lot/Expiry 的关系 | ✅ RESOLVED | Batch/Lot/Expiry 是 NOT_IDENTITY_BOUNDARY |
 | DS-017 | Identity 与 Store Scope 的关系 | ✅ RESOLVED | Store 是 NOT_IDENTITY_BOUNDARY |
 | DS-018 | Identity 生命周期语义 | ✅ RESOLVED | 支持停用和归档，合并需人工干预 |

 **已解决 18/18，全部为业务语义**

 ---

 ## 七、Identity Resolution Matrix

 | 属性变化 | Same Identity? | Rule Type | 原因 | Status |
|----------|----------------|-----------|------|--------|
| Brand 不同 | ❌ NO | IDENTITY_BOUNDARY | Brand 是 Product Family 的归属属性 | CANDIDATE |
| Product Family 不同 | ❌ NO | IDENTITY_BOUNDARY | Product Family 是 Identity 的核心归属 | CANDIDATE |
| Specification 不同 | ❌ NO | IDENTITY_BOUNDARY | Specification 是 Identity 的物理边界 | CANDIDATE |
| Flavor / Variant 不同 | ❌ NO | IDENTITY_BOUNDARY | Flavor/Variant 是 Identity 的业务变体 | CANDIDATE |
| Packaging 不同 | ✅ YES | NOT_IDENTITY_BOUNDARY | Packaging 是 Context-specific 配置 | CANDIDATE |
| Pack Size 不同 | ✅ YES | NOT_IDENTITY_BOUNDARY | Pack Size 是商业配置 | CANDIDATE |
| UOM 不同 | ✅ YES | NOT_IDENTITY_BOUNDARY | UOM 是 Context-specific 计量配置 | CANDIDATE |
| SKU 不同 | ✅ YES | NOT_IDENTITY_BOUNDARY | SKU 是 Commercial Context 标识符 | CANDIDATE |
| Barcode 不同 | ✅ YES | NOT_IDENTITY_BOUNDARY | Barcode 是 Context-specific Identifier | CANDIDATE |
| Supplier 不同 | ✅ YES | NOT_IDENTITY_BOUNDARY | Supplier 是 Procurement Context 配置 | CANDIDATE |
| Supplier Product Code 不同 | ✅ YES | NOT_IDENTITY_BOUNDARY | Supplier Code 是 Supplier Context 标识符 | CANDIDATE |
| Batch / Lot 不同 | ✅ YES | NOT_IDENTITY_BOUNDARY | Batch/Lot 是 Inventory Context 事件属性 | CANDIDATE |
| Expiry Date 不同 | ✅ YES | NOT_IDENTITY_BOUNDARY | Expiry 是 Inventory Context 事件属性 | CANDIDATE |
| Store 不同 | ✅ YES | NOT_IDENTITY_BOUNDARY | Store 是 Location Context | CANDIDATE |
| Price 不同 | ✅ YES | NOT_IDENTITY_BOUNDARY | Price 是 Commercial Context 动态属性 | CANDIDATE |
| Commercial Offer 不同 | ✅ YES | NOT_IDENTITY_BOUNDARY | Commercial Offer 是 Sales Context 配置 | CANDIDATE |

 **5 个 IDENTITY_BOUNDARY Rules + 12 个 NOT_IDENTITY_BOUNDARY Rules = 全部 CANDIDATE (NOT CONFIRMED)**

 ---

 ## 八、Case 002 重新检查

 **Coca-Cola 330ml vs 500ml (多规格变体)**

 | 属性 | 330ml | 500ml | Same? |
|------|-------|-------|-------|
| Brand | Coca-Cola | Coca-Cola | ✅ Same |
| Product Family | Coca-Cola Classic | Coca-Cola Classic | ✅ Same |
| Specification | 330ml | 500ml | ❌ Different |

 **Identity Rule Applied**: DS-005 — Specification 是 IDENTITY_BOUNDARY

 **结论**: Coca-Cola 330ml 和 Coca-Cola 500ml 是 **两个不同的 Canonical Identity**。

 它们共享 Brand + Product Family，但 Specification 不同 = Different Canonical Identity。

 **不是** "共享 Canonical Identity + 规格扩展"，**而是** "同一 Product Family 下的两个不同 Canonical Identity"。

 ---

 ## 九、Case 003 重新检查

 **Coca-Cola 330ml 单罐 vs 24 罐整箱 (Case Packs)**

 | 属性 | 单罐 | 整箱 | Same? |
|------|------|------|-------|
| Brand | Coca-Cola | Coca-Cola | ✅ Same |
| Product Family | Coca-Cola Classic | Coca-Cola Classic | ✅ Same |
| Specification | 330ml | 330ml | ✅ Same |
| Packaging | 单罐 | 整箱 | ✅ Same Identity (NOT Boundary) |
| Pack Size | 1 | 24 | ✅ Same Identity (NOT Boundary) |
| UOM | 罐 | 箱 | ✅ Same Identity (NOT Boundary) |
| SKU | SKU-A | SKU-B | ✅ Same Identity (NOT Boundary) |
| Barcode | Barcode-A | Barcode-B | ✅ Same Identity (NOT Boundary) |

 **Identity Rule Applied**: DS-011~014 — Packaging / Pack Size / UOM / SKU / Barcode 都是 NOT_IDENTITY_BOUNDARY

 **结论**: Coca-Cola 330ml 单罐 和 Coca-Cola 330ml × 24 整箱 是 **同一个 Canonical Identity**。

 它们是不同的：
 - **SKU** (商业标识符)
 - **Barcode** (条码标识符)
 - **Purchase Unit** (采购单位: 箱)
 - **Stock Unit** (库存单位: 罐)
 - **Sales Unit** (销售单位: 罐 或 箱)

 这些差异属于 Context-specific Profile，不是 Identity 差异。

 ---

 ## 十、Blocking 关系 (Corrected)

 ### 被此决策阻塞的后续决策

 | 决策 ID | 决策标题 | 阻塞关系 |
 |---------|----------|----------|
 | PD-CANONICAL-002 | Product 语义 | 必须先确定 Canonical Identity 语义层 |
 | PD-CANONICAL-003 | Food 语义 | Food 的 Canonical 定位依赖此决策 |
 | PD-CANONICAL-004 | Material 语义 | Material 的 Canonical 定位依赖此决策 |
 | PD-CANONICAL-006 | Consumable 定义 | Consumable 的 Canonical 归属依赖此决策 |

 ### 参考此决策的后续决策 (Informed-By, NOT Blocked)

 | 决策 ID | 决策标题 | 关系 | Scope |
 |---------|----------|------|-------|
 | AD-IDENTITY-001 | Identity Storage | INFORMED_BY | 表名、列设计、JSON vs relational、Index |
 | DE-PK-001 | Primary Key | INFORMED_BY | UUID v7 / BIGINT / Composite PK |
 | DE-SKU-001 | SKU 生成规则 | INFORMED_BY | SKU generation algorithm |
 | DE-STORAGE-001 | Storage 策略 | INFORMED_BY | JSON vs relational / Materialized View |
 | DE-MIGRATION-001 | 数据迁移策略 | INFORMED_BY | Migration strategy / mapping table |

 **关键变更：AD / DE 决策从 "被阻塞" 改为 "参考"，不强制等待 PD-001 确认**

 ---

 ## 十一、Downstream Decisions

 | 决策 ID | 决策标题 | 关系 | Scope |
 |---------|----------|------|-------|
 | PD-CANONICAL-002 | Product 语义 | BLOCKED | Product 定义（Umbrella / Commercial / Legacy） |
 | PD-CANONICAL-003 | Food 语义 | BLOCKED | Food 定义（Entity / Type / Profile） |
 | PD-CANONICAL-004 | Material 语义 | BLOCKED | Material 定义（Identity / Type / Profile） |
 | PD-CANONICAL-006 | Consumable 定义 | BLOCKED | Consumable 定义（Recipe / Supply / Operational） |
 | AD-IDENTITY-001 | Identity Storage | INFORMED_BY | 表结构设计 |
 | DE-PK-001 | Primary Key | INFORMED_BY | PK 类型选择 |
 | DE-SKU-001 | SKU 生成规则 | INFORMED_BY | SKU 算法设计 |
 | DE-STORAGE-001 | Storage 策略 | INFORMED_BY | 存储方案选择 |
 | DE-MIGRATION-001 | 数据迁移策略 | INFORMED_BY | 迁移方案设计 |

 ---

 ## 十二、Conflict Registry 关联

 | Conflict ID | Conflict 标题 | PD-CANONICAL-001 解决方式 |
 |-------------|---------------|---------------------------|
 | CONFLICT-A | Product 语义 | Product 的 Canonical 定位由 PD-CANONICAL-002 进一步明确 |
 | CONFLICT-B | Material 语义 | Material 降级为 Profile，Canonical Identity 由 PD-CANONICAL-001 定义 |
 | CONFLICT-C | Food 语义 | Food 的 Canonical 定位由 PD-CANONICAL-003 进一步明确 |
 | CONFLICT-E | CONSUMABLE 定义 | Consumable 定义为 Capability，归属由 PD-CANONICAL-006 进一步明确 |
 | CONFLICT-G | Location 模型 | Canonical Identity 独立于 Location |

 ---

 ## 十三、Provenance

 | 字段 | 值 |
 |------|-----|
 | **Decision ID** | PD-CANONICAL-001 |
 | **Origin** | BUSINESS-ITEM-CANONICAL-SEMANTIC-REASSESSMENT-001 |
 | **Scope Finalization** | PD-CANONICAL-001-DECISION-SCOPE-FINALIZATION-001 |
 | **Source Documents** | business-item-canonical-semantic-reassessment.md, business-item-canonical-decision-boundary.md, current-business-semantic-baseline.md (v2.0), canonical-identity-analysis.md |
 | **Owner** | Product Owner |
 | **Decision Date** | 2026-09-10 |
 | **Decision Status** | RECOMMENDED (NOT CONFIRMED) |
 | **Evidence Status** | VERIFIED |
 | **Execution Status** | BLOCKED (等待 Product Owner 确认) |

 ---

 ## 十四、验收标准 (Semantic Only)

 - [ ] Canonical Business Identity 的业务语义已明确定义
 - [ ] Identity 稳定性原则已明确定义
 - [ ] Same Identity 判断规则已定义（Identity Resolution Matrix）
 - [ ] Different Identity 判断规则已定义（Identity Resolution Matrix）
 - [ ] Identity 与 Context / Capability / Role / Profile / Relationship 的关系已明确
 - [ ] Identity 与 Packaging / UOM / SKU / Barcode / Supplier / Batch / Store 的关系已明确
 - [ ] Identity 生命周期语义已定义
 - [ ] Product Decision / Architecture Decision / Engineering Decision 三层边界清晰

 ---

 ## 十五、Decision Record

 | 版本 | 日期 | 决策者 | 决策内容 |
 |------|------|--------|----------|
 | v1.0 | 2026-09-10 | 架构总控 | 初始提案，标记为 RECOMMENDED (NOT CONFIRMED) |
 | v2.0 | 2026-09-10 | 架构总控 | Scope Finalization — 收敛为 Canonical Business Identity Semantics；新增 Identity Resolution Matrix；修正 Case 002/003；剥离 Implementation 细节 |

 ---

 ## 十六、下一步行动

 | 行动 | 负责人 | 截止时间 | 优先级 |
 |------|--------|----------|--------|
 | 确认 PD-CANONICAL-001 Option B | Product Owner | Week 1 | P0 |
 | 进入 PD-CANONICAL-002 (Product 语义) | Product Owner | Week 2 | P1 |
 | 进入 PD-CANONICAL-003 (Food 语义) | Product Owner | Week 2 | P1 |
 | 进入 PD-CANONICAL-004 (Material 语义) | Product Owner | Week 2 | P1 |
 | 并行推进 AD-IDENTITY-001 (参考 PD-001 语义) | Architecture Owner | Week 2 | P1 |
 | 并行推进 DE-PK-001 (参考 PD-001 语义) | Architecture Owner | Week 2 | P1 |

 ---

 **文档状态**: ✅ READY_FOR_PRODUCT_OWNER_CONFIRMATION  
 **下一步**: Product Owner 正式确认 PD-CANONICAL-001 为 Option B
