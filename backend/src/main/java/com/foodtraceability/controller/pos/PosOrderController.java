package com.foodtraceability.controller.pos;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.*;
import com.foodtraceability.service.PosOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 收银终端控制器
 * 负责POS终端的API接口定义，业务逻辑委托给PosOrderService处理
 * <p>
 * VULN-08 修复：类级别 @PreAuthorize 统一要求 POS 收银角色，
 * 防止非授权用户（如厨房、员工端账号）调用 POS 订单接口。
 * 退款等敏感操作由 Service 层 @PreAuthorize 二次校验（仅 ADMIN/MANAGER）。
 * </p>
 */
@RestController
@RequestMapping("/v1/pos/orders")
@Tag(name = "收银终端API", description = "收银终端专用接口")
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_CASHIER', 'ROLE_POS_OPERATOR', '*')")
public class PosOrderController {

    private static final Logger log = LoggerFactory.getLogger(PosOrderController.class);
    private final PosOrderService posOrderService;

    public PosOrderController(PosOrderService posOrderService) {
        this.posOrderService = posOrderService;
    }

    /**
     * 查询订单列表
     */
    @GetMapping("")
    @Operation(summary = "查询订单列表")
    public Result<List<OrderQueryDTO>> getOrders(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String keyword) {
        return posOrderService.getOrders(startDate, endDate, keyword);
    }

    /**
     * 根据桌台号查询订单
     */
    @GetMapping("/table/{tableNumber}")
    @Operation(summary = "根据桌台号查询订单")
    public Result<List<CustomerOrderDTO>> getOrdersByTable(@PathVariable String tableNumber) {
        return posOrderService.getOrdersByTable(tableNumber);
    }

    /**
     * 创建订单（委托给Service）
     */
    @PostMapping("/order")
    @Operation(summary = "创建订单")
    public Result<OrderResultDTO> createOrder(@RequestBody @Valid OrderRequestDTO request) {
        return posOrderService.createOrder(request);
    }

    /**
     * 获取订单详情（委托给Service）
     */
    @GetMapping("/order/{orderNumber}")
    @Operation(summary = "获取订单详情")
    public Result<OrderDetailDTO> getOrderDetail(@PathVariable String orderNumber) {
        return posOrderService.getOrderDetail(orderNumber);
    }

    /**
     * 通过openid查询订单列表
     */
    @GetMapping("/byOpenid")
    @Operation(summary = "通过openid查询订单列表")
    public Result<List<OrderDetailDTO>> getOrdersByOpenid(@RequestParam String openid) {
        return posOrderService.getOrdersByOpenid(openid);
    }

    /**
     * 通过订单号查询订单详情（委托给getOrderDetail）
     */
    @GetMapping("/byOrderNumber")
    @Operation(summary = "通过订单号查询订单详情")
    public Result<OrderDetailDTO> getOrderByNumber(@RequestParam String orderNumber) {
        return posOrderService.getOrderDetail(orderNumber);
    }

    /**
     * 订单退款（委托给Service）
     */
    @PostMapping("/order/{orderNumber}/refund")
    @Operation(summary = "订单退款")
    public Result<Boolean> refundOrder(
            @PathVariable String orderNumber,
            @RequestParam(required = false, defaultValue = "用户申请退款") String refundReason,
            HttpServletRequest request) {
        return posOrderService.refundOrder(orderNumber, refundReason, request);
    }

    /**
     * 创建桌台订单（扫码点餐）（委托给Service）
     */
    @PostMapping("/order/table")
    @Operation(summary = "创建桌台订单（扫码点餐）")
    public Result<OrderResultDTO> createTableOrder(@RequestBody @Valid TableOrderDTO request) {
        return posOrderService.createTableOrder(request);
    }

    /**
     * 订单支付（委托给Service）
     * <p>
     * 微信/支付宝：返回 qrCodeUrl，订单状态保持未支付，前端需弹窗展示二维码，
     * 用户扫码后调用 {@link #confirmQrPayment} 完成支付
     * </p>
     */
    @PostMapping("/order/pay")
    @Operation(summary = "订单支付")
    public Result<OrderResultDTO> payOrder(@RequestBody @Valid PayRequestDTO request) {
        return posOrderService.payOrder(request);
    }

    /**
     * 确认扫码支付完成（微信/支付宝）
     * <p>
     * 用户在前端完成微信/支付宝扫码后，调用此接口通知后端完成支付。
     * 后端将订单状态从未支付更新为已支付，并触发后续流程（后厨通知、流水记录等）。
     * </p>
     * <p>
     * 开发模式：前端主动调用此接口模拟"用户已扫码支付"。
     * 生产模式：应由微信/支付宝服务端异步回调触发，而非前端调用。
     * </p>
     *
     * @param qrTransactionId 扫码支付交易号（由 payOrder 返回）
     * @return 订单结果
     */
    @PostMapping("/order/pay/confirm")
    @Operation(summary = "确认扫码支付完成")
    public Result<OrderResultDTO> confirmQrPayment(@RequestParam String qrTransactionId) {
        return posOrderService.confirmQrPayment(qrTransactionId);
    }

    /**
     * 重新打印小票
     * <p>
     * 根据订单ID查询订单详情并重新发送打印指令到热敏打印机。
     * 适用于打印失败后手动补打或顾客要求重打场景。
     * </p>
     *
     * @param orderId 订单ID（路径参数）
     * @return 重打结果：true成功，false失败
     */
    @PostMapping("/order/{orderId}/receipt")
    @Operation(summary = "重新打印小票")
    public Result<Boolean> reprintReceipt(@PathVariable String orderId) {
        log.info("收到重打小票请求: orderId={}", orderId);
        return posOrderService.reprintReceipt(orderId);
    }
}
