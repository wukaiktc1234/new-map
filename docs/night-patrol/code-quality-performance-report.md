# 代码质量与性能优化建议报告（夜间巡检 · ai-night-chore-1）

> 产出：夜间苦力任务池 B/C/D 执行过程中的实测发现汇总。**只报不改**：所有缺陷均未修改生产代码，均以特征化测试或只读验证固化。
> 证据口径：每条含 `文件:行`、触发条件、实测表现；"建议"均为最小改动方案，是否实施由 Owner/白天 Agent 决定。
> 关联：`WORKER_LOG.md`（进度与避坑）、`docs/night-patrol/api-contract-diff.md`（前后端契约 58 条）。

## 0. 结论速览

| 级别 | 条数 | 说明 |
|------|------|------|
| P0（口径/账实不符） | 4 | 金额单位与排序方向错误，直接影响报表与对账 |
| P1（静默失败/假成功） | 4 | 缺料、未扣库存、审批痕迹缺失，日志与事实背离 |
| P2（性能/健壮性） | 6 | N+1 查询、重复写、无界缓存、空指针边界 |

本轮可为这些缺陷补的测试已全部落地：**23 个测试类 / 263 个用例**（`backend/src/test/java/com/foodtraceability/service/impl/` 等），单类命令 `mvn -o test -Dtest=<类名>`。

## 1. P0：口径与账实不符

### P0-1 定价历史排序方向反转（实测）
- 位置：`service/impl/PricingServiceImpl.java:138-139`
- 代码：`boolean isDesc = "desc".equalsIgnoreCase(...); wrapper.orderBy(true, isDesc, ProductPricingHistory::getCreateTime);`
- 根因：MyBatis-Plus `orderBy(condition, isAsc, column)` 第二参是 **isAsc**，这里传的是 isDesc。
- 实测：`sortOrder=desc`（默认）生成 `ORDER BY create_time ASC`；`asc` 生成 `DESC`。调价历史永远反着排。
- 建议修法：改传 `!isDesc`（一行）；补一条断言排序方向的用例即可（本报告对应测试 `PricingServiceImplTest#queryHistory_*` 已固化现状，修复后需同步改断言）。

### P0-2 库存调整单「表头 vs 明细」金额口径不一致（实测）
- 位置：`service/impl/InventoryAdjustServiceImpl.java:112-114`（创建）与 `:219-226`（更新）
- 创建：表头 `totalAdjustAmount` 直接累加入参（按"元"）；同方法明细 `adjustAmount` 却 ×100 存"分"（`:147-150`）。
- 更新：表头改为按明细的分值累加，且**可为负**（`-2 × 5.00 元 = -1000`）。
- 影响：同一字段"创建按元、更新按分、可为负"，调整报表/总账无法对齐。
- 建议：统一「分」为唯一存储单位；创建时 `totalAmount += 明细adjustAmount(分)`；表头取绝对值或明确允许负数并写进口径说明。

### P0-3 日结列表在 currentUser 为空时丢弃全部筛选条件（实测·特征化）
- 位置：`service/impl/DailySettlementServiceImpl.java:341-343`
- 现状：`if (currentUser == null) return wrapper;` —— 提前返回，后面的日期/状态/门店条件全部不生效。
- 影响：任何未注入用户的调用（内部任务、测试、未来匿名导出）都会拉全表分页。
- 建议：把「门店收敛」与「查询条件」拆开，空用户只跳过门店收敛，条件照常拼装。

### P0-4 出库执行金额按 long 截断（实测）
- 位置：`service/impl/InventoryOutboundServiceImpl.java:306-307`
- 代码：`long itemAmount = item.getUnitCost() * actualQty.longValue();`
- 影响：`actualQty=1.5` 时按 1 计，金额系统性偏小；与明细分单位口径不一致。
- 建议：`unitCost * actualQty` 用 `BigDecimal` 相乘后 `setScale(0, HALF_UP)` 再转 long。

## 2. P1：静默失败与假成功

### P1-1 批次扣料不足只告警（实测）
- 位置：`service/impl/InventoryDeductionServiceImpl.java:199-201`
- 现状：`remainingQty > 0` 只 `log.warn`，方法正常返回；调用方视为成功。
- 建议：抛业务异常或在返回值中携带 `shortage`，由上层显式处理（与 STATUS §6.4「假成功」同族）。

### P1-2 按配方扣料完全不写库存（实测）
- 位置：`service/impl/InventoryDeductionServiceImpl.java:78-92`
- 现状：只登记 `MaterialUsageRecord` 与内存 JSON，**没有任何库存写入**；账面与实际持续背离。
- 建议：接入 `InventoryService.decreaseStock/decreaseInventory`，或明确该模式只做记录并在接口注释声明。

### P1-3 报损审批不落审批痕迹（实测）
- 位置：`service/impl/InventoryLossServiceImpl.java:72-84`
- 现状：方法收了 `remark` 参数但从未使用；`approveUserId/approveTime` 的赋值被注释掉。
- 影响：审批链无审计痕迹（谁批的、什么时候批的、意见是什么全部丢失）。
- 建议：从安全上下文取审批人 + 写 `approveTime` + 把 `remark` 落到 `remark` 字段。

### P1-4 自采入库仓库硬编码（实测）
- 位置：`service/impl/SelfPurchaseServiceImpl.java:80-81`
- 现状：`warehouseId = 1L; warehouseName = "主仓库";` 不读主表 `warehouseId`。
- 影响：多仓场景必然入错仓；追溯码批次号也与真实仓库无关。
- 建议：优先取 `selfPurchase.getWarehouseId()`，为空再回退默认仓并告警。

## 3. P2：性能与健壮性

| # | 位置 | 问题 | 建议 |
|---|------|------|------|
| P2-1 | `PricingServiceImpl#create`（`:56-57,78`） | 单次调价对同一 product 调 3 次 `selectById`（取旧价/取名/更新前再查），`batchPricing` 100 条即 300 次读 | 一次查询取实体，复用同一对象更新 |
| P2-2 | `InventoryOutboundServiceImpl#executeOutbound`（`:285,310`） | `unitCost != null` 时每条明细 `updateById` **两次** | 合并为一次更新（先算金额再落库） |
| P2-3 | `ReportConfigServiceImpl`（`:32`） | 本地缓存 `Map<Long, Map<Integer, Map<String,String>>>` **无上限、无 TTL、无淘汰**；多实例部署还会脏读 | 换 Caffeine（带 maximumSize + expireAfterWrite）或去掉本地缓存 |
| P2-4 | `DailySettlementServiceImpl#getSettlementList`（`:114-115`） | 逐条调 `getSettlementBasicInfo` 做 N+1，而 `DailySettlementDataService` 已提供 `batchGetSettlementBasicInfo` 未使用 | 改用批量接口一次取回 |
| P2-5 | `DiningTableNewServiceImpl#update`（`:69-70`） | `updateById` 后直接 `selectById(...).getStoreId()`，记录不存在即 NPE | 判空后返回 404 语义 |
| P2-6 | `StoreInventoryLogServiceImpl#createLog`（`:80`） | 无备注时 `sourceRef="store-log-"+currentTimeMillis()`，同毫秒并发会撞号 | 用 UUID 或雪花 ID |

## 4. 契约层（详见 api-contract-diff.md）

Top 3 直接可修、影响面最大的：
1. **日结金额二次换算**：后端 VO 已是「元」字符串，前端 `api/store-ops/converters.ts:97-99` 又除 100 → 金额小 100 倍。
2. **日结状态类型错位**：后端 `draft/pending/approved` 字符串，前端按 `0/1/2` 数字映射 → 恒显示「待日结」。
3. **一批写操作打空**：会员冻结/拉黑、HR 四类配置保存、优惠券发放核销、薪资批次审发、POS 会员检索、排班引擎生成 —— 后端路由不存在或方法不符（`404/405`）。

## 5. 测试覆盖现状与继续方式

- 已覆盖 23 个 `ServiceImpl`（263 用例）：日结、库存设置/预警/盘点/库位/出库/报损/调整/扣料、桌台、预约、定价、预测、自采、原料消耗与审计、告警、验证码、通知偏好、报表配置、库存汇总。
- 全仓仍有 **300+ `ServiceImpl` 无对应测试类**。挑选方法：
  ```powershell
  $tests = Get-ChildItem backend\src\test -Recurse -Filter *Test.java | % BaseName
  Get-ChildItem backend\src\main -Recurse -Filter *ServiceImpl.java |
    ? { $n=$_.BaseName; -not ($tests | ? { $_ -like "$n*" }) } |
    % { [pscustomobject]@{ Name=$_.BaseName; Lines=(Get-Content $_.FullName | Measure-Object -Line).Lines } } |
    Sort-Object Lines | Select-Object -First 30
  ```
- 写测试的 4 个坑（已踩过，见 `WORKER_LOG.md §3.1`）：`getOne` 需双打桩、`baseMapper` 要反射注入、Lambda 断言先 `initTableInfo`、**ORDER BY 在 `getCustomSqlSegment()` 而非 `getSqlSegment()`**。

## 6. 边界声明

- 本报告不含任何生产代码改动；所有"建议"均未实施。
- 涉及状态机、权限链、订单主流程的判断（如 P0-3 的筛选收敛）均**只描述现状**，改动需 Owner 决策。
- 数据侧结论（table/schema 相关）来自既有 STATUS 记录与代码静态阅读，本轮未连库、未执行任何 SQL。
