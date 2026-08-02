package com.foodtraceability.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.Result;
import com.foodtraceability.entity.InventoryCategory;
import com.foodtraceability.service.InventoryCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 库存分类控制器
 * 处理库存分类管理相关的HTTP请求
 */
@RestController
@RequestMapping("/v1/inventory/category")
@Tag(name = "库存分类管理")
public class InventoryCategoryController {
    

    public InventoryCategoryController(InventoryCategoryService inventoryCategoryService) {
        this.inventoryCategoryService = inventoryCategoryService;
    }

    private final InventoryCategoryService inventoryCategoryService;
    
    /**
     * 创建库存分类
     */
    @PostMapping
    @Operation(summary = "创建库存分类")
    @ApiResponse(responseCode = "200", description = "创建成功")
    @PreAuthorize("hasAuthority('inventory:create')")
    public Result<InventoryCategory> createInventoryCategory(@RequestBody InventoryCategory inventoryCategory) {
        try {
            InventoryCategory createdCategory = inventoryCategoryService.createInventoryCategory(inventoryCategory);
            return Result.success(createdCategory, "创建库存分类成功");
        } catch (Exception e) {
            return Result.error(500, "创建库存分类失败：" + e.getMessage());
        }
    }
    
    /**
     * 更新库存分类
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新库存分类")
    @ApiResponse(responseCode = "200", description = "更新成功")
    public Result<InventoryCategory> updateInventoryCategory(
            @Parameter(description = "库存分类ID") @PathVariable("id") Long id,
            @RequestBody InventoryCategory inventoryCategory) {
        try {
            InventoryCategory updatedCategory = inventoryCategoryService.updateInventoryCategory(id, inventoryCategory);
            return Result.success(updatedCategory, "更新库存分类成功");
        } catch (Exception e) {
            return Result.error(500, "更新库存分类失败：" + e.getMessage());
        }
    }
    
    /**
     * 根据ID获取库存分类
     */
    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取库存分类")
    @ApiResponse(responseCode = "200", description = "获取成功")
    public Result<InventoryCategory> getInventoryCategoryById(
            @Parameter(description = "库存分类ID") @PathVariable("id") Long id) {
        try {
            InventoryCategory inventoryCategory = inventoryCategoryService.getInventoryCategoryById(id);
            return Result.success(inventoryCategory, "获取库存分类成功");
        } catch (Exception e) {
            return Result.error(500, "获取库存分类失败：" + e.getMessage());
        }
    }
    
    /**
     * 根据ID删除库存分类
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "根据ID删除库存分类")
    @ApiResponse(responseCode = "200", description = "删除成功")
    public Result<Void> deleteInventoryCategory(
            @Parameter(description = "库存分类ID") @PathVariable("id") Long id) {
        try {
            inventoryCategoryService.deleteInventoryCategory(id);
            return Result.success(null, "删除库存分类成功");
        } catch (Exception e) {
            return Result.error(500, "删除库存分类失败：" + e.getMessage());
        }
    }
    
    /**
     * 分页查询库存分类列表
     */
    @GetMapping
    @Operation(summary = "分页查询库存分类列表")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @PreAuthorize("hasAuthority('inventory:query')")
    public Result<com.baomidou.mybatisplus.core.metadata.IPage<InventoryCategory>> getInventoryCategoryPage(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(description = "分类名称") @RequestParam(required = false) String name,
            @Parameter(description = "分类编码") @RequestParam(required = false) String code,
            @Parameter(description = "父分类ID") @RequestParam(required = false) Long parentId,
            @Parameter(description = "状态") @RequestParam(required = false) Boolean status) {
        try {
            Page<InventoryCategory> pageParam = new Page<>(page, pageSize);
            com.baomidou.mybatisplus.core.metadata.IPage<InventoryCategory> categoryPage = 
                    inventoryCategoryService.getInventoryCategoryPage(pageParam, name, code, parentId, status);
            return Result.success(categoryPage, "查询库存分类列表成功");
        } catch (Exception e) {
            return Result.error(500, "查询库存分类列表失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取所有库存分类列表
     */
    @GetMapping("/all")
    @Operation(summary = "获取所有库存分类列表")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @PreAuthorize("hasAuthority('inventory:query')")
    public Result<List<InventoryCategory>> getAllInventoryCategories() {
        try {
            List<InventoryCategory> categories = inventoryCategoryService.getAllInventoryCategories();
            return Result.success(categories, "查询所有库存分类成功");
        } catch (Exception e) {
            return Result.error(500, "查询所有库存分类失败：" + e.getMessage());
        }
    }
    
    /**
     * 根据父分类ID获取子分类列表
     */
    @GetMapping("/parent/{parentId}")
    @Operation(summary = "根据父分类ID获取子分类列表")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @PreAuthorize("hasAuthority('inventory:query')")
    public Result<List<InventoryCategory>> getInventoryCategoriesByParentId(
            @Parameter(description = "父分类ID") @PathVariable("parentId") Long parentId) {
        try {
            List<InventoryCategory> categories = inventoryCategoryService.getInventoryCategoriesByParentId(parentId);
            return Result.success(categories, "查询子分类列表成功");
        } catch (Exception e) {
            return Result.error(500, "查询子分类列表失败：" + e.getMessage());
        }
    }
}