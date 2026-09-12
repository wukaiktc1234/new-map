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

本记录不重新执行 E1–E4，而是将三份外部复审输入转化为 D-IG 可追踪的治理处置：

1. C-001 Food candidate-level adversarial review；
2. C-002 Material candidate-level adversarial review；
3. D-IG governance-chain audit。

外部复审被视为 **adversarial input**，不是 Business Owner Decision，也不是 Identity Decision。

外部模型提出的本地源码/数据库事实，在进入提交证据包前保持 `LOCAL_ONLY / EXTERNAL_UNREPRODUCED`，不得直接升级为 repository fact。

## 2. Overall Disposition

### 2.1 C-001 Food

`CANDIDATE_PASS_NOT_YET_PROVEN`

保留四项既有 E-stage PASS 作为测试输入，但不写入 Candidate-level PASS，原因是外部复审提出了尚未被提交证据包调和的 candidate-level referent / scope 问题：

- `food` 与 `foods` 双物理载体的所指关系；
- `order_items.product_type / combo_id` 多态对 E4 正证明范围的影响。

这些攻击尚未证明 Food 候选无效，但足以要求在 candidate-level PASS 前完成定向证据调和。

Owner E3 输入本身另有正式元数据缺口，见 §4。

### 2.2 C-002 Material

`CANDIDATE_PASS_NOT_YET_PROVEN`

保留四项既有 E-stage PASS 作为测试输入，但不写入 Candidate-level PASS，原因是外部复审提出两项直接影响 referent 的证据承重墙：

- `purchase_request_item.food_id` 中部分值与 Material referent 的关系尚未闭合；
- `inventory.material_id` 与 `inventory.product_id` 的业务真相尚未完成裁决。

这两项首先是 candidate-level evidence integrity 问题，不得以“属于 FM”简单后置。

### 2.3 Governance chain

`GATE_NEEDS_TARGETED_REPAIR`

治理链审计中，最重要的正式问题为 Charter E1–E4 定义与执行记录漂移；该问题已通过 `d-ig-charter-amendment-002.md` 建立修正路径。

治理形式修复不等于候选证据修复；两条 lane 必须同时完成。

## 3. C-001 Dispositions

| Finding | Classification | Disposition |
|---|---|---|
| `food` / `foods` double carrier | BLOCKING evidence gap | 需将双写事实复制入提交证据文档或证伪，并逐一明确 `order_items.foodId`、Recipe、traceability 等引用的物理所指；在调和前不得写 Candidate PASS。 |
| `product_type` / `combo_id` polymorphism | TARGETED REPAIR | 将 E4 Order/POS 正证明范围限定为已验证的单品 Food 行；Combo/package 作为 FM-002 输入，不在本记录中裁定。 |
| E1 A1 circularity | NON_FATAL | 外部复审未提出比现有 E1 adversarial counter-refutation 更强的循环反例；维持 E1 PASS，保持不把候选名称当作测试前提。 |
| E2 propagation / provenance | NON_FATAL / SCOPE | E2 不要求两个独立来源；一条强 direct reference 足以满足字面标准。traceability path 改列为 corroborative/provenance evidence，不再作为“必须独立起源”的条件。 |
| E3 owner metadata | TARGETED REPAIR | Owner decision record 当前仍标记 `PENDING_FORMAL_IDENTIFIER` / `PENDING_FORMAL_RECORD`；在 Identity Decision 前必须闭合正式 provenance。 |
| E3 code-state mapping | NON_BLOCKING / ENGINEERING GROUNDING | E3 PASS 明确为 business-semantic conclusion，不升级为实现映射结论；代码状态与 Owner 状态映射留待 Requirement/Engineering evidence。 |
| FM-002 dependency | LATER-STAGE, NAMED | FM-002 已在正式边界登记簿中登记；当前只要求 E4 scope restriction，不裁定 Combo Identity。 |

## 4. C-002 Dispositions

| Finding | Classification | Disposition |
|---|---|---|
| `purchase_request_item.food_id` partial/non-matching referents | BLOCKING evidence gap | 查明所有异常样本实际指称；在该结果进入包内前，不得维持 E2 行1 的无条件 STRONG 表述。 |
| `inventory.material_id` vs `inventory.product_id` | BLOCKING evidence gap | 由相应 Product/Business Owner 完成业务真相裁决；反方不代裁。裁决完成前，E2/E4 库存段至少标记为 conditional。 |
| E2 six-path independence wording | TARGETED REPAIR | 将 E2 证明范围收敛为“一处直接引用创建/锚定 + 多阶段稳定传播”；删除把传播误称为多处独立 direct-reference 的表述。 |
| E4 `product_id` exclusion wording | TARGETED REPAIR | 按具体出现点重新定性 purchase_orders / inventory / inventory_transactions，不使用“product_id 实际指向 Material”这一笼统表述。 |
| E3 lifecycle independence instance | TARGETED / LATER-STAGE | 保留 Owner lifecycle business meaning，但将“独立生命周期”与工程实现分离；需要额外实例化证据时，在 Requirement / production-evidence lane 补充，不据此直接判 C-002 FAIL。 |
| FM-003 consumption semantics | LATER-STAGE NAMED DEPENDENCY | 正式登记 FM-003 ↔ C-002 E4 consumption-segment dependency；若 FM-003 改变 referent，再局部重跑 E4 consumption segment。 |
| supplier / spec grain axes | SCOPE LIMITATION | 显式登记为未测试粒度轴，不将未测试当成反证。 |

## 5. Governance Audit Dispositions

### 5.1 Charter E-definition drift

正式采用 `d-ig-charter-amendment-002.md` 的修正路径。

Amendment 002 明确规定：

- 后续 E1–E4 使用统一定义；
- 旧测试记录不自动作废；
- Required Identity Object Set 成集前进行定义对齐核验；
- 若核验发现测试目标实质不同，必须重新执行受影响 E-stage。

### 5.2 Evidence corpus independence

Candidate-level review 必须增加 evidence-corpus → E-stage 映射。

本记录确认：

- 引用/传播语料可同时支持 E1/E2/E4；
- Owner business-rule 语料承担 E3 的业务语义输入；
- 不能把多个 E-stage 自动描述成多个相互独立的证据来源。

### 5.3 Quantifier discipline

候选级结论不得使用“候选已经是最终 Identity”“四项 PASS 已证明 H1/H2”等强表述。

允许的主张仅覆盖：在**已测试范围**内，候选对应的稳定业务角色是否表现出不可压缩、直接引用、独立生命周期和稳定语义一致性。

### 5.4 Required Set completeness

进入 Required Identity Object Set 前，必须对所有 `PENDING_CAPABILITY_CONFIRMATION` 候选做显式处理。不得因为它们目前没有完整工程实现而静默删除。

已知 C-012 等替代粒度假说必须保留到 H1/H2 分析，不因 C-001 PASS 预先消灭。

### 5.5 Candidate re-entry

FM 审查或后续证据调查若发现新的真实候选粒度，必须进入受治理的 candidate re-entry 流程，不得强制归并到 C-001/C-002。

## 6. Evidence Status Rules

外部复审中出现的下列事实，在尚未复制进正式 evidence document 前统一为：

`EXTERNAL_UNREPRODUCED / LOCAL_ONLY`

包括但不限于：

- `food` / `foods` 双写代码位置；
- `product_type=2` / `combo_id` 具体代码行；
- `purchase_request_item.food_id` 46/50 样本结果；
- `inventory_id=4` 双身份列的具体实例；
- 代码中状态值 `1/0/2` 或 `DISCONTINUED` 命中/未命中；
- 任何未提交数据库查询结果。

它们可以作为 remediation trigger，但不作为已验证 repository evidence。

## 7. Current Gate State

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

## 8. Next-Gate Conditions

Candidate-level PASS 不得在以下 blocking items 未闭合前写入：

### C-001

1. Food/foods referent 调和；
2. Order/POS polymorphic scope 修正；
3. Candidate-level wording / provenance repair。

### C-002

1. purchase request 异常 referent 查明；
2. inventory business truth 对 `material_id` / `product_id` 完成相应 Owner/Product Decision；
3. E2/E4 wording and scope repair。

### Shared governance

1. Charter Amendment 002 的定义对齐核验；
2. Candidate-level evidence corpus mapping；
3. Owner formal provenance 在 Identity Decision 前闭合。

完成上述条件后，重新执行 C-001/C-002 candidate-level adversarial review；只有在 blocking evidence gap 清零、且无新的 FAIL/INCONCLUSIVE 结果时，才允许考虑 `CANDIDATE_PASS_SURVIVES`。

## 9. Explicit Non-Decisions

本记录不：

- 判断 Food 是否最终 Identity；
- 判断 Material 是否最终 Identity；
- 判断 Food = Material 或 Food ≠ Material；
- 关闭 FM-001–FM-004；
- 决定 Required Identity Object Set 内容；
- 决定 H1/H2；
- 授权 Schema / Migration；
- 把 Production Evidence 标记为 VERIFIED。
