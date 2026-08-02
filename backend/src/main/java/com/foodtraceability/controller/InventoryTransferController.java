package com.foodtraceability.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.InventoryTransferApproveDTO;
import com.foodtraceability.dto.InventoryTransferCreateDTO;
import com.foodtraceability.dto.InventoryTransferUpdateDTO;
import com.foodtraceability.entity.InventoryTransfer;
import com.foodtraceability.service.InventoryTransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 调拨单控制器
 * 处理调拨管理相关的HTTP请求
 */
@RestController
@RequestMapping("/v1/inventory/transfers")
@Tag(name = "库存调拨管理")
public class InventoryTransferController {

    private static final Logger log = LoggerFactory.getLogger(InventoryTransferController.class);

    public InventoryTransferController(InventoryTransferService inventoryTransferService) {
        this.inventoryTransferService = inventoryTransferService;
    }

    private final InventoryTransferService inventoryTransferService;
    
    /**
     * 创建调拨单
     */
    @PostMapping
    @Operation(summary = "创建调拨单")
    @ApiResponse(responseCode = "200", description = "创建成功")
    public Result<InventoryTransfer> createInventoryTransfer(@Valid @RequestBody InventoryTransferCreateDTO createDTO) {
        try {
            InventoryTransfer inventoryTransfer = new InventoryTransfer();
            inventoryTransfer.setFromWarehouseId(createDTO.getFromWarehouseId());
            inventoryTransfer.setToWarehouseId(createDTO.getToWarehouseId());
            inventoryTransfer.setProductId(createDTO.getProductId());
            inventoryTransfer.setProductName(createDTO.getProductName());
            inventoryTransfer.setTransferQuantity(createDTO.getTransferQuantity());
            inventoryTransfer.setRemark(createDTO.getRemark());
            InventoryTransfer createdTransfer = inventoryTransferService.createInventoryTransfer(inventoryTransfer);
            return Result.success(createdTransfer, "创建调拨单成功");
        } catch (Exception e) {
            return Result.error(500, "创建调拨单失败：" + e.getMessage());
        }
    }
    
    /**
     * 更新调拨单
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新调拨单")
    @ApiResponse(responseCode = "200", description = "更新成功")
    public Result<InventoryTransfer> updateInventoryTransfer(
            @Parameter(description = "调拨单ID") @PathVariable("id") Long id,
            @Valid @RequestBody InventoryTransferUpdateDTO updateDTO) {
        try {
            InventoryTransfer inventoryTransfer = new InventoryTransfer();
            inventoryTransfer.setFromWarehouseId(updateDTO.getFromWarehouseId());
            inventoryTransfer.setToWarehouseId(updateDTO.getToWarehouseId());
            inventoryTransfer.setProductId(updateDTO.getProductId());
            inventoryTransfer.setProductName(updateDTO.getProductName());
            inventoryTransfer.setTransferQuantity(updateDTO.getTransferQuantity());
            inventoryTransfer.setRemark(updateDTO.getRemark());
            InventoryTransfer updatedTransfer = inventoryTransferService.updateInventoryTransfer(id, inventoryTransfer);
            return Result.success(updatedTransfer, "更新调拨单成功");
        } catch (Exception e) {
            return Result.error(500, "更新调拨单失败：" + e.getMessage());
        }
    }
    
    /**
     * 根据ID获取调拨单
     */
    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取调拨单")
    @ApiResponse(responseCode = "200", description = "获取成功")
    public Result<InventoryTransfer> getInventoryTransferById(
            @Parameter(description = "调拨单ID") @PathVariable("id") Long id) {
        try {
            InventoryTransfer inventoryTransfer = inventoryTransferService.getInventoryTransferById(id);
            return Result.success(inventoryTransfer, "获取调拨单成功");
        } catch (Exception e) {
            return Result.error(500, "获取调拨单失败：" + e.getMessage());
        }
    }
    
    /**
     * 根据ID删除调拨单
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "根据ID删除调拨单")
    @ApiResponse(responseCode = "200", description = "删除成功")
    public Result<Void> deleteInventoryTransfer(
            @Parameter(description = "调拨单ID") @PathVariable("id") Long id) {
        try {
            inventoryTransferService.deleteInventoryTransfer(id);
            return Result.success(null, "删除调拨单成功");
        } catch (Exception e) {
            return Result.error(500, "删除调拨单失败：" + e.getMessage());
        }
    }
    
    /**
     * 分页查询调拨单列表
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询调拨单列表")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @PreAuthorize("hasAuthority('inventory:query')")
    public Result<com.baomidou.mybatisplus.core.metadata.IPage<InventoryTransfer>> getInventoryTransferPage(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(description = "调拨单号") @RequestParam(required = false) String transferNo,
            @Parameter(description = "调出仓库ID") @RequestParam(required = false) Long fromWarehouseId,
            @Parameter(description = "调入仓库ID") @RequestParam(required = false) Long toWarehouseId,
            @Parameter(description = "状态") @RequestParam(required = false) String status,
            @Parameter(description = "申请开始时间") @RequestParam(required = false) String applyTimeStart,
            @Parameter(description = "申请结束时间") @RequestParam(required = false) String applyTimeEnd) {
        try {
            Page<InventoryTransfer> pageParam = new Page<>(page, pageSize);
            com.baomidou.mybatisplus.core.metadata.IPage<InventoryTransfer> transferPage = 
                    inventoryTransferService.getInventoryTransferPage(pageParam, transferNo, fromWarehouseId, toWarehouseId, status, applyTimeStart, applyTimeEnd);
            return Result.success(transferPage, "查询调拨单列表成功");
        } catch (Exception e) {
            return Result.error(500, "查询调拨单列表失败：" + e.getMessage());
        }
    }
    
    /**
     * 审批调拨单
     */
    @PostMapping("/{id}/approve")
    @Operation(summary = "审批调拨单")
    @ApiResponse(responseCode = "200", description = "审批成功")
    @PreAuthorize("hasAuthority('inventory:approve')")
    public Result<InventoryTransfer> approveInventoryTransfer(
            @Parameter(description = "调拨单ID") @PathVariable("id") Long id,
            @Valid @RequestBody InventoryTransferApproveDTO approveDTO) {
        try {
            InventoryTransfer inventoryTransfer = inventoryTransferService.approveInventoryTransfer(
                    id, approveDTO.getStatus(), approveDTO.getRemark());
            return Result.success(inventoryTransfer, "审批调拨单成功");
        } catch (Exception e) {
            return Result.error(500, "审批调拨单失败：" + e.getMessage());
        }
    }
    
    /**
     * 执行调拨
     */
    @PostMapping("/{id}/execute")
    @Operation(summary = "执行调拨")
    @ApiResponse(responseCode = "200", description = "执行成功")
    public Result<InventoryTransfer> executeInventoryTransfer(
            @Parameter(description = "调拨单ID") @PathVariable("id") Long id) {
        try {
            InventoryTransfer inventoryTransfer = inventoryTransferService.executeInventoryTransfer(id);
            return Result.success(inventoryTransfer, "执行调拨成功");
        } catch (Exception e) {
            log.error("执行调拨失败, transferId={}", id, e);
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getName();
            return Result.error(500, "执行调拨失败：" + msg);
        }
    }
}
