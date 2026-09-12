# D-IG — E3 Owner Rule Re-evaluation 001

> **状态**: ACTIVE_REFERENCE — TEST RE-EVALUATION
> **Decision ID**: D-IG
> **适用候选**: C-001 Food / C-002 Material
> **Owner rule source**: `03-review/d-ig-e3-owner-decision-001.md`
> **Formal owner_identifier**: PENDING_FORMAL_IDENTIFIER
> **Formal review date/session metadata**: PENDING_FORMAL_RECORD
> **Identity Decision**: NOT_MADE
> **H1/H2**: NOT_MADE
> **Schema Authorization**: NO
> **Migration Authorization**: NO
> **Production Evidence**: BLOCKED

## 1. Re-evaluation purpose

本记录依据已明确的 Business Owner 业务规则重新评价 C-001 Food 与 C-002 Material 的 E3（独立生命周期）测试。

本次复评只解决 E3 原有的 `INCONCLUSIVE_PENDING_OWNER_BUSINESS_RULE` 缺口，不重做 E1/E2/E4，也不直接作出最终 Identity Decision。

## 2. C-001 Food — E3 re-evaluation

### 2.1 Independent lifecycle

工程证据已经显示 Food 具备独立的 CRUD / Read 路径、独立 API 与权限表面。此前 E3 的主要缺口不是“是否存在独立对象入口”，而是这些状态是否具有明确的业务生命周期语义。

Business Owner 现已明确：

- `INACTIVE` = 暂时下架：暂时不卖、不在菜单展示，但历史订单继续保留并可正常查看、解释和引用；
- `DISCONTINUED` = 正式停售：以后不再作为正式销售菜品，但历史订单、成本和追溯记录继续保留；
- 历史订单同时保留 Food 编号与当时菜名快照；
- 状态变化仅部分触发业务事件；具体转换和各端后续行为留到 Requirement 阶段；
- `DISCONTINUED → ACTIVE` 可以直接恢复销售。

这些规则共同建立了独立于单笔交易存续的 Food 对象生命周期：Food 可以被创建、下架、停售、恢复，其状态表达的是 Food 自身的业务状态，而不是某一订单的交易状态。

### 2.2 E3 adversarial check

原有主要反方观点是“状态只是技术开关”。该观点在 Owner 规则确认后不再成立为完整解释，因为：

- `INACTIVE` 与 `DISCONTINUED` 分别承担明确、不同的业务含义；
- 历史订单引用与展示规则被明确指定；
- 状态变化是否产生业务事件已被明确为业务规则的一部分；
- `DISCONTINUED → ACTIVE` 的恢复规则也是对象生命周期规则，而非单纯 API 可用性开关。

具体哪些转换向 POS、菜单、小程序、外部平台发送什么通知，仍属于 Requirement 阶段，不能反向削弱当前 E3 的生命周期判断。

**E3 result: `PASS`**

## 3. C-002 Material — E3 re-evaluation

### 3.1 Independent lifecycle

工程证据已经显示 Material 具备独立的 Material truth source、CRUD / Read 路径与权限表面。此前缺口同样集中于 `ACTIVE / INACTIVE` 的业务含义与状态行为。

Business Owner 现已明确：

- `INACTIVE` = 停止新的采购、库存和使用，但历史采购、库存、追溯等记录继续保留；
- 历史记录同时保留 Material 编号与当时名称快照；
- 状态变化仅部分触发业务事件；具体转换与各业务端处理留到 Requirement 阶段；
- 当前不另外设置独立的 `DISCONTINUED` 状态。

这些规则建立了独立于采购、库存或消耗单据的 Material 对象生命周期：Material 自身可以处于 active/inactive 生命周期状态，而历史业务记录继续保持可解释性。

### 3.2 E3 adversarial check

“Material 状态只是技术开关”的反方解释不再能够完整覆盖当前规则，因为：

- `INACTIVE` 明确影响未来采购、库存和使用资格；
- 历史引用保留规则明确；
- 状态变化事件是否存在已被定义为业务规则，而非纯技术属性；
- Material 生命周期与具体采购/库存/消耗单据并非同一生命周期。

具体哪些状态转换触发什么下游动作，属于 Requirement 阶段，不在本次 E3 中提前展开。

**E3 result: `PASS`**

## 4. Cross-candidate relationship safety

本次复评不把 Food 与 Material 的 E3 结果解释为二者是同一 Identity。

当前明确的是：

- Food 与 Material 的生命周期模型由 Owner 显式确认 **分离**；
- 两者都具有独立生命周期；
- 两者的 INACTIVE 都包含“停止新的业务使用并保留历史”的管理思想，但实际业务含义不同；
- Food 具有独立 `DISCONTINUED` 语义，而 Material 当前没有该独立状态。

该结论只用于 E3 业务语义复评，不关闭 FM-001–FM-004，也不替代 Required Identity Object Set / H1/H2。

## 5. Candidate-level consequence

当前已有：

- C-001: E1 PASS / E2 PASS / **E3 PASS** / E4 PASS
- C-002: E1 PASS / E2 PASS / **E3 PASS** / E4 PASS

因此两个候选均满足 candidate-level PASS 的 E-stage 条件。

但 candidate-level conclusion 仍应执行独立的候选级复核 / adversarial review，并不因四个 E-stage 当前均为 PASS 而自动产生最终 Identity Decision。

## 6. State transition safety

本次 E3 复评不提前把以下内容写成已完成 Requirement：

- 哪个具体状态转换通知哪个端；
- POS、菜单、小程序、外部平台如何具体响应；
- 状态转换的接口、事件类型、队列、事务或数据模型；
- Schema / Migration 方案。

这些内容继续留在后续 Requirement / Engineering 阶段。

## 7. Governance state

- C-001 E3: `PASS`
- C-002 E3: `PASS`
- C-001 E-stage set: `E1 PASS / E2 PASS / E3 PASS / E4 PASS`
- C-002 E-stage set: `E1 PASS / E2 PASS / E3 PASS / E4 PASS`
- Candidate-level conclusion: `PENDING_CANDIDATE_ADVERSARIAL_REVIEW`
- Required Identity Object Set: `NOT_STARTED`
- FM-001–FM-004: `OPEN`
- H1/H2: `NOT_MADE`
- Identity Decision: `NOT_MADE`
- Schema: `BLOCKED`
- Migration: `BLOCKED`
- Production evidence: `BLOCKED`
- Requirement-driven status: `REQUIREMENT_DRIVEN_INCOMPLETE`
