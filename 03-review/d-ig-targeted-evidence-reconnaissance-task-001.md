# D-IG — Targeted Evidence Reconnaissance Task 001

> **Status**: ACTIVE_REFERENCE — EXTERNAL EVIDENCE RECONNAISSANCE TASK
> **Decision ID**: D-IG
> **Scope**: C-001 Food / C-002 Material candidate-level blocking evidence repair
> **Purpose**: 在 Candidate-level re-review 前，由独立大模型/工程勘察方对当前 LOCAL_ONLY / EXTERNAL_UNREPRODUCED 攻击点进行实地取证。
> **Identity Decision**: NOT_MADE
> **Required Identity Object Set**: NOT_STARTED
> **H1/H2**: NOT_MADE
> **Schema Authorization**: NO
> **Migration Authorization**: NO
> **Production Evidence**: BLOCKED

## 1. Execution Role

执行者为 **independent evidence reconnaissance lane**，不是 Business Owner，不得自行裁定业务真相、Identity、FM、H1/H2、Schema 或 Migration。

执行者必须区分：

- `REPOSITORY_EVIDENCE`
- `LOCAL_REPRODUCED_EVIDENCE`
- `PRODUCTION_EVIDENCE`
- `UNRESOLVED`
- `INFERENCE`

不得把 LOCAL_ONLY 直接写成 repository fact。

## 2. Mandatory Ordering

必须严格遵循：

```text
1. Read D-IG Charter Amendment 002
2. Perform E-definition alignment check against existing E1–E4 records
3. If alignment reveals material test-target mismatch, STOP targeted repair for affected E-stage and report re-test requirement
4. Perform targeted evidence reconnaissance below
5. Produce evidence report with exact file/table/query/record provenance
6. Do not issue candidate PASS
```

## 3. C-001 — Food

### C001-R1 — food / foods double-carrier referent reconciliation

调查目标：确认当前工程实际是否同时写入 `food` 与 `foods`，以及这两个物理载体分别承担什么业务/技术语义。

必须检查：

1. POS 建单主事务及相关 service/repository write path；
2. `Food` / legacy `food` entity/table mapping；
3. `order_items.foodId` 的实际 FK/查询目标；
4. `food_trace_code.foodId` 的实际 referent；
5. Recipe 相关 Food referent；
6. 是否存在明确的 canonical/deprecated 转换关系、双写一致性规则或不同用途。

输出要求：

- exact source locations;
- write/read path;
- physical table/column target;
- evidence classification;
- whether the original governance statement “foods is current truth source / food deprecated” is confirmed, contradicted, or only partially supported.

**不得**自行选择哪张表应成为未来 canonical source。

### C001-R2 — order item polymorphism

调查目标：确认 `order_items.product_type`、`combo_id`、`foodId` 的实际业务语义与数据路径。

必须检查：

1. schema/migration/entity definition；
2. order creation/read/completion paths；
3. product_type 各已观察值的语义；
4. combo row 是否可能同时带 Food reference；
5. Food trace / inventory / pricing 对 single-item 与 combo 的分别处理。

输出要求：

- exact source locations;
- representative records if safely available;
- whether E4 positive evidence can be restricted to a demonstrable single-item subset;
- any unresolved combo/package referent, without deciding FM-002.

### C001-R3 — E4 quantifier repair evidence

基于 R1/R2，不修改业务结论，只提供足够证据让治理执行方把 E4 Order/POS 正证明范围收敛到准确子集。

## 4. C-002 — Material

### C002-R1 — purchase_request_item.food_id anomaly referents

调查目标：针对此前报告的 50 个样本中约 4 个未匹配记录，查明每一条到底指向：

- Material；
- Food；
- 其他业务对象；
- 悬空/非法值；
- 或无法恢复。

优先顺序：

1. production DB + relevant history;
2. reproducible local snapshot with provenance;
3. migration/import/audit/history evidence;
4. other direct submitted evidence.

不得使用以下方式作为唯一证据：字段名称、数字 ID 相等、模型推断、表名猜测。

对于每条异常记录输出：

`record identifier → stored value → candidate referent → evidence source → confidence/status`

若最终仍无法解析，必须标记 `UNRESOLVED_REFERENT`，不得为了推进流程降级阻塞状态。

### C002-R2 — inventory material_id / product_id semantic reconciliation

调查目标：确认 inventory、purchase、transaction、trace 等相关表/服务中 `material_id` 与 `product_id` 的真实语义。

必须逐出现点调查：

1. schema / FK / ORM mapping；
2. write path；
3. read/query path；
4. representative data instances；
5. purchase_orders / inventory / inventory_transactions 中各字段是否指向相同或不同业务对象；
6. 是否只是历史命名残留，还是实际承担第二业务 referent。

输出必须分为：

- technical referent;
- observed business usage;
- unresolved business-truth question.

**不要代替 Business Owner / Product Owner 做最终业务真相裁决。**

### C002-R3 — E2 proof-range repair

调查现有 E2 六路径中哪些是：

- direct-reference creation/anchor;
- propagation/inheritance;
- merely attribute/lookup.

输出准确证据映射，供治理记录把 “six independent direct references” 收敛为实际被证据支持的命题。

## 5. Optional Non-Blocking Checks

如不增加主要任务范围，可顺带核查：

- C-001 `DISCONTINUED` Owner state 与代码状态映射；
- C-002 supplier/spec grain；
- recipe-consumption semantic details relevant to FM-003。

这些不能挤占 R1/R2/C002-R1/C002-R2 的核心取证资源。

## 6. Output Contract

最终报告必须包含：

```text
A. Environment / provenance
B. E-definition alignment result
C. C-001 evidence findings
D. C-002 evidence findings
E. Per-finding status: CONFIRMED / CONTRADICTED / PARTIAL / UNRESOLVED
F. Exact remediation recommendation
G. What the evidence does NOT establish
```

不得输出：

- Identity Decision；
- Required Identity Object Set membership；
- H1/H2 decision；
- FM closure;
- schema/migration design;
- “candidate PASS” declaration。

## 7. Gate Effect

只有当：

- E-definition alignment 已完成；
- C-001/C-002 所有 blocking evidence gaps 均有可接受证据结论；
- 未出现新的 FAIL/INCONCLUSIVE；
- evidence provenance 已闭合；

才可以进入 Candidate-level adversarial re-review。
