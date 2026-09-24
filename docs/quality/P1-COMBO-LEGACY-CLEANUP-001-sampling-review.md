# P1-COMBO-LEGACY-CLEANUP-001 — DS 抽检报告（sampling-review）

- **日期**：2026-09-25
- **性质**：DS 抽检（5 靶点抽样，非全审；不替代 QA 独立验收）
- **审查对象**：commit `05d4404`（代码）/ `58329c8`+`11e05d4`（实施记录与任务板回写）/ `docs/architecture/03-review/p1-combo-legacy-cleanup-001-implementation-record-001.md`
- **方法**：只读取证（`git show/diff/stash show` + grep），未改代码、未重跑全流程、未重审 Scope

## 靶点 1 — commit `05d4404` 精确 diff — **PASS**

- `git show 05d4404 --name-only` 恰 **4 个文件**（PosApiServiceImpl / OrderMaterialRequirementServiceImpl / KitchenScanServiceImpl / ComboIngredientMapper），**无第五文件**
- stat = **+55/-40**，与实施记录 §0/§2 完全一致
- `git diff --cached` 前审查证据：unstaged M 数 466→463 与 4 文件入 staged 吻合（PG-001 v2 第 3/4 条闭环）

## 靶点 2 — PosApiServiceImpl 切表正确性 — **PASS**

| 检查项 | 证据（committed 版本） | 结论 |
|--------|------------------------|------|
| 数据源切换 | L128 `dishComboNewMapper.selectList(...DishComboNew...)`；L137-138 `comboIngredientNewMapper.selectList(...ComboIngredientNew::getComboId...)`；旧 mapper/entity 引用计数 = **0**（grep `DishComboMapper\|ComboIngredientMapper[^N]\|DishCombo\b\|ComboIngredient\b`） | ✅ 真实切到 dish_combos/combo_ingredients |
| 价格 分→元 | L137-138 `BigDecimal.valueOf(combo.getComboPrice(), 2)`（Long 分 → scale-2 元，1990→19.90）；食材价 `BigDecimal.valueOf(food.getSalePrice(), 2)` | ✅ 与 DatabaseFixConfig 同步的 `combo_price / 100.00` 口径一致 |
| status 映射 | L130 `.eq(DishComboNew::getStatus, 1)`；同步侧 `CASE dc.status WHEN 1 THEN 'active'`（DatabaseFixConfig L846）双向印证 1=active | ✅ |
| 食材反查走 FoodNewMapper | L82/L144 `foodNewMapper.selectById(ing.getFoodId())`（Long=foods.food_id）→ `getFoodCode()` 喂给 POS 菜单 | ✅ |

## 靶点 3 — OrderMaterialRequirementServiceImpl 切表正确性 — **PASS**

- L74-76：`ComboIngredientNewMapper` + `.eq(ComboIngredientNew::getComboId, Long.valueOf(comboId))`——真实切新表；`Long.valueOf` 依赖调用方传 dish_combos.combo_id 空间（当前无生产调用方 = 实施记录 L-02，风险已登记）
- foodCode 解析链路：L82 `foodNewMapper.selectById(ing.getFoodId())` → L83-85 `getFoodCode()` → L86 `generateRequirementsForDish(foodCode, foodNew.getFoodName(), ...)`——**中转正确**
- 下游配方链：diff 中配方相关仅 4 行 = 调用点参数名变化（foodId→foodCode）+ 构造器签名换位；`generateRequirementsForDish` 方法体与 `dishRecipeMapper` 链路**零触碰** ✅

## 靶点 4 — KitchenScanServiceImpl 死依赖移除 — **PASS**

- committed 版本 `grep -c comboIngredientMapper|ComboIngredientMapper` = **0**（字段/构造器参数/赋值全净；该文件 import 为 `mapper.*`/`entity.*` 通配，无显式 import 残留）
- 编译残留：`mvn compile` EXIT=0（实施记录 §0）+ 单测 33/33 PASS
- "死依赖"结论可信：改前版本（`05d4404~1`）grep 显示 `comboIngredientMapper` 仅出现在字段声明/构造器参数/this 赋值 3 处，**调用点 = 0**——死依赖结论成立

## 靶点 5 — stash 隔离完整性 — **PASS**

- stash 在册：`stash@{0}` = `pre-card isolation: ... PG-001v2`（**1 条 stash 恰含 3 个隔离文件**：ComboIngredientMapper.java / Scan.vue / request.ts）；另有 8 条 `eol-phantom backup`（幻影清理兜底，内容与 HEAD 一致，无独有数据）
- 3 个文件对 HEAD `git diff --stat` = **0 行**（真实零差异；ComboIngredientMapper 的 WIP +5 修复安全存于 stash@{0}，未丢失）
- 声明外 WIP 未受影响：M 数维持 **463**（= 466 真实改动 − 3 隔离文件）；`05d4404` 文件集与 WIP 集无交集

## 裁决

**5/5 PASS，无 FAIL / PARTIAL / UNVERIFIED。**

- 交接 QA 独立验收；DS 边界提示：生产证据（L-02 同族）、REG-ORDER-012 建议、Batch 2 stash 修复去留裁决均不在本次抽检范围
- 实施记录与任务板回写内容与代码事实一致，未发现夸大或失实表述
