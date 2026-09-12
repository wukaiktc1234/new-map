# D-IG Business Capability Owner Review 001

- Decision: D-IG
- Status: ACTIVE_REFERENCE
- Review type: BUSINESS_OWNER_COMPLETENESS_REVIEW
- Capability Set basis:
  - `03-review/d-ig-business-capability-set-001.yaml` — V1 physical baseline
  - `03-review/d-ig-business-capability-set-review-001.md` — mandatory review dispositions
  - `03-review/d-ig-business-capability-set-v2-logical-001.md` — deterministic Logical v2 composition
- Adversarial review of Capability Set: `COMPLETED_PASSED`
- Owner completeness review: `COMPLETED`
- Requirement-driven status: `REQUIREMENT_DRIVEN_INCOMPLETE`
- Production evidence status: `BLOCKED`
- Pre-Screen authorization: `READY_FOR_NEXT_GATE_PENDING_ACTIVE_RULES`
- Identity decision: `NOT_MADE`
- Schema authorization: `NO`
- Migration authorization: `NO`

## 1. Review purpose and boundary

This review exists only to let `BUSINESS_OWNER` assess whether the business-capability boundary represented by the current evidence package has any **obvious omissions**, and to record Owner business-boundary conclusions that affect that completeness assessment.

This review is not a feature-prioritization exercise, product roadmap approval, Identity decision, schema decision, migration authorization, or implementation declaration.

The Owner review must distinguish three output classes:

1. **Completeness confirmation** — whether the currently enumerated capability boundary appears complete with respect to the review scope.
2. **Supplemental requirement input** — any additional business capability, business rule, or future requirement raised during review that is not already represented by a locatable requirement-driven source record.
3. **Boundary/semantic decision** — an Owner conclusion about system scope or business semantics that must be preserved as provenance but does not by itself create implementation evidence.

These output classes must remain separate throughout this record.

## 2. Review basis

The BUSINESS_OWNER reviews the three source records together, in this order:

1. V1 physical baseline YAML.
2. Review-001 mandatory dispositions.
3. Logical v2 deterministic composition.

The merge method is fixed by Logical v2 §4: start from untouched V1, apply Review-001 mandatory dispositions, and use Logical v2 §2 as authoritative for composition ambiguity.

The review scope is **completeness of the business-capability boundary represented by the current evidence package**, together with explicit Owner clarification of system-wide business scope and business-endpoint ownership where those clarifications materially affect that boundary.

Owner conclusions in this file do not convert unavailable requirement-driven sources into confirmed requirements and do not constitute implementation evidence.

## 3. Owner reviewer identity

```yaml
owner_review_metadata:
  owner_identifier: BUSINESS_OWNER
  review_date: 2026-09-13
  review_session_context: "Owner Review — System Boundary, Business Endpoint Attribution, and Capability Completeness"
  review_scope_confirmed: CONFIRMED
```

`owner_identifier`, `review_date`, and `review_session_context` are mandatory for substantive Owner output. `review_scope_confirmed` is explicitly recorded before completion.

## 4. Owner system-boundary and business-endpoint decisions

### 4.1 System is a multi-end business system

Owner confirms that **the system is not limited to the management端**. The system is a multi-end business system consisting of, at minimum:

- 管理端
- POS端
- 后厨端
- 员工端
- 小程序
- 供应商门户
- 必要的第三方/外部集成端

The system boundary and business-end attribution are separate decisions:

1. A business capability is first assessed for membership in the overall system.
2. A capability that belongs to the overall system is then assigned a primary business endpoint and, where applicable, entry, execution, collaboration, and management-data responsibilities.
3. A capability must not be judged outside the system merely because the management端 is not its primary execution endpoint.
4. Single-store, chain, large-chain, and custom operating modes may determine applicability, activation, permissions, approvals, and simplification, but do not by themselves determine whether a capability belongs to the system.

Core Owner principle:

> **能力归属于系统；端归属决定谁主责、谁承载、谁协同、谁承接数据；经营模式决定谁能用、什么时候用、如何限制或简化；实施状态决定当前是否交付；Owner 决策状态决定业务判断是否已经确认。**

These dimensions must not be substituted for one another.

### 4.2 Base endpoint responsibilities confirmed for review scope

| 端 | Owner-confirmed baseline responsibility |
|---|---|
| 管理端 | 配置、主数据、规则、账本、审批、报表、对账、合规、供应链管理；不承担现场履约、收银、后厨执行 |
| POS端 | 收银、点单、支付、退款、核销、积分抵扣、外卖接单/履约、封签、排队叫号、桌位预约、门店日结、托盘绑定与出餐激活 |
| 后厨端 | KDS、制作、出餐、估清、叫号、扣料执行 |
| 员工端 | 排班查看、打卡、健康证、培训、申诉、员工自助、任务 |
| 小程序 | 顾客点餐、会员、积分、优惠券、外卖下单、预约、排队、评价；当前为系统正式边界内的未来需求、可暂缓实施 |
| 供应商门户 | 属于系统边界内的供应商协作端；具体能力范围仍待进一步 Owner 定义 |
| 第三方/外部端 | 外卖平台、支付渠道、配送骑手、税务/发票验真、招聘渠道等外部服务，系统通过集成或数据承接与其协作 |

### 4.3 Owner-confirmed capability decisions

#### OR-001 自采

- System boundary: `CONFIRMED_IN_SYSTEM`
- Primary endpoint: 管理端 / 门店业务端
- Collaborating endpoints: 供应商门户、POS端、财务相关流程
- Business conclusion: 自采是正式系统业务能力；不得因大型连锁需要限制而从系统边界移除。
- Mode rule: 单店/连锁可以允许；大型连锁可以通过金额、品类、审批、统一采购等规则限制或约束；具体限制规则后续确定。

#### OR-002 供应商门户

- System boundary: `CONFIRMED_IN_SYSTEM`
- Primary endpoint: 供应商门户
- Collaborating endpoints: 管理端、财务
- Business conclusion: 供应商协作属于系统业务边界。
- Scope status: `PENDING_OWNER_SCOPE_DECISION`
- Not yet locked: 电子签约、订单查看、到货确认、对账、发票等具体能力的最终纳入范围。
- Governance note: 技术实现难度不得反向决定系统业务边界。

#### OR-003A 库存流水 / 变动历史

- System boundary: `CONFIRMED_IN_SYSTEM`
- Business conclusion: 系统正式需要可查询的库存流水 / 库存变动历史。
- Primary endpoint: 管理端库存账
- Collaborating endpoints: POS端、后厨端、门店侧及库存相关业务端
- Additional Owner topic: 大量历史库存数据后续是否按年度结转、归档以及如何保留查询能力，属于后续业务/治理规则，不改变当前“可查询库存历史”的确认。

#### OR-003B 库存数量权威账

- System boundary: `CONFIRMED_IN_SYSTEM`
- Business conclusion: **以管理端库存台账作为业务上的库存数量权威账。** POS端、后厨端及其他现场端产生业务动作或扣减请求，但不作为库存数量的最终权威台账。
- Mode baseline:
  - 单店：门店即仓。
  - 连锁：可存在门店仓，也可按组织需要设置独立仓。
  - 大型连锁：支持中央仓、调配仓、门店仓等仓库形态。

#### OR-004 盘点与库存调整

- System boundary: `CONFIRMED_IN_SYSTEM`
- Business conclusion: 盘点属于系统正式业务能力。
- Owner decision: 采用**双通道制**作为库存调整业务方向。
  - 日常业务调整：允许独立发起，例如报损、试吃、赠品、员工餐、内部消耗、录入错误等；必须原因分类、业务留痕、审批、过账、可追溯。
  - 盘点差异调整：适用于周期盘点、不明差异、大额异常等场景，形成“盘点 → 差异确认 → 调整审批 → 过账”的独立控制链。
- Mode rule: 不同经营模式可以配置不同审批层级、复核要求及流程简化程度，但不得取消调整的可追溯性。
- Further rule detail: 具体金额阈值、审批层级、角色权限和各模式细则仍待后续业务规则记录。

#### OR-005 支付 / 退款 / 充值

- System boundary: `CONFIRMED_IN_SYSTEM`
- Primary endpoint: POS端 / 小程序（按具体业务场景）
- Collaborating endpoint: 支付渠道外部端、管理端
- Business conclusion: 正式业务需要真实支付、退款、充值及对账闭环。
- Implementation status: 当前尚未完成正式真实对接，不代表系统边界不存在。
- Further rule detail: 对账周期、差异处理、退款审批层级、充值赠送等后续规则仍可另行确认。

#### OR-007A 损耗

- System boundary: `CONFIRMED_IN_SYSTEM`
- Business conclusion: 损耗属于系统正式业务问题。
- Owner decision: 采用**理论扣料与损耗分离**方向，不强制“成本口径 = 扣料口径 = 损耗口径”。
  - 标准成本：以 BOM 理论用量和标准单价形成理论成本基础。
  - 实际扣料：按销售数量 × BOM 理论用量形成销售相关库存扣减，不在每笔 POS 扣料时预先叠加无法准确确定的损耗。
  - 损耗：通过报损、内部消耗、试吃、盘点差异等独立业务事件记录。
  - 损耗率：作为经营分析指标按周期、品类、门店等维度统计。
- Further rule detail: 具体损耗率公式、哪些差异计入损耗、财务最终核算口径等仍需后续业务规则记录。

#### OR-007B 菜谱 / 配方版本

- System boundary: `CONFIRMED_IN_SYSTEM`
- Business conclusion: 正式需要历史版本 / 生效版本，以支持成本与业务追溯。

#### OR-008 积分 / 优惠券 / 促销

- System boundary: `CONFIRMED_IN_SYSTEM`
- Business conclusion: 三者均属于正式业务能力，整体业务链路需要完整存在。
- Confirmed minimum chain:
  - 积分：获取 → 账户 → 抵扣
  - 优惠券：发放 → 核销
  - 促销：规则 → 下单生效
- Further rule detail: 积分比例、有效期、抵扣上限、优惠券类型、促销类型等具体业务规则后续确定。

#### OR-009 小程序

- System boundary: `CONFIRMED_IN_SYSTEM`
- Current status: `FUTURE_REQUIREMENT / CURRENTLY_DEFERRED`
- Business conclusion: 小程序属于系统正式业务边界，当前阶段可以暂缓实施；具体功能范围在启动时进一步定义。

#### OR-010A 招聘

- System boundary: `CONFIRMED_IN_SYSTEM`
- Primary endpoints: 管理端 + 员工端 / 候选人相关入口
- Business conclusion: 招聘属于系统正式业务范围；不同经营模式可简化实施深度，但不改变系统归属。

#### OR-010B 排班

- System boundary: `CONFIRMED_IN_SYSTEM`
- Business users: 门店等一线销售运营部门
- Primary current endpoint: 管理端“门店运营”菜单下的排班模块
- Collaborating endpoints: 员工端 / 门店侧；POS端、后厨端按业务需要引用排班结果
- Business rationale: 一线销售运营部门不能按行政班作息，排班是其考勤和部门运作的重要基础。
- Management endpoint responsibility: 排班规则、排班表、编排、发布、调整、合规校验以及相关考勤数据承接与汇总。
- Business conclusion: 排班属于系统正式业务能力。不能因为当前主承载入口位于管理端“门店运营”，就将其理解为仅属于管理端行政功能；同样不能因为其服务对象是一线部门而判定其不属于系统。

#### OR-010C 员工自助

- System boundary: `CONFIRMED_IN_SYSTEM`
- Primary endpoint: 员工端
- Collaborating endpoint: 管理端
- Business conclusion: 员工自助属于系统正式业务能力；大型连锁可完整启用，单店可简化。

#### OR-011 外卖 / 配送

- System boundary: `CONFIRMED_IN_SYSTEM`
- Primary endpoint: POS端，承担外卖接单及门店履约
- Collaborating endpoints: 后厨端、小程序、第三方外卖平台、管理端
- External endpoint: 配送骑手 / 第三方配送平台
- Management responsibility: 数据承接、对账、报表、经营分析，不承担现场履约。
- Business conclusion: 外卖 / 配送属于系统正式业务能力，端归属按具体职责区分。

#### OR-012 仓库主数据

- System boundary: `CONFIRMED_IN_SYSTEM`
- Primary endpoint: 管理端
- Business conclusion: 系统正式负责仓库主数据创建、编辑、启停及属性维护。
- Mode baseline:
  - 单店：门店即仓。
  - 连锁：门店仓可按组织需要设置独立仓。
  - 大型连锁：中央仓、调配仓、门店仓等。
  - 自定义：按组织模式配置。

#### OR-013 员工申诉

- System boundary: `CONFIRMED_IN_SYSTEM`
- Current status: `FORMAL_BUSINESS_CAPABILITY`
- Primary endpoint: 员工端（申诉入口）
- Collaborating endpoint: 管理端（正式处置与协同处理）
- Business rationale: 多门店场景下，为防止管理缺位、员工申诉无门，提供正式的垂直管理救济途径。
- Business conclusion: 员工申诉不是未来需求替代项，而是系统正式业务能力。

## 5. PUC boundary decisions

The following PUC items are explicitly confirmed by the Owner as within the overall system boundary. This confirmation does not declare them implemented.

| PUC | System boundary | Primary endpoint / main responsibility | Owner note |
|---|---|---|---|
| PUC-02 召回 | `CONFIRMED_IN_SYSTEM` | 管理端 + 门店/POS | 食品安全、通知、追溯 |
| PUC-03 封签 | `CONFIRMED_IN_SYSTEM` | POS端 / 后厨端 | 门店现场能力，管理端记录 |
| PUC-04 资产 | `CONFIRMED_IN_SYSTEM` | 管理端 | 单店可简化，不改变系统归属 |
| PUC-05 招聘 | `CONFIRMED_IN_SYSTEM` | 管理端 + 员工端/候选人入口 | 与 OR-010A 一致 |
| PUC-06 健康证 | `CONFIRMED_IN_SYSTEM` | 员工端 + 管理端 | 餐饮合规相关 |
| PUC-07 门店日结 | `CONFIRMED_IN_SYSTEM` | POS端 + 管理端 | 日结、汇总、对账 |
| PUC-09 设备激活 | `CONFIRMED_IN_SYSTEM` | POS端 / 设备侧 | 管理端配置设备白名单/档案 |
| PUC-10 培训 | `CONFIRMED_IN_SYSTEM` | 员工端 + 管理端 | 单店可简化，不改变系统归属 |
| PUC-11 超龄用工 | `CONFIRMED_IN_SYSTEM` | 管理端 + 员工端 | 具体适用规则后续确定 |
| PUC-12 桌位预约 | `CONFIRMED_IN_SYSTEM` | POS端 / 小程序 | 管理端配置与数据承接 |
| PUC-13 发票 OCR/RPA | `CONFIRMED_IN_SYSTEM` | 管理端 / 供应商门户 | 财务相关能力，具体实施方式后续确定 |
| PUC-14 托盘 | `CONFIRMED_IN_SYSTEM` | POS端 | 托盘为出餐环节物理载体，POS绑定订单与托盘唯一码并执行出餐激活，后厨协同；非供应链物流托盘循环 |
| PUC-15 排班 | `CONFIRMED_IN_SYSTEM` | 管理端“门店运营”排班模块 | 与 OR-010B 一致 |
| PUC-16 排队叫号 | `CONFIRMED_IN_SYSTEM` | POS端 / 小程序 | 后厨协同，管理端承接数据 |
| PUC-17 会计期间关闭 | `CONFIRMED_IN_SYSTEM` | 管理端 | 财务关账 / 锁账 / 报表 |
| PUC-18 促销 | `CONFIRMED_IN_SYSTEM` | 管理端 | 与 OR-008 促销一致 |

## 6. Completeness result and omitted/unsupported boundary items

Owner confirms that the current review scope identifies additional business capabilities/boundaries that are not all represented as locatable requirement-driven records in the current Capability Set. These are recorded as **business-boundary omissions / supplemental Owner inputs**, not as implementation evidence.

```yaml
enumerated_capability_completeness:
  status: OBVIOUS_OMISSIONS_FOUND
  obvious_omissions:
    - review_output_type: OMISSION
      review_id: OM-001
      owner_identifier: BUSINESS_OWNER
      review_date: 2026-09-13
      review_session_context: "Owner Review — System Boundary, Business Endpoint Attribution, and Capability Completeness"
      omission_summary: "PUC-02 召回、PUC-03 封签、PUC-04 资产、PUC-06 健康证、PUC-07 门店日结、PUC-09 设备激活、PUC-10 培训、PUC-11 超龄用工、PUC-12 桌位预约、PUC-13 发票 OCR/RPA、PUC-14 托盘、PUC-16 排队叫号、PUC-17 会计期间关闭等能力经 Owner 确认为系统边界内，但当前不应被视为已由本 Owner Review 宣告实现。"
      omission_quote_or_faithful_summary: "Owner explicitly confirmed PUC-02/03/04/05/06/07/09/10/11/12/13/14/15/16/17/18 are within the overall system boundary."
      candidate_reference: NOT_APPLIED
      recording_status: OWNER_CONFIRMED_BOUNDARY_PENDING_REQUIREMENT_EVIDENCE
      capability_set_effect: NOT_APPLIED
```

Items already represented through corresponding OR decisions (for example recruitment, scheduling, and promotion) remain linked by provenance rather than duplicated as separate implementation claims.

### 6.1 Supplemental requirement / business-rule inputs

The following are Owner-confirmed supplemental business statements that are not all backed by locatable requirement-driven source records. They remain separate from implementation classification.

```yaml
supplemental_requirement_inputs:
  - review_output_type: SUPPLEMENTAL_INPUT
    review_id: SI-001
    owner_identifier: BUSINESS_OWNER
    review_date: 2026-09-13
    review_session_context: "Owner Review — System Boundary, Business Endpoint Attribution, and Capability Completeness"
    owner_statement_summary: "库存历史数据需要长期可查询；历史数据规模较大时，后续可讨论按年度结转/归档，但不得因此取消历史追溯能力。"
    owner_statement_quote_or_faithful_summary: "需要更改的地方，比如当历史数据过多的时候如何处理，能否按照按年度进行数据结转，就和财务结转类似。"
    requirement_record_reference: NOT_AVAILABLE
    recording_status: ORAL_UNLOCATED_PENDING_OWNER_RECORD
    capability_set_effect: NOT_APPLIED
    declared_status: NOT_DECLARED
    implementation_status: NOT_DECLARED
    pre_screen_effect: PENDING_REQUIREMENT_INPUT

  - review_output_type: SUPPLEMENTAL_INPUT
    review_id: SI-002
    owner_identifier: BUSINESS_OWNER
    review_date: 2026-09-13
    review_session_context: "Owner Review — System Boundary, Business Endpoint Attribution, and Capability Completeness"
    owner_statement_summary: "供应商门户属于系统边界，但电子签约等具体能力范围需要进一步讨论；技术实现难度不能反向取消系统边界。"
    owner_statement_quote_or_faithful_summary: "供应商门户可能需要充分讨论，如果实现难度大则不实现在管理端中。"
    requirement_record_reference: NOT_AVAILABLE
    recording_status: ORAL_UNLOCATED_PENDING_OWNER_RECORD
    capability_set_effect: NOT_APPLIED
    declared_status: NOT_DECLARED
    implementation_status: NOT_DECLARED
    pre_screen_effect: PENDING_REQUIREMENT_INPUT

  - review_output_type: SUPPLEMENTAL_INPUT
    review_id: SI-003
    owner_identifier: BUSINESS_OWNER
    review_date: 2026-09-13
    review_session_context: "Owner Review — System Boundary, Business Endpoint Attribution, and Capability Completeness"
    owner_statement_summary: "OR-002供应商协作、OR-004库存调整细则、OR-007A损耗细则、OR-005支付对账细则、OR-008积分/优惠券/促销细则、OR-001自采模式限制等属于后续业务规则工作，不应反向改变已确认的系统边界。"
    owner_statement_quote_or_faithful_summary: "能力归属于系统，规则决定谁能用、什么时候用、在什么组织模式下用。"
    requirement_record_reference: NOT_AVAILABLE
    recording_status: ORAL_UNLOCATED_PENDING_OWNER_RECORD
    capability_set_effect: NOT_APPLIED
    declared_status: NOT_DECLARED
    implementation_status: NOT_DECLARED
    pre_screen_effect: PENDING_REQUIREMENT_INPUT
```

### 6.2 Boundary decisions explicitly fixed against prior model inference

The following corrections are explicitly recorded so that prior model inference is not mistaken for Owner judgment:

- `OR-010B / PUC-15 排班`: **belongs to the overall system**. Current primary entry/management point is the management端 “门店运营” scheduling module; business users are front-line store operating departments. Prior inference that scheduling was outside the system is invalidated.
- `OR-004 盘点`: **belongs to the overall system**. The double-channel adjustment direction is an Owner Decision, while detailed thresholds/roles/permissions remain later business-rule work.
- `OR-007A 损耗`: **belongs to the overall system**. Theory-based stock deduction and separately recorded loss are the Owner-confirmed direction; detailed formulas/accounting treatment remain open.
- `OR-013 员工申诉`: **belongs to the overall system as a formal capability**, with 员工端 as entry and management端 as formal handling/collaboration. It is not downgraded to a future-only requirement.
- `OR-002 供应商门户`: **belongs to the overall system**, but the final capability scope is not yet locked.
- `OR-009 小程序`: **belongs to the overall system**, currently a future/deferred implementation item.
- `PUC-11 超龄用工`: **belongs to the overall system**; detailed activation/applicability rules remain future business rules.
- The system boundary is the multi-end system as a whole; a capability's current UI/menu location does not by itself redefine system boundary.

## 7. Three-layer conclusion

```yaml
owner_review_conclusion:
  enumerated_capability_boundary: OBVIOUS_OMISSIONS_RECORDED
  requirement_driven_status: REQUIREMENT_DRIVEN_INCOMPLETE
  future_or_supplemental_inputs:
    - SI-001: ORAL_UNLOCATED_PENDING_OWNER_RECORD
    - SI-002: ORAL_UNLOCATED_PENDING_OWNER_RECORD
    - SI-003: ORAL_UNLOCATED_PENDING_OWNER_RECORD
```

`requirement_driven_status` remains `REQUIREMENT_DRIVEN_INCOMPLETE`. This Owner review does not independently upgrade requirement evidence status.

## 8. Prohibitions

This file must not be used to:

- declare any business capability as implemented;
- change any `IMPLEMENTED` or `UNKNOWN` capability classification in the Capability Set;
- convert an oral/unlocated Owner statement into `DECLARED` status;
- change `REQUIREMENT_DRIVEN_INCOMPLETE` to `COMPLETE` without the established requirement records;
- authorize Schema, Migration, or Canonical Table Creation;
- make an Identity decision;
- treat Owner completeness confirmation as proof of production implementation;
- treat current UI/menu location as proof that a capability belongs only to one endpoint or only to the management端.

## 9. Review completion gate

```yaml
completion_gate:
  owner_identity_recorded: YES
  review_scope_confirmed: YES
  enumerated_capability_boundary_reviewed: YES
  every_obvious_omission_has_id: YES
  supplemental_inputs_separated_from_completeness_result: YES
  every_supplemental_input_has_owner_context: YES
  requirement_driven_status_preserved: YES
  implementation_classification_unchanged_by_this_file: YES
  pre_screen_authorization_unchanged_until_gate: YES
  overall_status: COMPLETED
```

Owner review completion does not alter the underlying evidence gaps. The next gate may proceed only under the active D-IG Pre-Screen rules, with all `PENDING_REQUIREMENT_INPUT` items preserved.

## 10. Downstream handoff

This Owner review is now complete and may be used as an input to the next governance stage.

A later Capability Set revision may reference Owner outputs, for example:

```text
OM-001 -> later Capability Set disposition
SI-001 -> later requirement record / capability disposition
```

Such a revision must separately establish the evidence and status required for the resulting capability record. The Owner review ID is provenance, not implementation evidence.

For any IMPLEMENTED capability, the promotion/evidence conditions remain those established in `d-ig-business-capability-set-charter-001.md` §4.

**Current downstream posture:** Owner completeness review is complete; Pre-Screen may now proceed under the active rules while preserving `REQUIREMENT_DRIVEN_INCOMPLETE` and `PENDING_REQUIREMENT_INPUT` where applicable. Identity, Schema, and Migration remain unauthorized.
