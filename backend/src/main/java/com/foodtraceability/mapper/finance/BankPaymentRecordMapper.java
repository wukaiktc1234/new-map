package com.foodtraceability.mapper.finance;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.finance.BankPaymentRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 银行打款记录Mapper
 */
@Mapper
public interface BankPaymentRecordMapper extends BaseMapper<BankPaymentRecord> {
}