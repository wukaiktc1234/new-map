package com.foodtraceability.entity.schedule;

import com.baomidou.mybatisplus.annotation.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 排班方案实体类
 * 存储排班方案的元数据和状态信息
 */
@TableName("schedule_plans")
public class SchedulePlan implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(value = "plan_id", type = IdType.ASSIGN_ID)
    private String planId;

    /** 方案名称 */
    @TableField("plan_name")
    private String planName;

    /** 门店ID(数据隔离) */
    @TableField("store_id")
    private Long storeId;

    /** 周期起始日期 */
    @TableField("start_date")
    private LocalDate startDate;

    /** 周期结束日期 */
    @TableField("end_date")
    private LocalDate endDate;

    /**
     * 状态(语义化字符串, ADR-001决策)
     * draft-草稿 published-已发布 executing-执行中 archived-已归档
     */
    @TableField("status")
    private String status;

    /** 版本号(乐观锁, ADR-004决策) */
    @Version
    @TableField("version")
    private Integer version;

    /** 使用的模板ID(可选) */
    @TableField("template_id")
    private Long templateId;

    /** 涉及员工总数(冗余) */
    @TableField("employee_count")
    private Integer employeeCount;

    /** 总工时(单位:分钟) */
    @TableField("total_work_hours")
    private Integer totalWorkHours;

    /** 发布人用户ID */
    @TableField("publisher_id")
    private Long publisherId;

    /** 发布人姓名(冗余) */
    @TableField("publisher_name")
    private String publisherName;

    /** 发布时间 */
    @TableField("publish_time")
    private LocalDateTime publishTime;

    /** 撤回人用户ID */
    @TableField("withdrawer_id")
    private Long withdrawerId;

    /** 撤回人姓名 */
    @TableField("withdrawer_name")
    private String withdrawerName;

    /** 撤回时间 */
    @TableField("withdraw_time")
    private LocalDateTime withdrawTime;

    /** 撤回原因 */
    @TableField("withdraw_reason")
    private String withdrawReason;

    /**
     * 考勤同步状态
     * not_synced-未同步 synced-已同步 failed-失败
     */
    @TableField("sync_status")
    private String syncStatus;

    /** 最后同步时间 */
    @TableField("sync_to_attendance_time")
    private LocalDateTime syncToAttendanceTime;

    /** 同步结果说明 */
    @TableField("sync_message")
    private String syncMessage;

    /** 逻辑删除标记 */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    // ==================== 状态常量 ====================

    /** 草稿状态 */
    public static final String STATUS_DRAFT = "draft";
    /** 已发布状态 */
    public static final String STATUS_PUBLISHED = "published";
    /** 执行中状态 */
    public static final String STATUS_EXECUTING = "executing";
    /** 已归档状态 */
    public static final String STATUS_ARCHIVED = "archived";

    /** 未同步 */
    public static final String SYNC_STATUS_NOT_SYNCED = "not_synced";
    /** 已同步 */
    public static final String SYNC_STATUS_SYNCED = "synced";
    /** 同步失败 */
    public static final String SYNC_STATUS_FAILED = "failed";

    // ==================== Getter & Setter ====================

    public String getPlanId() {
        return planId;
    }

    public void setPlanId(String planId) {
        this.planId = planId;
    }

    public String getPlanName() {
        return planName;
    }

    public void setPlanName(String planName) {
        this.planName = planName;
    }

    public Long getStoreId() {
        return storeId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
    }

    public Integer getEmployeeCount() {
        return employeeCount;
    }

    public void setEmployeeCount(Integer employeeCount) {
        this.employeeCount = employeeCount;
    }

    public Integer getTotalWorkHours() {
        return totalWorkHours;
    }

    public void setTotalWorkHours(Integer totalWorkHours) {
        this.totalWorkHours = totalWorkHours;
    }

    public Long getPublisherId() {
        return publisherId;
    }

    public void setPublisherId(Long publisherId) {
        this.publisherId = publisherId;
    }

    public String getPublisherName() {
        return publisherName;
    }

    public void setPublisherName(String publisherName) {
        this.publisherName = publisherName;
    }

    public LocalDateTime getPublishTime() {
        return publishTime;
    }

    public void setPublishTime(LocalDateTime publishTime) {
        this.publishTime = publishTime;
    }

    public Long getWithdrawerId() {
        return withdrawerId;
    }

    public void setWithdrawerId(Long withdrawerId) {
        this.withdrawerId = withdrawerId;
    }

    public String getWithdrawerName() {
        return withdrawerName;
    }

    public void setWithdrawerName(String withdrawerName) {
        this.withdrawerName = withdrawerName;
    }

    public LocalDateTime getWithdrawTime() {
        return withdrawTime;
    }

    public void setWithdrawTime(LocalDateTime withdrawTime) {
        this.withdrawTime = withdrawTime;
    }

    public String getWithdrawReason() {
        return withdrawReason;
    }

    public void setWithdrawReason(String withdrawReason) {
        this.withdrawReason = withdrawReason;
    }

    public String getSyncStatus() {
        return syncStatus;
    }

    public void setSyncStatus(String syncStatus) {
        this.syncStatus = syncStatus;
    }

    public LocalDateTime getSyncToAttendanceTime() {
        return syncToAttendanceTime;
    }

    public void setSyncToAttendanceTime(LocalDateTime syncToAttendanceTime) {
        this.syncToAttendanceTime = syncToAttendanceTime;
    }

    public String getSyncMessage() {
        return syncMessage;
    }

    public void setSyncMessage(String syncMessage) {
        this.syncMessage = syncMessage;
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
}
