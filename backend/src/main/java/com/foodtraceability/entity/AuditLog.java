package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@TableName("audit_log")
@Schema(description = "审计日志实体")
public class AuditLog {
    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;
    @TableField("tenant_id")
    @Schema(description = "租户ID")
    private String tenantId;
    @TableField("request_id")
    @Schema(description = "请求追踪ID")
    private String requestId;
    @TableField("user_id")
    @Schema(description = "操作用户ID")
    private String userId;
    @TableField("username")
    @Schema(description = "操作用户名")
    private String username;
    @TableField("operation")
    @Schema(description = "操作描述")
    private String operation;
    @TableField("module")
    @Schema(description = "所属模块")
    private String module;
    @TableField("operation_type")
    @Schema(description = "操作类型: LOGIN/CREATE/UPDATE/DELETE/EXPORT/VIEW/AUTH/SYSTEM/SECURITY")
    private String operationType;
    @TableField("ip")
    @Schema(description = "客户端IP地址")
    private String ip;
    @TableField("user_agent")
    @Schema(description = "用户代理")
    private String userAgent;
    @TableField("request_url")
    @Schema(description = "请求URL")
    private String requestUrl;
    @TableField("request_method")
    @Schema(description = "请求方法: GET/POST/PUT/DELETE")
    private String requestMethod;
    @TableField("request_params")
    @Schema(description = "请求参数(JSON)")
    private String requestParams;
    @TableField("response_status")
    @Schema(description = "响应状态: SUCCESS/FAILED/PARTIAL")
    private String responseStatus;
    @TableField("response_body")
    @Schema(description = "响应内容(可选)")
    private String responseBody;
    @TableField("error_message")
    @Schema(description = "错误信息")
    private String errorMessage;
    @TableField("execution_time")
    @Schema(description = "执行耗时(毫秒)")
    private Long executionTime;
    @TableField("business_key")
    @Schema(description = "业务主键(如订单号)")
    private String businessKey;
    @TableField("risk_level")
    @Schema(description = "风险等级: LOW/MEDIUM/HIGH/CRITICAL")
    private String riskLevel;
    @TableField("sensitive_flag")
    @Schema(description = "是否包含敏感数据")
    private Integer sensitiveFlag;
    /**
     * 操作前值（JSON 格式）。
     * 用于证明数据未被篡改，满足会计法/网络安全法对审计日志完整性的要求。
     */
    @TableField(value = "before_value")
    @Schema(description = "操作前值(JSON格式，用于防篡改)")
    private String beforeValue;
    /**
     * 操作后值（JSON 格式）。
     * 用于证明数据未被篡改，满足会计法/网络安全法对审计日志完整性的要求。
     */
    @TableField(value = "after_value")
    @Schema(description = "操作后值(JSON格式，用于防篡改)")
    private String afterValue;
    @TableField("session_id")
    @Schema(description = "会话ID")
    private String sessionId;
    @TableField("create_time")
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    public AuditLog() {
    }

    public Long getId() {
        return this.id;
    }

    public String getTenantId() {
        return this.tenantId;
    }

    public String getRequestId() {
        return this.requestId;
    }

    public String getUserId() {
        return this.userId;
    }

    public String getUsername() {
        return this.username;
    }

    public String getOperation() {
        return this.operation;
    }

    public String getModule() {
        return this.module;
    }

    public String getOperationType() {
        return this.operationType;
    }

    public String getIp() {
        return this.ip;
    }

    public String getUserAgent() {
        return this.userAgent;
    }

    public String getRequestUrl() {
        return this.requestUrl;
    }

    public String getRequestMethod() {
        return this.requestMethod;
    }

    public String getRequestParams() {
        return this.requestParams;
    }

    public String getResponseStatus() {
        return this.responseStatus;
    }

    public String getResponseBody() {
        return this.responseBody;
    }

    public String getErrorMessage() {
        return this.errorMessage;
    }

    public Long getExecutionTime() {
        return this.executionTime;
    }

    public String getBusinessKey() {
        return this.businessKey;
    }

    public String getRiskLevel() {
        return this.riskLevel;
    }

    public Integer getSensitiveFlag() {
        return this.sensitiveFlag;
    }

    public String getBeforeValue() {
        return this.beforeValue;
    }

    public String getAfterValue() {
        return this.afterValue;
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

    public void setTenantId(final String tenantId) {
        this.tenantId = tenantId;
    }

    public void setRequestId(final String requestId) {
        this.requestId = requestId;
    }

    public void setUserId(final String userId) {
        this.userId = userId;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public void setOperation(final String operation) {
        this.operation = operation;
    }

    public void setModule(final String module) {
        this.module = module;
    }

    public void setOperationType(final String operationType) {
        this.operationType = operationType;
    }

    public void setIp(final String ip) {
        this.ip = ip;
    }

    public void setUserAgent(final String userAgent) {
        this.userAgent = userAgent;
    }

    public void setRequestUrl(final String requestUrl) {
        this.requestUrl = requestUrl;
    }

    public void setRequestMethod(final String requestMethod) {
        this.requestMethod = requestMethod;
    }

    public void setRequestParams(final String requestParams) {
        this.requestParams = requestParams;
    }

    public void setResponseStatus(final String responseStatus) {
        this.responseStatus = responseStatus;
    }

    public void setResponseBody(final String responseBody) {
        this.responseBody = responseBody;
    }

    public void setErrorMessage(final String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public void setExecutionTime(final Long executionTime) {
        this.executionTime = executionTime;
    }

    public void setBusinessKey(final String businessKey) {
        this.businessKey = businessKey;
    }

    public void setRiskLevel(final String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public void setSensitiveFlag(final Integer sensitiveFlag) {
        this.sensitiveFlag = sensitiveFlag;
    }

    public void setBeforeValue(final String beforeValue) {
        this.beforeValue = beforeValue;
    }

    public void setAfterValue(final String afterValue) {
        this.afterValue = afterValue;
    }

    public void setSessionId(final String sessionId) {
        this.sessionId = sessionId;
    }

    public void setCreatedAt(final LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof AuditLog)) return false;
        final AuditLog other = (AuditLog) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$executionTime = this.getExecutionTime();
        final java.lang.Object other$executionTime = other.getExecutionTime();
        if (this$executionTime == null ? other$executionTime != null : !this$executionTime.equals(other$executionTime)) return false;
        final java.lang.Object this$sensitiveFlag = this.getSensitiveFlag();
        final java.lang.Object other$sensitiveFlag = other.getSensitiveFlag();
        if (this$sensitiveFlag == null ? other$sensitiveFlag != null : !this$sensitiveFlag.equals(other$sensitiveFlag)) return false;
        final java.lang.Object this$beforeValue = this.getBeforeValue();
        final java.lang.Object other$beforeValue = other.getBeforeValue();
        if (this$beforeValue == null ? other$beforeValue != null : !this$beforeValue.equals(other$beforeValue)) return false;
        final java.lang.Object this$afterValue = this.getAfterValue();
        final java.lang.Object other$afterValue = other.getAfterValue();
        if (this$afterValue == null ? other$afterValue != null : !this$afterValue.equals(other$afterValue)) return false;
        final java.lang.Object this$tenantId = this.getTenantId();
        final java.lang.Object other$tenantId = other.getTenantId();
        if (this$tenantId == null ? other$tenantId != null : !this$tenantId.equals(other$tenantId)) return false;
        final java.lang.Object this$requestId = this.getRequestId();
        final java.lang.Object other$requestId = other.getRequestId();
        if (this$requestId == null ? other$requestId != null : !this$requestId.equals(other$requestId)) return false;
        final java.lang.Object this$userId = this.getUserId();
        final java.lang.Object other$userId = other.getUserId();
        if (this$userId == null ? other$userId != null : !this$userId.equals(other$userId)) return false;
        final java.lang.Object this$username = this.getUsername();
        final java.lang.Object other$username = other.getUsername();
        if (this$username == null ? other$username != null : !this$username.equals(other$username)) return false;
        final java.lang.Object this$operation = this.getOperation();
        final java.lang.Object other$operation = other.getOperation();
        if (this$operation == null ? other$operation != null : !this$operation.equals(other$operation)) return false;
        final java.lang.Object this$module = this.getModule();
        final java.lang.Object other$module = other.getModule();
        if (this$module == null ? other$module != null : !this$module.equals(other$module)) return false;
        final java.lang.Object this$operationType = this.getOperationType();
        final java.lang.Object other$operationType = other.getOperationType();
        if (this$operationType == null ? other$operationType != null : !this$operationType.equals(other$operationType)) return false;
        final java.lang.Object this$ip = this.getIp();
        final java.lang.Object other$ip = other.getIp();
        if (this$ip == null ? other$ip != null : !this$ip.equals(other$ip)) return false;
        final java.lang.Object this$userAgent = this.getUserAgent();
        final java.lang.Object other$userAgent = other.getUserAgent();
        if (this$userAgent == null ? other$userAgent != null : !this$userAgent.equals(other$userAgent)) return false;
        final java.lang.Object this$requestUrl = this.getRequestUrl();
        final java.lang.Object other$requestUrl = other.getRequestUrl();
        if (this$requestUrl == null ? other$requestUrl != null : !this$requestUrl.equals(other$requestUrl)) return false;
        final java.lang.Object this$requestMethod = this.getRequestMethod();
        final java.lang.Object other$requestMethod = other.getRequestMethod();
        if (this$requestMethod == null ? other$requestMethod != null : !this$requestMethod.equals(other$requestMethod)) return false;
        final java.lang.Object this$requestParams = this.getRequestParams();
        final java.lang.Object other$requestParams = other.getRequestParams();
        if (this$requestParams == null ? other$requestParams != null : !this$requestParams.equals(other$requestParams)) return false;
        final java.lang.Object this$responseStatus = this.getResponseStatus();
        final java.lang.Object other$responseStatus = other.getResponseStatus();
        if (this$responseStatus == null ? other$responseStatus != null : !this$responseStatus.equals(other$responseStatus)) return false;
        final java.lang.Object this$responseBody = this.getResponseBody();
        final java.lang.Object other$responseBody = other.getResponseBody();
        if (this$responseBody == null ? other$responseBody != null : !this$responseBody.equals(other$responseBody)) return false;
        final java.lang.Object this$errorMessage = this.getErrorMessage();
        final java.lang.Object other$errorMessage = other.getErrorMessage();
        if (this$errorMessage == null ? other$errorMessage != null : !this$errorMessage.equals(other$errorMessage)) return false;
        final java.lang.Object this$businessKey = this.getBusinessKey();
        final java.lang.Object other$businessKey = other.getBusinessKey();
        if (this$businessKey == null ? other$businessKey != null : !this$businessKey.equals(other$businessKey)) return false;
        final java.lang.Object this$riskLevel = this.getRiskLevel();
        final java.lang.Object other$riskLevel = other.getRiskLevel();
        if (this$riskLevel == null ? other$riskLevel != null : !this$riskLevel.equals(other$riskLevel)) return false;
        final java.lang.Object this$sessionId = this.getSessionId();
        final java.lang.Object other$sessionId = other.getSessionId();
        if (this$sessionId == null ? other$sessionId != null : !this$sessionId.equals(other$sessionId)) return false;
        final java.lang.Object this$createdAt = this.getCreatedAt();
        final java.lang.Object other$createdAt = other.getCreatedAt();
        if (this$createdAt == null ? other$createdAt != null : !this$createdAt.equals(other$createdAt)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof AuditLog;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $executionTime = this.getExecutionTime();
        result = result * PRIME + ($executionTime == null ? 43 : $executionTime.hashCode());
        final java.lang.Object $sensitiveFlag = this.getSensitiveFlag();
        result = result * PRIME + ($sensitiveFlag == null ? 43 : $sensitiveFlag.hashCode());
        final java.lang.Object $beforeValue = this.getBeforeValue();
        result = result * PRIME + ($beforeValue == null ? 43 : $beforeValue.hashCode());
        final java.lang.Object $afterValue = this.getAfterValue();
        result = result * PRIME + ($afterValue == null ? 43 : $afterValue.hashCode());
        final java.lang.Object $tenantId = this.getTenantId();
        result = result * PRIME + ($tenantId == null ? 43 : $tenantId.hashCode());
        final java.lang.Object $requestId = this.getRequestId();
        result = result * PRIME + ($requestId == null ? 43 : $requestId.hashCode());
        final java.lang.Object $userId = this.getUserId();
        result = result * PRIME + ($userId == null ? 43 : $userId.hashCode());
        final java.lang.Object $username = this.getUsername();
        result = result * PRIME + ($username == null ? 43 : $username.hashCode());
        final java.lang.Object $operation = this.getOperation();
        result = result * PRIME + ($operation == null ? 43 : $operation.hashCode());
        final java.lang.Object $module = this.getModule();
        result = result * PRIME + ($module == null ? 43 : $module.hashCode());
        final java.lang.Object $operationType = this.getOperationType();
        result = result * PRIME + ($operationType == null ? 43 : $operationType.hashCode());
        final java.lang.Object $ip = this.getIp();
        result = result * PRIME + ($ip == null ? 43 : $ip.hashCode());
        final java.lang.Object $userAgent = this.getUserAgent();
        result = result * PRIME + ($userAgent == null ? 43 : $userAgent.hashCode());
        final java.lang.Object $requestUrl = this.getRequestUrl();
        result = result * PRIME + ($requestUrl == null ? 43 : $requestUrl.hashCode());
        final java.lang.Object $requestMethod = this.getRequestMethod();
        result = result * PRIME + ($requestMethod == null ? 43 : $requestMethod.hashCode());
        final java.lang.Object $requestParams = this.getRequestParams();
        result = result * PRIME + ($requestParams == null ? 43 : $requestParams.hashCode());
        final java.lang.Object $responseStatus = this.getResponseStatus();
        result = result * PRIME + ($responseStatus == null ? 43 : $responseStatus.hashCode());
        final java.lang.Object $responseBody = this.getResponseBody();
        result = result * PRIME + ($responseBody == null ? 43 : $responseBody.hashCode());
        final java.lang.Object $errorMessage = this.getErrorMessage();
        result = result * PRIME + ($errorMessage == null ? 43 : $errorMessage.hashCode());
        final java.lang.Object $businessKey = this.getBusinessKey();
        result = result * PRIME + ($businessKey == null ? 43 : $businessKey.hashCode());
        final java.lang.Object $riskLevel = this.getRiskLevel();
        result = result * PRIME + ($riskLevel == null ? 43 : $riskLevel.hashCode());
        final java.lang.Object $sessionId = this.getSessionId();
        result = result * PRIME + ($sessionId == null ? 43 : $sessionId.hashCode());
        final java.lang.Object $createdAt = this.getCreatedAt();
        result = result * PRIME + ($createdAt == null ? 43 : $createdAt.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "AuditLog(id=" + this.getId() + ", tenantId=" + this.getTenantId() + ", requestId=" + this.getRequestId() + ", userId=" + this.getUserId() + ", username=" + this.getUsername() + ", operation=" + this.getOperation() + ", module=" + this.getModule() + ", operationType=" + this.getOperationType() + ", ip=" + this.getIp() + ", userAgent=" + this.getUserAgent() + ", requestUrl=" + this.getRequestUrl() + ", requestMethod=" + this.getRequestMethod() + ", requestParams=" + this.getRequestParams() + ", responseStatus=" + this.getResponseStatus() + ", responseBody=" + this.getResponseBody() + ", errorMessage=" + this.getErrorMessage() + ", executionTime=" + this.getExecutionTime() + ", businessKey=" + this.getBusinessKey() + ", riskLevel=" + this.getRiskLevel() + ", sensitiveFlag=" + this.getSensitiveFlag() + ", beforeValue=" + this.getBeforeValue() + ", afterValue=" + this.getAfterValue() + ", sessionId=" + this.getSessionId() + ", createdAt=" + this.getCreatedAt() + ")";
    }
}
