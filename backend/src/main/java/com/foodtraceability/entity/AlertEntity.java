package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统告警实体类
 * 持久化存储系统产生的告警信息，替代原内存HashMap存储
 */
@TableName("alert")
public class AlertEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "alert_id", type = IdType.AUTO)
    private Long alertId;

    @TableField("alert_name")
    private String name;

    @TableField("description")
    private String description;

    @TableField("severity")
    private String severity;

    @TableField("alert_status")
    private String status;

    @TableField("alert_value")
    private Double value;

    @TableField("threshold_value")
    private Double threshold;

    @TableField("source")
    private String source;

    @TableField("source_type")
    private String sourceType;

    @TableField("source_id")
    private String sourceId;

    @TableField("alert_time")
    private LocalDateTime alertTime;

    @TableField("resolve_time")
    private LocalDateTime resolveTime;

    @TableField("acknowledge_time")
    private LocalDateTime acknowledgeTime;

    @TableField("acknowledged_by")
    private String acknowledgedBy;

    @TableField("resolved_by")
    private String resolvedBy;

    @TableField("resolve_description")
    private String resolveDescription;

    @TableField("assigned_to")
    private Long assignedTo;

    @Version
    @TableField("version")
    private Long version;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    public Long getAlertId() {
        return alertId;
    }

    public void setAlertId(Long alertId) {
        this.alertId = alertId;
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

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public Double getThreshold() {
        return threshold;
    }

    public void setThreshold(Double threshold) {
        this.threshold = threshold;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
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

    public Long getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(Long assignedTo) {
        this.assignedTo = assignedTo;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    @Override
    public String toString() {
        return "Alert{" +
            "alertId=" + alertId +
            ", name='" + name + '\'' +
            ", severity='" + severity + '\'' +
            ", status='" + status + '\'' +
            ", source='" + source + '\'' +
            '}';
    }
}
