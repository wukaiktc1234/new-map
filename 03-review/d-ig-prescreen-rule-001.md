# D-IG — Identity Necessity Pre-Screen Rule 001

> **状态**: ACTIVE_REFERENCE — METHOD RULE
> **Decision ID**: D-IG
> **性质**: Pre-E1 screening rule
> **禁止事项**: 本规则不确认任何 Identity，不决定 H1/H2，不授权 Schema、Migration 或 Canonical Table Creation。

## 1. Purpose

Identity Necessity Pre-Screen 的目的，是在 E1–E4 之前减少不必要的深测对象，同时保留所有 E0 候选的可追溯性。

Pre-Screen 不是业务能力清单，也不是 Identity Decision。它只回答：

> **某候选是否存在足以进入 E1–E4 的“不可补偿语义损失”信号？**

## 2. Inputs

Pre-Screen 必须使用：

1. D-IG E0 Candidate Object List；
2. BUSINESS_OWNER 确认边界内的 Business Capability Set；
3. 可定位的测试案例；
4. A1/A2 已确认的语义边界。

单一案例（例如采购→库存→配方→销售）不得作为全局业务能力全集。测试案例只是业务能力集合中的验证案例。

## 3. Business Capability Source Boundary

Business Capability Set 必须区分来源，不得把当前系统能力等同于完整业务需求：

- `IMPLEMENTED`: 当前代码路径或现有工程证据明确证明已实现的业务能力；
- `DECLARED`: BUSINESS_OWNER 已明确记录、但当前可能尚未实现的业务能力；必须有可定位 Owner 记录；
- `UNKNOWN`: 业务上可能存在但尚未被 Owner 或可靠证据确认的能力。

Pre-Screen 只可使用 `IMPLEMENTED` 与 `DECLARED` 作为排除候选的依据。

`UNKNOWN` 不得用于证明某候选“不需要”；相关候选应标记 `PENDING_CAPABILITY_CONFIRMATION`，等待 Owner 裁定。

工程团队不得自行把行业惯例、未来推测或未确认规划加入 `DECLARED`。

## 4. Necessity Test

对候选对象 O 和业务能力 B：

> 从语义模型中移除 O 后，B 是否出现不可补偿的语义损失？

只有至少出现以下一种情况，才产生 `NEEDS_TESTING` 信号：

1. **不可表达**：B 的核心业务操作无法执行；
2. **不可引用**：B 必须直接引用 O，而不存在等价替代路径；
3. **语义不一致**：B 的多个实例必须共享稳定语义，但移除 O 后只能通过复制、重写或不受控重复维护保持一致。

上述损失必须同时满足：

> 无法通过 Attribute / Role / Relationship / Quantity / Location / Tracking / Event 等已允许的非-Identity 语义组合进行无损补偿。

“实现困难”“当前代码没有”“需要更多字段”“未来可能方便”均不足以构成不可补偿损失。

## 5. Outcomes

每个候选必须得到以下之一：

- `OBSERVED_ONLY`: 当前证据下没有发现不可补偿语义损失；不进入 E1–E4，但保留在 E0；
- `NEEDS_TESTING`: 至少一个已确认业务能力出现不可补偿语义损失信号；进入 E1–E4；
- `PENDING_CAPABILITY_CONFIRMATION`: 结论依赖 `UNKNOWN` 能力；不得据此排除候选；
- `PROVISIONAL_NEEDS_PRODUCTION`: 判断依赖生产证据，而 Production Gate-0 仍 BLOCKED。

Pre-Screen outcome 不等于 Identity Decision。

## 6. Non-Goals

Pre-Screen 不得：

- 预设最终 Identity 名称；
- 把 Food / Material / Product 解释为已确认的 Identity；
- 把 Holding / Batch / UOM / Recipe / Event 自动升级为 Identity；
- 以候选数量作为质量标准；
- 以行业模型作为项目业务需求；
- 以当前工程缺失反推出业务不需要某能力；
- 替代 E1–E4。

## 7. Required Output

Pre-Screen 结果应记录：

- `candidate_id`；
- `capability_id`；
- `capability_source`；
- `removal_effect`；
- `loss_type`（`UNEXPRESSIBLE` / `UNREFERENCABLE` / `SEMANTIC_INCONSISTENCY`）；
- `compensation_attempt`；
- `compensation_result`；
- `evidence_reference`；
- `outcome`；
- `production_dependency`；
- `review_status`。

建议结果文件：

`03-review/d-ig-e0-prescreen-result-001.yaml`

## 8. Governance State

当前规则建立后：

- D-IG = OPEN；
- E0 = STRUCTURALLY_CORRECTED_PENDING_OWNER_REVIEW；
- Business Capability Set = NOT_YET_ESTABLISHED；
- Pre-Screen execution = NOT_STARTED；
- E1–E4 = BLOCKED_PENDING_PRE_SCREEN;
- H1/H2 = NOT_MADE；
- Schema = BLOCKED；
- Migration = BLOCKED。
