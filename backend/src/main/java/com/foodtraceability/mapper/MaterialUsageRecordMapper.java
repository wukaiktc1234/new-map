package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.MaterialUsageRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MaterialUsageRecordMapper extends BaseMapper<MaterialUsageRecord> {

    @Select("SELECT * FROM material_usage_record WHERE trace_code_id = #{traceCodeId} AND deleted = 0 ORDER BY usage_time DESC")
    List<MaterialUsageRecord> findByTraceCodeId(@Param("traceCodeId") String traceCodeId);

    @Select("SELECT * FROM material_usage_record WHERE order_id = #{orderId} AND deleted = 0 ORDER BY usage_time DESC")
    List<MaterialUsageRecord> findByOrderId(@Param("orderId") String orderId);

    @Select("SELECT * FROM material_usage_record WHERE kitchen_order_id = #{kitchenOrderId} AND deleted = 0 ORDER BY usage_time DESC")
    List<MaterialUsageRecord> findByKitchenOrderId(@Param("kitchenOrderId") String kitchenOrderId);
}
