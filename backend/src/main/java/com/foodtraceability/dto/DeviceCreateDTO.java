package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;

/**
 * 设备创建DTO
 */
@Schema(description = "设备创建请求")
public class DeviceCreateDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 设备编号 */
    @NotBlank(message = "设备编号不能为空")
    @Size(max = 50, message = "设备编号长度不能超过50个字符")
    @Schema(description = "设备编号", required = true)
    private String deviceCode;

    /** 设备名称 */
    @NotBlank(message = "设备名称不能为空")
    @Size(max = 100, message = "设备名称长度不能超过100个字符")
    @Schema(description = "设备名称", required = true)
    private String deviceName;

    /** 设备类型：1打印机 2扫码枪 3称重秤 4取餐柜 5其他 */
    @NotNull(message = "设备类型不能为空")
    @Min(value = 1, message = "设备类型无效")
    @Max(value = 5, message = "设备类型无效")
    @Schema(description = "设备类型", required = true)
    private Integer deviceType;

    /** 型号 */
    @Size(max = 100, message = "型号长度不能超过100个字符")
    @Schema(description = "型号")
    private String deviceModel;

    /** 厂商 */
    @Size(max = 100, message = "厂商长度不能超过100个字符")
    @Schema(description = "厂商")
    private String manufacturer;

    /** 序列号 */
    @Size(max = 100, message = "序列号长度不能超过100个字符")
    @Schema(description = "序列号")
    private String serialNo;

    /** 连接类型：1USB 2串口 3网络 4蓝牙 */
    @Min(value = 1, message = "连接类型无效")
    @Max(value = 4, message = "连接类型无效")
    @Schema(description = "连接类型")
    private Integer connectionType;

    /** 连接参数JSON */
    @Size(max = 2000, message = "连接参数长度不能超过2000个字符")
    @Schema(description = "连接参数JSON")
    private String connectionParams;

    /** 安装位置 */
    @Size(max = 200, message = "安装位置长度不能超过200个字符")
    @Schema(description = "安装位置")
    private String location;

    /** 所属门店ID */
    @Schema(description = "所属门店ID")
    private Long storeId;

    /** 固件版本 */
    @Size(max = 50, message = "固件版本长度不能超过50个字符")
    @Schema(description = "固件版本")
    private String firmwareVersion;

    /** 驱动类全限定名 */
    @Size(max = 200, message = "驱动类全限定名长度不能超过200个字符")
    @Schema(description = "驱动类全限定名")
    private String driverClass;

    /** 设备配置JSON */
    @Size(max = 2000, message = "设备配置长度不能超过2000个字符")
    @Schema(description = "设备配置JSON")
    private String configJson;

    /** 备注 */
    @Size(max = 500, message = "备注长度不能超过500个字符")
    @Schema(description = "备注")
    private String remark;

    // ==================== Getter & Setter 方法 ====================

    public String getDeviceCode() { return deviceCode; }
    public void setDeviceCode(String deviceCode) { this.deviceCode = deviceCode; }

    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }

    public Integer getDeviceType() { return deviceType; }
    public void setDeviceType(Integer deviceType) { this.deviceType = deviceType; }

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
