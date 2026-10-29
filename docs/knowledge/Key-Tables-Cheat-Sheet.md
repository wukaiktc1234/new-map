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

### 实例清单（截至 2026-10-29 共 10 条「假设某个东西存在」）

| # | 假设了什么 | 实际 |
|---|-----------|------|
| 1 | `order_items_new` 表存在 | **不存在**（明细在 `order_items`） |
| 2 | `order_items.order_id` = `orders.order_number` | 存的是 `orders.order_id`（O1793…） |
| 3 | `dining_table` 是表 | **是视图**（基表 `dining_tables`） |
| 4 | `food_trace` 有数据 | 死实体/空表 |
| 5 | `KitchenOrder` 主键是 `kitchenOrderId` | `@TableId` 是 **`id`** |
| 6 | `material_locked` 是独立表 | 是 `kitchen_order` 的 **integer 列** |
| 7 | `role_permissions` 有 `created_at` 列 | 实际是 **`create_time`** |
| 8 | `AuthenticationServiceImpl` 有 `logger` 字段 | **没有**（类里根本没声明） |
| 9 | `new JwtToken(String)` 单参构造器 | 实际需要 **`(token, refreshToken, expiresIn)`** 三参 |
| 10 | `pos_shifts.shift_id` 可显式插入 | 是 `GENERATED ALWAYS AS IDENTITY`，**不能显式插** |

**一句话**：动代码/写 SQL 前，先确认它存在（`\d 表名` / 看类字段与方法签名 / 看注解），不要假设。

### `orders` 真实列清单（54 列，2026-10-29 实测；写 UPDATE 前必看）

```text
1 order_id varchar(32) NOT NULL     2 user_id varchar(50)        3 order_number varchar(32) NOT NULL
4 order_type int                    5 order_status int          6 order_amount numeric
7 discount_amount numeric           8 actual_amount numeric     9 payment_method int
10 transaction_id varchar(100)     11 payment_time timestamp   12 delivery_address text
13 contact_name varchar(50)        14 contact_phone varchar(20) 15 remarks text
16 idempotency_key varchar(100)    17 order_source int         18 merchant_id varchar(50)
19 deliveryman_id varchar(50)      20 estimated_delivery_time  21 actual_delivery_time
22 cancel_reason **text**          ← 存在（text，无长度限制）
23 refund_reason text              24 refund_amount numeric    25 refund_time timestamp
26 create_time timestamp           27 update_time timestamp    28 create_by varchar(50)
29 update_by varchar(50)           30 deleted int              31 store_id bigint
32 order_code varchar(32)          33 customer_id bigint       34 customer_name varchar(50)
35 customer_phone varchar(20)      36 table_id bigint          37 table_name varchar(50)
38 dining_people_count int         39 payment_status int       40 total_amount bigint(分)
41 coupon_amount bigint            42 points_amount bigint     43 delivery_fee bigint
44 packaging_fee bigint            45 final_amount bigint(分)  46 paid_amount bigint(分)
47 points_earned int               48 remark text              49 expected_time timestamp
50 cashier_user_id bigint          51 create_user_id bigint    52 customer_phone_encrypted varchar(255)
53 call_number varchar(8)          54 shift_id bigint
```

**⚠ 不存在 `cancel_time` 列** —— 第 11 次「假设存在」就是在这里翻车（`UPDATE ... cancel_time = now()` → 92 笔整批失败）。
关单时间请用 `update_time` 承载。

## 三、`users.employee_code` 允许为空 —— 鉴权/交班统一用 `userId`（2026-10-29 实测）

**实测：13 个用户 `employee_code` 为 NULL**

```text
1                  | admin
999                | test-admin
2080170081205395460| finqa_register
2080170081205395461| finqa_confirm
2080170081205395462| finqa_noperm
2080170081205395463| **vcashier01**   ← 收银员
2080170081205395464| qa_store_b
2080170081205395465| qa_auditor
2110437068670001154| **vtest01**      ← 测试员A
2110437068670001200| qa_admin
… 共 13 个
```

**为什么不能回填假工号**：补 `employee_code = username` 等于**造身份数据**，且会与历史 `pos_shifts.employee_id`
（旧工号风格 `2110437068670001210`）继续错位 —— 一个用户两种标识，对账/审计都乱。

**正确做法（现行代码已在用）**：
```text
鉴权/交班一律**候选集**或 userId：
  employee_code → userId → username（依次尝试，命中即用）
POS 登录 findUserByEmployeeId（PosAuthServiceImpl:655-673）：
  先查 employee_code，**查不到再查 username** ← 这就是 employee_code 为 NULL 仍能登录的原因
  实测：employeeId=vcashier01 登录成功（userId=2080170081205395463，permissions=44）
  实测：employeeId=2110437068670001210（旧工号风格）→ code=500 员工号或密码错误
```

**注意**：`PosAuthServiceImpl:658` 是**唯一**只按 employee_code 精确匹配的 POS 鉴权点，但有 username 兜底；
其它 14 处精确匹配都在 HR/档案/合同域（`employee_code` 是该域业务主键且必填），**不应统一改**。

## 四、框架层错误（401/403/404）不得包装成 HTTP 200（2026-10-29 教训）

```text
修前：POST /v1/pos/auth/logout（后端无此映射）→ **HTTP 200** + body.code=404「资源不存在」
后果：前端只看 HTTP 状态 → 当成成功 → 进入业务分支 → 表现为「无班次」状态崩塌
修后：GlobalExceptionHandler 的 NoHandlerFound / NoResourceFound 返回 **真 HTTP 404**
      （ResponseEntity.status(NOT_FOUND)，body 保留 code=404 兼容旧前端）
```

**通用结论**：「401/403/404 这类**框架层错误**，如果被包装成 HTTP 200 + body.code，前端必然误判；
而如果前端只看 HTTP 状态，**它会一路走到业务分支里**，表现成业务错误（如『无班次』）。」

对照：交班闸 `PENDING_EXCEPTIONS` 已按契约返回**真 HTTP 409** —— 同一个项目里两种风格并存，
**判断依据必须是 HTTP 码 + body.code 双看**，前端不能只认一个。
