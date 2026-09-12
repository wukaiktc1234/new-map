# D-IG — C-002 Inventory Business-Truth Owner Decision Request 001

> **状态**: ACTIVE_REFERENCE — OWNER DECISION REQUEST
> **Decision ID**: D-IG
> **Candidate**: C-002 Material
> **Scope**: 仅裁决 `inventory` 在业务语义上的对象归属与双列关系；不裁决 Identity、H1/H2、Schema、Migration。
> **Current Candidate State**: `CANDIDATE_PASS_NOT_YET_PROVEN`
> **Production Evidence**: BLOCKED

## 1. Purpose

本记录将定向实勘发现的 `inventory.material_id` / `inventory.product_id` 双列问题转化为可直接裁决的 Business Owner / Product Owner 输入。

本记录不根据字段名、外键数量、数值相等或工程实现自行推导业务真相。

## 2. Evidence Pack for Owner Review

Owner 裁决输入已经由以下正式证据记录组成：

1. `03-review/d-ig-targeted-evidence-reconnaissance-report-001.md`
   - `inventory` 双列存在性；
   - `material_id` / `product_id` 的数据库约束差异；
   - 代表性本地双身份实例；
   - `purchase_request_item` 46/50 与 4/50 异常概况。
2. `03-review/d-ig-e1-e4-targeted-reclassification-001.md`
   - C-002 E2/E4 定向重定级；
   - inventory 冲突为何属于 candidate-level blocking business-truth dependency。
3. `03-review/d-ig-candidate-level-adversarial-disposition-001.md`
   - C-002 blocking condition；
   - Engineering 与 Business Owner / Product Owner 的责任边界；
   - Candidate-level re-review 前置条件。

因此，本 Decision Request **已有可供 Owner 裁决的正式 evidence pack**。其中涉及的数据库/代码观察保持其正式记录所标注的 local/documentary provenance；Production Evidence 仍为 `BLOCKED`，不得将本地实例当作生产事实。

## 3. Evidence Already Established

当前已提交定向实勘记录确认：

- `inventory` 同时存在 `material_id` 与 `product_id`；
- `product_id` 存在数据库外键指向 `product(product_id)`；
- `material_id` 当前无对应数据库外键；
- ORM 层存在将 Product 访问器映射到 `material_id` 的历史/兼容性实现；
- 至少存在一个本地实例，其中同一库存记录的 `material_id=1` 与 `product_id=1` 分别对应不同业务对象；
- 定向重定级已将该冲突分类为 C-002 E2/E4 的 blocking business-truth dependency，而不是自动推导 Material 或 Product 为业务真相。

以上是技术/本地证据事实，不等于业务真相裁决。

## 4. Owner Decision Questions

### Q1 — `inventory` 的业务对象范围是什么？

请选择一个正式业务语义：

**A. 仅物料库存**

`inventory` 的业务记录本质上都是 Material Inventory。`material_id` 是库存对象的业务主引用；`product_id` 不承担独立库存业务语义，应视为兼容字段、历史字段、冗余字段或待清理字段。

**B. 仅商品库存**

`inventory` 的业务记录本质上都是 Product Inventory。`product_id` 是库存对象的业务主引用；`material_id` 不承担独立库存业务语义，应视为兼容字段、历史字段、冗余字段或待清理字段。

**C. 两者都合法，按明确类型规则区分**

`inventory` 可以承载两类不同业务对象；Material 与 Product 都是合法库存对象。但本选项只有在同时给出 Q2 的稳定、可验证的类型区分规则时才视为完成裁决。

**D. 当前证据不足，暂不裁决业务真相**

保留 `inventory` business referent unresolved；允许继续证据收集，但 C-002 的 inventory 证据继续保持 `CONDITIONAL / BLOCKING`，不得作为 Candidate PASS 的无条件 E2/E4 正证明。

### Q2 — 仅当 Q1=C 时：如何区分 Material Inventory 与 Product Inventory？

请明确以下哪一种规则成立，或提供业务上等价的正式规则：

- **C1：按一项显式类型字段区分** — 请给出字段名及取值语义；
- **C2：按不同业务表/仓型区分** — 请说明区分条件及适用范围；
- **C3：按业务流程上下文区分** — 请定义可重复执行的判定规则，并说明 purchase / receipt / stock / sale / consumption 等流程如何识别对象类型；
- **C4：其他稳定规则** — 请直接描述规则。

若无法给出稳定可执行的区分规则，则不能仅以“两者都合法”作为完成裁决的充分条件。

### Q3 — 若 Q1=A 或 Q1=B：另一列如何解释？

请从以下类别中选择最接近的业务语义，并补充必要说明：

- **R1：历史兼容字段**
- **R2：冗余/缓存字段**
- **R3：技术误用/错误写入字段**
- **R4：特定流程临时字段，但不代表库存主对象**
- **R5：其他** — 请描述。

## 5. Decision Constraints

Owner 的回答只解决 `inventory` 的业务语义问题，不直接决定：

- C-002 是否最终为 Identity；
- Material 是否进入 Required Identity Object Set；
- H1/H2；
- Schema 设计；
- Migration 方案；
- 现有实现是否需要重构。

## 6. Effect on C-002 Gate

在 Q1/Q2/Q3 所需业务真相未闭合前：

```text
inventory E2/E4 segment = CONDITIONAL
C-002 candidate-level = CANDIDATE_PASS_NOT_YET_PROVEN
```

即使 Owner 完成裁决，也**不自动使 C-002 E2/E4 PASS**；定向证据重定级、E-definition 对齐及 candidate-level adversarial re-review 仍必须完成。

## 7. Required Response Record

请至少记录：

```text
Owner: BUSINESS_OWNER / PRODUCT_OWNER
Decision date: YYYY-MM-DD
Q1: A / B / C / D
Q2: <required if Q1=C>
Q3: <required if Q1=A or B>
Business rationale: <brief but explicit>
Evidence provenance accepted by Owner: <optional reference>
```

## 8. Current Non-Decision

在 Owner 正式回答前，本记录不选择 A/B/C/D，不把技术外键或当前 ORM 行为升级为业务语义，不关闭 C-002 inventory blocking condition。
