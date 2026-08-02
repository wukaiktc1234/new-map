package com.foodtraceability.controller;

import com.foodtraceability.common.Result;
import com.foodtraceability.entity.ComboInventory;
import com.foodtraceability.service.ComboInventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/v1/combo-inventories")
@Tag(name = "套餐库存管理")
public class ComboInventoryController {


    public ComboInventoryController(ComboInventoryService comboInventoryService) {
        this.comboInventoryService = comboInventoryService;
    }

    private final ComboInventoryService comboInventoryService;

    @GetMapping("/combo/{comboId}")
    @Operation(summary = "获取套餐的库存产品列表")
    public Result<List<ComboInventory>> getComboInventories(
            @Parameter(description = "套餐ID") @PathVariable Long comboId) {
        List<ComboInventory> inventories = comboInventoryService.getComboInventories(comboId);
        return Result.success(inventories, "获取套餐库存产品列表成功");
    }

    @PostMapping
    @Operation(summary = "添加套餐库存产品关联")
    public Result<ComboInventory> addComboInventory(
            @Parameter(description = "套餐ID") @RequestParam Long comboId,
            @Parameter(description = "库存产品ID") @RequestParam Long inventoryId,
            @Parameter(description = "使用数量") @RequestParam BigDecimal quantity,
            @Parameter(description = "计量单位") @RequestParam(required = false) String unit,
            @Parameter(description = "备注") @RequestParam(required = false) String remark) {
        ComboInventory comboInventory = comboInventoryService.addComboInventory(comboId, inventoryId, quantity, unit, remark);
        return Result.success(comboInventory, "添加套餐库存产品关联成功");
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新套餐库存产品关联")
    public Result<Boolean> updateComboInventory(
            @Parameter(description = "关联ID") @PathVariable Long id,
            @Parameter(description = "使用数量") @RequestParam(required = false) BigDecimal quantity,
            @Parameter(description = "计量单位") @RequestParam(required = false) String unit,
            @Parameter(description = "备注") @RequestParam(required = false) String remark) {
        boolean result = comboInventoryService.updateComboInventory(id, quantity, unit, remark);
        return Result.success(result, "更新套餐库存产品关联成功");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除套餐库存产品关联")
    public Result<Boolean> deleteComboInventory(
            @Parameter(description = "关联ID") @PathVariable Long id) {
        boolean result = comboInventoryService.deleteComboInventory(id);
        return Result.success(result, "删除套餐库存产品关联成功");
    }

    @GetMapping("/combo/{comboId}/cost")
    @Operation(summary = "计算套餐成本")
    public Result<BigDecimal> calculateComboCost(
            @Parameter(description = "套餐ID") @PathVariable Long comboId) {
        BigDecimal cost = comboInventoryService.calculateComboCost(comboId);
        return Result.success(cost, "计算套餐成本成功");
    }
}
