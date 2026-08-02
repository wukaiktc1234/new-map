package com.foodtraceability.service.approval.handler;

import com.foodtraceability.entity.PurchaseOrder;
import com.foodtraceability.service.PurchaseOrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 采购订单审批处理器
 *
 * <p>委托 PurchaseOrderService.approveOrder/rejectOrder 完成业务状态联动：
 * <ul>
 *   <li>审批通过：待审核(1) -> 已审核(2)</li>
 *   <li>审批驳回：待审核(1) -> 已取消(5)</li>
 * </ul>
 *
 * <p>该 handler 是 RBAC+动态审批链第二层（业务状态联动）的采购订单实现，
 * 审批流程服务在审批通过/驳回后自动路由到该 handler。
 */
@Component
public class PurchaseOrderApprovalHandler implements BusinessApprovalHandler {

    private static final Logger log = LoggerFactory.getLogger(PurchaseOrderApprovalHandler.class);
    private static final String BUSINESS_TYPE = "purchase_order";

    private final PurchaseOrderService purchaseOrderService;

    /**
     * 构造函数注入 PurchaseOrderService
     */
    public PurchaseOrderApprovalHandler(PurchaseOrderService purchaseOrderService) {
        this.purchaseOrderService = purchaseOrderService;
    }

    @Override
    public String getBusinessType() {
        return BUSINESS_TYPE;
    }

    @Override
    public void onApproved(String businessId, String comment, String operatorId) {
        try {
            Long orderId = Long.parseLong(businessId);
            PurchaseOrder order = purchaseOrderService.approveOrder(orderId, comment);
            log.info("采购订单审批通过联动成功: orderId={}, status={}, operatorId={}",
                    orderId, order.getOrderStatus(), operatorId);
        } catch (NumberFormatException e) {
            log.error("采购订单ID格式错误: businessId={}", businessId);
            throw new IllegalArgumentException("采购订单ID格式错误: " + businessId);
        } catch (Exception e) {
            log.error("采购订单审批通过联动失败: businessId={}, error={}", businessId, e.getMessage());
            throw new RuntimeException("采购订单审批通过失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void onRejected(String businessId, String comment, String operatorId) {
        try {
            Long orderId = Long.parseLong(businessId);
            PurchaseOrder order = purchaseOrderService.rejectOrder(orderId, comment);
            log.info("采购订单审批驳回联动成功: orderId={}, status={}, operatorId={}",
                    orderId, order.getOrderStatus(), operatorId);
        } catch (NumberFormatException e) {
            log.error("采购订单ID格式错误: businessId={}", businessId);
            throw new IllegalArgumentException("采购订单ID格式错误: " + businessId);
        } catch (Exception e) {
            log.error("采购订单审批驳回联动失败: businessId={}, error={}", businessId, e.getMessage());
            throw new RuntimeException("采购订单审批驳回失败: " + e.getMessage(), e);
        }
    }
}
