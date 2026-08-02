package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.MaterialConsumption;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 原料消耗记录Mapper接口
 */
@Mapper
public interface MaterialConsumptionMapper extends BaseMapper<MaterialConsumption> {

    @Select("SELECT * FROM material_consumption WHERE kitchen_order_id = #{kitchenOrderId} AND deleted = 0")
    List<MaterialConsumption> selectByKitchenOrderId(@Param("kitchenOrderId") String kitchenOrderId);

    @Select("SELECT * FROM material_consumption WHERE order_id = #{orderId} AND deleted = 0")
    List<MaterialConsumption> selectByOrderId(@Param("orderId") String orderId);

    @Select("SELECT * FROM material_consumption WHERE material_trace_code = #{traceCode} AND deleted = 0")
    List<MaterialConsumption> selectByTraceCode(@Param("traceCode") String traceCode);

    @Select("SELECT * FROM material_consumption WHERE product_id = #{productId} AND deleted = 0 ORDER BY consume_time DESC")
    List<MaterialConsumption> selectByProductId(@Param("productId") Long productId);

    @Select("SELECT SUM(total_cost) FROM material_consumption WHERE order_id = #{orderId} AND deleted = 0")
    BigDecimal sumCostByOrderId(@Param("orderId") String orderId);

    @Select("SELECT SUM(total_cost) FROM material_consumption WHERE store_id = #{storeId} AND consume_time BETWEEN #{startTime} AND #{endTime} AND deleted = 0")
    BigDecimal sumCostByStoreAndTime(@Param("storeId") Long storeId, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    @Select("SELECT * FROM material_consumption WHERE inventory_deducted = 0 AND deleted = 0")
    List<MaterialConsumption> selectUndeducted();
}
