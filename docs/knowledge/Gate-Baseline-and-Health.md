# Gate 基线 · 健康数字 · 判读手册

> 冻结开发期的知识沉淀。目标：**新人在 10 分钟内知道「有哪些闸门 / 现在什么数字算正常 / 看到异常先查哪里」**。
> 所有数字标注实测时间；被引用但未复测的数字标注来源。最近一次实测：**2026-10-28 17:2x**（本地 `food_traceability`）。

---

## 0. 一句话总览

**闸门只保证「没有变得更差」，不保证「现在已经很好」。**
每个 gate 下面都写了「它不能证明什么」—— 那一列比「通过标准」更重要，因为它是后人误把 gate 当万能保证的唯一边界。

---

## ① 全部 Gate 清单

| # | Gate 名 | 脚本 / 命令 | 通过标准 | 失败 EXIT | **它不能证明什么** |
|---|---|---|---|---|---|
| 1 | 迁移版本 gate | `python scripts/qa/migration_version_gate.py` | 版本号解析后唯一 + 命名规范 `V<8位日期>_<3位序号>__desc.sql`（实测 258 个文件，放行 41 个历史白名单） | **2** 阻断 | 只查文件名/版本号。不能证明迁移能跑通、不能证明迁移与实体一致、不能证明生产库已应用 |
| 2 | from-zero（干净库迁移） | `preflight.py --full` 内 `FlywayMigrateRunner` | `ok=true`、`failed=0`、`executed=252`（实测） | **2**（`[RESULT] BLOCKED：干净库迁移 gate 未通过`） | 不能证明**生产库**与干净库同构 —— 生产有手工建表/手工 ALTER 的历史包袱（正是部署缺口的来源） |
| 3 | 部署缺口棘轮 | `python scripts/qa/deploy_gap_audit.py` + `deploy-gap-baseline.txt` | 缺口 ≤ 基线（实测剩余 **16**，基线 33，已还 17） | **3** 警告（低于基线但不为 0）/ **2** 超基线或执行错误 | 只证明「实体声明的表在库里存在」。不能证明表**结构正确**，也不能证明已还的表真的可写（要 e2e/人工验收） |
| 4 | 裸异常棘轮（形态①） | `scripts/qa/runtime-exception-baseline.txt`=**483** + `preflight.py` | `throw new RuntimeException(` 计数 ≤ 483 | **2**（`BLOCKED：裸 RuntimeException 棘轮上升`） | 只能证明**没有新增**。**不能证明这 483 处都合理**（历史欠账照旧） |
| 5 | Result.error 字面量棘轮（形态②） | `scripts/qa/result-error-string-baseline.txt`=**828** + `preflight.py` | `Result.error("字面量")` 计数 ≤ 828（实测 828，较基线 841 已减 13） | **2**（`BLOCKED：Result.error(字面量) 棘轮上升`） | 只保证**形态**（不是 `ErrorCode.X` 重载）。不能证明语义正确、不能证明前端能解析 |
| 6 | 权限 A44（形态自洽） | `python scripts/qa/consistency_check.py`（A44） | `rows=0`，分母 **1737**（`deleted=0`） | **1**（存在 FAIL） | 只证明 `permission_id` 形态合法。不能证明授权**合理**（多给/少给都不管） |
| 7 | 权限 A45（引用可解析） | 同上（A45） | `rows=0`（permission/role 都能落到实体表），分母 1737 | **1** | 只证明 join 不掉行。不能证明这些授权被业务真正使用 |
| 8 | 权限 A46（代码 ↔ 目录） | `python scripts/qa/permission_code_audit.py` | 代码声明的权限码全部在 `permissions` 目录（实测 317 个码、缺失 **0**） | **2** | 静态扫 `hasAuthority('X')` 字面量。**不能证明**运行时鉴权正确（不执行 `@PreAuthorize` 表达式，也不覆盖动态拼接串） |
| 9 | 501 覆盖率 gate | `python scripts/qa/gate_coverage_audit.py` | 缺表对应入口全被 `@UnfinishedFeature` 覆盖（实测已覆盖 14 / **未覆盖 6** / 无入口 23） | **2** | 只查**注解**。不能证明未覆盖的 6 个入口真的会 500，也不能证明已覆盖入口的 501 文案符合契约 |
| 10 | 实体列对拍 | `python scripts/qa/entity_column_audit.py --only-existing-with-data` | `@TableField` 声明列 ↔ `information_schema.columns` 无差异 | **1** 有差异 / **2** 执行错误 | 只覆盖**显式写了列名**的字段。不覆盖 MyBatis XML 手写列名、不覆盖运行期动态 SQL |
| 11 | `*_fen` 影子列校验 | `python scripts/qa/fen_shadow_check.py` | 12 个 `*_fen` 列 NULL 值 100% | **1**（出现非空 → 双写可能已上线而元列未同步） | 只证明「现在没人写 `_fen`」。不能证明将来双写上线时元列一致 |
| 12 | e2e 三步断言（退款归属） | `python scripts/qa/e2e_cash_recon.py`（三步段） | 三步①~⑤ 共 **12 条**全 PASS（增量 == 0 / 400 契约文案 / 重算不累加） | **1** 断言失败 / **2** 环境未就绪 | 只覆盖**单终端串行**流程。不能证明并发确认、多终端竞争、UI 展示 |
| 13 | e2e 全量 | `python scripts/qa/e2e_cash_recon.py`（不带 `--only-triple`/`--skip-boundary-wait`） | `PASS=56 FAIL=0`、`EXIT=0`（2026-10-28 实测，含 610s 设计内等待，整套约 22 分钟） | **1** / **2** | 跳过段（`--skip-boundary-wait`）覆盖不到；也不覆盖其它业务线（对账工作台/报表） |
| 14 | SQL 一致性巡检 | `python scripts/qa/consistency_check.py` | `PASS=27 / FAIL=0 / skipped=3`（44 条断言，27 判定） | **1** 有 FAIL / **2** 执行错误 | `skipped_no_data` 的断言（分母=0）**这一轮等于没验**；skipped 变大＝覆盖变小 |
| 15 | 新表体检（2026-10-28 起） | `python scripts/qa/table_health_check.py --tables <新表清单>`（`preflight --full` 跑 `new-tables.txt`） | 六项全通过（主键 / `deleted` / `create_time`+`update_time` / `_id` 有索引 / 金额 `bigint` / 时间列 `timestamptz`） | **2** | 只证明**有**索引、`_id` 列**有**覆盖；不证明索引最优，也不管既有表老账（见 `tech-debt-schema.md`） |
| 16 | 入库 location 强约束 | `python scripts/qa/schema_guard.py` | S1~S4 全 PASS（NOT NULL、两条 FK、`location_id is null` = 0） | **1** FAIL / **2** 执行错误 | 只覆盖 `purchase_stockin(s)_items` 一个业务；不证明其它表外键完整 |
| 17 | preflight 总闸 | `python scripts/qa/preflight.py` / `--full` | 快档：棘轮 + `test-compile` + 巡检；真档：再加 `mvn test` + from-zero + 部署缺口 + 新表体检 | **0** 通过 / **2** BLOCKED / **3** WARN | EXIT=3 的含义是「**不可发版**」：`--full` 必须 EXIT=0 才准发版 |
| 18 | 安全回归（S1~S6） | `python scripts/qa/security_regression.py`（`preflight --full` 自动跑，硬门禁） | `PASS=14 FAIL=0`、`EXIT=0`、清理残留 `orders_left=0 / shifts_left=0` | **1** 有 FAIL / **2** 未就绪（自检未过或后端没起） | 只覆盖 S1~S6 六类**已知**攻击面；**未覆盖**：IDOR（越权访问他人订单）、批量导出限流、并发竞态（同 key 并发下单）、CSRF —— 这些仍靠人工/AI 审计。**安全回归绿了 ≠ 系统安全** |

---

## ② 当前健康数字（一张表）

| 指标 | 当前值（2026-10-28 实测） | 正常范围 | 恶化定义 |
|---|---|---|---|
| `mvn test` | **766 tests / 11 Failures / 5 Errors / 5 Skipped** | 失败类集合 ⊆ §2 备注的 11 个已知欠账类 | 出现**新类名**，或 11F → 12F |
| e2e 全量 | **PASS=56 / FAIL=0**，EXIT=0 | FAIL=0 且 PASS ≥ 56 | FAIL>0，或 PASS 下降（断言被删/被跳过） |
| e2e 快速（`--skip-boundary-wait`） | **PASS=52 / FAIL=2** | **52/2 是正常的**：两条 FAIL 是「跳过边界等待」的直接后果 | FAIL>2，或 PASS < 52 |
| 巡检 `consistency_check` | **PASS=27 / skipped=3 / FAIL=0** | FAIL=0；skipped ≤ 3 | FAIL>0；或 skipped 变大 |
| 巡检 A39 分母 | **4**（干净基线） | 4 ~ 8 | 清理 e2e 后仍 >4 → 有残留夹具 |
| from-zero 迁移 | `executed=252`、`failed=0`、`target=2026022701` | `ok=true` 且 `failed=0` | 有 `failed`，或 `executed` 下降（迁移被删） |
| 部署缺口 | **16**（基线 33，已还 17） | ≤ 33 且持续下降 | > 33（超基线 → preflight BLOCKED） |
| 裸异常棘轮 | **483** | ≤ 483 | 484+ |
| `Result.error(字面量)` 棘轮 | **828**（基线 841，已减 13） | ≤ 828 | 829+ |
| A46 权限码 | 317 个码 / 目录缺失 **0** | 缺失 0 | 缺失 > 0 |
| `*_fen` 影子列 | 12 列全 NULL | 非空值 0 | 任一列非空 → 先确认双写是否上线 |
| 501 覆盖 | 已覆盖 14 / **未覆盖 6** / 无入口 23 | 未覆盖 = 0 | 未覆盖 > 6 |
| 迁移文件数 | 258（白名单 41） | 单调不减 | 减少＝有人删迁移 |

### 备注 1：`mvn test` 的 11 个已知欠账类（2026-10-28 冻结点）

```
1.  com.foodtraceability.integration.OrderManagementIntegrationTest
2.  com.foodtraceability.integration.SystemSettingsDatabaseTest
3.  com.foodtraceability.integration.SystemSettingsIntegrationTest
4.  com.foodtraceability.service.finance.impl.CostRecordServiceImplTest
5.  com.foodtraceability.service.finance.impl.InvoiceReimbursementServiceImplTest$PayTest
6.  com.foodtraceability.service.finance.impl.PayableServiceImplTest
7.  com.foodtraceability.service.FoodTracePromptServiceTest
8.  com.foodtraceability.service.impl.SysDictServiceImplTest$EdgeCaseTests
9.  com.foodtraceability.service.PositionSortingTest
10. com.foodtraceability.service.SysSettingServiceTest
11. com.foodtraceability.utils.FoodCodeGeneratorTest
```

- 1~3 是**集成测试**（依赖 DB 状态/单测上下文）；4~11 是**纯单测既有断言问题**（如 `FoodCodeGeneratorTest` 期望 `D00006` 实得 `D00001`、`PositionCodeGeneratorTest` 期望 `MGR` 实得 `GM`、`PositionSortingTest resultPage null`）。
- **Errors 会在 5 ~ 39 间跳**：`SystemSettings*` 依赖 Flyway 全链，本地测试库迁移链是否完整决定它是 5E 还是 39E。**看失败类名集合，不要只看数字**。

### 备注 2：与 Lead 口径不一致的两处（以实测为准，别当断言坏了）

| 指标 | Lead 口径 | 实测 | 说明 |
|---|---|---|---|
| from-zero `executed` | 257 | **252** | 迁移文件 258 / 白名单 41；不同时点跑的，`target=2026022701` 一致即视为同一条链 |
| 部署缺口 | 45 | **16**（基线 33，已还 17） | 45 更像「实体表 vs 生产库缺表」的**粗口径**（含无活引用的表）；棘轮只算「缺表**且有活引用**」 |

---

## ③ 每次提交前必须跑什么（三档）

| 档位 | 触发场景 | 命令 | 耗时 | 拒绝进入下一档的条件 |
|---|---|---|---|---|
| **日常** | 任何提交前 | `python scripts/qa/preflight.py` | **< 1 分钟** | EXIT≠0（棘轮上升 / test-compile 失败 / 巡检 FAIL） |
| **改迁移 / 改实体后** | 动了 `db/migration/*.sql`、`entity/*.java`、`@TableField` | `mvn -o -B clean test-compile`<br>`python scripts/qa/migration_version_gate.py`<br>`python scripts/qa/entity_column_audit.py` | 2~5 分钟 | 任一 EXIT≠0（版本重复 / 列名对不上） |
| **发版前** | 交付 / 合并主干前 | `python scripts/qa/preflight.py --full`（clean + 全量 test + from-zero + 全 gate + 新表体检）<br>`python scripts/qa/e2e_cash_recon.py`（全量，约 22 分钟） | 30~50 分钟 | `--full` **必须 EXIT=0**；EXIT=3 不得发版；e2e 必须 FAIL=0 |

补充约定：
- e2e 跑完**必须**按 tag 清理 `python scripts/qa/e2e_cleanup.py --tag <tag> --apply`，再复跑巡检确认 A39 分母回到 4。
- 跑完 e2e 必须核对四张夹具 slip 的 `rcf == attr_sum`（见 `docs/audit/e2e-boundary-refund-fixtures.md` §2）。

---

## ④ 判读表：看到什么数字 → 先查哪里

| 你看到的 | 含义 | 第一步 |
|---|---|---|
| `mvn test` **12F**（多 1 个） | 新增 1 个失败 | `git stash` 后再跑一次对照；确认是自己引入还是环境（DB/迁移）导致 |
| `mvn test` Errors **5 → 39** | 大概率测试库**迁移链断了**（`SystemSettings*` 依赖全链） | 看 `SystemSettingsDatabaseTest` 失败信息里第一条 Flyway 报错 |
| 部署缺口 **16 → 17** | 有人建了实体（`@TableName`）没建表，且**有活引用** | `deploy_gap_audit.py` 输出的表清单 → 补迁移或补 `@UnfinishedFeature` |
| 部署缺口 **16 → 15** | 有人真的把表补上了 | 顺手 `table_health_check.py --tables <那张表>` 确认六项 |
| 迁移版本 gate **EXIT=2** | 版本号解析后重复（`V20261028_001` 与 `V20261028_01`） | 看 offenders 列表改文件名序号；**不要**改白名单 |
| from-zero **failed>0** | 干净库跑不过 → 新环境/CI 必炸 | 按报错定位那条迁移；生产能跑 ≠ 干净库能跑 |
| 棘轮 **483 → 484** | 有人新增一处裸异常 | 找新出现的 `throw new RuntimeException(`；改 `BusinessException` 后基线调回 483 |
| `Result.error` **828 → 829** | 新增字面量错误码 | 换 `ErrorCode.X` 重载，基线回填 |
| 巡检 **A44/A45 rows=1** | 一条授权形态非法/引用不可解析 → **静默失效** | `permission_matrix_diff.py` 定位；归一到 `PERM_*`；**别直接 UPDATE** |
| 巡检 **A39 rows=1** | 缴款单恒等式不成立（产品算错 / 夹具污染） | 先看夹具表 `rcf == attr_sum`：不等＝夹具污染；相等＝产品缺陷 |
| 巡检 **A39 分母 4 → 6/8** | e2e 数据没清（T-E2E 班次窗口结束就进分母） | `e2e_cleanup.py --tag <tag> --apply`；**不是断言坏了** |
| 巡检 **skipped 3 → 5** | 有断言分母变 0（数据没了） | 查是否清理过头/夹具被删；skipped 变多＝覆盖变少 |
| e2e **52/2**（`--skip-boundary-wait`） | **正常**：两条边界断言拿不到数据 | 想真验证就跑全量 |
| e2e 全量 **FAIL=2 且都在三步段** | 十有八九**夹具隔离**问题 | 看是否 `[NOT_READY] 夹具隔离守卫`；修夹具，不是改断言 |
| e2e **EXIT=2** | 环境未就绪（后端没起/登录失败/端口占用） | 先健康检查；`start-backend.cmd` 前先 `mvn -o -B -DskipTests compile`（否则 `NoClassDefFoundError: InventoryCheckAsset`） |
| 新表体检 **EXIT=2** | 新表缺主键/`deleted`/时间列/索引，或金额用 `numeric` | 看逐表问题；金额必须 `bigint` 存**分** |
| `fen_shadow_check` **EXIT=1** | `*_fen` 出现非空值 → 双写可能已上线 | 立刻确认双写与元列同步，否则静默错账 |
| `preflight --full` **EXIT=3** | 巡检分母不足（skipped>50%）或 `mvn test` 红 | **不得发版**；先补覆盖或修测试 |

---

## ⑤ 为什么每个 gate 都要写「它不能证明什么」（硬要求）

三个真实教训，都来自「把 gate 当万能保证」：

1. **审计工具自己错了**：`permission_matrix_diff.py` 一开始用 `permission_id::text` 当 join 键 + 没按 `deleted=0` 过滤，
   把 **1732 条正常授权**全判成「未解析」，还差点让 95 条真问题藏一轮。修好后加了 `--self-test`（1 主键/1 兼容/1 未解析三方判定）。
   → **gate 必须能自证**：喂它已知的坏样本，它必须报红。
2. **棘轮只证明「没变差」**：483 处裸异常是历史欠账，棘轮通过 ≠ 这 483 处合理；`Result.error` 828 同理。
3. **静态扫描 ≠ 运行期正确**：`entity_column_audit` 只看 `@TableField` 显式列名，`gate_coverage_audit` 只看注解，
   `permission_code_audit` 只看字面量权限码 —— 能挡「已知形态的错」，挡不住「新形态的错」。

**结论**：gate 的作用是**防回归**，不是**正确性证明**。要证明正确性只有一条路：**e2e 真跑一遍 + 巡检对拍 + 人工看关键页面**。

---

## 附：文件索引

| 用途 | 路径 |
|---|---|
| 总闸 | `scripts/qa/preflight.py` |
| 巡检 | `scripts/qa/consistency_check.py` / `.sql`（44 条断言） |
| 棘轮基线 | `scripts/qa/runtime-exception-baseline.txt`（483）、`result-error-string-baseline.txt`（828）、`deploy-gap-baseline.txt`（33）、`mvn-test-baseline.txt` |
| 迁移 gate + 白名单 | `scripts/qa/migration_version_gate.py`、`migration-legacy-allowlist.txt`（41 个历史命名） |
| 权限三断言 | A44/A45 在 `consistency_check.sql`；A46 在 `scripts/qa/permission_code_audit.py` |
| 缺表/覆盖/新表体检 | `scripts/qa/deploy_gap_audit.py`、`gate_coverage_audit.py`、`table_health_check.py`、`new-tables.txt` |
| 缺表分类与债务 | `docs/audit/missing-tables-triage.md`、`docs/audit/tech-debt-schema.md` |
| e2e 夹具期望状态 | `docs/audit/e2e-boundary-refund-fixtures.md` |
