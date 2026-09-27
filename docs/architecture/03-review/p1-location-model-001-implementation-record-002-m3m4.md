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
| S5 | 流水统一：InventoryMovementService（source_type+source_ref NOT NULL）；关 inventory_log 后门（Controller update/delete 下线） | 代码 | ⏳ |
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
