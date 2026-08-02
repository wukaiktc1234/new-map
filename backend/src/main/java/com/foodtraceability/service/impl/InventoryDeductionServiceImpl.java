package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.foodtraceability.dto.InventoryDeductionDTO;
import com.foodtraceability.entity.*;
import com.foodtraceability.mapper.*;
import com.foodtraceability.service.InventoryDeductionService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class InventoryDeductionServiceImpl implements InventoryDeductionService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(InventoryDeductionServiceImpl.class);
    private final MaterialTraceCodeMapper materialTraceCodeMapper;
    private final DishRecipeMapper dishRecipeMapper;
    private final FoodTraceCodeMapper foodTraceCodeMapper;
    private final MaterialUsageRecordMapper materialUsageRecordMapper;
    private final ObjectMapper objectMapper;
    private final Map<String, InventoryDeductionDTO> deductionRecords = new HashMap<>();

    @Override
    @Transactional
    public void deductByTraceCode(String traceCode, String kitchenOrderId, String operatorId, String operatorName) {
        log.info("模式A扣减：追溯码={}, 后厨订单={}", traceCode, kitchenOrderId);
        MaterialTraceCode traceCodeEntity = materialTraceCodeMapper.selectOne(new LambdaQueryWrapper<MaterialTraceCode>().eq(MaterialTraceCode::getTraceCode, traceCode));
        if (traceCodeEntity == null) {
            throw new RuntimeException("追溯码不存在: " + traceCode);
        }
        if ("used".equals(traceCodeEntity.getStatus())) {
            throw new RuntimeException("该追溯码已使用完毕");
        }
        BigDecimal availableQty = traceCodeEntity.getAvailableQuantity();
        if (availableQty == null) {
            availableQty = traceCodeEntity.getQuantity();
            if (availableQty == null) {
                availableQty = traceCodeEntity.getWeight();
            }
        }
        if (availableQty == null || availableQty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("该追溯码无可用数量");
        }
        BigDecimal useQty = availableQty.min(new BigDecimal("1"));
        BigDecimal newAvailableQty = availableQty.subtract(useQty);
        traceCodeEntity.setAvailableQuantity(newAvailableQty);
        traceCodeEntity.setUsedQuantity(traceCodeEntity.getUsedQuantity() != null ? traceCodeEntity.getUsedQuantity().add(useQty) : useQty);
        if (newAvailableQty.compareTo(BigDecimal.ZERO) <= 0) {
            traceCodeEntity.setStatus("used");
            traceCodeEntity.setUseTime(LocalDateTime.now());
        }
        materialTraceCodeMapper.updateById(traceCodeEntity);
        MaterialUsageRecord usageRecord = new MaterialUsageRecord();
        usageRecord.setRecordId("UR" + System.currentTimeMillis());
        usageRecord.setTraceCodeId(traceCodeEntity.getTraceCodeId());
        usageRecord.setTraceCode(traceCode);
        usageRecord.setMaterialId(traceCodeEntity.getMaterialId());
        usageRecord.setMaterialName(traceCodeEntity.getMaterialName());
        usageRecord.setUsedQuantity(useQty);
        usageRecord.setUnit(traceCodeEntity.getUnit() != null ? traceCodeEntity.getUnit() : traceCodeEntity.getWeightUnit());
        usageRecord.setKitchenOrderId(kitchenOrderId);
        usageRecord.setOperatorId(operatorId);
        usageRecord.setOperatorName(operatorName);
        usageRecord.setUsageTime(LocalDateTime.now());
        materialUsageRecordMapper.insert(usageRecord);
        addModeADeduction(kitchenOrderId, traceCodeEntity.getMaterialId(), traceCodeEntity.getMaterialName(), traceCode, useQty, traceCodeEntity.getUnit() != null ? traceCodeEntity.getUnit() : traceCodeEntity.getWeightUnit());
        log.info("模式A扣减完成：原料={}, 数量={}", traceCodeEntity.getMaterialName(), useQty);
    }

    @Override
    @Transactional
    public void deductByRecipe(String dishId, Integer quantity, String kitchenOrderId, String foodTraceCodeId) {
        log.info("模式B扣减：菜品={}, 数量={}, 后厨订单={}", dishId, quantity, kitchenOrderId);
        List<DishRecipe> recipes = dishRecipeMapper.selectList(new LambdaQueryWrapper<DishRecipe>().eq(DishRecipe::getDishId, dishId));
        if (recipes.isEmpty()) {
            log.warn("菜品{}没有配方信息", dishId);
            return;
        }
        for (DishRecipe recipe : recipes) {
            BigDecimal requiredQty = recipe.getQuantity().multiply(new BigDecimal(quantity));
            String materialName = recipe.getIngredientName();
            addModeBDeduction(kitchenOrderId, recipe.getIngredientId(), materialName, null, requiredQty, recipe.getUnit());
            log.info("模式B扣减：原料={}, 数量={}", materialName, requiredQty);
        }
        saveDeductionToFoodTraceCode(kitchenOrderId, foodTraceCodeId);
    }

    @Override
    public InventoryDeductionDTO getDeductionRecord(String foodTraceCodeId) {
        return deductionRecords.get(foodTraceCodeId);
    }

    private void addModeADeduction(String kitchenOrderId, String materialId, String materialName, String traceCode, BigDecimal quantity, String unit) {
        InventoryDeductionDTO deduction = deductionRecords.computeIfAbsent(kitchenOrderId, k -> new InventoryDeductionDTO());
        if (deduction.getModeAItems() == null) {
            deduction.setModeAItems(new ArrayList<>());
        }
        InventoryDeductionDTO.DeductionItem item = new InventoryDeductionDTO.DeductionItem();
        item.setMaterialId(materialId);
        item.setMaterialName(materialName);
        item.setTraceCode(traceCode);
        item.setQuantity(quantity);
        item.setUnit(unit);
        deduction.getModeAItems().add(item);
    }

    private void addModeBDeduction(String kitchenOrderId, String materialId, String materialName, String batchCode, BigDecimal quantity, String unit) {
        InventoryDeductionDTO deduction = deductionRecords.computeIfAbsent(kitchenOrderId, k -> new InventoryDeductionDTO());
        if (deduction.getModeBItems() == null) {
            deduction.setModeBItems(new ArrayList<>());
        }
        InventoryDeductionDTO.DeductionItem item = new InventoryDeductionDTO.DeductionItem();
        item.setMaterialId(materialId);
        item.setMaterialName(materialName);
        item.setBatchCode(batchCode);
        item.setQuantity(quantity);
        item.setUnit(unit);
        deduction.getModeBItems().add(item);
    }

    private void saveDeductionToFoodTraceCode(String kitchenOrderId, String foodTraceCodeId) {
        try {
            InventoryDeductionDTO deduction = deductionRecords.get(kitchenOrderId);
            if (deduction != null && foodTraceCodeId != null) {
                FoodTraceCode foodTraceCode = foodTraceCodeMapper.selectOne(new LambdaQueryWrapper<FoodTraceCode>().eq(FoodTraceCode::getTraceCodeId, foodTraceCodeId));
                if (foodTraceCode != null) {
                    String deductionJson = objectMapper.writeValueAsString(deduction);
                    foodTraceCode.setInventoryDeduction(deductionJson);
                    foodTraceCodeMapper.updateById(foodTraceCode);
                    deductionRecords.put(foodTraceCodeId, deduction);
                }
            }
        } catch (JsonProcessingException e) {
            log.error("序列化扣减记录失败", e);
        }
    }

    @Override
    @Transactional
    public void deductByBatchCode(String materialId, String batchCode, BigDecimal quantity, String kitchenOrderId, String operatorId, String operatorName) {
        log.info("混合模式A扣减：原料={}, 批次={}, 数量={}, 后厨订单={}", materialId, batchCode, quantity, kitchenOrderId);
        List<MaterialTraceCode> batchCodes = materialTraceCodeMapper.selectList(new LambdaQueryWrapper<MaterialTraceCode>().eq(MaterialTraceCode::getMaterialId, materialId).eq(MaterialTraceCode::getBatchNumber, batchCode).eq(MaterialTraceCode::getStatus, "available").orderByAsc(MaterialTraceCode::getExpiryDate));
        if (batchCodes.isEmpty()) {
            throw new RuntimeException("未找到可用的批次：原料=" + materialId + ", 批次=" + batchCode);
        }
        BigDecimal remainingQty = quantity;
        String materialName = null;
        String unit = null;
        for (MaterialTraceCode traceCodeEntity : batchCodes) {
            if (remainingQty.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            if (materialName == null) {
                materialName = traceCodeEntity.getMaterialName();
                unit = traceCodeEntity.getUnit() != null ? traceCodeEntity.getUnit() : traceCodeEntity.getWeightUnit();
            }
            BigDecimal availableQty = traceCodeEntity.getAvailableQuantity();
            if (availableQty == null) {
                availableQty = traceCodeEntity.getQuantity();
                if (availableQty == null) {
                    availableQty = traceCodeEntity.getWeight();
                }
            }
            if (availableQty == null || availableQty.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal useQty = availableQty.min(remainingQty);
            BigDecimal newAvailableQty = availableQty.subtract(useQty);
            traceCodeEntity.setAvailableQuantity(newAvailableQty);
            traceCodeEntity.setUsedQuantity(traceCodeEntity.getUsedQuantity() != null ? traceCodeEntity.getUsedQuantity().add(useQty) : useQty);
            if (newAvailableQty.compareTo(BigDecimal.ZERO) <= 0) {
                traceCodeEntity.setStatus("used");
                traceCodeEntity.setUseTime(LocalDateTime.now());
            }
            materialTraceCodeMapper.updateById(traceCodeEntity);
            MaterialUsageRecord usageRecord = new MaterialUsageRecord();
            usageRecord.setRecordId("UR" + System.currentTimeMillis());
            usageRecord.setTraceCodeId(traceCodeEntity.getTraceCodeId());
            usageRecord.setTraceCode(traceCodeEntity.getTraceCode());
            usageRecord.setMaterialId(traceCodeEntity.getMaterialId());
            usageRecord.setMaterialName(traceCodeEntity.getMaterialName());
            usageRecord.setUsedQuantity(useQty);
            usageRecord.setUnit(unit);
            usageRecord.setKitchenOrderId(kitchenOrderId);
            usageRecord.setOperatorId(operatorId);
            usageRecord.setOperatorName(operatorName);
            usageRecord.setUsageTime(LocalDateTime.now());
            materialUsageRecordMapper.insert(usageRecord);
            addModeADeduction(kitchenOrderId, traceCodeEntity.getMaterialId(), traceCodeEntity.getMaterialName(), traceCodeEntity.getTraceCode(), useQty, unit);
            remainingQty = remainingQty.subtract(useQty);
            log.info("批次扣减完成：追溯码={}, 扣减数量={}, 剩余数量={}", traceCodeEntity.getTraceCode(), useQty, remainingQty);
        }
        if (remainingQty.compareTo(BigDecimal.ZERO) > 0) {
            log.warn("批次库存不足，缺少数量：{}", remainingQty);
        }
    }

    @Override
    @Transactional
    public void deductByMaterialName(String materialName, BigDecimal quantity, String kitchenOrderId, String operatorId, String operatorName) {
        log.info("混合模式B扣减：原料={}, 数量={}, 后厨订单={}", materialName, quantity, kitchenOrderId);
        addModeBDeduction(kitchenOrderId, null, materialName, null, quantity, "单位");
        log.info("混合模式B扣减完成：原料={}, 数量={}", materialName, quantity);
    }

    public InventoryDeductionServiceImpl(final MaterialTraceCodeMapper materialTraceCodeMapper, final DishRecipeMapper dishRecipeMapper, final FoodTraceCodeMapper foodTraceCodeMapper, final MaterialUsageRecordMapper materialUsageRecordMapper, final ObjectMapper objectMapper) {
        this.materialTraceCodeMapper = materialTraceCodeMapper;
        this.dishRecipeMapper = dishRecipeMapper;
        this.foodTraceCodeMapper = foodTraceCodeMapper;
        this.materialUsageRecordMapper = materialUsageRecordMapper;
        this.objectMapper = objectMapper;
    }
}
