# P1-INVENTORY-LOG-FILTER-001 — 实施记录

## 0. 状态

| 项 | 值 |
|----|------|
| Task ID | `P1-INVENTORY-LOG-FILTER-001` |
| Stage | **IMPLEMENTED_VERIFIED（2026-09-26）**——修复 + 4/4 测试通过 + BUILD SUCCESS；待 DS 抽检 + QA 独立验收 |
| 根因 | `docs/quality/pos-menu-inventory-category-diagnostics-001.md` 诊断 1：`InventoryLogMapper.xml` 的 `selectInventoryLogPage` 无 WHERE，mapper 接口 6 个 @Param 全部被静默丢弃 |
| PG-005.1 豁免 | Owner 批准（修复方向唯一 / 无产品决策 / 无架构变更，跳过设计阶段） |

## 1. 修复内容（主文件 1 + 测试 1）

| 文件 | 改动 |
|------|------|
| `backend/src/main/resources/mapper/InventoryLogMapper.xml` | `selectInventoryLogPage` 补动态 `<where>`：`deleted = 0` 基线 + 5 个 `<if>` 过滤（productId / warehouseId / operationType / operatorId / startTime+endTime 区间）。String 参数判 `!= null and != ''`，Long 参数判 `!= null`；时间参数显式 `::timestamp` 强转（兼容 `yyyy-MM-dd` 与 `yyyy-MM-dd HH:mm:ss`）。模式复刻同目录 `StoreInventoryLogMapper.xml` L6-19 |
| `backend/src/test/java/com/foodtraceability/integration/InventoryLogFilterIntegrationTest.java`（新） | 4 用例：① productId+warehouseId 过滤 → 仅 1 条匹配行；② 全参数 null（全局）→ 4 条未删除全返回（分页）；③ 仅 operationType="out" → 仅 2 条 out；④ 基线：deleted=1 行不进任何查询。fixture 走 JdbcTemplate 插删，`remark LIKE 'IT-%'` 隔离不碰真实数据；@SpringBootTest 模式沿用 `MaterialDeductionAuditIntegrationTest`（直连 dev 库 food_traceability + 禁 Flyway） |

**未改动**（对照禁止项）：Service / Controller / 前端 / mapper 接口签名 / 其他 mapper XML / DB schema。

## 2. 影响面核对（只读，开卡前完成）

| 调用方 | 传参 | 修复前 → 修复后 |
|--------|------|----------------|
| `InventoryLogController`（`/v1/inventory/logs/page`，前端库存详情弹窗） | materialId→productId + warehouseId | 全局日志 → 按物料+仓库过滤 ✓（本 bug） |
| `InventoryConsumptionController` L47 | operationType="out"，其余 null | 全类型混排 → 仅 out（与方法注释"消耗记录"语义一致）✓ |
| `WarehouseOverview.vue` L271（近期动态） | 不传参；若筛选表单选了仓库则仅传 warehouseId | 不传参 → 仅 `deleted=0`，全局语义维持（且排除逻辑删除行，更准确）；传 warehouseId → 按仓库过滤（与页面筛选语义一致）✓ |
| 其他 Java 调用方 | grep 全库：仅 `InventoryLogServiceImpl` L50 透传 | 无第三方依赖"无 WHERE"行为 ✓ |

## 3. 测试与构建

| 项 | 结果 |
|----|------|
| `mvn test -Dtest=InventoryLogFilterIntegrationTest` | **Tests run: 4, Failures: 0, Errors: 0, Skipped: 0** |
| 构建 | **BUILD SUCCESS，MVN_EXIT=0** |
| typecheck | 全模块 `testCompile` 通过，零新增错误 |
| 环境注记 | 本机 Windows + JVM 25 下 ByteBuddy self-attach 失败，所有 @SpringBootTest 需 `-DargLine=-Djdk.attach.allowAttachSelf=true`（触发链：Spring Boot `ResetMocksTestExecutionListener` → `Mockito.<clinit>`，与本卡改动无关；存量 `SystemSettingsIntegrationTest` 等在本机应同样触发） |

## 4. 残留（登记不顺手修）

1. **`selectConsumptionStats` 列名错位**（同文件 L8-10）：SQL 写 `SUM(change_quantity)`，但实体/实表列名是 `change_amount` → 运行时会报列不存在。存量问题，非本卡范围。
2. **argLine 环境问题**：如需根治（pom 加 surefire `argLine` 配置），属构建基建改动，需单独立卡或按 ENV 登记。
3. **startTime/endTime 无格式校验**：String 参数 + `::timestamp` 强转，非法格式在 DB 层报错（维持现有接口契约，未新增校验）。

## 5. PG-001 v2 门禁

- 开卡前：464 M 全部真实改动（463 存量 + 1 本卡编辑）+ 1 存量删除，**零幻影**；目标 XML 开卡时零 WIP 重叠
- 精确 add：本卡 3 文件（XML + 测试 + 实施记录），无 `.`/`-A`/目录通配
- commit 前 `git diff --cached` 审查通过
