package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.OrderRefundRecordNew;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 退款记录Mapper接口
 */
@Mapper
public interface OrderRefundRecordNewMapper extends BaseMapper<OrderRefundRecordNew> {

    /**
     * 根据订单ID查询退款记录列表
     */
    @Select("SELECT * FROM order_refund_records WHERE order_id = #{orderId} AND deleted = 0 ORDER BY create_time ASC")
    java.util.List<OrderRefundRecordNew> selectByOrderId(@Param("orderId") String orderId);

    /**
     * 统计订单已退款总额
     */
    @Select("SELECT COALESCE(SUM(refund_amount), 0) FROM order_refund_records WHERE order_id = #{orderId} AND refund_status = 3 AND deleted = 0")
    long sumRefundedAmountByOrderId(@Param("orderId") String orderId);

    /**
     * 统计待处理退款单数（refund_status = 0 待审核）
     * @return 待处理退款单数
     */
    @Select("SELECT COUNT(*) FROM order_refund_records WHERE refund_status = 0 AND deleted = 0")
    long countPendingRefunds();

    /**
     * 统计已退款总金额（refund_status = 3 已退款）
     * @return 已退款总金额（分）
     */
    @Select("SELECT COALESCE(SUM(refund_amount), 0) FROM order_refund_records WHERE refund_status = 3 AND deleted = 0")
    long sumRefundedAmount();

    /**
     * 统计退款单总数（不限状态，用于计算退款率）
     * @return 退款单总数
     */
    @Select("SELECT COUNT(*) FROM order_refund_records WHERE deleted = 0")
    long countAllRefunds();

    /**
     * 统计已退款单的平均处理时长（小时）
     * 通过 complete_time - create_time 计算，仅对已退款（refund_status = 3）的记录统计
     * 使用 EXTRACT(EPOCH FROM interval) / 3600 将时间差转换为小时，兼容 PostgreSQL 和 H2(PostgreSQL 模式)
     * @return 平均处理时长（小时）；无数据返回 0
     */
    @Select("SELECT COALESCE(AVG(EXTRACT(EPOCH FROM (complete_time - create_time))) / 3600, 0) " +
            "FROM order_refund_records WHERE refund_status = 3 AND complete_time IS NOT NULL AND deleted = 0")
    double avgRefundProcessHours();
}
