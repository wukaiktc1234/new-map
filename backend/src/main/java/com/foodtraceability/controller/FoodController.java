package com.foodtraceability.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.product.*;
import com.foodtraceability.service.FoodService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 菜品管理控制器
 * 提供菜品/食品的完整REST API
 * 遵循RESTful规范，返回统一Result<T>格式
 * 权限要求：需要product:food基础权限
 */
@RestController
@RequestMapping("/v1/product-center/foods")
@Tag(name = "菜品管理", description = "菜品/食品的增删改查、状态管理、批量操作接口")
@PreAuthorize("hasAuthority('product:food:view')")
public class FoodController {

    private final FoodService foodService;

    public FoodController(FoodService foodService) {
        this.foodService = foodService;
    }

    /**
     * 创建菜品
     */
    @PostMapping
    @PreAuthorize("hasAuthority('product:food:create')")
    @Operation(summary = "创建菜品", description = "新增一个菜品/食品，自动生成编码")
    public Result<FoodVO> create(@Valid @RequestBody FoodCreateDTO dto) {
        return Result.success(foodService.create(dto));
    }

    /**
     * 更新菜品信息
     */
    @PutMapping("/{foodId}")
    @PreAuthorize("hasAuthority('product:food:update')")
    @Operation(summary = "更新菜品", description = "根据ID更新菜品信息（部分更新）")
    public Result<FoodVO> update(
            @Parameter(description = "菜品ID") @PathVariable Long foodId,
            @Valid @RequestBody FoodUpdateDTO dto) {
        return Result.success(foodService.update(foodId, dto));
    }

    /**
     * 根据ID获取菜品详情
     */
    @GetMapping("/{foodId}")
    @Operation(summary = "获取菜品详情", description = "根据菜品ID获取详细信息，包含分类名称和毛利计算")
    public Result<FoodVO> getById(
            @Parameter(description = "菜品ID") @PathVariable Long foodId) {
        return Result.success(foodService.getById(foodId));
    }

    /**
     * 分页查询菜品列表
     */
    @GetMapping
    @Operation(summary = "分页查询菜品", description = "支持多条件筛选的分页查询")
    public Result<Page<FoodVO>> queryPage(FoodQueryDTO queryDto) {
        return Result.success(foodService.queryPage(queryDto));
    }

    /**
     * 查询在售菜品列表（用于菜单展示）
     */
    @GetMapping("/on-sale")
    @Operation(summary = "在售菜品列表", description = "获取所有在售状态的菜品，用于POS菜单展示")
    public Result<List<FoodVO>> listOnSale() {
        return Result.success(foodService.listOnSale());
    }

    /**
     * 查询推荐菜品
     */
    @GetMapping("/recommend")
    @Operation(summary = "推荐菜品", description = "获取推荐的在售菜品列表")
    public Result<List<FoodVO>> listRecommend(
            @Parameter(description = "数量限制") @RequestParam(defaultValue = "10") int limit) {
        return Result.success(foodService.listRecommend(limit));
    }

    /**
     * 根据分类查询菜品
     */
    @GetMapping("/category/{categoryId}")
    @Operation(summary = "按分类查询菜品", description = "获取指定分类下的在售菜品列表")
    public Result<List<FoodVO>> listByCategory(
            @Parameter(description = "分类ID") @PathVariable Long categoryId) {
        return Result.success(foodService.listByCategory(categoryId));
    }

    /**
     * 更新菜品状态
     */
    @PutMapping("/{foodId}/status/{status}")
    @PreAuthorize("hasAuthority('product:food:update')")
    @Operation(summary = "更新菜品状态", description = "更新菜品的上下架状态：1在售 0停售 2售罄")
    public Result<Void> updateStatus(
            @Parameter(description = "菜品ID") @PathVariable Long foodId,
            @Parameter(description = "目标状态: 1在售 0停售 2售罄") @PathVariable Integer status) {
        foodService.updateStatus(foodId, status);
        return Result.success();
    }

    /**
     * 批量更新菜品状态
     */
    @PutMapping("/batch-status")
    @PreAuthorize("hasAuthority('product:food:update')")
    @Operation(summary = "批量更新菜品状态", description = "批量更新多个菜品的上下架状态")
    public Result<Void> batchUpdateStatus(
            @Valid @RequestBody BatchStatusUpdateDTO batchDto) {
        foodService.batchUpdateStatus(batchDto.getFoodIds(), batchDto.getStatus());
        return Result.success();
    }

    /**
     * 删除菜品（逻辑删除）
     */
    @DeleteMapping("/{foodId}")
    @PreAuthorize("hasAuthority('product:food:delete')")
    @Operation(summary = "删除菜品", description = "逻辑删除指定菜品")
    public Result<Void> delete(
            @Parameter(description = "菜品ID") @PathVariable Long foodId) {
        foodService.delete(foodId);
        return Result.success();
    }

    /**
     * 批量删除菜品（逻辑删除）
     */
    @DeleteMapping("/batch")
    @PreAuthorize("hasAuthority('product:food:delete')")
    @Operation(summary = "批量删除菜品", description = "批量逻辑删除多个菜品")
    public Result<Void> batchDelete(@RequestBody List<Long> foodIds) {
        foodService.batchDelete(foodIds);
        return Result.success();
    }

    /**
     * 导入菜品（Excel）
     */
    @PostMapping("/import")
    @PreAuthorize("hasAuthority('product:food:import') or hasAuthority('*')")
    @Operation(summary = "导入菜品", description = "通过Excel文件批量导入菜品")
    public Result<FoodImportResultDTO> importFoods(@RequestParam("file") MultipartFile file) {
        return Result.success(foodService.importFoods(file));
    }

    /**
     * 导出菜品（Excel）
     */
    @GetMapping("/export")
    @PreAuthorize("hasAuthority('product:food:export') or hasAuthority('*')")
    @Operation(summary = "导出菜品", description = "根据查询条件导出菜品数据为Excel文件")
    public void exportFoods(FoodQueryDTO queryDTO, HttpServletResponse response) {
        foodService.exportFoods(queryDTO, response);
    }

    /**
     * 下载导入模板（Excel）
     */
    @GetMapping("/import-template")
    @PreAuthorize("hasAuthority('product:food:import') or hasAuthority('*')")
    @Operation(summary = "下载导入模板", description = "下载菜品导入Excel模板文件")
    public void downloadImportTemplate(HttpServletResponse response) {
        foodService.downloadImportTemplate(response);
    }
}
