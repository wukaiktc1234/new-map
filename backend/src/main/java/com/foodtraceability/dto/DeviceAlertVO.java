package com.foodtraceability.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 设备告警VO
 */
@Schema(description = "设备告警视图对象")
public class DeviceAlertVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "告警ID")
    private Long alertId;

    @Schema(description = "设备ID")
    private Long deviceId;

    @Schema(description = "设备名称")
    private String deviceName;

    @Schema(description = "设备编号")
    private String deviceCode;

    @Schema(description = "告警类型")
    private Integer alertType;

    @Schema(description = "告警类型名称")
    private String alertTypeName;

    @Schema(description = "告警级别")
    private Integer alertLevel;

    @Schema(description = "告警级别名称")
    private String alertLevelName;

    @Schema(description = "告警消息")
    private String alertMessage;

    @Schema(description = "是否已处理")
    private Boolean isHandled;

    @Schema(description = "处理时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime handleTime;

    @Schema(description = "处理结果")
    private String handleResult;

    @Schema(description = "处理人ID")
    private Long handleUserId;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    // ==================== Getter & Setter 方法 ====================

    public Long getAlertId() { return alertId; }
    public void setAlertId(Long alertId) { this.alertId = alertId; }

    public Long getDeviceId() { return deviceId; }
    public void setDeviceId(Long deviceId) { this.deviceId = deviceId; }

    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }

    public String getDeviceCode() { return deviceCode; }
    public void setDeviceCode(String deviceCode) { this.deviceCode = deviceCode; }

    public Integer getAlertType() { return alertType; }
    public void setAlertType(Integer alertType) { this.alertType = alertType; }

    public String getAlertTypeName() { return alertTypeName; }
    public void setAlertTypeName(String alertTypeName) { this.alertTypeName = alertTypeName; }

    public Integer getAlertLevel() { return alertLevel; }
    public void setAlertLevel(Integer alertLevel) { this.alertLevel = alertLevel; }

    public String getAlertLevelName() { return alertLevelName; }
    public void setAlertLevelName(String alertLevelName) { this.alertLevelName = alertLevelName; }

    public String getAlertMessage() { return alertMessage; }
    public void setAlertMessage(String alertMessage) { this.alertMessage = alertMessage; }

    public Boolean getIsHandled() { return isHandled; }
    public void setIsHandled(Boolean isHandled) { this.isHandled = isHandled; }

    public LocalDateTime getHandleTime() { return handleTime; }
    public void setHandleTime(LocalDateTime handleTime) { this.handleTime = handleTime; }

    public String getHandleResult() { return handleResult; }
    public void setHandleResult(String handleResult) { this.handleResult = handleResult; }

    public Long getHandleUserId() { return handleUserId; }
    public void setHandleUserId(Long handleUserId) { this.handleUserId = handleUserId; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
