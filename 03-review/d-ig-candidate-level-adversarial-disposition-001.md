# D-IG — Candidate-level Adversarial Disposition 001

> **状态**: ACTIVE_REFERENCE — EXTERNAL REVIEW DISPOSITION
> **Decision ID**: D-IG
> **Scope**: 对 C-001 / C-002 外部反方复审及治理链审计进行正式处置。
> **Identity Decision**: NOT_MADE
> **Required Identity Object Set**: NOT_STARTED
> **H1/H2**: NOT_MADE
> **Schema Authorization**: NO
> **Migration Authorization**: NO
> **Production Evidence**: BLOCKED
> **Requirement-Driven Status**: REQUIREMENT_DRIVEN_INCOMPLETE

## 1. Purpose

本记录不重新执行 E1–E4，而是将外部复审输入及后续定向实勘转化为 D-IG 可追踪的候选级治理处置。

外部复审被视为 **adversarial input**，不是 Business Owner Decision，也不是 Identity Decision。

外部模型提出的本地源码/数据库事实，在进入提交证据包前保持 `LOCAL_ONLY / EXTERNAL_UNREPRODUCED`；只有已经写入正式证据记录并完成来源核对的事实，才能作为当前治理证据使用。

## 2. Overall Disposition

### 2.1 C-001 Food

`CANDIDATE_PASS_NOT_YET_PROVEN`

定向实勘已经确认：原 E2 证据中 `food_trace_code.foodId -> foods` 的字段主张不成立；实际追溯字段为 `dish_id`，承载 food code 而非已证明的 `foods.food_id`。同时，`order_items.food_id -> foods.food_id` 只能作为成功解析的单品路径，不得按全量订单项无条件表述。

因此，C-001 必须先完成 E2/E4 定向证据重定级与范围收窄，再进入 candidate-level adversarial re-review。**不得预设定向修复后的 E2/E4 会 PASS。**

### 2.2 C-002 Material

`CANDIDATE_PASS_NOT_YET_PROVEN`

定向实勘确认 `purchase_request_item.food_id` 中 46/50 可解析到 Material、4/50 仍存在异常；同时 `inventory.material_id` 与 `inventory.product_id` 存在已复现的技术双列冲突，业务真相尚未由 Business Owner / Product Owner 裁决。

因此，C-002 必须先完成 E2/E4 定向证据重定级及 inventory business-truth decision。**不得预设定向修复后的 E2/E4 会 PASS。**

### 2.3 Governance chain

`GATE_NEEDS_TARGETED_REPAIR`

治理链审计中的 E1–E4 定义漂移问题已通过 `d-ig-charter-amendment-002.md` 建立修正路径。治理形式修复不等于候选证据修复；两条 lane 必须同时完成。

## 3. C-001 Dispositions

| Finding | Classification | Disposition |
|---|---|---|
| `food` / `foods` double carrier | BLOCKING evidence gap | 已由定向实勘确认存在双载体。必须明确不同业务路径的物理所指；在调和前不得写 Candidate PASS。 |
| `product_type` / `combo_id` polymorphism | TARGETED REPAIR | 将 E4 Order/POS 正证明范围限定为已验证的单品 Food 路径；Combo/package 单独登记为 FM-002 输入，不在本记录中裁定。 |
| E1 A1 circularity | NON_FATAL | 现有 E1 反方反证未被后续实勘推翻；维持 E1 PASS，不把候选名称当作测试前提。 |
| E2 `food_trace_code.foodId` | CONTRADICTED — REMOVE / REPLACE | 原字段事实不存在；改为记录 `dish_id = food_code` 的实际结构，但该编码路径不得直接视为 `foods.food_id` 的 E4 正证明。 |
| E2 `order_items.food_id` | PARTIAL / CONDITIONAL | 仅保留成功解析的单品 Food 路径。当前本地样本 6/6 为 NULL，因此不能据此形成当前样本的正向实例证明。 |
| E2 direct-reference sufficiency | NON_FATAL / SCOPE | E2 的字面标准不要求两个独立来源；但修复后必须重新确认是否仍有足够、可复现且 referent 明确的 direct reference。不能因“理论上一条 direct reference 足够”而预设 PASS。 |
| E4 trace `dish_id = food_code` | CANDIDATE REFERENCE PATH / NOT POSITIVE EVIDENCE | 单独登记为候选引用路径；除非存在独立证据闭合 `food_code -> foods.food_id` 的稳定映射，否则不得进入 E4 正证明集。 |
| E4 quantifier / scope | TARGETED REPAIR | 重新限定 Recipe、成功解析的单品 Order/POS 等实际可验证路径；NULL、Combo/package、legacy menu 等不得被全量包进正证明。 |
| E3 owner metadata | TARGETED REPAIR | Owner decision record 当前仍需闭合正式 provenance；Candidate-level E3 claim 必须继续标明当前 provenance level。 |
| E3 code-state mapping | NON_BLOCKING / ENGINEERING GROUNDING | E3 business-semantic conclusion 与工程状态映射分离；代码状态与 Owner 状态映射留待 Requirement / Engineering evidence。 |
| FM-002 dependency | LATER-STAGE, NAMED | FM-002 保持正式登记；当前只要求 E4 scope restriction，不裁定 Combo Identity。 |

### 3.1 C-001 expected post-repair gate

定向修复完成后，C-001 **默认进入 `INCONCLUSIVE_UNTIL_REEVALUATED` 而非 PASS**。原因不是人为预判测试结果，而是当前已知正向证据在重定级后明显收缩：

- 原 `food_trace_code.foodId` 证据被移除；
- `order_items.food_id -> foods.food_id` 仅为条件路径，当前本地样本 6/6 NULL；
- `dish_id = food_code` 只能作为候选引用路径，不能直接作为 ID referent 正证明；
- E4 必须排除 polymorphic / NULL / legacy 范围。

因此，只有在重新对齐后的 E2/E4 证据仍足以支撑测试目标，并经 candidate-level adversarial re-review 后，才可能转为 PASS；也可能保持 INCONCLUSIVE 或进入 BLOCKED/FAIL，均不得预设。

## 4. C-002 Dispositions

| Finding | Classification | Disposition |
|---|---|---|
| `purchase_request_item.food_id` partial/non-matching referents | BLOCKING evidence gap | 46/50 可解析、4/50 异常；在异常全部归属被直接证据闭合前，不得维持 E2 主锚点的无条件 STRONG 表述。 |
| `inventory.material_id` vs `inventory.product_id` | BLOCKING evidence gap | Engineering 负责提供裁决输入证据；Business Owner / Product Owner 负责业务真相裁决。在证据到位和裁决完成前，E2/E4 库存段保持 conditional。 |
| E2 six-path independence wording | TARGETED REPAIR | E2 证明范围收敛为“一处可证明的 direct anchor/reference + 多阶段稳定传播”；传播不得计数为多个独立 direct origins。 |
| E4 `product_id` exclusion wording | TARGETED REPAIR | 按具体出现点重新定性 purchase_orders / inventory / inventory_transactions；不得使用笼统“product_id 实际指向 Material”的表述。 |
| E3 lifecycle independence instance | TARGETED / LATER-STAGE | 保留 Owner lifecycle business meaning，但与工程实现分离；需要额外实例化证据时进入 Requirement / production-evidence lane，不据此直接判 FAIL。 |
| FM-003 consumption / cross-context dependency | LATER-STAGE NAMED DEPENDENCY | 若 Recipe input 是不同 grain，则 C-002 E4 consumption segment 必须重新定界，相关 cross-context referent stability 主张也必须重新评价；采购/收货/库存/追溯段不因 FM-003 未决自动失败。 |
| supplier / spec grain axes | SCOPE LIMITATION | 显式登记为未测试粒度轴，不将未测试当成反证。 |

### 4.1 C-002 expected post-repair gate

定向修复完成后，C-002 **默认进入 `INCONCLUSIVE_UNTIL_REEVALUATED` 或保持 `BLOCKED`，而非 PASS**。

原因：

- 4/50 `purchase_request_item.food_id` 异常尚未全部闭合；
- inventory 的业务 referent 尚未由 Owner 裁决；
- E2 必须降低对“多条独立 direct references”的承诺；
- E4 必须重新限定各库存/采购/收货出现点的实际 referent。

只有在这些 blocking items 清零、aligned E2/E4 evidence 足够且 candidate-level adversarial re-review 无新 FAIL/INCONCLUSIVE 时，才允许考虑 PASS。不得把“修复记录完成”解释为“测试通过”。

## 5. Governance Audit Dispositions

### 5.1 Charter E-definition drift

正式采用 `d-ig-charter-amendment-002.md` 的修正路径。

Amendment 002 明确规定：

- 后续 E1–E4 使用统一定义；
- 旧测试记录不自动作废；
- E-definition alignment check 是 Candidate-level PASS 的前置条件；
- 若核验发现测试目标实质不同，必须重新执行受影响 E-stage。

因此，在当前 C-001/C-002 candidate-level review 中，定义对齐必须先完成；未完成前，两候选均不得写入 `CANDIDATE_PASS_SURVIVES`。

### 5.2 Evidence corpus independence

Candidate-level review 必须增加 evidence-corpus → E-stage 映射。

引用/传播语料可同时支持 E1/E2/E4；Owner business-rule 语料承担 E3 的业务语义输入；不能把多个 E-stage 自动描述成多个相互独立的证据来源。

### 5.3 Quantifier discipline

候选级结论不得使用“候选已经是最终 Identity”“四项 PASS 已证明 H1/H2”等强表述。

允许的主张仅覆盖：在**已测试范围**内，候选对应的稳定业务角色是否表现出不可压缩、直接引用、独立生命周期和稳定语义一致性。

### 5.4 Required Set completeness

进入 Required Identity Object Set 前，必须对所有 `PENDING_CAPABILITY_CONFIRMATION` 候选做显式处理。不得因为它们目前没有完整工程实现而静默删除。

已知 C-012 等替代粒度假说必须保留到 H1/H2 分析，不因 C-001 PASS 预先消灭。

### 5.5 Candidate re-entry

FM 审查或后续证据调查若发现新的真实候选粒度，必须进入受治理的 candidate re-entry 流程，不得强制归并到 C-001/C-002。

## 6. Evidence Status Rules

已经复制并核对进入正式证据记录的定向实勘事实，按正式证据记录中的 provenance 级别使用；尚未进入提交证据包的外部事实仍保持：

`EXTERNAL_UNREPRODUCED / LOCAL_ONLY`

任何本地观察都不能自动升级为生产事实。

## 7. Repair Evidence Inputs and Ownership

Targeted evidence repair 不默认具有同一输入来源。每项修复必须先标明证据来源、责任方和是否可在当前生产证据阻断状态下完成：

| Repair item | Required input source | Evidence provider / executor | Business decision owner | Blocking? |
|---|---|---|---|---|
| C-001 `food` / `foods` double-carrier referent reconciliation | Local source/code + existing submitted evidence; production if available | Engineering / evidence executor | BUSINESS_OWNER only if a business referent rule must be chosen | Yes |
| C-001 `product_type` / `combo_id` polymorphic scope | Local source/code + migration/schema evidence | Engineering / evidence executor | BUSINESS_OWNER if business interpretation remains unresolved | Yes for candidate PASS scope |
| C-001 E4 quantifier/scope repair | Repaired evidence and test wording | Governance executor | N/A unless business boundary remains unresolved | Yes |
| C-002 `purchase_request_item.food_id` 4/50 anomaly referents | Production DB preferred; otherwise reproducible local snapshot + migration/history evidence | Engineering / evidence executor | BUSINESS_OWNER if an ambiguous business referent remains after evidence | Yes |
| C-002 `inventory.material_id` vs `product_id` | Production DB preferred; otherwise reproducible local evidence + schema/FK/migration evidence | Engineering / evidence executor | BUSINESS_OWNER / PRODUCT OWNER | Yes |
| C-002 E2 reference-scope repair | Revised evidence table + aligned E2 method | Governance executor with Engineering evidence input | N/A | Yes |

### 7.1 Definition-alignment ordering

The execution order is mandatory:

```text
1. E-definition alignment check
2. If required, re-execute affected E-stage(s)
3. Only then perform blocking targeted evidence repair against the aligned E-stage set
4. Evidence-corpus mapping and wording repair
5. Candidate-level adversarial re-review
6. Candidate-level verdict
```

A repair performed against a pre-alignment E-stage definition does not count as a completed Candidate-level prerequisite until it is revalidated against the aligned definition.

### 7.2 `purchase_request_item.food_id` anomaly fallback

The four anomalous records must be classified without silently treating historical absence as proof of any business meaning:

**Primary path — production evidence available**

- Query the production record and relevant referent tables/history.
- Classify each anomaly as a Material referent, another business referent, or unresolved/invalid value based on direct evidence.

**Fallback path — production unavailable or record no longer exists**

Use, in order:

1. A reproducible local data snapshot with provenance and matching schema/version;
2. Migration, import, audit-log, or historical evidence that directly establishes what the stored value was intended or observed to reference;
3. Other submitted evidence that can establish the referent without relying on naming coincidence or equal numeric IDs.

The following are **not sufficient by themselves** to resolve the anomaly:

- field name `food_id`;
- equal numeric values across tables;
- undocumented assumptions about an old table's meaning;
- an external model's interpretation.

If the anomaly remains unresolved after the fallback evidence chain, classify it as `UNRESOLVED_REFERENT` and retain BLOCKING status for the affected C-002 E2 claim.

### 7.3 Scope of repair

Targeted evidence repair must not expand into redesign, refactoring, schema migration, or implementation authorization. Its purpose is limited to reproducing/falsifying evidence, identifying referents and provenance, narrowing tested scope, recording required business decisions, and re-running only affected E-stages when necessary.

## 8. Current Gate State

```text
C-001 candidate-level: CANDIDATE_PASS_NOT_YET_PROVEN
C-002 candidate-level: CANDIDATE_PASS_NOT_YET_PROVEN

Governance chain: GATE_NEEDS_TARGETED_REPAIR
Charter Amendment 002: ACTIVE_REFERENCE
FM-001..FM-004: OPEN
Required Identity Object Set: NOT_STARTED
H1/H2: NOT_MADE
Identity Decision: NOT_MADE
Schema Authorization: NO
Migration Authorization: NO
Production Evidence: BLOCKED
Requirement-Driven Status: REQUIREMENT_DRIVEN_INCOMPLETE
```

## 9. Next-Gate Conditions

### Shared precondition — before Candidate PASS

1. E-definition alignment check completed against Charter Amendment 002;
2. If definition change affects the test target, re-execute the affected E-stage;
3. Candidate-level evidence-corpus mapping completed;
4. Owner provenance for candidate-level E3 claims remains explicitly identified.

### C-001

1. Food/foods referent reconciliation;
2. Order/POS polymorphic scope repair;
3. `food_trace_code.foodId` removal/replacement with actual `dish_id` treatment;
4. `dish_id = food_code` retained outside the E4 positive set until the ID mapping is independently closed;
5. Candidate-level wording / provenance repair;
6. After repair, re-evaluate E2/E4 and enter candidate-level adversarial review. **Expected initial gate is INCONCLUSIVE_UNTIL_REEVALUATED, not PASS.**

### C-002

1. Resolve/classify the 4 anomalous purchase-request referents;
2. Supply inventory evidence package and complete Business Owner / Product Owner business-truth decision;
3. E2 six-path wording narrowed to anchor/propagation structure;
4. E4 purchase/inventory/transaction scope repair;
5. After repair, re-evaluate E2/E4 and enter candidate-level adversarial review. **Expected initial gate is INCONCLUSIVE_UNTIL_REEVALUATED or BLOCKED, not PASS.**

### Shared downstream preconditions — before Required Identity Object Set

1. Charter Amendment 002 definition alignment completed;
2. All `PENDING_CAPABILITY_CONFIRMATION` candidates explicitly handled;
3. FM-001–FM-004 boundary state formally registered;
4. known alternative-grain hypotheses retained;
5. candidate re-entry mechanism available if FM discovers a new candidate.

Completion of repair work alone never authorizes `CANDIDATE_PASS_SURVIVES`; only the subsequent adversarial re-review and verdict can do so.

## 10. Explicit Non-Decisions

本记录不：

- 判断 Food 是否最终 Identity；
- 判断 Material 是否最终 Identity；
- 判断 Food = Material 或 Food ≠ Material；
- 关闭 FM-001–FM-004；
- 决定 Required Identity Object Set 内容；
- 决定 H1/H2；
- 授权 Schema / Migration；
- 把 Production Evidence 标记为 VERIFIED；
- 代替 Business Owner / Product Owner 裁决 inventory 的业务真相。
