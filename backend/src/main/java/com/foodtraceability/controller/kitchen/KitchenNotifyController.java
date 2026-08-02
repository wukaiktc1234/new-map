package com.foodtraceability.controller.kitchen;

import com.foodtraceability.dto.ApiResponse;
import com.foodtraceability.dto.PickupNotificationDTO;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/v1/kitchen/notify")
public class KitchenNotifyController {


    public KitchenNotifyController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    private final SimpMessagingTemplate messagingTemplate;

    @PostMapping("/notify-ready")
    public ApiResponse<String> notifyReadyForPickup(@RequestBody PickupNotificationDTO notification) {
        System.out.println("[KitchenNotifyController] 收到取餐通知请求");
        System.out.println("  orderNumber: " + notification.getOrderNumber());
        System.out.println("  tableNumber: " + notification.getTableNumber());
        System.out.println("  orderType: " + notification.getOrderType());
        System.out.println("  totalDishes: " + notification.getTotalDishes());

        Map<String, Object> message = new HashMap<>();
        message.put("orderNumber", notification.getOrderNumber());
        message.put("tableNumber", notification.getTableNumber());
        message.put("orderType", notification.getOrderType());
        message.put("totalDishes", notification.getTotalDishes());
        message.put("timestamp", System.currentTimeMillis());

        System.out.println("[KitchenNotifyController] 发送WebSocket消息到 /topic/orders/ready");
        messagingTemplate.convertAndSend("/topic/orders/ready", message);
        System.out.println("[KitchenNotifyController] WebSocket消息发送完成");

        return ApiResponse.success("取餐通知已发送");
    }

    @PostMapping("/notify-new-order")
    public ApiResponse<String> notifyNewOrder(@RequestBody Map<String, Object> orderInfo) {
        System.out.println("[KitchenNotifyController] 收到新订单通知请求: " + orderInfo);
        orderInfo.put("timestamp", System.currentTimeMillis());
        messagingTemplate.convertAndSend("/topic/orders/new", orderInfo);
        return ApiResponse.success("新订单通知已发送");
    }
}
