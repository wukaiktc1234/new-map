package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.entity.OrderNew;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 订单Mapper接口
 * 系统最核心的数据访问层
 */
@Mapper
public interface OrderNewMapper extends BaseMapper<OrderNew> {

    /**
     * 根据订单编号查询
     */
    @Select("SELECT * FROM orders WHERE order_code = #{orderCode} AND deleted = 0")
    OrderNew selectByOrderCode(@Param("orderCode") String orderCode);

    /**
     * 更新订单状态
     */
    @Update("UPDATE orders SET order_status = #{status}, update_time = NOW() WHERE order_id = #{orderId} AND deleted = 0")
    int updateStatus(@Param("orderId") String orderId, @Param("status") Integer status);

    /**
     * 更新支付状态
     */
    @Update("UPDATE orders SET payment_status = #{paymentStatus}, paid_amount = #{paidAmount}, update_time = NOW() WHERE order_id = #{orderId} AND deleted = 0")
    int updatePaymentStatus(@Param("orderId") String orderId, @Param("paymentStatus") Integer paymentStatus, @Param("paidAmount") Long paidAmount);

    /**
     * 更新退款金额
     */
    @Update("UPDATE orders SET refund_amount = COALESCE(refund_amount, 0) + #{refundAmount}, order_status = #{orderStatus}, update_time = NOW() WHERE order_id = #{orderId} AND deleted = 0")
    int addRefundAmount(@Param("orderId") String orderId, @Param("refundAmount") Long refundAmount, @Param("orderStatus") Integer orderStatus);

    /**
     * 获取当日最大订单序号
     * order_code 格式: ORD + yyyyMMddHHmmss(14位) + 4位序号，序号在最后4位
     * 使用 RIGHT(string, n) 函数提取最后4位序号，兼容 PostgreSQL 和 H2(PostgreSQL模式)
     */
    @Select("SELECT COALESCE(MAX(CAST(RIGHT(order_code, 4) AS INTEGER)), 0) FROM orders WHERE order_code LIKE CONCAT(#{prefix}, '%') AND CAST(create_time AS DATE) = CURRENT_DATE AND deleted = 0")
    int getMaxTodaySequence(@Param("prefix") String prefix);

    /**
     * 统计今日订单数
     */
    @Select("SELECT COUNT(*) FROM orders WHERE CAST(create_time AS DATE) = CURRENT_DATE AND deleted = 0")
    long countTodayOrders();

    /**
     * 统计今日销售额
     */
    @Select("SELECT COALESCE(SUM(final_amount), 0) FROM orders WHERE payment_status = 2 AND CAST(create_time AS DATE) = CURRENT_DATE AND deleted = 0")
    long sumTodaySales();

    /**
     * 按门店统计今日订单数
     * @param storeId 门店ID（null 表示全部）
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM orders WHERE CAST(create_time AS DATE) = CURRENT_DATE AND deleted = 0" +
            "<if test='storeId != null'> AND store_id = #{storeId}</if>" +
            "</script>")
    long countTodayOrdersByStore(@Param("storeId") Long storeId);

    /**
     * 按门店统计今日销售额（仅已支付订单）
     * @param storeId 门店ID（null 表示全部）
     */
    @Select("<script>" +
            "SELECT COALESCE(SUM(final_amount), 0) FROM orders WHERE payment_status = 2" +
            " AND CAST(create_time AS DATE) = CURRENT_DATE AND deleted = 0" +
            "<if test='storeId != null'> AND store_id = #{storeId}</if>" +
            "</script>")
    long sumTodaySalesByStore(@Param("storeId") Long storeId);

    /**
     * 按门店统计今日已完成订单数（order_status = 2 已完成）
     * 用于计算翻台率
     * @param storeId 门店ID（null 表示全部）
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM orders WHERE order_status = 2" +
            " AND CAST(create_time AS DATE) = CURRENT_DATE AND deleted = 0" +
            "<if test='storeId != null'> AND store_id = #{storeId}</if>" +
            "</script>")
    long countCompletedOrdersTodayByStore(@Param("storeId") Long storeId);

    /**
     * 按门店统计今日堂食订单数
     * @param storeId 门店ID（null 表示全部）
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM orders WHERE order_type = 1" +
            " AND CAST(create_time AS DATE) = CURRENT_DATE AND deleted = 0" +
            "<if test='storeId != null'> AND store_id = #{storeId}</if>" +
            "</script>")
    long countDineInOrdersTodayByStore(@Param("storeId") Long storeId);

    /**
     * 统计各订单类型数量
     */
    @Select("SELECT order_type, COUNT(*) as cnt FROM orders WHERE CAST(create_time AS DATE) = CURRENT_DATE AND deleted = 0 GROUP BY order_type")
    java.util.List<java.util.Map<String, Object>> countByOrderTypeToday();

    /**
     * 锁定桌台（关联订单）
     */
    @Update("UPDATE dining_tables SET status = 2, current_order_id = #{orderId}, update_time = NOW() WHERE table_id = #{tableId} AND status = 1 AND deleted = 0")
    int lockTable(@Param("tableId") Long tableId, @Param("orderId") String orderId);

    /**
     * 解锁桌台
     */
    @Update("UPDATE dining_tables SET status = 1, current_order_id = NULL, update_time = NOW() WHERE current_order_id = #{orderId} AND deleted = 0")
    int unlockTableByOrderId(@Param("orderId") String orderId);

    // ==================== 运营监控扩展方法（按日期查询，支持历史日期） ====================

    /**
     * 按门店和日期统计订单数
     * @param storeId 门店ID
     * @param date 查询日期
     * @return 订单数
     */
    @Select("SELECT COUNT(*) FROM orders WHERE store_id = #{storeId} AND CAST(create_time AS DATE) = #{date} AND deleted = 0")
    int countOrdersByStoreAndDate(@Param("storeId") Long storeId, @Param("date") LocalDate date);

    /**
     * 按门店和日期统计销售额（分，仅已完成/进行中订单）
     * @param storeId 门店ID
     * @param date 查询日期
     * @return 销售额（分）
     */
    @Select("SELECT COALESCE(SUM(final_amount), 0) FROM orders WHERE store_id = #{storeId} AND CAST(create_time AS DATE) = #{date} AND order_status IN (1,2) AND deleted = 0")
    long sumSalesByStoreAndDate(@Param("storeId") Long storeId, @Param("date") LocalDate date);

    /**
     * 按门店和日期统计堂食订单数
     * @param storeId 门店ID
     * @param date 查询日期
     * @return 堂食订单数
     */
    @Select("SELECT COUNT(*) FROM orders WHERE store_id = #{storeId} AND CAST(create_time AS DATE) = #{date} AND order_type = 1 AND deleted = 0")
    int countDineInOrdersByStoreAndDate(@Param("storeId") Long storeId, @Param("date") LocalDate date);

    /**
     * 按日期统计所有门店订单数
     * @param date 查询日期
     * @return 订单数
     */
    @Select("SELECT COUNT(*) FROM orders WHERE CAST(create_time AS DATE) = #{date} AND deleted = 0")
    int countOrdersByDate(@Param("date") LocalDate date);

    /**
     * 按日期统计所有门店销售额（分）
     * @param date 查询日期
     * @return 销售额（分）
     */
    @Select("SELECT COALESCE(SUM(final_amount), 0) FROM orders WHERE CAST(create_time AS DATE) = #{date} AND order_status IN (1,2) AND deleted = 0")
    long sumSalesByDate(@Param("date") LocalDate date);

    /**
     * 按日期范围统计每日趋势（用于 LiveMonitor.trend 和 DecisionBoard.revenue-trend）
     * @param startTime 开始时间（含）
     * @param endTime 结束时间（不含）
     * @return 每日趋势列表，包含 date/order_count/revenue 字段
     */
    @Select("SELECT CAST(create_time AS DATE) as date, COUNT(*) as order_count, COALESCE(SUM(final_amount), 0) as revenue " +
            "FROM orders WHERE create_time >= #{startTime} AND create_time < #{endTime} " +
            "AND order_status IN (1,2) AND deleted = 0 " +
            "GROUP BY CAST(create_time AS DATE) ORDER BY date")
    List<Map<String, Object>> getDailyTrend(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 按日期范围统计每日趋势（支持门店过滤，用于运营中心数据权限隔离）
     * @param startTime 开始时间（含）
     * @param endTime 结束时间（不含）
     * @param storeId 门店ID（null 表示全部门店聚合）
     * @return 每日趋势列表，包含 date/order_count/revenue 字段
     */
    @Select("<script>" +
            "SELECT CAST(create_time AS DATE) as date, COUNT(*) as order_count, COALESCE(SUM(final_amount), 0) as revenue " +
            "FROM orders WHERE create_time &gt;= #{startTime} AND create_time &lt; #{endTime} " +
            "AND order_status IN (1,2) AND deleted = 0 " +
            "<if test='storeId != null'> AND store_id = #{storeId} </if>" +
            "GROUP BY CAST(create_time AS DATE) ORDER BY date" +
            "</script>")
    List<Map<String, Object>> getDailyTrendByStore(@Param("startTime") LocalDateTime startTime,
                                                     @Param("endTime") LocalDateTime endTime,
                                                     @Param("storeId") Long storeId);

    /**
     * 按门店统计今日营收排名
     * @return 门店排名列表，包含 store_id/store_name/today_revenue/order_count 字段
     */
    @Select("SELECT s.store_id as store_id, s.store_name as store_name, " +
            "COALESCE(SUM(o.final_amount), 0) as today_revenue, COUNT(o.order_id) as order_count " +
            "FROM stores_new s LEFT JOIN orders o " +
            "ON s.store_id = o.store_id AND CAST(o.create_time AS DATE) = CURRENT_DATE " +
            "AND o.order_status IN (1,2) AND o.deleted = 0 " +
            "WHERE s.status = 1 AND s.deleted = 0 " +
            "GROUP BY s.store_id, s.store_name ORDER BY today_revenue DESC")
    List<Map<String, Object>> getStoreRankingToday();

    /**
     * 统计所有订单总数（不限时间）
     * @return 订单总数
     */
    @Select("SELECT COUNT(*) FROM orders WHERE deleted = 0")
    long countAllOrders();

    /**
     * 统计所有已完成订单数（order_status = 2）
     * @return 已完成订单数
     */
    @Select("SELECT COUNT(*) FROM orders WHERE order_status = 2 AND deleted = 0")
    long countAllCompletedOrders();

    /**
     * 统计所有订单销售额（仅已支付订单，payment_status = 2）
     * @return 销售额（分）
     */
    @Select("SELECT COALESCE(SUM(final_amount), 0) FROM orders WHERE payment_status = 2 AND deleted = 0")
    long sumAllSales();

    /**
     * 按订单类型统计全部订单数量分布
     * @return 每种订单类型的数量，包含 order_type/cnt 字段
     */
    @Select("SELECT order_type, COUNT(*) as cnt FROM orders WHERE deleted = 0 GROUP BY order_type")
    List<Map<String, Object>> countByOrderType();

    /**
     * 统计去重顾客数（customer_id 非空）
     * @return 去重顾客数
     */
    @Select("SELECT COUNT(DISTINCT customer_id) FROM orders WHERE customer_id IS NOT NULL AND deleted = 0")
    long countDistinctCustomers();
}
