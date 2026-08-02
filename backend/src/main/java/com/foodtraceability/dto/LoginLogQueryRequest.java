package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "登录日志查询请求")
public class LoginLogQueryRequest {
    @Schema(description = "用户ID")
    private Long userId;
    @Schema(description = "用户名")
    private String username;
    @Schema(description = "登录状态：1-成功，0-失败")
    private Integer loginStatus;
    @Schema(description = "开始时间")
    private LocalDateTime startTime;
    @Schema(description = "结束时间")
    private LocalDateTime endTime;
    @Schema(description = "页码")
    private Integer pageNum = 1;
    @Schema(description = "每页大小")
    private Integer pageSize = 10;

    public LoginLogQueryRequest() {
    }

    public Long getUserId() {
        return this.userId;
    }

    public String getUsername() {
        return this.username;
    }

    public Integer getLoginStatus() {
        return this.loginStatus;
    }

    public LocalDateTime getStartTime() {
        return this.startTime;
    }

    public LocalDateTime getEndTime() {
        return this.endTime;
    }

    public Integer getPageNum() {
        return this.pageNum;
    }

    public Integer getPageSize() {
        return this.pageSize;
    }

    public void setUserId(final Long userId) {
        this.userId = userId;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public void setLoginStatus(final Integer loginStatus) {
        this.loginStatus = loginStatus;
    }

    public void setStartTime(final LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(final LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public void setPageNum(final Integer pageNum) {
        this.pageNum = pageNum;
    }

    public void setPageSize(final Integer pageSize) {
        this.pageSize = pageSize;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof LoginLogQueryRequest)) return false;
        final LoginLogQueryRequest other = (LoginLogQueryRequest) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$userId = this.getUserId();
        final java.lang.Object other$userId = other.getUserId();
        if (this$userId == null ? other$userId != null : !this$userId.equals(other$userId)) return false;
        final java.lang.Object this$loginStatus = this.getLoginStatus();
        final java.lang.Object other$loginStatus = other.getLoginStatus();
        if (this$loginStatus == null ? other$loginStatus != null : !this$loginStatus.equals(other$loginStatus)) return false;
        final java.lang.Object this$pageNum = this.getPageNum();
        final java.lang.Object other$pageNum = other.getPageNum();
        if (this$pageNum == null ? other$pageNum != null : !this$pageNum.equals(other$pageNum)) return false;
        final java.lang.Object this$pageSize = this.getPageSize();
        final java.lang.Object other$pageSize = other.getPageSize();
        if (this$pageSize == null ? other$pageSize != null : !this$pageSize.equals(other$pageSize)) return false;
        final java.lang.Object this$username = this.getUsername();
        final java.lang.Object other$username = other.getUsername();
        if (this$username == null ? other$username != null : !this$username.equals(other$username)) return false;
        final java.lang.Object this$startTime = this.getStartTime();
        final java.lang.Object other$startTime = other.getStartTime();
        if (this$startTime == null ? other$startTime != null : !this$startTime.equals(other$startTime)) return false;
        final java.lang.Object this$endTime = this.getEndTime();
        final java.lang.Object other$endTime = other.getEndTime();
        if (this$endTime == null ? other$endTime != null : !this$endTime.equals(other$endTime)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof LoginLogQueryRequest;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $userId = this.getUserId();
        result = result * PRIME + ($userId == null ? 43 : $userId.hashCode());
        final java.lang.Object $loginStatus = this.getLoginStatus();
        result = result * PRIME + ($loginStatus == null ? 43 : $loginStatus.hashCode());
        final java.lang.Object $pageNum = this.getPageNum();
        result = result * PRIME + ($pageNum == null ? 43 : $pageNum.hashCode());
        final java.lang.Object $pageSize = this.getPageSize();
        result = result * PRIME + ($pageSize == null ? 43 : $pageSize.hashCode());
        final java.lang.Object $username = this.getUsername();
        result = result * PRIME + ($username == null ? 43 : $username.hashCode());
        final java.lang.Object $startTime = this.getStartTime();
        result = result * PRIME + ($startTime == null ? 43 : $startTime.hashCode());
        final java.lang.Object $endTime = this.getEndTime();
        result = result * PRIME + ($endTime == null ? 43 : $endTime.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "LoginLogQueryRequest(userId=" + this.getUserId() + ", username=" + this.getUsername() + ", loginStatus=" + this.getLoginStatus() + ", startTime=" + this.getStartTime() + ", endTime=" + this.getEndTime() + ", pageNum=" + this.getPageNum() + ", pageSize=" + this.getPageSize() + ")";
    }
}
