package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 密码策略实体类
 * 用于管理系统密码安全策略配置
 */
@TableName("password_policies")
@Schema(description = "密码策略实体")
public class PasswordPolicy {

    /**
     * 策略ID
     */
    @TableId(type = IdType.AUTO, value = "policy_id")
    @Schema(description = "策略ID", example = "1")
    private Long policyId;

    /**
     * 策略名称
     */
    @TableField("policy_name")
    @Schema(description = "策略名称", example = "默认密码策略")
    private String policyName;

    /**
     * 最小密码长度
     */
    @TableField("min_length")
    @Schema(description = "最小密码长度", example = "8")
    private Integer minLength;

    /**
     * 最大密码长度
     */
    @TableField("max_length")
    @Schema(description = "最大密码长度", example = "32")
    private Integer maxLength;

    /**
     * 是否要求大写字母
     */
    @TableField("require_uppercase")
    @Schema(description = "是否要求大写字母", example = "true")
    private Boolean requireUppercase;

    /**
     * 是否要求小写字母
     */
    @TableField("require_lowercase")
    @Schema(description = "是否要求小写字母", example = "true")
    private Boolean requireLowercase;

    /**
     * 是否要求数字
     */
    @TableField("require_digit")
    @Schema(description = "是否要求数字", example = "true")
    private Boolean requireDigit;

    /**
     * 是否要求特殊字符
     */
    @TableField("require_special")
    @Schema(description = "是否要求特殊字符", example = "true")
    private Boolean requireSpecial;

    /**
     * 密码过期天数（0表示永不过期）
     */
    @TableField("password_expiry_days")
    @Schema(description = "密码过期天数", example = "90")
    private Integer passwordExpiryDays;

    /**
     * 历史密码检查次数（不能重复使用最近N次密码）
     */
    @TableField("history_check_count")
    @Schema(description = "历史密码检查次数", example = "5")
    private Integer historyCheckCount;

    /**
     * 最大登录失败次数
     */
    @TableField("max_login_failures")
    @Schema(description = "最大登录失败次数", example = "5")
    private Integer maxLoginFailures;

    /**
     * 锁定时间（分钟）
     */
    @TableField("lock_duration_minutes")
    @Schema(description = "锁定时间（分钟）", example = "30")
    private Integer lockDurationMinutes;

    /**
     * 是否启用（0-禁用，1-启用）
     */
    @TableField("status")
    @Schema(description = "是否启用", example = "1")
    private Integer status;

    /**
     * 是否为默认策略
     */
    @TableField("is_default")
    @Schema(description = "是否为默认策略", example = "true")
    private Boolean isDefault;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    /**
     * 乐观锁版本号
     */
    @Version
    @TableField("version")
    @Schema(description = "乐观锁版本号", example = "0")
    private Integer version;

    /**
     * 逻辑删除标记
     */
    @TableLogic
    @TableField("deleted")
    @Schema(description = "逻辑删除标记", example = "0")
    private Integer deleted;

    public Long getPolicyId() {
        return policyId;
    }

    public void setPolicyId(Long policyId) {
        this.policyId = policyId;
    }

    public String getPolicyName() {
        return policyName;
    }

    public void setPolicyName(String policyName) {
        this.policyName = policyName;
    }

    public Integer getMinLength() {
        return minLength;
    }

    public void setMinLength(Integer minLength) {
        this.minLength = minLength;
    }

    public Integer getMaxLength() {
        return maxLength;
    }

    public void setMaxLength(Integer maxLength) {
        this.maxLength = maxLength;
    }

    public Boolean getRequireUppercase() {
        return requireUppercase;
    }

    public void setRequireUppercase(Boolean requireUppercase) {
        this.requireUppercase = requireUppercase;
    }

    public Boolean getRequireLowercase() {
        return requireLowercase;
    }

    public void setRequireLowercase(Boolean requireLowercase) {
        this.requireLowercase = requireLowercase;
    }

    public Boolean getRequireDigit() {
        return requireDigit;
    }

    public void setRequireDigit(Boolean requireDigit) {
        this.requireDigit = requireDigit;
    }

    public Boolean getRequireSpecial() {
        return requireSpecial;
    }

    public void setRequireSpecial(Boolean requireSpecial) {
        this.requireSpecial = requireSpecial;
    }

    public Integer getPasswordExpiryDays() {
        return passwordExpiryDays;
    }

    public void setPasswordExpiryDays(Integer passwordExpiryDays) {
        this.passwordExpiryDays = passwordExpiryDays;
    }

    public Integer getHistoryCheckCount() {
        return historyCheckCount;
    }

    public void setHistoryCheckCount(Integer historyCheckCount) {
        this.historyCheckCount = historyCheckCount;
    }

    public Integer getMaxLoginFailures() {
        return maxLoginFailures;
    }

    public void setMaxLoginFailures(Integer maxLoginFailures) {
        this.maxLoginFailures = maxLoginFailures;
    }

    public Integer getLockDurationMinutes() {
        return lockDurationMinutes;
    }

    public void setLockDurationMinutes(Integer lockDurationMinutes) {
        this.lockDurationMinutes = lockDurationMinutes;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }
}
