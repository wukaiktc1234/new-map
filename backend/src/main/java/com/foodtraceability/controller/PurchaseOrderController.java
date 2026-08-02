package com.foodtraceability.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.PurchaseOrderCreateDTO;
import com.foodtraceability.dto.PurchaseOrderQueryDTO;
import com.foodtraceability.dto.PurchaseOrderUpdateDTO;
import com.foodtraceability.dto.approval.ApprovalActionDTO;
import com.foodtraceability.entity.PurchaseOrder;
import com.foodtraceability.service.PurchaseOrderService;
import com.foodtraceability.service.approval.ApprovalWorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 采购订单控制器
 * 提供采购订单的全生命周期管理API接口
 */
@Tag(name = "采购订单管理", description = "采购订单相关接口")
@RestController
@RequestMapping("/v1/purchase/orders")
public class PurchaseOrderController {

    private static final Logger log = LoggerFactory.getLogger(PurchaseOrderController.class);

    private final PurchaseOrderService purchaseOrderService;
    private final ApprovalWorkflowService approvalWorkflowService;

    public PurchaseOrderController(PurchaseOrderService purchaseOrderService,
                                   ApprovalWorkflowService approvalWorkflowService) {
        this.purchaseOrderService = purchaseOrderService;
        this.approvalWorkflowService = approvalWorkflowService;
    }

    @Operation(summary = "分页查询采购订单列表")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public Result<Page<PurchaseOrder>> getOrderList(PurchaseOrderQueryDTO queryDTO) {
        try {
            Page<PurchaseOrder> result = purchaseOrderService.getPurchaseOrderPage(queryDTO);
            return Result.success(result);
        } catch (Exception e) {
            log.error("查询采购订单列表失败", e);
            return Result.error("查询采购订单列表失败");
        }
    }

    @Operation(summary = "获取采购订单详情")
    @GetMapping("/{orderId}")
    @PreAuthorize("isAuthenticated()")
    public Result<PurchaseOrder> getOrderDetail(@PathVariable Long orderId) {
        try {
            PurchaseOrder order = purchaseOrderService.getOrderDetail(orderId);
            return Result.success(order);
        } catch (Exception e) {
            log.error("获取采购订单详情失败，ID：{}", orderId, e);
            return Result.error(e.getMessage() != null ? e.getMessage() : "获取订单详情失败");
        }
    }

    @Operation(summary = "创建采购订单（草稿，多供应商自动拆分）")
    @PostMapping
    @PreAuthorize("hasAnyRole('admin', 'purchase_manager', 'purchaser')")
    public Result<java.util.List<PurchaseOrder>> createOrder(@Valid @RequestBody PurchaseOrderCreateDTO createDTO) {
        try {
            java.util.List<PurchaseOrder> orders = purchaseOrderService.createOrder(createDTO);
            return Result.success(orders);
        } catch (Exception e) {
            log.error("创建采购订单失败", e);
            return Result.error(e.getMessage() != null ? e.getMessage() : "创建采购订单失败");
        }
    }

    @Operation(summary = "更新采购订单（仅草稿/已取消状态）")
    @PutMapping("/{orderId}")
    @PreAuthorize("hasAnyRole('admin', 'purchase_manager', 'purchaser')")
    public Result<PurchaseOrder> updateOrder(@PathVariable Long orderId,
                                               @Valid @RequestBody PurchaseOrderUpdateDTO updateDTO) {
        try {
            PurchaseOrder order = purchaseOrderService.updateOrder(orderId, updateDTO);
            return Result.success(order);
        } catch (Exception e) {
            log.error("更新采购订单失败，ID：{}", orderId, e);
            return Result.error(e.getMessage() != null ? e.getMessage() : "更新采购订单失败");
        }
    }

    @Operation(summary = "提交审核（草稿 -> 待审核）")
    @PutMapping("/{orderId}/submit")
    @PreAuthorize("hasAnyRole('admin', 'purchase_manager', 'purchaser')")
    public Result<PurchaseOrder> submitOrder(@PathVariable Long orderId) {
        try {
            PurchaseOrder order = purchaseOrderService.submitOrder(orderId);
            // 触发审批流程（第一层：写入审批记录，进入审批链）
            approvalWorkflowService.submitApproval("purchase_order", orderId.toString());
            return Result.success(order);
        } catch (Exception e) {
            log.error("提交采购订单审核失败，ID：{}", orderId, e);
            return Result.error(e.getMessage() != null ? e.getMessage() : "提交审核失败");
        }
    }

    @Operation(summary = "审批通过（待审核 -> 已审核）")
    @PutMapping("/{orderId}/approve")
    @PreAuthorize("hasAnyRole('admin', 'finance_manager')")
    public Result<PurchaseOrder> approveOrder(@PathVariable Long orderId,
                                                @Parameter(description = "审批备注") @RequestParam(required = false) String approvalRemark) {
        try {
            ApprovalActionDTO actionDTO = new ApprovalActionDTO();
            actionDTO.setBusinessType("purchase_order");
            actionDTO.setBusinessId(orderId.toString());
            actionDTO.setComment(approvalRemark);
            approvalWorkflowService.approve(actionDTO);
            PurchaseOrder order = purchaseOrderService.getOrderDetail(orderId);
            return Result.success(order);
        } catch (Exception e) {
            log.error("审批通过采购订单失败，ID：{}", orderId, e);
            return Result.error(e.getMessage() != null ? e.getMessage() : "审批操作失败");
        }
    }

    @Operation(summary = "确认下单（已审核 -> 已下单）")
    @PutMapping("/{orderId}/confirm")
    @PreAuthorize("hasAuthority('purchase:order:confirm') or hasAuthority('*')")
    public Result<Void> confirmOrder(@PathVariable Long orderId) {
        try {
            return purchaseOrderService.confirmOrder(orderId);
        } catch (Exception e) {
            log.error("确认下单失败，ID：{}", orderId, e);
            return Result.error(e.getMessage() != null ? e.getMessage() : "确认下单失败");
        }
    }

    @Operation(summary = "驳回订单（待审核 -> 已取消）")
    @PutMapping("/{orderId}/reject")
    @PreAuthorize("hasAnyRole('admin', 'finance_manager')")
    public Result<PurchaseOrder> rejectOrder(@PathVariable Long orderId,
                                               @Parameter(description = "驳回原因") @RequestParam String reason) {
        try {
            ApprovalActionDTO actionDTO = new ApprovalActionDTO();
            actionDTO.setBusinessType("purchase_order");
            actionDTO.setBusinessId(orderId.toString());
            actionDTO.setComment(reason);
            approvalWorkflowService.reject(actionDTO);
            PurchaseOrder order = purchaseOrderService.getOrderDetail(orderId);
            return Result.success(order);
        } catch (Exception e) {
            log.error("驳回采购订单失败，ID：{}", orderId, e);
            return Result.error(e.getMessage() != null ? e.getMessage() : "驳回操作失败");
        }
    }

    @Operation(summary = "取消订单（草稿/待审核 -> 已取消）")
    @PutMapping("/{orderId}/cancel")
    @PreAuthorize("hasAnyRole('admin', 'purchase_manager', 'purchaser')")
    public Result<PurchaseOrder> cancelOrder(@PathVariable Long orderId,
                                                @Parameter(description = "取消原因") @RequestParam String reason) {
        try {
            PurchaseOrder order = purchaseOrderService.cancelOrder(orderId, reason);
            return Result.success(order);
        } catch (Exception e) {
            log.error("取消采购订单失败，ID：{}", orderId, e);
            return Result.error(e.getMessage() != null ? e.getMessage() : "取消操作失败");
        }
    }

    @Operation(summary = "删除采购订单（仅草稿状态可删除）")
    @DeleteMapping("/{orderId}")
    @PreAuthorize("hasAnyRole('admin', 'purchase_manager')")
    public Result<Void> deleteOrder(@PathVariable Long orderId) {
        try {
            purchaseOrderService.deleteOrder(orderId);
            return Result.success();
        } catch (Exception e) {
            log.error("删除采购订单失败，ID：{}", orderId, e);
            return Result.error(e.getMessage() != null ? e.getMessage() : "删除订单失败");
        }
    }
}
