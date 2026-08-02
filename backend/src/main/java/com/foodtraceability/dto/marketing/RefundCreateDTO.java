package com.foodtraceability.dto.marketing;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 退款申请创建DTO
 * 对应前端 POST /v1/refunds
 * 金额字段以元为单位（字符串），由Service转换为分存储
 */
@Schema(description = "退款申请创建请求")
public class RefundCreateDTO {

    @Schema(description = "关联充值记录ID", example = "1")
    @NotBlank(message = "充值记录ID不能为空")
    private String rechargeRecordId;

    @Schema(description = "申请退款金额（元）", example = "200.00")
    @NotBlank(message = "申请退款金额不能为空")
    private String requestedAmount;

    @Schema(description = "退款原因", example = "客户申请部分退款")
    @NotBlank(message = "退款原因不能为空")
    private String refundReason;

    // ==================== Getter & Setter ====================

    public String getRechargeRecordId() { return rechargeRecordId; }
    public void setRechargeRecordId(String rechargeRecordId) { this.rechargeRecordId = rechargeRecordId; }

    public String getRequestedAmount() { return requestedAmount; }
    public void setRequestedAmount(String requestedAmount) { this.requestedAmount = requestedAmount; }

    public String getRefundReason() { return refundReason; }
    public void setRefundReason(String refundReason) { this.refundReason = refundReason; }
}
