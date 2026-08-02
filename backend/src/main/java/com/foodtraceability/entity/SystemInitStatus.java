package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统初始化状态实体
 * 用于记录系统初始化向导的进度
 */
@TableName("system_init_status")
@Schema(description = "系统初始化状态实体")
public class SystemInitStatus implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Integer id;
    /**
     * 当前步骤
     * welcome-欢迎页面, enterprise-企业信息, organization-组织架构, 
     * roles-角色配置, completed-完成
     */
    @Schema(description = "当前步骤", example = "welcome")
    private String step;
    /**
     * 是否完成
     */
    @Schema(description = "是否完成")
    private Boolean isCompleted;
    /**
     * 完成时间
     */
    @Schema(description = "完成时间")
    private LocalDateTime completedAt;
    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
    /**
     * 更新时间
     */
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    public SystemInitStatus() {
    }

    /**
     * 主键ID
     */
    public Integer getId() {
        return this.id;
    }

    /**
     * 当前步骤
     * welcome-欢迎页面, enterprise-企业信息, organization-组织架构, 
     * roles-角色配置, completed-完成
     */
    public String getStep() {
        return this.step;
    }

    /**
     * 是否完成
     */
    public Boolean getIsCompleted() {
        return this.isCompleted;
    }

    /**
     * 完成时间
     */
    public LocalDateTime getCompletedAt() {
        return this.completedAt;
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
     * 主键ID
     */
    public void setId(final Integer id) {
        this.id = id;
    }

    /**
     * 当前步骤
     * welcome-欢迎页面, enterprise-企业信息, organization-组织架构, 
     * roles-角色配置, completed-完成
     */
    public void setStep(final String step) {
        this.step = step;
    }

    /**
     * 是否完成
     */
    public void setIsCompleted(final Boolean isCompleted) {
        this.isCompleted = isCompleted;
    }

    /**
     * 完成时间
     */
    public void setCompletedAt(final LocalDateTime completedAt) {
        this.completedAt = completedAt;
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
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof SystemInitStatus)) return false;
        final SystemInitStatus other = (SystemInitStatus) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
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
        return other instanceof SystemInitStatus;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
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
        return "SystemInitStatus(id=" + this.getId() + ", step=" + this.getStep() + ", isCompleted=" + this.getIsCompleted() + ", completedAt=" + this.getCompletedAt() + ", createdAt=" + this.getCreatedAt() + ", updatedAt=" + this.getUpdatedAt() + ")";
    }
}
