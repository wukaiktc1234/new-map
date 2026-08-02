package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 系统初始化状态DTO
 */
@Schema(description = "系统初始化状态DTO")
public class SystemInitStatusDTO {
    @Schema(description = "当前步骤")
    private String step;
    @Schema(description = "是否完成")
    private Boolean isCompleted;
    @Schema(description = "完成时间")
    private LocalDateTime completedAt;
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    public SystemInitStatusDTO() {
    }

    public String getStep() {
        return this.step;
    }

    public Boolean getIsCompleted() {
        return this.isCompleted;
    }

    public LocalDateTime getCompletedAt() {
        return this.completedAt;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return this.updatedAt;
    }

    public void setStep(final String step) {
        this.step = step;
    }

    public void setIsCompleted(final Boolean isCompleted) {
        this.isCompleted = isCompleted;
    }

    public void setCompletedAt(final LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public void setCreatedAt(final LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(final LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof SystemInitStatusDTO)) return false;
        final SystemInitStatusDTO other = (SystemInitStatusDTO) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$isCompleted = this.getIsCompleted();
        final java.lang.Object other$isCompleted = other.getIsCompleted();
        if (this$isCompleted == null ? other$isCompleted != null : !this$isCompleted.equals(other$isCompleted)) return false;
        final java.lang.Object this$step = this.getStep();
        final java.lang.Object other$step = other.getStep();
        if (this$step == null ? other$step != null : !this$step.equals(other$step)) return false;
        final java.lang.Object this$completedAt = this.getCompletedAt();
        final java.lang.Object other$completedAt = other.getCompletedAt();
        if (this$completedAt == null ? other$completedAt != null : !this$completedAt.equals(other$completedAt)) return false;
        final java.lang.Object this$createdAt = this.getCreatedAt();
        final java.lang.Object other$createdAt = other.getCreatedAt();
        if (this$createdAt == null ? other$createdAt != null : !this$createdAt.equals(other$createdAt)) return false;
        final java.lang.Object this$updatedAt = this.getUpdatedAt();
        final java.lang.Object other$updatedAt = other.getUpdatedAt();
        if (this$updatedAt == null ? other$updatedAt != null : !this$updatedAt.equals(other$updatedAt)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof SystemInitStatusDTO;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $isCompleted = this.getIsCompleted();
        result = result * PRIME + ($isCompleted == null ? 43 : $isCompleted.hashCode());
        final java.lang.Object $step = this.getStep();
        result = result * PRIME + ($step == null ? 43 : $step.hashCode());
        final java.lang.Object $completedAt = this.getCompletedAt();
        result = result * PRIME + ($completedAt == null ? 43 : $completedAt.hashCode());
        final java.lang.Object $createdAt = this.getCreatedAt();
        result = result * PRIME + ($createdAt == null ? 43 : $createdAt.hashCode());
        final java.lang.Object $updatedAt = this.getUpdatedAt();
        result = result * PRIME + ($updatedAt == null ? 43 : $updatedAt.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "SystemInitStatusDTO(step=" + this.getStep() + ", isCompleted=" + this.getIsCompleted() + ", completedAt=" + this.getCompletedAt() + ", createdAt=" + this.getCreatedAt() + ", updatedAt=" + this.getUpdatedAt() + ")";
    }
}
