package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 定时任务创建请求 DTO
 */
@Schema(description = "定时任务创建请求")
public class ScheduledTaskCreateDTO {
    @NotBlank(message = "任务名称不能为空")
    @Size(min = 2, max = 100, message = "任务名称长度必须在2-100个字符之间")
    @Schema(description = "任务名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String taskName;
    @NotBlank(message = "任务编码不能为空")
    @Size(min = 2, max = 100, message = "任务编码长度必须在2-100个字符之间")
    @Pattern(regexp = "^[a-z][a-z0-9_-]*$", message = "任务编码必须以小写字母开头，仅允许小写字母、数字、下划线和连字符")
    @Schema(description = "任务编码（唯一标识）", requiredMode = Schema.RequiredMode.REQUIRED)
    private String taskCode;
    @Size(max = 500, message = "任务描述不能超过500个字符")
    @Schema(description = "任务描述")
    private String description;
    @Size(max = 50, message = "任务分组不能超过50个字符")
    @Schema(description = "任务分组，默认DEFAULT")
    private String taskGroup = "DEFAULT";
    @NotBlank(message = "任务处理器不能为空")
    @Size(max = 200, message = "任务处理器不能超过200个字符")
    @Schema(description = "任务执行处理器类名/方法名", requiredMode = Schema.RequiredMode.REQUIRED)
    private String jobHandler;
    /**
     * CRON表达式（taskType=1时必填）
     */
    @Size(max = 100, message = "CRON表达式不能超过100个字符")
    @Pattern(regexp = "^$|^([\\d*/,-?]+\\s){5}[\\d*/,-?]+$|^[\\d*/,-?\\s]+$", message = "CRON表达式格式不正确")
    @Schema(description = "CRON表达式")
    private String cronExpression;
    /**
     * 固定间隔秒数（taskType=2时必填）
     */
    @Min(value = 1, message = "固定间隔秒数最小为1秒")
    @Max(value = 86400, message = "固定间隔秒数最大为86400秒(24小时)")
    @Schema(description = "固定间隔秒数")
    private Integer intervalSeconds;
    /**
     * 任务类型：1=CRON 2=FIXED_RATE 3=ONE_TIME
     */
    @NotNull(message = "任务类型不能为空")
    @Min(value = 1, message = "任务类型值无效")
    @Max(value = 3, message = "任务类型值无效")
    @Schema(description = "任务类型：1=CRON 2=FIXED_RATE 3=ONE_TIME", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer taskType = 1;
    /**
     * 并发策略：0=FORBID禁止 1=ALLOW允许 2=DISCARD丢弃
     */
    @Min(value = 0, message = "并发策略值无效")
    @Max(value = 2, message = "并发策略值无效")
    @Schema(description = "并发策略：0=FORBID 1=ALLOW 2=DISCARD")
    private Integer concurrentPolicy = 0;
    @Min(value = 0, message = "最大重试次数不能小于0")
    @Max(value = 10, message = "最大重试次数不能超过10")
    @Schema(description = "最大重试次数，默认3")
    private Integer maxRetryCount = 3;
    @Min(value = 1, message = "重试间隔秒数最小为1秒")
    @Max(value = 3600, message = "重试间隔秒数最大为3600秒(1小时)")
    @Schema(description = "重试间隔秒数，默认30")
    private Integer retryIntervalSec = 30;
    @Min(value = 1, message = "超时时间最小为1秒")
    @Max(value = 86400, message = "超时时间最大为86400秒(24小时)")
    @Schema(description = "超时时间（秒），默认300")
    private Integer timeoutSeconds = 300;
    /**
     * 错失触发策略：1=EXECUTE_IMMEDIATELY 2=SKIP 3=ONCE
     */
    @Min(value = 1, message = "错失触发策略值无效")
    @Max(value = 3, message = "错失触发策略值无效")
    @Schema(description = "错失触发策略：1=立即执行 2=跳过 3=仅一次")
    private Integer misfirePolicy = 1;

    public ScheduledTaskCreateDTO() {
    }

    public String getTaskName() {
        return this.taskName;
    }

    public String getTaskCode() {
        return this.taskCode;
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

    /**
     * CRON表达式（taskType=1时必填）
     */
    public String getCronExpression() {
        return this.cronExpression;
    }

    /**
     * 固定间隔秒数（taskType=2时必填）
     */
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

    public void setTaskCode(final String taskCode) {
        this.taskCode = taskCode;
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

    /**
     * CRON表达式（taskType=1时必填）
     */
    public void setCronExpression(final String cronExpression) {
        this.cronExpression = cronExpression;
    }

    /**
     * 固定间隔秒数（taskType=2时必填）
     */
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
        if (!(o instanceof ScheduledTaskCreateDTO)) return false;
        final ScheduledTaskCreateDTO other = (ScheduledTaskCreateDTO) o;
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
        final java.lang.Object this$taskCode = this.getTaskCode();
        final java.lang.Object other$taskCode = other.getTaskCode();
        if (this$taskCode == null ? other$taskCode != null : !this$taskCode.equals(other$taskCode)) return false;
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
        return other instanceof ScheduledTaskCreateDTO;
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
        final java.lang.Object $taskCode = this.getTaskCode();
        result = result * PRIME + ($taskCode == null ? 43 : $taskCode.hashCode());
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
        return "ScheduledTaskCreateDTO(taskName=" + this.getTaskName() + ", taskCode=" + this.getTaskCode() + ", description=" + this.getDescription() + ", taskGroup=" + this.getTaskGroup() + ", jobHandler=" + this.getJobHandler() + ", cronExpression=" + this.getCronExpression() + ", intervalSeconds=" + this.getIntervalSeconds() + ", taskType=" + this.getTaskType() + ", concurrentPolicy=" + this.getConcurrentPolicy() + ", maxRetryCount=" + this.getMaxRetryCount() + ", retryIntervalSec=" + this.getRetryIntervalSec() + ", timeoutSeconds=" + this.getTimeoutSeconds() + ", misfirePolicy=" + this.getMisfirePolicy() + ")";
    }
}
