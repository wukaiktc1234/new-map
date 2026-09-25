# P0-SCHEMA-SINGLE-SOURCE-001 — 实施记录 001

## 0. 状态

| 项 | 值 |
|----|------|
| Task ID | `P0-SCHEMA-SINGLE-SOURCE-001` |
| Stage | **CLOSED_WITH_REGISTERED_LIMITATION（2026-09-25，§4 收口）** |
| 背景 | P1-POS-MENU-500-001 根因之一：遗留 schema 脚本与 Flyway 并存冲突（`food_category`：遗留 `id` vs Flyway `category_id`）误导实体编码 |

## 1. 范围三件事

| # | 范围项 | 结果 |
|---|--------|------|
| 1 | 确立 Flyway 唯一真相源 | **完成**：`docs/project-context/schema-governance.md`（治理文档）+ **PG-003** 入 `process-guards.md`（禁止新增手写 schema 脚本、实体对齐义务、违规处置） |
| 2 | 清理冲突 schema 脚本 | **完成**：`backend/src/main/resources/sql/` 全部 **41 个脚本**（含 `postgres-schema-v0.12.sql`）`git mv` 归档至 `docs/archive/legacy-sql/`；归档前核实零 WIP 修改、零运行时引用（仅两处注释提及）；Flyway `db/migration/` 40 个 migration 原样保留（只增不改未触碰） |
| 3 | 对齐检查机制（最小实现） | **完成**：`scripts/check-schema-alignment.py`——解析 Flyway CREATE TABLE 列集合 vs 实体 @TableId/@TableField 映射，报告错位/无表实体；退出码 1=有错位 |

## 2. 对齐基线（2026-09-25 首跑）

- **299 实体 / 282 Flyway 表；既有错位 163 处**（基线快照：`docs/quality/schema-alignment-baseline-20260925.txt`）
- 构成：主要为**历史建表仅存在于 DatabaseFixConfig/遗留脚本、从未进 Flyway** 的表（NO-TABLE 类，如 deprecated 的 `combo_ingredient`）+ 部分列映射漂移（MISMATCH 类）
- `FoodCategory.java` 修复后 **0 错位**（已通过）
- **消分策略**：不一次性补齐（禁止范围外改实体），随后续涉表卡片按 PG-003 逐个消化；基线文件为比对锚点

## 3. 限制

- L-01：检查为单向（实体列 ⊆ Flyway 列），不做类型/默认值比对（最小实现口径）
- L-02：NO-TABLE 类错位的根治依赖各业务卡补 Flyway migration（与"只增不改"兼容），非本卡范围

---

## 4. 收口记录（2026-09-25）

| 项 | 值 |
|----|------|
| 终态 | **CLOSED_WITH_REGISTERED_LIMITATION** |
| RESIDUALS | **L-01**（单向检查：不做类型/默认值比对）/ **L-02**（NO-TABLE 根治依赖各业务卡补 Flyway，非本卡范围）/ **L-03**（deleted 过滤等既有行为未扩 scope）/ **KL-081**（163 处对齐债，消分初始）/ **KL-082**（47 张黑箱表 schema 仅存于活体 DB，高风险，应急快照已导出） |
| 溯源补充 | NO-TABLE 70 张已定位：归档 scripts 16 + DatabaseFixConfig 1 + 其他 Java 初始化类 6 + 真黑箱 47（精确复核："CREATE TABLE 与表名同行"规则，剔除 6 处注释误报） |
| 下游 | **P0-FLYWAY-COVERAGE-001**（P0，任务板 §24.3b）：23 张转写 + 47 张活体导出比对 |
| 同步回写 | 任务板 §24.2d、`remediation-roadmap.md` 页脚、`docs/project-context/repo-state.md`、KL-081/KL-082 |
