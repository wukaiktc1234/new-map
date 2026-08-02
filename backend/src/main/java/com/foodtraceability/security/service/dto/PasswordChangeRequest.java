package com.foodtraceability.security.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class PasswordChangeRequest {
    @NotBlank(message = "旧密码不能为空")
    private String oldPassword;
    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, message = "新密码长度至少8位")
    private String newPassword;
    @NotBlank(message = "确认新密码不能为空")
    private String confirmNewPassword;

    // Getter methods
    public String getOldPassword() {
        return oldPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public String getConfirmNewPassword() {
        return confirmNewPassword;
    }

    // Setter methods
    public void setOldPassword(String oldPassword) {
        this.oldPassword = oldPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public void setConfirmNewPassword(String confirmNewPassword) {
        this.confirmNewPassword = confirmNewPassword;
    }

    public PasswordChangeRequest() {
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof PasswordChangeRequest)) return false;
        final PasswordChangeRequest other = (PasswordChangeRequest) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$oldPassword = this.getOldPassword();
        final java.lang.Object other$oldPassword = other.getOldPassword();
        if (this$oldPassword == null ? other$oldPassword != null : !this$oldPassword.equals(other$oldPassword)) return false;
        final java.lang.Object this$newPassword = this.getNewPassword();
        final java.lang.Object other$newPassword = other.getNewPassword();
        if (this$newPassword == null ? other$newPassword != null : !this$newPassword.equals(other$newPassword)) return false;
        final java.lang.Object this$confirmNewPassword = this.getConfirmNewPassword();
        final java.lang.Object other$confirmNewPassword = other.getConfirmNewPassword();
        if (this$confirmNewPassword == null ? other$confirmNewPassword != null : !this$confirmNewPassword.equals(other$confirmNewPassword)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof PasswordChangeRequest;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $oldPassword = this.getOldPassword();
        result = result * PRIME + ($oldPassword == null ? 43 : $oldPassword.hashCode());
        final java.lang.Object $newPassword = this.getNewPassword();
        result = result * PRIME + ($newPassword == null ? 43 : $newPassword.hashCode());
        final java.lang.Object $confirmNewPassword = this.getConfirmNewPassword();
        result = result * PRIME + ($confirmNewPassword == null ? 43 : $confirmNewPassword.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "PasswordChangeRequest(oldPassword=" + this.getOldPassword() + ", newPassword=" + this.getNewPassword() + ", confirmNewPassword=" + this.getConfirmNewPassword() + ")";
    }
}
