package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 系统配置批量更新项DTO
 * 用于批量修改系统配置值的请求参数
 */
@Schema(description = "系统配置批量更新项DTO")
public class SysConfigBatchUpdateItemDTO {

    @NotBlank(message = "配置键不能为空")
    @Schema(description = "配置键", example = "system.name")
    private String configKey;

    @NotBlank(message = "配置值不能为空")
    @Schema(description = "配置值", example = "食品溯源系统")
    private String value;

    public SysConfigBatchUpdateItemDTO() {
    }

    public String getConfigKey() {
        return configKey;
    }

    public void setConfigKey(String configKey) {
        this.configKey = configKey;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
