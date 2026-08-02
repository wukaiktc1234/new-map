package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 注册码日志实体类
 */
@TableName("registration_code_log")
@Schema(description = "注册码日志实体")
public class RegistrationCodeLog {

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_UUID, value = "id")
    @Schema(description = "主键ID")
    private String id;

    /**
     * 注册码
     */
    @Schema(description = "注册码")
    private String code;

    @Schema(description = "注册码ID")
    @TableField(value = "code_id")
    private String codeId;

    @Schema(description = "操作类型")
    @TableField(value = "action")
    private String action;

    @Schema(description = "手机号")
    @TableField(value = "phone")
    private String phone;

    /**
     * 用户ID
     */
    @Schema(description = "用户ID")
    @TableField(value = "user_id")
    private String userId;

    /**
     * 用户名
     */
    @Schema(description = "用户名")
    private String username;

    /**
     * 使用时间
     */
    @Schema(description = "使用时间")
    @TableField(value = "use_time")
    private LocalDateTime useTime;

    /**
     * 使用状态（SUCCESS：成功，FAILED：失败）
     */
    @Schema(description = "使用状态")
    @TableField(value = "result")
    private String status;

    @Schema(description = "错误信息")
    @TableField(value = "error_message")
    private String errorMessage;

    /**
     * IP地址
     */
    @Schema(description = "IP地址")
    @TableField(value = "ip_address")
    private String ipAddress;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getCodeId() {
        return codeId;
    }

    public void setCodeId(String codeId) {
        this.codeId = codeId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public LocalDateTime getUseTime() {
        return useTime;
    }

    public void setUseTime(LocalDateTime useTime) {
        this.useTime = useTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}