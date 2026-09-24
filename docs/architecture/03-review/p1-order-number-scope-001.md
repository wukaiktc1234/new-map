# P1-ORDER-NUMBER — Scope 001

> **DOCUMENT STATUS:** SUPERSEDED  
> **SUPERSEDED_BY:** `docs/architecture/03-review/p1-order-number-scope-002.md`  
> **原因**: Scope Amendment（G1–G4：P2 边界 / E1a-c 定义 / KL-068 发布范围 / A/B/C/D 原始定义恢复 + Completion Criteria 重写 + 回滚演练时机冻结）——实质性替换，非追加。  
> **效力**: 本文件仅作历史记录；**后续 P1 实施只以 002 为准**；禁止两份并行有效 Scope。

> **状态**: ACTIVE_REFERENCE — SCOPE ONLY（可执行、可审查；**本文档不实施**）【已废止，见上 SUPERSEDED】
> **任务编号**: P1-ORDER-NUMBER-001
> **前置**: P0-KDS-DEDUCT 已 `CLOSED_WITH_REGISTERED_RISKS`（`p0-kds-deduct-closure-001.md`）
> **执行纪律**: Scope 阶段禁改业务代码 / schema / Flyway / V999；实施须另开任务卡并完成 §8 完成标准
> **Production Evidence**: BLOCKED（本地证据，不外推生产）

---

## 1. 问题定义

| 项 | 内容 |
|---|---|
| 现象 | `orders.order_number` 为 `NOT NULL` + `UNIQUE`（`postgres-schema-v0.12.sql:523`；活体库约束 `orders_order_number_not_null` / `uk_orders_order_number`）；但非 POS 创建链路只写 `order_code` |
| 根因 | `OrderNewServiceImpl.createOrder:114-147` → `generateOrderCode()` → `buildOrderEntity:1816-1838` **仅 `setOrderCode`，无 `setOrderNumber`** → INSERT 违约 NOT NULL |
| 对比 | `PosOrderCreateServiceImpl:285-286` **同时** `setOrderCode(orderNumber)` + `setOrderNumber(orderNumber)` → POS 主路径创建**不**被 NOT NULL 阻断 |
| 语义混用 | 多处把 `orderCode` 当业务订单号：`OrderNewServiceImpl:448/:550` `event.setOrderNumber(order.getOrderCode())`；`PosOrderQueryServiceImpl` 多处 `dto.setOrderNumber(order.getOrderCode())`；历史迁移 `V20260707_001:45` `order_code = order_number` 回填 |
| 双字段 | `OrderNew.java:26-34`：`orderCode`「订单编号，唯一」示例 ORD…；`orderNumber`「订单号（POS终端生成）」示例 T… |
| 影响 | ① 非 POS 创建 API 不可用；② 前端/事件/报表对 orderNumber/orderCode 消费不一致；③ 真实 HTTP E2E 在「从创建起」的链路上可能被阻断（见 §3） |

---

## 2. 影响范围（14 面审计）

| # | 面 | 关键落点（示例，实施前须再扫） | 风险 |
|---|---|---|---|
| 1 | DB 约束 | `orders.order_number` NOT NULL UNIQUE；`order_code` 可空 | 阻断 INSERT / 冲突 |
| 2 | Entity | `OrderNew` orderCode/orderNumber 双字段 | 映射遗漏 |
| 3 | Mapper/XML | 订单查询/分页按 order_code like | 口径分裂 |
| 4 | Service 创建 | `OrderNewServiceImpl.buildOrderEntity` 缺 setOrderNumber；`createPosQuickOrder→createOrder` | **P1 主修复点** |
| 5 | Service POS | `PosOrderCreateServiceImpl` 双写（已对齐） | 回归保护 |
| 6 | Controller | `POST /v1/orders`（OrderNewController:40）vs `POST /v1/pos/orders/order` | 入口分叉 |
| 7 | DTO/VO | OrderVO/查询 DTO 暴露字段名 | 契约 |
| 8 | 查询/报表 | POS 查询把 orderCode→orderNumber 展示 | 展示口径 |
| 9 | 日志/操作描述 | 支付/退款 remark 用 orderCode | 追溯混淆 |
| 10 | 事件 | OrderCreated/Completed `setOrderNumber(orderCode)` | 下游错绑 |
| 11 | 测试 | 集成 fixture 手工 INSERT 必须带 order_number | 回归 |
| 12 | 唯一约束/生成规则 | generateOrderCode（ORD+ts+seq）vs POS NumberGenerator | 冲突/重复 |
| 13 | 外部 API | 前端 POS/KDS/Admin 消费 orderNumber | 断链 |
| 14 | 下游表 | `kitchen_order.order_number`（可空）、`material_consumption.order_number`、`call_record` 等 | 拷贝语义 |

**历史数据**：`V20260707_001` 已做 `order_code ← order_number` 方向回填；实施时禁止破坏既有唯一值。

---

## 3. E2E 阻断扫描（POS → 订单 → KDS → 托盘 → Serve → 扣减）

逐步静态扫描（本 Scope 不做运行时全链路）：

| 步 | 链路 | 端点/代码 | 是否被 order_number 阻断 | 其他已知阻断 | 归属 |
|---|---|---|---|---|---|
| E1 | POS 创建订单 | `POST /v1/pos/orders/order` → `PosOrderCreateServiceImpl` **双写** | **否** | 权限/鉴权按 KDS/POS 白名单现状 | 非本 P1 |
| E1b | 通用/快速创建 | `POST /v1/orders` / `createPosQuickOrder`→`createOrder` | **是（唯一主阻断：NOT NULL）** | — | **P1-ORDER-NUMBER** |
| E2 | 支付 | `POST /v1/pos/orders/order/pay` 等 | 否（用 orderCode 查） | 口径混用观察 | P1 影响面 |
| E3 | 进 KDS | POS 创建时写 `kitchen_order.order_number`（:490） | 否 | KDS 旧 API `/v1/kitchen-order/*` 断链（前端 12 调用） | **P2（前端断链）**，非 order_number |
| E4 | 托盘绑定 | `POST /v1/tray/bind-order` | 否 | 需 kitchen_order 存在 | 非本 P1 |
| E5 | 后厨流转 | scan-kitchen-in / out | 否 | 防抖/状态机 | 非本 P1 |
| E6 | 出餐 | `POST /v1/tray/scan-serve` → `TrayServiceImpl.scanServe:285` → `deductMaterialsForServe` | 否（kitchen_order.order_number 来自 POS 双写） | ready 前置 + 2s 防抖；ServeWindow 旧路径 `POST /v1/kitchen/serve-order/{id}` **后端不存在**（实为 `/{id}/serve`） | **P2（路径 404）**，非 order_number |
| E7 | 扣减/审计 | P0 已收口 | 否 | R1–R3 登记风险 | 已关闭带风险 |
| E8 | 从 E1b 起的端到端 | 创建即失败 | **是** | — | **P1 必须消除** |

**扫描结论**：

```text
order_number 是否「唯一」已知 HTTP E2E 阻断？
→ 对「从 POST /v1/orders 创建起」的全链路：是（E1b）。
→ 对「从 POS 创建起」的链路：否；POS 路径创建不被阻断。
→ 是否另有阻断？是（但不归本 P1）：
   P2-A：frontend-kitchen kitchenOrderApi /v1/kitchen-order/* 断链；
   P2-B：ServeWindow POST /v1/kitchen/serve-order/{id} 后端不存在 → 404；
   P2-C：KDS 写操作身份/鉴权设计缺口（治理已知项）。
→ 归属：E1b → P1-ORDER-NUMBER；P2-A/B/C → P2 独立登记，不并入本卡冒充完成。
```

---

## 4. 方案比较（A/B/C/D）——含可验证预测列；**推荐见 §5，本 Scope 不实施**

| 方案 | 做法 | 优点 | 缺点 | **可验证预测（实施后可证伪）** |
|---|---|---|---|---|
| **A** | `buildOrderEntity` 补 `setOrderNumber(orderCode)`（创建时 orderNumber:=orderCode） | 改动面最小；NOT NULL 立解；POS 已双写同值风格一致 | 放大「双字段同值」语义债；与 POS NumberGenerator 格式可能不一致 | P1 后 `POST /v1/orders` INSERT 成功且 `order_number=order_code`；`SELECT` 两列相等（新单） |
| **B** | 统一生成规则：POS 序列号生成器抽公共，创建路径写同一 orderNumber，orderCode 可保留 ORD 前缀或对齐 | 消除双生成器；语义一次对齐 | 动 NumberGenerator/唯一键风险；历史回填复杂 | P1 后新建单 `order_number` 唯一且格式=选定生成器；旧单不改 |
| **C** | DB 层：`order_number` 改可空（或 GENERATED 从 order_code） | 阻断立即解除，代码可后补 | **动 schema**；与 UNIQUE/应用双写历史冲突；生产 Flyway 风险 | P1 后 information_schema `is_nullable=YES` 或 generation_expression 非空；应用创建仍成功 |
| **D** | 合并 orderCode/orderNumber 为一列（迁移+全量改引用） | 根治双字段 | 超出 P1；14 面全动；回滚难 | 长周期；**不建议本 P1 执行**（归 PD-015 订单迁移二期） |

---

## 5. 【推荐实施方案 + 证据】（禁实施）

**推荐：方案 A 为 P1 最小闭环；方案 B 作为 P1 内可选增强（若生成器冲突被证实）；不推荐 C/D 在本 P1 执行。**

| 项 | 内容 |
|---|---|
| 推荐 | **A**：`buildOrderEntity` 在设置 orderCode 后同步 `setOrderNumber(orderCode)`（或与 POS 同源的单一生成值） |
| 证据① | `buildOrderEntity:1816-1838` 无 setOrderNumber —— 直接根因 |
| 证据② | `PosOrderCreateServiceImpl:285-286` 双写 —— 项目内已有「双写」先例，A 与之同构 |
| 证据③ | 活体/DDL NOT NULL UNIQUE —— 唯一阻断在 E1b |
| 证据④ | `createPosQuickOrder:179` 委托 `createOrder` —— 修 A 即覆盖快速单 |
| 证据⑤ | 14 面中 POS 主路径、托盘、扣料均不依赖「orderNumber≠orderCode」才能跑通（E2–E7 扫描） |
| 回滚预案指针 | §6 |
| 明确不做 | 不改 NOT NULL（禁方案 C）；不动 V999；不合并双字段（禁 D）；不改 requiredQty；不调 Hikari |

---

## 6. 回滚方案与演练要求

| 项 | 内容 |
|---|---|
| 回滚触发 | P1 后创建 500 / 唯一键冲突 / POS 回归失败 / QA FAIL |
| 回滚步骤 | ① revert 实施 commit（仅 `OrderNewServiceImpl` 创建路径）；② 验证 `POST /v1/orders` 恢复为「已知失败形态」而非新形态；③ POS 创建回归 1 条；④ 记录 rollback 时间与操作人 |
| **演练要求** | **必须在 P1 实施阶段至少真实演练 1 次**（非仅书面）；演练记录写入本文件 §6.1 或实施卡 |
| 纸上回滚 | **不算完成** |

### 6.1 回滚演练记录（P1 实施时填写）

| 字段 | 内容 |
|---|---|
| 演练日期 | `{YYYY-MM-DD}` |
| 环境 | `{local / staging}` |
| 步骤 | `{revert commit → 冒烟 → 恢复}` |
| 结果 | `PASS` / `FAIL` |
| 证据 | `{git sha / 日志路径}` |

---

## 7. 生产影响 8 问（实施前逐项回答）

| # | 问题 | Scope 阶段默认 |
|---|---|---|
| 1 | 是否改 schema？ | **否**（推荐 A） |
| 2 | 是否改 Flyway？ | **否** |
| 3 | 是否动 V999？ | **否** |
| 4 | 是否影响 POS 双写路径？ | 回归必须覆盖；期望零行为变化 |
| 5 | 历史数据是否回填？ | 本 P1 **不回填**（仅新写入） |
| 6 | 唯一键冲突风险？ | A 使用已有 orderCode 值作 orderNumber 时须确认该值未被占用（POS 与 ORD 前缀空间） |
| 7 | 连接池/事务？ | 不改 |
| 8 | 回滚演练？ | §6 必做，未演练不得关 P1 |

---

## 8. HTTP E2E 测试矩阵（实施阶段）

| ID | 场景 | 期望 | 断言标记 |
|---|---|---|---|
| H-01 | `POST /v1/orders` 创建 | 200 + order_number 非空 | 可断言 |
| H-02 | `POST /v1/pos/orders/order` 创建 | 200，orderCode=orderNumber（现状保持） | 可断言 |
| H-03 | 快速单 createPosQuickOrder | 200 | 可断言 |
| H-04 | 重复 order_number 插入 | 拒绝（UNIQUE） | 可断言 |
| H-05 | POS 创建 → KDS 列表可见 | 200 | 可断言 |
| H-06 | 托盘 bind → serve → 扣料 | P0 行为保持 | 可断言 |
| H-07 | requiredQty≤0 配方出餐 | **待决策** | `E2E_EXPECTATION_PENDING_BUSINESS_DECISION` |
| H-08 | notes 是否落库 | **待决策** | `E2E_EXPECTATION_PENDING_BUSINESS_DECISION` |
| H-09 | 旧 ServeWindow serve-order 路径 | 404（已知，P2-B） | 登记非 FAIL |
| H-10 | kitchenOrderApi 旧前缀 | 断链（已知，P2-A） | 登记非 FAIL |

---

## 9. requiredQty 待决断言

| 断言 | 状态 |
|---|---|
| `requiredQty > 0` 正常扣减 | 可测（已有 L1） |
| `requiredQty ≤ 0` → 合法零消耗 / 数据异常 / 分类 | **`PENDING_BUSINESS_DECISION`（PD-043）** |
| 零数量 notes 持久化 | **`PENDING_BUSINESS_DECISION`** |

上述待决**不阻塞 P1-ORDER-NUMBER 关闭**；阻塞的仅是对应 E2E 断言从 PENDING 转正。

---

## 10. 完成标准（5 条；代码改完 ≠ 完成）

1. **HTTP E2E 硣定性场景全过**：H-01~H-06 为 PASS（H-07/H-08 允许 PENDING；H-09/H-10 为已知 P2 登记）。
2. **L1/集成不回归**：`OrderNewServiceImplDeductTest` 28、`MaterialDeductionAuditIntegrationTest` 4、`MaterialDeductionAuditSelfFailureIntegrationTest` 1 = 33/33 保持；禁止因 P1 破坏 P0。
3. **生产影响 8 问全部书面回答**（§7），且与实现一致。
4. **回滚方案至少一次真实演练并记录**（§6.1），纸上不算。
5. **requiredQty 相关断言保持 `PENDING_BUSINESS_DECISION` 直至 PD-043 决策**——**不因 pending 而阻塞本 P1 关闭**（决策可并行，见 Owner 路径）。

---

## 11. 实施边界（下一轮才允许）

**允许（实施轮）**：`OrderNewServiceImpl` 创建路径补写 orderNumber（方案 A/B 经选定）；对应单测/集成/H-01~H-06；回滚演练。

**禁止**：改 `orders` schema/Flyway/V999；改 P0 扣料逻辑；改 requiredQty 语义；动 Hikari；方案 C/D；无 Scope 擅自扩面（P2-A/B/C 另卡）。

---

## 12. Owner 决策路径（与工程线并行）

| 项 | 内容 |
|---|---|
| 决策 | requiredQty≤0 三裁定 + notes 持久化 |
| 发起人 | GPT（P0 收口后**已发起**） |
| 记录 | `d-ig-requiredqty-owner-decision-request-001.md` + PD-043 |
| 时点 | P1 实施前启动，P1 实施期间完成 |
| 未完成 | P1 可继续；§9 断言保持 PENDING |

---

## 13. 结束条件对照（本 Scope）

| 条件 | 状态 |
|---|---|
| P0 `CLOSED_WITH_REGISTERED_RISKS` | ✅ 见 `p0-kds-deduct-closure-001.md` |
| P1 形成可执行可审查 Scope（完成标准 / 回滚演练要求 / E2E 阻断扫描 / Owner 路径） | ✅ 本文档 §3/§6/§10/§12 |

→ **满足结束条件；下一轮方可改 `order_number` 业务代码。**
