package com.foodtraceability.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;
import java.util.List;

/**
 * 支付订单请求DTO
 * 支持多种支付方式组合支付
 */
@Schema(description = "支付订单请求")
public class OrderPayDTO {

    /**
     * 支付方式列表
     * 每个元素包含支付方式和金额
     */
    @NotEmpty(message = "支付方式列表不能为空")
    @Valid
    @Schema(description = "支付方式列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<PaymentDetail> payments;

    /** 操作人ID */
    @Schema(description = "操作人ID")
    private Long operatorId;

    /** 备注 */
    @Schema(description = "备注")
    private String remark;

    /**
     * 支付明细（内部类）
     */
    @Schema(description = "支付明细")
    public static class PaymentDetail {

        /** 支付方式：1现金 2微信 3支付宝 4银行卡 5积分 */
        @NotNull(message = "支付方式不能为空")
        @Min(value = 1, message = "支付方式无效")
        @Max(value = 5, message = "支付方式无效")
        @Schema(description = "支付方式: 1现金 2微信 3支付宝 4银行卡 5积分", requiredMode = Schema.RequiredMode.REQUIRED)
        private Integer paymentMethod;

        /** 支付金额（分） */
        @NotNull(message = "支付金额不能为空")
        @Min(value = 1, message = "支付金额必须大于0")
        @Schema(description = "支付金额（分）", requiredMode = Schema.RequiredMode.REQUIRED)
        private Long amount;

        /** 交易流水号（线上支付时必填） */
        @Schema(description = "交易流水号")
        private String transactionNo;

        // Getter和Setter
        public Integer getPaymentMethod() { return paymentMethod; }
        public void setPaymentMethod(Integer paymentMethod) { this.paymentMethod = paymentMethod; }
        public Long getAmount() { return amount; }
        public void setAmount(Long amount) { this.amount = amount; }
        public String getTransactionNo() { return transactionNo; }
        public void setTransactionNo(String transactionNo) { this.transactionNo = transactionNo; }
    }

    // Getter和Setter方法
    public List<PaymentDetail> getPayments() { return payments; }
    public void setPayments(List<PaymentDetail> payments) { this.payments = payments; }
    public Long getOperatorId() { return operatorId; }
    public void setOperatorId(Long operatorId) { this.operatorId = operatorId; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
