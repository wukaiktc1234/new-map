package com.foodtraceability.dto;

import jakarta.validation.constraints.NotBlank;

import java.io.Serializable;

/**
 * 库存设置查询DTO
 * 用于查询库存设置项
 */
public class InventorySettingQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 设置键名（模糊查询） */
    private String settingKey;

    /** 值类型筛选 */
    private String settingType;

    public String getSettingKey() {
        return settingKey;
    }

    public void setSettingKey(String settingKey) {
        this.settingKey = settingKey;
    }

    public String getSettingType() {
        return settingType;
    }

    public void setSettingType(String settingType) {
        this.settingType = settingType;
    }
}
