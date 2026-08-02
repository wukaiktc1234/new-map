package com.foodtraceability.controller;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.InventoryDeductionDTO;
import com.foodtraceability.service.InventoryDeductionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;

@RestController
@RequestMapping("/v1/inventory-deduction")
@Tag(name = "库存扣减管理", description = "库存扣减相关接口")
public class InventoryDeductionController {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(InventoryDeductionController.class);
    private final InventoryDeductionService inventoryDeductionService;

    @Operation(summary = "按追溯码扣减（模式A）")
    @PostMapping("/trace-code")
    @PreAuthorize("hasAuthority('inventory:deduct')")
    public Result<Void> deductByTraceCode(@RequestParam String traceCode, @RequestParam String kitchenOrderId, @RequestParam(required = false) String operatorId, @RequestParam(required = false) String operatorName) {
        try {
            inventoryDeductionService.deductByTraceCode(traceCode, kitchenOrderId, operatorId, operatorName);
            return Result.success();
        } catch (Exception e) {
            log.error("按追溯码扣减失败", e);
            return Result.error("按追溯码扣减失败: " + e.getMessage());
        }
    }

    @Operation(summary = "按配方扣减（模式B）")
    @PostMapping("/recipe")
    public Result<Void> deductByRecipe(@RequestParam String dishId, @RequestParam Integer quantity, @RequestParam String kitchenOrderId, @RequestParam(required = false) String foodTraceCodeId) {
        try {
            inventoryDeductionService.deductByRecipe(dishId, quantity, kitchenOrderId, foodTraceCodeId);
            return Result.success();
        } catch (Exception e) {
            log.error("按配方扣减失败", e);
            return Result.error("按配方扣减失败: " + e.getMessage());
        }
    }

    @Operation(summary = "按批次扣减（混合模式A）")
    @PostMapping("/batch-code")
    public Result<Void> deductByBatchCode(@RequestParam String materialId, @RequestParam String batchCode, @RequestParam BigDecimal quantity, @RequestParam String kitchenOrderId, @RequestParam(required = false) String operatorId, @RequestParam(required = false) String operatorName) {
        try {
            inventoryDeductionService.deductByBatchCode(materialId, batchCode, quantity, kitchenOrderId, operatorId, operatorName);
            return Result.success();
        } catch (Exception e) {
            log.error("按批次扣减失败", e);
            return Result.error("按批次扣减失败: " + e.getMessage());
        }
    }

    @Operation(summary = "按原料名称扣减（混合模式B）")
    @PostMapping("/material-name")
    public Result<Void> deductByMaterialName(@RequestParam String materialName, @RequestParam BigDecimal quantity, @RequestParam String kitchenOrderId, @RequestParam(required = false) String operatorId, @RequestParam(required = false) String operatorName) {
        try {
            inventoryDeductionService.deductByMaterialName(materialName, quantity, kitchenOrderId, operatorId, operatorName);
            return Result.success();
        } catch (Exception e) {
            log.error("按原料名称扣减失败", e);
            return Result.error("按原料名称扣减失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取扣减记录")
    @GetMapping("/deduction-record/{foodTraceCodeId}")
    public Result<InventoryDeductionDTO> getDeductionRecord(@PathVariable String foodTraceCodeId) {
        try {
            InventoryDeductionDTO deductionRecord = inventoryDeductionService.getDeductionRecord(foodTraceCodeId);
            if (deductionRecord == null) {
                return Result.error("未找到扣减记录");
            }
            return Result.success(deductionRecord);
        } catch (Exception e) {
            log.error("获取扣减记录失败", e);
            return Result.error("获取扣减记录失败: " + e.getMessage());
        }
    }

    public InventoryDeductionController(final InventoryDeductionService inventoryDeductionService) {
        this.inventoryDeductionService = inventoryDeductionService;
    }
}
