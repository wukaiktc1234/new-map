package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.foodtraceability.dto.PosCouponApplyResponse;
import com.foodtraceability.dto.PosCouponVerifyResponse;
import com.foodtraceability.entity.Coupon;
import com.foodtraceability.mapper.CouponMapper;
import com.foodtraceability.service.CouponService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * 优惠券服务实现类
 */
@Service
public class CouponServiceImpl implements CouponService {

    private static final Logger log = LoggerFactory.getLogger(CouponServiceImpl.class);

    private final CouponMapper couponMapper;

    /**
     * 构造器注入
     * @param couponMapper 优惠券数据访问层
     */
    public CouponServiceImpl(CouponMapper couponMapper) {
        this.couponMapper = couponMapper;
    }

    /**
     * 验证优惠券有效性
     * @param code 优惠券编码
     * @return 验证结果
     */
    @Override
    public PosCouponVerifyResponse verifyCoupon(String code) {
        log.info("验证优惠券: {}", code);
        PosCouponVerifyResponse response = new PosCouponVerifyResponse();

        // 校验券码非空
        if (code == null || code.trim().isEmpty()) {
            response.setValid(false);
            response.setMessage("请输入券码");
            return response;
        }

        // 根据编码查询优惠券
        Coupon coupon = getCouponByCode(code.trim().toUpperCase());
        if (coupon == null) {
            response.setValid(false);
            response.setMessage("券码无效或已过期");
            return response;
        }

        // 校验有效期
        LocalDateTime now = LocalDateTime.now();
        if (coupon.getValidFrom() != null && now.isBefore(coupon.getValidFrom())) {
            response.setValid(false);
            response.setMessage("优惠券尚未生效");
            return response;
        }
        if (coupon.getValidTo() != null && now.isAfter(coupon.getValidTo())) {
            response.setValid(false);
            response.setMessage("券码已过期");
            return response;
        }

        // 校验状态
        if (!"active".equals(coupon.getStatus())) {
            response.setValid(false);
            response.setMessage("券码已使用");
            return response;
        }

        // 校验使用次数
        if (coupon.getUsageLimit() != null && coupon.getUsageLimit() > 0
                && coupon.getUsedCount() != null && coupon.getUsedCount() >= coupon.getUsageLimit()) {
            response.setValid(false);
            response.setMessage("优惠券已达使用上限");
            return response;
        }

        // 验证通过，构建响应
        response.setValid(true);
        response.setCode(coupon.getCode());
        response.setName(coupon.getName());
        response.setDiscount(buildDiscountDescription(coupon));
        response.setDiscountAmount(coupon.getDiscountValue());
        response.setMinAmount(coupon.getMinOrderAmount());
        if (coupon.getValidFrom() != null && coupon.getValidTo() != null) {
            response.setValidPeriod(coupon.getValidFrom().toLocalDate().toString()
                    + " 至 " + coupon.getValidTo().toLocalDate().toString());
        }
        return response;
    }

    /**
     * 应用优惠券计算折扣
     * @param code 优惠券编码
     * @param orderAmount 订单金额
     * @return 应用结果
     */
    @Override
    public PosCouponApplyResponse applyCoupon(String code, BigDecimal orderAmount) {
        log.info("应用优惠券: {}, 订单金额: {}", code, orderAmount);
        PosCouponApplyResponse response = new PosCouponApplyResponse();

        // 查询优惠券
        Coupon coupon = getCouponByCode(code.trim().toUpperCase());
        if (coupon == null || !"active".equals(coupon.getStatus())) {
            response.setSuccess(false);
            response.setMessage("券码无效");
            return response;
        }

        // 校验有效期
        LocalDateTime now = LocalDateTime.now();
        if (coupon.getValidFrom() != null && now.isBefore(coupon.getValidFrom())) {
            response.setSuccess(false);
            response.setMessage("优惠券尚未生效");
            return response;
        }
        if (coupon.getValidTo() != null && now.isAfter(coupon.getValidTo())) {
            response.setSuccess(false);
            response.setMessage("优惠券已过期");
            return response;
        }

        // 校验使用次数
        if (coupon.getUsageLimit() != null && coupon.getUsageLimit() > 0
                && coupon.getUsedCount() != null && coupon.getUsedCount() >= coupon.getUsageLimit()) {
            response.setSuccess(false);
            response.setMessage("优惠券已达使用上限");
            return response;
        }

        // 校验最低订单金额
        if (coupon.getMinOrderAmount() != null && orderAmount.compareTo(coupon.getMinOrderAmount()) < 0) {
            response.setSuccess(false);
            response.setMessage("订单金额需满 ¥" + coupon.getMinOrderAmount() + " 才能使用此优惠券");
            return response;
        }

        // 计算折扣金额
        BigDecimal discount = calculateDiscount(coupon, orderAmount);
        BigDecimal finalAmount = orderAmount.subtract(discount);
        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
            finalAmount = BigDecimal.ZERO;
        }

        response.setSuccess(true);
        response.setDiscountAmount(discount);
        response.setFinalAmount(finalAmount);
        response.setMessage("优惠券应用成功");
        return response;
    }

    /**
     * 根据编码查询优惠券
     * @param code 优惠券编码
     * @return 优惠券实体
     */
    private Coupon getCouponByCode(String code) {
        LambdaQueryWrapper<Coupon> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Coupon::getCode, code);
        return couponMapper.selectOne(wrapper);
    }

    /**
     * 构建折扣描述
     * @param coupon 优惠券实体
     * @return 折扣描述文本
     */
    private String buildDiscountDescription(Coupon coupon) {
        if ("fixed".equals(coupon.getDiscountType())) {
            return "立减" + coupon.getDiscountValue() + "元";
        } else if ("percent".equals(coupon.getDiscountType())) {
            return "打" + coupon.getDiscountValue() + "折";
        }
        return coupon.getDiscountValue().toPlainString();
    }

    /**
     * 计算折扣金额
     * @param coupon 优惠券实体
     * @param orderAmount 订单金额
     * @return 折扣金额
     */
    private BigDecimal calculateDiscount(Coupon coupon, BigDecimal orderAmount) {
        BigDecimal discount;
        if ("fixed".equals(coupon.getDiscountType())) {
            // 固定金额折扣
            discount = coupon.getDiscountValue();
        } else if ("percent".equals(coupon.getDiscountType())) {
            // 百分比折扣：discountValue表示折扣率，如8.5表示85折
            discount = orderAmount.multiply(BigDecimal.ONE.subtract(
                    coupon.getDiscountValue().divide(BigDecimal.TEN, 2, RoundingMode.HALF_UP)));
        } else {
            discount = coupon.getDiscountValue();
        }

        // 如果有最大折扣限制
        if (coupon.getMaxDiscount() != null && discount.compareTo(coupon.getMaxDiscount()) > 0) {
            discount = coupon.getMaxDiscount();
        }

        return discount;
    }
}
