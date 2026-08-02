package com.foodtraceability.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.InventoryWarningCreateDTO;
import com.foodtraceability.dto.InventoryWarningUpdateDTO;
import com.foodtraceability.dto.PurchaseOrderCreateFromWarningDTO;
import com.foodtraceability.dto.PurchaseSuggestionVO;
import com.foodtraceability.entity.InventoryWarning;
import com.foodtraceability.service.InventoryWarningService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 库存预警控制器
 * 处理库存预警相关的HTTP请求
 */
@RestController
@RequestMapping("/v1/inventory/warnings")
@Tag(name = "库存预警管理")
public class InventoryWarningController {
    

    public InventoryWarningController(InventoryWarningService inventoryWarningService) {
        this.inventoryWarningService = inventoryWarningService;
    }

    private final InventoryWarningService inventoryWarningService;
    
    /**
     * 创建库存预警
     */
    @PostMapping
    @Operation(summary = "创建库存预警")
    @ApiResponse(responseCode = "200", description = "创建成功")
    @PreAuthorize("hasAuthority('inventory:create')")
    public Result<InventoryWarning> createWarning(@Valid @RequestBody InventoryWarningCreateDTO createDTO) {
        try {
            InventoryWarning warning = new InventoryWarning();
            warning.setInventoryId(createDTO.getInventoryId());
            warning.setProductId(createDTO.getProductId());
            warning.setProductName(createDTO.getProductName());
            warning.setWarehouseId(createDTO.getWarehouseId());
            warning.setWarehouseName(createDTO.getWarehouseName());
            warning.setCurrentStock(createDTO.getCurrentStock());
            warning.setSafeStock(createDTO.getSafeStock());
            warning.setWarningType(createDTO.getWarningType());
            warning.setWarningLevel(createDTO.getWarningLevel());
            InventoryWarning createdWarning = inventoryWarningService.createWarning(warning);
            return Result.success(createdWarning, "创建库存预警成功");
        } catch (Exception e) {
            return Result.error(500, "创建库存预警失败：" + e.getMessage());
        }
    }
    
    /**
     * 更新库存预警
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新库存预警")
    @ApiResponse(responseCode = "200", description = "更新成功")
    @PreAuthorize("hasAuthority('inventory:update')")
    public Result<InventoryWarning> updateWarning(
            @Parameter(description = "预警ID") @PathVariable("id") Long id,
            @Valid @RequestBody InventoryWarningUpdateDTO updateDTO) {
        try {
            InventoryWarning warning = new InventoryWarning();
            warning.setInventoryId(updateDTO.getInventoryId());
            warning.setProductId(updateDTO.getProductId());
            warning.setProductName(updateDTO.getProductName());
            warning.setWarehouseId(updateDTO.getWarehouseId());
            warning.setWarehouseName(updateDTO.getWarehouseName());
            warning.setCurrentStock(updateDTO.getCurrentStock());
            warning.setSafeStock(updateDTO.getSafeStock());
            warning.setWarningType(updateDTO.getWarningType());
            warning.setWarningLevel(updateDTO.getWarningLevel());
            warning.setStatus(updateDTO.getStatus());
            InventoryWarning updatedWarning = inventoryWarningService.updateWarning(id, warning);
            return Result.success(updatedWarning, "更新库存预警成功");
        } catch (Exception e) {
            return Result.error(500, "更新库存预警失败：" + e.getMessage());
        }
    }
    
    /**
     * 根据ID获取库存预警
     */
    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取库存预警")
    @ApiResponse(responseCode = "200", description = "获取成功")
    @PreAuthorize("hasAuthority('inventory:query')")
    public Result<InventoryWarning> getWarningById(
            @Parameter(description = "预警ID") @PathVariable("id") Long id) {
        try {
            InventoryWarning warning = inventoryWarningService.getWarningById(id);
            return Result.success(warning, "获取库存预警成功");
        } catch (Exception e) {
            return Result.error(500, "获取库存预警失败：" + e.getMessage());
        }
    }
    
    /**
     * 根据ID删除库存预警
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "根据ID删除库存预警")
    @ApiResponse(responseCode = "200", description = "删除成功")
    @PreAuthorize("hasAuthority('inventory:delete')")
    public Result<Void> deleteWarning(
            @Parameter(description = "预警ID") @PathVariable("id") Long id) {
        try {
            inventoryWarningService.deleteWarning(id);
            return Result.success(null, "删除库存预警成功");
        } catch (Exception e) {
            return Result.error(500, "删除库存预警失败：" + e.getMessage());
        }
    }
    
    /**
     * 分页查询库存预警列表
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询库存预警列表")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @PreAuthorize("hasAuthority('inventory:query')")
    public Result<com.baomidou.mybatisplus.core.metadata.IPage<InventoryWarning>> getWarningPage(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(description = "仓库ID") @RequestParam(required = false) Long warehouseId,
            @Parameter(description = "预警级别") @RequestParam(required = false) Integer warningLevel,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status) {
        try {
            Page<InventoryWarning> pageParam = new Page<>(page, pageSize);
            com.baomidou.mybatisplus.core.metadata.IPage<InventoryWarning> warningPage = 
                    inventoryWarningService.getWarningPage(pageParam, warehouseId, warningLevel, status);
            return Result.success(warningPage, "查询库存预警列表成功");
        } catch (Exception e) {
            return Result.error(500, "查询库存预警列表失败：" + e.getMessage());
        }
    }
    
    /**
     * 处理库存预警
     */
    @PutMapping("/{id}/handle")
    @Operation(summary = "处理库存预警")
    @ApiResponse(responseCode = "200", description = "处理成功")
    public Result<InventoryWarning> handleWarning(
            @Parameter(description = "预警ID") @PathVariable("id") Long id,
            @Parameter(description = "状态") @RequestParam Integer status,
            @Parameter(description = "处理人") @RequestParam String handler,
            @Parameter(description = "处理备注") @RequestParam(required = false) String handleRemark) {
        try {
            InventoryWarning warning = inventoryWarningService.handleWarning(id, status, handler, handleRemark);
            return Result.success(warning, "处理库存预警成功");
        } catch (Exception e) {
            return Result.error(500, "处理库存预警失败：" + e.getMessage());
        }
    }
    
    /**
     * 自动生成库存预警
     */
    @PostMapping("/generate")
    @Operation(summary = "自动生成库存预警")
    @ApiResponse(responseCode = "200", description = "生成成功")
    public Result<Void> generateWarnings() {
        try {
            inventoryWarningService.autoGenerateWarnings();
            return Result.success(null, "自动生成库存预警成功");
        } catch (Exception e) {
            return Result.error(500, "自动生成库存预警失败：" + e.getMessage());
        }
    }
    
    /**
     * 生成采购建议
     */
    @GetMapping("/purchase-suggestions")
    @Operation(summary = "生成采购建议")
    @ApiResponse(responseCode = "200", description = "生成成功")
    @PreAuthorize("hasAuthority('inventory:query')")
    public Result<List<PurchaseSuggestionVO>> generatePurchaseSuggestions() {
        try {
            @SuppressWarnings("unchecked")
            List<PurchaseSuggestionVO> purchaseSuggestions = (List<PurchaseSuggestionVO>) inventoryWarningService.generatePurchaseSuggestions();
            return Result.success(purchaseSuggestions, "生成采购建议成功");
        } catch (Exception e) {
            return Result.error(500, "生成采购建议失败：" + e.getMessage());
        }
    }
    
    /**
     * 创建采购单
     */
    @PostMapping("/create-purchase-order")
    @Operation(summary = "创建采购单")
    @ApiResponse(responseCode = "200", description = "创建成功")
    @PreAuthorize("hasAuthority('inventory:create')")
    public Result<Object> createPurchaseOrder(@Valid @RequestBody PurchaseOrderCreateFromWarningDTO purchaseDTO) {
        try {
            Object purchaseOrder = inventoryWarningService.createPurchaseOrder(purchaseDTO);
            return Result.success(purchaseOrder, "创建采购单成功");
        } catch (Exception e) {
            return Result.error(500, "创建采购单失败：" + e.getMessage());
        }
    }
}
