package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.FoodTraceCode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 食品追溯码Mapper接口
 */
@Mapper
public interface FoodTraceCodeMapper extends BaseMapper<FoodTraceCode> {

    @Select("SELECT * FROM food_trace_code WHERE trace_code = #{traceCode} AND deleted = 0")
    FoodTraceCode selectByTraceCode(@Param("traceCode") String traceCode);

    @Select("SELECT * FROM food_trace_code WHERE order_id = #{orderId} AND deleted = 0 ORDER BY create_time DESC")
    List<FoodTraceCode> selectByOrderId(@Param("orderId") String orderId);

    @Select("SELECT * FROM food_trace_code WHERE kitchen_order_id = #{kitchenOrderId} AND deleted = 0")
    List<FoodTraceCode> selectByKitchenOrderId(@Param("kitchenOrderId") Long kitchenOrderId);

    @Select("SELECT * FROM food_trace_code WHERE dish_id = #{dishId} AND deleted = 0 ORDER BY create_time DESC")
    List<FoodTraceCode> selectByDishId(@Param("dishId") String dishId);

    @Select("SELECT * FROM food_trace_code WHERE status = #{status} AND deleted = 0 ORDER BY create_time DESC")
    List<FoodTraceCode> selectByStatus(@Param("status") String status);

    @Select("SELECT * FROM food_trace_code WHERE make_status = #{makeStatus} AND deleted = 0 ORDER BY create_time DESC")
    List<FoodTraceCode> selectByMakeStatus(@Param("makeStatus") String makeStatus);

    @Select("SELECT COUNT(*) FROM food_trace_code WHERE status = #{status} AND deleted = 0")
    int countByStatus(@Param("status") String status);
}
