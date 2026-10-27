package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.foodtraceability.entity.OrderNew;
import com.foodtraceability.mapper.OrderNewMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * bug-㉒ 修复：幂等键回查必须跑在**独立事务**里。
 *
 * <p>背景（2026-10-28 实测）：并发同 key 下单时，后到的请求 INSERT 撞 uk_orders_idempotency_key，
 * PostgreSQL 会把当前事务标记为 aborted；此时在同一事务里再查一次，会报
 * 「当前事务被终止，事务块结束之前的查询被忽略」→ 原实现（catch 里直接回查）因此返回 500。</p>
 *
 * <p>本类用 REQUIRES_NEW 另开连接查询，绕开被中止的事务。</p>
 */
@Component
public class OrderIdempotencyLookup {

    private final OrderNewMapper orderNewMapper;

    public OrderIdempotencyLookup(OrderNewMapper orderNewMapper) {
        this.orderNewMapper = orderNewMapper;
    }

    /** 独立事务按幂等键查订单（不受外层 aborted 事务影响）。 */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public OrderNew findByKeyInNewTx(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isEmpty()) {
            return null;
        }
        List<OrderNew> found = orderNewMapper.selectList(new LambdaQueryWrapper<OrderNew>()
                .eq(OrderNew::getIdempotencyKey, idempotencyKey)
                .eq(OrderNew::getDeleted, 0)
                .orderByAsc(OrderNew::getOrderId)
                .last("LIMIT 1"));
        return found == null || found.isEmpty() ? null : found.get(0);
    }
}
