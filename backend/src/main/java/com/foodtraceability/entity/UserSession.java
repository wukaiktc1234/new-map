package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@TableName("user_sessions")
@Schema(description = "用户会话实体")
public class UserSession {
    @TableId(type = IdType.AUTO)
    @Schema(description = "会话ID")
    private Long id;
    @Schema(description = "用户ID")
    private Long userId;
    @Schema(description = "用户名")
    private String username;
    @Schema(description = "设备ID")
    private String deviceId;
    @Schema(description = "设备类型")
    private String deviceType;
    @Schema(description = "设备名称")
    private String deviceName;
    @Schema(description = "浏览器")
    private String browser;
    @Schema(description = "操作系统")
    private String os;
    @Schema(description = "登录IP")
    private String loginIp;
    @Schema(description = "登录地点")
    private String loginLocation;
    @Schema(description = "登录时间")
    private LocalDateTime loginTime;
    @Schema(description = "最后活跃时间")
    private LocalDateTime lastActiveTime;
    @Schema(description = "会话状态：1-活跃，0-已退出")
    private Integer status;
    @Schema(description = "是否当前会话")
    private Boolean isCurrent;
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    public UserSession() {
    }

    public Long getId() {
        return this.id;
    }

    public Long getUserId() {
        return this.userId;
    }

    public String getUsername() {
        return this.username;
    }

    public String getDeviceId() {
        return this.deviceId;
    }

    public String getDeviceType() {
        return this.deviceType;
    }

    public String getDeviceName() {
        return this.deviceName;
    }

    public String getBrowser() {
        return this.browser;
    }

    public String getOs() {
        return this.os;
    }

    public String getLoginIp() {
        return this.loginIp;
    }

    public String getLoginLocation() {
        return this.loginLocation;
    }

    public LocalDateTime getLoginTime() {
        return this.loginTime;
    }

    public LocalDateTime getLastActiveTime() {
        return this.lastActiveTime;
    }

    public Integer getStatus() {
        return this.status;
    }

    public Boolean getIsCurrent() {
        return this.isCurrent;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setUserId(final Long userId) {
        this.userId = userId;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public void setDeviceId(final String deviceId) {
        this.deviceId = deviceId;
    }

    public void setDeviceType(final String deviceType) {
        this.deviceType = deviceType;
    }

    public void setDeviceName(final String deviceName) {
        this.deviceName = deviceName;
    }

    public void setBrowser(final String browser) {
        this.browser = browser;
    }

    public void setOs(final String os) {
        this.os = os;
    }

    public void setLoginIp(final String loginIp) {
        this.loginIp = loginIp;
    }

    public void setLoginLocation(final String loginLocation) {
        this.loginLocation = loginLocation;
    }

    public void setLoginTime(final LocalDateTime loginTime) {
        this.loginTime = loginTime;
    }

    public void setLastActiveTime(final LocalDateTime lastActiveTime) {
        this.lastActiveTime = lastActiveTime;
    }

    public void setStatus(final Integer status) {
        this.status = status;
    }

    public void setIsCurrent(final Boolean isCurrent) {
        this.isCurrent = isCurrent;
    }

    public void setCreatedAt(final LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof UserSession)) return false;
        final UserSession other = (UserSession) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$userId = this.getUserId();
        final java.lang.Object other$userId = other.getUserId();
        if (this$userId == null ? other$userId != null : !this$userId.equals(other$userId)) return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
        final java.lang.Object this$isCurrent = this.getIsCurrent();
        final java.lang.Object other$isCurrent = other.getIsCurrent();
        if (this$isCurrent == null ? other$isCurrent != null : !this$isCurrent.equals(other$isCurrent)) return false;
        final java.lang.Object this$username = this.getUsername();
        final java.lang.Object other$username = other.getUsername();
        if (this$username == null ? other$username != null : !this$username.equals(other$username)) return false;
        final java.lang.Object this$deviceId = this.getDeviceId();
        final java.lang.Object other$deviceId = other.getDeviceId();
        if (this$deviceId == null ? other$deviceId != null : !this$deviceId.equals(other$deviceId)) return false;
        final java.lang.Object this$deviceType = this.getDeviceType();
        final java.lang.Object other$deviceType = other.getDeviceType();
        if (this$deviceType == null ? other$deviceType != null : !this$deviceType.equals(other$deviceType)) return false;
        final java.lang.Object this$deviceName = this.getDeviceName();
        final java.lang.Object other$deviceName = other.getDeviceName();
        if (this$deviceName == null ? other$deviceName != null : !this$deviceName.equals(other$deviceName)) return false;
        final java.lang.Object this$browser = this.getBrowser();
        final java.lang.Object other$browser = other.getBrowser();
        if (this$browser == null ? other$browser != null : !this$browser.equals(other$browser)) return false;
        final java.lang.Object this$os = this.getOs();
        final java.lang.Object other$os = other.getOs();
        if (this$os == null ? other$os != null : !this$os.equals(other$os)) return false;
        final java.lang.Object this$loginIp = this.getLoginIp();
        final java.lang.Object other$loginIp = other.getLoginIp();
        if (this$loginIp == null ? other$loginIp != null : !this$loginIp.equals(other$loginIp)) return false;
        final java.lang.Object this$loginLocation = this.getLoginLocation();
        final java.lang.Object other$loginLocation = other.getLoginLocation();
        if (this$loginLocation == null ? other$loginLocation != null : !this$loginLocation.equals(other$loginLocation)) return false;
        final java.lang.Object this$loginTime = this.getLoginTime();
        final java.lang.Object other$loginTime = other.getLoginTime();
        if (this$loginTime == null ? other$loginTime != null : !this$loginTime.equals(other$loginTime)) return false;
        final java.lang.Object this$lastActiveTime = this.getLastActiveTime();
        final java.lang.Object other$lastActiveTime = other.getLastActiveTime();
        if (this$lastActiveTime == null ? other$lastActiveTime != null : !this$lastActiveTime.equals(other$lastActiveTime)) return false;
        final java.lang.Object this$createdAt = this.getCreatedAt();
        final java.lang.Object other$createdAt = other.getCreatedAt();
        if (this$createdAt == null ? other$createdAt != null : !this$createdAt.equals(other$createdAt)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof UserSession;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $userId = this.getUserId();
        result = result * PRIME + ($userId == null ? 43 : $userId.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $isCurrent = this.getIsCurrent();
        result = result * PRIME + ($isCurrent == null ? 43 : $isCurrent.hashCode());
        final java.lang.Object $username = this.getUsername();
        result = result * PRIME + ($username == null ? 43 : $username.hashCode());
        final java.lang.Object $deviceId = this.getDeviceId();
        result = result * PRIME + ($deviceId == null ? 43 : $deviceId.hashCode());
        final java.lang.Object $deviceType = this.getDeviceType();
        result = result * PRIME + ($deviceType == null ? 43 : $deviceType.hashCode());
        final java.lang.Object $deviceName = this.getDeviceName();
        result = result * PRIME + ($deviceName == null ? 43 : $deviceName.hashCode());
        final java.lang.Object $browser = this.getBrowser();
        result = result * PRIME + ($browser == null ? 43 : $browser.hashCode());
        final java.lang.Object $os = this.getOs();
        result = result * PRIME + ($os == null ? 43 : $os.hashCode());
        final java.lang.Object $loginIp = this.getLoginIp();
        result = result * PRIME + ($loginIp == null ? 43 : $loginIp.hashCode());
        final java.lang.Object $loginLocation = this.getLoginLocation();
        result = result * PRIME + ($loginLocation == null ? 43 : $loginLocation.hashCode());
        final java.lang.Object $loginTime = this.getLoginTime();
        result = result * PRIME + ($loginTime == null ? 43 : $loginTime.hashCode());
        final java.lang.Object $lastActiveTime = this.getLastActiveTime();
        result = result * PRIME + ($lastActiveTime == null ? 43 : $lastActiveTime.hashCode());
        final java.lang.Object $createdAt = this.getCreatedAt();
        result = result * PRIME + ($createdAt == null ? 43 : $createdAt.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "UserSession(id=" + this.getId() + ", userId=" + this.getUserId() + ", username=" + this.getUsername() + ", deviceId=" + this.getDeviceId() + ", deviceType=" + this.getDeviceType() + ", deviceName=" + this.getDeviceName() + ", browser=" + this.getBrowser() + ", os=" + this.getOs() + ", loginIp=" + this.getLoginIp() + ", loginLocation=" + this.getLoginLocation() + ", loginTime=" + this.getLoginTime() + ", lastActiveTime=" + this.getLastActiveTime() + ", status=" + this.getStatus() + ", isCurrent=" + this.getIsCurrent() + ", createdAt=" + this.getCreatedAt() + ")";
    }
}
