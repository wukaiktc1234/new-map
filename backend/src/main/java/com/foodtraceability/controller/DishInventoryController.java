package com.foodtraceability.controller;

import com.foodtraceability.common.Result;
import com.foodtraceability.entity.DishInventory;
import com.foodtraceability.service.DishInventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/v1/dish-inventories")
@Tag(name = "菜品库存管理")
public class DishInventoryController {


    public DishInventoryController(DishInventoryService dishInventoryService) {
        this.dishInventoryService = dishInventoryService;
    }

    private final DishInventoryService dishInventoryService;

    @GetMapping("/dish/{dishId}")
    @Operation(summary = "获取菜品的库存产品列表")
    public Result<List<DishInventory>> getDishInventories(
            @Parameter(description = "菜品ID") @PathVariable String dishId) {
        List<DishInventory> inventories = dishInventoryService.getDishInventories(dishId);
        return Result.success(inventories, "获取菜品库存产品列表成功");
    }

    @PostMapping
    @Operation(summary = "添加菜品库存产品关联")
    public Result<DishInventory> addDishInventory(
            @Parameter(description = "菜品ID") @RequestParam String dishId,
            @Parameter(description = "库存产品ID") @RequestParam Long inventoryId,
            @Parameter(description = "使用数量") @RequestParam BigDecimal quantity,
            @Parameter(description = "计量单位") @RequestParam(required = false) String unit,
            @Parameter(description = "备注") @RequestParam(required = false) String remark) {
        DishInventory dishInventory = dishInventoryService.addDishInventory(dishId, inventoryId, quantity, unit, remark);
        return Result.success(dishInventory, "添加菜品库存产品关联成功");
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新菜品库存产品关联")
    public Result<Boolean> updateDishInventory(
            @Parameter(description = "关联ID") @PathVariable Long id,
            @Parameter(description = "使用数量") @RequestParam(required = false) BigDecimal quantity,
            @Parameter(description = "计量单位") @RequestParam(required = false) String unit,
            @Parameter(description = "备注") @RequestParam(required = false) String remark) {
        boolean result = dishInventoryService.updateDishInventory(id, quantity, unit, remark);
        return Result.success(result, "更新菜品库存产品关联成功");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除菜品库存产品关联")
    public Result<Boolean> deleteDishInventory(
            @Parameter(description = "关联ID") @PathVariable Long id) {
        boolean result = dishInventoryService.deleteDishInventory(id);
        return Result.success(result, "删除菜品库存产品关联成功");
    }

    @GetMapping("/dish/{dishId}/cost")
    @Operation(summary = "计算菜品成本")
    public Result<BigDecimal> calculateDishCost(
            @Parameter(description = "菜品ID") @PathVariable String dishId) {
        BigDecimal cost = dishInventoryService.calculateDishCost(dishId);
        return Result.success(cost, "计算菜品成本成功");
    }
}
