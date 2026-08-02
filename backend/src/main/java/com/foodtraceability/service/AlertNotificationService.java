package com.foodtraceability.service;

import com.foodtraceability.entity.DeviceAlert;

/**
 * 告警通知服务接口
 * 支持多种告警通知方式，如邮件、短信等
 */
public interface AlertNotificationService {
    
    /**
     * 发送告警通知
     * @param alert 告警信息
     */
    void sendNotification(DeviceAlert alert);
    
    /**
     * 发送邮件告警
     * @param alert 告警信息
     */
    void sendEmailNotification(DeviceAlert alert);
    
    /**
     * 发送短信告警
     * @param alert 告警信息
     */
    void sendSmsNotification(DeviceAlert alert);
    
    /**
     * 发送系统内部通知
     * @param alert 告警信息
     */
    void sendSystemNotification(DeviceAlert alert);
    
    /**
     * 发送WebSocket通知
     * @param alert 告警信息
     */
    void sendWebSocketNotification(DeviceAlert alert);
}
