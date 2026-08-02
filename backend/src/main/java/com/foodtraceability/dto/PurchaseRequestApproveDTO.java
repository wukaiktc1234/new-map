package com.foodtraceability.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 采购申请审批DTO
 */
public class PurchaseRequestApproveDTO {
    /**
     * 审批状态：approved(通过)/rejected(拒绝)
     */
    @NotBlank(message = "审批状态不能为空")
    private String status;
    /**
     * 审批备注（拒绝时必填）
     */
    private String remark;

    public PurchaseRequestApproveDTO() {
    }

    /**
     * 审批状态：approved(通过)/rejected(拒绝)
     */
    public String getStatus() {
        return this.status;
    }

    /**
     * 审批备注（拒绝时必填）
     */
    public String getRemark() {
        return this.remark;
    }

    /**
     * 审批状态：approved(通过)/rejected(拒绝)
     */
    public void setStatus(final String status) {
        this.status = status;
    }

    /**
     * 审批备注（拒绝时必填）
     */
    public void setRemark(final String remark) {
        this.remark = remark;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof PurchaseRequestApproveDTO)) return false;
        final PurchaseRequestApproveDTO other = (PurchaseRequestApproveDTO) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
        final java.lang.Object this$remark = this.getRemark();
        final java.lang.Object other$remark = other.getRemark();
        if (this$remark == null ? other$remark != null : !this$remark.equals(other$remark)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof PurchaseRequestApproveDTO;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $remark = this.getRemark();
        result = result * PRIME + ($remark == null ? 43 : $remark.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "PurchaseRequestApproveDTO(status=" + this.getStatus() + ", remark=" + this.getRemark() + ")";
    }
}
