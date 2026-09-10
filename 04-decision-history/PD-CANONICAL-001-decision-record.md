 > **DOCUMENT STATUS:** ACTIVE_REFERENCE
 > **SOURCE TASK:** PD-CANONICAL-001-FORMAL-DECISION-001

 # ============================================================================
 # PD-CANONICAL-001: Canonical Business Item 定义
 # ============================================================================
 # 决策编号: PD-CANONICAL-001
 # 决策标题: Canonical Business Item 定义
 # 决策类别: PRODUCT_DECISION
 # 生成日期: 2026-09-10
 # 决策状态: RECOMMENDED (NOT CONFIRMED)
 # 优先级: P0 (阻塞所有后续 Product / Architecture 语义决策)
 # ============================================================================

 ## 一、决策概览

 | 字段 | 值 |
 |------|-----|
 | **决策ID** | PD-CANONICAL-001 |
 | **决策标题** | Canonical Business Item 定义 |
 | **决策类别** | PRODUCT_DECISION |
 | **决策状态** | 🟡 RECOMMENDED (NOT CONFIRMED) |
 | **Evidence Status** | ✅ VERIFIED |
 | **执行状态** | 🔒 BLOCKED (等待 Product Owner 确认) |
 | **优先级** | P0 |
 | **负责人** | Product Owner |
 | **生成日期** | 2026-09-10 |
 | **阻塞影响** | 阻塞 PD-CANONICAL-002~006, AD-IDENTITY-001, DE-PK-001, DE-SKU-001 |

 ---

 ## 二、问题定义

 **核心问题：Business Item 是否应该拥有独立 Canonical Identity？**

 餐饮 ERP 系统中，Business Item（业务物品）在不同模块中有不同的表示方式：
 - POS 模块使用 `foods` 表
 - 采购模块使用 `material_archives` 表
 - 库存模块使用 `inventory` 表（以 material_id 关联）
 - 配方模块使用 `dish_recipe` 表（连接 food 和 material）

 这导致同一物理实体（如 Coca-Cola 330ml）在系统中有多个独立的表示，跨模块统计需要复杂的多表 JOIN 和人工匹配。

 **需决策：是否引入统一 Canonical Business Item Identity 层？**

 ---

 ## 三、当前现实

 ### 3.1 Evidence 分析

 | 检查项 | 当前现实 | 问题 |
 |--------|----------|------|
 | POS | `foods` 表 | 同一商品在不同 POS channel 有重复记录 |
 | Procurement | `material_archives` 表 | 同一可乐可能被建为两个 material |
 | Inventory | `inventory` 表 | Identity 来源不统一，以 material_id 关联 |
 | Recipe | `dish_recipe` 表 | 引用 material 或 product，缺乏统一 Identity |
 | Sales | `order_items` 表 | 使用 food_id |
 | Purchase | `purchase_order_items` 表 | 使用 material_id |

 ### 3.2 当前实现事实

 | 事实 | Evidence Status | 说明 |
 |------|-----------------|------|
 | inventory 以 material_id 管理库存 | VERIFIED | 库存查询以 material_id 为粒度 |
 | store_inventory 以 store_id + material_id 管理门店库存 | VERIFIED | 门店库存以 store_id + material_id 为粒度 |
 | dish_recipe 连接 food 和 material | VERIFIED | 配方模块跨越 food 和 material 两个 Identity |
 | order_items 使用 food_id | VERIFIED | 订单以 food_id 为粒度 |
 | purchase_order_items 使用 material_id | VERIFIED | 采购以 material_id 为粒度 |
 | product 表标记为 LEGACY | VERIFIED | product 表已废弃，但仍有历史数据 |

 ---

 ## 四、10 个业务案例验证

 ### CASE-001: Coca-Cola 330ml (单一 SKU，多场景)

 | 模块 | 场景 | ID 使用 | Identity 一致 |
 |------|------|---------|---------------|
 | POS | 扫码销售 ¥3 | food_id | ✅ |
 | Procurement | 供应商报价 ¥2/罐 | material_id | ❌ |
 | Inventory | 入库 100 罐、出库 5 罐 | material_id | ❌ |
 | Recipe | 可乐鸡翅配料 | ingredient_id → material_id | ❌ |

 **结论**：同一物理实体 4 个独立表示，Canonical Identity 必须统一。→ **Supports Option B**

 ### CASE-002: Coca-Cola 330ml vs 500ml (多 SKU 变体)

 **结论**：同一线型产品的不同规格需要共享 Canonical Identity + 规格扩展。→ **Supports Option B**

 ### CASE-003: Case Packs (整箱 vs 散装)

 **结论**：同一物理实体的不同包装形态需要 Canonical Identity + Packaging Profile。→ **Supports Option B**

 ### CASE-004: Suppliers (供应商维度)

 **结论**：Canonical Identity 必须独立于 Supplier，Supplier 属于 Procurement Profile。→ **Supports Option B**

 ### CASE-005: Barcodes (条码维度)

 **结论**：Barcode 是 Context-specific Identifier，不是 Canonical Identity。→ **Supports Option B**

 ### CASE-006: Batches (批次维度)

 **结论**：Batch 是 Inventory Context 的事件属性，不是 Canonical Identity。→ **Supports Option B**

 ### CASE-007: Stores (门店维度)

 **结论**：Canonical Identity 必须独立于 Location。→ **Supports Option B**

 ### CASE-008: Chicken Wings (多角色共存)

 **结论**：同一物理实体同时承担 Sellable + Purchasable + Recipe Component 三种角色，Canonical Identity 必须独立于 Capability。→ **Supports Option B**

 ### CASE-009: Kung Pao Chicken (菜品 = Recipe Output)

 **结论**：菜品的 Canonical Identity = food_id，但其 Identity 不应被 Inventory Context 定义。→ **Supports Option B**

 ### CASE-010: Takeaway Box / Cleaning / Service (非食用消耗品)

 **结论**：非食用消耗品也需要 Canonical Identity，不能仅用 Material 作为 Identity。→ **Supports Option B**

 **验证总结：10/10 案例全部支持 Option B**

 ---

 ## 五、3 Business Invariant 验证

 ### Invariant 1: 多角色共存

 > 同一个业务对象可以同时：Sell + Purchase + Stock + Consume + Become Recipe Component

 | Option | 验证结果 | 说明 |
 |--------|----------|------|
 | A (Type-specific) | ❌ FAIL | 每个类型独立 Identity，无法跨类型关联 |
 | B (Unified Canonical) | ✅ PASS | Canonical Identity 独立于任何 Capability，通过 domain_tags 标记能力 |
 | C (Hybrid) | ⚠️ PARTIAL | 混合模式仍有部分类型无法跨类型关联 |

 ### Invariant 2: Identity 稳定性

 > Business Identity 不应因为使用场景变化而变化

 | Option | 验证结果 | 说明 |
 |--------|----------|------|
 | A (Type-specific) | ❌ FAIL | 同一可乐在不同模块有不同 ID |
 | B (Unified Canonical) | ✅ PASS | Canonical UUID 不变，Context-specific Profile 各自独立 |
 | C (Hybrid) | ⚠️ PARTIAL | 部分类型共享 Identity，部分不共享 |

 ### Invariant 3: 概念分离

 > Role / Capability / Profile / Context 不得互相混淆。必须分别定义。

 | Option | 验证结果 | 说明 |
 |--------|----------|------|
 | A (Type-specific) | ❌ FAIL | Identity 混合了 Capability 和 Profile |
 | B (Unified Canonical) | ✅ PASS | Identity 层独立，Capability / Profile / Context 各自定义 |
 | C (Hybrid) | ❌ FAIL | 混合模式仍存在概念混淆 |

 **验证总结：3/3 Invariant 全部 PASS for Option B**

 ---

 ## 六、选项分析

 ### Option A: Type-specific Entity (每个类型独立 Identity)

 | 维度 | 评估 |
 |------|------|
 | **描述** | Food / Material / Ingredient 各自定义 Identity 表，无统一 Canonical 层 |
 | **优点** | 与现有实现最接近，改动最小 |
 | **缺点** | 跨模块统计仍需复杂 JOIN + 人工匹配；无法解决同一物理实体多表重复的问题 |
 | **Invariant 验证** | 3/3 FAIL |
 | **风险** | HIGH — 根本问题未解决 |
 | **Effort** | LOW |

 ### Option B: Unified Canonical Identity (统一 Canonical 层) ⭐ RECOMMENDED

 | 维度 | 评估 |
 |------|------|
 | **描述** | 引入统一 Canonical Business Item Identity 层，所有子类型共享 UUID + canonical_name + domain_tags |
 | **优点** | 满足 3 Invariant；跨模块统计只需 Canonical Identity JOIN；新增模块直接引用 Canonical Identity |
 | **缺点** | 需要新增 Canonical Identity 表；需要迁移现有数据建立 Canonical 映射；需要更新所有引用现有 ID 的服务 |
 | **Invariant 验证** | 3/3 PASS |
 | **风险** | MEDIUM — 数据迁移复杂度可控 |
 | **Effort** | MEDIUM |

 ### Option C: Hybrid (混合模式)

 | 维度 | 评估 |
 |------|------|
 | **描述** | 部分类型共享 Canonical Identity，部分类型保持独立 |
 | **优点** | 折中方案，改动适中 |
 | **缺点** | Decision Boundary 模糊；无法满足 Invariant 3；仍然存在跨模块统计的 JOIN 复杂度 |
 | **Invariant 验证** | 1 PASS / 1 PARTIAL / 1 FAIL |
 | **风险** | HIGH — 边界不清 |
 | **Effort** | MEDIUM |

 ---

 ## 七、Recommendation

 **推荐方案**: Option B — Unified Canonical Identity

 **推荐理由**:

 1. **满足 3 个 Business Invariant**: 只有 Option B 能同时满足多角色共存、Identity 稳定性、概念分离
 2. **10 个业务案例全部验证通过**: 每个案例都指向 Canonical Identity 的必要性
 3. **当前现实已证明不足**: 各模块自行维护 Identity 导致跨模块统计复杂、数据冗余
 4. **目标模型清晰**: Canonical Identity + Context-specific Profile 是业界成熟模式
 5. **与 L2-001~L2-010 收敛结论一致**: Capability / Profile / Context 已被识别为独立概念

 **Confidence**: HIGH

 **Status**: RECOMMENDED (NOT CONFIRMED — 等待 Product Owner 正式确认)

 ---

 ## 八、决策范围 (13 items)

 | # | Scope Item | Resolved By | 状态 |
 |---|------------|-------------|------|
 | 1 | Canonical Identity 核心属性 (UUID + canonical_name + domain_tags) | PD-CANONICAL-001 | ✅ RESOLVED |
 | 2 | Canonical Name 生成规则 | PD-CANONICAL-001 | ✅ RESOLVED (手工输入) |
 | 3 | Domain Tags 枚举 (SELLABLE / PURCHASABLE / STOCKABLE / CONSUMABLE) | PD-CANONICAL-001 | ✅ RESOLVED |
 | 4 | Canonical Identity 表名 | AD-IDENTITY-001 | ⏳ PENDING |
 | 5 | Primary Key 类型 | DE-PK-001 | ⏳ PENDING |
 | 6 | 各子类型如何引用 Canonical Identity | AD-IDENTITY-001 | ⏳ PENDING |
 | 7 | 多 SKU 变体如何表达 | PD-CANONICAL-002 | ⏳ PENDING |
 | 8 | 多 Packaging 如何表达 | PD-CANONICAL-004 | ⏳ PENDING |
 | 9 | Barcode 是否属于 Canonical Identity | PD-CANONICAL-001 | ✅ RESOLVED (NOT Canonical) |
 | 10 | Batch 是否属于 Canonical Identity | PD-CANONICAL-001 | ✅ RESOLVED (NOT Canonical) |
 | 11 | Location 是否影响 Canonical Identity | PD-CANONICAL-001 | ✅ RESOLVED (NOT Canonical) |
 | 12 | Supplier 是否影响 Canonical Identity | PD-CANONICAL-001 | ✅ RESOLVED (NOT Canonical) |
 | 13 | Canonical Business Item 的生命周期 | PD-CANONICAL-001 | ✅ RESOLVED (支持停用/归档) |

 **已解决 7/13，待后续决策解决 6/13**

 ---

 ## 九、阻塞影响

 ### 被此决策阻塞的后续决策

 | 决策 ID | 决策标题 | 阻塞关系 |
 |---------|----------|----------|
 | PD-CANONICAL-002 | Product 语义 | 必须先确定 Canonical Identity 层 |
 | PD-CANONICAL-003 | Food 语义 | Food 的 Canonical 定位依赖此决策 |
 | PD-CANONICAL-004 | Material 语义 | Material 的 Canonical 定位依赖此决策 |
 | PD-CANONICAL-006 | Consumable 定义 | Consumable 的 Canonical 归属依赖此决策 |
 | AD-IDENTITY-001 | Identity Storage | 表结构设计依赖此决策 |
 | DE-PK-001 | Primary Key | PK 类型依赖此决策 |
 | DE-SKU-001 | SKU 生成规则 | SKU 生成依赖 Canonical Identity |

 ### 此决策依赖的前置项

 无 (此为 Phase 1 第一个决策)

 ---

 ## 十、Conflict Registry 关联

 | Conflict ID | Conflict 标题 | PD-CANONICAL-001 解决方式 |
 |-------------|---------------|---------------------------|
| CONFLICT-A | Product 语义：废弃概念 vs Umbrella Concept | Product 的 Canonical 定位由 PD-CANONICAL-002 进一步明确，但 Canonical Identity 层由 PD-CANONICAL-001 奠定 |
| CONFLICT-B | Material 是 Canonical Identity 还是 Implementation Object | Material 降级为 Profile（Procurement Profile），Canonical Identity 由 PD-CANONICAL-001 定义 |
| CONFLICT-C | Food 是 Entity / Type / Commercial Profile | Food 的 Canonical 定位由 PD-CANONICAL-003 进一步明确，但 Canonical Identity 层由 PD-CANONICAL-001 奠定 |
| CONFLICT-E | CONSUMABLE 的三种消费场景 | Consumable 定义为 Capability，归属由 PD-CANONICAL-006 进一步明确 |
| CONFLICT-G | Location: Transit 是 Virtual Location 还是 Movement Context | Canonical Identity 独立于 Location，Location 由 AD-LOCATION-001 定义 |

 ---

 ## 十一、Provenance

 | 字段 | 值 |
 |------|-----|
 | **Decision ID** | PD-CANONICAL-001 |
 | **Origin** | BUSINESS-ITEM-CANONICAL-SEMANTIC-REASSESSMENT-001 |
 | **Source Documents** | business-item-canonical-semantic-reassessment.md, business-item-canonical-decision-boundary.md, current-business-semantic-baseline.md (v2.0), canonical-identity-analysis.md |
 | **Owner** | Product Owner |
 | **Decision Date** | 2026-09-10 |
 | **Decision Status** | RECOMMENDED (NOT CONFIRMED) |
 | **Evidence Status** | VERIFIED |
 | **Execution Status** | BLOCKED (等待 Product Owner 确认) |

 ---

 ## 十二、执行计划

 ### Phase 1: Decision Confirmation (0.5 天)
 - Product Owner 正式确认 PD-CANONICAL-001 为 Option B
 - 记录确认时间和决策者

 ### Phase 2: Canonical Identity 表设计 (1-2 天)
 - 依赖: AD-IDENTITY-001, DE-PK-001
 - 设计 canonical_business_item 表结构
 - 定义 UUID 生成规则
 - 定义 domain_tags 枚举

 ### Phase 3: 数据迁移 (3-5 天)
 - 依赖: DE-MIGRATION-001
 - 从 foods / material_archives / product 表提取 Canonical Identity
 - 建立 mapping table (canonical_id ↔ module_id)
 - 验证数据一致性

 ### Phase 4: 服务层适配 (5-8 天)
 - 创建 CanonicalBusinessItemService (CRUD)
 - 更新 FoodService / MaterialService 引用 Canonical Identity
 - 更新 InventoryService 支持 Canonical 查询

 **总预估: 10-16 人天**

 ---

 ## 十三、验收标准

 - [ ] canonical_business_item 表存在且包含所有现有 Food / Material / Product 的 Identity
 - [ ] 每个 Canonical Business Item 有唯一的 UUID + canonical_name
 - [ ] 每个 Canonical Business Item 有 domain_tags 标记其 Capabilities
 - [ ] Food / Material / Product 表可通过 mapping table 关联到 Canonical Identity
 - [ ] 跨模块统计（如：可乐的采购成本 vs 销售收入）只需 Canonical Identity JOIN
 - [ ] 新增业务对象可直接引用 Canonical Identity
 - [ ] 现有的 LOCK-A001 / LOCK-A002 不被破坏

 ---

 ## 十四、风险项

 | 风险 | 严重度 | 缓解措施 |
 |------|--------|----------|
 | 数据迁移复杂度 | MEDIUM | 增量迁移 + 双写过渡期 |
 | 现有服务引用破坏 | HIGH | 通过 mapping table 渐进式迁移，不直接修改现有表 |
 | 性能影响 | LOW | Canonical Identity 查询走缓存 (Redis) |
 | Product Owner 不确认 | HIGH | 提供 10 个业务案例验证 + 3 Invariant 验证矩阵 |

 ---

 ## 十五、决策记录

 | 版本 | 日期 | 决策者 | 决策内容 |
 |------|------|--------|----------|
 | v1.0 | 2026-09-10 | 架构总控 | 初始提案，标记为 RECOMMENDED (NOT CONFIRMED) |

 ---

 ## 十六、下一步行动

 | 行动 | 负责人 | 截止时间 | 优先级 |
 |------|--------|----------|--------|
 | 确认 PD-CANONICAL-001 Option B | Product Owner | Week 1 | P0 |
 | 进入 PD-CANONICAL-002 (Product 语义) | Product Owner | Week 2 | P1 |
 | 进入 PD-CANONICAL-003 (Food 语义) | Product Owner | Week 2 | P1 |
 | 进入 PD-CANONICAL-004 (Material 语义) | Product Owner | Week 2 | P1 |
 | 进入 AD-IDENTITY-001 (Identity Storage) | Architecture Owner | Week 1 | P1 |
 | 进入 DE-PK-001 (Primary Key) | Architecture Owner | Week 1 | P1 |

 ---

 **文档状态**: ✅ READY_FOR_OWNER_DECISION  
 **下一步**: Product Owner 正式确认 PD-CANONICAL-001 为 Option B
