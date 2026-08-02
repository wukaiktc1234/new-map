package com.foodtraceability.controller.pos;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.PosCouponApplyRequest;
import com.foodtraceability.dto.PosCouponApplyResponse;
import com.foodtraceability.dto.PosCouponVerifyRequest;
import com.foodtraceability.dto.PosCouponVerifyResponse;
import com.foodtraceability.service.CouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 优惠券控制器
 * <p>
 * S1 修复：类级别 @PreAuthorize 统一要求 POS 收银角色，
 * 防止非授权账号调用优惠券验证/应用接口。
 * </p>
 */
@RestController
@RequestMapping("/v1/pos/coupons")
@Tag(name = "优惠券API", description = "优惠券相关接口")
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_CASHIER', 'ROLE_POS_OPERATOR', '*')")
public class PosCouponController {

    private static final Logger log = LoggerFactory.getLogger(PosCouponController.class);

    private final CouponService couponService;

    /**
     * 构造器注入
     * @param couponService 优惠券服务
     */
    public PosCouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    /**
     * 验证优惠券
     * @param request 验证请求
     * @return 验证结果
     */
    @PostMapping("/verify")
    @Operation(summary = "验证优惠券")
    public Result<PosCouponVerifyResponse> verifyCoupon(@RequestBody PosCouponVerifyRequest request) {
        PosCouponVerifyResponse response = couponService.verifyCoupon(request.getCode());
        return Result.success(response);
    }

    /**
     * 应用优惠券
     * @param request 应用请求
     * @return 应用结果
     */
    @PostMapping("/apply")
    @Operation(summary = "应用优惠券")
    public Result<PosCouponApplyResponse> applyCoupon(@RequestBody PosCouponApplyRequest request) {
        PosCouponApplyResponse response = couponService.applyCoupon(request.getCode(), request.getOrderAmount());
        return Result.success(response);
    }
}
