# D-IG — Governance Scope Reassessment 001

> **状态**: ACTIVE_REFERENCE — GOVERNANCE SCOPE ASSESSMENT（评估记录，不覆盖任何历史文件）
> **任务**: 判断 A3/A4 是否仍具 Decision Value；是否具备进入 Owner Decision 的条件
> **Governance baseline**: `5923e7e585e718cf9ff1bfbec0380369ec85be32`
> **Production Evidence**: BLOCKED
> **Identity Decision**: NOT_MADE · **Schema/Migration Authorization**: NO

---

## 1. Mandatory Source Set（已读）

| 来源 | 状态 |
|---|---|
| 1. Reconstruction report（d-ig-inventory-semantic-reconstruction-001.md） | ✔ 已读 |
| 2. A1 independent adversarial review（a1-local-evidence-001 + a1-adversarial-review-001，4048e21） | ✔ 已读 |
| 3. A2 referential review（a2-referential-review-001，11985ec） | ✔ 已读 |
| 4. A2 Failure Closure 001（a2-failure-closure-001.md） | ✔ 已读 |
| 5. 当前 Governance / Attack Charter | ✔ Charter 001 + Amendment 001/002 已读 |
| 6. Owner Questions / Decision records | ✔ E3 Owner Decision/Reevaluation、Owner Decision Request 001 已读 |
| **A3 / A4 的正式 attack 定义** | **NOT_AVAILABLE**（全语料检索 0 命中；唯一出处是 a1-grain-review-001 §9 的条件触发语，非定义） |

---

## 2. Historical Preservation

本报告为新增评估记录。未修改：原 A2 report、A2 Failure Closure、A1 报告、Reconstruction report、任何历史 DEC。

---

## 3. Frozen Current A2 State

| Attack | Current State |
|--------|---------------|
| R1 | INCONCLUSIVE |
| R2 | PASS |
| R3 | NARROWED |
| R4 | CONFIRMED / FAIL |
| R5 | PASS |

```text
A2 = FAIL
UNRESOLVED_ATTACK = YES（R1）
```

本轮未重新攻击 R1–R5（仅执行 §6 允许的可审计性验证）。

---

## 4. Defect Classes（仅记录 Closure 已支持证据）

### F1 — Polymorphic consumer missing discriminator/type validation → `NARROWED`

```text
CODE_LEVEL       = CONFIRMED（W-A：客户端无约束 targetType/targetId；type 1=菜品追溯码为一等类型；出库消费无 discriminator）
EXECUTABILITY    = CONFIRMED（API 已暴露：POST /v1/trace-codes，trace:create 权限门；Consumer：ScanDeviceServiceImpl:202 设备出库扫码，活跃）
LOCAL_INSTANCE   = NOT_OBSERVED（food_trace_codes 0 行；前端/小程序/员工端对创建 API 零调用）
PRODUCTION_IMPACT = UNKNOWN
```

必须区分（已区分）：consumer defect confirmed ✔；producer activation not observed ✔；production impact unknown ✔。

### F2 — Input/write value-domain mismatch → `CONFIRMED`

```text
CODE_LEVEL       = CONFIRMED（product 表校验值 → material_id 定位/落库；OtherInbound 与 LossOutbound 同模式）
EXECUTABLE       = CONFIRMED（OtherInbound：映射定位器、单事务、活跃控制器；LossOutbound：定位器必抛=潜在）
REAL INVENTORY BALANCE REACHABLE = YES（material_id=1 等真实余额行存在：中央 3 行 + 门店 2 行）
LOCAL CORRUPTING TRANSACTION OBSERVED = NO（other_inbound / loss_outbound 均 0 行）
PRODUCTION_IMPACT = UNKNOWN
```

不得把 NOT_OBSERVED 写成 OBSERVED——已遵守。

---

## 5. F1 Conditional Consequence（仅登记风险）

> **CONDITIONAL_RISK**：若一个 Food-domain target_id 被生产出来并到达已识别的出库消费方（ScanDevice OUTBOUND 扫码 → scanTraceCodeOutbound），该消费方可能将该值解释为 Material 域库存标识符，subject to locator resolution 与扣减前置条件（状态门、余额存在性）。

不写"已经造成错账"、不写"必然造成错账"、不写"生产环境存在该问题"——现有证据不支持（本地 0 行、生产未知）。

---

## 6. R4 Auditability Verification（本轮唯一新增工程验证）

**范围**：OtherInbound 成功 update 分支（不建 fixture、不改代码/schema、不执行真实业务写入；纯 source-level 核验）。

**逐项事实**（`OtherInboundService.java:94-125`，create 为 `@Transactional`）：

| # | 检查项 | 结果 |
|---|---|---|
| 1 | balance update 是否发生 | **是**：`inventory.setQuantity(currentStock + quantity)` → `inventoryMapper.updateById`（或 insert 分支直接建行）——直接 mapper 写入 |
| 2 | 是否写 inventory_transactions | **否**——该类无 InventoryTransactionMapper 引用；全仓台账唯一写入方 = `InventoryServiceImpl.recordTransaction`（A1 已证），OtherInbound 绕过 |
| 3 | 是否写其他 ledger/history/audit | **否**——无 inventory_log/traceLog/任何 history 写入；仅 `log.info`（运行日志，非可查数据） |
| 4 | 与 balance update 同事务 | **是**——other_inbound insert 与 balance update 同一 `@Transactional` |
| 5 | 是否存在 bypass | **是**——绕过 W1/W2 的标准通道（`InventoryService.increase/decreaseInventory`，那两条才写台账）；本路径为直连 mapper 的旁路 |
| 6 | 错误值域输入能否经现有记录追溯 | **部分**——`other_inbound` 文档行记录 product_id/product_name/quantity/warehouse（同事务落库），事后可按 `document.product_id = balance.material_id` 数值连接重建；但 (a) 余额变动本身无台账痕迹（无 before/after、无 reference 关联）；(b) 该数值连接恰是 A2 已证不可靠的命名空间假设——文档只能证明"入了 product#1 的货"，**不能区分**"正确 material 入库"与"错误 product 入库" |

```text
R4 AUDITABILITY = PARTIALLY_AUDITABLE
（文档层可追溯 + 台账层不可追溯 + 连接依赖已被否证的数值等值假设）
```

---

## 7. A3 Decision Value

前置事实：**A3 无正式定义（NOT_AVAILABLE）**。唯一可考出处 = a1-grain-review-001 §9 的条件触发语。据此枚举全部合理候选并逐一判值：

| Candidate A3 Question | New Information Beyond A1/A2 | Could Change Owner Decision? | Could Change Identity Decision? | Could Change Schema Authorization? | Decision Value |
|---|---|---|---|---|---|
| ① storeId/inventoryType 物理持久化或旁路写入（grain-review §9 点名） | **NONE**——A1 运行期探针 + MP 源码（ReflectionKit transient 过滤）+ 数据三重已答（store_id 0/11、inventory_type 全 1） | NO | NO | NO | **NONE** |
| ② 经未识别 locator 更新库存的消费路径（grain-review §9 点名） | **NONE**——A2 已穷举六条 decrease 调用方 + 台账唯一写入方 | NO | NO | NO | **NONE** |
| ③ batch/lot 是否进入余额粒度 | 部分（Reconstruction 已证 batch 非粒度、双套批次事实） | NO（不改变 Q1/Q3/OQ-A/OQ-B） | NO（属后续 Grain/Requirement 决策） | 间接（远期） | **LOW** |
| ④ 数据质量溯源（999999、软删引用、17/27 台账孤儿） | 部分（来源未明） | NO（属工程修复，非业务裁决） | NO | NO | **LOW** |
| ⑤ store-vs-warehouse 同步语义（warehouseId 充当 storeId） | 部分 | NO（缺陷修复候选，非决策输入） | NO | NO | **LOW** |

**判定**：A3 的全部可构造候选均**无法改变任何待决治理决策**。"可能发现更多问题"不是 Decision Value。

---

## 8. A4 Decision Value

**A4 定义：NOT_AVAILABLE**（语料零命中）。

按其可能语义面（Food/Material referential、value-domain、polymorphic target semantics）评估：与 A2 已覆盖面**完全重叠** →

```text
A4 = DUPLICATIVE
Decision Value = NONE
```

除非治理层先给出 A1/A2 未触及的明确 scope 定义，否则保留 A4 只是为维持原计划（被 §十五原则禁止）。

---

## 9. Owner Decision Readiness

| 问题 | 1. Engineering evidence sufficient? | 2. Owner decision required? | 3. Can Owner answer now? | 4. Answering now materially reduces uncertainty? | 5. Would waiting for A3/A4 change the answer? | 结果 |
|---|---|---|---|---|---|---|
| **OQ-A** 菜品追溯码出库是否允许影响 Material 库存 | **是**（多态结构 + 无 discriminator + 消费方活跃——结构证据完备；实例为 0 不影响规则裁决） | 是 | 是（预防性规则裁决，不依赖实例数据） | 是（决定 F1 修复方向与 Rule Candidate 去留） | NO | **READY** |
| **OQ-B** OtherInbound 业务对象是 Product 还是 Material | **是**（冲突全特征化：product 校验 + material 余额键 + 碰撞后果 + 可审计性=PARTIALLY_AUDITABLE） | 是 | 是 | 是（直接决定 R4 修复方向，并反哺 Q1） | NO | **READY** |
| **Q1** Inventory 业务对象范围（A/B/C/D） | **是**（Decision Request 001 §2 的 evidence pack 已成立，A2 追加值域血证：FK 不对称、同值异指、product_id 列无活跃读写方、PARTIALLY_AUDITABLE） | 是 | 是（Decision Request 自身接受 documentary provenance） | **是**——解锁 C-002 E2/E4 inventory 段、决定 OtherInbound/LossOutbound 修复方向、收敛 product_id 列处置 | NO（A3 已被 A1 回答；A4 未定义） | **READY_WITH_CAVEAT**（provenance=local/documentary；生产 BLOCKED，若生产证据后续出现反向事实，须允许重开） |

N1（无 referent 库存行）：已被 A1 证伪前提（build-and-log 零持久化）→ 问题消亡。N2（销售域对账维度）：SalesOrder 扣减链 DEAD（三重死因）→ 降级为 Requirement 阶段事项，非当前 Owner 阻塞。

---

## 10. Identity Decision Boundary（防偏差检查）

```text
BUSINESS DECISION READY / IDENTITY DECISION NOT READY
```

- **Business Question（可现在回答）**："OtherInbound 的业务对象是什么？"、"菜品追溯码出库是否允许影响 material 库存？"、"inventory 的业务对象范围是 A/B/C/D？"——这些是**业务意图声明**，证据已足。
- **Identity Question（不可现在回答）**："Material 是否是整个 Inventory 系统唯一、稳定、跨域的 Identity？"——证据不足：Production BLOCKED、FM-001～004 OPEN、H1/H2 NOT_MADE、Required Identity Object Set NOT_STARTED、Requirement-driven INCOMPLETE。
- 明确禁止的推论：OQ-B/Q1 的回答**不自动等于** Identity Decision；A2 的 value-domain 血证**不自动等于**"Material 是业务真相"。

---

## 11. Governance Stop Criterion

```text
STOP_AND_ENTER_OWNER
```

（排除其余四项的理由见 §14 Why；HOLD 被排除：不存在"会改变治理决策的关键证据缺口"——生产未知与 4/50 异常只影响决策的注释列，不改变问题本身；A3/A4 无定义或已被回答。）

---

## 12. Anti-Governance-Inflation Check

1. **文档数量是否超过实际决策需要？** 是的临界已到：215+ 文件中，本链条（Reconstruction→A1→A2→Closure）每步都有决策贡献，但**边际贡献已归零**——下一步任何工程调查都不再改变待决问题集合。
2. **实质必要的既有步骤**：Reconstruction（基线）、A1（locator 粒度+运行期验证）、A2（值域+provenance）、Failure Closure（激活范围收窄）、Owner Decision Request 001（裁决入口）。
3. **重复审查的步骤**：R2 的跨攻击改判（A2 原判 FAIL-via-cross，V2 已划归 F1 承载）显示交叉判定在报告间产生了重复表述；继续加包会加剧。
4. **无 stop criterion 的未来步骤**：A3/A4（无定义 → 范围无界）；任何"再攻击一轮"的惯性提议。
5. **删除 A3/A4 会失去什么？** 决策相关：零。唯一损失是若干潜在工程缺陷的提前曝光——而那些属于**修复工程**范畴，不改变 Owner 现在要回答的问题。
6. **若保留，必须解决什么决策不确定性？** 回答：无。无法指出任何一个"其结果会改变 Q1/OQ-A/OQ-B 表述"的 A3/A4 子问题——这正是 §7/§8 表格的结论。

---

## 13. No New Attack Package（合规声明）

本任务未创建 A3/A4、未新建 Closure、未新增 fixture、未修改代码/数据库/schema/migration/Owner decision/Identity decision。唯一工程动作 = §6 的只读 source-level 可审计性核验（任务明示允许）。

---

## 14. Required Final Conclusion

### Current Governance State

```text
Reconstruction      = COMPLETED（A1 复核通过；TraceCode/Warehouse 两处治理修复项已登记）
A1                  = PASS_WITH_REPAIRS（运行期验证；UNRESOLVED_ATTACK = NO）
A2                  = FAIL（R1 INCONCLUSIVE / R2 PASS / R3 NARROWED / R4 CONFIRMED-FAIL / R5 PASS；UNRESOLVED_ATTACK = YES）
A2 Failure Closure  = F1 NARROWED / F2 CONFIRMED → A2 FAIL remains
Identity Decision   = NOT MADE
Schema/Migration    = NO AUTHORIZATION
Production Evidence = BLOCKED
Owner Decision      = Q1/Q2/Q3 + OQ-A/OQ-B 候选就绪，等待治理层提交
```

### Recommended Next Step

```text
STOP_AND_ENTER_OWNER
```

### Why（证据支持）

1. **A1 已以运行期实验关闭**：locator 唯一性经 MP 3.5.5 真实运行时验证（getProductId/getStoreId 抛异常、getMaterialId 正常解析），非静态推断。
2. **A2 已完成且缺陷类封闭**：恰两个缺陷类（F1 多态消费无 discriminator=NARROWED；F2 product→material 值域错配=CONFIRMED），全部有 source 级证据与四层状态；无未命名的攻击残留（R1 的 INCONCLUSIVE 本质是 Owner 问题而非工程问题）。
3. **Owner 裁决所需证据已足**：Decision Request 001 自认 evidence pack 成立；A2/Closure 追加的血证（FK 不对称、同值异指、product_id 列零读写、PARTIALLY_AUDITABLE）只增不减；OQ-A/OQ-B/Q1 三问均为 READY（Q1 带 provenance caveat）。
4. **A3 的唯一可定义问题已被 A1 回答**（storeId/inventoryType 持久性——运行期+源码+数据三重）；A4 无定义（NOT_AVAILABLE）且按语义面评估为 DUPLICATIVE——两者的可能结果**无法改变任何待决决策**。
5. **剩余未知全部不是"缺一轮攻击"**：生产分布（BLOCKED）、4/50 异常指称、999999 来源——要么属生产证据轨道，要么属 Owner 裁决后的修复工程；继续调查只增加注释，不减少决策不确定性。
6. **Identity Decision 的阻塞项不是 A3/A4**：是 FM-001～004、H1/H2、Required Set、Production Gate——这些在 Owner Decision 之前既不该做也不能做。

---

## 15. Governance Principle（合规自检）

> Governance is not the accumulation of evidence. Governance is the reduction of decision uncertainty.

本报告的结论即该原则的执行：停止积累（A3/A4 = 零决策价值），把已闭合的证据交给唯一的下一个决策者（Business Owner）。

> Implementation is evidence of what the system does, not proof of what the system should mean.

全部 ENGINEERING FACT 与 SEMANTIC FINDING 已分层；OQ 均以问题形式提交，未代答。

> Absence of evidence of failure is not evidence of correctness.

R1 的 INCONCLUSIVE、R3/R4 的"本地未观察"均如实保留，未转为 PASS 依据。

> A governance step must justify its existence by demonstrating that its possible outcomes can change a downstream decision.

A3/A4 未通过该检验（§7/§8 表格逐项为 NONE/LOW）；STOP_AND_ENTER_OWNER 通过（Owner 的每个回答都会实质改变下游：E2/E4 重定级、修复方向、Rule Candidate 处置）。

---

*本报告为评估记录。历史文件未修改。执行到此停止——不进入 A3/A4，不代 Owner 作答，不做 Schema/Migration。*
