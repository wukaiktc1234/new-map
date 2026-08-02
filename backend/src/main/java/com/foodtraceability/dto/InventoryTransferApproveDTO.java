package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 调拨单审批DTO
 */
@Schema(description = "调拨单审批DTO")
public class InventoryTransferApproveDTO {

    @NotBlank(message = "审批状态不能为空")
    @Schema(description = "审批状态（approved:通过 rejected:驳回）", example = "approved", required = true)
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
