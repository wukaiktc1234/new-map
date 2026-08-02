package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 操作类型表
 * 标准化管理食品溯源的各个操作类型
 */
@TableName("operation_type")
@Schema(description = "操作类型实体")
public class OperationType implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 操作类型ID，主键
     */
    @TableId(value = "op_type_id", type = IdType.AUTO)
    @Schema(description = "操作类型ID", example = "1")
    private Integer opTypeId;
    /**
     * 操作类型名称
     */
    @TableField("op_type_name")
    @Schema(description = "操作类型名称", example = "采摘")
    private String opTypeName;
    /**
     * 操作类型编码
     */
    @TableField("op_type_code")
    @Schema(description = "操作类型编码", example = "HARVEST")
    private String opTypeCode;
    /**
     * 所属阶段ID，外键
     */
    @TableField("stage_id")
    @Schema(description = "所属阶段ID", example = "1")
    private Integer stageId;
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

    public OperationType() {
    }

    /**
     * 操作类型ID，主键
     */
    public Integer getOpTypeId() {
        return this.opTypeId;
    }

    /**
     * 操作类型名称
     */
    public String getOpTypeName() {
        return this.opTypeName;
    }

    /**
     * 操作类型编码
     */
    public String getOpTypeCode() {
        return this.opTypeCode;
    }

    /**
     * 所属阶段ID，外键
     */
    public Integer getStageId() {
        return this.stageId;
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
     * 操作类型ID，主键
     */
    public void setOpTypeId(final Integer opTypeId) {
        this.opTypeId = opTypeId;
    }

    /**
     * 操作类型名称
     */
    public void setOpTypeName(final String opTypeName) {
        this.opTypeName = opTypeName;
    }

    /**
     * 操作类型编码
     */
    public void setOpTypeCode(final String opTypeCode) {
        this.opTypeCode = opTypeCode;
    }

    /**
     * 所属阶段ID，外键
     */
    public void setStageId(final Integer stageId) {
        this.stageId = stageId;
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
        return "OperationType(opTypeId=" + this.getOpTypeId() + ", opTypeName=" + this.getOpTypeName() + ", opTypeCode=" + this.getOpTypeCode() + ", stageId=" + this.getStageId() + ", createdAt=" + this.getCreatedAt() + ", updatedAt=" + this.getUpdatedAt() + ")";
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof OperationType)) return false;
        final OperationType other = (OperationType) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$opTypeId = this.getOpTypeId();
        final java.lang.Object other$opTypeId = other.getOpTypeId();
        if (this$opTypeId == null ? other$opTypeId != null : !this$opTypeId.equals(other$opTypeId)) return false;
        final java.lang.Object this$stageId = this.getStageId();
        final java.lang.Object other$stageId = other.getStageId();
        if (this$stageId == null ? other$stageId != null : !this$stageId.equals(other$stageId)) return false;
        final java.lang.Object this$opTypeName = this.getOpTypeName();
        final java.lang.Object other$opTypeName = other.getOpTypeName();
        if (this$opTypeName == null ? other$opTypeName != null : !this$opTypeName.equals(other$opTypeName)) return false;
        final java.lang.Object this$opTypeCode = this.getOpTypeCode();
        final java.lang.Object other$opTypeCode = other.getOpTypeCode();
        if (this$opTypeCode == null ? other$opTypeCode != null : !this$opTypeCode.equals(other$opTypeCode)) return false;
        final java.lang.Object this$createdAt = this.getCreatedAt();
        final java.lang.Object other$createdAt = other.getCreatedAt();
        if (this$createdAt == null ? other$createdAt != null : !this$createdAt.equals(other$createdAt)) return false;
        final java.lang.Object this$updatedAt = this.getUpdatedAt();
        final java.lang.Object other$updatedAt = other.getUpdatedAt();
        if (this$updatedAt == null ? other$updatedAt != null : !this$updatedAt.equals(other$updatedAt)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof OperationType;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $opTypeId = this.getOpTypeId();
        result = result * PRIME + ($opTypeId == null ? 43 : $opTypeId.hashCode());
        final java.lang.Object $stageId = this.getStageId();
        result = result * PRIME + ($stageId == null ? 43 : $stageId.hashCode());
        final java.lang.Object $opTypeName = this.getOpTypeName();
        result = result * PRIME + ($opTypeName == null ? 43 : $opTypeName.hashCode());
        final java.lang.Object $opTypeCode = this.getOpTypeCode();
        result = result * PRIME + ($opTypeCode == null ? 43 : $opTypeCode.hashCode());
        final java.lang.Object $createdAt = this.getCreatedAt();
        result = result * PRIME + ($createdAt == null ? 43 : $createdAt.hashCode());
        final java.lang.Object $updatedAt = this.getUpdatedAt();
        result = result * PRIME + ($updatedAt == null ? 43 : $updatedAt.hashCode());
        return result;
    }
}
