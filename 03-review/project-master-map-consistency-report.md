# PROJECT-MASTER-MAP-CONSISTENCY-001 一致性校验报告

| 字段 | 值 |
|------|-----|
| 报告编号 | PROJECT-MASTER-MAP-CONSISTENCY-001 |
| 校验日期 | 2026-09-09 |
| 校验范围 | Project Master Map 全量文档 |
| 校验结果 | **16 项不一致**（原有 10 项 + 新增 6 项） |

---

## 校验概览

本次校验对 Project Master Map 的统计数字、Upstream/Downstream 关系、Identity 追踪、Broken Chain 完整性、Foundation 对象定义及跨域所有权等维度进行一致性审查。

---

## 校验结果汇总

| # | 严重程度 | 类别 | 问题摘要 | 状态 |
|---|----------|------|----------|------|
| 1 | 中 | 统计数字不一致 | Pages: 200+ vs 170+ | 待处理 |
| 2 | 中 | 统计数字不一致 | Foundation Objects: 15 vs 19 | 待处理 |
| 3 | 中 | Upstream/Downstream 未闭合 | Warehouse → Inventory 未在 downstream 中明确 | 待处理 |
| 4 | 中 | Identity 追踪不一致 | productId Wrong Identity 在 Data Lineage 中未提及 | 待处理 |
| 5 | 高 | Identity 追踪不一致 | warehouseId Identity Loss 在 Data Lineage 中未追踪 | 待处理 |
| 6 | 低 | Broken Chain 重复 | BC-001/004/005 内容重叠 | 待处理 |
| 7 | 低 | Broken Chain 重复 | BC-003/030 内容重叠 | 待处理 |
| 8 | 中 | Missing Foundation 误判 | Warehouse/Unit/PaymentMethod 可能是 Embedded/Config | 待处理 |
| 9 | 中 | Cross-Domain Owner 不明确 | Order 的 Owner 在多端写入时需明确主数据所有权 | 待处理 |
| 10 | 中 | 统计口径 | Foundation 对象数量在不同文件中定义不一致 | 待处理 |
| 11 | **高** | 统计数字不一致 | Foundation Objects 三方不一致: 15 vs 19 vs 18 | 待处理 |
| 12 | **高** | 对象覆盖 Gap | BOM/Foundation/MDS 三张地图对象覆盖范围不一致 | 待处理 |
| 13 | 中 | Foundation 状态冲突 | Warehouse 状态矛盾: READY vs MISSING | 待处理 |
| 14 | 中 | 层级定义不一致 | upstream/downstream/foundation 使用不同层级体系 | 待处理 |
| 15 | 低 | 统计数字不一致 | 事件数量差异: 22 vs 10 | 待处理 |

---

## 详细问题清单

### 问题 1：统计数字不一致 - Pages 数量

| 字段 | 内容 |
|------|------|
| 严重程度 | 中 |
| 类别 | 统计数字不一致 |
| 描述 | Pages 数量在不同位置分别标注为 200+ 和 170+，存在约 30 页的差异 |
| 影响 | 文档可信度降低，读者无法确认实际页面规模 |
| 建议 | 统一统计口径，明确是否包含附属文档/子页面 |

### 问题 2：统计数字不一致 - Foundation Objects 数量

| 字段 | 内容 |
|------|------|
| 严重程度 | 中 |
| 类别 | 统计数字不一致 |
| 描述 | Foundation Objects 数量分别标注为 15 和 19，差异为 4 个对象 |
| 影响 | 无法确定实际 Foundation 对象数量，影响架构完整性评估 |
| 建议 | 重新盘点并统一 Foundation 对象定义 |

### 问题 3：Upstream/Downstream 未闭合

| 字段 | 内容 |
|------|------|
| 严重程度 | 中 |
| 类别 | Upstream/Downstream 未闭合 |
| 描述 | Warehouse → Inventory 的下游关系未在 downstream 中明确标注 |
| 影响 | 数据流向追踪不完整，可能遗漏关键依赖 |
| 建议 | 补充 Warehouse 的 downstream 标注，确保 Inventory 出现在其下游列表中 |

### 问题 4：Identity 追踪不一致 - productId

| 字段 | 内容 |
|------|------|
| 严重程度 | 中 |
| 类别 | Identity 追踪不一致 |
| 描述 | productId 的 Wrong Identity 问题在 Data Lineage 中未被提及 |
| 影响 | 数据质量风险未在数据血缘中体现，可能遗漏治理措施 |
| 建议 | 在 Data Lineage 中补充 productId 的 Wrong Identity 标注及处理策略 |

### 问题 5：Identity 追踪不一致 - warehouseId

| 字段 | 内容 |
|------|------|
| 严重程度 | **高** |
| 类别 | Identity 追踪不一致 |
| 描述 | warehouseId 的 Identity Loss 问题在 Data Lineage 中未被追踪 |
| 影响 | 仓库维度数据可能在流转过程中丢失，影响库存准确性 |
| 建议 | **优先处理**：在 Data Lineage 中补充 warehouseId Identity Loss 追踪，并建立监控机制 |

### 问题 6：Broken Chain 重复 - BC-001/004/005

| 字段 | 内容 |
|------|------|
| 严重程度 | 低 |
| 类别 | Broken Chain 重复 |
| 描述 | BC-001、BC-004、BC-005 三个 Broken Chain 记录存在内容重叠 |
| 影响 | 问题追踪冗余，可能导致修复工作重复 |
| 建议 | 合并相关记录，保留最完整的描述，删除重复项 |

### 问题 7：Broken Chain 重复 - BC-003/030

| 字段 | 内容 |
|------|------|
| 严重程度 | 低 |
| 类别 | Broken Chain 重复 |
| 描述 | BC-003 和 BC-030 两个 Broken Chain 记录存在内容重叠 |
| 影响 | 问题追踪冗余 |
| 建议 | 合并相关记录，统一编号 |

### 问题 8：Missing Foundation 误判

| 字段 | 内容 |
|------|------|
| 严重程度 | 中 |
| 类别 | Missing Foundation 误判 |
| 描述 | Warehouse、Unit、PaymentMethod 可能被误判为 Missing Foundation，实际应为 Embedded/Config 类型 |
| 影响 | Foundation 对象分类不准确，影响架构分层理解 |
| 建议 | 重新评估这三个对象的定位，明确其为 Embedded 还是 Config 类型，并更新分类 |

### 问题 9：Cross-Domain Owner 不明确

| 字段 | 内容 |
|------|------|
| 严重程度 | 中 |
| 类别 | Cross-Domain Owner 不明确 |
| 描述 | Order 在多端写入场景下，主数据所有权归属不明确 |
| 影响 | 数据冲突时无法确定权威来源，可能导致数据不一致 |
| 建议 | 明确 Order 的主数据所有权规则，定义多端写入时的优先级和冲突解决策略 |

### 问题 10：统计口径不一致

| 字段 | 内容 |
|------|------|
| 严重程度 | 中 |
| 类别 | 统计口径 |
| 描述 | Foundation 对象数量在不同文件中定义不一致（与问题 2 关联） |
| 影响 | 文档间数据不一致，降低整体可信度 |
| 建议 | 建立统一的统计口径定义，确保所有引用同一指标的文件保持一致 |

### 问题 11：Foundation Objects 数量三方不一致（MAP-META-CONFLICT 新增）

| 字段 | 内容 |
|------|------|
| 严重程度 | **高** |
| 类别 | 统计数字不一致 |
| 描述 | summary 声称 15 个，foundation-map 列出 19 个，registry 列出 18 个。summary 的 "12+2+3+2" 加总为 19，与声称的 15 自相矛盾 |
| 影响 | 无法确定实际 Foundation 对象数量，影响架构完整性评估 |
| 建议 | 统一为 19 个，更新 summary 的数字和描述 |

### 问题 12：三张地图对象覆盖范围不一致（MAP-META-CONFLICT 新增）

| 字段 | 内容 |
|------|------|
| 严重程度 | **高** |
| 类别 | 对象覆盖 Gap |
| 描述 | business-object-map 有 28 个对象但缺少 Foundation 级对象（Supplier, Store, Employee 等）；foundation-map 有 19 个但缺少业务级对象（Inventory, Order 等）；master-data-source-map 有 15 个但缺少财务对象 |
| 影响 | 对象在多地图中重复定义或遗漏，维护成本增加 |
| 建议 | 定义每张地图的对象收录规则，将 Foundation 级对象补充到 BOM |

### 问题 13：Warehouse 状态矛盾（MAP-META-CONFLICT 新增）

| 字段 | 内容 |
|------|------|
| 严重程度 | 中 |
| 类别 | Foundation 状态冲突 |
| 描述 | capability-readiness-map 将 Warehouse 标为 ✅ READY，但 foundation-map 和 registry 均标记为 MISSING |
| 影响 | 架构理解混乱，可能误导开发决策 |
| 建议 | 统一为 "MISSING — 无独立表，通过 inventory.warehouse_id 引用" |

### 问题 14：层级定义不一致（MAP-META-CONFLICT 新增）

| 字段 | 内容 |
|------|------|
| 严重程度 | 中 |
| 层级定义不一致 |
| 描述 | upstream-dependency-map 使用 Layer 0-3 分层；downstream-impact-map 使用 Foundation → Capability → Transaction；foundation-map 使用 ROOT/VERIFIED/MISSING 分类。三者语义不同但存在交叉 |
| 影响 | 跨地图引用时层级混淆，影响依赖分析准确性 |
| 建议 | 定义标准层级体系并更新所有地图 |

### 问题 15：事件数量差异（MAP-META-CONFLICT 新增）

| 字段 | 内容 |
|------|------|
| 严重程度 | 低 |
| 类别 | 统计数字不一致 |
| 描述 | summary 声称 22 个领域事件，registry 仅列出 10 个命名事件，差异 12 个 |
| 影响 | 事件覆盖范围不清晰 |
| 建议 | 核实实际事件数量，更新 registry 补充缺失事件 |

---

## 严重程度分布

| 严重程度 | 数量 | 占比 |
|----------|------|------|
| 高 | 2 | 12.5% |
| 中 | 10 | 62.5% |
| 低 | 4 | 25% |
| **合计** | **16** | **100%** |

---

## 建议处理优先级

| 优先级 | 问题编号 | 原因 |
|--------|----------|------|
| P0 - 立即处理 | #5, #11 | 高严重度：Identity Loss + Foundation Objects 数量不一致 |
| P1 - 尽快处理 | #1, #2, #10, #12 | 统计数字和对象覆盖不一致影响文档可信度 |
| P1 - 尽快处理 | #3 | Upstream/Downstream 未闭合影响数据流追踪 |
| P1 - 尽快处理 | #9 | Cross-Domain Owner 不明确影响数据治理 |
| P2 - 计划处理 | #4, #13, #14, #15 | Identity 追踪缺失、Warehouse 状态冲突、层级定义不一致 |
| P2 - 计划处理 | #8 | Foundation 分类误判，需重新评估 |
| P3 - 有空处理 | #6, #7, #16 | Broken Chain 重复、事件数量差异，低优先级清理 |

---

## 结论

本次校验发现 **16 项不一致**，其中高严重度 2 项、中严重度 10 项、低严重度 4 项。

**核心发现：**
- Foundation Objects 数量在不同位置存在明显差异（15 vs 19 vs 18），需统一为 19
- 三张地图（BOM/Foundation/MDS）的对象覆盖范围不一致，需定义收录规则
- Warehouse 状态在不同地图中矛盾（READY vs MISSING），需统一
- Identity 追踪存在遗漏，特别是 warehouseId 的 Identity Loss 问题需优先处理
- Broken Chain 记录存在重复，需合并清理

**建议行动：**
1. **立即** 统一 Foundation Objects 数量为 19，更新所有引用文档
2. **本周内** 定义对象归属规则，将 Foundation 级对象补充到 BOM
3. **两周内** 完善 Upstream/Downstream 关系和 Cross-Domain Owner 定义
4. **三周内** 统一层级定义，清理 Broken Chain 重复记录，完成 Warehouse 状态修正

---

*报告更新时间：2026-09-09*
