package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

/**
 * 任务实体类
 *
 * 对应数据库表 tasks
 * 支持五种任务类别：日常指派/培训考核/出差任务/盘点任务/考核任务
 */
@TableName("tasks")
public class Task {

    /** 任务ID */
    @TableId(type = IdType.AUTO)
    private Long taskId;

    /** 任务编号（对外展示） */
    private String refNo;

    /** 任务类别：daily/training/business_trip/inventory/assessment */
    private String category;

    /** 任务标题 */
    private String title;

    /** 任务详细描述 */
    private String description;

    /** 发布人ID */
    private String publisherId;

    /** 优先级：high/medium/low */
    private String priority;

    /** 截止时间 */
    private LocalDateTime deadline;

    /** 任务状态：draft/pending/in_progress/reviewing/completed/overdue/rejected */
    private String status;

    /** 当前工作流阶段key */
    private String currentStage;

    /** 工作流阶段配置（JSON） */
    private String stagesConfig;

    /** 子任务总数 */
    private Integer subTasksCount;

    /** 已完成子任务数 */
    private Integer completedSubTasks;

    /** 接收人ID列表（JSON数组）— 兼容旧数据，新任务优先使用targetType+targetId */
    private String assigneeIds;

    /** 目标类型：individual/department/store/role */
    private String targetType;

    /** 目标ID（对应targetType的实体ID） */
    private String targetId;

    /** 分配方式：designated(指定)/open_pickup(公开接取) */
    private String assignmentMode;

    /** 是否允许跨部门/跨店参与（仅open_pickup模式生效） */
    private Boolean allowCrossDept;

    /** 协同/抄送人员ID列表（JSON数组） */
    private String ccIds;

    /** 完成进度（0-100） */
    private Integer progress;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除：0=未删除 / 1=已删除 */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    // ==================== Getter & Setter ====================

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }

    public String getRefNo() { return refNo; }
    public void setRefNo(String refNo) { this.refNo = refNo; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPublisherId() { return publisherId; }
    public void setPublisherId(String publisherId) { this.publisherId = publisherId; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public LocalDateTime getDeadline() { return deadline; }
    public void setDeadline(LocalDateTime deadline) { this.deadline = deadline; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCurrentStage() { return currentStage; }
    public void setCurrentStage(String currentStage) { this.currentStage = currentStage; }

    public String getStagesConfig() { return stagesConfig; }
    public void setStagesConfig(String stagesConfig) { this.stagesConfig = stagesConfig; }

    public Integer getSubTasksCount() { return subTasksCount; }
    public void setSubTasksCount(Integer subTasksCount) { this.subTasksCount = subTasksCount; }

    public Integer getCompletedSubTasks() { return completedSubTasks; }
    public void setCompletedSubTasks(Integer completedSubTasks) { this.completedSubTasks = completedSubTasks; }

    public String getAssigneeIds() { return assigneeIds; }
    public void setAssigneeIds(String assigneeIds) { this.assigneeIds = assigneeIds; }

    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }

    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }

    public String getAssignmentMode() { return assignmentMode; }
    public void setAssignmentMode(String assignmentMode) { this.assignmentMode = assignmentMode; }

    public Boolean getAllowCrossDept() { return allowCrossDept; }
    public void setAllowCrossDept(Boolean allowCrossDept) { this.allowCrossDept = allowCrossDept; }

    public String getCcIds() { return ccIds; }
    public void setCcIds(String ccIds) { this.ccIds = ccIds; }

    public Integer getProgress() { return progress; }
    public void setProgress(Integer progress) { this.progress = progress; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }

    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
