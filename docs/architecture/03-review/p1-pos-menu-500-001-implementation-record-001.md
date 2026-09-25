# P1-POS-MENU-500-001 — 实施记录 001

## 0. 状态

| 项 | 值 |
|----|------|
| Task ID | `P1-POS-MENU-500-001` |
| Stage | **IMPLEMENTED_QA_PENDING（2026-09-25）** |
| 优先级裁决 | **P1**——前置澄清 1：前端在用 `/v1/pos/api/menu`（`frontend-pos/src/api/posApi.ts:416` + `CustomerOrder.vue:342`） |
| 前置澄清 2 | **生产 schema = category_id**——Flyway `V1.0.0.100__init_postgresql.sql:327`（生产真相源）`category_id VARCHAR(50) PRIMARY KEY` |
| 根因裁决 | **实体代码问题**：`FoodCategory` @TableId 默认映射 `id` 列、时间列映射 `create_time/update_time`，与 Flyway `category_id/created_at/updated_at` 全面错位（`resources/sql/*.sql` 的 `id` 主键版本为非权威遗留脚本，疑为实体错位来源） |
| 修复 | 实体列映射对齐 Flyway：`@TableId(value="category_id")`（字段名 id 保持，Java API 零变化）+ `created_at/updated_at/created_by/updated_by`；本地库 `ALTER TABLE ... RENAME create_time→created_at / update_time→updated_at` 对齐（环境动作，已执行） |
| commit | `872c874`（单文件 +6/-5，PG-001 v2：声明=FoodCategory.java、精确 add、staged 审查） |
| 自测 | compile EXIT=0；回归单测 OrderNewServiceImplDeductTest + PosOrderCreateServiceFoodIdMapTest EXIT=0 |

## 1. 活体验证

| 断言 | 结果 |
|------|------|
| `GET /api/v1/pos/api/menu` | **code=0（修复前 500）**；categories=0（本地表无种子数据，非缺陷）+ dishes=1 + combos=1（生菜套餐 13.5） |
| 回归 `/api/v1/pos/api/combos` | code=0，套餐数据不受影响 |

## 2. 风险与限制

- **L-01**：生产 DB 若存在按旧实体建出的 `food_category`（id 主键）实例，须先执行列迁移（Flyway 为准）；本修复以 Flyway schema 为唯一真相源，部署前建议核对生产 information_schema
- **L-02**：本地库 ALTER 为环境动作，未入库（如需幂等迁移脚本可后续补 Flyway Vxx）
- **L-03**：实体仍无 `deleted` 字段，selectList 不过滤逻辑删除（既有行为，保持原状未扩 scope）
