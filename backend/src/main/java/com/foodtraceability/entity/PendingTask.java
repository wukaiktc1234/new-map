package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 待办任务实体
 * 对应数据库表 pending_tasks
 * 用于门店运营模块的任务管理，支持多种任务类型和优先级
 */
@TableName("pending_tasks")
@Schema(description = "待办任务实体")
public class PendingTask implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 任务ID（雪花算法生成） */
    @TableId(type = IdType.ASSIGN_ID)
    @TableField("task_id")
    @Schema(description = "任务ID")
    private String taskId;

    /** 任务类型（如：approval/inspection/inventory/remind等） */
    @TableField("task_type")
    @Schema(description = "任务类型")
    private String taskType;

    /** 任务标题 */
    @TableField("title")
    @Schema(description = "任务标题")
    private String title;

    /** 任务详细描述 */
    @TableField("description")
    @Schema(description = "任务描述")
    private String description;

    /** 任务优先级（1=低 2=中 3=高 4=紧急） */
    @TableField("priority")
    @Schema(description = "优先级：1=低 2=中 3=高 4=紧急")
    private Integer priority;

    /** 被指派人ID */
    @TableField("assignee_id")
    @Schema(description = "被指派人ID")
    private String assigneeId;

    /** 被指派人角色 */
    @TableField("assignee_role")
    @Schema(description = "被指派人角色")
    private String assigneeRole;

    /** 关联来源类型（如：order/purchase/recruitment等） */
    @TableField("source_type")
    @Schema(description = "来源类型")
    private String sourceType;

    /** 关联来源业务ID */
    @TableField("source_id")
    @Schema(description = "来源业务ID")
    private String sourceId;

    /** 任务跳转链接（点击后跳转到对应业务页面） */
    @TableField("redirect_url")
    @Schema(description = "跳转链接")
    private String redirectUrl;

    /** 任务状态（pending/in_progress/completed/cancelled/expired） */
    @TableField("status")
    @Schema(description = "任务状态")
    private String status;

    /** 截止日期 */
    @TableField("due_date")
    @Schema(description = "截止日期")
    private LocalDate dueDate;

    /** 完成时间 */
    @TableField("completed_at")
    @Schema(description = "完成时间")
    private LocalDateTime completedAt;

    /** 创建人ID */
    @TableField("created_by")
    @Schema(description = "创建人ID")
    private String createdBy;

    /** 乐观锁版本号 */
    @Version
    @TableField("version")
    @Schema(description = "乐观锁版本号")
    private Long version;

    /** 逻辑删除标记（0=未删除 1=已删除） */
    @TableLogic
    @TableField("deleted")
    @Schema(description = "逻辑删除标记")
    private Integer deleted;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    // ==================== Getter & Setter 方法 ====================

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public String getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(String assigneeId) {
        this.assigneeId = assigneeId;
    }

    public String getAssigneeRole() {
        return assigneeRole;
    }

    public void setAssigneeRole(String assigneeRole) {
        this.assigneeRole = assigneeRole;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public String getRedirectUrl() {
        return redirectUrl;
    }

    public void setRedirectUrl(String redirectUrl) {
        this.redirectUrl = redirectUrl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
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

    public String getAssigneeName() {
        return null;
    }

    public void setAssigneeName(String assigneeName) {
    }

    public String getStoreId() {
        return null;
    }

    public void setStoreId(String storeId) {
    }
}
