package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 数据联动事件表
 * 用于跟踪多系统间的数据联动事件，确保数据一致性
 */
@TableName("data_link_event")
@Schema(description = "数据联动事件实体")
public class DataLinkEvent implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 事件ID，主键
     */
    @TableId(value = "event_id", type = IdType.ASSIGN_ID)
    @Schema(description = "事件ID", example = "1")
    private Long eventId;
    /**
     * 源系统标识
     */
    @TableField("source_system")
    @Schema(description = "源系统标识", example = "product")
    private String sourceSystem;
    /**
     * 目标系统标识
     */
    @TableField("target_system")
    @Schema(description = "目标系统标识", example = "inventory")
    private String targetSystem;
    /**
     * 业务类型
     */
    @TableField("business_type")
    @Schema(description = "业务类型", example = "product_create")
    private String businessType;
    /**
     * 业务ID
     */
    @TableField("business_id")
    @Schema(description = "业务ID", example = "P001")
    private String businessId;
    /**
     * 事件类型（CREATE/UPDATE/DELETE）
     */
    @TableField("event_type")
    @Schema(description = "事件类型", example = "CREATE")
    private String eventType;
    /**
     * 事件数据（JSON格式）
     */
    @TableField("event_data")
    @Schema(description = "事件数据", example = "{\"productId\":\"P001\",\"productName\":\"测试产品\"}")
    private String eventData;
    /**
     * 处理状态（PENDING/COMPLETED/FAILED/RETRYING）
     */
    @TableField("status")
    @Schema(description = "处理状态", example = "PENDING")
    private String status;
    /**
     * 重试次数
     */
    @TableField("retry_count")
    @Schema(description = "重试次数", example = "0")
    private Integer retryCount;
    /**
     * 错误信息
     */
    @TableField("error_msg")
    @Schema(description = "错误信息")
    private String errorMsg;
    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    public DataLinkEvent() {
    }

    /**
     * 事件ID，主键
     */
    public Long getEventId() {
        return this.eventId;
    }

    /**
     * 源系统标识
     */
    public String getSourceSystem() {
        return this.sourceSystem;
    }

    /**
     * 目标系统标识
     */
    public String getTargetSystem() {
        return this.targetSystem;
    }

    /**
     * 业务类型
     */
    public String getBusinessType() {
        return this.businessType;
    }

    /**
     * 业务ID
     */
    public String getBusinessId() {
        return this.businessId;
    }

    /**
     * 事件类型（CREATE/UPDATE/DELETE）
     */
    public String getEventType() {
        return this.eventType;
    }

    /**
     * 事件数据（JSON格式）
     */
    public String getEventData() {
        return this.eventData;
    }

    /**
     * 处理状态（PENDING/COMPLETED/FAILED/RETRYING）
     */
    public String getStatus() {
        return this.status;
    }

    /**
     * 重试次数
     */
    public Integer getRetryCount() {
        return this.retryCount;
    }

    /**
     * 错误信息
     */
    public String getErrorMsg() {
        return this.errorMsg;
    }

    /**
     * 创建时间
     */
    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    /**
     * 更新时间
     */
    public LocalDateTime getUpdatedAt() {
        return this.updatedAt;
    }

    /**
     * 事件ID，主键
     */
    public void setEventId(final Long eventId) {
        this.eventId = eventId;
    }

    /**
     * 源系统标识
     */
    public void setSourceSystem(final String sourceSystem) {
        this.sourceSystem = sourceSystem;
    }

    /**
     * 目标系统标识
     */
    public void setTargetSystem(final String targetSystem) {
        this.targetSystem = targetSystem;
    }

    /**
     * 业务类型
     */
    public void setBusinessType(final String businessType) {
        this.businessType = businessType;
    }

    /**
     * 业务ID
     */
    public void setBusinessId(final String businessId) {
        this.businessId = businessId;
    }

    /**
     * 事件类型（CREATE/UPDATE/DELETE）
     */
    public void setEventType(final String eventType) {
        this.eventType = eventType;
    }

    /**
     * 事件数据（JSON格式）
     */
    public void setEventData(final String eventData) {
        this.eventData = eventData;
    }

    /**
     * 处理状态（PENDING/COMPLETED/FAILED/RETRYING）
     */
    public void setStatus(final String status) {
        this.status = status;
    }

    /**
     * 重试次数
     */
    public void setRetryCount(final Integer retryCount) {
        this.retryCount = retryCount;
    }

    /**
     * 错误信息
     */
    public void setErrorMsg(final String errorMsg) {
        this.errorMsg = errorMsg;
    }

    /**
     * 创建时间
     */
    public void setCreatedAt(final LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * 更新时间
     */
    public void setUpdatedAt(final LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "DataLinkEvent(eventId=" + this.getEventId() + ", sourceSystem=" + this.getSourceSystem() + ", targetSystem=" + this.getTargetSystem() + ", businessType=" + this.getBusinessType() + ", businessId=" + this.getBusinessId() + ", eventType=" + this.getEventType() + ", eventData=" + this.getEventData() + ", status=" + this.getStatus() + ", retryCount=" + this.getRetryCount() + ", errorMsg=" + this.getErrorMsg() + ", createdAt=" + this.getCreatedAt() + ", updatedAt=" + this.getUpdatedAt() + ")";
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof DataLinkEvent)) return false;
        final DataLinkEvent other = (DataLinkEvent) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$eventId = this.getEventId();
        final java.lang.Object other$eventId = other.getEventId();
        if (this$eventId == null ? other$eventId != null : !this$eventId.equals(other$eventId)) return false;
        final java.lang.Object this$retryCount = this.getRetryCount();
        final java.lang.Object other$retryCount = other.getRetryCount();
        if (this$retryCount == null ? other$retryCount != null : !this$retryCount.equals(other$retryCount)) return false;
        final java.lang.Object this$sourceSystem = this.getSourceSystem();
        final java.lang.Object other$sourceSystem = other.getSourceSystem();
        if (this$sourceSystem == null ? other$sourceSystem != null : !this$sourceSystem.equals(other$sourceSystem)) return false;
        final java.lang.Object this$targetSystem = this.getTargetSystem();
        final java.lang.Object other$targetSystem = other.getTargetSystem();
        if (this$targetSystem == null ? other$targetSystem != null : !this$targetSystem.equals(other$targetSystem)) return false;
        final java.lang.Object this$businessType = this.getBusinessType();
        final java.lang.Object other$businessType = other.getBusinessType();
        if (this$businessType == null ? other$businessType != null : !this$businessType.equals(other$businessType)) return false;
        final java.lang.Object this$businessId = this.getBusinessId();
        final java.lang.Object other$businessId = other.getBusinessId();
        if (this$businessId == null ? other$businessId != null : !this$businessId.equals(other$businessId)) return false;
        final java.lang.Object this$eventType = this.getEventType();
        final java.lang.Object other$eventType = other.getEventType();
        if (this$eventType == null ? other$eventType != null : !this$eventType.equals(other$eventType)) return false;
        final java.lang.Object this$eventData = this.getEventData();
        final java.lang.Object other$eventData = other.getEventData();
        if (this$eventData == null ? other$eventData != null : !this$eventData.equals(other$eventData)) return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
        final java.lang.Object this$errorMsg = this.getErrorMsg();
        final java.lang.Object other$errorMsg = other.getErrorMsg();
        if (this$errorMsg == null ? other$errorMsg != null : !this$errorMsg.equals(other$errorMsg)) return false;
        final java.lang.Object this$createdAt = this.getCreatedAt();
        final java.lang.Object other$createdAt = other.getCreatedAt();
        if (this$createdAt == null ? other$createdAt != null : !this$createdAt.equals(other$createdAt)) return false;
        final java.lang.Object this$updatedAt = this.getUpdatedAt();
        final java.lang.Object other$updatedAt = other.getUpdatedAt();
        if (this$updatedAt == null ? other$updatedAt != null : !this$updatedAt.equals(other$updatedAt)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof DataLinkEvent;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $eventId = this.getEventId();
        result = result * PRIME + ($eventId == null ? 43 : $eventId.hashCode());
        final java.lang.Object $retryCount = this.getRetryCount();
        result = result * PRIME + ($retryCount == null ? 43 : $retryCount.hashCode());
        final java.lang.Object $sourceSystem = this.getSourceSystem();
        result = result * PRIME + ($sourceSystem == null ? 43 : $sourceSystem.hashCode());
        final java.lang.Object $targetSystem = this.getTargetSystem();
        result = result * PRIME + ($targetSystem == null ? 43 : $targetSystem.hashCode());
        final java.lang.Object $businessType = this.getBusinessType();
        result = result * PRIME + ($businessType == null ? 43 : $businessType.hashCode());
        final java.lang.Object $businessId = this.getBusinessId();
        result = result * PRIME + ($businessId == null ? 43 : $businessId.hashCode());
        final java.lang.Object $eventType = this.getEventType();
        result = result * PRIME + ($eventType == null ? 43 : $eventType.hashCode());
        final java.lang.Object $eventData = this.getEventData();
        result = result * PRIME + ($eventData == null ? 43 : $eventData.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $errorMsg = this.getErrorMsg();
        result = result * PRIME + ($errorMsg == null ? 43 : $errorMsg.hashCode());
        final java.lang.Object $createdAt = this.getCreatedAt();
        result = result * PRIME + ($createdAt == null ? 43 : $createdAt.hashCode());
        final java.lang.Object $updatedAt = this.getUpdatedAt();
        result = result * PRIME + ($updatedAt == null ? 43 : $updatedAt.hashCode());
        return result;
    }
}
