package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.common.exception.ErrorCode;
import com.foodtraceability.dto.product.*;
import com.foodtraceability.entity.DishRecipeNew;
import com.foodtraceability.entity.FoodNew;
import com.foodtraceability.mapper.DishRecipeNewMapper;
import com.foodtraceability.mapper.FoodNewMapper;
import com.foodtraceability.service.RecipeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 配方/BOM服务实现类
 * 管理菜品原料配方，支持损耗率计算和成本核算
 */
@Service
public class RecipeServiceImpl implements RecipeService {

    private static final Logger log = LoggerFactory.getLogger(RecipeServiceImpl.class);

    private final DishRecipeNewMapper recipeMapper;
    private final FoodNewMapper foodNewMapper;

    public RecipeServiceImpl(DishRecipeNewMapper recipeMapper,
                             FoodNewMapper foodNewMapper) {
        this.recipeMapper = recipeMapper;
        this.foodNewMapper = foodNewMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RecipeVO create(RecipeCreateDTO dto) {
        // 校验菜品是否存在
        FoodNew food = getExistingFood(dto.getFoodId());

        // 构建实体并保存
        DishRecipeNew recipe = new DishRecipeNew();
        recipe.setFoodId(dto.getFoodId());
        recipe.setMaterialId(dto.getMaterialId());
        recipe.setMaterialName(dto.getMaterialName());
        recipe.setSpecification(dto.getSpecification());
        recipe.setRequiredQuantity(dto.getRequiredQuantity());
        recipe.setUnit(dto.getUnit());
        recipe.setLossRate(dto.getLossRate() != null ? dto.getLossRate() : BigDecimal.ZERO);
        recipe.setUnitCost(dto.getUnitCost() != null ? dto.getUnitCost() : 0L);

        // 计算实际用量和该项成本
        BigDecimal actualQuantity = calculateActualQuantity(dto.getRequiredQuantity(), recipe.getLossRate());
        long subtotalCost = calculateSubtotalCost(recipe.getUnitCost(), actualQuantity);
        recipe.setSubtotalCost(subtotalCost);

        recipeMapper.insert(recipe);

        // 同步更新菜品的成本价
        updateFoodCostPrice(dto.getFoodId());

        log.info("添加配方成功: recipeId={}, foodId={}, materialName={}",
                recipe.getRecipeId(), dto.getFoodId(), dto.getMaterialName());

        return convertToVO(recipe);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<RecipeVO> batchCreate(Long foodId, List<RecipeCreateDTO> dtos) {
        getExistingFood(foodId);  // 校验菜品存在

        // 安全性校验：限制单次批量添加配方数量，防止恶意大批量请求
        if (dtos.size() > 100) {
            throw new IllegalArgumentException("单次批量添加配方不能超过100条记录");
        }

        List<RecipeVO> results = new java.util.ArrayList<>();
        for (RecipeCreateDTO dto : dtos) {
            dto.setFoodId(foodId);  // 强制使用传入的foodId
            results.add(create(dto));
        }

        return results;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long recipeId) {
        DishRecipeNew existing = getExistingRecipe(recipeId);
        
        Long foodId = existing.getFoodId();
        recipeMapper.deleteById(recipeId);
        
        // 重新计算菜品成本
        updateFoodCostPrice(foodId);
        
        log.info("删除配方: recipeId={}", recipeId);
    }

    @Override
    public List<RecipeVO> listByFoodId(Long foodId) {
        getExistingFood(foodId);  // 校验菜品存在

        LambdaQueryWrapper<DishRecipeNew> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DishRecipeNew::getFoodId, foodId)
               .orderByAsc(DishRecipeNew::getRecipeId);

        List<DishRecipeNew> recipes = recipeMapper.selectList(wrapper);
        long totalCost = 0;

        List<RecipeVO> result = new java.util.ArrayList<>();
        for (DishRecipeNew recipe : recipes) {
            RecipeVO vo = convertToVOWithRatio(recipe, totalCost);
            if (recipe.getSubtotalCost() != null) {
                totalCost += recipe.getSubtotalCost();
            }
            result.add(vo);
        }

        // 重新设置成本占比
        if (totalCost > 0) {
            for (RecipeVO vo : result) {
                if (vo.getSubtotalCost() != null && vo.getSubtotalCost() > 0) {
                    double ratio = Math.round((double) vo.getSubtotalCost() / totalCost * 10000) / 100.0;
                    vo.setCostRatio(ratio);
                }
            }
        }

        return result;
    }

    @Override
    public Page<RecipeVO> queryPage(RecipeQueryDTO queryDto) {
        Page<DishRecipeNew> page = new Page<>(queryDto.getPage(), queryDto.getSize());

        LambdaQueryWrapper<DishRecipeNew> wrapper = new LambdaQueryWrapper<>();

        if (queryDto.getFoodId() != null) {
            wrapper.eq(DishRecipeNew::getFoodId, queryDto.getFoodId());
        }
        if (StringUtils.hasText(queryDto.getMaterialName())) {
            wrapper.like(DishRecipeNew::getMaterialName, queryDto.getMaterialName());
        }

        // 排序
        boolean isAsc = "asc".equalsIgnoreCase(queryDto.getSortOrder());
        wrapper.orderBy(true, isAsc, DishRecipeNew::getRecipeId);

        Page<DishRecipeNew> resultPage = recipeMapper.selectPage(page, wrapper);

        // 计算总成本用于占比
        long totalCost = 0;
        for (DishRecipeNew r : resultPage.getRecords()) {
            if (r.getSubtotalCost() != null) {
                totalCost += r.getSubtotalCost();
            }
        }
        final long finalTotalCost = totalCost;

        Page<RecipeVO> voPage = new Page<>(resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal());
        voPage.setRecords(resultPage.getRecords().stream()
                .map(r -> {
                    RecipeVO vo = convertToVO(r);
                    if (finalTotalCost > 0 && r.getSubtotalCost() != null && r.getSubtotalCost() > 0) {
                        double ratio = Math.round((double) r.getSubtotalCost() / finalTotalCost * 10000) / 100.0;
                        vo.setCostRatio(ratio);
                    }
                    return vo;
                })
                .collect(Collectors.toList()));

        return voPage;
    }

    @Override
    public Long calculateFoodCost(Long foodId) {
        getExistingFood(foodId);  // 校验菜品存在

        LambdaQueryWrapper<DishRecipeNew> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DishRecipeNew::getFoodId, foodId);

        List<DishRecipeNew> recipes = recipeMapper.selectList(wrapper);
        long totalCost = 0;

        for (DishRecipeNew recipe : recipes) {
            if (recipe.getSubtotalCost() != null) {
                totalCost += recipe.getSubtotalCost();
            }
        }

        return totalCost;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMaterialCost(Long materialId, Long newUnitCost) {
        // 查找所有使用该原料的配方
        LambdaQueryWrapper<DishRecipeNew> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DishRecipeNew::getMaterialId, materialId);

        List<DishRecipeNew> recipes = recipeMapper.selectList(wrapper);
        java.util.Set<Long> affectedFoodIds = new java.util.HashSet<>();

        for (DishRecipeNew recipe : recipes) {
            recipe.setUnitCost(newUnitCost);
            
            // 重新计算实际用量和成本
            BigDecimal actualQuantity = calculateActualQuantity(recipe.getRequiredQuantity(), recipe.getLossRate());
            long subtotalCost = calculateSubtotalCost(newUnitCost, actualQuantity);
            recipe.setSubtotalCost(subtotalCost);
            
            recipeMapper.updateById(recipe);
            affectedFoodIds.add(recipe.getFoodId());
        }

        // 更新所有关联菜品的成本价
        for (Long foodId : affectedFoodIds) {
            updateFoodCostPrice(foodId);
        }

        log.info("更新原料成本: materialId={}, newUnitCost={}, affectedFoods={}",
                materialId, newUnitCost, affectedFoodIds.size());
    }

    // ==================== 私有辅助方法 ====================

    private FoodNew getExistingFood(Long foodId) {
        FoodNew food = foodNewMapper.selectById(foodId);
        if (food == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "菜品不存在: " + foodId);
        }
        return food;
    }

    private DishRecipeNew getExistingRecipe(Long recipeId) {
        DishRecipeNew recipe = recipeMapper.selectById(recipeId);
        if (recipe == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "配方记录不存在: " + recipeId);
        }
        return recipe;
    }

    /**
     * 计算实际用量（含损耗）
     * 实际用量 = 标准用量 / (1 - 损耗率/100)
     */
    private BigDecimal calculateActualQuantity(BigDecimal requiredQuantity, BigDecimal lossRate) {
        if (requiredQuantity == null) {
            return BigDecimal.ZERO;
        }
        if (lossRate == null || lossRate.compareTo(BigDecimal.ZERO) == 0) {
            return requiredQuantity;
        }
        
        // 损耗率转为小数
        BigDecimal lossDecimal = lossRate.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        BigDecimal divisor = BigDecimal.ONE.subtract(lossDecimal);
        
        if (divisor.compareTo(BigDecimal.ZERO) <= 0) {
            return requiredQuantity;  // 防止除零或负数
        }
        
        return requiredQuantity.divide(divisor, 2, RoundingMode.HALF_UP);
    }

    /**
     * 计算单项成本（分）
     * 成本 = 单价(分/单位) * 实际用量
     * 注意：单价单位需要与用量单位一致
     */
    private long calculateSubtotalCost(Long unitCost, BigDecimal actualQuantity) {
        if (unitCost == null || actualQuantity == null) {
            return 0L;
        }
        // unitCost是每单位的成本(分)，乘以数量得到总成本
        return BigDecimal.valueOf(unitCost).multiply(actualQuantity).setScale(0, RoundingMode.HALF_UP).longValue();
    }

    /**
     * 更新菜品的成本价（汇总所有配方项的成本）
     */
    private void updateFoodCostPrice(Long foodId) {
        Long totalCost = calculateFoodCost(foodId);
        
        FoodNew food = foodNewMapper.selectById(foodId);
        if (food != null) {
            food.setCostPrice(totalCost);
            foodNewMapper.updateById(food);
        }
    }

    /**
     * 将实体转换为VO（不含成本占比）
     */
    private RecipeVO convertToVO(DishRecipeNew recipe) {
        return convertToVOWithRatio(recipe, 0);
    }

    /**
     * 将实体转换为VO（含成本占比参数）
     */
    private RecipeVO convertToVOWithRatio(DishRecipeNew recipe, long totalCost) {
        if (recipe == null) {
            return null;
        }

        RecipeVO vo = new RecipeVO();
        vo.setRecipeId(recipe.getRecipeId());
        vo.setFoodId(recipe.getFoodId());
        vo.setMaterialId(recipe.getMaterialId());
        vo.setMaterialName(recipe.getMaterialName());
        vo.setSpecification(recipe.getSpecification());
        vo.setRequiredQuantity(recipe.getRequiredQuantity());
        vo.setUnit(recipe.getUnit());
        vo.setLossRate(recipe.getLossRate());
        vo.setUnitCost(recipe.getUnitCost());
        vo.setSubtotalCost(recipe.getSubtotalCost());
        vo.setCreateTime(recipe.getCreateTime());
        vo.setUpdateTime(recipe.getUpdateTime());

        // 计算实际用量
        if (recipe.getRequiredQuantity() != null && recipe.getLossRate() != null) {
            vo.setActualQuantity(calculateActualQuantity(recipe.getRequiredQuantity(), recipe.getLossRate()));
        }

        // 设置菜品名称
        if (recipe.getFoodId() != null) {
            FoodNew food = foodNewMapper.selectById(recipe.getFoodId());
            if (food != null) {
                vo.setFoodName(food.getFoodName());
            }
        }

        return vo;
    }
}
