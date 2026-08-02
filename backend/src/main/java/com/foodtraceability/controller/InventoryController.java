package com.foodtraceability.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.InventoryDeductDTO;
import com.foodtraceability.dto.IncreaseDTO;
import com.foodtraceability.dto.InventoryIncreaseDTO;
import com.foodtraceability.dto.InventoryLockDTO;
import com.foodtraceability.dto.InventoryQueryDTO;
import com.foodtraceability.entity.Inventory;
import com.foodtraceability.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * 库存控制器
 * 处理库存管理相关的HTTP请求，包括库存查询、锁定、扣减、增加等操作
 */
@RestController
@RequestMapping("/v1/inventory")
@Tag(name = "库存管理")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    /**
     * 分页查询库存列表
     */
    @GetMapping
    @Operation(summary = "分页查询库存列表")
    @PreAuthorize("hasAuthority('inventory:query')")
    public Result<com.baomidou.mybatisplus.core.metadata.IPage<Inventory>> getInventoryList(
            @Parameter(description = "仓库ID") @RequestParam(required = false) Long warehouseId,
            @Parameter(description = "物料ID") @RequestParam(required = false) Long materialId,
            @Parameter(description = "物料名称") @RequestParam(required = false) String materialName,
            @Parameter(description = "批次号") @RequestParam(required = false) String batchNo,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer size) {
        try {
            InventoryQueryDTO queryDTO = new InventoryQueryDTO();
            queryDTO.setWarehouseId(warehouseId);
            queryDTO.setMaterialId(materialId);
            queryDTO.setMaterialName(materialName);
            queryDTO.setBatchNo(batchNo);
            queryDTO.setStatus(status);
            queryDTO.setPage(page);
            queryDTO.setSize(size);

            Page<Inventory> pageParam = new Page<>(page, size);
            var result = inventoryService.getInventoryPage(pageParam, queryDTO);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(500, "查询库存列表失败：" + e.getMessage());
        }
    }

    /**
     * 获取库存详情
     */
    @GetMapping("/{inventoryId}")
    @Operation(summary = "获取库存详情")
    public Result<Inventory> getInventory(
            @Parameter(description = "库存ID") @PathVariable Long inventoryId) {
        try {
            Inventory inventory = inventoryService.getById(inventoryId);
            if (inventory == null) {
                return Result.error(404, "库存记录不存在");
            }
            return Result.success(inventory);
        } catch (Exception e) {
            return Result.error(500, "查询库存失败：" + e.getMessage());
        }
    }

    /**
     * 查询可用库存数量
     */
    @GetMapping("/{inventoryId}/available")
    @Operation(summary = "查询可用库存数量")
    public Result<BigDecimal> getAvailableQuantity(
            @Parameter(description = "库存ID") @PathVariable Long inventoryId) {
        try {
            BigDecimal availableQty = inventoryService.getAvailableQuantity(inventoryId);
            return Result.success(availableQty);
        } catch (Exception e) {
            return Result.error(500, "查询可用库存失败：" + e.getMessage());
        }
    }

    /**
     * 锁定库存（用于订单预留）
     */
    @PostMapping("/lock")
    @Operation(summary = "锁定库存")
    public Result<Void> lockInventory(@RequestBody InventoryLockDTO lockDTO) {
        try {
            inventoryService.lockInventory(lockDTO);
            return Result.success(null, "库存锁定成功");
        } catch (Exception e) {
            return Result.error(500, "库存锁定失败：" + e.getMessage());
        }
    }

    /**
     * 解锁库存（取消订单预留）
     */
    @PostMapping("/{inventoryId}/unlock")
    @Operation(summary = "解锁库存")
    @PreAuthorize("hasAuthority('inventory:lock')")
    public Result<Void> unlockInventory(
            @Parameter(description = "库存ID") @PathVariable Long inventoryId,
            @Parameter(description = "解锁数量") @RequestParam BigDecimal quantity) {
        try {
            inventoryService.unlockInventory(inventoryId, quantity);
            return Result.success(null, "库存解锁成功");
        } catch (Exception e) {
            return Result.error(500, "库存解锁失败：" + e.getMessage());
        }
    }

    /**
     * 扣减库存（销售出库等场景）
     */
    @PostMapping("/deduct")
    @Operation(summary = "扣减库存")
    @PreAuthorize("hasAuthority('inventory:deduct')")
    public Result<Void> deductInventory(@RequestBody InventoryDeductDTO deductDTO) {
        try {
            inventoryService.deductInventory(deductDTO);
            return Result.success(null, "库存扣减成功");
        } catch (Exception e) {
            return Result.error(500, "库存扣减失败：" + e.getMessage());
        }
    }

    /**
     * 增加库存（采购入库、调拨入库、盘盈等场景）
     */
    @PostMapping("/increase")
    @Operation(summary = "增加库存")
    public Result<Void> increaseInventory(@RequestBody InventoryIncreaseDTO increaseDTO) {
        try {
            inventoryService.increaseInventory(increaseDTO);
            return Result.success(null, "库存增加成功");
        } catch (Exception e) {
            return Result.error(500, "库存增加失败：" + e.getMessage());
        }
    }

    /**
     * 获取低库存列表
     */
    @GetMapping("/low-stock")
    @Operation(summary = "获取低库存列表")
    @PreAuthorize("hasAuthority('inventory:query')")
    public Result<java.util.List<Inventory>> getLowStockList(
            @Parameter(description = "仓库ID") @RequestParam(required = false) Long warehouseId) {
        try {
            var list = inventoryService.getLowStockList(warehouseId);
            return Result.success(list);
        } catch (Exception e) {
            return Result.error(500, "查询低库存列表失败：" + e.getMessage());
        }
    }
}
