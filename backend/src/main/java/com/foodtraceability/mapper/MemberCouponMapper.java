package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.MemberCoupon;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 用户优惠券数据访问层
 */
@Mapper
public interface MemberCouponMapper extends BaseMapper<MemberCoupon> {

    /**
     * 查询会员可用的优惠券
     */
    @Select("SELECT c.*, t.template_name, t.discount_type, t.discount_value, t.min_consumption " +
            "FROM member_coupon c " +
            "LEFT JOIN coupon_template t ON c.template_id = t.template_id " +
            "WHERE c.member_id = #{memberId} AND c.status = 0 " +
            "AND c.expiry_time > NOW() " +
            "ORDER BY c.expiry_time ASC")
    List<MemberCoupon> selectAvailableByMemberId(@Param("memberId") Long memberId);

    /**
     * 统计会员已领取某模板的优惠券数量
     */
    @Select("SELECT COUNT(*) FROM member_coupon WHERE template_id = #{templateId} AND member_id = #{memberId} AND deleted = 0")
    long countByTemplateAndMember(@Param("templateId") Long templateId, @Param("memberId") Long memberId);

    /**
     * 统计模板已领取数量
     */
    @Select("SELECT COUNT(*) FROM member_coupon WHERE template_id = #{templateId} AND deleted = 0")
    long countByTemplateId(@Param("templateId") Long templateId);

    /**
     * 核销优惠券（标记为已使用）
     */
    @Update("UPDATE member_coupon SET status = 1, use_time = NOW(), order_id = #{orderId}, order_no = #{orderNo} " +
            "WHERE coupon_id = #{couponId} AND status = 0")
    int useCoupon(@Param("couponId") Long couponId,
                  @Param("orderId") Long orderId,
                  @Param("orderNo") String orderNo);

    /**
     * 标记优惠券为已过期
     */
    @Update("UPDATE member_coupon SET status = 2 WHERE status = 0 AND expiry_time < NOW() AND deleted = 0")
    int expireCoupons();

    /**
     * 作废优惠券
     */
    @Update("UPDATE member_coupon SET status = 3 WHERE coupon_id = #{couponId} AND deleted = 0")
    int voidCoupon(@Param("couponId") Long couponId);
}
