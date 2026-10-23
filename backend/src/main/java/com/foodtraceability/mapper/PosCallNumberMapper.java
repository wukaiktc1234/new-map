package com.foodtraceability.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * POS 叫号号（callNumber）专用 Mapper。
 *
 * <p>号段复用现有通用号段表 {@code order_sequence}（不新造表）：
 * 键形如 {@code CALL-{storeId}-{yyyyMMdd}-{prefix}}，按「门店 + 自然日 + 类型前缀」隔离，
 * 跨天自然从 1 重新开始。自增使用单条
 * {@code INSERT ... ON CONFLICT DO UPDATE ... RETURNING}，行锁保证并发安全，
 * 不需要应用层加锁，也不存在「先查 MAX 再 +1」的竞态。</p>
 */
public interface PosCallNumberMapper {

    /**
     * 号段原子自增并返回新值（首次调用即初始化 1）。
     *
     * @param sequenceKey 号段键，如 {@code CALL-1-20261023-A}
     * @return 本次分配到的序号（从 1 开始）
     */
    @Select("INSERT INTO order_sequence (sequence_key, sequence_value, create_time, update_time) "
            + "VALUES (#{sequenceKey}, 1, NOW(), NOW()) "
            + "ON CONFLICT (sequence_key) DO UPDATE "
            + "SET sequence_value = order_sequence.sequence_value + 1, update_time = NOW() "
            + "RETURNING sequence_value")
    Integer nextSequenceValue(@Param("sequenceKey") String sequenceKey);

    /** 读取订单已落库的叫号号（用于幂等判定：已取号直接返回，不重复取号） */
    @Select("SELECT call_number FROM orders WHERE order_id = #{orderId} LIMIT 1")
    String selectOrderCallNumber(@Param("orderId") String orderId);

    /** 订单是否存在（orders 新表；用于避免给遗留单空取号浪费号段） */
    @Select("SELECT COUNT(1) FROM orders WHERE order_id = #{orderId}")
    int countOrder(@Param("orderId") String orderId);

    /** 读取订单门店ID */
    @Select("SELECT store_id FROM orders WHERE order_id = #{orderId} LIMIT 1")
    Long selectOrderStoreId(@Param("orderId") String orderId);

    /** 读取订单类型（0堂食 1外卖 2自提） */
    @Select("SELECT order_type FROM orders WHERE order_id = #{orderId} LIMIT 1")
    Integer selectOrderType(@Param("orderId") String orderId);

    /** 读取订单创建时间（用于把历史单的叫号号归入其自身业务日，避免污染当天号段） */
    @Select("SELECT create_time FROM orders WHERE order_id = #{orderId} LIMIT 1")
    java.time.LocalDateTime selectOrderCreateTime(@Param("orderId") String orderId);

    /**
     * 绑定叫号号到订单（CAS：仅当尚未取号时写入）。
     *
     * @return 1 表示本次写入成功；0 表示已被其它请求抢先取号（调用方应读回既有号）
     */
    @Update("UPDATE orders SET call_number = #{callNumber}, update_time = NOW() "
            + "WHERE order_id = #{orderId} AND (call_number IS NULL OR call_number = '')")
    int bindOrderCallNumber(@Param("orderId") String orderId, @Param("callNumber") String callNumber);

    /** 读取叫号屏记录的叫号号 */
    @Select("SELECT call_number FROM call_record WHERE order_id = #{orderId} LIMIT 1")
    String selectCallRecordCallNumber(@Param("orderId") String orderId);

    /** 把叫号号同步到叫号屏记录（幂等：已有值不覆盖） */
    @Update("UPDATE call_record SET call_number = #{callNumber}, update_time = NOW() "
            + "WHERE order_id = #{orderId} AND (call_number IS NULL OR call_number = '')")
    int bindCallRecordCallNumber(@Param("orderId") String orderId, @Param("callNumber") String callNumber);
}
