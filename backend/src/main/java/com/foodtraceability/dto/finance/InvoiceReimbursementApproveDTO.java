package com.foodtraceability.dto.finance;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 发票报销审批DTO
 *
 * <p>Sprint 3.1 P0 F-001：用于审批报销申请，含 approved（是否通过）+
 * remark（备注）+ approvedAmount（审批通过金额，单位：分）。</p>
 */
public class InvoiceReimbursementApproveDTO {

    /** 是否审批通过（true-通过 false-拒绝，必填） */
    @NotNull(message = "审批结果不能为空")
    private Boolean approved;

    /** 审批备注/拒绝原因（拒绝时必填） */
    @Size(max = 255, message = "审批备注长度不能超过255个字符")
    private String remark;

    /** 审批通过金额（单位：分，审批通过时必填，必须大于0；拒绝时忽略） */
    private Long approvedAmount;

    public InvoiceReimbursementApproveDTO() {
    }

    public Boolean getApproved() {
        return approved;
    }

    public void setApproved(Boolean approved) {
        this.approved = approved;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Long getApprovedAmount() {
        return approvedAmount;
    }

    public void setApprovedAmount(Long approvedAmount) {
        this.approvedAmount = approvedAmount;
    }
}
