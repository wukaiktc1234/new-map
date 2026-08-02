package com.foodtraceability.controller;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.InventoryCheckCreateDTO;
import com.foodtraceability.entity.InventoryCheck;
import com.foodtraceability.service.InventoryCheckService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 盘点单控制器
 */
@RestController
@RequestMapping("/v1/inventory-checks")
@Tag(name = "盘点管理")
public class InventoryCheckController {

    private final InventoryCheckService checkService;

    public InventoryCheckController(InventoryCheckService checkService) {
        this.checkService = checkService;
    }

    @PostMapping
    @Operation(summary = "创建盘点单")
    @PreAuthorize("hasAuthority('inventory:create')")
    public Result<InventoryCheck> createCheck(@RequestBody InventoryCheckCreateDTO createDTO) {
        try {
            InventoryCheck check = checkService.createCheck(createDTO);
            return Result.success(check, "创建盘点单成功");
        } catch (Exception e) {
            return Result.error(500, "创建盘点单失败：" + e.getMessage());
        }
    }

    @GetMapping
    @Operation(summary = "查询盘点列表")
    @PreAuthorize("hasAuthority('inventory:query')")
    public Result<List<InventoryCheck>> getChecks(
            @Parameter(description = "仓库ID") @RequestParam(required = false) Long warehouseId) {
        try {
            if (warehouseId != null) {
                List<InventoryCheck> checks = checkService.getChecksByWarehouse(warehouseId);
                return Result.success(checks);
            } else {
                var checks = checkService.list();
                return Result.success(checks);
            }
        } catch (Exception e) {
            return Result.error(500, "查询盘点列表失败：" + e.getMessage());
        }
    }

    /**
     * 获取盘点单详情
     * 复用 MyBatis-Plus IService 提供的 getById 方法
     */
    @GetMapping("/{checkId}")
    @Operation(summary = "获取盘点单详情")
    @PreAuthorize("hasAuthority('inventory:query')")
    public Result<InventoryCheck> getCheckById(
            @Parameter(description = "盘点单ID") @PathVariable Long checkId) {
        try {
            InventoryCheck check = checkService.getById(checkId);
            if (check == null) {
                return Result.error(404, "盘点单不存在");
            }
            return Result.success(check);
        } catch (Exception e) {
            return Result.error(500, "查询盘点单详情失败：" + e.getMessage());
        }
    }

    /**
     * 更新盘点单
     * 仅草稿状态（待盘点）允许更新，避免覆盖已审批/已完成的盘点结果
     */
    @PutMapping("/{checkId}")
    @Operation(summary = "更新盘点单")
    @PreAuthorize("hasAuthority('inventory:update')")
    public Result<InventoryCheck> updateCheck(
            @Parameter(description = "盘点单ID") @PathVariable Long checkId,
            @Valid @RequestBody InventoryCheckCreateDTO updateDTO) {
        try {
            InventoryCheck check = checkService.updateCheck(checkId, updateDTO);
            return Result.success(check, "更新盘点单成功");
        } catch (Exception e) {
            return Result.error(500, "更新盘点单失败：" + e.getMessage());
        }
    }

    /**
     * 删除盘点单（逻辑删除）
     * 仅草稿状态允许删除，防止误删已审批/已完成的盘点数据
     */
    @DeleteMapping("/{checkId}")
    @Operation(summary = "删除盘点单")
    @PreAuthorize("hasAuthority('inventory:delete')")
    public Result<Void> deleteCheck(
            @Parameter(description = "盘点单ID") @PathVariable Long checkId) {
        try {
            checkService.deleteCheck(checkId);
            return Result.success(null, "删除盘点单成功");
        } catch (Exception e) {
            return Result.error(500, "删除盘点单失败：" + e.getMessage());
        }
    }

    /**
     * 审批盘点单
     * 接收前端 { approved: Boolean, remark: String } 请求体
     * 审批通过：状态置为已审核(2)；驳回：状态回退为待盘点(0)
     */
    @PostMapping("/{checkId}/approve")
    @Operation(summary = "审批盘点单")
    @PreAuthorize("hasAuthority('inventory:approve')")
    public Result<InventoryCheck> approveCheck(
            @Parameter(description = "盘点单ID") @PathVariable Long checkId,
            @RequestBody Map<String, Object> body) {
        try {
            InventoryCheck check = checkService.getById(checkId);
            if (check == null) {
                return Result.error(404, "盘点单不存在");
            }
            Object approvedRaw = body.get("approved");
            boolean approved = Boolean.TRUE.equals(approvedRaw);
            // 兼容 remark / approveComment 两种字段名
            Object commentRaw = body.get("remark");
            if (commentRaw == null) {
                commentRaw = body.get("approveComment");
            }
            String approveComment = commentRaw == null ? "" : commentRaw.toString();

            check.setCheckStatus(approved ? 2 : 0);
            check.setApproveTime(LocalDateTime.now());
            // 备注：追加审批意见，便于追溯
            String existingRemark = check.getRemark() == null ? "" : check.getRemark();
            if (!approveComment.isEmpty()) {
                check.setRemark(existingRemark + (existingRemark.isEmpty() ? "" : " | ") + "审批意见：" + approveComment);
            }
            check.setUpdateTime(LocalDateTime.now());
            checkService.updateById(check);
            return Result.success(check, approved ? "审批通过" : "已驳回");
        } catch (Exception e) {
            return Result.error(500, "审批盘点单失败：" + e.getMessage());
        }
    }

    /**
     * 提交盘点条目
     * 前端请求体：{ items: [...] }
     * 将盘点条目持久化到 inventory_check_item 表，并更新盘点单状态为盘点中(1)
     */
    @PutMapping("/{checkId}/items")
    @Operation(summary = "提交盘点条目")
    @PreAuthorize("hasAuthority('inventory:update')")
    @SuppressWarnings("unchecked")
    public Result<InventoryCheck> submitCheckItems(
            @Parameter(description = "盘点单ID") @PathVariable Long checkId,
            @RequestBody Map<String, Object> body) {
        try {
            // 提取前端提交的盘点条目列表
            Object itemsRaw = body.get("items");
            List<Map<String, Object>> items = itemsRaw instanceof List
                    ? (List<Map<String, Object>>) itemsRaw
                    : List.of();
            // 委托 Service 层完成条目持久化和状态更新
            InventoryCheck check = checkService.saveCheckItems(checkId, items);
            return Result.success(check, "提交盘点条目成功");
        } catch (Exception e) {
            return Result.error(500, "提交盘点条目失败：" + e.getMessage());
        }
    }

    /**
     * 完成盘点
     * 更新盘点单状态为已完成(3)，并记录完成日期
     */
    @PostMapping("/{checkId}/complete")
    @Operation(summary = "完成盘点")
    @PreAuthorize("hasAuthority('inventory:execute')")
    public Result<InventoryCheck> completeCheck(
            @Parameter(description = "盘点单ID") @PathVariable Long checkId) {
        try {
            InventoryCheck check = checkService.getById(checkId);
            if (check == null) {
                return Result.error(404, "盘点单不存在");
            }
            check.setCheckStatus(3);
            check.setCompleteDate(LocalDate.now());
            check.setUpdateTime(LocalDateTime.now());
            checkService.updateById(check);
            return Result.success(check, "完成盘点成功");
        } catch (Exception e) {
            return Result.error(500, "完成盘点失败：" + e.getMessage());
        }
    }
}
