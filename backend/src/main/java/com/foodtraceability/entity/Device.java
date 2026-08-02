package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 设备实体类
 * 对应数据库表 devices
 */
@TableName(value = "devices", autoResultMap = true)
@Schema(description = "设备实体")
public class Device {

    /** 设备ID */
    @TableId(value = "device_id", type = IdType.AUTO)
    @Schema(description = "设备ID")
    private Long deviceId;

    /** 设备编号（数据库列 device_id_str） */
    @TableField("device_id_str")
    @Schema(description = "设备编号")
    private String deviceCode;

    /** 设备名称 */
    @TableField("device_name")
    @Schema(description = "设备名称")
    private String deviceName;

    /** 设备类型：1打印机 2扫码枪 3称重秤 4取餐柜 5其他 */
    @TableField("device_type")
    @Schema(description = "设备类型")
    private Integer deviceType;

    /** 型号 */
    @TableField("device_model")
    @Schema(description = "型号")
    private String deviceModel;

    /** 厂商（devices 表无 manufacturer 列，此字段不映射数据库） */
    @TableField(exist = false)
    @Schema(description = "厂商")
    private String manufacturer;

    /** 序列号（数据库列 serial_number） */
    @TableField("serial_number")
    @Schema(description = "序列号")
    private String serialNo;

    /** 连接类型：1USB 2串口 3网络 4蓝牙 */
    @TableField("connection_type")
    @Schema(description = "连接类型")
    private Integer connectionType;

    /** 连接参数JSON（数据库列 connection_config） */
    @TableField("connection_config")
    @Schema(description = "连接参数")
    private String connectionParams;

    /** 安装位置（devices 表无 location 列，此字段不映射数据库） */
    @TableField(exist = false)
    @Schema(description = "安装位置")
    private String location;

    /** 所属门店ID */
    @TableField("store_id")
    @Schema(description = "所属门店ID")
    private Long storeId;

    /**
     * 状态：0离线 1在线 2故障 3维护中
     * 数据库列为 varchar，Java 侧以 Integer 表示，通过 setter 转换为 String 存储。
     */
    @TableField(value = "status")
    @Schema(description = "状态")
    private String status;

    /** 最后心跳时间（devices 表无 last_heartbeat_time 列，此字段不映射数据库） */
    @TableField(exist = false)
    @Schema(description = "最后心跳时间")
    private LocalDateTime lastHeartbeatTime;

    /** 固件版本（devices 表无 firmware_version 列，此字段不映射数据库） */
    @TableField(exist = false)
    @Schema(description = "固件版本")
    private String firmwareVersion;

    /** 驱动类全限定名（devices 表无 driver_class 列，此字段不映射数据库） */
    @TableField(exist = false)
    @Schema(description = "驱动类名")
    private String driverClass;

    /** 设备配置JSON（devices 表无 config_json 列，此字段不映射数据库） */
    @TableField(exist = false)
    @Schema(description = "设备配置")
    private String configJson;

    /** 最后在线时间 */
    @TableField("last_online_time")
    @Schema(description = "最后在线时间")
    private LocalDateTime lastOnlineTime;

    /** 备注（devices 表无 remark 列，此字段不映射数据库） */
    @TableField(exist = false)
    @Schema(description = "备注")
    private String remark;

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

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
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

    public Integer getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(Integer deviceType) {
        this.deviceType = deviceType;
    }

    public String getDeviceModel() {
        return deviceModel;
    }

    public void setDeviceModel(String deviceModel) {
        this.deviceModel = deviceModel;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getSerialNo() {
        return serialNo;
    }

    public void setSerialNo(String serialNo) {
        this.serialNo = serialNo;
    }

    public Integer getConnectionType() {
        return connectionType;
    }

    public void setConnectionType(Integer connectionType) {
        this.connectionType = connectionType;
    }

    public String getConnectionParams() {
        return connectionParams;
    }

    public void setConnectionParams(String connectionParams) {
        this.connectionParams = connectionParams;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Long getStoreId() {
        return storeId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    /**
     * 获取状态（返回 Integer 兼容现有业务代码）
     * 数据库存储为 varchar，此处解析为 Integer。
     */
    public Integer getStatus() {
        if (status == null) {
            return null;
        }
        try {
            return Integer.parseInt(status);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * 设置状态（Integer → String 转换存储）
     */
    public void setStatus(Integer status) {
        this.status = status != null ? String.valueOf(status) : null;
    }

    /**
     * 设置状态（int → String 转换存储）
     */
    public void setStatus(int status) {
        this.status = String.valueOf(status);
    }

    /**
     * 设置状态（String 直接存储，供 MyBatis 读取数据库时调用）
     */
    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getLastHeartbeatTime() {
        return lastHeartbeatTime;
    }

    public void setLastHeartbeatTime(LocalDateTime lastHeartbeatTime) {
        this.lastHeartbeatTime = lastHeartbeatTime;
    }

    public String getFirmwareVersion() {
        return firmwareVersion;
    }

    public void setFirmwareVersion(String firmwareVersion) {
        this.firmwareVersion = firmwareVersion;
    }

    public String getDriverClass() {
        return driverClass;
    }

    public void setDriverClass(String driverClass) {
        this.driverClass = driverClass;
    }

    public String getConfigJson() {
        return configJson;
    }

    public void setConfigJson(String configJson) {
        this.configJson = configJson;
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

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    // ==================== 兼容性方法 ====================

    public Long getId() {
        return deviceId;
    }

    public void setId(Long id) {
        this.deviceId = id;
    }

    public String getModel() {
        return deviceModel;
    }

    public void setModel(String model) {
        this.deviceModel = model;
    }

    public String getConnectionConfig() {
        return connectionParams;
    }

    public void setConnectionConfig(String connectionConfig) {
        this.connectionParams = connectionConfig;
    }

    public LocalDateTime getLastOnlineTime() {
        return lastOnlineTime;
    }

    public void setLastOnlineTime(LocalDateTime lastOnlineTime) {
        this.lastOnlineTime = lastOnlineTime;
    }

    // ==================== enabled 兼容性方法 ====================

    /**
     * 兼容性方法：启用状态映射到status（int类型）
     * 1=启用(在线), 0=禁用(离线)
     */
    public void setEnabled(int enabled) {
        this.status = String.valueOf(enabled);
    }

    /**
     * 兼容性方法：获取启用状态
     */
    public int getEnabled() {
        if (status == null) {
            return 0;
        }
        try {
            return Integer.parseInt(status);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * 兼容性方法：启用状态（Boolean类型）
     */
    public void setEnabled(Boolean enabled) {
        this.status = (enabled != null && enabled) ? "1" : "0";
    }
}
