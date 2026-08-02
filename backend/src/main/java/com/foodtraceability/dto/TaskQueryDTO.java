package com.foodtraceability.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

/**
 * 待办任务查询DTO
 * 用于门店运营模块的任务筛选和分页查询
 */
@Schema(description = "待办任务查询DTO")
public class TaskQueryDTO extends PageQuery {

    @Schema(description = "任务类型（approval/inspection/inventory/remind等）", example = "approval")
    private String taskType;

    @Schema(description = "任务分类", example = "daily")
    private String category;

    @Schema(description = "任务状态（pending/in_progress/completed/cancelled/expired）", example = "pending")
    private String status;

    /**
     * 优先级：1=低 2=中 3=高 4=紧急
     */
    @Schema(description = "优先级（1=低 2=中 3=高 4=紧急）")
    private Integer priority;

    @Schema(description = "被指派人ID", example = "user001")
    private String assigneeId;

    @Schema(description = "来源类型（order/purchase/recruitment等）", example = "order")
    private String sourceType;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "截止日期-开始", example = "2026-05-01")
    private LocalDate startDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "截止日期-结束", example = "2026-05-31")
    private LocalDate endDate;

    @Schema(description = "关键词（搜索标题或描述）", example = "审核")
    private String keyword;

    // ==================== Getter & Setter ====================

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
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

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }
}
