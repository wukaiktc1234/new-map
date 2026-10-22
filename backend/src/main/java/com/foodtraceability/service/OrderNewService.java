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
     * 由后厨完成链路触发的主订单完成（2026-10-23 状态机闭环）。
     *
     * <p>与 {@link #completeOrder(String)} 的区别：<b>不扣减库存、不写成本</b>。
     * 扣料与成本由 {@code deductMaterialsForServe} 负责（口径 A：完成制作即扣料），
     * 两者同时调用会导致同一份库存被扣两次。</p>
     *
     * <p>职责：未确认订单先置「已确认」→ 置「已完成」→ 解锁桌台。幂等。</p>
     *
     * @param orderId 主订单 ID
     */
    void markCompletedFromKitchen(String orderId);

    /**
     * 落库订单成本（cost_record），供后厨完成链路在扣料后调用。
     *
     * @param orderId  主订单 ID
     * @param totalCostFen 扣料实际出库成本（分）；<=0 时跳过
     */
    void persistOrderCostFromDeduction(String orderId, long totalCostFen);

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
     * 分页查询POS终端订单（orders 表，canonical read）
     * W1-EC-04C: 从 orders_legacy 迁移到 orders 表。
     * orders 表已包含所有订单（POS端+管理端），order_status/payment_status 为 canonical 值。
     * @param queryDTO 查询条件（orderCode/orderStatus/startTime/endTime/storeId 等）
     * @return 分页结果（OrderVO 列表）
     */
    Page<OrderVO> queryPosOrders(OrderQueryDTO queryDTO);

    /**
     * 根据订单编号获取POS终端订单详情（含菜品明细）
     * W1-EC-04C: 从 orders_legacy + order_items_legacy 迁移到 orders + order_items（canonical read）。
     * orders 表已包含所有订单（POS端+管理端），order_status/payment_status/orderType 为 canonical 值。
     * @param orderNumber 订单编号（如 ORD20260713002），非订单ID
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
     * KDS 出餐时扣减原料库存（P0 严格版：禁止任何"部分跳过后静默成功"）
     * 幂等：通过 kitchenOrder.materialConsumed 原子占位防止重复扣减
     * 原子性：所有应扣原料在同一事务内扣减，任一原料失败 → 整体失败（MaterialDeductionFailureException），
     *         主事务回滚、material_consumed 恢复 0，并由独立 REQUIRES_NEW 事务写入 FAILED 审计行
     * @param kitchenOrderId 后厨订单ID
     * @param sourceRef 来源引用（写入库存流水）
     * @param trayCode 触发服务时的托盘码（可空，写入失败审计行）
     */
    long deductMaterialsForServe(String kitchenOrderId, String sourceRef, String trayCode);

    /**
     * 按日期分组统计（聚合 orders + orders_legacy 两表数据）
     * @param startDate 开始日期（yyyy-MM-dd，可空，默认近7天起始）
     * @param endDate 结束日期（yyyy-MM-dd，可空，默认今天）
     * @param storeName 门店名称（可空，暂未实现门店过滤）
     * @return 每日统计列表
     */
    List<com.foodtraceability.dto.order.DailyStatsVO> getDailyStats(String startDate, String endDate, String storeName);
}
