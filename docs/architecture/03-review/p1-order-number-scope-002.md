# P1-ORDER-NUMBER — Scope Amendment 002（启动前修正 / Pre-Implementation Gate）

> **DOCUMENT STATUS:** `FROZEN` — 唯一有效 Scope（Scope-002 = FROZEN）  
> **SUPERSEDES:** `docs/architecture/03-review/p1-order-number-scope-001.md`  
> **任务编号**: P1-ORDER-NUMBER-002（Amendment；原 001 → SUPERSEDED）  
> **前置**: P0-KDS-DEDUCT 已 `CLOSED_WITH_REGISTERED_RISKS`（`p0-kds-deduct-closure-001.md`）——**不重开**  
> **阶段**: Final IR **PASS** → `READY_FOR_SCOPE_FREEZE` → Scope Freeze（独立、可记录的治理动作）→ **`Frozen`（已执行）**；再经**实施轮正式选定**后，才允许 `order_number` 实施（Final IR PASS ≠ Scope Freeze ≠ Frozen ≠ Implementation Approved；**Final IR PASS 本身不构成 Implementation Approved**）  
> **Final Independent Review**: **PASS**（F-01/F-02/F-03 = PASS；六项既有 regression = PASS）  
> **本轮性质**: 记录 Final IR PASS；进入 `READY_FOR_SCOPE_FREEZE`；执行独立 Scope Freeze → `FROZEN`；**零业务代码 / 零 schema / 零 Flyway / 零 P0 / 零 requiredQty / 零 P2 修复；不进入 Implementation**  
> **Evidence discipline**: FACT / INFERENCE / RECOMMENDATION / PENDING / UNKNOWN 分列；本地 ≠ 生产  
> **Production Evidence**: `PROVISIONAL_PENDING_PRODUCTION_EVIDENCE`（生产不可访问）  
> **Scope 当前状态**: `FROZEN`（路径：`ESTABLISHED_AMENDED` → `READY_FOR_SCOPE_FREEZE` → Scope Freeze → `FROZEN`；Implementation = `NOT STARTED` / `BLOCKED_PENDING_IMPLEMENTATION_ROUND`；Implementation Approved = **NO**）  
> **Freeze 记录**: Freeze 时间 `2026-09-23T15:55:44+08:00`；动作 = 独立 Scope Freeze（非 IR PASS 自动冻结）；标识 = `P1-ORDER-NUMBER-002 / Scope-002 / FREEZE_ID=P1-OSN-002-FREEZE-20260923T155544+0800`  
> **修订记录**: 2026-09-23 DS Independent Review 后「六项治理澄清」；F-01/F-02/F-03 wording closure；2026-09-23 Final IR PASS + READY_FOR_SCOPE_FREEZE + Scope Freeze → FROZEN；**不新建 Scope-003、不恢复 001、不进入实施、不改业务/DB/P0/P2/PD-043**

---

## 状态语义字典（本文件强制）

| 符号/状态 | 含义 | 不表示 |
|---|---|---|
| `NOT_YET_SATISFIED` | 尚未实施/尚未执行验证，**证据上未满足** | 不是「实施后验证失败」 |
| `FAIL` | **已实施或已执行验证后**，结果证明不满足 | 不是「还没做」 |
| `PASS` | 已执行验证且满足判据 | — |
| `RESIDUAL_REGISTERED` | P2 residual 已按 CC-8 登记 | **不是** P1 功能完成 |
| `UNKNOWN` / `PENDING_CONFIRMATION` | 证据不足，结论未定 | 不是「已解决」也不是「已否决」 |
| Gate 表 `✅ 已按证据纪律处理` | **审查项处理完毕** | **≠ underlying issue resolved** |

**实施状态机（全文强制，四处一致）**: `Final IR PASS → READY_FOR_SCOPE_FREEZE → Scope Freeze（独立、可记录）→ Frozen → 实施轮正式选定 → Implementation`。  
**「继承」语义（本文件强制）**: Scope-002 是当前**唯一有效 Scope**；Scope-001 已被 Scope-002 supersede。Scope-002 中「继承 Scope-001 …」仅表示**沿用所引用的 Scope-001 历史约束/内容**，**不表示** Scope-001 恢复有效，**也不恢复** Scope-001 的独立治理效力；Scope-001 保持 `SUPERSEDED`。

**FINAL_IR / FREEZE 登记（现有机制，本文件强制）**:
- Final Independent Review = **PASS**；F-01 = **PASS**；F-02 = **PASS**；F-03 = **PASS**。
- `Final IR PASS ≠ Scope Freeze ≠ Frozen ≠ Implementation Approved`；**Final IR PASS 本身不构成 Implementation Approved**。
- 状态迁移（独立治理动作分步）: `ESTABLISHED_AMENDED` → **`READY_FOR_SCOPE_FREEZE`**（仅解锁 Scope Freeze 执行权）→ **Scope Freeze（独立、可记录）** → **`Scope-002 = FROZEN`**。
- FREEZE_ID: `P1-OSN-002-FREEZE-20260923T155544+0800`；Freeze 时间: `2026-09-23T15:55:44+08:00`；Scope 标识: `P1-ORDER-NUMBER-002` / `docs/architecture/03-review/p1-order-number-scope-002.md`。
- Frozen 后: 仅在后续正式 Implementation round + 正式选定实施方案后才可按本 Scope 实施；内容变更须重新走治理流程，**不得直接编辑 Frozen 内容**。

---

## A. Gate Resolution（先于正文）

### G1 — P2 / P1 Boundary

**结论（FACT 路径追踪）**:

| P2 | 实际 HTTP / 代码路径 | 入口 | Controller/Service | 失败点 | 可复现 | 主链路 POS→KDS→Serve? | 阻塞 P1 order_number 验证? | 归属 | 情例 |
|---|---|---|---|---|---|---|---|---|---|
| **P2-A** | 前端 `frontend-kitchen/src/api/kitchenOrderApi.ts:56-138` 调 `/v1/kitchen-order/*`、`/v1/kitchen/pending-orders` 等 | 仅该模块导出；**全 frontend-kitchen `import kitchenOrderApi` = 0 命中（FACT）** | 后端无对等 `@RequestMapping("/v1/kitchen-order")` 业务 Controller；仅 `SecurityConfig.java:107-110` 残留 matcher + `JwtAuthenticationFilter:46` 路径片段 | 运行时不可达（死代码契约样本）；调用方已不存在 | **否（无调用方）**；历史 CSV `api-test-results-4modules.csv` 中部分 `/v1/kitchen-order/*` 500 为脚本直打旧前缀 | **否**——活链路为 `Home.vue:341-342` → `/v1/kitchen/scan/*`（KitchenScanController）与 `Home.vue:471-488` → `/v1/kitchen/orders/*`（KitchenOrderController:36） | **否** | **P2 独立跟踪**（死代码/契约残留清理） | **C** |
| **P2-B** | `ServeWindow.vue:258` `POST /v1/kitchen/serve-order/{id}`；`ServeWindow.vue:227` `GET /v1/kitchen/completed-orders` | ServeWindow 出餐按钮 / 列表加载 | 后端**无** `serve-order`、**无** `completed-orders` 映射；等价出餐 = `KitchenOrderController:248` `POST /v1/kitchen/orders/{id}/serve`（`@PreAuthorize kitchen:edit`） | HTTP **404**（路径不存在，非 order_number） | **是（路径静态可证 + 既有 OIC2 登记 DEV-004）**；本 Amendment 不跑活体（禁改码轮可只读实测，未作为关闭依据） | **否（非主出餐）**——主出餐 E2E = `POST /v1/tray/scan-serve`（`Scan.vue:148` / `Home.vue:435,549` → Tray 扣料，P0 已收口）；ServeWindow 为**旁路 UI 出餐** | **否**（不挡 H-01~H-06 主链）；**是** ServeWindow 专项 E2E 的 residual | **P2 独立跟踪**；Completion 记 **E2E residual blocker** | **B（旁路）+ C（对 P1 主链）** → 裁决：**对 P1 主链 = C；对全 UI 出餐面 = 显式 residual（不得计入 P1 FAIL）** |
| **P2-C** | KDS 写：`KitchenOrderController` receive/start-make/complete-make/serve/cancel 等 `@PreAuthorize("hasAuthority('kitchen:edit') or hasAuthority('*')")`（:61,:136,:157,:178,:250,:271…） | 后厨前端携带 JWT | Spring Security 方法级鉴权；`SecurityConfig` 对 `/v1/kitchen-order/**` POST=authenticated（僵尸 matcher，非现用前缀） | 缺 `kitchen:edit`（或无 token）→ **403**；与 order_number **无关** | 鉴权矩阵可静态证；活体 403 未在本轮执行（禁止顺手改码；非 P1 关闭前提） | 写操作在主链「KDS 接单→制作→完成」上；**order_number 不经这些写路径生成** | **否**（P1 H-01~H-04/H-06 不依赖绕过 KDS 写鉴权；H-05 列表为读） | **P2/安全独立**（鉴权设计缺口，非 order_number） | **C**（对 P1 Completion） |

**主链路逐步判定（FACT）**:

```text
POS POST /v1/pos/orders/order
  → PosOrderCreateServiceImpl:285-286 双写 orderCode+orderNumber  → 不被 NOT NULL 阻断
Order（orders.order_number NOT NULL UNIQUE, postgres-schema-v0.12.sql:523）
  → POS 写入 kitchen_order.order_number（PosOrderCreateServiceImpl:490）
KDS 列表/流转
  → Home/Scan 已对齐 /v1/kitchen/scan/* 与 /v1/kitchen/orders/*  → P2-A 不在活链
Serve（主）
  → POST /v1/tray/scan-serve → TrayServiceImpl → deductMaterialsForServe  → P0 已收口
Serve（旁路 UI）
  → ServeWindow /v1/kitchen/serve-order/{id} 404  → P2-B residual，非 order_number
Material Deduction / Audit
  → P0 CLOSED_WITH_REGISTERED_RISKS（KL-065~067）  → 非本 P1
非 POS 创建
  → POST /v1/orders → createOrder:114-147 → buildOrderEntity:1816-1838 仅 setOrderCode
  → INSERT 违约 order_number NOT NULL  → E1b 主阻断 → 本 P1
```

**对 Completion Criteria 的影响（裁决 + DS 澄清：H-09/H-10 / CC 自洽）**:

#### G1.1 H-09 / H-10 路径归属（FACT）

| ID | 路径 | 根因类型 | 是否经过 `buildOrderEntity` 缺 `setOrderNumber` + `orders.order_number` NOT NULL？ | 归属 |
|---|---|---|---|---|
| **H-09** | `ServeWindow.vue:258` `POST /v1/kitchen/serve-order/{id}` → 后端**无映射** → 404 | **路径名缺失**（等价端点 `KitchenOrderController:248` `/{id}/serve` 存在） | **否** | **P2-B residual** |
| **H-10** | `kitchenOrderApi.ts` `/v1/kitchen-order/*` 旧前缀 | **死代码/前缀漂移**（import=0；无对等 Controller） | **否** | **P2-A residual** |
| H-01/H-03（对照） | `POST /v1/orders` / 快速单 → `createOrder` → `buildOrderEntity` | **order_number 未赋值 + NOT NULL** | **是** | **P1 必须 gate（CC-1）** |

#### G1.2 为何 H-09/H-10 不构成 P1 `order_number` 实施的 FAIL（机械规则）

```text
R-BLOCKER-ONLY:
  P1 功能 FAIL ⇔ 失败点 ∈ {order_number 未赋值, orders.order_number NOT NULL 违约,
                          由本 P1 修复引入的回归}
  H-09/H-10 失败点 = 路径 404 / 死代码前缀
  ∴ H-09/H-10 结果 ∈ {404, 未调用} ⇔ 不落入 R-BLOCKER-ONLY
  ∴ 不得记为 P1 FAIL
```

**禁止**将 H-09/H-10 的 404/断链写入 CC-1~CC-7/CC-9 的 FAIL 格。

#### G1.3 P1 Completion vs P2 residual —— 机械判定规则

定义两个互斥集合（本 Scope 唯一口径）：

| 集合 | 成员 | 关闭含义 |
|---|---|---|
| **GATE**（P1 必须满足） | **CC-1, CC-2, CC-3, CC-4, CC-5, CC-6, CC-7, CC-9** | 全部满足才允许宣称 P1 功能完成 |
| **REG**（residual 登记机制） | **CC-8**（登记 P2-A/B/C 与 H-09/H-10 等已知非 order_number 残余） | 仅证明「残余已如实登记」；**不要求修好 P2** |

```text
MECHANICAL_RULE_P1_COMPLETION:
  P1 Completion :=
      ∀ c ∈ GATE: state(c) ∈ {PASS}          # 功能/回归/回滚/边界门禁
    ∧ state(CC-8) = PASS                      # 登记合规（非功能完成）
    ∧ ∀ h ∈ {H-09, H-10}: 记为 RESIDUAL_REGISTERED，且 ∉ GATE 的 FAIL 源

  P1 NOT COMPLETABLE if:
      ∃ c ∈ GATE: state(c) ∈ {FAIL, NOT_YET_SATISFIED}
    ∨ state(CC-8) = FAIL                      # 登记缺失/残余被标成 P1 PASS/被静默删除

  禁止模式:
    「E2E 没有全部通过，但 P1 又被宣布完成」
      ⇔ 仅当被宣称「未通过」的用例 ∈ GATE 且 state=FAIL 时矛盾
      ⇔ H-09/H-10 ∈ REG，其 404 ∈ RESIDUAL_REGISTERED，不产生该矛盾
    「整体 E2E 通过 / 基本通过 / 不影响整体 / 视情况处理」→ 本文件禁用
```

**允许的关闭表述（唯一模板）**:

```text
P1-ORDER-NUMBER Completion:
  GATE CC-1..CC-7, CC-9 = PASS
  CC-8 = PASS（P2 residual 已登记：H-09, H-10, P2-A/B/C — 见登记表）
  H-09/H-10 = RESIDUAL_REGISTERED（非 P1 completion gate；不计入 P1 FAIL）
  P2-A/B/C = 独立跟踪，不在本卡关闭范围内
```

**residual 是否允许 P1 关闭**: **允许**——在且仅在 CC-8=PASS（登记完整）且 GATE 全 PASS 时；residual **永不**豁免 GATE 中任何一条（见 §12.1）。

**登记表（CC-8 强制内容）**:

| residual ID | 路径/现象 | 状态 | 是否 P1 gate | 独立卡 |
|---|---|---|---|---|
| H-09 / P2-B | ServeWindow `serve-order`、`completed-orders` → 404 | `RESIDUAL_REGISTERED`（已知 404；P2-B 未修，预期保持 404） | **否** | P2-B |
| H-10 / P2-A | `kitchenOrderApi` `/v1/kitchen-order/*` 死代码 | `RESIDUAL_REGISTERED`（静态） | **否** | P2-A |
| P2-C | KDS 写 `kitchen:edit` 鉴权 | `RESIDUAL_REGISTERED`（独立） | **否** | P2/安全 |

**禁止**把 P2-A/B/C 修复塞进本 P1 实施边界（§实施边界）。

---

### G2 — E1 Definition

**结论**: 仓库**原无**完整 E1a/E1b/E1c 编号体系（仅 `p1-order-number-scope-001.md` 使用「E1」「E1b」且 E1a 缺失）。本轮按实际扫描建立**最小定义**如下；后续只增不改语义。

| ID | 检查对象 | HTTP / Code Path | implementation root cause | 当前状态（证据纪律） | 实际阻断点 | 证据 | 归属 | verification requirement | 独立 regression verification? |
|---|---|---|---|---|---|---|---|---|---|
| **E1a** | POS 主创建 | `POST /v1/pos/orders/order` → `PosOrderCreateServiceImpl:283-286` **双写** | 已正确双写（非缺陷） | **PASS**（既有行为，不被 order_number 阻断） | 无 | `PosOrderCreateServiceImpl.java:285-286` | **非修复对象**（回归保护） | 实施后保持双写不回归 | **是（回归）**：H-02 + CC-2/CC-4 |
| **E1b** | 通用创建 | `POST /v1/orders` → `OrderNewController:37-41` → `createOrder:114-130` → `buildOrderEntity:1816-1838` **无 `setOrderNumber`** | `buildOrderEntity` 缺 `setOrderNumber` + `orders.order_number` NOT NULL | **NOT_YET_SATISFIED**（未实施；当前代码路径会阻断——**非**「已验证 FAIL」） | NOT NULL + 未赋值 → INSERT 违约 | `OrderNewController.java:21,37-41`；`OrderNewServiceImpl.java:126-130,1816-1838`；`postgres-schema-v0.12.sql:523` | **P1 主阻断** | 实施后 H-01 PASS → CC-1(E1b 部分) | **是**：H-01 独立执行 |
| **E1c** | 快速单 | `createPosQuickOrder` → **委托** `createOrder`（与 E1b **共享** root cause） | **同 E1b**（同一 `buildOrderEntity` 缺失赋值） | **NOT_YET_SATISFIED**（同 E1b，未实施） | 同 E1b | Scope001 §3 证据④（`createPosQuickOrder:179`→createOrder） | **P1；共享 A 实现修复点** | **独立**：实施后必须单独跑 H-03，不得仅以 H-01 PASS 推定 E1c PASS | **是（强制独立）**：H-03 必须单独记录；当前**无**「单测/集成已完整覆盖 E1c 入口」的既有证据（UNKNOWN→实施时补证） |
| E2…E8 | 支付/KDS/…/E2E | 继承 Scope001 §3 | E8 根因=E1b | E2–E7 不被 order_number 阻断；E8=从 E1b 起 | — | Scope001 §3 | E8→P1；P2 残余→CC-8 | 见矩阵 | E8 随 E1b/E1c 验证；P2 见 CC-8 |

#### G2.1 E1b vs E1c 治理语义（DS 澄清；禁止「并入=只验 E1b」）

| 问题 | 裁决 |
|---|---|
| E1b 是什么 | `POST /v1/orders` → `createOrder` → `buildOrderEntity` 缺赋值 |
| E1c 是什么 | `createPosQuickOrder` → **委托** `createOrder` → 同一 `buildOrderEntity` |
| 为何可共享 implementation | 修复点同一：在 `createOrder`/`buildOrderEntity` 正确 `setOrderNumber`（原 A）→ **一处改码覆盖两入口** |
| 是否需要独立验证 | **需要。** 共享根因 ≠ 自动证明两路径都验证完成 |
| 何时可不独立跑 E1c | **仅当**存在既有证据证明同一测试**完整覆盖** `createPosQuickOrder` 入口——当前状态 = **`UNKNOWN`（无此证据）** → 实施轮 **必须** H-03 独立执行并落盘 |
| Completion 如何记录 | CC-1 拆两条子断言：`CC-1a` = H-01（E1b）、`CC-1b` = H-03（E1c）；**二者皆 PASS** 方满足 CC-1；禁止「E1c 并入 E1b，所以只验 E1b 即 E1c PASS」 |

**为何 /v1/orders ∈ E1b 且属 P1（FACT）**:  
- 检查对象 = 「通用创建是否写出 order_number」；  
- 路径 = `@RequestMapping("/v1/orders")` + `@PostMapping` 空路径 → `POST /v1/orders`；  
- 阻断点 = NOT NULL 列 + `buildOrderEntity` 未 `setOrderNumber`；  
- 修复只需改创建路径写入 → **原定义 A**，故归属 P1。

**为何 P2-A/B/C ≠ 同一问题（FACT）**:  
- P2-A = 死代码前缀漂移（无调用方、无后端 Controller）；  
- P2-B = **路径名 404**（`serve-order` vs `orders/{id}/serve`），非 NOT NULL；  
- P2-C = **鉴权 403**（`kitchen:edit`），非 NOT NULL；  
- 三者均**不经过** `buildOrderEntity` 缺失赋值这一阻断点。

---

### G3 — KL-068 Release Blocking Scope（双状态冲突消解）

**唯一 Current State**: **`UNKNOWN / PENDING_CONFIRMATION`**  
（新读者问答：**「KL-068 现在是什么状态？」→ 见下表 Current；不是「阻断发布=是」的现行结论。**）

| 层 | 状态 | 标记 | 说明 |
|---|---|---|---|
| **Historical** | 登记时写入 `是否阻断发布 = 是（从 /v1/orders 创建起的全链路）` | **`HISTORICAL / SUPERSEDED_INTERPRETATION`**（provenance 保留，**不删除**） | `production-known-limitations.md:89` 原文；括号=功能影响面 **INFERENCE**，**不是**已批准发布政策 |
| **Current** | **`UNKNOWN / PENDING_CONFIRMATION`** | **`CURRENT`（唯一）** | 依据：无政策证据证明「是」的发布单元（整系统/含 KDS/仅 /v1 链）；亦无证据证明「不阻断」 |

| 项 | 内容 |
|---|---|
| Provenance | 2026-09-23 首次登记（P0 收口轮，planner）→ 同日 Scope-002 §G3 裁决 + known-limitations 页脚 G3 → **本轮澄清：Historical 与 Current 分列，消除双「当前状态」并存** |
| 仓库内发布规则 | `SKILL.md:161-166`：仅回归 `FAIL>0 禁止放行`——**未**定义 KL-068 类限制的发布单元 |
| KL 清单用途 | 「发布评审**输入**」≠ 自动=「阻断整个系统发布」 |
| CI/CD | **未发现**映射「阻断发布」→发布单元的政策/门禁 |
| **当前不能确认** | ① 是否阻断**整系统**发布 ② 是否仅阻断**含 KDS** 发布 ③ 是否仅阻断**/v1/orders 链路**相关版本 ④ Release Gate 是否将本 KL 计入 WAITING/FAIL |
| 下一步证据 | ① 发布政策对 KL「阻断发布」列语义与发布单元定义 ② Gate 是否计入未关闭高风险 KL ③ 发布负责人书面放行条件 |
| 禁止 | 删除历史「是」；凭空补 release policy；把 Current 写成「是」或「不阻断」 |
| 修正位置 | known-limitations 主表行 = Historical 标注 + Current 列；页脚 = Current 唯一源；task-board/roadmap 引用视为指向 Current（见各页脚） |

---

### G4 — A/B/C/D Definition Integrity（本轮**不回退**确认）

**结论**: Scope001 §4 曾发生 **B/C 漂移**；Scope-002 已恢复原始定义并**冻结**；DS 六项澄清**不重新设计 A/B/C/D、不引入 E**。

| 方案 | **原始定义（任务书，冻结）** | Scope001 漂移定义 | Scope-002 Current（冻结，不回退） |
|---|---|---|---|
| **A** | `createOrder()` correctly generates/assigns `order_number` | buildOrderEntity 补写 | **A = createOrder 正确生成/赋值 order_number**；`buildOrderEntity` 补写 = **A 的实现落点** |
| **B** | make `order_number` **nullable** | ~~统一生成器~~ | **B = order_number 改为可空** |
| **C** | **DB DEFAULT / trigger** auto-generates | ~~改可空（或 GENERATED）~~ | **C = DB DEFAULT / trigger 自动生成**；「改可空」归还 B |
| **D** | **unify/rename** `orderCode` and `orderNumber` | 合并单列 | **D = 语义统一/重命名**；合并单列=激进实现 |

**漂移纠正声明（保持）**:  
- ~~B = 统一生成器~~ → **A 的 implementation variant**（仍在 createOrder 赋值）→ **继续归入 A、不新建 E**。  
- ~~C = 改可空~~ → **原 B**；禁止 B/C 再互换。  
- **Scope 唯一性**: 001 = SUPERSEDED；002 = 唯一 ACTIVE；**禁止 Scope-003**；**禁止恢复 001 为有效**。

---

### Gate 5 — 新增 E？

**结论: 不新增 E（NO_E）——本轮维持，不重新评估。**  
理由: 「统一生成器」= A variant；可空=B；DEFAULT/trigger=C；合并/重命名=D。A/B/C/D 已覆盖。  
比较表仅 A/B/C/D；A 下 A1/A2 为实现注记。

---

### Gate 3 — 符号语义（DS 澄清）

**规则**: Gate 表中的 `✅` **一律读作**：

```text
✅ 已按证据纪律处理；结论 = <见该行结论列>
```

**≠** 「underlying issue 已解决」。

| Gate | 条件 | 处理状态（非问题解决状态） | 结论摘要 |
|---|---|---|---|
| 3 | KL-068 范围有证据或标 UNKNOWN/PENDING | **✅ 已按证据纪律处理** | **Current = UNKNOWN/PENDING_CONFIRMATION**（**问题本身未解决**） |

---

## B. Scope Amendment 正文

### 1. 文件与状态

| 项 | 值 |
|---|---|
| 新文件路径 | `docs/architecture/03-review/p1-order-number-scope-002.md` |
| 旧文件路径 | `docs/architecture/03-review/p1-order-number-scope-001.md` |
| 旧文件状态 | **`SUPERSEDED_BY_p1-order-number-scope-002.md`**（保留历史，禁止并行有效） |
| SUPERSEDES / SUPERSEDED_BY | 002 **SUPERSEDES** 001；001 **SUPERSEDED_BY** 002 |
| Scope 当前状态 | **唯一有效 = FROZEN**（002）；Final Independent Review = **PASS**；状态迁移：`ESTABLISHED_AMENDED` → **`READY_FOR_SCOPE_FREEZE`**（含义仅限：Amendment 完成 + Final IR 已 PASS + **允许**执行独立 Scope Freeze；当时尚未 Frozen、未批准 Implementation）→ 独立 Scope Freeze 动作 → **`Scope-002 = FROZEN`**；Implementation = `NOT STARTED` / Implementation Approved = **NO**；仍须**实施轮正式选定**后方可 P1 Implementation（完整状态机保留：`Final IR PASS → READY_FOR_SCOPE_FREEZE → Scope Freeze → Frozen → 实施轮正式选定 → Implementation`；IR PASS ≠ Scope Freeze ≠ Frozen ≠ Implementation Approved） |

### 2. 问题定义

| 项 | 内容 |
|---|---|
| 现象 | `orders.order_number` NOT NULL UNIQUE（`postgres-schema-v0.12.sql:523`）；非 POS 创建只写 `order_code` |
| 根因 | `createOrder` → `buildOrderEntity:1816-1838` 无 `setOrderNumber` → E1b/E1c INSERT 失败 |
| 对比 | POS `PosOrderCreateServiceImpl:285-286` 双写 → E1a PASS |
| 双字段 | `OrderNew`：`orderCode`（订单编号）vs `orderNumber`（POS 终端生成）；语义混用见 Scope001 §1（继承） |
| 影响 | ① `/v1/orders` 创建不可用 ② 展示/事件口径分裂 ③ 从 E1b 起 E2E 全阻断（E8） |

### 3. 真实影响范围

继承 Scope001 §2 **14 面审计**（DB/Entity/Mapper/Service 创建/Service POS/Controller/DTO/查询/日志/事件/测试/唯一键/外部 API/下游表），本 Amendment **不缩小**该清单；实施前仍须再扫。历史数据：`V20260707_001` 已回填方向登记——**本 P1 不回填历史**。

### 4. order_number / orderCode 关系

| 列 | DDL | 写入现状 | 本 P1 |
|---|---|---|---|
| `order_number` | NOT NULL UNIQUE | POS 写入；通用创建**不写** | A：创建时正确赋值 |
| `order_code` | 可空 | `generateOrderCode` / POS 与 orderNumber 同值 | **保持既有语义与写入**（CC-2） |

禁止本 P1 合并列（D 非选定则不动）；禁止改 NOT NULL 除非选定 B/C 且过完整比较+审查（本轮推荐仍见 §7，**推荐 ≠ 实施决定**）。

### 5. E1a / E1b / E1c

见 **§G2** 表（唯一定义源）。E2–E8 继承 Scope001 §3。

### 6. P2-A/B/C 关系

见 **§G1** 表。P2 **不纳入**本 P1 代码范围；Completion 见 §CC。

### 7. 候选方案比较（原始 A/B/C/D；无 E）

维度：业务语义 / 代码改动 / DB 改动 / 数据兼容 / 历史数据 / 唯一性 / 外部 API / 测试 / 回滚 / 生产影响 / 实际证据。

| 维度 | **A** createOrder 正确赋值 order_number | **B** order_number 改为可空 | **C** DB DEFAULT/trigger 生成 | **D** orderCode↔orderNumber 语义统一/重命名 |
|---|---|---|---|---|
| 业务语义 | 新单具备订单号；可与 orderCode 同值（A1）或独立格式（A2） | 允许无号订单存在 → **削弱**「必有订单号」 | 号由库生成，应用可不感知 → 语义迁到 DB | 单一权威号，根治双轨 |
| 代码改动范围 | **小**：`buildOrderEntity`（+ 快速单自动覆盖） | 中：应用可忽略赋值，但读路径/校验需容忍 null | 小-中：应用可不写；读路径容忍生成值 | **大**：实体/Mapper/事件/前端/报表 |
| DB 改动 | **无**（推荐 A1） | **有**：DROP/ALTER NOT NULL | **有**：DEFAULT/trigger/Flyway | **有**：列合并/重命名迁移 |
| 数据兼容 | 新写入即兼容；旧数据不动 | 旧 NOT NULL 行仍非 null，新行可 null → **双态** | 新行库生成；须防与应用双写冲突 | 需全量映射与回填策略 |
| 历史数据 | **不回填** | 不回填 | 不回填（或仅默认回填，需单列评估） | **必须**迁移策略（超出最小 P1 时禁做） |
| 唯一性 | 须确保赋值唯一（复用 orderCode 时查占用/前缀空间） | null 在 UNIQUE 下多 null 行（PG）→ 唯一性语义弱化 | DB 生成须保证唯一（序列/随机+冲突重试） | 统一后单一唯一键 |
| 外部 API 兼容 | 字段出现非空 orderNumber；POS 已有同形 | 响应/前端可能收到 null | 响应为库生成值，格式可能变 | **破坏性**契约变更风险 |
| 测试影响 | H-01/H-03 新断言；33 基线不回归 | 可空断言 + 双态 | DDL 断言 + 创建仍成功 | 全链路契约测试大增 |
| 回滚 | revert 提交即可（无 DDL） | 恢复 NOT NULL（须先清 null 或失败） | DROP DEFAULT/trigger + 迁移回滚 | 迁移回滚困难 |
| 生产影响 | **最低**；不动 schema/Flyway/V999 | 中-高：约束变更进生产 | 中-高：触发器/默认值进生产 | **高**：超出 P1 常规风险 |
| 实际证据 | `OrderNewServiceImpl:1816-1838` 无赋值；POS:285-286 双写先例；DDL:523 | DDL 现为 NOT NULL；他表存在 `DEFAULT NULL` 可空 order_number（schema 其它表 :465 等） | 仓库未见 orders 上 order_number 的 DEFAULT/trigger（FACT：v0.12 仅 NOT NULL UNIQUE 定义） | PD-015 双结构并存、orderNo↔双候选列（`sales-order-orders-mapping.md`） |

**推荐（RECOMMENDATION，非实施决定）**: **A**（A1 最小闭环；A2 仅当 A1 唯一键冲突被证实）。B/C/D **不推荐**在本 P1 执行（B/C 动生产约束且非阻断唯一解；D 归 PD-015 二期语义）。  
**实施决定**: 完整状态机 = `Final IR PASS → READY_FOR_SCOPE_FREEZE → Scope Freeze（独立、可记录）→ Frozen → 实施轮正式选定 → Implementation`，**实施轮正式选定后**才允许改码；**禁止**「IR PASS → 直接实施」捷径。**Final IR PASS 本身不构成 Implementation Approved。**

### 8. 风险

| ID | 风险 | 缓解 |
|---|---|---|
| R-P1-1 | A1 复用 orderCode 占用 orderNumber 唯一空间 | 实施前查 POS 前缀/序列冲突；H-04 |
| R-P1-2 | 双字段同值放大语义债 | 登记；不阻塞 A；D/PD-015 另案 |
| R-P1-3 | 误把 P2 residual 计为 P1 FAIL | CC-8 隔离 |
| R-P1-4 | 回滚未真实演练即关闭 | CC-7 |
| R-P1-5 | KL-068 发布范围被误读为已裁决 | G3 = PENDING_CONFIRMATION |

### 9. 生产影响（8 问，实施前书面回答）

继承 Scope001 §7 默认：① 不改 schema ② 不改 Flyway ③ 不动 V999 ④ POS 回归零行为变化期望 ⑤ 不回填历史 ⑥ 唯一键占用核查 ⑦ 不改连接池 ⑧ 回滚真实演练见 §11。**生产仍不可访问 → 全部 `PROVISIONAL_PENDING_PRODUCTION_EVIDENCE`。**

### 10. HTTP E2E 矩阵

| ID | 场景 | 期望 | 断言 |
|---|---|---|---|
| H-01 | `POST /v1/orders`（E1b） | 200 + order_number 非空 | **CC-1a** |
| H-02 | `POST /v1/pos/orders/order`（E1a） | 200；现状双写保持 | CC-2/CC-4 |
| H-03 | 快速单（E1c） | 200 + order_number 非空 | **CC-1b（独立，不并入 1a）** |
| H-04 | 重复 order_number | 拒绝 UNIQUE | CC-1 |
| H-05 | POS 创建 → KDS 列表 | 200 可见 | CC-4 |
| H-06 | 托盘 bind → scan-serve → 扣料 | P0 行为保持 | CC-6 |
| H-07 | requiredQty≤0 出餐 | **待决策** | `E2E_EXPECTATION_PENDING_BUSINESS_DECISION` + PD-043 |
| H-08 | notes 持久化 | **待决策** | 同上 |
| H-09 | ServeWindow `serve-order`（P2-B 路径） | 404 已知 | **CC-8 REG → `RESIDUAL_REGISTERED`，∉ GATE，非 P1 FAIL** |
| H-10 | kitchenOrderApi 旧前缀（P2-A） | 死代码/断链已知 | **CC-8 REG → `RESIDUAL_REGISTERED`，∉ GATE，非 P1 FAIL** |

### 11. 回滚方案与演练时机

| 项 | 内容 |
|---|---|
| **演练时机** | **实施中真实演练（硬性）** |
| 顺序 | P1 实施 → **首次实施完成** → **立即完整回滚** → **验证回滚结果** → **重新实施** → **最终验证** |
| 回滚触发 | 创建 500 / UNIQUE 冲突 / POS 回归失败 / QA FAIL |
| 回滚操作 | revert 仅创建路径实施提交（无 DDL 时）；若有 DDL 则先逆向迁移并记录 |
| 回滚验证 | API：`POST /v1/orders` 回到**已知失败形态**或回滚点形态（禁止悬空新形态）；POS 创建 1 条回归；订单状态/数据计数与回滚点一致 |
| 数据验证 | 回滚窗口内新建单可见性/是否残留半截数据；无脏写 order_number |
| 失败时 | 演练 FAIL → 停止重新实施，登记缺陷，不得关闭 P1 |
| 重新实施前置 | 演练 PASS + 记录落盘 §11.1 + 独立 QA 确认 |
| 纸面说明 | **不算证据** |
| §11.1 记录表 | 实施时填：日期/环境/步骤/结果 PASS\|FAIL/证据 sha 或日志路径 |

### 12. Completion Criteria（可枚举、可验证；与 G1 一致）

每条格式：**验证方式 → PASS → FAIL → 证据**。

| ID | 集合 | 覆盖 | 怎么验证 | PASS | FAIL（仅指已验证不满足） | 当前状态（未实施轮） | 证据 |
|---|---|---|---|---|---|---|---|
| **CC-1** | **GATE** | 新单 order_number 正确产生；**拆 CC-1a/1b** | **CC-1a**=H-01 `POST /v1/orders`；**CC-1b**=H-03 快速单（E1c **独立**）；SQL 查 `order_number` | 两子项均 2xx 且 order_number 非空且唯一 | 已实施后任一子项 4xx/5xx/null/重复 | **`NOT_YET_SATISFIED`**（未实施） | 实施后：请求/响应 + `SELECT order_number` |
| **CC-2** | **GATE** | orderCode 保持既有语义 | 同批创建读 order_code 规则 | 与基线一致 | 已验证被改义/清空 | `NOT_YET_SATISFIED`（待实施后回归） | diff + 断言 |
| **CC-3** | **GATE** | 历史订单不受影响 | 抽样旧 order_id | 旧值不变 | 已验证被 UPDATE | `NOT_YET_SATISFIED` | 回滚点前后 SQL |
| **CC-4** | **GATE** | POS→Order 主链路保持 | E1a + H-05 | POS 200 + KDS 可见 | 已验证失败 | `NOT_YET_SATISFIED`（E1a 现为 PASS，实施后须再回归） | H-02/H-05 |
| **CC-5** | **GATE** | P1 直接相关 HTTP E2E | 执行 **H-01~H-06**（**不含** H-09/H-10） | **H-01~H-06 全 PASS** | 已验证任一 FAIL | `NOT_YET_SATISFIED` | 矩阵 + 日志 |
| **CC-6** | **GATE** | P0 回归保持 | 33 套件 | **33/33** | 已验证任一失败 | 33/33 曾于 P0 收口 PASS；**P1 后须再跑** → 实施后 `NOT_YET_SATISFIED` 直至复跑 | surefire |
| **CC-7** | **GATE** | 回滚演练完成 | §11 真实顺序 | 记录 PASS+证据 | 未演练/FAIL/仅纸面 | `NOT_YET_SATISFIED`（禁止纸面充数） | §11.1 |
| **CC-8** | **REG（登记机制）** | P2 residual 处理 | 对照 G1 登记表 | P2-A/B/C + H-09/H-10 **单列**；标 `RESIDUAL_REGISTERED`；**未计入 P1 FAIL/完成数** | 登记缺失 / 残余被标 P1 PASS / 被静默删除 / 用 CC-8 **豁免** GATE 项 | 登记表已成文 → 实施轮关闭前复核 | 本文件 §G1.3 |
| **CC-9** | **GATE** | requiredQty≤0 pending | 断言标记 | 保持 `E2E_EXPECTATION_PENDING_BUSINESS_DECISION` + PD-043 | 被擅自裁定/改 P0 | **PENDING（PD-043）——合规保持即 PASS 条件**；**非** implementation blocker | 测试标记 + PD-043 |

#### §12.1 CC-8 与 CC-1~CC-7/CC-9 的关系（DS 澄清；机械规则）

| 问题 | 裁决 |
|---|---|
| CC-1~CC-7、CC-9 是什么 | **P1 completion gate（GATE）**——功能/回归/回滚/边界必须满足 |
| CC-8 是什么 | **Residual registration mechanism（REG）**——**不是**与 GATE 平行的「功能完成条件」，而是「已知非本卡残余的强制登记簿」 |
| P1 完成是否必须同时满足 CC-8 | **必须** CC-8=PASS（登记合规），但 CC-8 PASS **只**表示登记完成，**不**表示 P2 已修好 |
| CC-8 能否豁免其它 CC | **不能。** 形式化：`CC-8 PASS ⇏ 任何 GATE PASS`；`GATE FAIL ⇏ 可用 CC-8 抵消` |
| 禁止 | 用 CC-8 替代真正失败的 GATE 项；把「residual 已登记」写成「E2E 全部通过」 |

```text
P1 Completion =
      ∀ c ∈ {CC-1..CC-7, CC-9}: state(c) = PASS
  ∧   state(CC-8) = PASS
  ∧   H-09/H-10/P2-* ∈ RESIDUAL_REGISTERED（由 CC-8 承载）
  ∧   CC-8 对 GATE 的豁免 = ∅
```

**关闭门禁（唯一）**: 上式成立才允许宣称 P1-ORDER-NUMBER 功能完成；**当前全部 GATE 项 = `NOT_YET_SATISFIED`（除 CC-9 的 PENDING 合规态）→ 不得宣称完成。**

### 13. requiredQty ≤ 0 硬边界（本轮及实施轮）

- **禁止**业务裁定、改语义、改 P0、改 E2E 期望为确定值、选 zero/reject/accept、代定 notes 持久化。  
- 相关测试恒为 `E2E_EXPECTATION_PENDING_BUSINESS_DECISION` ↔ **PD-043**。  
- **不阻塞** P1 `order_number` 实施与关闭（CC-9）。

### 14. 实施边界（Scope Freeze / Frozen 后的实施轮）

**允许**: **仅在**完整状态机 `Final IR PASS → READY_FOR_SCOPE_FREEZE → Scope Freeze（独立、可记录）→ Frozen → 实施轮正式选定` 全部完成后，按最终选定方案（推荐 A，仍为 **RECOMMENDATION ≠ 已选定**）修改 `OrderNewServiceImpl` 创建路径赋值 order_number；对应单测/集成/H-01~H-06；§11 回滚演练。**Final IR PASS 本身不构成 Implementation Approved。**  
**禁止**: schema/Flyway/V999；P0 扣料；requiredQty；Hikari；MQ/异步审计兜底；修复 P2-A/B/C；顺手修其它 P1/P2；无 002 Scope 擅自扩面；未经 Scope Freeze/Frozen/实施轮正式选定即实施。

### 15. Owner 决策路径

与 Scope001 §12 相同：PD-043 / `d-ig-requiredqty-owner-decision-request-001.md`；P1 实施前启动、实施期间完成；未完成不断 P1。

### 16. 本 Amendment 结束条件（Gate 对照）— 符号语义见「状态语义字典」

| Gate | 条件 | 处理状态 | 结论摘要（≠ 问题已解决） |
|---|---|---|---|
| 1 | P2 与 Completion 关系经路径证据确定 | **✅ 已按证据纪律处理** | G1.1–G1.3 机械规则 + H-09/H-10 归 REG |
| 2 | E1a/b/c 明确可追溯 | **✅ 已按证据纪律处理** | G2 表 + G2.1 E1c 独立验证 |
| 3 | KL-068 范围有证据或标 UNKNOWN/PENDING | **✅ 已按证据纪律处理；结论 = UNKNOWN/PENDING_CONFIRMATION** | **Current 唯一；Historical「是」= superseded interpretation；问题未解决** |
| 4 | A/B/C/D 恢复原始定义 | **✅ 已按证据纪律处理** | G4 冻结；本轮不回退 |
| 5 | E 规则（无 E） | **✅ 已按证据纪律处理** | NO_E 维持 |
| 6 | 新 Scope 唯一 + 旧 SUPERSEDED | **✅ 已按证据纪律处理** | 001=SUPERSEDED；002=唯一 ACTIVE；无 003 |
| 7 | Completion 可枚举且与 G1 一致 | **✅ 已按证据纪律处理** | §12 + §12.1 GATE/REG |
| 8 | 回滚 = 实施中真实演练 | **✅ 已按证据纪律处理** | §11 顺序冻结；**演练本身尚未执行** → CC-7=`NOT_YET_SATISFIED` |
| 9 | requiredQty 仍待决不阻 P1 | **✅ 已按证据纪律处理** | CC-9 保持 PENDING；PD-043 非 implementation blocker |

→ **Final Independent Review = PASS（F-01/F-02/F-03 + 六项 regression = PASS）；已记录 `READY_FOR_SCOPE_FREEZE`；已执行独立 Scope Freeze → `Scope-002 = FROZEN`（`2026-09-23T15:55:44+08:00` / `P1-OSN-002-FREEZE-20260923T155544+0800`）。Frozen 后禁止未经治理重新批准的 Scope 内容修改。Implementation = NOT STARTED；Implementation Approved = NO；本轮到 FROZEN 结束，不进入实施。**
