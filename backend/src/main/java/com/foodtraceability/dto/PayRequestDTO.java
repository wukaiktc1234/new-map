package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "支付请求DTO")
public class PayRequestDTO {
    @Schema(description = "订单ID")
    @NotBlank(message = "订单ID不能为空")
    @Size(max = 64, message = "订单ID长度不能超过64个字符")
    private String orderId;
    @Schema(description = "支付方式: 微信支付/余额支付/现金")
    @NotBlank(message = "支付方式不能为空")
    @Pattern(regexp = "^(微信支付|余额支付|现金|支付宝|银行卡|wechat|balance|cash|alipay|card|WECHAT|BALANCE|CASH|ALIPAY|CARD)$", message = "支付方式不合法")
    private String paymentMethod;
    @Schema(description = "用户openid")
    @Size(max = 128, message = "openid长度不能超过128个字符")
    private String openid;
    @Schema(description = "支付金额")
    @NotNull(message = "支付金额不能为空")
    @DecimalMin(value = "0.01", message = "支付金额必须大于0")
    @DecimalMax(value = "999999.00", message = "支付金额不能超过999999.00")
    private java.math.BigDecimal amount;

    public PayRequestDTO() {
    }

    public String getOrderId() {
        return this.orderId;
    }

    public String getPaymentMethod() {
        return this.paymentMethod;
    }

    public String getOpenid() {
        return this.openid;
    }

    public java.math.BigDecimal getAmount() {
        return this.amount;
    }

    public void setOrderId(final String orderId) {
        this.orderId = orderId;
    }

    public void setPaymentMethod(final String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public void setOpenid(final String openid) {
        this.openid = openid;
    }

    public void setAmount(final java.math.BigDecimal amount) {
        this.amount = amount;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof PayRequestDTO)) return false;
        final PayRequestDTO other = (PayRequestDTO) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$orderId = this.getOrderId();
        final java.lang.Object other$orderId = other.getOrderId();
        if (this$orderId == null ? other$orderId != null : !this$orderId.equals(other$orderId)) return false;
        final java.lang.Object this$paymentMethod = this.getPaymentMethod();
        final java.lang.Object other$paymentMethod = other.getPaymentMethod();
        if (this$paymentMethod == null ? other$paymentMethod != null : !this$paymentMethod.equals(other$paymentMethod)) return false;
        final java.lang.Object this$openid = this.getOpenid();
        final java.lang.Object other$openid = other.getOpenid();
        if (this$openid == null ? other$openid != null : !this$openid.equals(other$openid)) return false;
        final java.lang.Object this$amount = this.getAmount();
        final java.lang.Object other$amount = other.getAmount();
        if (this$amount == null ? other$amount != null : !this$amount.equals(other$amount)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof PayRequestDTO;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $orderId = this.getOrderId();
        result = result * PRIME + ($orderId == null ? 43 : $orderId.hashCode());
        final java.lang.Object $paymentMethod = this.getPaymentMethod();
        result = result * PRIME + ($paymentMethod == null ? 43 : $paymentMethod.hashCode());
        final java.lang.Object $openid = this.getOpenid();
        result = result * PRIME + ($openid == null ? 43 : $openid.hashCode());
        final java.lang.Object $amount = this.getAmount();
        result = result * PRIME + ($amount == null ? 43 : $amount.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "PayRequestDTO(orderId=" + this.getOrderId() + ", paymentMethod=" + this.getPaymentMethod() + ", openid=" + this.getOpenid() + ", amount=" + this.getAmount() + ")";
    }
}
