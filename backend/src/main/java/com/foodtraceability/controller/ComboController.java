package com.foodtraceability.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.product.*;
import com.foodtraceability.service.ComboService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 套餐管理控制器
 * 提供套餐的完整REST API，包含成本计算功能
 * 权限要求：需要product:combo基础权限
 */
@RestController
@RequestMapping("/v1/product-center/combos")
@Tag(name = "套餐管理", description = "套餐的增删改查、明细管理、成本计算接口")
@PreAuthorize("hasAuthority('product:combo:view')")
public class ComboController {

    private final ComboService comboService;

    public ComboController(ComboService comboService) {
        this.comboService = comboService;
    }

    /**
     * 创建套餐（含明细）
     */
    @PostMapping
    @PreAuthorize("hasAuthority('product:combo:create')")
    @Operation(summary = "创建套餐", description = "新增套餐及其组成菜品明细，自动计算原价和优惠")
    public Result<ComboVO> create(@Valid @RequestBody ComboCreateDTO dto) {
        return Result.success(comboService.create(dto));
    }

    /**
     * 更新套餐信息（含明细全量更新）
     */
    @PutMapping("/{comboId}")
    @PreAuthorize("hasAuthority('product:combo:update')")
    @Operation(summary = "更新套餐", description = "更新套餐信息及明细（全量替换）")
    public Result<ComboVO> update(
            @Parameter(description = "套餐ID") @PathVariable Long comboId,
            @Valid @RequestBody ComboUpdateDTO dto) {
        return Result.success(comboService.update(comboId, dto));
    }

    /**
     * 根据ID获取套餐详情（含明细和成本）
     */
    @GetMapping("/{comboId}")
    @Operation(summary = "获取套餐详情", description = "获取套餐详细信息，包含明细列表、成本价、毛利")
    public Result<ComboVO> getById(
            @Parameter(description = "套餐ID") @PathVariable Long comboId) {
        return Result.success(comboService.getById(comboId));
    }

    /**
     * 分页查询套餐列表
     */
    @GetMapping
    @Operation(summary = "分页查询套餐", description = "支持多条件筛选的分页查询")
    public Result<Page<ComboVO>> queryPage(ComboQueryDTO queryDto) {
        return Result.success(comboService.queryPage(queryDto));
    }

    /**
     * 查询在售套餐列表
     */
    @GetMapping("/on-sale")
    @Operation(summary = "在售套餐列表", description = "获取所有在售状态的套餐")
    public Result<java.util.List<ComboVO>> listOnSale() {
        return Result.success(comboService.listOnSale());
    }

    /**
     * 计算套餐成本价
     */
    @GetMapping("/{comboId}/cost")
    @Operation(summary = "计算套餐成本", description = "根据组成菜品明细计算套餐总成本价（分）")
    public Result<Long> calculateCost(
            @Parameter(description = "套餐ID") @PathVariable Long comboId) {
        return Result.success(comboService.calculateCost(comboId));
    }

    /**
     * 更新套餐状态
     */
    @PutMapping("/{comboId}/status/{status}")
    @PreAuthorize("hasAuthority('product:combo:update')")
    @Operation(summary = "更新套餐状态", description = "更新套餐的上下架状态：1在售 0停售")
    public Result<Void> updateStatus(
            @Parameter(description = "套餐ID") @PathVariable Long comboId,
            @Parameter(description = "目标状态: 1在售 0停售") @PathVariable Integer status) {
        comboService.updateStatus(comboId, status);
        return Result.success();
    }

    /**
     * 删除套餐（逻辑删除，同时删除明细）
     */
    @DeleteMapping("/{comboId}")
    @PreAuthorize("hasAuthority('product:combo:delete')")
    @Operation(summary = "删除套餐", description = "删除套餐及其所有明细")
    public Result<Void> delete(
            @Parameter(description = "套餐ID") @PathVariable Long comboId) {
        comboService.delete(comboId);
        return Result.success();
    }
}
