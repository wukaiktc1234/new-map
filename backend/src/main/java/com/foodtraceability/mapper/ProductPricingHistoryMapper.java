package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.ProductPricingHistory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 产品定价历史Mapper
 */
@Mapper
public interface ProductPricingHistoryMapper extends BaseMapper<ProductPricingHistory> {
}
