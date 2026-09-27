package com.foodtraceability.controller;

import com.foodtraceability.common.Result;
import com.foodtraceability.entity.Location;
import com.foodtraceability.service.LocationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 库存位置接口（M1-M2 核心层，只读）。
 * 门店与仓库统一位置模型；写路径（落账/调拨切换）在后续批次。
 */
@RestController
@RequestMapping("/v1/locations")
@PreAuthorize("hasAnyRole('admin', 'regional_manager', 'manager', 'store_manager', 'warehouse_manager', 'purchase_manager', 'department_manager', 'employee')")
public class LocationController {

    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    /** 活跃位置列表（可按类型过滤） */
    @GetMapping
    public Result<List<Location>> listActive(@RequestParam(required = false) String locationType) {
        return Result.success(locationService.listActive(locationType));
    }

    /** 按位置ID查询 */
    @GetMapping("/{locationId}")
    public Result<Location> getById(@PathVariable Long locationId) {
        return Result.success(locationService.getById(locationId));
    }

    /** 按门店 ID（stores_new.store_id 别名）解析 STORE 型位置 */
    @GetMapping("/by-store/{storeId}")
    public Result<Location> resolveByStoreId(@PathVariable Long storeId) {
        return Result.success(locationService.resolveByStoreId(storeId));
    }

    /** 按仓库 ID（warehouses.warehouse_id 别名）解析 CENTRAL/DEPOT 型位置 */
    @GetMapping("/by-warehouse/{warehouseId}")
    public Result<Location> resolveByWarehouseId(@PathVariable Long warehouseId) {
        return Result.success(locationService.resolveByWarehouseId(warehouseId));
    }
}
