# ADR-20261030：schema 漂移（迁移全绿 ≠ 生产一致）

> **一句话结论**：本项目的 schema 有**三个写入者** —— Flyway、7 个启动期 Java 类、手工 psql。
> 只审 Flyway 就等于只审三分之一；今晚追了 8 小时的「迁移全绿但 schema 不一致」正是这个原因。

## 1. 三个写入者模型（核心结论）

| 写入者 | 是否进 flyway_schema_history | 是否可审计 | 证据 |
|---|---|---|---|
| Flyway 迁移（274 条） | 是 | 是 | `flyway_schema_history` 274 行 |
| **启动期 Java DDL 类（12 个）** | **否** | **否** | `config/DatabaseFixConfig.java:19`（CommandLineRunner）等，见 §2 |
| 手工 psql | 否 | 否 | 生产 orders 54 列 vs Flyway 干净库 30 列，差 24 列无任何迁移记录 |

**推论**：任何「schema 是否一致」的判断，必须比对**实际库结构**（information_schema），不能只读迁移历史。
本结论落地为 `scripts/qa/schema_parity_check.py`（五类互斥计数）。

## 2. 12 个启动期 DDL 类全景表

| # | 文件 | 启动钩子 | 会执行的 DDL | 本次启动是否执行 | 代码依赖但无 Flyway 记录 | 处置 |
|---|---|---|---|---|---|---|
| 1 | `config/DatabaseFixConfig` | CommandLineRunner | **DROP TABLE orders / order_items** + 13 张 CREATE TABLE + ALTER | 否（已拦截） | 否 | ✅ 已止血 |
| 2 | `config/KitchenOrderMigrationConfig` | @EventListener(ApplicationReadyEvent) | `kitchen_order` 加 id、**DROP COLUMN total_amount / payment_method** | 否（已拦截） | 是（删列不可逆） | ✅ 已止血 |
| 3 | `config/TraceCodeSystemConfig` | @EventListener(ApplicationReadyEvent) | `material_trace_code` 加 5 列 + `kitchen_order` 加 2 列 | **是**（日志实证） | **是** | 已补迁移 016（3 列）；4 列原有迁移；**待关闭** |
| 4 | `config/SchemaFixMigration` | @PostConstruct | `orders.idempotency_key`、**CREATE TABLE pos_shifts**、`scheduled_task.task_code` | 待查 | 是（pos_shifts 是核心表） | 明天补迁移 |
| 5 | `config/TableInitConfig` | @PostConstruct | CREATE TABLE dining_table / member / combo_ingredient | 待查 | 是 | 明天补迁移 |
| 6 | `config/PositionRoleMappingTableConfig` | @PostConstruct | CREATE TABLE position_role_mapping / employee_data_scope / permission_assignment_log | 待查 | 是 | 明天补迁移 |
| 7 | `config/PasswordResetLogTableConfig` | ApplicationRunner | CREATE TABLE password_reset_log | 待查 | 是 | 明天补迁移 |
| 8 | `config/DataFixConfig` | @EventListener | `approval_record` 加 2 列 | 待查 | 待判 | 明天判 |
| 9 | `controller/hardware/HardwareInitController` | **无** | 1 处 DDL | 否（手动触发） | 否 | 不加注解 |
| 10 | `controller/maintenance/DataFixController` | **无** | 5 处 DDL | 否（手动触发） | 否 | 不加注解 |
| 11 | `controller/maintenance/HrMigrationController` | **无** | 10 处 DDL | 否（手动触发） | 否 | 不加注解 |
| 12 | `service/impl/DatabaseBackupServiceImpl` | @PostConstruct | DDL 是**备份导出字符串**（不执行） | 否 | 否 | 不用处理 |

**已止血两个的验收证据**：重启日志中 `===== 检查并修复数据库表结构 =====` 与 `=== 开始执行 kitchen_order 表重构迁移 ===` **出现 0 次**。

## 3. 113 列缺列的来源分类

```
B 类（仅生产有列）= 56 列（补完 011~015 后），补前 113 列。
来源分类：
  · orders 24 列：手工 psql（无迁移） → 已由 V20261030_011 补录
  · finance_records 17 列：手工 psql → 已由 015 补录
  · order_items 8 列：Flyway 里是 legacy 结构（V20260707_005 曾把 order_item_id RENAME 成 item_id）→ 012 补录
  · payment 6 列 / pos_shifts 2 列：手工 → 013 / 014 补录
  · material_trace_code 3 列：**TraceCodeSystemConfig 启动期加列**（available_quantity / bind_dish_id / bind_dish_name）→ 016 补录
  · 其余 56 列（22 张表）：待补（明天分批）
```

## 4. *_legacy 命名差异 + 僵尸列（P2）

```
4 张表在干净库有正式名、生产只有 _legacy：inventory_log / inventory_transactions / store_inventory / store_inventory_log
  列集合 4/4 完全一致（类型 0 差异）→ 判定为生产 RENAME 重构，无迁移记录
  代码 @TableName 指向正式名（InventoryLog.java:15 / InventoryTransaction.java:15 / StoreInventoryLog.java:12）
  → **相关端点在生产必然 500**（relation does not exist），端点已标 @UnfinishedFeature
  → 处置：选 γ 保持现状 + exclusions 登记 + 记 P1（产品决策 α/β/γ）

order_items 干净库 20 列 vs 生产 17 列，多出 order_item_id / food_name / subtotal_amount
  → 这 3 列由 DatabaseFixConfig 的 CREATE TABLE order_items 引入（含 DROP TABLE 重建路径）
  → 而 OrderItem 实体 @TableName 是 **order_items_legacy**（不是 order_items）→ 代码没错
  → 记 P2 僵尸列（不删，删列不可逆）
```

## 5. 待做

| 项 | 下一步动作 | 验收判据 |
|---|---|---|
| ft_verify 从零重放 | 建库 + Flyway 全量 274+6 条 | 全绿 + 关键 6 表列数达标（orders 54 / order_items 20 / pos_shifts 18 / finance_records 41 / payment 35 / material_trace_code 含 3 新列） |
| 其余 6 个启动期 DDL 类 | 逐个补迁移 + 隔离验证 + 加拦截器 | 重启日志 banner 全部消失 |
| 剩余 56 列（22 张表） | 分批补录迁移，每批跑 parity | B 类从 56 递减到 0 |
