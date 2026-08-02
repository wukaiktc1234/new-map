package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 密码重置操作日志实体
 * 用于记录忘记密码相关的所有操作，便于安全审计和监控
 */
@TableName("password_reset_log")
@Schema(description = "密码重置操作日志实体")
public class PasswordResetLog {
    @TableId(type = IdType.AUTO)
    @Schema(description = "日志ID")
    private Long id;
    @Schema(description = "用户ID")
    private Long userId;
    @Schema(description = "用户名")
    private String username;
    @Schema(description = "操作类型：SEND_CODE-发送验证码, VERIFY_CODE-验证验证码, RESET_PASSWORD-重置密码")
    private String operationType;
    @Schema(description = "操作状态：SUCCESS-成功, FAILED-失败")
    private String operationStatus;
    @Schema(description = "操作详情")
    private String operationDetail;
    @Schema(description = "验证码类型：EMAIL-邮箱, PHONE-手机")
    private String codeType;
    @Schema(description = "客户端IP地址")
    private String clientIp;
    @Schema(description = "客户端地点")
    private String clientLocation;
    @Schema(description = "设备类型")
    private String deviceType;
    @Schema(description = "浏览器信息")
    private String browser;
    @Schema(description = "操作系统")
    private String os;
    @Schema(description = "失败原因")
    private String failureReason;
    @Schema(description = "错误次数")
    private Integer errorCount;
    @Schema(description = "是否异常操作：0-正常, 1-异常")
    private Integer isAbnormal;
    @Schema(description = "异常类型：FREQUENCY_LIMIT-频率限制, ATTEMPT_LIMIT-尝试次数限制, INVALID_TOKEN-无效令牌等")
    private String abnormalType;
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
    /**
     * 是否删除（0-未删除，1-已删除）
     */
    @TableLogic
    @TableField("deleted")
    @Schema(description = "是否删除")
    private Integer deleted;

    public PasswordResetLog() {
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

    public String getOperationType() {
        return this.operationType;
    }

    public String getOperationStatus() {
        return this.operationStatus;
    }

    public String getOperationDetail() {
        return this.operationDetail;
    }

    public String getCodeType() {
        return this.codeType;
    }

    public String getClientIp() {
        return this.clientIp;
    }

    public String getClientLocation() {
        return this.clientLocation;
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

    public String getFailureReason() {
        return this.failureReason;
    }

    public Integer getErrorCount() {
        return this.errorCount;
    }

    public Integer getIsAbnormal() {
        return this.isAbnormal;
    }

    public String getAbnormalType() {
        return this.abnormalType;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return this.updatedAt;
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

    public void setOperationType(final String operationType) {
        this.operationType = operationType;
    }

    public void setOperationStatus(final String operationStatus) {
        this.operationStatus = operationStatus;
    }

    public void setOperationDetail(final String operationDetail) {
        this.operationDetail = operationDetail;
    }

    public void setCodeType(final String codeType) {
        this.codeType = codeType;
    }

    public void setClientIp(final String clientIp) {
        this.clientIp = clientIp;
    }

    public void setClientLocation(final String clientLocation) {
        this.clientLocation = clientLocation;
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

    public void setFailureReason(final String failureReason) {
        this.failureReason = failureReason;
    }

    public void setErrorCount(final Integer errorCount) {
        this.errorCount = errorCount;
    }

    public void setIsAbnormal(final Integer isAbnormal) {
        this.isAbnormal = isAbnormal;
    }

    public void setAbnormalType(final String abnormalType) {
        this.abnormalType = abnormalType;
    }

    public void setCreatedAt(final LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(final LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
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
        if (!(o instanceof PasswordResetLog)) return false;
        final PasswordResetLog other = (PasswordResetLog) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$userId = this.getUserId();
        final java.lang.Object other$userId = other.getUserId();
        if (this$userId == null ? other$userId != null : !this$userId.equals(other$userId)) return false;
        final java.lang.Object this$errorCount = this.getErrorCount();
        final java.lang.Object other$errorCount = other.getErrorCount();
        if (this$errorCount == null ? other$errorCount != null : !this$errorCount.equals(other$errorCount)) return false;
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
        final java.lang.Object this$operationDetail = this.getOperationDetail();
        final java.lang.Object other$operationDetail = other.getOperationDetail();
        if (this$operationDetail == null ? other$operationDetail != null : !this$operationDetail.equals(other$operationDetail)) return false;
        final java.lang.Object this$codeType = this.getCodeType();
        final java.lang.Object other$codeType = other.getCodeType();
        if (this$codeType == null ? other$codeType != null : !this$codeType.equals(other$codeType)) return false;
        final java.lang.Object this$clientIp = this.getClientIp();
        final java.lang.Object other$clientIp = other.getClientIp();
        if (this$clientIp == null ? other$clientIp != null : !this$clientIp.equals(other$clientIp)) return false;
        final java.lang.Object this$clientLocation = this.getClientLocation();
        final java.lang.Object other$clientLocation = other.getClientLocation();
        if (this$clientLocation == null ? other$clientLocation != null : !this$clientLocation.equals(other$clientLocation)) return false;
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
        final java.lang.Object this$abnormalType = this.getAbnormalType();
        final java.lang.Object other$abnormalType = other.getAbnormalType();
        if (this$abnormalType == null ? other$abnormalType != null : !this$abnormalType.equals(other$abnormalType)) return false;
        final java.lang.Object this$createdAt = this.getCreatedAt();
        final java.lang.Object other$createdAt = other.getCreatedAt();
        if (this$createdAt == null ? other$createdAt != null : !this$createdAt.equals(other$createdAt)) return false;
        final java.lang.Object this$updatedAt = this.getUpdatedAt();
        final java.lang.Object other$updatedAt = other.getUpdatedAt();
        if (this$updatedAt == null ? other$updatedAt != null : !this$updatedAt.equals(other$updatedAt)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof PasswordResetLog;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $userId = this.getUserId();
        result = result * PRIME + ($userId == null ? 43 : $userId.hashCode());
        final java.lang.Object $errorCount = this.getErrorCount();
        result = result * PRIME + ($errorCount == null ? 43 : $errorCount.hashCode());
        final java.lang.Object $isAbnormal = this.getIsAbnormal();
        result = result * PRIME + ($isAbnormal == null ? 43 : $isAbnormal.hashCode());
        final java.lang.Object $username = this.getUsername();
        result = result * PRIME + ($username == null ? 43 : $username.hashCode());
        final java.lang.Object $operationType = this.getOperationType();
        result = result * PRIME + ($operationType == null ? 43 : $operationType.hashCode());
        final java.lang.Object $operationStatus = this.getOperationStatus();
        result = result * PRIME + ($operationStatus == null ? 43 : $operationStatus.hashCode());
        final java.lang.Object $operationDetail = this.getOperationDetail();
        result = result * PRIME + ($operationDetail == null ? 43 : $operationDetail.hashCode());
        final java.lang.Object $codeType = this.getCodeType();
        result = result * PRIME + ($codeType == null ? 43 : $codeType.hashCode());
        final java.lang.Object $clientIp = this.getClientIp();
        result = result * PRIME + ($clientIp == null ? 43 : $clientIp.hashCode());
        final java.lang.Object $clientLocation = this.getClientLocation();
        result = result * PRIME + ($clientLocation == null ? 43 : $clientLocation.hashCode());
        final java.lang.Object $deviceType = this.getDeviceType();
        result = result * PRIME + ($deviceType == null ? 43 : $deviceType.hashCode());
        final java.lang.Object $browser = this.getBrowser();
        result = result * PRIME + ($browser == null ? 43 : $browser.hashCode());
        final java.lang.Object $os = this.getOs();
        result = result * PRIME + ($os == null ? 43 : $os.hashCode());
        final java.lang.Object $failureReason = this.getFailureReason();
        result = result * PRIME + ($failureReason == null ? 43 : $failureReason.hashCode());
        final java.lang.Object $abnormalType = this.getAbnormalType();
        result = result * PRIME + ($abnormalType == null ? 43 : $abnormalType.hashCode());
        final java.lang.Object $createdAt = this.getCreatedAt();
        result = result * PRIME + ($createdAt == null ? 43 : $createdAt.hashCode());
        final java.lang.Object $updatedAt = this.getUpdatedAt();
        result = result * PRIME + ($updatedAt == null ? 43 : $updatedAt.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "PasswordResetLog(id=" + this.getId() + ", userId=" + this.getUserId() + ", username=" + this.getUsername() + ", operationType=" + this.getOperationType() + ", operationStatus=" + this.getOperationStatus() + ", operationDetail=" + this.getOperationDetail() + ", codeType=" + this.getCodeType() + ", clientIp=" + this.getClientIp() + ", clientLocation=" + this.getClientLocation() + ", deviceType=" + this.getDeviceType() + ", browser=" + this.getBrowser() + ", os=" + this.getOs() + ", failureReason=" + this.getFailureReason() + ", errorCount=" + this.getErrorCount() + ", isAbnormal=" + this.getIsAbnormal() + ", abnormalType=" + this.getAbnormalType() + ", createdAt=" + this.getCreatedAt() + ", updatedAt=" + this.getUpdatedAt() + ")";
    }
}
