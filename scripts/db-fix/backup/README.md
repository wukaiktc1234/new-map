# 测试数据备份（本地保留，不进 git）

本目录存放 **purge 前的数据备份**（`pg_dump --data-only`）。
**约定**：`*.sql` 一律不进 git（见同目录 `.gitignore`），避免 2MB 级备份让仓库膨胀。

## 现有备份（本地）

| 文件 | 内容 | 大小 | 生成时间 | 用途 |
|---|---|---|---|---|
| `20261025-pre-purge.sql` | 14 张表（P3 前置清库前） | 1.84 MB | 2026-10-25 | P3 清库前的完整回滚点 |
| `20261026-pre-purge-finance.sql` | 14 张财务表 | 1.95 MB | 2026-10-26 01:3x | P4-4 收尾清库前的完整回滚点 |
| `20261026-pre-purge-orders.sql` | `orders` / `order_payment_records` / `pos_shifts` | 53 KB | 2026-10-26 01:3x | **软删 109 条已支付测试订单**前的保护性备份（可逐行回滚） |

## 回滚方式

```powershell
$env:PGPASSWORD='123456'
& 'P:\my-new-project\PostgreSQL\18\bin\psql.exe' -h 127.0.0.1 -U postgres -d food_traceability `
  -f 'P:\my-new-project\scripts\db-fix\backup\20261026-pre-purge-orders.sql'
```

> 只回滚 `orders` 时注意：`fund_flows` 已重建，回滚订单会让「已支付但无 SALE 流水」的订单重新出现 → A1（零容忍）会红；
> 正确顺序是：先回滚备份 → 再让 seed 重建对应流水，或同时回滚 `20261026-pre-purge-finance.sql`。

## 保留策略

- 本地保留最近 **3 次** purge 备份；更早的移到 `../archive/`（同样不进 git）；
- 任何 `purge` / 数据修复脚本都必须在本 README 登记一行（文件、内容、时间、用途）。