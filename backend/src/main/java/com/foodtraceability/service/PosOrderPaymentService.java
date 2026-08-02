package com.foodtraceability.service;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.OrderResultDTO;
import com.foodtraceability.dto.PayRequestDTO;
import jakarta.servlet.http.HttpServletRequest;

/**
 * POS订单支付服务接口
 * 负责订单支付、退款等业务逻辑
 */
public interface PosOrderPaymentService {

    /**
     * 订单支付
     * 包含CAS锁机制、状态管理、余额支付、交易ID生成等完整流程
     * <p>
     * 微信/支付宝走扫码支付：返回二维码URL，订单状态保持未支付，
     * 等待调用 {@link #confirmQrPayment} 完成支付
     * </p>
     * @param request 支付请求DTO
     * @return 订单结果（扫码支付时包含 qrCodeUrl）
     */
    Result<OrderResultDTO> payOrder(PayRequestDTO request);

    /**
     * 确认扫码支付完成
     * <p>
     * 用户在前端完成微信/支付宝扫码支付后，调用此接口确认。
     * 后端将订单状态从未支付（0）更新为已支付（1），并触发后续流程（后厨通知、小票打印等）。
     * </p>
     * <p>
     * 生产环境对接微信/支付宝官方支付时，此接口应由支付平台的服务端回调触发，
     * 而非前端调用。当前为开发模拟模式。
     * </p>
     *
     * @param qrTransactionId 扫码支付交易号（由 payOrder 返回）
     * @return 订单结果
     */
    Result<OrderResultDTO> confirmQrPayment(String qrTransactionId);

    /**
     * 订单退款
     * 包含库存回补、后厨通知、日志记录等完整流程
     * @param orderNumber 订单号
     * @param refundReason 退款原因
     * @param request HTTP请求
     * @return 退款结果
     */
    Result<Boolean> refundOrder(String orderNumber, String refundReason, HttpServletRequest request);
}
