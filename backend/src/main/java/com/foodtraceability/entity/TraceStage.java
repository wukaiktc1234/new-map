package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 溯源阶段表
 * 标准化管理食品溯源的各个阶段
 */
@TableName("trace_stage")
@Schema(description = "溯源阶段实体")
public class TraceStage implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 阶段ID，主键
     */
    @TableId(value = "stage_id", type = IdType.AUTO)
    @Schema(description = "阶段ID", example = "1")
    private Integer stageId;
    /**
     * 阶段名称
     */
    @TableField("stage_name")
    @Schema(description = "阶段名称", example = "生产")
    private String stageName;
    /**
     * 阶段编码
     */
    @TableField("stage_code")
    @Schema(description = "阶段编码", example = "PRODUCTION")
    private String stageCode;
    /**
     * 排序顺序
     */
    @TableField("sort_order")
    @Schema(description = "排序顺序", example = "1")
    private Integer sortOrder;
    /**
     * 状态（1-启用，0-禁用）
     */
    @TableField("status")
    @Schema(description = "状态", example = "1")
    private Integer status;
    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    public TraceStage() {
    }

    /**
     * 阶段ID，主键
     */
    public Integer getStageId() {
        return this.stageId;
    }

    /**
     * 阶段名称
     */
    public String getStageName() {
        return this.stageName;
    }

    /**
     * 阶段编码
     */
    public String getStageCode() {
        return this.stageCode;
    }

    /**
     * 排序顺序
     */
    public Integer getSortOrder() {
        return this.sortOrder;
    }

    /**
     * 状态（1-启用，0-禁用）
     */
    public Integer getStatus() {
        return this.status;
    }

    /**
     * 创建时间
     */
    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    /**
     * 更新时间
     */
    public LocalDateTime getUpdatedAt() {
        return this.updatedAt;
    }

    /**
     * 阶段ID，主键
     */
    public void setStageId(final Integer stageId) {
        this.stageId = stageId;
    }

    /**
     * 阶段名称
     */
    public void setStageName(final String stageName) {
        this.stageName = stageName;
    }

    /**
     * 阶段编码
     */
    public void setStageCode(final String stageCode) {
        this.stageCode = stageCode;
    }

    /**
     * 排序顺序
     */
    public void setSortOrder(final Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    /**
     * 状态（1-启用，0-禁用）
     */
    public void setStatus(final Integer status) {
        this.status = status;
    }

    /**
     * 创建时间
     */
    public void setCreatedAt(final LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * 更新时间
     */
    public void setUpdatedAt(final LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "TraceStage(stageId=" + this.getStageId() + ", stageName=" + this.getStageName() + ", stageCode=" + this.getStageCode() + ", sortOrder=" + this.getSortOrder() + ", status=" + this.getStatus() + ", createdAt=" + this.getCreatedAt() + ", updatedAt=" + this.getUpdatedAt() + ")";
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof TraceStage)) return false;
        final TraceStage other = (TraceStage) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$stageId = this.getStageId();
        final java.lang.Object other$stageId = other.getStageId();
        if (this$stageId == null ? other$stageId != null : !this$stageId.equals(other$stageId)) return false;
        final java.lang.Object this$sortOrder = this.getSortOrder();
        final java.lang.Object other$sortOrder = other.getSortOrder();
        if (this$sortOrder == null ? other$sortOrder != null : !this$sortOrder.equals(other$sortOrder)) return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
        final java.lang.Object this$stageName = this.getStageName();
        final java.lang.Object other$stageName = other.getStageName();
        if (this$stageName == null ? other$stageName != null : !this$stageName.equals(other$stageName)) return false;
        final java.lang.Object this$stageCode = this.getStageCode();
        final java.lang.Object other$stageCode = other.getStageCode();
        if (this$stageCode == null ? other$stageCode != null : !this$stageCode.equals(other$stageCode)) return false;
        final java.lang.Object this$createdAt = this.getCreatedAt();
        final java.lang.Object other$createdAt = other.getCreatedAt();
        if (this$createdAt == null ? other$createdAt != null : !this$createdAt.equals(other$createdAt)) return false;
        final java.lang.Object this$updatedAt = this.getUpdatedAt();
        final java.lang.Object other$updatedAt = other.getUpdatedAt();
        if (this$updatedAt == null ? other$updatedAt != null : !this$updatedAt.equals(other$updatedAt)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof TraceStage;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $stageId = this.getStageId();
        result = result * PRIME + ($stageId == null ? 43 : $stageId.hashCode());
        final java.lang.Object $sortOrder = this.getSortOrder();
        result = result * PRIME + ($sortOrder == null ? 43 : $sortOrder.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $stageName = this.getStageName();
        result = result * PRIME + ($stageName == null ? 43 : $stageName.hashCode());
        final java.lang.Object $stageCode = this.getStageCode();
        result = result * PRIME + ($stageCode == null ? 43 : $stageCode.hashCode());
        final java.lang.Object $createdAt = this.getCreatedAt();
        result = result * PRIME + ($createdAt == null ? 43 : $createdAt.hashCode());
        final java.lang.Object $updatedAt = this.getUpdatedAt();
        result = result * PRIME + ($updatedAt == null ? 43 : $updatedAt.hashCode());
        return result;
    }
}
