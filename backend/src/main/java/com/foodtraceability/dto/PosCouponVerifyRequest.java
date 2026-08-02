package com.foodtraceability.dto;

/**
 * 优惠券验证请求
 */
public class PosCouponVerifyRequest {

    /** 优惠券编码 */
    private String code;

    public PosCouponVerifyRequest() {
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
