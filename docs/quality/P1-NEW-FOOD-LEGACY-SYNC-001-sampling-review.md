# P1-NEW-FOOD-LEGACY-SYNC-001 — DS 抽检报告（sampling-review）

- **日期**：2026-09-25
- **审查对象**：commit `ae6ddba`（F6 修复）；实施记录（对话内）+ `docs/quality/f6-new-food-sync-diagnosis-001.md`
- **性质**：DS 抽检（5 靶点），不替代 QA；只读取证

## 靶点 1 — commit 精确 diff — **PASS（口径说明）**

- `ae6ddba` 恰 **4 文件**：2 代码（FoodServiceImpl / PosOrderCreateServiceImpl）+ 2 文档（03-foods-recipes.md / 任务板）——**无第五文件**
- 代码 numstat：FoodServiceImpl **+66/-3**、PosOrderCreateServiceImpl **+37/-0**，与实施报告完全一致
- 口径说明：指令问"是否仅 2 文件"——指代码文件则 ✅；commit 总文件数为 4（含 2 文档）

## 靶点 2 — 双写逻辑（syncLegacyFood）— **PASS，附 3 处口径偏差**

| 检查项 | 证据 | 结论 |
|--------|------|------|
| 分类映射 | `COALESCE(fc.category_name,'未分类')`（sync L787）vs `categoryName != null ? name : "未分类"` | ✅ 同口径 |
| 分→元 | `BigDecimal.valueOf(salePrice, 2)`（1990→19.90）vs sync `sale_price / 100.00` | ✅ |
| 状态映射 | sync：`CASE 1→active, 0→inactive, 2→sold_out`；**update 分支**同构 ✅；**insert 分支** `status==1 ? active : inactive`——**status=2 落 inactive 而非 sold_out** | ⚠ 偏差 D1 |
| create_by/update_by | sync 显式 `'system'`；双写 insert 未写 → 实测落库行两列为空 | ⚠ 偏差 D2（列 nullable，无运行时影响；展示口径差异） |
| deleted | sync 显式 `f.deleted`；双写 insert 未写 → **DB 列默认 0 兜底**（实测行 deleted=0） | ⚠ 偏差 D3（当前安全，依赖列默认值；若 DDL 变更成坑） |
| 活体实证 | FD260926001 行：food_price=25.00、stock=49、food_status=active、deleted=0 | 功能正确 |

D1~D3 均为低危口径偏差（功能正确），建议随补测小修对齐。

## 靶点 3 — 自愈逻辑（ensureLegacyFoodRow）— **PASS**

| 检查项 | 证据 | 结论 |
|--------|------|------|
| 兜底条件 | 两处调用（L554 主路径 / L753 快速单）均在 `deductResult == 0` 后触发 | ✅ |
| 重试次数 | ensure 后**单次**重试 deductStock，无循环结构 | ✅ |
| 幂等 | `selectByFoodCodeForUpdate != null` 即 return（行已存在不重复补） | ✅ |
| 与 B2 拒绝兼容 | ensure 全程 try/catch（失败仅 log.warn）→ 自愈失败**不改变**原 `BusinessException(400, "库存不足")` 语义；B2 的 400（套餐不存在/无配料）发生在扣减之前，互不干扰；无 400→500 升级路径 | ✅ |
| 真库存不足语义保留 | 行存在但 stock 不足：ensure 检测行存在直接 return → 重试仍 0 → 仍抛 400"库存不足"（此时语义为真） | ✅ |

## 靶点 4 — 单测覆盖 — **验证不完整**

- `ae6ddba` **不含任何测试文件**（`git show ae6ddba --name-only | grep test/` = 0）
- 活体验证（新建即下单 T20260926001 + legacy 行断言）与回归单测（2 既有套件 EXIT=0）部分弥补，但 `syncLegacyFood`（insert/update 两分支 + D1 偏差）与 `ensureLegacyFoodRow`（幂等/自愈失败降级）**无单测固化**
- **按 DS 判定规则：验证不完整**——需补单测（建议 ≥3 个：双写 insert 含 status=2 映射 / 双写 update / ensure 行缺失补行+重试）

## 靶点 5 — WIP 隔离完整性 — **PASS**

- 本卡两文件开卡时**零 WIP 差异**（无 stash/部分提交需要，比 F4 卡更简单）
- 提交后工作树两文件对 HEAD diff = **0 行**；HEAD 后无再改（`git diff ae6ddba HEAD` 两文件 = 0）
- 工作区 M 总数维持 **463**（WIP 未被触碰）；FD260926001 验证数据为业务测试数据（DB 操作，非 git 范围）

## 裁决

| 项 | 结论 |
|----|------|
| 代码功能 | **PASS**（活体 + 静态证据链完整；F6 断链已消除） |
| 单测覆盖 | **验证不完整**（卡片 commit 无测试；按判定规则如实降级） |
| 口径偏差 | D1（status=2 映射）/ D2（create_by 空）/ D3（deleted 隐式默认）——低危，随补测卡对齐 |
| **总判定** | **验证不完整（代码 PASS + 单测缺失）** |
| 移交 | ① 补单测卡（syncLegacyFood×2 + ensureLegacyFoodRow×1 + D1 对齐）② D2/D3 是否对齐 'system'/显式 0 由 Owner 定 ③ QA 独立验收可在补测后合并进行 |
