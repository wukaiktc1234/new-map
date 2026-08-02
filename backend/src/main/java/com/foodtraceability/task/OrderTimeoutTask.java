package com.foodtraceability.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.foodtraceability.entity.Order;
import com.foodtraceability.entity.OrderItem;
import com.foodtraceability.mapper.FoodMapper;
import com.foodtraceability.mapper.FoodNewMapper;
import com.foodtraceability.mapper.OperationLogMapper;
import com.foodtraceability.mapper.OrderItemMapper;
import com.foodtraceability.mapper.OrderMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 超时未支付订单自动取消定时任务
 * 每5分钟执行一次，检查并取消超过30分钟未支付的订单
 */
@Component
public class OrderTimeoutTask {

    private static final Logger log = LoggerFactory.getLogger(OrderTimeoutTask.class);


    public OrderTimeoutTask(OrderMapper orderMapper, OrderItemMapper orderItemMapper, FoodMapper foodMapper, FoodNewMapper foodNewMapper, OperationLogMapper operationLogMapper) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.foodMapper = foodMapper;
        this.foodNewMapper = foodNewMapper;
        this.operationLogMapper = operationLogMapper;
    }

    /** 订单超时时间（分钟） */
    private static final long ORDER_TIMEOUT_MINUTES = 30;

    /** 待支付状态 */
    private static final int STATUS_PENDING_PAYMENT = 0;

    /** 已取消状态 */
    private static final int STATUS_CANCELLED = 5;

    private final OrderMapper orderMapper;

    private final OrderItemMapper orderItemMapper;

    private final FoodMapper foodMapper;

    /** foods 表 Mapper（菜品展示来源），用于同步回补 foods 表库存，保证两表一致 */
    private final FoodNewMapper foodNewMapper;

    private final OperationLogMapper operationLogMapper;

    /**
     * 取消超时未支付的订单
     * 每5分钟执行一次，启动后延迟1分钟首次执行
     */
    @Scheduled(fixedRate = 300000, initialDelay = 60000)
    public void cancelExpiredOrders() {
        try {
            // 计算超时阈值：当前时间减去30分钟
            LocalDateTime expireTime = LocalDateTime.now().minusMinutes(ORDER_TIMEOUT_MINUTES);

            // 查询待支付且超过超时时间的订单
            List<Order> expiredOrders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                    .eq(Order::getOrderStatus, STATUS_PENDING_PAYMENT)
                    .lt(Order::getCreateTime, expireTime)
            );

            if (expiredOrders == null || expiredOrders.isEmpty()) {
                return;
            }

            log.info("发现{}个超时未支付订单需要取消", expiredOrders.size());

            // 逐个处理超时订单
            for (Order order : expiredOrders) {
                cancelOrderWithStockRefund(order);
            }
        } catch (Exception e) {
            log.error("超时订单取消任务执行异常", e);
        }
    }

    /**
     * 取消单个订单并回补库存
     * @param order 需要取消的订单
     */
    @Transactional(rollbackFor = Exception.class)
    private void cancelOrderWithStockRefund(Order order) {
        try {
            // 1. 更新订单状态为已取消
            Order updateOrder = new Order();
            updateOrder.setOrderId(order.getOrderId());
            updateOrder.setOrderStatus(STATUS_CANCELLED);
            orderMapper.updateById(updateOrder);

            // 2. 查询订单项并回补库存
            List<OrderItem> orderItems = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>()
                    .eq(OrderItem::getOrderId, order.getOrderId())
            );

            if (orderItems != null && !orderItems.isEmpty()) {
                int restoredCount = 0;
                for (OrderItem item : orderItems) {
                    if (item.getFoodId() != null && item.getQuantity() != null && item.getQuantity() > 0) {
                        // 调用库存回补方法（food 表 + foods 表，保证两表一致）
                        foodMapper.addStock(item.getFoodId(), item.getQuantity());
                        foodNewMapper.addStock(item.getFoodId(), item.getQuantity());
                        restoredCount++;
                    }
                }
                // 3. 记录操作日志
                log.info("超时取消订单成功: orderId={}, 回补库存项数={}",
                    order.getOrderId(), restoredCount);
            } else {
                log.info("超时取消订单成功（无订单项）: orderId={}", order.getOrderId());
            }
        } catch (Exception e) {
            log.error("处理超时订单异常: orderId={}", order.getOrderId(), e);
        }
    }
}
