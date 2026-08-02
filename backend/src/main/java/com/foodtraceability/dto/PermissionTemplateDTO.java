package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * 权限模板DTO
 */
@Schema(description = "权限模板DTO")
public class PermissionTemplateDTO {
    @Schema(description = "模板ID")
    private Integer id;
    @NotBlank(message = "模板名称不能为空")
    @Size(max = 100, message = "模板名称长度不能超过100个字符")
    @Schema(description = "模板名称")
    private String name;
    @NotBlank(message = "模板编码不能为空")
    @Size(max = 50, message = "模板编码长度不能超过50个字符")
    @Pattern(regexp = "^[a-z][a-z0-9_-]*$", message = "模板编码必须以小写字母开头，仅允许小写字母、数字、下划线和连字符")
    @Schema(description = "模板编码")
    private String code;
    @Size(max = 500, message = "模板描述长度不能超过500个字符")
    @Schema(description = "模板描述")
    private String description;
    @Size(max = 50, message = "适用企业类型长度不能超过50个字符")
    @Schema(description = "适用企业类型")
    private String enterpriseType;
    @Size(max = 50, message = "适用规模范围长度不能超过50个字符")
    @Schema(description = "适用规模范围")
    private String scaleRange;
    @Size(max = 10000, message = "角色配置长度不能超过10000个字符")
    @Schema(description = "角色配置（JSON格式）")
    private String roleConfig;
    @Schema(description = "是否系统模板")
    private Boolean isSystem;
    @Schema(description = "状态")
    private Integer status;
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    public PermissionTemplateDTO() {
    }

    public Integer getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public String getCode() {
        return this.code;
    }

    public String getDescription() {
        return this.description;
    }

    public String getEnterpriseType() {
        return this.enterpriseType;
    }

    public String getScaleRange() {
        return this.scaleRange;
    }

    public String getRoleConfig() {
        return this.roleConfig;
    }

    public Boolean getIsSystem() {
        return this.isSystem;
    }

    public Integer getStatus() {
        return this.status;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return this.updatedAt;
    }

    public void setId(final Integer id) {
        this.id = id;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public void setCode(final String code) {
        this.code = code;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public void setEnterpriseType(final String enterpriseType) {
        this.enterpriseType = enterpriseType;
    }

    public void setScaleRange(final String scaleRange) {
        this.scaleRange = scaleRange;
    }

    public void setRoleConfig(final String roleConfig) {
        this.roleConfig = roleConfig;
    }

    public void setIsSystem(final Boolean isSystem) {
        this.isSystem = isSystem;
    }

    public void setStatus(final Integer status) {
        this.status = status;
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
        if (!(o instanceof PermissionTemplateDTO)) return false;
        final PermissionTemplateDTO other = (PermissionTemplateDTO) o;
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
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof PermissionTemplateDTO;
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
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "PermissionTemplateDTO(id=" + this.getId() + ", name=" + this.getName() + ", code=" + this.getCode() + ", description=" + this.getDescription() + ", enterpriseType=" + this.getEnterpriseType() + ", scaleRange=" + this.getScaleRange() + ", roleConfig=" + this.getRoleConfig() + ", isSystem=" + this.getIsSystem() + ", status=" + this.getStatus() + ", createdAt=" + this.getCreatedAt() + ", updatedAt=" + this.getUpdatedAt() + ")";
    }
}
