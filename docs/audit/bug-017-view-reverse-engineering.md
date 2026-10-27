# bug-⑰：`v_inventory_summary` —— 疑似视图，禁止反推（2026-10-28）

## 1. 现状事实

```text
pg_class: relname='v_inventory_summary' → 0 行（既不是表，也不是视图）
实体   : com.foodtraceability.entity.InventorySummary  @TableName("v_inventory_summary")
字段   : productId, productName, unit, costPrice, warehouseStock, storeStock, totalStock, totalValue
后端引用: StoreInventoryController:109-119（GET /v1/store-inventory/summary）唯一调用点
          → InventorySummaryService.getSummaryPage()
前端   : 0 处引用（frontend/src 下搜 inventory-summary / inventorySummary 均无命中）
```

## 2. 判定：**不建**，立 bug-⑰ 等产品给口径

**理由（Lead 定，已采纳）**：视图的定义是**业务口径**（从哪些表、怎么聚合、成本价取哪一版、库存在库/在途怎么算），
不能从实体反推 —— 反推出来的视图「能跑但数字是错的」，**比缺表更危险**（缺表报错，错视图给人错数）。

## 3. 口径候选（**这是推测，不是结论**，仅供产品评审时参考）

从实体字段反推，最可能的定义是「产品维度 × 库位类型的库存汇总」：

```sql
-- 【推测，未采用】需要产品确认：成本价来源、是否含在途、是否排除 deleted、门店/仓库口径
CREATE VIEW v_inventory_summary AS
SELECT p.product_id                                   AS product_id,
       p.product_name                                 AS product_name,
       p.unit                                          AS unit,
       COALESCE(p.cost_price, 0)                       AS cost_price,
       COALESCE(SUM(CASE WHEN l.location_type='WAREHOUSE' THEN s.quantity END), 0) AS warehouse_stock,
       COALESCE(SUM(CASE WHEN l.location_type='STORE'     THEN s.quantity END), 0) AS store_stock,
       COALESCE(SUM(s.quantity), 0)                    AS total_stock,
       COALESCE(SUM(s.quantity) * COALESCE(p.cost_price,0), 0) AS total_value
  FROM products p
  LEFT JOIN stocks s ON s.product_id = p.product_id AND s.deleted = 0
  LEFT JOIN locations l ON l.location_id = s.location_id AND l.deleted = 0
 WHERE p.deleted = 0
 GROUP BY p.product_id, p.product_name, p.unit, p.cost_price;
```

**未采用的原因**：上面 `products`/`stocks` 两张表在本库都不存在（本项目用 `foods`/`inventory` 等），
连表名都要猜，说明口径信息完全缺失 —— 这正是「不能反推」的实证。

## 4. 当前处置

- 该表**不建**；`GET /v1/store-inventory/summary` 保持 `@UnfinishedFeature(table="v_inventory_summary")` → 501「功能尚未上线」；
- 实体 `InventorySummary` 暂不删（等产品定「做 or 下线」）；
- **不要去 pg_class 里建一张同名表** —— 那会让人以为功能已上线。

