# D-IG Business Capability Set v1 实证质量审阅处置

- Review ID: D-IG-CAPSET-REVIEW-001
- Decision: D-IG
- Subject: `03-review/d-ig-business-capability-set-001.yaml`
- Reviewer: DEEPSEEK (independent adversarial review)
- Review date: 2026-09-12
- Review status: REMEDIATION_COMPLETE_READY_FOR_V2
- Production evidence status: BLOCKED
- Requirement-driven status: REQUIREMENT_DRIVEN_INCOMPLETE
- Pre-screen authorization: BLOCKED
- Identity decision: NOT_MADE
- H1/H2 decision: NOT_MADE
- Schema authorization: NO
- Migration authorization: NO

## 1. Review conclusion

Capability Set v1 的 metadata、UNKNOWN 纪律和大部分 IMPLEMENTED 能力的证据表达方式通过审阅；需要按本记录完成 3 个已确认的拆分、1 个 UNKNOWN 边界移位，以及 4 个缺口的证据级分类后进入 v2。

本记录不替换 v1。v1 保持为原始 V1 基线，v2 是经本审阅处置后的新版本。

## 2. 已确认必须修正

### CAP-IMPL-030 — 拆分

当前名称：`Manage employee attendance and salary records`

处置：`SPLIT_REQUIRED`

依据：现有 behavior_path 已明确存在 `AttendanceService -> attendance_record` 与 `SalaryService -> salary_records` 两条业务路径，且 attendance 与 salary 在触发条件、业务结果、责任角色和生命周期上均不同。当前 notes 又明确写明二者是 separate business actions，因此保持合并违反 Charter §4 Step 3 的五项边界检查。

建议的新能力边界：
- `Maintain employee attendance records`
- `Process employee salary records`

### CAP-IMPL-031 — 拆分

当前名称：`Manage membership, levels and loyalty/recharge operations`

处置：`SPLIT_REQUIRED`

依据：现有证据至少区分 `MemberService -> members`、`MemberLevelService -> member_level`、`CouponService -> coupon_template`、`RechargeService -> recharge_record`。会员主体、会员等级、优惠券、充值具有不同业务动作与生命周期；积分是否有独立业务行为路径必须单独核对，不能仅凭当前名称把其视为一个能力。

v2 第一轮拆分：
- `Maintain member records`
- `Manage member levels`
- `Manage member coupon operations`
- `Process member recharge operations`

积分：`EVIDENCE_CHECK_REQUIRED`。若后续找到独立积分业务行为路径，再作为新增 IMPLEMENTED capability；否则不登记。

### CAP-IMPL-026 — 拆分

当前名称：`Record receipts and fund flows`

处置：`SPLIT_REQUIRED`

依据：业务对象地图分别列出 Receipt 与 FundFlow，API 地图也分别存在收款与资金流水路由；当前 `Receipt/FundFlow service paths` 的斜杠表达不足以证明二者为一个业务能力。

建议的新能力边界：
- `Record receipts`
- `Record fund flows`

## 3. UNKNOWN 边界重新定位

### UNKNOWN-008

处置：`MOVE_OUT_OF_CAPABILITY_SET`

原因：它描述的是 E0-F-001/E0-F-002 对商业包装/规格/商业单位候选的实现状态，而不是一个业务能力边界。继续放在 `unknown_boundaries` 会混淆“UNKNOWN business capability boundary”和“E0 candidate implementation status”。

### E0 接收方（已明确）

该状态移回并保留在 `03-review/d-ig-e0-candidate-enumeration-001.yaml` 的新增 `candidate_implementation_status` 区域中。

固定记录：

```yaml
candidate_implementation_status:
  - status_id: E0-CIS-001
    related_candidates: [C-007, C-008]
    status: NO_IMPLEMENTED_BEHAVIOR_PATH_FOUND
    evidence_check_date: 2026-09-12
    evidence_reference:
      - "01-engineering-reality/api-map.md §2"
      - "03-review/business-item-canonical-independent-review.md §1.3"
    note: "Commercial packaging/specification questions remain E0 candidate questions; no independent implemented capability lifecycle has been established."
```

该记录不是 Identity 决策，也不是 capability count。

## 4. 四处缺口的证据级处置

### Recipe

现有 `api-map.md` 明确列出 `/v1/dish-recipes` 为“菜品配方” API；但当前证据没有形成足够的“业务行为路径”证明它是活跃 IMPLEMENTED capability。

处置：`UNKNOWN_REQUIRES_BEHAVIOR_PATH`

### Pricing

Owner pricing rules 仍为 `NOT_OBTAINED_IN_REPOSITORY`，当前没有足够的价格管理业务行为路径证据。

处置：`UNKNOWN_REQUIRES_OWNER_AND_BEHAVIOR_EVIDENCE`

### Inventory Count / Adjustment

frontend map 明确列出“库存盘点”，但当前 Capability Set 没有可定位的盘点业务行为路径。

处置：`UNKNOWN_REQUIRES_BEHAVIOR_PATH`

### Inventory Transaction Query / Trace

当前证据确认存在 `inventory_transactions` 数据与库存流水问题，但没有足够证据证明存在独立的流水查询业务路径。

处置：`UNKNOWN_REQUIRES_BEHAVIOR_PATH`

以上四项在 v2 中以独立 `unknown_capability_checks` 区域保留，不计入 `unknown_boundary_count`，避免与 UNKNOWN business-capability boundary 混淆。

## 5. 数量计算（已修正）

起始 IMPLEMENTED：32

- CAP-IMPL-030：1 -> 2，净 +1
- CAP-IMPL-031：1 -> 4，净 +3（积分待定、不计）
- CAP-IMPL-026：1 -> 2，净 +1

所以结构性拆分后的明确 IMPLEMENTED 数量：

```text
32 - 3 + 2 + 4 + 2 = 37
```

若后续证明积分存在独立业务行为路径，则：`37 + 1 = 38`。

因此 v2 初始 `implemented_capability_count` 必须为 **37**，不是 39/40。

v2 的 `unknown_boundary_count` 为 **7**（原 8 条中 UNKNOWN-008 移出）。

## 6. Owner 审阅与 v2 顺序

严格顺序：

```text
v1 修订处置
  ↓
v2 生成
  ↓
独立 adversarial review（DEEPSEEK）
  ↓
若 adversarial review 通过
  ↓
BUSINESS_OWNER completeness review
  ↓
Pre-Screen authorization 条件评估
```

Owner completeness review 仍要求 requirement-driven input 有记录；在其缺失时继续保持 `REQUIREMENT_DRIVEN_INCOMPLETE`，不把“Owner 审阅”伪装成已完成的 requirement confirmation。

## 7. 不允许的动作

- 不因为 UI、endpoint、table、Service 单独出现就建立 IMPLEMENTED capability；
- 不为了把数量补到某个目标而制造 Recipe/Pricing/Count/Transaction Query 能力；
- 不把 UNKNOWN 改成 DECLARED；
- 不把 UNKNOWN-008 当成新的业务能力；
- 不因为本轮拆分数量增加而推导任何 Identity 结论；
- 不得因为拆分后条目数量变化，就重新解释任何候选的 Identity 状态；
- 不得把 CAP-031 的四个已拆分能力直接映射为 C-007 / C-008 / C-012 / C-013 等 Identity 候选结论；
- 不解除 Pre-Screen、E1-E4、Schema、Migration 的 BLOCKED 状态。

## 8. v2 生成条件

以下条件现已满足，可以生成 v2：

1. CAP-030 / CAP-031 / CAP-026 拆分边界已确定；
2. Recipe / Pricing / Inventory Count / Inventory Transaction Query 已完成证据级分类；
3. UNKNOWN-008 接收方已明确为 E0 YAML 的 `candidate_implementation_status`；
4. 数量计算已更正为 37（积分另计潜在 +1）；
5. adversarial review 与 Owner review 顺序已明确；
6. 防止“拆分数量 → Identity 结论”滑坡的约束已加入。

## 9. Governance state after v2 generation

```yaml
D-IG: OPEN
Capability_Set: V2_GENERATED_PENDING_ADVERSARIAL_REVIEW
Owner_Review: BLOCKED_PENDING_ADVERSARIAL_REVIEW
Requirement_Driven: INCOMPLETE
Production_Evidence: BLOCKED
Pre_Screen: BLOCKED
E1_E4: BLOCKED
Schema: BLOCKED
Migration: BLOCKED
Identity_Decision: NOT_MADE
```
