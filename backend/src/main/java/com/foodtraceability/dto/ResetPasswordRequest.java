package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 重置密码请求DTO
 */
@Schema(description = "重置密码请求")
public class ResetPasswordRequest {
    @Schema(description = "邮箱", example = "user@example.com")
    private String email;
    @Schema(description = "验证码", example = "123456")
    private String captcha;
    @Schema(description = "验证码ID", example = "123456")
    private String captchaId;
    @Schema(description = "新密码", example = "newpassword123")
    private String newPassword;

    public String getCaptchaId() {
        return this.captchaId;
    }

    public String getCaptcha() {
        return this.captcha;
    }

    public String getEmail() {
        return this.email;
    }

    public String getNewPassword() {
        return this.newPassword;
    }

    public ResetPasswordRequest() {
    }

    public void setEmail(final String email) {
        this.email = email;
    }

    public void setCaptcha(final String captcha) {
        this.captcha = captcha;
    }

    public void setCaptchaId(final String captchaId) {
        this.captchaId = captchaId;
    }

    public void setNewPassword(final String newPassword) {
        this.newPassword = newPassword;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof ResetPasswordRequest)) return false;
        final ResetPasswordRequest other = (ResetPasswordRequest) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$email = this.getEmail();
        final java.lang.Object other$email = other.getEmail();
        if (this$email == null ? other$email != null : !this$email.equals(other$email)) return false;
        final java.lang.Object this$captcha = this.getCaptcha();
        final java.lang.Object other$captcha = other.getCaptcha();
        if (this$captcha == null ? other$captcha != null : !this$captcha.equals(other$captcha)) return false;
        final java.lang.Object this$captchaId = this.getCaptchaId();
        final java.lang.Object other$captchaId = other.getCaptchaId();
        if (this$captchaId == null ? other$captchaId != null : !this$captchaId.equals(other$captchaId)) return false;
        final java.lang.Object this$newPassword = this.getNewPassword();
        final java.lang.Object other$newPassword = other.getNewPassword();
        if (this$newPassword == null ? other$newPassword != null : !this$newPassword.equals(other$newPassword)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof ResetPasswordRequest;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $email = this.getEmail();
        result = result * PRIME + ($email == null ? 43 : $email.hashCode());
        final java.lang.Object $captcha = this.getCaptcha();
        result = result * PRIME + ($captcha == null ? 43 : $captcha.hashCode());
        final java.lang.Object $captchaId = this.getCaptchaId();
        result = result * PRIME + ($captchaId == null ? 43 : $captchaId.hashCode());
        final java.lang.Object $newPassword = this.getNewPassword();
        result = result * PRIME + ($newPassword == null ? 43 : $newPassword.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "ResetPasswordRequest(email=" + this.getEmail() + ", captcha=" + this.getCaptcha() + ", captchaId=" + this.getCaptchaId() + ", newPassword=" + this.getNewPassword() + ")";
    }
}
