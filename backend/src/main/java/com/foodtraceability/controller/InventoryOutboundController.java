package com.foodtraceability.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.InventoryOutboundApproveDTO;
import com.foodtraceability.dto.InventoryOutboundCreateDTO;
import com.foodtraceability.entity.InventoryOutbound;
import com.foodtraceability.service.InventoryOutboundService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 库存出库单控制器
 * 处理库存出库管理相关的HTTP请求
 *
 * 端点说明：
 * - GET  /v1/inventory/outbounds/page           分页查询出库单列表
 * - GET  /v1/inventory/outbounds/{outboundCode}  获取出库单详情
 * - POST /v1/inventory/outbounds                 创建出库单
 * - PUT  /v1/inventory/outbounds/{outboundCode}/approve  审批出库单
 * - PUT  /v1/inventory/outbounds/{outboundCode}/execute  执行出库
 */
@RestController
@RequestMapping("/v1/inventory/outbounds")
@Tag(name = "库存出库管理", description = "库存出库单的创建、审批、执行等接口")
public class InventoryOutboundController {

    private final InventoryOutboundService inventoryOutboundService;

    public InventoryOutboundController(InventoryOutboundService inventoryOutboundService) {
        this.inventoryOutboundService = inventoryOutboundService;
    }

    /**
     * 分页查询库存出库单列表
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询库存出库单列表")
    @ApiResponse(responseCode = "200", description = "查询成功")
    public Result<IPage<Map<String, Object>>> getOutboundPage(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "出库类型") @RequestParam(required = false) String outboundType,
            @Parameter(description = "状态") @RequestParam(required = false) String status,
            @Parameter(description = "仓库ID") @RequestParam(required = false) String warehouseId,
            @Parameter(description = "开始日期") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) String endDate,
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword) {
        try {
            Page<InventoryOutbound> pageParam = new Page<>(page, size);
            IPage<Map<String, Object>> result = inventoryOutboundService.getOutboundPage(
                    pageParam, outboundType, status, warehouseId, keyword, startDate, endDate);
            return Result.success(result, "查询库存出库单列表成功");
        } catch (Exception e) {
            return Result.error(500, "查询库存出库单列表失败：" + e.getMessage());
        }
    }

    /**
     * 根据出库单号获取出库单详情
     */
    @GetMapping("/{outboundCode}")
    @Operation(summary = "根据出库单号获取出库单详情")
    @ApiResponse(responseCode = "200", description = "获取成功")
    public Result<Map<String, Object>> getOutboundByCode(
            @Parameter(description = "出库单号") @PathVariable("outboundCode") String outboundCode) {
        try {
            Map<String, Object> outbound = inventoryOutboundService.getOutboundByCode(outboundCode);
            if (outbound == null) {
                return Result.error(404, "出库单不存在：" + outboundCode);
            }
            return Result.success(outbound, "获取出库单详情成功");
        } catch (Exception e) {
            return Result.error(500, "获取出库单详情失败：" + e.getMessage());
        }
    }

    /**
     * 创建库存出库单
     */
    @PostMapping
    @Operation(summary = "创建库存出库单")
    @ApiResponse(responseCode = "200", description = "创建成功")
    public Result<Map<String, Object>> createOutbound(@Valid @RequestBody InventoryOutboundCreateDTO createDTO) {
        try {
            InventoryOutbound outbound = new InventoryOutbound();
            outbound.setOutboundType(createDTO.getOutboundType());
            outbound.setWarehouseId(createDTO.getWarehouseId());
            outbound.setTargetId(createDTO.getTargetId());
            outbound.setTargetName(createDTO.getTargetName());
            outbound.setReferenceNo(createDTO.getReferenceNo());
            outbound.setOutboundDate(LocalDate.parse(createDTO.getOutboundDate()));
            outbound.setRemark(createDTO.getRemark());

            // 转换明细DTO为Map列表
            List<Map<String, Object>> items = new ArrayList<>();
            for (InventoryOutboundCreateDTO.OutboundItemDTO itemDTO : createDTO.getItems()) {
                Map<String, Object> itemMap = new HashMap<>();
                itemMap.put("materialId", itemDTO.getMaterialId());
                itemMap.put("requestQuantity", itemDTO.getRequestQuantity());
                itemMap.put("remark", itemDTO.getRemark());
                items.add(itemMap);
            }

            InventoryOutbound created = inventoryOutboundService.createOutbound(outbound, items);

            // 返回详情格式
            Map<String, Object> result = inventoryOutboundService.getOutboundByCode(created.getOutboundCode());
            return Result.success(result, "创建库存出库单成功");
        } catch (Exception e) {
            return Result.error(500, "创建库存出库单失败：" + e.getMessage());
        }
    }

    /**
     * 更新库存出库单
     * 仅草稿状态（pending）允许更新，避免修改已审批/已执行的出库单
     */
    @PutMapping("/{outboundCode}")
    @Operation(summary = "更新库存出库单")
    @ApiResponse(responseCode = "200", description = "更新成功")
    @PreAuthorize("hasAuthority('inventory:update')")
    public Result<Map<String, Object>> updateOutbound(
            @Parameter(description = "出库单号") @PathVariable("outboundCode") String outboundCode,
            @Valid @RequestBody InventoryOutboundCreateDTO updateDTO) {
        try {
            inventoryOutboundService.updateOutbound(outboundCode, updateDTO);
            Map<String, Object> result = inventoryOutboundService.getOutboundByCode(outboundCode);
            return Result.success(result, "更新库存出库单成功");
        } catch (Exception e) {
            return Result.error(500, "更新库存出库单失败：" + e.getMessage());
        }
    }

    /**
     * 删除库存出库单（逻辑删除）
     * 仅草稿状态允许删除，防止误删已审批/已执行的出库单
     */
    @DeleteMapping("/{outboundCode}")
    @Operation(summary = "删除库存出库单")
    @ApiResponse(responseCode = "200", description = "删除成功")
    @PreAuthorize("hasAuthority('inventory:delete')")
    public Result<Void> deleteOutbound(
            @Parameter(description = "出库单号") @PathVariable("outboundCode") String outboundCode) {
        try {
            inventoryOutboundService.deleteOutbound(outboundCode);
            return Result.success(null, "删除库存出库单成功");
        } catch (Exception e) {
            return Result.error(500, "删除库存出库单失败：" + e.getMessage());
        }
    }

    /**
     * 审批库存出库单
     */
    @PutMapping("/{outboundCode}/approve")
    @Operation(summary = "审批库存出库单")
    @ApiResponse(responseCode = "200", description = "审批成功")
    public Result<Void> approveOutbound(
            @Parameter(description = "出库单号") @PathVariable("outboundCode") String outboundCode,
            @Valid @RequestBody InventoryOutboundApproveDTO approveDTO) {
        try {
            inventoryOutboundService.approveOutbound(outboundCode, approveDTO.getApproved(), approveDTO.getOpinion());
            return Result.success(null, "审批库存出库单成功");
        } catch (Exception e) {
            return Result.error(500, "审批库存出库单失败：" + e.getMessage());
        }
    }

    /**
     * 执行出库（确认出库）
     */
    @PutMapping("/{outboundCode}/execute")
    @Operation(summary = "执行出库")
    @ApiResponse(responseCode = "200", description = "执行成功")
    @PreAuthorize("hasAuthority('inventory:execute')")
    public Result<Void> executeOutbound(
            @Parameter(description = "出库单号") @PathVariable("outboundCode") String outboundCode) {
        try {
            inventoryOutboundService.executeOutbound(outboundCode);
            return Result.success(null, "执行出库成功");
        } catch (Exception e) {
            return Result.error(500, "执行出库失败：" + e.getMessage());
        }
    }
}
