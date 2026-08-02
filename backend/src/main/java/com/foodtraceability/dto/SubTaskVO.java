package com.foodtraceability.dto;

import java.time.LocalDateTime;

/**
 * 子任务视图对象
 */
public class SubTaskVO {

    /** 子任务ID */
    private String subTaskId;

    /** 所属任务ID */
    private String taskId;

    /** 子任务标题 */
    private String title;

    /** 子任务描述 */
    private String description;

    /** 排序序号 */
    private Integer sortOrder;

    /** 状态：pending/in_progress/completed */
    private String status;

    /** 完成时间 */
    private LocalDateTime completedAt;

    public String getSubTaskId() { return subTaskId; }
    public void setSubTaskId(String subTaskId) { this.subTaskId = subTaskId; }

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}
