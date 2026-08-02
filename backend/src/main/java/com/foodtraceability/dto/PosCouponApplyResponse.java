package com.foodtraceability.dto;

import java.math.BigDecimal;

/**
 * 优惠券应用响应
 */
public class PosCouponApplyResponse {

    /** 是否成功 */
    private boolean success;

    /** 折扣金额 */
    private BigDecimal discountAmount;

    /** 最终金额 */
    private BigDecimal finalAmount;

    /** 提示消息 */
    private String message;

    public PosCouponApplyResponse() {
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public BigDecimal getFinalAmount() {
        return finalAmount;
    }

    public void setFinalAmount(BigDecimal finalAmount) {
        this.finalAmount = finalAmount;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
