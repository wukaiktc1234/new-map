package com.foodtraceability.dto.store.operation;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * 待办任务创建DTO
 * 用于创建新的待办任务，支持多种任务类型和优先级设置
 */
@Schema(description = "待办任务创建DTO")
public class PendingTaskCreateDTO {

    /** 任务类型（如：approval/inspection/inventory/remind等） */
    @NotBlank(message = "任务类型不能为空")
    @Schema(description = "任务类型（approval/inspection/inventory/remind等）", example = "approval", requiredMode = Schema.RequiredMode.REQUIRED)
    private String taskType;

    /** 任务标题 */
    @NotBlank(message = "任务标题不能为空")
    @Size(max = 200, message = "任务标题长度不能超过200个字符")
    @Schema(description = "任务标题", example = "采购订单审批", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    /** 任务优先级（1=低 2=中 3=高 4=紧急） */
    @Min(value = 1, message = "优先级不能小于1")
    @Max(value = 4, message = "优先级不能大于4")
    @Schema(description = "优先级：1=低 2=中 3=高 4=紧急", example = "3", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer priority;

    /** 被指派人ID */
    @NotBlank(message = "被指派人ID不能为空")
    @Schema(description = "被指派人ID", example = "user001", requiredMode = Schema.RequiredMode.REQUIRED)
    private String assigneeId;

    /** 关联来源类型（如：order/purchase/recruitment等） */
    @Schema(description = "来源类型（order/purchase/recruitment等）", example = "purchase")
    private String sourceType;

    /** 关联来源业务ID */
    @Schema(description = "来源业务ID", example = "PO20260511001")
    private String sourceId;

    /** 任务跳转链接（点击后跳转到对应业务页面） */
    @Schema(description = "跳转链接", example = "/purchase/orders/PO20260511001")
    private String redirectUrl;

    /** 任务详细描述 */
    @Size(max = 1000, message = "任务描述长度不能超过1000个字符")
    @Schema(description = "任务描述", example = "请审批采购订单PO20260511001，金额5000元")
    private String description;

    /** 截止日期 */
    @Schema(description = "截止日期", example = "2026-05-20")
    private LocalDate dueDate;

    // ==================== Getter & Setter ====================

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }
}
