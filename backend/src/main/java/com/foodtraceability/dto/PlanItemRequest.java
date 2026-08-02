package com.foodtraceability.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

/**
 * 计划项请求DTO
 * 
 * @author demo
 * @since 1.0.0
 */
public class PlanItemRequest {

    /**
     * 任务标题
     */
    @NotBlank(message = "任务标题不能为空")
    private String title;

    /**
     * 任务描述
     */
    private String description;

    /**
     * 任务类型：task（任务）、idea（想法）
     */
    @NotBlank(message = "任务类型不能为空")
    private String type;

    /**
     * 优先级：high（高）、medium（中）、low（低）
     */
    @NotBlank(message = "优先级不能为空")
    private String priority;

    /**
     * 状态：todo（未开始）、doing（进行中）、done（已完成）
     */
    @NotBlank(message = "状态不能为空")
    private String status;

    /**
     * 负责人
     */
    private String assignee;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 备注
     */
    private String remark;

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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAssignee() {
        return assignee;
    }

    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
