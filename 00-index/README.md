# 项目架构知识库
## Project Architecture Knowledge Base

> 项目工程现实、业务依赖、评审、决策、治理及业务语义模型的统一索引。

---

## 目录结构

| 目录 | 名称 | 用途 | 状态 |
|---|---|---|---|
| [`01-engineering-reality`](../01-engineering-reality/) | Engineering Reality | 当前系统工程现实 | Active |
| [`02-business-dependency`](../02-business-dependency/) | Business Dependency | Foundation、上下游、数据依赖与业务流 | Active |
| [`03-review`](../03-review/) | Review | 事实、需求、原则、问题、冲突与整改评审 | **Current Baseline** |
| [`04-decision-history`](../04-decision-history/) | Decision History | Product / Architecture Decision 及其历史 | Reference |
| [`05-governance`](../05-governance/) | Governance | ID、状态、Provenance、层级、Gate | Canonical |
| [`06-business-item-model`](../06-business-item-model/) | Business Item Model | Product / Food / Material / Item 等业务语义分析 | Analysis / Reference |
| [`07-inventory-semantic-model`](../07-inventory-semantic-model/) | Inventory Semantic Model | Inventory、Location、Quantity、UOM、Movement 等语义分析 | Analysis / Reference |
| [`99-archive/delivery-history`](../99-archive/delivery-history/) | Delivery History | 已完成 Wave、Production Gate 等历史材料 | Archive |

---

## Current Baseline

Business Item / Inventory 当前任务统一入口：

[`03-review/current-business-semantic-baseline.md`](../03-review/current-business-semantic-baseline.md)

后续相关任务默认从该文件开始，再按需要回查 06 / 07 历史分析。

---

# 01. Engineering Reality

[`01-engineering-reality/`](../01-engineering-reality/)

当前系统工程现实基线。

| 文档 | 内容 |
|---|---|
| [`project-master-map.md`](../01-engineering-reality/project-master-map.md) | 工程现实总览 |
| [`frontend-map.md`](../01-engineering-reality/frontend-map.md) | 前端与终端 |
| [`business-object-map.md`](../01-engineering-reality/business-object-map.md) | 业务对象 |
| [`db-reality-map.md`](../01-engineering-reality/db-reality-map.md) | 数据库现实 |
| [`api-map.md`](../01-engineering-reality/api-map.md) | API / Endpoint |
| [`event-job-map.md`](../01-engineering-reality/event-job-map.md) | Event / Job |
| [`permission-data-scope-map.md`](../01-engineering-reality/permission-data-scope-map.md) | Permission / Data Scope |
| [`state-machine-map.md`](../01-engineering-reality/state-machine-map.md) | 状态机 |
| [`truth-conflict-map.md`](../01-engineering-reality/truth-conflict-map.md) | Truth / Conflict |
| [`legacy-map.md`](../01-engineering-reality/legacy-map.md) | Legacy |
| [`issue-lifecycle-map.md`](../01-engineering-reality/issue-lifecycle-map.md) | Issue 生命周期 |

---

# 02. Business Dependency

[`02-business-dependency/`](../02-business-dependency/)

业务 Foundation、Master Data、上下游依赖、数据继承、血缘及跨域关系。

| 文档 | 内容 |
|---|---|
| [`foundation-map.md`](../02-business-dependency/foundation-map.md) | Foundation |
| [`upstream-dependency-map.md`](../02-business-dependency/upstream-dependency-map.md) | Upstream |
| [`downstream-impact-map.md`](../02-business-dependency/downstream-impact-map.md) | Downstream |
| [`capability-readiness-map.md`](../02-business-dependency/capability-readiness-map.md) | Capability Readiness |
| [`broken-chain-map.md`](../02-business-dependency/broken-chain-map.md) | Broken Chain |
| [`master-data-source-map.md`](../02-business-dependency/master-data-source-map.md) | Master Data Source |
| [`business-identity-propagation-map.md`](../02-business-dependency/business-identity-propagation-map.md) | Identity Propagation |
| [`business-data-flow-map.md`](../02-business-dependency/business-data-flow-map.md) | Business Data Flow |
| [`data-inheritance-map.md`](../02-business-dependency/data-inheritance-map.md) | Data Inheritance |
| [`data-lineage-map.md`](../02-business-dependency/data-lineage-map.md) | Data Lineage |
| [`data-design-map.md`](../02-business-dependency/data-design-map.md) | Data Design |
| [`dialog-form-data-contract-map.md`](../02-business-dependency/dialog-form-data-contract-map.md) | Dialog / Form Contract |
| [`cross-domain-coordination-matrix.md`](../02-business-dependency/cross-domain-coordination-matrix.md) | Cross-Domain Ownership |
| [`truth-source-matrix.md`](../02-business-dependency/truth-source-matrix.md) | Truth Source |

---

# 03. Review

[`03-review/`](../03-review/)

对工程现实与业务依赖进行一致性、需求、架构及系统性问题评审。

| 文档 | 内容 |
|---|---|
| [`system-fact-baseline.md`](../03-review/system-fact-baseline.md) | System Fact |
| [`business-requirements-baseline.md`](../03-review/business-requirements-baseline.md) | Business Requirement |
| [`architecture-principle-baseline.md`](../03-review/architecture-principle-baseline.md) | Architecture Principle |
| [`systemic-problem-register.md`](../03-review/systemic-problem-register.md) | Systemic Problem |
| [`systemic-remediation-candidates.md`](../03-review/systemic-remediation-candidates.md) | Remediation Candidates |
| [`project-master-review-summary.md`](../03-review/project-master-review-summary.md) | Review Summary |
| [`project-master-map-consistency-report.md`](../03-review/project-master-map-consistency-report.md) | Consistency |
| [`project-master-map-gap-analysis.md`](../03-review/project-master-map-gap-analysis.md) | Gap Analysis |
| [`project-master-map-conflict-registry.yaml`](../03-review/project-master-map-conflict-registry.yaml) | Conflict Registry |
| [`architecture-product-decision-register.yaml`](../03-review/architecture-product-decision-register.yaml) | Decision Register |
| [`remediation-gate-report.md`](../03-review/remediation-gate-report.md) | Remediation Gate |
| [`MAP-META-CONFLICT.md`](../03-review/MAP-META-CONFLICT.md) | Map Meta Conflict |

---

# 04. Decision History

[`04-decision-history/`](../04-decision-history/)

Product / Architecture Decision 的历史、恢复、验证、依赖与失效记录。

### Product Decisions

- [`DEC-001-Warehouse-Decision.yaml`](../04-decision-history/DEC-001-Warehouse-Decision.yaml)
- [`DEC-002-Unit-Decision.yaml`](../04-decision-history/DEC-002-Unit-Decision.yaml)
- [`DEC-003-PaymentMethod-Decision.yaml`](../04-decision-history/DEC-003-PaymentMethod-Decision.yaml)
- [`DEC-004-CustomerMember-Decision.yaml`](../04-decision-history/DEC-004-CustomerMember-Decision.yaml)
- [`DEC-005-Price-Decision.yaml`](../04-decision-history/DEC-005-Price-Decision.yaml)

### Decision Governance / History

- [`INDEX.md`](../04-decision-history/INDEX.md)
- [`decision-baseline-recovery-summary.md`](../04-decision-history/decision-baseline-recovery-summary.md)
- [`decision-provenance-matrix.md`](../04-decision-history/decision-provenance-matrix.md)
- [`decision-dependency-graph.md`](../04-decision-history/decision-dependency-graph.md)
- [`decision-requirement-matrix.md`](../04-decision-history/decision-requirement-matrix.md)
- [`verified-locked-decisions.md`](../04-decision-history/verified-locked-decisions.md)
- [`open-product-decisions.md`](../04-decision-history/open-product-decisions.md)
- [`open-architecture-decisions.md`](../04-decision-history/open-architecture-decisions.md)
- [`engineering-only-decisions.md`](../04-decision-history/engineering-only-decisions.md)
- [`DECISION-INVALIDATION-REPORT.md`](../04-decision-history/DECISION-INVALIDATION-REPORT.md)
- [`invalidated-decisions.md`](../04-decision-history/invalidated-decisions.md)

---

# 05. Governance

[`05-governance/`](../05-governance/)

项目当前治理基线。

| 文档 | 内容 |
|---|---|
| [`canonical-id-registry.md`](../05-governance/canonical-id-registry.md) | Canonical ID |
| [`canonical-status-model.md`](../05-governance/canonical-status-model.md) | Evidence / Decision / Execution Status |
| [`decision-provenance.md`](../05-governance/decision-provenance.md) | Decision Provenance |
| [`hierarchical-layer-model.md`](../05-governance/hierarchical-layer-model.md) | 文档与治理层级 |
| [`remediation-gate-review.md`](../05-governance/remediation-gate-review.md) | Remediation Gate |
| [`governance-baseline-summary.md`](../05-governance/governance-baseline-summary.md) | Governance Baseline |

---

# 06. Business Item Model

[`06-business-item-model/`](../06-business-item-model/)

Business Item 统一语义分析。

核心范围：

```text
Identity
Type
Role
Capability
Relationship
Profile
Context
Product
Food
Material
UOM
Store Scope
Recipe
Procurement
Sales
Inventory
````

入口：

* [`README.md`](../06-business-item-model/README.md)
* [`business-item-model-summary.md`](../06-business-item-model/business-item-model-summary.md)
* [`business-item-model-reconciliation-summary.md`](../06-business-item-model/business-item-model-reconciliation-summary.md)
* [`canonical-identity-analysis.md`](../06-business-item-model/canonical-identity-analysis.md)
* [`concept-taxonomy.md`](../06-business-item-model/concept-taxonomy.md)
* [`business-role-model.md`](../06-business-item-model/business-role-model.md)
* [`product-food-material-boundary-analysis.md`](../06-business-item-model/product-food-material-boundary-analysis.md)
* [`candidate-model-comparison.md`](../06-business-item-model/candidate-model-comparison.md)
* [`counterexample-validation.md`](../06-business-item-model/counterexample-validation.md)
* [`recommendation.md`](../06-business-item-model/recommendation.md)

当前状态：

**Semantic Analysis / Recommendation / Not Final Domain Model**

---

# 07. Inventory Semantic Model

[`07-inventory-semantic-model/`](../07-inventory-semantic-model/)

Inventory 业务语义分析。

核心范围：

```text
Inventory Truth
Stockable Object
Inventory Location
Store / Warehouse
Quantity
UOM
Conversion
Movement
Transfer
Consumption
Batch / Lot / Expiry
Costing
Cross-Domain Flow
```

入口：

* [`README.md`](../07-inventory-semantic-model/README.md)
* [`inventory-semantic-summary.md`](../07-inventory-semantic-model/inventory-semantic-summary.md)
* [`inventory-truth-analysis.md`](../07-inventory-semantic-model/inventory-truth-analysis.md)
* [`inventory-identity-analysis.md`](../07-inventory-semantic-model/inventory-identity-analysis.md)
* [`inventory-location-model.md`](../07-inventory-semantic-model/inventory-location-model.md)
* [`inventory-quantity-model.md`](../07-inventory-semantic-model/inventory-quantity-model.md)
* [`inventory-uom-model.md`](../07-inventory-semantic-model/inventory-uom-model.md)
* [`inventory-movement-model.md`](../07-inventory-semantic-model/inventory-movement-model.md)
* [`candidate-inventory-models.md`](../07-inventory-semantic-model/candidate-inventory-models.md)
* [`counterexample-inventory-validation.md`](../07-inventory-semantic-model/counterexample-inventory-validation.md)
* [`inventory-semantic-recommendation.md`](../07-inventory-semantic-model/inventory-semantic-recommendation.md)

当前状态：

**Semantic Analysis / Recommendation / Not Final Domain Model**

---

# 99. Archive

[`99-archive/delivery-history/`](../99-archive/delivery-history/)

已完成工程 Wave、Production Gate 及历史交付材料。

* [`wave-0-engineering-plan.md`](../99-archive/delivery-history/wave-0-engineering-plan.md)
* [`wave0-final-engineering-plan.md`](../99-archive/delivery-history/wave0-final-engineering-plan.md)
* [`wave1-production-gate-001.md`](../99-archive/delivery-history/wave1-production-gate-001.md)
* [`wave1-production-gate-002.md`](../99-archive/delivery-history/wave1-production-gate-002.md)
* [`system-fact-mapsystem-remediation-dependency-map.md`](../99-archive/delivery-history/system-fact-mapsystem-remediation-dependency-map.md)

状态：

**Historical / Archive**

---

# 知识库阅读顺序

```text
00-index
  ↓
05-governance
  ↓
01-engineering-reality
  ↓
02-business-dependency
  ↓
03-review
  ↓
04-decision-history
  ↓
06-business-item-model
  ↓
07-inventory-semantic-model
```

按具体任务选择对应目录，不要求每次完整阅读全部文档。

---

# 当前阶段

```text
Engineering Reality            ✅
Business Dependency            ✅
System Review                  ✅
Decision Recovery              ✅
Governance Baseline            ✅
Business Item Semantic Model   ✅
Inventory Semantic Model       ✅

下一阶段：

Business Item × Inventory Integration
        ↓
Formal Decision
        ↓
Canonical Domain Model
        ↓
Data Contract
        ↓
API Contract
        ↓
Remediation / Engineering
```

---

# 状态定义

## Document Status

| 状态 | 含义 | 说明 |
|------|------|------|
| **CURRENT_BASELINE** | 当前基线 | 最新综合结论，可作为下一任务直接输入 |
| **ACTIVE_REFERENCE** | 有效参考 | 仍有证据价值，但不是最终综合结论 |
| **SUPERSEDED** | 已替代 | 内容已被新的分析/Reassessment 替代 |
| **HISTORICAL** | 历史追溯 | 用于历史追溯 |
| **INVALIDATED** | 已废止 | 明确证明错误或废止 |

## Legacy Status（旧分类，仍可用于非 Business Item 目录）

| 状态 | 含义 |
|------|------|
| Active | 当前有效参考 |
| Canonical | 当前正式基线 |
| Analysis | 分析材料 |
| Recommendation | 推荐方案，未正式锁定 |
| Reference | 历史/辅助参考 |
| Archive | 历史交付材料 |
| Invalidated | 已明确失效 |

> 文档状态以各目录及具体文档中的声明为准。
> Business Item / Inventory 相关任务以 `03-review/current-business-semantic-baseline.md` 为统一入口。

````
