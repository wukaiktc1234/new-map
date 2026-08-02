package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 定时任务定义实体
 * 对应数据库表 scheduled_task
 */
@TableName("scheduled_task")
@Schema(description = "定时任务定义实体")
public class ScheduledTask {
    @TableId(type = IdType.AUTO)
    @Schema(description = "任务ID")
    private Long taskId;
    @TableField("task_name")
    @Schema(description = "任务名称")
    private String taskName;
    @TableField("task_code")
    @Schema(description = "任务编码，唯一标识")
    private String taskCode;
    @Schema(description = "任务描述")
    private String description;
    @TableField("task_group")
    @Schema(description = "任务分组")
    private String taskGroup;
    @TableField("job_handler")
    @Schema(description = "任务执行处理器")
    private String jobHandler;
    @TableField("cron_expression")
    @Schema(description = "CRON表达式")
    private String cronExpression;
    @TableField("interval_seconds")
    @Schema(description = "固定间隔秒数")
    private Integer intervalSeconds;
    /**
     * 任务类型：1=CRON 2=FIXED_RATE 3=ONE_TIME
     */
    @Schema(description = "任务类型：1=CRON 2=FIXED_RATE 3=ONE_TIME")
    private Integer taskType;
    /**
     * 任务状态：0=CREATED 1=ENABLED 2=PAUSED 3=DISABLED 4=ERROR
     */
    @Schema(description = "任务状态：0=CREATED 1=ENABLED 2=PAUSED 3=DISABLED 4=ERROR")
    private Integer status;
    /**
     * 执行状态：0=IDLE 1=PENDING 2=RUNNING
     */
    @Schema(description = "执行状态：0=IDLE 1=PENDING 2=RUNNING")
    private Integer executionStatus;
    /**
     * 并发策略：0=FORBID禁止 1=ALLOW允许 2=DISCARD丢弃
     */
    @Schema(description = "并发策略：0=FORBID 1=ALLOW 2=DISCARD")
    private Integer concurrentPolicy;
    @Schema(description = "最大重试次数")
    private Integer maxRetryCount;
    @TableField("retry_interval_sec")
    @Schema(description = "重试间隔秒数")
    private Integer retryIntervalSec;
    @TableField("timeout_seconds")
    @Schema(description = "超时时间（秒）")
    private Integer timeoutSeconds;
    /**
     * 错失触发策略：1=EXECUTE_IMMEDIATELY 2=SKIP 3=ONCE
     */
    @Schema(description = "错失触发策略：1=立即执行 2=跳过 3=仅一次")
    private Integer misfirePolicy;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("last_execution_time")
    @Schema(description = "上次执行时间")
    private LocalDateTime lastExecutionTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("next_execution_time")
    @Schema(description = "下次执行时间")
    private LocalDateTime nextExecutionTime;
    @TableField("last_execution_msg")
    @Schema(description = "上次执行消息")
    private String lastExecutionMsg;
    /**
     * 是否内置任务：0=否 1=是
     */
    @Schema(description = "是否内置任务：0=否 1=是")
    private Integer isBuiltin;
    @Version
    @TableField("version")
    @Schema(description = "乐观锁版本号")
    private Long version;
    @TableLogic
    @TableField("deleted")
    @Schema(description = "逻辑删除标记")
    private Integer deleted;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
    @TableField("create_user_id")
    @Schema(description = "创建用户ID")
    private Long createUserId;
    @TableField("create_username")
    @Schema(description = "创建用户名")
    private String createUsername;
    @TableField("update_user_id")
    @Schema(description = "更新用户ID")
    private Long updateUserId;
    @TableField("update_username")
    @Schema(description = "更新用户名")
    private String updateUsername;

    public ScheduledTask() {
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
     * 任务状态：0=CREATED 1=ENABLED 2=PAUSED 3=DISABLED 4=ERROR
     */
    public Integer getStatus() {
        return this.status;
    }

    /**
     * 执行状态：0=IDLE 1=PENDING 2=RUNNING
     */
    public Integer getExecutionStatus() {
        return this.executionStatus;
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

    public LocalDateTime getLastExecutionTime() {
        return this.lastExecutionTime;
    }

    public LocalDateTime getNextExecutionTime() {
        return this.nextExecutionTime;
    }

    public String getLastExecutionMsg() {
        return this.lastExecutionMsg;
    }

    /**
     * 是否内置任务：0=否 1=是
     */
    public Integer getIsBuiltin() {
        return this.isBuiltin;
    }

    public Long getVersion() {
        return this.version;
    }

    public Integer getDeleted() {
        return this.deleted;
    }

    public LocalDateTime getCreateTime() {
        return this.createTime;
    }

    public LocalDateTime getUpdateTime() {
        return this.updateTime;
    }

    public Long getCreateUserId() {
        return this.createUserId;
    }

    public String getCreateUsername() {
        return this.createUsername;
    }

    public Long getUpdateUserId() {
        return this.updateUserId;
    }

    public String getUpdateUsername() {
        return this.updateUsername;
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
     * 任务状态：0=CREATED 1=ENABLED 2=PAUSED 3=DISABLED 4=ERROR
     */
    public void setStatus(final Integer status) {
        this.status = status;
    }

    /**
     * 执行状态：0=IDLE 1=PENDING 2=RUNNING
     */
    public void setExecutionStatus(final Integer executionStatus) {
        this.executionStatus = executionStatus;
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

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public void setLastExecutionTime(final LocalDateTime lastExecutionTime) {
        this.lastExecutionTime = lastExecutionTime;
    }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public void setNextExecutionTime(final LocalDateTime nextExecutionTime) {
        this.nextExecutionTime = nextExecutionTime;
    }

    public void setLastExecutionMsg(final String lastExecutionMsg) {
        this.lastExecutionMsg = lastExecutionMsg;
    }

    /**
     * 是否内置任务：0=否 1=是
     */
    public void setIsBuiltin(final Integer isBuiltin) {
        this.isBuiltin = isBuiltin;
    }

    public void setVersion(final Long version) {
        this.version = version;
    }

    public void setDeleted(final Integer deleted) {
        this.deleted = deleted;
    }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public void setCreateTime(final LocalDateTime createTime) {
        this.createTime = createTime;
    }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public void setUpdateTime(final LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public void setCreateUserId(final Long createUserId) {
        this.createUserId = createUserId;
    }

    public void setCreateUsername(final String createUsername) {
        this.createUsername = createUsername;
    }

    public void setUpdateUserId(final Long updateUserId) {
        this.updateUserId = updateUserId;
    }

    public void setUpdateUsername(final String updateUsername) {
        this.updateUsername = updateUsername;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof ScheduledTask)) return false;
        final ScheduledTask other = (ScheduledTask) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$taskId = this.getTaskId();
        final java.lang.Object other$taskId = other.getTaskId();
        if (this$taskId == null ? other$taskId != null : !this$taskId.equals(other$taskId)) return false;
        final java.lang.Object this$intervalSeconds = this.getIntervalSeconds();
        final java.lang.Object other$intervalSeconds = other.getIntervalSeconds();
        if (this$intervalSeconds == null ? other$intervalSeconds != null : !this$intervalSeconds.equals(other$intervalSeconds)) return false;
        final java.lang.Object this$taskType = this.getTaskType();
        final java.lang.Object other$taskType = other.getTaskType();
        if (this$taskType == null ? other$taskType != null : !this$taskType.equals(other$taskType)) return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
        final java.lang.Object this$executionStatus = this.getExecutionStatus();
        final java.lang.Object other$executionStatus = other.getExecutionStatus();
        if (this$executionStatus == null ? other$executionStatus != null : !this$executionStatus.equals(other$executionStatus)) return false;
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
        final java.lang.Object this$isBuiltin = this.getIsBuiltin();
        final java.lang.Object other$isBuiltin = other.getIsBuiltin();
        if (this$isBuiltin == null ? other$isBuiltin != null : !this$isBuiltin.equals(other$isBuiltin)) return false;
        final java.lang.Object this$version = this.getVersion();
        final java.lang.Object other$version = other.getVersion();
        if (this$version == null ? other$version != null : !this$version.equals(other$version)) return false;
        final java.lang.Object this$deleted = this.getDeleted();
        final java.lang.Object other$deleted = other.getDeleted();
        if (this$deleted == null ? other$deleted != null : !this$deleted.equals(other$deleted)) return false;
        final java.lang.Object this$createUserId = this.getCreateUserId();
        final java.lang.Object other$createUserId = other.getCreateUserId();
        if (this$createUserId == null ? other$createUserId != null : !this$createUserId.equals(other$createUserId)) return false;
        final java.lang.Object this$updateUserId = this.getUpdateUserId();
        final java.lang.Object other$updateUserId = other.getUpdateUserId();
        if (this$updateUserId == null ? other$updateUserId != null : !this$updateUserId.equals(other$updateUserId)) return false;
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
        final java.lang.Object this$lastExecutionTime = this.getLastExecutionTime();
        final java.lang.Object other$lastExecutionTime = other.getLastExecutionTime();
        if (this$lastExecutionTime == null ? other$lastExecutionTime != null : !this$lastExecutionTime.equals(other$lastExecutionTime)) return false;
        final java.lang.Object this$nextExecutionTime = this.getNextExecutionTime();
        final java.lang.Object other$nextExecutionTime = other.getNextExecutionTime();
        if (this$nextExecutionTime == null ? other$nextExecutionTime != null : !this$nextExecutionTime.equals(other$nextExecutionTime)) return false;
        final java.lang.Object this$lastExecutionMsg = this.getLastExecutionMsg();
        final java.lang.Object other$lastExecutionMsg = other.getLastExecutionMsg();
        if (this$lastExecutionMsg == null ? other$lastExecutionMsg != null : !this$lastExecutionMsg.equals(other$lastExecutionMsg)) return false;
        final java.lang.Object this$createTime = this.getCreateTime();
        final java.lang.Object other$createTime = other.getCreateTime();
        if (this$createTime == null ? other$createTime != null : !this$createTime.equals(other$createTime)) return false;
        final java.lang.Object this$updateTime = this.getUpdateTime();
        final java.lang.Object other$updateTime = other.getUpdateTime();
        if (this$updateTime == null ? other$updateTime != null : !this$updateTime.equals(other$updateTime)) return false;
        final java.lang.Object this$createUsername = this.getCreateUsername();
        final java.lang.Object other$createUsername = other.getCreateUsername();
        if (this$createUsername == null ? other$createUsername != null : !this$createUsername.equals(other$createUsername)) return false;
        final java.lang.Object this$updateUsername = this.getUpdateUsername();
        final java.lang.Object other$updateUsername = other.getUpdateUsername();
        if (this$updateUsername == null ? other$updateUsername != null : !this$updateUsername.equals(other$updateUsername)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof ScheduledTask;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $taskId = this.getTaskId();
        result = result * PRIME + ($taskId == null ? 43 : $taskId.hashCode());
        final java.lang.Object $intervalSeconds = this.getIntervalSeconds();
        result = result * PRIME + ($intervalSeconds == null ? 43 : $intervalSeconds.hashCode());
        final java.lang.Object $taskType = this.getTaskType();
        result = result * PRIME + ($taskType == null ? 43 : $taskType.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $executionStatus = this.getExecutionStatus();
        result = result * PRIME + ($executionStatus == null ? 43 : $executionStatus.hashCode());
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
        final java.lang.Object $isBuiltin = this.getIsBuiltin();
        result = result * PRIME + ($isBuiltin == null ? 43 : $isBuiltin.hashCode());
        final java.lang.Object $version = this.getVersion();
        result = result * PRIME + ($version == null ? 43 : $version.hashCode());
        final java.lang.Object $deleted = this.getDeleted();
        result = result * PRIME + ($deleted == null ? 43 : $deleted.hashCode());
        final java.lang.Object $createUserId = this.getCreateUserId();
        result = result * PRIME + ($createUserId == null ? 43 : $createUserId.hashCode());
        final java.lang.Object $updateUserId = this.getUpdateUserId();
        result = result * PRIME + ($updateUserId == null ? 43 : $updateUserId.hashCode());
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
        final java.lang.Object $lastExecutionTime = this.getLastExecutionTime();
        result = result * PRIME + ($lastExecutionTime == null ? 43 : $lastExecutionTime.hashCode());
        final java.lang.Object $nextExecutionTime = this.getNextExecutionTime();
        result = result * PRIME + ($nextExecutionTime == null ? 43 : $nextExecutionTime.hashCode());
        final java.lang.Object $lastExecutionMsg = this.getLastExecutionMsg();
        result = result * PRIME + ($lastExecutionMsg == null ? 43 : $lastExecutionMsg.hashCode());
        final java.lang.Object $createTime = this.getCreateTime();
        result = result * PRIME + ($createTime == null ? 43 : $createTime.hashCode());
        final java.lang.Object $updateTime = this.getUpdateTime();
        result = result * PRIME + ($updateTime == null ? 43 : $updateTime.hashCode());
        final java.lang.Object $createUsername = this.getCreateUsername();
        result = result * PRIME + ($createUsername == null ? 43 : $createUsername.hashCode());
        final java.lang.Object $updateUsername = this.getUpdateUsername();
        result = result * PRIME + ($updateUsername == null ? 43 : $updateUsername.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "ScheduledTask(taskId=" + this.getTaskId() + ", taskName=" + this.getTaskName() + ", taskCode=" + this.getTaskCode() + ", description=" + this.getDescription() + ", taskGroup=" + this.getTaskGroup() + ", jobHandler=" + this.getJobHandler() + ", cronExpression=" + this.getCronExpression() + ", intervalSeconds=" + this.getIntervalSeconds() + ", taskType=" + this.getTaskType() + ", status=" + this.getStatus() + ", executionStatus=" + this.getExecutionStatus() + ", concurrentPolicy=" + this.getConcurrentPolicy() + ", maxRetryCount=" + this.getMaxRetryCount() + ", retryIntervalSec=" + this.getRetryIntervalSec() + ", timeoutSeconds=" + this.getTimeoutSeconds() + ", misfirePolicy=" + this.getMisfirePolicy() + ", lastExecutionTime=" + this.getLastExecutionTime() + ", nextExecutionTime=" + this.getNextExecutionTime() + ", lastExecutionMsg=" + this.getLastExecutionMsg() + ", isBuiltin=" + this.getIsBuiltin() + ", version=" + this.getVersion() + ", deleted=" + this.getDeleted() + ", createTime=" + this.getCreateTime() + ", updateTime=" + this.getUpdateTime() + ", createUserId=" + this.getCreateUserId() + ", createUsername=" + this.getCreateUsername() + ", updateUserId=" + this.getUpdateUserId() + ", updateUsername=" + this.getUpdateUsername() + ")";
    }
}
