package com.foodtraceability.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Date;

/**
 * 设备测试结果实体类
 * 用于存储设备连接测试的详细结果
 */
@Schema(description = "设备测试结果")
public class TestResult {
    @Schema(description = "设备ID")
    private Long deviceId;
    @Schema(description = "设备类型")
    private String deviceType;
    @Schema(description = "设备名称")
    private String deviceName;
    @Schema(description = "连接类型")
    private String connectionType;
    @Schema(description = "IP地址")
    private String ipAddress;
    @Schema(description = "端口")
    private String port;
    @Schema(description = "测试是否成功")
    private Boolean success;
    @Schema(description = "测试消息")
    private String message;
    @Schema(description = "详细信息")
    private String details;
    @Schema(description = "错误信息")
    private String errorMessage;
    @Schema(description = "响应时间（毫秒）")
    private Long responseTime;
    @Schema(description = "测试时间")
    private Date testTime;

    public TestResult() {
        this.testTime = new Date();
    }

    /**
     * 创建失败的测试结果
     */
    public static TestResult failure(String deviceType, String deviceName, String connectionType, Long responseTime, String message, String errorMessage, String details) {
        TestResult result = new TestResult();
        result.deviceType = deviceType;
        result.deviceName = deviceName;
        result.connectionType = connectionType;
        result.responseTime = responseTime;
        result.success = false;
        result.message = message;
        result.errorMessage = errorMessage;
        result.details = details;
        return result;
    }

    public Long getDeviceId() {
        return this.deviceId;
    }

    public String getDeviceType() {
        return this.deviceType;
    }

    public String getDeviceName() {
        return this.deviceName;
    }

    public String getConnectionType() {
        return this.connectionType;
    }

    public String getIpAddress() {
        return this.ipAddress;
    }

    public String getPort() {
        return this.port;
    }

    public Boolean getSuccess() {
        return this.success;
    }

    public String getMessage() {
        return this.message;
    }

    public String getDetails() {
        return this.details;
    }

    public String getErrorMessage() {
        return this.errorMessage;
    }

    public Long getResponseTime() {
        return this.responseTime;
    }

    public Date getTestTime() {
        return this.testTime;
    }

    public void setDeviceId(final Long deviceId) {
        this.deviceId = deviceId;
    }

    public void setDeviceType(final String deviceType) {
        this.deviceType = deviceType;
    }

    public void setDeviceName(final String deviceName) {
        this.deviceName = deviceName;
    }

    public void setConnectionType(final String connectionType) {
        this.connectionType = connectionType;
    }

    public void setIpAddress(final String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public void setPort(final String port) {
        this.port = port;
    }

    public void setSuccess(final Boolean success) {
        this.success = success;
    }

    public void setMessage(final String message) {
        this.message = message;
    }

    public void setDetails(final String details) {
        this.details = details;
    }

    public void setErrorMessage(final String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public void setResponseTime(final Long responseTime) {
        this.responseTime = responseTime;
    }

    public void setTestTime(final Date testTime) {
        this.testTime = testTime;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof TestResult)) return false;
        final TestResult other = (TestResult) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$deviceId = this.getDeviceId();
        final java.lang.Object other$deviceId = other.getDeviceId();
        if (this$deviceId == null ? other$deviceId != null : !this$deviceId.equals(other$deviceId)) return false;
        final java.lang.Object this$success = this.getSuccess();
        final java.lang.Object other$success = other.getSuccess();
        if (this$success == null ? other$success != null : !this$success.equals(other$success)) return false;
        final java.lang.Object this$responseTime = this.getResponseTime();
        final java.lang.Object other$responseTime = other.getResponseTime();
        if (this$responseTime == null ? other$responseTime != null : !this$responseTime.equals(other$responseTime)) return false;
        final java.lang.Object this$deviceType = this.getDeviceType();
        final java.lang.Object other$deviceType = other.getDeviceType();
        if (this$deviceType == null ? other$deviceType != null : !this$deviceType.equals(other$deviceType)) return false;
        final java.lang.Object this$deviceName = this.getDeviceName();
        final java.lang.Object other$deviceName = other.getDeviceName();
        if (this$deviceName == null ? other$deviceName != null : !this$deviceName.equals(other$deviceName)) return false;
        final java.lang.Object this$connectionType = this.getConnectionType();
        final java.lang.Object other$connectionType = other.getConnectionType();
        if (this$connectionType == null ? other$connectionType != null : !this$connectionType.equals(other$connectionType)) return false;
        final java.lang.Object this$ipAddress = this.getIpAddress();
        final java.lang.Object other$ipAddress = other.getIpAddress();
        if (this$ipAddress == null ? other$ipAddress != null : !this$ipAddress.equals(other$ipAddress)) return false;
        final java.lang.Object this$port = this.getPort();
        final java.lang.Object other$port = other.getPort();
        if (this$port == null ? other$port != null : !this$port.equals(other$port)) return false;
        final java.lang.Object this$message = this.getMessage();
        final java.lang.Object other$message = other.getMessage();
        if (this$message == null ? other$message != null : !this$message.equals(other$message)) return false;
        final java.lang.Object this$details = this.getDetails();
        final java.lang.Object other$details = other.getDetails();
        if (this$details == null ? other$details != null : !this$details.equals(other$details)) return false;
        final java.lang.Object this$errorMessage = this.getErrorMessage();
        final java.lang.Object other$errorMessage = other.getErrorMessage();
        if (this$errorMessage == null ? other$errorMessage != null : !this$errorMessage.equals(other$errorMessage)) return false;
        final java.lang.Object this$testTime = this.getTestTime();
        final java.lang.Object other$testTime = other.getTestTime();
        if (this$testTime == null ? other$testTime != null : !this$testTime.equals(other$testTime)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof TestResult;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $deviceId = this.getDeviceId();
        result = result * PRIME + ($deviceId == null ? 43 : $deviceId.hashCode());
        final java.lang.Object $success = this.getSuccess();
        result = result * PRIME + ($success == null ? 43 : $success.hashCode());
        final java.lang.Object $responseTime = this.getResponseTime();
        result = result * PRIME + ($responseTime == null ? 43 : $responseTime.hashCode());
        final java.lang.Object $deviceType = this.getDeviceType();
        result = result * PRIME + ($deviceType == null ? 43 : $deviceType.hashCode());
        final java.lang.Object $deviceName = this.getDeviceName();
        result = result * PRIME + ($deviceName == null ? 43 : $deviceName.hashCode());
        final java.lang.Object $connectionType = this.getConnectionType();
        result = result * PRIME + ($connectionType == null ? 43 : $connectionType.hashCode());
        final java.lang.Object $ipAddress = this.getIpAddress();
        result = result * PRIME + ($ipAddress == null ? 43 : $ipAddress.hashCode());
        final java.lang.Object $port = this.getPort();
        result = result * PRIME + ($port == null ? 43 : $port.hashCode());
        final java.lang.Object $message = this.getMessage();
        result = result * PRIME + ($message == null ? 43 : $message.hashCode());
        final java.lang.Object $details = this.getDetails();
        result = result * PRIME + ($details == null ? 43 : $details.hashCode());
        final java.lang.Object $errorMessage = this.getErrorMessage();
        result = result * PRIME + ($errorMessage == null ? 43 : $errorMessage.hashCode());
        final java.lang.Object $testTime = this.getTestTime();
        result = result * PRIME + ($testTime == null ? 43 : $testTime.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "TestResult(deviceId=" + this.getDeviceId() + ", deviceType=" + this.getDeviceType() + ", deviceName=" + this.getDeviceName() + ", connectionType=" + this.getConnectionType() + ", ipAddress=" + this.getIpAddress() + ", port=" + this.getPort() + ", success=" + this.getSuccess() + ", message=" + this.getMessage() + ", details=" + this.getDetails() + ", errorMessage=" + this.getErrorMessage() + ", responseTime=" + this.getResponseTime() + ", testTime=" + this.getTestTime() + ")";
    }
}
