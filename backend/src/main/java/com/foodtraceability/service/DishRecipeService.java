package com.foodtraceability.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.entity.DishRecipe;

import java.math.BigDecimal;
import java.util.List;

/**
 * 菜品配方服务接口
 */
public interface DishRecipeService extends IService<DishRecipe> {

    /**
     * 根据菜品ID获取配方
     */
    List<DishRecipe> getByDishId(String dishId);

    /**
     * 根据原料ID获取使用该原料的菜品
     */
    List<DishRecipe> getByIngredientId(String ingredientId);

    /**
     * 保存或更新配方
     */
    boolean saveOrUpdateRecipe(DishRecipe recipe);

    /**
     * 批量保存配方
     */
    boolean batchSaveRecipes(String dishId, List<DishRecipe> recipes);

    /**
     * 计算菜品预估成本
     */
    BigDecimal calculateEstimatedCost(String dishId);

    /**
     * 更新实际成本
     */
    boolean updateActualCost(String dishId, String ingredientId, BigDecimal actualCost);

    /**
     * 删除菜品配方
     */
    boolean deleteByDishId(String dishId);
}
