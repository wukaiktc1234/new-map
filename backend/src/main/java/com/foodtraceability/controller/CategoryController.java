package com.foodtraceability.controller;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.product.*;
import com.foodtraceability.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 分类管理控制器
 * 提供菜品分类的树形CRUD接口
 * 权限要求：需要product:category基础权限
 */
@RestController
@RequestMapping("/v1/product-center/categories")
@Tag(name = "分类管理", description = "菜品分类的增删改查、树形结构、层级管理接口")
@PreAuthorize("hasAuthority('product:category:view')")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /**
     * 创建分类
     */
    @PostMapping
    @PreAuthorize("hasAuthority('product:category:create')")
    @Operation(summary = "创建分类", description = "新增一个菜品分类，支持指定父级形成树形结构")
    public Result<CategoryVO> create(@Valid @RequestBody CategoryCreateDTO dto) {
        return Result.success(categoryService.create(dto));
    }

    /**
     * 更新分类信息
     */
    @PutMapping("/{categoryId}")
    @PreAuthorize("hasAuthority('product:category:update')")
    @Operation(summary = "更新分类", description = "根据ID更新分类信息")
    public Result<CategoryVO> update(
            @Parameter(description = "分类ID") @PathVariable Long categoryId,
            @Valid @RequestBody CategoryUpdateDTO dto) {
        return Result.success(categoryService.update(categoryId, dto));
    }

    /**
     * 根据ID获取分类详情
     */
    @GetMapping("/{categoryId}")
    @Operation(summary = "获取分类详情", description = "根据分类ID获取详细信息，包含菜品数量统计")
    public Result<CategoryVO> getById(
            @Parameter(description = "分类ID") @PathVariable Long categoryId) {
        return Result.success(categoryService.getById(categoryId));
    }

    /**
     * 获取分类树形结构（全部）
     */
    @GetMapping("/tree")
    @Operation(summary = "获取分类树", description = "获取完整的分类树形结构，包含启用和停用的分类")
    public Result<List<CategoryVO>> getTree() {
        return Result.success(categoryService.getTree());
    }

    /**
     * 获取启用的分类树形结构
     */
    @GetMapping("/tree/enabled")
    @Operation(summary = "获取启用分类树", description = "获取仅包含启用状态的分类树形结构")
    public Result<List<CategoryVO>> getEnabledTree() {
        return Result.success(categoryService.getEnabledTree());
    }

    /**
     * 获取所有分类列表（扁平）
     */
    @GetMapping("/list")
    @Operation(summary = "获取分类列表", description = "获取所有分类的扁平列表")
    public Result<List<CategoryVO>> listAll() {
        return Result.success(categoryService.listAll());
    }

    /**
     * 获取子分类列表
     */
    @GetMapping("/{parentId}/children")
    @Operation(summary = "获取子分类", description = "获取指定分类的直接子分类列表")
    public Result<List<CategoryVO>> listChildren(
            @Parameter(description = "父级分类ID") @PathVariable Long parentId) {
        return Result.success(categoryService.listChildren(parentId));
    }

    /**
     * 更新分类状态
     */
    @PutMapping("/{categoryId}/status/{status}")
    @PreAuthorize("hasAuthority('product:category:update')")
    @Operation(summary = "更新分类状态", description = "更新分类的启用/停用状态，停用时同步停用子分类和关联菜品")
    public Result<Void> updateStatus(
            @Parameter(description = "分类ID") @PathVariable Long categoryId,
            @Parameter(description = "目标状态: 1启用 0停用") @PathVariable Integer status) {
        categoryService.updateStatus(categoryId, status);
        return Result.success();
    }

    /**
     * 排序分类
     */
    @PutMapping("/{categoryId}/sort/{sortOrder}")
    @PreAuthorize("hasAuthority('product:category:update')")
    @Operation(summary = "更新分类排序", description = "更新分类的排序权重值")
    public Result<Void> updateSortOrder(
            @Parameter(description = "分类ID") @PathVariable Long categoryId,
            @Parameter(description = "排序值") @PathVariable Integer sortOrder) {
        categoryService.updateSortOrder(categoryId, sortOrder);
        return Result.success();
    }

    /**
     * 移动分类（更改父级）
     */
    @PutMapping("/{categoryId}/move/{newParentId}")
    @PreAuthorize("hasAuthority('product:category:update')")
    @Operation(summary = "移动分类", description = "将分类移动到新的父级分类下")
    public Result<Void> moveCategory(
            @Parameter(description = "分类ID") @PathVariable Long categoryId,
            @Parameter(description = "新的父级分类ID") @PathVariable Long newParentId) {
        categoryService.moveCategory(categoryId, newParentId);
        return Result.success();
    }

    /**
     * 删除分类（逻辑删除）
     */
    @DeleteMapping("/{categoryId}")
    @PreAuthorize("hasAuthority('product:category:delete')")
    @Operation(summary = "删除分类", description = "逻辑删除指定分类（需先清空子分类和关联菜品）")
    public Result<Void> delete(
            @Parameter(description = "分类ID") @PathVariable Long categoryId) {
        categoryService.delete(categoryId);
        return Result.success();
    }
}
