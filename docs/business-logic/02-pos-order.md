# POS 链业务逻辑（菜单 → 下单 → 支付 → KDS → 扣料）

> 验证状态：✅ 2026-09-25 活体全链（证据 `docs/quality/business-chain-verification-20260925.md`）
> 文档性质：AI 理解稿，**待 Owner 审定**

## 应该做什么

顾客点单：POS 拉菜单（菜品+套餐）→ 下单（单品/套餐）→ 收银支付 → KDS 后厨看单 → 出餐扫码确认 → 按配方扣减原料库存并落流水。

## 数据流

```
菜单  GET /v1/pos/api/combos            ← dish_combos（新表）+ combo_ingredients + foods
      GET /v1/pos/api/dishes            ← food（legacy，启动时由 foods 同步）
      GET /v1/pos/api/menu              ← 聚合以上（categories 来自 food_category）
下单  POST /v1/pos/orders/order
      items[].id：单品=food_code（如 FD260925001）；套餐=combo_id（dish_combos.combo_id）
      items[].dishType：single | combo
      → orders(order_number=T 码) + order_items
        单品：product_type=1, food_id=foods.food_id(Long), combo_id=NULL
        套餐：product_type=2, combo_id= Long, food_id=NULL（合法 null）
      → kitchen_order 自动创建（KO 码）
支付  POST /v1/pos/orders/order/pay（orderId——⚠ 不是 orderNumber）
      → order_payment_records（payment_method 枚举数字）+ finance_records
KDS   GET /v1/kitchen/orders/recent/full ← kitchen_order.dish_items 内嵌 JSON（套餐含 components[]）
出餐  /v1/tray：create → bind-order(数值 kitchen_order.id！) → scan-kitchen-in
      → scan-kitchen-out（防抖≥5s）→ scan-serve（防抖≥2s）
      → kitchen_order.status=served + material_consumed=1
扣料  按配方（dish_recipes/ 新表）×数量 → store_inventory 扣减 + store_inventory_log（before/after/remark 带 T 码）
      同时 foods.food 新表+legacy 表 stock -1/份（菜品份数）
```

## 关键字段与约束

| 字段 | 约束 | 说明 |
|------|------|------|
| items[].id | 单品=**food_code 字符串**；套餐=dish_combos.combo_id | 双语义，按 dishType 区分（F4 卡 FOODID 修复后的契约） |
| pay 的 orderId | **orders.order_id（O 码）**，非 orderNumber | 传 orderNumber 会 400"订单ID不能为空" |
| tray bind-order.kitchenOrderId | **数值主键**（kitchen_order.id），非 KO 码 | 传 KO 码会 JSON 反序列化 500 |
| 支付方式枚举 | 数字（2=现金等） | 见 PayRequestDTO/payment 服务 |
| 菜品价格 | 请求价 ¥ 元（Decimal），落库 unit_price 分 | 后端做元→分，价格差异只 warn |

## 不变量

1. **扣料主体 = store_inventory（按 store_id+material_id）**，扣减量 = 配方 required_quantity(斤) × 份数；每笔必有 store_inventory_log（before/after/remark 含订单号）
2. 套餐行 food_id=NULL 是合法语义；单品行 food_id 不得为 NULL
3. 库存双表：原料库存扣 **store_inventory**；菜品份数扣 **foods + food**（两表都要扣，legacy 表漏行会导致 deductStock=0 报错）
4. 菜单价格：foods.salePrice / dish_combos.combo_price 一律 Long 分，出口转元（scale 2）

## 已知问题

- **F6（高，上线阻断）**：新建菜品只写 foods 新表，legacy food 表**仅在启动时同步** → 新菜品立即 POS 下单报误导性"库存不足"（实为 legacy 行不存在 deductStock=0）；需重启恢复。修复方向：创建菜品时同步双表，或运行时 upsert
- /pos/api/menu 聚合 500（food_category 实体/schema 主键错位）——**已修复**（P1-POS-MENU-500-001，commit 872c874）
- 套餐菜单数据源已切 dish_combos（P1-COMBO-LEGACY-CLEANUP-001）；legacy dish_combo/combo_ingredient 由 DatabaseFixConfig 过渡期同步，双表并存窗口待最终废弃

## 验证方式

1. 建测试菜品+配方（见 03-foods-recipes.md）→ 确认 legacy food 同步存在（或修复后创建即同步）
2. 下单：psql 断言 order_items 三字段语义正确
3. 支付：psql 断言 order_payment_records + finance_records（注意存在 amount=0 伴随行，F7）
4. tray 全链 serve：psql 断言 kitchen_order.material_consumed=1 + store_inventory_log 增量 = 配方量 × 份数
