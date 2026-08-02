package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

/**
 * 子任务实体类
 *
 * 对应数据库表 task_sub_tasks
 * 属于某个任务的细分步骤项，支持独立跟踪完成状态
 */
@TableName("task_sub_tasks")
public class SubTask {

    /** 子任务ID */
    @TableId(type = IdType.AUTO)
    private Long subTaskId;

    /** 所属任务ID */
    private Long taskId;

    /** 子任务标题 */
    private String title;

    /** 子任务描述 */
    private String description;

    /** 排序序号 */
    private Integer sortOrder;

    /** 子任务状态：pending/in_progress/completed */
    private String status;

    /** 完成时间 */
    private LocalDateTime completedAt;

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

    public Long getSubTaskId() { return subTaskId; }
    public void setSubTaskId(Long subTaskId) { this.subTaskId = subTaskId; }

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }

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

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }

    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
