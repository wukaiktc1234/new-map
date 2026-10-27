package com.foodtraceability.service.impl;

import com.foodtraceability.entity.OrderNew;
import com.foodtraceability.mapper.OrderNewMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * bug-㉒：订单插入的**保存点级**尝试（NESTED 事务）。
 *
 * <p>为什么不是 ON CONFLICT：那需要把 orders 的 50+ 列 INSERT 在 XML 里重写一遍，
 * 与实体/表结构**双向漂移**风险高；而 NESTED 保存点能达到同样的关键性质 ——
 * 撞唯一键时只回滚到保存点，**外层事务不会被 PG 标记 aborted**，后续取叫号号/建明细/扣库存可正常执行。</p>
 *
 * <p>若将来改为 ON CONFLICT，只需替换本类实现，调用方不动。</p>
 */
@Component
public class OrderInsertAttempt {

    private final OrderNewMapper orderNewMapper;

    public OrderInsertAttempt(OrderNewMapper orderNewMapper) {
        this.orderNewMapper = orderNewMapper;
    }

    /** 在保存点内插入；撞唯一键抛 DuplicateKeyException，但**不污染外层事务**。 */
    @Transactional(propagation = Propagation.NESTED)
    public void insertWithinSavepoint(OrderNew orderNew) {
        orderNewMapper.insert(orderNew);
    }
}
