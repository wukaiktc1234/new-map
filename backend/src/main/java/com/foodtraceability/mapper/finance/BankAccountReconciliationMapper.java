package com.foodtraceability.mapper.finance;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.finance.BankAccountReconciliation;
import org.apache.ibatis.annotations.Mapper;

/**
 * 银行账户对账结果Mapper接口
 */
@Mapper
public interface BankAccountReconciliationMapper extends BaseMapper<BankAccountReconciliation> {
}