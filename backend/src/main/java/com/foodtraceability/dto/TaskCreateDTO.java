package com.foodtraceability.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 创建任务请求DTO
 */
public class TaskCreateDTO {

    /** 任务类别：daily/training/business_trip/inventory/assessment */
    @NotBlank(message = "任务类别不能为空")
    private String category;

    /** 任务标题 */
    @NotBlank(message = "任务标题不能为空")
    @Size(max = 200, message = "标题不超过200个字符")
    private String title;

    /** 任务详细描述 */
    private String description;

    /** 优先级：high/medium/low */
    @NotNull(message = "优先级不能为空")
    private String priority;

    /** 截止时间 */
    private LocalDateTime deadline;

    /** 目标类型：individual/department/store/role */
    @NotBlank(message = "目标类型不能为空")
    private String targetType;

    /** 目标ID（对应targetType的实体ID，targetType=individual时为员工ID） */
    private String targetId;

    /** 分配方式：designated(指定)/open_pickup(公开接取)，默认designated */
    private String assignmentMode = "designated";

    /** 是否允许跨部门/跨店参与（仅open_pickup模式生效） */
    private Boolean allowCrossDept = false;

    /** 接收人ID列表（targetType=individual时的精确指定，可选） */
    private List<String> assigneeIds;

    /** 协同/抄送人员ID列表（出差任务等场景使用） */
    private List<String> ccIds;

    /** 基于模板创建时指定模板ID */
    private String templateId;

    /** 初始子任务列表 */
    private List<SubTaskCreateDTO> subTasks;

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public LocalDateTime getDeadline() { return deadline; }
    public void setDeadline(LocalDateTime deadline) { this.deadline = deadline; }

    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }

    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }

    public String getAssignmentMode() { return assignmentMode; }
    public void setAssignmentMode(String assignmentMode) { this.assignmentMode = assignmentMode; }

    public Boolean getAllowCrossDept() { return allowCrossDept; }
    public void setAllowCrossDept(Boolean allowCrossDept) { this.allowCrossDept = allowCrossDept; }

    public List<String> getAssigneeIds() { return assigneeIds; }
    public void setAssigneeIds(List<String> assigneeIds) { this.assigneeIds = assigneeIds; }

    public List<String> getCcIds() { return ccIds; }
    public void setCcIds(List<String> ccIds) { this.ccIds = ccIds; }

    public String getTemplateId() { return templateId; }
    public void setTemplateId(String templateId) { this.templateId = templateId; }

    public List<SubTaskCreateDTO> getSubTasks() { return subTasks; }
    public void setSubTasks(List<SubTaskCreateDTO> subTasks) { this.subTasks = subTasks; }
}
