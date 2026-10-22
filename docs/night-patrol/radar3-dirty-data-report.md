# 夜间自动巡检 · 雷达 3：历史脏数据安全清理（脚本 + 报告）

| 项 | 值 |
|----|----|
| 分支 | `ai-night-patrol-1`（只读巡检 + 产出脚本，**未执行任何变更语句**） |
| 采样时点 | 2026-10-23 |
| 数据源 | PostgreSQL 18 `food_traceability`（全部结论来自 psql SELECT 实测） |
| 关联卡 | DATA-006（0 元财务流水）、TRAY-005（托盘门店归属）、DATA-004（桌台为空）、OPS-BOM-003 |
| 产出 | `scripts/night-patrol/01~06-*.sql`（6 个数据脚本）+ `07-schema-drift-*.sql`（雷达 2 复核脚本） |
| 红线遵守 | 全部脚本默认只跑 SELECT；变更语句在被注释的 `[OWNER-EXECUTE]` 块内；无任何物理移除语句、无 DDL；对库只做过 SELECT |

---

## 0. 摘要

| # | 候选 | 实测影响行数 | 处置建议 | 脚本 |
|---|------|--------------|----------|------|
| 1 | DATA-006：`finance_records` 0 元「订单收入」流水 | **10 行**（10 条全 `deleted=0`；10 个 `order_code` 在 `orders` 全部命中且 `payment_status=2`） | ⚠️ **先别软删**，见 §1.3 | `01-finance-records-zero-amount_preview.sql` |
| 2 | `cost_records.remark` 字面 `- null` 拼接残留 | **25 行** | 低风险，可裁剪备注尾巴 | `02-cost-records-remark-literal-null_preview.sql` |
| 3 | `food_trace_code` 菜品字段为空 | **8 行 / 共 14 行** | 可确定性回填 `dish_name`（只回填名称） | `03-food-trace-code-empty-dish_preview.sql` |
| 4 | TRAY-005：`tray.store_id IS NULL` | **2 行**（id=8/9） | 属**业务决策**，不是脏数据清理 | `04-tray-null-store_preview.sql` |
| 5 | `orders.actual_amount` 已支付却恒为 0 | **25 行 / 25 行已支付订单（100%）** | 财务口径回填，需 Owner 定 canonical 列 | `05-orders-paid-zero-actual-amount_preview.sql` |
| 6 | 孤儿引用（厨师/托盘/厨单） | **0 / 0 / 0 行** | 无脏数据，脚本留作回归基线 | `06-orphan-integrity-checks_preview.sql` |

**一句话**：DATA-006 的表象（10 条 0 元流水）只是冰山一角，根因是 `orders.actual_amount` 全库 25 条已支付订单**全部为 0.00**。此时软删流水会让「0 元」痕迹消失而报表总额不变 —— 属**掩盖而非修复**。

---

## 1. 候选 1：DATA-006 的 10 条 0 元「订单收入」流水

### 1.1 现状核实（全部命中，未被处理过）

| record_id | record_no | amount | deleted | order_code | business_date |
|-----------|-----------|--------|---------|------------|---------------|
| 9 | SZ202609170001 | 0 | 0 | T20260915012 | 2026-09-17 |
| 10 | SZ202609210001 | 0 | 0 | T20260915011 | 2026-09-21 |
| 20 | SZ202609250002 | 0 | 0 | T20260925003 | 2026-09-25 |
| 23 | SZ202609260002 | 0 | 0 | T20260926006 | 2026-09-26 |
| 25 | SZ202609300002 | 0 | 0 | T20260930001 | 2026-09-30 |
| 28 | SZ202609300005 | 0 | 0 | T20260930003 | 2026-09-30 |
| 31 | SZ202609300008 | 0 | 0 | T20260930005 | 2026-09-30 |
| 34 | SZ202609300011 | 0 | 0 | T20260930007 | 2026-09-30 |
| 41 | SZ202610230005 | 0 | 0 | T20261023014 | 2026-10-23 |
| 74 | SZ202610230007 | 0 | 0 | T20261023015 | 2026-10-23 |

核实结论：
- `finance_records` 全表 32 行、收入类（`record_type='1'`）32 行，其中 `amount=0` 恰好这 10 行；`deleted=1` 的行数 = **0** → 尚未被处理。
- 10 个 `order_code` 在 `orders` 中**全部存在**（命中 10/10），且 `payment_status=2`（已支付）、`order_status` 1~2、`deleted=0` → 不是「订单不存在」的纯脏记录。

### 1.2 关键矛盾：订单侧金额也是 0

| order_code | total_amount | final_amount | actual_amount |
|------------|--------------|--------------|---------------|
| T20260915011 | 80 | 80 | **0.00** |
| T20260915012 | 400 | 400 | **0.00** |
| T20260925003 | 1800 | 1800 | **0.00** |
| T20260926006 | 2200 | 2200 | **0.00** |
| T20260930001/003/005/007 | 1800 | 1800 | **0.00** |
| T20261023014/015 | 1800 | 1800 | **0.00** |

而且这不是这 10 单的特例：
```text
SELECT count(*) AS paid_orders, count(*) FILTER (WHERE coalesce(actual_amount,0)=0) FROM orders WHERE deleted=0 AND payment_status=2;
→ 25 | 25        -- 已支付订单 100% 的 actual_amount = 0
```
同时对 25 行做金额口径核对：`final_amount = total_amount` 25/25 成立、`paid_amount > 0` 25/25 成立、`total_amount = 0` 为 **0 行**。
→ 结论：**不是「真 0 元订单」，是支付成功时没有把金额写回 `actual_amount`**。

### 1.3 因此：不建议直接软删

软删这 10 条流水后：
- 报表里「0 元收入」消失，看起来干净了；
- 但这 10 笔真实支付（80 / 400 / 1800 / 2200 / 1800×4 / 1800×2）**仍然没有进收入**；
- 之后无人再能从流水侧发现缺口 —— 问题从「看得见的错」变成「看不见的错」。

建议处置顺序（Owner 裁决）：
1. 先修 `orders.actual_amount` 写回（或明确 canonical 取值列），`05` 脚本已备好模板；
2. 再决定这 10 条流水是「回填 amount」还是「软删作废」，`01` 脚本两种模板都在，默认给软删版本；
3. 两步不要同时执行。

---

## 2. 候选 2：`cost_records.remark` 的字面 `null`（25 行）

现状：`remark` 形如 `采购入库成本 - null`，`cost_type='7'`（采购入库成本），`amount` 本身有效（350 ~ 1000000）。共 **25 行**，最早 2026-07-27（`cost_id=6`），最新 2026-09-30（`cost_id=30`）。

根因：字符串拼接时把一个 null 对象直接 `+` 进备注（`"采购入库成本 - " + xxx`），属展示层瑕疵，**不影响金额与凭证关联**。

处置：`UPDATE ... SET remark = btrim(replace(remark,' - null',''))`，只裁剪文本。幂等（`WHERE remark LIKE '% - null'`），二次执行 0 行。

⚠️ 回滚限制：备注文本裁剪后**无法从库内还原**，脚本已强制要求先把第 1/2 段输出导出为 CSV 作为回滚依据。

---

## 3. 候选 3：`food_trace_code` 菜品字段为空（8 行 / 共 14 行）

| id | trace_code | order_no | dish_id | dish_name |
|----|------------|----------|---------|-----------|
| 1-5 | FTC202609160001D28 … FTC202609160005323 | T20260915012 | 空 | 空 |
| 12 | FTC202610230001CB8 | T20261023008 | 空 | 空 |
| 15 | FTC2026102300037D8 | T20261023004 | 空 | 空 |
| 17 | FTC202610230005F72 | T20261023003 | 空 | 空 |

其余 6 行（id 13/16/20/53/54/55）正常：`dish_id='FD260925001'`、`dish_name='QA-红烧土豆'`、`store_id=1`。

可回填性已验证：这 8 行对应的订单在 `order_items` 中**恰好各有 1 条有效明细**（`item_rows=1`），可确定性取值 `product_name`。

⚠️ **只回填 `dish_name`，不回填 `dish_id`**：已正常的 6 行 `dish_id` 是业务编码形态 `FD260925001`，而 `order_items.food_id` 是整型主键（如 `2`）。直接回填会制造 8 条新的脏数据。这条冲突写进了脚本第 3 段作为显式警告。

---

## 4. 候选 4：TRAY-005 的 2 个无门店托盘（业务决策，非清理）

| id | tray_code | status | store_id | current_kitchen_order_id | yolo |
|----|-----------|--------|----------|--------------------------|------|
| 8 | KDS-COMBO-224311 | ready | NULL | 141 | 0 |
| 9 | KDS-COMBOSRV-224348 | ready | NULL | 143 | 0 |

两个托盘当前**都还挂在在途厨单上**（141 / 143），状态 `ready`。所以这不是「可以随手补个 store_id」的脏数据：
- 直接改 `store_id` 等于静默改写归属（TRAY-005 已明确不建议）；
- 正确路径是补「托盘分配门店」业务入口，或做「托盘调拨」动作（换店 + 审计）。

脚本因此**默认不含变更**，只提供带 `STORE_ID` / `STORE_NAME` 占位符的注释模板 + 回滚语句。可用门店实测只有 `1 测试门店A` / `2 测试门店B` 两条。

---

## 5. 候选 5：`orders.actual_amount` 恒为 0（25 行，DATA-006 根因面）

见 §1.2/§1.3。额外发现：这 25 条里包含 4 条**测试/夹具订单**，处置时要排除：

| order_id | order_code | final_amount |
|----------|------------|--------------|
| DEV-L206-TEST-001 | （空字符串） | 300000 |
| TEST-C01 | ORD-CANONICAL-001 | 10000 |
| TEST-C02 | ORD-CANONICAL-002 | 15000 |
| TEST-C03 | ORD-CANONICAL-003 | 8000 |

其中 `ORD-CANONICAL-002/003` 与那条 `order_code=''` 的订单**没有任何 `finance_records` 流水**（`05` 脚本第 2 段会显式列出），属「已支付但完全没进收入」的第二类缺口。

---

## 6. 被剔除的候选（实测零行，脚本保留为回归基线）

| 候选 | 实测 | 说明 |
|------|------|------|
| `kitchen_order.chef_id` 指向不存在员工 | **0 行** | 13 个厨单的 `chef_id` 全部能对上 `employees.employee_id` |
| `tray.current_order_id` 指向不存在订单 | **0 行** | — |
| `kitchen_order` 指向不存在/已删订单 | **0 行** | — |

另外登记一类**无法自动修**的数据缺口（不是脏数据，是缺失数据）：

```text
SELECT (SELECT count(*) FROM kitchen_order WHERE material_consumed=1 AND deleted=0) AS consumed_kitchen_orders,
       (SELECT count(*) FROM order_material_requirement) AS omr_rows;
→ 21 | 0     -- 21 张厨单标记已扣料，扣料明细表全空
```

`order_material_requirement` 0 行而 21 单 `material_consumed=1` → 扣料明细从未落库，**没有可信来源可以回填**，因此不写变更段，只登记。这同时是雷达 2 里「`order_material_requirement` 不是双表、是没数据」的直接证据。

---

## 7. 脚本清单与安全设计

| 脚本 | 目标 | 影响行数 | 回滚方式 | 幂等守卫 | 默认是否变更 |
|------|------|----------|----------|----------|--------------|
| `01-finance-records-zero-amount_preview.sql` | DATA-006 的 10 条 0 元流水 | 10 | 按 `record_no` 白名单置回 `deleted=0` | `deleted=0 AND amount=0` 双条件 | 否 |
| `02-cost-records-remark-literal-null_preview.sql` | `remark` 字面 `- null` | 25 | 依赖前置 CSV 归档（文本不可逆） | `WHERE remark LIKE '% - null'` | 否 |
| `03-food-trace-code-empty-dish_preview.sql` | 追溯码空 `dish_name` | 8 | `SET dish_name = NULL WHERE id IN (...)` | `dish_name IS NULL` + 唯一明细子查询 | 否 |
| `04-tray-null-store_preview.sql` | 无门店托盘（业务动作） | 2 | `SET store_id = NULL WHERE id IN (8,9)` | `store_id IS NULL` | 否 |
| `05-orders-paid-zero-actual-amount_preview.sql` | `actual_amount` 回填 | 25 | 置回 0（原值恒 0） | `coalesce(actual_amount,0)=0` | 否 |
| `06-orphan-integrity-checks_preview.sql` | 孤儿引用回归基线 | 0 | 无变更 | 纯只读 | 否 |

统一安全设计：
1. **默认只读**：直接 `psql -f` 只会输出预览结果，7 个脚本实测退出码全部 0（`ON_ERROR_STOP=1`）。
2. **变更语句被注释包围**：每个文件的变更段都在 `[OWNER-EXECUTE]` 注释块里，并标注「需 Owner 执行」。
3. **幂等**：每条 UPDATE 的 `WHERE` 都带当前值条件（`deleted=0` / `amount=0` / `IS NULL` / `LIKE`），重复执行影响 0 行。
4. **无物理移除、无 DDL**：全仓 7 个脚本静态扫描确认——剥离注释后**没有任何 UPDATE/INSERT/ALTER/CREATE 处于活跃状态**，全文也不含任何物理移除类关键字。
5. **回滚与变更一一对应**：优先按业务单号（`record_no` / `order_code` / `id`）白名单回滚，不依赖自增 id 顺序。
6. **可复核**：每个脚本末尾都有「变更后复核」SELECT，能立刻看出还剩多少行。

---

## 8. 局限与风险

1. 所有变更段**均未执行**，影响行数来自 2026-10-23 的 SELECT 实测；执行当天必须重跑预览段确认行数未变（行数变化 = 有人动过数据，应重新评估）。
2. 候选 5（`actual_amount`）属**财务口径回填**，canonical 取值列（`final_amount` / `total_amount` / `paid_amount`）当前三列存在差异，脚本不替 Owner 选择。
3. 候选 1 与候选 5 强耦合：先做候选 5 再决定候选 1，顺序反了会掩盖真实缺口。
4. `02` 的备注裁剪不可逆，回滚只能靠 CSV 归档 —— 这条限制写进了脚本头部。
5. `03` 的回填只覆盖「恰好 1 条明细」的 8 行；若后续出现多明细订单，脚本会自动跳过（子查询守卫），不会误填。
