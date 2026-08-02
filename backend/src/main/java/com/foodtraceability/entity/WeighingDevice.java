package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

/**
 * 称重设备实体类
 * @author example
 * @since 2026-01-08
 */
@TableName("weighing_device")
public class WeighingDevice {
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 设备编号
     */
    @TableField("device_code")
    private String deviceCode;
    /**
     * 设备名称
     */
    @TableField("device_name")
    private String deviceName;
    /**
     * 设备类型：ELECTRONIC_SCALE（电子秤）、PLATFORM_SCALE（平台秤）、COUNTING_SCALE（计数秤）
     */
    @TableField("device_type")
    private String deviceType;
    /**
     * 设备IP地址
     */
    @TableField("device_ip")
    private String deviceIp;
    /**
     * 设备端口
     */
    @TableField("device_port")
    private Integer devicePort;
    /**
     * 设备状态：ONLINE（在线）、OFFLINE（离线）、MAINTENANCE（维护中）
     */
    @TableField("status")
    private String status;
    /**
     * 连接方式：TCP（网络）、SERIAL（串口）、USB（USB）
     */
    @TableField("connection_type")
    private String connectionType;
    /**
     * 串口配置（JSON格式）
     */
    @TableField("serial_config")
    private String serialConfig;
    /**
     * 设备精度（克）
     */
    @TableField("precision")
    private Double precision;
    /**
     * 最大称量（克）
     */
    @TableField("max_weight")
    private Double maxWeight;
    /**
     * 设备位置
     */
    @TableField("location")
    private String location;
    /**
     * 备注
     */
    @TableField("remark")
    private String remark;
    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    /**
     * 创建人
     */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private String createBy;
    /**
     * 更新人
     */
    @TableField(value = "update_by", fill = FieldFill.INSERT_UPDATE)
    private String updateBy;
    /**
     * 逻辑删除标志
     */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    // Getter and Setter methods
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDeviceCode() {
        return deviceCode;
    }

    public void setDeviceCode(String deviceCode) {
        this.deviceCode = deviceCode;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public String getDeviceIp() {
        return deviceIp;
    }

    public void setDeviceIp(String deviceIp) {
        this.deviceIp = deviceIp;
    }

    public Integer getDevicePort() {
        return devicePort;
    }

    public void setDevicePort(Integer devicePort) {
        this.devicePort = devicePort;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getConnectionType() {
        return connectionType;
    }

    public void setConnectionType(String connectionType) {
        this.connectionType = connectionType;
    }

    public String getSerialConfig() {
        return serialConfig;
    }

    public void setSerialConfig(String serialConfig) {
        this.serialConfig = serialConfig;
    }

    public Double getPrecision() {
        return precision;
    }

    public void setPrecision(Double precision) {
        this.precision = precision;
    }

    public Double getMaxWeight() {
        return maxWeight;
    }

    public void setMaxWeight(Double maxWeight) {
        this.maxWeight = maxWeight;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
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

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    public WeighingDevice() {
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "WeighingDevice(id=" + this.getId() + ", deviceCode=" + this.getDeviceCode() + ", deviceName=" + this.getDeviceName() + ", deviceType=" + this.getDeviceType() + ", deviceIp=" + this.getDeviceIp() + ", devicePort=" + this.getDevicePort() + ", status=" + this.getStatus() + ", connectionType=" + this.getConnectionType() + ", serialConfig=" + this.getSerialConfig() + ", precision=" + this.getPrecision() + ", maxWeight=" + this.getMaxWeight() + ", location=" + this.getLocation() + ", remark=" + this.getRemark() + ", createTime=" + this.getCreateTime() + ", updateTime=" + this.getUpdateTime() + ", createBy=" + this.getCreateBy() + ", updateBy=" + this.getUpdateBy() + ", deleted=" + this.getDeleted() + ")";
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof WeighingDevice)) return false;
        final WeighingDevice other = (WeighingDevice) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$devicePort = this.getDevicePort();
        final java.lang.Object other$devicePort = other.getDevicePort();
        if (this$devicePort == null ? other$devicePort != null : !this$devicePort.equals(other$devicePort)) return false;
        final java.lang.Object this$precision = this.getPrecision();
        final java.lang.Object other$precision = other.getPrecision();
        if (this$precision == null ? other$precision != null : !this$precision.equals(other$precision)) return false;
        final java.lang.Object this$maxWeight = this.getMaxWeight();
        final java.lang.Object other$maxWeight = other.getMaxWeight();
        if (this$maxWeight == null ? other$maxWeight != null : !this$maxWeight.equals(other$maxWeight)) return false;
        final java.lang.Object this$deleted = this.getDeleted();
        final java.lang.Object other$deleted = other.getDeleted();
        if (this$deleted == null ? other$deleted != null : !this$deleted.equals(other$deleted)) return false;
        final java.lang.Object this$deviceCode = this.getDeviceCode();
        final java.lang.Object other$deviceCode = other.getDeviceCode();
        if (this$deviceCode == null ? other$deviceCode != null : !this$deviceCode.equals(other$deviceCode)) return false;
        final java.lang.Object this$deviceName = this.getDeviceName();
        final java.lang.Object other$deviceName = other.getDeviceName();
        if (this$deviceName == null ? other$deviceName != null : !this$deviceName.equals(other$deviceName)) return false;
        final java.lang.Object this$deviceType = this.getDeviceType();
        final java.lang.Object other$deviceType = other.getDeviceType();
        if (this$deviceType == null ? other$deviceType != null : !this$deviceType.equals(other$deviceType)) return false;
        final java.lang.Object this$deviceIp = this.getDeviceIp();
        final java.lang.Object other$deviceIp = other.getDeviceIp();
        if (this$deviceIp == null ? other$deviceIp != null : !this$deviceIp.equals(other$deviceIp)) return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
        final java.lang.Object this$connectionType = this.getConnectionType();
        final java.lang.Object other$connectionType = other.getConnectionType();
        if (this$connectionType == null ? other$connectionType != null : !this$connectionType.equals(other$connectionType)) return false;
        final java.lang.Object this$serialConfig = this.getSerialConfig();
        final java.lang.Object other$serialConfig = other.getSerialConfig();
        if (this$serialConfig == null ? other$serialConfig != null : !this$serialConfig.equals(other$serialConfig)) return false;
        final java.lang.Object this$location = this.getLocation();
        final java.lang.Object other$location = other.getLocation();
        if (this$location == null ? other$location != null : !this$location.equals(other$location)) return false;
        final java.lang.Object this$remark = this.getRemark();
        final java.lang.Object other$remark = other.getRemark();
        if (this$remark == null ? other$remark != null : !this$remark.equals(other$remark)) return false;
        final java.lang.Object this$createTime = this.getCreateTime();
        final java.lang.Object other$createTime = other.getCreateTime();
        if (this$createTime == null ? other$createTime != null : !this$createTime.equals(other$createTime)) return false;
        final java.lang.Object this$updateTime = this.getUpdateTime();
        final java.lang.Object other$updateTime = other.getUpdateTime();
        if (this$updateTime == null ? other$updateTime != null : !this$updateTime.equals(other$updateTime)) return false;
        final java.lang.Object this$createBy = this.getCreateBy();
        final java.lang.Object other$createBy = other.getCreateBy();
        if (this$createBy == null ? other$createBy != null : !this$createBy.equals(other$createBy)) return false;
        final java.lang.Object this$updateBy = this.getUpdateBy();
        final java.lang.Object other$updateBy = other.getUpdateBy();
        if (this$updateBy == null ? other$updateBy != null : !this$updateBy.equals(other$updateBy)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof WeighingDevice;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $devicePort = this.getDevicePort();
        result = result * PRIME + ($devicePort == null ? 43 : $devicePort.hashCode());
        final java.lang.Object $precision = this.getPrecision();
        result = result * PRIME + ($precision == null ? 43 : $precision.hashCode());
        final java.lang.Object $maxWeight = this.getMaxWeight();
        result = result * PRIME + ($maxWeight == null ? 43 : $maxWeight.hashCode());
        final java.lang.Object $deleted = this.getDeleted();
        result = result * PRIME + ($deleted == null ? 43 : $deleted.hashCode());
        final java.lang.Object $deviceCode = this.getDeviceCode();
        result = result * PRIME + ($deviceCode == null ? 43 : $deviceCode.hashCode());
        final java.lang.Object $deviceName = this.getDeviceName();
        result = result * PRIME + ($deviceName == null ? 43 : $deviceName.hashCode());
        final java.lang.Object $deviceType = this.getDeviceType();
        result = result * PRIME + ($deviceType == null ? 43 : $deviceType.hashCode());
        final java.lang.Object $deviceIp = this.getDeviceIp();
        result = result * PRIME + ($deviceIp == null ? 43 : $deviceIp.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $connectionType = this.getConnectionType();
        result = result * PRIME + ($connectionType == null ? 43 : $connectionType.hashCode());
        final java.lang.Object $serialConfig = this.getSerialConfig();
        result = result * PRIME + ($serialConfig == null ? 43 : $serialConfig.hashCode());
        final java.lang.Object $location = this.getLocation();
        result = result * PRIME + ($location == null ? 43 : $location.hashCode());
        final java.lang.Object $remark = this.getRemark();
        result = result * PRIME + ($remark == null ? 43 : $remark.hashCode());
        final java.lang.Object $createTime = this.getCreateTime();
        result = result * PRIME + ($createTime == null ? 43 : $createTime.hashCode());
        final java.lang.Object $updateTime = this.getUpdateTime();
        result = result * PRIME + ($updateTime == null ? 43 : $updateTime.hashCode());
        final java.lang.Object $createBy = this.getCreateBy();
        result = result * PRIME + ($createBy == null ? 43 : $createBy.hashCode());
        final java.lang.Object $updateBy = this.getUpdateBy();
        result = result * PRIME + ($updateBy == null ? 43 : $updateBy.hashCode());
        return result;
    }
}
