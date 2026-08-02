package com.foodtraceability.controller.websocket;

import com.foodtraceability.dto.OrderStatusNotification;
import com.foodtraceability.entity.KitchenOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 订单WebSocket控制器
 * 用于实时推送订单状态变化
 */
@Controller
public class OrderWebSocketController {

    private static final Logger log = LoggerFactory.getLogger(OrderWebSocketController.class);


    public OrderWebSocketController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 推送新订单通知
     * @param kitchenOrder 后厨订单
     */
    public void pushNewOrder(KitchenOrder kitchenOrder) {
        OrderStatusNotification notification = OrderStatusNotification.builder()
                .notificationType("new_order")
                .kitchenOrderId(kitchenOrder.getKitchenOrderId())
                .orderId(kitchenOrder.getOrderId())
                .orderNumber(kitchenOrder.getOrderNumber())
                .orderType(kitchenOrder.getOrderType())
                .tableNumber(kitchenOrder.getTableNumber())
                .status(kitchenOrder.getStatus())
                .totalDishes(kitchenOrder.getTotalDishes())
                .priority(kitchenOrder.getPriority())
                .remark(kitchenOrder.getRemark())
                .notificationTime(LocalDateTime.now())
                .build();

        messagingTemplate.convertAndSend("/topic/orders/new", notification);
        log.info("推送新订单通知: 订单编号={}", kitchenOrder.getOrderNumber());
    }

    /**
     * 推送订单状态变更通知
     * @param kitchenOrder 后厨订单
     * @param previousStatus 变更前状态
     */
    public void pushOrderStatusChange(KitchenOrder kitchenOrder, String previousStatus) {
        OrderStatusNotification notification = OrderStatusNotification.builder()
                .notificationType("status_change")
                .kitchenOrderId(kitchenOrder.getKitchenOrderId())
                .orderId(kitchenOrder.getOrderId())
                .orderNumber(kitchenOrder.getOrderNumber())
                .orderType(kitchenOrder.getOrderType())
                .tableNumber(kitchenOrder.getTableNumber())
                .status(kitchenOrder.getStatus())
                .previousStatus(previousStatus)
                .totalDishes(kitchenOrder.getTotalDishes())
                .priority(kitchenOrder.getPriority())
                .chefName(kitchenOrder.getChefName())
                .remark(kitchenOrder.getRemark())
                .notificationTime(LocalDateTime.now())
                .build();

        messagingTemplate.convertAndSend("/topic/orders/status", notification);
        log.info("推送订单状态变更通知: 订单编号={}, 状态从{}变更为{}", 
                kitchenOrder.getOrderNumber(), previousStatus, kitchenOrder.getStatus());
    }

    /**
     * 推送订单准备取餐通知
     * @param kitchenOrder 后厨订单
     */
    public void pushReadyForPickup(KitchenOrder kitchenOrder) {
        log.info("========== 推送取餐通知开始 ==========");
        log.info("订单信息: orderNumber={}, orderType={}, tableNumber={}", 
            kitchenOrder.getOrderNumber(), 
            kitchenOrder.getOrderType(), 
            kitchenOrder.getTableNumber());
        
        OrderStatusNotification notification = OrderStatusNotification.builder()
                .notificationType("ready_for_pickup")
                .kitchenOrderId(kitchenOrder.getKitchenOrderId())
                .orderId(kitchenOrder.getOrderId())
                .orderNumber(kitchenOrder.getOrderNumber())
                .orderType(kitchenOrder.getOrderType())
                .tableNumber(kitchenOrder.getTableNumber())
                .status(kitchenOrder.getStatus())
                .totalDishes(kitchenOrder.getTotalDishes())
                .priority(kitchenOrder.getPriority())
                .chefName(kitchenOrder.getChefName())
                .remark(kitchenOrder.getRemark())
                .notificationTime(LocalDateTime.now())
                .build();

        log.info("发送消息到 /topic/orders/ready: {}", notification);
        messagingTemplate.convertAndSend("/topic/orders/ready", notification);
        log.info("WebSocket消息发送完成");
        log.info("========== 推送取餐通知结束 ==========");
    }
    
    /**
     * 推送菜品/价格变更通知
     */
    public void pushMenuUpdate() {
        Map<String, Object> notification = new HashMap<>();
        notification.put("notificationType", "menu_update");
        notification.put("notificationTime", LocalDateTime.now());
        
        messagingTemplate.convertAndSend("/topic/menu/update", notification);
        log.info("推送菜单更新通知");
    }

    /**
     * 推送订单退款通知
     * @param kitchenOrder 后厨订单
     */
    public void pushOrderRefund(KitchenOrder kitchenOrder) {
        OrderStatusNotification notification = OrderStatusNotification.builder()
                .notificationType("order_refund")
                .kitchenOrderId(kitchenOrder.getKitchenOrderId())
                .orderId(kitchenOrder.getOrderId())
                .orderNumber(kitchenOrder.getOrderNumber())
                .orderType(kitchenOrder.getOrderType())
                .tableNumber(kitchenOrder.getTableNumber())
                .status("refunded")
                .totalDishes(kitchenOrder.getTotalDishes())
                .priority(kitchenOrder.getPriority())
                .remark(kitchenOrder.getRemark())
                .notificationTime(LocalDateTime.now())
                .build();

        messagingTemplate.convertAndSend("/topic/orders/refund", notification);
        log.info("推送订单退款通知: 订单编号={}", kitchenOrder.getOrderNumber());
    }
}
