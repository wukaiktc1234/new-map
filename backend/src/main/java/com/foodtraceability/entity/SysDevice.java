package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 系统设备实体类
 */
@TableName("sys_devices")
@Schema(description = "系统设备表")
public class SysDevice {
    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "设备ID")
    private Long id;
    @TableField("device_name")
    @Schema(description = "设备名称")
    private String deviceName;
    @TableField("device_type")
    @Schema(description = "设备类型：printer/monitor/scanner/cashbox/display/scale/other")
    private String deviceType;
    @TableField("brand")
    @Schema(description = "品牌")
    private String brand;
    @TableField("model")
    @Schema(description = "型号")
    private String model;
    @TableField("connection_type")
    @Schema(description = "连接方式：usb/network/bluetooth/serial")
    private String connectionType;
    @TableField("connection_config")
    @Schema(description = "连接配置（JSON格式）")
    private String connectionConfig;
    @TableField("store_id")
    @Schema(description = "所属门店ID")
    private Long storeId;
    @TableField("status")
    @Schema(description = "状态：online-在线/offline-离线/error-故障/disabled-禁用")
    private String status;
    @TableField("last_online_time")
    @Schema(description = "最后在线时间")
    private LocalDateTime lastOnlineTime;
    @TableField("supported_functions")
    @Schema(description = "支持功能（JSON格式）")
    private String supportedFunctions;
    @TableField("description")
    @Schema(description = "设备描述")
    private String description;
    @TableField("location")
    @Schema(description = "安装位置")
    private String location;
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    public SysDevice() {
    }

    public Long getId() {
        return this.id;
    }

    public String getDeviceName() {
        return this.deviceName;
    }

    public String getDeviceType() {
        return this.deviceType;
    }

    public String getBrand() {
        return this.brand;
    }

    public String getModel() {
        return this.model;
    }

    public String getConnectionType() {
        return this.connectionType;
    }

    public String getConnectionConfig() {
        return this.connectionConfig;
    }

    public Long getStoreId() {
        return this.storeId;
    }

    public String getStatus() {
        return this.status;
    }

    public LocalDateTime getLastOnlineTime() {
        return this.lastOnlineTime;
    }

    public String getSupportedFunctions() {
        return this.supportedFunctions;
    }

    public String getDescription() {
        return this.description;
    }

    public String getLocation() {
        return this.location;
    }

    public LocalDateTime getCreateTime() {
        return this.createTime;
    }

    public LocalDateTime getUpdateTime() {
        return this.updateTime;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setDeviceName(final String deviceName) {
        this.deviceName = deviceName;
    }

    public void setDeviceType(final String deviceType) {
        this.deviceType = deviceType;
    }

    public void setBrand(final String brand) {
        this.brand = brand;
    }

    public void setModel(final String model) {
        this.model = model;
    }

    public void setConnectionType(final String connectionType) {
        this.connectionType = connectionType;
    }

    public void setConnectionConfig(final String connectionConfig) {
        this.connectionConfig = connectionConfig;
    }

    public void setStoreId(final Long storeId) {
        this.storeId = storeId;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public void setLastOnlineTime(final LocalDateTime lastOnlineTime) {
        this.lastOnlineTime = lastOnlineTime;
    }

    public void setSupportedFunctions(final String supportedFunctions) {
        this.supportedFunctions = supportedFunctions;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public void setLocation(final String location) {
        this.location = location;
    }

    public void setCreateTime(final LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public void setUpdateTime(final LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof SysDevice)) return false;
        final SysDevice other = (SysDevice) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$storeId = this.getStoreId();
        final java.lang.Object other$storeId = other.getStoreId();
        if (this$storeId == null ? other$storeId != null : !this$storeId.equals(other$storeId)) return false;
        final java.lang.Object this$deviceName = this.getDeviceName();
        final java.lang.Object other$deviceName = other.getDeviceName();
        if (this$deviceName == null ? other$deviceName != null : !this$deviceName.equals(other$deviceName)) return false;
        final java.lang.Object this$deviceType = this.getDeviceType();
        final java.lang.Object other$deviceType = other.getDeviceType();
        if (this$deviceType == null ? other$deviceType != null : !this$deviceType.equals(other$deviceType)) return false;
        final java.lang.Object this$brand = this.getBrand();
        final java.lang.Object other$brand = other.getBrand();
        if (this$brand == null ? other$brand != null : !this$brand.equals(other$brand)) return false;
        final java.lang.Object this$model = this.getModel();
        final java.lang.Object other$model = other.getModel();
        if (this$model == null ? other$model != null : !this$model.equals(other$model)) return false;
        final java.lang.Object this$connectionType = this.getConnectionType();
        final java.lang.Object other$connectionType = other.getConnectionType();
        if (this$connectionType == null ? other$connectionType != null : !this$connectionType.equals(other$connectionType)) return false;
        final java.lang.Object this$connectionConfig = this.getConnectionConfig();
        final java.lang.Object other$connectionConfig = other.getConnectionConfig();
        if (this$connectionConfig == null ? other$connectionConfig != null : !this$connectionConfig.equals(other$connectionConfig)) return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
        final java.lang.Object this$lastOnlineTime = this.getLastOnlineTime();
        final java.lang.Object other$lastOnlineTime = other.getLastOnlineTime();
        if (this$lastOnlineTime == null ? other$lastOnlineTime != null : !this$lastOnlineTime.equals(other$lastOnlineTime)) return false;
        final java.lang.Object this$supportedFunctions = this.getSupportedFunctions();
        final java.lang.Object other$supportedFunctions = other.getSupportedFunctions();
        if (this$supportedFunctions == null ? other$supportedFunctions != null : !this$supportedFunctions.equals(other$supportedFunctions)) return false;
        final java.lang.Object this$description = this.getDescription();
        final java.lang.Object other$description = other.getDescription();
        if (this$description == null ? other$description != null : !this$description.equals(other$description)) return false;
        final java.lang.Object this$location = this.getLocation();
        final java.lang.Object other$location = other.getLocation();
        if (this$location == null ? other$location != null : !this$location.equals(other$location)) return false;
        final java.lang.Object this$createTime = this.getCreateTime();
        final java.lang.Object other$createTime = other.getCreateTime();
        if (this$createTime == null ? other$createTime != null : !this$createTime.equals(other$createTime)) return false;
        final java.lang.Object this$updateTime = this.getUpdateTime();
        final java.lang.Object other$updateTime = other.getUpdateTime();
        if (this$updateTime == null ? other$updateTime != null : !this$updateTime.equals(other$updateTime)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof SysDevice;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $storeId = this.getStoreId();
        result = result * PRIME + ($storeId == null ? 43 : $storeId.hashCode());
        final java.lang.Object $deviceName = this.getDeviceName();
        result = result * PRIME + ($deviceName == null ? 43 : $deviceName.hashCode());
        final java.lang.Object $deviceType = this.getDeviceType();
        result = result * PRIME + ($deviceType == null ? 43 : $deviceType.hashCode());
        final java.lang.Object $brand = this.getBrand();
        result = result * PRIME + ($brand == null ? 43 : $brand.hashCode());
        final java.lang.Object $model = this.getModel();
        result = result * PRIME + ($model == null ? 43 : $model.hashCode());
        final java.lang.Object $connectionType = this.getConnectionType();
        result = result * PRIME + ($connectionType == null ? 43 : $connectionType.hashCode());
        final java.lang.Object $connectionConfig = this.getConnectionConfig();
        result = result * PRIME + ($connectionConfig == null ? 43 : $connectionConfig.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $lastOnlineTime = this.getLastOnlineTime();
        result = result * PRIME + ($lastOnlineTime == null ? 43 : $lastOnlineTime.hashCode());
        final java.lang.Object $supportedFunctions = this.getSupportedFunctions();
        result = result * PRIME + ($supportedFunctions == null ? 43 : $supportedFunctions.hashCode());
        final java.lang.Object $description = this.getDescription();
        result = result * PRIME + ($description == null ? 43 : $description.hashCode());
        final java.lang.Object $location = this.getLocation();
        result = result * PRIME + ($location == null ? 43 : $location.hashCode());
        final java.lang.Object $createTime = this.getCreateTime();
        result = result * PRIME + ($createTime == null ? 43 : $createTime.hashCode());
        final java.lang.Object $updateTime = this.getUpdateTime();
        result = result * PRIME + ($updateTime == null ? 43 : $updateTime.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "SysDevice(id=" + this.getId() + ", deviceName=" + this.getDeviceName() + ", deviceType=" + this.getDeviceType() + ", brand=" + this.getBrand() + ", model=" + this.getModel() + ", connectionType=" + this.getConnectionType() + ", connectionConfig=" + this.getConnectionConfig() + ", storeId=" + this.getStoreId() + ", status=" + this.getStatus() + ", lastOnlineTime=" + this.getLastOnlineTime() + ", supportedFunctions=" + this.getSupportedFunctions() + ", description=" + this.getDescription() + ", location=" + this.getLocation() + ", createTime=" + this.getCreateTime() + ", updateTime=" + this.getUpdateTime() + ")";
    }
}
