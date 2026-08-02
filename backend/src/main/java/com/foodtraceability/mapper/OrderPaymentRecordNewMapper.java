package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.OrderPaymentRecordNew;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 支付记录Mapper接口
 */
@Mapper
public interface OrderPaymentRecordNewMapper extends BaseMapper<OrderPaymentRecordNew> {

    /**
     * 根据订单ID查询支付记录列表
     */
    @Select("SELECT * FROM order_payment_records WHERE order_id = #{orderId} AND deleted = 0 ORDER BY payment_time ASC")
    java.util.List<OrderPaymentRecordNew> selectByOrderId(@Param("orderId") String orderId);

    /**
     * 统计订单已支付总额
     */
    @Select("SELECT COALESCE(SUM(payment_amount), 0) FROM order_payment_records WHERE order_id = #{orderId} AND deleted = 0")
    long sumPaidAmountByOrderId(@Param("orderId") String orderId);
}
