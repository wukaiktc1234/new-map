package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "会话设备响应")
public class SessionDeviceResponse {
    
    @Schema(description = "会话ID")
    private String sessionId;
    
    @Schema(description = "设备信息")
    private String deviceInfo;
    
    @Schema(description = "IP地址")
    private String ipAddress;
    
    @Schema(description = "登录时间")
    private LocalDateTime loginTime;
    
    @Schema(description = "最后活动时间")
    private LocalDateTime lastActivityTime;
    
    @Schema(description = "是否为当前设备")
    private Boolean isCurrentDevice;
    
    @Schema(description = "过期时间")
    private LocalDateTime expiresAt;

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getDeviceInfo() {
        return deviceInfo;
    }

    public void setDeviceInfo(String deviceInfo) {
        this.deviceInfo = deviceInfo;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public LocalDateTime getLoginTime() {
        return loginTime;
    }

    public void setLoginTime(LocalDateTime loginTime) {
        this.loginTime = loginTime;
    }

    public LocalDateTime getLastActivityTime() {
        return lastActivityTime;
    }

    public void setLastActivityTime(LocalDateTime lastActivityTime) {
        this.lastActivityTime = lastActivityTime;
    }

    public Boolean getIsCurrentDevice() {
        return isCurrentDevice;
    }

    public void setIsCurrentDevice(Boolean isCurrentDevice) {
        this.isCurrentDevice = isCurrentDevice;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
}
