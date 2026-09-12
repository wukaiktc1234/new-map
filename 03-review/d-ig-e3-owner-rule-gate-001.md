# D-IG — E3 Owner Business Rule Gate 001

> **状态**: ACTIVE_REFERENCE — GOVERNANCE ADDENDUM
> **Decision ID**: D-IG
> **适用候选**: C-001 Food / C-002 Material
> **Identity Decision**: NOT_MADE
> **H1/H2**: NOT_MADE
> **Schema Authorization**: NO
> **Migration Authorization**: NO
> **Production Evidence**: BLOCKED

## 1. Purpose

本增补记录用于明确 E3 `INCONCLUSIVE_PENDING_OWNER_BUSINESS_RULE` 的后续闸门。

它不替代 `03-review/d-ig-e1-e4-testing-001.md` 的执行记录，也不把 Owner 回答预设为 PASS。

## 2. Core distinction

E3 当前的剩余缺口不是单纯的代码搜索缺口，而是业务生命周期语义尚未由 Business Owner 定义。

因此：

`Owner 回答 ≠ E3 PASS`

`Owner 回答 = E3 获得业务判定所需输入`

只有在 Owner 规则明确后，才重新评估 E3；若规则仍为“暂不决定”，E3 继续保持 `INCONCLUSIVE`。

## 3. Required Owner business-rule questions

### 3.1 C-001 Food

#### E3A — INACTIVE 的业务含义

- A：下架：不再在菜单展示，但历史订单可正常引用
- B：停用：不可再被任何新业务引用，历史订单只读展示
- C：技术开关：仅影响 API 可用性，不影响业务语义
- D：暂不决定

#### E3B — DISCONTINUED 的业务含义

- A：停售：不再销售，但保留历史引用和追溯
- B：删除：逻辑删除，历史订单仍可查，但不再作为可用对象
- C：归档：移出活跃数据，仅用于历史查询
- D：暂不决定

#### E3C — 历史订单的引用规则

- A：历史订单保留 `foodId` 引用，Food 状态不影响历史解释
- B：历史订单保留 `foodName` 快照，Food 状态变化不影响历史解释
- C：历史订单同时保留 `foodId + foodName` 快照，双重保障
- D：暂不决定

#### E3D — 状态变更是否触发业务事件

- A：是，状态变更触发下游行为并需要记录业务事件
- B：否，状态变更仅是对象属性，不触发下游业务事件
- C：暂不决定

### 3.2 C-002 Material

#### E3M-A — INACTIVE 的业务含义

- A：停用：不可用于新的采购/库存/消耗业务，但保留历史引用
- B：下架/归档：不再作为活跃物料使用，但历史记录继续可查
- C：技术开关：仅影响 API/界面可用性，不改变业务语义
- D：暂不决定

#### E3M-B — 历史采购 / 库存引用规则

- A：历史采购、库存、追溯、消耗记录保留 `materialId` 引用，Material 状态变化不影响历史解释
- B：历史记录主要依赖名称/快照，不要求 Material 当前状态维持可引用性
- C：历史记录同时保留 `materialId` + 名称快照
- D：暂不决定

#### E3M-C — 状态变更是否触发业务事件

- A：是，状态变更触发下游行为并需要记录业务事件
- B：否，状态变更仅是对象属性，不触发下游业务事件
- C：暂不决定

## 4. Explicit non-default relationship rule

Food 与 Material 的 E3 生命周期模型：

- 可以由 Owner **显式确认共享**；
- 也可以由 Owner **显式确认分离**；
- 不得默认共享；
- 不得默认分离。

两者即使选择相同答案，也只代表 Owner 对两个候选分别作出了相同的业务规则判断，不自动证明 Food 与 Material 是同一业务对象或同一 Identity。

两者选择不同答案，也不自动证明它们必然是不同 Identity；Identity 仍需经过完整的 E1–E4、candidate-level conclusion、Required Identity Object Set、FM boundary 与后续 H1/H2 闸门。

## 5. Candidate-level conclusion gate

Candidate-level conclusion 不采用测试结果投票，而采用闸门逻辑：

```text
candidate-level conclusion = PASS
    iff E1 = PASS
    and E2 = PASS
    and E3 = PASS
    and E4 = PASS

candidate-level conclusion = FAIL
    iff 任一 E-stage = FAIL

candidate-level conclusion = INCONCLUSIVE
    iff 不满足 PASS / FAIL 条件，且至少存在一个 E-stage = INCONCLUSIVE

candidate-level conclusion = BLOCKED
    iff 所需前置条件或证据状态明确阻断判定
```

因此：

- Owner 回答 7 个问题后，不保证 E3 PASS；
- 任一问题选择“暂不决定”，只要该问题仍是 E3 判定必要条件，E3 继续为 `INCONCLUSIVE`；
- 任一 E-stage FAIL 时，candidate-level conclusion 直接为 `FAIL`；
- 全部 E-stage PASS 后，candidate 才有资格进入 Required Identity Object Set；
- candidate-level conclusion 本身仍不等于最终 Identity Decision。

## 6. Interaction with FM-001–FM-004

FM-001～FM-004 当前继续 `OPEN`。

在 C-001 / C-002 完成 candidate-level conclusion 之前，不使用未解决的 E3 生命周期语义去预判 Food/Material 边界。

当前顺序保持：

`Owner E3 rules → C-001/C-002 candidate-level conclusion → Required Identity Object Set → FM-001–FM-004 boundary review → H1/H2 → Identity Decision`

## 7. Governance state

- C-001 E3: `INCONCLUSIVE_PENDING_OWNER_BUSINESS_RULE`
- C-002 E3: `INCONCLUSIVE_PENDING_OWNER_BUSINESS_RULE`
- C-001 candidate-level conclusion: `NOT_READY`
- C-002 candidate-level conclusion: `NOT_READY`
- Required Identity Object Set: `NOT_STARTED`
- FM-001–FM-004: `OPEN`
- H1/H2: `NOT_MADE`
- Identity Decision: `NOT_MADE`
