package com.foodtraceability.event;

import com.foodtraceability.entity.HealthCertificate;
import org.springframework.context.ApplicationEvent;

/**
 * 健康证事件类
 * 用于发布健康证数据变更事件
 */
public class HealthCertificateEvent extends ApplicationEvent {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 事件类型枚举
     */
    public enum EventType {
        CREATE,  // 创建事件
        UPDATE,  // 更新事件
        DELETE,  // 删除事件
        APPROVE, // 审核事件
        REJECT   // 拒绝事件
    }
    
    private EventType eventType;    // 事件类型
    private String operator;        // 操作人
    private String comment;         // 操作备注
    
    /**
     * 构造方法
     * @param source 健康证数据
     * @param eventType 事件类型
     * @param operator 操作人
     * @param comment 操作备注
     */
    public HealthCertificateEvent(HealthCertificate source, EventType eventType, String operator, String comment) {
        super(source);
        this.eventType = eventType;
        this.operator = operator;
        this.comment = comment;
    }
    
    /**
     * 获取健康证数据
     * @return 健康证数据
     */
    public HealthCertificate getHealthCertificate() {
        return (HealthCertificate) getSource();
    }
    
    // getter and setter methods
    public EventType getEventType() {
        return eventType;
    }
    
    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }
    
    public String getOperator() {
        return operator;
    }
    
    public void setOperator(String operator) {
        this.operator = operator;
    }
    
    public String getComment() {
        return comment;
    }
    
    public void setComment(String comment) {
        this.comment = comment;
    }
}