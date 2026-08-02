package com.foodtraceability.dto;

import java.math.BigDecimal;

/**
 * 优惠券验证响应
 */
public class PosCouponVerifyResponse {

    /** 是否有效 */
    private boolean valid;

    /** 优惠券编码 */
    private String code;

    /** 优惠券名称 */
    private String name;

    /** 折扣描述 */
    private String discount;

    /** 折扣金额 */
    private BigDecimal discountAmount;

    /** 最低订单金额 */
    private BigDecimal minAmount;

    /** 有效期描述 */
    private String validPeriod;

    /** 提示消息 */
    private String message;

    public PosCouponVerifyResponse() {
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDiscount() {
        return discount;
    }

    public void setDiscount(String discount) {
        this.discount = discount;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public BigDecimal getMinAmount() {
        return minAmount;
    }

    public void setMinAmount(BigDecimal minAmount) {
        this.minAmount = minAmount;
    }

    public String getValidPeriod() {
        return validPeriod;
    }

    public void setValidPeriod(String validPeriod) {
        this.validPeriod = validPeriod;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
