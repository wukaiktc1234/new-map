package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.DishInventory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface DishInventoryMapper extends BaseMapper<DishInventory> {

    @Select("SELECT * FROM dish_inventory WHERE dish_id = #{dishId} AND deleted = 0")
    List<DishInventory> selectByDishId(@Param("dishId") String dishId);

    // 注意：inventory 表主键为 inventory_id，成本字段为 cost_price
    @Select("SELECT COALESCE(SUM(di.quantity * i.cost_price), 0) " +
            "FROM dish_inventory di " +
            "LEFT JOIN inventory i ON di.inventory_id = i.inventory_id " +
            "WHERE di.dish_id = #{dishId} AND di.deleted = 0")
    BigDecimal calculateDishCost(@Param("dishId") String dishId);
}
