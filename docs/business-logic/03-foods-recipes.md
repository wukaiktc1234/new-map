# 商品与配方业务逻辑（建档 → 配方 → 同步）

> 验证状态：✅ 2026-09-25 活体（`docs/quality/business-chain-verification-20260925.md`）；2026-09-25 F6 修复后复验（新建即下单 T20260926001）
> 文档性质：AI 理解稿，**待 Owner 审定**

## 应该做什么

产品中心维护三类主数据：**原料模板**（material_template）、**菜品/商品**（foods，含配方行 dish_recipes）、**套餐**（dish_combos + combo_ingredients）。POS/采购/KDS 均消费这些主数据。

## 数据流

```
原料  POST /v1/material-templates（⚠ 必须自带 templateCode，接口不自动生成）
      → material_template
菜品  POST /v1/product-center/foods（FoodCreateDTO：foodName/categoryId/salePrice(分)/stock + recipes[]）
      → foods（新表，food_id Long 自增 + food_code FD 码自动生成）
      → 同时写 dish_recipes（recipes[].materialId 引用 material_archives.material_id）
      → 双写 legacy food（P1-NEW-FOOD-LEGACY-SYNC-001，2026-09-25：create/update/updateStatus 后按
        DatabaseFixConfig 同口径回填 legacy food 行，消除启动同步真空期）
      → 另有下单自愈兜底：POS 扣减返 0 时按 foods 补 legacy 行再重试一次（PosOrderCreateServiceImpl）
配方  无独立管理端点（F2）：只能随菜品创建携带；编辑/删除路径待产品确认
套餐  P1-COMBO-LEGACY-CLEANUP-001 起 POS 读 dish_combos/combo_ingredients；
      产品中心写新表，DatabaseFixConfig 启动时向 legacy dish_combo/combo_ingredient 反向同步
```

## 关键字段与约束

| 字段 | 约束 | 说明 |
|------|------|------|
| FoodCreateDTO.categoryId | NotNull，Long | 引用分类表（实体 food_categories；⚠ 实体列映射曾错位已修 P1-POS-MENU-500-001） |
| salePrice/costPrice | Long，**分** | 菜品利润率由后端按配方成本计算 |
| recipes[].materialId | Long | = material_archives.material_id；**必须与 store_inventory.material_id 同一空间**，否则扣料找不到库存行 |
| recipes[].quantity | Double，单位斤 | 扣料语义：1 份菜品消耗该数量（0.4 斤 = 200g） |
| MaterialTemplate.templateCode | **必填**（DB NOT NULL） | 缺失 → 500"系统繁忙"（实为约束违例，F1） |

## 不变量

1. food_code 全局唯一（FD 码），POS 下单、legacy 同步、扣料全部以它关联
2. 配方的 material_id 必须能在 store_inventory 中按 (store_id, material_id) 找到行——否则出餐扣料静默失败（与 OBS-S1 同族风险）
3. foods 与 legacy food 的 stock 双表扣减必须一致（见 02-pos-order.md 不变量 3）

## 已知问题

- **F6（高）→ 已修复（2026-09-25）**：P1-NEW-FOOD-LEGACY-SYNC-001（A 双写 + B 下单自愈）；活体验证：新建菜品立即下单 code=0（T20260926001），legacy 行同步存在（诊断 `docs/quality/f6-new-food-sync-diagnosis-001.md`）。残留：delete/批量删除无 legacy 对应（残留行）、OrderTimeoutTask 只回 legacy、Pricing 只改新表价格——均为低危漂移源，登记待后续
- **F2（中）**：配方无独立 CRUD 端点
- **F1（低）**：templateCode 不自动生成 + 错误响应为"系统繁忙"500 而非 400 字段提示

## 验证方式

1. POST foods 带 recipes[] → psql 断言 foods + dish_recipes 两表行一致（required_quantity 精度 3 位小数）
2. 启动后检查 legacy food 行存在（F6 修复后应为创建即同步）
3. material_template 缺 templateCode 应返回 400 字段级提示（F1 修复后）
