package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 企业信息DTO
 */
@Schema(description = "企业信息DTO")
public class EnterpriseInfoDTO {
    @NotBlank(message = "企业名称不能为空")
    @Schema(description = "企业名称", example = "XX餐饮有限公司")
    private String enterpriseName;
    @NotBlank(message = "企业类型不能为空")
    @Schema(description = "企业类型", example = "restaurant")
    private String enterpriseType;
    @NotBlank(message = "企业规模不能为空")
    @Schema(description = "企业规模", example = "1-20")
    private String scale;
    @Schema(description = "自定义配置")
    private String customConfig;

    public EnterpriseInfoDTO() {
    }

    public String getEnterpriseName() {
        return this.enterpriseName;
    }

    public String getEnterpriseType() {
        return this.enterpriseType;
    }

    public String getScale() {
        return this.scale;
    }

    public String getCustomConfig() {
        return this.customConfig;
    }

    public void setEnterpriseName(final String enterpriseName) {
        this.enterpriseName = enterpriseName;
    }

    public void setEnterpriseType(final String enterpriseType) {
        this.enterpriseType = enterpriseType;
    }

    public void setScale(final String scale) {
        this.scale = scale;
    }

    public void setCustomConfig(final String customConfig) {
        this.customConfig = customConfig;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof EnterpriseInfoDTO)) return false;
        final EnterpriseInfoDTO other = (EnterpriseInfoDTO) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$enterpriseName = this.getEnterpriseName();
        final java.lang.Object other$enterpriseName = other.getEnterpriseName();
        if (this$enterpriseName == null ? other$enterpriseName != null : !this$enterpriseName.equals(other$enterpriseName)) return false;
        final java.lang.Object this$enterpriseType = this.getEnterpriseType();
        final java.lang.Object other$enterpriseType = other.getEnterpriseType();
        if (this$enterpriseType == null ? other$enterpriseType != null : !this$enterpriseType.equals(other$enterpriseType)) return false;
        final java.lang.Object this$scale = this.getScale();
        final java.lang.Object other$scale = other.getScale();
        if (this$scale == null ? other$scale != null : !this$scale.equals(other$scale)) return false;
        final java.lang.Object this$customConfig = this.getCustomConfig();
        final java.lang.Object other$customConfig = other.getCustomConfig();
        if (this$customConfig == null ? other$customConfig != null : !this$customConfig.equals(other$customConfig)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof EnterpriseInfoDTO;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $enterpriseName = this.getEnterpriseName();
        result = result * PRIME + ($enterpriseName == null ? 43 : $enterpriseName.hashCode());
        final java.lang.Object $enterpriseType = this.getEnterpriseType();
        result = result * PRIME + ($enterpriseType == null ? 43 : $enterpriseType.hashCode());
        final java.lang.Object $scale = this.getScale();
        result = result * PRIME + ($scale == null ? 43 : $scale.hashCode());
        final java.lang.Object $customConfig = this.getCustomConfig();
        result = result * PRIME + ($customConfig == null ? 43 : $customConfig.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "EnterpriseInfoDTO(enterpriseName=" + this.getEnterpriseName() + ", enterpriseType=" + this.getEnterpriseType() + ", scale=" + this.getScale() + ", customConfig=" + this.getCustomConfig() + ")";
    }
}
