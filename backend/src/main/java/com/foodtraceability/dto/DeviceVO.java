package com.foodtraceability.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 设备VO
 */
@Schema(description = "设备视图对象")
public class DeviceVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "设备ID")
    private Long deviceId;

    @Schema(description = "设备编号")
    private String deviceCode;

    @Schema(description = "设备名称")
    private String deviceName;

    @Schema(description = "设备类型")
    private Integer deviceType;

    @Schema(description = "设备类型名称")
    private String deviceTypeName;

    @Schema(description = "型号")
    private String deviceModel;

    @Schema(description = "厂商")
    private String manufacturer;

    @Schema(description = "序列号")
    private String serialNo;

    @Schema(description = "连接类型")
    private Integer connectionType;

    @Schema(description = "连接类型名称")
    private String connectionTypeName;

    @Schema(description = "安装位置")
    private String location;

    @Schema(description = "所属门店ID")
    private Long storeId;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "状态名称")
    private String statusName;

    @Schema(description = "最后心跳时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastHeartbeatTime;

    @Schema(description = "固件版本")
    private String firmwareVersion;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    // ==================== Getter & Setter 方法 ====================

    public Long getDeviceId() { return deviceId; }
    public void setDeviceId(Long deviceId) { this.deviceId = deviceId; }

    public String getDeviceCode() { return deviceCode; }
    public void setDeviceCode(String deviceCode) { this.deviceCode = deviceCode; }

    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }

    public Integer getDeviceType() { return deviceType; }
    public void setDeviceType(Integer deviceType) { this.deviceType = deviceType; }

    public String getDeviceTypeName() { return deviceTypeName; }
    public void setDeviceTypeName(String deviceTypeName) { this.deviceTypeName = deviceTypeName; }

    public String getDeviceModel() { return deviceModel; }
    public void setDeviceModel(String deviceModel) { this.deviceModel = deviceModel; }

    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }

    public String getSerialNo() { return serialNo; }
    public void setSerialNo(String serialNo) { this.serialNo = serialNo; }

    public Integer getConnectionType() { return connectionType; }
    public void setConnectionType(Integer connectionType) { this.connectionType = connectionType; }

    public String getConnectionTypeName() { return connectionTypeName; }
    public void setConnectionTypeName(String connectionTypeName) { this.connectionTypeName = connectionTypeName; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public String getStatusName() { return statusName; }
    public void setStatusName(String statusName) { this.statusName = statusName; }

    public LocalDateTime getLastHeartbeatTime() { return lastHeartbeatTime; }
    public void setLastHeartbeatTime(LocalDateTime lastHeartbeatTime) { this.lastHeartbeatTime = lastHeartbeatTime; }

    public String getFirmwareVersion() { return firmwareVersion; }
    public void setFirmwareVersion(String firmwareVersion) { this.firmwareVersion = firmwareVersion; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
