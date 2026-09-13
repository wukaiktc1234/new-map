# D-IG — A1 Independent Adversarial Review 001

> **状态**: ACTIVE_REFERENCE — INDEPENDENT ADVERSARIAL REVIEW
> **Attack target**: `03-review/d-ig-inventory-semantic-reconstruction-a1-local-evidence-001.md`（A1_RESULT = PASS / UNRESOLVED_ATTACK = NO）
> **Responds to**: `03-review/d-ig-inventory-semantic-reconstruction-a1-grain-review-001.md`
> **Governance baseline**: `5923e7e585e718cf9ff1bfbec0380369ec85be32`
> **Production Evidence**: BLOCKED
> **Identity Decision**: NOT_MADE · **Schema/Migration Authorization**: NO

---

## 0. Review stance

本复核不重复 Reconstruction，也不为原 A1 报告辩护。目标是以最小成本找到能推翻 `A1_RESULT = PASS` 的真实反例。攻击面：R0（新增未跟踪文件）、R1（MP lambda 运行期行为——核心）、R2（LossOutbound）、R3（SalesOrder）、R4（warehouse 新事实）、R5（TraceCode 旧结论）。

**关于 811 dirty files**（按任务 §三）：本轮不作为争议。前序核查已确认构成 = 464 个已跟踪文件真实未提交改动（+18,216/−6,935 行，0 个 CRLF 幻影）+ 347 个未跟踪文件 + 1 个删除；不要求纳入治理提交。

---

## 1. A1-R0 — 47 个新增 backend 文件扫描

**实际清单**：untracked `backend/` 条目共 **48** 个 = 44 个 java + 3 个 controller/dto 拆分外的 java + … 精确构成：java 35、SQL migration 12、cmd 脚本 1（`backend/start-backend.cmd`，非源码）。

**扫描方法**：三轮 grep（直接符号 `Inventory|inventory`；定位符号 `getByMaterialAndWarehouse|getByStoreAndMaterial|materialId|warehouseId`；行为动词 `increaseInventory|decreaseInventory|InventoryService|InventoryMapper|current_stock|currentStock`），再加一轮间接域动词（`tockin|onsumption|raceCode|ransfer|djust|utbound|stock`），最后对命中文件与 `CanonicalOrderCommand` 引用链单独核查。

**结果**：

- 直接符号：**0 命中**（48 个文件全部无 Inventory/inventory 字符串）
- 间接动词：唯一命中 `dto/AccountBalanceOpeningDTO.java`——为 "adjustment"（财务账户期初调整），与库存无关，`A1-IRRELEVANT`
- `V20260905_009__legacy_payment_data_inventory.sql`：文件名中的 "inventory" 是"数据盘点"之义，内容为 **payment 表只读盘点脚本**，不触碰 inventory 表 → `A1-IRRELEVANT`
- `CanonicalOrderCommand/CanonicalOrderItemCommand` 被活跃引用（PosOrderCreateServiceImpl），但该链路对 `Inventory|inventoryMapper|materialId|InventoryService` **零命中**——POS 链仅扣 food/foods.stock，不触碰 material 库存表
- 其余新文件：AppDeviceRegistration、BankPaymentRecord/Reconciliation、Receipt*（收货凭证/签名/打印）、Training 等域，均无库存语义

**分类**：全部 `A1-IRRELEVANT` 或 `A1-RELEVANT + NO-BALANCE-EFFECT`；**NEW-LOCATOR：0 个**。

```text
A1-R0 = PASS
```

---

## 2. A1-R1 — MyBatis-Plus Lambda Runtime Reproduction（核心攻击）

### 2.1 实际版本钉死（不信任报告假设）

`mvn dependency:tree -Dincludes="com.baomidou"`（backend，2026-09-13 17:47 +08）：

```text
com.foodtraceability:food-traceability:jar:1.0.0
\- com.baomidou:mybatis-plus-spring-boot3-starter:jar:3.5.5:compile
   +- com.baomidou:mybatis-plus:jar:3.5.5:compile
   |  +- com.baomidou:mybatis-plus-core:jar:3.5.5:compile
   |  +- com.baomidou:mybatis-plus-annotation:jar:3.5.5:compile
   |  \- com.baomidou:mybatis-plus-extension:jar:3.5.5:compile
   \- com.baomidou:mybatis-plus-spring-boot-autoconfigure:jar:3.5.5:compile
```

**运行时 = MP 3.5.5 全家桶，无多版本冲突**（.m2 中虽存在 3.5.3.1–3.5.11 多版本，属其他项目缓存，本 pom 解析唯一）。`MybatisPlusConfig.java` 仅注册乐观锁+分页拦截器，**无 GlobalConfig/列格式定制** → 默认 TableInfo 初始化即与生产等价。

### 2.2 最小运行期实验（只读，不修改业务数据与业务代码）

实验环境：`backend/src/main/java/com/foodtraceability/entity/{Inventory,SalesOrder,SalesOrderDetail}.java` 独立编译（仅依赖 mp-annotation）+ MP 3.5.5 core/annotation + mybatis 3.5.13 真实运行时；`TableInfoHelper.initTableInfo`（与 Spring 启动同一初始化路径）后构建 `LambdaQueryWrapper` 并调用 `getSqlSegment()`，Throwable 全捕获。

**运行输出（逐字）**：

```text
==== [1] Inventory TableInfo (runtime MP 3.5.5) ====
keyProperty=inventoryId keyColumn=inventory_id
  field: materialId -> column: material_id        （…共 23 个映射字段，逐字见附录）
  （fieldList 中不存在 productId、productName、storeId、inventoryType）

columnMap keys contain productId? false
columnMap keys contain storeId?   false

==== [2] lambda resolution probes ====
[RESOLVED ] Inventory::getMaterialId  => SQL segment: (material_id = #{ew.paramNameValuePairs.MPGENVAL1})
[THROWN   ] Inventory::getProductId  => com.baomidou.mybatisplus.core.exceptions.MybatisPlusException
             message: can not find lambda cache for this property [productId] of entity [com.foodtraceability.entity.Inventory]
[THROWN   ] Inventory::getStoreId  => com.baomidou.mybatisplus.core.exceptions.MybatisPlusException
             message: can not find lambda cache for this property [storeId] of entity [com.foodtraceability.entity.Inventory]

==== [3] SalesOrder TableInfo (exist=false check) ====
columnMap contains 'status'?  false
=> selectById 将不包含 status 列 -> getStatus() 返回 null（字段不存在于映射）

==== [4] SalesOrderDetail TableInfo (productId→food_id 验证) ====
  productId -> column: food_id
[RESOLVED ] SalesOrderDetail::getProductId  => SQL segment: (food_id = #{ew.paramNameValuePairs.MPGENVAL1})
```

**对照任务 §八三种 Case**：

| Case | 预测 | 运行实测 | 结论 |
|---|---|---|---|
| A：runtime exception | — | **✔ 命中**：`MybatisPlusException: can not find lambda cache for this property [productId]` | 原 A1 判断（W4/W5 无法形成有效 locator）**获得运行期强支持** |
| B：静默失败→空 column | — | 未出现 | — |
| C：解析成 material_id（别名生效） | 若出现则 A1 → INCONCLUSIVE | **未出现**——`getProductId()` 虽然返回 materialId 值，但 lambda 解析按方法名属性 `productId` 查列缓存，**不做别名回退** | Case C 被运行期否证；不触发 INCONCLUSIVE |

**方法学说明（透明度）**：第一版探针用 `LambdaUtils.getColumnMap(...).containsKey("materialId")` 打出 false，与 RESOLVED 矛盾——系 **MP 3.5.5 列缓存 key 为大写格式**（`LOCATIONID/QUANTITY/...`，KeyProbe 实测 size=24），containsKey 检查用错 key 形态。**审计者请以 §2.2 的 wrapper 实测输出与 fieldList 打印为权威**（那两条不依赖 key 格式）。KeyProbe 同时证明：24 个 key 中无 `PRODUCTID`/`STOREID`。

**R1 判定**：`Inventory::getProductId / getStoreId` 在运行期抛异常、不生成 SQL、不可能定位或修改任何余额行；`getMaterialId` 正常解析。**A1-R1 = 原判断获得运行期验证（Case A）。**

### 2.3 实验保真度声明

- 探针使用与生产**完全相同的 jar 版本**（3.5.5）与**相同的 TableInfo 初始化路径**，且应用无 GlobalConfig 定制 → 等价。
- 探针 JVM 为 Java 25（应用 target 21）：MP 3.5.5 lambda 序列化提取不含 JDK 版本分叉逻辑；不影响结论（如需可在 JDK21 重跑，探针与步骤见附录）。
- Production 仍 BLOCKED：本实验证明的是**框架运行期行为**，不是生产数据行为——该边界不变。

---

## 3. A1-R2 — LossOutbound 重溯

链路：`LossOutboundController`（存在，ACTIVE entry）→ `create`（insert loss_outbound，referent=product 表校验）→ `approve(:105)` → `updateInventory(:122)` → wrapper `eq(Inventory::getProductId, …):124`。

**R1 实测直接适用**：`:124` 的 `Inventory::getProductId` 在运行期抛 `MybatisPlusException`（无 try/catch 包裹）→ `approve` 事务回滚 → **库存余额不可能被修改**。本地 `loss_outbound=0` 行仅作辅助佐证（依任务要求不作为判死主因）。

```text
分类：ACTIVE entry / DEAD at balance effect
判定：NOT_A_DIFFERENT_ACTIVE_LOCATOR（运行期验证，非仅静态推断）
```

## 4. A1-R3 — SalesOrder 重溯

三重死因，其中两条已运行期验证：

1. **入口门不可达**（运行期实证 §2.2-[3]）：`SalesOrder.status` 为 `@TableField(exist=false)` → 列映射无 status → `selectById` 不含该列 → `getStatus()` 恒 null → `!"preparing".equals(null)` 恒真 → `completeOrder` 恒 `return false`，`:143` 扣减永不执行。
2. **lambda 解析失败**（运行期实证 §2.2-[2]）：`Inventory::getProductId/getStoreId` 抛 `MybatisPlusException`。
3. **值域错位**（注解实证）：`SalesOrderDetail.productId → order_items.food_id`（运行期实证 §2.2-[4] 解析为 food_id），本地 6/6 NULL。

```text
分类：DEAD（三重独立证据，两条运行期）
判定：NOT_A_DIFFERENT_ACTIVE_LOCATOR
```

## 5. A1-R4 — Warehouse 新事实

实测：`warehouses` 表存在（10 行）；inventory 活跃 11 行中 **10 行** warehouse_id 可解析到 warehouses（1 行悬空）。`OtherInboundService/LossOutboundService` 均 `warehouseMapper.selectById` 校验。

```text
GOVERNANCE_REPAIR_REQUIRED #1：DEC-001「Warehouse 无独立表 / inventory.warehouse_id 引用目标不明」→ SUPERSEDED（由 V20260629_015 建立 warehouses 主数据）
```

## 6. A1-R5 — TraceCode 旧结论

复核 `MaterialTraceCodeServiceImpl` 全部 `inventoryService` 引用（:36/:39/:46 注入；:134/:417 使用点）：两个 `new Inventory()` 构建块（:134-146/:417-428）均以 `log.info` 结尾，**无 save/saveBatch/insert/update 任何持久化调用**；其后仅写 `store_inventory_log`。原 Reconstruction「TraceCode 扫描创建 text-only Inventory row」**不成立**（构建即弃置）。

同时确认：`TraceCodeServiceImpl:150` 追溯出库经 `(materialId ← TraceCode.setProductId→target_id, warehouseId)` 走 W2 粒度键——`target_id → materialId 值域` 属 **referent/value provenance → 登记 A2 candidate**，不属 A1 grain。

```text
GOVERNANCE_REPAIR_REQUIRED #2：Reconstruction §10-C8「TraceCode 创建 text-only Inventory row」→ SUPERSEDED（build-and-log，零持久化）
GOVERNANCE_REPAIR_REQUIRED #3：A1 Evidence Matrix 的 W3（deduct/lock by id）/W10（HardwareDevice）/W11（Scheduler）——核查结果：a1-local-evidence-001 §5 矩阵已包含，无需补录
```

---

## 7. 独立 Evidence Matrix

| Attack | Path | Runtime Evidence | Activity | Locator | Result |
|--------|------|------------------|----------|---------|--------|
| R0 | 48 untracked backend files | 3+1 轮符号扫描全零命中；唯二疑点（inventory 命名 SQL、CanonicalOrderCommand）均为 A1-IRRELEVANT | — | none | **R0 = PASS（NEW-LOCATOR=0）** |
| R1 | `Inventory::getProductId` | `MybatisPlusException: can not find lambda cache … [productId]`（运行期逐字） | — | none（Case A） | **原判断成立** |
| R1 | `Inventory::getStoreId` | `MybatisPlusException: … [storeId]`（运行期逐字） | — | none（Case A） | **原判断成立** |
| R1 | `Inventory::getMaterialId` | RESOLVED → `(material_id = #{MPGENVAL1})` | — | (materialId, warehouseId) grain 腿 | **grain 合法腿确认** |
| R1+ | `SalesOrderDetail::getProductId` | RESOLVED → `(food_id = …)` | — | 域外（order_items） | 证明"有映射才可解析"的反向锚点 |
| R2 | LossOutbound approve | 定位前必抛（R1 结论适用）；loss_outbound 0 行（辅助） | ACTIVE entry / DEAD at balance | attempted (productId, warehouseId) | **NOT_A_DIFFERENT_ACTIVE_LOCATOR** |
| R3 | SalesOrder completeOrder | status 列不存在（运行期）+ lambda 必抛（运行期）+ 值域 food_id/NULL | DEAD | attempted (productId, storeId) | **NOT_A_DIFFERENT_ACTIVE_LOCATOR** |
| R4 | warehouses 表 | 10 行；10/11 活跃库存行可解析 | — | warehouse 腿 referent 已闭合 | **Governance Repair #1** |
| R5 | TraceCode build-and-log | 零持久化调用（grep 全量） | DEAD-CODE | none | **Governance Repair #2**；target_id provenance → A2 |

---

## 8. 最终结论

```text
A1_INDEPENDENT_REVIEW_RESULT = PASS_WITH_REPAIRS
UNRESOLVED_ATTACK = NO
```

**PASS_WITH_REPAIRS 而非 PASS 的理由**：A1 核心结论（locator 唯一性）在 R0–R3 全部攻击下成立，且核心攻击（R1）由**运行期实验**而非静态推断闭合——满足 PASS 条件 1–5；但治理文档存在三处必须修复的事实（§5/§6），按任务 §十三归入 PASS_WITH_REPAIRS。

## 9. A1 语义边界（依任务 §十六）

本复核仅支持以下表述：

> **Material 作为 Inventory 的主工程 referent，在库存余额粒度/定位层面未发现有效的活动型替代 locator；所有可到达余额的 locator 收敛于 (materialId, warehouseId)、(storeId, materialId) 与 inventory.id。**

明确不允许引申为：

- ~~Material is the business identity of Inventory.~~
- ~~Material is proven as the final identity truth.~~

**Referential/value-domain correctness remains an A2 question**（已登记候选：TraceCode target_id 值 provenance、OtherInbound product 校验值落 material_id、purchase_request_item 4/50 异常、inventory.product_id 列裁决=Owner Q1/Q3）。

## 10. Residual Notes（不构成 UNRESOLVED_ATTACK）

1. 探针 JVM=Java 25 / 应用 target 21（MP 3.5.5 该路径无 JDK 分叉逻辑；可在 JDK21 重跑，见附录步骤）。
2. Production Evidence = BLOCKED（不变）：本报告证明框架运行期行为与本地工程现实，不证明生产数据分布。
3. 软删物料被库存引用（2 行）、material_id=999999 悬空、台账 17/27 孤儿——数据质量轨道，不影响 locator 粒度结论。

## 11. Next

等待治理层确认 A1 正式关闭后进入 **A2 — Referential Integrity / Identifier Semantics**。本报告不开始 A2。

---

## 附录 A — Runtime Probe 复现步骤

```bash
# 1. 实体独立编译（仅依赖 mp-annotation）
javac -encoding UTF-8 -cp mybatis-plus-annotation-3.5.5.jar -d out \
  com/foodtraceability/entity/{Inventory,SalesOrder,SalesOrderDetail}.java
# 2. 探针编译运行（classpath: mp-core-3.5.5 + mp-annotation-3.5.5 + mybatis-3.5.13 + out）
javac -cp "$CP" -d out A1Lab.java && java -cp "$CP" A1Lab
# 3. 期望输出：materialId RESOLVED → material_id；productId/storeId THROWN（消息见 §2.2 逐字记录）
```

探针源码要点：`MybatisConfiguration + MapperBuilderAssistant.setCurrentNamespace → TableInfoHelper.initTableInfo(assistant, clazz)`（与 Spring 启动同一初始化路径；应用无 GlobalConfig 定制，默认配置等价）。三实体源文件取自 engineering worktree `6b54411` + dirty 状态（§2 基线），未做任何修改。

## 附录 B — 本轮证据基线

```text
engineering worktree HEAD = 6b54411d4833f4e832b347567ecfddf7e72949a1 (master)
WORKTREE_DIRTY = YES（811 = 464 真实改动 + 346 未跟踪 + 1 删除；构成已单独核验，非争议项）
DB snapshot = 2026-09-13 16:16:21 +08（PostgreSQL 18.3 / food_traceability / flyway head 20260909.003）
governance remote main at review time = d10a877（a1-local-evidence-001.md 所在提交）
```
