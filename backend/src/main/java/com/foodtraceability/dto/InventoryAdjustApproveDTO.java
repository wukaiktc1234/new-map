package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * 库存调整单审批DTO
 */
@Schema(description = "库存调整单审批DTO")
public class InventoryAdjustApproveDTO {

    @NotNull(message = "审批结果不能为空")
    @Schema(description = "是否通过", example = "true", required = true)
    private Boolean approved;

    @Schema(description = "审批意见")
    private String opinion;

    public Boolean getApproved() {
        return approved;
    }

    public void setApproved(Boolean approved) {
        this.approved = approved;
    }

    public String getOpinion() {
        return opinion;
    }

    public void setOpinion(String opinion) {
        this.opinion = opinion;
    }
}
