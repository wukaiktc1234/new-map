package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 操作日志实体类
 * 用于记录用户操作行为，支持审计追踪
 */
@TableName("operation_logs")
@Schema(description = "操作日志实体")
public class OperationLog {

    /**
     * 日志ID
     */
    @TableId(type = IdType.AUTO, value = "log_id")
    @Schema(description = "日志ID", example = "1")
    private Long logId;

    /**
     * 操作模块
     */
    @TableField("module")
    @Schema(description = "操作模块", example = "用户管理")
    private String module;

    /**
     * 操作类型
     */
    @TableField("operation_type")
    @Schema(description = "操作类型", example = "CREATE")
    private String operationType;

    /**
     * 操作描述
     */
    @TableField("description")
    @Schema(description = "操作描述", example = "创建用户")
    private String description;

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
    private Long operatorId;

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
     * 操作地点
     */
    @TableField("operator_location")
    @Schema(description = "操作地点", example = "北京市")
    private String operatorLocation;

    /**
     * 操作状态（0-失败，1-成功）
     */
    @TableField("status")
    @Schema(description = "操作状态", example = "1")
    private Integer status;

    /**
     * 错误信息
     */
    @TableField("error_msg")
    @Schema(description = "错误信息", example = "")
    private String errorMsg;

    /**
     * 执行时长（毫秒）
     */
    @TableField("duration")
    @Schema(description = "执行时长（毫秒）", example = "100")
    private Long duration;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    public Long getLogId() {
        return logId;
    }

    public void setLogId(Long logId) {
        this.logId = logId;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public Long getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(Long operatorId) {
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

    public String getOperatorLocation() {
        return operatorLocation;
    }

    public void setOperatorLocation(String operatorLocation) {
        this.operatorLocation = operatorLocation;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    public Long getDuration() {
        return duration;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
