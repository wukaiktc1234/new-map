# D-IG — E3 Owner Business Rule Decision 001

> **状态**: ACTIVE_REFERENCE — OWNER BUSINESS RULE RECORD
> **Decision ID**: D-IG
> **适用候选**: C-001 Food / C-002 Material
> **来源**: Business Owner confirmation in the current governance session
> **Formal owner_identifier**: PENDING_FORMAL_IDENTIFIER
> **Formal review date/session metadata**: PENDING_FORMAL_RECORD
> **Identity Decision**: NOT_MADE
> **H1/H2**: NOT_MADE
> **Schema Authorization**: NO
> **Migration Authorization**: NO
> **Production Evidence**: BLOCKED

## 1. Purpose

本记录把 Business Owner 在当前治理会话中对 E3 业务规则问题作出的明确选择单独固化。

这些选择不再属于 DeepSeek recommendation，也不是模型推断；它们是当前 Business Owner 明确确认的业务定义输入。

本记录不替代 Owner Review 所需的正式身份、日期、会话等元数据；上述元数据仍待正式补齐。

## 2. C-001 Food — Owner confirmed rules

### E3A — INACTIVE

**Owner decision: A — 暂时下架**

含义：菜品暂时不卖、不在菜单上展示，但已经发生的历史订单仍然保留并可以正常查看、解释和引用。

### E3B — DISCONTINUED

**Owner decision: A — 正式停售**

含义：菜品以后不再作为正式销售菜品使用，但历史订单、成本和追溯记录继续保留。

### E3C — Historical order record

**Owner decision: C — 同时保留 Food 编号和当时菜名快照**

含义：历史订单既保留指向该菜品的稳定引用，也保留当时展示给顾客的菜名快照。

### E3D — State-change business event

**Owner decision: B — 仅部分状态变化触发业务事件**

当前只确认这一业务原则：不是所有状态变化都必须产生业务事件。

具体哪些状态转换触发什么下游行为，以及 POS、菜单、小程序、外部平台等各端如何响应，留到 Requirement 阶段定义；本记录不预先锁定这些实现或下游行为。

### Additional rule — DISCONTINUED → ACTIVE

**Owner decision: A — 可以直接恢复**

含义：正式停售后的菜品，后续业务仍可以直接恢复销售，不强制要求重新创建一个全新的菜品对象。

如后续需要审批、恢复条件或特殊流程，再在 Requirement 阶段定义。

## 3. C-002 Material — Owner confirmed rules

### E3M-A — INACTIVE

**Owner decision: A — 停止继续用于新的业务**

含义：原材料以后不能用于新的采购、库存和使用，但历史采购、库存、追溯等记录继续保留。

### E3M-B — Historical records

**Owner decision: C — 同时保留 Material 编号和当时名称快照**

含义：历史记录既保留稳定的原材料引用，也保留当时的名称信息。

### E3M-C — State-change business event

**Owner decision: B — 仅部分状态变化触发业务事件**

当前只确认这一业务原则：不是所有状态变化都必须产生业务事件。

具体状态转换以及后续各业务端的处理方式，留到 Requirement 阶段定义。

## 4. Food / Material lifecycle relationship

**Owner decision: 显式分离，但基本管理理念可以对齐。**

### Food

- ACTIVE：正常销售
- INACTIVE：暂时下架
- DISCONTINUED：正式停售

### Material

- ACTIVE：正常使用
- INACTIVE：停止新的采购、库存和使用

两者不是同一个生命周期模型。

Food 具有独立的 DISCONTINUED 业务语义；Material 当前不另外设置独立的 DISCONTINUED 状态。

两者的 INACTIVE 都表达“停止新的业务使用，同时保留历史记录”的管理原则，但具体业务含义不同：Food 偏向暂时不销售，Material 偏向停止继续用于新的采购、库存和使用。

## 5. Governance interpretation

以上选择属于 Owner business-rule inputs，不能被解释为：

- DeepSeek recommendation；
- 工程事实的自动推导；
- Requirement 已完成；
- Schema/Migration authorization；
- 最终 Identity Decision。

它们只解决 E3 当前要求的业务规则缺口，使 C-001 / C-002 的 E3 进入重新评价条件。

## 6. Re-evaluation prerequisites

重新评价 E3 时，应结合：

1. 已验证的独立 CRUD / Read / API / permission engineering evidence；
2. 本 Owner decision record；
3. E3 原有 adversarial lifecycle review；
4. 不把具体下游技术实现提前升级为 E3 的业务结论。

Owner 已明确回答的问题不应再次按“未知业务含义”处理。

## 7. Current governance state

- C-001 E3 owner rules: `CONFIRMED_IN_CHAT`
- C-002 E3 owner rules: `CONFIRMED_IN_CHAT`
- C-001 E3 final re-evaluation: `PENDING_REEVALUATION`
- C-002 E3 final re-evaluation: `PENDING_REEVALUATION`
- C-001 candidate-level conclusion: `NOT_READY`
- C-002 candidate-level conclusion: `NOT_READY`
- Required Identity Object Set: `NOT_STARTED`
- FM-001–FM-004: `OPEN`
- H1/H2: `NOT_MADE`
- Identity Decision: `NOT_MADE`
- Schema: `BLOCKED`
- Migration: `BLOCKED`
- Production evidence: `BLOCKED`
- Requirement-driven status: `REQUIREMENT_DRIVEN_INCOMPLETE`
