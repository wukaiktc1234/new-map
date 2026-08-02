package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 密码策略DTO
 */
@Schema(description = "密码策略数据传输对象")
public class PasswordPolicyDTO {

    @Schema(description = "策略名称", example = "默认密码策略")
    @NotBlank(message = "策略名称不能为空")
    @Size(max = 100, message = "策略名称长度不能超过100")
    private String policyName;

    @Schema(description = "最小密码长度", example = "8")
    @NotNull(message = "最小密码长度不能为空")
    @Min(value = 4, message = "最小长度不能小于4")
    @Max(value = 32, message = "最小长度不能超过32")
    private Integer minLength;

    @Schema(description = "最大密码长度", example = "32")
    @NotNull(message = "最大密码长度不能为空")
    @Min(value = 8, message = "最大长度不能小于8")
    @Max(value = 128, message = "最大长度不能超过128")
    private Integer maxLength;

    @Schema(description = "是否要求大写字母", example = "true")
    @NotNull(message = "是否要求大写字母不能为空")
    private Boolean requireUppercase;

    @Schema(description = "是否要求小写字母", example = "true")
    @NotNull(message = "是否要求小写字母不能为空")
    private Boolean requireLowercase;

    @Schema(description = "是否要求数字", example = "true")
    @NotNull(message = "是否要求数字不能为空")
    private Boolean requireDigit;

    @Schema(description = "是否要求特殊字符", example = "true")
    @NotNull(message = "是否要求特殊字符不能为空")
    private Boolean requireSpecial;

    @Schema(description = "密码过期天数", example = "90")
    @NotNull(message = "密码过期天数不能为空")
    @Min(value = 0, message = "过期天数不能小于0")
    @Max(value = 3650, message = "过期天数不能超过10年")
    private Integer passwordExpiryDays;

    @Schema(description = "历史密码检查次数", example = "5")
    @NotNull(message = "历史密码检查次数不能为空")
    @Min(value = 0, message = "检查次数不能小于0")
    @Max(value = 20, message = "检查次数不能超过20")
    private Integer historyCheckCount;

    @Schema(description = "最大登录失败次数", example = "5")
    @NotNull(message = "最大登录失败次数不能为空")
    @Min(value = 1, message = "失败次数不能小于1")
    @Max(value = 20, message = "失败次数不能超过20")
    private Integer maxLoginFailures;

    @Schema(description = "锁定时间（分钟）", example = "30")
    @NotNull(message = "锁定时间不能为空")
    @Min(value = 1, message = "锁定时间不能小于1")
    @Max(value = 1440, message = "锁定时间不能超过24小时")
    private Integer lockDurationMinutes;

    /**
     * 验证密码策略参数合理性
     */
    @jakarta.validation.constraints.AssertTrue(message = "最小长度不能大于最大长度")
    public boolean isValidLengthRange() {
        if (minLength == null || maxLength == null) {
            return true;
        }
        return minLength <= maxLength;
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
}
