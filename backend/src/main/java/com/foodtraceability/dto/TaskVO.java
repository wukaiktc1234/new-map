package com.foodtraceability.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 任务列表视图对象
 */
public class TaskVO {

    /** 任务ID */
    private String taskId;

    /** 任务编号 */
    private String refNo;

    /** 任务类别 */
    private String category;

    /** 任务标题 */
    private String title;

    /** 优先级 */
    private String priority;

    /** 状态 */
    private String status;

    /** 当前工作流阶段 */
    private String currentStage;

    /** 进度（0-100） */
    private Integer progress;

    /** 截止时间 */
    private LocalDateTime deadline;

    /** 接收人数量 */
    private Integer assigneeCount;

    /** 子任务总数 */
    private Integer subTasksCount;

    /** 已完成子任务数 */
    private Integer completedSubTasks;

    /** 创建时间 */
    private LocalDateTime createTime;

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }

    public String getRefNo() { return refNo; }
    public void setRefNo(String refNo) { this.refNo = refNo; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCurrentStage() { return currentStage; }
    public void setCurrentStage(String currentStage) { this.currentStage = currentStage; }

    public Integer getProgress() { return progress; }
    public void setProgress(Integer progress) { this.progress = progress; }

    public LocalDateTime getDeadline() { return deadline; }
    public void setDeadline(LocalDateTime deadline) { this.deadline = deadline; }

    public Integer getAssigneeCount() { return assigneeCount; }
    public void setAssigneeCount(Integer assigneeCount) { this.assigneeCount = assigneeCount; }

    public Integer getSubTasksCount() { return subTasksCount; }
    public void setSubTasksCount(Integer subTasksCount) { this.subTasksCount = subTasksCount; }

    public Integer getCompletedSubTasks() { return completedSubTasks; }
    public void setCompletedSubTasks(Integer completedSubTasks) { this.completedSubTasks = completedSubTasks; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
