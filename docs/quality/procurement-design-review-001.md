# 采购链设计评审 001（procurement-design-review-001）

- **日期**：2026-09-26
- **输入**：`docs/quality/procurement-chain-flow-audit-001.md`（全流向排查）识别的 6 个设计议题
- **性质**：设计评审建议（AI 理解稿，**待 Owner 审定**）；本轮不改代码

---

## 议题 1：申请 → 订单"半自动"——是否应改为"从申请自动带出"？

**事实**（已核实）：
- `POST /v1/purchase/orders` 要求 items 手工传入（"订单明细不能为空"强校验），requestId 仅落库关联
- `purchase_orders.request_id` 有列且落库；`request_no` 有列但不自动回填
- 无任何"从申请生成订单"端点（PurchaseOrderController 仅通用 CRUD + 状态流转）

**影响**：申请信息（数量/单位/预估价/规格/remark）全部依赖调用方手工重输——漏传不校验、错传不拦截（本次审计订单 item 的 specification 即未带出）；申请与订单数量无一致性约束（申请 100 斤可下 999 斤的单，无告警）。

**建议**：**修（增量，非替换）**——新增"从申请生成订单"的辅助端点或 createOrder 的 `requestId` 分支：requestId 存在时自动带入申请 items 作为默认值（quantity/foodName/foodCode/spec/remark），调用方可改；回填 request_no；可选校验"订单数量 ≤ 申请数量"（超采需显式标记）。

**改动面估计**：PurchaseOrderServiceImpl +1 个私有方法（~40 行）+ 可选 1 端点；前端对接可选。**1 文件，中低**。

---

## 议题 2：双轨 status（字符串恒 'pending'）——是否应删除字符串字段？

**事实**（已核实）：
- 全文件 `setStatus(` 仅 **1 处**（createOrder L286，注释"旧字段字符串状态"）；submit/approve/confirm 只改数字 order_status
- purchase_orders.status 列 NOT NULL；字符串值创建后**永不变**（实测 0→1→2→6 全程恒 'pending'）
- 字段注释自认"旧字段"

**影响**：读方若用字符串 status 判断状态必错；两套状态并存是持续的认知陷阱（本次审计初期即被误导）。无运行时危害（无人消费）。

**建议**：**修（低危清理，非紧急）**——阶段化：① 文档标注 deprecated（本次已做）② 排查消费方（前端/报表若读 status 字符串需切 order_status）③ 确认零消费后迁移至废弃列或删除。**不建议立即删列**（DDL 需走 Flyway 且要先证明零消费）。

**改动面估计**：代码 0（只读排查）→ DDL 1 个 Flyway migration（最终阶段）。**分两步，各低**。

---

## 议题 3：申请侧字段被丢弃（storeId / item.planned*）——落库还是"申请侧不收集"？

**事实**（已核实）：
- DTO 有 storeId / items[].plannedReceiverType / plannedStoreId，但 purchase_request / purchase_request_item **无对应列**——创建时静默丢弃
- 订单侧 purchase_order_items **有** planned_receiver_type / planned_store_id / planned_warehouse_id 三列并落库（且是到货 ReceiverKey 的数据源，见议题 5）
- 到货行实测：planned_store_id 继承自订单 item；purchase_arrivals.store_id = '' （空串，来自 order item 无值时的行为，见审计 A5 邻近发现）

**影响**：申请阶段表达的"送哪个店"在申请单上不可追溯（审计断点）；下游订单要靠再次手工输入，错店风险后移到收货环节才暴露。

**建议**：**待讨论（二选一，取决于业务意图）**——
- 路线①（落库）：purchase_request_item 加 planned_receiver_type/planned_store_id 列（Flyway），create 端点落库，生成订单时带出（与议题 1 联动）
- 路线②（明确不收集）：从 PurchaseRequestCreateDTO **删除** storeId/planned* 字段（消除"传了也没用"的陷阱），门店归属由订单阶段唯一决定
- 倾向：**路线②**——申请阶段通常尚不知供应商/配送安排，先收集易错；删除字段成本最低且消除静默丢弃。**待产品确认申请单是否需要展示送达门店**。

**改动面估计**：路线② = DTO 1 文件 + 前端表单；路线① = Flyway 1 + 实体/DTO/服务 2~3 文件。**均低~中**。

---

## 议题 4：收货与应付耦合——解耦 / 异步 / 失败不回滚？

**事实**（已核实）：
- confirm 内 `createPayableForStockin` 为**同事务强一致**（代码注释明确"T-038：同事务强一致性；应付创建失败则整个收货事务回滚"——是**裁决过的设计**，非偶然）
- PayableService.createForStockin 幂等设计：payableNo = "AP" + 0填充10位(stockinId)，重复调用不重复建
- 撞号根因（A1）：该确定性编号空间与历史 payables 编号（AP0000000001~0026，2026-07/08 旧体系）**重叠**——stockinId≤26 必炸；stockinId>26 后自然越过
- payables 列含 supplier_id/order_no/stockin_id/amounts/status——财务对账要素完整

**影响**：A1 场景下收货完全无法完成（库存不入、应付不建、stockin 卡 0）；且失败信息是 500"系统繁忙"级（BusinessException 带底层 SQL 报错，但被全局处理器包装）。

**建议**：**修，但保持强一致（不做异步解耦）**——
- 根因是**编号空间冲突**，不是耦合本身：应付与库存同事务入账是财务正确性的合理选择（异步会造成"库存已入、应付缺失"的对账窗口）
- 最小修复：payableNo 生成改为**不可能与历史冲突的命名空间**（如 `"AP" + stockinId 前置区分符` 或日期+序号格式 `AP20260926001`，同时保留幂等键——幂等应改为按 stockin_id 查重而非靠 payableNo 编码）
- 异步/事件方案（事件隔离）：可作为后续演进，但需补"应付缺失补偿任务"，复杂度更高——**不建议现在做**
- 失败不回滚（先收货后补应付）：**不建议**——正是对账混乱之源

**改动面估计**：PayableService 编号生成 + 幂等查重改造，1~2 文件 + 1 测试。**低**。另需数据核对：stockinId≤26 且尚未成功 confirm 的入库单（当前仅 audit 单 72 一张卡住）。

---

## 议题 5：item 级 planned 字段——是否应提升到订单表头？

**事实**（已核实）：
- planned_receiver_type/planned_store_id/planned_warehouse_id 为 **item 级**三列
- 到货 ReceiverKey 按 item 的三元组分组——**不同 item 可自动拆成不同到货单**（门店收一部分、仓收一部分）
- 同一物料多 item 不同 planned 值会拆多张到货单（设计支持混合配送）

**影响**：item 级是"一单多收货方"能力的载体；提升到表头会**移除按明细拆分到货的能力**——若业务存在"一张采购单部分送店、部分送仓"，表头化是功能回退。

**建议**：**不修（维持 item 级），修的是大小写归一**（A2 的真正修复点：ReceiverKey/落库时 toUpperCase 或白名单校验）。若业务确认"一单只会送一个地方"，再考虑表头化（需 Owner 明示）。

**改动面估计**：A2 归一化修复 = PurchaseArrivalServiceImpl（或订单落库侧）+1 处归一化，**低**；表头化迁移 = DDL + 实体/服务/到货分组逻辑重写，**高**（不建议无业务输入时做）。

---

## 议题 6：供应商/部门/申请人不跨单——审计要求是什么？

**事实**（已核实）：
- purchase_request：有 applicant_id/name、department_id/name ✅；purchase_orders：**均无**；purchase_arrivals/payables：均无申请人/部门；payables 有 supplier_id ✅
- purchase_orders.approval_user_id 有列（实测为空——审批人未落库，confirm/approve 接口未写入）
- 申请与订单经 request_id 可 join 追溯申请人/部门（关联键存在，但 request_no 不回填使正向查询不便）

**影响**：财务/审计视角：payable→order→request 可经 id 链回查申请人（join 可达），但**无冗余字段意味着每查必 join、且 request_no 断链降低可读性**。合规上"谁申请、谁审批、付给谁"三要素：付给谁 ✅、谁审批 ⚠（approval_user_id 列空）、谁申请 ⚠（需 join）。

**建议**：**部分修（低成本低风险）**——
- ① approve/confirm 时落 approval_user_id（从 SecurityUtils 取，~5 行）——补齐审批留痕
- ② 订单冗余 applicant_id/applicant_name/department 两列（Flyway + createOrder 从 request 带出，议题 1 联动）——可选
- ③ request_no 自动回填（与议题 1 联动）
- 均为审计留痕增强，非紧急；**待财务/审计方明确要求后立卡**。

**改动面估计**：① = 1 文件 ~5 行（低）；② = Flyway 1 + 服务 1（中）；③ = 随议题 1。

---

## 汇总

| 议题 | 建议 | 优先级 | 改动面 |
|------|------|--------|--------|
| 1 申请→订单带出 | 修（增量端点/分支） | 中（体验+防错） | 1 文件，中低 |
| 2 双轨 status | 分阶段清理（先标废、后删列） | 低 | 两步各低 |
| 3 申请侧 planned/store | 路线②：DTO 删字段（待产品确认） | 低 | 低 |
| 4 收货↔应付耦合 | 修编号空间 + 保持强一致（不做异步） | **高（A1 阻断）** | 1~2 文件，低 |
| 5 planned 字段层级 | 不修；修 A2 大小写归一 | **高（A2 阻断）** | 归一化低 |
| 6 审计留痕 | ① 审批人落库先做；②③ 待审计要求 | 中低 | ①极低 |
| A1/A2 | **立卡 P1（上线阻断，与业务链验证 F4/F6 同级）** | 高 | 见各项 |

> 本评审仅建议，未实施；A1/A2 修复卡与议题 1/4/5 的联动（建议合卡：A1+A2+议题4 编号改造为一张"P1-PROCUREMENT-BLOCKERS-001"）待 Owner 裁定。
