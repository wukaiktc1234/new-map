# D-IG — Charter Amendment 001

> **状态**: ACTIVE_REFERENCE — GOVERNANCE AMENDMENT
> **Decision ID**: D-IG
> **Scope**: Minimal amendment to D-IG Charter 001; no change to H1/H2 decision logic.

## 1. Requirement-driven Input Incomplete

当 Requirement-driven sources 当前不可得时，D-IG 使用明确状态：

`REQUIREMENT_DRIVEN_INCOMPLETE`

该状态表示：

- Requirement-driven inputs 尚未取得、尚未定位或无法在当前 evidence package 中验证；
- 不表示相关业务需求不存在；
- 不允许工程团队以“没有记录”推断“没有需求”。

## 2. Effect on E0

E0 可以在 Evidence-driven inputs 已完成枚举、Requirement-driven sources 已明确 status 的前提下继续进行结构化整理，但：

- `REQUIREMENT_DRIVEN_INCOMPLETE` 不得解释为 requirement-complete；
- BUSINESS_OWNER 的完整性审核仍必须区分“当前已知候选是否有明显遗漏”和“未来/未记录需求是否已确认”；
- E0 完成不得自动解除 requirement-driven incompleteness。

## 3. Effect on Required Identity Object Set

在 Requirement-driven inputs 仍为 `REQUIREMENT_DRIVEN_INCOMPLETE` 时：

- Required Identity Object Set 的结论若受缺失需求直接影响，必须标记 `PENDING_REQUIREMENT_INPUT`；
- 不得把当前代码没有实现解释为业务不需要；
- 任何依赖未确认 Requirement-driven capability 的 Identity 排除结论，不得作为最终 Decision。

## 4. Effect on Schema / Migration

`REQUIREMENT_DRIVEN_INCOMPLETE` 不改变既有治理边界：

- Schema Design = BLOCKED；
- Migration = BLOCKED；
- Canonical Identity Table Creation = NOT_AUTHORIZED。

## 5. Relationship to Pre-Screen

Identity Necessity Pre-Screen 必须对 Business Capability 来源分层：

- `IMPLEMENTED`：当前工程证据已证明；
- `DECLARED`：BUSINESS_OWNER 已明确记录；
- `UNKNOWN`：尚未确认。

Pre-Screen 不得使用 `UNKNOWN` 能力排除候选。相关候选保持 `PENDING_CAPABILITY_CONFIRMATION`，直到 Owner 裁定能力边界。

## 6. No Change to D-IG Decision Logic

本 Amendment 不修改：

- E1–E4 定义；
- Required Identity Object Set 的最终 Decision 规则；
- H1/H2 推导规则；
- Identity Resolution Matrix；
- A1/A2 已确认语义边界。
