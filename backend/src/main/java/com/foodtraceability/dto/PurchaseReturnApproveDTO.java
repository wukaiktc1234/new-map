package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 采购退货审批DTO
 */
@Schema(description = "采购退货审批请求")
public class PurchaseReturnApproveDTO {

    @NotBlank(message = "审批状态不能为空")
    @Pattern(regexp = "approved|rejected", message = "审批状态只能是 approved 或 rejected")
    @Schema(description = "审批状态：approved(通过)/rejected(拒绝)", requiredMode = Schema.RequiredMode.REQUIRED)
    private String status;

    @Schema(description = "审批备注")
    private String remark;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
