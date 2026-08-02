package com.foodtraceability.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.dto.PageResult;
import com.foodtraceability.dto.order.*;
import java.util.List;
import java.util.Map;

/**
 * 订单服务接口
 * 系统最核心的服务接口，管理订单完整生命周期
 */
public interface OrderNewService {

    /**
     * 创建订单
     * 完整的订单创建流程：校验 -> 计算 -> 生成编号 -> 保存 -> 锁桌台 -> 发MQ
     * @param createDTO 创建订单请求
     * @return 订单视图对象
     */
    OrderVO createOrder(OrderCreateDTO createDTO);

    /**
     * POS快速下单
     * 简化版下单流程，用于收银台快速操作
     * @param quickOrderDTO 快速下单请求
     * @return 订单视图对象
     */
    OrderVO createPosQuickOrder(PosQuickOrderDTO quickOrderDTO);

    /**
     * 支付订单
     * 支持多种支付方式组合支付
     * @param orderId 订单ID
     * @param payDTO 支付请求
     * @return 更新后的订单视图对象
     */
    OrderVO payOrder(String orderId, OrderPayDTO payDTO);

    /**
     * 取消订单
     * 根据订单状态决定是否需要退款、解锁桌台等
     * @param orderId 订单ID
     * @param cancelReason 取消原因
     * @return 更新后的订单视图对象
     */
    OrderVO cancelOrder(String orderId, String cancelReason);

    /**
     * 申请退款
     * 全额退款或部分退款
     * @param orderId 订单ID
     * @param refundDTO 退款请求
     * @return 退款记录视图对象
     */
    OrderVO.OrderRefundRecordVO applyRefund(String orderId, OrderRefundDTO refundDTO);

    /**
     * 审批退款
     * @param refundId 退款记录ID
     * @param approved 是否同意
     * @param approveUserId 审批人ID
     */
    void approveRefund(Long refundId, boolean approved, Long approveUserId);

    /**
     * 确认订单（待确认->已确认/制作中）
     * @param orderId 订单ID
     */
    void confirmOrder(String orderId);

    /**
     * 完成订单
     * @param orderId 订单ID
     */
    void completeOrder(String orderId);

    /**
     * 根据ID获取订单详情
     * @param orderId 订单ID
     * @return 订单视图对象（含明细和支付记录）
     */
    OrderVO getOrderDetail(String orderId);

    /**
     * 分页查询订单列表
     * @param queryDTO 查询条件
     * @return 分页结果
     */
    Page<OrderVO> queryOrders(OrderQueryDTO queryDTO);

    /**
     * 分页查询POS终端订单（orders_legacy 表）
     * 专门查询POS收银端创建的订单，与 queryOrders（查询 orders 表）互补。
     * 内部完成订单状态/支付方式/金额单位（元→分）的映射转换，
     * 返回的 OrderVO 与管理端订单格式一致，前端无需额外处理。
     * @param queryDTO 查询条件（orderCode/orderStatus/startTime/endTime 等）
     * @return 分页结果（OrderVO 列表）
     */
    Page<OrderVO> queryPosOrders(OrderQueryDTO queryDTO);

    /**
     * 根据订单编号获取POS终端订单详情（含菜品明细）
     * 专门查询POS收银端创建的订单（orders_legacy + order_items_legacy）。
     * 内部完成订单状态/支付方式/金额单位（元→分）的映射转换，
     * 返回的 OrderVO 与管理端订单格式一致，包含 items 列表用于数据追溯。
     * @param orderNumber 订单编号（如 T20260713002），非订单ID
     * @return 订单视图对象（含明细）
     */
    OrderVO getPosOrderDetail(String orderNumber);

    /**
     * 获取今日销售统计
     * @return 统计数据
     */
    TodayStatisticsVO getTodayStatistics();

    /**
     * 获取交接班汇总
     * @param cashierUserId 收银员ID
     * @return 汇总数据
     */
    Map<String, Object> getShiftSummary(Long cashierUserId);

    /**
     * 更新厨房状态
     * @param itemId 明细ID
     * @param kitchenStatus 厨房状态
     */
    void updateItemKitchenStatus(String itemId, Integer kitchenStatus);

    /**
     * 分页查询退款申请
     * 支持按退款状态、订单编号、时间范围筛选
     * @param queryDTO 查询条件
     * @return 分页结果
     */
    PageResult<OrderRefundListVO> queryRefunds(OrderRefundQueryDTO queryDTO);

    /**
     * 获取订单总览统计
     * 包含总订单数、已完成订单数、今日订单数、总销售额、今日销售额
     * @return 统计数据 Map
     */
    Map<String, Object> getOrderStatistics();

    /**
     * 获取退款统计
     * 包含待处理退款数、已退款总金额、退款率、平均处理时长
     * @return 退款统计 VO
     */
    OrderRefundStatsVO getRefundStats();

    /**
     * 获取订单趋势分析数据
     * 包含总览统计、按天时间序列、订单类型分布、同比环比、趋势分析
     * @param startDate 开始日期（yyyy-MM-dd，可空，默认近7天起始）
     * @param endDate 结束日期（yyyy-MM-dd，可空，默认今天）
     * @param granularity 统计粒度：day/week/month（当前仅实现 day）
     * @return 趋势数据 Map
     */
    Map<String, Object> getOrderTrends(String startDate, String endDate, String granularity);

    /**
     * 按日期分组统计（聚合 orders + orders_legacy 两表数据）
     * @param startDate 开始日期（yyyy-MM-dd，可空，默认近7天起始）
     * @param endDate 结束日期（yyyy-MM-dd，可空，默认今天）
     * @param storeName 门店名称（可空，暂未实现门店过滤）
     * @return 每日统计列表
     */
    List<com.foodtraceability.dto.order.DailyStatsVO> getDailyStats(String startDate, String endDate, String storeName);
}
