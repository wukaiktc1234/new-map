package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 操作日志实体类
 * 用于记录用户操作行为，支持审计追踪
 */
@TableName("sys_operation_logs")
@Schema(description = "操作日志实体")
public class OperationLogEntity {

    /**
     * 日志ID
     */
    @TableId(type = IdType.AUTO, value = "id")
    @Schema(description = "日志ID", example = "1")
    private Long id;

    /**
     * 操作类型
     */
    @TableField("operation_type")
    @Schema(description = "操作类型", example = "CREATE")
    private String operationType;

    /**
     * 操作模块
     */
    @TableField("operation_module")
    @Schema(description = "操作模块", example = "用户管理")
    private String operationModule;

    /**
     * 操作描述
     */
    @TableField("operation_desc")
    @Schema(description = "操作描述", example = "创建用户")
    private String operationDesc;

    /**
     * 请求方法
     */
    @TableField("request_method")
    @Schema(description = "请求方法", example = "POST")
    private String requestMethod;

    /**
     * 请求URL
     */
    @TableField("request_url")
    @Schema(description = "请求URL", example = "/api/v1/users")
    private String requestUrl;

    /**
     * 请求参数
     */
    @TableField("request_params")
    @Schema(description = "请求参数", example = "{\"username\":\"admin\"}")
    private String requestParams;

    /**
     * 响应结果
     */
    @TableField("response_result")
    @Schema(description = "响应结果", example = "{\"code\":0}")
    private String responseResult;

    /**
     * 操作人ID
     */
    @TableField("operator_id")
    @Schema(description = "操作人ID", example = "1")
    private String operatorId;

    /**
     * 操作人用户名
     */
    @TableField("operator_name")
    @Schema(description = "操作人用户名", example = "admin")
    private String operatorName;

    /**
     * 操作IP地址
     */
    @TableField("operator_ip")
    @Schema(description = "操作IP地址", example = "192.168.1.1")
    private String operatorIp;

    /**
     * 操作状态
     */
    @TableField("status")
    @Schema(description = "操作状态", example = "SUCCESS")
    private String status;

    /**
     * 错误信息
     */
    @TableField("error_msg")
    @Schema(description = "错误信息", example = "")
    private String errorMsg;

    /**
     * 操作时间
     */
    @TableField(value = "operation_time", fill = FieldFill.INSERT)
    @Schema(description = "操作时间")
    private LocalDateTime operationTime;

    /**
     * 执行时长（毫秒）
     */
    @TableField("duration")
    @Schema(description = "执行时长（毫秒）", example = "100")
    private Long duration;

    /**
     * 业务ID
     */
    @TableField("business_id")
    @Schema(description = "业务ID", example = "123456")
    private String businessId;

    /**
     * 逻辑删除标记
     */
    @TableLogic
    @TableField("deleted")
    @Schema(description = "逻辑删除标记", example = "0")
    private Integer deleted;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    public String getOperationModule() {
        return operationModule;
    }

    public void setOperationModule(String operationModule) {
        this.operationModule = operationModule;
    }

    public String getOperationDesc() {
        return operationDesc;
    }

    public void setOperationDesc(String operationDesc) {
        this.operationDesc = operationDesc;
    }

    public String getRequestMethod() {
        return requestMethod;
    }

    public void setRequestMethod(String requestMethod) {
        this.requestMethod = requestMethod;
    }

    public String getRequestUrl() {
        return requestUrl;
    }

    public void setRequestUrl(String requestUrl) {
        this.requestUrl = requestUrl;
    }

    public String getRequestParams() {
        return requestParams;
    }

    public void setRequestParams(String requestParams) {
        this.requestParams = requestParams;
    }

    public String getResponseResult() {
        return responseResult;
    }

    public void setResponseResult(String responseResult) {
        this.responseResult = responseResult;
    }

    public String getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(String operatorId) {
        this.operatorId = operatorId;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
    }

    public String getOperatorIp() {
        return operatorIp;
    }

    public void setOperatorIp(String operatorIp) {
        this.operatorIp = operatorIp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    public LocalDateTime getOperationTime() {
        return operationTime;
    }

    public void setOperationTime(LocalDateTime operationTime) {
        this.operationTime = operationTime;
    }

    public Long getDuration() {
        return duration;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }

    public String getBusinessId() {
        return businessId;
    }

    public void setBusinessId(String businessId) {
        this.businessId = businessId;
    }

    public String getRecordId() {
        return businessId;
    }

    public void setRecordId(String recordId) {
        this.businessId = recordId;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }
}
