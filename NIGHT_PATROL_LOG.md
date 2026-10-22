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
