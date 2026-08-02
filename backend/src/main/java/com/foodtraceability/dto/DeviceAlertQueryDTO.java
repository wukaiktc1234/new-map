package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;

/**
 * 设备告警查询DTO
 */
@Schema(description = "设备告警查询条件")
public class DeviceAlertQueryDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 设备ID */
    @Schema(description = "设备ID")
    private Long deviceId;

    /** 告警类型：1离线超时 2纸张缺 3碳带缺 4故障 5维护提醒 */
    @Schema(description = "告警类型")
    private Integer alertType;

    /** 告警级别：1信息 2警告 3严重 4紧急 */
    @Schema(description = "告警级别")
    private Integer alertLevel;

    /** 是否已处理 */
    @Schema(description = "是否已处理")
    private Boolean isHandled;

    /** 告警状态：0未处理 1处理中 2已解决 3已忽略 */
    @Schema(description = "告警状态：0未处理 1处理中 2已解决 3已忽略")
    private Integer alertStatus;

    /** 开始时间 */
    @Schema(description = "开始时间")
    private String startTime;

    /** 结束时间 */
    @Schema(description = "结束时间")
    private String endTime;

    // ==================== Getter & Setter 方法 ====================

    public Long getDeviceId() { return deviceId; }
    public void setDeviceId(Long deviceId) { this.deviceId = deviceId; }

    public Integer getAlertType() { return alertType; }
    public void setAlertType(Integer alertType) { this.alertType = alertType; }

    public Integer getAlertLevel() { return alertLevel; }
    public void setAlertLevel(Integer alertLevel) { this.alertLevel = alertLevel; }

    public Boolean getIsHandled() { return isHandled; }
    public void setIsHandled(Boolean isHandled) { this.isHandled = isHandled; }

    public Integer getAlertStatus() { return alertStatus; }
    public void setAlertStatus(Integer alertStatus) { this.alertStatus = alertStatus; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
}
