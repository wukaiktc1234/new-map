package com.foodtraceability.mapper.finance;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.finance.BankAccount;
import org.apache.ibatis.annotations.Mapper;

/**
 * 银行账户Mapper接口
 */
@Mapper
public interface BankAccountMapper extends BaseMapper<BankAccount> {
}
