package com.foodtraceability.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.InventoryLocationCreateDTO;
import com.foodtraceability.dto.InventoryLocationUpdateDTO;
import com.foodtraceability.dto.InventoryLocationVO;
import com.foodtraceability.entity.InventoryLocation;
import com.foodtraceability.service.InventoryLocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 库位管理控制器
 * 处理库位管理相关的HTTP请求
 */
@RestController
@RequestMapping("/v1/inventory-locations")
@Tag(name = "库位管理")
public class InventoryLocationController {

    private final InventoryLocationService locationService;

    public InventoryLocationController(InventoryLocationService locationService) {
        this.locationService = locationService;
    }

    /**
     * 分页查询库位列表
     */
    @GetMapping
    @Operation(summary = "分页查询库位列表")
    public Result<com.baomidou.mybatisplus.core.metadata.IPage<InventoryLocationVO>> getLocationList(
            @Parameter(description = "仓库ID") @RequestParam(required = false) Long warehouseId,
            @Parameter(description = "库位编码") @RequestParam(required = false) String locationCode,
            @Parameter(description = "库位类型") @RequestParam(required = false) Integer locationType,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer size) {
        try {
            Page<InventoryLocation> pageParam = new Page<>(page, size);
            var result = locationService.getLocationPage(pageParam, warehouseId, locationCode, locationType, status);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(500, "查询库位列表失败：" + e.getMessage());
        }
    }

    /**
     * 获取库位详情
     */
    @GetMapping("/{locationId}")
    @Operation(summary = "获取库位详情")
    @PreAuthorize("hasAuthority('inventory:query')")
    public Result<InventoryLocationVO> getLocation(
            @Parameter(description = "库位ID") @PathVariable Long locationId) {
        try {
            InventoryLocationVO vo = locationService.getLocationDetail(locationId);
            return Result.success(vo);
        } catch (Exception e) {
            return Result.error(500, "查询库位失败：" + e.getMessage());
        }
    }

    /**
     * 创建库位
     */
    @PostMapping
    @Operation(summary = "创建库位")
    @PreAuthorize("hasAuthority('inventory:create')")
    public Result<InventoryLocation> createLocation(@Valid @RequestBody InventoryLocationCreateDTO createDTO) {
        try {
            InventoryLocation location = locationService.createLocation(createDTO);
            return Result.success(location, "创建库位成功");
        } catch (Exception e) {
            return Result.error(500, "创建库位失败：" + e.getMessage());
        }
    }

    /**
     * 更新库位
     */
    @PutMapping("/{locationId}")
    @Operation(summary = "更新库位")
    @PreAuthorize("hasAuthority('inventory:update')")
    public Result<InventoryLocation> updateLocation(
            @Parameter(description = "库位ID") @PathVariable Long locationId,
            @Valid @RequestBody InventoryLocationUpdateDTO updateDTO) {
        try {
            InventoryLocation location = locationService.updateLocation(locationId, updateDTO);
            return Result.success(location, "更新库位成功");
        } catch (Exception e) {
            return Result.error(500, "更新库位失败：" + e.getMessage());
        }
    }

    /**
     * 启用/停用库位
     */
    @PutMapping("/{locationId}/status")
    @Operation(summary = "启用/停用库位")
    @PreAuthorize("hasAuthority('inventory:update')")
    public Result<Void> toggleStatus(
            @Parameter(description = "库位ID") @PathVariable Long locationId,
            @Parameter(description = "状态（1:启用 0:停用）") @RequestParam Integer status) {
        try {
            locationService.toggleLocationStatus(locationId, status);
            return Result.success(null, "操作成功");
        } catch (Exception e) {
            return Result.error(500, "操作失败：" + e.getMessage());
        }
    }

    /**
     * 删除库位（逻辑删除）
     * 用于清理废弃库位，业务核心数据不物理删除
     */
    @DeleteMapping("/{locationId}")
    @Operation(summary = "删除库位")
    @PreAuthorize("hasAuthority('warehouse:location:delete')")
    public Result<Void> deleteLocation(
            @Parameter(description = "库位ID") @PathVariable Long locationId) {
        try {
            locationService.deleteLocation(locationId);
            return Result.success(null, "删除库位成功");
        } catch (Exception e) {
            return Result.error(500, "删除库位失败：" + e.getMessage());
        }
    }

    /**
     * 根据仓库ID获取库位列表
     */
    @GetMapping("/by-warehouse/{warehouseId}")
    @Operation(summary = "根据仓库ID获取库位列表")
    @PreAuthorize("hasAuthority('inventory:query')")
    public Result<List<InventoryLocation>> getLocationsByWarehouseId(
            @Parameter(description = "仓库ID") @PathVariable Long warehouseId) {
        try {
            List<InventoryLocation> locations = locationService.getLocationsByWarehouseId(warehouseId);
            return Result.success(locations);
        } catch (Exception e) {
            return Result.error(500, "查询库位列表失败：" + e.getMessage());
        }
    }
}
