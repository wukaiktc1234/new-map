package com.foodtraceability.service;

import com.foodtraceability.dto.InventoryDeductionDTO;

import java.math.BigDecimal;

public interface InventoryDeductionService {

    void deductByTraceCode(String traceCode, String kitchenOrderId, String operatorId, String operatorName);

    void deductByRecipe(String dishId, Integer quantity, String kitchenOrderId, String foodTraceCodeId);

    InventoryDeductionDTO getDeductionRecord(String foodTraceCodeId);

    void deductByBatchCode(String materialId, String batchCode, BigDecimal quantity, String kitchenOrderId, String operatorId, String operatorName);

    void deductByMaterialName(String materialName, BigDecimal quantity, String kitchenOrderId, String operatorId, String operatorName);
}
