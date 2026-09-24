# P1-COMBO-LEGACY-CLEANUP-001 — 实施记录 001

## 0. 状态

| 项 | 值 |
|----|------|
| Task ID | `P1-COMBO-LEGACY-CLEANUP-001` |
| Stage | **IMPLEMENTED_READY_FOR_DS（2026-09-25）** |
| 前置 | P1-COMBO-ORDER-001 CLOSED_WITH_REGISTERED_LIMITATION；PG-001 v2 门禁 PASS（2026-09-25） |
| 声明改动文件 | OrderNewServiceImpl / PosApiServiceImpl / OrderMaterialRequirementServiceImpl / KitchenScanServiceImpl / ComboIngredientMapper / DatabaseFixConfig / 前端 KitScan（若有） |
| 实际改动文件 | **4 个**：`PosApiServiceImpl` / `OrderMaterialRequirementServiceImpl` / `KitchenScanServiceImpl` / `ComboIngredientMapper`（见 §2） |
| commit | `05d4404`（精确 add 4 文件，+55/-40；PG-001 v2 第 3/4 条执行：无 `git add .`，staged 审查通过） |
| 自测 | `mvn compile` EXIT=0；`OrderNewServiceImplDeductTest` + `PosOrderCreateServiceFoodIdMapTest` **33/33 PASS（0 Fail 0 Error）** |

## 1. 范围执行情况（5 项）

| 范围项 | 结果 |
|--------|------|
| 废弃 combo_ingredient 旧表 | **部分**：`ComboIngredientMapper` 标记 `@Deprecated`（javadoc 指向 `ComboIngredientNewMapper`，旧表保留供 legacy 前端过渡期由 DatabaseFixConfig 同步写入）；物理删表不在本卡 |
| getFullMenu 切 legacy dish_combo → dish_combos | **完成**：`getAllCombos` 改读 `dish_combos` + `combo_ingredients` + `foods`（FoodNew），价格分→元（`BigDecimal.valueOf(v,2)`），status 语义 1=active，people_count 按同步口径固定 2（dish_combos 无该列），comboType 不再回填（新表无列，旧同步亦不写=历史值恒 null） |
| 清理旧 POS 兼容代码 | **完成**：`KitchenScanServiceImpl` 移除死依赖（`comboIngredientMapper` 字段/构造器参数/赋值——该字段从未被调用） |
| KL-080 三文件归并 | **完成**：trio 中 `PosApiServiceImpl` / `OrderMaterialRequirementServiceImpl` 均切新表；`KitchenScanServiceImpl` 为死依赖移除（KL-080 的"仍读旧表"状态消除）；`OrderNewServiceImpl` 经 combo 卡已是新表（本卡零改动，grep 验证无旧表引用残留） |
| DatabaseFixConfig 列名 bug | **免做**：combo 卡 `aec5c45` 已修（created_at/updated_at → create_time/update_time，`git show aec5c45` 证据）；本卡零改动 |

## 2. 改动明细

| 文件 | 改动 |
|------|------|
| `PosApiServiceImpl.java` | getAllCombos/getFullMenu 数据源切 `DishComboNewMapper`/`ComboIngredientNewMapper`；食材反查改 `FoodNewMapper`（foodId Long = foods.food_id） |
| `OrderMaterialRequirementServiceImpl.java` | `generateRequirementsForCombo` 切 `ComboIngredientNewMapper`（comboId String → Long）；foodCode 经 `FoodNewMapper.selectById` 解析后走既有 `generateRequirementsForDish`（配方链不变） |
| `KitchenScanServiceImpl.java` | 移除未使用的 `ComboIngredientMapper` 依赖（字段/构造器/赋值） |
| `ComboIngredientMapper.java` | `@Deprecated` + javadoc 迁移指引 |

## 3. PG-001 v2 门禁记录

- 开卡前幻影清零（M 数 = 真实改动数，730 幻影已清理，见 `docs/quality/workspace-wip-diagnosis-001.md`）
- 声明文件中 3 个与其他批次 WIP 真实重叠的文件（`ComboIngredientMapper` +5/-0、`frontend-kitchen Scan.vue` +2/-1、`api/request.ts` +6/-0）已 **stash 隔离**（stash 标注 `pre-card isolation: ... PG-001v2`，可恢复未丢弃），开卡时全部声明文件对 HEAD 零差异
- commit 精确 add、staged 审查（staged stat = 4 声明文件，unstaged 466→463 吻合）

## 4. 风险与限制

- **L-01（数据依赖）**：`getAllCombos` 现读 `dish_combos`，其数据由产品中心维护；若新表无数据而旧表有（同步方向 new→old），POS 菜单套餐列表将为空。缓解：DatabaseFixConfig 双向同步中 new→old 仍活跃，产品中心为唯一数据源口径
- **L-02（无生产调用方的死 API）**：`generateRequirementsForCombo` 当前无生产调用方（grep 证据），本次切换属 KL-080 归并完整性，无运行时回归面
- **L-03（JDK 25 测试工具链）**：Mockito inline mock 需 `-XX:+EnableDynamicAgentLoading -Dnet.bytebuddy.experimental=true`（否则 5 个 mock 类 Error，ENV-3 同族环境问题）；与业务代码无关
- 旧表 `combo_ingredient` / `dish_combo` 物理删除与 DatabaseFixConfig 过渡期同步的下线 → **后续卡**（须先确认 legacy 前端/小程序无消费）

## 5. 下一步

DS 抽检 → QA 独立验收 → regression（建议 REG-ORDER-012：POS 菜单套餐列表数据源切换一致性）→ 收口回写。
