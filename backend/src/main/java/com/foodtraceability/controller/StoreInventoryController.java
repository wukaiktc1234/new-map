package com.foodtraceability.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.Result;
import com.foodtraceability.entity.Store;
import com.foodtraceability.entity.StoreInventory;
import com.foodtraceability.entity.StoreInventoryLog;
import com.foodtraceability.entity.InventorySummary;
import com.foodtraceability.service.InventorySummaryService;
import com.foodtraceability.service.StoreInventoryLogService;
import com.foodtraceability.service.StoreInventoryService;
import com.foodtraceability.service.StoreService;
import com.foodtraceability.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/v1/store-inventory")
@Tag(name = "门店库存管理")
public class StoreInventoryController {


    public StoreInventoryController(StoreInventoryService storeInventoryService,
                                    StoreInventoryLogService storeInventoryLogService,
                                    InventorySummaryService inventorySummaryService,
                                    StoreService storeService) {
        this.storeInventoryService = storeInventoryService;
        this.storeInventoryLogService = storeInventoryLogService;
        this.inventorySummaryService = inventorySummaryService;
        this.storeService = storeService;
    }

    private final StoreInventoryService storeInventoryService;

    private final StoreInventoryLogService storeInventoryLogService;

    private final InventorySummaryService inventorySummaryService;

    private final StoreService storeService;

    @GetMapping("/stores")
    @PreAuthorize("hasAuthority('warehouse:inventory:view') or hasAuthority('warehouse:overview:view') or hasAuthority('*')")
    @Operation(summary = "获取所有活跃门店")
    public Result<List<Store>> getActiveStores() {
        List<Store> stores = storeService.getActiveStores();
        return Result.success(stores, "获取门店列表成功");
    }

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('warehouse:inventory:view') or hasAuthority('*')")
    @Operation(summary = "分页查询门店库存列表")
    public Result<IPage<StoreInventory>> getStoreInventoryPage(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(description = "门店ID") @RequestParam(required = false) Long storeId,
            @Parameter(description = "产品ID") @RequestParam(required = false) Long productId,
            @Parameter(description = "产品名称") @RequestParam(required = false) String productName) {
        try {
            Page<StoreInventory> pageParam = new Page<>(page, pageSize);

            // 门店ID处理：非管理员且具有门店权限时，强制使用当前用户的门店ID
            String storeIdStr = storeId != null ? String.valueOf(storeId) : null;

            if (!SecurityUtils.isAdmin() && SecurityUtils.hasStorePermission()) {
                String currentUserStoreId = SecurityUtils.getCurrentUserStoreId();
                if (currentUserStoreId != null && !currentUserStoreId.isEmpty()) {
                    storeIdStr = currentUserStoreId;
                }
            }

            IPage<StoreInventory> inventoryPage = storeInventoryService.getStoreInventoryPage(
                    pageParam, storeIdStr, productId, productName);
            return Result.success(inventoryPage, "查询门店库存列表成功");
        } catch (Exception e) {
            // 容错：store_inventory 表尚未创建或其他底层异常时，返回空分页结果，避免前端崩溃
            Page<StoreInventory> emptyPage = new Page<>(page, pageSize);
            emptyPage.setTotal(0);
            return Result.success(emptyPage, "查询门店库存列表成功");
        }
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('warehouse:overview:view') or hasAuthority('warehouse:inventory:view') or hasAuthority('*')")
    @Operation(summary = "分页查询库存汇总（包含所有门店和仓库）")
    public Result<IPage<InventorySummary>> getInventorySummaryPage(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(description = "产品名称") @RequestParam(required = false) String productName) {
        try {
            Page<InventorySummary> pageParam = new Page<>(page, pageSize);
            IPage<InventorySummary> summaryPage = inventorySummaryService.getSummaryPage(pageParam, productName);
            return Result.success(summaryPage, "查询库存汇总成功");
        } catch (Exception e) {
            return Result.error(500, "查询库存汇总失败：" + e.getMessage());
        }
    }

    @PostMapping("/adjust")
    @PreAuthorize("hasAuthority('warehouse:inbound:manage') or hasAuthority('warehouse:outbound:manage') or hasAuthority('*')")
    @Operation(summary = "调整门店库存")
    public Result<StoreInventory> adjustStoreInventory(
            @Parameter(description = "库存记录ID") @RequestParam Long inventoryId,
            @Parameter(description = "调整数量（正数为增加，负数为减少）") @RequestParam Integer quantity,
            @Parameter(description = "调整类型（1:入库,2:出库,3:调拨,4:盘点,5:损耗）") @RequestParam Integer type,
            @Parameter(description = "单位成本（分，仅入库调整时有效，不传则沿用当前单位成本）") @RequestParam(required = false) Long unitCost,
            @Parameter(description = "备注") @RequestParam(required = false) String remark) {
        try {
            StoreInventory inventory = storeInventoryService.getById(inventoryId);
            if (inventory == null) {
                return Result.error(404, "库存记录不存在");
            }

            BigDecimal changeQty = new BigDecimal(quantity);

            // 根据调整数量正负调用增加或扣减（P2-1：内部统一写入库存流水）
            if (changeQty.compareTo(BigDecimal.ZERO) > 0) {
                // 入库调整：如果未传单位成本，沿用当前单位成本（保持成本不变）
                Long costToUse = (unitCost != null) ? unitCost : inventory.getUnitCost();
                storeInventoryService.increaseStock(
                        inventory.getStoreId(),
                        inventory.getMaterialId(),
                        inventory.getMaterialName(),
                        changeQty,
                        inventory.getUnit(),
                        costToUse,
                        type,
                        remark);
            } else if (changeQty.compareTo(BigDecimal.ZERO) < 0) {
                storeInventoryService.decreaseStock(
                        inventory.getStoreId(),
                        inventory.getMaterialId(),
                        changeQty.negate(),
                        type,
                        remark);
            } else {
                return Result.error(400, "调整数量不能为0");
            }

            // 重新查询获取调整后的库存
            StoreInventory updated = storeInventoryService.getById(inventoryId);

            return Result.success(updated, "调整门店库存成功");
        } catch (Exception e) {
            return Result.error(500, "调整门店库存失败：" + e.getMessage());
        }
    }

    @GetMapping("/logs")
    @PreAuthorize("hasAuthority('warehouse:inventory:view') or hasAuthority('warehouse:overview:view') or hasAuthority('*')")
    @Operation(summary = "分页查询门店库存日志")
    public Result<IPage<StoreInventoryLog>> getStoreInventoryLogPage(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(description = "门店ID") @RequestParam(required = false) String storeId,
            @Parameter(description = "产品ID") @RequestParam(required = false) Long productId,
            @Parameter(description = "变动类型") @RequestParam(required = false) Integer type) {
        try {
            Page<StoreInventoryLog> pageParam = new Page<>(page, pageSize);
            IPage<StoreInventoryLog> logPage = storeInventoryLogService.getLogPage(pageParam, storeId, productId, type);
            return Result.success(logPage, "查询门店库存日志成功");
        } catch (Exception e) {
            return Result.error(500, "查询门店库存日志失败：" + e.getMessage());
        }
    }
}
