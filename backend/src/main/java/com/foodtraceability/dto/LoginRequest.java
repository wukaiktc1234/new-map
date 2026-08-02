package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 登录请求DTO
 */
@Schema(description = "登录请求")
public class LoginRequest {
    @NotBlank(message = "用户名不能为空")
    @Schema(description = "用户名", example = "admin")
    private String username;
    @NotBlank(message = "密码不能为空")
    @Schema(description = "密码", example = "123456")
    private String password;
    @Schema(description = "验证码", example = "ABCD")
    private String captcha;
    @Schema(description = "验证码ID", example = "123456")
    private String captchaId;
    @Schema(description = "是否记住我", example = "false")
    private Boolean rememberMe;

    public LoginRequest() {
    }

    public String getUsername() {
        return this.username;
    }

    public String getPassword() {
        return this.password;
    }

    public String getCaptcha() {
        return this.captcha;
    }

    public String getCaptchaId() {
        return this.captchaId;
    }

    public Boolean getRememberMe() {
        return this.rememberMe;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

    public void setCaptcha(final String captcha) {
        this.captcha = captcha;
    }

    public void setCaptchaId(final String captchaId) {
        this.captchaId = captchaId;
    }

    public void setRememberMe(final Boolean rememberMe) {
        this.rememberMe = rememberMe;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof LoginRequest)) return false;
        final LoginRequest other = (LoginRequest) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$rememberMe = this.getRememberMe();
        final java.lang.Object other$rememberMe = other.getRememberMe();
        if (this$rememberMe == null ? other$rememberMe != null : !this$rememberMe.equals(other$rememberMe)) return false;
        final java.lang.Object this$username = this.getUsername();
        final java.lang.Object other$username = other.getUsername();
        if (this$username == null ? other$username != null : !this$username.equals(other$username)) return false;
        final java.lang.Object this$password = this.getPassword();
        final java.lang.Object other$password = other.getPassword();
        if (this$password == null ? other$password != null : !this$password.equals(other$password)) return false;
        final java.lang.Object this$captcha = this.getCaptcha();
        final java.lang.Object other$captcha = other.getCaptcha();
        if (this$captcha == null ? other$captcha != null : !this$captcha.equals(other$captcha)) return false;
        final java.lang.Object this$captchaId = this.getCaptchaId();
        final java.lang.Object other$captchaId = other.getCaptchaId();
        if (this$captchaId == null ? other$captchaId != null : !this$captchaId.equals(other$captchaId)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof LoginRequest;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $rememberMe = this.getRememberMe();
        result = result * PRIME + ($rememberMe == null ? 43 : $rememberMe.hashCode());
        final java.lang.Object $username = this.getUsername();
        result = result * PRIME + ($username == null ? 43 : $username.hashCode());
        final java.lang.Object $password = this.getPassword();
        result = result * PRIME + ($password == null ? 43 : $password.hashCode());
        final java.lang.Object $captcha = this.getCaptcha();
        result = result * PRIME + ($captcha == null ? 43 : $captcha.hashCode());
        final java.lang.Object $captchaId = this.getCaptchaId();
        result = result * PRIME + ($captchaId == null ? 43 : $captchaId.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "LoginRequest(username=" + this.getUsername() + ", password=" + this.getPassword() + ", captcha=" + this.getCaptcha() + ", captchaId=" + this.getCaptchaId() + ", rememberMe=" + this.getRememberMe() + ")";
    }
}
