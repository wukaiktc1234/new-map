package com.foodtraceability.dto.marketing;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 会员充值请求DTO
 * 对应前端 POST /v1/recharge-records/recharge
 */
@Schema(description = "会员充值请求")
public class RechargeRequestDTO {

    @Schema(description = "会员ID", example = "1")
    @NotBlank(message = "会员ID不能为空")
    private String memberId;

    @Schema(description = "充值方案ID", example = "2")
    @NotBlank(message = "充值方案ID不能为空")
    private String planId;

    @Schema(description = "支付方式: wechat/alipay/cash/bank_card/balance", example = "wechat")
    @NotBlank(message = "支付方式不能为空")
    private String paymentMethod;

    // ==================== Getter & Setter ====================

    public String getMemberId() { return memberId; }
    public void setMemberId(String memberId) { this.memberId = memberId; }

    public String getPlanId() { return planId; }
    public void setPlanId(String planId) { this.planId = planId; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
}
