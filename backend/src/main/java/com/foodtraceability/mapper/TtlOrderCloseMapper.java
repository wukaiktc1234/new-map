package com.foodtraceability.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

/**
 * TTL 超时关单（canonical orders）。
 *
 * <p><b>为什么不再用 OrderTimeoutTask：</b>该类操作的是**遗留 Order/OrderMapper（orders 旧表）**，
 * 对 canonical orders 完全无效（实测 order_status=0 且未支付的死单 92 笔一直躺着）。</p>
 */
@Mapper
public interface TtlOrderCloseMapper {

    /** 作业级互斥：多实例部署时只允许一个实例扫（PG advisory lock，会话级） */
    @Select("SELECT pg_try_advisory_lock(#{key})")
    boolean tryAdvisoryLock(@Param("key") long key);

    @Select("SELECT pg_advisory_unlock(#{key})")
    boolean releaseAdvisoryLock(@Param("key") long key);

    /** 取一批死单（LIMIT 由调用方控制，禁止无界扫描） */
    @Select("SELECT order_id FROM orders "
            + "WHERE order_status = 0 AND COALESCE(payment_status, 0) <> 2 AND deleted = 0 "
            + "  AND create_time < now() - (#{minutes} || ' minutes')::interval "
            + "ORDER BY create_time LIMIT #{limit}")
    List<String> findExpiredOrderIds(@Param("minutes") int minutes, @Param("limit") int limit);

    /**
     * 双 CAS 关单：状态 + 未支付 两个条件同时成立才更新（防「顾客刚好已支付」被覆盖）。
     *
     * <p>取消值 3=已取消（OrderStatusCodes:31-32 / OrderMapper:92,:131）。</p>
     *
     * @return 影响行数：0 = CAS 未命中（已被支付/已处理）→ 放弃，不覆盖
     */
    // ⚠ 实测：orders 只有 cancel_reason，**没有 cancel_time 列**（我第一次写时假设存在，任务 92 笔全部报
    //   关系 "orders" 的 "cancel_time" 字段不存在）。关单时间用既有 update_time 承载，不新增列。
    @Update("UPDATE orders SET order_status = 3, cancel_reason = #{reason}, update_time = now() "
            + "WHERE order_id = #{orderId} AND order_status = 0 "
            + "  AND COALESCE(payment_status, 0) <> 2 AND deleted = 0")
    int casCancelExpired(@Param("orderId") String orderId, @Param("reason") String reason);

    /** 该单明细（取 food_id，供 foods.stock 回补；与 recoverStockForRefundNew 同源） */
    @Select("SELECT food_id, quantity FROM order_items WHERE order_id = #{orderId} AND deleted = 0")
    List<Map<String, Object>> findItems(@Param("orderId") String orderId);

    /** 原料锁定总量（本项目从未实现锁定，用于 T6a 断言「全程保持 0」） */
    @Select("SELECT COALESCE(SUM(locked_quantity), 0) FROM inventory")
    java.math.BigDecimal sumLockedQuantity();
}
