package com.foodtraceability.dto.schedule;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

/**
 * 考勤同步记录视图对象
 */
@Schema(description = "排班考勤同步记录")
public class AttendanceSyncVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 同步记录ID */
    @Schema(description = "同步记录ID")
    private String syncId;

    /** 排班方案ID */
    @Schema(description = "排班方案ID")
    private String planId;

    /** 同步状态 */
    @Schema(description = "同步状态: not_synced/synced/failed")
    private String syncStatus;

    /** 同步时间 */
    @Schema(description = "同步时间")
    private String syncTime;

    /** 总条目数 */
    @Schema(description = "总排班条目数")
    private Integer totalEntries;

    /** 已同步条目数 */
    @Schema(description = "已同步条目数")
    private Integer syncedEntries;

    /** 失败条目数 */
    @Schema(description = "失败条目数")
    private Integer failedEntries;

    /** 错误信息 */
    @Schema(description = "错误信息")
    private String errorMessage;

    /** 操作人ID */
    @Schema(description = "操作人ID")
    private String operatorId;

    /** 操作人姓名 */
    @Schema(description = "操作人姓名")
    private String operatorName;

    /** 创建时间 */
    @Schema(description = "创建时间")
    private String createTime;

    public String getSyncId() { return syncId; }
    public void setSyncId(String syncId) { this.syncId = syncId; }

    public String getPlanId() { return planId; }
    public void setPlanId(String planId) { this.planId = planId; }

    public String getSyncStatus() { return syncStatus; }
    public void setSyncStatus(String syncStatus) { this.syncStatus = syncStatus; }

    public String getSyncTime() { return syncTime; }
    public void setSyncTime(String syncTime) { this.syncTime = syncTime; }

    public Integer getTotalEntries() { return totalEntries; }
    public void setTotalEntries(Integer totalEntries) { this.totalEntries = totalEntries; }

    public Integer getSyncedEntries() { return syncedEntries; }
    public void setSyncedEntries(Integer syncedEntries) { this.syncedEntries = syncedEntries; }

    public Integer getFailedEntries() { return failedEntries; }
    public void setFailedEntries(Integer failedEntries) { this.failedEntries = failedEntries; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public String getOperatorId() { return operatorId; }
    public void setOperatorId(String operatorId) { this.operatorId = operatorId; }

    public String getOperatorName() { return operatorName; }
    public void setOperatorName(String operatorName) { this.operatorName = operatorName; }

    public String getCreateTime() { return createTime; }
    public void setCreateTime(String createTime) { this.createTime = createTime; }
}
