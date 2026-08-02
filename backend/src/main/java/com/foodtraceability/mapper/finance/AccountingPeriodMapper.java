package com.foodtraceability.mapper.finance;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.finance.AccountingPeriod;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会计期间Mapper接口
 */
@Mapper
public interface AccountingPeriodMapper extends BaseMapper<AccountingPeriod> {
}
