package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 定时任务查询条件 DTO
 */
@Schema(description = "定时任务查询条件")
public class ScheduledTaskQueryDTO {
    @Schema(description = "任务名称（模糊查询）")
    private String taskName;
    @Schema(description = "任务编码（模糊查询）")
    private String taskCode;
    @Schema(description = "任务分组")
    private String taskGroup;
    /**
     * 任务状态：0=CREATED 1=ENABLED 2=PAUSED 3=DISABLED 4=ERROR
     */
    @Schema(description = "任务状态：0=CREATED 1=ENABLED 2=PAUSED 3=DISABLED 4=ERROR")
    private Integer status;
    /**
     * 任务类型：1=CRON 2=FIXED_RATE 3=ONE_TIME
     */
    @Schema(description = "任务类型：1=CRON 2=FIXED_RATE 3=ONE_TIME")
    private Integer taskType;
    /**
     * 是否内置任务：0=否 1=是
     */
    @Schema(description = "是否内置任务：0=否 1=是")
    private Integer isBuiltin;

    public ScheduledTaskQueryDTO() {
    }

    public String getTaskName() {
        return this.taskName;
    }

    public String getTaskCode() {
        return this.taskCode;
    }

    public String getTaskGroup() {
        return this.taskGroup;
    }

    /**
     * 任务状态：0=CREATED 1=ENABLED 2=PAUSED 3=DISABLED 4=ERROR
     */
    public Integer getStatus() {
        return this.status;
    }

    /**
     * 任务类型：1=CRON 2=FIXED_RATE 3=ONE_TIME
     */
    public Integer getTaskType() {
        return this.taskType;
    }

    /**
     * 是否内置任务：0=否 1=是
     */
    public Integer getIsBuiltin() {
        return this.isBuiltin;
    }

    public void setTaskName(final String taskName) {
        this.taskName = taskName;
    }

    public void setTaskCode(final String taskCode) {
        this.taskCode = taskCode;
    }

    public void setTaskGroup(final String taskGroup) {
        this.taskGroup = taskGroup;
    }

    /**
     * 任务状态：0=CREATED 1=ENABLED 2=PAUSED 3=DISABLED 4=ERROR
     */
    public void setStatus(final Integer status) {
        this.status = status;
    }

    /**
     * 任务类型：1=CRON 2=FIXED_RATE 3=ONE_TIME
     */
    public void setTaskType(final Integer taskType) {
        this.taskType = taskType;
    }

    /**
     * 是否内置任务：0=否 1=是
     */
    public void setIsBuiltin(final Integer isBuiltin) {
        this.isBuiltin = isBuiltin;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof ScheduledTaskQueryDTO)) return false;
        final ScheduledTaskQueryDTO other = (ScheduledTaskQueryDTO) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
        final java.lang.Object this$taskType = this.getTaskType();
        final java.lang.Object other$taskType = other.getTaskType();
        if (this$taskType == null ? other$taskType != null : !this$taskType.equals(other$taskType)) return false;
        final java.lang.Object this$isBuiltin = this.getIsBuiltin();
        final java.lang.Object other$isBuiltin = other.getIsBuiltin();
        if (this$isBuiltin == null ? other$isBuiltin != null : !this$isBuiltin.equals(other$isBuiltin)) return false;
        final java.lang.Object this$taskName = this.getTaskName();
        final java.lang.Object other$taskName = other.getTaskName();
        if (this$taskName == null ? other$taskName != null : !this$taskName.equals(other$taskName)) return false;
        final java.lang.Object this$taskCode = this.getTaskCode();
        final java.lang.Object other$taskCode = other.getTaskCode();
        if (this$taskCode == null ? other$taskCode != null : !this$taskCode.equals(other$taskCode)) return false;
        final java.lang.Object this$taskGroup = this.getTaskGroup();
        final java.lang.Object other$taskGroup = other.getTaskGroup();
        if (this$taskGroup == null ? other$taskGroup != null : !this$taskGroup.equals(other$taskGroup)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof ScheduledTaskQueryDTO;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $taskType = this.getTaskType();
        result = result * PRIME + ($taskType == null ? 43 : $taskType.hashCode());
        final java.lang.Object $isBuiltin = this.getIsBuiltin();
        result = result * PRIME + ($isBuiltin == null ? 43 : $isBuiltin.hashCode());
        final java.lang.Object $taskName = this.getTaskName();
        result = result * PRIME + ($taskName == null ? 43 : $taskName.hashCode());
        final java.lang.Object $taskCode = this.getTaskCode();
        result = result * PRIME + ($taskCode == null ? 43 : $taskCode.hashCode());
        final java.lang.Object $taskGroup = this.getTaskGroup();
        result = result * PRIME + ($taskGroup == null ? 43 : $taskGroup.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "ScheduledTaskQueryDTO(taskName=" + this.getTaskName() + ", taskCode=" + this.getTaskCode() + ", taskGroup=" + this.getTaskGroup() + ", status=" + this.getStatus() + ", taskType=" + this.getTaskType() + ", isBuiltin=" + this.getIsBuiltin() + ")";
    }
}
