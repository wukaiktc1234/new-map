package com.foodtraceability.mapper.marketing;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.marketing.RechargeFinanceLog;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * 储值财务流水 Mapper
 */
@Mapper
@Repository
public interface RechargeFinanceLogMapper extends BaseMapper<RechargeFinanceLog> {
}
