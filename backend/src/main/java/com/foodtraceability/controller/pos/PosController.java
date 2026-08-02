package com.foodtraceability.controller.pos;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.order.*;
import com.foodtraceability.entity.CallNumberQueueNew;
import com.foodtraceability.service.CallNumberQueueNewService;
import com.foodtraceability.service.OrderNewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * POS收银集成控制器
 * 提供收银台专用的高效操作接口
 */
@RestController
@RequestMapping("/v1/pos")
@Tag(name = "POS收银", description = "收银台专用快速操作接口")
public class PosController {

    private final OrderNewService orderNewService;
    private final CallNumberQueueNewService callNumberQueueNewService;

    public PosController(OrderNewService orderNewService, CallNumberQueueNewService callNumberQueueNewService) {
        this.orderNewService = orderNewService;
        this.callNumberQueueNewService = callNumberQueueNewService;
    }

    // ==================== 快速下单 ====================

    /**
     * POS快速下单
     * 简化版下单流程：只需传入商品列表，自动计算金额
     */
    @PostMapping("/orders/create")
    @PreAuthorize("hasAuthority('order:create') or hasAuthority('*')")
    @Operation(summary = "POS快速下单", description = "收银台快速下单，简化流程")
    public Result<OrderVO> createQuickOrder(@Valid @RequestBody PosQuickOrderDTO quickOrderDTO) {
        return Result.success(orderNewService.createPosQuickOrder(quickOrderDTO));
    }

    // ==================== 快速支付 ====================

    /**
     * 快速支付（简化版）
     * 支持单种或多种支付方式
     */
    @PostMapping("/orders/{orderId}/quick-pay")
    @PreAuthorize("hasAuthority('order:pay') or hasAuthority('*')")
    @Operation(summary = "POS快速支付", description = "收银台快速支付订单")
    public Result<OrderVO> quickPay(
            @PathVariable String orderId,
            @Valid @RequestBody OrderPayDTO payDTO) {
        return Result.success(orderNewService.payOrder(orderId, payDTO));
    }

    // ==================== 退款操作 ====================

    /**
     * 快速退款
     */
    @PostMapping("/orders/{orderId}/quick-refund")
    @PreAuthorize("hasAuthority('order:refund') or hasAuthority('*')")
    @Operation(summary = "POS快速退款", description = "收银台快速退款操作")
    public Result<OrderVO.OrderRefundRecordVO> quickRefund(
            @PathVariable String orderId,
            @Valid @RequestBody OrderRefundDTO refundDTO) {
        return Result.success(orderNewService.applyRefund(orderId, refundDTO));
    }

    // ==================== 统计报表 ====================

    /**
     * 今日销售统计（实时）
     */
    @GetMapping("/today/statistics")
    @Operation(summary = "今日统计", description = "获取今日实时销售统计数据")
    public Result<TodayStatisticsVO> getTodayStatistics() {
        return Result.success(orderNewService.getTodayStatistics());
    }

    /**
     * 交接班汇总
     */
    @GetMapping("/shift-summary")
    @Operation(summary = "交接班汇总", description = "获取当前收银员的交接班数据")
    public Result<Map<String, Object>> getShiftSummary(
            @RequestParam(required = false) Long cashierUserId) {
        return Result.success(orderNewService.getShiftSummary(cashierUserId));
    }

    // ==================== 叫号排队 ====================

    /** 取号 */
    @PostMapping("/queue/take")
    @PreAuthorize("hasAuthority('order:queue') or hasAuthority('*')")
    @Operation(summary = "取号", description = "顾客取号排队")
    public Result<CallNumberQueueNew> takeNumber(
            @RequestParam Long storeId,
            @RequestParam Integer queueType,
            @RequestParam(defaultValue = "1") Integer peopleCount,
            @RequestParam(required = false) String tablePreference) {
        return Result.success(callNumberQueueNewService.takeNumber(storeId, queueType, peopleCount, tablePreference));
    }

    /** 叫号 */
    @PostMapping("/queue/call-next")
    @PreAuthorize("hasAuthority('order:queue') or hasAuthority('*')")
    @Operation(summary = "叫下一个", description = "呼叫下一个等待中的号码")
    public Result<CallNumberQueueNew> callNext(@RequestParam Integer queueType) {
        return Result.success(callNumberQueueNewService.callNext(queueType));
    }

    /** 重叫 */
    @PostMapping("/queue/re-call/{queueId}")
    @PreAuthorize("hasAuthority('order:queue') or hasAuthority('*')")
    @Operation(summary = "重叫", description = "对指定号码重新呼叫")
    public Result<Void> reCall(@PathVariable Long queueId) {
        callNumberQueueNewService.reCall(queueId);
        return Result.success();
    }

    /** 标记已用餐 */
    @PostMapping("/queue/dined/{queueId}")
    @PreAuthorize("hasAuthority('order:queue') or hasAuthority('*')")
    @Operation(summary = "标记用餐", description = "标记顾客已入座用餐")
    public Result<Void> markDined(@PathVariable Long queueId) {
        callNumberQueueNewService.markDined(queueId);
        return Result.success();
    }

    /** 取消排队 */
    @PostMapping("/queue/cancel/{queueId}")
    @PreAuthorize("hasAuthority('order:queue') or hasAuthority('*')")
    @Operation(summary = "取消排队", description = "取消指定排队记录")
    public Result<Void> cancelQueue(@PathVariable Long queueId) {
        callNumberQueueNewService.cancel(queueId);
        return Result.success();
    }

    /** 排队情况 */
    @GetMapping("/queue/status")
    @Operation(summary = "排队情况", description = "获取当前各类型排队情况")
    public Result<Map<String, Object>> getQueueStatus(@RequestParam Integer queueType) {
        return Result.success(callNumberQueueNewService.getQueueStatus(queueType));
    }

    /** 等待列表 */
    @GetMapping("/queue/waiting-list")
    @Operation(summary = "等待列表", description = "获取当前等待中的排队列表")
    public Result<List<CallNumberQueueNew>> getWaitingList(@RequestParam Integer queueType) {
        return Result.success(callNumberQueueNewService.getWaitingList(queueType));
    }
}
