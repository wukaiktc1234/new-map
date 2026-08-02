package com.foodtraceability.dto;

import java.math.BigDecimal;

/**
 * 优惠券应用请求
 */
public class PosCouponApplyRequest {

    /** 优惠券编码 */
    private String code;

    /** 订单金额 */
    private BigDecimal orderAmount;

    public PosCouponApplyRequest() {
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public BigDecimal getOrderAmount() {
        return orderAmount;
    }

    public void setOrderAmount(BigDecimal orderAmount) {
        this.orderAmount = orderAmount;
    }
}
