package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 任务执行日志实体
 * 对应数据库表 task_execution_log
 */
@TableName("task_execution_log")
@Schema(description = "任务执行日志实体")
public class TaskExecutionLog {
    @TableId(type = IdType.AUTO)
    @Schema(description = "日志ID")
    private Long logId;
    @TableField("task_id")
    @Schema(description = "关联任务ID")
    private Long taskId;
    @TableField("task_name")
    @Schema(description = "任务名称（冗余存储）")
    private String taskName;
    @TableField("task_code")
    @Schema(description = "任务编码（冗余存储）")
    private String taskCode;
    /**
     * 触发类型：1=SCHEDULED定时 2=MANUAL手动 3=RETRY重试 4=COMPENSATE补偿
     */
    @Schema(description = "触发类型：1=SCHEDULED 2=MANUAL 3=RETRY 4=COMPENSATE")
    private Integer triggerType;
    /**
     * 执行状态：0=RUNNING 1=SUCCESS 2=FAILED 3=TIMEOUT 4=CANCELLED 5=PARTIAL
     */
    @Schema(description = "执行状态：0=RUNNING 1=SUCCESS 2=FAILED 3=TIMEOUT 4=CANCELLED 5=PARTIAL")
    private Integer executionStatus;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("start_time")
    @Schema(description = "开始时间")
    private LocalDateTime startTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("end_time")
    @Schema(description = "结束时间")
    private LocalDateTime endTime;
    @TableField("duration_ms")
    @Schema(description = "执行耗时（毫秒）")
    private Long durationMs;
    @Schema(description = "错误信息")
    private String errorMessage;
    @TableField("result_data")
    @Schema(description = "结果数据（JSON格式）")
    private String resultData;
    @Schema(description = "重试次数")
    private Integer retryCount;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    public TaskExecutionLog() {
    }

    public Long getLogId() {
        return this.logId;
    }

    public Long getTaskId() {
        return this.taskId;
    }

    public String getTaskName() {
        return this.taskName;
    }

    public String getTaskCode() {
        return this.taskCode;
    }

    /**
     * 触发类型：1=SCHEDULED定时 2=MANUAL手动 3=RETRY重试 4=COMPENSATE补偿
     */
    public Integer getTriggerType() {
        return this.triggerType;
    }

    /**
     * 执行状态：0=RUNNING 1=SUCCESS 2=FAILED 3=TIMEOUT 4=CANCELLED 5=PARTIAL
     */
    public Integer getExecutionStatus() {
        return this.executionStatus;
    }

    public LocalDateTime getStartTime() {
        return this.startTime;
    }

    public LocalDateTime getEndTime() {
        return this.endTime;
    }

    public Long getDurationMs() {
        return this.durationMs;
    }

    public String getErrorMessage() {
        return this.errorMessage;
    }

    public String getResultData() {
        return this.resultData;
    }

    public Integer getRetryCount() {
        return this.retryCount;
    }

    public LocalDateTime getCreateTime() {
        return this.createTime;
    }

    public void setLogId(final Long logId) {
        this.logId = logId;
    }

    public void setTaskId(final Long taskId) {
        this.taskId = taskId;
    }

    public void setTaskName(final String taskName) {
        this.taskName = taskName;
    }

    public void setTaskCode(final String taskCode) {
        this.taskCode = taskCode;
    }

    /**
     * 触发类型：1=SCHEDULED定时 2=MANUAL手动 3=RETRY重试 4=COMPENSATE补偿
     */
    public void setTriggerType(final Integer triggerType) {
        this.triggerType = triggerType;
    }

    /**
     * 执行状态：0=RUNNING 1=SUCCESS 2=FAILED 3=TIMEOUT 4=CANCELLED 5=PARTIAL
     */
    public void setExecutionStatus(final Integer executionStatus) {
        this.executionStatus = executionStatus;
    }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public void setStartTime(final LocalDateTime startTime) {
        this.startTime = startTime;
    }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public void setEndTime(final LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public void setDurationMs(final Long durationMs) {
        this.durationMs = durationMs;
    }

    public void setErrorMessage(final String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public void setResultData(final String resultData) {
        this.resultData = resultData;
    }

    public void setRetryCount(final Integer retryCount) {
        this.retryCount = retryCount;
    }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public void setCreateTime(final LocalDateTime createTime) {
        this.createTime = createTime;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof TaskExecutionLog)) return false;
        final TaskExecutionLog other = (TaskExecutionLog) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$logId = this.getLogId();
        final java.lang.Object other$logId = other.getLogId();
        if (this$logId == null ? other$logId != null : !this$logId.equals(other$logId)) return false;
        final java.lang.Object this$taskId = this.getTaskId();
        final java.lang.Object other$taskId = other.getTaskId();
        if (this$taskId == null ? other$taskId != null : !this$taskId.equals(other$taskId)) return false;
        final java.lang.Object this$triggerType = this.getTriggerType();
        final java.lang.Object other$triggerType = other.getTriggerType();
        if (this$triggerType == null ? other$triggerType != null : !this$triggerType.equals(other$triggerType)) return false;
        final java.lang.Object this$executionStatus = this.getExecutionStatus();
        final java.lang.Object other$executionStatus = other.getExecutionStatus();
        if (this$executionStatus == null ? other$executionStatus != null : !this$executionStatus.equals(other$executionStatus)) return false;
        final java.lang.Object this$durationMs = this.getDurationMs();
        final java.lang.Object other$durationMs = other.getDurationMs();
        if (this$durationMs == null ? other$durationMs != null : !this$durationMs.equals(other$durationMs)) return false;
        final java.lang.Object this$retryCount = this.getRetryCount();
        final java.lang.Object other$retryCount = other.getRetryCount();
        if (this$retryCount == null ? other$retryCount != null : !this$retryCount.equals(other$retryCount)) return false;
        final java.lang.Object this$taskName = this.getTaskName();
        final java.lang.Object other$taskName = other.getTaskName();
        if (this$taskName == null ? other$taskName != null : !this$taskName.equals(other$taskName)) return false;
        final java.lang.Object this$taskCode = this.getTaskCode();
        final java.lang.Object other$taskCode = other.getTaskCode();
        if (this$taskCode == null ? other$taskCode != null : !this$taskCode.equals(other$taskCode)) return false;
        final java.lang.Object this$startTime = this.getStartTime();
        final java.lang.Object other$startTime = other.getStartTime();
        if (this$startTime == null ? other$startTime != null : !this$startTime.equals(other$startTime)) return false;
        final java.lang.Object this$endTime = this.getEndTime();
        final java.lang.Object other$endTime = other.getEndTime();
        if (this$endTime == null ? other$endTime != null : !this$endTime.equals(other$endTime)) return false;
        final java.lang.Object this$errorMessage = this.getErrorMessage();
        final java.lang.Object other$errorMessage = other.getErrorMessage();
        if (this$errorMessage == null ? other$errorMessage != null : !this$errorMessage.equals(other$errorMessage)) return false;
        final java.lang.Object this$resultData = this.getResultData();
        final java.lang.Object other$resultData = other.getResultData();
        if (this$resultData == null ? other$resultData != null : !this$resultData.equals(other$resultData)) return false;
        final java.lang.Object this$createTime = this.getCreateTime();
        final java.lang.Object other$createTime = other.getCreateTime();
        if (this$createTime == null ? other$createTime != null : !this$createTime.equals(other$createTime)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof TaskExecutionLog;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $logId = this.getLogId();
        result = result * PRIME + ($logId == null ? 43 : $logId.hashCode());
        final java.lang.Object $taskId = this.getTaskId();
        result = result * PRIME + ($taskId == null ? 43 : $taskId.hashCode());
        final java.lang.Object $triggerType = this.getTriggerType();
        result = result * PRIME + ($triggerType == null ? 43 : $triggerType.hashCode());
        final java.lang.Object $executionStatus = this.getExecutionStatus();
        result = result * PRIME + ($executionStatus == null ? 43 : $executionStatus.hashCode());
        final java.lang.Object $durationMs = this.getDurationMs();
        result = result * PRIME + ($durationMs == null ? 43 : $durationMs.hashCode());
        final java.lang.Object $retryCount = this.getRetryCount();
        result = result * PRIME + ($retryCount == null ? 43 : $retryCount.hashCode());
        final java.lang.Object $taskName = this.getTaskName();
        result = result * PRIME + ($taskName == null ? 43 : $taskName.hashCode());
        final java.lang.Object $taskCode = this.getTaskCode();
        result = result * PRIME + ($taskCode == null ? 43 : $taskCode.hashCode());
        final java.lang.Object $startTime = this.getStartTime();
        result = result * PRIME + ($startTime == null ? 43 : $startTime.hashCode());
        final java.lang.Object $endTime = this.getEndTime();
        result = result * PRIME + ($endTime == null ? 43 : $endTime.hashCode());
        final java.lang.Object $errorMessage = this.getErrorMessage();
        result = result * PRIME + ($errorMessage == null ? 43 : $errorMessage.hashCode());
        final java.lang.Object $resultData = this.getResultData();
        result = result * PRIME + ($resultData == null ? 43 : $resultData.hashCode());
        final java.lang.Object $createTime = this.getCreateTime();
        result = result * PRIME + ($createTime == null ? 43 : $createTime.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "TaskExecutionLog(logId=" + this.getLogId() + ", taskId=" + this.getTaskId() + ", taskName=" + this.getTaskName() + ", taskCode=" + this.getTaskCode() + ", triggerType=" + this.getTriggerType() + ", executionStatus=" + this.getExecutionStatus() + ", startTime=" + this.getStartTime() + ", endTime=" + this.getEndTime() + ", durationMs=" + this.getDurationMs() + ", errorMessage=" + this.getErrorMessage() + ", resultData=" + this.getResultData() + ", retryCount=" + this.getRetryCount() + ", createTime=" + this.getCreateTime() + ")";
    }
}
