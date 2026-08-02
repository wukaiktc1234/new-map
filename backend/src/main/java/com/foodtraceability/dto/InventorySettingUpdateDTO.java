package com.foodtraceability.dto;

import jakarta.validation.constraints.NotBlank;

import java.io.Serializable;

/**
 * 库存设置更新DTO
 * 用于更新单个设置项的值
 */
public class InventorySettingUpdateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 设置键名 */
    @NotBlank(message = "设置键名不能为空")
    private String settingKey;

    /** 设置值 */
    @NotBlank(message = "设置值不能为空")
    private String settingValue;

    /** 设置描述（可选） */
    private String description;

    public String getSettingKey() {
        return settingKey;
    }

    public void setSettingKey(String settingKey) {
        this.settingKey = settingKey;
    }

    public String getSettingValue() {
        return settingValue;
    }

    public void setSettingValue(String settingValue) {
        this.settingValue = settingValue;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
