package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.entity.DishRecipe;
import com.foodtraceability.mapper.DishRecipeMapper;
import com.foodtraceability.service.DishRecipeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * 菜品配方服务实现类
 */
@Service
public class DishRecipeServiceImpl extends ServiceImpl<DishRecipeMapper, DishRecipe> implements DishRecipeService {


    public DishRecipeServiceImpl(DishRecipeMapper dishRecipeMapper) {
        this.dishRecipeMapper = dishRecipeMapper;
    }

    private final DishRecipeMapper dishRecipeMapper;

    private static final String PREFIX = "DR";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    public List<DishRecipe> getByDishId(String dishId) {
        return dishRecipeMapper.selectByDishId(dishId);
    }

    @Override
    public List<DishRecipe> getByIngredientId(String ingredientId) {
        return dishRecipeMapper.selectByIngredientId(ingredientId);
    }

    @Override
    @Transactional
    public boolean saveOrUpdateRecipe(DishRecipe recipe) {
        if (recipe.getRecipeId() == null || recipe.getRecipeId().isEmpty()) {
            String dateStr = LocalDateTime.now().format(DATE_FORMAT);
            recipe.setRecipeId(PREFIX + dateStr + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }
        
        if (recipe.getCreateTime() == null) {
            recipe.setCreateTime(new java.util.Date());
        }
        recipe.setUpdateTime(new java.util.Date());
        
        return saveOrUpdate(recipe);
    }

    @Override
    @Transactional
    public boolean batchSaveRecipes(String dishId, List<DishRecipe> recipes) {
        deleteByDishId(dishId);
        
        int sortOrder = 0;
        for (DishRecipe recipe : recipes) {
            recipe.setDishId(dishId);
            recipe.setSortOrder(sortOrder++);
            saveOrUpdateRecipe(recipe);
        }
        
        return true;
    }

    @Override
    public BigDecimal calculateEstimatedCost(String dishId) {
        Double cost = dishRecipeMapper.sumEstimatedCostByDishId(dishId);
        return cost != null ? new BigDecimal(cost.toString()) : BigDecimal.ZERO;
    }

    @Override
    @Transactional
    public boolean updateActualCost(String dishId, String ingredientId, BigDecimal actualCost) {
        DishRecipe recipe = dishRecipeMapper.selectByDishAndIngredient(dishId, ingredientId);
        if (recipe == null) {
            return false;
        }
        
        recipe.setActualCost(actualCost);
        recipe.setUpdateTime(new java.util.Date());
        
        return updateById(recipe);
    }

    @Override
    @Transactional
    public boolean deleteByDishId(String dishId) {
        List<DishRecipe> recipes = getByDishId(dishId);
        for (DishRecipe recipe : recipes) {
            removeById(recipe.getId());
        }
        return true;
    }
}
