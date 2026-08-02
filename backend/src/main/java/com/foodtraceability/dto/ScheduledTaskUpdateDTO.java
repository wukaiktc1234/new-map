package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 定时任务更新请求 DTO
 */
@Schema(description = "定时任务更新请求")
public class ScheduledTaskUpdateDTO {
    @NotBlank(message = "任务名称不能为空")
    @Schema(description = "任务名称")
    private String taskName;
    @Schema(description = "任务描述")
    private String description;
    @Schema(description = "任务分组")
    private String taskGroup;
    @Schema(description = "任务执行处理器")
    private String jobHandler;
    @Schema(description = "CRON表达式")
    private String cronExpression;
    @Schema(description = "固定间隔秒数")
    private Integer intervalSeconds;
    /**
     * 任务类型：1=CRON 2=FIXED_RATE 3=ONE_TIME
     */
    @Schema(description = "任务类型：1=CRON 2=FIXED_RATE 3=ONE_TIME")
    private Integer taskType;
    /**
     * 并发策略：0=FORBID禁止 1=ALLOW允许 2=DISCARD丢弃
     */
    @Schema(description = "并发策略：0=FORBID 1=ALLOW 2=DISCARD")
    private Integer concurrentPolicy;
    @Schema(description = "最大重试次数")
    private Integer maxRetryCount;
    @Schema(description = "重试间隔秒数")
    private Integer retryIntervalSec;
    @Schema(description = "超时时间（秒）")
    private Integer timeoutSeconds;
    /**
     * 错失触发策略：1=EXECUTE_IMMEDIATELY 2=SKIP 3=ONCE
     */
    @Schema(description = "错失触发策略：1=立即执行 2=跳过 3=仅一次")
    private Integer misfirePolicy;

    public ScheduledTaskUpdateDTO() {
    }

    public String getTaskName() {
        return this.taskName;
    }

    public String getDescription() {
        return this.description;
    }

    public String getTaskGroup() {
        return this.taskGroup;
    }

    public String getJobHandler() {
        return this.jobHandler;
    }

    public String getCronExpression() {
        return this.cronExpression;
    }

    public Integer getIntervalSeconds() {
        return this.intervalSeconds;
    }

    /**
     * 任务类型：1=CRON 2=FIXED_RATE 3=ONE_TIME
     */
    public Integer getTaskType() {
        return this.taskType;
    }

    /**
     * 并发策略：0=FORBID禁止 1=ALLOW允许 2=DISCARD丢弃
     */
    public Integer getConcurrentPolicy() {
        return this.concurrentPolicy;
    }

    public Integer getMaxRetryCount() {
        return this.maxRetryCount;
    }

    public Integer getRetryIntervalSec() {
        return this.retryIntervalSec;
    }

    public Integer getTimeoutSeconds() {
        return this.timeoutSeconds;
    }

    /**
     * 错失触发策略：1=EXECUTE_IMMEDIATELY 2=SKIP 3=ONCE
     */
    public Integer getMisfirePolicy() {
        return this.misfirePolicy;
    }

    public void setTaskName(final String taskName) {
        this.taskName = taskName;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public void setTaskGroup(final String taskGroup) {
        this.taskGroup = taskGroup;
    }

    public void setJobHandler(final String jobHandler) {
        this.jobHandler = jobHandler;
    }

    public void setCronExpression(final String cronExpression) {
        this.cronExpression = cronExpression;
    }

    public void setIntervalSeconds(final Integer intervalSeconds) {
        this.intervalSeconds = intervalSeconds;
    }

    /**
     * 任务类型：1=CRON 2=FIXED_RATE 3=ONE_TIME
     */
    public void setTaskType(final Integer taskType) {
        this.taskType = taskType;
    }

    /**
     * 并发策略：0=FORBID禁止 1=ALLOW允许 2=DISCARD丢弃
     */
    public void setConcurrentPolicy(final Integer concurrentPolicy) {
        this.concurrentPolicy = concurrentPolicy;
    }

    public void setMaxRetryCount(final Integer maxRetryCount) {
        this.maxRetryCount = maxRetryCount;
    }

    public void setRetryIntervalSec(final Integer retryIntervalSec) {
        this.retryIntervalSec = retryIntervalSec;
    }

    public void setTimeoutSeconds(final Integer timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    /**
     * 错失触发策略：1=EXECUTE_IMMEDIATELY 2=SKIP 3=ONCE
     */
    public void setMisfirePolicy(final Integer misfirePolicy) {
        this.misfirePolicy = misfirePolicy;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof ScheduledTaskUpdateDTO)) return false;
        final ScheduledTaskUpdateDTO other = (ScheduledTaskUpdateDTO) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$intervalSeconds = this.getIntervalSeconds();
        final java.lang.Object other$intervalSeconds = other.getIntervalSeconds();
        if (this$intervalSeconds == null ? other$intervalSeconds != null : !this$intervalSeconds.equals(other$intervalSeconds)) return false;
        final java.lang.Object this$taskType = this.getTaskType();
        final java.lang.Object other$taskType = other.getTaskType();
        if (this$taskType == null ? other$taskType != null : !this$taskType.equals(other$taskType)) return false;
        final java.lang.Object this$concurrentPolicy = this.getConcurrentPolicy();
        final java.lang.Object other$concurrentPolicy = other.getConcurrentPolicy();
        if (this$concurrentPolicy == null ? other$concurrentPolicy != null : !this$concurrentPolicy.equals(other$concurrentPolicy)) return false;
        final java.lang.Object this$maxRetryCount = this.getMaxRetryCount();
        final java.lang.Object other$maxRetryCount = other.getMaxRetryCount();
        if (this$maxRetryCount == null ? other$maxRetryCount != null : !this$maxRetryCount.equals(other$maxRetryCount)) return false;
        final java.lang.Object this$retryIntervalSec = this.getRetryIntervalSec();
        final java.lang.Object other$retryIntervalSec = other.getRetryIntervalSec();
        if (this$retryIntervalSec == null ? other$retryIntervalSec != null : !this$retryIntervalSec.equals(other$retryIntervalSec)) return false;
        final java.lang.Object this$timeoutSeconds = this.getTimeoutSeconds();
        final java.lang.Object other$timeoutSeconds = other.getTimeoutSeconds();
        if (this$timeoutSeconds == null ? other$timeoutSeconds != null : !this$timeoutSeconds.equals(other$timeoutSeconds)) return false;
        final java.lang.Object this$misfirePolicy = this.getMisfirePolicy();
        final java.lang.Object other$misfirePolicy = other.getMisfirePolicy();
        if (this$misfirePolicy == null ? other$misfirePolicy != null : !this$misfirePolicy.equals(other$misfirePolicy)) return false;
        final java.lang.Object this$taskName = this.getTaskName();
        final java.lang.Object other$taskName = other.getTaskName();
        if (this$taskName == null ? other$taskName != null : !this$taskName.equals(other$taskName)) return false;
        final java.lang.Object this$description = this.getDescription();
        final java.lang.Object other$description = other.getDescription();
        if (this$description == null ? other$description != null : !this$description.equals(other$description)) return false;
        final java.lang.Object this$taskGroup = this.getTaskGroup();
        final java.lang.Object other$taskGroup = other.getTaskGroup();
        if (this$taskGroup == null ? other$taskGroup != null : !this$taskGroup.equals(other$taskGroup)) return false;
        final java.lang.Object this$jobHandler = this.getJobHandler();
        final java.lang.Object other$jobHandler = other.getJobHandler();
        if (this$jobHandler == null ? other$jobHandler != null : !this$jobHandler.equals(other$jobHandler)) return false;
        final java.lang.Object this$cronExpression = this.getCronExpression();
        final java.lang.Object other$cronExpression = other.getCronExpression();
        if (this$cronExpression == null ? other$cronExpression != null : !this$cronExpression.equals(other$cronExpression)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof ScheduledTaskUpdateDTO;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $intervalSeconds = this.getIntervalSeconds();
        result = result * PRIME + ($intervalSeconds == null ? 43 : $intervalSeconds.hashCode());
        final java.lang.Object $taskType = this.getTaskType();
        result = result * PRIME + ($taskType == null ? 43 : $taskType.hashCode());
        final java.lang.Object $concurrentPolicy = this.getConcurrentPolicy();
        result = result * PRIME + ($concurrentPolicy == null ? 43 : $concurrentPolicy.hashCode());
        final java.lang.Object $maxRetryCount = this.getMaxRetryCount();
        result = result * PRIME + ($maxRetryCount == null ? 43 : $maxRetryCount.hashCode());
        final java.lang.Object $retryIntervalSec = this.getRetryIntervalSec();
        result = result * PRIME + ($retryIntervalSec == null ? 43 : $retryIntervalSec.hashCode());
        final java.lang.Object $timeoutSeconds = this.getTimeoutSeconds();
        result = result * PRIME + ($timeoutSeconds == null ? 43 : $timeoutSeconds.hashCode());
        final java.lang.Object $misfirePolicy = this.getMisfirePolicy();
        result = result * PRIME + ($misfirePolicy == null ? 43 : $misfirePolicy.hashCode());
        final java.lang.Object $taskName = this.getTaskName();
        result = result * PRIME + ($taskName == null ? 43 : $taskName.hashCode());
        final java.lang.Object $description = this.getDescription();
        result = result * PRIME + ($description == null ? 43 : $description.hashCode());
        final java.lang.Object $taskGroup = this.getTaskGroup();
        result = result * PRIME + ($taskGroup == null ? 43 : $taskGroup.hashCode());
        final java.lang.Object $jobHandler = this.getJobHandler();
        result = result * PRIME + ($jobHandler == null ? 43 : $jobHandler.hashCode());
        final java.lang.Object $cronExpression = this.getCronExpression();
        result = result * PRIME + ($cronExpression == null ? 43 : $cronExpression.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "ScheduledTaskUpdateDTO(taskName=" + this.getTaskName() + ", description=" + this.getDescription() + ", taskGroup=" + this.getTaskGroup() + ", jobHandler=" + this.getJobHandler() + ", cronExpression=" + this.getCronExpression() + ", intervalSeconds=" + this.getIntervalSeconds() + ", taskType=" + this.getTaskType() + ", concurrentPolicy=" + this.getConcurrentPolicy() + ", maxRetryCount=" + this.getMaxRetryCount() + ", retryIntervalSec=" + this.getRetryIntervalSec() + ", timeoutSeconds=" + this.getTimeoutSeconds() + ", misfirePolicy=" + this.getMisfirePolicy() + ")";
    }
}
