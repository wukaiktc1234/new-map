package com.foodtraceability.dto.store.operation;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 招聘审批更新DTO
 * 用于审批操作，支持通过或驳回
 *
 * <h2>使用场景</h2>
 * <ul>
 *   <li><b>审批通过</b>：设置approvalStatus='approved'，填写approverId</li>
 *   <li><b>驳回申请</b>：设置approvalStatus='rejected'，填写rejectionReason（必填）</li>
 * </ul>
 */
@Schema(description = "招聘审批更新DTO")
public class RecruitmentApprovalUpdateDTO {

    /**
     * 审批状态
     * approved: 审批通过
     * rejected: 审批驳回
     */
    @NotBlank(message = "审批状态不能为空")
    @Pattern(regexp = "^(approved|rejected)$", message = "审批状态只能是approved或rejected")
    @Schema(description = "审批状态（approved=通过 rejected=驳回）", example = "approved", requiredMode = Schema.RequiredMode.REQUIRED)
    private String approvalStatus;

    /** 审批人ID */
    @Schema(description = "审批人ID", example = "approver001")
    private String approverId;

    /**
     * 驳回原因
     * 当approvalStatus为rejected时必填
     * 需要详细说明驳回理由
     */
    @Size(min = 10, max = 500, message = "驳回原因长度必须在10-500个字符之间")
    @Schema(description = "驳回原因（10-500字，驳回时必填）", example = "该应聘者缺乏相关岗位的工作经验...")
    private String rejectionReason;

    // ==================== Getter & Setter ====================

    public String getApprovalStatus() {
        return approvalStatus;
    }

    public void setApprovalStatus(String approvalStatus) {
        this.approvalStatus = approvalStatus;
    }

    public String getApproverId() {
        return approverId;
    }

    public void setApproverId(String approverId) {
        this.approverId = approverId;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }
}
