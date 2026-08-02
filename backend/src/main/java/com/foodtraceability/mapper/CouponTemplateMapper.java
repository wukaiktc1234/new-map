package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.CouponTemplate;
import org.apache.ibatis.annotations.Mapper;

/**
 * 优惠券模板数据访问层
 */
@Mapper
public interface CouponTemplateMapper extends BaseMapper<CouponTemplate> {
}
