package com.foodtraceability.dto.store.operation.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 待办任务视图对象VO
 * 用于返回给前端的待办任务信息
 * 包含所有业务字段，排除version/deleted等内部字段
 */
@Schema(description = "待办任务视图对象")
public class PendingTaskVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 任务ID */
    @Schema(description = "任务ID", example = "1234567890123456789")
    private String taskId;

    /** 任务类型 */
    @Schema(description = "任务类型", example = "approval")
    private String taskType;

    /** 任务类型名称 */
    @Schema(description = "任务类型名称", example = "审批任务")
    private String taskTypeName;

    /** 任务标题 */
    @Schema(description = "任务标题", example = "采购订单审批")
    private String title;

    /** 任务详细描述 */
    @Schema(description = "任务描述", example = "请审批采购订单PO20260511001")
    private String description;

    /** 任务优先级（1=低 2=中 3=高 4=紧急） */
    @Schema(description = "优先级：1=低 2=中 3=高 4=紧急", example = "3")
    private Integer priority;

    /** 优先级名称 */
    @Schema(description = "优先级名称", example = "高")
    private String priorityName;

    /** 被指派人ID */
    @Schema(description = "被指派人ID", example = "user001")
    private String assigneeId;

    /** 被指派人姓名 */
    @Schema(description = "被指派人姓名", example = "张三")
    private String assigneeName;

    /** 被指派人角色 */
    @Schema(description = "被指派人角色", example = "店长")
    private String assigneeRole;

    /** 关联来源类型 */
    @Schema(description = "来源类型", example = "purchase")
    private String sourceType;

    /** 关联来源类型名称 */
    @Schema(description = "来源类型名称", example = "采购订单")
    private String sourceTypeName;

    /** 关联来源业务ID */
    @Schema(description = "来源业务ID", example = "PO20260511001")
    private String sourceId;

    /** 任务跳转链接 */
    @Schema(description = "跳转链接", example = "/purchase/orders/PO20260511001")
    private String redirectUrl;

    /** 任务状态（pending/in_progress/completed/cancelled/expired） */
    @Schema(description = "任务状态", example = "pending")
    private String status;

    /** 任务状态名称 */
    @Schema(description = "任务状态名称", example = "待处理")
    private String statusName;

    /** 截止日期 */
    @Schema(description = "截止日期", example = "2026-05-20")
    private LocalDate dueDate;

    /** 是否过期（根据dueDate计算） */
    @Schema(description = "是否过期", example = "false")
    private Boolean overdue;

    /** 完成时间 */
    @Schema(description = "完成时间", example = "2026-05-11T14:30:00")
    private LocalDateTime completedAt;

    /** 创建人ID */
    @Schema(description = "创建人ID", example = "admin")
    private String createdBy;

    /** 创建人姓名 */
    @Schema(description = "创建人姓名", example = "系统管理员")
    private String createdByName;

    /** 创建时间 */
    @Schema(description = "创建时间", example = "2026-05-11T10:00:00")
    private LocalDateTime createTime;

    // ==================== Getter & Setter ====================

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }

    public String getTaskType() { return taskType; }
    public void setTaskType(String taskType) { this.taskType = taskType; }

    public String getTaskTypeName() { return taskTypeName; }
    public void setTaskTypeName(String taskTypeName) { this.taskTypeName = taskTypeName; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }

    public String getPriorityName() { return priorityName; }
    public void setPriorityName(String priorityName) { this.priorityName = priorityName; }

    public String getAssigneeId() { return assigneeId; }
    public void setAssigneeId(String assigneeId) { this.assigneeId = assigneeId; }

    public String getAssigneeName() { return assigneeName; }
    public void setAssigneeName(String assigneeName) { this.assigneeName = assigneeName; }

    public String getAssigneeRole() { return assigneeRole; }
    public void setAssigneeRole(String assigneeRole) { this.assigneeRole = assigneeRole; }

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }

    public String getSourceTypeName() { return sourceTypeName; }
    public void setSourceTypeName(String sourceTypeName) { this.sourceTypeName = sourceTypeName; }

    public String getSourceId() { return sourceId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }

    public String getRedirectUrl() { return redirectUrl; }
    public void setRedirectUrl(String redirectUrl) { this.redirectUrl = redirectUrl; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getStatusName() { return statusName; }
    public void setStatusName(String statusName) { this.statusName = statusName; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public Boolean getOverdue() { return overdue; }
    public void setOverdue(Boolean overdue) { this.overdue = overdue; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getCreatedByName() { return createdByName; }
    public void setCreatedByName(String createdByName) { this.createdByName = createdByName; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
