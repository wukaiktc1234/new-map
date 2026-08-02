package com.foodtraceability.entity.schedule;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 排班考勤同步记录实体类
 * 记录排班数据同步到考勤系统的执行情况
 *
 * 同步状态（sync_status）：
 * - not_synced: 未同步
 * - synced: 已同步
 * - failed: 同步失败
 */
@TableName("schedule_attendance_syncs")
public class ScheduleAttendanceSync implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 同步记录ID */
    @TableId(value = "sync_id", type = IdType.ASSIGN_ID)
    private String syncId;

    /** 排班方案ID */
    @TableField("plan_id")
    private String planId;

    /** 同步状态 */
    @TableField("sync_status")
    private String syncStatus;

    /** 同步时间 */
    @TableField("sync_time")
    private LocalDateTime syncTime;

    /** 总条目数 */
    @TableField("total_entries")
    private Integer totalEntries;

    /** 已同步条目数 */
    @TableField("synced_entries")
    private Integer syncedEntries;

    /** 失败条目数 */
    @TableField("failed_entries")
    private Integer failedEntries;

    /** 错误信息 */
    @TableField("error_message")
    private String errorMessage;

    /** 操作人ID */
    @TableField("operator_id")
    private String operatorId;

    /** 操作人姓名 */
    @TableField("operator_name")
    private String operatorName;

    /** 创建时间 */
    @TableField("create_time")
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField("update_time")
    private LocalDateTime updateTime;

    /** 逻辑删除标记 */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    /** 同步状态：未同步 */
    public static final String STATUS_NOT_SYNCED = "not_synced";
    /** 同步状态：已同步 */
    public static final String STATUS_SYNCED = "synced";
    /** 同步状态：失败 */
    public static final String STATUS_FAILED = "failed";

    // ==================== Getter & Setter ====================

    public String getSyncId() { return syncId; }
    public void setSyncId(String syncId) { this.syncId = syncId; }

    public String getPlanId() { return planId; }
    public void setPlanId(String planId) { this.planId = planId; }

    public String getSyncStatus() { return syncStatus; }
    public void setSyncStatus(String syncStatus) { this.syncStatus = syncStatus; }

    public LocalDateTime getSyncTime() { return syncTime; }
    public void setSyncTime(LocalDateTime syncTime) { this.syncTime = syncTime; }

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

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }

    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
