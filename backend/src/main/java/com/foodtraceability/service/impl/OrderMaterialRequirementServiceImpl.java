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
    private final ComboIngredientMapper comboIngredientMapper;
    private final FoodMapper foodMapper;
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
        LambdaQueryWrapper<ComboIngredient> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ComboIngredient::getComboId, comboId);
        List<ComboIngredient> comboIngredients = comboIngredientMapper.selectList(queryWrapper);
        if (comboIngredients == null || comboIngredients.isEmpty()) {
            log.warn("套餐 {} 没有关联菜品数据", comboName);
            return allRequirements;
        }
        for (ComboIngredient comboIngredient : comboIngredients) {
            String foodId = comboIngredient.getFoodId();
            Food food = foodMapper.selectOne(new LambdaQueryWrapper<Food>().eq(Food::getFoodCode, foodId));
            if (food != null) {
                Integer dishQuantity = comboIngredient.getQuantity() != null ? comboIngredient.getQuantity().intValue() * quantity : quantity;
                List<OrderMaterialRequirement> dishRequirements = generateRequirementsForDish(foodId, food.getFoodName(), dishQuantity, orderId, kitchenOrderId, orderNumber);
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

    public OrderMaterialRequirementServiceImpl(final OrderMaterialRequirementMapper requirementMapper, final DishRecipeMapper dishRecipeMapper, final ComboIngredientMapper comboIngredientMapper, final FoodMapper foodMapper, final ObjectMapper objectMapper) {
        this.requirementMapper = requirementMapper;
        this.dishRecipeMapper = dishRecipeMapper;
        this.comboIngredientMapper = comboIngredientMapper;
        this.foodMapper = foodMapper;
        this.objectMapper = objectMapper;
    }
}
