package com.foodtraceability.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 忘记密码请求DTO - 第一步：验证身份
 */
public class ForgotPasswordRequest {
    /**
     * 用户名或邮箱
     */
    @NotBlank(message = "用户名或邮箱不能为空")
    private String usernameOrEmail;
    /**
     * 邮箱验证码
     */
    @NotBlank(message = "邮箱验证码不能为空")
    private String emailCode;
    /**
     * 手机验证码
     */
    @NotBlank(message = "手机验证码不能为空")
    private String phoneCode;

    public ForgotPasswordRequest() {
    }

    /**
     * 用户名或邮箱
     */
    public String getUsernameOrEmail() {
        return this.usernameOrEmail;
    }

    /**
     * 邮箱验证码
     */
    public String getEmailCode() {
        return this.emailCode;
    }

    /**
     * 手机验证码
     */
    public String getPhoneCode() {
        return this.phoneCode;
    }

    /**
     * 用户名或邮箱
     */
    public void setUsernameOrEmail(final String usernameOrEmail) {
        this.usernameOrEmail = usernameOrEmail;
    }

    /**
     * 邮箱验证码
     */
    public void setEmailCode(final String emailCode) {
        this.emailCode = emailCode;
    }

    /**
     * 手机验证码
     */
    public void setPhoneCode(final String phoneCode) {
        this.phoneCode = phoneCode;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof ForgotPasswordRequest)) return false;
        final ForgotPasswordRequest other = (ForgotPasswordRequest) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$usernameOrEmail = this.getUsernameOrEmail();
        final java.lang.Object other$usernameOrEmail = other.getUsernameOrEmail();
        if (this$usernameOrEmail == null ? other$usernameOrEmail != null : !this$usernameOrEmail.equals(other$usernameOrEmail)) return false;
        final java.lang.Object this$emailCode = this.getEmailCode();
        final java.lang.Object other$emailCode = other.getEmailCode();
        if (this$emailCode == null ? other$emailCode != null : !this$emailCode.equals(other$emailCode)) return false;
        final java.lang.Object this$phoneCode = this.getPhoneCode();
        final java.lang.Object other$phoneCode = other.getPhoneCode();
        if (this$phoneCode == null ? other$phoneCode != null : !this$phoneCode.equals(other$phoneCode)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof ForgotPasswordRequest;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $usernameOrEmail = this.getUsernameOrEmail();
        result = result * PRIME + ($usernameOrEmail == null ? 43 : $usernameOrEmail.hashCode());
        final java.lang.Object $emailCode = this.getEmailCode();
        result = result * PRIME + ($emailCode == null ? 43 : $emailCode.hashCode());
        final java.lang.Object $phoneCode = this.getPhoneCode();
        result = result * PRIME + ($phoneCode == null ? 43 : $phoneCode.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "ForgotPasswordRequest(usernameOrEmail=" + this.getUsernameOrEmail() + ", emailCode=" + this.getEmailCode() + ", phoneCode=" + this.getPhoneCode() + ")";
    }
}
