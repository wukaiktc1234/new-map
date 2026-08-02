package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.OrderMaterialRequirement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrderMaterialRequirementMapper extends BaseMapper<OrderMaterialRequirement> {

    @Select("SELECT * FROM order_material_requirement WHERE order_id = #{orderId} AND deleted = 0 ORDER BY create_time")
    List<OrderMaterialRequirement> findByOrderId(@Param("orderId") String orderId);

    @Select("SELECT * FROM order_material_requirement WHERE kitchen_order_id = #{kitchenOrderId} AND deleted = 0 ORDER BY create_time")
    List<OrderMaterialRequirement> findByKitchenOrderId(@Param("kitchenOrderId") String kitchenOrderId);

    @Select("SELECT * FROM order_material_requirement WHERE material_name = #{materialName} AND status IN ('pending', 'partial') AND deleted = 0 ORDER BY create_time ASC")
    List<OrderMaterialRequirement> findPendingByMaterialName(@Param("materialName") String materialName);
}
