package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.DishRecipe;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 菜品配方Mapper接口
 */
@Mapper
public interface DishRecipeMapper extends BaseMapper<DishRecipe> {

    @Select("SELECT * FROM dish_recipe WHERE dish_id = #{dishId} AND deleted = 0 ORDER BY sort_order ASC")
    List<DishRecipe> selectByDishId(@Param("dishId") String dishId);

    @Select("SELECT * FROM dish_recipe WHERE ingredient_id = #{ingredientId} AND deleted = 0")
    List<DishRecipe> selectByIngredientId(@Param("ingredientId") String ingredientId);

    @Select("SELECT * FROM dish_recipe WHERE dish_id = #{dishId} AND ingredient_id = #{ingredientId} AND deleted = 0")
    DishRecipe selectByDishAndIngredient(@Param("dishId") String dishId, @Param("ingredientId") String ingredientId);

    @Select("SELECT SUM(estimated_cost) FROM dish_recipe WHERE dish_id = #{dishId} AND deleted = 0")
    Double sumEstimatedCostByDishId(@Param("dishId") String dishId);
}
