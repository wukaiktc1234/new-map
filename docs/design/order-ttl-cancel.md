# 订单生命周期 TTL 自动关单机制 — 技术方案（后端）

> 前置更正：**本项目不是 MySQL + Redis**。实测栈为 PostgreSQL 18 + Spring Boot 3.2 + MyBatis-Plus，README 中 Redis/RabbitMQ 标注为「待对接」（当前未接入）。
> 因此方案 A（Redis ZSet 延迟队列）与 B（MQ 延时消息）**当前不可用** —— 不是偏好问题，是依赖不存在。采用 **方案 C（DB 扫表）+ 四条工程约束**。

---

## 1. 技术选型理由

~~~
方案 C（DB 扫表）必备的四条约束（否则就是「全表扫描 + 大事务」）：
  ① 部分索引：只索引「在飞」订单，恒定小，不随历史膨胀
     CREATE INDEX ... ON orders (create_time)
       WHERE order_status = 0 AND COALESCE(payment_status,0) <> 2 AND deleted = 0;
  ② 批量上限 500：避免长事务、锁表、WAL 放大
  ③ 单订单独立事务 REQUIRES_NEW：一单失败不拖垮整批
  ④ 多实例互斥 pg_try_advisory_lock：防两个副本同时扫同一批

何时换 A/B：接入 Redis 或 MQ 后，只把「扫表找候选」换成「消费延时消息」，
            CAS + 库存释放的订单级逻辑原样复用 —— 核心不绑定调度方式。

没选数据库触发器：关单要联动库存/券/积分/流水，触发器里做等于把业务埋进 DDL，不可测不可回滚。
~~~

---

## 2. 落地前必须先取证的 5 项（不许假设）

本项目今天已有 11 次「假设列名/字段存在」的事故，以下五项必须先 \d 或 grep 确认：

| # | 待确认 | 取证方式 | 已知信息（今日实测） |
|---|---|---|---|
| 1 | orders 是否有 cancel_reason 列 | \d orders | **未确认**；kitchen_order 有该列，orders 不保证有 |
| 2 | 订单「取消」的状态值 | grep OrderStatusCodes | **已知 order_status=3 已取消**（OrderStatusCodes:31-32；OrderMapper:92/131 canonical）。注意 OrderTimeoutTask:45 的 STATUS_CANCELLED=5 是**遗留 Order 旧表** |
| 3 | 「待支付」判据 | grep payment_status | **已知 payment_status <> 2**（PosOrderPaymentServiceImpl:715-717 权威判定） |
| 4 | 库存锁定列 | \d inventory | **表名是 inventory 不是 inventories**；locked_quantity 列**需确认** |
| 5 | 券/积分预扣表 | grep + \d | **未确认**；若无则记 TODO，不许空写一段退券代码 |

---

## 3. 核心定时任务（CAS + 事务边界）

~~~java
@Component
public class OrderTtlCancelJob {
    private static final int BATCH = 500;
    private static final String REASON = "EXPIRED_AUTO";
    private static final long ADVISORY_KEY = 981273001L;

    private final OrderNewMapper orderNewMapper;
    private final OrderTtlCancelTxBean txBean;   // 跨 Bean 调用，否则 @Transactional 静默失效
    private final OrderProperties props;         // order.pending.timeout.minutes 默认 15

    @Scheduled(fixedDelayString = "${order.pending.scan.delay.ms:60000}")
    public void tick() {
        if (!orderNewMapper.tryAdvisoryLock(ADVISORY_KEY)) {
            log.debug("[TTL] 另一实例正在处理，跳过本轮");
            return;
        }
        try {
            int total = 0;
            while (true) {
                List<String> ids = orderNewMapper.selectExpiredCandidateIds(
                        props.getPendingTimeoutMinutes(), BATCH);
                if (ids.isEmpty()) break;
                for (String orderId : ids) {
                    try {
                        CancelOutcome r = txBean.cancelOne(orderId, REASON);
                        if (r == CancelOutcome.CANCELLED) total++;
                        else if (r == CancelOutcome.CAS_MISS)
                            log.info("[TTL] CAS 未命中（顾客刚好已支付），放弃不覆盖 orderId={}", orderId);
                    } catch (Exception e) {
                        log.error("[TTL] 关单异常 orderId={} err={}", orderId, e.toString(), e);
                    }
                }
                if (ids.size() < BATCH) break;
            }
            if (total > 0) log.info("[TTL] 本轮自动关单 {} 笔", total);
        } finally {
            orderNewMapper.releaseAdvisoryLock(ADVISORY_KEY);
        }
    }
}
~~~

~~~java
@Component
public class OrderTtlCancelTxBean {
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public CancelOutcome cancelOne(String orderId, String reason) {
        int rows = orderNewMapper.casCancelExpired(orderId, reason);
        if (rows == 0) return CancelOutcome.CAS_MISS;   // 绝不覆盖已支付状态
        lockReleaseService.releaseByOrderInCurrentTx(orderId);   // 同事务
        assetRollbackService.rollbackInCurrentTx(orderId, reason); // 同事务，见 §5
        return CancelOutcome.CANCELLED;
    }
}
~~~

~~~xml
<select id="selectExpiredCandidateIds" resultType="java.lang.String">
    SELECT order_id FROM orders
     WHERE order_status = 0
       AND COALESCE(payment_status, 0) &lt;&gt; 2
       AND deleted = 0
       AND create_time &lt; now() - (#{minutes} || ' minutes')::interval
     ORDER BY create_time
     LIMIT #{limit}
</select>

<update id="casCancelExpired">
    UPDATE orders
       SET order_status = 3,
           cancel_reason = #{reason},
           update_time = now()
     WHERE order_id = #{orderId}
       AND order_status = 0
       AND COALESCE(payment_status, 0) &lt;&gt; 2
       AND deleted = 0
</update>
~~~

为什么 CAS 同时带 order_status=0 与 payment_status<>2：
  收银员补收款走的是 payment_status CAS；定时任务若只看 order_status 会与它错位。
  两个条件都带 → 任何一方先成功，另一方必然 0 行。

---

## 4. 库存释放联动

必须先解决：order_items 只带 food_id（菜品），不带 location_id / material_id —— 无法定位库位与原料
（与「退款原料级回补」同一缺口，需 dish_ingredient 映射，属产品决策）。

情形 A（锁的是菜品维度可售/锁定数）—— 可行：
~~~xml
<update id="releaseLockedByFood">
    UPDATE inventory
       SET locked_quantity = GREATEST(0, locked_quantity - #{qty}),
           update_time = now()
     WHERE food_id = #{foodId} AND location_id = #{locationId}
</update>
~~~
GREATEST(0, ...) 是必须的：并发或历史脏数据可能让 locked_quantity 小于释放量，
直接减法会变负数 —— 负锁定会让可用量虚高。

情形 B（锁的是原料库位维度）—— 当前不具备，只能记 metric：
  log.warn("[METRIC] ttl_cancel_inventory_release_skipped orderId={} reason=no_material_mapping", orderId);

事务边界：同事务（本项目无 MQ，同事务比自造补偿表简单且正确）。
失败即回滚 → 订单仍待支付 → 下轮重试。宁可多扫几轮，不可「已关单但库存没释放」。

---

## 5. 优惠券/积分回退（资损防范）

~~~java
public interface OrderAssetRollbackService {
    /** 在当前事务内原路退回；返回实际退回清单（用于写流水） */
    List<AssetRollback> rollbackInCurrentTx(String orderId, String reason);
}
~~~

要求：① 只退回该订单实际预扣的资产（按 order_id 查预扣记录，不按用户当前余额盲加）；
     ② 每笔退回写一条资产流水（remark="[TTL] 订单超时关单退回"）；
     ③ 幂等：退回前查「该单是否已有 TTL 退回流水」，有则跳过（防重跑双退）；
     ④ 券已过期时退成「已过期」还是「可用」需产品确认，先记 TODO。

---

## 6. 交班异常池隔离（修改现有 SQL）

~~~xml
<select id="countPendingExceptionOrders" resultType="long">
    SELECT count(*) FROM orders o
     WHERE o.shift_id = #{shiftId}
       AND o.deleted = 0
       AND o.order_status = 0
       AND COALESCE(o.payment_status, 0) &lt;&gt; 2
       AND NOT EXISTS (SELECT 1 FROM exception_ops e
                        WHERE e.order_id = o.order_id AND e.deleted = 0)
       AND COALESCE(o.cancel_reason, '') &lt;&gt; 'EXPIRED_AUTO'   <!-- 防御性冗余 -->
</select>
~~~

逻辑说明：TTL 关单把 order_status 置 3 后，order_status = 0 已排除它们。
新增 cancel_reason 条件是防御性冗余 —— 防止将来有人把「待支付」判据改成只看 payment_status（那样 3 会被算进来）。
两道条件都在，改错一处也不会把死单放进异常池。

---

## 7. 对前端暴露 expiresAt

~~~java
public LocalDateTime getExpiresAt() {
    return createTime == null ? null : createTime.plusMinutes(pendingTimeoutMinutes);
}
~~~
不建议在 orders 加 expires_at 列：它随配置变化，落库会与配置漂移（改配置后历史值全错）。

---

## 8. 测试用例清单

| # | 用例 | 期望 | 证据要求 |
|---|---|---|---|
| T1 | 正常过期 | create_time=now()-16min 未支付 → 跑一轮 | order_status=3、cancel_reason=EXPIRED_AUTO、locked_quantity 减少对应量，贴前后值 |
| T2 | **并发竞态（最关键）** | 过期单 → 同时发起「补收款」与「job 关单」 | **恰好一个成功**：要么 payment_status=2（收银员赢），要么 order_status=3（job 赢且补收款 409）；绝不两者都成功；贴两个响应+终态 |
| T3 | CAS 命中 0 行 | 手工置 payment_status=2 后跑 job | 打 CAS 未命中日志，订单状态不变 |
| T4 | 不进异常池 | TTL 死单(shift_id 指向某班次) → endShift | **不返回 409**，交班成功；贴 countPendingExceptionOrders 前后 |
| T5 | 批量上限 | 造 600 笔过期单跑一轮 | 单轮最多 500，无长事务/超时 |
| T6 | 库存不为负 | locked_quantity=1，需释放 5 | 结果 0（不是 -4），贴前后值 |
| T7 | 多实例互斥 | 两实例/两轮并发触发 | 只有一方执行，贴 advisory lock 日志 |
| T8 | 券/积分退回（若实现） | 用券下单后过期关单 | 券回可用 + 一条资产流水；重复跑不双退 |

T2 是唯一能证明「没有资损」的用例，必须真的并发（两客户端同时发，不能用先后模拟）。

---

## 9. 配置项与前置开关

~~~yaml
order:
  pending:
    timeout:
      minutes: 15
    scan:
      delay:
        ms: 60000
      batch: 500
~~~

⚠ 本项目实测 app.scheduling.enabled 在三个 profile 里都是 false（今天刚修过，撤权延迟因此一直是 45 秒）。
  本方案依赖 @Scheduled，上线前必须先确认该开关为 true，否则任务静默不跑且不报错。

---

## 10. 落地顺序

1. 先取证 §2 五项（尤其 orders.cancel_reason、inventory.locked_quantity 是否存在）；
2. 迁移：补 cancel_reason（若无）+ 部分索引（带 DO $$ 断言）；
3. 实现 Mapper + job + txBean（库存释放先按情形 A）；
4. 改 countPendingExceptionOrders（加 cancel_reason 双保险）；
5. 跑 T1/T2/T3/T4/T6/T7；
6. 券/积分回退与原料级库存释放作为独立任务（都依赖产品决策）。
