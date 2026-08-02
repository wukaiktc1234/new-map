package com.foodtraceability.dto.store.operation;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDateTime;

/**
 * 待办任务更新DTO
 * 用于更新待办任务状态，支持完成或取消操作
 */
@Schema(description = "待办任务更新DTO")
public class PendingTaskUpdateDTO {

    /**
     * 任务状态（仅允许completed或cancelled）
     * completed: 任务已完成
     * cancelled: 任务已取消
     */
    @NotBlank(message = "任务状态不能为空")
    @Pattern(regexp = "^(completed|cancelled)$", message = "任务状态只能是completed或cancelled")
    @Schema(description = "任务状态（completed=已完成 cancelled=已取消）", example = "completed", requiredMode = Schema.RequiredMode.REQUIRED)
    private String status;

    /** 完成时间（完成时必填） */
    @Schema(description = "完成时间", example = "2026-05-11T14:30:00")
    private LocalDateTime completedAt;

    // ==================== Getter & Setter ====================

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
}
