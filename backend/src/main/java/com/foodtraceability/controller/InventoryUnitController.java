package com.foodtraceability.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.Result;
import com.foodtraceability.entity.InventoryUnit;
import com.foodtraceability.service.InventoryUnitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 计量单位控制器
 * 处理计量单位管理相关的HTTP请求
 */
@RestController
@RequestMapping("/v1/inventory/unit")
@Tag(name = "计量单位管理")
public class InventoryUnitController {
    

    public InventoryUnitController(InventoryUnitService inventoryUnitService) {
        this.inventoryUnitService = inventoryUnitService;
    }

    private final InventoryUnitService inventoryUnitService;
    
    /**
     * 创建计量单位
     */
    @PostMapping
    @Operation(summary = "创建计量单位")
    @ApiResponse(responseCode = "200", description = "创建成功")
    public Result<InventoryUnit> createInventoryUnit(@RequestBody InventoryUnit inventoryUnit) {
        try {
            InventoryUnit createdUnit = inventoryUnitService.createInventoryUnit(inventoryUnit);
            return Result.success(createdUnit, "创建计量单位成功");
        } catch (Exception e) {
            return Result.error(500, "创建计量单位失败：" + e.getMessage());
        }
    }
    
    /**
     * 更新计量单位
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新计量单位")
    @ApiResponse(responseCode = "200", description = "更新成功")
    @PreAuthorize("hasAuthority('inventory:update')")
    public Result<InventoryUnit> updateInventoryUnit(
            @Parameter(description = "计量单位ID") @PathVariable("id") Long id,
            @RequestBody InventoryUnit inventoryUnit) {
        try {
            InventoryUnit updatedUnit = inventoryUnitService.updateInventoryUnit(id, inventoryUnit);
            return Result.success(updatedUnit, "更新计量单位成功");
        } catch (Exception e) {
            return Result.error(500, "更新计量单位失败：" + e.getMessage());
        }
    }
    
    /**
     * 根据ID获取计量单位
     */
    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取计量单位")
    @ApiResponse(responseCode = "200", description = "获取成功")
    public Result<InventoryUnit> getInventoryUnitById(
            @Parameter(description = "计量单位ID") @PathVariable("id") Long id) {
        try {
            InventoryUnit inventoryUnit = inventoryUnitService.getInventoryUnitById(id);
            return Result.success(inventoryUnit, "获取计量单位成功");
        } catch (Exception e) {
            return Result.error(500, "获取计量单位失败：" + e.getMessage());
        }
    }
    
    /**
     * 根据ID删除计量单位
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "根据ID删除计量单位")
    @ApiResponse(responseCode = "200", description = "删除成功")
    public Result<Void> deleteInventoryUnit(
            @Parameter(description = "计量单位ID") @PathVariable("id") Long id) {
        try {
            inventoryUnitService.deleteInventoryUnit(id);
            return Result.success(null, "删除计量单位成功");
        } catch (Exception e) {
            return Result.error(500, "删除计量单位失败：" + e.getMessage());
        }
    }
    
    /**
     * 分页查询计量单位列表
     */
    @GetMapping
    @Operation(summary = "分页查询计量单位列表")
    @ApiResponse(responseCode = "200", description = "查询成功")
    public Result<com.baomidou.mybatisplus.core.metadata.IPage<InventoryUnit>> getInventoryUnitPage(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(description = "单位名称") @RequestParam(required = false) String name,
            @Parameter(description = "单位编码") @RequestParam(required = false) String code,
            @Parameter(description = "单位类型") @RequestParam(required = false) String type,
            @Parameter(description = "状态") @RequestParam(required = false) Boolean status) {
        try {
            Page<InventoryUnit> pageParam = new Page<>(page, pageSize);
            com.baomidou.mybatisplus.core.metadata.IPage<InventoryUnit> unitPage = 
                    inventoryUnitService.getInventoryUnitPage(pageParam, name, code, type, status);
            return Result.success(unitPage, "查询计量单位列表成功");
        } catch (Exception e) {
            return Result.error(500, "查询计量单位列表失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取所有计量单位列表
     */
    @GetMapping("/all")
    @Operation(summary = "获取所有计量单位列表")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @PreAuthorize("hasAuthority('inventory:query')")
    public Result<List<InventoryUnit>> getAllInventoryUnits() {
        try {
            List<InventoryUnit> units = inventoryUnitService.getAllInventoryUnits();
            return Result.success(units, "查询所有计量单位成功");
        } catch (Exception e) {
            return Result.error(500, "查询所有计量单位失败：" + e.getMessage());
        }
    }
    
    /**
     * 根据类型获取计量单位列表
     */
    @GetMapping("/type/{type}")
    @Operation(summary = "根据类型获取计量单位列表")
    @ApiResponse(responseCode = "200", description = "查询成功")
    public Result<List<InventoryUnit>> getInventoryUnitsByType(
            @Parameter(description = "单位类型") @PathVariable("type") String type) {
        try {
            List<InventoryUnit> units = inventoryUnitService.getInventoryUnitsByType(type);
            return Result.success(units, "根据类型查询计量单位成功");
        } catch (Exception e) {
            return Result.error(500, "根据类型查询计量单位失败：" + e.getMessage());
        }
    }
}