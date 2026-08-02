package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@TableName("login_log")
@Schema(description = "登录日志实体")
public class LoginLog {
    @TableId(type = IdType.AUTO)
    @Schema(description = "日志ID")
    private Long id;
    @Schema(description = "用户ID")
    private Long userId;
    @Schema(description = "用户名")
    private String username;
    @Schema(description = "登录时间")
    private LocalDateTime loginTime;
    @Schema(description = "登出时间")
    private LocalDateTime logoutTime;
    @Schema(description = "登录IP")
    private String loginIp;
    @Schema(description = "登录地点")
    private String loginLocation;
    @Schema(description = "设备类型")
    private String deviceType;
    @Schema(description = "浏览器")
    private String browser;
    @Schema(description = "操作系统")
    private String os;
    @Schema(description = "登录状态：1-成功，0-失败")
    private Integer loginStatus;
    @Schema(description = "失败原因")
    private String failureReason;
    @Schema(description = "会话ID")
    private String sessionId;
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
    /**
     * 是否删除（0-未删除，1-已删除）
     */
    @TableLogic
    @TableField("deleted")
    @Schema(description = "是否删除")
    private Integer deleted;

    public LoginLog() {
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

    public LocalDateTime getLoginTime() {
        return this.loginTime;
    }

    public LocalDateTime getLogoutTime() {
        return this.logoutTime;
    }

    public String getLoginIp() {
        return this.loginIp;
    }

    public String getLoginLocation() {
        return this.loginLocation;
    }

    public String getDeviceType() {
        return this.deviceType;
    }

    public String getBrowser() {
        return this.browser;
    }

    public String getOs() {
        return this.os;
    }

    public Integer getLoginStatus() {
        return this.loginStatus;
    }

    public String getFailureReason() {
        return this.failureReason;
    }

    public String getSessionId() {
        return this.sessionId;
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

    public void setLoginTime(final LocalDateTime loginTime) {
        this.loginTime = loginTime;
    }

    public void setLogoutTime(final LocalDateTime logoutTime) {
        this.logoutTime = logoutTime;
    }

    public void setLoginIp(final String loginIp) {
        this.loginIp = loginIp;
    }

    public void setLoginLocation(final String loginLocation) {
        this.loginLocation = loginLocation;
    }

    public void setDeviceType(final String deviceType) {
        this.deviceType = deviceType;
    }

    public void setBrowser(final String browser) {
        this.browser = browser;
    }

    public void setOs(final String os) {
        this.os = os;
    }

    public void setLoginStatus(final Integer loginStatus) {
        this.loginStatus = loginStatus;
    }

    public void setFailureReason(final String failureReason) {
        this.failureReason = failureReason;
    }

    public void setSessionId(final String sessionId) {
        this.sessionId = sessionId;
    }

    public void setCreatedAt(final LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Integer getDeleted() {
        return this.deleted;
    }

    public void setDeleted(final Integer deleted) {
        this.deleted = deleted;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof LoginLog)) return false;
        final LoginLog other = (LoginLog) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$userId = this.getUserId();
        final java.lang.Object other$userId = other.getUserId();
        if (this$userId == null ? other$userId != null : !this$userId.equals(other$userId)) return false;
        final java.lang.Object this$loginStatus = this.getLoginStatus();
        final java.lang.Object other$loginStatus = other.getLoginStatus();
        if (this$loginStatus == null ? other$loginStatus != null : !this$loginStatus.equals(other$loginStatus)) return false;
        final java.lang.Object this$username = this.getUsername();
        final java.lang.Object other$username = other.getUsername();
        if (this$username == null ? other$username != null : !this$username.equals(other$username)) return false;
        final java.lang.Object this$loginTime = this.getLoginTime();
        final java.lang.Object other$loginTime = other.getLoginTime();
        if (this$loginTime == null ? other$loginTime != null : !this$loginTime.equals(other$loginTime)) return false;
        final java.lang.Object this$logoutTime = this.getLogoutTime();
        final java.lang.Object other$logoutTime = other.getLogoutTime();
        if (this$logoutTime == null ? other$logoutTime != null : !this$logoutTime.equals(other$logoutTime)) return false;
        final java.lang.Object this$loginIp = this.getLoginIp();
        final java.lang.Object other$loginIp = other.getLoginIp();
        if (this$loginIp == null ? other$loginIp != null : !this$loginIp.equals(other$loginIp)) return false;
        final java.lang.Object this$loginLocation = this.getLoginLocation();
        final java.lang.Object other$loginLocation = other.getLoginLocation();
        if (this$loginLocation == null ? other$loginLocation != null : !this$loginLocation.equals(other$loginLocation)) return false;
        final java.lang.Object this$deviceType = this.getDeviceType();
        final java.lang.Object other$deviceType = other.getDeviceType();
        if (this$deviceType == null ? other$deviceType != null : !this$deviceType.equals(other$deviceType)) return false;
        final java.lang.Object this$browser = this.getBrowser();
        final java.lang.Object other$browser = other.getBrowser();
        if (this$browser == null ? other$browser != null : !this$browser.equals(other$browser)) return false;
        final java.lang.Object this$os = this.getOs();
        final java.lang.Object other$os = other.getOs();
        if (this$os == null ? other$os != null : !this$os.equals(other$os)) return false;
        final java.lang.Object this$failureReason = this.getFailureReason();
        final java.lang.Object other$failureReason = other.getFailureReason();
        if (this$failureReason == null ? other$failureReason != null : !this$failureReason.equals(other$failureReason)) return false;
        final java.lang.Object this$sessionId = this.getSessionId();
        final java.lang.Object other$sessionId = other.getSessionId();
        if (this$sessionId == null ? other$sessionId != null : !this$sessionId.equals(other$sessionId)) return false;
        final java.lang.Object this$createdAt = this.getCreatedAt();
        final java.lang.Object other$createdAt = other.getCreatedAt();
        if (this$createdAt == null ? other$createdAt != null : !this$createdAt.equals(other$createdAt)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof LoginLog;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $userId = this.getUserId();
        result = result * PRIME + ($userId == null ? 43 : $userId.hashCode());
        final java.lang.Object $loginStatus = this.getLoginStatus();
        result = result * PRIME + ($loginStatus == null ? 43 : $loginStatus.hashCode());
        final java.lang.Object $username = this.getUsername();
        result = result * PRIME + ($username == null ? 43 : $username.hashCode());
        final java.lang.Object $loginTime = this.getLoginTime();
        result = result * PRIME + ($loginTime == null ? 43 : $loginTime.hashCode());
        final java.lang.Object $logoutTime = this.getLogoutTime();
        result = result * PRIME + ($logoutTime == null ? 43 : $logoutTime.hashCode());
        final java.lang.Object $loginIp = this.getLoginIp();
        result = result * PRIME + ($loginIp == null ? 43 : $loginIp.hashCode());
        final java.lang.Object $loginLocation = this.getLoginLocation();
        result = result * PRIME + ($loginLocation == null ? 43 : $loginLocation.hashCode());
        final java.lang.Object $deviceType = this.getDeviceType();
        result = result * PRIME + ($deviceType == null ? 43 : $deviceType.hashCode());
        final java.lang.Object $browser = this.getBrowser();
        result = result * PRIME + ($browser == null ? 43 : $browser.hashCode());
        final java.lang.Object $os = this.getOs();
        result = result * PRIME + ($os == null ? 43 : $os.hashCode());
        final java.lang.Object $failureReason = this.getFailureReason();
        result = result * PRIME + ($failureReason == null ? 43 : $failureReason.hashCode());
        final java.lang.Object $sessionId = this.getSessionId();
        result = result * PRIME + ($sessionId == null ? 43 : $sessionId.hashCode());
        final java.lang.Object $createdAt = this.getCreatedAt();
        result = result * PRIME + ($createdAt == null ? 43 : $createdAt.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "LoginLog(id=" + this.getId() + ", userId=" + this.getUserId() + ", username=" + this.getUsername() + ", loginTime=" + this.getLoginTime() + ", logoutTime=" + this.getLogoutTime() + ", loginIp=" + this.getLoginIp() + ", loginLocation=" + this.getLoginLocation() + ", deviceType=" + this.getDeviceType() + ", browser=" + this.getBrowser() + ", os=" + this.getOs() + ", loginStatus=" + this.getLoginStatus() + ", failureReason=" + this.getFailureReason() + ", sessionId=" + this.getSessionId() + ", createdAt=" + this.getCreatedAt() + ")";
    }
}
