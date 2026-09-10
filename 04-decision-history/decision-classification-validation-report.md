# PROJECT-DECISION-RECON-001 — Decision 分类校验报告

> **文档类型**: Classification Validation Report  
> **文档版本**: v1.0  
> **生成日期**: 2026-09-09  
> **范围**: decision-recon 目录下全部 Decision 分类校验  
> **基于**: decision-recon-registry.yaml, decision-recon-summary.md, decision-dependency-graph.md, decision-requirement-matrix.md, product-decision-pack.md, architecture-decision-pack.md

---

## 一、校验摘要

| 校验项 | 结果 |
|--------|------|
| Decision 总数（含 IMPLICIT） | 20 |
| 分类正确 | 12 |
| 分类错误（需修正） | **2** |
| 需标记 SUPERSEDED | **1** |
| 需标记 SUBSUMED | **6** |
| 需修订推荐方案 | **1** |
| 需细化角色定义 | **1** |

---

## 二、逐项分类校验

### 2.1 ALREADY_DECIDED — 已裁决决策

| Decision | 当前分类 | 正确分类 | 校验结论 | 问题说明 |
|----------|----------|----------|----------|----------|
| **DEC-006** | ALREADY_DECIDED | ALREADY_DECIDED ⚠️ 需修订 | **推荐方案需修正** | 见 §三.1 |
| **DEC-007** | ALREADY_DECIDED | ALREADY_DECIDED ✅ | 正确 | — |
| **DEC-008** | ALREADY_DECIDED | **SUPERSEDED** | **需重分类** | 见 §三.2 |
| **DEC-009** | ALREADY_DECIDED | ALREADY_DECIDED ⚠️ 需细化 | **角色定义不足** | 见 §三.3 |

### 2.2 PRODUCT_DECISION — 产品决策

| Decision | 当前分类 | 正确分类 | 校验结论 | 问题说明 |
|----------|----------|----------|----------|----------|
| **DEC-001** | PRODUCT_DECISION | PRODUCT_DECISION ✅ | 正确 | — |
| **DEC-004** | PRODUCT_DECISION | PRODUCT_DECISION ✅ | 正确 | — |

### 2.3 ARCHITECTURE_DECISION — 架构决策

| Decision | 当前分类 | 正确分类 | 校验结论 | 问题说明 |
|----------|----------|----------|----------|----------|
| **DEC-010** | ARCHITECTURE_DECISION | ARCHITECTURE_DECISION ✅ | 正确 | — |
| **DEC-011** | ARCHITECTURE_DECISION | ARCHITECTURE_DECISION ✅ | 正确 | — |

### 2.4 SECURITY_DECISION — 安全决策

| Decision | 当前分类 | 正确分类 | 校验结论 | 问题说明 |
|----------|----------|----------|----------|----------|
| **DEC-013** | SECURITY_DECISION | SECURITY_DECISION ✅ | 正确 | — |
| **DEC-014** | SECURITY_DECISION | SECURITY_DECISION ✅ | 正确 | — |

### 2.5 ENGINEERING_EXECUTION — 工程执行

| Decision | 当前分类 | 正确分类 | 校验结论 | 问题说明 |
|----------|----------|----------|----------|----------|
| **DEC-002** | ENGINEERING_EXECUTION | **PRODUCT_DECISION** | **分类错误** | 见 §三.4 |
| **DEC-003** | ENGINEERING_EXECUTION | **PRODUCT_DECISION** | **分类错误** | 见 §三.5 |
| **DEC-005** | ENGINEERING_EXECUTION | **PRODUCT_DECISION** | **分类错误** | 见 §三.6 |
| **DEC-012** | ENGINEERING_EXECUTION | ENGINEERING_EXECUTION ✅ | 正确 | Legacy 清理是纯执行项 |

### 2.6 IMPLICIT_DECISION — 隐含决策

| Decision | 当前分类 | 正确分类 | 校验结论 | 问题说明 |
|----------|----------|----------|----------|----------|
| **IMPLICIT-001** | IMPLICIT_DECISION | **SUBSUMED** | **需标记 SUBSUMED** | 见 §三.7 |
| **IMPLICIT-002** | IMPLICIT_DECISION | **SUBSUMED** | **需标记 SUBSUMED** | 见 §三.7 |
| **IMPLICIT-003** | IMPLICIT_DECISION | **SUBSUMED** | **需标记 SUBSUMED** | 见 §三.7 |
| **IMPLICIT-004** | IMPLICIT_DECISION | **SUBSUMED** | **需标记 SUBSUMED** | 见 §三.7 |
| **IMPLICIT-005** | IMPLICIT_DECISION | **SUBSUMED** | **需标记 SUBSUMED** | 见 §三.7 |
| **IMPLICIT-006** | IMPLICIT_DECISION | **SUBSUMED** | **需标记 SUBSUMED** | 见 §三.7 |

---

## 三、问题详解与修正建议

### 3.1 DEC-006: 继承模型无数据支撑 — 需修订推荐方案

**问题**: DEC-006 推荐 B（继承关系：Material→Food→Product），但该方案缺乏数据支撑。

**矛盾点**:
- `decision-recon-registry.yaml` 推荐 B_继承关系
- `product-decision-pack.md` 推荐 C_deprecate_product + KEEP双Master（废弃通用 Product，保留 Food 和 Material 作为两个独立主数据）
- product-decision-pack 的推荐更符合餐饮业务实际：Food 专注可售菜品管理，Material 专注原材料采购管理，概念清晰、业务逻辑解耦
- 继承模型（Material→Food→Product）在实际业务中存在语义问题：Product 不是 Food 的"子类"，Product 是 Food 的"包装形式"，继承关系不合理

**修正建议**:
- 状态保持 ALREADY_DECIDED
- 推荐方案从 B_继承关系 修订为 **C_deprecate_product + KEEP双Master**
- 理由：餐饮场景下 Food 和 Material 概念清晰，废弃冗余的 Product 概念，减少维护成本。非餐饮场景可按需扩展

---

### 3.2 DEC-008: 与 LOCK-003 冲突 — 应标记 SUPERSEDED

**问题**: DEC-008 (Amount/Money Contract) 与 LOCK-003 存在直接冲突。

**冲突详情**:
| 维度 | DEC-008 推荐 | LOCK-003 锁定 | 冲突 |
|------|-------------|--------------|------|
| 存储精度 | Decimal(10,2) | 整数存储（分） | **直接冲突** |
| 金额单位 | 元（带2位小数） | 分（整数） | **直接冲突** |
| 计算方式 | Decimal 运算 | 整数运算 | **直接冲突** |

**LOCK-003 已锁定内容**:
> "金额以分为准 (整数存储) — 新表统一用「分」，统一金额单位标准"  
> 来源: `decision-dependency-graph.md:16`, `decision-recon-summary.md:31`

**修正建议**:
- DEC-008 状态从 CONFIRMED 改为 **SUPERSEDED**
- 添加 `superseded_by: LOCK-003`
- 决策结论：金额统一以分（整数）存储，所有新表金额字段类型为 `BIGINT`，单位为分
- 原 DEC-008 的选项分析保留作为历史参考

---

### 3.3 DEC-009: 数据所有权粒度不足 — 需细化角色定义

**问题**: DEC-009 仅确定"业务域所有"的宏观原则，但缺乏角色级别的细化定义。

**缺失内容**:
| 角色 | 缺失定义 |
|------|---------|
| Truth Owner | 未明确哪个角色是数据的权威所有者 |
| Data Steward | 未明确谁负责数据质量维护 |
| Data Consumer | 未明确跨域读取的权限边界 |
| Data Custodian | 未明确技术层面的数据管理责任 |

**修正建议**:
- 状态保持 ALREADY_DECIDED
- 在 DEC-009 中补充角色定义矩阵:

| 角色 | 职责 | 权限 | 示例 |
|------|------|------|------|
| **Truth Owner** | 数据权威来源，负责数据创建和核心变更 | WRITE + READ | 订单域 Owner: Management Core |
| **Data Steward** | 数据质量维护，负责数据校验和清洗 | READ + 有限 WRITE | 菜品位 Steward: Product Manager |
| **Data Consumer** | 数据消费方，只读访问 | READ ONLY | 财务域消费订单数据 |
| **Data Custodian** | 技术管理，负责备份、归档、性能 | SYSTEM ADMIN | DBA |

---

### 3.4 DEC-002: Engineering Execution 错误包装为 Product Decision — 应重分类

**问题**: DEC-002 (Unit 管理模式) 在 registry 中被分类为 ENGINEERING_EXECUTION，但其核心问题本质是产品决策。

**分析**:
- DEC-002 的核心问题是: "Unit 是独立主数据还是其他对象的属性？"
- 选项包括: 独立表管理、枚举值、配置项
- 这些选项的选择直接影响业务语义（单位换算、动态扩展）
- decision-recon-summary.md 已将其标记为 SOLUTION_CLEAR（推荐 A_enum），但其本质仍是需要产品负责人确认的决策

**修正建议**:
- 分类从 ENGINEERING_EXECUTION 改为 **PRODUCT_DECISION**
- 状态保持 SOLUTION_CLEAR（推荐方案已明确）
- 理由：是否需要独立单位管理是产品层面的决策，而非纯工程执行

---

### 3.5 DEC-003: Engineering Execution 错误包装为 Product Decision — 应重分类

**问题**: DEC-003 (Payment Method 管理模式) 同样被错误分类为 ENGINEERING_EXECUTION。

**分析**:
- DEC-003 的核心问题是: "支付方式如何管理？支持哪些支付方式？如何扩展？"
- 选项包括: 枚举值、配置表、插件化
- 支付方式的管理粒度直接影响业务灵活性（分门店配置、动态扩展）
- decision-recon-summary.md 已将其标记为 SOLUTION_CLEAR（推荐 A_enum），但其本质仍是需要产品负责人确认的决策

**修正建议**:
- 分类从 ENGINEERING_EXECUTION 改为 **PRODUCT_DECISION**
- 状态保持 SOLUTION_CLEAR（推荐方案已明确）
- 理由：支付方式的管理模式选择是产品层面的决策

---

### 3.6 DEC-005: Engineering Execution 错误包装为 Product Decision — 应重分类

**问题**: DEC-005 (Price 管理模式) 同样被错误分类为 ENGINEERING_EXECUTION。

**分析**:
- DEC-005 的核心问题是: "价格如何定义？如何计算？如何管理价格策略？"
- 选项包括: 固定价格、价格策略、动态定价
- 价格管理的粒度直接影响业务能力（会员价、促销价、时段价）
- decision-recon-summary.md 已将其标记为 SOLUTION_CLEAR（推荐 A_inline），但其本质仍是需要产品负责人确认的决策

**修正建议**:
- 分类从 ENGINEERING_EXECUTION 改为 **PRODUCT_DECISION**
- 状态保持 SOLUTION_CLEAR（推荐方案已明确）
- 理由：价格管理模式的选择是产品层面的决策

---

### 3.7 IMPLICIT-001~006: 全部冗余 — 应标记 SUBSUMED

**问题**: IMPLICIT-001~006 与已有的 DEC 决策完全重复，存在严重冗余。

**冗余对照表**:

| IMPLICIT | 对应 DEC | 冗余程度 | 说明 |
|----------|---------|---------|------|
| IMPLICIT-001: Product/Food/Material Boundary | DEC-006 (ALREADY_DECIDED) | **完全重复** | DEC-006 已裁决 PFM 关系 |
| IMPLICIT-002: Order State Machine | DEC-007 (ALREADY_DECIDED) | **完全重复** | DEC-007 已裁决订单状态机 |
| IMPLICIT-003: Amount/Money Contract | DEC-008 (SUPERSEDED) + LOCK-003 | **完全重复** | LOCK-003 已锁定金额以分准 |
| IMPLICIT-004: Data Ownership | DEC-009 (ALREADY_DECIDED) | **完全重复** | DEC-009 已裁决数据所有权 |
| IMPLICIT-005: Cross-Domain Access | DEC-010 (ARCHITECTURE_DECISION) | **完全重复** | DEC-010 已是待决架构决策 |
| IMPLICIT-006: Event Architecture | DEC-011 (ARCHITECTURE_DECISION) | **完全重复** | DEC-011 已是待决架构决策 |

**冗余影响**:
- 决策注册表膨胀（20 项中 6 项为冗余）
- 依赖图复杂度虚增（IMPLICIT 节点增加无意义的依赖边）
- 决策顺序表混乱（同一决策出现两次）

**修正建议**:
- IMPLICIT-001~006 全部标记为 **SUBSUMED**
- 添加 `subsumed_by` 字段指向对应的 DEC
- 依赖图中删除 IMPLICIT-001~006 节点及其依赖边
- 保留 IMPLICIT 编号空间用于未来真正的隐含决策

---

## 四、分类错误模式分析

### 4.1 Engineering Execution 被错误包装为 Product Decision

**发现**: 无

**说明**: 当前 registry 中没有 Engineering Execution 被错误标记为 Product Decision 的情况。DEC-002/003/005 的问题是反方向的——Product Decision 被错误标记为 Engineering Execution。

### 4.2 Product Decision 被错误降级为 Engineering Execution

**发现**: **3 项** — DEC-002, DEC-003, DEC-005

**影响**:
- 这些决策的产品负责人确认环节被跳过
- 工程团队可能在未获得产品确认的情况下执行
- 决策权归属错误

**根因**: 这些决策的推荐方案已明确（SOLUTION_CLEAR），可能导致分类者认为"方案已定 = 执行阶段"，忽略了产品确认的必要性。

### 4.3 已确认旧决策重复开放

**发现**: **1 项** — IMPLICIT-003 与 DEC-008 重复

**详情**:
- LOCK-003 已锁定"金额以分为准"
- DEC-008 仍标记为 CONFIRMED（推荐 Decimal(10,2)）
- IMPLICIT-003 再次提出金额合约问题
- 三者对同一问题给出不同答案，且 DEC-008 与 LOCK-003 直接冲突

---

## 五、修正后的决策分类全景

### 5.1 修正后分类统计

| 分类 | 数量 | 决策列表 |
|------|------|---------|
| **LOCKED** | 10 | LOCK-001~010 |
| **SUPERSEDED** | 1 | DEC-008 (by LOCK-003) |
| **ALREADY_DECIDED** | 3 | DEC-006 (修订), DEC-007, DEC-009 (细化) |
| **PRODUCT_DECISION** | 5 | DEC-001, DEC-002, DEC-003, DEC-004, DEC-005 |
| **ARCHITECTURE_DECISION** | 2 | DEC-010, DEC-011 |
| **SECURITY_DECISION** | 2 | DEC-013, DEC-014 |
| **ENGINEERING_EXECUTION** | 1 | DEC-012 |
| **SUBSUMED** | 6 | IMPLICIT-001~006 |

### 5.2 修正后 Product Decision 清单

| Decision | 标题 | 状态 | 推荐方案 | 优先级 |
|----------|------|------|---------|--------|
| DEC-001 | Warehouse 管理模式 | PENDING_PRODUCT | E_hybrid | P0 |
| **DEC-002** | Unit 管理模式 | **SOLUTION_CLEAR** | A_enum | P1 |
| **DEC-003** | PaymentMethod 管理模式 | **SOLUTION_CLEAR** | A_enum | P1 |
| DEC-004 | Customer/Member 关系 | PENDING_PRODUCT | E_guest_member | P1 |
| **DEC-005** | Price 管理模式 | **SOLUTION_CLEAR** | A_inline | P2 |

### 5.3 修正后 ALREADY_DECIDED 清单

| Decision | 标题 | 推荐方案 | 修正说明 |
|----------|------|---------|---------|
| DEC-006 | Product-Food-Material 关系 | **C_deprecate_product + KEEP双Master** | 修订推荐方案 |
| DEC-007 | Order State Machine | B_完整状态机 | 无变更 |
| DEC-009 | Data Ownership | B_业务域所有 | 补充角色定义矩阵 |

---

## 六、修正后的依赖图影响

### 6.1 删除 IMPLICIT 节点后的依赖简化

**删除节点**: IMPLICIT-001~006

**删除的依赖边**:
- LOCK-004 → IMPLICIT-001
- LOCK-005 → IMPLICIT-001
- IMPLICIT-001 → IMPLICIT-002
- IMPLICIT-003 → DEC-008
- IMPLICIT-004 → DEC-009
- IMPLICIT-005 → DEC-010
- IMPLICIT-006 → DEC-011

**保留的依赖边**（原 IMPLICIT 依赖已由 DEC 覆盖）:
- LOCK-001 → DEC-006 (PFM 关系)
- DEC-007 → DEC-010 (订单状态→跨域)
- LOCK-003 → DEC-010 (金额→跨域)
- DEC-010 → DEC-011 (跨域→事件)

### 6.2 DEC-008 SUPERSEDED 后的影响

**原依赖**:
- DEC-008 → DEC-010 (金额一致性→跨域所有权)

**修正后**:
- LOCK-003 → DEC-010 (金额以分准→跨域所有权) — **已在 LOCK-003 覆盖**
- DEC-008 的依赖边删除（SUPERSEDED 决策不产生依赖）

---

## 七、修正行动清单

| 序号 | 行动 | 涉及 Decision | 优先级 | Owner |
|------|------|--------------|--------|-------|
| 1 | DEC-008 标记 SUPERSEDED，superseded_by = LOCK-003 | DEC-008 | P0 | 架构组 |
| 2 | DEC-006 推荐方案从 B 修订为 C | DEC-006 | P0 | 产品+架构 |
| 3 | DEC-009 补充角色定义矩阵 | DEC-009 | P1 | 数据治理 |
| 4 | DEC-002 分类从 ENGINEERING_EXECUTION 改为 PRODUCT_DECISION | DEC-002 | P1 | 架构组 |
| 5 | DEC-003 分类从 ENGINEERING_EXECUTION 改为 PRODUCT_DECISION | DEC-003 | P1 | 架构组 |
| 6 | DEC-005 分类从 ENGINEERING_EXECUTION 改为 PRODUCT_DECISION | DEC-005 | P1 | 架构组 |
| 7 | IMPLICIT-001~006 全部标记 SUBSUMED | IMPLICIT-001~006 | P1 | 架构组 |
| 8 | 更新 decision-recon-registry.yaml | 全部 | P0 | 架构组 |
| 9 | 更新 decision-dependency-graph.md | 全部 | P1 | 架构组 |
| 10 | 更新 decision-recon-summary.md | 全部 | P1 | 架构组 |
| 11 | 更新 decision-requirement-matrix.md | 全部 | P2 | 架构组 |

---

## 八、修正后的分类定义验证

| 分类 | 定义 | 本次校验结果 |
|------|------|-------------|
| **ALREADY_DECIDED** | 已确定的决策，需要确认和记录 | DEC-006/007/009 正确，但 DEC-006 需修订方案，DEC-009 需细化 |
| **PRODUCT_DECISION** | 产品层面的决策，需要产品负责人确认 | DEC-001/004 正确，DEC-002/003/005 需从 ENGINEERING 升级 |
| **ARCHITECTURE_DECISION** | 架构层面的决策，需要架构师确认 | DEC-010/011 正确 |
| **SECURITY_DECISION** | 安全相关的决策，需要安全团队确认 | DEC-013/014 正确 |
| **ENGINEERING_EXECUTION** | 工程执行层面的决策，需要开发团队确认 | DEC-012 正确，DEC-002/003/005 不应在此分类 |
| **IMPLICIT_DECISION** | 隐含的决策，需要识别和明确化 | IMPLICIT-001~006 均为冗余，应 SUBSUMED |
| **SUPERSEDED** | 已被更高优先级决策取代 | 新增分类，适用于 DEC-008 |
| **SUBSUMED** | 已被已有决策完全覆盖 | 新增分类，适用于 IMPLICIT-001~006 |

---

*生成时间: 2026-09-09*  
*评审编号: PROJECT-MASTER-REVIEW-001*  
*维护者: opencode*
