package com.foodtraceability.entity;

import java.time.LocalDateTime;

/**
 * 告警实体类
 * 用于存储系统产生的告警信息
 */
public class Alert {
    
    /**
     * 告警ID
     */
    private String id;
    
    /**
     * 告警名称
     */
    private String name;
    
    /**
     * 告警描述
     */
    private String description;
    
    /**
     * 告警级别
     * ERROR, WARNING, INFO
     */
    private String severity;
    
    /**
     * 告警状态
     * ACTIVE, RESOLVED, ACKNOWLEDGED
     */
    private String status;
    
    /**
     * 告警值
     */
    private double value;
    
    /**
     * 告警阈值
     */
    private double threshold;
    
    /**
     * 告警来源
     */
    private String source;
    
    /**
     * 告警时间
     */
    private LocalDateTime alertTime;
    
    /**
     * 解决时间
     */
    private LocalDateTime resolveTime;
    
    /**
     * 确认时间
     */
    private LocalDateTime acknowledgeTime;
    
    /**
     * 确认人
     */
    private String acknowledgedBy;
    
    /**
     * 解决人
     */
    private String resolvedBy;
    
    /**
     * 解决描述
     */
    private String resolveDescription;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public double getThreshold() {
        return threshold;
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public LocalDateTime getAlertTime() {
        return alertTime;
    }

    public void setAlertTime(LocalDateTime alertTime) {
        this.alertTime = alertTime;
    }

    public LocalDateTime getResolveTime() {
        return resolveTime;
    }

    public void setResolveTime(LocalDateTime resolveTime) {
        this.resolveTime = resolveTime;
    }

    public LocalDateTime getAcknowledgeTime() {
        return acknowledgeTime;
    }

    public void setAcknowledgeTime(LocalDateTime acknowledgeTime) {
        this.acknowledgeTime = acknowledgeTime;
    }

    public String getAcknowledgedBy() {
        return acknowledgedBy;
    }

    public void setAcknowledgedBy(String acknowledgedBy) {
        this.acknowledgedBy = acknowledgedBy;
    }

    public String getResolvedBy() {
        return resolvedBy;
    }

    public void setResolvedBy(String resolvedBy) {
        this.resolvedBy = resolvedBy;
    }

    public String getResolveDescription() {
        return resolveDescription;
    }

    public void setResolveDescription(String resolveDescription) {
        this.resolveDescription = resolveDescription;
    }
}
