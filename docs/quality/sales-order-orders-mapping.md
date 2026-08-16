# SalesOrder ↔ orders 字段映射表（PD-015 阶段一交付物）

> 编号：OICBE-B1-002（阶段一产出）
> 依据：PD-015 架构裁决（2026-08-16）：**两阶段，禁止一次性大改**——阶段一 = orders 唯一生产真相源确认 + 字段映射表 + 禁止新增 sales_order 引用（立即，无表变更）；阶段二 = 按批迁移代码（实体/查询/接口/报表/统计），每批 QA + 基线（**本阶段不执行阶段二**）。
> 性质：**只读核对 + 文档产出，零代码改动、零 migration 落库**（flyway 最新版本仍为 20260816.001）。
> 核对方式：information_schema 只读实测（2026-08-16，localhost:5432/food_traceability）+ 全仓引用扫描。
> 维护者：developer（会话4）产出；qa 独立验收；阶段二契约确认输入。

---

## 一、orders 唯一生产真相源确认书

### 1.1 表存在性（information_schema 只读实测）

```sql
SELECT table_name FROM information_schema.tables
WHERE table_schema='public' AND table_name IN ('orders','order_items','sales_order','sales_order_detail');
-- 结果：order_items / orders（sales_order、sales_order_detail 不存在）
```

| 表 | 实测 | 列数 | 行数（2026-08-16） | 对应实体 |
|---|---|---|---|---|
| `orders` | ✅ 存在 | **51 列**（新旧双结构并存） | 1 | `OrderNew.java` @TableName("orders") |
| `order_items` | ✅ 存在 | 17 列 | 0 | `OrderItemNew.java` @TableName("order_items") |
| `sales_order` | ❌ **不存在** | — | — | `SalesOrder.java` @TableName("sales_order")（漂移） |
| `sales_order_detail` | ❌ **不存在** | — | — | `SalesOrderDetail.java` @TableName("sales_order_detail")（漂移） |

**结论 1：sales_order / sales_order_detail 生产表不存在，实体-表漂移事实成立**（与 KL-054 / ETM-002 / ETM-003 一致）。

### 1.2 orders 51 列 = 新旧双结构并存（完整列清单实测）

orders 一表内同时存在旧列集（`order_number/order_amount/actual_amount/contact_name/contact_phone/remarks/user_id/merchant_id/payment_method/transaction_id/...`）与新列集（`order_code/order_status/total_amount/final_amount/paid_amount/...`）：

| # | 列名 | 类型 | 列集归属 | # | 列名 | 类型 | 列集归属 |
|---|---|---|---|---|---|---|---|
| 1 | order_id | varchar(32) PK | 新（业务主键） | 27 | create_time | timestamp | 公共 |
| 2 | user_id | varchar(50) | 旧 | 28 | update_time | timestamp | 公共 |
| 3 | order_number | varchar(32) NOT NULL | **旧** | 29 | create_by | varchar(50) | 旧 |
| 4 | order_type | integer | 新 | 30 | update_by | varchar(50) | 旧 |
| 5 | order_status | integer | 新 | 31 | deleted | integer | 公共 |
| 6 | order_amount | numeric | 旧 | 32 | store_id | bigint | 新（V20260629_003） |
| 7 | discount_amount | numeric | 旧 | 33 | order_code | varchar(32) | **新** |
| 8 | actual_amount | numeric | 旧 | 34 | customer_id | bigint | 新 |
| 9 | payment_method | integer | 旧 | 35 | customer_name | varchar(50) | 新 |
| 10 | transaction_id | varchar(100) | 旧 | 36 | customer_phone | varchar(20) | 新 |
| 11 | payment_time | timestamp | 旧 | 37 | table_id | bigint | 新 |
| 12 | delivery_address | text | 旧 | 38 | table_name | varchar(50) | 新 |
| 13 | contact_name | varchar(50) | 旧 | 39 | dining_people_count | integer | 新 |
| 14 | contact_phone | varchar(20) | 旧 | 40 | payment_status | integer | 新 |
| 15 | remarks | text | 旧 | 41 | total_amount | bigint | 新 |
| 16 | idempotency_key | varchar(100) | 新（V20260406） | 42 | coupon_amount | bigint | 新 |
| 17 | order_source | integer | 新 | 43 | points_amount | bigint | 新 |
| 18 | merchant_id | varchar(50) | 旧 | 44 | delivery_fee | bigint | 新 |
| 19 | deliveryman_id | varchar(50) | 旧 | 45 | packaging_fee | bigint | 新 |
| 20 | estimated_delivery_time | timestamp | 旧 | 46 | final_amount | bigint | 新 |
| 21 | actual_delivery_time | timestamp | 旧 | 47 | paid_amount | bigint | 新 |
| 22 | cancel_reason | text | 旧 | 48 | points_earned | integer | 新 |
| 23 | refund_reason | text | 旧 | 49 | remark | text | 新 |
| 24 | refund_amount | numeric | 旧 | 50 | expected_time | timestamp | 新 |
| 25 | refund_time | timestamp | 旧 | 51 | cashier_user_id | bigint | 新 |
| 26 |  |  |  | 52 | create_user_id | bigint | 新 |

（51 列实测：`order_id/user_id/order_number/order_type/order_status/order_amount/discount_amount/actual_amount/payment_method/transaction_id/payment_time/delivery_address/contact_name/contact_phone/remarks/idempotency_key/order_source/merchant_id/deliveryman_id/estimated_delivery_time/actual_delivery_time/cancel_reason/refund_reason/refund_amount/refund_time/create_time/update_time/create_by/update_by/deleted/store_id/order_code/customer_id/customer_name/customer_phone/table_id/table_name/dining_people_count/payment_status/total_amount/coupon_amount/points_amount/delivery_fee/packaging_fee/final_amount/paid_amount/points_earned/remark/expected_time/cashier_user_id/create_user_id`）

### 1.3 OrderNew 映射面（orders 实体侧完整性验证）

`OrderNew.java` 全部 **33 个 @TableField 列逐一比对 information_schema → 0 缺失**（order_id/order_code/order_type/order_source/store_id/customer_id/customer_name/customer_phone/table_id/table_name/dining_people_count/order_status/payment_status/total_amount/discount_amount/coupon_amount/points_amount/delivery_fee/packaging_fee/final_amount/paid_amount/refund_amount/points_earned/remark/cancel_reason/delivery_address/expected_time/actual_delivery_time/cashier_user_id/create_user_id/create_time/update_time/deleted 全部在表）。

**结论 2：OrderNew ↔ orders 实体-表完全对齐，orders 是生产中真实存在、有数据、被核心订单链路（OrderNewServiceImpl L124 insert、OperationsReportServiceImpl、LiveMonitor/DecisionBoard/MarketingMember 等）消费的订单主表。**

### 1.4 确认结论

> **✅ 确认（阶段一）：`orders` 为销售订单域唯一生产真相源。**
> 事实链：① sales_order/sales_order_detail 生产表不存在（仅存在于已废弃参考脚本 `sql/sales_order.sql` 与硬编码 SQL 中）；② orders 51 列双结构并存且有真实数据；③ OrderNew（orders 的实体面）33/33 列对齐、为核心链路消费；④ flyway 最新版本 20260816.001，阶段一无任何 migration 落库。
>
> 边界说明（事实登记，不裁决）：订单域另有 `orders_legacy`（32 列，`Order.java` 映射，POS 旧链路 `OrderMapper` 使用）与 `order_items_legacy`（12 列，`OrderItem.java`）两张独立旧表、`kitchen_order`（42 列，KDS 端）。**PD-015 裁决的 SalesOrder 合并目标为 `orders`**；orders_legacy 为独立旧链路表，不在本映射面内（如阶段二契约确认 SalesOrder 语义另有归属，需另行裁决——本阶段不猜测）。

---

## 二、SalesOrder 19 字段逐列映射表

> SalesOrder.java（@TableName("sales_order")，表不存在）→ 目标：orders（51 列，唯一生产真相源）。
> 映射结论标注：🟢 可直映 / 🟡 需语义转换（含命名/类型/状态机漂移）/ 🔴 无对应（表无对应列，8 列）。
> 金额单位：SalesOrder Integer 金额按注释为「分」；orders 新列集 bigint 为「分」，旧列集 numeric（order_amount/actual_amount/discount_amount）单位语义未证实 → 标 🟡 待契约。

| # | SalesOrder 字段（Java 类型） | orders 目标列（类型） | 类型对比 | 状态机/语义对比 | 漂移标注 | 映射结论 | 处置建议（阶段二决策输入） |
|---|---|---|---|---|---|---|---|
| 1 | id（Long AUTO） | order_id（varchar(32) PK） | Long vs varchar | 主键生成：AUTO vs 业务字符串主键（"O"+时间戳） | **主键策略漂移** | 🟡 | 阶段二契约：SalesOrder.id 语义并入 order_id 的生成/关联方案 |
| 2 | orderNo（String） | order_number（varchar(32)，旧列集）/ order_code（varchar(32)，新列集） | 一致（varchar） | — | **命名漂移**：order_no ↔ order_number；且新旧列集双候选 | 🟡 | 阶段二契约：orderNo 落旧列集 order_number 还是新列集 order_code（双候选歧义） |
| 3 | customerId（Long） | customer_id（bigint） | Long vs bigint 兼容 | — | 无 | 🟢 可直映 | — |
| 4 | customerName（String） | customer_name（varchar(50)） | 兼容 | — | 无 | 🟢 可直映 | — |
| 5 | customerPhone（String） | customer_phone（varchar(20)） | 兼容 | — | 无 | 🟢 可直映 | — |
| 6 | orderType（String：dinein/takeout/delivery） | order_type（integer：1堂食 2外卖 3自提 4打包） | String vs integer | 字符串枚举 vs 数字枚举，映射关系未定义 | **类型+状态机漂移** | 🟡 | 阶段二契约：dinein/takeout/delivery ↔ 1/2/3 映射确认（含 4打包 是否用） |
| 7 | totalAmount（Integer） | total_amount（bigint，新列集）/ order_amount（numeric，旧列集） | Integer vs bigint/numeric | 单位「分」vs 新列集「分」/旧列集 numeric 单位未证实 | **类型漂移** + 双候选 | 🟡 | 阶段二契约：落 total_amount（新）还是 order_amount（旧）；旧列集单位确认 |
| 8 | discountAmount（Integer） | discount_amount（numeric，旧列集；同名新列集为 bigint coupon 面） | Integer vs numeric | 同名列存在但类型漂移；语义=优惠金额 | **类型漂移** | 🟡 | 阶段二契约：类型转换与单位确认 |
| 9 | actualAmount（Integer） | actual_amount（numeric，旧列集） | Integer vs numeric | 实收金额语义近似 | **类型漂移** | 🟡 | 阶段二契约：类型转换（Integer→Long 参考 T-039 既有转换逻辑）；或落 final_amount/paid_amount（新列集实付/已付）需契约 |
| 10 | status（String：pending/preparing/completed/delivered） | order_status（integer：0待确认 1已确认 2已完成 3已取消 4部分退款 5全额退款 6待评价） | String vs integer | **状态机漂移**：'completed' vs 2；pending/preparing/delivered 在 orders 状态机中无对应语义（1=制作中近似 preparing；delivered 无对应） | **状态机漂移（最大漂移点）** | 🔴→🟡 | **表无对应列（列名）**：orders 无 status 列；语义候选 order_status。阶段二契约：状态机映射表（pending/preparing/completed/delivered ↔ 0-6），delivered 无对应需决策（新增状态 vs 用 update_time 派生 vs 废弃） |
| 11 | orderTime（Date） | create_time（timestamp，公共列） | Date vs timestamp | 下单时间 vs 创建时间（语义近似） | **命名漂移**：order_time → create_time | 🔴→🟡 | **表无对应列（列名）**：orders 无 order_time 列；语义候选 create_time（下单=创建）。阶段二契约确认 |
| 12 | estimatedTime（Date） | estimated_delivery_time（timestamp，旧列集） | Date vs timestamp | 预计完成时间 vs 预计送达时间（外卖语义） | **命名+语义漂移** | 🔴→🟡 | **表无对应列（列名）**：orders 无 estimated_time；语义候选 estimated_delivery_time/expected_time（新列集期望送达）。阶段二契约确认 |
| 13 | completedTime（Date） | — | — | orders 无完成时间列（actual_delivery_time=实际送达，非完成） | 无对应 | 🔴 | **表无对应列**：处置建议=阶段二契约三选一（orders 补列 / 由 order_status=2 + update_time 派生 / 废弃该字段） |
| 14 | tableNo（String 桌号） | —（orders 有 table_id bigint + table_name varchar(50)） | String vs bigint/varchar | 桌号 vs 桌台ID/桌台名（号≠名） | **命名+语义漂移** | 🔴→🟡 | **表无对应列（列名）**：语义候选 table_id/table_name，映射关系待阶段二契约确认 |
| 15 | peopleCount（Integer） | dining_people_count（integer，新列集） | Integer vs integer 兼容 | 用餐人数语义一致 | **命名漂移**：people_count → dining_people_count | 🔴→🟡 | **表无对应列（列名）**：语义候选 dining_people_count（类型一致，直映度最高）。阶段二契约确认 |
| 16 | remark（String） | remark（text，新列集）/ remarks（text，旧列集） | String vs text 兼容 | 备注语义一致 | **命名漂移**：remark vs remarks 双候选 | 🟢（双候选） | 可直映；阶段二契约确认落 remark（新）还是 remarks（旧） |
| 17 | storeId（Long） | store_id（bigint） | Long vs bigint | 门店语义一致 | 无 | 🟢 可直映 | — |
| 18 | createdBy（String） | —（orders 有 create_by varchar(50) 旧列集 / create_user_id bigint 新列集） | String vs varchar/bigint | 创建人 String vs 创建人ID Long | **命名+类型漂移** + 双候选 | 🔴→🟡 | **表无对应列（列名）**：语义候选 create_by（旧）/create_user_id（新），类型/语义均需契约确认 |
| 19 | updatedBy（String） | —（orders 有 update_by varchar(50) 旧列集；无 update_user_id） | String vs varchar | 更新人 | **命名漂移** | 🔴→🟡 | **表无对应列（列名）**：语义候选 update_by（旧列集）。阶段二契约确认 |

### 2.1 表无对应列清单（8 列，阶段二决策输入）

| # | SalesOrder 字段 | orders 无此列 | 语义候选（存在但非同名，需契约） | 处置建议 |
|---|---|---|---|---|
| 1 | status | ✅ | order_status（integer，状态机全异） | 状态机映射契约（见 §2 第 10 行） |
| 2 | order_time | ✅ | create_time | 命名映射契约 |
| 3 | estimated_time | ✅ | estimated_delivery_time / expected_time | 语义选边契约 |
| 4 | completed_time | ✅ | 无（actual_delivery_time ≠ 完成时间） | 补列 / 派生 / 废弃三选一 |
| 5 | table_no | ✅ | table_id / table_name | 号↔ID/名映射契约 |
| 6 | people_count | ✅ | dining_people_count（类型一致） | 命名映射契约（直映度最高） |
| 7 | created_by | ✅ | create_by（旧）/ create_user_id（新） | 双候选+类型契约 |
| 8 | updated_by | ✅ | update_by（旧） | 命名映射契约 |

> 红线声明：上表「语义候选/处置建议」为**阶段二契约确认的决策输入，不是实现方案**——不猜测业务规则（状态机映射、候选列选边、补列/派生/废弃均待阶段二契约确认后才可实施）。

---

## 三、SalesOrderDetail ↔ order_items 归属核对

> SalesOrderDetail.java（@TableName("sales_order_detail")，表不存在，13 字段）→ 候选归属：order_items（17 列，存在，0 行）。

| # | SalesOrderDetail 字段（类型） | order_items 目标列（类型） | 结论 | 说明 |
|---|---|---|---|---|
| 1 | id（Long AUTO） | item_id（varchar，PK） | 🟡 | 主键策略漂移（同 SalesOrder.id） |
| 2 | orderId（Long） | order_id（varchar） | 🟡 | 类型漂移（Long vs varchar，订单主键 String 化） |
| 3 | productId（Long） | food_id（varchar） | 🟡 | 类型漂移（Long vs varchar）；⚠️ 观察：order_items.food_id 实测 varchar，与 `OrderItemNew.foodId`（Long）存在既存类型漂移（非本卡范围，登记观察） |
| 4 | productName（String） | product_name（varchar） | 🟢 可直映 | — |
| 5 | productCode（String） | — | 🔴 | order_items 无 product_code 列；处置=阶段二契约（补列 vs 由 food_id 关联派生 vs 废弃） |
| 6 | quantity（Integer） | quantity（integer） | 🟢 可直映 | — |
| 7 | unit（String） | — | 🔴 | order_items 无 unit 列（有 specification 规格）；处置=阶段二契约 |
| 8 | unitPrice（BigDecimal） | unit_price（bigint，分） | 🟡 | 类型漂移（BigDecimal vs bigint）+ 单位确认 |
| 9 | totalPrice（BigDecimal） | amount（bigint，分） | 🟡 | 命名漂移（total_price ↔ amount）+ 类型漂移 |
| 10 | inventoryCode（String） | — | 🔴 | order_items 无 inventory_code 列；处置=阶段二契约 |
| 11 | storeId（Long） | — | 🔴 | order_items 无 store_id 列；处置=阶段二契约 |
| 12 | createdBy（String） | — | 🔴 | order_items 无 created_by 列（有 create_time）；处置=阶段二契约 |
| 13 | updatedBy（String） | — | 🔴 | order_items 无 updated_by 列；处置=阶段二契约 |

**归属核对结论**：sales_order_detail 表不存在（唯一建表来源为已废弃脚本 `sql/sales_order.sql`）；order_items 为存在的明细归属候选（OrderItemNew 对齐 17 列）；**明细归属最终落点（order_items vs 其他）由阶段二契约确认，本阶段不裁决**（同 PD-015 决策事实「明细归属 sales_order_detail vs order_items」）。

---

## 四、映射结论汇总

| 结论 | SalesOrder 主表（19 字段） | SalesOrderDetail（13 字段） |
|---|---|---|
| 🟢 可直映（同名列、类型兼容） | 4：customer_id / customer_name / customer_phone / store_id（remark 双候选可直映） | 2：product_name / quantity |
| 🟡 需语义转换（命名/类型/状态机/主键漂移） | 7：id / orderNo / orderType / totalAmount / discountAmount / actualAmount / remark | 4：id / orderId / productId / unitPrice / totalPrice |
| 🔴 无对应（表无对应列，需阶段二契约决策） | **8**：status / order_time / estimated_time / completed_time / table_no / people_count / created_by / updated_by（其中 7 列有语义候选、completed_time 完全无候选） | 7：productCode / unit / inventoryCode / storeId / createdBy / updatedBy（另 remark 候选待定） |

**结论 3：SalesOrder 19 字段中 8 列在 orders 无对应列（其中 7 列存在语义候选需契约确认、1 列 completed_time 完全无候选）；9 个同名列中仅 4 个可直映；状态机（'completed' vs 2）、主键生成（Long AUTO vs String）、金额类型（Integer vs bigint/numeric）为三大系统性漂移点——阶段二按批迁移的契约输入。**

---

## 五、全仓 sales_order / SalesOrder 引用扫描清单（现状，2026-08-16）

> 分类：A 实体 / B 查询 / C 接口 / D 测试 / E 前端 / F 脚本与资源 / G 治理文档。**本清单为现状登记，不修改。**

### A. 实体层（2 文件）
| 文件 | 引用点 | 说明 |
|---|---|---|
| `backend/.../entity/SalesOrder.java` | L8 `@TableName("sales_order")` | 19 字段实体，映射表不存在 |
| `backend/.../entity/SalesOrderDetail.java` | L8 `@TableName("sales_order_detail")` | 13 字段实体，映射表不存在 |

### B. 查询层（3 文件）
| 文件 | 引用点 | 说明 |
|---|---|---|
| `backend/.../resources/mapper/SalesOrderMapper.xml` | L28 `SELECT * FROM sales_order`；L52 `SELECT * FROM sales_order_detail`；L56 `INSERT INTO sales_order_detail`；L61 `DELETE FROM sales_order_detail` | 硬编码 SQL 4 处，全部命中缺表 |
| `backend/.../mapper/SalesOrderMapper.java` | L12 `BaseMapper<SalesOrder>` + L14-20 自定义 4 方法 | 泛型绑定 sales_order 实体 |
| `backend/.../service/impl/DashboardServiceImpl.java` | L41/45 注入 SalesOrderMapper；L97-102 selectCount 查 sales_order（order_time/status='completed'） | KL-054 命中点；catch 先行已改（明确错误态，commit 72f0d5a） |

### C. 接口层（3 文件）
| 文件 | 引用点 | 说明 |
|---|---|---|
| `backend/.../controller/SalesOrderController.java` | `/v1/sales/order` 8 端点（page/create/update/delete/confirm/complete/detail 增删） | 全部走缺表链路 → 500（api-test-results-4modules.csv L129 实测） |
| `backend/.../service/SalesOrderService.java` | 接口 10 方法 | — |
| `backend/.../service/impl/SalesOrderServiceImpl.java` | CRUD + confirmOrder/completeOrder/confirmDelivery（T-039 应收联动）+ deductInventoryForOrder + createFinanceVoucherForOrder | 状态机 pending/preparing/completed/delivered 定义于此（阶段二状态机契约输入） |

### D. 测试层（1 文件）
| 文件 | 引用点 | 说明 |
|---|---|---|
| `backend/.../test/.../SalesOrderServiceImplTest.java` | Mockito 单测 6 例（T-039 confirmDelivery 应收联动） | 无 DB 依赖（mock），实体迁移后需同步更新 |

### E. 前端（1 文件，注释级）
| 文件 | 引用点 | 说明 |
|---|---|---|
| `frontend/src/api/dashboard/index.ts` | L44 / L191 注释「数据源: sales_order 表（status=completed）」 | **注释漂移**（无运行时依赖）；前端全仓无 /v1/sales/order 调用（grep 0 命中） |

### F. 脚本与资源（3 文件）
| 文件 | 引用点 | 说明 |
|---|---|---|
| `backend/src/main/resources/sql/sales_order.sql` | L11/L41 CREATE TABLE sales_order / sales_order_detail | **[DEPRECATED] 参考性脚本**（2026-06-30 标注废弃，未启用；MySQL 语法） |
| `backend/src/main/resources/db/clear-data.sql` | L43 TRUNCATE sales_order_item；L44 TRUNCATE sales_order | 维护脚本；`sales_order_item` 表名与实体名（sales_order_detail）亦不符（PENDING_ISSUES_BACKLOG PO-7 同型） |
| `backend/scripts/test-api-4modules.ps1`（L154-155）+ `backend/scripts/api-test-results-4modules.csv`（L129） | GET /v1/sales/order/page 测试项 | 实测结果 FAIL 500 已在案 |

### G. 治理文档（登记引用，非代码）
`production-known-limitations.md`（KL-054 L75/485-492/514/722/726）、`docs/quality/entity-table-mapping-audit.md`（ETM-002/003）、`docs/quality/oicbe-b1-qa-report.md`、`production-regression-test.md`（REG-ETM-002/003）、`docs/quality/client-audit-{pos,miniprogram,kitchen,receiving-mobile}.md`、`docs/quality/multi-client-audit-summary.md`、`oic-1-task-board.md`（O-20260814-01）、`oic-2-task-board.md`（POS-BE-005/MP-BE-001）、`docs/data-business-chain.md`（L28/170/171）、`docs/data-chain-action-plan.md`（L55-56）、`docs/design/terminal-responsibility-spec.md`（L179）、`docs/audit/PENDING_ISSUES_BACKLOG.md`（L1714）、`docs/audit/IMPLEMENTATION_DESIGN.md`（L3216）、`documents/产品管理板块——产品数据与业务逻辑流程图 V1.0.md`（L358）、`scan-report-*.csv`（鉴权扫描产物，8 端点）、`.opencode/skills/production-collab/SKILL.md`（L84 协议样本）、`product-decision-backlog.md`（PD-015）、`oic-be-task-board.md`（本卡）。

**扫描统计：主代码引用 9 文件（A 2 + B 3 + C 3 + D 1）+ 前端注释 1 + 脚本/资源 3 + 治理文档约 19 处。前端无运行时消费；报表/统计面（OperationsReportServiceImpl / DailySettlementServiceImpl 等）**零 sales_order 引用**（走 orders / orders_legacy，事实登记）。**

---

## 六、禁止新增 sales_order 引用声明（阶段一裁决生效）

> 依据：PD-015 架构裁决（2026-08-16）：阶段一包含「**禁止新增 sales_order 引用**」。
> 登记位置：`oic-be-task-board.md` §〇 红线 R-8（本文件同步登记）。

**声明（自 2026-08-16 起生效）：**

1. **后续任何开发（含新功能、修复、重构）不得新增对 `sales_order` / `sales_order_detail` 表名、`SalesOrder` / `SalesOrderDetail` 实体、`SalesOrderMapper` 系列接口的引用**（含 @TableName、硬编码 SQL、Mapper 方法签名、QueryWrapper 列名、前端数据源注释、测试用例、脚本 SQL）。
2. **新增订单相关代码一律走 `orders` / `order_items`（`OrderNew` / `OrderItemNew` 实体体系）**。
3. 存量引用（§五清单）**只减不增**：随阶段二按批迁移收敛；迁移前的既有引用维持现状（明确错误态，不吞错）。
4. 例外：治理文档（KL/ETM/PD/QA/回归记录）对历史漂移事实的登记性引用不受限，但须标注「历史事实」。
5. 违反本声明的新增引用 = 违反 OIC-BE 红线 R-8，QA 验收直接 FAIL 项。

---

## 七、阶段二批次计划（占位登记，本阶段不执行）

> 依据：PD-015 裁决「按批迁移代码（实体/查询/接口/报表/统计），每批 QA + 基线」；OIC-BE 节奏每批 2~3 卡、每批 developer → qa → 部署 → observe。
> 登记位置：`oic-be-task-board.md` §四·2（本文件同步登记）。**阶段二启动须由 planner 按序放卡 + 映射契约确认后执行，禁止一次性大改。**

| 批次 | 范围 | 建议卡（占位，2~3 卡/批） | 依赖契约 | QA + 基线 |
|---|---|---|---|---|
| **Batch 2**（实体+查询） | 实体迁移 / 查询迁移 | OICBE-B2-001：SalesOrder/SalesOrderDetail 实体对齐（@TableName + 字段映射 + 8 无对应列处置） | §二/§三映射契约（状态机/主键/类型/候选列选边） | QA：三向核对+冒烟；REG-ETM-002/003 扩展真实数据断言 |
| | | OICBE-B2-002：SalesOrderMapper.xml 4 语句 + Mapper 接口对齐 orders/order_items；DashboardServiceImpl 查询改走 orders 真实数据（今日销售概览恢复） | 同上 | 同上 |
| | | OICBE-B2-003：SalesOrderServiceImplTest 测试对齐（实体构造/状态机） | 同上 | 同上 |
| **Batch 3**（接口） | 接口/服务迁移 | OICBE-B3-001：/v1/sales/order 8 端点语义迁移（SalesOrderController + SalesOrderService 层） | 接口契约（是否保留端点/字段形态） | QA：接口冒烟；REG-接口基线 |
| | | OICBE-B3-002：T-039 应收联动（confirmDelivery→ReceivableService）与财务凭证链路迁移到 orders 体系 | 应收触发契约 | QA：联动冒烟；REG-T039 基线 |
| **Batch 4**（报表/统计+清理） | 报表/统计核对 / 遗留清理 | OICBE-B4-001：报表/统计面核对（OperationsReport/DailySettlement 等 orders 面确认无 sales_order 引用 + dashboard 统计真实数据断言） | — | QA：报表核对；REG-统计基线 |
| | | OICBE-B4-002：遗留清理（废弃 sql/sales_order.sql、clear-data.sql 表名修正、前端注释同步、测试脚本更新） | — | QA：清理核对；REG-回归基线 |

> 阶段二全部批次启动前需：① 映射契约确认（§二/§三待确认点逐项闭环）；② planner 排卡；③ 每批独立 QA + 基线，禁止跨批合并。

---

## 八、修改证据（developer 阶段一）

- **修改文件**：`docs/quality/sales-order-orders-mapping.md`（新建，本文档）；`oic-be-task-board.md`（回写卡 2 + R-8 + 阶段二占位）
- **修改内容**：只读核对产出——orders 真相源确认书（§一）、19 字段映射表（§二）、明细归属核对（§三）、结论汇总（§四）、引用扫描清单（§五）、禁止新增引用声明（§六）、阶段二批次计划（§七）
- **影响范围**：无代码、无表结构影响；仅治理文档与任务板
- **自测结果**：information_schema 只读实测 2026-08-16（表存在性/orders 51 列全清单/order_items 17 列/OrderNew 33 列 0 缺失/flyway 最新 20260816.001/orders 1 行 order_items 0 行）；全仓 grep 扫描 sales_order（69 命中）与 SalesOrder（100+ 命中）已分类归位
- **风险**：低。阶段二契约未确认点已全部标注「待阶段二契约确认」，无猜测实现；migration 未落库（红线合规）
- **未发现事实性错误**：无（阶段一核对与 QA 已验事实一致；新发现 facts：orders_legacy/order_items_legacy 独立旧表边界、order_items.food_id varchar 与 OrderItemNew.foodId Long 既存类型漂移——登记观察，不在本卡范围）

*产出：2026-08-16，developer（会话4）。*
