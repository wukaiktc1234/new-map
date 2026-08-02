package com.foodtraceability.event.listener;

import com.foodtraceability.event.OrderCreatedEvent;
import com.foodtraceability.service.KitchenScanService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class OrderCreatedEventListener {
    
    private static final Logger log = LoggerFactory.getLogger(OrderCreatedEventListener.class);
    

    public OrderCreatedEventListener(KitchenScanService kitchenScanService) {
        this.kitchenScanService = kitchenScanService;
    }

    private final KitchenScanService kitchenScanService;
    
    @Async
    @EventListener
    public void handleOrderCreatedEvent(OrderCreatedEvent event) {
        log.info("处理订单创建事件: kitchenOrderId={}, orderNumber={}", 
                event.getKitchenOrderId(), event.getOrderNumber());
        
        try {
            lockMaterialsForOrder(event);
            
            log.info("订单创建事件处理完成: kitchenOrderId={}", event.getKitchenOrderId());
        } catch (Exception e) {
            log.error("处理订单创建事件失败: {}", e.getMessage(), e);
        }
    }
    
    private void lockMaterialsForOrder(OrderCreatedEvent event) {
        log.info("锁定订单原料: kitchenOrderId={}", event.getKitchenOrderId());
        
        try {
            kitchenScanService.lockMaterialsForOrder(event.getKitchenOrderId());
            log.info("订单原料锁定成功: kitchenOrderId={}", event.getKitchenOrderId());
        } catch (Exception e) {
            log.error("锁定订单原料失败: {}", e.getMessage(), e);
        }
    }
}
