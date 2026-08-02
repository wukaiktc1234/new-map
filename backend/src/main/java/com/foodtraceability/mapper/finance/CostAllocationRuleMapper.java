package com.foodtraceability.mapper.finance;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.finance.CostAllocationRule;
import org.apache.ibatis.annotations.Mapper;

/**
 * 成本分摊规则Mapper接口
 */
@Mapper
public interface CostAllocationRuleMapper extends BaseMapper<CostAllocationRule> {
}
