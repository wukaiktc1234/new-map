package com.foodtraceability.service;

import com.foodtraceability.dto.PosCouponApplyResponse;
import com.foodtraceability.dto.PosCouponVerifyResponse;

import java.math.BigDecimal;

/**
 * 优惠券服务接口
 */
public interface CouponService {

    /**
     * 验证优惠券有效性
     * @param code 优惠券编码
     * @return 验证结果
     */
    PosCouponVerifyResponse verifyCoupon(String code);

    /**
     * 应用优惠券计算折扣
     * @param code 优惠券编码
     * @param orderAmount 订单金额
     * @return 应用结果
     */
    PosCouponApplyResponse applyCoupon(String code, BigDecimal orderAmount);
}
