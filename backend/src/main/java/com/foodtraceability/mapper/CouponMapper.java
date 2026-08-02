package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.Coupon;
import org.apache.ibatis.annotations.Mapper;

/**
 * 优惠券数据访问层
 */
@Mapper
public interface CouponMapper extends BaseMapper<Coupon> {
}
