package com.foodtraceability.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 发送验证码请求DTO
 */
public class SendVerificationCodeRequest {
    /**
     * 用户名或邮箱
     */
    @NotBlank(message = "用户名或邮箱不能为空")
    private String usernameOrEmail;
    /**
     * 验证码类型：EMAIL / PHONE
     */
    @NotBlank(message = "验证码类型不能为空")
    @Pattern(regexp = "^(EMAIL|PHONE)$", message = "验证码类型必须是EMAIL或PHONE")
    private String type;

    public SendVerificationCodeRequest() {
    }

    /**
     * 用户名或邮箱
     */
    public String getUsernameOrEmail() {
        return this.usernameOrEmail;
    }

    /**
     * 验证码类型：EMAIL / PHONE
     */
    public String getType() {
        return this.type;
    }

    /**
     * 用户名或邮箱
     */
    public void setUsernameOrEmail(final String usernameOrEmail) {
        this.usernameOrEmail = usernameOrEmail;
    }

    /**
     * 验证码类型：EMAIL / PHONE
     */
    public void setType(final String type) {
        this.type = type;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof SendVerificationCodeRequest)) return false;
        final SendVerificationCodeRequest other = (SendVerificationCodeRequest) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$usernameOrEmail = this.getUsernameOrEmail();
        final java.lang.Object other$usernameOrEmail = other.getUsernameOrEmail();
        if (this$usernameOrEmail == null ? other$usernameOrEmail != null : !this$usernameOrEmail.equals(other$usernameOrEmail)) return false;
        final java.lang.Object this$type = this.getType();
        final java.lang.Object other$type = other.getType();
        if (this$type == null ? other$type != null : !this$type.equals(other$type)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof SendVerificationCodeRequest;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $usernameOrEmail = this.getUsernameOrEmail();
        result = result * PRIME + ($usernameOrEmail == null ? 43 : $usernameOrEmail.hashCode());
        final java.lang.Object $type = this.getType();
        result = result * PRIME + ($type == null ? 43 : $type.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "SendVerificationCodeRequest(usernameOrEmail=" + this.getUsernameOrEmail() + ", type=" + this.getType() + ")";
    }
}
