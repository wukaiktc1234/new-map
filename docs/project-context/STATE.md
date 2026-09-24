# Project State

> Snapshot date: **2026-09-24**
>
> Purpose: 在新会话中恢复“当前有效状态”，而不是重放历史对话。

## 1. Project Identity

- Repository: `wukaiktc1234/new-map`
- Domain: 餐饮管理系统
- Governance principle: **决策有 ID、有状态、有证据、有 Owner、有溯源**
- Production availability: **不可用**
- 任何依赖生产环境的结论必须标记为 `BLOCKED` / `PROVISIONAL_PENDING_PRODUCTION_EVIDENCE`

## 2. Working Roles

| Role | Responsibility |
|---|---|
| BUSINESS_OWNER | 业务判断、正式决策 |
| GPT | 主写手 / 主控 / 全局判断 / 治理记录 / 最终裁决 |
| OPEN | 本地执行代理，执行 GPT 下达的任务并产出报告 |
| DeepSeek (DS) | 独立 Reviewer；检查结构、证据质量、边界与遗漏 |

**关键边界：DS 不执行任务；OPEN 不替代 GPT 做最终治理裁决。**

## 3. Governance Rules That Must Persist

1. Recommendation ≠ Decision ≠ Implementation.
2. 未经 Owner 正式决策，不锁定实现方案。
3. Frozen Scope 的 substantive content 不得在实施后被悄然修改；只能追加明确的治理元数据/更正记录，且不得改变原语义。
4. 生产不可用时，不得把本地验证写成生产验证。
5. 残余风险不能用于豁免 Gate；应登记并保留。
6. 不为了修补证据而事后制造历史证据。
7. 新任务必须先防止与现有 WIP 混入；禁止 `git add .` 作为治理收口手段。

## 4. P1 Order Number — Current Decision

### Scope

- Current effective scope: `docs/architecture/03-review/p1-order-number-scope-002.md`
- Status: **FROZEN**
- Scope-001: **SUPERSEDED**，保留作为历史来源/溯源，不恢复其有效性
- No Scope-003
- Frozen scope does not include P0/P2/PD-043 or schema/Flyway changes

### Owner decision

Owner formally selected:

> **A1 — reuse the existing `/v1/orders` generated `orderCode` and assign the same value to `orderNumber`.**

A1 does not redesign:

- `generateOrderCode()`
- POS T/W generator
- database schema / Flyway
- P0 deduction
- P2-A/B/C
- PD-043 / `requiredQty <= 0`

### Implementation boundary

Implemented change:

```text
OrderNewServiceImpl.buildOrderEntity()
    setOrderCode(orderCode)
    setOrderNumber(orderCode)
```

A1 commit chain recorded by the local closeout evidence:

```text
7f40ab7  first implementation
  -> 6f0f2b9  immediate rollback
  -> 0acf99b  exact reapply
```

Independent review confirmed:

- A1 code scope: PASS
- H-01 ~ H-06: PASS
- Regression: PASS
- Local release: PASS
- Scope contamination: NONE

## 5. A1 Closeout Status

Final independent closeout result:

```text
IMPLEMENTATION_CLOSEOUT = PASS_WITH_LIMITATION
ROLLBACK_REHEARSAL = PASS_WITH_LIMITATION
A1_CODE_SCOPE = PASS
H-01..H-06 = PASS
QA_LIMITATION_IMPACT = NON_BLOCKING
REGRESSION = PASS
LOCAL_RELEASE = PASS
PRODUCTION_STATUS = PROVISIONAL_PENDING_PRODUCTION_EVIDENCE
SCOPE_CONTAMINATION = NONE
```

Disposition:

> **CLOSE_WITH_REGISTERED_LIMITATIONS**

### Important limitation: L-02

The rollback itself was independently proven by Git tree hashes and exact reapply.

However, during the literal rollback window required by Scope-002 §11, the expected HTTP-layer probe was not executed. The rollback window has passed.

**Do not recreate or backfill that probe as historical evidence.**

Correct handling:

- register the gap transparently;
- correct the Implementation Record with a factual addendum;
- next rehearsal must include windowed HTTP probe + POS regression + DB count + evidence hashes.

### Other registered limitations / observations

- **L-01**: production evidence unavailable; local closeout only.
- **L-03**: existing POS numeric-id → `food_id` mapping defect; separately tracked, not caused by A1.
- **L-04**: governance/evidence files are still largely untracked in the local workspace; this is the current Git evidence-persistence problem.
- **L-05**: existing presentation/data-shape limitations (for example OrderVO / scan-serve short-circuit); not introduced by A1.
- **OBS-02**: broader full-suite failures exist outside the frozen CC-6 scope; do not claim a blanket full-suite PASS.

## 6. Git / Evidence Closure — Current Position

The latest local read-only inventory established:

- tracked modified lines: 1204 porcelain M lines
- tracked deleted: 1
- staged: 0
- untracked: 363 porcelain lines / 829 expanded files
- ignored: 323 entries
- real-content diff files: 472 = 471 modified + 1 deleted
- 733 tracked files are EOL-only state changes under `core.autocrlf=true`

Crucial fact:

> A1 code + A1 test are already in the A1 local commit chain; current `OrderNewServiceImpl.java` worktree diff (+434/-334) is pre-existing WIP and must remain DEFER.

### Evidence closure candidate

The A1 governance chain currently identified for Git persistence is:

1. `p1-order-number-scope-001.md`
2. `p1-order-number-scope-002.md`
3. `p1-order-number-002-implementation-record-001.md`
4. `P1-ORDER-NUMBER-002-A1-qa-report.md`
5. `P1-ORDER-NUMBER-002-A1-release-gate.md`
6. `production-regression-test.md`
7. `p1-order-number-002-evidence/` — 44 files

The 44 evidence files were individually inventoried and classified as A1 implementation / rollback / H-01~H-06 / CC validation. No non-A1 evidence was found; all were text files and none exceeded 1 MB.

### Cross-batch governance files

These remain **DEFERRED** from the A1 evidence commit until their own submission boundary is decided:

- `remediation-roadmap.md`
- `production-known-limitations.md`
- `production-remediation-task-board.md`
- `product-decision-backlog.md`

Reason: they contain mixed-batch governance content and must not be bundled merely to repair A1 L-04.

## 6.5. Final Manifest Security Review

Latest independent manifest security review status:

```text
FINAL_MANIFEST_INPUT = READY
H00_LOGIN_JSON = EXCLUDE_FROM_GIT
H05_KDS_LIST = SAFE_FOR_GIT
H05_KDS_PENDING = SAFE_FOR_GIT
STATUS_SNAPSHOTS = KEEP_01_AND_05_ONLY
```

### H00 login evidence

`h00-login.json` contains live-form JWT `token` and `refreshToken` values plus user identity and authorization data. The original file must not enter Git history.

Do not retroactively redact/overwrite the original evidence file. If a sanitized derivative is ever needed, create it as a distinct derived artifact and explicitly document the derivation.

### H05 evidence

`h05-kds-list.json` and `h05-kds-pending.json` were inspected and classified as local test data. No customer PII or credentials were found in the checked fields. They remain eligible for the final A1 evidence manifest.

### Rollback status snapshots

Five status snapshots were compared. `02`, `03`, `05`, and `06` are byte-identical; `01` is the only snapshot with distinct staged-state information.

Final recommendation: retain `01-status-before-phase1-commit.txt` and `05-status-after-rollback-and-wip-restore.txt`; exclude `02`, `03`, and `06` from the A1 Git evidence commit.

### Current A1 manifest size

The current candidate is **47 files**:

- 6 Core-6 governance files
- 41 evidence files after excluding `h00-login.json` and three redundant status snapshots

Cross-batch governance files remain DEFERRED.

## 7. Current Next Action

The immediate objective is **Git evidence persistence**, not new feature development.

Sequence:

```text
1. Final manifest review
2. L-02 factual addendum to Implementation Record
3. Exact-path git add of A1 governance chain only
4. Review staged diff
5. Commit the A1 governance/evidence chain
6. Keep cross-batch governance files DEFERRED
7. Push only with explicit Owner authorization
```

### Explicitly NOT the next task

Do not immediately start:

```text
P1-POS-FOODID-MAP-001
```

It is an existing defect/task and is separate from A1 closure.

## 8. New-Session Safety Checks

Before starting work in a new session:

- Re-read this file.
- Re-check whether the local/remote Git state has advanced beyond this snapshot.
- Treat local-only evidence as local-only until verified in Git.
- Do not assume production is available.
- Do not reopen A1 candidate selection; A1 is already an Owner decision.
- Do not reopen Frozen Scope-002 unless new evidence demonstrates a governance inconsistency.
- Do not use this file to override the actual repository artifacts; this file is a context index, not a substitute for primary evidence.

## 9. Primary References

These are the primary project artifacts referred to by this state snapshot:

- `docs/architecture/03-review/p1-order-number-scope-001.md`
- `docs/architecture/03-review/p1-order-number-scope-002.md`
- `docs/architecture/03-review/p1-order-number-002-implementation-record-001.md`
- `docs/architecture/03-review/p1-order-number-002-evidence/`
- `docs/quality/P1-ORDER-NUMBER-002-A1-qa-report.md`
- `docs/quality/P1-ORDER-NUMBER-002-A1-release-gate.md`
- `production-regression-test.md`

### Important provenance note

This snapshot describes the current working/local governance state established in the 2026-09-23/24 work session. Some referenced governance/evidence artifacts were confirmed by the local workspace but had not yet been confirmed as tracked on GitHub during this snapshot.

Do not interpret “document exists locally” as “document is already in the remote repository”.

## 10. Update Policy

Update this file only when one of these changes:

- Owner makes a new formal decision;
- a Scope changes or becomes Frozen/Superseded;
- an implementation round changes state;
- a major review result changes state;
- a blocker/limitation is opened, closed, or materially reclassified;
- the immediate next action changes.

Do not append conversation transcripts. Prefer replacing stale state with the new authoritative state while preserving historical provenance in the project's actual governance artifacts.