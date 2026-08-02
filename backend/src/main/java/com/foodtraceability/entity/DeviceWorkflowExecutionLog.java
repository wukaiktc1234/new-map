package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 设备协同工作流执行日志实体类
 * 记录设备协同工作流的执行情况
 */
@TableName("device_workflow_execution_log")
public class DeviceWorkflowExecutionLog {
    /**
     * 主键ID
     */
    private Long id;
    /**
     * 工作流ID
     */
    private Long workflowId;
    /**
     * 工作流名称
     */
    private String workflowName;
    /**
     * 触发设备ID
     */
    private String triggerDeviceId;
    /**
     * 触发设备类型
     */
    private String triggerDeviceType;
    /**
     * 触发事件类型
     */
    private String triggerEventType;
    /**
     * 触发事件数据JSON
     */
    private String triggerEventData;
    /**
     * 执行状态：0-失败，1-成功
     */
    private Integer executionStatus;
    /**
     * 执行结果JSON
     */
    private String executionResult;
    /**
     * 执行耗时（毫秒）
     */
    private Long executionTime;
    /**
     * 错误信息
     */
    private String errorMessage;
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    public DeviceWorkflowExecutionLog() {
    }

    /**
     * 主键ID
     */
    public Long getId() {
        return this.id;
    }

    /**
     * 工作流ID
     */
    public Long getWorkflowId() {
        return this.workflowId;
    }

    /**
     * 工作流名称
     */
    public String getWorkflowName() {
        return this.workflowName;
    }

    /**
     * 触发设备ID
     */
    public String getTriggerDeviceId() {
        return this.triggerDeviceId;
    }

    /**
     * 触发设备类型
     */
    public String getTriggerDeviceType() {
        return this.triggerDeviceType;
    }

    /**
     * 触发事件类型
     */
    public String getTriggerEventType() {
        return this.triggerEventType;
    }

    /**
     * 触发事件数据JSON
     */
    public String getTriggerEventData() {
        return this.triggerEventData;
    }

    /**
     * 执行状态：0-失败，1-成功
     */
    public Integer getExecutionStatus() {
        return this.executionStatus;
    }

    /**
     * 执行结果JSON
     */
    public String getExecutionResult() {
        return this.executionResult;
    }

    /**
     * 执行耗时（毫秒）
     */
    public Long getExecutionTime() {
        return this.executionTime;
    }

    /**
     * 错误信息
     */
    public String getErrorMessage() {
        return this.errorMessage;
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
     * 主键ID
     */
    public void setId(final Long id) {
        this.id = id;
    }

    /**
     * 工作流ID
     */
    public void setWorkflowId(final Long workflowId) {
        this.workflowId = workflowId;
    }

    /**
     * 工作流名称
     */
    public void setWorkflowName(final String workflowName) {
        this.workflowName = workflowName;
    }

    /**
     * 触发设备ID
     */
    public void setTriggerDeviceId(final String triggerDeviceId) {
        this.triggerDeviceId = triggerDeviceId;
    }

    /**
     * 触发设备类型
     */
    public void setTriggerDeviceType(final String triggerDeviceType) {
        this.triggerDeviceType = triggerDeviceType;
    }

    /**
     * 触发事件类型
     */
    public void setTriggerEventType(final String triggerEventType) {
        this.triggerEventType = triggerEventType;
    }

    /**
     * 触发事件数据JSON
     */
    public void setTriggerEventData(final String triggerEventData) {
        this.triggerEventData = triggerEventData;
    }

    /**
     * 执行状态：0-失败，1-成功
     */
    public void setExecutionStatus(final Integer executionStatus) {
        this.executionStatus = executionStatus;
    }

    /**
     * 执行结果JSON
     */
    public void setExecutionResult(final String executionResult) {
        this.executionResult = executionResult;
    }

    /**
     * 执行耗时（毫秒）
     */
    public void setExecutionTime(final Long executionTime) {
        this.executionTime = executionTime;
    }

    /**
     * 错误信息
     */
    public void setErrorMessage(final String errorMessage) {
        this.errorMessage = errorMessage;
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
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof DeviceWorkflowExecutionLog)) return false;
        final DeviceWorkflowExecutionLog other = (DeviceWorkflowExecutionLog) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$workflowId = this.getWorkflowId();
        final java.lang.Object other$workflowId = other.getWorkflowId();
        if (this$workflowId == null ? other$workflowId != null : !this$workflowId.equals(other$workflowId)) return false;
        final java.lang.Object this$executionStatus = this.getExecutionStatus();
        final java.lang.Object other$executionStatus = other.getExecutionStatus();
        if (this$executionStatus == null ? other$executionStatus != null : !this$executionStatus.equals(other$executionStatus)) return false;
        final java.lang.Object this$executionTime = this.getExecutionTime();
        final java.lang.Object other$executionTime = other.getExecutionTime();
        if (this$executionTime == null ? other$executionTime != null : !this$executionTime.equals(other$executionTime)) return false;
        final java.lang.Object this$workflowName = this.getWorkflowName();
        final java.lang.Object other$workflowName = other.getWorkflowName();
        if (this$workflowName == null ? other$workflowName != null : !this$workflowName.equals(other$workflowName)) return false;
        final java.lang.Object this$triggerDeviceId = this.getTriggerDeviceId();
        final java.lang.Object other$triggerDeviceId = other.getTriggerDeviceId();
        if (this$triggerDeviceId == null ? other$triggerDeviceId != null : !this$triggerDeviceId.equals(other$triggerDeviceId)) return false;
        final java.lang.Object this$triggerDeviceType = this.getTriggerDeviceType();
        final java.lang.Object other$triggerDeviceType = other.getTriggerDeviceType();
        if (this$triggerDeviceType == null ? other$triggerDeviceType != null : !this$triggerDeviceType.equals(other$triggerDeviceType)) return false;
        final java.lang.Object this$triggerEventType = this.getTriggerEventType();
        final java.lang.Object other$triggerEventType = other.getTriggerEventType();
        if (this$triggerEventType == null ? other$triggerEventType != null : !this$triggerEventType.equals(other$triggerEventType)) return false;
        final java.lang.Object this$triggerEventData = this.getTriggerEventData();
        final java.lang.Object other$triggerEventData = other.getTriggerEventData();
        if (this$triggerEventData == null ? other$triggerEventData != null : !this$triggerEventData.equals(other$triggerEventData)) return false;
        final java.lang.Object this$executionResult = this.getExecutionResult();
        final java.lang.Object other$executionResult = other.getExecutionResult();
        if (this$executionResult == null ? other$executionResult != null : !this$executionResult.equals(other$executionResult)) return false;
        final java.lang.Object this$errorMessage = this.getErrorMessage();
        final java.lang.Object other$errorMessage = other.getErrorMessage();
        if (this$errorMessage == null ? other$errorMessage != null : !this$errorMessage.equals(other$errorMessage)) return false;
        final java.lang.Object this$createdAt = this.getCreatedAt();
        final java.lang.Object other$createdAt = other.getCreatedAt();
        if (this$createdAt == null ? other$createdAt != null : !this$createdAt.equals(other$createdAt)) return false;
        final java.lang.Object this$updatedAt = this.getUpdatedAt();
        final java.lang.Object other$updatedAt = other.getUpdatedAt();
        if (this$updatedAt == null ? other$updatedAt != null : !this$updatedAt.equals(other$updatedAt)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof DeviceWorkflowExecutionLog;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $workflowId = this.getWorkflowId();
        result = result * PRIME + ($workflowId == null ? 43 : $workflowId.hashCode());
        final java.lang.Object $executionStatus = this.getExecutionStatus();
        result = result * PRIME + ($executionStatus == null ? 43 : $executionStatus.hashCode());
        final java.lang.Object $executionTime = this.getExecutionTime();
        result = result * PRIME + ($executionTime == null ? 43 : $executionTime.hashCode());
        final java.lang.Object $workflowName = this.getWorkflowName();
        result = result * PRIME + ($workflowName == null ? 43 : $workflowName.hashCode());
        final java.lang.Object $triggerDeviceId = this.getTriggerDeviceId();
        result = result * PRIME + ($triggerDeviceId == null ? 43 : $triggerDeviceId.hashCode());
        final java.lang.Object $triggerDeviceType = this.getTriggerDeviceType();
        result = result * PRIME + ($triggerDeviceType == null ? 43 : $triggerDeviceType.hashCode());
        final java.lang.Object $triggerEventType = this.getTriggerEventType();
        result = result * PRIME + ($triggerEventType == null ? 43 : $triggerEventType.hashCode());
        final java.lang.Object $triggerEventData = this.getTriggerEventData();
        result = result * PRIME + ($triggerEventData == null ? 43 : $triggerEventData.hashCode());
        final java.lang.Object $executionResult = this.getExecutionResult();
        result = result * PRIME + ($executionResult == null ? 43 : $executionResult.hashCode());
        final java.lang.Object $errorMessage = this.getErrorMessage();
        result = result * PRIME + ($errorMessage == null ? 43 : $errorMessage.hashCode());
        final java.lang.Object $createdAt = this.getCreatedAt();
        result = result * PRIME + ($createdAt == null ? 43 : $createdAt.hashCode());
        final java.lang.Object $updatedAt = this.getUpdatedAt();
        result = result * PRIME + ($updatedAt == null ? 43 : $updatedAt.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "DeviceWorkflowExecutionLog(id=" + this.getId() + ", workflowId=" + this.getWorkflowId() + ", workflowName=" + this.getWorkflowName() + ", triggerDeviceId=" + this.getTriggerDeviceId() + ", triggerDeviceType=" + this.getTriggerDeviceType() + ", triggerEventType=" + this.getTriggerEventType() + ", triggerEventData=" + this.getTriggerEventData() + ", executionStatus=" + this.getExecutionStatus() + ", executionResult=" + this.getExecutionResult() + ", executionTime=" + this.getExecutionTime() + ", errorMessage=" + this.getErrorMessage() + ", createdAt=" + this.getCreatedAt() + ", updatedAt=" + this.getUpdatedAt() + ")";
    }
}
