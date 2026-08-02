package com.foodtraceability.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.dto.PageResult;
import com.foodtraceability.dto.marketing.CouponReceiveDTO;
import com.foodtraceability.dto.marketing.CouponTemplateCreateDTO;
import com.foodtraceability.entity.CouponTemplate;
import com.foodtraceability.entity.MemberCoupon;

import java.util.List;

/**
 * 优惠券服务接口
 * 同时承担优惠券模板与会员优惠券实例的管理职责
 */
public interface CouponTemplateService extends IService<CouponTemplate> {

    /** 创建优惠券模板 */
    CouponTemplate createTemplate(CouponTemplateCreateDTO createDTO);

    /** 用户领取优惠券 */
    MemberCoupon receiveCoupon(CouponReceiveDTO receiveDTO);

    /** 核销优惠券（订单使用时） */
    boolean useCoupon(Long couponId, Long orderId, String orderNo);

    /** 查询会员可用优惠券 */
    List<MemberCoupon> getAvailableCoupons(Long memberId);

    /** 查询会员所有优惠券 */
    List<MemberCoupon> getMemberCoupons(Long memberId, Integer status);

    /** 过期优惠券处理（定时任务） */
    int expireCoupons();

    /** 作废优惠券 */
    void voidCoupon(Long couponId);

    /**
     * 根据ID获取会员优惠券详情
     *
     * @param couponId 优惠券ID
     * @return 优惠券实例，不存在返回null
     */
    MemberCoupon getCouponById(Long couponId);

    /**
     * 更新会员优惠券信息
     * 仅允许更新过期时间、状态等可变字段
     *
     * @param couponId 优惠券ID
     * @param coupon   含可更新字段的实体
     * @return 更新后的优惠券实例
     */
    MemberCoupon updateCoupon(Long couponId, MemberCoupon coupon);

    /**
     * 删除会员优惠券（逻辑删除）
     *
     * @param couponId 优惠券ID
     * @return 是否删除成功
     */
    boolean deleteCoupon(Long couponId);

    /**
     * 分页查询会员优惠券列表
     *
     * @param page       页码
     * @param size       每页条数
     * @param memberId   会员ID（可选）
     * @param templateId 模板ID（可选）
     * @param status     状态（可选）：0未使用 1已使用 2已过期 3已作废
     * @return 分页结果
     */
    PageResult<MemberCoupon> getCouponPage(Integer page, Integer size,
                                           Long memberId, Long templateId, Integer status);
}
