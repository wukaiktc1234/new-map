# D-IG — Charter Amendment 002

> **状态**: ACTIVE_REFERENCE — GOVERNANCE AMENDMENT
> **Decision ID**: D-IG
> **Scope**: 对 D-IG Charter 001 的 E1–E4 定义、Candidate-level gate 接线及 Required Identity Object Set 成集前置条件进行最小治理修正。
> **Identity Decision**: NOT_MADE
> **H1/H2**: NOT_MADE
> **Schema Authorization**: NO
> **Migration Authorization**: NO
> **Production Evidence**: BLOCKED

## 1. Amendment Purpose

远程治理包存在一个需在 Required Identity Object Set 成集前解决的定义漂移：

- Charter 001 §2.2 / §4 的 E1–E4 语义与现有 `d-ig-e1-e4-testing-001.md` 的执行定义不一致；
- Candidate-level 记录已经将 E1–E4 执行为“不可替代表达 / 独立直接引用 / 独立生命周期 / 稳定语义一致性”；
- 若不先对齐，Required Identity Object Set 的入集规则 `E1 ∧ E2` 会产生语义悬空。

本 Amendment 不重新裁定 C-001/C-002，也不授权 Identity Decision、H1/H2、Schema 或 Migration。

## 2. Canonical E1–E4 Definitions for Subsequent Execution

自本 Amendment 起，D-IG 后续执行记录统一采用以下定义：

- **E1 — 不可替代表达（Non-Substitutability / Inexpressibility）**：若移除候选对象，其已确认业务能力能否仅通过既有 A1 语义层及其组合无损表达；若必须重新引入等价、独立、可寻址对象才能保持业务行为，则候选的稳定业务对象角色不可被压缩。
- **E2 — 独立直接引用（Independent Direct Reference）**：至少存在一条已确认业务能力直接以稳定对象引用该候选，而非只携带其属性或快照。多个传播路径可作为佐证，不要求彼此独立起源。
- **E3 — 独立生命周期（Independent Lifecycle）**：候选具有独立于具体交易/单据的业务生命周期语义，并可被独立治理；工程 CRUD/API/permission 只能作为工程表面证据，不能单独推出业务生命周期。
- **E4 — 稳定语义一致性（Stable Semantic Consistency）**：在明确测试范围内，同一候选在不同业务上下文中的指称保持稳定且无已证实语义分裂；不得以数字 ID 相同、字段同名或传播连续自动证明同一业务对象。

## 3. Relation to Charter 001 Admission Rule

Charter 001 原有“`E1 = 是 且 E2 = 是` 才默认进入 Required Identity Object Set”的结构继续保留，但其中 E1/E2 必须采用本 Amendment §2 的定义解释。

同时：

1. E3/E4 继续作为一致性检查，不因本 Amendment 自动改变入集阈值；
2. 若 E1/E2 未同时满足但 E3/E4 显示强独立语义证据，继续触发 `IDENTITY_EXCEPTION_REVIEW`；
3. 已完成的旧版 E1–E4 记录不被自动删除或宣告无效，但在 **Candidate-level PASS 之前** 必须进行一次“定义对齐核验”，确认其测试实际覆盖本 Amendment §2 的语义；
4. 定义对齐核验若发现实质性测试目标不同，必须重新执行受影响 E-stage，而不能只修改文字；
5. 定义对齐核验未完成时，候选可以继续保持 `CANDIDATE_PASS_NOT_YET_PROVEN`，但不得写入 `CANDIDATE_PASS_SURVIVES`。

## 4. Candidate-level Gate Relationship

Candidate-level gate 采用以下结构：

```text
candidate-level PASS
  = E1 PASS ∧ E2 PASS ∧ E3 PASS ∧ E4 PASS
  ∧ E-definition alignment check completed
  ∧ candidate-level adversarial review completed
  ∧ no unresolved blocking evidence gap in the combined claim
```

其余状态保持：

```text
FAIL        = 任一 E-stage 明确 FAIL
INCONCLUSIVE = 未满足 PASS/FAIL 且至少一个 E-stage INCONCLUSIVE
BLOCKED     = 必要前置条件或证据状态明确阻断判定
NOT_READY   = 尚未满足 candidate-level PASS 前置条件
```

`NOT_READY` 不与 `FAIL` 或 `BLOCKED` 同义；应记录具体原因和 remediation class。

### 4.1 Blocking evidence gap — operational criteria

`blocking evidence gap` 指满足以下任一条件、且该条件直接承重 candidate-level combined claim 的未闭合证据缺口：

1. **Referent uncertainty**：候选在一个进入 E2/E4 正证明集的核心路径中，实际指称对象尚不能被证据包唯一或条件性明确；
2. **Contradictory referent evidence**：同一核心路径存在相互冲突的对象指称、真相源或语义定性，且尚未完成调和；
3. **Unresolved business-truth dependency**：候选级正证明依赖一个尚未完成的 Business Owner / Product Owner 业务真相裁决，而该裁决结果可能使相关 E-stage 证据成立、失效或改写；
4. **Required stage-input invalidation**：某项 E-stage PASS 的关键输入已被同层级证据部分否证，而尚未完成定向复核、重定级或重测；
5. **Scope-defining omission**：核心正证明使用了过宽量词，且已知存在合法子场景可能不满足该主张，尚未完成范围收敛；
6. **Evidence provenance failure**：某项被宣称为候选级承重证据的事实无法确定其来源、版本或证据级别，以至于无法判断是否可用于当前结论。

以下情况本身 **不自动构成** blocking evidence gap：

- 尚未测试但明确属于 later-stage 的边界问题；
- 仅影响增强性说明而不改变 combined claim 的证据；
- 未经验证的外部模型/LOCAL_ONLY 观察本身；
- 生产证据 BLOCKED（除非当前命题明确依赖生产事实）；
- Requirement-driven incomplete 本身。

任何 blocking 判定必须在 Candidate-level Disposition 中点名其**候选、E-stage、证据对象、影响方式和 remediation owner**，不得只写“evidence incomplete”。

## 5. Evidence Composition Requirement

Candidate-level 记录必须附带“证据语料 → E-stage”映射，至少区分：

- 引用/传播证据语料；
- Owner business-rule 语料；
- 本地工程观察；
- 生产证据；
- 需求驱动来源。

同一证据语料可以支持多个 E-stage，但不得因此声称这些 E-stage 彼此独立证明。Candidate-level synthesis 必须避免把“4 个阶段”表述成“4 份独立证据”。

## 6. Quantifier / Proposition Discipline

Candidate-level PASS 的允许表述应接近：

> 当前证据显示，候选对应的稳定业务引用角色不能在不重新引入等价独立对象的情况下被压缩，且该候选在已测试范围内承担该角色并显示独立生命周期及稳定语义一致性。

禁止将 Candidate-level PASS 直接表述为：

- “该候选已经是最终 Identity”；
- “该候选是唯一 Identity”；
- “4×PASS 已证明最终 H1/H2”；
- “生产系统已经验证”；
- “FM 已解决”。

## 7. Required Identity Object Set Gate

在 Required Identity Object Set 成集之前，必须完成：

1. 本 Amendment 的 E1–E4 定义对齐；
2. 所有 `PENDING_CAPABILITY_CONFIRMATION` 候选的显式处置，不得静默排除；
3. 已知替代粒度假说（例如 C-012）必须在成集说明中保留；
4. FM-001–FM-004 的边界状态必须有正式登记；
5. 如 FM 审查发现新的候选业务粒度，必须通过受治理的 candidate re-entry 机制重新进入 E0/E1–E4，而不得强制归并到既有候选。

## 8. Production and Requirement Cap

Production Evidence = `BLOCKED` 时，所有依赖生产事实的候选/Identity 结论保持 provisional，不得写成 production verified。

Requirement-driven status = `REQUIREMENT_DRIVEN_INCOMPLETE` 时：

- 不能以“当前无代码/无字段”推断“业务不需要”；
- 受缺失需求直接影响的 Identity 排除结论保持 `PENDING_REQUIREMENT_INPUT`；
- Schema / Migration / Canonical Identity Table Creation 继续禁止。

## 9. Non-Effect on Existing Candidate Results

本 Amendment 不自动把 C-001/C-002 当前状态改为 PASS、FAIL 或 INVALID。

当前候选状态由独立的 Candidate-level Adversarial Disposition 记录管理；该记录必须吸收任务一、任务二发现的候选级证据问题。

## 10. Governance Safety

本 Amendment：

- 不选择 H1 或 H2；
- 不创建 Required Identity Object Set 内容；
- 不关闭 FM-001–FM-004；
- 不创建 Canonical Identity Name；
- 不授权 Schema/Migration；
- 不把外部模型的 LOCAL_ONLY 观察自动升级为 repository fact。
