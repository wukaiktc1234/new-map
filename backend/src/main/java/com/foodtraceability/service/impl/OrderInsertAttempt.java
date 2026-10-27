package com.foodtraceability.service.impl;

import com.foodtraceability.entity.OrderNew;
import com.foodtraceability.mapper.OrderNewMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * bug-㉒：订单插入的**保存点级**尝试（NESTED 事务）。
 *
 * <h2>⚠ 为什么不用 INSERT ... ON CONFLICT（请勿「优化」回来）</h2>
 *
 * <p>ON CONFLICT 要在 Mapper XML 里把 orders 的 <b>50+ 列 INSERT 重写一份</b>，等于手工维护<b>第二份列清单</b>。
 * 2026-10-28 当天刚因「实体与表列不一致」修了 12 处（roles / employees 全大写列名、accounting_subject 缺
 * create_by / update_by、permission_assignment_log 缺 deleted、dish_combo.combo_name 写错）——
 * <b>为避免一个 bug 而制造一个更隐蔽的 bug，不值得。</b></p>
 *
 * <p>本类要达到的<b>关键性质只有一个</b>：撞唯一键时<b>外层事务不被 PG 标记 aborted</b>（SQLState 25P02），
 * 从而后续「取叫号号 / 建明细 / 扣库存」仍能正常写。Propagation.NESTED（保存点）一行注解即可做到，
 * 且<b>不引入任何列清单</b>。</p>
 *
 * <h2>将来若要换成 ON CONFLICT</h2>
 * <p>只替换本类实现即可，调用方（PosOrderCreateServiceImpl）不动；但请先补测试：
 * 并发同 key × N 轮「主表 1 行 + 子表不多行」断言（见 docs/audit/bug-022-concurrent-idempotency.md）。</p>
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
