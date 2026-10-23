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

    /**
     * W1-EC-04A: 从 orders 表取 MAX 序号（canonical source）
     * 注意：此方法仍保留但不再被 PosOrderNumberGenerator 使用（已改用 orderNewMapper）
     */
    @Select("SELECT COALESCE(MAX(CAST(SUBSTRING(ORDER_NUMBER, 10) AS BIGINT)), 0) FROM orders WHERE ORDER_NUMBER LIKE CONCAT(#{prefix}, '%') AND ORDER_NUMBER ~ CONCAT('^', #{prefix}, '[0-9]+$')")
    int getMaxOrderSequence(@Param("prefix") String prefix);

    @Insert("INSERT INTO order_sequence (sequence_key, sequence_value, create_time) VALUES (#{sequenceKey}, 1, NOW()) ON CONFLICT (sequence_key) DO NOTHING")
    int initSequence(@Param("sequenceKey") String sequenceKey);

    @Update("UPDATE order_sequence SET sequence_value = sequence_value + 1 WHERE sequence_key = #{sequenceKey} RETURNING sequence_value")
    int incrementSequence(@Param("sequenceKey") String sequenceKey);

    /**
     * 原实现 {@code int getLastInsertId()} 没有入参却引用 #{sequenceKey}，MyBatis 绑不出参数 →
     * 永远返回 null → 基本类型转换异常 → POS 订单号每次都降级到「遗留表 max+1」，
     * 一旦遗留表与 orders 表不同步（如数据库异常恢复后）就会撞 orders_order_number_key 唯一键。
     *
     * <p>2026-10-23 修复：改为单语句原子取号（插入或 +1 后直接 RETURNING 新值），
     * 行锁串行化并发请求，不再有「先 increment 再 SELECT」的读-改-写竞态。</p>
     */
    @Select("INSERT INTO order_sequence (sequence_key, sequence_value, create_time) VALUES (#{sequenceKey}, 1, NOW()) "
            + "ON CONFLICT (sequence_key) DO UPDATE SET sequence_value = order_sequence.sequence_value + 1 "
            + "RETURNING sequence_value")
    Integer nextSequenceValue(@Param("sequenceKey") String sequenceKey);

    // ==================== 日结对账聚合查询方法 ====================

    /**
     * 按支付方式聚合订单数据
     * 从orders表关联order_payment_records表获取支付方式，返回每种支付方式的金额总和和订单数
     * 支付方式：1=现金, 2=微信, 3=支付宝, 4=银行卡, 5=积分, 6=混合支付
     *
     * @param storeId   门店ID（对应store_id字段）
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 支付方式聚合结果列表，每条记录包含payment_method, total_amount, order_count
     */
    List<Map<String, Object>> aggregateByPaymentMethod(@Param("storeId") String storeId,
                                                        @Param("startTime") LocalDateTime startTime,
                                                        @Param("endTime") LocalDateTime endTime);

    /**
     * 汇总优惠金额
     * 计算指定时间范围内所有订单的discount_amount总和（单位：分）
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
     * 统计指定时间范围内已退款订单（order_status IN (4, 5)）的退款金额和笔数
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
     * 统计指定时间范围内已作废订单（order_status = 3）的总金额和笔数
     *
     * @param storeId   门店ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 包含totalCancelledAmount和cancelledCount的Map
     */
    Map<String, Object> aggregateCancelled(@Param("storeId") String storeId,
                                           @Param("startTime") LocalDateTime startTime,
                                           @Param("endTime") LocalDateTime endTime);

    // ==================== 订单统计聚合（canonical 表 orders；状态码 0待确认/1已确认/2已完成/3已取消/5全额退款） ====================

    /**
     * 统计今日订单数（canonical orders 表）
     * POS端产生的交易行为是管理端获取订单数据的主要来源
     */
    @Select("SELECT COUNT(*) FROM orders WHERE CAST(create_time AS DATE) = CURRENT_DATE AND deleted = 0")
    long countTodayPosOrders();

    /**
     * 统计今日销售额（canonical orders 表，仅已支付 payment_status=2）
     * POS 端订单状态：0待支付 1已支付 2待配送 3配送中 4已完成 5已取消 6退款中 7已退款
     * 已支付订单包括：1, 2, 3, 4
     * actual_amount 单位为元（BigDecimal），需乘以 100 转换为分
     */
    @Select("SELECT COALESCE(SUM(final_amount), 0) FROM orders " +
            "WHERE payment_status = 2 AND CAST(create_time AS DATE) = CURRENT_DATE AND deleted = 0")
    long sumTodayPosSales();

    /**
     * 统计今日已完成订单数（canonical：order_status = 2）
     */
    @Select("SELECT COUNT(*) FROM orders WHERE order_status = 2 AND CAST(create_time AS DATE) = CURRENT_DATE AND deleted = 0")
    long countTodayPosCompletedOrders();

    /**
     * 统计今日已取消订单数（canonical：order_status = 3）
     */
    @Select("SELECT COUNT(*) FROM orders WHERE order_status = 3 AND CAST(create_time AS DATE) = CURRENT_DATE AND deleted = 0")
    long countTodayPosCancelledOrders();

    /**
     * 统计 POS 端订单总数（全部）
     */
    @Select("SELECT COUNT(*) FROM orders WHERE deleted = 0")
    long countAllPosOrders();

    /**
     * 统计已完成订单总数（canonical：order_status = 2）
     */
    @Select("SELECT COUNT(*) FROM orders WHERE order_status = 2 AND deleted = 0")
    long countAllPosCompletedOrders();

    /**
     * 统计销售额总数（仅已支付订单，单位：分）
     */
    @Select("SELECT COALESCE(SUM(final_amount), 0) FROM orders " +
            "WHERE payment_status = 2 AND deleted = 0")
    long sumAllPosSales();

    /**
     * 按日期统计 POS 端订单数
     */
    @Select("SELECT COUNT(*) FROM orders WHERE CAST(create_time AS DATE) = #{date} AND deleted = 0")
    long countPosOrdersByDate(@Param("date") java.time.LocalDate date);

    /**
     * 按日期统计 POS 端销售额（仅已支付订单，单位：分）
     */
    @Select("SELECT COALESCE(SUM(final_amount), 0) FROM orders " +
            "WHERE payment_status = 2 AND CAST(create_time AS DATE) = #{date} AND deleted = 0")
    long sumPosSalesByDate(@Param("date") java.time.LocalDate date);

    /**
     * 按日期范围统计 POS 端每日趋势（聚合 orders_legacy 表）
     * 已支付订单状态：1, 2, 3, 4
     * actual_amount 单位为元（BigDecimal），需乘以 100 转换为分
     * @param startDate 开始日期（含）
     * @param endDate 结束日期（含）
     * @return 每日趋势列表，包含 date/order_count/revenue 字段
     */
    /**
     * 统计「已完成但未支付」的订单数（canonical：order_status = 2 且 payment_status 非 2）。
     *
     * <p>业务含义：后厨已出餐、顾客尚未结账，属应收挂账来源。
     * 日结对账必须单列，否则会出现「做完了但没营收」却无法解释的账面缺口。</p>
     */
    @Select("SELECT COUNT(*) FROM orders WHERE order_status = 2 " +
            "AND (payment_status IS NULL OR payment_status != 2) " +
            "AND store_id = #{storeId} AND create_time >= #{startTime} AND create_time < #{endTime} AND deleted = 0")
    long countCompletedUnpaid(@Param("storeId") String storeId,
                              @Param("startTime") LocalDateTime startTime,
                              @Param("endTime") LocalDateTime endTime);

    /**
     * 统计「已完成但未支付」的金额合计（分），口径同 {@link #countCompletedUnpaid}。
     */
    @Select("SELECT COALESCE(SUM(final_amount), 0) FROM orders WHERE order_status = 2 " +
            "AND (payment_status IS NULL OR payment_status != 2) " +
            "AND store_id = #{storeId} AND create_time >= #{startTime} AND create_time < #{endTime} AND deleted = 0")
    long sumCompletedUnpaidAmount(@Param("storeId") String storeId,
                                  @Param("startTime") LocalDateTime startTime,
                                  @Param("endTime") LocalDateTime endTime);

    @Select("SELECT CAST(create_time AS DATE) as date, COUNT(*) as order_count, " +
            "COALESCE(SUM(final_amount), 0) as revenue " +
            "FROM orders WHERE CAST(create_time AS DATE) BETWEEN #{startDate} AND #{endDate} " +
            "AND payment_status = 2 AND deleted = 0 " +
            "GROUP BY CAST(create_time AS DATE) ORDER BY date")
    List<Map<String, Object>> getDailyTrendForPos(@Param("startDate") java.time.LocalDate startDate,
                                                   @Param("endDate") java.time.LocalDate endDate);
}
