package com.foodtraceability.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/**
 * 事件发布服务
 * 用于发布各种业务事件，触发数据联动
 */
@Service
public class EventPublisher {
    
    private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);
    

    public EventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    private final ApplicationEventPublisher applicationEventPublisher;
    
    /**
     * 发布采购入库完成事件
     */
    public void publishPurchaseStockInEvent(PurchaseStockInEvent event) {
        log.info("发布采购入库完成事件: stockinId={}, orderNo={}, amount={}", 
                event.getStockinId(), event.getOrderNo(), event.getTotalAmount());
        applicationEventPublisher.publishEvent(event);
    }
    
    /**
     * 发布订单完成事件
     */
    public void publishOrderCompletedEvent(OrderCompletedEvent event) {
        log.info("发布订单完成事件: orderId={}, orderNumber={}, amount={}", 
                event.getOrderId(), event.getOrderNumber(), event.getActualAmount());
        applicationEventPublisher.publishEvent(event);
    }
    
    /**
     * 发布薪资发放事件
     */
    public void publishSalaryPaidEvent(SalaryPaidEvent event) {
        log.info("发布薪资发放事件: employeeId={}, employeeName={}, amount={}", 
                event.getEmployeeId(), event.getEmployeeName(), event.getActualSalary());
        applicationEventPublisher.publishEvent(event);
    }
    
    /**
     * 发布追溯码生成事件
     */
    public void publishTraceCodeGeneratedEvent(TraceCodeGeneratedEvent event) {
        log.info("发布追溯码生成事件: traceCode={}, type={}", 
                event.getTraceCode(), event.getTraceCodeType());
        applicationEventPublisher.publishEvent(event);
    }

    /**
     * 发布收货确认完成事件（到货确认 / 无单直收共用）
     */
    public void publishReceiptConfirmationEvent(ReceiptConfirmationCompletedEvent event) {
        log.info("发布收货确认完成事件: confirmationId={}, receiptSource={}",
                event.getConfirmationId(), event.getReceiptSource());
        applicationEventPublisher.publishEvent(event);
    }
}
