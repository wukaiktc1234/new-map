package com.foodtraceability.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.WarehouseCreateDTO;
import com.foodtraceability.dto.WarehouseQueryDTO;
import com.foodtraceability.dto.WarehouseUpdateDTO;
import com.foodtraceability.entity.Warehouse;
import com.foodtraceability.service.WarehouseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 仓库控制器
 * 处理仓库管理相关的HTTP请求
 */
@RestController
@RequestMapping("/v1/warehouses")
@Tag(name = "仓库管理")
public class WarehouseController {

    private final WarehouseService warehouseService;

    public WarehouseController(WarehouseService warehouseService) {
        this.warehouseService = warehouseService;
    }

    /**
     * 创建仓库
     */
    @PostMapping
    @Operation(summary = "创建仓库")
    @PreAuthorize("hasAuthority('warehouse:create')")
    public Result<Warehouse> createWarehouse(@RequestBody WarehouseCreateDTO createDTO) {
        try {
            Warehouse warehouse = warehouseService.createWarehouse(createDTO);
            return Result.success(warehouse, "创建仓库成功");
        } catch (Exception e) {
            return Result.error(500, "创建仓库失败：" + e.getMessage());
        }
    }

    /**
     * 更新仓库
     */
    @PutMapping("/{warehouseId}")
    @Operation(summary = "更新仓库")
    @PreAuthorize("hasAuthority('warehouse:update')")
    public Result<Warehouse> updateWarehouse(
            @Parameter(description = "仓库ID") @PathVariable Long warehouseId,
            @RequestBody WarehouseUpdateDTO updateDTO) {
        try {
            Warehouse warehouse = warehouseService.updateWarehouse(warehouseId, updateDTO);
            return Result.success(warehouse, "更新仓库成功");
        } catch (Exception e) {
            return Result.error(500, "更新仓库失败：" + e.getMessage());
        }
    }

    /**
     * 获取仓库详情
     */
    @GetMapping("/{warehouseId}")
    @Operation(summary = "获取仓库详情")
    @PreAuthorize("hasAuthority('warehouse:query')")
    public Result<Warehouse> getWarehouse(
            @Parameter(description = "仓库ID") @PathVariable Long warehouseId) {
        try {
            Warehouse warehouse = warehouseService.getById(warehouseId);
            if (warehouse == null) {
                return Result.error(404, "仓库不存在");
            }
            return Result.success(warehouse);
        } catch (Exception e) {
            return Result.error(500, "查询仓库失败：" + e.getMessage());
        }
    }

    /**
     * 分页查询仓库列表
     */
    @GetMapping
    @Operation(summary = "分页查询仓库列表")
    @PreAuthorize("hasAuthority('warehouse:query')")
    public Result<com.baomidou.mybatisplus.core.metadata.IPage<Warehouse>> getWarehouseList(
            @Parameter(description = "仓库名称") @RequestParam(required = false) String warehouseName,
            @Parameter(description = "仓库编码") @RequestParam(required = false) String warehouseCode,
            @Parameter(description = "仓库类型") @RequestParam(required = false) Integer warehouseType,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer size) {
        try {
            Page<Warehouse> pageParam = new Page<>(page, size);
            var result = warehouseService.getWarehousePage(pageParam, warehouseName, warehouseCode, warehouseType, status);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(500, "查询仓库列表失败：" + e.getMessage());
        }
    }

    /**
     * 获取所有启用的仓库列表
     */
    @GetMapping("/active")
    @Operation(summary = "获取所有启用的仓库")
    @PreAuthorize("hasAuthority('warehouse:query')")
    public Result<List<Warehouse>> getActiveWarehouses() {
        try {
            List<Warehouse> warehouses = warehouseService.getActiveWarehouses();
            return Result.success(warehouses);
        } catch (Exception e) {
            return Result.error(500, "查询仓库列表失败：" + e.getMessage());
        }
    }

    /**
     * 启用/停用仓库
     */
    @PutMapping("/{warehouseId}/status")
    @Operation(summary = "启用/停用仓库")
    @PreAuthorize("hasAuthority('warehouse:update')")
    public Result<Void> toggleStatus(
            @Parameter(description = "仓库ID") @PathVariable Long warehouseId,
            @Parameter(description = "状态（1:启用 0:停用）") @RequestParam Integer status) {
        try {
            warehouseService.toggleWarehouseStatus(warehouseId, status);
            return Result.success(null, "操作成功");
        } catch (Exception e) {
            return Result.error(500, "操作失败：" + e.getMessage());
        }
    }

    /**
     * 删除仓库（逻辑删除）
     */
    @DeleteMapping("/{warehouseId}")
    @Operation(summary = "删除仓库")
    @PreAuthorize("hasAuthority('warehouse:delete')")
    public Result<Void> deleteWarehouse(
            @Parameter(description = "仓库ID") @PathVariable Long warehouseId) {
        try {
            warehouseService.deleteWarehouse(warehouseId);
            return Result.success(null, "删除仓库成功");
        } catch (Exception e) {
            return Result.error(500, "删除仓库失败：" + e.getMessage());
        }
    }
}
