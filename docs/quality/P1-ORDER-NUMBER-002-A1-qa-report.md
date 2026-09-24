# P1-ORDER-NUMBER-002 / A1 — 独立 QA 验收报告

> **结论：PASS_WITH_LIMITATION**
> **角色**: 独立 QA（与开发隔离；本报告为唯一可写产物）
> **日期**: 2026-09-23
> **验收对象**: `buildOrderEntity` 内 `order.setOrderNumber(orderCode)` 单行（A1）+ 单测 `OrderNewServiceImplOrderNumberA1Test`
> **Scope**: `docs/architecture/03-review/p1-order-number-scope-002.md`（FROZEN）
> **实施记录**: `docs/architecture/03-review/p1-order-number-002-implementation-record-001.md`
> **环境**: local — 后端 `127.0.0.1:8081`，context-path `/api`；DB `food_traceability`（psql 18）
> **Production Evidence**: `PROVISIONAL_PENDING_PRODUCTION_EVIDENCE`（生产不可访问；本报告全部结论基于本地独立复核）

---

## 0. 结论摘要

| 项 | 结果 |
|---|---|
| 功能 H-01~H-06 | 6/6 PASS（QA 独立复跑，非采信 developer 自测） |
| CC-1~CC-9 | 全 PASS；其中 **CC-7 = PASS_WITH_LIMITATION**（回滚窗口内 API 探针证据缺失） |
| 边界（generateOrderCode/Schema/POS/P0） | PASS（git 三提交仅触及 2 文件、业务侧仅 +1 行，独立 diff 核对） |
| 单测 A1 | 独立复跑 **3/3 PASS**（mvn，BUILD SUCCESS） |
| CC-6 33 套件 | 独立复跑 **33/33 PASS**（DeductTest 28 + Integration 4 + SelfFailure 1） |
| Git 提交链 | `7f40ab7 → 6f0f2b9(revert) → 0acf99b(reapply)` 独立核对一致 |
| FAIL 项 | **无** → 无需返回开发的 `-R{n}` 编号 |
| 是否可进回归 | **可进回归**（限制项不构成回归阻断；生产放行仍待生产证据 + Release Gate） |

---

## 1. 逐项验收矩阵（H-01~H-06 + CC + 边界）

| ID | 项 | QA 独立结论 | 复核方式（QA 自执行） | 关键证据 |
|---|---|---|---|---|
| **H-01** | `POST /api/v1/orders`：order_number = order_code 非空唯一 | **PASS** | 登录后真实创建订单 `ORD202609231710330001`（orderId=2102687050529816577）；psql 查 `order_code=order_number=t, nonempty=t`；全表 `total=30, with_on=30, null_on=0, distinct_on=30` | 本报告 §2.1；DB 查询输出 |
| **H-02** | POS `/v1/pos/orders/order`：T 码双写保持 | **PASS** | 真实创建 `T20260923002`（orderId=O1790153488596）；psql 查 `order_code=T20260923002, order_number=T20260923002, eq=t` | §2.2 |
| **H-03** | 快速单 `/v1/pos/orders/create`：独立执行，order_number 非空 | **PASS** | 独立调用（非并入 H-01）：`ORD202609231710430001`（orderId=2102687091873071105）；psql `order_code=order_number=t` | §2.3 |
| **H-04** | 重复 order_number：DB UNIQUE 拒绝 | **PASS** | psql 插入重复 `order_number=ORD202609231710330001` → `错误: 重复键违反唯一约束 "orders_order_number_key"`，exit=1 | §2.4 |
| **H-05** | POS 创建后 KDS 可见（orderNumber 回传） | **PASS** | `GET /api/v1/kitchen/orders/list?page=1&size=50` 命中 `T20260923002`，`kitchenOrderId=KO1790153488595, id=79, status=pending`；`status=pending` 过滤亦命中 | §2.5 |
| **H-06** | 托盘 bind → scan-kitchen-in → scan-kitchen-out → scan-serve → material_consumed | **PASS**（带限制 L-03，见 §5） | 全新 POS 单 `T20260923003`/`O1790153488598`/`KO1790153488597`（**零数据补丁**）：bind=bound → kin=making → kout=ready → serve code=0/served；DB：`material_consumed=1`，`store_inventory` 生菜 8.900→8.800，`store_inventory_log` 备注 `KDS出餐扣料 - 订单:T20260923003`；托盘回 idle | §2.6 |
| **CC-1a** | H-01 子项 | **PASS** | 见 H-01 | — |
| **CC-1b** | H-03 子项（E1c 独立，不并入 1a） | **PASS** | H-03 单独发起、单独 SQL 断言 | — |
| **CC-2** | order_code 语义不变 | **PASS** | `git diff 7f40ab7^..0acf99b -- OrderNewServiceImpl.java` 仅 `+order.setOrderNumber(orderCode);` 一行；`generateOrderCode()`/`getMaxTodaySequence(prefix)+1` 在父提交与 HEAD 完全一致；新单均 `ORD` 前缀且 code=number | §3.1、§3.4 |
| **CC-3** | 历史订单不受影响 | **PASS（本地抽样）** | 抽样旧行 `O1789455942447/449`、`O-CONCURRENCY-TEST-…` 值保持原样；A1 链无任何 UPDATE/回填代码（仅 INSERT 赋值） | §2.7 |
| **CC-4** | POS→KDS 主链保持 | **PASS** | H-02 + H-05 独立复跑 | — |
| **CC-5** | H-01~H-06 全 PASS | **PASS（6/6）** | 上表 | — |
| **CC-6** | 33 套件 | **PASS 33/33** | QA 独立执行 `mvn -Dtest='OrderNewServiceImplDeductTest,MaterialDeductionAuditIntegrationTest,MaterialDeductionAuditSelfFailureIntegrationTest' test` → `Tests run: 33, Failures: 0, Errors: 0, BUILD SUCCESS` | §2.8 |
| **CC-7** | 回滚演练五 Phase 真实执行 | **PASS_WITH_LIMITATION**（限制 L-02） | git 独立核对：`7f40ab7`(16:43:59, +1行+205行测试) → `6f0f2b9`(16:45:04, revert 精确 -1/-205) → `0acf99b`(16:46:55, reapply +1/+205)；Phase3 证据 `05-buildOrderEntity-method-after-rollback.txt` 无 `setOrderNumber`、`05-a1-test-file-exists.txt=False`；Phase5 `07-…final-head.txt` 含 A1 行。**限制**：回滚窗口内无 `POST /v1/orders` API 探针/POS 回归证据（§11 字面要求） | §2.9、§5 L-02 |
| **CC-8** | P2 residual 登记保持 | **PASS** | Scope-002 §G1.3 登记表仍在（H-09/H-10/P2-A/B/C = RESIDUAL_REGISTERED）；A1 提交未触碰 P2 相关代码 | Scope 文本 |
| **CC-9** | requiredQty≤0 pending 合规保持 | **PASS** | `OrderNewServiceImplDeductTest:485` 保留 `PENDING_BUSINESS_DECISION` 标记；`product-decision-backlog.md` PD-043 条目未动；A1 链无相关测试/语义改动 | §3.3 |
| **边界** | 不改 generateOrderCode/MAX+1、Schema/Flyway、POS 语义、P0 扣料逻辑 | **PASS** | `git diff --name-only 7f40ab7^..0acf99b` 仅 2 文件（service + test）；`-- backend/src/main/resources`（Flyway）为空；POS `PosOrderCreateServiceImpl`、扣料核心逻辑零改动 | §3 |

---

## 2. QA 独立复核明细（复现方法）

### 2.1 H-01
```text
POST http://127.0.0.1:8081/api/v1/auth/login  {"username":"admin","password":"Admin@123"} → code=0
POST http://127.0.0.1:8081/api/v1/orders
  {"orderType":3,"storeId":1,"customerName":"QA-A1-TEST",
   "items":[{"productType":1,"productId":1,"productName":"生菜串","unitPrice":8000,"quantity":1}]}
  → code=0, orderId=2102687050529816577, orderCode=ORD202609231710330001
psql: SELECT order_code, order_number … WHERE order_id='2102687050529816577'
  → ORD202609231710330001 | ORD202609231710330001 | eq=t | nonempty=t
全表: total=30 with_on=30 null_on=0 distinct_on=30  → 非空且唯一
```

### 2.2 H-02（POS T 码双写）
```text
POST /api/v1/pos/orders/order  {"tableNumber":98,"orderType":"dinein",…,"items":[{"id":"1","name":"生菜串",…}]}
  → code=0, orderId=O1790153488596, orderNumber=T20260923002
psql → order_code=T20260923002 | order_number=T20260923002 | eq=t   （双写保持，POS 语义未动）
```

### 2.3 H-03（快速单，独立执行）
```text
POST /api/v1/pos/orders/create  {"orderType":3,"storeId":1,"items":[{"productId":1,"productType":1,"quantity":1}]}
  → code=0, orderId=2102687091873071105, orderCode=ORD202609231710430001
psql → order_code=order_number=ORD202609231710430001, eq=t
```

### 2.4 H-04（UNIQUE 拒绝）
```text
psql INSERT … order_number='ORD202609231710330001'（重复）
  → 错误: 重复键违反唯一约束 "orders_order_number_key"；键值"(order_number)=(ORD202609231710330001)" 已经存在；exit=1
```

### 2.5 H-05（KDS 可见）
```text
GET /api/v1/kitchen/orders/list?page=1&size=50 → code=0
  命中 orderNumber=T20260923002, kitchenOrderId=KO1790153488595, id=79, status=pending
GET …?status=pending 同样命中（orderNumber 字段回传正常）
```

### 2.6 H-06（完整托盘链，零数据补丁的全新单）
```text
前置（QA 独立查明契约）：foods 表 food_id=1 的 food_code='FD202608010001'
POST /api/v1/pos/orders/order  items[0].id='FD202608010001'
  → O1790153488598 / T20260923003；order_items.food_id=1（无需 UPDATE 补数据）
POST /api/v1/tray/bind-order   {KDS-TEST-001, O1790153488598, 80} → bound
sleep 4s → POST /api/v1/tray/scan-kitchen-in  → making
sleep 6s → POST /api/v1/tray/scan-kitchen-out → ready
sleep 3s → POST /api/v1/tray/scan-serve       → code=0, status=served
DB 断言:
  kitchen_order KO1790153488597: status=served, material_consumed=1, material_consume_time=17:12:29
  store_inventory 生菜: 8.900 → 8.800（version 3→4）
  store_inventory_log id=51: "KDS出餐扣料 - 订单:T20260923003", before 8.900 after 8.800
  tray KDS-TEST-001 → idle（GET /api/v1/tray/KDS-TEST-001）
```
说明：`scan-serve` 响应体 `materialConsumed=0` 为**响应实体未回读的既有显示问题**，DB 断言为准（developer 证据中同样现象）。

### 2.7 CC-3 历史抽样
```text
旧单 O1789455942447/449（T20260915011/012）、O-CONCURRENCY-TEST-…（TEST-NUM-…）值保持不变；
A1 三提交无 UPDATE/回填语句（buildOrderEntity 仅 INSERT 前赋值）。
```

### 2.8 单测与 CC-6（QA 独立执行，workdir=backend，JAVA_HOME=H:\jdk-25.0.1.8-hotspot）
```text
mvn -Dtest=OrderNewServiceImplOrderNumberA1Test test
  → Tests run: 3, Failures: 0, Errors: 0, Skipped: 0; BUILD SUCCESS; exit=0
mvn -Dtest='OrderNewServiceImplDeductTest,MaterialDeductionAuditIntegrationTest,MaterialDeductionAuditSelfFailureIntegrationTest' test
  → MaterialDeductionAuditIntegrationTest 4/4; SelfFailure 1/1; DeductTest 28/28
  → Tests run: 33, Failures: 0, Errors: 0; BUILD SUCCESS; exit=0
```

### 2.9 CC-7 Git 提交链（QA 独立核对）
```text
git show 7f40ab7 → OrderNewServiceImpl +1 行（+order.setOrderNumber(orderCode);）+ 测试 +205 行；仅 2 文件
git show 6f0f2b9 → "This reverts commit 7f40ab7…"，-1/-205
git show 0acf99b → "This reverts commit 6f0f2b9…"（reapply），+1/+205
时间序: 16:43:59 → 16:45:04 → 16:46:55（实施→立即回滚→重施）
HEAD buildOrderEntity 恰含 1 处 order.setOrderNumber(orderCode)（紧随 setOrderCode），与工作区一致
证据: evidence/05-*（回滚后无 A1 行、测试文件不存在=False）、07-*（重施后含 A1 行）与 git 重建状态一致
```

---

## 3. 边界与 Scope 合规

| 检查 | 方法 | 结果 |
|---|---|---|
| A1 链仅触及 2 文件 | `git diff --name-only 7f40ab7^..0acf99b` | 仅 `OrderNewServiceImpl.java` + `OrderNewServiceImplOrderNumberA1Test.java` ✓ |
| 业务改动恰为单行 | `git diff 7f40ab7^ 0acf99b -- OrderNewServiceImpl.java` | 仅 `+order.setOrderNumber(orderCode);` ✓ |
| generateOrderCode / MAX+1 未改 | 父提交 vs HEAD 文本比对（`generateOrderCode`、`getMaxTodaySequence(prefix)+1`） | 完全一致 ✓ |
| Schema/Flyway/V999 未改 | `git diff … -- backend/src/main/resources` | 空 ✓ |
| POS 语义未改 | `PosOrderCreateServiceImpl` 不在提交文件列表 | ✓（H-02 双写实测保持） |
| P0 扣料逻辑未改 | 扣料核心（deductMaterialsForServe 等）不在 A1 单行内；CC-6 33/33 复跑 | ✓ |
| CC-9 / PD-043 未越权裁定 | `OrderNewServiceImplDeductTest:485` 保留 PENDING 标记；PD-043 条目未动 | ✓ |
| Scope FROZEN 内容未被 A1 实质改写 | Scope §10/§12 矩阵与验收口径一致；A1 提交不含 docs | ✓（另见 L-04 关于未入库） |

---

## 4. 已知单列项（OBS）独立核实

| ID | developer 报告 | QA 独立核实 | 裁定 |
|---|---|---|---|
| **OBS-01** | POS 单 food_id 空 → 扣料失败 `ITEM_FOOD_ID_NULL`；补数据后 H-06 PASS；NOT_CAUSED_BY_A1 | **属实且复现**：QA 用数值 id `id="1"` 创建的 `O1790153488596` 的 `order_items.food_id = NULL`（psql 实测）；根因在 `PosOrderCreateServiceImpl`（W1-EC-01 映射：`foodCodeToIdMap.get(item.id)` 找不到仅 `log.warn` 后**仍插入 null**，约 :448-456）；POS 创建路径**不经** `buildOrderEntity`，A1 三提交未触及该文件 → 归因 A1 不成立。另证：改用 `foodCode` 载荷的全新单 `O1790153488598` **food_id=1、零补丁、扣料成功** | **NOT_CAUSED_BY_A1 成立**；H-06 不因数据补丁降级（QA 已用无补丁路径独立复跑 PASS）。但「数值 id 静默写 null food_id」为**既有隐患**，登记限制 L-03，建议另开卡 |
| **OBS-02** | 全仓既有测试失败（Flyway V999 等）与 A1 无关 | 未独立复跑全仓 `mvn test`（非 CC-6 定义范围）；CC-6 定义的 33 套件已 33/33 | **UNKNOWN（范围外）**：不作为 A1 通过/不通过依据；A1 链未触碰 V999/Flyway（git 实证） |
| **OBS-03** | `OrderVO` 无 `orderNumber` 字段，order_number 断言靠 SQL | 实测确认：`OrderVO.java` 仅 `orderCode`（无 `orderNumber` 属性）；H-01/H-03 断言以 SQL 落证 | **接受为非 A1**（VO 形状为既有设计；A1 不扩面改 VO 符合边界）。登记限制 L-05 |
| H-06 首次 500 后 `UPDATE order_items SET food_id=1` | 是否降级 H-06 | 该补丁修正的是**测试载荷误用数值 id** 造成的既有映射空值；A1 范围内 H-06 判据（P0 扣料行为保持）已由 QA **无补丁路径**独立复现 PASS；developer 当次运行的补丁不构成对 A1 的豁免依据，也不构成 FAIL | **不降级 H-06 结论**；补丁事实与 OBS-01 一并登记为限制/建议 |

---

## 5. 风险与限制（PASS_WITH_LIMITATION 依据）

| 编号 | 限制 | 原因/条件 | 影响 | 是否阻断发布 |
|---|---|---|---|---|
| **L-01** | 生产证据缺失 | 生产环境不可访问 | 全部结论为 local，状态 `PROVISIONAL_PENDING_PRODUCTION_EVIDENCE` | **是（对生产放行）**——须生产侧抽验 H-01/H-02/H-04 后由 Release Gate 裁定 |
| **L-02** | CC-7 回滚窗口内无 API 探针证据 | Scope §11 要求窗口内 `POST /v1/orders` 回到已知失败形态 + POS 1 条回归 + 数据计数比对；evidence `03/05-*` 仅有 git/代码级验证（§3.1 自述「HTTP 层见 §4」）。窗口已过，无法追溯补测 | 回滚演练的「代码形态」已被 git 链+证据独立证真（无 DDL、无悬空形态风险低）；但 §11 字面要求未完全满足 | 否（残余风险低）；建议回归/下轮演练补窗口内 HTTP 证据 |
| **L-03** | POS 数值 id → food_id 静默 null → 扣料必败（既有） | `PosOrderCreateServiceImpl` foodCode 映射失败仅 warn 不阻断插入；QA 新单 `O1790153488596` 实测 food_id=NULL | 若生产 POS 客户端发送数值 id，出餐扣料将失败（500 + FAILED 审计）；**与 A1 无关**但影响 H-06 真实成功率 | 否（非 A1 引入、非本卡 GATE）；**建议另立 P 卡**（如 P1-POS-FOODID-MAP-001，编号由 planner 定） |
| **L-04** | Scope-002 / 实施记录 / 证据目录均未入库（`git status = ??`） | 三者为 untracked 文件 | FROZEN 文档「冻结后不可改」无法用 git 证明（当前内容与验收口径一致，仅能内容核对） | 否；建议尽快入库固化（架构/规划侧动作） |
| **L-05** | `OrderVO` 无 `orderNumber`；`scan-serve` 响应 `materialConsumed` 不回读 | 既有 VO/实体返回形状 | HTTP 层 order_number 断言依赖 SQL；H-06 响应字段与 DB 不一致（DB=1 为准） | 否（既有显示问题，不扩面） |
| **UNKNOWN** | OBS-02 全仓测试失败清单 | 未独立复跑全仓测试 | 不知是否含 A1 相关新增失败（A1 链资源零改动使风险极低，但证据上未证明） | 条件：若回归轮全仓跑测，须按「既有失败基线」比对确认无**新增**失败 |

---

## 6. 证据引用（相对路径）

| 类别 | 路径 |
|---|---|
| Scope（FROZEN） | `docs/architecture/03-review/p1-order-number-scope-002.md` |
| 实施记录 | `docs/architecture/03-review/p1-order-number-002-implementation-record-001.md` |
| Developer 证据目录 | `docs/architecture/03-review/p1-order-number-002-evidence/`（`SUMMARY.txt`、`01/02/03/04/05/06/07-*`、`h00`–`h06`、`git-log.txt`、`final-*`） |
| 代码 | `backend/src/main/java/com/foodtraceability/service/impl/OrderNewServiceImpl.java`（`buildOrderEntity`，`order.setOrderNumber(orderCode)` 恰 1 处） |
| 测试 | `backend/src/test/java/com/foodtraceability/service/impl/OrderNewServiceImplOrderNumberA1Test.java` |
| Git 提交 | `7f40ab7` → `6f0f2b9` → `0acf99b`（`git show` 独立核对） |
| QA 独立执行输出 | 本报告 §2（HTTP 响应、psql 查询、mvn 输出均为 QA 会话内实时执行；临时产物在系统临时目录，不入库） |

---

## 7. 任务状态与回归判定

| 项 | 值 |
|---|---|
| 任务结论 | **PASS_WITH_LIMITATION** |
| 任务状态建议 | `P1-ORDER-NUMBER-002 / A1` → **QA 通过（带限制）**；限制 L-01~L-05 登记 `production-known-limitations`（planner 动作） |
| FAIL / 返回开发编号 | **无**（未发现 A1 范围内 FAIL；L-03 建议另立新卡，非本卡 `-R{n}`） |
| 是否可进回归 | **可进回归**——H-01~H-06 与 CC-6 已由 QA 独立复现，限制项均不构成回归基线阻断；回归轮需执行：① 既有失败基线比对（UNKNOWN 项）② 生产抽验后解除 L-01 |
| Release Gate | 生产证据未到 → 维持 `PROVISIONAL_PENDING_PRODUCTION_EVIDENCE`，**本报告不构成生产发布批准** |

---

**签署**: 独立 QA 会话（开发与验收隔离；仅写 `docs/quality/`）
**结论（唯一）**: **PASS_WITH_LIMITATION**
