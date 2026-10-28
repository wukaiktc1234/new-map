# ADR 与危险区

### R4 补充：成对 ADJUST 的适用边界（2026-10-28）

> 当**产品路径强制「流水 + 库存」同改**、而实际只需要「补流水」时，
> 可用**成对 ADJUST（正 + 负）在同一个事务内**把净库存还原。**必须同事务**，否则中间态会被并发读到。

**为什么必须同事务（真实隐患）**：
```text
inventory 5/material 26 修复时：+100 → stock 100→200，再 −100 → 200→100
若两次各自 commit，中间态 200 会真实存在；此刻并发的 POS 扣料/库存查询会读到 200（错误可用量），
可能放行本应失败的扣减 → 超卖。组 13 的中间态 40 同理（会导致本可扣的料扣不动）。
```
**证据（本仓库实证）**：`scripts/qa/db/InventoryAlignRunner.java` 用 `TransactionTemplate` 包住成对调用；
两条流水的 `xmin` 相同即同一事务：
```text
movement 1815 change=+2 balance=102 ADJUST xmin=1010672
movement 1816 change=−2 balance=100 ADJUST xmin=1010672   ← 同一 tx
库存 5：100 → 100（净 0，中间态 102 从未提交）
```
**禁止事项**：
1. ❌ 两次独立 Service 调用各自开事务（本项目 Service 方法是 `@Transactional`，直接连调即是两个事务）；
2. ❌ 用「先改库存、后补流水」两步操作；
3. ❌ 手写 `INSERT INTO inventory_movement`（绕过 Service 会漏库存更新与状态刷新）。
