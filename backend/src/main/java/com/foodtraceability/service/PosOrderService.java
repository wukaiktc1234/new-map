package com.foodtraceability.service;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * POS订单服务门面类
 * 负责处理POS终端的订单创建、支付、退款等核心业务逻辑
 * 委托给各子服务处理具体业务
 */
@Service
public class PosOrderService {

    private final PosOrderCreateService createService;
    private final PosOrderPaymentService paymentService;
    private final PosOrderQueryService queryService;

    public PosOrderService(
            PosOrderCreateService createService,
            PosOrderPaymentService paymentService,
            PosOrderQueryService queryService) {
        this.createService = createService;
        this.paymentService = paymentService;
        this.queryService = queryService;
    }

    /**
     * 创建订单
     */
    public Result<OrderResultDTO> createOrder(OrderRequestDTO request) {
        return createService.createOrder(request);
    }

    /**
     * 创建桌台订单（扫码点餐）
     */
    public Result<OrderResultDTO> createTableOrder(TableOrderDTO request) {
        return createService.createTableOrder(request);
    }

    /**
     * 订单支付
     */
    public Result<OrderResultDTO> payOrder(PayRequestDTO request) {
        return paymentService.payOrder(request);
    }

    /**
     * 确认扫码支付完成（微信/支付宝）
     * <p>
     * 开发模式：前端主动调用此接口模拟"用户已扫码支付"。
     * 生产模式：应由微信/支付宝服务端异步回调触发，而非前端调用。
     * </p>
     *
     * @param qrTransactionId 扫码支付交易号（由 payOrder 返回）
     * @return 订单结果
     */
    public Result<OrderResultDTO> confirmQrPayment(String qrTransactionId) {
        return paymentService.confirmQrPayment(qrTransactionId);
    }

    /**
     * 订单退款
     */
    public Result<Boolean> refundOrder(String orderNumber, String refundReason, HttpServletRequest request) {
        return paymentService.refundOrder(orderNumber, refundReason, request);
    }

    /**
     * 获取订单详情
     */
    public Result<OrderDetailDTO> getOrderDetail(String orderNumber) {
        return queryService.getOrderDetail(orderNumber);
    }

    /**
     * 查询订单列表
     */
    public Result<List<OrderQueryDTO>> getOrders(String startDate, String endDate, String keyword) {
        return queryService.getOrders(startDate, endDate, keyword);
    }

    /**
     * 根据桌台号查询订单
     */
    public Result<List<CustomerOrderDTO>> getOrdersByTable(String tableNumber) {
        return queryService.getOrdersByTable(tableNumber);
    }

    /**
     * 通过openid查询订单列表
     */
    public Result<List<OrderDetailDTO>> getOrdersByOpenid(String openid) {
        return queryService.getOrdersByOpenid(openid);
    }

    /**
     * 重新打印小票
     */
    public Result<Boolean> reprintReceipt(String orderId) {
        return queryService.reprintReceipt(orderId);
    }
}
