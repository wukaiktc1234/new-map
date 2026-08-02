package com.foodtraceability.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.PageResult;
import com.foodtraceability.dto.order.*;
import com.foodtraceability.service.OrderNewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 订单管理控制器
 * 系统最核心的控制器，提供订单完整生命周期的API
 */
@RestController
@RequestMapping("/v1/orders")
@Tag(name = "订单管理", description = "订单的创建、支付、取消、退款等核心接口")
public class OrderNewController {

    private final OrderNewService orderNewService;

    public OrderNewController(OrderNewService orderNewService) {
        this.orderNewService = orderNewService;
    }

    // ==================== 订单创建 ====================

    /**
     * 创建订单
     * 完整流程：校验 -> 计算 -> 生成编号 -> 保存 -> 锁桌台
     */
    @PostMapping
    @PreAuthorize("hasAuthority('order:add') or hasAuthority('order:manage') or hasAuthority('*')")
    @Operation(summary = "创建订单", description = "创建新订单，支持堂食/外卖/自提/打包四种类型")
    public Result<OrderVO> createOrder(@Valid @RequestBody OrderCreateDTO createDTO) {
        return Result.success(orderNewService.createOrder(createDTO));
    }

    // ==================== 退款查询 ====================

    /**
     * 分页查询退款申请
     * 支持按退款单号、订单号、状态、时间范围筛选
     * 字面量路径必须在 /{orderId} 之前定义，避免路径变量冲突
     */
    @GetMapping("/refunds")
    @PreAuthorize("hasAuthority('order:view') or hasAuthority('order:manage') or hasAuthority('*')")
    @Operation(summary = "分页查询退款申请", description = "支持按退款单号、订单号、状态、时间范围筛选")
    public Result<PageResult<OrderRefundListVO>> getRefundPage(OrderRefundQueryDTO queryDTO) {
        return Result.success(orderNewService.queryRefunds(queryDTO), "查询退款列表成功");
    }

    /**
     * 退款统计
     * 获取退款汇总统计数据（待处理数、已退款金额、退款率、平均处理时长）
     */
    @GetMapping("/refunds/stats")
    @PreAuthorize("hasAuthority('order:view') or hasAuthority('order:manage') or hasAuthority('*')")
    @Operation(summary = "退款统计", description = "获取退款汇总统计数据")
    public Result<OrderRefundStatsVO> getRefundStats() {
        return Result.success(orderNewService.getRefundStats(), "获取退款统计成功");
    }

    /**
     * 订单总览统计
     * 字面量路径必须在 /{orderId} 之前定义，避免路径变量冲突
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasAuthority('order:view') or hasAuthority('order:manage') or hasAuthority('*')")
    @Operation(summary = "订单总览统计", description = "获取总订单数、已完成订单数、今日订单数、总销售额、今日销售额")
    public Result<Map<String, Object>> getOrderStatistics() {
        return Result.success(orderNewService.getOrderStatistics(), "获取订单统计成功");
    }

    // ==================== 订单查询 ====================

    /**
     * 获取订单详情（含明细、支付记录、退款记录）
     * <p>
     * 路径冲突 BUG 修复（G3-01）：
     * 原 /v1/orders/page、/v1/orders/refunds 等字面量路径会被 /{orderId} 误匹配
     * 已通过显式声明字面量端点（/refunds、/page、/pos 等）解决
     * 此处增加 orderId 格式校验，避免无效 ID 触发 500 错误
     * </p>
     */
    @GetMapping("/{orderId}")
    @PreAuthorize("hasAuthority('order:view') or hasAuthority('order:manage') or hasAuthority('*')")
    @Operation(summary = "获取订单详情", description = "获取订单完整信息，包括明细、支付记录、退款记录")
    public Result<OrderVO> getOrderDetail(@PathVariable String orderId) {
        // 格式校验：订单ID通常为 O + 数字 或纯数字
        if (orderId == null || orderId.isEmpty() || orderId.length() > 50
                || !orderId.matches("^[A-Za-z0-9_-]+$")) {
            return Result.error(400, "订单ID格式不正确");
        }
        return Result.success(orderNewService.getOrderDetail(orderId));
    }

    /**
     * 分页查询订单列表
     * 支持多条件筛选：订单类型、状态、时间范围、金额范围等
     */
    @GetMapping
    @PreAuthorize("hasAuthority('order:view') or hasAuthority('order:manage') or hasAuthority('*')")
    @Operation(summary = "分页查询订单", description = "支持多条件筛选的分页查询")
    public Result<Page<OrderVO>> queryOrders(OrderQueryDTO queryDTO) {
        return Result.success(orderNewService.queryOrders(queryDTO));
    }

    /**
     * 分页查询订单列表（/page 别名）
     * <p>
     * 路径冲突 BUG 修复（G3-01）：
     * 显式声明 /page 端点，避免被 /{orderId} 拦截返回 500 错误
     * 与 {@link #queryOrders} 行为一致，仅路径不同
     * </p>
     */
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('order:view') or hasAuthority('order:manage') or hasAuthority('*')")
    @Operation(summary = "分页查询订单（/page 别名）", description = "与 GET /v1/orders 行为一致，提供 /page 路径别名")
    public Result<Page<OrderVO>> queryOrdersByPage(OrderQueryDTO queryDTO) {
        return Result.success(orderNewService.queryOrders(queryDTO));
    }

    /**
     * 分页查询POS终端订单
     * 专门查询POS收银端创建的订单（orders_legacy 表），与管理端订单（orders 表）互补。
     * 返回格式与 {@link #queryOrders} 一致，前端可统一展示。
     * 解决：POS收银端创建的订单无法在管理端订单中心查看的问题。
     */
    @GetMapping("/pos")
    @PreAuthorize("hasAuthority('order:view') or hasAuthority('order:manage') or hasAuthority('*')")
    @Operation(summary = "分页查询POS终端订单", description = "查询POS收银端创建的订单（orders_legacy 表），返回格式与管理端订单一致")
    public Result<Page<OrderVO>> queryPosOrders(OrderQueryDTO queryDTO) {
        return Result.success(orderNewService.queryPosOrders(queryDTO));
    }

    /**
     * 获取POS终端订单详情（含菜品明细）
     * <p>
     * 字面量路径 /pos/{orderNumber} 必须在 /{orderId} 之前声明，
     * 否则会被 /{orderId} 误匹配导致 500 错误。
     * </p>
     * <p>
     * 解决：管理端订单中心查看POS订单时明细显示"暂无数据"的问题。
     * 通过 orderNumber 查询 orders_legacy + order_items_legacy，返回完整 OrderVO。
     * </p>
     *
     * @param orderNumber 订单编号（如 T20260713002），非订单ID
     */
    @GetMapping("/pos/{orderNumber}")
    @PreAuthorize("hasAuthority('order:view') or hasAuthority('order:manage') or hasAuthority('*')")
    @Operation(summary = "获取POS终端订单详情", description = "通过订单编号查询POS订单完整信息（orders_legacy + order_items_legacy），含菜品明细")
    public Result<OrderVO> getPosOrderDetail(@PathVariable String orderNumber) {
        if (orderNumber == null || orderNumber.isEmpty() || orderNumber.length() > 50
                || !orderNumber.matches("^[A-Za-z0-9_-]+$")) {
            return Result.error(400, "订单编号格式不正确");
        }
        return Result.success(orderNewService.getPosOrderDetail(orderNumber));
    }

    // ==================== 支付操作 ====================

    /**
     * 支付订单
     * 支持多种支付方式组合（现金+微信+积分等）
     */
    @PostMapping("/{orderId}/pay")
    @PreAuthorize("hasAuthority('order:manage') or hasAuthority('*')")
    @Operation(summary = "支付订单", description = "对订单进行支付，支持多种支付方式组合")
    public Result<OrderVO> payOrder(
            @PathVariable String orderId,
            @Valid @RequestBody OrderPayDTO payDTO) {
        return Result.success(orderNewService.payOrder(orderId, payDTO));
    }

    // ==================== 取消操作 ====================

    /**
     * 取消订单
     * 自动处理退款、解锁桌台等逻辑
     */
    @PostMapping("/{orderId}/cancel")
    @PreAuthorize("hasAuthority('order:manage') or hasAuthority('*')")
    @Operation(summary = "取消订单", description = "取消订单并自动处理相关业务")
    public Result<OrderVO> cancelOrder(
            @PathVariable String orderId,
            @RequestParam(required = false, defaultValue = "") String cancelReason) {
        return Result.success(orderNewService.cancelOrder(orderId, cancelReason));
    }

    // ==================== 退款操作 ====================

    /**
     * 申请退款
     * 支持全额退款和部分退款
     */
    @PostMapping("/{orderId}/refund")
    @PreAuthorize("hasAuthority('order:manage') or hasAuthority('*')")
    @Operation(summary = "申请退款", description = "提交退款申请，支持全额或部分退款")
    public Result<OrderVO.OrderRefundRecordVO> applyRefund(
            @PathVariable String orderId,
            @Valid @RequestBody OrderRefundDTO refundDTO) {
        return Result.success(orderNewService.applyRefund(orderId, refundDTO));
    }

    /**
     * 审批退款
     */
    @PostMapping("/refunds/{refundId}/approve")
    @PreAuthorize("hasAuthority('order:manage') or hasAuthority('*')")
    @Operation(summary = "审批退款", description = "审批退款申请，同意或拒绝")
    public Result<Void> approveRefund(
            @PathVariable Long refundId,
            @RequestParam boolean approved,
            @RequestParam Long approveUserId) {
        orderNewService.approveRefund(refundId, approved, approveUserId);
        return Result.success();
    }

    // ==================== 状态变更 ====================

    /** 确认订单（待确认->已确认/制作中） */
    @PostMapping("/{orderId}/confirm")
    @PreAuthorize("hasAuthority('order:manage') or hasAuthority('*')")
    @Operation(summary = "确认订单", description = "将待确认订单标记为已确认/制作中")
    public Result<Void> confirmOrder(@PathVariable String orderId) {
        orderNewService.confirmOrder(orderId);
        return Result.success();
    }

    /** 完成订单 */
    @PostMapping("/{orderId}/complete")
    @PreAuthorize("hasAuthority('order:manage') or hasAuthority('*')")
    @Operation(summary = "完成订单", description = "将订单标记为已完成")
    public Result<Void> completeOrder(@PathVariable String orderId) {
        orderNewService.completeOrder(orderId);
        return Result.success();
    }

    // ==================== 厨房状态 ====================

    /** 更新明细厨房状态 */
    @PutMapping("/items/{itemId}/kitchen-status/{kitchenStatus}")
    @PreAuthorize("hasAuthority('order:manage') or hasAuthority('*')")
    @Operation(summary = "更新厨房状态", description = "更新订单明细的制作状态")
    public Result<Void> updateKitchenStatus(
            @PathVariable String itemId,
            @PathVariable Integer kitchenStatus) {
        orderNewService.updateItemKitchenStatus(itemId, kitchenStatus);
        return Result.success();
    }

    // ==================== 统计报表 ====================

    /** 今日销售统计 */
    @GetMapping("/today/statistics")
    @PreAuthorize("hasAuthority('order:view') or hasAuthority('order:manage') or hasAuthority('*')")
    @Operation(summary = "今日销售统计", description = "获取今日销售概况数据")
    public Result<TodayStatisticsVO> getTodayStatistics() {
        return Result.success(orderNewService.getTodayStatistics());
    }

    /** 交接班汇总 */
    @GetMapping("/cashier/shift-summary")
    @PreAuthorize("hasAuthority('order:view') or hasAuthority('order:manage') or hasAuthority('*')")
    @Operation(summary = "交接班汇总", description = "获取指定收银员的交接班数据")
    public Result<Map<String, Object>> getShiftSummary(
            @RequestParam(required = false) Long cashierUserId) {
        return Result.success(orderNewService.getShiftSummary(cashierUserId));
    }
}
