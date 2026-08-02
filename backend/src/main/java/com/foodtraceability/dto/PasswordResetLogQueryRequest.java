package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 密码重置日志查询请求
 */
@Schema(description = "密码重置日志查询请求")
public class PasswordResetLogQueryRequest extends PageRequest {
    @Schema(description = "用户ID")
    private Long userId;
    @Schema(description = "用户名")
    private String username;
    @Schema(description = "操作类型：SEND_CODE-发送验证码, VERIFY_CODE-验证验证码, RESET_PASSWORD-重置密码")
    private String operationType;
    @Schema(description = "操作状态：SUCCESS-成功, FAILED-失败")
    private String operationStatus;
    @Schema(description = "客户端IP地址")
    private String clientIp;
    @Schema(description = "是否异常操作：0-正常, 1-异常")
    private Integer isAbnormal;
    @Schema(description = "异常类型")
    private String abnormalType;
    @Schema(description = "开始时间")
    private LocalDateTime startTime;
    @Schema(description = "结束时间")
    private LocalDateTime endTime;

    public PasswordResetLogQueryRequest() {
    }

    public Long getUserId() {
        return this.userId;
    }

    public String getUsername() {
        return this.username;
    }

    public String getOperationType() {
        return this.operationType;
    }

    public String getOperationStatus() {
        return this.operationStatus;
    }

    public String getClientIp() {
        return this.clientIp;
    }

    public Integer getIsAbnormal() {
        return this.isAbnormal;
    }

    public String getAbnormalType() {
        return this.abnormalType;
    }

    public LocalDateTime getStartTime() {
        return this.startTime;
    }

    public LocalDateTime getEndTime() {
        return this.endTime;
    }

    public void setUserId(final Long userId) {
        this.userId = userId;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public void setOperationType(final String operationType) {
        this.operationType = operationType;
    }

    public void setOperationStatus(final String operationStatus) {
        this.operationStatus = operationStatus;
    }

    public void setClientIp(final String clientIp) {
        this.clientIp = clientIp;
    }

    public void setIsAbnormal(final Integer isAbnormal) {
        this.isAbnormal = isAbnormal;
    }

    public void setAbnormalType(final String abnormalType) {
        this.abnormalType = abnormalType;
    }

    public void setStartTime(final LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(final LocalDateTime endTime) {
        this.endTime = endTime;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "PasswordResetLogQueryRequest(userId=" + this.getUserId() + ", username=" + this.getUsername() + ", operationType=" + this.getOperationType() + ", operationStatus=" + this.getOperationStatus() + ", clientIp=" + this.getClientIp() + ", isAbnormal=" + this.getIsAbnormal() + ", abnormalType=" + this.getAbnormalType() + ", startTime=" + this.getStartTime() + ", endTime=" + this.getEndTime() + ")";
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof PasswordResetLogQueryRequest)) return false;
        final PasswordResetLogQueryRequest other = (PasswordResetLogQueryRequest) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        if (!super.equals(o)) return false;
        final java.lang.Object this$userId = this.getUserId();
        final java.lang.Object other$userId = other.getUserId();
        if (this$userId == null ? other$userId != null : !this$userId.equals(other$userId)) return false;
        final java.lang.Object this$isAbnormal = this.getIsAbnormal();
        final java.lang.Object other$isAbnormal = other.getIsAbnormal();
        if (this$isAbnormal == null ? other$isAbnormal != null : !this$isAbnormal.equals(other$isAbnormal)) return false;
        final java.lang.Object this$username = this.getUsername();
        final java.lang.Object other$username = other.getUsername();
        if (this$username == null ? other$username != null : !this$username.equals(other$username)) return false;
        final java.lang.Object this$operationType = this.getOperationType();
        final java.lang.Object other$operationType = other.getOperationType();
        if (this$operationType == null ? other$operationType != null : !this$operationType.equals(other$operationType)) return false;
        final java.lang.Object this$operationStatus = this.getOperationStatus();
        final java.lang.Object other$operationStatus = other.getOperationStatus();
        if (this$operationStatus == null ? other$operationStatus != null : !this$operationStatus.equals(other$operationStatus)) return false;
        final java.lang.Object this$clientIp = this.getClientIp();
        final java.lang.Object other$clientIp = other.getClientIp();
        if (this$clientIp == null ? other$clientIp != null : !this$clientIp.equals(other$clientIp)) return false;
        final java.lang.Object this$abnormalType = this.getAbnormalType();
        final java.lang.Object other$abnormalType = other.getAbnormalType();
        if (this$abnormalType == null ? other$abnormalType != null : !this$abnormalType.equals(other$abnormalType)) return false;
        final java.lang.Object this$startTime = this.getStartTime();
        final java.lang.Object other$startTime = other.getStartTime();
        if (this$startTime == null ? other$startTime != null : !this$startTime.equals(other$startTime)) return false;
        final java.lang.Object this$endTime = this.getEndTime();
        final java.lang.Object other$endTime = other.getEndTime();
        if (this$endTime == null ? other$endTime != null : !this$endTime.equals(other$endTime)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof PasswordResetLogQueryRequest;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = super.hashCode();
        final java.lang.Object $userId = this.getUserId();
        result = result * PRIME + ($userId == null ? 43 : $userId.hashCode());
        final java.lang.Object $isAbnormal = this.getIsAbnormal();
        result = result * PRIME + ($isAbnormal == null ? 43 : $isAbnormal.hashCode());
        final java.lang.Object $username = this.getUsername();
        result = result * PRIME + ($username == null ? 43 : $username.hashCode());
        final java.lang.Object $operationType = this.getOperationType();
        result = result * PRIME + ($operationType == null ? 43 : $operationType.hashCode());
        final java.lang.Object $operationStatus = this.getOperationStatus();
        result = result * PRIME + ($operationStatus == null ? 43 : $operationStatus.hashCode());
        final java.lang.Object $clientIp = this.getClientIp();
        result = result * PRIME + ($clientIp == null ? 43 : $clientIp.hashCode());
        final java.lang.Object $abnormalType = this.getAbnormalType();
        result = result * PRIME + ($abnormalType == null ? 43 : $abnormalType.hashCode());
        final java.lang.Object $startTime = this.getStartTime();
        result = result * PRIME + ($startTime == null ? 43 : $startTime.hashCode());
        final java.lang.Object $endTime = this.getEndTime();
        result = result * PRIME + ($endTime == null ? 43 : $endTime.hashCode());
        return result;
    }
}
