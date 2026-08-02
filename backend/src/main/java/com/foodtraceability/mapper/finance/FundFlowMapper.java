package com.foodtraceability.mapper.finance;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.finance.FundFlow;
import org.apache.ibatis.annotations.Mapper;

/**
 * 资金流水Mapper接口
 */
@Mapper
public interface FundFlowMapper extends BaseMapper<FundFlow> {
}
