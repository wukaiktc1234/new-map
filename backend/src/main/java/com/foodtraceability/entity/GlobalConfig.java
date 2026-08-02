package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableId;
import com.foodtraceability.common.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 全局配置实体
 */
@TableName("global_config")
@Schema(description = "全局配置表")
public class GlobalConfig extends BaseEntity {

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "配置ID")
    private Long id;

    @TableField("config_key")
    @Schema(description = "配置键")
    private String configKey;

    @TableField("config_value")
    @Schema(description = "配置值")
    private String configValue;

    @TableField("config_desc")
    @Schema(description = "配置描述")
    private String configDesc;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getConfigKey() {
        return configKey;
    }

    public void setConfigKey(String configKey) {
        this.configKey = configKey;
    }

    public String getConfigValue() {
        return configValue;
    }

    public void setConfigValue(String configValue) {
        this.configValue = configValue;
    }

    public String getConfigDesc() {
        return configDesc;
    }

    public void setConfigDesc(String configDesc) {
        this.configDesc = configDesc;
    }
}
