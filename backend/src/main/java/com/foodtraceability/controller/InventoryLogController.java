package com.foodtraceability.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.InventoryLogExportDTO;
import com.foodtraceability.entity.InventoryLog;
import com.foodtraceability.service.InventoryLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 库存日志控制器
 * 处理库存日志管理相关的HTTP请求
 */
@RestController
@RequestMapping("/v1/inventory/logs")
@Tag(name = "库存日志管理")
public class InventoryLogController {
    

    public InventoryLogController(InventoryLogService inventoryLogService) {
        this.inventoryLogService = inventoryLogService;
    }

    private final InventoryLogService inventoryLogService;
    
    /**
     * 创建库存日志
     * 日志通常由系统自动生成，此接口仅供内部调用
     */
    @PostMapping
    @Operation(summary = "创建库存日志")
    @ApiResponse(responseCode = "200", description = "创建成功")
    @PreAuthorize("hasAuthority('inventory:create')")
    public Result<InventoryLog> createInventoryLog(@RequestBody InventoryLog inventoryLog) {
        try {
            InventoryLog createdLog = inventoryLogService.createInventoryLog(inventoryLog);
            return Result.success(createdLog, "创建库存日志成功");
        } catch (Exception e) {
            return Result.error(500, "创建库存日志失败：" + e.getMessage());
        }
    }
    
    /**
     * 根据ID获取库存日志
     */
    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取库存日志")
    @ApiResponse(responseCode = "200", description = "获取成功")
    public Result<InventoryLog> getInventoryLogById(
            @Parameter(description = "库存日志ID") @PathVariable("id") Long id) {
        try {
            InventoryLog inventoryLog = inventoryLogService.getInventoryLogById(id);
            return Result.success(inventoryLog, "获取库存日志成功");
        } catch (Exception e) {
            return Result.error(500, "获取库存日志失败：" + e.getMessage());
        }
    }
    
    /**
     * 分页查询库存日志列表
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询库存日志列表")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @PreAuthorize("hasAuthority('inventory:query')")
    public Result<com.baomidou.mybatisplus.core.metadata.IPage<InventoryLog>> getInventoryLogPage(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(description = "产品ID") @RequestParam(required = false) Long productId,
            @Parameter(description = "仓库ID") @RequestParam(required = false) Long warehouseId,
            @Parameter(description = "操作类型") @RequestParam(required = false) String operationType,
            @Parameter(description = "操作人ID") @RequestParam(required = false) Long operatorId,
            @Parameter(description = "开始时间") @RequestParam(required = false) String startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false) String endTime) {
        try {
            Page<InventoryLog> pageParam = new Page<>(page, pageSize);
            com.baomidou.mybatisplus.core.metadata.IPage<InventoryLog> logPage = 
                    inventoryLogService.getInventoryLogPage(pageParam, productId, warehouseId, operationType, operatorId, startTime, endTime);
            return Result.success(logPage, "查询库存日志列表成功");
        } catch (Exception e) {
            return Result.error(500, "查询库存日志列表失败：" + e.getMessage());
        }
    }
    
    /**
     * 导出库存日志
     */
    @PostMapping("/export")
    @Operation(summary = "导出库存日志")
    @ApiResponse(responseCode = "200", description = "导出成功")
    @PreAuthorize("hasAuthority('inventory:query')")
    public Result<Void> exportInventoryLog(@Valid @RequestBody InventoryLogExportDTO exportDTO) {
        try {
            inventoryLogService.exportInventoryLog(
                    exportDTO.getProductId(),
                    exportDTO.getWarehouseId(),
                    exportDTO.getOperationType(),
                    exportDTO.getOperatorId(),
                    exportDTO.getStartTime(),
                    exportDTO.getEndTime());
            return Result.success(null, "导出库存日志成功");
        } catch (Exception e) {
            return Result.error(500, "导出库存日志失败：" + e.getMessage());
        }
    }
}
