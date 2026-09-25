# P1-NEW-FOOD-LEGACY-SYNC-001 — 实施记录 002（DS 抽检整改）

- **日期**：2026-09-25
- **来源**：DS 抽检 `docs/quality/P1-NEW-FOOD-LEGACY-SYNC-001-sampling-review.md` 判定"验证不完整"（单测缺失 + D1~D3 口径偏差）
- **commit**：见 git log（代码 1 文件 + 测试 2 文件 + 文档）

## 1. 整改内容

| 项 | 整改 |
|----|------|
| D1 | `syncLegacyFood` insert 分支 status 映射对齐 sync：1→active / 0→inactive / 2→sold_out（修复前 2 落 inactive）；`ensureLegacyFoodRow` 同口径对齐 |
| D2 | insert 显式 `create_by='system'` / `update_by='system'` |
| D3 | insert 显式 `deleted=0`（不再依赖 DB 列默认值） |
| 单测 | `FoodLegacySyncTest`（5）：create status=1→active 含审计字段 / status=2→sold_out / update stock+price 双写（1234 分→12.34）/ updateStatus 售罄同步 / 无分类兜底"未分类"；`PosOrderEnsureLegacyRowTest`（3）：行缺失补行（字段级断言）/ 行存在幂等 return / 双缺失静默 return——共 **8/8 PASS** |

## 2. 活体验证（D1 专项）

新建 status=2 菜品 → legacy food 行：**food_status=sold_out、create_by=system、deleted=0**（FD260926002，QA-D1-售罄验证菜）——三处整改全部实证。

## 3. 残留

Pricing 单侧改价 / OrderTimeoutTask 单侧回补 / delete 无 legacy 对应——维持登记不修（见 001）。

---

*DS 复核：见 sampling-review 追加段（补测后升级 PASS）。*
