package com.foodtraceability.mapper.finance;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.finance.TaxRateConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * 税率配置Mapper接口
 */
@Mapper
public interface TaxRateConfigMapper extends BaseMapper<TaxRateConfig> {
}
