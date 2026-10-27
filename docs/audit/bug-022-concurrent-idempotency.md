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

## 6. 补充验证：重放路径**不写子表**（Lead 2026-10-28 要求）

每轮并发前/后各取一次四张表行数，算 Δ（期望：orders=1、order_items≤1、pos_call_numbers≤1、kitchen_order≤1）：

| 轮 | A code | B code | reused | ordersΔ | itemsΔ | callnumΔ | kitchenΔ | 判定 |
|----|--------|--------|--------|---------|--------|----------|----------|------|
| 1–10 | 0 | 0 | True | **1** | **1** | **0** | **1** | **OK** |

```
子表被多写的轮次: 0
清理后残留: 0
```

**为什么这条重要**（Lead 原话被我采纳写进文档）：部分写入是最难查的一类数据不一致 ——
主表 1 行但子表 2 份，对账时才会发现；本次 10 轮验证专门盯这个。

### `pos_call_numbers` Δ=0 的解释（不是漏写，是号段表不存这一单）
`pos_call_numbers` 是**按门店+日期的号段计数器**，不是 per-order 表；
订单只把 `call_number` 落到 `orders.call_number` 列（本轮 10 轮 orders Δ 已含该列）。
故该表 Δ=0 属正确，不视为漏写。

## 7. 代码内注释（Lead 要求：理由写进代码，防止半年后被「优化」回去）

`OrderInsertAttempt` 类头已写：
```java
/**
 * <h2>⚠ 为什么不用 INSERT ... ON CONFLICT（请勿「优化」回来）</h2>
 * <p>ON CONFLICT 要在 Mapper XML 里把 orders 的 50+ 列 INSERT 重写一份，等于手工维护第二份列清单。
 * 2026-10-28 当天刚因「实体与表列不一致」修了 12 处（roles / employees 全大写列名、accounting_subject 缺
 * create_by / update_by、permission_assignment_log 缺 deleted、dish_combo.combo_name 写错）——
 * 为避免一个 bug 而制造一个更隐蔽的 bug，不值得。</p>
 * <p>本类要达到的关键性质只有一个：撞唯一键时外层事务不被 PG 标记 aborted（SQLState 25P02）…</p>
 * <h2>将来若要换成 ON CONFLICT</h2>
 * <p>只替换本类实现即可，调用方（PosOrderCreateServiceImpl）不动；但请先补测试：
 * 并发同 key × N 轮「主表 1 行 + 子表不多行」断言。</p>
 */
```
