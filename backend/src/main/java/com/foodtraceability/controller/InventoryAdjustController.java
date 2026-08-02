package com.foodtraceability.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.InventoryAdjustApproveDTO;
import com.foodtraceability.dto.InventoryAdjustCreateDTO;
import com.foodtraceability.entity.InventoryAdjust;
import com.foodtraceability.service.InventoryAdjustService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 库存调整单控制器
 * 处理库存调整管理相关的HTTP请求
 *
 * 端点说明：
 * - GET  /v1/inventory/adjustments/page         分页查询调整单列表
 * - GET  /v1/inventory/adjustments/{adjustCode}  获取调整单详情
 * - POST /v1/inventory/adjustments               创建调整单
 * - PUT  /v1/inventory/adjustments/{adjustCode}/approve  审批调整单
 * - PUT  /v1/inventory/adjustments/{adjustCode}/execute  执行调整
 */
@RestController
@RequestMapping("/v1/inventory/adjustments")
@Tag(name = "库存调整管理", description = "库存调整单的创建、审批、执行等接口")
public class InventoryAdjustController {

    private final InventoryAdjustService inventoryAdjustService;

    public InventoryAdjustController(InventoryAdjustService inventoryAdjustService) {
        this.inventoryAdjustService = inventoryAdjustService;
    }

    /**
     * 分页查询库存调整单列表
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询库存调整单列表")
    @ApiResponse(responseCode = "200", description = "查询成功")
    public Result<IPage<Map<String, Object>>> getAdjustPage(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "调整类型") @RequestParam(required = false) String adjustType,
            @Parameter(description = "状态") @RequestParam(required = false) String status,
            @Parameter(description = "仓库ID") @RequestParam(required = false) String warehouseId,
            @Parameter(description = "开始日期") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) String endDate,
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword) {
        try {
            Page<InventoryAdjust> pageParam = new Page<>(page, size);
            IPage<Map<String, Object>> result = inventoryAdjustService.getAdjustPage(
                    pageParam, adjustType, status, warehouseId, keyword, startDate, endDate);
            return Result.success(result, "查询库存调整单列表成功");
        } catch (Exception e) {
            return Result.error(500, "查询库存调整单列表失败：" + e.getMessage());
        }
    }

    /**
     * 根据调整单号获取调整单详情
     */
    @GetMapping("/{adjustCode}")
    @Operation(summary = "根据调整单号获取调整单详情")
    @ApiResponse(responseCode = "200", description = "获取成功")
    public Result<Map<String, Object>> getAdjustByCode(
            @Parameter(description = "调整单号") @PathVariable("adjustCode") String adjustCode) {
        try {
            Map<String, Object> adjust = inventoryAdjustService.getAdjustByCode(adjustCode);
            if (adjust == null) {
                return Result.error(404, "调整单不存在：" + adjustCode);
            }
            return Result.success(adjust, "获取调整单详情成功");
        } catch (Exception e) {
            return Result.error(500, "获取调整单详情失败：" + e.getMessage());
        }
    }

    /**
     * 创建库存调整单
     */
    @PostMapping
    @Operation(summary = "创建库存调整单")
    @ApiResponse(responseCode = "200", description = "创建成功")
    public Result<Map<String, Object>> createAdjust(@Valid @RequestBody InventoryAdjustCreateDTO createDTO) {
        try {
            InventoryAdjust adjust = new InventoryAdjust();
            adjust.setAdjustType(createDTO.getAdjustType());
            adjust.setWarehouseId(createDTO.getWarehouseId());
            adjust.setReferenceCheckCode(createDTO.getReferenceCheckCode());
            adjust.setAdjustReason(createDTO.getAdjustReason());
            adjust.setReferenceNo(createDTO.getReferenceNo());
            adjust.setRemark(createDTO.getRemark());

            // 参与部门列表转JSON字符串
            if (createDTO.getParticipatingDepts() != null && !createDTO.getParticipatingDepts().isEmpty()) {
                adjust.setParticipatingDepts(createDTO.getParticipatingDepts().toString());
            }

            // 转换明细DTO为Map列表
            List<Map<String, Object>> items = new ArrayList<>();
            for (InventoryAdjustCreateDTO.AdjustItemDTO itemDTO : createDTO.getItems()) {
                Map<String, Object> itemMap = new HashMap<>();
                itemMap.put("materialId", itemDTO.getMaterialId());
                itemMap.put("materialName", itemDTO.getMaterialName());
                itemMap.put("specification", itemDTO.getSpecification());
                itemMap.put("unit", itemDTO.getUnit());
                itemMap.put("beforeQuantity", itemDTO.getBeforeQuantity());
                itemMap.put("adjustQuantity", itemDTO.getAdjustQuantity());
                itemMap.put("batchNo", itemDTO.getBatchNo());
                itemMap.put("unitCost", itemDTO.getUnitCost());
                // 计算调整金额（元）
                if (itemDTO.getUnitCost() != null && itemDTO.getAdjustQuantity() != null) {
                    BigDecimal unitCost = new BigDecimal(itemDTO.getUnitCost());
                    BigDecimal adjustAmount = unitCost.multiply(itemDTO.getAdjustQuantity());
                    itemMap.put("adjustAmount", adjustAmount.toString());
                }
                itemMap.put("reason", itemDTO.getReason());
                items.add(itemMap);
            }

            InventoryAdjust created = inventoryAdjustService.createAdjust(adjust, items);

            // 返回详情格式
            Map<String, Object> result = inventoryAdjustService.getAdjustByCode(created.getAdjustCode());
            return Result.success(result, "创建库存调整单成功");
        } catch (Exception e) {
            return Result.error(500, "创建库存调整单失败：" + e.getMessage());
        }
    }

    /**
     * 更新库存调整单
     * 仅草稿状态（pending）允许更新，避免修改已审批/已执行的调整单
     */
    @PutMapping("/{adjustCode}")
    @Operation(summary = "更新库存调整单")
    @ApiResponse(responseCode = "200", description = "更新成功")
    @PreAuthorize("hasAuthority('inventory:update')")
    public Result<Map<String, Object>> updateAdjust(
            @Parameter(description = "调整单号") @PathVariable("adjustCode") String adjustCode,
            @Valid @RequestBody InventoryAdjustCreateDTO updateDTO) {
        try {
            inventoryAdjustService.updateAdjust(adjustCode, updateDTO);
            Map<String, Object> result = inventoryAdjustService.getAdjustByCode(adjustCode);
            return Result.success(result, "更新库存调整单成功");
        } catch (Exception e) {
            return Result.error(500, "更新库存调整单失败：" + e.getMessage());
        }
    }

    /**
     * 删除库存调整单（逻辑删除）
     * 仅草稿状态允许删除，防止误删已审批/已执行的调整单
     */
    @DeleteMapping("/{adjustCode}")
    @Operation(summary = "删除库存调整单")
    @ApiResponse(responseCode = "200", description = "删除成功")
    @PreAuthorize("hasAuthority('inventory:delete')")
    public Result<Void> deleteAdjust(
            @Parameter(description = "调整单号") @PathVariable("adjustCode") String adjustCode) {
        try {
            inventoryAdjustService.deleteAdjust(adjustCode);
            return Result.success(null, "删除库存调整单成功");
        } catch (Exception e) {
            return Result.error(500, "删除库存调整单失败：" + e.getMessage());
        }
    }

    /**
     * 审批库存调整单
     */
    @PutMapping("/{adjustCode}/approve")
    @Operation(summary = "审批库存调整单")
    @ApiResponse(responseCode = "200", description = "审批成功")
    @PreAuthorize("hasAuthority('inventory:approve')")
    public Result<Void> approveAdjust(
            @Parameter(description = "调整单号") @PathVariable("adjustCode") String adjustCode,
            @Valid @RequestBody InventoryAdjustApproveDTO approveDTO) {
        try {
            inventoryAdjustService.approveAdjust(adjustCode, approveDTO.getApproved(), approveDTO.getOpinion());
            return Result.success(null, "审批库存调整单成功");
        } catch (Exception e) {
            return Result.error(500, "审批库存调整单失败：" + e.getMessage());
        }
    }

    /**
     * 执行库存调整
     */
    @PutMapping("/{adjustCode}/execute")
    @Operation(summary = "执行库存调整")
    @ApiResponse(responseCode = "200", description = "执行成功")
    public Result<Void> executeAdjust(
            @Parameter(description = "调整单号") @PathVariable("adjustCode") String adjustCode) {
        try {
            inventoryAdjustService.executeAdjust(adjustCode);
            return Result.success(null, "执行库存调整成功");
        } catch (Exception e) {
            return Result.error(500, "执行库存调整失败：" + e.getMessage());
        }
    }
}
