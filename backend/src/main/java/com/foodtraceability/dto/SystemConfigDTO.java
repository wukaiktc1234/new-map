package com.foodtraceability.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 系统配置DTO
 */
public class SystemConfigDTO {
    private Long id;
    private String configKey;
    private String configValue;
    private String configType;
    private String description;
    private Boolean encrypted;
    public static final String TYPE_TAX_PLATFORM = "tax_platform";
    public static final String KEY_TAX_PLATFORM_ENABLED = "tax_platform.enabled";
    public static final String KEY_TAX_PLATFORM_TEST_MODE = "tax_platform.test_mode";
    public static final String KEY_TAX_PLATFORM_URL = "tax_platform.url";
    public static final String KEY_TAX_PLATFORM_APP_KEY = "tax_platform.app_key";
    public static final String KEY_TAX_PLATFORM_APP_SECRET = "tax_platform.app_secret";
    public static final String KEY_TAX_PLATFORM_TIMEOUT = "tax_platform.timeout";


    public static class SystemConfigDTOBuilder {
        private Long id;
        private String configKey;
        private String configValue;
        private String configType;
        private String description;
        private Boolean encrypted;

        SystemConfigDTOBuilder() {
        }

        /**
         * @return {@code this}.
         */
        public SystemConfigDTO.SystemConfigDTOBuilder id(final Long id) {
            this.id = id;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public SystemConfigDTO.SystemConfigDTOBuilder configKey(final String configKey) {
            this.configKey = configKey;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public SystemConfigDTO.SystemConfigDTOBuilder configValue(final String configValue) {
            this.configValue = configValue;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public SystemConfigDTO.SystemConfigDTOBuilder configType(final String configType) {
            this.configType = configType;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public SystemConfigDTO.SystemConfigDTOBuilder description(final String description) {
            this.description = description;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public SystemConfigDTO.SystemConfigDTOBuilder encrypted(final Boolean encrypted) {
            this.encrypted = encrypted;
            return this;
        }

        public SystemConfigDTO build() {
            return new SystemConfigDTO(this.id, this.configKey, this.configValue, this.configType, this.description, this.encrypted);
        }

        @java.lang.Override
        public java.lang.String toString() {
            return "SystemConfigDTO.SystemConfigDTOBuilder(id=" + this.id + ", configKey=" + this.configKey + ", configValue=" + this.configValue + ", configType=" + this.configType + ", description=" + this.description + ", encrypted=" + this.encrypted + ")";
        }
    }

    public static SystemConfigDTO.SystemConfigDTOBuilder builder() {
        return new SystemConfigDTO.SystemConfigDTOBuilder();
    }

    public Long getId() {
        return this.id;
    }

    public String getConfigKey() {
        return this.configKey;
    }

    public String getConfigValue() {
        return this.configValue;
    }

    public String getConfigType() {
        return this.configType;
    }

    public String getDescription() {
        return this.description;
    }

    public Boolean getEncrypted() {
        return this.encrypted;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setConfigKey(final String configKey) {
        this.configKey = configKey;
    }

    public void setConfigValue(final String configValue) {
        this.configValue = configValue;
    }

    public void setConfigType(final String configType) {
        this.configType = configType;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public void setEncrypted(final Boolean encrypted) {
        this.encrypted = encrypted;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof SystemConfigDTO)) return false;
        final SystemConfigDTO other = (SystemConfigDTO) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$encrypted = this.getEncrypted();
        final java.lang.Object other$encrypted = other.getEncrypted();
        if (this$encrypted == null ? other$encrypted != null : !this$encrypted.equals(other$encrypted)) return false;
        final java.lang.Object this$configKey = this.getConfigKey();
        final java.lang.Object other$configKey = other.getConfigKey();
        if (this$configKey == null ? other$configKey != null : !this$configKey.equals(other$configKey)) return false;
        final java.lang.Object this$configValue = this.getConfigValue();
        final java.lang.Object other$configValue = other.getConfigValue();
        if (this$configValue == null ? other$configValue != null : !this$configValue.equals(other$configValue)) return false;
        final java.lang.Object this$configType = this.getConfigType();
        final java.lang.Object other$configType = other.getConfigType();
        if (this$configType == null ? other$configType != null : !this$configType.equals(other$configType)) return false;
        final java.lang.Object this$description = this.getDescription();
        final java.lang.Object other$description = other.getDescription();
        if (this$description == null ? other$description != null : !this$description.equals(other$description)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof SystemConfigDTO;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $encrypted = this.getEncrypted();
        result = result * PRIME + ($encrypted == null ? 43 : $encrypted.hashCode());
        final java.lang.Object $configKey = this.getConfigKey();
        result = result * PRIME + ($configKey == null ? 43 : $configKey.hashCode());
        final java.lang.Object $configValue = this.getConfigValue();
        result = result * PRIME + ($configValue == null ? 43 : $configValue.hashCode());
        final java.lang.Object $configType = this.getConfigType();
        result = result * PRIME + ($configType == null ? 43 : $configType.hashCode());
        final java.lang.Object $description = this.getDescription();
        result = result * PRIME + ($description == null ? 43 : $description.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "SystemConfigDTO(id=" + this.getId() + ", configKey=" + this.getConfigKey() + ", configValue=" + this.getConfigValue() + ", configType=" + this.getConfigType() + ", description=" + this.getDescription() + ", encrypted=" + this.getEncrypted() + ")";
    }

    public SystemConfigDTO() {
    }

    public SystemConfigDTO(final Long id, final String configKey, final String configValue, final String configType, final String description, final Boolean encrypted) {
        this.id = id;
        this.configKey = configKey;
        this.configValue = configValue;
        this.configType = configType;
        this.description = description;
        this.encrypted = encrypted;
    }
}
