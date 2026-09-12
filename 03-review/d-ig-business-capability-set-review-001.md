# D-IG Business Capability Set v1 实证质量审阅处置

- Review ID: D-IG-CAPSET-REVIEW-001
- Decision: D-IG
- Subject: `03-review/d-ig-business-capability-set-001.yaml`
- Reviewer: DEEPSEEK (independent adversarial review)
- Review date: 2026-09-12
- Review status: REMEDIATION_REQUIRED_NOT_OWNER_READY
- Production evidence status: BLOCKED
- Requirement-driven status: REQUIREMENT_DRIVEN_INCOMPLETE
- Pre-screen authorization: BLOCKED
- Identity decision: NOT_MADE
- H1/H2 decision: NOT_MADE
- Schema authorization: NO
- Migration authorization: NO

## 1. Review conclusion

Capability Set v1 的 metadata、UNKNOWN 纪律和大部分 IMPLEMENTED 能力的证据表达方式通过审阅；但存在明确的过度合并和能力边界遗漏，因此暂不进入 BUSINESS_OWNER 审阅。

本记录不直接替换 v1 YAML。v1 保持为原始 V1 基线，所有修订先在本记录中完成处置分类，待证据核对完成后再产生下一版 Capability Set。

## 2. 已确认必须修正

### CAP-IMPL-030 — 拆分

当前名称：`Manage employee attendance and salary records`

处置：`SPLIT_REQUIRED`

依据：现有 behavior_path 已明确存在 `AttendanceService -> attendance_record` 与 `SalaryService -> salary_records` 两条业务路径，且 attendance 与 salary 在触发条件、业务结果、责任角色和生命周期上均不同。当前 notes 又明确写明二者是 separate business actions，因此保持合并会违反 Charter §4 Step 3 的五项边界检查。

建议的新能力边界：
- `Maintain employee attendance records`
- `Process employee salary records`

### CAP-IMPL-031 — 拆分

当前名称：`Manage membership, levels and loyalty/recharge operations`

处置：`SPLIT_REQUIRED`

依据：现有证据至少区分 `MemberService -> members`、`MemberLevelService -> member_level`、`CouponService -> coupon_template`、`RechargeService -> recharge_record`。会员主体、会员等级、优惠券、充值具有不同业务动作与生命周期；积分能力是否独立于会员服务还需继续核对具体证据，不能仅凭 Capability Set 当前名称把其视为一个能力。

建议第一轮拆分为已具备明确行为路径的能力：
- `Maintain member records`
- `Manage member levels`
- `Manage member coupon operations`
- `Process member recharge operations`

积分能力：`EVIDENCE_CHECK_REQUIRED`。只有找到明确的积分业务行为路径后才单独登记为 IMPLEMENTED；否则保持在待核对状态，禁止补造。

## 3. 已有证据支持进一步拆分

### CAP-IMPL-026 — 拆分

当前名称：`Record receipts and fund flows`

处置：`SPLIT_REQUIRED`

依据：现有业务对象地图将 `Receipt` 与 `FundFlow` 明确列为两个 VERIFIED 业务对象，API 地图也分别列出收款与资金流水路由；因此当前 `Receipt/FundFlow service paths` 的斜杠写法不足以证明它们是单一业务能力。按 Charter §4 Step 3，应分别表达。

建议：
- `Record receipts`
- `Record fund flows`

## 4. UNKNOWN 边界重新定位

### UNKNOWN-008

当前名称：`Commercial packaging/specification behavior relevant to item resolution`

处置：`MOVE_OUT_OF_CAPABILITY_SET`

原因：它描述的是 E0-F-001/E0-F-002 对包装/规格/商业单位的候选假设是否有实现证据，而不是一个已经识别出的“业务能力边界”。继续放在 `unknown_boundaries` 会混淆：

- `UNKNOWN business capability boundary`
- `E0 candidate implementation status`

后续应保留为 E0/Candidate 状态，例如“商业包装/规格能力未建立实现证据”，但不得把它作为 Capability Set 的未知业务能力条目。

## 5. 四处缺口的证据级处置

### Recipe

现有 `api-map.md` 明确列出 `/v1/dish-recipes` 为“菜品配方” API；但当前审阅到的证据没有形成足够的“业务行为路径”证明其为活跃 IMPLEMENTED capability。

处置：`UNKNOWN_REQUIRES_BEHAVIOR_PATH`

不得仅凭 endpoint、table 或模块名称升级为 IMPLEMENTED。下一版应：
- 找到 Recipe/DishRecipe 具体 create/update/read/delete 或维护/生效行为路径，才升格为 IMPLEMENTED；
- 若确认只有孤立/断链代码，则登记为 UNKNOWN 或保留为 engineering residue。

### Pricing

当前 evidence package 的 Owner pricing rules 明确为 `NOT_OBTAINED_IN_REPOSITORY`；现有 capability evidence 也没有足够的价格管理业务行为路径。

处置：`UNKNOWN_REQUIRES_OWNER_AND_BEHAVIOR_EVIDENCE`

原因：Pricing 对 Commercial Unit / Variant 的 Pre-Screen 具有直接影响，但不能因“供应商报价/促销/价格字段”等弱线索自动建立 IMPLEMENTED capability。

### Inventory Count / Adjustment

当前 frontend map 明确把“库存盘点”列为仓储管理功能，但现有 Capability Set 没有对应业务行为路径。

处置：`UNKNOWN_REQUIRES_BEHAVIOR_PATH`

不能因为前端功能名出现“库存盘点”就把它直接标记 IMPLEMENTED；需要定位盘点发起、盘点结果确认、调整结果写入等具体路径。若仅证明存在 UI/module 而无业务路径，则保持 UNKNOWN。

### Inventory Transaction Query / Trace

现有 evidence 明确存在 `inventory_transactions` 数据与库存流水风险，但当前 Capability Set 没有足够证据证明存在独立的“库存流水查询/追溯”业务能力。

处置：`UNKNOWN_REQUIRES_BEHAVIOR_PATH`

需要定位查询 API / service / repository 行为链。只有形成可定位的业务行为路径后才可进入 IMPLEMENTED。

## 6. 数量影响

在当前证据假设下：

- CAP-030：1 -> 2
- CAP-031：1 -> 4 个已证实边界 + 1 个积分待核对项
- CAP-026：1 -> 2

因此，若积分最终也有明确行为路径，IMPLEMENTED 数量将从 32 增至 40；若积分不能独立证实，则当前结构性拆分后的明确 IMPLEMENTED 数量为 39。

这里不直接修改 v1 的 `implemented_capability_count`，因为 Recipe/Pricing/Inventory Count/Inventory Transaction Query 仍需证据闭环，而 CAP-031 的积分边界也需要单独核对。

## 7. Owner 审阅闸门

当前仍然：`NOT_OWNER_READY`。

原因不是要求“能力看起来完整”，而是：

1. 至少 3 条已知能力边界需要修正；
2. 4 个缺口需要完成证据分类；
3. UNKNOWN-008 需要从 Capability Set 的 UNKNOWN business-capability boundary 移位；
4. Owner 仍然尚未提供可定位的 declared records，因此 `declared_capability_count` 保持 0 正确。

## 8. 不允许的动作

- 不因为 UI、endpoint、table、Service 单独出现就建立 IMPLEMENTED capability；
- 不为了把数量补到某个目标而制造 Recipe/Pricing/Count/Transaction Query 能力；
- 不把 UNKNOWN 改成 DECLARED；
- 不把 UNKNOWN-008 当成新的业务能力；
- 不因为本轮拆分数量增加而推导任何 Identity 结论；
- 不解除 Pre-Screen、E1-E4、Schema、Migration 的 BLOCKED 状态。

## 9. 下一版生成条件

只有完成以下动作，才产生 `d-ig-business-capability-set-002.yaml`：

1. 按上述边界拆分 CAP-030 / CAP-031 / CAP-026；
2. 对 Recipe、Pricing、Inventory Count、Inventory Transaction Query 完成 behavior-path evidence check；
3. 将 UNKNOWN-008 从 Capability Set 的 unknown boundary 移回 E0 candidate status；
4. 重新计算 metadata counts；
5. 对所有新增/拆分条目检查 source_reference 可定位性；
6. 再进行一次独立 adversarial review；
7. Owner completeness review 仍需在 requirement-driven input 有记录后执行。

## 10. Governance state

```yaml
D-IG: OPEN
Capability_Set: REVISION_REQUIRED
Owner_Review: BLOCKED_PENDING_REVISION
Requirement_Driven: INCOMPLETE
Production_Evidence: BLOCKED
Pre_Screen: BLOCKED
E1_E4: BLOCKED
Schema: BLOCKED
Migration: BLOCKED
Identity_Decision: NOT_MADE
```
