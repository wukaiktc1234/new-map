package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 企业配置实体
 * 用于存储企业基本信息和初始化状态
 */
@TableName("enterprise_config")
@Schema(description = "企业配置实体")
public class EnterpriseConfig implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 配置ID
     */
    @TableId(type = IdType.AUTO)
    @Schema(description = "配置ID")
    private Integer id;
    /**
     * 企业名称
     */
    @Schema(description = "企业名称", example = "XX餐饮有限公司")
    private String enterpriseName;
    /**
     * 企业类型
     * restaurant-餐饮, retail-零售, service-服务
     */
    @Schema(description = "企业类型", example = "restaurant")
    private String enterpriseType;
    /**
     * 企业规模
     * 1-20人, 20-200人, 200人以上
     */
    @Schema(description = "企业规模", example = "1-20")
    private String scale;
    /**
     * 使用的权限模板ID
     */
    @Schema(description = "使用的权限模板ID")
    private Integer initTemplateId;
    /**
     * 初始化是否完成
     */
    @Schema(description = "初始化是否完成")
    private Boolean initCompleted;
    /**
     * 初始化完成时间
     */
    @Schema(description = "初始化完成时间")
    private LocalDateTime initCompletedAt;
    /**
     * 自定义配置（JSON格式）
     */
    @Schema(description = "自定义配置（JSON格式）")
    private String customConfig;
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
    /**
     * 创建人ID
     */
    @Schema(description = "创建人ID")
    private String createdBy;
    /**
     * 更新人ID
     */
    @Schema(description = "更新人ID")
    private String updatedBy;

    public EnterpriseConfig() {
    }

    /**
     * 配置ID
     */
    public Integer getId() {
        return this.id;
    }

    /**
     * 企业名称
     */
    public String getEnterpriseName() {
        return this.enterpriseName;
    }

    /**
     * 企业类型
     * restaurant-餐饮, retail-零售, service-服务
     */
    public String getEnterpriseType() {
        return this.enterpriseType;
    }

    /**
     * 企业规模
     * 1-20人, 20-200人, 200人以上
     */
    public String getScale() {
        return this.scale;
    }

    /**
     * 使用的权限模板ID
     */
    public Integer getInitTemplateId() {
        return this.initTemplateId;
    }

    /**
     * 初始化是否完成
     */
    public Boolean getInitCompleted() {
        return this.initCompleted;
    }

    /**
     * 初始化完成时间
     */
    public LocalDateTime getInitCompletedAt() {
        return this.initCompletedAt;
    }

    /**
     * 自定义配置（JSON格式）
     */
    public String getCustomConfig() {
        return this.customConfig;
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
     * 创建人ID
     */
    public String getCreatedBy() {
        return this.createdBy;
    }

    /**
     * 更新人ID
     */
    public String getUpdatedBy() {
        return this.updatedBy;
    }

    /**
     * 配置ID
     */
    public void setId(final Integer id) {
        this.id = id;
    }

    /**
     * 企业名称
     */
    public void setEnterpriseName(final String enterpriseName) {
        this.enterpriseName = enterpriseName;
    }

    /**
     * 企业类型
     * restaurant-餐饮, retail-零售, service-服务
     */
    public void setEnterpriseType(final String enterpriseType) {
        this.enterpriseType = enterpriseType;
    }

    /**
     * 企业规模
     * 1-20人, 20-200人, 200人以上
     */
    public void setScale(final String scale) {
        this.scale = scale;
    }

    /**
     * 使用的权限模板ID
     */
    public void setInitTemplateId(final Integer initTemplateId) {
        this.initTemplateId = initTemplateId;
    }

    /**
     * 初始化是否完成
     */
    public void setInitCompleted(final Boolean initCompleted) {
        this.initCompleted = initCompleted;
    }

    /**
     * 初始化完成时间
     */
    public void setInitCompletedAt(final LocalDateTime initCompletedAt) {
        this.initCompletedAt = initCompletedAt;
    }

    /**
     * 自定义配置（JSON格式）
     */
    public void setCustomConfig(final String customConfig) {
        this.customConfig = customConfig;
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

    /**
     * 创建人ID
     */
    public void setCreatedBy(final String createdBy) {
        this.createdBy = createdBy;
    }

    /**
     * 更新人ID
     */
    public void setUpdatedBy(final String updatedBy) {
        this.updatedBy = updatedBy;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof EnterpriseConfig)) return false;
        final EnterpriseConfig other = (EnterpriseConfig) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$initTemplateId = this.getInitTemplateId();
        final java.lang.Object other$initTemplateId = other.getInitTemplateId();
        if (this$initTemplateId == null ? other$initTemplateId != null : !this$initTemplateId.equals(other$initTemplateId)) return false;
        final java.lang.Object this$initCompleted = this.getInitCompleted();
        final java.lang.Object other$initCompleted = other.getInitCompleted();
        if (this$initCompleted == null ? other$initCompleted != null : !this$initCompleted.equals(other$initCompleted)) return false;
        final java.lang.Object this$enterpriseName = this.getEnterpriseName();
        final java.lang.Object other$enterpriseName = other.getEnterpriseName();
        if (this$enterpriseName == null ? other$enterpriseName != null : !this$enterpriseName.equals(other$enterpriseName)) return false;
        final java.lang.Object this$enterpriseType = this.getEnterpriseType();
        final java.lang.Object other$enterpriseType = other.getEnterpriseType();
        if (this$enterpriseType == null ? other$enterpriseType != null : !this$enterpriseType.equals(other$enterpriseType)) return false;
        final java.lang.Object this$scale = this.getScale();
        final java.lang.Object other$scale = other.getScale();
        if (this$scale == null ? other$scale != null : !this$scale.equals(other$scale)) return false;
        final java.lang.Object this$initCompletedAt = this.getInitCompletedAt();
        final java.lang.Object other$initCompletedAt = other.getInitCompletedAt();
        if (this$initCompletedAt == null ? other$initCompletedAt != null : !this$initCompletedAt.equals(other$initCompletedAt)) return false;
        final java.lang.Object this$customConfig = this.getCustomConfig();
        final java.lang.Object other$customConfig = other.getCustomConfig();
        if (this$customConfig == null ? other$customConfig != null : !this$customConfig.equals(other$customConfig)) return false;
        final java.lang.Object this$createdAt = this.getCreatedAt();
        final java.lang.Object other$createdAt = other.getCreatedAt();
        if (this$createdAt == null ? other$createdAt != null : !this$createdAt.equals(other$createdAt)) return false;
        final java.lang.Object this$updatedAt = this.getUpdatedAt();
        final java.lang.Object other$updatedAt = other.getUpdatedAt();
        if (this$updatedAt == null ? other$updatedAt != null : !this$updatedAt.equals(other$updatedAt)) return false;
        final java.lang.Object this$createdBy = this.getCreatedBy();
        final java.lang.Object other$createdBy = other.getCreatedBy();
        if (this$createdBy == null ? other$createdBy != null : !this$createdBy.equals(other$createdBy)) return false;
        final java.lang.Object this$updatedBy = this.getUpdatedBy();
        final java.lang.Object other$updatedBy = other.getUpdatedBy();
        if (this$updatedBy == null ? other$updatedBy != null : !this$updatedBy.equals(other$updatedBy)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof EnterpriseConfig;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $initTemplateId = this.getInitTemplateId();
        result = result * PRIME + ($initTemplateId == null ? 43 : $initTemplateId.hashCode());
        final java.lang.Object $initCompleted = this.getInitCompleted();
        result = result * PRIME + ($initCompleted == null ? 43 : $initCompleted.hashCode());
        final java.lang.Object $enterpriseName = this.getEnterpriseName();
        result = result * PRIME + ($enterpriseName == null ? 43 : $enterpriseName.hashCode());
        final java.lang.Object $enterpriseType = this.getEnterpriseType();
        result = result * PRIME + ($enterpriseType == null ? 43 : $enterpriseType.hashCode());
        final java.lang.Object $scale = this.getScale();
        result = result * PRIME + ($scale == null ? 43 : $scale.hashCode());
        final java.lang.Object $initCompletedAt = this.getInitCompletedAt();
        result = result * PRIME + ($initCompletedAt == null ? 43 : $initCompletedAt.hashCode());
        final java.lang.Object $customConfig = this.getCustomConfig();
        result = result * PRIME + ($customConfig == null ? 43 : $customConfig.hashCode());
        final java.lang.Object $createdAt = this.getCreatedAt();
        result = result * PRIME + ($createdAt == null ? 43 : $createdAt.hashCode());
        final java.lang.Object $updatedAt = this.getUpdatedAt();
        result = result * PRIME + ($updatedAt == null ? 43 : $updatedAt.hashCode());
        final java.lang.Object $createdBy = this.getCreatedBy();
        result = result * PRIME + ($createdBy == null ? 43 : $createdBy.hashCode());
        final java.lang.Object $updatedBy = this.getUpdatedBy();
        result = result * PRIME + ($updatedBy == null ? 43 : $updatedBy.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "EnterpriseConfig(id=" + this.getId() + ", enterpriseName=" + this.getEnterpriseName() + ", enterpriseType=" + this.getEnterpriseType() + ", scale=" + this.getScale() + ", initTemplateId=" + this.getInitTemplateId() + ", initCompleted=" + this.getInitCompleted() + ", initCompletedAt=" + this.getInitCompletedAt() + ", customConfig=" + this.getCustomConfig() + ", createdAt=" + this.getCreatedAt() + ", updatedAt=" + this.getUpdatedAt() + ", createdBy=" + this.getCreatedBy() + ", updatedBy=" + this.getUpdatedBy() + ")";
    }
}
