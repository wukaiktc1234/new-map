package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 驱动配置实体类
 * 对应数据库表 device_driver_configs
 */
@TableName("device_driver_configs")
@Schema(description = "驱动配置实体")
public class DeviceDriverConfig {

    /** 配置ID */
    @TableId(value = "config_id", type = IdType.AUTO)
    @Schema(description = "配置ID")
    private Long configId;

    /** 驱动类型：printer_tspl/printer_wsd/scanner_generic/scale_xiaomi */
    @TableField("driver_type")
    @Schema(description = "驱动类型")
    private String driverType;

    /** 驱动类全限定名 */
    @TableField("driver_class_name")
    @Schema(description = "驱动类全限定名")
    private String driverClassName;

    /** 默认参数JSON */
    @TableField("default_params")
    @Schema(description = "默认参数")
    private String defaultParams;

    /** 支持的命令列表JSON */
    @TableField("supported_commands")
    @Schema(description = "支持的命令列表")
    private String supportedCommands;

    /** 描述 */
    @TableField("description")
    @Schema(description = "描述")
    private String description;

    /** 是否启用 */
    @TableField("is_enabled")
    @Schema(description = "是否启用")
    private Boolean isEnabled;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    /** 逻辑删除标记 */
    @TableLogic
    @TableField("deleted")
    @Schema(description = "逻辑删除标记")
    private Integer deleted;

    // ==================== Getter & Setter 方法 ====================

    public Long getConfigId() {
        return configId;
    }

    public void setConfigId(Long configId) {
        this.configId = configId;
    }

    public String getDriverType() {
        return driverType;
    }

    public void setDriverType(String driverType) {
        this.driverType = driverType;
    }

    public String getDriverClassName() {
        return driverClassName;
    }

    public void setDriverClassName(String driverClassName) {
        this.driverClassName = driverClassName;
    }

    public String getDefaultParams() {
        return defaultParams;
    }

    public void setDefaultParams(String defaultParams) {
        this.defaultParams = defaultParams;
    }

    public String getSupportedCommands() {
        return supportedCommands;
    }

    public void setSupportedCommands(String supportedCommands) {
        this.supportedCommands = supportedCommands;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getIsEnabled() {
        return isEnabled;
    }

    public void setIsEnabled(Boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }
}
