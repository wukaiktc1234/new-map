package com.foodtraceability.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.dto.OrderRequest;
import com.foodtraceability.dto.OrderResponse;
import com.foodtraceability.entity.Order;

import java.util.List;

/**
 * 订单服务接口
 * 定义订单管理相关的业务逻辑方法
 */
public interface OrderService {
    
    /**
     * 创建订单
     * @param orderRequest 订单请求DTO
     * @return 订单响应DTO
     */
    OrderResponse createOrder(OrderRequest orderRequest);
    
    /**
     * 根据ID获取订单
     * @param orderId 订单ID
     * @return 订单响应DTO
     */
    OrderResponse getOrderById(String orderId);
    
    /**
     * 分页查询订单列表
     * @param pageParam 分页参数
     * @param orderNumber 订单编号
     * @param orderType 订单类型
     * @param orderStatus 订单状态
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 订单分页列表
     */
    Page<OrderResponse> getOrderPage(Page<Order> pageParam, String orderNumber, Integer orderType, Integer orderStatus, String startTime, String endTime);
    
    /**
     * 更新订单状态
     * @param orderId 订单ID
     * @param orderStatus 订单状态
     * @return 更新后的订单响应DTO
     */
    OrderResponse updateOrderStatus(String orderId, Integer orderStatus);
    
    /**
     * 更新订单信息
     * @param orderId 订单ID
     * @param orderRequest 订单请求DTO
     * @return 更新后的订单响应DTO
     */
    OrderResponse updateOrder(String orderId, OrderRequest orderRequest);
    
    /**
     * 根据ID删除订单
     * @param orderId 订单ID
     */
    void deleteOrder(String orderId);
    
    /**
     * 批量更新订单状态
     * @param orderIds 订单ID列表
     * @param orderStatus 订单状态
     * @return 更新成功的订单数量
     */
    int batchUpdateOrderStatus(List<String> orderIds, Integer orderStatus);
    
    /**
     * 批量删除订单
     * @param orderIds 订单ID列表
     * @return 删除成功的订单数量
     */
    int batchDeleteOrders(List<String> orderIds);
    
    /**
     * 根据用户ID查询订单列表
     * @param userId 用户ID
     * @return 订单列表
     */
    List<OrderResponse> getOrdersByUserId(String userId);
    
    /**
     * 查询订单统计数据
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 订单统计数据
     */
    List<Object> getOrderStatistics(String startTime, String endTime);
    
    /**
     * 查询订单趋势数据
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 订单趋势数据
     */
    List<Object> getOrderTrends(String startTime, String endTime);
}
