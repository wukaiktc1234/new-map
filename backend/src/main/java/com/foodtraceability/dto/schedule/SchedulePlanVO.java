package com.foodtraceability.dto.schedule;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 排班方案视图对象
 * 包含完整方案信息及关联数据（门店名、模板名、状态显示名等）
 */
@Schema(description = "排班方案VO")
public class SchedulePlanVO {

    // ==================== 基础字段 ====================

    @Schema(description = "主键ID")
    private String planId;

    @Schema(description = "方案名称")
    private String planName;

    @Schema(description = "门店ID")
    private Long storeId;

    /** 门店名称（冗余，避免前端二次查询） */
    @Schema(description = "门店名称")
    private String storeName;

    @Schema(description = "周期起始日期", pattern = "yyyy-MM-dd")
    private String startDate;

    @Schema(description = "周期结束日期", pattern = "yyyy-MM-dd")
    private String endDate;

    /**
     * 状态(draft-草稿/published-已发布/executing-执行中/archived-已归档)
     */
    @Schema(description = "状态(draft/published/executing/archived)",
            allowableValues = {"draft", "published", "executing", "archived"})
    private String status;

    /** 状态中文名称（冗余，方便前端直接展示） */
    @Schema(description = "状态中文名称")
    private String statusName;

    @Schema(description = "版本号(乐观锁)")
    private Integer version;

    @Schema(description = "使用的模板ID")
    private Long templateId;

    /** 模板名称（冗余，方便前端直接展示） */
    @Schema(description = "模板名称")
    private String templateName;

    @Schema(description = "涉及员工总数")
    private Integer employeeCount;

    /** 总工时显示文本（单位转换为小时） */
    @Schema(description = "总工时显示(小时)")
    private String totalWorkHoursDisplay;

    // ==================== 发布信息 ====================

    @Schema(description = "发布人用户ID")
    private Long publisherId;

    @Schema(description = "发布人姓名")
    private String publisherName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "发布时间")
    private LocalDateTime publishTime;

    // ==================== 撤回信息 ====================

    @Schema(description = "撤回人用户ID")
    private Long withdrawerId;

    @Schema(description = "撤回人姓名")
    private String withdrawerName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "撤回时间")
    private LocalDateTime withdrawTime;

    @Schema(description = "撤回原因")
    private String withdrawReason;

    // ==================== 同步信息 ====================

    @Schema(description = "考勤同步状态(not_synced/synced/failed)")
    private String syncStatus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "最后同步时间")
    private LocalDateTime syncToAttendanceTime;

    @Schema(description = "同步结果说明")
    private String syncMessage;

    // ==================== 时间戳 ====================

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    // ==================== 关联条目列表（详情接口返回） ====================

    @Schema(description = "排班条目列表（仅详情接口返回）")
    private List<ScheduleEntryVO> entries;

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

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatusName() {
        return statusName;
    }

    public void setStatusName(String statusName) {
        this.statusName = statusName;
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

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public Integer getEmployeeCount() {
        return employeeCount;
    }

    public void setEmployeeCount(Integer employeeCount) {
        this.employeeCount = employeeCount;
    }

    public String getTotalWorkHoursDisplay() {
        return totalWorkHoursDisplay;
    }

    public void setTotalWorkHoursDisplay(String totalWorkHoursDisplay) {
        this.totalWorkHoursDisplay = totalWorkHoursDisplay;
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

    public List<ScheduleEntryVO> getEntries() {
        return entries;
    }

    public void setEntries(List<ScheduleEntryVO> entries) {
        this.entries = entries;
    }
}
