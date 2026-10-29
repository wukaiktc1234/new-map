package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.ExceptionOps;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ExceptionOpsMapper extends BaseMapper<ExceptionOps> {

    /** 幂等查重：同 key 直接返回（不做任何写） */
    @Select("SELECT * FROM exception_ops WHERE idempotency_key = #{key} LIMIT 1")
    ExceptionOps selectByKey(@Param("key") String key);

    /**
     * CAS 置已支付（异常单补收款）。
     *
     * <p>条件 `payment_status &lt;&gt; 2`（未支付）—— <b>抄自既有权威判定</b>
     * `PosOrderPaymentServiceImpl:715-717`（注释 :708-709：orders 的 payment_status 才是资金口径，2=已支付，
     * order_status 是业务流转进度）。**不要改成 order_status 条件**：该列是 integer 且无 pending 值，实测只有 0/2/5。</p>
     *
     * @return 影响行数：1=抢到；0=已被其它终端处理 → 409
     */
    @Update("UPDATE orders SET payment_status = 2, paid_amount = #{amountFen}, payment_method = #{paymentMethod}, "
            + "update_time = now() "
            + "WHERE order_id = #{orderId} AND COALESCE(payment_status, 0) <> 2 AND deleted = 0")
    int casMarkPaid(@Param("orderId") String orderId,
                    @Param("amountFen") Long amountFen,
                    @Param("paymentMethod") Integer paymentMethod);

    /**
     * CAS 取消（异常单作废）。
     *
     * <p>取消值 **3 = 已取消**，出处：{@code OrderStatusCodes:31-32 ORDER_CANCELLED = 3}
     * （注释 :11「order_status：0待确认·1已确认·2已完成·3已取消·5全额退款」），
     * 且 {@code OrderMapper:92/:131} 统计「已作废/已取消」用的就是 {@code order_status = 3}（canonical 表口径）。</p>
     *
     * <p>⚠ 已知冲突：{@code OrderTimeoutTask:45 STATUS_CANCELLED = 5}，但该类操作的是**遗留 Order（orders 旧表）**，
     * 不是 canonical orders；故本方法取 3。</p>
     *
     * @return 1=抢到；0=已被处理 → 409
     */
    @Update("UPDATE orders SET order_status = 3, update_time = now() "
            + "WHERE order_id = #{orderId} AND COALESCE(payment_status, 0) <> 2 AND deleted = 0")
    int casCancel(@Param("orderId") String orderId);

    /** 已支付判定（作废前拦截：不能作废已收款单） */
    @Select("SELECT COALESCE(payment_status, 0) FROM orders WHERE order_id = #{orderId} AND deleted = 0")
    Integer paymentStatusOf(@Param("orderId") String orderId);

    /**
     * 本班次「异常单总数」（Lead 2026-10-29 ②）：
     * = 待处理（pending 口径） + 已处理（有 exception_ops） + 已因 TTL 过期关单。
     * **语义是"历史全部"，不代表待处理**；前端徽标只认 pendingExceptionCount。
     */
    @Select("SELECT count(*) FROM orders o WHERE o.shift_id = #{shiftId} AND o.deleted = 0 "
            + "  AND (COALESCE(o.payment_status,0) <> 2 OR o.cancel_reason = 'EXPIRED_AUTO' "
            + "       OR EXISTS (SELECT 1 FROM exception_ops e WHERE e.order_id=o.order_id AND e.deleted=0))")
    long countExceptionOrderTotal(@Param("shiftId") Long shiftId);

    /** 交班闸：本班次待处理异常单数（未支付 + 无 exception_ops 记录） */
    @Select("SELECT count(*) FROM orders o "
            + "WHERE o.shift_id = #{shiftId} AND o.deleted = 0 "
            + "  AND COALESCE(o.payment_status, 0) <> 2 "
            + "  AND COALESCE(o.cancel_reason, '') <> 'EXPIRED_AUTO' "   // TTL 自动关的单不算「待处理异常单」
            + "  AND NOT EXISTS (SELECT 1 FROM exception_ops e WHERE e.order_id = o.order_id AND e.deleted = 0)")
    long countPendingExceptionOrders(@Param("shiftId") Long shiftId);
}
