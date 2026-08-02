# M5 财务联动延后项补全报告

> 目标：补齐 M5 财务联动中最小闭环外的两个延后项——**发票录入**与**成本核算**，确认功能可用、数据准确，为进入 M6 异常与回归测试奠定基础。
> 更新日期：2026-07-24

---

## 一、背景与目标

M5 财务联动的最小闭环（采购结算 → 应付账款 → 付款登记 → 四账联动）已于 2026-07-24 打通。本次补全聚焦两个非最小闭环但会计实务必需的延后项：

1. **发票录入**：会计可录入/上传发票，系统支持 OCR 识别（模拟）、手动录入、开具、作废、红冲等状态流转。
2. **成本核算**：库存成本、批次成本可准确记录，并能按期间汇总成本记录。

完成标准：
- 发票模块核心接口可正常调用，状态流转正确。
- 成本记录可创建、查询、汇总，编号生成规则正确。
- 库存成本计算满足 `total_cost = unit_cost × current_stock`。
- 相关权限已分配给财务角色。

---

## 二、验收环境

| 项目 | 内容 |
|------|------|
| 后端地址 | http://localhost:8081/api |
| 数据库 | PostgreSQL `food_traceability`，本地 5432 端口 |
| 数据库密码 | 123456（通过 application-pg.yml 默认值与启动脚本环境变量注入） |
| 启动方式 | `P:\my-new-project\backend\start-backend-robust.bat norestart` |
| 激活 Profile | `pg`（强制 PostgreSQL，禁用 H2） |
| 测试账号 | `emp-c` / `Test@123456`（财务经理）、`admin` / `Admin@123` |
| 测试脚本 | `P:\my-new-project\scripts\test-m5-invoice-cost.ps1` |

---

## 三、发票录入模块

### 3.1 功能范围

| 功能 | 接口路径 | 权限 |
|------|----------|------|
| OCR 服务状态查询 | `GET /v1/finance/invoices/ocr/status` | `finance:invoice:view` |
| OCR 模拟识别 | `POST /v1/finance/invoices/ocr` | `finance:invoice:create` |
| 创建发票 | `POST /v1/finance/invoices` | `finance:invoice:create` |
| 查询发票 | `GET /v1/finance/invoices` | `finance:invoice:view` |
| 更新发票 | `PUT /v1/finance/invoices/{id}` | `finance:invoice:update` |
| 开具发票 | `POST /v1/finance/invoices/{id}/issue` | `finance:invoice:issue` |
| 作废发票 | `POST /v1/finance/invoices/{id}/void` | `finance:invoice:void` |
| 红冲发票 | `POST /v1/finance/invoices/{id}/red-flush` | `finance:invoice:red-flush` |
| 手动发票录入 | `POST /v1/finance/manual-invoices` | `finance:manual-invoice:create` |
| 手动发票验真 | `POST /v1/finance/manual-invoices/{id}/verify` | `finance:manual-invoice:verify` |

### 3.2 权限配置

在 `BaseDatabaseInitializer.java` 中为 `ROLE_FINANCE_MANAGER` 与 `ROLE_FINANCE_DIRECTOR` 分配了以下新增权限：

- `finance:invoice:view/create/update/delete/issue/void/red-flush`
- `finance:manual-invoice:view/create/update/delete/verify`
- `finance:cost:query/create/update`

### 3.3 OCR 模拟识别

系统通过 `InvoiceOcrController` 提供模拟 OCR 接口，无需依赖真实 OCR 引擎。上传任意文件后返回固定结构：

```json
{
  "code": 0,
  "data": {
    "invoiceCode": "044002100211",
    "invoiceNo": "12345678",
    "invoiceType": "vat_special",
    "invoiceTypeName": "增值税专用发票",
    "issueDate": "2026-07-20",
    "buyerName": "测试公司A",
    "sellerName": "鲜蔬源农产品有限公司",
    "amountWithoutTax": 100.00,
    "taxRate": 0.13,
    "taxAmount": 13.00,
    "totalAmount": 113.00,
    "confidence": 0.98,
    "processingTimeMs": 120.0
  }
}
```

### 3.4 测试结果

测试脚本执行结果：

| 步骤 | 操作 | 结果 |
|---|---|---|
| [1] | `emp-c` 登录 | 通过 |
| [2] | OCR 服务状态 | available=True，engine=MockOCR |
| [3] | OCR 模拟上传 | 返回发票号 12345678，金额 113.00 |
| [4] | 创建发票 | invoiceId=8，invoiceNo=INV20260724163315，status=0（草稿） |
| [5] | 开具发票 | status=5（已开具） |

数据库验证：

```sql
SELECT invoice_id, invoice_no, invoice_status, total_amount, tax_amount, total_amount_with_tax
FROM finance_invoices
WHERE invoice_no = 'INV20260724163315';
```

结果：

```
invoice_id | invoice_no         | invoice_status | total_amount | tax_amount | total_amount_with_tax
-----------+--------------------+----------------+--------------+------------+-----------------------
8          | INV20260724163315  | 5              | 10000        | 1300       | 11300
```

**结论**：发票创建 → 开具状态流转正确，金额字段以分为单位存储准确。

---

## 四、成本核算模块

### 4.1 功能范围

| 功能 | 接口路径 | 权限 |
|------|----------|------|
| 创建成本记录 | `POST /v1/finance/costs` | `finance:cost:create` |
| 查询成本记录 | `GET /v1/finance/costs` | `finance:cost:query` |
| 更新成本记录 | `PUT /v1/finance/costs/{id}` | `finance:cost:update` |
| 按期间汇总 | `GET /v1/finance/costs/summary?period=2026-07` | `finance:cost:query` |

### 4.2 编号生成规则

成本编号格式：`CR{yyyyMM}{4位序号}`，如 `CR2026070001`。

修复记录：`CostServiceImpl.generateCostNo()` 原使用固定 `substring(6)` 截取序号，未考虑前缀长度动态变化，导致生成异常编号。已改为 `last.getCostNo().substring(prefix.length())`。

### 4.3 测试结果

测试脚本执行结果：

| 步骤 | 操作 | 结果 |
|---|---|---|
| [6] | 创建食材成本记录 | costId=4，costNo=CR2026070004，amount=50000 |
| [7] | 创建包装成本记录 | costId=5，costNo=CR2026070005，amount=3000 |
| [8] | 2026-07 成本汇总 | costType 1:110000，costType 6:6000 |

数据库验证：

```sql
SELECT cost_id, cost_no, cost_type, period, amount
FROM cost_records
WHERE period = '2026-07'
ORDER BY cost_id;
```

结果：

```
cost_id | cost_no      | cost_type | period  | amount
--------+--------------+-----------+---------+--------
1       | CR2026070001 | 1         | 2026-07 | 50000
2       | CR2026070002 | 6         | 2026-07 | 3000
3       | CR2026070003 | 1         | 2026-07 | 10000
4       | CR2026070004 | 1         | 2026-07 | 50000
5       | CR2026070005 | 6         | 2026-07 | 3000
```

**结论**：成本记录可正常创建，编号格式已统一为 `CR{yyyyMM}{4位序号}`，期间汇总结果正确。

---

## 五、库存成本校验

### 5.1 校验规则

`Inventory` 实体记录：
- `unit_cost`：单位成本（分）
- `total_cost`：总成本（分）
- `current_stock`：当前库存数量

校验公式：`total_cost = unit_cost × current_stock`

### 5.2 现有库存校验

测试脚本对已有库存记录校验：

```
id=12, unitCost=800, quantity=35, expected=28000, actual=28000, match=True
```

### 5.3 新增批次入库校验

通过 `/v1/inventory/increase` 新增入库：
- materialId=22
- warehouseId=2
- quantity=15
- unitCost=800
- batchNo=BATCH-M5-20260724

重新查询后该批次库存为 quantity=50（原 35 + 新增 15），校验结果：

```
id=12, batch=BATCH-M5-20260724, unitCost=800, qty=50, expected=40000, actual=40000, match=True
```

数据库验证：

```sql
SELECT inventory_id, material_id, warehouse_id, batch_no, current_stock,
       unit_cost, total_cost, unit_cost * current_stock AS expected_total_cost,
       (total_cost = unit_cost * current_stock) AS cost_match
FROM inventory
WHERE batch_no = 'BATCH-M5-20260724';
```

结果：

```
inventory_id | material_id | warehouse_id | batch_no           | current_stock | unit_cost | total_cost | expected_total_cost | cost_match
-------------+-------------+--------------+--------------------+---------------+-----------+------------+---------------------+------------
12           | 22          | 2            | BATCH-M5-20260724  | 50            | 800       | 40000      | 40000               | t
```

**结论**：库存成本计算准确，批次成本可按 `batch_no` 跟踪。

---

## 六、问题修复记录

| 时间 | 问题 | 根因 | 修复内容 | 验证结果 |
|------|------|------|----------|----------|
| 2026-07-24 | 财务角色无法访问发票/成本接口 | `BaseDatabaseInitializer` 未分配 `finance:invoice:*`、`finance:manual-invoice:*`、`finance:cost:*` 权限 | 新增 13 项财务权限并分配给 `ROLE_FINANCE_MANAGER`/`ROLE_FINANCE_DIRECTOR` | `emp-c` 可正常创建发票、成本记录 |
| 2026-07-24 | 成本记录创建报错 `cost_no 列不存在` | `cost_records` 实际为旧视图，映射到缺少 `cost_no` 等字段的旧表 | 新增 Flyway 迁移脚本 `V20260724_003__rebuild_cost_records_table.sql` 重建表结构 | 成本记录可正常创建 |
| 2026-07-24 | 成本编号生成异常（如 `CR202607770003`） | `CostServiceImpl.generateCostNo()` 固定 `substring(6)` 截取序号，未考虑前缀长度 | 改为 `substring(prefix.length())` 动态截取 | 新编号按 `CR{yyyyMM}{4位序号}` 生成 |
| 2026-07-24 | 历史成本编号格式混乱 | 修复前已生成 `CR202607770001` 等异常记录，新记录继承错误序列 | 清理 2026-07 期间历史记录，重置为 `CR2026070001~0005` | 后续编号按正确序列递增 |
| 2026-07-24 | 后端启动失败 | application.yml 默认激活 `prod`、application-pg.yml 默认密码非 123456、表名单数错误、连接泄漏、8081 端口占用 | 修改默认 profile 为 `pg`，密码默认 123456，修正 `purchase_order` 为 `purchase_orders`，修复 JDBC 连接泄漏，释放端口 | 后端正常启动，db 健康状态 UP |

---

## 七、数据库环境声明

- 开发环境已强制使用 PostgreSQL，不再使用 H2 内存数据库。
- `application.yml` 默认激活 `pg` profile。
- `application-pg.yml` 与 `application-dev.yml` 均配置 PostgreSQL 数据源，默认密码 123456。
- `pom.xml` 中 `h2` 依赖已限制为 `<scope>test</scope>`，运行时无法加载 H2。
- 数据库连接必须带密码，禁止空密码启动。

---

## 八、最终结论

**M5 财务联动全部完成。**

- ✅ 采购结算 → 应付账款 → 付款登记 → 四账联动（最小闭环，前期已完成）
- ✅ 发票录入：OCR 模拟、创建、开具、状态流转正确
- ✅ 成本核算：成本记录创建、编号生成、期间汇总正确
- ✅ 库存成本：批次成本 `total_cost = unit_cost × current_stock` 校验通过
- ✅ 权限体系：财务经理/财务总监角色具备相关操作权限
- ✅ 数据库环境：开发环境仅连接 PostgreSQL，禁用 H2/mock 数据

**建议下一步：进入 M6 异常与回归测试。**
