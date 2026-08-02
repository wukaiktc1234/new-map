package com.foodtraceability.dto.approval;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 审批操作参数 DTO
 * 用于审批通过 / 驳回 / 撤回 等操作
 */
public class ApprovalActionDTO {

    /** 业务ID */
    @NotBlank(message = "业务ID不能为空")
    @Size(max = 64, message = "业务ID长度不能超过64个字符")
    private String businessId;

    /** 业务类型 */
    @NotBlank(message = "业务类型不能为空")
    @Size(max = 50, message = "业务类型长度不能超过50个字符")
    private String businessType;

    /** 审批意见 */
    @Size(max = 500, message = "审批意见长度不能超过500个字符")
    private String comment;

    public String getBusinessId() {
        return businessId;
    }

    public void setBusinessId(String businessId) {
        this.businessId = businessId;
    }

    public String getBusinessType() {
        return businessType;
    }

    public void setBusinessType(String businessType) {
        this.businessType = businessType;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
