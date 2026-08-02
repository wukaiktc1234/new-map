package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 事件发件箱实体类（Outbox Pattern）
 * 用于在业务事务中持久化业务事件，确保事件可靠投递，避免服务重启或
 * 线程池满时事件丢失。由事件发布器内部使用，不对外暴露 API。
 *
 * @author foodtraceability
 * @since 2026-07-17
 */
@TableName("event_outbox")
public class EventOutbox implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID（自增）
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 事件类型（如 PurchaseStockInEvent）
     */
    @TableField("event_type")
    private String eventType;

    /**
     * 事件唯一 ID
     */
    @TableField("event_id")
    private String eventId;

    /**
     * 聚合根 ID（如订单号/采购单号）
     */
    @TableField("aggregate_id")
    private String aggregateId;

    /**
     * 事件 JSON 负载
     */
    @TableField("payload")
    private String payload;

    /**
     * 状态（PENDING/PROCESSED/FAILED）
     */
    @TableField("status")
    private String status;

    /**
     * 重试次数
     */
    @TableField("retry_count")
    private Integer retryCount;

    /**
     * 创建时间（由数据库 DEFAULT CURRENT_TIMESTAMP 维护）
     */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /**
     * 处理完成时间
     */
    @TableField("processed_at")
    private LocalDateTime processedAt;

    /**
     * 错误信息
     */
    @TableField("error_message")
    private String errorMessage;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public void setAggregateId(String aggregateId) {
        this.aggregateId = aggregateId;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Override
    public String toString() {
        return "EventOutbox{" +
            "id=" + id +
            ", eventType='" + eventType + '\'' +
            ", eventId='" + eventId + '\'' +
            ", aggregateId='" + aggregateId + '\'' +
            ", status='" + status + '\'' +
            ", retryCount=" + retryCount +
            ", createdAt=" + createdAt +
            ", processedAt=" + processedAt +
            '}';
    }
}
