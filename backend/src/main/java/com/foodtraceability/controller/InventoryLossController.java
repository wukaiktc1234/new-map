package com.foodtraceability.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.InventoryLossApproveDTO;
import com.foodtraceability.dto.InventoryLossCreateDTO;
import com.foodtraceability.dto.InventoryLossUpdateDTO;
import com.foodtraceability.entity.InventoryLoss;
import com.foodtraceability.service.InventoryLossService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 报损单控制器
 * 处理报损管理相关的HTTP请求
 */
@RestController
@RequestMapping("/v1/inventory/losses")
@Tag(name = "库存报损管理")
public class InventoryLossController {
    

    public InventoryLossController(InventoryLossService inventoryLossService) {
        this.inventoryLossService = inventoryLossService;
    }

    private final InventoryLossService inventoryLossService;
    
    /**
     * 创建报损单
     */
    @PostMapping
    @Operation(summary = "创建报损单")
    @ApiResponse(responseCode = "200", description = "创建成功")
    public Result<InventoryLoss> createInventoryLoss(@Valid @RequestBody InventoryLossCreateDTO createDTO) {
        try {
            InventoryLoss inventoryLoss = new InventoryLoss();
            inventoryLoss.setWarehouseId(createDTO.getWarehouseId());
            inventoryLoss.setLossType(createDTO.getLossType());
            inventoryLoss.setTotalQuantity(createDTO.getTotalQuantity());
            inventoryLoss.setTotalAmount(createDTO.getTotalAmount());
            inventoryLoss.setReason(createDTO.getReason());
            InventoryLoss createdLoss = inventoryLossService.createInventoryLoss(inventoryLoss);
            return Result.success(createdLoss, "创建报损单成功");
        } catch (Exception e) {
            return Result.error(500, "创建报损单失败：" + e.getMessage());
        }
    }
    
    /**
     * 更新报损单
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新报损单")
    @ApiResponse(responseCode = "200", description = "更新成功")
    public Result<InventoryLoss> updateInventoryLoss(
            @Parameter(description = "报损单ID") @PathVariable("id") Long id,
            @Valid @RequestBody InventoryLossUpdateDTO updateDTO) {
        try {
            InventoryLoss inventoryLoss = new InventoryLoss();
            inventoryLoss.setWarehouseId(updateDTO.getWarehouseId());
            inventoryLoss.setLossType(updateDTO.getLossType());
            inventoryLoss.setTotalQuantity(updateDTO.getTotalQuantity());
            inventoryLoss.setTotalAmount(updateDTO.getTotalAmount());
            inventoryLoss.setReason(updateDTO.getReason());
            InventoryLoss updatedLoss = inventoryLossService.updateInventoryLoss(id, inventoryLoss);
            return Result.success(updatedLoss, "更新报损单成功");
        } catch (Exception e) {
            return Result.error(500, "更新报损单失败：" + e.getMessage());
        }
    }
    
    /**
     * 根据ID获取报损单
     */
    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取报损单")
    @ApiResponse(responseCode = "200", description = "获取成功")
    public Result<InventoryLoss> getInventoryLossById(
            @Parameter(description = "报损单ID") @PathVariable("id") Long id) {
        try {
            InventoryLoss inventoryLoss = inventoryLossService.getInventoryLossById(id);
            return Result.success(inventoryLoss, "获取报损单成功");
        } catch (Exception e) {
            return Result.error(500, "获取报损单失败：" + e.getMessage());
        }
    }
    
    /**
     * 根据ID删除报损单
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "根据ID删除报损单")
    @ApiResponse(responseCode = "200", description = "删除成功")
    public Result<Void> deleteInventoryLoss(
            @Parameter(description = "报损单ID") @PathVariable("id") Long id) {
        try {
            inventoryLossService.deleteInventoryLoss(id);
            return Result.success(null, "删除报损单成功");
        } catch (Exception e) {
            return Result.error(500, "删除报损单失败：" + e.getMessage());
        }
    }
    
    /**
     * 分页查询报损单列表
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询报损单列表")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @PreAuthorize("hasAuthority('inventory:query')")
    public Result<com.baomidou.mybatisplus.core.metadata.IPage<InventoryLoss>> getInventoryLossPage(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(description = "报损单号") @RequestParam(required = false) String lossNo,
            @Parameter(description = "仓库ID") @RequestParam(required = false) Long warehouseId,
            @Parameter(description = "状态") @RequestParam(required = false) String status,
            @Parameter(description = "申请开始时间") @RequestParam(required = false) String applyTimeStart,
            @Parameter(description = "申请结束时间") @RequestParam(required = false) String applyTimeEnd) {
        try {
            Page<InventoryLoss> pageParam = new Page<>(page, pageSize);
            com.baomidou.mybatisplus.core.metadata.IPage<InventoryLoss> lossPage = 
                    inventoryLossService.getInventoryLossPage(pageParam, lossNo, warehouseId, status, applyTimeStart, applyTimeEnd);
            return Result.success(lossPage, "查询报损单列表成功");
        } catch (Exception e) {
            return Result.error(500, "查询报损单列表失败：" + e.getMessage());
        }
    }
    
    /**
     * 审批报损单
     */
    @PostMapping("/{id}/approve")
    @Operation(summary = "审批报损单")
    @ApiResponse(responseCode = "200", description = "审批成功")
    public Result<InventoryLoss> approveInventoryLoss(
            @Parameter(description = "报损单ID") @PathVariable("id") Long id,
            @Valid @RequestBody InventoryLossApproveDTO approveDTO) {
        try {
            InventoryLoss inventoryLoss = inventoryLossService.approveInventoryLoss(
                    id, approveDTO.getStatus(), approveDTO.getRemark());
            return Result.success(inventoryLoss, "审批报损单成功");
        } catch (Exception e) {
            return Result.error(500, "审批报损单失败：" + e.getMessage());
        }
    }
    
    /**
     * 处理报损
     */
    @PostMapping("/{id}/process")
    @Operation(summary = "处理报损")
    @ApiResponse(responseCode = "200", description = "处理成功")
    @PreAuthorize("hasAuthority('inventory:execute')")
    public Result<InventoryLoss> processInventoryLoss(
            @Parameter(description = "报损单ID") @PathVariable("id") Long id) {
        try {
            InventoryLoss inventoryLoss = inventoryLossService.processInventoryLoss(id);
            return Result.success(inventoryLoss, "处理报损成功");
        } catch (Exception e) {
            return Result.error(500, "处理报损失败：" + e.getMessage());
        }
    }
}
