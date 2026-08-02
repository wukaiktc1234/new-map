package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.Order;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface OrderMapper extends BaseMapper<Order> {
    
    @Update("UPDATE orders_legacy SET ORDER_STATUS = #{status}, UPDATE_TIME = NOW() WHERE ORDER_ID = #{orderId}")
    int updateOrderStatus(@Param("orderId") String orderId, @Param("status") Integer status);

    @Update("UPDATE orders_legacy SET ORDER_STATUS = 7, REFUND_AMOUNT = #{refundAmount}, REFUND_REASON = #{refundReason}, REFUND_TIME = NOW(), UPDATE_TIME = NOW(), UPDATE_BY = #{updateBy} WHERE ORDER_ID = #{orderId}")
    int updateOrderRefund(@Param("orderId") String orderId, @Param("refundAmount") BigDecimal refundAmount, @Param("refundReason") String refundReason, @Param("updateBy") String updateBy);

    @Select("SELECT COALESCE(MAX(CAST(SUBSTRING(ORDER_NUMBER, 10) AS BIGINT)), 0) FROM orders_legacy WHERE ORDER_NUMBER LIKE CONCAT(#{prefix}, '%') AND ORDER_NUMBER ~ CONCAT('^', #{prefix}, '[0-9]+$')")
    int getMaxOrderSequence(@Param("prefix") String prefix);

    @Insert("INSERT INTO order_sequence (sequence_key, sequence_value, create_time) VALUES (#{sequenceKey}, 1, NOW()) ON CONFLICT (sequence_key) DO NOTHING")
    int initSequence(@Param("sequenceKey") String sequenceKey);

    @Update("UPDATE order_sequence SET sequence_value = sequence_value + 1 WHERE sequence_key = #{sequenceKey} RETURNING sequence_value")
    int incrementSequence(@Param("sequenceKey") String sequenceKey);

    @Select("SELECT sequence_value FROM order_sequence WHERE sequence_key = #{sequenceKey}")
    int getLastInsertId();

    // ==================== 日结对账聚合查询方法 ====================

    /**
     * 按支付方式聚合订单数据
     * 从orders表按PAYMENT_METHOD GROUP BY，返回每种支付方式的金额总和和订单数
     * 支付方式：0=微信, 1=支付宝, 2=现金, 4=会员余额
     *
     * @param storeId   门店ID（对应MERCHANT_ID字段）
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 支付方式聚合结果列表，每条记录包含payment_method, total_amount, order_count
     */
    List<Map<String, Object>> aggregateByPaymentMethod(@Param("storeId") String storeId,
                                                        @Param("startTime") LocalDateTime startTime,
                                                        @Param("endTime") LocalDateTime endTime);

    /**
     * 汇总优惠金额
     * 计算指定时间范围内所有订单的DISCOUNT_AMOUNT总和（单位：分）
     *
     * @param storeId   门店ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 优惠总金额（分）
     */
    Long sumDiscountAmount(@Param("storeId") String storeId,
                           @Param("startTime") LocalDateTime startTime,
                           @Param("endTime") LocalDateTime endTime);

    /**
     * 聚合退款数据
     * 统计指定时间范围内已退款订单（ORDER_STATUS=7）的退款金额和笔数
     *
     * @param storeId   门店ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 包含totalRefundAmount和refundCount的Map
     */
    Map<String, Object> aggregateRefunds(@Param("storeId") String storeId,
                                         @Param("startTime") LocalDateTime startTime,
                                         @Param("endTime") LocalDateTime endTime);

    /**
     * 聚合作废订单数据
     * 统计指定时间范围内已作废订单（ORDER_STATUS=5）的总金额和笔数
     *
     * @param storeId   门店ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 包含totalCancelledAmount和cancelledCount的Map
     */
    Map<String, Object> aggregateCancelled(@Param("storeId") String storeId,
                                           @Param("startTime") LocalDateTime startTime,
                                           @Param("endTime") LocalDateTime endTime);

    // ==================== orders_legacy 表的统计方法（用于订单统计聚合） ====================

    /**
     * 统计今日 POS 端订单数（orders_legacy 表）
     * POS端产生的交易行为是管理端获取订单数据的主要来源
     */
    @Select("SELECT COUNT(*) FROM orders_legacy WHERE CAST(CREATE_TIME AS DATE) = CURRENT_DATE AND DELETED = 0")
    long countTodayPosOrders();

    /**
     * 统计今日 POS 端销售额（orders_legacy 表，仅已支付订单）
     * POS 端订单状态：0待支付 1已支付 2待配送 3配送中 4已完成 5已取消 6退款中 7已退款
     * 已支付订单包括：1, 2, 3, 4
     * actual_amount 单位为元（BigDecimal），需乘以 100 转换为分
     */
    @Select("SELECT COALESCE(CAST(SUM(ACTUAL_AMOUNT) * 100 AS BIGINT), 0) FROM orders_legacy " +
            "WHERE ORDER_STATUS IN (1, 2, 3, 4) AND CAST(CREATE_TIME AS DATE) = CURRENT_DATE AND DELETED = 0")
    long sumTodayPosSales();

    /**
     * 统计今日 POS 端已完成订单数（orders_legacy 表，order_status = 4）
     */
    @Select("SELECT COUNT(*) FROM orders_legacy WHERE ORDER_STATUS = 4 AND CAST(CREATE_TIME AS DATE) = CURRENT_DATE AND DELETED = 0")
    long countTodayPosCompletedOrders();

    /**
     * 统计今日 POS 端已取消订单数（orders_legacy 表，order_status = 5）
     */
    @Select("SELECT COUNT(*) FROM orders_legacy WHERE ORDER_STATUS = 5 AND CAST(CREATE_TIME AS DATE) = CURRENT_DATE AND DELETED = 0")
    long countTodayPosCancelledOrders();

    /**
     * 统计 POS 端订单总数（全部）
     */
    @Select("SELECT COUNT(*) FROM orders_legacy WHERE DELETED = 0")
    long countAllPosOrders();

    /**
     * 统计 POS 端已完成订单总数（order_status = 4）
     */
    @Select("SELECT COUNT(*) FROM orders_legacy WHERE ORDER_STATUS = 4 AND DELETED = 0")
    long countAllPosCompletedOrders();

    /**
     * 统计 POS 端销售额总数（仅已支付订单，单位：分）
     */
    @Select("SELECT COALESCE(CAST(SUM(ACTUAL_AMOUNT) * 100 AS BIGINT), 0) FROM orders_legacy " +
            "WHERE ORDER_STATUS IN (1, 2, 3, 4) AND DELETED = 0")
    long sumAllPosSales();

    /**
     * 按日期统计 POS 端订单数
     */
    @Select("SELECT COUNT(*) FROM orders_legacy WHERE CAST(CREATE_TIME AS DATE) = #{date} AND DELETED = 0")
    long countPosOrdersByDate(@Param("date") java.time.LocalDate date);

    /**
     * 按日期统计 POS 端销售额（仅已支付订单，单位：分）
     */
    @Select("SELECT COALESCE(CAST(SUM(ACTUAL_AMOUNT) * 100 AS BIGINT), 0) FROM orders_legacy " +
            "WHERE ORDER_STATUS IN (1, 2, 3, 4) AND CAST(CREATE_TIME AS DATE) = #{date} AND DELETED = 0")
    long sumPosSalesByDate(@Param("date") java.time.LocalDate date);

    /**
     * 按日期范围统计 POS 端每日趋势（聚合 orders_legacy 表）
     * 已支付订单状态：1, 2, 3, 4
     * actual_amount 单位为元（BigDecimal），需乘以 100 转换为分
     * @param startDate 开始日期（含）
     * @param endDate 结束日期（含）
     * @return 每日趋势列表，包含 date/order_count/revenue 字段
     */
    @Select("SELECT CAST(CREATE_TIME AS DATE) as date, COUNT(*) as order_count, " +
            "COALESCE(CAST(SUM(ACTUAL_AMOUNT) * 100 AS BIGINT), 0) as revenue " +
            "FROM orders_legacy WHERE CAST(CREATE_TIME AS DATE) BETWEEN #{startDate} AND #{endDate} " +
            "AND ORDER_STATUS IN (1, 2, 3, 4) AND DELETED = 0 " +
            "GROUP BY CAST(CREATE_TIME AS DATE) ORDER BY date")
    List<Map<String, Object>> getDailyTrendForPos(@Param("startDate") java.time.LocalDate startDate,
                                                   @Param("endDate") java.time.LocalDate endDate);
}
