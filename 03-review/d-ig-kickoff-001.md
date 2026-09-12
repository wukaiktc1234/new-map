# D-IG Kickoff 001 — Identity Hierarchy Question

> **Status**: ACTIVE WORKING SESSION  
> **Decision**: D-IG  
> **Upstream**: DEC-BI-001 / DEC-BI-002 = CONFIRMED  
> **First Question**: H1 vs H2

## 1. Question

> 业务世界是否需要多个合法的 Identity 层级？

### H1 — Single Identity Grain

整个业务模型只有一个 Identity Grain。所有需要不同抽象程度的业务表达，均通过非-Identity 关系、属性或商业/数量语义表达。

### H2 — Multiple Identity Layers

业务模型存在两个或多个合法 Identity 层级，并且不同层级回答不同业务问题；层级之间存在明确、稳定的关系。

## 2. H1/H2 不得通过以下方式决定

- 不能以当前数据库表数量决定；
- 不能以现有主键设计决定；
- 不能以 Product / Food / Material 名称决定；
- 不能以单个本地字段的历史用法决定；
- 不能以外部 ERP/SAP 模型直接套用决定。

## 3. H1/H2 必须回答的业务问题

1. 是否存在两个业务上都必须保留、但抽象粒度不同的“同一对象”概念？
2. 上层对象是否需要独立生命周期、定价、采购、销售或治理？
3. 下层对象是否可以在不改变上层 Identity 的情况下独立变化？
4. 是否存在稳定、可验证的层间一对多/多对一/版本关系？
5. 如果强制单层，会不会把不同业务语义压缩进一个 Identity？
6. 如果采用多层，会不会只是把 Role / Commercial Unit / UOM / Holding 错误升级成 Identity？

## 4. 必须使用的案例

- 同品牌不同容量：330ml vs 500ml
- each / pack / case
- 不同采购单位
- Store / Warehouse
- Batch / Lot
- Recipe usage
- Product / Food / Material cross-domain reference
- Sellable + Purchasable
- Sellable + Recipe input
- Role 随 Organization / Temporal 改变

## 5. 已知本地事实与解读限制

### 已知本地事实

- `foods` 与 `material_archives` 分别存在明确的当前真相源角色。
- `inventory` / `store_inventory` 使用不同的持有结构。
- `purchase_request_item.food_id` 存在大量实际匹配 material ID 的本地记录。
- `product` 仍有 active-code residue。

### 解读限制

- `foods` 与 `material_archives` 的当前真相源角色，不等于它们已经被证明为最终 Identity 层级。
- `inventory` / `store_inventory` 的持有结构差异，不等于存在多个 Identity 层级。
- `purchase_request_item.food_id` 的 namespace 漂移证明存在语义冲突，但不能单独证明 H1 或 H2。
- `product` 表存在 active-code residue，不得因为表名直接推导 Identity。
- 未取得独立 Production DB，因此生产数据不能用于证明 H1/H2。
- 合成业务案例只能用于反证和需求压力测试，不能伪装为生产事实。

## 6. Decision Method

先回答 H1/H2，再建立 Identity Resolution Matrix。

每个支持 H1 或 H2 的论据必须给出：

- 业务问题；
- 假设；
- 可观察预测；
- 当前证据；
- 反证条件；
- 是否 provisional。

在 H1/H2 判断阶段，允许临时构建最小结构示例，用于比较两种模型的表达能力；但任何此类试建结构均不得写成 Decision、Canonical Model 或 Schema Authorization。

## 7. Current Status

```text
H1 = OPEN
H2 = OPEN
Selected Hierarchy = NONE
Identity Resolution Matrix = NOT STARTED
Schema Design = BLOCKED_BY_D_IG
```
