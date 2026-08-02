package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 采购到货单关闭请求
 */
@Schema(description = "采购到货单关闭请求")
public class PurchaseArrivalCloseDTO {

    @NotBlank(message = "关闭原因不能为空")
    @Size(max = 50, message = "关闭原因不能超过50个字符")
    @Schema(description = "关闭原因（manual_closed-手动关闭, auto_closed_overdue-超期自动关闭）", requiredMode = Schema.RequiredMode.REQUIRED)
    private String closeReason;

    @Size(max = 500, message = "备注不能超过500个字符")
    @Schema(description = "备注")
    private String remark;

    public String getCloseReason() {
        return closeReason;
    }

    public void setCloseReason(String closeReason) {
        this.closeReason = closeReason;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
