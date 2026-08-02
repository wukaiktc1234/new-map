package com.foodtraceability.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.PurchaseOrderCreateDTO;
import com.foodtraceability.dto.PurchaseOrderQueryDTO;
import com.foodtraceability.dto.PurchaseOrderUpdateDTO;
import com.foodtraceability.entity.PurchaseOrder;

/**
 * 采购订单服务接口
 * 提供采购订单的全生命周期管理：创建、提交审核、审批、入库、取消等
 */
public interface PurchaseOrderService extends IService<PurchaseOrder> {

    /**
     * 分页查询采购订单
     * @param queryDTO 查询条件
     * @return 分页结果
     */
    Page<PurchaseOrder> getPurchaseOrderPage(PurchaseOrderQueryDTO queryDTO);

    /**
     * 根据ID获取采购订单详情（含明细）
     * @param orderId 订单主键ID
     * @return 采购订单（含items）
     */
    PurchaseOrder getOrderDetail(Long orderId);

    /**
     * 创建采购订单（草稿状态）
     * @param createDTO 创建数据
     * @return 创建后的采购订单（多供应商时按供应商拆分为多张）
     */
    java.util.List<PurchaseOrder> createOrder(PurchaseOrderCreateDTO createDTO);

    /**
     * 更新采购订单（仅草稿/已驳回状态可修改）
     * @param orderId 订单主键ID
     * @param updateDTO 更新数据
     * @return 更新后的采购订单
     */
    PurchaseOrder updateOrder(Long orderId, PurchaseOrderUpdateDTO updateDTO);

    /**
     * 提交审核（草稿 -> 待审核）
     * @param orderId 订单主键ID
     * @return 更新后的订单
     */
    PurchaseOrder submitOrder(Long orderId);

    /**
     * 审批通过（待审核 -> 已审核）
     * @param orderId 订单主键ID
     * @param approvalRemark 审批备注
     * @return 更新后的订单
     */
    PurchaseOrder approveOrder(Long orderId, String approvalRemark);

    /**
     * 确认下单（已审核 -> 已下单）
     * 校验当前状态为"已审核"，更新为"已下单"
     * @param orderId 订单主键ID
     * @return 操作结果
     */
    Result<Void> confirmOrder(Long orderId);

    /**
     * 驳回订单（待审核 -> 已取消）
     * @param orderId 订单主键ID
     * @param approvalRemark 驳回原因
     * @return 更新后的订单
     */
    PurchaseOrder rejectOrder(Long orderId, String approvalRemark);

    /**
     * 取消订单（草稿/待审核 -> 已取消）
     * @param orderId 订单主键ID
     * @param reason 取消原因
     * @return 更新后的订单
     */
    PurchaseOrder cancelOrder(Long orderId, String reason);

    /**
     * 删除采购订单（逻辑删除，仅草稿状态可删除）
     * @param orderId 订单主键ID
     */
    void deleteOrder(Long orderId);
}
