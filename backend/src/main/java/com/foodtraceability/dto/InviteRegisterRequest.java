package com.foodtraceability.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 邀请注册请求DTO
 */
public class InviteRegisterRequest extends RegisterRequest {
    /**
     * 邀请码
     */
    @NotBlank(message = "邀请码不能为空")
    private String invitationCode;
    /**
     * 真实姓名
     */
    @NotBlank(message = "真实姓名不能为空")
    @Size(max = 50, message = "真实姓名长度不能超过50位")
    private String fullName;

    public InviteRegisterRequest() {
    }

    /**
     * 邀请码
     */
    public String getInvitationCode() {
        return this.invitationCode;
    }

    /**
     * 真实姓名
     */
    public String getFullName() {
        return this.fullName;
    }

    /**
     * 邀请码
     */
    public void setInvitationCode(final String invitationCode) {
        this.invitationCode = invitationCode;
    }

    /**
     * 真实姓名
     */
    public void setFullName(final String fullName) {
        this.fullName = fullName;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "InviteRegisterRequest(invitationCode=" + this.getInvitationCode() + ", fullName=" + this.getFullName() + ")";
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof InviteRegisterRequest)) return false;
        final InviteRegisterRequest other = (InviteRegisterRequest) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$invitationCode = this.getInvitationCode();
        final java.lang.Object other$invitationCode = other.getInvitationCode();
        if (this$invitationCode == null ? other$invitationCode != null : !this$invitationCode.equals(other$invitationCode)) return false;
        final java.lang.Object this$fullName = this.getFullName();
        final java.lang.Object other$fullName = other.getFullName();
        if (this$fullName == null ? other$fullName != null : !this$fullName.equals(other$fullName)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof InviteRegisterRequest;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $invitationCode = this.getInvitationCode();
        result = result * PRIME + ($invitationCode == null ? 43 : $invitationCode.hashCode());
        final java.lang.Object $fullName = this.getFullName();
        result = result * PRIME + ($fullName == null ? 43 : $fullName.hashCode());
        return result;
    }
}
