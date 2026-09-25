# Schema 治理 — Flyway 单一真相源

- **日期**：2026-09-25
- **来源**：P0-SCHEMA-SINGLE-SOURCE-001（FoodCategory 实体/DB 主键错位事故 P1-POS-MENU-500-001 的根治项）
- **性质**：治理规则文档；执行细则见同目录 `process-guards.md` PG-003

## 规则

1. **Flyway 是唯一 schema 真相源**：所有表结构以 `backend/src/main/resources/db/migration/V*.sql` 为准。任何文档、脚本、口头描述与 Flyway 冲突时，以 Flyway 为准。
2. **禁止新增手写 schema 脚本**：不得再向 `backend/src/main/resources/sql/`（已归档至 `docs/archive/legacy-sql/`）或任何非 Flyway 位置新增建表/改表 SQL；schema 变更一律新增 Flyway migration（`V<版本>__<描述>.sql`，只增不改）。
3. **实体必须与 Flyway 对齐**：`@TableId` / `@TableField` 映射的列名必须存在于对应表的 Flyway 定义中；新增/改表时同步检查引用实体。
4. **对齐检查**：每次涉及 schema 或实体的卡片收口前运行 `scripts/check-schema-alignment.py`，新增错位即修复或单独立卡，不得留盲。

## 历史背景（事故链）

- `resources/sql/postgres-schema-v0.12.sql` 等遗留脚本与 Flyway 并存且互冲突（如 `food_category`：遗留脚本 `id` 主键 vs Flyway `category_id` 主键），误导实体编码 → `FoodCategory` 实体错位 → `/v1/pos/api/menu` 500（P1-POS-MENU-500-001，2026-09-25 修复）。
- 2026-09-25：41 个遗留 sql 脚本（含 `postgres-schema-v0.12.sql`）归档至 `docs/archive/legacy-sql/`（git mv，保留历史；零运行时引用已核实）。

## 边界

- 本治理不追溯重写 Flyway 既有 migration（只增不改原则）。
- `db/init-data.sql`（种子数据）与 Flyway 同目录管理，不属于本次归档范围。
