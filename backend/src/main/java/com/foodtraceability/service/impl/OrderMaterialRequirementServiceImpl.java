package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.foodtraceability.entity.*;
import com.foodtraceability.mapper.*;
import com.foodtraceability.service.OrderMaterialRequirementService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;

@Service
public class OrderMaterialRequirementServiceImpl implements OrderMaterialRequirementService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(OrderMaterialRequirementServiceImpl.class);
    private final OrderMaterialRequirementMapper requirementMapper;
    private final DishRecipeMapper dishRecipeMapper;
    private final ComboIngredientNewMapper comboIngredientNewMapper;
    private final FoodMapper foodMapper;
    private final FoodNewMapper foodNewMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public List<OrderMaterialRequirement> generateRequirementsForOrder(String orderId, String kitchenOrderId, String orderNumber) {
        log.info("为订单生成原料需求: orderId={}, kitchenOrderId={}", orderId, kitchenOrderId);
        List<OrderMaterialRequirement> allRequirements = new ArrayList<>();
        return allRequirements;
    }

    @Override
    @Transactional
    public List<OrderMaterialRequirement> generateRequirementsForDish(String dishId, String dishName, Integer quantity, String orderId, String kitchenOrderId, String orderNumber) {
        log.info("为菜品生成原料需求: dishId={}, dishName={}, quantity={}", dishId, dishName, quantity);
        List<OrderMaterialRequirement> requirements = new ArrayList<>();
        LambdaQueryWrapper<DishRecipe> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DishRecipe::getDishId, dishId);
        List<DishRecipe> recipes = dishRecipeMapper.selectList(queryWrapper);
        if (recipes == null || recipes.isEmpty()) {
            log.warn("菜品 {} 没有配方数据", dishName);
            return requirements;
        }
        for (DishRecipe recipe : recipes) {
            OrderMaterialRequirement requirement = new OrderMaterialRequirement();
            requirement.setRequirementId("REQ" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4));
            requirement.setOrderId(orderId);
            requirement.setKitchenOrderId(kitchenOrderId);
            requirement.setOrderNumber(orderNumber);
            requirement.setDishId(dishId);
            requirement.setDishName(dishName);
            requirement.setDishQuantity(quantity);
            requirement.setIsCombo(0);
            requirement.setMaterialId(recipe.getIngredientId());
            requirement.setMaterialName(recipe.getIngredientName());
            requirement.setRequiredQuantity(recipe.getQuantity().multiply(BigDecimal.valueOf(quantity)));
            requirement.setUnit(recipe.getUnit());
            requirement.setLockedQuantity(BigDecimal.ZERO);
            requirement.setUsedQuantity(BigDecimal.ZERO);
            requirement.setStatus("pending");
            requirementMapper.insert(requirement);
            requirements.add(requirement);
        }
        return requirements;
    }

    @Override
    @Transactional
    public List<OrderMaterialRequirement> generateRequirementsForCombo(String comboId, String comboName, Integer quantity, String orderId, String kitchenOrderId, String orderNumber) {
        log.info("为套餐生成原料需求: comboId={}, comboName={}, quantity={}", comboId, comboName, quantity);
        List<OrderMaterialRequirement> allRequirements = new ArrayList<>();
        // P1-COMBO-LEGACY-CLEANUP-001: 读新表 combo_ingredients（替代 legacy combo_ingredient）
        LambdaQueryWrapper<ComboIngredientNew> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ComboIngredientNew::getComboId, Long.valueOf(comboId));
        List<ComboIngredientNew> comboIngredients = comboIngredientNewMapper.selectList(queryWrapper);
        if (comboIngredients == null || comboIngredients.isEmpty()) {
            log.warn("套餐 {} 没有关联菜品数据", comboName);
            return allRequirements;
        }
        for (ComboIngredientNew comboIngredient : comboIngredients) {
            FoodNew foodNew = comboIngredient.getFoodId() != null ? foodNewMapper.selectById(comboIngredient.getFoodId()) : null;
            if (foodNew != null) {
                String foodCode = foodNew.getFoodCode();
                Integer dishQuantity = comboIngredient.getQuantity() != null ? comboIngredient.getQuantity() * quantity : quantity;
                List<OrderMaterialRequirement> dishRequirements = generateRequirementsForDish(foodCode, foodNew.getFoodName(), dishQuantity, orderId, kitchenOrderId, orderNumber);
                for (OrderMaterialRequirement req : dishRequirements) {
                    req.setIsCombo(1);
                    req.setComboId(comboId);
                    requirementMapper.updateById(req);
                }
                allRequirements.addAll(dishRequirements);
            }
        }
        return allRequirements;
    }

    @Override
    public List<OrderMaterialRequirement> getPendingRequirementsByMaterial(String materialName) {
        return requirementMapper.findPendingByMaterialName(materialName);
    }

    @Override
    public List<OrderMaterialRequirement> getRequirementsByKitchenOrder(String kitchenOrderId) {
        return requirementMapper.findByKitchenOrderId(kitchenOrderId);
    }

    public OrderMaterialRequirementServiceImpl(final OrderMaterialRequirementMapper requirementMapper, final DishRecipeMapper dishRecipeMapper, final ComboIngredientNewMapper comboIngredientNewMapper, final FoodMapper foodMapper, final FoodNewMapper foodNewMapper, final ObjectMapper objectMapper) {
        this.requirementMapper = requirementMapper;
        this.dishRecipeMapper = dishRecipeMapper;
        this.comboIngredientNewMapper = comboIngredientNewMapper;
        this.foodMapper = foodMapper;
        this.foodNewMapper = foodNewMapper;
        this.objectMapper = objectMapper;
    }
}
