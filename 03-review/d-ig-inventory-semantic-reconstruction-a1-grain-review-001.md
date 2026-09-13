# D-IG — Inventory Semantic Reconstruction A1 Grain Review 001

> **Status**: ACTIVE_REVIEW_RECORD
> **Attack**: A1 — Inventory Balance Grain / Locator Uniqueness
> **Target**: `d-ig-inventory-semantic-reconstruction-001.md`
> **Governance baseline**: `5923e7e585e718cf9ff1bfbec0380369ec85be32`
> **Production Evidence**: `BLOCKED`
> **Current A1 result**: `INCONCLUSIVE`
> **Unresolved attack**: `UNRESOLVED_ATTACK`

## 1. Review question

是否能够证明：在当前声明的证据范围内，所有 materially relevant 且被证明为活跃的 Inventory 定位/写入路径，都遵循当前 balance grain：

- 中央/仓库侧：`(materialId, warehouseId)`；
- 门店侧：`(storeId, materialId)`；
- 且不存在已证明活跃、业务相关、语义实质不同的绕过路径。

本攻击不把“存在另一种查询方法”自动视为 FAIL；必须建立 `active + business relevant + materially different grain/referent` 的完整证据链。

## 2. Evidence baseline

### 2.1 Governance baseline

本轮审查固定在 GitHub governance repository `wukaiktc1234/new-map` 的 commit：

`5923e7e585e718cf9ff1bfbec0380369ec85be32`

该 commit 同时包含本独立反方审查任务定义。

### 2.2 Local engineering workspace

未能从当前可访问的 governance repository 独立取得 Reconstruction 所引用的 local working-tree source snapshot / commit SHA。现有治理文档只声明 source observations 来自 local working tree，且部分明确标记为 `LOCAL_ONLY`。

因此本轮不能把先前调查者报告的源码观察重新当作已独立复现的 source evidence。

### 2.3 Database evidence

治理包中可复核的既有报告记录：PostgreSQL 18.3、database `food_traceability`、read-only local queries；但没有提供本轮可独立重连的 DB instance identity / snapshot timestamp。

因此 DB evidence 只能作为 submitted local evidence，不足以单独完成“同基线独立复核”。

### 2.4 Production

`PROD = BLOCKED`。本攻击不使用生产证据替代缺失的本地源码复核。

## 3. Submitted evidence relevant to A1

### 3.1 Current reconstruction claim

Reconstruction 报告称：

- 中央/仓库余额主要按 `(materialId, warehouseId)` 定位；
- 主要代码查询机制为 `getByMaterialAndWarehouse`；
- 门店余额主要按 `(storeId, materialId)` 定位；
- `uk_store_inventory_store_material` 是唯一索引；
- 中央侧缺少 DB 唯一约束，粒度主要依赖代码约定。

### 3.2 Already-reported path evidence

现有 Reconstruction 还报告：

- MaterialArchive 存在 `ensureInventoryForMaterial` 创建零库存路径；
- 采购入库以 `materialId` 为主要 referent；
- 多条库存扣减/出库路径使用 `materialId` 定位余额；
- LossOutbound 从 `productId` 参数进入，但经兼容别名实际写入 `material_id`；
- SalesOrder 是已被明确标记为显著例外的路径：以销售域 product 值 + transient `storeId` 查找 Inventory，但其运行期具体列解析仍未闭合；
- TraceCode 扫码可以创建没有稳定 ID referent 的 Inventory 行。

这些内容均来自既有调查记录；本轮尚未取得对应 local source snapshot 进行独立复现。

### 3.3 Database-side evidence already submitted

既有报告记录 `uk_store_inventory_store_material` 为唯一索引，并记录中央侧不存在等价 DB unique constraint。

这支持“门店侧存在 DB 层唯一性、中央侧主要依赖代码约定”的初步判断，但不等于已经证明所有活跃写入路径都遵循相同 grain。

## 4. Adversarial coverage performed

本轮已经完成对 governance evidence package 的 A1 定点审查，重点核对：

1. 当前 grain claim 的精确定义；
2. 已报告的 create / inbound / deduction / SalesOrder / TraceCode 路径；
3. 是否已经存在对直接 `inventory.id` 定位、其他组合键、Mapper/XML/native SQL、scheduler/event/script/data-sync 等旁路的完整可复核证据；
4. 是否能够证明每条可疑路径为 active business path；
5. 是否能够从同一 baseline 独立重跑 source-level locator coverage。

结果：治理包中存在“需要检查这些路径”的任务要求，但没有提供足以让当前独立审查者完成 source-level exhaustive coverage 的源码快照或 local workspace baseline。

## 5. Findings

### F-A1-01 — Core grain claim has supporting submitted evidence

`(materialId, warehouseId)` / `(storeId, materialId)` 作为当前工程 grain 模型有既有调查证据支持，且门店唯一索引为独立 DB evidence。

**Disposition**: `SUPPORTED_AS_SUBMITTED_EVIDENCE`

这不是 A1 PASS，因为尚未证明所有 materially relevant active paths 均符合该模型。

### F-A1-02 — SalesOrder remains an explicit unresolved locator path

既有 Reconstruction 已明确指出 SalesOrder 是显著例外：其从销售域 product 值与 transient `storeId` 进入 Inventory 查找，而运行期具体列解析仍为未决。

在没有重新取得对应 source snapshot 前，无法证明该路径：

- 实际调用了哪个 repository/mapper；
- 最终使用 `(materialId, warehouseId)`、`(storeId, materialId)`、`inventory.id` 还是其他组合；
- 是否 active；
- 是否形成与当前 grain 实质不同的余额 referent。

**Disposition**: `UNRESOLVED`

### F-A1-03 — No independent proof of exhaustive locator coverage

当前可访问证据没有足够的 source-level coverage 来独立证明：

- 所有 Inventory Repository/Service locator；
- 直接 `inventory.id` 查询后更新；
- XML / Mapper / native SQL；
- scheduler / event listener / batch / sync；
- 其他组合字段定位；
- 测试/废弃/死代码与 active business path 的区分。

因此不能从“现有报告没有列出反例”推导“反例不存在”。

**Disposition**: `INCONCLUSIVE`

### F-A1-04 — TraceCode is not by itself an A1 FAIL

TraceCode 可以形成无 stable ID referent 的 Inventory row，但该事实首先攻击 referent completeness，而不是自动证明 balance locator grain 被绕过。

在没有证明该路径如何定位/更新既有余额，以及是否形成另一种余额粒度前，不将其升级为 A1 FAIL。

**Disposition**: `NOT_A1_FAIL_ON_CURRENT_EVIDENCE`

## 6. A1 acceptance assessment

### PASS test

要求：在已声明证据范围内，所有 materially relevant 且被证明为活跃的 Inventory 定位/写入路径均符合当前 grain，或可证明与其业务等价。

**当前不能满足。** 原因不是已经发现反例，而是 source-level exhaustive coverage 与 active-path proof 不足。

### FAIL test

要求：存在至少一条已证明 active + business relevant + materially different 的绕过路径。

**当前没有足够证据宣布 FAIL。** SalesOrder 是可疑路径，但其最终 locator/referent 尚未独立复现。

### INCONCLUSIVE test

要求：关键代码面无法可靠覆盖，或存在可疑路径但无法证明 active / business effect / actual referent。

**满足。**

## 7. Result

`A1 = INCONCLUSIVE`

`UNRESOLVED_ATTACK = A1`

影响级别暂定：

`RECONSTRUCTION_CORE_CREDIBILITY — CONDITIONAL`

理由：Inventory balance grain 是 Reconstruction 的核心语义之一。当前不是证明 grain 错误，而是尚不能独立证明“所有 active locator paths 都遵循 grain”。在完成 source-level coverage 前，不应把 A1 标成 PASS。

## 8. Required evidence to resolve A1

需要在与治理 baseline 对应的同一 engineering baseline 上取得并核验：

1. Inventory Entity；
2. Inventory Repository 全部方法；
3. Inventory Service / ServiceImpl 全部定位、创建、增加、扣减、更新路径；
4. 所有 Inventory Mapper / XML / native SQL；
5. 所有直接引用 `inventory.id`、`materialId`、`warehouseId`、`storeId`、`productId` 的业务路径；
6. scheduler / event listener / batch / sync 对 Inventory 的写入；
7. SalesOrder locator 的完整调用链；
8. test / deprecated / unreachable path 的 active-status 判定依据；
9. local engineering workspace commit/state；
10. DB snapshot timestamp / migration baseline。

## 9. Re-run trigger

若后续 A3 证明 `storeId` / `inventoryType` 等存在 physical persistence 或活跃旁路写入，并改变当前 grain interpretation，则 A1 必须重新执行受影响部分。

若 A2 发现某消费路径通过尚未识别的 locator 直接更新 Inventory，则 A1 必须回溯该 locator 并重新判定。

## 10. Governance disposition

A1 当前不允许：

- 自动 PASS；
- 自动 FAIL；
- 将缺失的 source evidence 解释成“没有绕过路径”；
- 将 SalesOrder 的可疑性直接升级为事实；
- 解除 Inventory Reconstruction 的独立反方审查门禁。

A1 当前允许：

- 保持 `INCONCLUSIVE / UNRESOLVED_ATTACK`；
- 在取得 source baseline 后继续定点复核；
- 将当前 Reconstruction 的 grain wording 保持为 conditional / engineering-model wording。
