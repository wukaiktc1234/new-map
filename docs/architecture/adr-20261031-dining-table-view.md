# ADR-20261031：`dining_table` 是视图而非表（含两条方法论规则）

> **一句话结论**：`public.dining_table` 是 **VIEW**，底层 `public.dining_tables` 是启动期 Java DDL 创建的 BASE TABLE；
> 视图含表达式列 → **不可自动更新** → 所有写入必失败（SQLSTATE 0A000）。桌台功能在生产**从未成功落库过**。

## 1. 事实层（全部实测）

```
information_schema.tables：
  dining_table   → table_type = VIEW
  dining_tables  → table_type = BASE TABLE（16 列，0 行，MAX(table_id)=0）

视图定义（pg_get_viewdef 原文）：
SELECT table_id AS id, table_code AS table_number, table_name,
       seats_count AS capacity, area_id::text AS area,          -- 表达式列
       CASE status WHEN 1 THEN 'available' WHEN 2 THEN 'occupied'
            ELSE status::text END AS status,                     -- 表达式列
       qr_code, current_order_id::text AS current_order_id,
       min_people AS guest_count, NULL::timestamp AS seated_at,  -- 常量列
       create_time AS created_at, update_time AS updated_at
  FROM dining_tables d WHERE deleted = 0;

实测写入对照（事务内回滚，未改数据）：
  INSERT INTO dining_table (...)  → 失败：PSQLException: 无法插入列"area"到视图"dining_table"（0A000）
  INSERT INTO dining_tables(...)  → INSERT 0 1 成功（已 ROLLBACK）
```

## 2. 两次判据错误（各记一次）

| 谁 | 错在哪 | 缺了哪个证据维度 |
|---|---|---|
| Lead | 查了行数（两表 0 行）+ 列数（12 vs 16），得出「权威表是单数表、复数表是孤儿」 | **没查 `table_type`**（VIEW / BASE TABLE） |
| fund-500 | 查了实体字段 vs 视图 columns（12=12），先断言「minPeople/maxPeople 缺列」，再据此支持「缺列」假设 | **只查 columns**，未查视图依赖/可更新性 |

**提炼规则**：
> **任何「表结构」结论必须同时查四个维度：`table_type` + `columns` + 约束（PK/FK/NOT NULL）+ 依赖（视图/触发器/继承）**。
> 缺任一维度的结论只能作为假设，不能作为迁移依据。

## 3. 方案对照与裁定

| 方案 | 做法 | 代价 | 裁定 |
|---|---|---|---|
| A | 给视图建 `INSTEAD OF` 触发器（area→area_id、status→int 反解） | 新增迁移 + 触发器隐式逻辑（与「隐式行为」同族，难审计） | ❌ |
| **B** | 实体指向权威表 `dining_tables` + 字段映射（tableId/tableCode/seatsCount/areaId/status int…） | 跨层（实体/DTO/VO/前端 status 口径） | ✅ **目标方案** |
| C | 重建视图为自动可更新（去掉表达式列） | 破坏前端字段语义（status 变数字） | ❌ |

**三步路径**：
```
Step 1（迁移已写草稿，未执行）：Flyway 建 dining_tables + 建 dining_table 视图
        文件：docs/migration-drafts/V20261031_001__dining_tables_with_view.sql
        ⚠ 放在 drafts 而非 db/migration —— 否则下次重启会被 Flyway 自动执行
Step 2：实体 DiningTable 改指 dining_tables，字段映射 + DTO/VO 调整
Step 3：前端 status 语义（'available' 文本 → 1/2 整数或提供兼容层）
```

## 4. 「从未落地」的证据

```
dining_tables 行数 = 0 ｜ MAX(table_id) = 0   （序列从未推进 → 从未插入成功过）
orders.table_id 非空 = 0 行                   （订单从未引用真实桌台主数据）
orders.table_name 非空 = 18 行 / 4 个不同值（桌5、桌9 …）  （只有手填字符串）
→ 结论：「桌台」在生产只是订单上的自由文本；桌台主数据从未创建
→ 这也解释了「视图不可写」为何长期无人发现：写接口从未被真正使用过
```

## 5. 流程规则（今晚两个实例）

> **当发现方案所依赖的前提被证伪时，正确动作是停止并上报，不是换个写法继续。**

```
实例 1（今晚）：桌台 020 迁移方案的前提「dining_table 缺列」被 table_type=VIEW 证伪
  → 停手上报，未写迁移（宁可交白卷，不写基于假设的迁移）
实例 2（今晚）：五处数据边界的前提「有 @PreAuthorize 就等于有边界」被 A/B 实测证伪
  → 停手上报，改为按数据边界修（权限门禁 ≠ 数据边界）
```

## 6. 待办

| 项 | 下一步 | 验收判据 |
|---|---|---|
| 草稿迁移验证 | 明天在 ft_gate 用 **Flyway** 双跑（先贴 ft_gate 现状：dining_tables 是否存在、dining_table 是否 VIEW） | 两次均通过 + 断言打印；幂等无变更 |
| 方案 B Step 2/3 | 实体改指 + 前端 status 口径 | POST /v1/pos/tables 返回 200 且 DB 有行 |
| dining_tables 0 行 | 产品确认是否需要真实桌台主数据 | 有主数据且 orders.table_id 能关联 |