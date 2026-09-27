package com.foodtraceability.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.Result;
import com.foodtraceability.entity.InventoryLog;
import com.foodtraceability.service.InventoryLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


/**
 * 库存消耗控制器
 * S5：写后门已关闭（宪法 §III.7 / 禁区 7：inventory_log 为只读历史表，不提供 create/update/delete API），仅保留查询
 */
@RestController
@RequestMapping("/v1/inventory/consumptions")
@Tag(name = "库存消耗管理")
public class InventoryConsumptionController {
    

    public InventoryConsumptionController(InventoryLogService inventoryLogService) {
        this.inventoryLogService = inventoryLogService;
    }

    private final InventoryLogService inventoryLogService;
    
    /**
     * 分页查询库存消耗记录列表
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询库存消耗记录列表")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @PreAuthorize("hasAuthority('inventory:query')")
    public Result<com.baomidou.mybatisplus.core.metadata.IPage<InventoryLog>> getConsumptionPage(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "分类") @RequestParam(required = false) String category) {
        try {
            Page<InventoryLog> pageParam = new Page<>(page, pageSize);
            // 使用库存日志服务查询消耗记录，operationType为"out"或"consume"
            com.baomidou.mybatisplus.core.metadata.IPage<InventoryLog> consumptionPage = 
                    inventoryLogService.getInventoryLogPage(pageParam, null, null, "out", null, null, null);
            return Result.success(consumptionPage, "查询库存消耗记录列表成功");
        } catch (Exception e) {
            return Result.error(500, "查询库存消耗记录列表失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取单个库存消耗记录详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取单个库存消耗记录详情")
    @ApiResponse(responseCode = "200", description = "获取成功")
    @PreAuthorize("hasAuthority('inventory:query')")
    public Result<InventoryLog> getConsumptionDetail(
            @Parameter(description = "消耗记录ID") @PathVariable("id") Long id) {
        try {
            InventoryLog consumption = inventoryLogService.getInventoryLogById(id);
            return Result.success(consumption, "获取库存消耗记录详情成功");
        } catch (Exception e) {
            return Result.error(500, "获取库存消耗记录详情失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取库存消耗统计数据
     */
    @GetMapping("/stats")
    @Operation(summary = "获取库存消耗统计数据")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @PreAuthorize("hasAuthority('inventory:query')")
    public Result<java.util.Map<String, Object>> getConsumptionStats(
            @Parameter(description = "开始时间") @RequestParam(required = false) String startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false) String endTime,
            @Parameter(description = "分类") @RequestParam(required = false) String category) {
        try {
            java.util.Map<String, Object> stats = inventoryLogService.getConsumptionStats(startTime, endTime, category);
            return Result.success(stats, "查询库存消耗统计数据成功");
        } catch (Exception e) {
            return Result.error(500, "查询库存消耗统计数据失败：" + e.getMessage());
        }
    }
}
