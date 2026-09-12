# D-IG — Targeted Repair Execution Plan 001

> **状态**: ACTIVE_REFERENCE — EXECUTION PLAN
> **Decision ID**: D-IG
> **Scope**: C-001 Food / C-002 Material 的 E2/E4 定向证据修复、Owner 业务真相输入及 candidate-level re-review 前置执行。
> **Identity Decision**: NOT_MADE
> **Required Identity Object Set**: NOT_STARTED
> **H1/H2**: NOT_MADE
> **Schema Authorization**: NO
> **Migration Authorization**: NO
> **Production Evidence**: BLOCKED

## 1. Purpose

本计划把 `d-ig-candidate-level-adversarial-disposition-001.md`、`d-ig-targeted-evidence-reconnaissance-report-001.md`、`d-ig-e1-e4-targeted-reclassification-001.md` 中的修复项转化为可执行、可验收的工作单元。

本计划不重新设计业务模型，不创建 Identity，不推进 Required Identity Object Set，不授权 Schema / Migration。

## 2. Governing State Rule

`INCONCLUSIVE_UNTIL_REEEVALUATED`（规范写法：`INCONCLUSIVE_UNTIL_REEVALUATED`）不是独立最终状态，而是 `INCONCLUSIVE + REEVALUATION_REQUIRED` 的过渡标签。

```text
INCONCLUSIVE_UNTIL_REEVALUATED
        ↓ reevaluation completed
PASS / FAIL / BLOCKED / INCONCLUSIVE
```

因此，任何定向修复完成都不能直接写成 PASS；必须经过 aligned E2/E4 re-evaluation，再进入 candidate-level adversarial re-review。

## 3. Preconditions

### 3.1 E-definition alignment

依据 `03-review/d-ig-charter-amendment-002.md`：

- E1 = 不可替代表达；
- E2 = 独立直接引用；
- E3 = 独立生命周期；
- E4 = 稳定语义一致性。

本轮目标是确认受影响的 E2/E4 记录与上述定义一致。若发现测试目标实质改变，先重跑受影响 E-stage；若仅证据事实被纠正，则进行 targeted evidence repair + reclassification。

### 3.2 Current candidate state

```text
C-001 = CANDIDATE_PASS_NOT_YET_PROVEN
C-002 = CANDIDATE_PASS_NOT_YET_PROVEN
```

## 4. C-001 Execution Track

| Repair item | Responsible | Evidence/input | Output / record | Acceptance criteria | Blocking before re-review |
|---|---|---|---|---|---|
| E2 `food_trace_code.foodId` correction | Engineering evidence executor + Governance | Targeted reconnaissance report | `d-ig-e1-e4-targeted-reclassification-001.md` | 原 `foodTrace.foodId -> foods.food_id` 命题删除/标记 CONTRADICTED；实际 `dish_id=food_code` 单独登记 | Yes |
| E2 `order_items.food_id` scope correction | Engineering evidence executor + Governance | Local DB/code evidence + reclassification | Same reclassification record and candidate-level disposition | 明确为条件性 single-item resolved path；不得把 NULL sample 当正向实例；不得写全量订单项 direct reference | Yes |
| E4 positive-set narrowing | Governance executor | Order/POS polymorphism evidence; FM-002 boundary | Candidate-level disposition + E2/E4 evidence repair record | 正证明仅覆盖实际验证的 Food single-item paths；NULL、Combo/package、legacy carrier 显式排除 | Yes |
| `dish_id = food_code` positioning | Governance executor | Targeted reconnaissance report | E2/E4 repaired evidence table | 标记 `CANDIDATE REFERENCE PATH / NOT POSITIVE EVIDENCE`；除非独立闭合 `food_code -> foods.food_id` 映射，不得进入 E4 正证明集 | Yes |
| Post-repair E2/E4 result | Governance executor / independent reviewer | Repaired evidence set | Re-evaluation result | 结果只能为 PASS / FAIL / BLOCKED / INCONCLUSIVE；不得自动继承旧 PASS | Yes |

### 4.1 C-001 expected-state rule

当前无需预测最终测试结果。鉴于关键 E2 证据被移除、order_items 本地样本 6/6 NULL、trace path 仅编码引用，定向修复完成后首先标记：

`INCONCLUSIVE_UNTIL_REEVALUATED`

随后由新的 E2/E4 结果决定最终标准状态。

## 5. C-002 Execution Track

### 5.1 `purchase_request_item.food_id` anomaly track

| Problem | Responsible | Input | Output | E2 blocking? |
|---|---|---|---|---|
| 4/50 anomaly referent investigation | Engineering / evidence executor | Production DB preferred；不可得则 local snapshot + matching schema/version + migration/import/audit/history | 异常分类调查记录，每条记录有 provenance | Yes |
| 46/50 positive anchor characterization | Engineering + Governance | Reproduced query/result | E2 evidence row rewritten as bounded claim | Yes |
| Unresolved anomaly handling | Governance | Investigation result | `UNRESOLVED_REFERENT` + retained BLOCKING | Yes |

The four anomalies and inventory Owner Decision are **parallelizable tracks** because they answer different questions：

```text
Track A: purchase_request_item 4/50 anomaly referent
Track B: inventory business truth Q1/Q2/Q3
             ↓
       both inputs available
             ↓
     C-002 aligned E2/E4 re-evaluation
```

No track is allowed to silently substitute assumptions for the other.

### 5.2 Anomaly fallback and terminal rule

Primary evidence order：

1. Production record + relevant referent/history;
2. Reproducible local snapshot with provenance and matching schema/version;
3. Migration / import / audit-log / historical evidence;
4. Other submitted direct evidence.

Insufficient evidence包括：字段名、数字相等、未记录假设、外部模型解释。

If still unresolved：

```text
UNRESOLVED_REFERENT
→ affected C-002 E2 claim remains BLOCKING
→ no unconditional Material referent claim
→ no Candidate PASS
```

The unresolved anomaly does **not automatically mean E2 FAIL**; it means the affected unconditional claim cannot be accepted, and candidate-level passage remains blocked until the evidence gap is cleared or the test scope is formally redefined.

## 6. C-002 Inventory Owner Decision Track

### Evidence readiness

Owner Decision Request 001 now includes a formal evidence pack consisting of：

- targeted reconnaissance report 001；
- targeted E1-E4 reclassification 001；
- candidate-level adversarial disposition 001。

The pack covers inventory schema/columns, FK differences, representative local conflict instance, and the candidate-level effect. Production Evidence remains BLOCKED.

### Responsibility

```text
Engineering / evidence executor
    ↓
provide/verify evidence package
    ↓
Business Owner / Product Owner
    ↓
Q1 → Q2/Q3 as applicable
    ↓
Governance records decision
    ↓
C-002 E2/E4 re-evaluation
```

Owner decision questions are defined in：

`03-review/d-ig-c002-inventory-business-truth-owner-decision-request-001.md`

Q1 is mandatory. Q2 is mandatory if Q1=C. Q3 is mandatory if Q1=A or B.

Owner completion does not itself make E2/E4 PASS.

## 7. Dependencies and Ordering

The following ordering is mandatory：

```text
1. E-definition alignment check
2. If required, re-execute affected E-stage(s)
3. C-001 targeted repair and C-002 Track A/B evidence work may proceed in parallel
4. Complete aligned E2/E4 re-evaluation inputs
5. Candidate-level evidence-corpus mapping / wording repair
6. Candidate-level adversarial re-review
7. Candidate-level verdict
```

C-002 Track A and Track B are therefore parallel work tracks, but their completion is a joint precondition for the C-002 candidate-level re-review where both remain candidate-level blocking items.

## 8. Re-review Admission Criteria

Candidate-level adversarial re-review may start only when：

- E-definition alignment is completed;
- affected E-stage has been re-executed if alignment showed a material target change;
- C-001 E2/E4 repair record is complete;
- C-002 4/50 anomaly track is resolved or its unresolved terminal state is formally recorded and its affected scope is explicitly removed from any PASS claim;
- C-002 inventory Owner Decision is recorded as A/B/C(+Q2)/D as applicable, or the blocking dependency is explicitly retained;
- evidence-corpus → E-stage mapping is updated;
- no stale, contradicted direct-reference row remains in the active candidate-level evidence set.

## 9. Explicit Non-Goals

本计划不：

- 决定 Food / Material 是否最终为 Identity；
- 创建 Required Identity Object Set；
- 决定 H1/H2；
- 设计/修改 Schema；
- 执行 Migration；
- 声称 Production Evidence 已 VERIFIED；
- 把定向修复完成等同于 Candidate PASS。
