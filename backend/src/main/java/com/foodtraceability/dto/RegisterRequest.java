package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

/**
 * 注册请求DTO
 *
 * 安全增强说明：
 * - 添加了完整的输入校验注解，防止非法数据进入系统
 * - 用户名：3-20位，只允许字母数字下划线
 * - 密码：8-20位，必须包含字母和数字
 * - 邮箱：符合RFC 5322标准格式
 * - 手机号：11位数字，符合中国大陆手机号格式
 */
@Schema(description = "注册请求")
public class RegisterRequest {
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度必须在3-20个字符之间")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
    @Schema(description = "用户名", example = "newuser")
    private String username;
    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 20, message = "密码长度必须在8-20个字符之间")
    @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d).+$", message = "密码必须包含至少一个字母和一个数字")
    @Schema(description = "密码", example = "Password123")
    private String password;
    @NotBlank(message = "确认密码不能为空")
    @Schema(description = "确认密码", example = "Password123")
    private String confirmPassword;
    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱长度不能超过100个字符")
    @Schema(description = "邮箱", example = "newuser@example.com")
    private String email;
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Schema(description = "手机号", example = "13800138000")
    private String phone;
    @Size(min = 4, max = 6, message = "验证码长度必须在4-6个字符之间")
    @Schema(description = "验证码", example = "ABCD")
    private String captcha;
    @NotBlank(message = "验证码ID不能为空")
    @Schema(description = "验证码ID", example = "123456")
    private String captchaId;
    @Size(min = 10, max = 20, message = "注册码长度必须在10-20个字符之间")
    @Schema(description = "注册码", example = "RC202512260001")
    private String registrationCode;
    @Pattern(regexp = "^\\d{6}$", message = "手机验证码必须为6位数字")
    @Schema(description = "手机验证码", example = "123456")
    private String phoneCode;

    public RegisterRequest() {
    }

    public String getUsername() {
        return this.username;
    }

    public String getPassword() {
        return this.password;
    }

    public String getConfirmPassword() {
        return this.confirmPassword;
    }

    public String getEmail() {
        return this.email;
    }

    public String getPhone() {
        return this.phone;
    }

    public String getCaptcha() {
        return this.captcha;
    }

    public String getCaptchaId() {
        return this.captchaId;
    }

    public String getRegistrationCode() {
        return this.registrationCode;
    }

    public String getPhoneCode() {
        return this.phoneCode;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

    public void setConfirmPassword(final String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    public void setEmail(final String email) {
        this.email = email;
    }

    public void setPhone(final String phone) {
        this.phone = phone;
    }

    public void setCaptcha(final String captcha) {
        this.captcha = captcha;
    }

    public void setCaptchaId(final String captchaId) {
        this.captchaId = captchaId;
    }

    public void setRegistrationCode(final String registrationCode) {
        this.registrationCode = registrationCode;
    }

    public void setPhoneCode(final String phoneCode) {
        this.phoneCode = phoneCode;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof RegisterRequest)) return false;
        final RegisterRequest other = (RegisterRequest) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$username = this.getUsername();
        final java.lang.Object other$username = other.getUsername();
        if (this$username == null ? other$username != null : !this$username.equals(other$username)) return false;
        final java.lang.Object this$password = this.getPassword();
        final java.lang.Object other$password = other.getPassword();
        if (this$password == null ? other$password != null : !this$password.equals(other$password)) return false;
        final java.lang.Object this$confirmPassword = this.getConfirmPassword();
        final java.lang.Object other$confirmPassword = other.getConfirmPassword();
        if (this$confirmPassword == null ? other$confirmPassword != null : !this$confirmPassword.equals(other$confirmPassword)) return false;
        final java.lang.Object this$email = this.getEmail();
        final java.lang.Object other$email = other.getEmail();
        if (this$email == null ? other$email != null : !this$email.equals(other$email)) return false;
        final java.lang.Object this$phone = this.getPhone();
        final java.lang.Object other$phone = other.getPhone();
        if (this$phone == null ? other$phone != null : !this$phone.equals(other$phone)) return false;
        final java.lang.Object this$captcha = this.getCaptcha();
        final java.lang.Object other$captcha = other.getCaptcha();
        if (this$captcha == null ? other$captcha != null : !this$captcha.equals(other$captcha)) return false;
        final java.lang.Object this$captchaId = this.getCaptchaId();
        final java.lang.Object other$captchaId = other.getCaptchaId();
        if (this$captchaId == null ? other$captchaId != null : !this$captchaId.equals(other$captchaId)) return false;
        final java.lang.Object this$registrationCode = this.getRegistrationCode();
        final java.lang.Object other$registrationCode = other.getRegistrationCode();
        if (this$registrationCode == null ? other$registrationCode != null : !this$registrationCode.equals(other$registrationCode)) return false;
        final java.lang.Object this$phoneCode = this.getPhoneCode();
        final java.lang.Object other$phoneCode = other.getPhoneCode();
        if (this$phoneCode == null ? other$phoneCode != null : !this$phoneCode.equals(other$phoneCode)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof RegisterRequest;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $username = this.getUsername();
        result = result * PRIME + ($username == null ? 43 : $username.hashCode());
        final java.lang.Object $password = this.getPassword();
        result = result * PRIME + ($password == null ? 43 : $password.hashCode());
        final java.lang.Object $confirmPassword = this.getConfirmPassword();
        result = result * PRIME + ($confirmPassword == null ? 43 : $confirmPassword.hashCode());
        final java.lang.Object $email = this.getEmail();
        result = result * PRIME + ($email == null ? 43 : $email.hashCode());
        final java.lang.Object $phone = this.getPhone();
        result = result * PRIME + ($phone == null ? 43 : $phone.hashCode());
        final java.lang.Object $captcha = this.getCaptcha();
        result = result * PRIME + ($captcha == null ? 43 : $captcha.hashCode());
        final java.lang.Object $captchaId = this.getCaptchaId();
        result = result * PRIME + ($captchaId == null ? 43 : $captchaId.hashCode());
        final java.lang.Object $registrationCode = this.getRegistrationCode();
        result = result * PRIME + ($registrationCode == null ? 43 : $registrationCode.hashCode());
        final java.lang.Object $phoneCode = this.getPhoneCode();
        result = result * PRIME + ($phoneCode == null ? 43 : $phoneCode.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "RegisterRequest(username=" + this.getUsername() + ", password=" + this.getPassword() + ", confirmPassword=" + this.getConfirmPassword() + ", email=" + this.getEmail() + ", phone=" + this.getPhone() + ", captcha=" + this.getCaptcha() + ", captchaId=" + this.getCaptchaId() + ", registrationCode=" + this.getRegistrationCode() + ", phoneCode=" + this.getPhoneCode() + ")";
    }
}
