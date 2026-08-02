package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 批量生成邀请码DTO
 */
@Schema(description = "批量生成邀请码DTO")
public class BatchGenerateInvitationCodeDTO {
    @Schema(description = "生成数量", example = "10")
    private Integer count;
    @Schema(description = "邀请人ID")
    private Long inviterId;
    @Schema(description = "预设角色ID")
    private String roleId;
    @Schema(description = "预设部门ID")
    private String departmentId;
    @Schema(description = "预设门店ID")
    private String storeId;
    @Schema(description = "过期天数", example = "7")
    private Integer expireDays;
    @Schema(description = "备注")
    private String remark;

    public BatchGenerateInvitationCodeDTO() {
    }

    public Integer getCount() {
        return this.count;
    }

    public Long getInviterId() {
        return this.inviterId;
    }

    public String getRoleId() {
        return this.roleId;
    }

    public String getDepartmentId() {
        return this.departmentId;
    }

    public String getStoreId() {
        return this.storeId;
    }

    public Integer getExpireDays() {
        return this.expireDays;
    }

    public String getRemark() {
        return this.remark;
    }

    public void setCount(final Integer count) {
        this.count = count;
    }

    public void setInviterId(final Long inviterId) {
        this.inviterId = inviterId;
    }

    public void setRoleId(final String roleId) {
        this.roleId = roleId;
    }

    public void setDepartmentId(final String departmentId) {
        this.departmentId = departmentId;
    }

    public void setStoreId(final String storeId) {
        this.storeId = storeId;
    }

    public void setExpireDays(final Integer expireDays) {
        this.expireDays = expireDays;
    }

    public void setRemark(final String remark) {
        this.remark = remark;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof BatchGenerateInvitationCodeDTO)) return false;
        final BatchGenerateInvitationCodeDTO other = (BatchGenerateInvitationCodeDTO) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$count = this.getCount();
        final java.lang.Object other$count = other.getCount();
        if (this$count == null ? other$count != null : !this$count.equals(other$count)) return false;
        final java.lang.Object this$inviterId = this.getInviterId();
        final java.lang.Object other$inviterId = other.getInviterId();
        if (this$inviterId == null ? other$inviterId != null : !this$inviterId.equals(other$inviterId)) return false;
        final java.lang.Object this$expireDays = this.getExpireDays();
        final java.lang.Object other$expireDays = other.getExpireDays();
        if (this$expireDays == null ? other$expireDays != null : !this$expireDays.equals(other$expireDays)) return false;
        final java.lang.Object this$roleId = this.getRoleId();
        final java.lang.Object other$roleId = other.getRoleId();
        if (this$roleId == null ? other$roleId != null : !this$roleId.equals(other$roleId)) return false;
        final java.lang.Object this$departmentId = this.getDepartmentId();
        final java.lang.Object other$departmentId = other.getDepartmentId();
        if (this$departmentId == null ? other$departmentId != null : !this$departmentId.equals(other$departmentId)) return false;
        final java.lang.Object this$storeId = this.getStoreId();
        final java.lang.Object other$storeId = other.getStoreId();
        if (this$storeId == null ? other$storeId != null : !this$storeId.equals(other$storeId)) return false;
        final java.lang.Object this$remark = this.getRemark();
        final java.lang.Object other$remark = other.getRemark();
        if (this$remark == null ? other$remark != null : !this$remark.equals(other$remark)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof BatchGenerateInvitationCodeDTO;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $count = this.getCount();
        result = result * PRIME + ($count == null ? 43 : $count.hashCode());
        final java.lang.Object $inviterId = this.getInviterId();
        result = result * PRIME + ($inviterId == null ? 43 : $inviterId.hashCode());
        final java.lang.Object $expireDays = this.getExpireDays();
        result = result * PRIME + ($expireDays == null ? 43 : $expireDays.hashCode());
        final java.lang.Object $roleId = this.getRoleId();
        result = result * PRIME + ($roleId == null ? 43 : $roleId.hashCode());
        final java.lang.Object $departmentId = this.getDepartmentId();
        result = result * PRIME + ($departmentId == null ? 43 : $departmentId.hashCode());
        final java.lang.Object $storeId = this.getStoreId();
        result = result * PRIME + ($storeId == null ? 43 : $storeId.hashCode());
        final java.lang.Object $remark = this.getRemark();
        result = result * PRIME + ($remark == null ? 43 : $remark.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "BatchGenerateInvitationCodeDTO(count=" + this.getCount() + ", inviterId=" + this.getInviterId() + ", roleId=" + this.getRoleId() + ", departmentId=" + this.getDepartmentId() + ", storeId=" + this.getStoreId() + ", expireDays=" + this.getExpireDays() + ", remark=" + this.getRemark() + ")";
    }
}
