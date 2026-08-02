package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;

/**
 * 设备查询DTO
 */
@Schema(description = "设备查询条件")
public class DeviceQueryDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 设备编号（模糊查询） */
    @Schema(description = "设备编号")
    private String deviceCode;

    /** 设备名称（模糊查询） */
    @Schema(description = "设备名称")
    private String deviceName;

    /** 设备类型 */
    @Schema(description = "设备类型")
    private Integer deviceType;

    /** 所属门店ID */
    @Schema(description = "所属门店ID")
    private Long storeId;

    /** 状态：0离线 1在线 2故障 3维护中 */
    @Schema(description = "状态")
    private Integer status;

    /** 连接类型 */
    @Schema(description = "连接类型")
    private Integer connectionType;

    // ==================== Getter & Setter 方法 ====================

    public String getDeviceCode() { return deviceCode; }
    public void setDeviceCode(String deviceCode) { this.deviceCode = deviceCode; }

    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }

    public Integer getDeviceType() { return deviceType; }
    public void setDeviceType(Integer deviceType) { this.deviceType = deviceType; }

    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public Integer getConnectionType() { return connectionType; }
    public void setConnectionType(Integer connectionType) { this.connectionType = connectionType; }
}
