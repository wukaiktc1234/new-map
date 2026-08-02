package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 设备更新DTO
 */
@Schema(description = "设备更新请求")
public class DeviceUpdateDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 设备名称 */
    @Schema(description = "设备名称")
    private String deviceName;

    /** 型号 */
    @Schema(description = "型号")
    private String deviceModel;

    /** 厂商 */
    @Schema(description = "厂商")
    private String manufacturer;

    /** 序列号 */
    @Schema(description = "序列号")
    private String serialNo;

    /** 连接类型：1USB 2串口 3网络 4蓝牙 */
    @Schema(description = "连接类型")
    private Integer connectionType;

    /** 连接参数JSON */
    @Schema(description = "连接参数JSON")
    private String connectionParams;

    /** 安装位置 */
    @Schema(description = "安装位置")
    private String location;

    /** 所属门店ID */
    @Schema(description = "所属门店ID")
    private Long storeId;

    /** 固件版本 */
    @Schema(description = "固件版本")
    private String firmwareVersion;

    /** 驱动类全限定名 */
    @Schema(description = "驱动类全限定名")
    private String driverClass;

    /** 设备配置JSON */
    @Schema(description = "设备配置JSON")
    private String configJson;

    /** 备注 */
    @Schema(description = "备注")
    private String remark;

    // ==================== Getter & Setter 方法 ====================

    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }

    public String getDeviceModel() { return deviceModel; }
    public void setDeviceModel(String deviceModel) { this.deviceModel = deviceModel; }

    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }

    public String getSerialNo() { return serialNo; }
    public void setSerialNo(String serialNo) { this.serialNo = serialNo; }

    public Integer getConnectionType() { return connectionType; }
    public void setConnectionType(Integer connectionType) { this.connectionType = connectionType; }

    public String getConnectionParams() { return connectionParams; }
    public void setConnectionParams(String connectionParams) { this.connectionParams = connectionParams; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }

    public String getFirmwareVersion() { return firmwareVersion; }
    public void setFirmwareVersion(String firmwareVersion) { this.firmwareVersion = firmwareVersion; }

    public String getDriverClass() { return driverClass; }
    public void setDriverClass(String driverClass) { this.driverClass = driverClass; }

    public String getConfigJson() { return configJson; }
    public void setConfigJson(String configJson) { this.configJson = configJson; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
