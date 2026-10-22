# NIGHT_PATROL_LOG.md — 夜间自主巡航日志

> 分支：`ai-night-patrol-1`（自 master `86af662` 拉出）
> 团队：Lead（lead）+ radar1-silent-catch + radar2-schema-drift + radar4-business-boundary
> 起始时间：2026-10-23 04:33 (+08:00)

## 0. 上下文加载结论（遍读 STATUS / 避坑指南 / PG 纪律 / 实施记录尾部 §24.17a–§24.24a）

**已建立的项目认知要点**
- 单一真相源 = `docs/STATUS.md`；与 git 冲突时以 git 为准（视图层性质）。
- 主链路已闭环：POS 点单 → 支付 → 后厨接单 → 开始制作 → 完成制作（出餐）→ 扣料 → 追溯码 → 成本入账 → 财务收入（幂等）。
- 项目特有坑（避坑指南固化、实测成立）：
  1. **雪花 ID 19 位**超出 JS 安全整数 → 响应体必须 `@JsonSerialize(ToStringSerializer)`；前端 `===`/`Number()` 会踩坑；API 级 PASS ≠ UI 级 PASS。
  2. **严格 `===` 匹配**：element tree-select 用 `===` 匹配 option，类型不一致（number vs string）直接回退渲染裸 ID。
  3. **实体/表漂移**：`@TableField(exist=false)` 不影响 XML `resultType`；`resultType` 只看属性名不看注解；`dining_table` 是视图不是表；单复数双表并存。
  4. **MyBatis XML 改完必须重启**才能生效；HMR 只管前端。
  5. 行尾混用（多数 CRLF，`UserMapper.xml`/`user.ts`/实施记录 LF）—— edit 前先 read。
- 纪律：PG-001 精确 `git add <文件>`（禁 `git add .`/`-A`/目录通配）、commit 前 `git diff --cached` 复核、禁静默 auto-push hook；PG-003 schema 只走 Flyway；PG-005 三阶段（本次为纯 bug/硬化，属轻档）。
- 禁区（本次巡航严格遵守）：`OrderLifecycleService`（状态机）、权限底层（security 包 / SecurityConfig / RoleCodeNormalizer / AdminPermissionCatalog / PosAuthServiceImpl）、`flyway_schema_history` 不 repair、单数 legacy 表 `inventory_warning`/`inventory_warning_rule` 不碰、治理文档只增不改、Owner WIP（539 条目）不入库。

## 1. 环境

| 项 | 值 |
|----|----|
| 分支 | `ai-night-patrol-1`（自 master `86af662`） |
| JAVA_HOME | `P:\my-new-project\JDK21`（**未预设，必须显式设置**，否则 `mvn` 报 “JAVA_HOME ... not defined correctly” — 本次首跑即踩） |
| mvn | `H:\fuwu\apache-maven-3.9.11\bin\mvn.cmd`，离线 `-o` |
| DB | PostgreSQL 18 本机 5432 / `food_traceability` / 316 张表；**仅 SELECT** |
| 工作区 | 539 条目 Owner WIP（未动、未暂存） |

## 2. 扫描与修复记录

（滚动追加，每 3 个修复点一次独立提交）
### 批次 1 — 雷达 4（业务边界）· 提交 `b746e66`

| # | 文件:行 | 问题 | 改法 |
|---|---------|------|------|
| 1 | controller/marketing/MemberLevelController.java:119 | `Integer status == 1` 拆箱 NPE（status 列 nullable） | `Integer.valueOf(1).equals(...)` |
| 2-3 | utils/DepartmentConverter.java:34,75 | 部门树/列表转换同族拆箱 NPE | 同上 |
| 4 | utils/IpUtils.java:86-103 | X-Forwarded-For 可控输入 `172.`→AIOOBE、`172.abc`→NFE | 先判前缀 + try/catch NFE 返回 false |
| 5-6 | util/JwtUtil.java:218,254 | JWT 无 exp 声明 → `getExpiration().before()` NPE | 判 null，无 exp 视为已过期 |
| 7 | utils/DesensitizeUtils.java:95 | `switch(type)` type 为 null → NPE | 补 `type == null` 早退 |
| 8 | utils/ImageUploadUtils.java:92 | `file.isEmpty()` 未判 file null | `file == null ||` |
| 9 | controller/finance/FinanceAssetController.java:147-159 | assetId null 透传→500；months=`12.0`→NFE 400 | null→400 明确错误；Number 兼容 |

验证：`mvn -o compile` **BUILD SUCCESS**（2766 源文件全量重编）；`git diff --cached --numstat` 确认仅 7 文件（45+/13-）。

**发现但未修（登记）**
- `common/exception/GlobalExceptionHandler.java:178` `getConstraintViolations().iterator().next()` 无 hasNext → 空集合时 500（该文件带 Owner WIP，只读）。
- 同文件缺 `HttpMessageNotReadableException` handler → 非法 JSON 落 500（同上 WIP）。
- `interceptor/PermissionInterceptor.java:123` `permission.getStatus() == 1` 无判空 —— **鉴权链路 NPE**，但改法涉及「null 视为禁用」的授权语义，属权限底层，**红线不碰**，留 Owner 裁决。
- service/ 层同族拆箱 NPE 6 处（AccountingSubjectServiceImpl:166、BankAccountServiceImpl:303、FoodServiceImpl:152/172、PrintFormatTemplateServiceImpl:60、MarketingAnalysisServiceImpl:258）→ 已转交批次 2。
- `controller/AssetController.java:54` `((Number) stat.get("count")).intValue()` 键缺失 NPE 且被外层 catch 静默降级为 0（数据静默错误）—— 未确认 mapper 别名恒为 count，不敢改。
### 批次 2 — 雷达 1（静默吞异常 / 假成功）· 提交 `d1c1f9d`

| # | 文件:行 | 假成功形态 | 改法 |
|---|---------|-----------|------|
| 1 | event/listener/OrderCompletedEventListener.java:84-115 | 收入/应收各自 catch 只 log，外层仍打「处理完成」 | 内层抛 BusinessException，外层 firstFailure 聚合后抛 |
| 2 | 同上 :88,182-218 | **CODE-007**：recordOrderCost 死代码（4 个构造点均不设 materialCost/laborCost） | 删除方法与调用点，保留事件字段（Serializable 契约）与 costRecordService 注入 |
| 3 | event/listener/OrderCreatedEventListener.java:44-51 | 原料锁定失败被伪装成功（超卖/追溯断链） | 内层抛业务异常，外层隔离但明确记失败 |
| 4 | event/listener/PurchaseStockInEventListener.java:55-84,122-131,152-161 | 追溯码/采购成本吞异常仍报完成 | 两步独立 try 都执行 + firstFailure 聚合 |
| 5 | event/HealthCertificateEventListener.java:111-122 | hrSyncResult=false 后仍打「处理完成」 | 完成日志移入成功分支 |
| 6 | service/impl/DailySettlementServiceImpl.java:474-503 | 逐日 catch 吞异常，缺日列表当完整对账结果 | failedDates 汇总 + 全失败抛异常 |
| 7 | service/impl/MaterialTraceCodeServiceImpl.java:375-533 | 忽略 updateById 返回值、库存/入库记录吞异常、门店ID非法静默跳过 → 「确认入库成功」但没入账 | 失败即抛；批量逐条留痕汇总 |

**事务安全性核对**：4 个监听器均为 `@Async` + `@TransactionalEventListener(AFTER_COMMIT)` + `@Transactional(REQUIRES_NEW)`（OrderCompletedEventListener:75-77 实证）→ 抛出不影响主事务。

验证：`mvn -o compile` **BUILD SUCCESS**；`git diff --cached --numstat` 仅 6 文件（144+/68-）。

**发现但未修（登记，均因 Owner WIP 或需新增依赖）**
- `task/OrderTimeoutTask.java:102,134-136`（WIP）：`@Transactional` 加在 private 方法上 = 自调用不过代理，事务注解失效。
- `service/impl/PurchaseOrderServiceImpl.java:242-249`（WIP）：`getMaterialPrimarySupplier` 静默 return null 无日志 → 采购单可能落 null 供应商、应付错账。
- `service/impl/PosOrderQueryServiceImpl.java:226-233`（WIP）：详情转换失败 return null 后 filter 丢单，仍返回 success → 小程序少单但报成功。
- `service/impl/MaterialTraceCodeServiceImpl.java:441-453`：组装 Inventory 后**从未落库**（无 save/insert），只打「更新门店库存」日志 —— 比吞异常更彻底的假成功，属功能缺口（InventoryService 无对应写入方法），需 Owner 定接口。
- `scheduler/MonthlyClosingScheduler.java:67-122`：成本结转/折旧金额写死 0L、账龄更新空 TODO，日志却打印「月末结账任务完成」（结构性假成功）。
- `scheduler/impl/DefaultTaskExecutor.java:90-133`：内置任务为 `Thread.sleep(100)` 模拟却返回 success("备份完成")。
- `task/CertificateExpiryCheckTask.java:133-137`：`isTaskAlreadyCreatedToday` 恒 false（TODO 桩）→ 每天重复生成证件待办。
- `listener/HREventListener.java:43-53,80-110,136-186`：NumberFormatException 只 warn 后跳过 → 职位/部门ID 格式错误静默不同步。
- `service/impl/OrderLifecycleServiceImpl.java:118`（**核心状态机红线，未改**）：javadoc 仍描述「监听器 recordOrderCost 会再写一条成本」，该方法已于本批次删除 → 注释过时，请 Owner 自行更新。
- 保留不动的可容忍降级（有明确 ADR/注释依据）：SalaryPaidEventListener:74-107（ADR-004）、ReceiptConfirmationServiceImpl:754-783（SR-7）、scheduler/BackupHealthChecker:394-397（fail-closed 方向正确）、PurchaseOrderCleanupTask:33-35、BackupTaskScheduler:181,233。
### 批次 3 — 雷达 1（静默吞异常·无日志优先）· 提交 `20adc43`

7 文件 10 处「catch 完全静默（无 log 无 throw）」→ 显式 log.error / log.warn，**不改方法签名与返回契约**（mapper/config/printer/driver 层抛异常会炸启动或整条链路，故只硬化日志）：

| 文件:行 | 静默吞掉的后果 |
|---------|---------------|
| config/handler/JsonbListTypeHandler.java:70 | JSONB 反序列化失败静默返回空列表 → **追溯码列表凭空清空且无任何日志** |
| service/VoucherNoGenerator.java:95 | 凭证流水号解析失败静默 return 0 → 期间内凭证重号（违反连续唯一） |
| config/KitchenOrderMigrationConfig.java:97 | 列探测失败静默 return false（=当作迁移未执行）→ 重跑迁移/重复 ALTER |
| runner/PermissionInitRunner.java:69,136,176 | 权限初始化/插入/admin 授权失败只 `System.err` → 不进日志体系，应用照常启动 |
| driver/TsplLabelPrinterDriver.java:1037 | 标签尺寸参数非法静默取默认 → 版式错但算打印成功 |
| config/PositionRoleMappingInitConfig.java:140 | 查职位ID异常只 warn 无异常 → 职位-角色映射静默缺失 |
| printer/XprinterSdkService.java:275,287 | 打印元素 mm/边框参数解析失败静默取默认 |

验证：`mvn -o compile` **BUILD SUCCESS**；仅 7 文件入库（Owner WIP 的 `CorsConfig.java` 未入）。

**发现但未修**：`service/PosOrderNumberGenerator.java:136-141`（WIP）MAX 序号解析失败静默归零 → 可能生成重复 order_code；`config/SchemaFixMigration.java:71-74` 表存在性探测失败降为 debug 且当「表不存在」；`config/WebSocketConfig.java:79-90` STOMP 鉴权 catch 后 return null（fail-closed，鉴权语义只报不改）。

### 批次 4 — 雷达 2 + 雷达 3 审计交付 · 提交 `947cf80`

产出：`docs/night-patrol/radar2-schema-drift-report.md`(318 行) / `radar3-dirty-data-report.md`(189 行) + `scripts/night-patrol/` 7 个脚本（01–07）。

**雷达 2 实测结论（Lead 已用 psql 独立复核）**
| 项 | 实测 |
|----|------|
| @TableName 指向不存在对象 | **67 处**（其中 40 处 Mapper 已被 Controller 链路注入） |
| 根因 | 64 处**在 222 个 Flyway 脚本中从来没有 CREATE TABLE**；3 处有迁移且 success=t，但数据躺在手工建的 `*_legacy` 表 |
| 单复数并存 | 18 对，其中 6 对**代码读写的是空表** |
| 双表行数（Lead psql 复核） | `food_trace_codes` 0 / `food_trace_code` 14；`dish_recipe` 0 / `dish_recipes` 12；`food_category` 0 / `food_categories` 9；`recharge_plans` 0 / `recharge_plan` 496；`cost_record` 0 / `cost_records` 33；`order_material_requirement` 0 |
| `trays` 表 | **根本不存在**（STATUS.md TECH-DEBT-001 记载有误，实际只有 `tray` 13 行） |

**雷达 3 实测结论**
- DATA-006 的 10 条 `amount=0` 财务流水：仍 `deleted=0`，对应 10 个 order_code 在 `orders` 全部命中且 `payment_status=2`。
- **更严重的连带发现**：`orders` 里 **25 条已支付订单的 `actual_amount` 100% 为 0.00**（`final_amount = total_amount` 25/25、真 0 元订单 0 条）→ **此时软删流水等于掩盖缺口而非修复**。
- 其他：`cost_records.remark` 含字面 ` - null` 25 行；`food_trace_code.dish_name` 空 8/14 行；`tray.store_id IS NULL` 2 行（挂 141/143 在途厨单）；`kitchen_order material_consumed=1` 21 单但 `order_material_requirement` **全表 0 行**（扣料明细从未落库）；孤儿引用 0。

**脚本安全性（Lead 独立复核）**：7 个脚本剥离注释后 `destructive=0 / writes=0`（连 UPDATE/INSERT 都只在注释内），全部实跑 exit=0。

### 批次 5 — 雷达 2 自动化闸门 · 提交 `5491cda` + `7dfd3fa`

新增 `scripts/check-table-name-exists.py`（237 行）+ `docs/night-patrol/schema-drift-baseline.md`（69 项基线）。

**为什么要新写一个**：既有 `scripts/check-schema-alignment.py` 有两个盲区 —— ① 只扫 `entity/*.java` 顶层，漏掉 `entity/finance`、`entity/marketing`、`entity/approval` 等子包；② 只比「实体 vs Flyway CREATE TABLE」，比不到活库真实对象，也发现不了「数据在手工 legacy 表」。

**实测输出**：370 处 @TableName / 312 表 / 4 视图 → 命中表 300、**命中视图 1**（`DiningTable → dining_table`，视图可读不可写，任何写操作必失败）、缺失 65、单复数错侧 4（`Budget`/`Receivable`/`SalaryRecord`/`CostAllocationRule` 读单数空表而数据在复数表 —— 这 4 对是雷达2 报告未覆盖的，由闸门的单复数启发式新发现）。

**闸门语义**：基线内 69 项不触发失败；**基线外新增即 exit 1**；`--update-baseline` 需 Owner 批准后使用。

**踩坑登记**：基线最初写成 `.txt` → 被 `.gitignore:238` 的全局 `*.txt` 规则忽略，无法入库；改用 `.md`（不改 .gitignore，遵守 ENV-4 红线）。
### 批次 6 — 雷达 4（业务边界·第二批）· 提交 `0ded263`

5 文件 6 处：

| 文件:行 | 问题 |
|---------|------|
| service/finance/impl/AccountingSubjectServiceImpl.java:166 | `Integer status == 1` 拆箱 NPE（`accounting_subjects.status` nullable） |
| service/trace/impl/FoodTraceabilityServiceImpl.java:468 | 追溯码风险重算漏判空（同文件 :433 已判） |
| service/finance/impl/BudgetServiceImpl.java:92-94,161-171 | `budgets.actual_amount`/`budget_amount` nullable 拆箱 NPE 三处 → 统一提 `long`（顺带消掉除零可能） |
| service/finance/impl/PayableServiceImpl.java:284-293,419-427 | YR/YF 应付号尾段 `substring(len-4)`+`parseInt` 无长度判断/无 try → NFE / SIOOBE 500（同文件 :302-318 早已有同构守卫） |
| service/finance/impl/ReceivableServiceImpl.java:284-293 | YS 应收号同构缺陷 |

验证：`mvn -o compile` **BUILD SUCCESS**；仅 5 文件入库（`BankAccountServiceImpl` 等 5 个 Owner WIP 未入）。

**发现但未修**：`BankAccountServiceImpl.java:303`（WIP，同族拆箱 NPE）；`MaterialRequestServiceImpl`/`PurchasePlanServiceImpl` 共 11 处 `getStatus() != STATUS_*` —— psql 确认两表 status 均 `NOT NULL`，DB 路径不可达，按「不引入回归」未改；`PurchasePlanServiceImpl:448/385` 的状态码属采购自有体系，**不能**替换成 `OrderStatusCodes`（不同表口径）。

## 3. 复核（reviewer）与收口

（待 reviewer 结论回填）
