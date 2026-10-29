# 关键表主键 · 外键 · 生成方式 速查（2026-10-29 补）

> 为什么补这份：2026-10-28 一天内**同类错误踩了 5 次**（`order_items_new`、`order_number` vs `order_id`、
> `dining_table` 是视图、`food_trace` 空表、`KitchenOrder.@TableId` 是 `id` 不是 `kitchenOrderId`），
> 每次都浪费一轮排查。**写任何查询/更新前，先看这张表。**

## 一、最容易错的 6 张表

| 表 | 主键（实体 @TableId） | **业务号 ≠ 主键** | 生成方式 | 关键外键列 |
|----|----------------------|-------------------|----------|------------|
| `orders` | `order_id`（varchar） | `order_number` 是业务号，**不能当外键用** | 应用生成（O1793…） | — |
| `order_items` | `item_id`（varchar） | — | 应用生成 | `order_id` → **orders.order_id**（不是 order_number） |
| `kitchen_order` | **`id`**（bigint, `@TableId(value="id", AUTO)`） | `kitchen_order_id` 是业务号（KO1793…），**不是主键** | IDENTITY | `order_id` → **orders.order_id** |
| `pos_shifts` | `shift_id`（bigint） | — | **`GENERATED ALWAYS AS IDENTITY`**（**不能显式插入**） | `terminal_id`、`employee_id`（存员工标识，非用户名） |
| `deposit_slips` | `slip_id`（bigint） | `slip_no` 是业务号 | IDENTITY | `shift_id` → pos_shifts.shift_id（一班一单） |
| `refund_shift_attribution` | `id`（bigint） | — | IDENTITY | `refund_record_id`（幂等键）、`shift_id`（真相）、`slip_id`（派生） |

## 一.5 更正：`material_locked` 是**列**，不是表（2026-10-29，第 6 次「名字≠实际」）

```text
material_locked 不是独立表，它是 kitchen_order 表的 integer 列（0=未锁 / 1=已锁）；
同表还有 material_consumed、material_consume_time。
之前只查 information_schema.tables 搜不到它 -> 误判「该功能未实现」，是错误结论。
教训：查「是否存在」时要同时查 tables 和 columns。
```

## 二、`dining_table` 是**视图**，不是表

```text
dining_table  → VIEW（由 dining_tables 派生，见 V20261028_021）
dining_tables → BASE TABLE（真实存储）
→ 对 dining_table **不能 ALTER / INSERT**；实体 DiningTable 通过视图映射。
```

## 三、`users.employee_code` 可能为空

```sql
SELECT username, employee_code FROM users;   -- 实测 vcashier01 的 employee_code 为空字符串
```
→ 需要「当前登录人的员工标识」时，**要按候选集试**：`employee_code` → `user_id` → `username`，
不能只取其一（`pos_shifts.employee_id` 在不同入口写入的值不同源）。

## 四、金额单位速查（详见 money-unit-boundary.md）

| 表 | 元（numeric） | 分（bigint） |
|----|--------------|--------------|
| `orders` | `order_amount`、`discount_amount`、`actual_amount`、`refund_amount` | `total_amount`、`final_amount`、`paid_amount` |
| `pos_shifts` | `opening_cash`、`closing_cash`、`total_amount` | — |
| `deposit_slips` | — | 全部 `*_fen` + `expected/actual/difference` |

## 五、今后写查询前的三步

```text
1. \d 表名         — 看主键、看 identity、看有没有 terminal_id 这类维度列
2. 确认「业务号」与「主键」哪个是外键（orders 用 order_id，不是 order_number）
3. 确认金额单位（元 or 分），无 _fen 后缀一律当元
```

## 一.5 更正：material_locked 是列，不是表（2026-10-29，第 6 次「名字≠实际」）

```text
material_locked 不是独立表，它是 kitchen_order 表的 integer 列（0=未锁 / 1=已锁）；
同表还有 material_consumed、material_consume_time。
之前只查 information_schema.tables 搜不到它 -> 误判「该功能未实现」，该结论已撤回。
教训：查「某东西是否存在」时要同时查 tables 和 columns。
```

## 一.6 第 7 次「名字≠实际」：role_permissions 是 create_time，不是 created_at

```text
实测：INSERT INTO role_permissions (role_id, permission_id, created_at, ...) → 报列不存在，
      而我当时只看了 'INSERT 0 1' 之类的输出/或没看 stderr → 恢复语句静默失败，行数停在 33（应为 34）。
role_permissions 真实列：id, role_id, permission_id, deleted, create_time（无 updated_at/created_at）。
```

**铁律（2026-10-29 起）**：任何**恢复/回滚**语句执行后，**必须 SELECT 复核目标状态**（行数 + 关键行是否存在），
不能只看 `INSERT 0 1` / `UPDATE n` 就认为成了 —— 成功输出 ≠ 目标状态正确。
