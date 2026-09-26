# P1-INVENTORY-CONSUMPTION-STATS-001 — 实施记录（阶段 3）

## 0. 状态

| 项 | 值 |
|---|---|
| Task ID | `P1-INVENTORY-CONSUMPTION-STATS-001` |
| Stage | **IMPLEMENTED（待 DS 抽检 + QA 独立验收）** |
| 豁免 | PG-005.1 纯缺陷豁免：诊断已完成（诊断报告 §InventoryLogMapper.xml selectConsumptionStats 两层缺陷），修复方向唯一、无产品决策、无架构变更 → Owner 批准跳过设计阶段直接实施 |
| 代码变更 | 后端 1 文件（`InventoryLogMapper.xml`）+ 集成测试 1 文件（新增） |
| 日期 | 2026-09-25 |
| 下一步 | 交 DS 抽检；随后 QA 独立验收（验收 1–5） |

---

## 1. 本卡改动文件清单（PG-001 v2 开卡声明）

| # | 文件 | 变更类型 |
|---|------|----------|
| 1 | `backend/src/main/resources/mapper/InventoryLogMapper.xml` | M（仅 `selectConsumptionStats` 方法体，L27-39） |
| 2 | `backend/src/test/java/com/foodtraceability/integration/InventoryLogConsumptionStatsIntegrationTest.java` | A（新增） |
| 3 | `docs/architecture/03-review/p1-inventory-consumption-stats-001-implementation-record-001.md` | A（本记录） |

commit 精确 add 上述 3 文件，无 `-A` / `.` / 目录通配。

**WIP 重叠核对**：`InventoryLogMapper.xml` 工作区修改内容 = 本卡 `selectConsumptionStats` 修复本身（`git diff HEAD` 逐行确认，diff 仅含本卡 10 行改动 + 既有文件尾无换行符状态不变）；前卡 `selectInventoryLogPage` 修复已随 commit `ece7e6e` 入库，无残留重叠。

---

## 2. 诊断回顾（两层缺陷，详见诊断报告）

| 层 | 缺陷 | 表现 |
|---|------|------|
| 第一层 | XML 列名写成 `SUM(change_quantity)`，实体（`InventoryLog` L56 `@TableField("change_amount")`）/实表列名为 `change_amount` | 任何调用抛 PG `column change_quantity does not exist` → `GET /v1/inventory/consumptions/stats` 500 |
| 第二层 | mapper 接口 3 个 `@Param`（startTime/endTime/operationType）在 XML 全部未引用（无 WHERE） | 即使修列名仍是全局统计，参数过滤不生效 |

活体 DB `information_schema` 已确认 `inventory_log.change_amount` 列存在（诊断阶段只读核对）。

---

## 3. 修复内容（两层一并修）

### 3.1 主文件 — `backend/src/main/resources/mapper/InventoryLogMapper.xml`

`selectConsumptionStats`（修复后 L27-39）：

```xml
<select id="selectConsumptionStats" resultType="java.util.Map">
    SELECT SUM(change_amount) AS total FROM inventory_log
    WHERE deleted = 0
    <if test="operationType != null and operationType != ''">
        AND operation_type = #{operationType}
    </if>
    <if test="startTime != null and startTime != ''">
        AND create_time &gt;= #{startTime}::timestamp
    </if>
    <if test="endTime != null and endTime != ''">
        AND create_time &lt;= #{endTime}::timestamp
    </if>
</select>
```

- 第一层：`SUM(change_quantity)` → `SUM(change_amount)`
- 第二层：补动态 WHERE：`deleted = 0`（恒生效）+ `operationType` / `startTime` / `endTime` 区间（有值时生效）
- 参数名与 `InventoryLogMapper.java` L45-47 三参数 `@Param` 绑定逐一对齐（startTime / endTime / operationType）
- 时间戳写法 `::timestamp` 与同文件 `selectInventoryLogPage`（L20/L23）既有写法一致

### 3.2 集成测试 — `InventoryLogConsumptionStatsIntegrationTest.java`（新增）

模式同 `InventoryLogFilterIntegrationTest`（直连本地 dev 库、禁用 Flyway）；fixture 仅清理 `remark LIKE 'IT-CS-%'` 的行，不触碰真实数据；开卡时活体核对本表 0 行。

fixture（4 行）：

| 行 | type | amount | create_time | deleted |
|---|---|---|---|---|
| IT-CS-OUT-0901 | out | 10 | 2026-09-01（区间外） | 0 |
| IT-CS-OUT-0915 | out | 20 | 2026-09-15（区间内） | 0 |
| IT-CS-IN-0910 | in | 5 | 2026-09-10（区间内） | 0 |
| IT-CS-OUT-DELETED | out | 100 | 2026-09-15 | 1 |

测试 5 项，覆盖验收 1–4 全部三种参数组合 + 端点：

| # | 场景 | 断言（精确值） |
|---|------|----------------|
| 1 | 不传参 → 全局统计 | SUM = 35（3 条未删除行之和，逻辑删除行被 `deleted=0` 排除） |
| 2 | 仅 operationType="out" | SUM = 30（in 行与逻辑删除行排除） |
| 3 | 仅 startTime/endTime | SUM = 25（区间外 09-01 行排除） |
| 4 | 组合（out + 区间）→ 交集 | SUM = 20（仅 IT-CS-OUT-0915） |
| 5 | 端点 `GET /v1/inventory/consumptions/stats` 无参 | HTTP 200 且 `code=0`（不再 500）；`data.total=30`（Service 硬编码 "out" 语义保持：含区间外 out 行，排除 in 行与逻辑删除行） |

---

## 4. 影响面核对（只读）

- `selectConsumptionStats` 全仓调用方 = 1 处：`InventoryLogServiceImpl` L62（`getConsumptionStats`，operationType 硬编码 `"out"`）→ `InventoryConsumptionController` L127-141（`GET /v1/inventory/consumptions/stats`，传 startTime/endTime/category）。
- 修复后端点语义：Service 恒传 `"out"` → 统计 out 类型；startTime/endTime 由前端可选传入（有值过滤，无值不过滤）；`category` 参数 Service 层未使用（既有语义，本卡不动）。
- 无其他调用方，无端点契约变化（返回结构 `Map{total}` 不变），不破坏既有语义。

---

## 5. 禁止项核对

| 禁止项 | 核对结果 |
|--------|----------|
| 不改 Service / Controller / 前端 | ✅ `InventoryLogServiceImpl` / `InventoryConsumptionController` / 前端均未动 |
| 不改 mapper 接口签名 | ✅ `InventoryLogMapper.java` L45-47 未动 |
| 不改其他 mapper XML | ✅ 本文件内仅 `selectConsumptionStats` 方法体变化（diff 确认） |
| 不改 operationType 硬编码逻辑 | ✅ Service L62 `"out"` 保留 |
| 不顺手修其他发现 | ✅ |
| 不动 DB schema | ✅ |
| 不动 WIP | ✅ 精确 add 3 文件 |

---

## 6. 验收核对

| # | 验收项 | 结果 |
|---|--------|------|
| 1 | `GET /v1/inventory/consumptions/stats` 返回真实统计（不再 500） | 测试 5 断言 `code=0` + `data.total=30`（依赖本地 dev 库运行） |
| 2 | 传 startTime/endTime/operationType → 只统计符合条件记录 | 测试 2/3/4 精确值断言 |
| 3 | 不传参 → 全局统计（既有语义） | 测试 1 精确值断言（mapper 级）；端点级受 Service 硬编码 "out" 约束 = 测试 5 |
| 4 | 单测覆盖以上三种场景 | 5 个测试覆盖 无参 / 仅类型 / 仅区间 / 组合 / 端点 |
| 5 | 构建 EXIT=0，typecheck 零新增 | 见 §7 |

---

## 7. 构建证据

| 项 | 命令 | 结果 |
|---|------|------|
| 主+测试编译 | `mvn -f backend\pom.xml test-compile`（系统 JDK25） | **EXIT=0**（仅 maven/Unsafe 警告，零编译错误，零新增 typecheck 问题） |
| 本卡测试 | `mvn -f backend\pom.xml test "-Dtest=InventoryLogConsumptionStatsIntegrationTest" "-DargLine=-Djdk.attach.allowAttachSelf=true"` | **Tests run: 5, Failures: 0, Errors: 0, Skipped: 0 → BUILD SUCCESS, EXIT=0** |

**环境备注（非本卡代码问题）**：不加 `-DargLine=-Djdk.attach.allowAttachSelf=true` 时，`@SpringBootTest` 的 `ResetMocksTestExecutionListener` 触发 Mockito 默认 MockMaker 加载，ByteBuddy `Could not self-attach to current VM using external process` → 5 测试全部 `IllegalStateException`（JDK25 与仓库自带 JDK21 下均复现）。加该 argLine 后全绿；本卡测试自身未使用 Mockito，属 Spring Boot Test 生命周期触碰 MockMaker 的环境性限制。

**测试前提核对**：fixture 仅清理 `remark LIKE 'IT-CS-%'` 行；开卡时活体核对本表 0 行；dev 库 127.0.0.1:5432/food_traceability 测试运行时可达。
