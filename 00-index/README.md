# 项目架构知识库总索引
## Project Architecture Knowledge Base

> 本仓库用于保存项目的工程现实、业务基础、业务依赖、系统评审、决策历史、治理规则、业务项语义模型、库存语义模型以及后续正式领域模型与数据契约。
>
> 本 README 是整个知识库的**总入口 / 总导航 / 阅读指南**。
>
> 本文档本身不负责定义某一个业务域的最终模型；它负责回答：
>
> - 当前知识库有哪些内容？
> - 每类文档解决什么问题？
> - 哪些文档描述 Current Reality？
> - 哪些文档属于 Analysis / Recommendation？
> - 哪些文档属于 Decision / Governance？
> - 哪些文档可以直接指导工程？
> - 哪些文档仅用于追溯历史？
>
> **重要：本 README 不替代 Canonical Decision、Canonical Domain Model、Canonical Data Contract。**

---

# 1. 项目知识库定位

本仓库不是普通的开发文档目录。

它的目标是建立一套：

```text
事实可追溯
决策可追溯
需求可追溯
模型可追溯
数据可追溯
工程可追溯
````

的项目知识体系。

核心链路：

```text
Engineering Reality
        ↓
Business Dependency
        ↓
Review / Reconciliation
        ↓
Decision History
        ↓
Governance
        ↓
Business Semantic Model
        ↓
Domain Model
        ↓
Data Contract
        ↓
API Contract
        ↓
Remediation
        ↓
Engineering
```

---

# 2. 当前仓库结构

当前 `main` 分支知识库结构：

```text
new-map/
│
├── 00-index/
│   ├── README.md
│   ├── project-master-map-summary.md
│   └── project-master-registry.yaml
│
├── 01-engineering-reality/
│
├── 02-business-dependency/
│
├── 03-review/
│
├── 04-decision-history/
│
├── 05-governance/
│
├── 06-business-item-model/
│
├── 07-inventory-semantic-model/
│
└── 99-archive/
    └── delivery-history/
```

当前仓库已经完成从早期：

```text
Project Master Map
```

向：

```text
Project Architecture Knowledge Base
```

的结构演进。

---

# 3. 文档层级

当前知识库按照以下层级理解：

```text
00  Index
    ↓
01  Engineering Reality
    ↓
02  Business Dependency
    ↓
03  Review
    ↓
04  Decision History
    ↓
05  Governance
    ↓
06  Business Item Semantic Model
    ↓
07  Inventory Semantic Model
    ↓
99  Archive / Delivery History
```

未来可继续扩展：

```text
08-domain-models
09-data-contracts
10-api-contracts
11-remediation
```

这些目录当前尚未作为正式目录建立。

---

# 4. 文档状态与权威关系

必须区分以下状态：

## 4.1 Current Reality

描述：

> 当前系统实际上是什么。

例如：

* 当前数据库结构
* 当前 API
* 当前前端
* 当前业务对象
* 当前状态机
* 当前遗留代码
* 当前数据流

主要来源：

```text
01-engineering-reality
02-business-dependency
```

---

## 4.2 Review / Analysis

描述：

> 已经分析出来的问题、冲突、依赖与候选方向。

主要来源：

```text
03-review
06-business-item-model
07-inventory-semantic-model
```

这些文档可以非常详细，但是：

```text
Analysis ≠ Decision
Recommendation ≠ Decision
```

---

## 4.3 Decision

描述：

> 已经经过正式裁决的业务或架构决定。

主要来源：

```text
04-decision-history
```

但是 `04-decision-history` 中同时存在：

* 历史 Decision
* Open Decision
* Recovered Decision
* Invalidated Decision
* Decision Analysis

因此：

> **不能把整个 `04-decision-history` 目录都当作当前 Decision Baseline。**

当前决策必须结合：

```text
Decision Status
Decision Provenance
Governance Baseline
```

一起判断。

---

## 4.4 Governance

描述：

> 项目如何定义事实、状态、决策、问题、整改和工程准入。

主要来源：

```text
05-governance
```

这是当前知识库中最重要的治理层之一。

---

## 4.5 Canonical Target Model

未来正式建立：

```text
08-domain-models
09-data-contracts
10-api-contracts
```

这些目录中的内容一旦正式确认，将成为开发直接依据。

---

# 5. 文档权威优先级

当文档之间发生冲突时，不能单纯按照“最新文件”判断。

原则上应按照以下顺序确认：

```text
1. Canonical / Locked Decision
2. Canonical Domain Model
3. Canonical Data Contract
4. Canonical API Contract
5. Confirmed Business Requirement
6. Architecture Principle
7. Current Engineering Reality
8. Semantic Analysis / Recommendation
9. Historical Decision
10. Invalidated / Superseded Document
```

但有一个例外必须特别注意：

> Target Model 已确认，并不意味着 Current Reality 可以被改写。

例如：

```text
Current Reality:
目前系统使用 A

Target Model:
未来应该使用 B
```

这两个事实必须同时存在。

---

# 6. MAP-001：Engineering Reality

目录：

[`01-engineering-reality/`](../01-engineering-reality/)

核心问题：

> **系统现在实际上是什么？**

这是整个知识库的“工程事实层”。

---

## 6.1 MAP-001 的覆盖范围

包含：

```text
Frontend
Business Object
Database
API
Event / Job
Permission / Data Scope
State Machine
Truth Source
Conflict
Legacy
Issue Lifecycle
```

---

## 6.2 MAP-001 文件清单

| 文件                                                                                       | 内容                 | 用途                                                 |
| ---------------------------------------------------------------------------------------- | ------------------ | -------------------------------------------------- |
| [`api-map.md`](../01-engineering-reality/api-map.md)                                     | API 地图             | Controller、Route、Endpoint、消费情况                     |
| [`business-object-map.md`](../01-engineering-reality/business-object-map.md)             | 业务对象地图             | 当前业务对象、遗留对象、对象关系                                   |
| [`db-reality-map.md`](../01-engineering-reality/db-reality-map.md)                       | 数据库现实地图            | 表、字段、索引、约束、重复表                                     |
| [`event-job-map.md`](../01-engineering-reality/event-job-map.md)                         | 事件 / Job 地图        | Domain Event、Listener、Dispatcher、Scheduler         |
| [`frontend-map.md`](../01-engineering-reality/frontend-map.md)                           | 前端地图               | Management、POS、Kitchen、Employee、MiniProgram、Mobile |
| [`issue-lifecycle-map.md`](../01-engineering-reality/issue-lifecycle-map.md)             | Issue 生命周期         | 审计、决策、开发、QA、关闭流程                                   |
| [`legacy-map.md`](../01-engineering-reality/legacy-map.md)                               | Legacy 地图          | 废弃表、Service、Controller、Interface                   |
| [`permission-data-scope-map.md`](../01-engineering-reality/permission-data-scope-map.md) | 权限 / 数据范围          | Permission、Role、Data Scope                         |
| [`project-master-map.md`](../01-engineering-reality/project-master-map.md)               | Project Master Map | MAP-001 工程现实总览                                     |
| [`state-machine-map.md`](../01-engineering-reality/state-machine-map.md)                 | 状态机地图              | Order、Voucher、Inventory 等状态模型                      |
| [`truth-conflict-map.md`](../01-engineering-reality/truth-conflict-map.md)               | Truth / Conflict   | 真相源、冲突点、双写、Owner 冲突                                |

---

## 6.3 MAP-001 使用场景

当任务是：

```text
“当前代码在哪里？”
“这个接口现在谁在调用？”
“数据库到底有哪些表？”
“这个状态现在实际有哪些值？”
“有没有旧表？”
“这个业务对象到底有几个实现？”
```

优先阅读：

```text
01-engineering-reality
```

---

# 7. MAP-002：Business Dependency

目录：

[`02-business-dependency/`](../02-business-dependency/)

核心问题：

> **这些业务能力为什么能够成立？依赖什么基础？影响哪些下游？**

它连接：

```text
Foundation
↓
Master Data
↓
Capability
↓
Transaction
↓
Settlement
↓
Reporting
```

---

## 7.1 MAP-002 文件清单

| 文件                                                                                                       | 内容                                                                       |
| -------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------ |
| [`PROJECT-ID-REGISTRY.md`](../02-business-dependency/PROJECT-ID-REGISTRY.md)                             | 项目级 ID / 对象登记资料；当前属于业务依赖目录，后续应与 Governance 中的 Canonical ID Registry 保持一致 |
| [`broken-chain-map.md`](../02-business-dependency/broken-chain-map.md)                                   | 业务断链分析                                                                   |
| [`business-data-flow-map.md`](../02-business-dependency/business-data-flow-map.md)                       | 业务数据流                                                                    |
| [`business-identity-propagation-map.md`](../02-business-dependency/business-identity-propagation-map.md) | Business Identity 传播路径                                                   |
| [`capability-readiness-map.md`](../02-business-dependency/capability-readiness-map.md)                   | Capability 就绪度                                                           |
| [`cross-domain-coordination-matrix.md`](../02-business-dependency/cross-domain-coordination-matrix.md)   | 跨域职责 / Owner / 协调关系                                                      |
| [`data-design-map.md`](../02-business-dependency/data-design-map.md)                                     | 数据设计和字段关系                                                                |
| [`data-inheritance-map.md`](../02-business-dependency/data-inheritance-map.md)                           | 数据继承链                                                                    |
| [`data-lineage-map.md`](../02-business-dependency/data-lineage-map.md)                                   | 数据血缘                                                                     |
| [`dialog-form-data-contract-map.md`](../02-business-dependency/dialog-form-data-contract-map.md)         | Dialog / Form 数据契约                                                       |
| [`downstream-impact-map.md`](../02-business-dependency/downstream-impact-map.md)                         | Downstream 影响                                                            |
| [`foundation-map.md`](../02-business-dependency/foundation-map.md)                                       | Foundation 对象及基础依赖                                                       |
| [`master-data-source-map.md`](../02-business-dependency/master-data-source-map.md)                       | Master Data Source                                                       |
| [`truth-source-matrix.md`](../02-business-dependency/truth-source-matrix.md)                             | Truth Source 判断矩阵                                                        |
| [`upstream-dependency-map.md`](../02-business-dependency/upstream-dependency-map.md)                     | Upstream Dependency                                                      |

---

## 7.2 MAP-002 核心分析方向

### Foundation

回答：

```text
什么是基础数据？
谁依赖它？
它是否真正存在？
它是 Root / Embedded / Config / Master Data？
```

---

### Upstream

回答：

```text
某个业务能力依赖哪些基础？
为什么这个功能现在无法完整工作？
缺少哪个 upstream capability？
```

---

### Downstream

回答：

```text
如果停用某个 Foundation / Master Data，
哪些业务能力会受到影响？
```

---

### Data Inheritance

回答：

```text
页面
↓
Dialog
↓
Form
↓
API
↓
Service
↓
DB
```

数据是否一致传递。

---

### Data Lineage

回答：

```text
数据从哪里创建？
谁修改？
谁消费？
经过哪些转换？
是否发生 Identity Loss？
是否发生 Wrong Identity？
```

---

### Cross-Domain Coordination

回答：

```text
这个数据到底属于哪个 Domain？
谁是 Truth Owner？
谁可以写？
谁只能读？
谁负责派生？
```

---

# 8. MAP-REVIEW：系统评审层

目录：

[`03-review/`](../03-review/)

核心问题：

> **我们对工程现实和业务依赖进行校验后，真正发现了什么？**

这是从：

```text
Reality
```

走向：

```text
Governance / Decision
```

的中间层。

---

## 8.1 Review 文件清单

| 文件                                                                                                        | 内容                            |
| --------------------------------------------------------------------------------------------------------- | ----------------------------- |
| [`MAP-META-CONFLICT.md`](../03-review/MAP-META-CONFLICT.md)                                               | 地图元信息 / 统计 / 定义冲突             |
| [`architecture-principle-baseline.md`](../03-review/architecture-principle-baseline.md)                   | 架构原则基线                        |
| [`architecture-product-decision-register.yaml`](../03-review/architecture-product-decision-register.yaml) | 架构 / 产品决策登记                   |
| [`business-requirements-baseline.md`](../03-review/business-requirements-baseline.md)                     | 业务需求基线                        |
| [`project-master-map-conflict-registry.yaml`](../03-review/project-master-map-conflict-registry.yaml)     | Map 冲突注册表                     |
| [`project-master-map-consistency-report.md`](../03-review/project-master-map-consistency-report.md)       | 全量一致性检查                       |
| [`project-master-map-gap-analysis.md`](../03-review/project-master-map-gap-analysis.md)                   | Gap Analysis                  |
| [`project-master-review-summary.md`](../03-review/project-master-review-summary.md)                       | Project Master Review 汇总      |
| [`remediation-gate-report.md`](../03-review/remediation-gate-report.md)                                   | Remediation Gate              |
| [`system-fact-baseline.md`](../03-review/system-fact-baseline.md)                                         | System Fact Baseline          |
| [`systemic-problem-register.md`](../03-review/systemic-problem-register.md)                               | Systemic Problem / Root Cause |
| [`systemic-remediation-candidates.md`](../03-review/systemic-remediation-candidates.md)                   | 系统性整改候选                       |

---

## 8.2 Review 的核心产物

### System Fact

回答：

```text
已经确认什么事实？
哪些事实仍然冲突？
哪些只是 Partial Evidence？
```

---

### Business Requirement

回答：

```text
产品真正需要什么？
哪些是真实需求？
哪些只是旧代码推导出来的行为？
```

---

### Architecture Principle

回答：

```text
后续架构应该遵守什么原则？
```

---

### Systemic Problem

回答：

```text
问题到底是单点 Bug，
还是系统性模型问题？
```

---

### Remediation Candidate

回答：

```text
什么应该整改？
整改前提是什么？
是否已经具备工程执行条件？
```

---

# 9. Review 中的重要文档可信度说明

当前 Review 层仍保留部分统计口径不一致，这属于：

```text
Documentation Consistency Problem
```

例如：

* Pages 数量存在不同统计口径
* Foundation Object 数量存在不同统计口径
* Event 数量存在不同统计口径
* 不同地图的对象覆盖范围存在差异
* Warehouse 的 Foundation 状态存在冲突
* Identity Tracking 存在遗漏

详细记录见：

[`project-master-map-consistency-report.md`](../03-review/project-master-map-consistency-report.md)

因此：

> `project-master-map-summary.md` 中的统计数字主要用于 MAP-001 / MAP-002 历史结果参考，不应自动作为新的 Canonical Fact。

---

# 10. Decision History

目录：

[`04-decision-history/`](../04-decision-history/)

核心问题：

> **项目过去做过哪些决策？为什么做？哪些被确认？哪些被废止？哪些仍然开放？**

---

## 10.1 Decision 文件

| 文件                                                                                                    | 用途                         |
| ----------------------------------------------------------------------------------------------------- | -------------------------- |
| [`DEC-001-Warehouse-Decision.yaml`](../04-decision-history/DEC-001-Warehouse-Decision.yaml)           | Warehouse Decision 历史记录    |
| [`DEC-002-Unit-Decision.yaml`](../04-decision-history/DEC-002-Unit-Decision.yaml)                     | Unit Decision 历史记录         |
| [`DEC-003-PaymentMethod-Decision.yaml`](../04-decision-history/DEC-003-PaymentMethod-Decision.yaml)   | PaymentMethod Decision     |
| [`DEC-004-CustomerMember-Decision.yaml`](../04-decision-history/DEC-004-CustomerMember-Decision.yaml) | Customer / Member Decision |
| [`DEC-005-Price-Decision.yaml`](../04-decision-history/DEC-005-Price-Decision.yaml)                   | Price Decision             |

---

## 10.2 Decision Reconciliation

| 文件                                                                                        | 用途                     |
| ----------------------------------------------------------------------------------------- | ---------------------- |
| [`decision-recon-registry.yaml`](../04-decision-history/decision-recon-registry.yaml)     | Decision Recon 注册表     |
| [`decision-recon-summary.md`](../04-decision-history/decision-recon-summary.md)           | Decision Recon 汇总      |
| [`decision-requirement-matrix.md`](../04-decision-history/decision-requirement-matrix.md) | Decision ↔ Requirement |
| [`decision-dependency-graph.md`](../04-decision-history/decision-dependency-graph.md)     | Decision 依赖图           |
| [`decision-provenance-matrix.md`](../04-decision-history/decision-provenance-matrix.md)   | Decision Provenance    |

---

## 10.3 Decision Recovery / Validation

| 文件                                                                                                                    | 用途                     |
| --------------------------------------------------------------------------------------------------------------------- | ---------------------- |
| [`decision-baseline-recovery-summary.md`](../04-decision-history/decision-baseline-recovery-summary.md)               | 恢复可信 Decision Baseline |
| [`decision-classification-validation-report.md`](../04-decision-history/decision-classification-validation-report.md) | Decision 分类验证          |
| [`DECISION-INVALIDATION-REPORT.md`](../04-decision-history/DECISION-INVALIDATION-REPORT.md)                           | 无效 Decision 报告         |
| [`invalidated-decisions.md`](../04-decision-history/invalidated-decisions.md)                                         | 已废止 Decision           |

---

## 10.4 Open Decision

| 文件                                                                                        | 用途                          |
| ----------------------------------------------------------------------------------------- | --------------------------- |
| [`open-product-decisions.md`](../04-decision-history/open-product-decisions.md)           | 当前开放的 Product Decision      |
| [`open-architecture-decisions.md`](../04-decision-history/open-architecture-decisions.md) | 当前开放的 Architecture Decision |
| [`engineering-only-decisions.md`](../04-decision-history/engineering-only-decisions.md)   | Engineering-only Decision   |

---

## 10.5 Decision Package

| 文件                                                                                                                  | 用途                          |
| ------------------------------------------------------------------------------------------------------------------- | --------------------------- |
| [`architecture-decision-pack.md`](../04-decision-history/architecture-decision-pack.md)                             | Architecture Decision Pack  |
| [`product-decision-pack.md`](../04-decision-history/product-decision-pack.md)                                       | Product Decision Pack       |
| [`product-architecture-separation-analysis.md`](../04-decision-history/product-architecture-separation-analysis.md) | Product / Architecture 分离分析 |
| [`remediation-gate.md`](../04-decision-history/remediation-gate.md)                                                 | Decision → Remediation Gate |
| [`verified-locked-decisions.md`](../04-decision-history/verified-locked-decisions.md)                               | 已验证的 Locked Decisions       |
| [`INDEX.md`](../04-decision-history/INDEX.md)                                                                       | Decision History 内部索引       |

---

# 11. Decision 使用原则

不能简单认为：

```text
04-decision-history/
```

里的任何内容都是当前有效决策。

必须同时检查：

```text
Decision Status
+
Decision Provenance
+
Governance Status
+
是否 INVALIDATED
```

特别是：

```text
LOCKED
≠
VERIFIED
```

以及：

```text
CONFIRMED
≠
LOCKED
```

---

# 12. Governance

目录：

[`05-governance/`](../05-governance/)

核心问题：

> **以后所有事实、Decision、Requirement、Remediation 如何被统一管理？**

这是当前知识库的：

```text
Governance Baseline
```

---

## 12.1 Governance 文件

| 文件                                                                                  | 内容                            |
| ----------------------------------------------------------------------------------- | ----------------------------- |
| [`canonical-id-registry.md`](../05-governance/canonical-id-registry.md)             | Canonical ID 注册规则             |
| [`canonical-status-model.md`](../05-governance/canonical-status-model.md)           | Canonical Status 模型           |
| [`decision-provenance.md`](../05-governance/decision-provenance.md)                 | Decision 来源 / 证据 / Provenance |
| [`governance-baseline-summary.md`](../05-governance/governance-baseline-summary.md) | 治理基线汇总                        |
| [`hierarchical-layer-model.md`](../05-governance/hierarchical-layer-model.md)       | 层级模型                          |
| [`remediation-gate-review.md`](../05-governance/remediation-gate-review.md)         | Remediation Gate 规则           |

---

# 13. Canonical Status 模型

当前治理必须区分三个维度。

## Evidence Status

```text
UNVERIFIED
PARTIAL
VERIFIED
CONFLICT
```

---

## Decision Status

```text
OPEN
RECOMMENDED
CONFIRMED
LOCKED
INVALIDATED
```

---

## Execution Status

```text
ENGINEERING_READY
BLOCKED
IMPLEMENTED
```

---

## 三个维度不能混淆

例如：

```text
VERIFIED
≠
LOCKED
```

```text
LOCKED
≠
IMPLEMENTED
```

```text
ENGINEERING_READY
≠
IMPLEMENTED
```

这套区分是后续 AI Agent 协同开发的基础。

---

# 14. Governance Layer

当前知识库治理链：

```text
FACT
  ↓
PRINCIPLE
  ↓
DECISION
  ↓
REQUIREMENT
  ↓
SYSTEMIC PROBLEM
  ↓
REMEDIATION
  ↓
ENGINEERING CARD
```

只有走完必要的 Gate，才能进入 Engineering。

---

# 15. Business Item Semantic Model

目录：

[`06-business-item-model/`](../06-business-item-model/)

核心问题：

> **系统中“一个业务上的物”到底是什么？**

当前重点解决：

```text
Product
Food
Material
Item
Type
Role
Capability
Relationship
Profile
Context
Store Scope
UOM
Recipe
Procurement
Sales
Inventory
```

之间的关系。

---

## 15.1 目录说明

[`06-business-item-model/README.md`](../06-business-item-model/README.md)

当前目录定义：

> Business Item Model 的只读语义分析目录。

当前主要结论包括：

* Food / Product / Material 目前存在概念边界问题
* 同一业务对象可能在不同场景承担不同角色
* Inventory、Procurement、Sales、Recipe 等会共享部分业务对象
* 当前推荐采用 Hybrid Domain Model
* 但 recommendation 不能自动转化为最终 Decision

---

# 16. Business Item 文件分类

## 16.1 总览 / 注册

* [`README.md`](../06-business-item-model/README.md)
* [`business-item-model-summary.md`](../06-business-item-model/business-item-model-summary.md)
* [`business-item-model-reconciliation-summary.md`](../06-business-item-model/business-item-model-reconciliation-summary.md)
* [`business-item-model-reconciliation-registry.yaml`](../06-business-item-model/business-item-model-reconciliation-registry.yaml)

---

## 16.2 Identity / Taxonomy

* [`canonical-identity-analysis.md`](../06-business-item-model/canonical-identity-analysis.md)
* [`concept-taxonomy.md`](../06-business-item-model/concept-taxonomy.md)
* [`identity-resolution-rules.md`](../06-business-item-model/identity-resolution-rules.md)
* [`identity-type-role-capability.md`](../06-business-item-model/identity-type-role-capability.md)
* [`type-vs-role-analysis.md`](../06-business-item-model/type-vs-role-analysis.md)
* [`relationship-model.md`](../06-business-item-model/relationship-model.md)

这一组回答：

```text
Identity 是什么？
Type 是什么？
Role 是什么？
Capability 是什么？
Relationship 是什么？
Context 是什么？
```

---

## 16.3 Product / Food / Material

* [`product-semantic-analysis.md`](../06-business-item-model/product-semantic-analysis.md)
* [`food-semantic-analysis.md`](../06-business-item-model/food-semantic-analysis.md)
* [`material-semantic-analysis.md`](../06-business-item-model/material-semantic-analysis.md)
* [`product-food-material-boundary-analysis.md`](../06-business-item-model/product-food-material-boundary-analysis.md)
* [`dec-006-refined-decision-pack.md`](../06-business-item-model/dec-006-refined-decision-pack.md)

---

## 16.4 Business Capability

* [`sellable-capability-analysis.md`](../06-business-item-model/sellable-capability-analysis.md)
* [`purchasable-capability-analysis.md`](../06-business-item-model/purchasable-capability-analysis.md)
* [`stockable-capability-analysis.md`](../06-business-item-model/stockable-capability-analysis.md)
* [`inventory-stockable-model.md`](../06-business-item-model/inventory-stockable-model.md)
* [`inventory-object-model.md`](../06-business-item-model/inventory-object-model.md)

这一组回答：

```text
什么东西可以卖？
什么东西可以买？
什么东西可以库存？
这些 Capability 是属性、Role 还是 Context？
```

---

## 16.5 Store / UOM / Context

* [`store-context-analysis.md`](../06-business-item-model/store-context-analysis.md)
* [`store-scope-analysis.md`](../06-business-item-model/store-scope-analysis.md)
* [`unit-context-analysis.md`](../06-business-item-model/unit-context-analysis.md)
* [`unit-uom-analysis.md`](../06-business-item-model/unit-uom-analysis.md)

---

## 16.6 Procurement / Recipe / POS

* [`procurement-model.md`](../06-business-item-model/procurement-model.md)
* [`procurement-object-model.md`](../06-business-item-model/procurement-object-model.md)
* [`recipe-bom-model.md`](../06-business-item-model/recipe-bom-model.md)
* [`ingredient-recipe-relationship-analysis.md`](../06-business-item-model/ingredient-recipe-relationship-analysis.md)
* [`sales-pos-model.md`](../06-business-item-model/sales-pos-model.md)

---

## 16.7 Candidate / Validation / Recommendation

* [`candidate-model-comparison.md`](../06-business-item-model/candidate-model-comparison.md)
* [`candidate-semantic-models.md`](../06-business-item-model/candidate-semantic-models.md)
* [`model-stress-points.md`](../06-business-item-model/model-stress-points.md)
* [`counterexample-validation.md`](../06-business-item-model/counterexample-validation.md)
* [`current-to-target-semantic-mapping.md`](../06-business-item-model/current-to-target-semantic-mapping.md)
* [`cross-domain-impact.md`](../06-business-item-model/cross-domain-impact.md)
* [`recommendation.md`](../06-business-item-model/recommendation.md)

---

# 17. Business Item 当前状态

当前目录状态：

```text
READ-ONLY
SEMANTIC ANALYSIS
RECOMMENDATION
NOT FINAL DOMAIN MODEL
```

因此：

```text
Business Item Recommendation
≠
Canonical Business Item Decision
```

尤其以下内容仍需要正式 Decision：

```text
Product / Food / Material 最终边界
Canonical Identity 粒度
Item 是否最终成为统一 Canonical Object
Type / Role / Capability 关系
UOM 与 Packaging 关系
Store Scope
Inventory 对 Business Item 的依附关系
```

---

# 18. Inventory Semantic Model

目录：

[`07-inventory-semantic-model/`](../07-inventory-semantic-model/)

核心问题：

> **库存到底是什么？**

这是当前业务语义建模中非常关键的一层。

---

## 18.1 Inventory 分析主链

当前目录的整体分析链：

```text
Canonical Business Identity
        ↓
Stockable Capability
        ↓
Inventory Location
        ↓
Stock Quantity
        ↓
UOM / Conversion
        ↓
Inventory Movement
        ↓
Inventory State
        ↓
Derived Balance / View
```

同时分析：

```text
Purchase
   ↓
Receive
   ↓
Stock
   ↓
Transfer
   ↓
Sale / Consumption
   ↓
Adjustment
```

---

# 19. Inventory 文件清单

## 19.1 基础模型

* [`inventory-truth-analysis.md`](../07-inventory-semantic-model/inventory-truth-analysis.md)
* [`stockable-object-analysis.md`](../07-inventory-semantic-model/stockable-object-analysis.md)
* [`stockable-capability-analysis.md`](../07-inventory-semantic-model/stockable-capability-analysis.md)
* [`inventory-identity-analysis.md`](../07-inventory-semantic-model/inventory-identity-analysis.md)
* [`inventory-location-model.md`](../07-inventory-semantic-model/inventory-location-model.md)
* [`store-vs-warehouse-analysis.md`](../07-inventory-semantic-model/store-vs-warehouse-analysis.md)

---

## 19.2 Quantity / UOM

* [`inventory-quantity-model.md`](../07-inventory-semantic-model/inventory-quantity-model.md)
* [`inventory-uom-model.md`](../07-inventory-semantic-model/inventory-uom-model.md)
* [`uom-conversion-model.md`](../07-inventory-semantic-model/uom-conversion-model.md)
* [`partial-consumption-analysis.md`](../07-inventory-semantic-model/partial-consumption-analysis.md)
* [`open-package-analysis.md`](../07-inventory-semantic-model/open-package-analysis.md)

重点解决：

```text
Base Quantity
Purchase UOM
Stock UOM
Recipe UOM
Sales UOM
Conversion
Partial Consumption
Open Package
```

---

## 19.3 Movement / Flow

* [`inventory-movement-model.md`](../07-inventory-semantic-model/inventory-movement-model.md)
* [`sale-consumption-inventory-flow.md`](../07-inventory-semantic-model/sale-consumption-inventory-flow.md)
* [`purchase-receipt-inventory-flow.md`](../07-inventory-semantic-model/purchase-receipt-inventory-flow.md)
* [`transfer-inventory-flow.md`](../07-inventory-semantic-model/transfer-inventory-flow.md)
* [`cross-domain-inventory-flow.md`](../07-inventory-semantic-model/cross-domain-inventory-flow.md)

重点解决：

```text
Business Event
    ↓
Inventory Movement
    ↓
State Change
    ↓
Derived Result
```

---

## 19.4 State / Batch / Costing

* [`inventory-state-model.md`](../07-inventory-semantic-model/inventory-state-model.md)
* [`batch-lot-expiry-analysis.md`](../07-inventory-semantic-model/batch-lot-expiry-analysis.md)
* [`inventory-costing-interaction.md`](../07-inventory-semantic-model/inventory-costing-interaction.md)

---

## 19.5 Current → Target

* [`current-to-target-inventory-semantic-mapping.md`](../07-inventory-semantic-model/current-to-target-inventory-semantic-mapping.md)

---

## 19.6 Candidate / Stress / Recommendation

* [`candidate-inventory-models.md`](../07-inventory-semantic-model/candidate-inventory-models.md)
* [`inventory-model-stress-points.md`](../07-inventory-semantic-model/inventory-model-stress-points.md)
* [`inventory-semantic-recommendation.md`](../07-inventory-semantic-model/inventory-semantic-recommendation.md)
* [`counterexample-inventory-validation.md`](../07-inventory-semantic-model/counterexample-inventory-validation.md)

---

## 19.7 Inventory Registry / Summary

* [`inventory-semantic-summary.md`](../07-inventory-semantic-model/inventory-semantic-summary.md)
* [`inventory-semantic-registry.yaml`](../07-inventory-semantic-model/inventory-semantic-registry.yaml)
* [`README.md`](../07-inventory-semantic-model/README.md)

---

# 20. Inventory 当前状态

当前 Inventory 目录明确标记为：

```text
RECOMMENDATION
NOT CONFIRMED
```

因此：

```text
Inventory Semantic Recommendation
≠
Final Inventory Domain Model
```

当前分析已经形成的重要概念包括：

```text
Inventory Truth
Stockable Object
Stockable Capability
Inventory Location
Quantity
UOM
Conversion
Movement
State
Batch / Lot / Expiry
Consumption
Costing
Cross-Domain Flow
```

但最终模型仍需要和：

```text
Business Item
Product
Food
Material
Recipe
Procurement
Sales
Store
Warehouse
```

一起进行统一决策。

---

# 21. Business Item × Inventory 的关系

这是当前最重要的后续整合点之一。

两个目录不能独立永久存在。

需要最终形成：

```text
Business Item
       ↓
Type / Role / Capability
       ↓
Stockable Capability
       ↓
Inventory Location
       ↓
Stock Quantity
       ↓
Movement
```

以及：

```text
Product
Food
Material
Beverage
Packaged Goods
Ingredient
Sellable Item
Purchasable Item
Stockable Item
```

之间统一的语义关系。

---

# 22. 业务反例验证的重要性

当前模型不能只用“Food”作为唯一测试对象。

必须能够解释例如：

```text
Coca-Cola 330ml
```

同时可能：

```text
可销售
可采购
可库存
可被作为 Recipe Component 使用
```

并且：

```text
100 bottles
```

能够存在于：

```text
Warehouse
Store
Transit
```

等不同 Inventory Location。

因此最终模型必须能够表达：

```text
One Business Identity
        ↓
Multiple Business Capabilities
        ↓
Multiple Contexts
        ↓
Multiple Locations
        ↓
Multiple UOMs
```

而不是通过创建多个互相独立的“物”来解决问题。

---

# 23. Archive / Delivery History

目录：

[`99-archive/delivery-history/`](../99-archive/delivery-history/)

核心目的：

> 保存已经完成的工程 Wave、Production Gate、旧整改计划和历史交付材料。

这些文件：

```text
不是当前 Canonical Decision
不是当前 Domain Model
不是当前开发规范
```

但必须保留，以便追溯：

```text
为什么曾经这么做？
什么时候做的？
当时的工程依据是什么？
后来为什么改变？
```

---

## 23.1 Archive 文件

| 文件                                                                                                                                          | 内容                                   |
| ------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------ |
| [`system-fact-mapsystem-remediation-dependency-map.md`](../99-archive/delivery-history/system-fact-mapsystem-remediation-dependency-map.md) | 历史 System Remediation Dependency Map |
| [`wave-0-engineering-plan.md`](../99-archive/delivery-history/wave-0-engineering-plan.md)                                                   | Wave 0 工程计划                          |
| [`wave0-final-engineering-plan.md`](../99-archive/delivery-history/wave0-final-engineering-plan.md)                                         | Wave 0 最终工程计划                        |
| [`wave1-production-gate-001.md`](../99-archive/delivery-history/wave1-production-gate-001.md)                                               | Wave 1 Production Gate 001           |
| [`wave1-production-gate-002.md`](../99-archive/delivery-history/wave1-production-gate-002.md)                                               | Wave 1 Production Gate 002           |

---

# 24. 当前工程 Reality 的历史统计快照

以下数字来自早期：

```text
PROJECT-MASTER-MAP-001
+
PROJECT-MASTER-MAP-002
```

的 Summary。

它们用于理解系统规模，不作为新的 Canonical Fact。

---

## MAP-001 历史统计快照

```text
Core Objects        16
Tables              100+
Controllers / APIs  154
Pages               200+
Events / Jobs       47
Legacy Components   36
Conflict Points     10
Active Blockers     10
Dead Code Items     5
Engineering WPs     4
```

其中：

```text
Pages
Tables
Foundation Objects
Events
```

等部分统计后来在 Review 层发现存在不同口径。

因此应结合：

```text
03-review/project-master-map-consistency-report.md
```

一起阅读。

---

# 25. MAP-002 历史统计快照

早期 Summary 中记录：

```text
Foundation Objects
Upstream Dependencies
Broken Chains
Capability Readiness
Cross-Domain Risks
Data Lineage Risks
```

等多项指标。

但后续 Review 已发现：

```text
Foundation Objects:
15 vs 19 vs 18
```

等统计差异。

因此：

> **不要直接复制旧 Summary 中的数字作为当前事实。**

真正的统计基线应该通过后续统一的 Evidence / Registry 机制重新收敛。

---

# 26. 历史 Project Master Map 的 Top Risk 主题

早期 MAP-001 / MAP-002 已经识别出多个高价值问题：

```text
1. Order State Machine 多套并存
2. Amount Unit 分 / 元 双轨
3. account_balance Truth 问题
4. fund_flows 状态模型不完整
5. Publish Audit 缺失
6. Legacy Database
7. Legacy Service
8. Legacy Controller
9. Dead Code
10. Voucher Status Mapping
11. food / foods 双写
12. inventory / store_inventory 双写
13. Cost 双写
14. finance_record Metric Scope
15. role_stores / role_departments
16. Raw API Permission
17. Appeal API Ownership
18. Supplier H5 Authentication
19. Generic Approval Permission
20. Refund Approval Identity
```

这些项目已经从“单个 Bug”升级为：

```text
Systemic Problem / Remediation Candidate
```

部分历史问题已经通过新的工程 Wave 处理，部分仍需要 Decision 或新的 Systemic Remediation。

---

# 27. 当前业务 Truth 的阅读原则

当前知识库中必须避免：

```text
数据库存在
=
业务真相
```

以及：

```text
API 能写
=
API 是 Owner
```

以及：

```text
页面展示
=
Canonical Data
```

以及：

```text
Derived Balance
=
Truth
```

应始终区分：

```text
Truth Source
Write Authority
Write Actor
Read Consumer
Derived View
Snapshot
Reference
```

---

# 28. 当前主要 Canonical 思维

项目当前整体架构方向包括：

```text
一个 Core
+
Configurable Capability
```

同时保持：

```text
Group Unified Definition
+
Store Autonomy
```

并且：

```text
Definition
≠
Operating Scope
≠
Sales State
≠
Inventory Fact
≠
Financial Event
```

---

# 29. Domain 边界原则

后续所有 Domain Model 应明确：

```text
谁拥有 Truth？
谁可以创建？
谁可以修改？
谁可以停用？
谁可以引用？
谁只能读取？
谁可以派生？
谁负责事件？
```

跨域数据不能仅通过：

```text
“大家直接访问同一张表”
```

解决。

---

# 30. Business Event 与 Financial Event

必须保持：

```text
Business Event
≠
Financial Event
```

例如：

```text
Order Created
≠
Payment Confirmed
≠
Inventory Deducted
≠
Finance Confirmed
```

这些事件可以在业务流程上关联，但不能在模型上强制合并。

---

# 31. Inventory Truth 原则

当前 Inventory Analysis 的重要原则：

```text
Inventory Fact
≠
Balance Table
```

更重要的是：

```text
Store Inventory
+
Warehouse Inventory
```

不能因为存在两个页面或两个表，就自动形成两个 Inventory Truth。

聚合视图：

```text
Store A
Store B
Warehouse
Group Total
```

都可以展示库存，但不能因此产生第二套库存真相。

---

# 32. UOM 原则

库存相关数量不能简单假设：

```text
1 row = 1 item
```

必须考虑：

```text
Purchase UOM
Stock UOM
Recipe UOM
Sales UOM
Base UOM
Conversion
```

同时必须支持：

```text
Partial Consumption
Open Package
```

---

# 33. Identity 原则

最终系统必须区分：

```text
Canonical Identity
Type
Role
Capability
Relationship
Profile
Context
Scope
```

不能简单把所有概念都塞进：

```text
“Role”
```

也不能因为当前数据库里有：

```text
food
material
product
```

就直接假设它们一定对应三个完全独立的业务对象。

---

# 34. 当前知识库阅读顺序

## 第一次进入项目

```text
00-index/README.md
        ↓
05-governance
        ↓
01-engineering-reality
        ↓
02-business-dependency
        ↓
03-review
```

---

## 做业务建模

```text
05-governance
        ↓
03-review
        ↓
06-business-item-model
        ↓
07-inventory-semantic-model
```

---

## 做历史 Decision 追溯

```text
04-decision-history
```

---

## 做当前工程排查

```text
01-engineering-reality
        ↓
02-business-dependency
        ↓
03-review
```

---

## 做库存相关设计

```text
06-business-item-model
        ↓
07-inventory-semantic-model
```

而不能只看 Inventory。

---

# 35. 按问题类型查文档

| 问题                              | 第一入口                                              |
| ------------------------------- | ------------------------------------------------- |
| 当前代码在哪里？                        | 01-engineering-reality                            |
| 当前数据库是什么？                       | 01-engineering-reality/db-reality-map.md          |
| 当前 API 有哪些？                     | 01-engineering-reality/api-map.md                 |
| 当前业务对象有哪些？                      | 01-engineering-reality/business-object-map.md     |
| 数据从哪里来？                         | 02-business-dependency/data-lineage-map.md        |
| 谁是数据真相源？                        | 02-business-dependency/truth-source-matrix.md     |
| 上游依赖是什么？                        | 02-business-dependency/upstream-dependency-map.md |
| 下游影响是什么？                        | 02-business-dependency/downstream-impact-map.md   |
| 当前系统有哪些问题？                      | 03-review/systemic-problem-register.md            |
| 需求是什么？                          | 03-review/business-requirements-baseline.md       |
| 哪些 Decision 已存在？                | 04-decision-history                               |
| 为什么做出这个 Decision？               | 04-decision-history/decision-provenance-matrix.md |
| 治理规则是什么？                        | 05-governance                                     |
| Product / Food / Material 怎么理解？ | 06-business-item-model                            |
| Inventory 到底是什么？                | 07-inventory-semantic-model                       |
| 历史 Wave 怎么做的？                   | 99-archive/delivery-history                       |

---

# 36. AI Agent 使用原则

任何 AI Agent 在修改代码之前：

```text
必须知道当前属于哪个 Domain
        ↓
必须知道 Current Reality
        ↓
必须知道 Truth Source
        ↓
必须知道相关 Decision
        ↓
必须知道相关 Requirement
        ↓
必须知道 Domain Boundary
        ↓
必须知道 Data Contract
        ↓
才能进入 Engineering
```

不得：

```text
看到一个页面
→ 猜一个表
→ 写一个 API
→ 自己决定业务语义
```

---

# 37. 禁止把历史文档当成当前规范

以下情况必须特别警惕：

```text
历史 Wave
旧 Production Gate
旧 Decision
旧 Remediation Plan
Invalidated Decision
Superseded Model
```

它们只能回答：

> “过去发生了什么？”

不能自动回答：

> “现在应该怎么做？”

---

# 38. 当前分析模型的生命周期

一个新的业务模型建议按照：

```text
Analysis
    ↓
Candidate
    ↓
Comparison
    ↓
Stress Test
    ↓
Counterexample
    ↓
Recommendation
    ↓
Product / Architecture Review
    ↓
Decision
    ↓
Canonical Domain Model
```

推进。

---

# 39. 当前项目阶段

知识库当前已经完成：

```text
✅ Engineering Reality Mapping
✅ Business Dependency Mapping
✅ System Review
✅ Decision History Reconstruction
✅ Governance Baseline
✅ Business Item Semantic Analysis
✅ Inventory Semantic Analysis
```

当前重点不是继续无限扩展地图数量。

而是：

```text
Business Item
        +
Inventory
        ↓
Semantic Integration
        ↓
Decision
        ↓
Canonical Domain Model
```

---

# 40. 下一阶段的目标

下一阶段应逐步形成：

```text
Business Item + Inventory Integration
        ↓
Formal Decision
        ↓
Canonical Domain Model
        ↓
Canonical Data Contract
        ↓
Canonical API Contract
        ↓
Systemic Remediation
        ↓
Engineering
```

---

# 41. 未来目录规划

当前尚未建立正式目录，但后续知识库可继续扩展：

```text
08-domain-models/
```

保存正式确认的 Domain Model。

```text
09-data-contracts/
```

保存 Canonical Data Contract。

```text
10-api-contracts/
```

保存 Canonical API Contract。

```text
11-remediation/
```

保存 Systemic Remediation 与 Engineering Cards。

---

# 42. 文档进入 Canonical 层的条件

一个 Analysis 文档只有在满足：

```text
Evidence 可追溯
+
Requirement 明确
+
Decision 已确认
+
Domain Owner 明确
+
Truth Source 明确
+
Upstream / Downstream 明确
+
Data Contract 可定义
+
Acceptance Criteria 可验证
```

后，才能进入正式：

```text
Canonical Domain Model
```

---

# 43. 文档维护规则

## 新增工程现实

进入：

```text
01-engineering-reality
```

---

## 新增业务依赖分析

进入：

```text
02-business-dependency
```

---

## 新增问题 / Gap / Reconciliation

进入：

```text
03-review
```

---

## 新增 Decision / Decision History

进入：

```text
04-decision-history
```

---

## 新增治理规则

进入：

```text
05-governance
```

---

## 新增 Business Item 语义分析

进入：

```text
06-business-item-model
```

---

## 新增 Inventory 语义分析

进入：

```text
07-inventory-semantic-model
```

---

## 已完成的工程 Wave / Production Gate

进入：

```text
99-archive/delivery-history
```

---

# 44. 文档命名原则

优先使用能够表达语义的名称：

```text
*-map.md
*-analysis.md
*-model.md
*-summary.md
*-registry.yaml
*-matrix.md
*-report.md
```

其中：

```text
map
```

偏向：

> 当前现实 / 范围 / 分布

```text
analysis
```

偏向：

> 研究 / 比较 / 推导

```text
model
```

偏向：

> 结构 / 语义关系

```text
decision
```

偏向：

> 正式裁决

```text
registry
```

偏向：

> 可枚举、可机器读取的登记数据

---

# 45. 当前 README 的职责

本文件只负责：

```text
Index
Navigation
Document Classification
Reading Order
Governance Boundary
Project Knowledge Structure
```

本文件不负责：

```text
定义 Product
定义 Food
定义 Material
定义 Inventory Domain
定义 API
定义数据库
替代 Decision
替代 Domain Model
```

---

# 46. 最终知识库目标

最终希望任何一个 Product Owner、Architecture Owner、工程师或 AI Agent 都能够从这里回答：

```text
这个东西是什么？
        ↓
谁拥有它？
        ↓
它属于哪个 Domain？
        ↓
它的 Identity 是什么？
        ↓
它有什么 Capability？
        ↓
它在哪里生效？
        ↓
它如何被创建？
        ↓
它如何被修改？
        ↓
它如何产生 Business Event？
        ↓
它如何进入 Inventory？
        ↓
它如何进入 Finance？
        ↓
谁消费它？
        ↓
哪个数据是真相？
        ↓
哪个只是 View？
        ↓
当前实现是什么？
        ↓
目标模型是什么？
        ↓
这个目标模型是谁决定的？
        ↓
最后由哪个 Engineering Card 实现？
```

---

# 47. 最终原则

本项目不再采用：

```text
发现一个问题
→ 直接修代码
→ 再发现另一个问题
→ 再修代码
```

而采用：

```text
Reality
  ↓
Model
  ↓
Review
  ↓
Decision
  ↓
Contract
  ↓
Remediation
  ↓
Engineering
```

最终目标不是：

> “让现有代码继续跑。”

而是：

> **建立一个能够从单店扩展到标准连锁、集团化、多门店、多仓储、多终端，并且能够保持业务 Truth、数据一致性和治理可追溯性的餐饮运营系统。**

---

## 当前核心入口

### 工程现实

[`01-engineering-reality/`](../01-engineering-reality/)

### 业务依赖

[`02-business-dependency/`](../02-business-dependency/)

### 系统评审

[`03-review/`](../03-review/)

### 决策历史

[`04-decision-history/`](../04-decision-history/)

### 治理基线

[`05-governance/`](../05-governance/)

### Business Item

[`06-business-item-model/`](../06-business-item-model/)

### Inventory Semantic Model

[`07-inventory-semantic-model/`](../07-inventory-semantic-model/)

### 历史工程交付

[`99-archive/delivery-history/`](../99-archive/delivery-history/)

```

另外，我特意**没有把 `08/09/10/11` 当成已经存在的目录来写链接**，只把它们作为后续规划；同时也没有把 `06`、`07` 的推荐模型写成已经锁定的架构，因为你仓库当前明确还是 Recommendation / Not Confirmed。:contentReference[oaicite:2]{index=2}
```

[1]: https://github.com/wukaiktc1234/new-map/tree/main "GitHub - wukaiktc1234/new-map: 项目地图文档 · GitHub"
