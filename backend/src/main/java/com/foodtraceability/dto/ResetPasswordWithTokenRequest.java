package com.foodtraceability.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 重置密码请求DTO - 第二步：设置新密码
 */
public class ResetPasswordWithTokenRequest {
    /**
     * 重置令牌
     */
    @NotBlank(message = "重置令牌不能为空")
    private String resetToken;
    /**
     * 新密码
     */
    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, max = 32, message = "密码长度必须在8-32位之间")
    private String newPassword;
    /**
     * 确认新密码
     */
    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;

    public ResetPasswordWithTokenRequest() {
    }

    /**
     * 重置令牌
     */
    public String getResetToken() {
        return this.resetToken;
    }

    /**
     * 新密码
     */
    public String getNewPassword() {
        return this.newPassword;
    }

    /**
     * 确认新密码
     */
    public String getConfirmPassword() {
        return this.confirmPassword;
    }

    /**
     * 重置令牌
     */
    public void setResetToken(final String resetToken) {
        this.resetToken = resetToken;
    }

    /**
     * 新密码
     */
    public void setNewPassword(final String newPassword) {
        this.newPassword = newPassword;
    }

    /**
     * 确认新密码
     */
    public void setConfirmPassword(final String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof ResetPasswordWithTokenRequest)) return false;
        final ResetPasswordWithTokenRequest other = (ResetPasswordWithTokenRequest) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$resetToken = this.getResetToken();
        final java.lang.Object other$resetToken = other.getResetToken();
        if (this$resetToken == null ? other$resetToken != null : !this$resetToken.equals(other$resetToken)) return false;
        final java.lang.Object this$newPassword = this.getNewPassword();
        final java.lang.Object other$newPassword = other.getNewPassword();
        if (this$newPassword == null ? other$newPassword != null : !this$newPassword.equals(other$newPassword)) return false;
        final java.lang.Object this$confirmPassword = this.getConfirmPassword();
        final java.lang.Object other$confirmPassword = other.getConfirmPassword();
        if (this$confirmPassword == null ? other$confirmPassword != null : !this$confirmPassword.equals(other$confirmPassword)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof ResetPasswordWithTokenRequest;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $resetToken = this.getResetToken();
        result = result * PRIME + ($resetToken == null ? 43 : $resetToken.hashCode());
        final java.lang.Object $newPassword = this.getNewPassword();
        result = result * PRIME + ($newPassword == null ? 43 : $newPassword.hashCode());
        final java.lang.Object $confirmPassword = this.getConfirmPassword();
        result = result * PRIME + ($confirmPassword == null ? 43 : $confirmPassword.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "ResetPasswordWithTokenRequest(resetToken=" + this.getResetToken() + ", newPassword=" + this.getNewPassword() + ", confirmPassword=" + this.getConfirmPassword() + ")";
    }
}
