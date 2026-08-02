package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 修改密码请求DTO
 */
@Schema(description = "修改密码请求")
public class ChangePasswordRequest {
    @Schema(description = "旧密码", example = "oldpassword123")
    private String oldPassword;
    @Schema(description = "新密码", example = "newpassword123")
    private String newPassword;
    @Schema(description = "确认新密码", example = "newpassword123")
    private String confirmPassword;

    public String getNewPassword() {
        return this.newPassword;
    }

    public String getConfirmPassword() {
        return this.confirmPassword;
    }

    public String getOldPassword() {
        return this.oldPassword;
    }

    public ChangePasswordRequest() {
    }

    public void setOldPassword(final String oldPassword) {
        this.oldPassword = oldPassword;
    }

    public void setNewPassword(final String newPassword) {
        this.newPassword = newPassword;
    }

    public void setConfirmPassword(final String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof ChangePasswordRequest)) return false;
        final ChangePasswordRequest other = (ChangePasswordRequest) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$oldPassword = this.getOldPassword();
        final java.lang.Object other$oldPassword = other.getOldPassword();
        if (this$oldPassword == null ? other$oldPassword != null : !this$oldPassword.equals(other$oldPassword)) return false;
        final java.lang.Object this$newPassword = this.getNewPassword();
        final java.lang.Object other$newPassword = other.getNewPassword();
        if (this$newPassword == null ? other$newPassword != null : !this$newPassword.equals(other$newPassword)) return false;
        final java.lang.Object this$confirmPassword = this.getConfirmPassword();
        final java.lang.Object other$confirmPassword = other.getConfirmPassword();
        if (this$confirmPassword == null ? other$confirmPassword != null : !this$confirmPassword.equals(other$confirmPassword)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof ChangePasswordRequest;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $oldPassword = this.getOldPassword();
        result = result * PRIME + ($oldPassword == null ? 43 : $oldPassword.hashCode());
        final java.lang.Object $newPassword = this.getNewPassword();
        result = result * PRIME + ($newPassword == null ? 43 : $newPassword.hashCode());
        final java.lang.Object $confirmPassword = this.getConfirmPassword();
        result = result * PRIME + ($confirmPassword == null ? 43 : $confirmPassword.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "ChangePasswordRequest(oldPassword=" + this.getOldPassword() + ", newPassword=" + this.getNewPassword() + ", confirmPassword=" + this.getConfirmPassword() + ")";
    }
}
