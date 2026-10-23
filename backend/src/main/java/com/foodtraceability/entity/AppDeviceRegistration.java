package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * APK 设备激活白名单实体
 * 记录已发放激活码的移动设备，白名单设备才允许连接后端
 */
@TableName("app_device_registrations")
@Schema(description = "APK 设备激活白名单")
public class AppDeviceRegistration {

    @TableId(value = "registration_id", type = IdType.AUTO)
    @Schema(description = "登记ID")
    private Long registrationId;

    /** 设备唯一标识（H5 在 localStorage 生成的 UUID，随 X-Device-Id 头上报） */
    @TableField("device_id")
    @Schema(description = "设备唯一标识")
    private String deviceId;

    @TableField("device_name")
    @Schema(description = "设备名称/型号")
    private String deviceName;

    /** 一次性激活码（管理端生成） */
    @TableField("activation_code")
    @Schema(description = "激活码")
    private String activationCode;

    /** 状态：PENDING 待激活 / ACTIVE 已激活 / DISABLED 已禁用 */
    @TableField("status")
    @Schema(description = "状态（PENDING-待激活, ACTIVE-已激活, DISABLED-已禁用）")
    private String status;

    @TableField("bound_username")
    @Schema(description = "激活绑定用户名")
    private String boundUsername;

    @TableField("code_expire_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "激活码过期时间")
    private LocalDateTime codeExpireAt;

    @TableField("activated_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "激活时间")
    private LocalDateTime activatedAt;

    @TableField("last_seen_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "最近一次访问时间")
    private LocalDateTime lastSeenAt;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @TableLogic
    @TableField("deleted")
    @Schema(description = "逻辑删除标记")
    private Integer deleted;

    public Long getRegistrationId() {
        return registrationId;
    }

    public void setRegistrationId(Long registrationId) {
        this.registrationId = registrationId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getActivationCode() {
        return activationCode;
    }

    public void setActivationCode(String activationCode) {
        this.activationCode = activationCode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getBoundUsername() {
        return boundUsername;
    }

    public void setBoundUsername(String boundUsername) {
        this.boundUsername = boundUsername;
    }

    public LocalDateTime getCodeExpireAt() {
        return codeExpireAt;
    }

    public void setCodeExpireAt(LocalDateTime codeExpireAt) {
        this.codeExpireAt = codeExpireAt;
    }

    public LocalDateTime getActivatedAt() {
        return activatedAt;
    }

    public void setActivatedAt(LocalDateTime activatedAt) {
        this.activatedAt = activatedAt;
    }

    public LocalDateTime getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(LocalDateTime lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }
}
