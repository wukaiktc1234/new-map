# P1-LOCATION-MODEL-001 实施记录（二）：M3-M4 开工（implementation-record-002-m3m4）

- **日期**：2026-09-27 · **性质**：PG-005 阶段 3 实施（Owner 授权"一张卡做透，不拆 M3/M4/M5/M6/M7"）
- **纪律**：`docs/quality/m3m4-preflight/implementation-constitution-001.md`（19 拍板 / 6 ID 规则 / 禁区 / 裁决规则）
- **清单**：`inventory-chain-archaeology-matrix-001.md`（方法/SQL 粒度）· **回归基线**：`inventory-chain-snapshot-20260927-001.json`
- **安全网**：`backups/m3m4-predump-20260927.dump`（pg_dump custom，308 张表数据，2026-09-27 23:18）

---

## 0. 范围（Owner 指令原文归纳）

合并 T1 store_inventory + T2 inventory 单表（键 location_id+material_id）；合并 T3/T4a/T4b 流水；污染全清（宪法 §一.19）；5 个绕过直写文件收编；4 处 `String.valueOf(warehouseId)` 经 map 改写；死代码删除；InventorySummaryMapper UNION ALL 重写；inventory_log 后门关闭。
**禁区**：users.store_id / JWT / 经营链列名 / 默认兜底 / 历史 Flyway / 24.3f 文件。
**行为敏感点**：4 项未批复按现状迁移；其余 5 处按矩阵建议。

## 1. 原子步骤计划（每步完成更新本记录 + 可停点）

| 步 | 内容 | 产出 | 状态 |
|---|---|---|---|
| S1 | pg_dump 安全快照 | `backups/m3m4-predump-20260927.dump` | ✅ 本记录 |
| S2 | DDL 迁移脚本草案（V20260927_002）+ 数据迁移裁定日志 | 本文件 §3 | ✅ 草案，未应用 |
| S3 | 核心实体/Mapper 层：新 Inventory 实体（locationId 键）、InventoryMovement 实体、两 Mapper；legacy 实体改造（StoreInventory→新表视图兼容或删除） | 代码 | ✅ 完成（编译绿） |
| S4 | 服务层合并：InventoryService 收编 StoreInventoryService 全部签名（含入建出抛不对称、3 次重试）；5 个旁路文件收编（SalesOrder/LossOutbound/OtherInbound/HardwareDevice/WarningScheduler） | 代码 | **进行中**——S4a ✅（接口新增 6 个位置维度方法，S4a 委托实现，编译绿）；**S4b ✅（2026-09-28）**：6 方法改为统一账直算（Inventory entity/mapper 直算，String.valueOf 委托已删除；入建出抛不对称/3 次乐观锁重试/最新入库价格法/流水降级 全部现状移植）；单测 `InventoryServiceImplLocationStockTest` **9/9 绿**（五类覆盖：入建/出抛×2/乐观锁重试/成本结转/source 约束×2 + 已有行累加/解析委托）；**grep 确认新方法仍无业务调用方**（仅接口/实现/测试）；行为敏感点 4 项签字见方法头 javadoc；**S4c-1 ✅（2026-09-28）**：4 处 `String.valueOf(warehouseId)` 冒充点改为 `LocationService.resolveByWarehouseId` 经 map 解析（PurchaseStockin increase+void / PurchaseArrival / PurchaseReturn / InventoryTransfer 双向），Arrival 兜底仓库"1"删除，未映射仓库显式拒绝（宪法 §三.4），周边事务语义原样（Q4 未批复）；受影响 mock 测试重锚全绿（PurchaseStockin 8/8、PurchaseReturn 5/5、MaterialDeductionAudit 4/4、OrderNewDeduct 28/28、ConsumptionStats 5/5）。**新增发现（未动，上报）**：PurchaseArrivalServiceImpl:554-555 存在第 5 处同族冒充（DTO setStoreId=String.valueOf(warehouseId) 喂收货确认链），超出点名范围。S4c-2 ✅（2026-09-28 Owner 批复后执行）：#1 SalesOrder 收编（Q5=B：不足抛 INVENTORY_INSUFFICIENT，钳 0 废止；productId varchar 非数值报 PARAM_ERROR）、#2 LossOutbound/#3 OtherInbound 收编（直写与 intValue 截断移除）、#4/#5 现状迁移；测试 29/29 绿；执行结果详见对照表"执行结果"节 |
| S5 | 流水统一：InventoryMovementService（source_type+source_ref NOT NULL）；关 inventory_log 后门（Controller update/delete 下线） | 代码 | **✅（2026-09-28）**：InventoryMovementService/Impl 新增（严格校验：locationId/materialId/changeQty/movementType/sourceType/sourceRef 缺一即 PARAM_ERROR 拒绝，无兜底填充、无 update/delete API，§III.4/§IV.4）；InventoryService 两位置方法 **changeType 移除**、sourceType/sourceRef 必填（movementType 派生 IN/OUT）；**8 个业务调用点重接**（Loss/Other/Transfer 出+入/Arrival/Return/Stockin 入+作废出/SalesOrder）；**inventory_log 后门关闭**（InventoryConsumptionController POST/PUT/DELETE 下线、InventoryLogController createInventoryLog 下线、InventoryLogService/Impl create/update/delete 下线，GET/export/stats 保留）；DRAFT 修订 3 处（重复 min_safe_qty 列删除、movement_type 按 quantity_change 符号派生、注释合并）；单测重锚（LocationStockTest 降级语义重写）+ 新增 InventoryMovementServiceImplTest 9 例；详见 §7 |
| S6 | 4 处 warehouseId 冒充点改经 map（PurchaseStockin:716 / PurchaseArrival:366 / PurchaseReturn:443 / InventoryTransfer:258-261）+ 双写删除 + 调拨 updateInventory 收编 | 代码 | ⏳ |
| S7 | 读取方与 XML：InventorySummaryMapper 重写、DishInventoryMapper JOIN 修正、StockForecastMapper 改键、死代码删除 | 代码 | ⏳ |
| S8 | 编译 + 测试重锚定（矩阵 §5 缺口补乐观锁/成本/双写用例） | 测试 | ⏳ |
| S9 | 停机窗口：冻结写 → 应用 V20260927_002 → 快照 diff（13 端点重放）→ 演练回滚 | 证据 | ⏳ |
| S10 | 前端适配（门店库存页/调拨页/汇总页）+ E2E | 代码 | ⏳ |

## 2. 数据迁移裁定日志（依据宪法，非新拍板；实施中如有新增发现按裁决规则 1 停手上报）

| 数据 | 处置 | 依据 |
|---|---|---|
| T2 inventory 15 行（deleted=0） | 迁 13 行（经 map：warehouse_id→location_id）；**剔除** warehouse_id NULL 1 行（测试残值）与 material_id=999999 1 行（§一.19 虚账） | §一.19 + 规则 3 |
| T1 store_inventory 18 行 | **不迁**（D-4 门店账全清起账）；其中 '584'/'9901' 与仓库语义行按 §一.19 清除 | §一.19 + D-4 |
| T4a inventory_transactions 30 行 | 迁 15 行（wh1 13 + wh2 2，经 map，source_type 取 reference_type 映射）；**剔除** wh584 2 行（部门 ID 污染）、测试仓 wh4/5/8/9/10 6 行（仓未迁移）、NULL 1 行 | §一.19 同族污染 + 规则 3 |
| T3 store_inventory_log 47 行 | **全部不迁**：store 账已全清（D-4），流水所载账本不复存在；其中假流水（Q2 待批复项）数据上随账清除。Q2 批复"是"则维持；批复"否"则需逐笔补账（将再停点上请） | D-4 + §四.6 |
| T4b inventory_log 0 行 | 空表，无迁移 | 活体核对 |
| T5 material_consumption | **本卡不动**（不在 T3/T4a/T4b 流水合并范围；store_id Long 保留，属后续卡） | Owner 范围指令 |
| 阈值双列裁定 | **safety_stock 与 min_safe_qty 两列都留**：消费方矩阵核查（Owner 指令）确认唯一重叠点为 NotificationScheduleService:78-82 在 T2 上读 safety_stock，但该消费方现状空转（活体两列 0 填充）；两列语义分域（safety_stock=门店安全线，min_safe_qty=仓库预警阈值），合并将致门店起账后通知服务误读门店行——例外呈报，S7 中该服务维持现状迁移 | 宪法裁决规则 1（重叠判据边界情况，停手上报 Owner，等一句话裁定） |

## 3. S2 产出：V20260927_002 草案要点（脚本 `V20260927_002__m3m4_unified_inventory.sql`，已起草未应用）

1. legacy 更名：`inventory→inventory_legacy`、`store_inventory→store_inventory_legacy`、`store_inventory_log→store_inventory_log_legacy`、`inventory_transactions→inventory_transactions_legacy`、`inventory_log→inventory_log_legacy`（保留观察期，M7 退役）
2. 新建 `inventory`（-001 §1.3 + 行为保留列：locked_quantity / status / expiry_date / min_safe_qty——矩阵 §7 调度器与锁定语义按现状迁移）+ `inventory_movement`（-001 §1.4，source_type+source_ref NOT NULL）
3. 数据按 §2 裁定表迁移；唯一索引 `(location_id, material_id)`
4. **应用时点**：S8 完成、编译测试绿后，与代码同一停机窗口应用（宪法 §四.5）

## 4. 停点报告（本记录撰写时点）

- 已完成：S1、S2（草案）；开工前清（任务板 / INDEX.md 独立 commit `23aa2e1` / 前置包 commit `0c3126e` / `14b9639` 已上远端）
- 下一步：S3 核心实体/Mapper 层（工作量最大段 S3-S8 的起点）
- 风险提示：S4 服务合并触及 16 个 InventoryService 调用方文件与 5 个旁路，回归面全在行为快照覆盖内

## 5. S4c-1 影响面核实（Owner 指令，2026-09-28 活体）

「未映射仓库显式拒绝」的影响面（三条链逐查）：

1. **purchase_stockins**：13 张 wh∈{3..10}（wh4/5/8/9/10，全为 7 月底 E2E/Test 产物）**全部 status=1 已入库（终态）**，无待确认单。唯一暴露面 = 作废这些历史测试单时将命中新拒绝逻辑（测试遗留，可接受）。
2. **purchase_arrivals**：0 张 wh∈{3..10}（全部 wh1 或 NULL），零暴露。
3. **inventory_transfers**：全部 7 张均为 1→2（WH_A/WH_B，两仓皆已映射）✅；4 张待执行（transfer_status=2 已审批）中 TR20260724001/002 的 product_id=999999 为虚构物料——执行将命中"出抛 NOT_FOUND"（999999 虚账已按 §一.19 清除），属预期拒绝而非事故。

**结论**：无真实业务路径受影响，测试遗留为主 → S4c-1 维持通过。
**测试重锚性质确认**：5 个测试文件中实际修改仅 2 个（PurchaseStockinServiceImplTest / PurchaseReturnServiceImplTest），均为 mock 目标变更；断言值变化仅 1 处——PurchaseStockin 的 verify 由 `anyString()` 收紧为 `eq(1001L)`（= fixture warehouseId 的恒等映射，语义等价更严格）；其余 3 个测试文件（MaterialDeductionAudit / OrderNewDeduct / ConsumptionStats）零改动全绿。

## 6. ENV-4 登记（2026-09-28，PG-001 首次违规，Owner 裁决不冻结）

- **事件**：S4c-2 提交使用 `git add .`，违反 PG-001 v2 commit 隔离条——commit `cb8ee06` 实际含 **524 文件**：S4c-2 合法改动 **5**（LossOutboundService / OtherInboundService / SalesOrderServiceImpl / 本实施记录 / s4c2 对照表）+ 工作区残留 **519**（498 新 docs + 16 陈旧 Flyway SQL（bank/finance/position 系列 WIP）+ 5 个 cb8ee06 既有修改的 WIP docs）。
- **处置**：clean redo **`bf4f6b2`**（恰 5 文件，与 cb8ee06 合法部分逐字节一致，标题同，共同父提交 `9e8aa29`）。push 前三项验证全 PASS（**A**：16 个 SQL 全部为磁盘 untracked 残留；**B**：5 个 WIP docs 工作树内容与 cb8ee06 版本逐字节一致）→ Owner 批准 force-push → **remote master = `bf4f6b2`**（GitHub API 验证，tree `e209be07`，ahead=0）。
- **残留处置**：519 文件**留盘不删除**；`.gitignore` 新增 ENV-4 专用段 **576 条逐文件条目**（514 残留 + 32 根目录工作产物 + 3 scripts + `test/`）防止再次误吞。5 个 WIP docs（purchase-action/field/permission/state-matrix.md + project-diagnosis-20260802.md）为 tracked WIP，**不** gitignore，随后续合法提交入库。`.gitignore` 变更本身在既有 462 个已修改 tracked 文件中，随下一提交入库。
- **裁定**：PG-001 **首次违规**，登记 **ENV-4**，**不冻结**（工作区不冻结，S5 继续）；不再二次 rewrite history；后续严格执行 PG-001 v2（开卡文件 clean + commit 隔离，禁用 `git add .`）。
- **登记位置**：`production-known-limitations.md` 主表 ENV-4 行（86→87）+ 文末 ENV-4 块 + 追加行；`docs/project-context/repo-state.md` ENV 序列表。

## 7. S5 执行结果与裁定日志（2026-09-28）

**裁定日志**（宪法 §四.2：细节缺口 → 本地约定 + 裁定日志；均非新拍板）：

1. **sourceType 词表映射**（全部取 -001 §1.4 既有词表，无新造词）：LossOutbound → `LOSS`（出库/回补均 LOSS，出入由 movement_type 区分）；OtherInbound → `OTHER`；InventoryTransfer 出 → `TRANSFER_OUT` / 入 → `TRANSFER_IN`；PurchaseStockin 入库+作废回滚出 → `PURCHASE_STOCKIN`×2；PurchaseArrival → `PURCHASE_STOCKIN`；PurchaseReturn → `OTHER`（词表无退货出库专词）；SalesOrder → `SALE_DEDUCT`。source_ref 统一为"业务名 - 单据号: 编码"人读描述串（新表语义定义，非旧行为迁移）。
2. **changeType 参数移除**：S4b 遗留的两位置方法 Integer changeType 参数移除，接口收敛为 sourceType/sourceRef 必填；movementType 实现派生（increase→IN / decrease→OUT）。S4b 曾以 changeType=3 记调拨入（新表下会被记成 IN 的家族问题）随之消除——仅新表语义，legacy inventory_transactions 路径不受影响。
3. **change_qty 符号与 DRAFT 4b 修订**：新表按 -001 §1.4"正=入 负=出"（S5 写路径 decrease 传 `quantity.negate()`）。**legacy 实查（2026-09-28 活体 DB）**：inventory_transactions 未删 31 行**全为正**，15 行待迁全正且逐行 `after_qty = before_qty + quantity_change` 无 OUT 行；DDL 注释同为"正数为增加，负数为减少" → **DRAFT 4b 直拷原值不取反**（早期草稿对 OUT 取反的 CASE 与实查数据不符，撤回）；movement_type 按 quantity_change 符号派生（`>=0 → IN`），15 行中 3 行 transaction_type NULL（TXN 2/3/28）由此判 IN。
4. **recordTransaction 保留**：legacy InventoryService 11 个方法（increaseInventory/decreaseInventory/deduct/lock/unlock 等）在 S6/S8 前继续写 inventory_transactions（S9 更名为 legacy）。
5. **T3 假流水保留 + 标记**：MaterialTraceCode:159/:442 假流水（Q2 未批复）不动，§2 已登记。
6. **unitCost 0 兜底保留**：§6-6 现状迁移 unitCost=null 按 0，行级与流水级同口径（S5 不改变）。
7. **DRAFT 4a 重复列修复**：`min_safe_qty` 重复列定义删除，注释合并至保留列。

**后门关闭明细**：
- `InventoryConsumptionController`：POST/PUT/DELETE 下线（createLog/updateLog/deleteLog），GET 列表/导出保留；
- `InventoryLogController`：createInventoryLog 下线（原即 createLog 透传），GET/导出/统计保留；
- `InventoryLogService/Impl`：createLog/updateLog/deleteLog 下线（updateLog 无外部调用方），读侧 3 方法保留；
- `InventoryLogMapper` + XML 零改动（读侧仍经 Service）。

**测试**：定向 4 类 31/31 绿（InventoryServiceImplLocationStockTest 9/9、InventoryMovementServiceImplTest 9/9 新增、PurchaseStockinServiceImplTest 8/8、PurchaseReturnServiceImplTest 5/5；PurchaseReturn 仅 L172 verify 重锚，`any()`×5 签名兼容零改动）。**环境限制**：本会话本机 JAVA_HOME 失效（H:\fuwu\jdk-17.0.17+10 缺失），仅 JDK 25 可用（pom target 21）；JDK 25 下 bytebuddy 1.14.x 不识别 class file 69，跑单测须附加 `-DargLine=-Dnet.bytebuddy.experimental=true`（S8 测试重锚定同样适用，非代码问题）。

## 8. S6a 执行结果与裁定日志（2026-09-28）

**改动面**（StoreInventoryMapper → InventoryMapper 收编；T1 写路径全部切统一账 location 维度）：

| 文件 | 改动 |
|---|---|
| `InventoryService.java` | `resolveLocationIdByStoreId` 接口方法已存在（S4a L69），无新增（本次曾误加重复声明，已撤） |
| `OrderNewServiceImpl.java` | 注入 StoreInventoryService→InventoryService；3 调用点（L644 扣料 / L901 KDS / L1125-1131 退款回补）改 `resolveLocationIdByStoreId` + `decreaseStockAtLocation`/`increaseStockAtLocation`（8 参，sourceType=SALE_DEDUCT/SALE_REFUND）；未映射门店 → BusinessException PARAM_ERROR |
| `ReceiptConfirmationServiceImpl.java` | 移除 StoreInventoryService 注入；store 分支改 resolveLocationIdByStoreId + increaseStockAtLocation（sourceType=PURCHASE_STOCKIN）；默认门店兜底 `resolveStoreIdIfMissing` 禁区不动 |
| `StoreInventoryController.java` | /list 改 getStockPageAtLocation(location 维度)；/adjust 改 increase/decreaseStockAtLocation(sourceType=OTHER)；响应实体 StoreInventory→Inventory（S10 前端适配）；未映射门店 → 空页容错（现状保留）；/summary、/logs、/stores 不变 |
| `PurchasePlanServiceImpl.java` | 注入 StoreInventoryMapper→InventoryMapper；低库存查询 selectList(wrapper) 改 Inventory 字段（quantity/safety_stock/max_stock/unit_cost） |
| `InventoryTransferServiceImpl.java` | 移除死注入 StoreInventoryService（零调用） |
| `PurchaseArrivalServiceImpl.java` | 移除死注入 StoreInventoryService（零调用） |
| `PurchaseReturnServiceImpl.java` | 移除死注入 StoreInventoryService（零调用） |
| `PurchaseStockinServiceImpl.java` | 移除死注入 StoreInventoryService（零调用，含 @Lazy） |
| **删除** `StoreInventoryService.java` / `StoreInventoryServiceImpl.java` / `StoreInventoryMapper.java` | T1 服务/实现/mapper 三文件下线；实体 `StoreInventory` 保留（MaterialDeductionAuditIntegrationTest fixture + legacy 观察用） |

**测试重锚**：
- `OrderNewServiceImplDeductTest`（28 例）：mock StoreInventoryService→InventoryService；stubHappyPath 加 `resolveLocationIdByStoreId(anyLong())→1L`；全部 `decreaseStock(anyString(),anyLong(),any(BigDecimal),anyInt(),anyString())` → `decreaseStockAtLocation(anyLong(),anyLong(),any(BigDecimal),anyString(),anyString())`。
- `OrderNewServiceImplOrderNumberA1Test`（3 例）：构造器参数 swap。
- `PurchaseReturnServiceImplTest`（5 例）/ `PurchaseStockinServiceImplTest`（8 例）：移除死注入 mock 字段 + ReflectionTestUtils.setField。
- `MaterialDeductionAuditIntegrationTest`：**未重锚**——该集成测试 fixture 用 store_id=9901（不在 location_id_map），且直插 `store_inventory` 表；S6a 后 deduct 路径走 location 维度，该测试在 DDL 应用前无法通过（预期，S8 重锚）。

**测试结果**：定向 4 类 **44/44 绿**（OrderNewDeduct 28 + OrderNumberA1 3 + PurchaseReturn 5 + PurchaseStockin 8）。compile + test-compile 全绿。

**裁定日志**（宪法 §四.2）：
1. **Controller 响应实体变更**：/list、/adjust 返回 Inventory 而非 StoreInventory——API 契约字段名变化（current_stock→quantity, store_id→location_id 等），S10 前端适配；路径/参数不变。
2. **/adjust sourceType=OTHER**：手动调整无专属词表项，取 OTHER（-001 §1.4 既有词表）。
3. **未映射门店 /list 空页**：保持原 try-catch 容错现状（不抛异常），仅数据为空；与 §三.4"显式拒绝"的写路径不同——读路径空页是前端兼容现状迁移。
4. **PurchasePlanServiceImpl 低库存查询改 Inventory 字段**：原 StoreInventory.current_stock/safety_stock/max_stock → Inventory.quantity/min_safe_qty/max_stock_qty（语义等价，列名映射）。

**⚠️ 关键发现 + 排序冲突（需 Owner 裁决）**：

**实查活体 DB**：`inventory` 表**已有 location_id 列**（nullable），但 **15 行数据全部 location_id=NULL**（全为 warehouse_id 键控）；`store_inventory` 仍有 18 行。即：
- S6a/S6b 改写后的读路径按 location_id 查询 → **当前返回空**（无行有 location_id）。
- 13 端点快照重放（S6b 后）将显示"库存全空"而非预期的"伪门店账消失"——**diff 不可接受**。
- 根因：DDL V20260927_002（rename inventory→inventory_legacy + create new inventory + 数据迁移 location_id 回填）**尚未应用**（宪法 §四.5 定 S8/S9 停机窗口）。

**选项**：
- **(A) 提前应用 DDL**（S6b 后、S7 前）：V20260927_002 执行后 inventory 表有 location_id 数据 → 13 端点重放可正确 diff（仅伪门店账消失）。需 Owner 批准提前停机窗口（宪法 §四.5 红线条目，非我单方决定）。
- **(B) 维持现状**：S6a/S6b 仅代码级完成（compile/test 绿），13 端点重放推迟到 S8/S9 DDL 应用后。S7 继续（DishInventoryMapper JOIN 修复等，不依赖 DDL）。
- **(C) in-place 回填**：不改表结构，仅 `UPDATE inventory SET location_id = (SELECT location_id FROM location_id_map WHERE ...)` 回填现有 15 行 → 统一读路径立即可用；最终 DDL 简化为后续清理。需 Owner 批准变更 DRAFT 策略。

**待 Owner 裁决后继续 S6b。**

## 9. S6b 执行结果与沙箱验证（2026-09-28）

**Owner 裁决**：选 B + 沙箱验证。活体 DB 不动，DRAFT SQL 仍在 preflight。S9 真应用。

**改动面**：

| 文件 | 改动 |
|---|---|
| `InventorySummaryMapper.xml` | T2+T1 UNION ALL 整段重写为统一 inventory 单表 GROUP BY（经 locations JOIN 区分 CENTRAL/DEPOT vs STORE） |
| `InventoryMapper.java` | 删除 `selectByMaterialAndWarehouse` 方法声明 |
| `InventoryMapper.xml` | 删除 `selectByMaterialAndWarehouse` SQL |
| `InventoryServiceImpl.getByMaterialAndWarehouse` | 改为 warehouse→location 解析 + LambdaQueryWrapper（兼容 legacy 调用方） |
| `InventoryServiceImpl.getLowStockList` | raw SQL `current_stock` → `quantity`；warehouseId→locationId 解析 |

**测试**：compile + test-compile 全绿；定向 6 类 **62/62 绿**（同 S6a 范围）。

**沙箱验证**（Owner 指令）：
- 本地 PG 18.3 创建 `m3m4_sandbox` 库 → `pg_restore` 从 `backups/m3m4-predump-20260927.dump` 恢复 → 手工执行 V20260927_002 DDL（5 RENAME + 2 CREATE + 数据迁移 13+15 行）
- Spring Boot 启动指向沙箱（`PG_DB_NAME=m3m4_sandbox`，flyway 禁用），JWT 认证通过
- **13 端点快照重放结果**：

| 端点 | 结果 | diff 判定 |
|---|---|---|
| /v1/inventory?page=1&size=100 | total=13 | 伪门店账消失（原 15→13）✓ |
| /v1/store-inventory/list | 空页 | 伪门店账消失（原 18→0）✓ |
| /v1/store-inventory/summary | total=12 | 伪门店账消失（UNION ALL→单表）✓ |
| /v1/inventory/low-stock | 空列表 | 与快照一致（原亦空）✓ |
| /v1/locations, by-store/*, by-warehouse/* | ✅ | 零差异 ✓ |
| /v1/stores/active, /v1/warehouses/active | ✅ | 零差异 ✓ |
| /v1/inventory/logs/page | 500（表更名） | **预期**：S7/S8 范围（log 合并） |
| /v1/store-inventory/logs | 500（表更名） | **预期**：S7/S8 范围 |

**结论**：除"伪门店账消失"（3 端点）+ 2 个表更名 500（S7/S8 范围，非 S6b 代码缺陷）外，**零差异**。沙箱 diff **通过**。

**裁定日志**：
1. **getLowStockList 列名修复**：raw SQL `current_stock`→`quantity`（新表列名）；warehouseId→locationId 解析（未映射仓返回空列表）。属 S6b 发现的代码缺陷，非新拍板。
2. **getByMaterialAndWarehouse 兼容 shim**：原 raw SQL 删除后改为 resolve+LambdaQueryWrapper——legacy 调用方（InventoryServiceImpl 内部 + MaterialArchiveServiceImpl）无需改动即可工作。
3. **沙箱 500 端点不阻塞 S6b**：inventory_log/store_inventory_log 表更名是 DDL 的 RENAME 操作，代码侧对应改写属 S7（log 合并）/S8（编译测试重锚）。

**下一步**：S7（DishInventoryMapper JOIN 修复、StockForecastMapper 改键、死代码清理）。

## 10. S7a 执行结果与沙箱验证（2026-09-28）

**Owner 裁决**：24.3f InventoryLogMapper.xml **已解锁**（建议解锁→执行）。S7 拆两步：S7a 读路径收尾 + S7b 死代码清理。

**改动面**（S7a 读路径收尾）：

| 文件 | 改动 |
|---|---|
| `InventoryLogMapper.xml` | 原 `inventory_log`（T4b，0 行）→ 统一 `inventory_movement` 读取；列别名映射到 InventoryLog 实体字段 |
| `StoreInventoryLogMapper.xml` | 原 `store_inventory_log`（T1 log）→ `inventory_movement` JOIN `locations`（type=STORE）过滤 |
| `DishInventoryMapper.xml:25` | `i.cost_price` → `i.unit_cost`（新表列名） |
| `StockForecastMapper.java` | 3 方法改键：getActualConsumed→inventory_movement；getAvailableStock/getSafetyStock→unified inventory |

**沙箱验证**：13 端点重放 **13/13 PASS**（含之前 500 的 /v1/inventory/logs/page + /v1/store-inventory/logs）。零失败。

**裁定日志**：
1. **InventoryLogMapper.xml 解锁执行**：24.3f 禁区解除（Owner 批准）；原表 T4b 本身 0 行，改读统一流水无数据风险。
2. **StoreInventoryLogMapper JOIN locations**：STORE 类型过滤等价于原 store_inventory_log 语义（仅门店维度的出入记录）。
3. **StockForecastMapper 改键**：getActualConsumed 从 `store_inventory_log.type='out'` → `inventory_movement.change_qty<0`（负值=出）；getAvailableStock/getSafetyStock 从 `store_inventory` → unified `inventory` by location。

**S7b 待做**：死代码清理（transient 字段引用、已删除方法的残留 import 等）。
