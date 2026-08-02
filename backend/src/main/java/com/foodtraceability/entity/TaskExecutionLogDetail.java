package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 任务执行详情实体（步骤级别）
 * 对应数据库表 task_execution_log_detail
 */
@TableName("task_execution_log_detail")
@Schema(description = "任务执行详情实体")
public class TaskExecutionLogDetail {
    @TableId(type = IdType.AUTO)
    @Schema(description = "详情ID")
    private Long detailId;
    @TableField("log_id")
    @Schema(description = "关联日志ID")
    private Long logId;
    @TableField("step_name")
    @Schema(description = "步骤名称")
    private String stepName;
    /**
     * 步骤状态：0=PENDING 1=RUNNING 2=SUCCESS 3=FAILED 4=SKIPPED
     */
    @Schema(description = "步骤状态：0=PENDING 1=RUNNING 2=SUCCESS 3=FAILED 4=SKIPPED")
    private Integer stepStatus;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("start_time")
    @Schema(description = "开始时间")
    private LocalDateTime startTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("end_time")
    @Schema(description = "结束时间")
    private LocalDateTime endTime;
    @TableField("duration_ms")
    @Schema(description = "耗时（毫秒）")
    private Long durationMs;
    @Schema(description = "步骤消息/备注")
    private String message;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    public TaskExecutionLogDetail() {
    }

    public Long getDetailId() {
        return this.detailId;
    }

    public Long getLogId() {
        return this.logId;
    }

    public String getStepName() {
        return this.stepName;
    }

    /**
     * 步骤状态：0=PENDING 1=RUNNING 2=SUCCESS 3=FAILED 4=SKIPPED
     */
    public Integer getStepStatus() {
        return this.stepStatus;
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

    public String getMessage() {
        return this.message;
    }

    public LocalDateTime getCreateTime() {
        return this.createTime;
    }

    public void setDetailId(final Long detailId) {
        this.detailId = detailId;
    }

    public void setLogId(final Long logId) {
        this.logId = logId;
    }

    public void setStepName(final String stepName) {
        this.stepName = stepName;
    }

    /**
     * 步骤状态：0=PENDING 1=RUNNING 2=SUCCESS 3=FAILED 4=SKIPPED
     */
    public void setStepStatus(final Integer stepStatus) {
        this.stepStatus = stepStatus;
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

    public void setMessage(final String message) {
        this.message = message;
    }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public void setCreateTime(final LocalDateTime createTime) {
        this.createTime = createTime;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof TaskExecutionLogDetail)) return false;
        final TaskExecutionLogDetail other = (TaskExecutionLogDetail) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$detailId = this.getDetailId();
        final java.lang.Object other$detailId = other.getDetailId();
        if (this$detailId == null ? other$detailId != null : !this$detailId.equals(other$detailId)) return false;
        final java.lang.Object this$logId = this.getLogId();
        final java.lang.Object other$logId = other.getLogId();
        if (this$logId == null ? other$logId != null : !this$logId.equals(other$logId)) return false;
        final java.lang.Object this$stepStatus = this.getStepStatus();
        final java.lang.Object other$stepStatus = other.getStepStatus();
        if (this$stepStatus == null ? other$stepStatus != null : !this$stepStatus.equals(other$stepStatus)) return false;
        final java.lang.Object this$durationMs = this.getDurationMs();
        final java.lang.Object other$durationMs = other.getDurationMs();
        if (this$durationMs == null ? other$durationMs != null : !this$durationMs.equals(other$durationMs)) return false;
        final java.lang.Object this$stepName = this.getStepName();
        final java.lang.Object other$stepName = other.getStepName();
        if (this$stepName == null ? other$stepName != null : !this$stepName.equals(other$stepName)) return false;
        final java.lang.Object this$startTime = this.getStartTime();
        final java.lang.Object other$startTime = other.getStartTime();
        if (this$startTime == null ? other$startTime != null : !this$startTime.equals(other$startTime)) return false;
        final java.lang.Object this$endTime = this.getEndTime();
        final java.lang.Object other$endTime = other.getEndTime();
        if (this$endTime == null ? other$endTime != null : !this$endTime.equals(other$endTime)) return false;
        final java.lang.Object this$message = this.getMessage();
        final java.lang.Object other$message = other.getMessage();
        if (this$message == null ? other$message != null : !this$message.equals(other$message)) return false;
        final java.lang.Object this$createTime = this.getCreateTime();
        final java.lang.Object other$createTime = other.getCreateTime();
        if (this$createTime == null ? other$createTime != null : !this$createTime.equals(other$createTime)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof TaskExecutionLogDetail;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $detailId = this.getDetailId();
        result = result * PRIME + ($detailId == null ? 43 : $detailId.hashCode());
        final java.lang.Object $logId = this.getLogId();
        result = result * PRIME + ($logId == null ? 43 : $logId.hashCode());
        final java.lang.Object $stepStatus = this.getStepStatus();
        result = result * PRIME + ($stepStatus == null ? 43 : $stepStatus.hashCode());
        final java.lang.Object $durationMs = this.getDurationMs();
        result = result * PRIME + ($durationMs == null ? 43 : $durationMs.hashCode());
        final java.lang.Object $stepName = this.getStepName();
        result = result * PRIME + ($stepName == null ? 43 : $stepName.hashCode());
        final java.lang.Object $startTime = this.getStartTime();
        result = result * PRIME + ($startTime == null ? 43 : $startTime.hashCode());
        final java.lang.Object $endTime = this.getEndTime();
        result = result * PRIME + ($endTime == null ? 43 : $endTime.hashCode());
        final java.lang.Object $message = this.getMessage();
        result = result * PRIME + ($message == null ? 43 : $message.hashCode());
        final java.lang.Object $createTime = this.getCreateTime();
        result = result * PRIME + ($createTime == null ? 43 : $createTime.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "TaskExecutionLogDetail(detailId=" + this.getDetailId() + ", logId=" + this.getLogId() + ", stepName=" + this.getStepName() + ", stepStatus=" + this.getStepStatus() + ", startTime=" + this.getStartTime() + ", endTime=" + this.getEndTime() + ", durationMs=" + this.getDurationMs() + ", message=" + this.getMessage() + ", createTime=" + this.getCreateTime() + ")";
    }
}
