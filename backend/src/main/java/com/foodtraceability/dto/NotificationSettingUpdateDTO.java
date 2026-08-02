package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.util.Map;

/**
 * 通知设置更新DTO
 * 用于更新通知通道配置（如SMTP配置）
 */
@Schema(description = "通知设置更新请求")
public class NotificationSettingUpdateDTO {
    @NotBlank(message = "设置键不能为空")
    @Size(min = 1, max = 100, message = "设置键长度必须在1-100个字符之间")
    @Pattern(regexp = "^[a-zA-Z0-9_.-]+$", message = "设置键只能包含字母、数字、下划线、点和横线")
    @Schema(description = "设置键(唯一)", example = "smtp.host", requiredMode = Schema.RequiredMode.REQUIRED)
    private String settingKey;
    @NotBlank(message = "设置值不能为空")
    @Size(max = 2000, message = "设置值长度不能超过2000个字符")
    @Schema(description = "设置值", example = "smtp.example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String settingValue;
    @Size(max = 50, message = "设置分组长度不能超过50个字符")
    @Schema(description = "设置分组", example = "SMTP")
    private String settingGroup;
    @Size(max = 500, message = "描述长度不能超过500个字符")
    @Schema(description = "描述说明", example = "SMTP服务器地址")
    private String description;
    /**
     * 是否加密存储: 0=否 1=是（密码等敏感字段需要加密）
     */
    @Min(value = 0, message = "加密标记不合法")
    @Max(value = 1, message = "加密标记不合法")
    @Schema(description = "是否加密存储:0=否 1=是", example = "0")
    private Integer isEncrypted = 0;
    /**
     * 状态: 0=DISABLED 1=ENABLED
     */
    @Min(value = 0, message = "状态值不合法")
    @Max(value = 1, message = "状态值不合法")
    @Schema(description = "状态:0=DISABLED 1=ENABLED", example = "1")
    private Integer status = 1;


    public static class NotificationSettingUpdateDTOBuilder {
        private String settingKey;
        private String settingValue;
        private String settingGroup;
        private String description;
        private Integer isEncrypted;
        private Integer status;

        NotificationSettingUpdateDTOBuilder() {
        }

        /**
         * @return {@code this}.
         */
        public NotificationSettingUpdateDTO.NotificationSettingUpdateDTOBuilder settingKey(final String settingKey) {
            this.settingKey = settingKey;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public NotificationSettingUpdateDTO.NotificationSettingUpdateDTOBuilder settingValue(final String settingValue) {
            this.settingValue = settingValue;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public NotificationSettingUpdateDTO.NotificationSettingUpdateDTOBuilder settingGroup(final String settingGroup) {
            this.settingGroup = settingGroup;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public NotificationSettingUpdateDTO.NotificationSettingUpdateDTOBuilder description(final String description) {
            this.description = description;
            return this;
        }

        /**
         * 是否加密存储: 0=否 1=是（密码等敏感字段需要加密）
         * @return {@code this}.
         */
        public NotificationSettingUpdateDTO.NotificationSettingUpdateDTOBuilder isEncrypted(final Integer isEncrypted) {
            this.isEncrypted = isEncrypted;
            return this;
        }

        /**
         * 状态: 0=DISABLED 1=ENABLED
         * @return {@code this}.
         */
        public NotificationSettingUpdateDTO.NotificationSettingUpdateDTOBuilder status(final Integer status) {
            this.status = status;
            return this;
        }

        public NotificationSettingUpdateDTO build() {
            return new NotificationSettingUpdateDTO(this.settingKey, this.settingValue, this.settingGroup, this.description, this.isEncrypted, this.status);
        }

        @java.lang.Override
        public java.lang.String toString() {
            return "NotificationSettingUpdateDTO.NotificationSettingUpdateDTOBuilder(settingKey=" + this.settingKey + ", settingValue=" + this.settingValue + ", settingGroup=" + this.settingGroup + ", description=" + this.description + ", isEncrypted=" + this.isEncrypted + ", status=" + this.status + ")";
        }
    }

    public static NotificationSettingUpdateDTO.NotificationSettingUpdateDTOBuilder builder() {
        return new NotificationSettingUpdateDTO.NotificationSettingUpdateDTOBuilder();
    }

    public String getSettingKey() {
        return this.settingKey;
    }

    public String getSettingValue() {
        return this.settingValue;
    }

    public String getSettingGroup() {
        return this.settingGroup;
    }

    public String getDescription() {
        return this.description;
    }

    /**
     * 是否加密存储: 0=否 1=是（密码等敏感字段需要加密）
     */
    public Integer getIsEncrypted() {
        return this.isEncrypted;
    }

    /**
     * 状态: 0=DISABLED 1=ENABLED
     */
    public Integer getStatus() {
        return this.status;
    }

    public void setSettingKey(final String settingKey) {
        this.settingKey = settingKey;
    }

    public void setSettingValue(final String settingValue) {
        this.settingValue = settingValue;
    }

    public void setSettingGroup(final String settingGroup) {
        this.settingGroup = settingGroup;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    /**
     * 是否加密存储: 0=否 1=是（密码等敏感字段需要加密）
     */
    public void setIsEncrypted(final Integer isEncrypted) {
        this.isEncrypted = isEncrypted;
    }

    /**
     * 状态: 0=DISABLED 1=ENABLED
     */
    public void setStatus(final Integer status) {
        this.status = status;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof NotificationSettingUpdateDTO)) return false;
        final NotificationSettingUpdateDTO other = (NotificationSettingUpdateDTO) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$isEncrypted = this.getIsEncrypted();
        final java.lang.Object other$isEncrypted = other.getIsEncrypted();
        if (this$isEncrypted == null ? other$isEncrypted != null : !this$isEncrypted.equals(other$isEncrypted)) return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
        final java.lang.Object this$settingKey = this.getSettingKey();
        final java.lang.Object other$settingKey = other.getSettingKey();
        if (this$settingKey == null ? other$settingKey != null : !this$settingKey.equals(other$settingKey)) return false;
        final java.lang.Object this$settingValue = this.getSettingValue();
        final java.lang.Object other$settingValue = other.getSettingValue();
        if (this$settingValue == null ? other$settingValue != null : !this$settingValue.equals(other$settingValue)) return false;
        final java.lang.Object this$settingGroup = this.getSettingGroup();
        final java.lang.Object other$settingGroup = other.getSettingGroup();
        if (this$settingGroup == null ? other$settingGroup != null : !this$settingGroup.equals(other$settingGroup)) return false;
        final java.lang.Object this$description = this.getDescription();
        final java.lang.Object other$description = other.getDescription();
        if (this$description == null ? other$description != null : !this$description.equals(other$description)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof NotificationSettingUpdateDTO;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $isEncrypted = this.getIsEncrypted();
        result = result * PRIME + ($isEncrypted == null ? 43 : $isEncrypted.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $settingKey = this.getSettingKey();
        result = result * PRIME + ($settingKey == null ? 43 : $settingKey.hashCode());
        final java.lang.Object $settingValue = this.getSettingValue();
        result = result * PRIME + ($settingValue == null ? 43 : $settingValue.hashCode());
        final java.lang.Object $settingGroup = this.getSettingGroup();
        result = result * PRIME + ($settingGroup == null ? 43 : $settingGroup.hashCode());
        final java.lang.Object $description = this.getDescription();
        result = result * PRIME + ($description == null ? 43 : $description.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "NotificationSettingUpdateDTO(settingKey=" + this.getSettingKey() + ", settingValue=" + this.getSettingValue() + ", settingGroup=" + this.getSettingGroup() + ", description=" + this.getDescription() + ", isEncrypted=" + this.getIsEncrypted() + ", status=" + this.getStatus() + ")";
    }

    public NotificationSettingUpdateDTO() {
    }

    /**
     * Creates a new {@code NotificationSettingUpdateDTO} instance.
     *
     * @param settingKey
     * @param settingValue
     * @param settingGroup
     * @param description
     * @param isEncrypted 是否加密存储: 0=否 1=是（密码等敏感字段需要加密）
     * @param status 状态: 0=DISABLED 1=ENABLED
     */
    public NotificationSettingUpdateDTO(final String settingKey, final String settingValue, final String settingGroup, final String description, final Integer isEncrypted, final Integer status) {
        this.settingKey = settingKey;
        this.settingValue = settingValue;
        this.settingGroup = settingGroup;
        this.description = description;
        this.isEncrypted = isEncrypted;
        this.status = status;
    }
}
