package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.DishIngredient;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 菜品-配料关联Mapper接口
 * 
 * @author demo
 * @since 2025-12-29
 */
@Mapper
public interface DishIngredientMapper extends BaseMapper<DishIngredient> {

    /**
     * 根据菜品ID查找配料列表
     * 
     * @param dishId 菜品ID
     * @return 配料关联列表
     */
    @Select("SELECT * FROM dish_ingredient WHERE dish_id = #{dishId} AND deleted = 0 ORDER BY create_time DESC")
    List<DishIngredient> selectByDishId(@Param("dishId") String dishId);

    /**
     * 根据配料ID查找关联的菜品列表
     * 
     * @param ingredientId 配料ID
     * @return 菜品关联列表
     */
    @Select("SELECT * FROM dish_ingredient WHERE ingredient_id = #{ingredientId} AND deleted = 0 ORDER BY create_time DESC")
    List<DishIngredient> selectByIngredientId(@Param("ingredientId") String ingredientId);

    /**
     * 计算菜品的总成本价
     * 
     * @param dishId 菜品ID
     * @return 总成本价
     */
    @Select("SELECT COALESCE(SUM(total_cost), 0) FROM dish_ingredient WHERE dish_id = #{dishId} AND deleted = 0")
    java.math.BigDecimal calculateTotalCostByDishId(@Param("dishId") String dishId);

    /**
     * 删除菜品的所有配料关联
     * 
     * @param dishId 菜品ID
     * @return 删除的记录数
     */
    @Select("UPDATE dish_ingredient SET deleted = 1 WHERE dish_id = #{dishId}")
    int deleteByDishId(@Param("dishId") String dishId);

    /**
     * 检查菜品是否已经关联了某个配料
     * 
     * @param dishId 菜品ID
     * @param ingredientId 配料ID
     * @return 是否存在
     */
    @Select("SELECT COUNT(*) > 0 FROM dish_ingredient WHERE dish_id = #{dishId} AND ingredient_id = #{ingredientId} AND deleted = 0")
    boolean existsByDishIdAndIngredientId(@Param("dishId") String dishId, @Param("ingredientId") String ingredientId);
}