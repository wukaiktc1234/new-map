# F6 根因诊断报告 — 新建菜品无法立即 POS 下单（legacy 同步缺口）

- **日期**：2026-09-25（只读诊断，未修任何代码/DB）
- **现象**：`POST /v1/product-center/foods` 创建菜品成功（foods 行存在），紧接 `POST /v1/pos/orders/order` 报 500「菜品【QA-红烧土豆】库存不足」；重启后端后自动恢复

## 现象

新建菜品后立即 POS 下单失败，报"库存不足"；文案误导——真实原因是 legacy `food` 表**没有该菜品行**，扣减 UPDATE 影响 0 行，与"库存不够"共用同一错误出口。

## 数据流追踪

### 1. deductStock 完整调用链（POS 下单）

```
POST /v1/pos/orders/order
  → PosOrderCreateServiceImpl
    → L553 foodCodeForDeduct = dbFood.getFoodCode()（legacy food 表行）
    → L554 foodMapper.deductStock(foodCodeForDeduct, quantity)          【legacy food 表】
        SQL（FoodMapper.java:71-73，@Update 注解）:
        UPDATE food SET stock = stock - ? WHERE food_code = ? AND stock >= ? AND deleted = 0
        → 返回 0 = 「行不存在」或「库存不足」，上层不可区分
    → L556 返 0 → throw BusinessException(400, "菜品【…】库存不足")       ← F6 报错点
    → L558 foodNewMapper.deductStock(foodCodeForDeduct, quantity)        【新表 foods】（同样依赖 legacy 行先存在才能拿到 foodCode）
```

另一路径 L753/L758（快速单）同构。

### 2. 新建菜品路径（写哪些表）

`FoodServiceImpl.create`（FoodController POST /v1/product-center/foods）：
- `foodNewMapper.insert(food)` → **只写新表 foods**（L102）
- 配方 → dishRecipeNewMapper → dish_recipes
- **对 legacy food 的写入次数 = 0**（全文件 grep foodMapper = 0 处）
- 结论：新建菜品后，legacy food 表在下次启动同步前**必然没有该行** → POS 下单必断（每张新菜品 100% 复现）

### 3. DatabaseFixConfig 同步逻辑

| 项 | 事实 |
|----|------|
| 触发时机 | `CommandLineRunner fixDatabaseSchema`——**仅应用启动时**执行一次 |
| 范围 | 全量 foods → legacy food（syncFoodsToLegacyFood，L766） |
| 幂等性 | ✅ `ON CONFLICT (food_code) DO UPDATE SET …`（upsert） |
| 前置检查 | 两表都存在才执行 |

→ 启动时点之后创建的菜品存在**同步真空期**，直到下一次重启。

## 三选一方案

| 方案 | 内容 | 优点 | 缺点 |
|------|------|------|------|
| **A. 创建/更新菜品时双写 legacy** | FoodServiceImpl.create/update 末尾对 legacy food 做 upsert（复用 ON CONFLICT food_code 逻辑，含 stock 初始值） | 最小闭环；创建即可下单；改动集中 1 文件 | 双写是临时债（legacy 退役时移除）；update 路径也要覆盖否则改价/改库存又不同步 |
| B. POS 下单失败自愈 | deductStock 返 0 时，按 foods 行 upsert legacy 再重试一次扣减 | 兜底所有同步遗漏（不止 create 路径）；对调用方透明 | 把修复藏进失败路径；每次兜底都有一次报错噪音；掩盖其他"行不存在"类 bug |
| C. POS 库存全切新表 foods | 校验（L269-274 已读 FoodNew.stock）与扣减（L554）全部改用 foodNewMapper，移除 legacy food 依赖 | 治本；与 legacy 退役方向一致 | 牵动面大：legacy food 还有其他消费方（菜单 GET /pos/api/dishes、扣减外的 addStock 等），需一并盘点；跨卡范围 |

## 推荐

**A（主修复）+ 可选叠加 B（兜底）**：
- A 解决 100% 的"新建即断"场景，改动最小（FoodServiceImpl.create/update 各 +1 段 upsert，约 30 行 + 1 测试）
- B 为可选增强（+15 行），防其他路径遗漏；若追求最小改动可只做 A
- C 不建议本卡做：应归入 legacy 退役整体方案（与 P0-FLYWAY-COVERAGE-001 / P0-WORKSPACE-WIP-CONSOLIDATION-001 的双表窗口收敛协同）

## 影响评估

- 修复触及文件：A = 1（FoodServiceImpl.java；该文件 WIP 状态开卡时按 PG-001 v2 核查）+ 1 测试文件
- 历史数据：不触及——修复只影响**未来创建**的菜品；已存在菜品的 legacy 行由启动同步覆盖过，无缺口
- 风险：双写期间 legacy food 的 stock 初值取 foods.stock（新建默认 stock 字段），两边语义一致；update 路径需保证 stock 变更也双向（当前改库存走独立端点，需一并核查）

## 证据清单

| 证据 | 位置 |
|------|------|
| legacy deductStock SQL | FoodMapper.java:71-73（@Update，stock>=? 条件） |
| 报错出口 | PosOrderCreateServiceImpl.java:554-557 |
| 新表扣减（依赖 legacy 行存在） | :558-561, :758 |
| 新建菜品只写新表 | FoodServiceImpl.java:102（insert foodNew），全文件无 foodMapper |
| 同步仅启动时 | DatabaseFixConfig.java:5/19/73（CommandLineRunner → syncFoodsToLegacyFood L766） |
| 同步幂等 | DatabaseFixConfig.java:796 ON CONFLICT (food_code) DO UPDATE |
| 活体复现 | business-chain-verification-20260925.md 步骤 5（首轮 500 + 重启后 code=0） |
