# 工作区 WIP 诊断报告 001（workspace-wip-diagnosis-001）

- **日期**：2026-09-25
- **性质**：只读诊断（未 stash / 未 commit / 未 discard / 未改任何既有文件；`mvn compile` 仅产生 gitignored 构建产物）
- **触发**：P1-COMBO-LEGACY-CLEANUP-001 开卡被 PG-001 门禁拦截（任务板 §24.2），Owner 指令对 ~1196 文件 WIP 做诊断
- **快照基线**：HEAD = `5bf59c9`（与 origin/master 同步）

## 1. 总量分类

**总口径**：`git status` 1655 条 = 修改 1196 + 未跟踪 458 + 删除 1。

**关键修正——"1196 个修改"中 730 个是行尾幻影**：`core.autocrlf` 环境下 LF/CRLF 不一致导致 status 报 M，但 `git diff --numstat` 显示**仅 467 个文件有真实内容改动（+17295/-6479）**，其余 730 个与 HEAD 内容逐字节一致（仅行尾差异，`git checkout -- <file>` 即可无损消除）。

### 真实内容改动（467 文件）按区域

| 区域 | 文件数 | 说明 |
|------|--------|------|
| frontend/ | 218 | 含 vue 视图、测试、脚本 |
| backend/ | 216 | 见 §2 抽查 |
| employee-frontend/ | 13 | |
| frontend-pos/ | 5 | |
| docs/ | 5 | |
| miniprogram/ | 4 | |
| frontend-kitchen/ | 2 | |
| 其他（.agents、launcher.bat、rm-receiving-task-board.md） | 4 | |

### 未跟踪（458 文件）

| 区域 | 数量 | 性质 |
|------|------|------|
| docs/quality | 189 | 历史会话产出的质量报告（从未入库） |
| docs/architecture | 98 | 同上（scope/记录类） |
| backend/src | 57 | **41 个新 java 文件**（见 §2） |
| frontend/src | 21 | |
| docs/design | 18 | |
| 根目录散件（scan-report*.csv、sprint-* 快照等） | ~70 | 会话产物 |
| .agents/.claude/scripts/SDK 开发包（中文目录名） | ~90 | 工具/第三方 |

### 删除（1 文件）

`frontend/src/api/hr/recruitment-mock.ts`（未提交的删除）

## 2. WIP 内容性质（抽查）

**结论：不是单一性质，至少四类混合，且分属不同批次**：

1. **Canonical 迁移（W1-EC 批次）**——主力。证据：`PosOrderPaymentServiceImpl`（+362/-216）引入 `W1-EC-04A: canonical 订单表 Mapper（orders 表）`、`W1-EC-04A-R: 支付记录 Mapper（order_payment_records）`、`W1-EC-05: 收支流水 FinanceRecordService`；diff 中 `orders_legacy` 出现 49 次；`PosOrderQueryServiceImpl`（+132/-87）同族。未跟踪新文件含 `CanonicalOrderCommand` / `CanonicalOrderItemCommand` / `MaterialDeductionContext` / `MaterialDeductionFailureException`。
2. **新功能**——供应商域（`Supplier.java` +166、`SupplierCreateDTO` +182、`SupplierUpdateDTO` +183、`SupplierVO` +171，纯新增字段如 `shortName`）、`AccountBalance*`（+232/+220）、`finance/BankPaymentRecordController`（新文件）、`OnnxOcrServiceImpl`（+115）。
3. **Bug fix**——`ComboIngredientMapper`：补 `@Select` 绑定修复 BindingException（注释标日期 2026-09-22："此前该方法无注解SQL也无XML…套餐扣料路径整体不可用"）。
4. **安全加固**——`ComboInventoryController` / `DishCostController` / `DishInventoryController` 批量加 `@PreAuthorize` 权限注解。

半成品迹象：新增行中 TODO/FIXME 仅 3 处，无结构性烂尾；配合 §6 编译通过，**WIP 是一致的工作状态，非半成品堆**。

## 3. WIP 的 origin

- **任务板**：存在正式批次——`W1-EC-01｜POS 收银写入迁移（阶段二-A）`（任务板 L6228 起）、W1-EC-04A/04B（canonical orders + 统计验证）、W1-EC-05（收支流水）。WIP 中的标记字符串与之逐一对应。
- **git log**：`15f712f`（W1-EC-04B-1-R 任务卡）、`dbb99c1`（修正测试数据基线）、`6b54411`（开发者证据）——该批次**部分已入库**，工作区是其余部分。
- **scope 文档**：`docs/architecture/03-review/business-item-canonical-*` 系列（冲突边界/语义重评/独立评审）与 W1-EC 同源。
- 供应商/账户余额/OCR 等新功能**未找到**对应任务板条目（抽查未见），origin 不明，需 Owner 指认批次。

## 4. WIP 与 P1-COMBO-LEGACY-CLEANUP-001 的关系

**重大修正（较任务板 §24.2 的 BLOCKED 判断降级）**：

| 本卡目标 | WIP 是否真实改动 |
|----------|------------------|
| `PosApiServiceImpl`（含 getFullMenu，L167） | **否**（M 为行尾幻影） |
| `KitchenScanServiceImpl` | **否**（幻影） |
| `OrderMaterialRequirementServiceImpl` | **否**（幻影——§24.2 此前判断"在 WIP 修改集中"系行尾噪声误导） |
| `DatabaseFixConfig` / `DataFixConfig` | **否**（幻影） |
| `ComboIngredientMapper`（旧表 mapper） | **是**（BindingException 修复，+@Select 读旧表） |
| dish_combos 新表基础设施 | **部分已存在**：`DishComboNew` / `DishComboNewMapper` 为**未跟踪新文件**；已提交的 `DatabaseFixConfig`（HEAD）已引用 dish_combos |

- **WIP 尚未做本卡计划的事**：无任何 `getFullMenu` 切换、无 legacy 三文件改读新表、无旧表废弃动作（diff 与未跟踪文件中均无）。
- 真实域内重叠仅 4 文件（ComboIngredientMapper + 3 个 Dish/Combo Controller 的权限注解），与本卡动作方向不冲突（本卡动 service 层与数据源，不动这些注解）。

## 5. WIP 与已收口卡的关系

- **ENV-1（FOODID 卡，PosOrderCreateServiceImpl +681 行 WIP）**：**已被吸收**——`aec5c45` 以 +767/-? 提交了该文件（canonical WIP + P0 编译修复混合入库，即 ENV-2 登记的内容混合）。当前该文件对 HEAD **无真实 diff**（仅幻影）。ENV-1 所指的 WIP 已不复存在。
- **ENV-2（combo 卡 commit `aec5c45` 内容混合）**：其"P0 编译修复"成分已入库；当前工作区 WIP 是其**延续层**（W1-EC-04A/05 在别的文件上继续），而非同一批未提交内容。

## 6. 可编译性

- **`mvn -DskipTests compile` = BUILD SUCCESS（exit 0）**（JDK：`/h/jdk-25.0.1.8-hotspot`，Temurin 25.0.1）——工作区（含 WIP 与未跟踪新文件）是**自洽可编译状态**。
- 环境问题：`mvn` 内置 JAVA_HOME 指向 `H:\fuwu\jdk-17.0.17+10` **不存在**（默认编译直接失败），需显式覆盖 JAVA_HOME——建议另行登记。

## 结论与给 Owner 的处置建议

1. **WIP 真实体量 ≈ 467 文件真实改动 + 458 未跟踪**，其中约 44%（730/1196）的"M"是无害行尾幻影，可用 `git checkout -- <paths>` 无损清除，显著降噪。
2. **本卡（LEGACY-CLEANUP）的真实阻塞度低于 §24.2 的评估**：本卡全部目标文件内容与 HEAD 一致；理论上"幻影清理 + 最小化处置 4 个重叠真实文件"后即可满足 PG-001 精神启动。但 PG-001 字面要求全工作区 clean，**是否放宽/如何处置 467 真实改动文件由 Owner 裁决**。
3. WIP 建议按批次分拆入库（canonical 迁移 / 供应商等新功能 / bug fix / 安全加固），其中供应商等新功能缺任务板条目，需先补登记。
4. `docs/quality`+`docs/architecture` 287 个未跟踪治理文档建议单独入库（历史上多次"未 commit"约定的累积）。

---
*诊断方法：`git status/ls-files/log` + `git diff --numstat` 全量比对 + 关键文件 diff 抽查 + `mvn compile`。零修改、零 stash、零 commit（本报告文件除外）。*
