package com.foodtraceability.service;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.CustomerOrderDTO;
import com.foodtraceability.dto.OrderDetailDTO;
import com.foodtraceability.dto.OrderQueryDTO;

import java.util.List;

/**
 * POS订单查询服务接口
 * 负责订单查询、详情获取、小票重打等业务逻辑
 */
public interface PosOrderQueryService {

    /**
     * 获取订单详情
     * @param orderNumber 订单号
     * @return 订单详情
     */
    Result<OrderDetailDTO> getOrderDetail(String orderNumber);

    /**
     * 查询订单列表
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param keyword 关键词
     * @return 订单列表
     */
    Result<List<OrderQueryDTO>> getOrders(String startDate, String endDate, String keyword);

    /**
     * 根据桌台号查询订单
     * @param tableNumber 桌台号
     * @return 订单列表
     */
    Result<List<CustomerOrderDTO>> getOrdersByTable(String tableNumber);

    /**
     * 通过openid查询订单列表
     * @param openid 微信openid
     * @return 订单详情列表
     */
    Result<List<OrderDetailDTO>> getOrdersByOpenid(String openid);

    /**
     * 重新打印小票
     * @param orderId 订单ID
     * @return 重打结果
     */
    Result<Boolean> reprintReceipt(String orderId);
}
