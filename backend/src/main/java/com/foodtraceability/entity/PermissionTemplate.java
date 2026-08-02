package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 权限模板实体
 * 用于系统初始化时快速配置权限
 */
@TableName("permission_templates")
@Schema(description = "权限模板实体")
public class PermissionTemplate implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 模板ID
     */
    @TableId(type = IdType.AUTO)
    @Schema(description = "模板ID")
    private Integer id;
    /**
     * 模板名称
     */
    @Schema(description = "模板名称", example = "单门店小型餐饮")
    private String name;
    /**
     * 模板编码
     */
    @Schema(description = "模板编码", example = "single_small")
    private String code;
    /**
     * 模板描述
     */
    @Schema(description = "模板描述")
    private String description;
    /**
     * 适用企业类型
     * restaurant-餐饮, retail-零售, service-服务, all-通用
     */
    @Schema(description = "适用企业类型", example = "restaurant")
    private String enterpriseType;
    /**
     * 适用规模范围
     * 1-20人, 20-200人, 200人以上, all-不限
     */
    @Schema(description = "适用规模范围", example = "1-20")
    private String scaleRange;
    /**
     * 角色配置（JSON格式）
     * 包含角色定义和权限分配
     */
    @Schema(description = "角色配置（JSON格式）")
    private String roleConfig;
    /**
     * 是否系统模板
     */
    @Schema(description = "是否系统模板")
    private Boolean isSystem;
    /**
     * 状态（1-启用, 0-禁用）
     */
    @Schema(description = "状态（1-启用, 0-禁用）")
    private Integer status;
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
    /**
     * 是否删除（0-未删除，1-已删除）
     */
    @TableLogic
    @TableField("deleted")
    @Schema(description = "是否删除")
    private Integer deleted;

    public PermissionTemplate() {
    }

    /**
     * 模板ID
     */
    public Integer getId() {
        return this.id;
    }

    /**
     * 模板名称
     */
    public String getName() {
        return this.name;
    }

    /**
     * 模板编码
     */
    public String getCode() {
        return this.code;
    }

    /**
     * 模板描述
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * 适用企业类型
     * restaurant-餐饮, retail-零售, service-服务, all-通用
     */
    public String getEnterpriseType() {
        return this.enterpriseType;
    }

    /**
     * 适用规模范围
     * 1-20人, 20-200人, 200人以上, all-不限
     */
    public String getScaleRange() {
        return this.scaleRange;
    }

    /**
     * 角色配置（JSON格式）
     * 包含角色定义和权限分配
     */
    public String getRoleConfig() {
        return this.roleConfig;
    }

    /**
     * 是否系统模板
     */
    public Boolean getIsSystem() {
        return this.isSystem;
    }

    /**
     * 状态（1-启用, 0-禁用）
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
     * 模板ID
     */
    public void setId(final Integer id) {
        this.id = id;
    }

    /**
     * 模板名称
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * 模板编码
     */
    public void setCode(final String code) {
        this.code = code;
    }

    /**
     * 模板描述
     */
    public void setDescription(final String description) {
        this.description = description;
    }

    /**
     * 适用企业类型
     * restaurant-餐饮, retail-零售, service-服务, all-通用
     */
    public void setEnterpriseType(final String enterpriseType) {
        this.enterpriseType = enterpriseType;
    }

    /**
     * 适用规模范围
     * 1-20人, 20-200人, 200人以上, all-不限
     */
    public void setScaleRange(final String scaleRange) {
        this.scaleRange = scaleRange;
    }

    /**
     * 角色配置（JSON格式）
     * 包含角色定义和权限分配
     */
    public void setRoleConfig(final String roleConfig) {
        this.roleConfig = roleConfig;
    }

    /**
     * 是否系统模板
     */
    public void setIsSystem(final Boolean isSystem) {
        this.isSystem = isSystem;
    }

    /**
     * 状态（1-启用, 0-禁用）
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

    /**
     * 是否删除（0-未删除，1-已删除）
     */
    public Integer getDeleted() {
        return this.deleted;
    }

    /**
     * 是否删除（0-未删除，1-已删除）
     */
    public void setDeleted(final Integer deleted) {
        this.deleted = deleted;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof PermissionTemplate)) return false;
        final PermissionTemplate other = (PermissionTemplate) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$isSystem = this.getIsSystem();
        final java.lang.Object other$isSystem = other.getIsSystem();
        if (this$isSystem == null ? other$isSystem != null : !this$isSystem.equals(other$isSystem)) return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
        final java.lang.Object this$name = this.getName();
        final java.lang.Object other$name = other.getName();
        if (this$name == null ? other$name != null : !this$name.equals(other$name)) return false;
        final java.lang.Object this$code = this.getCode();
        final java.lang.Object other$code = other.getCode();
        if (this$code == null ? other$code != null : !this$code.equals(other$code)) return false;
        final java.lang.Object this$description = this.getDescription();
        final java.lang.Object other$description = other.getDescription();
        if (this$description == null ? other$description != null : !this$description.equals(other$description)) return false;
        final java.lang.Object this$enterpriseType = this.getEnterpriseType();
        final java.lang.Object other$enterpriseType = other.getEnterpriseType();
        if (this$enterpriseType == null ? other$enterpriseType != null : !this$enterpriseType.equals(other$enterpriseType)) return false;
        final java.lang.Object this$scaleRange = this.getScaleRange();
        final java.lang.Object other$scaleRange = other.getScaleRange();
        if (this$scaleRange == null ? other$scaleRange != null : !this$scaleRange.equals(other$scaleRange)) return false;
        final java.lang.Object this$roleConfig = this.getRoleConfig();
        final java.lang.Object other$roleConfig = other.getRoleConfig();
        if (this$roleConfig == null ? other$roleConfig != null : !this$roleConfig.equals(other$roleConfig)) return false;
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
        return other instanceof PermissionTemplate;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $isSystem = this.getIsSystem();
        result = result * PRIME + ($isSystem == null ? 43 : $isSystem.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $name = this.getName();
        result = result * PRIME + ($name == null ? 43 : $name.hashCode());
        final java.lang.Object $code = this.getCode();
        result = result * PRIME + ($code == null ? 43 : $code.hashCode());
        final java.lang.Object $description = this.getDescription();
        result = result * PRIME + ($description == null ? 43 : $description.hashCode());
        final java.lang.Object $enterpriseType = this.getEnterpriseType();
        result = result * PRIME + ($enterpriseType == null ? 43 : $enterpriseType.hashCode());
        final java.lang.Object $scaleRange = this.getScaleRange();
        result = result * PRIME + ($scaleRange == null ? 43 : $scaleRange.hashCode());
        final java.lang.Object $roleConfig = this.getRoleConfig();
        result = result * PRIME + ($roleConfig == null ? 43 : $roleConfig.hashCode());
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
        return "PermissionTemplate(id=" + this.getId() + ", name=" + this.getName() + ", code=" + this.getCode() + ", description=" + this.getDescription() + ", enterpriseType=" + this.getEnterpriseType() + ", scaleRange=" + this.getScaleRange() + ", roleConfig=" + this.getRoleConfig() + ", isSystem=" + this.getIsSystem() + ", status=" + this.getStatus() + ", createdAt=" + this.getCreatedAt() + ", updatedAt=" + this.getUpdatedAt() + ", createdBy=" + this.getCreatedBy() + ", updatedBy=" + this.getUpdatedBy() + ")";
    }
}
