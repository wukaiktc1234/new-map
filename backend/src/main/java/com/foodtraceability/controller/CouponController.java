package com.foodtraceability.controller;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.PageResult;
import com.foodtraceability.dto.marketing.CouponReceiveDTO;
import com.foodtraceability.dto.marketing.CouponTemplateCreateDTO;
import com.foodtraceability.entity.CouponTemplate;
import com.foodtraceability.entity.MemberCoupon;
import com.foodtraceability.service.CouponTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 优惠券管理控制器
 *
 * 端点说明：
 * 1. 模板管理：POST/GET /templates，PUT/DELETE /templates/{templateId}
 * 2. 优惠券实例：GET/PUT/DELETE /{couponId}，GET /page
 * 3. 用户领券/用券：POST /receive，GET /member/{memberId}/available|all，POST /{couponId}/use，PUT /{couponId}/void
 *
 * 路径匹配优先级：字面量路径（templates/page/member/receive/expire）优先于 {couponId} 匹配
 */
@RestController
@RequestMapping("/v1/coupons")
@Tag(name = "优惠券管理", description = "优惠券模板管理、领券、核销")
public class CouponController {

    private final CouponTemplateService couponService;

    public CouponController(CouponTemplateService couponService) {
        this.couponService = couponService;
    }

    // ========== 模板管理 ==========

    @PostMapping("/templates")
    @Operation(summary = "创建优惠券模板")
    @PreAuthorize("hasAuthority('coupon:manage') or hasAuthority('*')")
    public Result<CouponTemplate> createTemplate(@RequestBody CouponTemplateCreateDTO createDTO) {
        try {
            CouponTemplate template = couponService.createTemplate(createDTO);
            return Result.success(template, "模板创建成功");
        } catch (RuntimeException e) {
            return Result.error(6001, e.getMessage());
        }
    }

    @GetMapping("/templates")
    @Operation(summary = "查询优惠券模板列表")
    @PreAuthorize("hasAuthority('coupon:view') or hasAuthority('*')")
    public Result<List<CouponTemplate>> listTemplates() {
        List<CouponTemplate> list = couponService.list();
        return Result.success(list);
    }

    /**
     * 更新优惠券模板
     */
    @PutMapping("/templates/{templateId}")
    @Operation(summary = "更新优惠券模板")
    @PreAuthorize("hasAuthority('coupon:manage') or hasAuthority('*')")
    public Result<CouponTemplate> updateTemplate(
            @Parameter(description = "模板ID") @PathVariable Long templateId,
            @RequestBody CouponTemplate template) {
        try {
            template.setTemplateId(templateId);
            boolean success = couponService.updateById(template);
            if (!success) {
                return Result.error(6005, "优惠券模板不存在");
            }
            return Result.success(couponService.getById(templateId), "模板更新成功");
        } catch (RuntimeException e) {
            return Result.error(6005, e.getMessage());
        }
    }

    /**
     * 删除优惠券模板（逻辑删除）
     */
    @DeleteMapping("/templates/{templateId}")
    @Operation(summary = "删除优惠券模板（逻辑删除）")
    @PreAuthorize("hasAuthority('coupon:manage') or hasAuthority('*')")
    public Result<Void> deleteTemplate(
            @Parameter(description = "模板ID") @PathVariable Long templateId) {
        try {
            boolean success = couponService.removeById(templateId);
            if (!success) {
                return Result.error(6006, "优惠券模板不存在");
            }
            return Result.success(null, "模板删除成功");
        } catch (RuntimeException e) {
            return Result.error(6006, e.getMessage());
        }
    }

    // ========== 优惠券实例管理 ==========

    /**
     * 分页查询优惠券列表
     * 字面量路径 /page 优先于 /{couponId} 匹配
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询优惠券列表")
    @PreAuthorize("hasAuthority('coupon:view') or hasAuthority('*')")
    public Result<PageResult<MemberCoupon>> getCouponPage(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "会员ID") @RequestParam(required = false) Long memberId,
            @Parameter(description = "模板ID") @RequestParam(required = false) Long templateId,
            @Parameter(description = "状态：0未使用 1已使用 2已过期 3已作废") @RequestParam(required = false) Integer status) {
        PageResult<MemberCoupon> result = couponService.getCouponPage(page, size, memberId, templateId, status);
        return Result.success(result, "查询优惠券列表成功");
    }

    /**
     * 根据ID获取优惠券详情
     */
    @GetMapping("/{couponId}")
    @Operation(summary = "根据ID获取优惠券详情")
    @PreAuthorize("hasAuthority('coupon:view') or hasAuthority('*')")
    public Result<MemberCoupon> getCouponById(
            @Parameter(description = "优惠券ID") @PathVariable Long couponId) {
        MemberCoupon coupon = couponService.getCouponById(couponId);
        if (coupon == null) {
            return Result.error(6007, "优惠券不存在");
        }
        return Result.success(coupon, "获取优惠券详情成功");
    }

    /**
     * 更新优惠券信息
     * 仅允许更新过期时间、状态等可变字段
     */
    @PutMapping("/{couponId}")
    @Operation(summary = "更新优惠券信息")
    @PreAuthorize("hasAuthority('coupon:manage') or hasAuthority('*')")
    public Result<MemberCoupon> updateCoupon(
            @Parameter(description = "优惠券ID") @PathVariable Long couponId,
            @RequestBody MemberCoupon coupon) {
        try {
            MemberCoupon updated = couponService.updateCoupon(couponId, coupon);
            return Result.success(updated, "更新优惠券成功");
        } catch (RuntimeException e) {
            return Result.error(6008, e.getMessage());
        }
    }

    /**
     * 删除优惠券（逻辑删除）
     */
    @DeleteMapping("/{couponId}")
    @Operation(summary = "删除优惠券（逻辑删除）")
    @PreAuthorize("hasAuthority('coupon:manage') or hasAuthority('*')")
    public Result<Void> deleteCoupon(
            @Parameter(description = "优惠券ID") @PathVariable Long couponId) {
        try {
            boolean success = couponService.deleteCoupon(couponId);
            if (!success) {
                return Result.error(6009, "优惠券不存在");
            }
            return Result.success(null, "删除优惠券成功");
        } catch (RuntimeException e) {
            return Result.error(6009, e.getMessage());
        }
    }

    // ========== 用户领券/用券 ==========

    @PostMapping("/receive")
    @Operation(summary = "领取优惠券")
    @PreAuthorize("hasAuthority('coupon:view') or hasAuthority('*')")
    public Result<MemberCoupon> receive(@RequestBody CouponReceiveDTO receiveDTO) {
        try {
            MemberCoupon coupon = couponService.receiveCoupon(receiveDTO);
            return Result.success(coupon, "领券成功");
        } catch (RuntimeException e) {
            return Result.error(6002, e.getMessage());
        }
    }

    @GetMapping("/member/{memberId}/available")
    @Operation(summary = "获取会员可用优惠券")
    @PreAuthorize("hasAuthority('coupon:view') or hasAuthority('*')")
    public Result<List<MemberCoupon>> getAvailable(@PathVariable Long memberId) {
        List<MemberCoupon> coupons = couponService.getAvailableCoupons(memberId);
        return Result.success(coupons);
    }

    @GetMapping("/member/{memberId}/all")
    @Operation(summary = "获取会员所有优惠券")
    @PreAuthorize("hasAuthority('coupon:view') or hasAuthority('*')")
    public Result<List<MemberCoupon>> getAllByMember(@PathVariable Long memberId,
                                                     @RequestParam(required = false) Integer status) {
        List<MemberCoupon> coupons = couponService.getMemberCoupons(memberId, status);
        return Result.success(coupons);
    }

    @PostMapping("/{couponId}/use")
    @Operation(summary = "核销优惠券（订单使用时调用）")
    @PreAuthorize("hasAuthority('coupon:manage') or hasAuthority('*')")
    public Result<Boolean> useCoupon(@PathVariable Long couponId,
                                    @RequestParam Long orderId,
                                    @RequestParam String orderNo) {
        boolean success = couponService.useCoupon(couponId, orderId, orderNo);
        if (success) {
            return Result.success(true, "核销成功");
        }
        return Result.error(6003, "核销失败，优惠券可能已使用或不存在");
    }

    @PutMapping("/{couponId}/void")
    @Operation(summary = "作废优惠券")
    @PreAuthorize("hasAuthority('coupon:manage') or hasAuthority('*')")
    public Result<Void> voidCoupon(@PathVariable Long couponId) {
        try {
            couponService.voidCoupon(couponId);
            return Result.success(null, "作废成功");
        } catch (RuntimeException e) {
            return Result.error(6004, e.getMessage());
        }
    }

    @PostMapping("/expire")
    @Operation(summary = "手动触发过期处理（管理员）")
    @PreAuthorize("hasAuthority('coupon:manage') or hasAuthority('*')")
    public Result<Integer> expireCoupons() {
        int count = couponService.expireCoupons();
        return Result.success(count, "过期处理完成，标记" + count + "张");
    }
}
