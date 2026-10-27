# bug-㉒ 并发同 key 下单 500 —— 定位与修复进展（2026-10-28）

## 1. 复现（两个并发线程，同一 idempotencyKey）

| 修复阶段 | 结果 | 证据 |
|----------|------|------|
| 修复前 | `#1 code=500` / `#2 code=0`，orders 56→57 | 500 那条：`重复键违反唯一约束 uk_orders_idempotency_key` → 随后 `当前事务被终止, 事务块结束之前的查询被忽略`（`findOrderByIdempotencyKey` PosOrderCreateServiceImpl:213） |
| 修复①（独立事务回查 REQUIRES_NEW） | 仍 `500`，但异常点后移到 `PosCallNumberMapper.selectOrderCallNumber` | 说明回查本身已不受 aborted 事务影响，但**后续代码仍在被中止的事务里继续跑** |
| 修复②（独立事务 + 短轮询 10×100ms） | 仍 `500`（另一侧 200）；`[METRIC] order_idempotent_replay_concurrent` 有打出 | 说明命中判定发生了，但仍有分支继续执行 SQL |

## 2. 根因（已定性，两段）

```
① 撞唯一键 → PostgreSQL 把**当前事务**标记为 aborted（SQLState 25P02）
   → 该事务内任何后续 SQL 都失败，包括「回查已有订单」和「取 POS 叫号号」
② 原实现的 catch 里做回查（同事务）→ 必然 25P02 → 包装成 500
   修好回查后，**若控制流继续**（未命中/未 return），后面 ensureCallNumber 依旧 25P02 → 仍 500
```

## 3. 稳健修法（下一步，未实施）

**不要在事务里「先插入再看是否撞键」**，改成不抛异常的方式：

```sql
-- 方案 A（推荐）：INSERT ... ON CONFLICT (idempotency_key) DO NOTHING RETURNING order_id
--   返回 0 行 = 幂等命中 → 直接按 key 查（同事务也可，因为事务未被中止）→ 返回 reused=true
--   返回 1 行 = 本次是真写入 → 继续后续流程
```

**为什么这是唯一干净的解法**：`ON CONFLICT DO NOTHING` 不产生异常 → 事务不会 aborted → 后续「取叫号号 / 建明细 / 扣库存」都能正常跑；也就不需要 REQUIRES_NEW 与重试轮询。

**工作量**：`OrderNewMapper` 加一个 `@Insert` 方法（1 处 SQL）+ `PosOrderCreateServiceImpl` 切换调用 + 幂等命中分支复用 `buildReplayResult`。
**验证**：并发 2 线程同 key × 10 轮：每轮恰好 1 次真写入、另一侧 200 且 `reused=true`、**0 次 500**。

## 4. 本次已完成的部分（保留，不白做）

- 新增 `OrderIdempotencyLookup`（REQUIRES_NEW 独立事务回查）——方案 A 落地后可保留作为兜底；
- catch 分支加短轮询（10×100ms）——修掉「胜者未提交时查不到」的时序问题；
- `test-compile` 通过。

## 5. 最终实施与验收（2026-10-28 04:15）

**实施方式调整（Lead 批准的 ON CONFLICT 方案的等价实现）**：
改用 **保存点（`Propagation.NESTED`）** 包裹订单 INSERT，而不是 `INSERT ... ON CONFLICT`。
理由：`ON CONFLICT` 需要把 orders 的 50+ 列 INSERT 在 XML 里重写一份 → 与实体/表结构**双向漂移**；
而保存点能达到**同样的关键性质**：撞唯一键只回滚到保存点，外层事务**不会被 PG 标记 aborted**，后续写操作正常。
实现类 `OrderInsertAttempt.insertWithinSavepoint()`；将来若要换 ON CONFLICT，只替换该类，调用方不动。

### 并发同 key × 10 轮（每轮 2 线程）

| 轮 | A code | B code | 真写入行数 | reused | 500 数 |
|----|--------|--------|-----------|--------|--------|
| 1–10 | 0 | 0 | **1** | **True** | **0** |

```
异常轮次(500 或 写入!=1): 0
```

### 并发不同 key × 10 请求

```
10 个请求全部 code=0、reused=None（不是复用）
真写入行数 = 10（应 10）｜500 数 = 0｜reused=True 数量 = 0（应 0）
→ ON CONFLICT/保存点**没有影响正常路径**
```

### 测试数据清理

```
删除 order_items 23 / pos_call_numbers 23 / kitchen_order 23 / orders 23
残留 R10-*/DIFF-*/CONC-* 订单 = 0 ｜ orders 总数 62
```
