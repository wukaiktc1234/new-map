package com.foodtraceability.mapper.finance;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.finance.ProfitStatement;
import org.apache.ibatis.annotations.Mapper;

/**
 * 利润表数据Mapper接口
 */
@Mapper
public interface ProfitStatementMapper extends BaseMapper<ProfitStatement> {
}
