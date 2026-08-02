package com.foodtraceability.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.PurchaseRequestApproveDTO;
import com.foodtraceability.dto.PurchaseRequestCreateDTO;
import com.foodtraceability.dto.PurchaseRequestDTO;
import com.foodtraceability.dto.PurchaseRequestUpdateDTO;
import com.foodtraceability.service.PurchaseRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 采购申请管理控制器
 * 提供采购申请的增删改查、提交、审批、生成订单等接口
 */
@Tag(name = "采购申请管理")
@RestController
@RequestMapping("/v1/purchase/requests")
public class PurchaseRequestController {
    private final PurchaseRequestService purchaseRequestService;

    /**
     * 分页查询采购申请
     */
    @Operation(summary = "分页查询采购申请")
    @GetMapping("/page")
    @PreAuthorize("hasAuthority(\'purchase:request:view\') or hasAuthority(\'*\')")
    public Result<IPage<PurchaseRequestDTO>> getPage(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size, @RequestParam(required = false) String requestNo, @RequestParam(required = false) String status, @RequestParam(required = false) String departmentId, @RequestParam(required = false) String applicantId, @RequestParam(required = false) String departmentName, @RequestParam(required = false) String applicantName, @RequestParam(required = false) String startDate, @RequestParam(required = false) String endDate) {
        IPage<PurchaseRequestDTO> result = purchaseRequestService.getPage(page, size, requestNo, status, departmentId, applicantId, departmentName, applicantName, startDate, endDate);
        return Result.success(result);
    }

    /**
     * 根据ID查询采购申请
     */
    @Operation(summary = "根据ID查询采购申请")
    @GetMapping("/{requestId}")
    @PreAuthorize("hasAuthority(\'purchase:request:view\')")
    public Result<PurchaseRequestDTO> getById(@PathVariable String requestId) {
        PurchaseRequestDTO dto = purchaseRequestService.getById(requestId);
        if (dto == null) {
            return Result.error("采购申请不存在");
        }
        return Result.success(dto);
    }

    /**
     * 创建采购申请
     */
    @Operation(summary = "创建采购申请")
    @PostMapping
    @PreAuthorize("hasAuthority(\'purchase:request:create\') or hasAuthority(\'*\')")
    public Result<PurchaseRequestDTO> create(@Valid @RequestBody PurchaseRequestCreateDTO dto) {
        PurchaseRequestDTO result = purchaseRequestService.create(dto);
        return Result.success(result);
    }

    /**
     * 更新采购申请
     */
    @Operation(summary = "更新采购申请")
    @PutMapping("/{requestId}")
    @PreAuthorize("hasAuthority(\'purchase:request:edit\') or hasAuthority(\'*\')")
    public Result<PurchaseRequestDTO> update(@PathVariable String requestId, @Valid @RequestBody PurchaseRequestUpdateDTO dto) {
        dto.setRequestId(requestId);
        PurchaseRequestDTO result = purchaseRequestService.update(dto);
        return Result.success(result);
    }

    /**
     * 删除采购申请
     */
    @Operation(summary = "删除采购申请")
    @DeleteMapping("/{requestId}")
    @PreAuthorize("hasAuthority(\'purchase:request:delete\') or hasAuthority(\'*\')")
    public Result<Void> delete(@PathVariable String requestId) {
        purchaseRequestService.delete(requestId);
        return Result.success();
    }

    /**
     * 提交采购申请
     */
    @Operation(summary = "提交采购申请")
    @PostMapping("/{requestId}/submit")
    @PreAuthorize("hasAuthority(\'purchase:request:edit\')")
    public Result<Void> submit(@PathVariable String requestId) {
        purchaseRequestService.submit(requestId);
        return Result.success();
    }

    /**
     * 审批采购申请
     */
    @Operation(summary = "审批采购申请")
    @PostMapping("/{requestId}/approve")
    @PreAuthorize("hasAuthority(\'purchase:request:approve\') or hasAuthority(\'*\')")
    public Result<Void> approve(@PathVariable String requestId, @Valid @RequestBody PurchaseRequestApproveDTO dto) {
        purchaseRequestService.approve(requestId, dto.getStatus(), dto.getRemark());
        return Result.success();
    }

    /**
     * 重置驳回次数（A1：管理员解锁被驳回 3 次限制重新提交的申请）
     */
    @Operation(summary = "重置驳回次数")
    @PostMapping("/{requestId}/reset-reject")
    @PreAuthorize("hasAuthority(\'purchase:request:approve\') or hasAuthority(\'*\')")
    public Result<Void> resetRejectCount(@PathVariable String requestId) {
        purchaseRequestService.resetRejectCount(requestId);
        return Result.success();
    }

    /**
     * 生成采购订单
     */
    @Operation(summary = "生成采购订单")
    @PostMapping("/{requestId}/generate-order")
    @PreAuthorize("hasAuthority(\'purchase:request:generate-order\') or hasAuthority(\'*\')")
    public Result<PurchaseRequestDTO> generateOrder(@PathVariable String requestId) {
        PurchaseRequestDTO result = purchaseRequestService.generateOrder(requestId);
        return Result.success(result);
    }

    /**
     * 根据状态查询采购申请
     */
    @Operation(summary = "根据状态查询采购申请")
    @GetMapping("/status/{status}")
    @PreAuthorize("hasAuthority(\'purchase:request:view\')")
    public Result<List<PurchaseRequestDTO>> getByStatus(@PathVariable String status) {
        List<PurchaseRequestDTO> result = purchaseRequestService.getByStatus(status);
        return Result.success(result);
    }

    /**
     * 获取采购申请统计
     */
    @Operation(summary = "获取采购申请统计")
    @GetMapping("/statistics")
    @PreAuthorize("hasAuthority('purchase:request:view')")
    public Result<Map<String, Object>> getStatistics() {
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("draft", purchaseRequestService.countByStatus("draft"));
        statistics.put("pending", purchaseRequestService.countByStatus("pending"));
        statistics.put("approved", purchaseRequestService.countByStatus("approved"));
        statistics.put("rejected", purchaseRequestService.countByStatus("rejected"));
        statistics.put("completed", purchaseRequestService.countByStatus("completed"));
        statistics.put("cancelled", purchaseRequestService.countByStatus("cancelled"));
        return Result.success(statistics);
    }

    /**
     * 获取待处理采购请求数量
     */
    @Operation(summary = "获取待处理采购请求数量")
    @GetMapping("/status/pending")
    @PreAuthorize("hasAuthority('purchase:request:view')")
    public Result<Map<String, Object>> getPendingCount() {
        Map<String, Object> result = new HashMap<>();
        long pendingCount = purchaseRequestService.countByStatus("pending");
        result.put("count", pendingCount);
        return Result.success(result);
    }

    public PurchaseRequestController(final PurchaseRequestService purchaseRequestService) {
        this.purchaseRequestService = purchaseRequestService;
    }
}
